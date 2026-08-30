import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { MessageSquare, RefreshCw, Search, X, ArrowLeft } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import LoadingState from '../components/ui/LoadingState';
import EmptyState from '../components/ui/EmptyState';
import ChannelBadge from '../components/conversations/ChannelBadge';
import StatusBadge from '../components/ui/StatusBadge';
import ErrorBanner from '../components/ui/ErrorBanner';
import ToastContainer from '../components/ui/ToastContainer';
import { useToast } from '../hooks/useToast';
import { conversationsApi } from '../api/conversationsApi';
import { ConversationStatus } from '../types/conversation';
import type {
  ConversationListItem,
  ConversationDetail,
  ConversationMessage,
  ConversationSummary,
} from '../types/conversation';

/**
  * Conversations admin page - Intercom-style operational inbox.
  *
  * @version 0.3.0
  * Feature: F-009 - Conversations Admin Page
  */
export default function Conversations() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { toasts, showToast, closeToast } = useToast();

  // State
  const [conversations, setConversations] = useState<ConversationListItem[]>([]);
  const [selectedConversation, setSelectedConversation] = useState<ConversationDetail | null>(null);
  const [messages, setMessages] = useState<ConversationMessage[]>([]);
  const [summary, setSummary] = useState<ConversationSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [updatingStatus, setUpdatingStatus] = useState(false);

  const VALID_STATUSES: ConversationStatus[] = [
    ConversationStatus.ACTIVE,
    ConversationStatus.PAUSED,
    ConversationStatus.CLOSED,
    ConversationStatus.ARCHIVED,
  ];

  // Filters from URL
  const search = searchParams.get('search') || '';
  const rawStatus = searchParams.get('status');
  const statusFilter =
    rawStatus && VALID_STATUSES.includes(rawStatus as ConversationStatus)
      ? (rawStatus as ConversationStatus)
      : null;
  const hasInvalidStatus = Boolean(rawStatus && !statusFilter);
  const [typedSearch, setTypedSearch] = useState(search);
  const [listPage, setListPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [totalElements, setTotalElements] = useState(0);
  const [loadingMore, setLoadingMore] = useState(false);
  const PAGE_SIZE = 20;
  const searchInputRef = useRef<HTMLInputElement>(null);
  const fetchGeneration = useRef(0);

  // Mobile view state
  const [showDetail, setShowDetail] = useState(false);

  useEffect(() => {
    setTypedSearch(search);
  }, [search]);

  useEffect(() => {
    loadSummary();
    void loadConversations(false, 0, false, {
      search: search || null,
      status: statusFilter,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  const loadSummary = async () => {
    try {
      const data = await conversationsApi.getConversationSummary();
      setSummary(data);
    } catch (err: any) {
      console.error('Failed to load summary:', err);
    }
  };

  const loadConversations = async (
    silent = false,
    nextPage = 0,
    append = false,
    overrides?: { search?: string | null; status?: ConversationStatus | null }
  ) => {
    const generation = append ? fetchGeneration.current : ++fetchGeneration.current;
    try {
      if (append) setLoadingMore(true);
      else if (!silent) {
        setLoading(true);
        setConversations([]);
      }
      setError(null);
      const requestedStatus =
        overrides && 'status' in overrides ? overrides.status : searchParams.get('status');
      const requestedSearch =
        overrides && 'search' in overrides ? overrides.search : searchParams.get('search');
      const trimmedSearch = requestedSearch?.trim() || undefined;
      const filters = {
        search: trimmedSearch,
        status:
          requestedStatus && VALID_STATUSES.includes(requestedStatus as ConversationStatus)
            ? (requestedStatus as ConversationStatus)
            : undefined,
        page: nextPage,
        size: PAGE_SIZE,
      };
      const response = await conversationsApi.getAllConversations(filters);
      if (generation !== fetchGeneration.current) {
        return;
      }
      setConversations((prev) => (append ? [...prev, ...response.items] : response.items));
      setListPage(response.pagination.page);
      setTotalElements(response.pagination.totalElements);
      setHasMore(response.pagination.page + 1 < response.pagination.totalPages);
    } catch (err: any) {
      if (generation !== fetchGeneration.current) {
        return;
      }
      setError(err.message || 'Failed to load conversations');
      if (!append) setConversations([]);
    } finally {
      if (generation === fetchGeneration.current) {
        setLoading(false);
        setLoadingMore(false);
      }
    }
  };

  const dismissPanel = () => {
    setSelectedConversation(null);
    setShowDetail(false);
    setMessages([]);
  };

  const loadConversationDetail = async (id: string) => {
    try {
      const detail = await conversationsApi.getConversationDetails(id);
      setSelectedConversation(detail);
      setShowDetail(true);
      loadMessages(id);
    } catch (err: any) {
      console.error('Failed to load conversation detail:', err);
      setSelectedConversation(null);
    }
  };

  const loadMessages = async (id: string) => {
    try {
      setLoadingMessages(true);
      const response = await conversationsApi.getConversationMessages(id, 0, 100);
      setMessages(response.items);
    } catch (err: any) {
      console.error('Failed to load messages:', err);
      setMessages([]);
    } finally {
      setLoadingMessages(false);
    }
  };

  const updateStatus = async (newStatus: ConversationStatus) => {
    if (!selectedConversation) return;
    try {
      setUpdatingStatus(true);
      const updated = await conversationsApi.updateConversationStatus(selectedConversation.id, { status: newStatus });
      setSelectedConversation(updated);
      // Refresh list
      loadConversations(true);
      loadSummary();
    } catch (err: any) {
      showToast('error', err.message || 'Failed to update status');
    } finally {
      setUpdatingStatus(false);
    }
  };

  const applySearch = (raw: string) => {
    const trimmed = raw.trim().slice(0, 200);
    setListPage(0);
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
    void loadConversations(false, 0, false, {
      search: trimmed || null,
      status: statusFilter,
    });
  };

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const live = String(new FormData(event.currentTarget).get('conversation-search') ?? '');
    applySearch(live);
  };

  const handleStatusFilter = (status: ConversationStatus | null) => {
    const params = new URLSearchParams(searchParams);
    if (status) {
      params.set('status', status);
    } else {
      params.delete('status');
    }
    setSearchParams(params);
  };

  const clearFilters = () => {
    setListPage(0);
    setTypedSearch('');
    setSearchParams({}, { replace: true });
    void loadConversations(false, 0, false, { search: null, status: null });
  };

  const formatRelativeTime = (dateStr?: string): string => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  const filtersActive = Boolean(search || statusFilter || hasInvalidStatus);

  if (loading && !summary && !error) {
    return <LoadingState label="Loading conversations…" />;
  }

  return (
    <>
      <ToastContainer toasts={toasts} onClose={closeToast} />
      <div className="h-full flex flex-col">
      <PageHeader title="Conversations" subtitle="Monitor customer conversations handled by your AI support agent" />

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
          <Card className="p-4">
            <div className="text-sm text-slate">Total</div>
            <div className="text-2xl font-semibold text-ink">{summary.totalConversations}</div>
          </Card>
          <Card className="p-4">
            <div className="text-sm text-slate">Active</div>
            <div className="text-2xl font-semibold text-ink">{summary.activeConversations}</div>
          </Card>
          <Card className="p-4">
            <div className="text-sm text-slate">Today</div>
            <div className="text-2xl font-semibold font-serif text-ink">{summary.conversationsToday}</div>
          </Card>
          <Card className="p-4">
            <div className="text-sm text-slate">Leads Captured</div>
            <div className="text-2xl font-semibold font-serif text-ink">{summary.leadsCaptured}</div>
          </Card>
          <Card className="p-4">
            <div className="text-sm text-slate">Avg Messages</div>
            <div className="text-2xl font-semibold text-ink">{summary.averageMessagesPerConversation.toFixed(1)}</div>
          </Card>
        </div>
      )}

      {/* Filters */}
      <Card className="p-4 mb-6">
        <form className="flex flex-wrap gap-3" onSubmit={handleSearchSubmit} autoComplete="off">
          <div className="flex-1 min-w-[200px]">
            <label htmlFor="conversation-search" className="sr-only">
              Search conversations
            </label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate" size={18} />
              <input
                id="conversation-search"
                ref={searchInputRef}
                type="text"
                name="conversation-search"
                value={typedSearch}
                autoComplete="off"
                onChange={(e) => setTypedSearch(e.target.value.slice(0, 200))}
                placeholder="Search conversations..."
                maxLength={200}
                className="input-dark pl-10 w-full"
              />
            </div>
          </div>
          <label htmlFor="conversation-status-filter" className="sr-only">
            Status
          </label>
          <select
            id="conversation-status-filter"
            value={statusFilter || ''}
            onChange={(e) => handleStatusFilter(e.target.value as ConversationStatus || null)}
            className="input-dark w-36"
          >
            <option value="">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="PAUSED">Paused</option>
            <option value="CLOSED">Closed</option>
            <option value="ARCHIVED">Archived</option>
          </select>
          <button type="submit" className="btn-secondary px-4" aria-label="Search conversations">
            <Search size={18} />
          </button>
          <button
            type="button"
            onClick={() => {
              loadConversations();
              loadSummary();
            }}
            className="btn-secondary px-4"
            aria-label="Refresh conversations"
          >
            <RefreshCw size={18} />
          </button>
          {filtersActive && (
            <button type="button" onClick={clearFilters} className="btn-secondary px-4">
              <X size={18} /> Clear
            </button>
          )}
        </form>
      </Card>

      {error && (
        <ErrorBanner
          message={error}
          onRetry={() => {
            loadConversations();
            loadSummary();
          }}
          onClear={filtersActive ? clearFilters : undefined}
        />
      )}

      {/* Main Content */}
      {error ? null : loading && conversations.length === 0 ? (
        <LoadingState label="Loading conversations…" />
      ) : conversations.length === 0 ? (
        <EmptyState
          icon={<MessageSquare size={26} />}
          title={filtersActive ? 'No conversations match these filters' : 'No conversations yet'}
          description={filtersActive ? 'Try adjusting your filters.' : 'Conversations will appear here when customers interact with your AI support agent.'}
        />
      ) : (
        <div className="flex-1 overflow-hidden">
          <Card className="h-full flex">
            {/* Conversation List - Desktop: Left Panel, Mobile: Full width when detail hidden */}
            <div className={`${showDetail ? 'hidden md:block' : 'block'} w-full md:w-96 border-r border-frost overflow-y-auto`}>
              {totalElements > 0 && (
                <div className="px-4 py-2 text-xs text-slate border-b border-frost">
                  Showing {conversations.length} of {totalElements}
                </div>
              )}
              {conversations.map((conv) => (
                <button
                  key={conv.id}
                  onClick={() => loadConversationDetail(conv.id)}
                  className={`w-full text-left p-4 border-b border-frost hover:bg-mist transition-colors ${
                    selectedConversation?.id === conv.id ? 'bg-mist' : ''
                  }`}
                >
                  <div className="flex items-start justify-between mb-2">
                    <div className="font-medium text-ink">{conv.customerName || 'Anonymous'}</div>
                    <div className="text-xs text-slate">{formatRelativeTime(conv.latestMessageAt)}</div>
                  </div>
                  {conv.customerEmail && <div className="text-sm text-slate mb-2">{conv.customerEmail}</div>}
                  <div className="flex items-center gap-2 mb-2 flex-wrap">
                    <StatusBadge status={conv.status} />
                    <div className="text-xs text-silver">{conv.businessName}</div>
                  </div>
                  {conv.latestMessagePreview && (
                    <div className="text-sm text-slate truncate">{conv.latestMessagePreview}</div>
                  )}
                  <div className="flex items-center gap-3 mt-2 text-xs text-slate">
                    <span>{conv.messageCount} messages</span>
                    {conv.leadCount > 0 && <span className="text-ink">{conv.leadCount} lead(s)</span>}
                  </div>
                </button>
              ))}
              {hasMore && (
                <div className="p-4">
                  <button
                    type="button"
                    onClick={() => loadConversations(true, listPage + 1, true)}
                    disabled={loadingMore}
                    className="btn-secondary w-full"
                  >
                    {loadingMore ? 'Loading…' : 'Load more'}
                  </button>
                </div>
              )}
            </div>

            {/* Conversation Detail - Desktop: Right Panel, Mobile: Full width when shown */}
            <div
              role={selectedConversation ? 'dialog' : undefined}
              aria-modal={selectedConversation ? false : undefined}
              aria-labelledby={selectedConversation ? 'conversation-detail-title' : undefined}
              className={`${showDetail ? 'block' : 'hidden md:block'} flex-1 flex flex-col`}
            >
              {!selectedConversation ? (
                <div className="flex-1 flex items-center justify-center text-slate">
                  <div className="text-center">
                    <MessageSquare size={48} className="mx-auto mb-4 opacity-20" />
                    <div>Select a conversation to view details</div>
                  </div>
                </div>
              ) : (
                <>
                  {/* Header */}
                  <div className="p-4 border-b border-frost">
                    <div className="flex items-start justify-between mb-3">
                      <div>
                        <div className="flex items-center gap-2 mb-1">
                          <button
                            type="button"
                            onClick={dismissPanel}
                            className="md:hidden mr-2"
                            aria-label="Back to conversation list"
                          >
                            <ArrowLeft size={20} />
                          </button>
                          <h3 id="conversation-detail-title" className="text-lg font-semibold">
                            {selectedConversation.customerName || 'Anonymous'}
                          </h3>
                        </div>
                        {selectedConversation.customerEmail && (
                          <div className="text-sm text-slate">{selectedConversation.customerEmail}</div>
                        )}
                        {selectedConversation.customerPhone && (
                          <div className="text-sm text-slate">{selectedConversation.customerPhone}</div>
                        )}
                      </div>
                      <div className="flex flex-col items-end gap-2">
                        <StatusBadge status={selectedConversation.status} />
                        <ChannelBadge channel={selectedConversation.channel} />
                      </div>
                    </div>
                    <div className="text-sm text-slate mb-3">{selectedConversation.businessName}</div>
                    {/* Status Actions */}
                    <div className="flex gap-2 flex-wrap">
                      <button
                        type="button"
                        onClick={dismissPanel}
                        className="btn-secondary px-3 py-1 text-sm"
                      >
                        Dismiss
                      </button>
                      {selectedConversation.status === 'ACTIVE' && (
                        <>
                          <button onClick={() => updateStatus('PAUSED' as ConversationStatus)} className="btn-secondary px-3 py-1 text-sm" disabled={updatingStatus}>
                            Pause
                          </button>
                          <button onClick={() => updateStatus('CLOSED' as ConversationStatus)} className="btn-secondary px-3 py-1 text-sm" disabled={updatingStatus}>
                            Mark closed
                          </button>
                        </>
                      )}
                      {selectedConversation.status === 'PAUSED' && (
                        <>
                          <button onClick={() => updateStatus('ACTIVE' as ConversationStatus)} className="btn-secondary px-3 py-1 text-sm" disabled={updatingStatus}>
                            Reopen
                          </button>
                          <button onClick={() => updateStatus('CLOSED' as ConversationStatus)} className="btn-secondary px-3 py-1 text-sm" disabled={updatingStatus}>
                            Mark closed
                          </button>
                        </>
                      )}
                      {selectedConversation.status === 'CLOSED' && (
                        <>
                          <button onClick={() => updateStatus('ACTIVE' as ConversationStatus)} className="btn-secondary px-3 py-1 text-sm" disabled={updatingStatus}>
                            Reopen
                          </button>
                          <button onClick={() => updateStatus('ARCHIVED' as ConversationStatus)} className="btn-secondary px-3 py-1 text-sm" disabled={updatingStatus}>
                            Archive
                          </button>
                        </>
                      )}
                      {selectedConversation.status === 'ARCHIVED' && (
                        <button onClick={() => updateStatus('ACTIVE' as ConversationStatus)} className="btn-secondary px-3 py-1 text-sm" disabled={updatingStatus}>
                          Restore
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Messages */}
                  <div className="flex-1 overflow-y-auto p-4 space-y-4">
                    {loadingMessages ? (
                      <div className="text-center text-slate">Loading messages...</div>
                    ) : messages.length === 0 ? (
                      <div className="text-center text-slate">No messages yet</div>
                    ) : (
                      messages.map((msg) => (
                        <div
                          key={msg.id}
                          className={`flex ${msg.role === 'USER' ? 'justify-end' : msg.role === 'SYSTEM' ? 'justify-center' : 'justify-start'}`}
                        >
                          <div
                            className={`max-w-[80%] rounded-lg p-3 ${
                              msg.role === 'USER'
                                ? 'bg-blue-600/20 text-ink'
                                : msg.role === 'SYSTEM'
                                ? 'bg-mist/50 text-slate text-sm'
                                : 'bg-mist text-ink'
                            }`}
                          >
                            <div className="text-xs opacity-70 mb-1">{msg.role}</div>
                            <div className="whitespace-pre-wrap break-words">{msg.content}</div>
                            <div className="text-xs opacity-50 mt-1">
                              {new Date(msg.createdAt).toLocaleString()}
                            </div>
                          </div>
                        </div>
                      ))
                    )}
                  </div>

                  {/* Footer Info */}
                  {selectedConversation.leadCaptureStatus && (
                    <div className="p-4 border-t border-frost bg-mist">
                      <div className="flex items-center gap-2 text-sm">
                        <span className="text-slate">Lead Capture:</span>
                        <StatusBadge status={selectedConversation.leadCaptureStatus} />
                      </div>
                      {selectedConversation.relatedLeads.length > 0 && (
                        <div className="mt-2 text-sm text-slate">
                          {selectedConversation.relatedLeads.length} related lead(s)
                        </div>
                      )}
                    </div>
                  )}
                </>
              )}
            </div>
          </Card>
        </div>
      )}
      </div>
    </>
  );
}
