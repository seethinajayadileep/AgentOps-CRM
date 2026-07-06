package com.agentopscrm.service;

import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.enums.LeadStatus;
import com.agentopscrm.exception.LeadNotFoundException;
import com.agentopscrm.repository.LeadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for Lead management operations.
 *
 * Why exists: Centralizes lead persistence logic in a transactional
 * boundary so that lazy associations (business, conversation) remain
 * initialized when mapped to response DTOs, avoiding
 * LazyInitializationException without resorting to open-in-view or
 * global EAGER fetching.
 *
 * @author AgentOps Team
 * @version 0.3.0
 */
@Service
@Transactional
public class LeadService {

    private static final Logger log = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;

    public LeadService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    /**
     * Get a lead by id with business and conversation initialized.
     */
    @Transactional(readOnly = true)
    public Lead getLeadById(UUID id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));
    }

    /**
     * Get all leads (newest first) with associations initialized.
     */
    @Transactional(readOnly = true)
    public List<Lead> getAllLeads() {
        return leadRepository.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Get leads for a business with associations initialized.
     */
    @Transactional(readOnly = true)
    public List<Lead> getLeadsByBusiness(UUID businessId) {
        return leadRepository.findByBusinessId(businessId);
    }

    /**
     * Update a lead's status.
     *
     * The entity is loaded, mutated and saved within a single transaction.
     * After save(), the persisted entity is explicitly reloaded via a
     * targeted fetch query (JOIN FETCH on business/conversation) to
     * guarantee those associations are fully initialized before the
     * caller maps the result to a response DTO - this protects against
     * merge()-created lazy proxies that could otherwise surface a
     * LazyInitializationException once the transaction/session ends.
     */
    public Lead updateStatus(UUID id, LeadStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));

        lead.setStatus(status);
        Lead saved = leadRepository.save(lead);

        // Reload with associations initialized before returning for DTO mapping.
        Lead reloaded = leadRepository.findWithAssociationsById(saved.getId())
                .orElseThrow(() -> new LeadNotFoundException("Lead not found with id: " + id));

        log.info("Lead status updated - leadId: {}, status: {}", reloaded.getId(), reloaded.getStatus());
        return reloaded;
    }
}
