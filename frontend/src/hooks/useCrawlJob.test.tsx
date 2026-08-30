import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useCrawlJob } from './useCrawlJob';

vi.mock('../api/crawl', () => ({
  crawlApi: {
    startCrawl: vi.fn(),
    getCrawlStatus: vi.fn(),
  },
}));

import { crawlApi } from '../api/crawl';

describe('useCrawlJob polling', () => {
  beforeEach(() => {
    vi.mocked(crawlApi.getCrawlStatus).mockReset();
    vi.mocked(crawlApi.startCrawl).mockReset();
  });

  it('restores a running crawl from the backend so refresh keeps CRAWLING', async () => {
    vi.mocked(crawlApi.getCrawlStatus).mockResolvedValue({
      success: true,
      data: { status: 'CRAWLING', message: 'running', pagesSaved: 2, pagesTotal: 10, elapsedSeconds: 12 },
    });

    const { result } = renderHook(() => useCrawlJob('biz-1'));

    await waitFor(() => {
      expect(result.current.status?.status).toBe('CRAWLING');
      expect(result.current.isActive).toBe(true);
    });
  });

  it('does not start a duplicate crawl while one is active', async () => {
    vi.mocked(crawlApi.getCrawlStatus).mockResolvedValue({
      success: true,
      data: { status: 'QUEUED', message: 'queued' },
    });
    const { result } = renderHook(() => useCrawlJob('biz-1'));
    await waitFor(() => expect(result.current.isActive).toBe(true));
    await result.current.startCrawl();
    expect(crawlApi.startCrawl).not.toHaveBeenCalled();
  });
});
