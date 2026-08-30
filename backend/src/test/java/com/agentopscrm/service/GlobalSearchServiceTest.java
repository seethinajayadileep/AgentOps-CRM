package com.agentopscrm.service;

import com.agentopscrm.dto.search.GlobalSearchResponse;
import com.agentopscrm.entity.Business;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.ConversationRepository;
import com.agentopscrm.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceTest {

    @Mock private BusinessRepository businessRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private ConversationRepository conversationRepository;

    private GlobalSearchService service;

    @BeforeEach
    void setUp() {
        service = new GlobalSearchService(businessRepository, leadRepository, conversationRepository);
    }

    @Test
    void emptyQueryIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> service.search("  "));
    }

    @Test
    void returnsMappedHits() {
        Business business = new Business();
        business.setId(UUID.randomUUID());
        business.setName("Acme Ads");
        business.setIndustry("Advertising");
        when(businessRepository.search(eq("Acme"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(business)));
        when(leadRepository.searchByTerm(eq("Acme"), any(Pageable.class))).thenReturn(List.of());
        when(conversationRepository.searchByTerm(eq("Acme"), any(Pageable.class))).thenReturn(List.of());

        GlobalSearchResponse response = service.search("Acme");

        assertEquals(1, response.getBusinesses().size());
        assertEquals("Acme Ads", response.getBusinesses().get(0).getTitle());
        assertTrue(response.getBusinesses().get(0).getHref().startsWith("/businesses/"));
        assertTrue(response.getLeads().isEmpty());
        assertTrue(response.getConversations().isEmpty());
    }
}
