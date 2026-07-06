package com.agentopscrm.service;

import com.agentopscrm.client.ApifyClient;
import com.agentopscrm.dto.BulkImportResultResponse;
import com.agentopscrm.dto.DiscoveredLeadResponse;
import com.agentopscrm.dto.LeadSourceRunResponse;
import com.agentopscrm.dto.StartLeadFinderRunRequest;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.DiscoveredLead;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.LeadSourceRun;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.DiscoveredLeadStatus;
import com.agentopscrm.entity.enums.LeadSourceRunStatus;
import com.agentopscrm.entity.enums.LeadStatus;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.DiscoveredLeadRepository;
import com.agentopscrm.repository.LeadRepository;
import com.agentopscrm.repository.LeadSourceRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for outbound lead discovery via Apify (F-010 Apify Lead Finder).
 *
 * Responsibilities:
 * <ul>
 *   <li>Start an Apify lead search and persist a {@link LeadSourceRun}</li>
 *   <li>Sync/fetch Apify results and normalize them into {@link DiscoveredLead}</li>
 *   <li>Prevent duplicate discovered leads (per run) and duplicate CRM leads (on import)</li>
 *   <li>Score discovered leads with a simple heuristic</li>
 *   <li>Import selected discovered leads into the CRM {@link Lead} table</li>
 *   <li>Create {@link AgentLog} audit entries for all key events</li>
 * </ul>
 *
 * All Apify-specific behaviour is delegated to {@link ApifyClient}; this service depends only
 * on the stable normalized {@link ApifyClient.ApifyLeadResult} shape.
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
@Service
public class LeadFinderService {

    private static final Logger logger = LoggerFactory.getLogger(LeadFinderService.class);
    private static final String AGENT_NAME = "ApifyLeadFinder";

    // Cost-control caps for outbound runs.
    private static final int DEFAULT_MAX_RESULTS = 25;
    private static final int HARD_MAX_RESULTS = 200;

    // AgentLog action constants (F-010).
    public static final String ACTION_SEARCH_STARTED = "APIFY_LEAD_SEARCH_STARTED";
    public static final String ACTION_SEARCH_COMPLETED = "APIFY_LEAD_SEARCH_COMPLETED";
    public static final String ACTION_SEARCH_FAILED = "APIFY_LEAD_SEARCH_FAILED";
    public static final String ACTION_LEAD_IMPORTED = "DISCOVERED_LEAD_IMPORTED";
    public static final String ACTION_LEAD_REJECTED = "DISCOVERED_LEAD_REJECTED";
    public static final String ACTION_DUPLICATE_SKIPPED = "DUPLICATE_LEAD_SKIPPED";

    private final LeadSourceRunRepository runRepository;
    private final DiscoveredLeadRepository discoveredLeadRepository;
    private final LeadRepository leadRepository;
    private final BusinessRepository businessRepository;
    private final AgentLogRepository agentLogRepository;
    private final ApifyClient apifyClient;

    public LeadFinderService(
        LeadSourceRunRepository runRepository,
        DiscoveredLeadRepository discoveredLeadRepository,
        LeadRepository leadRepository,
        BusinessRepository businessRepository,
        AgentLogRepository agentLogRepository,
        ApifyClient apifyClient
    ) {
        this.runRepository = runRepository;
        this.discoveredLeadRepository = discoveredLeadRepository;
        this.leadRepository = leadRepository;
        this.businessRepository = businessRepository;
        this.agentLogRepository = agentLogRepository;
        this.apifyClient = apifyClient;
    }

    /**
     * Start a new Apify lead discovery run.
     *
     * @throws ApifyNotConfiguredException if Apify is not enabled or the token is missing
     */
    @Transactional
    public LeadSourceRunResponse startRun(StartLeadFinderRunRequest request) {
        if (!apifyClient.isConfigured()) {
            logger.warn("Attempted to start lead finder run while Apify is not configured");
            throw new ApifyNotConfiguredException(ApifyClient.NOT_CONFIGURED_MESSAGE);
        }

        // Default/clamp maxResults to a safe range to avoid runaway Apify runs (cost control).
        int maxResults = request.getMaxResults() != null ? request.getMaxResults() : DEFAULT_MAX_RESULTS;
        if (maxResults <= 0) {
            maxResults = DEFAULT_MAX_RESULTS;
        }
        maxResults = Math.min(maxResults, HARD_MAX_RESULTS);

        LeadSourceRun run = new LeadSourceRun();
        run.setSearchName(request.getSearchName());
        run.setIndustry(request.getIndustry());
        run.setLocation(request.getLocation());
        run.setKeywords(request.getKeywords());
        run.setMaxResults(maxResults);
        String actorId = (request.getActorId() != null && !request.getActorId().isBlank())
            ? request.getActorId()
            : apifyClient.getDefaultActorId();
        run.setActorId(actorId);
        run.setStatus(LeadSourceRunStatus.PENDING);
        run = runRepository.save(run);

        logAction(ACTION_SEARCH_STARTED, AgentActionStatus.SUCCESS,
            "Started lead search '" + request.getSearchName() + "'", null);

        try {
            ApifyClient.ApifyRunInfo runInfo = apifyClient.startActorRun(
                actorId,
                request.getIndustry(),
                request.getLocation(),
                request.getKeywords(),
                request.getMaxResults()
            );
            run.setApifyRunId(runInfo.runId);
            run.setApifyDatasetId(runInfo.datasetId);
            run.setStatus(LeadSourceRunStatus.RUNNING);
            run = runRepository.save(run);
            logger.info("Apify run started: runId={}, datasetId={}", runInfo.runId, runInfo.datasetId);
        } catch (ApifyClient.ApifyException e) {
            logger.error("Failed to start Apify actor run", e);
            run.setStatus(LeadSourceRunStatus.FAILED);
            run.setFailureReason(e.getMessage());
            run = runRepository.save(run);
            logAction(ACTION_SEARCH_FAILED, AgentActionStatus.ERROR,
                "Failed to start Apify run", e.getMessage());
        }

        return mapRun(run);
    }

    /**
     * Fetch/sync results from the Apify run and persist normalized discovered leads.
     *
     * @throws ApifyNotConfiguredException if Apify is not enabled or the token is missing
     * @throws IllegalArgumentException if the run does not exist
     */
    @Transactional
    public LeadSourceRunResponse syncRun(UUID runId) {
        LeadSourceRun run = runRepository.findById(runId)
            .orElseThrow(() -> new IllegalArgumentException("Lead source run not found: " + runId));

        if (!apifyClient.isConfigured()) {
            throw new ApifyNotConfiguredException(ApifyClient.NOT_CONFIGURED_MESSAGE);
        }

        try {
            String datasetId = run.getApifyDatasetId();

            // Refresh run status/dataset id if we have a run id.
            if (run.getApifyRunId() != null) {
                ApifyClient.ApifyRunInfo info = apifyClient.getRun(run.getApifyRunId());
                if (info.datasetId != null) {
                    datasetId = info.datasetId;
                    run.setApifyDatasetId(datasetId);
                }
                if (info.isFinished() && !info.isSucceeded()) {
                    run.setStatus(LeadSourceRunStatus.FAILED);
                    run.setFailureReason("Apify run status: " + info.status);
                    runRepository.save(run);
                    logAction(ACTION_SEARCH_FAILED, AgentActionStatus.ERROR,
                        "Apify run did not succeed", "status=" + info.status);
                    return mapRun(run);
                }
            }

            List<ApifyClient.ApifyLeadResult> results = apifyClient.fetchDatasetItems(datasetId);

            // Safety cap: never save more than the requested maxResults, even if the actor
            // over-fetches (protects against runaway datasets / cost surprises).
            Integer cap = run.getMaxResults();
            long existing = discoveredLeadRepository.countByLeadSourceRunId(run.getId());

            int newCount = 0;
            for (ApifyClient.ApifyLeadResult item : results) {
                if (cap != null && cap > 0 && (existing + newCount) >= cap) {
                    logger.info("Reached maxResults cap ({}) for run {}, stopping import of further items",
                        cap, run.getId());
                    break;
                }
                if (isDuplicateWithinRun(run.getId(), item)) {
                    logger.debug("Skipping duplicate discovered lead within run {}", run.getId());
                    continue;
                }
                DiscoveredLead dl = new DiscoveredLead();
                dl.setLeadSourceRun(run);
                dl.setBusinessName(truncate(item.businessName, 500));
                dl.setWebsiteUrl(truncate(item.websiteUrl, 1000));
                dl.setContactName(truncate(item.contactName, 255));
                dl.setEmail(truncate(item.email, 255));
                dl.setPhone(truncate(item.phone, 50));
                dl.setLocation(truncate(item.location, 255));
                dl.setIndustry(truncate(item.industry, 255));
                dl.setSourceUrl(truncate(item.sourceUrl, 1000));
                dl.setRawDataJson(item.rawDataJson);
                dl.setScore(scoreDiscoveredLead(item));
                dl.setStatus(DiscoveredLeadStatus.NEW);
                discoveredLeadRepository.save(dl);
                newCount++;
            }

            long total = discoveredLeadRepository.countByLeadSourceRunId(run.getId());
            run.setTotalResults((int) total);
            run.setStatus(LeadSourceRunStatus.COMPLETED);
            run = runRepository.save(run);

            logAction(ACTION_SEARCH_COMPLETED, AgentActionStatus.SUCCESS,
                "Synced Apify results: " + newCount + " new, " + total + " total", null);
            logger.info("Synced run {}: {} new discovered leads ({} total)", run.getId(), newCount, total);

        } catch (ApifyClient.ApifyException e) {
            logger.error("Failed to sync Apify run {}", run.getId(), e);
            run.setStatus(LeadSourceRunStatus.FAILED);
            run.setFailureReason(e.getMessage());
            run = runRepository.save(run);
            logAction(ACTION_SEARCH_FAILED, AgentActionStatus.ERROR,
                "Failed to sync Apify run", e.getMessage());
        }

        return mapRun(run);
    }

    /**
     * List all runs (newest first).
     */
    @Transactional(readOnly = true)
    public List<LeadSourceRunResponse> listRuns() {
        return runRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::mapRun)
            .toList();
    }

    /**
     * Get a single run by id.
     */
    @Transactional(readOnly = true)
    public LeadSourceRunResponse getRun(UUID runId) {
        LeadSourceRun run = runRepository.findById(runId)
            .orElseThrow(() -> new IllegalArgumentException("Lead source run not found: " + runId));
        return mapRun(run);
    }

    /**
     * Get discovered leads for a run (highest score first).
     */
    @Transactional(readOnly = true)
    public List<DiscoveredLeadResponse> getResults(UUID runId) {
        if (!runRepository.existsById(runId)) {
            throw new IllegalArgumentException("Lead source run not found: " + runId);
        }
        return discoveredLeadRepository.findByLeadSourceRunIdOrderByScoreDesc(runId).stream()
            .map(this::mapDiscovered)
            .toList();
    }

    /**
     * Import a single discovered lead into the CRM Lead table.
     *
     * @throws IllegalArgumentException on missing entity / duplicate / missing target business
     */
    @Transactional
    public DiscoveredLeadResponse importDiscoveredLead(UUID discoveredLeadId, UUID targetBusinessId) {
        DiscoveredLead dl = discoveredLeadRepository.findById(discoveredLeadId)
            .orElseThrow(() -> new IllegalArgumentException("Discovered lead not found: " + discoveredLeadId));

        if (dl.getStatus() == DiscoveredLeadStatus.IMPORTED) {
            throw new IllegalArgumentException("Discovered lead has already been imported.");
        }

        Business business = resolveTargetBusiness(targetBusinessId);

        if (isDuplicateCrmLead(dl)) {
            logAction(ACTION_DUPLICATE_SKIPPED, AgentActionStatus.SUCCESS,
                "Skipped duplicate CRM lead for discovered lead " + dl.getId(), null);
            throw new DuplicateLeadException("A matching lead already exists in the CRM.");
        }

        Lead lead = new Lead();
        lead.setBusiness(business);
        lead.setName(resolveLeadName(dl));
        lead.setEmail(dl.getEmail());
        lead.setPhone(dl.getPhone());
        lead.setRequirementText("Outbound prospect discovered via Apify");
        lead.setLeadScore(dl.getScore() != null ? BigDecimal.valueOf(dl.getScore()) : null);
        lead.setStatus(LeadStatus.NEW);
        StringBuilder summary = new StringBuilder("Outbound lead (source: APIFY).");
        if (dl.getBusinessName() != null) summary.append(" Business: ").append(dl.getBusinessName()).append('.');
        if (dl.getWebsiteUrl() != null) summary.append(" Website: ").append(dl.getWebsiteUrl()).append('.');
        if (dl.getLocation() != null) summary.append(" Location: ").append(dl.getLocation()).append('.');
        lead.setSummary(summary.toString());
        lead = leadRepository.save(lead);

        dl.setStatus(DiscoveredLeadStatus.IMPORTED);
        dl.setImportedLeadId(lead.getId());
        discoveredLeadRepository.save(dl);

        // Bump imported count on the run.
        LeadSourceRun run = dl.getLeadSourceRun();
        run.setImportedCount((run.getImportedCount() == null ? 0 : run.getImportedCount()) + 1);
        runRepository.save(run);

        logAction(ACTION_LEAD_IMPORTED, AgentActionStatus.SUCCESS,
            "Imported discovered lead " + dl.getId() + " as CRM lead " + lead.getId(), null);
        logger.info("Imported discovered lead {} as CRM lead {}", dl.getId(), lead.getId());

        return mapDiscovered(dl);
    }

    /**
     * Import multiple discovered leads; duplicates and failures are counted, not fatal.
     */
    @Transactional
    public BulkImportResultResponse importBulk(List<UUID> discoveredLeadIds, UUID targetBusinessId) {
        BulkImportResultResponse result = new BulkImportResultResponse();
        result.setRequested(discoveredLeadIds == null ? 0 : discoveredLeadIds.size());
        if (discoveredLeadIds == null) {
            return result;
        }
        for (UUID id : discoveredLeadIds) {
            try {
                importDiscoveredLead(id, targetBusinessId);
                result.setImported(result.getImported() + 1);
            } catch (DuplicateLeadException e) {
                result.setSkippedDuplicates(result.getSkippedDuplicates() + 1);
                result.getMessages().add(id + ": " + e.getMessage());
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                result.getMessages().add(id + ": " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Reject an irrelevant discovered lead.
     */
    @Transactional
    public DiscoveredLeadResponse rejectDiscoveredLead(UUID discoveredLeadId) {
        DiscoveredLead dl = discoveredLeadRepository.findById(discoveredLeadId)
            .orElseThrow(() -> new IllegalArgumentException("Discovered lead not found: " + discoveredLeadId));
        dl.setStatus(DiscoveredLeadStatus.REJECTED);
        discoveredLeadRepository.save(dl);
        logAction(ACTION_LEAD_REJECTED, AgentActionStatus.SUCCESS,
            "Rejected discovered lead " + dl.getId(), null);
        return mapDiscovered(dl);
    }

    /**
     * @return true when Apify is configured and enabled.
     */
    public boolean isApifyConfigured() {
        return apifyClient.isConfigured();
    }

    // ------------------------------------------------------------------
    // Duplicate detection
    // ------------------------------------------------------------------

    private boolean isDuplicateWithinRun(UUID runId, ApifyClient.ApifyLeadResult item) {
        if (item.email != null && !item.email.isBlank()
            && discoveredLeadRepository.existsByLeadSourceRunIdAndEmailIgnoreCase(runId, item.email)) {
            return true;
        }
        if (item.phone != null && !item.phone.isBlank()
            && discoveredLeadRepository.existsByLeadSourceRunIdAndPhone(runId, item.phone)) {
            return true;
        }
        if (item.websiteUrl != null && !item.websiteUrl.isBlank()
            && discoveredLeadRepository.existsByLeadSourceRunIdAndWebsiteUrlIgnoreCase(runId, item.websiteUrl)) {
            return true;
        }
        if (item.businessName != null && !item.businessName.isBlank()) {
            return discoveredLeadRepository.existsByRunAndBusinessNameAndLocation(
                runId, item.businessName, item.location);
        }
        return false;
    }

    private boolean isDuplicateCrmLead(DiscoveredLead dl) {
        if (dl.getEmail() != null && !dl.getEmail().isBlank()
            && leadRepository.existsByEmailIgnoreCase(dl.getEmail())) {
            return true;
        }
        if (dl.getPhone() != null && !dl.getPhone().isBlank()
            && leadRepository.existsByPhone(dl.getPhone())) {
            return true;
        }
        // Business name + location fallback (scoped to the resolved business happens at name level).
        return false;
    }

    // ------------------------------------------------------------------
    // Scoring (simple heuristic; higher when more contact data is present)
    // ------------------------------------------------------------------

    private double scoreDiscoveredLead(ApifyClient.ApifyLeadResult item) {
        double score = 0.0;
        if (item.email != null && !item.email.isBlank()) score += 35;
        if (item.phone != null && !item.phone.isBlank()) score += 25;
        if (item.websiteUrl != null && !item.websiteUrl.isBlank()) score += 15;
        if (item.contactName != null && !item.contactName.isBlank()) score += 15;
        if (item.businessName != null && !item.businessName.isBlank()) score += 5;
        if (item.location != null && !item.location.isBlank()) score += 5;
        return Math.min(score, 100.0);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Business resolveTargetBusiness(UUID targetBusinessId) {
        if (targetBusinessId != null) {
            return businessRepository.findById(targetBusinessId)
                .orElseThrow(() -> new IllegalArgumentException("Target business not found: " + targetBusinessId));
        }
        // Lead requires a business. Fall back to the only business if exactly one exists,
        // otherwise require the caller to pick one.
        List<Business> all = businessRepository.findAll();
        if (all.size() == 1) {
            return all.get(0);
        }
        throw new IllegalArgumentException(
            "A target business is required to import this lead. Please select a business.");
    }

    private String resolveLeadName(DiscoveredLead dl) {
        if (dl.getContactName() != null && !dl.getContactName().isBlank()) {
            return dl.getContactName();
        }
        if (dl.getBusinessName() != null && !dl.getBusinessName().isBlank()) {
            return dl.getBusinessName();
        }
        return "Outbound prospect";
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private void logAction(String action, AgentActionStatus status, String output, String error) {
        try {
            AgentLog log = new AgentLog();
            log.setAgentName(AGENT_NAME);
            log.setAction(action);
            log.setStatus(status);
            log.setOutputJson(output);
            log.setErrorMessage(error);
            agentLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to write AgentLog for action {}: {}", action, e.getMessage());
        }
    }

    private LeadSourceRunResponse mapRun(LeadSourceRun run) {
        LeadSourceRunResponse r = new LeadSourceRunResponse();
        r.setId(run.getId());
        r.setSearchName(run.getSearchName());
        r.setIndustry(run.getIndustry());
        r.setLocation(run.getLocation());
        r.setKeywords(run.getKeywords());
        r.setActorId(run.getActorId());
        r.setApifyRunId(run.getApifyRunId());
        r.setMaxResults(run.getMaxResults());
        r.setStatus(run.getStatus());
        r.setTotalResults(run.getTotalResults());
        r.setImportedCount(run.getImportedCount());
        r.setFailureReason(run.getFailureReason());
        r.setCreatedAt(run.getCreatedAt());
        r.setUpdatedAt(run.getUpdatedAt());
        return r;
    }

    private DiscoveredLeadResponse mapDiscovered(DiscoveredLead dl) {
        DiscoveredLeadResponse r = new DiscoveredLeadResponse();
        r.setId(dl.getId());
        r.setLeadSourceRunId(dl.getLeadSourceRun() != null ? dl.getLeadSourceRun().getId() : null);
        r.setBusinessName(dl.getBusinessName());
        r.setWebsiteUrl(dl.getWebsiteUrl());
        r.setContactName(dl.getContactName());
        r.setEmail(dl.getEmail());
        r.setPhone(dl.getPhone());
        r.setLocation(dl.getLocation());
        r.setIndustry(dl.getIndustry());
        r.setSourceUrl(dl.getSourceUrl());
        r.setRawDataJson(dl.getRawDataJson());
        r.setScore(dl.getScore());
        r.setStatus(dl.getStatus());
        r.setImportedLeadId(dl.getImportedLeadId());
        r.setCreatedAt(dl.getCreatedAt());
        r.setUpdatedAt(dl.getUpdatedAt());
        return r;
    }

    /**
     * Thrown when Apify is not configured; mapped to a clean 200/400 message by the controller.
     */
    public static class ApifyNotConfiguredException extends RuntimeException {
        public ApifyNotConfiguredException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when an import would create a duplicate CRM lead.
     */
    public static class DuplicateLeadException extends RuntimeException {
        public DuplicateLeadException(String message) {
            super(message);
        }
    }
}
