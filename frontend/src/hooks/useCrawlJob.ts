import { useCallback, useEffect, useRef, useState } from 'react';
import { crawlApi, type CrawlStatusPayload } from '../api/crawl';

const MIN_INTERVAL_MS = 2000;
const MAX_INTERVAL_MS = 15000;
const BACKOFF_FACTOR = 1.5;

const ACTIVE = new Set(['QUEUED', 'CRAWLING', 'IN_PROGRESS']);

export function isCrawlActive(status?: string | null) {
  return !!status && ACTIVE.has(status);
}

/**
 * Polls persisted crawl status with backoff. Survives navigation/refresh.
 */
export function useCrawlJob(businessId: string) {
  const [status, setStatus] = useState<CrawlStatusPayload | null>(null);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const pollTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const intervalRef = useRef(MIN_INTERVAL_MS);
  const mountedRef = useRef(true);

  const stopPolling = useCallback(() => {
    if (pollTimeoutRef.current) {
      clearTimeout(pollTimeoutRef.current);
      pollTimeoutRef.current = null;
    }
    intervalRef.current = MIN_INTERVAL_MS;
  }, []);

  const schedulePoll = useCallback(() => {
    stopPolling();
    pollTimeoutRef.current = setTimeout(async () => {
      try {
        const response = await crawlApi.getCrawlStatus(businessId);
        if (!mountedRef.current) return;
        if (response.success && response.data) {
          setStatus(response.data);
          if (isCrawlActive(response.data.status)) {
            intervalRef.current = Math.min(intervalRef.current * BACKOFF_FACTOR, MAX_INTERVAL_MS);
            schedulePoll();
          } else {
            stopPolling();
          }
        } else {
          intervalRef.current = Math.min(intervalRef.current * BACKOFF_FACTOR, MAX_INTERVAL_MS);
          schedulePoll();
        }
      } catch {
        if (!mountedRef.current) return;
        intervalRef.current = Math.min(intervalRef.current * BACKOFF_FACTOR, MAX_INTERVAL_MS);
        schedulePoll();
      }
    }, intervalRef.current);
  }, [businessId, stopPolling]);

  const startCrawl = useCallback(async () => {
    if (starting || isCrawlActive(status?.status)) return;
    setStarting(true);
    setError(null);
    try {
      const response = await crawlApi.startCrawl(businessId);
      if (response.success && response.data) {
        setStatus(response.data);
        intervalRef.current = MIN_INTERVAL_MS;
        if (isCrawlActive(response.data.status)) {
          schedulePoll();
        }
      } else {
        setError(response.error || response.message || 'Failed to start crawl');
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to start crawl');
    } finally {
      setStarting(false);
    }
  }, [businessId, starting, status, schedulePoll]);

  useEffect(() => {
    mountedRef.current = true;
    const restore = async () => {
      try {
        const response = await crawlApi.getCrawlStatus(businessId);
        if (response.success && response.data) {
          setStatus(response.data);
          if (isCrawlActive(response.data.status)) {
            intervalRef.current = MIN_INTERVAL_MS;
            schedulePoll();
          }
        }
      } catch {
        // Keep last known UI state; backend remains source of truth on next poll.
      }
    };
    void restore();
    return () => {
      mountedRef.current = false;
      stopPolling();
    };
  }, [businessId, schedulePoll, stopPolling]);

  return {
    status,
    starting,
    error,
    startCrawl,
    isActive: isCrawlActive(status?.status),
    refresh: schedulePoll,
  };
}
