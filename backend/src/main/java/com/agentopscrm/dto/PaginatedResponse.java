package com.agentopscrm.dto;

import java.util.List;

/**
 * Response DTO for paginated lists.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
public class PaginatedResponse<T> {
    private List<T> items;
    private PaginationMeta pagination;

    public PaginatedResponse() {
    }

    public PaginatedResponse(List<T> items, PaginationMeta pagination) {
        this.items = items;
        this.pagination = pagination;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public PaginationMeta getPagination() {
        return pagination;
    }

    public void setPagination(PaginationMeta pagination) {
        this.pagination = pagination;
    }
}