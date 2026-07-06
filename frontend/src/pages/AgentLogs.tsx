import { useEffect, useState } from 'react';
import { FileText, RefreshCw, X, AlertCircle } from 'lucide-react';
import { agentLogsApi, AgentLogsFilters } from '../api/agentLogsApi';
import { AgentLog, AgentLogSummary, AgentActionStatus } from '../types/agentLog';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import EmptyState from '../components/ui/EmptyState';
import LoadingState from '../components/ui/LoadingState';
import StatusBadge from '../components/ui/StatusBadge';
import ExecutionDetailsModal from '../components/agent-logs/ExecutionDetailsModal';

/**
 * Agent logs audit page - complete observability for AI agent executions.
 *
 * @version 0.3.0
 * Feature: F-012 - Agent Logs Observability
 */
export default function AgentLogs() {
  const [logs, setLogs] = useState<AgentLog[]>([]);
  const [summary, setSummary] = useState<AgentLogSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedLog, setSelectedLog] = useState<AgentLog | null>(null);

  // Filters
  const [search, setSearch] = useState('');
  const [agentFilter, setAgentFilter] = useState<string>('ALL');
  const [actionFilter, setActionFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<AgentActionStatus | 'ALL'>('ALL');
  const [showErrorsOnly, setShowErrorsOnly] = useState(false);

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalLogs, setTotalLogs] = useState(0);
  const pageSize = 20;

  useEffect(() => {
    loadSummary();
  }, []);

  useEffect(() => {
    loadLogs();
  }, [page, search, agentFilter, actionFilter, statusFilter, showErrorsOnly]);

  const loadSummary = async () => {
    try {
      setSummaryLoading(true);
      const data = await agentLogsApi.getSummary();
      setSummary(data);
    } catch (err: any) {
      console.error('Error loading summary:', err);
      // Don't block the page if summary fails
    } finally {
      setSummaryLoading(false);
    }
  };

  const loadLogs = async () => {
    try {
      setLoading(true);
      setError(null);

      const filters: AgentLogsFilters = {
        page,
        size: pageSize,
        sort: 'createdAt,desc',
      };

      if (search) filters.search = search;
      if (agentFilter !== 'ALL') filters.agentName = agentFilter;
      if (actionFilter !== 'ALL') filters.action = actionFilter;
      if (showErrorsOnly) {
        filters.status = AgentActionStatus.ERROR;
      } else if (statusFilter !== 'ALL') {
        filters.status = statusFilter;
      }

      const response = await agentLogsApi.getAllLogs(filters);
      setLogs(response.items);
      setTotalPages(response.totalPages);
      setTotalLogs(response.total);
    } catch (err: any) {
      console.error('Error loading logs:', err);
      setError(err.message || 'Failed to load agent logs');
      setLogs([]);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    loadSummary();
    loadLogs();
  };

  const handleClearFilters = () => {
    setSearch('');
    setAgentFilter('ALL');
    setActionFilter('ALL');
    setStatusFilter('ALL');
    setShowErrorsOnly(false);
    setPage(0);
  };

  const hasActiveFilters = search || agentFilter !== 'ALL' || actionFilter !== 'ALL' || statusFilter !== 'ALL' || showErrorsOnly;

  const formatActionLabel = (action: string) => {
    return action
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  };

  const formatDuration = (ms?: number) => {
    if (!ms) return '-';
    if (ms < 1000) return `${ms}ms`;
    if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
    return `${(ms / 60000).toFixed(1)}m`;
  };

  const formatTimeAgo = (timestamp: string) => {
    const now = new Date();
    const past = new Date(timestamp);
    const diffMs = now.getTime() - past.getTime();
    const diffSecs = Math.floor(diffMs / 1000);
    const diffMins = Math.floor(diffSecs / 60);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffSecs < 60) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return past.toLocaleDateString();
  };

  const getRelatedItem = (log: AgentLog) => {
    if (log.businessName) return { type: 'Business', name: log.businessName };
    if (log.leadName) return { type: 'Lead', name: log.leadName };
    if (log.conversationId) return { type: 'Conversation', name: log.conversationId.slice(0, 8) };
    return null;
  };

  if (loading && page === 0) {
    return <LoadingState label="Loading agent logs…" />;
  }

  return (
    <div>
      <PageHeader
        title="Agent Logs"
        subtitle="Complete audit trail of AI agent executions"
        action={
          <button onClick={handleRefresh} className="btn-primary flex items-center gap-2">
            <RefreshCw size={16} />
            Refresh
          </button>
        }
      />

      {/* Summary Cards */}
      {summary && (
        <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Card className="p-4">
            <p className="text-xs font-medium uppercase tracking-wider text-zinc-500">Executions Today</p>
            <p className="mt-2 text-2xl font-bold text-white">
              {summaryLoading ? '...' : summary.executionsToday.toLocaleString()}
            </p>
          </Card>
          <Card className="p-4">
            <p className="text-xs font-medium uppercase tracking-wider text-zinc-500">Success Rate</p>
            <p className="mt-2 text-2xl font-bold text-green-400">
              {summaryLoading ? '...' : `${summary.successRate.toFixed(1)}%`}
            </p>
          </Card>
          <Card className="p-4">
            <p className="text-xs font-medium uppercase tracking-wider text-zinc-500">Errors</p>
            <p className="mt-2 text-2xl font-bold text-red-400">
              {summaryLoading ? '...' : summary.errorCount.toLocaleString()}
            </p>
          </Card>
          <Card className="p-4">
            <p className="text-xs font-medium uppercase tracking-wider text-zinc-500">Average Duration</p>
            <p className="mt-2 text-2xl font-bold text-cyan-400">
              {summaryLoading ? '...' : formatDuration(summary.averageDurationMs)}
            </p>
          </Card>
        </div>
      )}

      {/* Filter Bar */}
      <Card className="mb-6 p-4">
        <div className="space-y-4">
          {/* Search */}
          <div>
            <label className="label-dark">Search</label>
            <input
              type="text"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              placeholder="Search by execution ID, agent, or action..."
              className="input-dark w-full"
            />
          </div>

          {/* Filters Row */}
          <div className="flex flex-wrap gap-4">
            <div>
              <label className="label-dark">Agent</label>
              <select
                value={agentFilter}
                onChange={(e) => {
                  setAgentFilter(e.target.value);
                  setPage(0);
                }}
                className="input-dark w-48"
              >
                <option value="ALL">All Agents</option>
                <option value="RagSearch">RagSearch</option>
                <option value="RagAnswer">RagAnswer</option>
                <option value="SupportChatAgent">SupportChatAgent</option>
                <option value="LeadQualificationAgent">LeadQualificationAgent</option>
                <option value="EvaluationAgent">EvaluationAgent</option>
                <option value="FollowUpAgent">FollowUpAgent</option>
                <option value="KnowledgeBaseBuilder">KnowledgeBaseBuilder</option>
                <option value="Crawler">Crawler</option>
                <option value="ApifyLeadFinder">ApifyLeadFinder</option>
              </select>
            </div>

            <div>
              <label className="label-dark">Action</label>
              <select
                value={actionFilter}
                onChange={(e) => {
                  setActionFilter(e.target.value);
                  setPage(0);
                }}
                className="input-dark w-48"
              >
                <option value="ALL">All Actions</option>
                <option value="SEARCH_COMPLETED">Search Completed</option>
                <option value="ANSWER_GENERATED">Answer Generated</option>
                <option value="CHAT_RESPONSE">Chat Response</option>
                <option value="LEAD_QUALIFIED">Lead Qualified</option>
                <option value="BUILD_KB_STARTED">Build KB Started</option>
                <option value="BUILD_KB_COMPLETED">Build KB Completed</option>
                <option value="CRAWL_STARTED">Crawl Started</option>
                <option value="CRAWL_COMPLETED">Crawl Completed</option>
              </select>
            </div>

            <div>
              <label className="label-dark">Status</label>
              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value as AgentActionStatus | 'ALL');
                  setShowErrorsOnly(false);
                  setPage(0);
                }}
                className="input-dark w-44"
                disabled={showErrorsOnly}
              >
                <option value="ALL">All Statuses</option>
                <option value={AgentActionStatus.SUCCESS}>Success</option>
                <option value={AgentActionStatus.PARTIAL}>Partial</option>
                <option value={AgentActionStatus.FALLBACK_USED}>Fallback Used</option>
                <option value={AgentActionStatus.ERROR}>Error</option>
                <option value={AgentActionStatus.FAILED}>Failed</option>
              </select>
            </div>

            <div className="flex items-end">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={showErrorsOnly}
                  onChange={(e) => {
                    setShowErrorsOnly(e.target.checked);
                    if (e.target.checked) {
                      setStatusFilter('ALL');
                    }
                    setPage(0);
                  }}
                  className="rounded border-white/20 bg-black/30 text-red-500 focus:ring-2 focus:ring-red-500 focus:ring-offset-0"
                />
                <span className="text-sm text-zinc-300">Errors only</span>
              </label>
            </div>
          </div>

          {/* Clear Filters */}
          {hasActiveFilters && (
            <button
              onClick={handleClearFilters}
              className="flex items-center gap-2 text-sm text-zinc-400 hover:text-white transition-colors"
            >
              <X size={14} />
              Clear filters
            </button>
          )}
        </div>
      </Card>

      {/* Error Display */}
      {error && (
        <div className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 p-4 flex items-start gap-3">
          <AlertCircle size={20} className="text-red-400 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="text-red-300">{error}</p>
            <button onClick={loadLogs} className="mt-2 text-sm text-red-400 hover:text-red-300 underline">
              Retry
            </button>
          </div>
        </div>
      )}

      {/* Table or Empty State */}
      {!loading && logs.length === 0 ? (
        <EmptyState
          icon={<FileText size={26} />}
          title={hasActiveFilters ? 'No results found' : 'No agent logs recorded yet'}
          description={
            hasActiveFilters
              ? 'Try adjusting your filters or search query.'
              : 'Agent actions will be logged here for complete transparency.'
          }
        />
      ) : (
        <>
          <div className="table-card">
            <div className="overflow-x-auto">
              <table className="min-w-full">
                <thead className="border-b border-white/[0.06] bg-white/[0.02]">
                  <tr>
                    {['Time', 'Agent', 'Action', 'Related Item', 'Status', 'Duration', ''].map((h, i) => (
                      <th
                        key={h || `col-${i}`}
                        className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-zinc-400"
                      >
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {logs.map((log) => {
                    const relatedItem = getRelatedItem(log);
                    return (
                      <tr
                        key={log.id}
                        className="border-b border-white/[0.04] transition-colors duration-200 hover:bg-white/[0.03]"
                      >
                        <td className="whitespace-nowrap px-6 py-4 text-sm text-zinc-400">
                          {formatTimeAgo(log.createdAt)}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-zinc-200">
                          {log.agentName}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm text-zinc-300">
                          {formatActionLabel(log.action)}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm text-zinc-400">
                          {relatedItem ? (
                            <span title={relatedItem.name}>
                              <span className="text-zinc-500">{relatedItem.type}:</span> {relatedItem.name}
                            </span>
                          ) : (
                            <span className="text-zinc-600">-</span>
                          )}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4">
                          <StatusBadge status={log.status} />
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm text-zinc-400">
                          {formatDuration(log.durationMs)}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm">
                          <button
                            onClick={() => setSelectedLog(log)}
                            className="rounded-lg border border-white/10 px-3 py-1.5 text-xs font-medium text-indigo-300 transition-colors hover:bg-indigo-500/10"
                          >
                            View
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-6 flex items-center justify-between">
              <p className="text-sm text-zinc-400">
                Showing {page * pageSize + 1}-{Math.min((page + 1) * pageSize, totalLogs)} of {totalLogs} logs
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Execution Details Modal */}
      {selectedLog && <ExecutionDetailsModal log={selectedLog} onClose={() => setSelectedLog(null)} />}
    </div>
  );
}
