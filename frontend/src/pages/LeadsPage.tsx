import { useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Search, Users, X } from 'lucide-react';
import { leadsApi } from '../api/leadsApi';
import type { Lead } from '../types/lead';
import { LeadStatus } from '../types/lead';
import { LeadStatusBadge } from '../components/leads/LeadStatusBadge';
import { LeadScoreBadge } from '../components/leads/LeadScoreBadge';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import EmptyState from '../components/ui/EmptyState';
import ErrorBanner from '../components/ui/ErrorBanner';
import LoadingState from '../components/ui/LoadingState';
import { formatServerDate } from '../util/serverDate';

const VALID_STATUSES = Object.values(LeadStatus);

const STATUS_OPTIONS: { value: LeadStatus; label: string }[] = [
  { value: LeadStatus.NEW, label: 'New' },
  { value: LeadStatus.QUALIFIED, label: 'Qualified' },
  { value: LeadStatus.HOT, label: 'Hot' },
  { value: LeadStatus.COLD, label: 'Cold' },
  { value: LeadStatus.FOLLOWED_UP, label: 'Followed up' },
  { value: LeadStatus.CLOSED, label: 'Closed' },
];

function leadMatchesSearch(lead: Lead, query: string): boolean {
  const q = query.trim().toLowerCase();
  if (!q) return true;
  const haystack = [lead.name, lead.email, lead.phone, lead.requirementText, lead.summary, lead.businessName]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();
  return haystack.includes(q);
}

export default function LeadsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [leads, setLeads] = useState<Lead[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const search = searchParams.get('search') || '';
  const rawStatus = searchParams.get('status');
  const statusFilter =
    rawStatus && VALID_STATUSES.includes(rawStatus as LeadStatus) ? (rawStatus as LeadStatus) : null;
  const hasInvalidStatus = Boolean(rawStatus && !statusFilter);
  const [typedSearch, setTypedSearch] = useState(search);

  useEffect(() => {
    setTypedSearch(search);
  }, [search]);

  useEffect(() => {
    void loadLeads();
  }, []);

  const loadLeads = async () => {
    try {
      setLoading(true);
      const data = await leadsApi.getAllLeads();
      setLeads(data);
      setError(null);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load leads';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const applySearch = (raw: string) => {
    const trimmed = raw.trim().slice(0, 200);
    setSearchParams(
      (prev) => {
        const params = new URLSearchParams(prev);
        if (trimmed) {
          params.set('search', trimmed);
        } else {
          params.delete('search');
        }
        return params;
      },
      { replace: true }
    );
    setTypedSearch(trimmed);
  };

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const live = String(new FormData(event.currentTarget).get('lead-search') ?? '');
    applySearch(live);
  };

  const handleStatusFilter = (status: LeadStatus | null) => {
    setSearchParams(
      (prev) => {
        const params = new URLSearchParams(prev);
        if (status) {
          params.set('status', status);
        } else {
          params.delete('status');
        }
        return params;
      },
      { replace: true }
    );
  };

  const clearFilters = () => {
    setTypedSearch('');
    setSearchParams({}, { replace: true });
  };

  const filtersActive = Boolean(search || statusFilter || hasInvalidStatus);
  const filteredLeads = leads.filter((lead) => {
    if (statusFilter && lead.status !== statusFilter) return false;
    return leadMatchesSearch(lead, search);
  });

  return (
    <div>
      <PageHeader title="Leads" subtitle="Manage qualified leads from customer conversations" />

      <Card className="mb-6 p-4">
        <form className="flex flex-wrap gap-3" onSubmit={handleSearchSubmit} autoComplete="off">
          <div className="min-w-[200px] flex-1">
            <label htmlFor="lead-search" className="sr-only">
              Search leads
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate" size={18} />
              <input
                id="lead-search"
                type="text"
                name="lead-search"
                value={typedSearch}
                autoComplete="off"
                onChange={(e) => setTypedSearch(e.target.value.slice(0, 200))}
                placeholder="Search leads..."
                maxLength={200}
                className="input-dark w-full pl-10"
              />
            </div>
          </div>
          <label htmlFor="lead-status-filter" className="sr-only">
            Status
          </label>
          <select
            id="lead-status-filter"
            value={statusFilter || ''}
            onChange={(e) => handleStatusFilter((e.target.value as LeadStatus) || null)}
            className="input-dark w-44"
          >
            <option value="">All Status</option>
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <button type="submit" className="btn-secondary px-4" aria-label="Search leads">
            <Search size={18} />
          </button>
          {filtersActive && (
            <button type="button" onClick={clearFilters} className="btn-secondary px-4">
              <X size={18} /> Clear
            </button>
          )}
        </form>
      </Card>

      {error && (
        <ErrorBanner message={`Error: ${error}`} onRetry={loadLeads} onClear={filtersActive ? clearFilters : undefined} />
      )}

      {loading ? (
        <LoadingState label="Loading leads…" />
      ) : error ? null : filteredLeads.length === 0 ? (
        <EmptyState
          icon={<Users size={26} />}
          title={filtersActive ? 'No leads match these filters' : 'No leads found'}
          description={
            filtersActive
              ? 'Try a different search or status, or clear filters to see all leads.'
              : 'Leads will appear here when customers show buying intent'
          }
          action={
            filtersActive ? (
              <button type="button" className="btn-secondary" onClick={clearFilters}>
                Clear Filters
              </button>
            ) : undefined
          }
        />
      ) : (
        <div className="table-card">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="border-b border-frost bg-mist">
                <tr>
                  {['Name', 'Requirement', 'Score', 'Status', 'Created', 'Actions'].map((h) => (
                    <th
                      key={h}
                      className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate"
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filteredLeads.map((lead) => (
                  <tr
                    key={lead.id}
                    className="border-b border-frost transition-colors duration-200 hover:bg-mist"
                  >
                    <td className="whitespace-nowrap px-6 py-4">
                      <div className="text-sm font-medium text-ink">{lead.name}</div>
                      {lead.email && <div className="text-sm text-slate">{lead.email}</div>}
                      {lead.phone && <div className="text-sm text-slate">{lead.phone}</div>}
                    </td>
                    <td className="px-6 py-4">
                      <div className="max-w-xs truncate text-sm text-ink">{lead.requirementText || '-'}</div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <LeadScoreBadge score={lead.leadScore} />
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <LeadStatusBadge status={lead.status} />
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate">
                      {formatServerDate(lead.createdAt)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm">
                      <Link to={`/leads/${lead.id}`} className="font-medium text-ink hover:text-ink">
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
