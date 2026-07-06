import { useState } from 'react';
import { X, RefreshCw, RotateCcw } from 'lucide-react';
import type { LeadSourceRun } from '../../types/leadFinder';
import StatusBadge from '../ui/StatusBadge';

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
    Date.now() - new Date(run.lastSyncedAt).getTime() > 30 * 60 * 1000;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="max-h-[85vh] w-full max-w-2xl overflow-y-auto rounded-2xl border border-white/10 bg-zinc-900 p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-white">Run Details</h2>
          <button onClick={onClose} className="text-zinc-400 hover:text-zinc-100" aria-label="Close">
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
            <Field label="Actor ID" value={run.actorId} />
          </Section>

          <Section title="Apify Status">
            <Field label="Local Status" value={run.status} />
            <Field label="Apify Run ID" value={run.apifyRunId} mono />
            <Field label="Apify Dataset ID" value={run.apifyDatasetId} mono />
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
          <div className="mt-4 rounded-lg border border-red-500/30 bg-red-500/10 p-3">
            <div className="text-xs font-semibold uppercase tracking-wide text-red-400">
              {run.failureCode || 'Failure Reason'}
            </div>
            <div className="mt-1 text-sm text-red-300">
              {run.failureReason || 'This run failed for an unknown reason.'}
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
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-lg bg-white/[0.02] p-3">
      <div className="mb-2 text-xs font-semibold uppercase tracking-wide text-zinc-500">{title}</div>
      <div className="space-y-1.5">{children}</div>
    </div>
  );
}

function Field({ label, value, mono }: { label: string; value?: string | null; mono?: boolean }) {
  return (
    <div className="flex justify-between text-sm">
      <span className="text-zinc-500">{label}</span>
      <span className={`text-zinc-200 ${mono ? 'font-mono text-xs' : ''}`}>{value || '—'}</span>
    </div>
  );
}

function formatDate(value?: string | null): string {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}
