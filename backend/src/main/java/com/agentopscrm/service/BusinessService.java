package com.agentopscrm.service;

import com.agentopscrm.dto.BusinessDependenciesResponse;
import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.CrawlStatus;
import com.agentopscrm.exception.BusinessAlreadyExistsException;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.util.SafeErrorMessages;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for business management operations.
 */
@Service
@Transactional
public class BusinessService {

    private static final Logger log = LoggerFactory.getLogger(BusinessService.class);
    public static final int MAX_SEARCH_LENGTH = 200;

    private final BusinessRepository businessRepository;
    private final AgentLogRepository agentLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public BusinessService(BusinessRepository businessRepository, AgentLogRepository agentLogRepository) {
        this.businessRepository = businessRepository;
        this.agentLogRepository = agentLogRepository;
    }

    public Business createBusiness(String name, String websiteUrl, String industry,
                                   String description, String contactEmail, String contactPhone) {
        if (businessRepository.existsByWebsiteUrl(websiteUrl)) {
            throw new BusinessAlreadyExistsException(
                "Business with website URL '" + websiteUrl + "' already exists"
            );
        }

        String normalizedPhone = normalizeBlankToNull(contactPhone);

        Business business = new Business();
        business.setName(name);
        business.setWebsiteUrl(websiteUrl);
        business.setIndustry(industry);
        business.setDescription(description);
        business.setContactEmail(contactEmail);
        business.setContactPhone(normalizedPhone);
        business.setCrawlStatus(CrawlStatus.NOT_STARTED);

        business = businessRepository.save(business);

        logAgentAction("BusinessManager", "CREATE_BUSINESS", null, business.getId().toString(),
            "{\"businessName\":\"" + name + "\"}", AgentActionStatus.SUCCESS);

        return business;
    }

    public Business getBusinessById(UUID id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new BusinessNotFoundException(
                    "Business not found with id: " + id
                ));
    }

    public Page<Business> getAllBusinesses(Pageable pageable) {
        return businessRepository.findAll(pageable);
    }

    public Business updateBusiness(UUID id, String name, String websiteUrl, String industry,
                                   String description, String contactEmail, String contactPhone) {
        Business business = getBusinessById(id);

        if (websiteUrl != null && !websiteUrl.equals(business.getWebsiteUrl())) {
            if (businessRepository.existsByWebsiteUrl(websiteUrl)) {
                throw new BusinessAlreadyExistsException(
                    "Business with website URL '" + websiteUrl + "' already exists"
                );
            }
        }

        if (name != null) {
            business.setName(name);
        }
        if (websiteUrl != null) {
            business.setWebsiteUrl(websiteUrl);
        }
        if (industry != null) {
            business.setIndustry(industry);
        }
        if (description != null) {
            business.setDescription(description);
        }
        if (contactEmail != null) {
            business.setContactEmail(contactEmail);
        }
        if (contactPhone != null) {
            business.setContactPhone(normalizeBlankToNull(contactPhone));
        }

        business.setUpdatedAt(LocalDateTime.now());
        business = businessRepository.save(business);

        logAgentAction("BusinessManager", "UPDATE_BUSINESS", null, business.getId().toString(),
            "{\"businessId\":\"" + id + "\"}", AgentActionStatus.SUCCESS);

        return business;
    }

    @Transactional(readOnly = true)
    public BusinessDependenciesResponse getDependencies(UUID id) {
        Business business = getBusinessById(id);
        BusinessDependenciesResponse deps = new BusinessDependenciesResponse();
        deps.setBusinessId(id.toString());
        deps.setBusinessName(business.getName());
        deps.setLeads(count("SELECT COUNT(*) FROM leads WHERE business_id = :id", id));
        deps.setConversations(count("SELECT COUNT(*) FROM conversations WHERE business_id = :id", id));
        deps.setMessages(count(
                "SELECT COUNT(*) FROM messages m WHERE m.conversation_id IN "
                        + "(SELECT c.id FROM conversations c WHERE c.business_id = :id)", id));
        deps.setDocuments(count("SELECT COUNT(*) FROM documents WHERE business_id = :id", id));
        deps.setKnowledgeChunks(count("SELECT COUNT(*) FROM knowledge_chunks WHERE business_id = :id", id));
        deps.setKnowledgeBaseJobs(count("SELECT COUNT(*) FROM knowledge_base_jobs WHERE business_id = :id", id));
        deps.setApprovals(count("SELECT COUNT(*) FROM approvals WHERE business_id = :id", id));
        deps.setAgentLogs(count("SELECT COUNT(*) FROM agent_logs WHERE business_id = :id", id));
        deps.setVoiceCalls(count("SELECT COUNT(*) FROM voice_calls WHERE business_id = :id", id));
        return deps;
    }

    /**
     * Transactionally cascade-deletes safe dependents in FK-safe order, then the business.
     * A failure rolls back the entire unit of work so the record stays intact.
     */
    public void deleteBusiness(UUID id) {
        Business business = getBusinessById(id);
        String name = business.getName();
        try {
            executeDelete("DELETE FROM messages WHERE conversation_id IN "
                    + "(SELECT id FROM conversations WHERE business_id = :id)", id);
            executeDelete("DELETE FROM voice_calls WHERE business_id = :id", id);
            executeDelete("DELETE FROM approvals WHERE business_id = :id", id);
            executeDelete("DELETE FROM agent_logs WHERE business_id = :id", id);
            executeDelete("DELETE FROM knowledge_chunks WHERE business_id = :id", id);
            executeDelete("DELETE FROM knowledge_base_jobs WHERE business_id = :id", id);
            executeDelete("DELETE FROM documents WHERE business_id = :id", id);
            executeDelete("UPDATE leads SET conversation_id = NULL WHERE business_id = :id", id);
            executeDelete("DELETE FROM leads WHERE business_id = :id", id);
            executeDelete("DELETE FROM conversations WHERE business_id = :id", id);
            executeDelete("DELETE FROM businesses WHERE id = :id", id);
            entityManager.flush();
            entityManager.clear();
        } catch (RuntimeException e) {
            log.error("Business delete failed for {} [{}]", id, SafeErrorMessages.newId(), e);
            throw e;
        }

        logAgentAction("BusinessManager", "DELETE_BUSINESS", null, id.toString(),
            "{\"businessId\":\"" + id + "\",\"businessName\":\"" + name + "\"}",
            AgentActionStatus.SUCCESS);
    }

    public Page<Business> searchBusinesses(String searchTerm, Pageable pageable) {
        String term = searchTerm == null ? "" : searchTerm.trim();
        if (term.length() > MAX_SEARCH_LENGTH) {
            throw new IllegalArgumentException("Search must be " + MAX_SEARCH_LENGTH + " characters or fewer");
        }
        if (term.isEmpty()) {
            return businessRepository.findAll(pageable);
        }
        return businessRepository.search(term, pageable);
    }

    public Page<Business> getBusinessesByCrawlStatus(CrawlStatus status, Pageable pageable) {
        return businessRepository.findByCrawlStatus(status, pageable);
    }

    public Business updateCrawlStatus(UUID id, CrawlStatus status) {
        Business business = getBusinessById(id);
        business.setCrawlStatus(status);
        business.setUpdatedAt(LocalDateTime.now());
        return businessRepository.save(business);
    }

    private long count(String sql, UUID id) {
        Number n = (Number) entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .getSingleResult();
        return n == null ? 0L : n.longValue();
    }

    private void executeDelete(String sql, UUID id) {
        entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .executeUpdate();
    }

    private void logAgentAction(String agentName, String action, String input, String output,
                                 AgentActionStatus status) {
        AgentLog logEntry = new AgentLog();
        logEntry.setAgentName(agentName);
        logEntry.setAction(action);
        logEntry.setInputJson(input);
        logEntry.setOutputJson(output);
        logEntry.setStatus(status);
        agentLogRepository.save(logEntry);
    }

    private void logAgentAction(String agentName, String action, String businessId, String inputJson, String outputJson,
                                 AgentActionStatus status) {
        AgentLog logEntry = new AgentLog();
        logEntry.setAgentName(agentName);
        logEntry.setAction(action);
        logEntry.setInputJson(inputJson);
        logEntry.setOutputJson(outputJson);
        logEntry.setStatus(status);
        if (businessId != null) {
            businessRepository.findById(UUID.fromString(businessId)).ifPresent(logEntry::setBusiness);
        }
        agentLogRepository.save(logEntry);
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}
