import { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, RefreshCw, Download } from 'lucide-react';
import { leadFinderApi } from '../api/leadFinderApi';
import { businessApi } from '../api/business';
import type { DiscoveredLead, LeadSourceRun } from '../types/leadFinder';
import type { Business } from '../types/index';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import EmptyState from '../components/ui/EmptyState';
import LoadingState from '../components/ui/LoadingState';
import StatusBadge from '../components/ui/StatusBadge';

/**
 * Lead Finder run results page (F-010).
 */
export default function LeadFinderResults() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [run, setRun] = useState<LeadSourceRun | null>(null);
  const [leads, setLeads] = useState<DiscoveredLead[]>([]);
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [targetBusinessId, setTargetBusinessId] = useState<string>('');
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [autoSyncing, setAutoSyncing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [rawViewer, setRawViewer] = useState<DiscoveredLead | null>(null);
  const syncingRef = useRef(false);

  useEffect(() => {
    if (id) void load(id);
  }, [id]);

  // Auto-polling for RUNNING searches (Bug #7 fix)
  useEffect(() => {
    if (!run || run.status !== 'RUNNING' || !id) return;

    console.log(`[Auto-sync] Starting auto-poll for run ${id} (status: ${run.status})`);
    
    const pollInterval = setInterval(async () => {
      // Skip if already syncing
      if (syncingRef.current) {
        console.log(`[Auto-sync] Skipping poll - sync already in progress`);
        return;
      }

      try {
        syncingRef.current = true;
        setAutoSyncing(true);
        console.log(`[Auto-sync] Polling run ${id}...`);
        
        await leadFinderApi.syncRun(id);
        await load(id);
        
        console.log(`[Auto-sync] Poll complete for run ${id}`);
      } catch (error: any) {
        console.error(`[Auto-sync] Error polling run ${id}:`, error);
      } finally {
        syncingRef.current = false;
        setAutoSyncing(false);
      }
    }, 7000); // Poll every 7 seconds

    return () => {
      console.log(`[Auto-sync] Stopping auto-poll for run ${id}`);
      clearInterval(pollInterval);
      syncingRef.current = false;
      setAutoSyncing(false);
    };
  }, [run?.status, id]);

  const load = async (runId: string) => {
    setLoading(true);
    try {
      const [runData, results, bizResp] = await Promise.all([
        leadFinderApi.getRun(runId),
        leadFinderApi.getResults(runId),
        businessApi.getAllBusinesses({ page: 0, size: 100 }).catch(() => null),
      ]);
      setRun(runData);
      setLeads(results);
      const items = bizResp?.data?.items ?? [];
      setBusinesses(items);
      if (items.length === 1) setTargetBusinessId(items[0].id);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to load results');
    } finally {
      setLoading(false);
    }
  };

  const handleSync = async () => {
    if (!id || syncingRef.current) return;
    syncingRef.current = true;
    setSyncing(true);
    setError(null);
    setSuccess(null);
    try {
      await leadFinderApi.syncRun(id);
      await load(id);
      setSuccess('Results synced from Apify.');
    } catch (err: any) {
      setError(err.message || 'Failed to sync results');
    } finally {
      setSyncing(false);
      syncingRef.current = false;
    }
  };

  const handleImport = async (leadId: string) => {
    setError(null);
    setSuccess(null);
    try {
      await leadFinderApi.importLead(leadId, targetBusinessId || undefined);
      if (id) await load(id);
      setSuccess('Lead imported into CRM.');
    } catch (err: any) {
      setError(err.message || 'Failed to import lead');
    }
  };

  const handleReject = async (leadId: string) => {
    setError(null);
    setSuccess(null);
    try {
      await leadFinderApi.rejectLead(leadId);
      if (id) await load(id);
    } catch (err: any) {
      setError(err.message || 'Failed to reject lead');
    }
  };

  const handleBulkImport = async () => {
    if (selected.size === 0) return;
    setError(null);
    setSuccess(null);
    try {
      const result = await leadFinderApi.importBulk(Array.from(selected), targetBusinessId || undefined);
      setSuccess(
        `Imported ${result.imported} of ${result.requested}. ` +
          `Skipped ${result.skippedDuplicates} duplicate(s), ${result.failed} failed.`
      );
      setSelected(new Set());
      if (id) await load(id);
    } catch (err: any) {
      setError(err.message || 'Failed to bulk import');
    }
  };

  const toggleSelect = (leadId: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(leadId)) next.delete(leadId);
      else next.add(leadId);
      return next;
    });
  };

  const toggleSelectAll = () => {
    const importable = leads.filter((l) => l.status === 'NEW' || l.status === 'REVIEWED');
    if (selected.size === importable.length && importable.length > 0) {
      setSelected(new Set());
    } else {
      setSelected(new Set(importable.map((l) => l.id)));
    }
  };

  if (loading) {
    return <LoadingState label="Loading results…" />;
  }

  return (
    <div>
      <PageHeader
        title={run?.searchName || 'Run Results'}
        subtitle={
          `${run?.industry ? `Industry: ${run.industry} · ` : ''}` +
          `${run?.location ? `Location: ${run.location} · ` : ''}` +
          `Status: ${run?.status} · ${run?.totalResults ?? 0} results · ${run?.importedCount ?? 0} imported`
        }
        back={
          <button
            onClick={() => navigate('/lead-finder')}
            className="inline-flex items-center gap-2 text-sm text-zinc-400 hover:text-zinc-100"
          >
            <ArrowLeft size={18} />
            Back to Lead Finder
          </button>
        }
        action={
          <div className="flex items-center gap-3">
            {run?.status === 'RUNNING' && autoSyncing && (
              <span className="flex items-center gap-2 text-sm text-yellow-400">
                <div className="h-2 w-2 animate-pulse rounded-full bg-yellow-500"></div>
                Auto-syncing...
              </span>
            )}
            <button onClick={handleSync} disabled={syncing || autoSyncing} className="btn-primary">
              <RefreshCw size={16} className={syncing || autoSyncing ? 'animate-spin' : ''} />
              {syncing || autoSyncing ? 'Syncing…' : 'Sync from Apify'}
            </button>
          </div>
        }
      />

      {error && (
        <div className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">{error}</div>
      )}
      {success && (
        <div className="mb-6 rounded-xl border border-[#22C55E]/30 bg-[#22C55E]/10 p-4 text-[#4ade80]">{success}</div>
      )}

      {/* Import controls */}
      <Card className="mb-6 flex flex-wrap items-end gap-4 p-4">
        <div>
          <label className="label-dark">Target Business (for import)</label>
          <select
            value={targetBusinessId}
            onChange={(e) => setTargetBusinessId(e.target.value)}
            className="input-dark min-w-[220px]"
          >
            <option value="">Auto (only if single business)</option>
            {businesses.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </select>
        </div>
        <button onClick={handleBulkImport} disabled={selected.size === 0} className="btn-success">
          <Download size={16} />
          Import Selected ({selected.size})
        </button>
      </Card>

      {/* Results table */}
      {leads.length === 0 ? (
        <EmptyState
          icon={<Download size={26} />}
          title="No discovered leads yet"
          description='Click "Sync from Apify" to fetch results for this run.'
        />
      ) : (
        <div className="table-card">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="border-b border-white/[0.06] bg-white/[0.02]">
                <tr>
                  <th className="px-4 py-3">
                    <input type="checkbox" onChange={toggleSelectAll} aria-label="Select all" className="accent-primary-500" />
                  </th>
                  {['Business', 'Contact', 'Email', 'Phone', 'Website', 'Location', 'Score', 'Status', 'Actions'].map(
                    (h) => (
                      <th
                        key={h}
                        className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-zinc-400"
                      >
                        {h}
                      </th>
                    )
                  )}
                </tr>
              </thead>
              <tbody>
                {leads.map((lead) => {
                  const importable = lead.status === 'NEW' || lead.status === 'REVIEWED';
                  return (
                    <tr
                      key={lead.id}
                      className="border-b border-white/[0.04] transition-colors duration-200 hover:bg-white/[0.03]"
                    >
                      <td className="px-4 py-4">
                        <input
                          type="checkbox"
                          checked={selected.has(lead.id)}
                          disabled={!importable}
                          onChange={() => toggleSelect(lead.id)}
                          aria-label={`Select ${lead.businessName || lead.id}`}
                          className="accent-primary-500"
                        />
                      </td>
                      <td className="px-4 py-4 text-sm font-medium text-zinc-100">{lead.businessName || '-'}</td>
                      <td className="px-4 py-4 text-sm text-zinc-400">{lead.contactName || '-'}</td>
                      <td className="px-4 py-4 text-sm text-zinc-400">{lead.email || '-'}</td>
                      <td className="px-4 py-4 text-sm text-zinc-400">{lead.phone || '-'}</td>
                      <td className="max-w-[160px] truncate px-4 py-4 text-sm text-zinc-400">
                        {lead.websiteUrl ? (
                          <a href={lead.websiteUrl} target="_blank" rel="noreferrer" className="text-blue-300 hover:underline">
                            {lead.websiteUrl}
                          </a>
                        ) : (
                          '-'
                        )}
                      </td>
                      <td className="px-4 py-4 text-sm text-zinc-400">{lead.location || '-'}</td>
                      <td className="px-4 py-4 text-sm text-zinc-400">
                        {lead.score != null ? Math.round(lead.score) : '-'}
                      </td>
                      <td className="whitespace-nowrap px-4 py-4">
                        <StatusBadge status={lead.status} />
                      </td>
                      <td className="whitespace-nowrap px-4 py-4 text-sm">
                        <div className="flex items-center gap-3">
                          {importable && (
                            <>
                              <button
                                onClick={() => handleImport(lead.id)}
                                className="font-medium text-[#4ade80] hover:text-[#22C55E]"
                              >
                                Import
                              </button>
                              <button
                                onClick={() => handleReject(lead.id)}
                                className="font-medium text-red-400 hover:text-red-300"
                              >
                                Reject
                              </button>
                            </>
                          )}
                          {lead.rawDataJson && (
                            <button onClick={() => setRawViewer(lead)} className="text-zinc-500 hover:text-zinc-200">
                              Raw
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Raw data modal */}
      {rawViewer && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
          onClick={() => setRawViewer(null)}
        >
          <div
            className="glass-card max-h-[80vh] w-full max-w-2xl overflow-auto p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-lg font-semibold text-white">
                Raw data · {rawViewer.businessName || rawViewer.id}
              </h3>
              <button onClick={() => setRawViewer(null)} className="text-zinc-400 hover:text-zinc-100">
                ✕
              </button>
            </div>
            <pre className="overflow-auto whitespace-pre-wrap rounded-xl border border-white/[0.06] bg-black/40 p-3 text-xs text-zinc-300">
              {(() => {
                try {
                  return JSON.stringify(JSON.parse(rawViewer.rawDataJson || '{}'), null, 2);
                } catch {
                  return rawViewer.rawDataJson;
                }
              })()}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
}
