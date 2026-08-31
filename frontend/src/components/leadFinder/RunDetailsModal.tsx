import { useState } from 'react';
import { X, RefreshCw, RotateCcw } from 'lucide-react';
import type { LeadSourceRun } from '../../types/leadFinder';
import StatusBadge from '../ui/StatusBadge';
import Modal from '../ui/Modal';
import { safeClientError } from '../../util/safeClientError';
import { formatServerDateTime, isOlderThan } from '../../util/serverDate';

interface RunDetailsModalProps {
  run: LeadSourceRun;
  onClose: () => void;
  onSync: (id: string) => Promise<void>;
}

/**
  * Run Details modal (Bug 4 requirement): shows search parameters, local status,
  * Apify run/dataset id, result/import counts, timestamps, and a safe failure
  * reason. Provides Refresh (manual sync) and Retry (for FAILED runs) actions.
  */
export default function RunDetailsModal({ run, onClose, onSync }: RunDetailsModalProps) {
  const [syncing, setSyncing] = useState(false);

  const handleSync = async () => {
    if (syncing) return;
    setSyncing(true);
    try {
      await onSync(run.id);
    } finally {
      setSyncing(false);
    }
  };

  const isStale =
    run.status === 'RUNNING' &&
    run.lastSyncedAt &&
    isOlderThan(run.lastSyncedAt, 30 * 60 * 1000);

  return (
    <Modal title="Run Details" onClose={onClose} className="max-w-2xl p-6">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-ink">Run Details</h2>
          <button onClick={onClose} className="text-slate hover:text-ink" aria-label="Close">
            <X size={20} />
          </button>
        </div>

        <div className="mb-4 flex items-center gap-3">
          <StatusBadge status={run.status} />
          {isStale && (
            <span className="rounded-full border border-amber-500/30 bg-amber-500/10 px-2 py-0.5 text-xs text-amber-300">
              Stale - no update in over 30 minutes
            </span>
          )}
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Section title="Search Parameters">
            <Field label="Search Name" value={run.searchName} />
            <Field label="Industry" value={run.industry} />
            <Field label="Location" value={run.location} />
            <Field label="Keywords" value={run.keywords} />
            <Field label="Max Results" value={run.maxResults?.toString()} />
            <Field label="Actor" value={run.actorId ? 'Recorded' : '—'} />
          </Section>

          <Section title="Apify Status">
            <Field label="Local Status" value={run.status} />
            <Field label="Provider run" value={run.apifyRunId ? 'Recorded' : '—'} />
            <Field label="Provider dataset" value={run.apifyDatasetId ? 'Recorded' : '—'} />
            <Field label="Last Synced" value={formatDate(run.lastSyncedAt)} />
          </Section>

          <Section title="Results">
            <Field label="Total Results" value={String(run.totalResults ?? 0)} />
            <Field label="Imported Count" value={String(run.importedCount ?? 0)} />
          </Section>

          <Section title="Timeline">
            <Field label="Started" value={formatDate(run.createdAt)} />
            <Field label="Updated" value={formatDate(run.updatedAt)} />
          </Section>
        </div>

        {run.status === 'FAILED' && (
          <div className="mt-4 rounded-lg border border-frost bg-mist p-3">
            <div className="text-xs font-semibold uppercase tracking-wide text-ink">
              {run.failureCode || 'Failure Reason'}
            </div>
            <div className="mt-1 text-sm text-ink">
              {safeClientError(run.failureReason, 'This run failed. Check Lead Finder settings and try again.')}
            </div>
          </div>
        )}

        <div className="mt-6 flex gap-2">
          <button onClick={handleSync} disabled={syncing} className="btn-secondary">
            <RefreshCw size={16} className={syncing ? 'animate-spin' : ''} />
            {syncing ? 'Refreshing…' : 'Refresh'}
          </button>
          {run.status === 'FAILED' && (
            <button onClick={handleSync} disabled={syncing} className="btn-primary">
              <RotateCcw size={16} />
              Retry
            </button>
          )}
        </div>
    </Modal>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-lg bg-mist p-3">
      <div className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate">{title}</div>
      <div className="space-y-1.5">{children}</div>
    </div>
  );
}

function Field({ label, value, mono }: { label: string; value?: string | null; mono?: boolean }) {
  return (
    <div className="flex justify-between text-sm">
      <span className="text-slate">{label}</span>
      <span className={`text-ink ${mono ? 'font-mono text-xs' : ''}`}>{value || '—'}</span>
    </div>
  );
}

function formatDate(value?: string | null): string {
  if (!value) return '—';
  return formatServerDateTime(value) || '—';
}
