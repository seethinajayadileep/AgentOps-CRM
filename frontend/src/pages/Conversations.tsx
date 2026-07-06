import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { MessageSquare, RefreshCw, Search, X, ArrowLeft } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import LoadingState from '../components/ui/LoadingState';
import EmptyState from '../components/ui/EmptyState';
import ConversationStatusBadge from '../components/conversations/ConversationStatusBadge';
import ChannelBadge from '../components/conversations/ChannelBadge';
import StatusBadge from '../components/ui/StatusBadge';
import ToastContainer from '../components/ui/ToastContainer';
import { useToast } from '../hooks/useToast';
import { conversationsApi } from '../api/conversationsApi';
import type {
  ConversationListItem,
  ConversationDetail,
  ConversationMessage,
  ConversationSummary,
  ConversationStatus,
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

  // Filters from URL
  const search = searchParams.get('search') || '';
  const statusFilter = searchParams.get('status') as ConversationStatus | null;
  const [searchInput, setSearchInput] = useState(search);

  // Mobile view state
  const [showDetail, setShowDetail] = useState(false);

  useEffect(() => {
    loadSummary();
    loadConversations();
  }, [searchParams]);

  const loadSummary = async () => {
    try {
      const data = await conversationsApi.getConversationSummary();
      setSummary(data);
    } catch (err: any) {
      console.error('Failed to load summary:', err);
    }
  };

  const loadConversations = async (silent = false) => {
    try {
      if (!silent) setLoading(true);
      const filters = {
        search: searchParams.get('search') || undefined,
        status: (searchParams.get('status') as ConversationStatus) || undefined,
      };
      const response = await conversationsApi.getAllConversations(filters);
      setConversations(response.items);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to load conversations');
    } finally {
      setLoading(false);
    }
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

  const handleSearch = () => {
    const params = new URLSearchParams(searchParams);
    if (searchInput) {
      params.set('search', searchInput);
    } else {
      params.delete('search');
    }
    setSearchParams(params);
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
    setSearchInput('');
    setSearchParams({});
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

  if (loading && !summary) {
    return <LoadingState label="Loading conversations…" />;
  }

  if (error) {
    return (
      <div className="p-6">
        <PageHeader title="Conversations" subtitle="Monitor customer conversations handled by your AI support agent" />
        <div className="mt-6 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">
          Error: {error}
          <button onClick={() => loadConversations()} className="ml-4 text-red-200 underline">
            Retry
          </button>
        </div>
      </div>
    );
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
            <div className="text-sm text-zinc-500">Total</div>
            <div className="text-2xl font-semibold text-zinc-100">{summary.totalConversations}</div>
          </Card>
          <Card className="p-4">
            <div className="text-sm text-zinc-500">Active</div>
            <div className="text-2xl font-semibold text-green-400">{summary.activeConversations}</div>
          </Card>
          <Card className="p-4">
            <div className="text-sm text-zinc-500">Today</div>
            <div className="text-2xl font-semibold text-cyan-400">{summary.conversationsToday}</div>
          </Card>
          <Card className="p-4">
            <div className="text-sm text-zinc-500">Leads Captured</div>
            <div className="text-2xl font-semibold text-purple-400">{summary.leadsCaptured}</div>
          </Card>
          <Card className="p-4">
            <div className="text-sm text-zinc-500">Avg Messages</div>
            <div className="text-2xl font-semibold text-zinc-100">{summary.averageMessagesPerConversation.toFixed(1)}</div>
          </Card>
        </div>
      )}

      {/* Filters */}
      <Card className="p-4 mb-6">
        <div className="flex flex-wrap gap-3">
          <div className="flex-1 min-w-[200px]">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
              <input
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                placeholder="Search conversations..."
                className="input-dark pl-10 w-full"
              />
            </div>
          </div>
          <select
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
          <button onClick={handleSearch} className="btn-secondary px-4">
            <Search size={18} />
          </button>
          <button onClick={() => { loadConversations(); loadSummary(); }} className="btn-secondary px-4">
            <RefreshCw size={18} />
          </button>
          {(search || statusFilter) && (
            <button onClick={clearFilters} className="btn-secondary px-4">
              <X size={18} /> Clear
            </button>
          )}
        </div>
      </Card>

      {/* Main Content */}
      {conversations.length === 0 ? (
        <EmptyState
          icon={<MessageSquare size={26} />}
          title={search || statusFilter ? 'No conversations match these filters' : 'No conversations yet'}
          description={search || statusFilter ? 'Try adjusting your filters.' : 'Conversations will appear here when customers interact with your AI support agent.'}
        />
      ) : (
        <div className="flex-1 overflow-hidden">
          <Card className="h-full flex">
            {/* Conversation List - Desktop: Left Panel, Mobile: Full width when detail hidden */}
            <div className={`${showDetail ? 'hidden md:block' : 'block'} w-full md:w-96 border-r border-white/[0.06] overflow-y-auto`}>
              {conversations.map((conv) => (
                <button
                  key={conv.id}
                  onClick={() => loadConversationDetail(conv.id)}
                  className={`w-full text-left p-4 border-b border-white/[0.04] hover:bg-white/[0.02] transition-colors ${
                    selectedConversation?.id === conv.id ? 'bg-white/[0.03]' : ''
                  }`}
                >
                  <div className="flex items-start justify-between mb-2">
                    <div className="font-medium text-zinc-100">{conv.customerName || 'Anonymous'}</div>
                    <div className="text-xs text-zinc-500">{formatRelativeTime(conv.latestMessageAt)}</div>
                  </div>
                  {conv.customerEmail && <div className="text-sm text-zinc-500 mb-2">{conv.customerEmail}</div>}
                  <div className="flex items-center gap-2 mb-2 flex-wrap">
                    <ConversationStatusBadge status={conv.status} />
                    <div className="text-xs text-zinc-600">{conv.businessName}</div>
                  </div>
                  {conv.latestMessagePreview && (
                    <div className="text-sm text-zinc-400 truncate">{conv.latestMessagePreview}</div>
                  )}
                  <div className="flex items-center gap-3 mt-2 text-xs text-zinc-500">
                    <span>{conv.messageCount} messages</span>
                    {conv.leadCount > 0 && <span className="text-green-400">{conv.leadCount} lead(s)</span>}
                  </div>
                </button>
              ))}
            </div>

            {/* Conversation Detail - Desktop: Right Panel, Mobile: Full width when shown */}
            <div className={`${showDetail ? 'block' : 'hidden md:block'} flex-1 flex flex-col`}>
              {!selectedConversation ? (
                <div className="flex-1 flex items-center justify-center text-zinc-500">
                  <div className="text-center">
                    <MessageSquare size={48} className="mx-auto mb-4 opacity-20" />
                    <div>Select a conversation to view details</div>
                  </div>
                </div>
              ) : (
                <>
                  {/* Header */}
                  <div className="p-4 border-b border-white/[0.06]">
                    <div className="flex items-start justify-between mb-3">
                      <div>
                        <div className="flex items-center gap-2 mb-1">
                          <button onClick={() => setShowDetail(false)} className="md:hidden mr-2">
                            <ArrowLeft size={20} />
                          </button>
                          <h3 className="text-lg font-semibold">{selectedConversation.customerName || 'Anonymous'}</h3>
                        </div>
                        {selectedConversation.customerEmail && (
                          <div className="text-sm text-zinc-400">{selectedConversation.customerEmail}</div>
                        )}
                        {selectedConversation.customerPhone && (
                          <div className="text-sm text-zinc-400">{selectedConversation.customerPhone}</div>
                        )}
                      </div>
                      <div className="flex flex-col items-end gap-2">
                        <ConversationStatusBadge status={selectedConversation.status} />
                        <ChannelBadge channel={selectedConversation.channel} />
                      </div>
                    </div>
                    <div className="text-sm text-zinc-500 mb-3">{selectedConversation.businessName}</div>
                    {/* Status Actions */}
                    <div className="flex gap-2 flex-wrap">
                      {selectedConversation.status === 'ACTIVE' && (
                        <>
                          <button onClick={() => updateStatus('PAUSED' as ConversationStatus)} className="btn-secondary px-3 py-1 text-sm" disabled={updatingStatus}>
                            Pause
                          </button>
                          <button onClick={() => updateStatus('CLOSED' as ConversationStatus)} className="btn-secondary px-3 py-1 text-sm" disabled={updatingStatus}>
                            Close
                          </button>
                        </>
                      )}
                      {selectedConversation.status === 'PAUSED' && (
                        <>
                          <button onClick={() => updateStatus('ACTIVE' as ConversationStatus)} className="btn-secondary px-3 py-1 text-sm" disabled={updatingStatus}>
                            Reopen
                          </button>
                          <button onClick={() => updateStatus('CLOSED' as ConversationStatus)} className="btn-secondary px-3 py-1 text-sm" disabled={updatingStatus}>
                            Close
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
                      <div className="text-center text-zinc-500">Loading messages...</div>
                    ) : messages.length === 0 ? (
                      <div className="text-center text-zinc-500">No messages yet</div>
                    ) : (
                      messages.map((msg) => (
                        <div
                          key={msg.id}
                          className={`flex ${msg.role === 'USER' ? 'justify-end' : msg.role === 'SYSTEM' ? 'justify-center' : 'justify-start'}`}
                        >
                          <div
                            className={`max-w-[80%] rounded-lg p-3 ${
                              msg.role === 'USER'
                                ? 'bg-blue-600/20 text-blue-100'
                                : msg.role === 'SYSTEM'
                                ? 'bg-zinc-800/50 text-zinc-400 text-sm'
                                : 'bg-purple-600/20 text-purple-100'
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
                    <div className="p-4 border-t border-white/[0.06] bg-white/[0.01]">
                      <div className="flex items-center gap-2 text-sm">
                        <span className="text-zinc-500">Lead Capture:</span>
                        <StatusBadge status={selectedConversation.leadCaptureStatus} />
                      </div>
                      {selectedConversation.relatedLeads.length > 0 && (
                        <div className="mt-2 text-sm text-zinc-500">
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
