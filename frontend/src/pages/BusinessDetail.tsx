import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Edit,
  Trash2,
  Globe,
  FileText,
  Calendar,
  Clock,
  AlertCircle,
  CheckCircle,
  Play,
  RefreshCw,
  Database,
  Search,
  Sparkles,
  X,
  MessageCircle,
} from 'lucide-react';
import { businessApi, type BusinessDependencies } from '../api/business';
import { crawlApi, type Document } from '../api/crawl';
import { ragApi, type RagResultItem } from '../api/rag';
import { useKnowledgeBaseBuildJob } from '../hooks/useKnowledgeBaseBuildJob';
import { useCrawlJob, isCrawlActive } from '../hooks/useCrawlJob';
import { useIntegrations } from '../hooks/useIntegrations';
import { useToast } from '../hooks/useToast';
import type { ApiResponse, Business } from '../types/index';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import LoadingState from '../components/ui/LoadingState';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import ToastContainer from '../components/ui/ToastContainer';

/** Light markdown cleanup for chunk previews so raw links/images aren't shown as prose. */
function stripMarkdown(text: string): string {
  return text
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ') // images
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1') // links -> label
    .replace(/https?:\/\/\S+/g, ' ') // bare URLs
    .replace(/[#*`>|_]+/g, ' ') // md markers
    .replace(/\s+/g, ' ')
    .trim();
}

/**
  * Business detail page.
  *
  * @version 0.4.0
  * Feature: F-002, F-003, F-004 (Build Knowledge Base + RAG test search)
  */
export default function BusinessDetail() {
  const { id } = useParams<{ id?: string }>();
  const navigate = useNavigate();

  if (!id) {
    return <div>Business ID is required</div>;
  }

  const [business, setBusiness] = useState<Business | null>(null);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const crawlJob = useCrawlJob(id);
  const { toasts, showToast, closeToast } = useToast();
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [dependencies, setDependencies] = useState<BusinessDependencies | null>(null);

  // Knowledge base build state (Bug 2: async job workflow with polling)
  const kbJob = useKnowledgeBaseBuildJob(id);
  const integrations = useIntegrations();
  const firecrawlReady = integrations.ready('Firecrawl');
  const openaiReady = integrations.ready('OpenAI');

  // RAG search state
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<RagResultItem[]>([]);
  const [searching, setSearching] = useState(false);
  const [searched, setSearched] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  // AI answer (RAG generation) state
  const [aiAnswer, setAiAnswer] = useState<string | null>(null);
  const [aiSources, setAiSources] = useState<string[]>([]);
  const [aiStatus, setAiStatus] = useState<string | null>(null);

  const fetchBusiness = async () => {
    setLoading(true);
    setError(null);

    try {
      const response: ApiResponse<Business> = await businessApi.getBusinessById(id);
      if (response.success && response.data) {
        setBusiness(response.data);
        fetchDocuments();
      } else {
        setError(response.error || 'Business not found');
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load business details');
    } finally {
      setLoading(false);
    }
  };

  const fetchDocuments = async () => {
    try {
      const response = await crawlApi.getDocuments(id);
      if (response.success && response.data) {
        setDocuments(response.data);
      }
    } catch (err) {
      // Silently fail for documents
    }
  };

  const openDeleteDialog = async () => {
    if (!business) return;
    try {
      const response = await businessApi.getDependencies(id);
      if (response.success && response.data) {
        setDependencies(response.data);
      } else {
        setDependencies(null);
      }
    } catch {
      setDependencies(null);
    }
    setDeleteOpen(true);
  };

  const handleDelete = async () => {
    if (!business) return;
    setDeleteBusy(true);
    try {
      const response: ApiResponse<void> = await businessApi.deleteBusiness(id);
      if (response.success) {
        showToast('success', 'Business deleted. Related records were removed.');
        navigate('/businesses', { state: { toast: 'Business deleted successfully' } });
      } else {
        setError(response.error || 'Failed to delete business');
        setDeleteOpen(false);
      }
    } catch (err: any) {
      setError(err.message || 'The business could not be deleted. No records were removed.');
      setDeleteOpen(false);
    } finally {
      setDeleteBusy(false);
    }
  };

  const handleStartCrawl = async () => {
    if (!business || !firecrawlReady) return;
    await crawlJob.startCrawl();
  };

  const handleBuildKB = async () => {
    if (!business || !openaiReady) return;
    // useKnowledgeBaseBuildJob already guards against duplicate submissions
    // while a build is starting or actively running.
    await kbJob.startBuild();
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!business || !openaiReady) return;

    const trimmedQuery = searchQuery.trim();
    if (!trimmedQuery) {
      setSearchError('Please enter a search query');
      return;
    }

    setSearching(true);
    setSearchError(null);
    setSearchResults([]);
    setAiAnswer(null);
    setAiSources([]);
    setAiStatus(null);
    setSearched(false);

    try {
      // Full RAG flow: grounded answer + sources + retrieved chunks (for debugging).
      const response = await ragApi.answer({
        businessId: id,
        query: trimmedQuery,
      });

      if (response.success && response.data) {
        setAiAnswer(response.data.answer);
        setAiSources(response.data.sources || []);
        setAiStatus(response.data.status);
        setSearchResults(response.data.results || []);
      } else {
        setSearchError(response.error || 'Search failed');
      }
    } catch (err: any) {
      setSearchError(err.message || 'Network error occurred');
    } finally {
      setSearching(false);
      setSearched(true);
    }
  };

  const clearSearch = () => {
    setSearchResults([]);
    setSearchQuery('');
    setSearched(false);
    setSearchError(null);
    setAiAnswer(null);
    setAiSources([]);
    setAiStatus(null);
  };

  useEffect(() => {
    fetchBusiness();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(() => {
    if (crawlJob.status?.status === 'COMPLETED' || crawlJob.status?.status === 'FAILED') {
      fetchDocuments();
      fetchBusiness();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [crawlJob.status?.status]);

  const liveCrawlStatus = crawlJob.status?.status || business?.crawlStatus || 'NOT_STARTED';

  const getCrawlStatusInfo = (status: string) => {
    switch (status) {
      case 'NOT_STARTED':
        return { color: 'gray' as const, icon: Clock, text: 'Not started' };
      case 'QUEUED':
        return { color: 'cyan' as const, icon: Clock, text: 'Queued' };
      case 'CRAWLING':
      case 'IN_PROGRESS':
        return { color: 'cyan' as const, icon: RefreshCw, text: 'Crawling…' };
      case 'COMPLETED':
        return { color: 'green' as const, icon: CheckCircle, text: 'Completed' };
      case 'FAILED':
        return { color: 'red' as const, icon: AlertCircle, text: 'Failed' };
      default:
        return { color: 'gray' as const, icon: Clock, text: status };
    }
  };

  const statusInfo = business ? getCrawlStatusInfo(liveCrawlStatus) : null;

  return (
    <div className="space-y-6">
      <ToastContainer toasts={toasts} onClose={closeToast} />
      {deleteOpen && (
        <ConfirmDialog
          title="Delete this business?"
          confirmLabel="Delete business"
          danger
          busy={deleteBusy}
          onConfirm={handleDelete}
          onClose={() => setDeleteOpen(false)}
        >
          <p>
            This permanently deletes <strong>{business?.name}</strong> and all related records in one
            transaction.
          </p>
          <ul className="mt-3 list-disc space-y-1 pl-5">
            <li>{dependencies?.leads ?? 0} leads</li>
            <li>{dependencies?.conversations ?? 0} conversations</li>
            <li>{dependencies?.documents ?? 0} documents</li>
            <li>{dependencies?.approvals ?? 0} approvals</li>
            <li>{dependencies?.agentLogs ?? 0} agent logs</li>
            <li>{dependencies?.voiceCalls ?? 0} voice calls</li>
          </ul>
          <p className="mt-3">If deletion fails, the business and its records stay intact.</p>
        </ConfirmDialog>
      )}
      {/* Page Header */}
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <Link
          to="/businesses"
          className="inline-flex items-center gap-2 text-sm text-slate hover:text-ink"
        >
          <ArrowLeft size={18} />
          <span>Back to Businesses</span>
        </Link>
        {business && (
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={handleStartCrawl}
              disabled={!firecrawlReady || crawlJob.starting || crawlJob.isActive}
              className="btn-primary"
              title={!firecrawlReady ? 'Firecrawl is not configured' : undefined}
            >
              <Play size={16} />
              {crawlJob.starting || crawlJob.isActive ? 'Crawling…' : 'Start Crawl'}
            </button>
            <button
              onClick={handleBuildKB}
              disabled={!openaiReady || kbJob.starting || kbJob.isBuildActive || !documents.length}
              className="btn-secondary"
              title={!openaiReady ? 'OpenAI is not configured' : undefined}
            >
              <Database size={16} />
              {kbJob.starting || kbJob.isBuildActive
                ? `Building… ${kbJob.job ? `${kbJob.job.progressPercentage}%` : ''}`
                : 'Build Knowledge Base'}
            </button>
            <button
              onClick={() => navigate(`/businesses/${business.id}/chat`)}
              disabled={!openaiReady}
              className="btn-success"
              title={!openaiReady ? 'OpenAI is not configured' : undefined}
            >
              <MessageCircle size={16} />
              <span>Test Chat</span>
            </button>
            <button onClick={() => navigate(`/businesses/${business.id}/edit`)} className="btn-secondary">
              <Edit size={16} />
              <span>Edit</span>
            </button>
            <button onClick={openDeleteDialog} className="btn-danger">
              <Trash2 size={16} />
              <span>Delete</span>
            </button>
          </div>
        )}
      </div>

      {!firecrawlReady && (
        <div className="rounded-sm border border-frost bg-mist p-4 text-ink">
          Website crawling is unavailable until Firecrawl is configured in Settings.
        </div>
      )}
      {!openaiReady && (
        <div className="rounded-sm border border-frost bg-mist p-4 text-ink">
          Knowledge base and chat actions are unavailable until OpenAI is configured in Settings.
        </div>
      )}
      {crawlJob.error && (
        <div className="rounded-sm border border-frost bg-mist p-4 text-ink">{crawlJob.error}</div>
      )}
      {crawlJob.isActive && crawlJob.status && (
        <div className="rounded-sm border border-frost bg-mist p-4 text-ink">
          Crawl {liveCrawlStatus.toLowerCase()}: {crawlJob.status.pagesSaved ?? 0}
          {crawlJob.status.pagesTotal ? ` / ${crawlJob.status.pagesTotal}` : ''} pages
          {crawlJob.status.elapsedSeconds != null ? ` · ${crawlJob.status.elapsedSeconds}s elapsed` : ''}.
          Status is saved and survives refresh.
        </div>
      )}
      {liveCrawlStatus === 'FAILED' && (crawlJob.status?.error || business?.crawlError) && (
        <div className="rounded-sm border border-frost bg-mist p-4 text-ink">
          Crawl failed: {crawlJob.status?.error || business?.crawlError}
        </div>
      )}
      {kbJob.error && (
        <div className="rounded-sm border border-frost bg-mist p-4 text-ink">{kbJob.error}</div>
      )}
      {kbJob.job && kbJob.isBuildActive && (
        <div className="rounded-sm border border-frost bg-mist p-4 text-ink">
          Knowledge base build in progress: <strong>{kbJob.job.status}</strong> (
          {kbJob.job.progressPercentage}%). The backend accepted this job and continues processing even if
          this page is refreshed.
        </div>
      )}
      {kbJob.job && kbJob.job.status === 'COMPLETED' && (
        <div className="rounded-sm border border-frost bg-mist p-4 text-ink">
          Knowledge base built: {kbJob.job.chunksCreated} chunks, {kbJob.job.embeddingsCreated} embeddings.
        </div>
      )}
      {kbJob.job && kbJob.job.status === 'PARTIAL' && (
        <div className="rounded-sm border border-amber-500/30 bg-amber-500/10 p-4 text-amber-300">
          Knowledge base build finished with warnings: {kbJob.job.errorMessage || 'Some content could not be processed.'}
        </div>
      )}
      {kbJob.job && kbJob.job.status === 'FAILED' && (
        <div className="rounded-sm border border-frost bg-mist p-4 text-ink">
          Knowledge base build failed: {kbJob.job.errorMessage || 'An unexpected error occurred.'}
          <button type="button" className="btn-secondary ml-3" onClick={handleBuildKB}>
            Retry
          </button>
        </div>
      )}
      {kbJob.job && kbJob.isBuildActive && (
        <p className="text-sm text-slate">
          Stage: {kbJob.job.status === 'QUEUED' ? 'Preparing' : kbJob.job.status.toLowerCase()} ·{' '}
          {kbJob.job.documentsProcessed}/{kbJob.job.documentsTotal} documents
        </p>
      )}

      {loading && <LoadingState label="Loading business…" />}

      {error && (
        <div className="rounded-sm border border-frost bg-mist p-4 text-ink">{error}</div>
      )}

      {!loading && !error && !business && (
        <Card className="p-12 text-center">
          <FileText size={40} className="mx-auto mb-4 text-silver" />
          <p className="text-slate">Business not found</p>
        </Card>
      )}

      {!loading && !error && business && (
        <div className="space-y-6">
          {/* Header Card */}
          <Card className="p-6">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <h2 className="text-2xl font-bold text-ink">{business.name}</h2>
                <a
                  href={business.websiteUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-2 inline-flex items-center gap-1 text-ink hover:underline"
                >
                  <Globe size={16} />
                  <span className="max-w-md truncate">{business.websiteUrl}</span>
                </a>
              </div>
              {statusInfo && (
                <Badge color={statusInfo.color} className="gap-1.5 !py-1">
                  <statusInfo.icon
                    size={14}
                    className={isCrawlActive(liveCrawlStatus) ? 'animate-spin' : ''}
                  />
                  {statusInfo.text}
                </Badge>
              )}
            </div>
          </Card>

          {/* RAG Search Section */}
          <Card className="p-6">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="flex items-center gap-2 text-lg font-semibold text-ink">
                <Sparkles size={18} className="text-slate" />
                RAG Search
              </h3>
              <span className="text-sm text-slate">{documents.length} documents available</span>
            </div>

            {!documents.length ? (
              <div className="py-8 text-center text-slate">
                <Database size={30} className="mx-auto mb-2 text-silver" />
                <p>No documents available</p>
                <p className="mt-1 text-sm">
                  {business.crawlStatus === 'NOT_STARTED'
                    ? 'Crawl the website first to create documents'
                    : 'No documents found'}
                </p>
              </div>
            ) : (
              <>
                <form onSubmit={handleSearch} className="mb-6">
                  <div className="flex gap-3">
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder="Ask a question about this business…"
                      className="input-dark flex-1"
                    />
                    <button type="submit" disabled={!openaiReady || searching} className="btn-primary">
                      <Search size={16} />
                      {searching ? 'Searching…' : 'Search'}
                    </button>
                    {(searchResults.length > 0 || searchQuery) && (
                      <button
                        type="button"
                        onClick={clearSearch}
                        className="btn-ghost"
                        aria-label="Clear search results"
                      >
                        <X size={16} />
                      </button>
                    )}
                  </div>
                </form>

                {searchError && (
                  <div className="mb-4 rounded-sm border border-frost bg-mist p-4 text-ink">
                    {searchError}
                  </div>
                )}

                {searching && <LoadingState label="Generating answer…" />}

                {/* AI Answer card (shown first) */}
                {!searching && aiAnswer && (
                  <div className="mb-6 rounded-sm border border-frost bg-mist p-5">
                    <div className="mb-3 flex items-center gap-2">
                      <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-ink">
                        <Sparkles size={14} className="text-snow" />
                      </span>
                      <h4 className="font-semibold text-ink">AI Answer</h4>
                      <Badge color="purple">AI Agent</Badge>
                      {aiStatus && aiStatus !== 'COMPLETED' && <Badge color="amber">{aiStatus}</Badge>}
                    </div>
                    <p className="whitespace-pre-wrap leading-relaxed text-ink">{aiAnswer}</p>
                    {aiStatus && aiStatus !== 'COMPLETED' && (
                      <p className="mt-3 text-sm text-slate">
                        The knowledge base does not contain confirmed information for this question, so no
                        sources are shown.
                      </p>
                    )}
                    {aiSources.length > 0 && aiStatus === 'COMPLETED' && (
                      <div className="mt-4 border-t border-frost pt-3">
                        <p className="mb-2 text-xs font-medium uppercase tracking-wide text-slate">Sources</p>
                        <div className="flex flex-wrap gap-2">
                          {aiSources.map((src) => (
                            <a
                              key={src}
                              href={src}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="inline-flex max-w-[420px] items-center gap-1 rounded-full border border-frost bg-mist px-3 py-1 text-xs text-ink transition-colors hover:bg-frost"
                            >
                              <Globe size={12} />
                              <span className="truncate">{src}</span>
                            </a>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* Retrieved chunks (debug/details) */}
                {!searching && searchResults.length > 0 && (
                  <div className="space-y-3">
                    <p className="text-sm font-medium text-slate">
                      Retrieved Knowledge Chunks ({searchResults.length}) · for "{searchQuery}"
                    </p>
                    {searchResults.map((result) => (
                      <div
                        key={result.chunkId}
                        className="rounded-sm border border-frost bg-snow p-4 transition-colors hover:border-silver"
                      >
                        <div className="mb-2 flex items-center gap-2">
                          <Badge color="blue">#{result.rank}</Badge>
                          {result.similarity !== null && (
                            <Badge color="cyan">{Math.round(result.similarity * 100)}% match</Badge>
                          )}
                        </div>
                        <p className="mb-2 line-clamp-3 text-sm text-ink">
                          {(() => {
                            const clean = stripMarkdown(result.content);
                            return clean.length > 400 ? clean.substring(0, 400) + '…' : clean;
                          })()}
                        </p>
                        <a
                          href={result.sourceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-1 text-sm text-ink hover:underline"
                        >
                          <Globe size={14} />
                          <span className="max-w-[300px] truncate">
                            {result.documentTitle || result.sourceUrl}
                          </span>
                        </a>
                      </div>
                    ))}
                  </div>
                )}

                {!searching && searched && searchResults.length === 0 && !searchError && (
                  <div className="py-8 text-center text-slate">
                    <Search size={30} className="mx-auto mb-2 text-silver" />
                    <p>No results found for "{searchQuery}"</p>
                    <p className="mt-1 text-sm">Try different keywords or build the knowledge base</p>
                  </div>
                )}

                {!searching && !searched && searchResults.length === 0 && (
                  <div className="py-8 text-center text-slate">
                    <Search size={30} className="mx-auto mb-2 text-silver" />
                    <p>Ask a question to search in the knowledge base</p>
                    <p className="mt-1 text-sm">Examples: "services offered", "contact information"</p>
                  </div>
                )}
              </>
            )}
          </Card>

          {/* Crawled Documents */}
          <Card className="p-6">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-lg font-semibold text-ink">Crawled Documents</h3>
              <span className="text-sm text-slate">{documents.length} pages</span>
            </div>
            {documents.length === 0 ? (
              <div className="py-8 text-center text-slate">
                <FileText size={30} className="mx-auto mb-2 text-silver" />
                <p>No documents crawled yet</p>
                {business.crawlStatus === 'NOT_STARTED' && (
                  <p className="mt-1 text-sm">Click "Start Crawl" to begin</p>
                )}
              </div>
            ) : (
              <div className="max-h-96 space-y-2 overflow-y-auto">
                {documents.map((doc) => (
                  <a
                    key={doc.id}
                    href={doc.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="block rounded-sm border border-frost p-3 transition-colors hover:border-silver hover:bg-mist"
                  >
                    <div className="truncate font-medium text-ink">{doc.title}</div>
                    <div className="truncate text-sm text-slate">{doc.url}</div>
                  </a>
                ))}
              </div>
            )}
          </Card>

          {/* Details Cards */}
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
            <Card className="p-6">
              <h3 className="mb-4 text-lg font-semibold text-ink">Contact Information</h3>
              <div className="space-y-3">
                {business.contactEmail && (
                  <div>
                    <p className="text-sm text-slate">Email</p>
                    <p className="text-ink">{business.contactEmail}</p>
                  </div>
                )}
                {business.contactPhone && (
                  <div>
                    <p className="text-sm text-slate">Phone</p>
                    <p className="text-ink">{business.contactPhone}</p>
                  </div>
                )}
                {!business.contactEmail && !business.contactPhone && (
                  <p className="italic text-slate">No contact information provided</p>
                )}
              </div>
            </Card>

            <Card className="p-6">
              <h3 className="mb-4 text-lg font-semibold text-ink">Business Details</h3>
              <div className="space-y-3">
                <div>
                  <p className="text-sm text-slate">Industry</p>
                  <p className="text-ink">{business.industry || '-'}</p>
                </div>
                <div>
                  <p className="text-sm text-slate">Description</p>
                  <p className="text-ink">{business.description || 'No description provided'}</p>
                </div>
              </div>
            </Card>

            <Card className="p-6 md:col-span-2">
              <h3 className="mb-4 text-lg font-semibold text-ink">Timeline</h3>
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <Calendar size={18} className="text-slate" />
                  <div>
                    <p className="text-sm text-slate">Created</p>
                    <p className="text-ink">{new Date(business.createdAt).toLocaleString()}</p>
                  </div>
                </div>
                {business.updatedAt && (
                  <div className="flex items-center gap-3">
                    <Clock size={18} className="text-slate" />
                    <div>
                      <p className="text-sm text-slate">Last Updated</p>
                      <p className="text-ink">{new Date(business.updatedAt).toLocaleString()}</p>
                    </div>
                  </div>
                )}
              </div>
            </Card>
          </div>
        </div>
      )}
    </div>
  );
}
