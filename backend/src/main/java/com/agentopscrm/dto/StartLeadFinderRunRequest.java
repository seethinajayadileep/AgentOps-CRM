package com.agentopscrm.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO to start a new Apify lead discovery run (F-010).
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
public class StartLeadFinderRunRequest {

    @NotBlank(message = "searchName is required")
    private String searchName;

    private String industry;
    private String location;
    private String keywords;
    private String actorId;
    private Integer maxResults;

    public StartLeadFinderRunRequest() {
    }

    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
}
