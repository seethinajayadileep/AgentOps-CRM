package com.agentopscrm.service;

import com.agentopscrm.client.FirecrawlClient;
import com.agentopscrm.entity.Business;
import com.agentopscrm.entity.enums.CrawlStatus;
import com.agentopscrm.repository.BusinessRepository;
import com.agentopscrm.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlServiceTest {

    @Mock private BusinessRepository businessRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private FirecrawlClient firecrawlClient;
    @Mock private CrawlStateWriter crawlStateWriter;
    @Mock private CrawlAsyncRunner crawlAsyncRunner;

    private CrawlService service;

    @BeforeEach
    void setUp() {
        service = new CrawlService(businessRepository, documentRepository, auditLogService,
                firecrawlClient, crawlStateWriter, crawlAsyncRunner);
    }

    @Test
    void startCrawl_commitsCrawlingAndReturnsImmediately() throws Exception {
        UUID id = UUID.randomUUID();
        Business business = new Business(id);
        business.setWebsiteUrl("https://linear.app");
        business.setCrawlStatus(CrawlStatus.NOT_STARTED);
        when(businessRepository.findById(id)).thenReturn(Optional.of(business));
        when(firecrawlClient.isConfigured()).thenReturn(true);
        Business crawling = new Business(id);
        crawling.setCrawlStatus(CrawlStatus.CRAWLING);
        when(businessRepository.findById(id)).thenReturn(Optional.of(business), Optional.of(crawling));

        CrawlService.CrawlResult result = service.startCrawl(id);

        assertTrue(result.isSuccess());
        assertEquals(CrawlStatus.CRAWLING, result.getStatus());
        verify(crawlStateWriter).markQueued(id);
        verify(crawlStateWriter).markCrawling(id);
        verify(crawlAsyncRunner).runCrawl(id);
        verify(firecrawlClient, never()).executeCrawl(any(), anyInt());
    }

    @Test
    void startCrawl_whenAlreadyActive_doesNotStartDuplicate() throws Exception {
        UUID id = UUID.randomUUID();
        Business business = new Business(id);
        business.setCrawlStatus(CrawlStatus.CRAWLING);
        when(businessRepository.findById(id)).thenReturn(Optional.of(business));

        CrawlService.CrawlResult result = service.startCrawl(id);

        assertFalse(result.isSuccess());
        assertTrue(result.getStatus().isActive());
        verify(crawlAsyncRunner, never()).runCrawl(any());
    }

    @Test
    void performCrawl_marksFailedWithoutLeakingExceptionText() throws Exception {
        UUID id = UUID.randomUUID();
        Business business = new Business(id);
        business.setWebsiteUrl("https://linear.app");
        business.setCrawlStatus(CrawlStatus.CRAWLING);
        when(businessRepository.findById(id)).thenReturn(Optional.of(business));
        when(firecrawlClient.executeCrawl(any(), anyInt(), any()))
                .thenThrow(new FirecrawlClient.FirecrawlException(
                        "PKIX path building failed https://api.firecrawl.dev/v1/crawl"));

        service.performCrawl(id);

        verify(crawlStateWriter).markFailed(eq(id), eq(com.agentopscrm.util.SafeErrorMessages.CRAWL_FAILED));
        verify(auditLogService).logAgentActionWithError(eq(id), eq("Crawler"), eq("CRAWL_FAILED"),
                any(), any(), any(), eq(com.agentopscrm.util.SafeErrorMessages.CRAWL_FAILED), any());
    }
}
