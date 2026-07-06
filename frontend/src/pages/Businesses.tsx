import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Search, Trash2, Edit, Globe, FileText, Building2 } from 'lucide-react';
import { businessApi } from '../api/business';
import type { ApiResponse, Business, PaginatedResponse } from '../types/index';
import PageHeader from '../components/ui/PageHeader';
import EmptyState from '../components/ui/EmptyState';
import LoadingState from '../components/ui/LoadingState';
import StatusBadge from '../components/ui/StatusBadge';

/**
 * Businesses list page with CRUD operations.
 *
 * @version 0.3.0
 * Feature: F-002
 */
export default function Businesses() {
  const navigate = useNavigate();
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalCount, setTotalCount] = useState(0);

  const fetchBusinesses = async () => {
    setLoading(true);
    setError(null);

    try {
      const response: ApiResponse<PaginatedResponse<Business>> = await businessApi.getAllBusinesses({
        page,
        size: 20,
      });

      if (response.success && response.data) {
        setBusinesses(response.data.items);
        setTotalPages(response.data.pagination.totalPages);
        setTotalCount(response.data.pagination.total);
      } else {
        setError(response.error || 'Failed to load businesses');
      }
    } catch (err) {
      setError('Network error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!searchTerm.trim()) {
      fetchBusinesses();
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response: ApiResponse<PaginatedResponse<Business>> = await businessApi.searchBusinesses(
        searchTerm,
        { page: 0, size: 20 }
      );

      if (response.success && response.data) {
        setBusinesses(response.data.items);
        setTotalPages(response.data.pagination.totalPages);
        setTotalCount(response.data.pagination.total);
        setPage(0);
      } else {
        setError(response.error || 'Search failed');
      }
    } catch (err) {
      setError('Network error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this business? This will also delete all related data.')) {
      return;
    }

    try {
      const response: ApiResponse<void> = await businessApi.deleteBusiness(id);
      if (response.success) {
        fetchBusinesses();
      } else {
        setError(response.error || 'Failed to delete business');
      }
    } catch (err) {
      setError('Network error occurred');
    }
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  useEffect(() => {
    fetchBusinesses();
  }, [page]);

  return (
    <div>
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
      <div className="mb-6 flex gap-3">
        <div className="relative flex-1">
          <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
          <input
            type="text"
            placeholder="Search businesses…"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            className="input-dark pl-10"
          />
        </div>
        <button onClick={handleSearch} className="btn-secondary">
          Search
        </button>
      </div>

      {loading && <LoadingState label="Loading businesses…" />}

      {error && (
        <div className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">
          {error}
        </div>
      )}

      {!loading && !error && businesses.length === 0 && (
        <EmptyState
          icon={<Building2 size={26} />}
          title="No businesses found"
          description={searchTerm ? 'Try a different search term' : 'Add your first business to get started'}
        />
      )}

      {!loading && !error && businesses.length > 0 && (
        <div className="table-card">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="border-b border-white/[0.06] bg-white/[0.02]">
                <tr>
                  {['Name', 'Website', 'Industry', 'Contact', 'Crawl Status', 'Actions'].map((h) => (
                    <th
                      key={h}
                      className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-zinc-400"
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
                    className="border-b border-white/[0.04] transition-colors duration-200 hover:bg-white/[0.03]"
                  >
                    <td className="px-6 py-4">
                      <Link
                        to={`/businesses/${business.id}`}
                        className="font-medium text-primary-300 hover:text-primary-200"
                      >
                        {business.name}
                      </Link>
                    </td>
                    <td className="px-6 py-4">
                      <a
                        href={business.websiteUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-1 text-blue-300 hover:underline"
                      >
                        <Globe size={14} />
                        <span className="max-w-xs truncate">{business.websiteUrl}</span>
                      </a>
                    </td>
                    <td className="px-6 py-4 text-zinc-300">{business.industry || '-'}</td>
                    <td className="px-6 py-4 text-sm">
                      {business.contactEmail && <div className="text-zinc-300">{business.contactEmail}</div>}
                      {business.contactPhone && <div className="text-zinc-500">{business.contactPhone}</div>}
                      {!business.contactEmail && !business.contactPhone && (
                        <span className="text-zinc-600">-</span>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge status={business.crawlStatus} />
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <Link
                          to={`/businesses/${business.id}`}
                          className="text-zinc-500 transition-colors hover:text-primary-300"
                          title="View details"
                        >
                          <FileText size={18} />
                        </Link>
                        <button
                          onClick={() => navigate(`/businesses/${business.id}/edit`)}
                          className="text-zinc-500 transition-colors hover:text-primary-300"
                          title="Edit"
                        >
                          <Edit size={18} />
                        </button>
                        <button
                          onClick={() => handleDelete(business.id)}
                          className="text-zinc-500 transition-colors hover:text-red-400"
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
            <div className="flex items-center justify-between border-t border-white/[0.06] px-6 py-4">
              <button
                onClick={() => handlePageChange(Math.max(0, page - 1))}
                disabled={page === 0}
                className="btn-secondary"
              >
                Previous
              </button>
              <span className="text-sm text-zinc-400">
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
