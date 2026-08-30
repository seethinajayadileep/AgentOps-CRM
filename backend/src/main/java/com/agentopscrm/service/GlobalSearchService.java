package com.agentopscrm.service;

import com.agentopscrm.dto.search.GlobalSearchResponse;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.Conversation;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.ConversationRepository;
import com.agentopscrm.repository.LeadRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GlobalSearchService {

    private static final int LIMIT = 5;
    private static final int MAX_QUERY_LENGTH = 200;

    private final BusinessRepository businessRepository;
    private final LeadRepository leadRepository;
    private final ConversationRepository conversationRepository;

    public GlobalSearchService(
            BusinessRepository businessRepository,
            LeadRepository leadRepository,
            ConversationRepository conversationRepository) {
        this.businessRepository = businessRepository;
        this.leadRepository = leadRepository;
        this.conversationRepository = conversationRepository;
    }

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query is required");
        }
        String query = rawQuery.trim();
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Search query must be " + MAX_QUERY_LENGTH + " characters or fewer");
        }

        GlobalSearchResponse response = new GlobalSearchResponse();
        PageRequest page = PageRequest.of(0, LIMIT);

        List<Business> businesses = businessRepository.search(query, page).getContent();
        response.setBusinesses(businesses.stream()
                .map(b -> new GlobalSearchResponse.SearchHit(
                        b.getId().toString(),
                        b.getName(),
                        b.getIndustry() != null ? b.getIndustry() : "Business",
                        "/businesses/" + b.getId()))
                .toList());

        response.setLeads(leadRepository.searchByTerm(query, page).stream()
                .map(this::toLeadHit)
                .toList());

        response.setConversations(conversationRepository.searchByTerm(query, page).stream()
                .map(this::toConversationHit)
                .toList());

        return response;
    }

    private GlobalSearchResponse.SearchHit toLeadHit(Lead lead) {
        String subtitle = lead.getEmail() != null ? lead.getEmail() : "Lead";
        if (lead.getBusiness() != null && lead.getBusiness().getName() != null) {
            subtitle = subtitle + " · " + lead.getBusiness().getName();
        }
        return new GlobalSearchResponse.SearchHit(
                lead.getId().toString(),
                lead.getName(),
                subtitle,
                "/leads/" + lead.getId());
    }

    private GlobalSearchResponse.SearchHit toConversationHit(Conversation conversation) {
        String title = conversation.getCustomerName() != null && !conversation.getCustomerName().isBlank()
                ? conversation.getCustomerName()
                : "Conversation";
        String subtitle = conversation.getStatus() != null ? conversation.getStatus().name() : "Conversation";
        return new GlobalSearchResponse.SearchHit(
                conversation.getId().toString(),
                title,
                subtitle,
                "/conversations");
    }
}
