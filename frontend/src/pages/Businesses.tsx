import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Plus, Search, Trash2, Edit, Globe, FileText, Building2 } from 'lucide-react';
import { businessApi, type BusinessDependencies } from '../api/business';
import type { ApiResponse, Business, PaginatedResponse } from '../types/index';
import PageHeader from '../components/ui/PageHeader';
import EmptyState from '../components/ui/EmptyState';
import LoadingState from '../components/ui/LoadingState';
import StatusBadge from '../components/ui/StatusBadge';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import ToastContainer from '../components/ui/ToastContainer';
import { useToast } from '../hooks/useToast';

/**
  * Businesses list page with CRUD operations.
  *
  * @version 0.3.0
  * Feature: F-002
  */
export default function Businesses() {
  const navigate = useNavigate();
  const location = useLocation();
  const { toasts, showToast, closeToast } = useToast();
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [typedSearch, setTypedSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [page, setPage] = useState(0);
  const [searchEpoch, setSearchEpoch] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [pendingDelete, setPendingDelete] = useState<{ id: string; name: string } | null>(null);
  const [dependencies, setDependencies] = useState<BusinessDependencies | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const fetchGeneration = useRef(0);

  const fetchBusinesses = async (currentPage: number, term: string) => {
    const generation = ++fetchGeneration.current;
    setLoading(true);
    setError(null);

    try {
      const response: ApiResponse<PaginatedResponse<Business>> = term
        ? await businessApi.searchBusinesses(term, { page: currentPage, size: 20 })
        : await businessApi.getAllBusinesses({
            page: currentPage,
            size: 20,
          });

      if (generation !== fetchGeneration.current) {
        return;
      }

      if (response.success && response.data) {
        setBusinesses(response.data.items);
        setTotalPages(response.data.pagination.totalPages);
        setTotalCount(response.data.pagination.total);
      } else {
        setError(response.error || 'Failed to load businesses');
      }
    } catch (err) {
      if (generation !== fetchGeneration.current) {
        return;
      }
      setError('Network error occurred');
    } finally {
      if (generation === fetchGeneration.current) {
        setLoading(false);
      }
    }
  };

  const applySearch = (raw: string) => {
    const term = raw.trim().slice(0, 200);
    setTypedSearch(term);
    setAppliedSearch(term);
    setPage(0);
    setSearchEpoch((n) => n + 1);
  };

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const live = String(new FormData(event.currentTarget).get('business-search') ?? '');
    applySearch(live);
  };

  const clearSearch = () => {
    applySearch('');
  };

  useEffect(() => {
    const toast = (location.state as { toast?: string } | null)?.toast;
    if (toast) {
      showToast('success', toast);
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location, navigate, showToast]);

  const requestDelete = async (id: string, name: string) => {
    setPendingDelete({ id, name });
    try {
      const response = await businessApi.getDependencies(id);
      setDependencies(response.success && response.data ? response.data : null);
    } catch {
      setDependencies(null);
    }
  };

  const handleDelete = async () => {
    if (!pendingDelete) return;
    setDeleteBusy(true);
    try {
      const response: ApiResponse<void> = await businessApi.deleteBusiness(pendingDelete.id);
      if (response.success) {
        showToast('success', 'Business deleted successfully');
        setPendingDelete(null);
        void fetchBusinesses(page, appliedSearch);
      } else {
        setError(response.error || 'Failed to delete business');
        setPendingDelete(null);
      }
    } catch (err) {
      setError('The business could not be deleted. No records were removed.');
      setPendingDelete(null);
    } finally {
      setDeleteBusy(false);
    }
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  useEffect(() => {
    void fetchBusinesses(page, appliedSearch);
  }, [page, appliedSearch, searchEpoch]);

  return (
    <div>
      <ToastContainer toasts={toasts} onClose={closeToast} />
      {pendingDelete && (
        <ConfirmDialog
          title="Delete this business?"
          confirmLabel="Delete business"
          danger
          busy={deleteBusy}
          onConfirm={handleDelete}
          onClose={() => setPendingDelete(null)}
        >
          <p>
            This permanently deletes <strong>{pendingDelete.name}</strong> and related records.
          </p>
          <ul className="mt-3 list-disc space-y-1 pl-5">
            <li>{dependencies?.leads ?? 0} leads</li>
            <li>{dependencies?.conversations ?? 0} conversations</li>
            <li>{dependencies?.documents ?? 0} documents</li>
            <li>{dependencies?.approvals ?? 0} approvals</li>
            <li>{dependencies?.agentLogs ?? 0} agent logs</li>
          </ul>
        </ConfirmDialog>
      )}
      <PageHeader
        title="Businesses"
        subtitle={`${totalCount} business${totalCount !== 1 ? 'es' : ''}`}
        action={
          <button onClick={() => navigate('/businesses/new')} className="btn-primary">
            <Plus size={18} />
            <span>Add Business</span>
          </button>
        }
      />

      {/* Search Bar */}
      <form className="mb-6 flex gap-3" onSubmit={handleSearchSubmit} autoComplete="off">
        <div className="relative flex-1">
          <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate" />
          <input
            ref={searchInputRef}
            type="text"
            name="business-search"
            placeholder="Search businesses…"
            value={typedSearch}
            maxLength={200}
            aria-label="Search businesses"
            autoComplete="off"
            onChange={(e) => setTypedSearch(e.target.value.slice(0, 200))}
            className="input-dark pl-10"
          />
        </div>
        <button type="submit" className="btn-secondary">
          Search
        </button>
        {(typedSearch || appliedSearch) && (
          <button type="button" onClick={clearSearch} className="btn-secondary">
            Clear
          </button>
        )}
      </form>

      {loading && <LoadingState label="Loading businesses…" />}

      {error && (
        <div className="mb-6 rounded-sm border border-frost bg-mist p-4 text-ink">
          {error}
        </div>
      )}

      {!loading && !error && businesses.length === 0 && (
        <EmptyState
          icon={<Building2 size={26} />}
          title="No businesses found"
          description={appliedSearch ? 'Try a different search term' : 'Add your first business to get started'}
        />
      )}

      {!loading && !error && businesses.length > 0 && (
        <div className="table-card">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="border-b border-frost bg-mist">
                <tr>
                  {['Name', 'Website', 'Industry', 'Contact', 'Crawl Status', 'Actions'].map((h) => (
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
                {businesses.map((business) => (
                  <tr
                    key={business.id}
                    className="border-b border-frost transition-colors duration-200 hover:bg-mist"
                  >
                    <td className="px-6 py-4">
                      <Link
                        to={`/businesses/${business.id}`}
                        className="font-medium text-ink hover:text-ink"
                      >
                        {business.name}
                      </Link>
                    </td>
                    <td className="px-6 py-4">
                      <a
                        href={business.websiteUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-1 text-ink hover:underline"
                      >
                        <Globe size={14} />
                        <span className="max-w-xs truncate">{business.websiteUrl}</span>
                      </a>
                    </td>
                    <td className="px-6 py-4 text-ink">{business.industry || '-'}</td>
                    <td className="px-6 py-4 text-sm">
                      {business.contactEmail && <div className="text-ink">{business.contactEmail}</div>}
                      {business.contactPhone && <div className="text-slate">{business.contactPhone}</div>}
                      {!business.contactEmail && !business.contactPhone && (
                        <span className="text-silver">-</span>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge status={business.crawlStatus} />
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <Link
                          to={`/businesses/${business.id}`}
                          className="text-slate transition-colors hover:text-ink"
                          title="View details"
                        >
                          <FileText size={18} />
                        </Link>
                        <button
                          onClick={() => navigate(`/businesses/${business.id}/edit`)}
                          className="text-slate transition-colors hover:text-ink"
                          title="Edit"
                        >
                          <Edit size={18} />
                        </button>
                        <button
                          onClick={() => requestDelete(business.id, business.name)}
                          className="text-slate transition-colors hover:text-ink"
                          title="Delete"
                        >
                          <Trash2 size={18} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-frost px-6 py-4">
              <button
                onClick={() => handlePageChange(Math.max(0, page - 1))}
                disabled={page === 0}
                className="btn-secondary"
              >
                Previous
              </button>
              <span className="text-sm text-slate">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => handlePageChange(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="btn-secondary"
              >
                Next
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
