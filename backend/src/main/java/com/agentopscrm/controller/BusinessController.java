package com.agentopscrm.controller;

import com.agentopscrm.dto.*;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.enums.CrawlStatus;
import com.agentopscrm.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for business management operations.
 *
 * API IDs: API-003 to API-007
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    /**
     * Create a new business.
     * Endpoint: POST /api/businesses (API-003)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BusinessResponse>> createBusiness(
            @Valid @RequestBody CreateBusinessRequest request) {
        Business business = businessService.createBusiness(
                request.getName(),
                request.getWebsiteUrl(),
                request.getIndustry(),
                request.getDescription(),
                request.getContactEmail(),
                request.getContactPhone()
        );

        BusinessResponse response = toResponse(business);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Business created successfully"));
    }

    /**
     * Get all businesses with pagination.
     * Endpoint: GET /api/businesses (API-004)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<BusinessResponse>>> getAllBusinesses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        Sort sort = Sort.by(sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Business> businesses = businessService.getAllBusinesses(pageable);

        List<BusinessResponse> items = businesses.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        PaginatedResponse<BusinessResponse> response = new PaginatedResponse<>(
                items,
                new PaginationMeta(
                        businesses.getNumber(),
                        businesses.getSize(),
                        businesses.getTotalElements(),
                        businesses.getTotalPages()
                )
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get business by ID.
     * Endpoint: GET /api/businesses/{id} (API-005)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessResponse>> getBusinessById(@PathVariable UUID id) {
        Business business = businessService.getBusinessById(id);
        BusinessResponse response = toResponse(business);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update an existing business.
     * Endpoint: PUT /api/businesses/{id} (API-006)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessResponse>> updateBusiness(
            @PathVariable UUID id,
            @RequestBody UpdateBusinessRequest request) {

        Business business = businessService.updateBusiness(
                id,
                request.getName(),
                request.getWebsiteUrl(),
                request.getIndustry(),
                request.getDescription(),
                request.getContactEmail(),
                request.getContactPhone()
        );

        BusinessResponse response = toResponse(business);
        return ResponseEntity.ok(ApiResponse.success(response, "Business updated successfully"));
    }

    /**
     * Delete a business.
     * Endpoint: DELETE /api/businesses/{id} (API-007)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBusiness(@PathVariable UUID id) {
        businessService.deleteBusiness(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Business deleted successfully"));
    }

    /**
     * Search businesses.
     * Endpoint: GET /api/businesses/search?term=xxx
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PaginatedResponse<BusinessResponse>>> searchBusinesses(
            @RequestParam String term,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Business> businesses = businessService.searchBusinesses(term, pageable);

        List<BusinessResponse> items = businesses.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        PaginatedResponse<BusinessResponse> response = new PaginatedResponse<>(
                items,
                new PaginationMeta(
                        businesses.getNumber(),
                        businesses.getSize(),
                        businesses.getTotalElements(),
                        businesses.getTotalPages()
                )
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get businesses by crawl status.
     */
    @GetMapping("/crawl-status/{status}")
    public ResponseEntity<ApiResponse<PaginatedResponse<BusinessResponse>>> getByCrawlStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Business> businesses = businessService.getBusinessesByCrawlStatus(
                CrawlStatus.valueOf(status.toUpperCase()),
                pageable
        );

        List<BusinessResponse> items = businesses.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        PaginatedResponse<BusinessResponse> response = new PaginatedResponse<>(
                items,
                new PaginationMeta(
                        businesses.getNumber(),
                        businesses.getSize(),
                        businesses.getTotalElements(),
                        businesses.getTotalPages()
                )
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Convert Business entity to DTO.
     */
    private BusinessResponse toResponse(Business business) {
        return new BusinessResponse(
                business.getId().toString(),
                business.getName(),
                business.getWebsiteUrl(),
                business.getIndustry(),
                business.getDescription(),
                business.getContactEmail(),
                business.getContactPhone(),
                business.getCrawlStatus().name(),
                business.getCreatedAt().toString(),
                business.getUpdatedAt() != null ? business.getUpdatedAt().toString() : null
        );
    }
}