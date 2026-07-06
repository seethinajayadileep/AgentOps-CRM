import { useCallback, useEffect, useRef, useState } from 'react';
import { ragApi, KB_JOB_TERMINAL_STATUSES, type KnowledgeBaseJob } from '../api/rag';

const STORAGE_KEY_PREFIX = 'kb-build-job:';

// Bounded exponential backoff for polling: starts fast for responsive UI,
// backs off to avoid hammering the server on long-running builds, and is
// capped so status is never stale for more than MAX_INTERVAL_MS.
const MIN_INTERVAL_MS = 2000;
const MAX_INTERVAL_MS = 15000;
const BACKOFF_FACTOR = 1.5;

/**
 * Hook powering the Bug 2 async knowledge-base build workflow on the frontend:
 *  - Starts a build (backend returns 202 immediately with a QUEUED job).
 *  - Polls the job status endpoint with bounded exponential backoff instead
 *    of a single blocking request that could exceed the frontend's own
 *    request timeout.
 *  - Persists the active jobId in localStorage per business so a page
 *    refresh restores polling for a build that is still running, and also
 *    asks the backend for any active job on mount (source of truth).
 *  - Never shows a false "timeout" once the backend has accepted the job -
 *    the UI only reflects the real job status/progress.
 */
export function useKnowledgeBaseBuildJob(businessId: string) {
  const [job, setJob] = useState<KnowledgeBaseJob | null>(null);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const pollTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const intervalRef = useRef(MIN_INTERVAL_MS);
  const mountedRef = useRef(true);

  const storageKey = `${STORAGE_KEY_PREFIX}${businessId}`;

  const stopPolling = useCallback(() => {
    if (pollTimeoutRef.current) {
      clearTimeout(pollTimeoutRef.current);
      pollTimeoutRef.current = null;
    }
    intervalRef.current = MIN_INTERVAL_MS;
  }, []);

  const isActive = (j: KnowledgeBaseJob | null) =>
    !!j && !KB_JOB_TERMINAL_STATUSES.includes(j.status);

  const schedulePoll = useCallback(
    (jobId: string) => {
      stopPolling();
      pollTimeoutRef.current = setTimeout(async () => {
        try {
          const response = await ragApi.getKnowledgeBaseJob(businessId, jobId);
          if (!mountedRef.current) return;

          if (response.success && response.data) {
            setJob(response.data);
            if (isActive(response.data)) {
              intervalRef.current = Math.min(intervalRef.current * BACKOFF_FACTOR, MAX_INTERVAL_MS);
              schedulePoll(jobId);
            } else {
              // Terminal state reached - stop polling and clear persisted job.
              localStorage.removeItem(storageKey);
              stopPolling();
            }
          } else {
            // Do not treat a transient polling error as a build timeout -
            // keep retrying with backoff rather than surfacing a false failure.
            intervalRef.current = Math.min(intervalRef.current * BACKOFF_FACTOR, MAX_INTERVAL_MS);
            schedulePoll(jobId);
          }
        } catch {
          if (!mountedRef.current) return;
          intervalRef.current = Math.min(intervalRef.current * BACKOFF_FACTOR, MAX_INTERVAL_MS);
          schedulePoll(jobId);
        }
      }, intervalRef.current);
    },
    [businessId, storageKey, stopPolling]
  );

  const startBuild = useCallback(async () => {
    if (starting || isActive(job)) return; // Prevent duplicate submissions.
    setStarting(true);
    setError(null);
    try {
      const response = await ragApi.buildKnowledgeBase(businessId);
      if (response.success && response.data) {
        setJob(response.data);
        localStorage.setItem(storageKey, response.data.jobId);
        intervalRef.current = MIN_INTERVAL_MS;
        schedulePoll(response.data.jobId);
      } else {
        setError(response.error || response.message || 'Failed to start knowledge base build');
      }
    } catch (err: any) {
      setError(err.message || 'Failed to start knowledge base build');
    } finally {
      setStarting(false);
    }
  }, [businessId, job, starting, storageKey, schedulePoll]);

  // Restore an in-flight job on mount (page refresh recovery). Prefer the
  // backend's active-job endpoint (source of truth); fall back to a
  // locally-persisted jobId if present so a slow network doesn't briefly
  // show "no build in progress" for an active build.
  useEffect(() => {
    mountedRef.current = true;

    const restore = async () => {
      try {
        const activeResponse = await ragApi.getActiveKnowledgeBaseJob(businessId);
        if (activeResponse.success && activeResponse.data) {
          setJob(activeResponse.data);
          localStorage.setItem(storageKey, activeResponse.data.jobId);
          intervalRef.current = MIN_INTERVAL_MS;
          schedulePoll(activeResponse.data.jobId);
          return;
        }
      } catch {
        // Fall through to localStorage-based restore below.
      }

      const savedJobId = localStorage.getItem(storageKey);
      if (savedJobId) {
        try {
          const response = await ragApi.getKnowledgeBaseJob(businessId, savedJobId);
          if (response.success && response.data && isActive(response.data)) {
            setJob(response.data);
            intervalRef.current = MIN_INTERVAL_MS;
            schedulePoll(savedJobId);
          } else {
            localStorage.removeItem(storageKey);
          }
        } catch {
          localStorage.removeItem(storageKey);
        }
      }
    };

    void restore();

    return () => {
      mountedRef.current = false;
      stopPolling();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [businessId]);

  return {
    job,
    starting,
    error,
    startBuild,
    isBuildActive: isActive(job),
  };
}
