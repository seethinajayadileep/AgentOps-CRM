import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { leadFinderApi } from '../api/leadFinderApi';
import type { LeadSourceRun, StartLeadFinderRunRequest } from '../types/leadFinder';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import EmptyState from '../components/ui/EmptyState';
import LoadingState from '../components/ui/LoadingState';
import StatusBadge from '../components/ui/StatusBadge';
import RunDetailsModal from '../components/leadFinder/RunDetailsModal';
import { safeClientError } from '../util/safeClientError';

/**
  * Lead Finder page (F-010 Apify Lead Finder).
  */
export default function LeadFinder() {
  const navigate = useNavigate();

  const [runs, setRuns] = useState<LeadSourceRun[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [apifyConfigured, setApifyConfigured] = useState<boolean | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [selectedRun, setSelectedRun] = useState<LeadSourceRun | null>(null);

  const [form, setForm] = useState<StartLeadFinderRunRequest>({
    searchName: '',
    industry: '',
    location: '',
    keywords: '',
    maxResults: 25,
  });

  useEffect(() => {
    void init();
  }, []);

  const init = async () => {
    setLoading(true);
    try {
      const [config, runList] = await Promise.all([
        leadFinderApi.getConfig().catch(() => ({ apifyConfigured: false })),
        leadFinderApi.listRuns().catch(() => []),
      ]);
      setApifyConfigured(config.apifyConfigured);
      setRuns(runList);
      setError(null);
    } catch (err: any) {
      setError(safeClientError(err, 'Failed to load lead finder'));
    } finally {
      setLoading(false);
    }
  };

  const loadRuns = async () => {
    try {
      const runList = await leadFinderApi.listRuns();
      setRuns(runList);
    } catch (err: any) {
      setError(safeClientError(err, 'Failed to load runs'));
    }
  };

  const handleChange = (field: keyof StartLeadFinderRunRequest, value: string) => {
    setForm((prev) => ({
      ...prev,
      [field]: field === 'maxResults' ? (value === '' ? undefined : Number(value)) : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!form.searchName.trim()) {
      setError('Search name is required');
      return;
    }

    setSubmitting(true);
    try {
      const run = await leadFinderApi.startRun(form);
      setSuccess(`Search "${run.searchName}" started.`);
      setForm({ searchName: '', industry: '', location: '', keywords: '', maxResults: 25 });
      await loadRuns();
    } catch (err: any) {
      setError(safeClientError(err, 'Failed to start search'));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <LoadingState label="Loading Lead Finder…" />;
  }

  return (
    <div>
      <PageHeader
        title="Lead Finder"
        subtitle="Discover new outbound prospects with Apify, review them, and import into your CRM."
      />

      {apifyConfigured === false && (
        <div className="mb-6 rounded-sm border border-frost bg-mist p-4 text-ink">
          <strong>Apify is not configured.</strong> Set <code className="text-amber-200">APIFY_ENABLED=true</code> and
          provide <code className="text-amber-200">APIFY_API_TOKEN</code> (and an actor id) on the server to enable
          outbound lead discovery.
        </div>
      )}

      {error && (
        <div className="mb-6 rounded-sm border border-frost bg-mist p-4 text-ink">{error}</div>
      )}
      {success && (
        <div className="mb-6 rounded-sm border border-frost bg-mist p-4 text-ink">{success}</div>
      )}

      <Card className="mb-6 p-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <h2 className="text-lg font-semibold text-ink">New Search</h2>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div>
              <label htmlFor="lead-finder-search-name" className="label-dark">
                Search Name *
              </label>
              <input
                id="lead-finder-search-name"
                type="text"
                value={form.searchName}
                onChange={(e) => handleChange('searchName', e.target.value)}
                placeholder="e.g. Hyderabad ad agencies"
                className="input-dark"
              />
            </div>
            <div>
              <label htmlFor="lead-finder-industry" className="label-dark">
                Industry
              </label>
              <input
                id="lead-finder-industry"
                type="text"
                value={form.industry}
                onChange={(e) => handleChange('industry', e.target.value)}
                placeholder="e.g. Advertising agencies"
                className="input-dark"
              />
            </div>
            <div>
              <label htmlFor="lead-finder-location" className="label-dark">
                Location
              </label>
              <input
                id="lead-finder-location"
                type="text"
                value={form.location}
                onChange={(e) => handleChange('location', e.target.value)}
                placeholder="e.g. Hyderabad"
                className="input-dark"
              />
            </div>
            <div>
              <label htmlFor="lead-finder-keywords" className="label-dark">
                Keywords
              </label>
              <input
                id="lead-finder-keywords"
                type="text"
                value={form.keywords}
                onChange={(e) => handleChange('keywords', e.target.value)}
                placeholder="e.g. media buying"
                className="input-dark"
              />
            </div>
            <div>
              <label htmlFor="lead-finder-max-results" className="label-dark">
                Max Results
              </label>
              <input
                id="lead-finder-max-results"
                type="number"
                min={1}
                value={form.maxResults ?? ''}
                onChange={(e) => handleChange('maxResults', e.target.value)}
                className="input-dark"
              />
            </div>
          </div>
          <div>
            <button type="submit" disabled={submitting || apifyConfigured === false} className="btn-primary">
              <Search size={16} />
              {submitting ? 'Starting…' : 'Start Search'}
            </button>
          </div>
        </form>
      </Card>

      {/* Runs table */}
      <h2 className="mb-3 text-lg font-semibold text-ink">Lead Finder Runs</h2>
      {runs.length === 0 ? (
        <EmptyState
          icon={<Search size={26} />}
          title="No searches yet"
          description="Start a search above to discover new prospects."
        />
      ) : (
        <div className="table-card">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="border-b border-frost bg-mist">
                <tr>
                  {['Search', 'Industry', 'Location', 'Status', 'Results', 'Imported', 'Created', 'Actions'].map(
                    (h) => (
                      <th
                        key={h}
                        className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate"
                      >
                        {h}
                      </th>
                    )
                  )}
                </tr>
              </thead>
              <tbody>
                {runs.map((run) => {
                  const isStale =
                    run.status === 'RUNNING' &&
                    run.lastSyncedAt &&
                    Date.now() - new Date(run.lastSyncedAt).getTime() > 30 * 60 * 1000;
                  return (
                    <tr
                      key={run.id}
                      onClick={() => setSelectedRun(run)}
                      className="cursor-pointer border-b border-frost transition-colors duration-200 hover:bg-mist"
                    >
                      <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-ink">
                        {run.searchName}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate">{run.industry || '-'}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate">{run.location || '-'}</td>
                      <td className="whitespace-nowrap px-6 py-4">
                        <StatusBadge status={run.status} />
                        {run.status === 'RUNNING' && (
                          <div className="mt-1 text-xs text-slate">
                            {isStale ? (
                              <span className="text-amber-400">Stale - no update in 30+ min</span>
                            ) : (
                              <span>
                                Last synced:{' '}
                                {run.lastSyncedAt ? new Date(run.lastSyncedAt).toLocaleTimeString() : 'never'}
                              </span>
                            )}
                          </div>
                        )}
                        {run.status === 'FAILED' && run.failureReason && (
                          <div className="mt-1 max-w-xs truncate text-xs text-ink">
                            {safeClientError(run.failureReason, 'This search failed. Check Lead Finder settings and try again.')}
                          </div>
                        )}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate">{run.totalResults}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate">{run.importedCount}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm text-slate">
                        {new Date(run.createdAt).toLocaleString()}
                      </td>
                      <td className="whitespace-nowrap px-6 py-4 text-sm" onClick={(e) => e.stopPropagation()}>
                        <button
                          onClick={() => navigate(`/lead-finder/${run.id}`)}
                          className="font-medium text-ink hover:text-ink"
                        >
                          View Results
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {selectedRun && (
        <RunDetailsModal
          run={selectedRun}
          onClose={() => setSelectedRun(null)}
          onSync={async (id) => {
            try {
              const updated = await leadFinderApi.syncRun(id);
              setSelectedRun(updated);
              await loadRuns();
            } catch (err: any) {
              setError(safeClientError(err, 'Failed to sync run'));
            }
          }}
        />
      )}
    </div>
  );
}
