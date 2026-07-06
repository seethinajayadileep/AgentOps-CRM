package com.agentopscrm.service;

import com.agentopscrm.entity.AgentLog;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.enums.AgentActionStatus;
import com.agentopscrm.entity.enums.CrawlStatus;
import com.agentopscrm.exception.BusinessAlreadyExistsException;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for business management operations.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@Service
@Transactional
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final AgentLogRepository agentLogRepository;

    public BusinessService(BusinessRepository businessRepository, AgentLogRepository agentLogRepository) {
        this.businessRepository = businessRepository;
        this.agentLogRepository = agentLogRepository;
    }

    /**
     * Create a new business.
     */
    public Business createBusiness(String name, String websiteUrl, String industry,
                                   String description, String contactEmail, String contactPhone) {
        // Check if business with same website URL exists
        if (businessRepository.existsByWebsiteUrl(websiteUrl)) {
            throw new BusinessAlreadyExistsException(
                "Business with website URL '" + websiteUrl + "' already exists"
            );
        }

        // Normalize blank phone to null
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

        // Log agent action
        logAgentAction("BusinessManager", "CREATE_BUSINESS", null, business.getId().toString(),
            "{\"businessName\":\"" + name + "\"}", AgentActionStatus.SUCCESS);

        return business;
    }

    /**
     * Get business by ID.
     */
    public Business getBusinessById(UUID id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new BusinessNotFoundException(
                    "Business not found with id: " + id
                ));
    }

    /**
     * Get all businesses with pagination.
     */
    public Page<Business> getAllBusinesses(Pageable pageable) {
        return businessRepository.findAll(pageable);
    }

    /**
     * Update an existing business.
     */
    public Business updateBusiness(UUID id, String name, String websiteUrl, String industry,
                                   String description, String contactEmail, String contactPhone) {
        Business business = getBusinessById(id);

        // Check if another business has this website URL
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

        // Log agent action
        logAgentAction("BusinessManager", "UPDATE_BUSINESS", null, business.getId().toString(),
            "{\"businessId\":\"" + id + "\"}", AgentActionStatus.SUCCESS);

        return business;
    }

    /**
     * Delete a business.
     */
    public void deleteBusiness(UUID id) {
        Business business = getBusinessById(id);
        businessRepository.delete(business);

        // Log agent action
        logAgentAction("BusinessManager", "DELETE_BUSINESS", null, id.toString(),
            "{\"businessId\":\"" + id + "\"}", AgentActionStatus.SUCCESS);
    }

    /**
     * Search businesses by name or industry.
     */
    public Page<Business> searchBusinesses(String searchTerm, Pageable pageable) {
        return businessRepository.search(searchTerm, pageable);
    }

    /**
     * Get businesses by crawl status.
     */
    public Page<Business> getBusinessesByCrawlStatus(CrawlStatus status, Pageable pageable) {
        return businessRepository.findByCrawlStatus(status, pageable);
    }

    /**
     * Update crawl status.
     */
    public Business updateCrawlStatus(UUID id, CrawlStatus status) {
        Business business = getBusinessById(id);
        business.setCrawlStatus(status);
        business.setUpdatedAt(LocalDateTime.now());
        return businessRepository.save(business);
    }

    /**
     * Log an agent action.
     */
    private void logAgentAction(String agentName, String action, String input, String output,
                                 AgentActionStatus status) {
        AgentLog log = new AgentLog();
        log.setAgentName(agentName);
        log.setAction(action);
        log.setInputJson(input);
        log.setOutputJson(output);
        log.setStatus(status);
        agentLogRepository.save(log);
    }

    /**
     * Log an agent action with business reference.
     */
    private void logAgentAction(String agentName, String action, String businessId, String inputJson, String outputJson,
                                 AgentActionStatus status) {
        AgentLog log = new AgentLog();
        log.setAgentName(agentName);
        log.setAction(action);
        log.setInputJson(inputJson);
        log.setOutputJson(outputJson);
        log.setStatus(status);
        if (businessId != null) {
            Business business = businessRepository.findById(UUID.fromString(businessId)).orElse(null);
            log.setBusiness(business);
        }
        agentLogRepository.save(log);
    }

    /**
     * Normalize blank strings (null, empty, or whitespace-only) to null.
     */
    private String normalizeBlankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}