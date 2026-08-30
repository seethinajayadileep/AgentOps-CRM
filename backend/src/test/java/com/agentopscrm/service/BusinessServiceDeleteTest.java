package com.agentopscrm.service;

import com.agentopscrm.dto.BusinessDependenciesResponse;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.enums.CrawlStatus;
import com.agentopscrm.exception.BusinessNotFoundException;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessServiceDeleteTest {

    @Mock private BusinessRepository businessRepository;
    @Mock private AgentLogRepository agentLogRepository;
    @Mock private EntityManager entityManager;
    @Mock private Query query;

    private BusinessService service;

    @BeforeEach
    void setUp() {
        service = new BusinessService(businessRepository, agentLogRepository);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), any())).thenReturn(query);
        lenient().when(query.getSingleResult()).thenReturn(2L);
        lenient().when(query.executeUpdate()).thenReturn(1);
    }

    @Test
    void getDependencies_returnsCountsForConfirmation() {
        UUID id = UUID.randomUUID();
        Business business = new Business(id);
        business.setName("Linear E2E Regression");
        when(businessRepository.findById(id)).thenReturn(Optional.of(business));

        BusinessDependenciesResponse deps = service.getDependencies(id);

        assertEquals("Linear E2E Regression", deps.getBusinessName());
        assertEquals(2L, deps.getLeads());
        assertTrue(deps.getTotal() >= 2);
    }

    @Test
    void deleteBusiness_executesNativeDeletesInFkSafeOrder_thenDeletesBusiness() {
        UUID id = UUID.randomUUID();
        Business business = new Business(id);
        business.setName("Temp");
        business.setCrawlStatus(CrawlStatus.COMPLETED);
        when(businessRepository.findById(id)).thenReturn(Optional.of(business));

        service.deleteBusiness(id);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager, atLeast(8)).createNativeQuery(sql.capture());
        String joined = String.join(" | ", sql.getAllValues());
        assertTrue(joined.contains("DELETE FROM messages"));
        assertTrue(joined.contains("DELETE FROM voice_calls"));
        assertTrue(joined.contains("DELETE FROM approvals"));
        assertTrue(joined.contains("DELETE FROM agent_logs"));
        assertTrue(joined.contains("DELETE FROM knowledge_chunks"));
        assertTrue(joined.contains("DELETE FROM knowledge_base_jobs"));
        assertTrue(joined.contains("DELETE FROM documents"));
        assertTrue(joined.contains("DELETE FROM leads"));
        assertTrue(joined.contains("DELETE FROM conversations"));
        assertTrue(joined.contains("DELETE FROM businesses"));
        verify(entityManager).flush();
        verify(agentLogRepository).save(any());
    }

    @Test
    void deleteBusiness_whenSqlFails_doesNotLogSuccess() {
        UUID id = UUID.randomUUID();
        Business business = new Business(id);
        business.setName("Temp");
        when(businessRepository.findById(id)).thenReturn(Optional.of(business));
        when(query.executeUpdate()).thenThrow(new RuntimeException("fk violation"));

        assertThrows(RuntimeException.class, () -> service.deleteBusiness(id));
        verify(agentLogRepository, never()).save(any());
    }

    @Test
    void deleteBusiness_whenMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(businessRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(BusinessNotFoundException.class, () -> service.deleteBusiness(id));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void searchBusinesses_rejectsMoreThan200Characters() {
        String term = "a".repeat(201);
        assertThrows(IllegalArgumentException.class, () -> service.searchBusinesses(term, null));
    }
}
