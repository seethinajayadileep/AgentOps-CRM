package com.agentopscrm.dto.search;

import java.util.ArrayList;
import java.util.List;

public class GlobalSearchResponse {

    private List<SearchHit> businesses = new ArrayList<>();
    private List<SearchHit> leads = new ArrayList<>();
    private List<SearchHit> conversations = new ArrayList<>();

    public List<SearchHit> getBusinesses() {
        return businesses;
    }

    public void setBusinesses(List<SearchHit> businesses) {
        this.businesses = businesses;
    }

    public List<SearchHit> getLeads() {
        return leads;
    }

    public void setLeads(List<SearchHit> leads) {
        this.leads = leads;
    }

    public List<SearchHit> getConversations() {
        return conversations;
    }

    public void setConversations(List<SearchHit> conversations) {
        this.conversations = conversations;
    }

    public static class SearchHit {
        private String id;
        private String title;
        private String subtitle;
        private String href;

        public SearchHit() {
        }

        public SearchHit(String id, String title, String subtitle, String href) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.href = href;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }
    }
}
