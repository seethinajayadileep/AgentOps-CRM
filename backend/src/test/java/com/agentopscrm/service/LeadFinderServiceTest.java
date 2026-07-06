package com.agentopscrm.service;

import com.agentopscrm.client.ApifyClient;
import com.agentopscrm.dto.BulkImportResultResponse;
import com.agentopscrm.dto.DiscoveredLeadResponse;
import com.agentopscrm.dto.LeadSourceRunResponse;
import com.agentopscrm.dto.StartLeadFinderRunRequest;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.DiscoveredLead;
import com.agentopscrm.entity.Lead;
import com.agentopscrm.entity.LeadSourceRun;
import com.agentopscrm.entity.enums.DiscoveredLeadStatus;
import com.agentopscrm.entity.enums.LeadSourceRunStatus;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.DiscoveredLeadRepository;
import com.agentopscrm.repository.LeadRepository;
import com.agentopscrm.repository.LeadSourceRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LeadFinderService} (F-010 Apify Lead Finder).
 * All collaborators are mocked; no database or network access is performed.
 */
@ExtendWith(MockitoExtension.class)
class LeadFinderServiceTest {

    @Mock private LeadSourceRunRepository runRepository;
    @Mock private DiscoveredLeadRepository discoveredLeadRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private AgentLogRepository agentLogRepository;
    @Mock private ApifyClient apifyClient;

    private LeadFinderService service;

    @BeforeEach
    void setUp() {
        service = new LeadFinderService(
            runRepository, discoveredLeadRepository, leadRepository,
            businessRepository, agentLogRepository, apifyClient, 30L);
    }

    private StartLeadFinderRunRequest request() {
        StartLeadFinderRunRequest r = new StartLeadFinderRunRequest();
        r.setSearchName("Hyderabad ad agencies");
        r.setIndustry("Advertising agencies");
        r.setLocation("Hyderabad");
        r.setKeywords("media buying");
        r.setMaxResults(25);
        return r;
    }

    // T-LF-01
    @Test
    void startRun_whenApifyNotConfigured_throwsCleanException() {
        when(apifyClient.isConfigured()).thenReturn(false);

        LeadFinderService.ApifyNotConfiguredException ex = assertThrows(
            LeadFinderService.ApifyNotConfiguredException.class,
            () -> service.startRun(request()));

        assertEquals(ApifyClient.NOT_CONFIGURED_MESSAGE, ex.getMessage());
        verify(runRepository, never()).save(any());
    }

    // T-LF-02
    @Test
    void startRun_whenConfigured_createsRunningRun() throws Exception {
        when(apifyClient.isConfigured()).thenReturn(true);
        when(apifyClient.getDefaultActorId()).thenReturn("compass~crawler-google-places");
        when(runRepository.save(any(LeadSourceRun.class))).thenAnswer(inv -> {
            LeadSourceRun r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        ApifyClient.ApifyRunInfo info = new ApifyClient.ApifyRunInfo();
        info.runId = "run-123";
        info.datasetId = "ds-123";
        info.status = "RUNNING";
        when(apifyClient.startActorRun(anyString(), any(), any(), any(), any())).thenReturn(info);

        LeadSourceRunResponse resp = service.startRun(request());

        assertEquals(LeadSourceRunStatus.RUNNING, resp.getStatus());
        assertEquals("run-123", resp.getApifyRunId());
        verify(agentLogRepository, atLeastOnce()).save(any());
    }

    // T-LF-03 + T-LF-04
    @Test
    void syncRun_savesNewLeads_andSkipsDuplicates() throws Exception {
        UUID runId = UUID.randomUUID();
        LeadSourceRun run = new LeadSourceRun(runId);
        run.setApifyRunId("run-123");
        run.setApifyDatasetId("ds-123");

        when(runRepository.findById(runId)).thenReturn(Optional.of(run));
        when(apifyClient.isConfigured()).thenReturn(true);

        ApifyClient.ApifyRunInfo info = new ApifyClient.ApifyRunInfo();
        info.datasetId = "ds-123";
        info.status = "SUCCEEDED";
        when(apifyClient.getRun("run-123")).thenReturn(info);

        ApifyClient.ApifyLeadResult a = new ApifyClient.ApifyLeadResult();
        a.businessName = "Acme Media";
        a.email = "a@acme.test";
        ApifyClient.ApifyLeadResult b = new ApifyClient.ApifyLeadResult();
        b.businessName = "Dup Co";
        b.email = "dup@dup.test";
        when(apifyClient.fetchDatasetItems("ds-123")).thenReturn(List.of(a, b));

        // First lead is new, second is a duplicate by email.
        when(discoveredLeadRepository.existsByLeadSourceRunIdAndEmailIgnoreCase(runId, "a@acme.test")).thenReturn(false);
        when(discoveredLeadRepository.existsByLeadSourceRunIdAndEmailIgnoreCase(runId, "dup@dup.test")).thenReturn(true);
        when(discoveredLeadRepository.countByLeadSourceRunId(runId)).thenReturn(1L);
        when(runRepository.save(any(LeadSourceRun.class))).thenAnswer(inv -> inv.getArgument(0));

        LeadSourceRunResponse resp = service.syncRun(runId);

        assertEquals(LeadSourceRunStatus.COMPLETED, resp.getStatus());
        assertEquals(1, resp.getTotalResults());
        // Only the non-duplicate should be saved.
        verify(discoveredLeadRepository, times(1)).save(any(DiscoveredLead.class));
    }

    // T-LF-05
    @Test
    void importDiscoveredLead_createsCrmLead() {
        UUID dlId = UUID.randomUUID();
        UUID bizId = UUID.randomUUID();

        LeadSourceRun run = new LeadSourceRun(UUID.randomUUID());
        run.setImportedCount(0);
        DiscoveredLead dl = new DiscoveredLead(dlId);
        dl.setLeadSourceRun(run);
        dl.setBusinessName("Acme Media");
        dl.setContactName("Jane Doe");
        dl.setEmail("jane@acme.test");
        dl.setScore(85.0);
        dl.setStatus(DiscoveredLeadStatus.NEW);

        Business business = new Business(bizId);
        business.setName("My Biz");

        when(discoveredLeadRepository.findById(dlId)).thenReturn(Optional.of(dl));
        when(businessRepository.findById(bizId)).thenReturn(Optional.of(business));
        when(leadRepository.existsByEmailIgnoreCase("jane@acme.test")).thenReturn(false);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
            Lead l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        DiscoveredLeadResponse resp = service.importDiscoveredLead(dlId, bizId);

        assertEquals(DiscoveredLeadStatus.IMPORTED, resp.getStatus());
        assertNotNull(resp.getImportedLeadId());
        verify(leadRepository).save(any(Lead.class));
        verify(discoveredLeadRepository).save(dl);
    }

    // T-LF-06
    @Test
    void importDiscoveredLead_whenDuplicate_throwsAndSkips() {
        UUID dlId = UUID.randomUUID();
        UUID bizId = UUID.randomUUID();

        DiscoveredLead dl = new DiscoveredLead(dlId);
        dl.setLeadSourceRun(new LeadSourceRun(UUID.randomUUID()));
        dl.setEmail("dup@dup.test");
        dl.setStatus(DiscoveredLeadStatus.NEW);

        when(discoveredLeadRepository.findById(dlId)).thenReturn(Optional.of(dl));
        when(businessRepository.findById(bizId)).thenReturn(Optional.of(new Business(bizId)));
        when(leadRepository.existsByEmailIgnoreCase("dup@dup.test")).thenReturn(true);

        assertThrows(LeadFinderService.DuplicateLeadException.class,
            () -> service.importDiscoveredLead(dlId, bizId));

        verify(leadRepository, never()).save(any());
    }

    // T-LF-07
    @Test
    void rejectDiscoveredLead_updatesStatus() {
        UUID dlId = UUID.randomUUID();
        DiscoveredLead dl = new DiscoveredLead(dlId);
        dl.setLeadSourceRun(new LeadSourceRun(UUID.randomUUID()));
        dl.setStatus(DiscoveredLeadStatus.NEW);

        when(discoveredLeadRepository.findById(dlId)).thenReturn(Optional.of(dl));
        when(discoveredLeadRepository.save(any(DiscoveredLead.class))).thenAnswer(inv -> inv.getArgument(0));

        DiscoveredLeadResponse resp = service.rejectDiscoveredLead(dlId);

        assertEquals(DiscoveredLeadStatus.REJECTED, resp.getStatus());
        verify(agentLogRepository, atLeastOnce()).save(any());
    }

    // T-LF-08
    @Test
    void importBulk_countsImportedAndSkipped() {
        UUID okId = UUID.randomUUID();
        UUID dupId = UUID.randomUUID();
        UUID bizId = UUID.randomUUID();
        Business business = new Business(bizId);

        LeadSourceRun run = new LeadSourceRun(UUID.randomUUID());
        run.setImportedCount(0);

        DiscoveredLead ok = new DiscoveredLead(okId);
        ok.setLeadSourceRun(run);
        ok.setEmail("ok@ok.test");
        ok.setStatus(DiscoveredLeadStatus.NEW);

        DiscoveredLead dup = new DiscoveredLead(dupId);
        dup.setLeadSourceRun(run);
        dup.setEmail("dup@dup.test");
        dup.setStatus(DiscoveredLeadStatus.NEW);

        when(businessRepository.findById(bizId)).thenReturn(Optional.of(business));
        when(discoveredLeadRepository.findById(okId)).thenReturn(Optional.of(ok));
        when(discoveredLeadRepository.findById(dupId)).thenReturn(Optional.of(dup));
        when(leadRepository.existsByEmailIgnoreCase("ok@ok.test")).thenReturn(false);
        when(leadRepository.existsByEmailIgnoreCase("dup@dup.test")).thenReturn(true);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
            Lead l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        BulkImportResultResponse result = service.importBulk(List.of(okId, dupId), bizId);

        assertEquals(2, result.getRequested());
        assertEquals(1, result.getImported());
        assertEquals(1, result.getSkippedDuplicates());
        assertEquals(0, result.getFailed());
    }
}
