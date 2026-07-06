package com.agentopscrm.dto;

/**
 * Pagination metadata DTO.
 *
 * @author AgentOps Team
 * @version 0.2.0
 */
public class PaginationMeta {
    private int page;
    private int size;
    private long total;
    private int totalPages;

    public PaginationMeta() {
    }

    public PaginationMeta(int page, int size, long total, int totalPages) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    // Alias for getTotal() to support getTotalElements() naming
    public long getTotalElements() {
        return total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}