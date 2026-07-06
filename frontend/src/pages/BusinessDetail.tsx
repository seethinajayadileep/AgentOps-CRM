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
import { businessApi } from '../api/business';
import { crawlApi, type Document } from '../api/crawl';
import { ragApi, type RagResultItem } from '../api/rag';
import { useKnowledgeBaseBuildJob } from '../hooks/useKnowledgeBaseBuildJob';
import type { ApiResponse, Business } from '../types/index';
import Card from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import LoadingState from '../components/ui/LoadingState';

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
  const [crawling, setCrawling] = useState(false);
  const [crawlError, setCrawlError] = useState<string | null>(null);

  // Knowledge base build state (Bug 2: async job workflow with polling)
  const kbJob = useKnowledgeBaseBuildJob(id);

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

  const handleDelete = async () => {
    if (!business) return;

    if (
      !confirm(
        'Are you sure you want to delete this business? This will also delete all related data including documents, conversations, leads, and agent logs.'
      )
    ) {
      return;
    }

    try {
      const response: ApiResponse<void> = await businessApi.deleteBusiness(id);
      if (response.success) {
        navigate('/businesses');
      } else {
        setError(response.error || 'Failed to delete business');
      }
    } catch (err: any) {
      setError(err.message || 'Network error occurred');
    }
  };

  const handleStartCrawl = async () => {
    if (!business) return;

    setCrawling(true);
    setCrawlError(null);

    try {
      const response = await crawlApi.startCrawl(id);
      if (response.success) {
        await fetchBusiness();
      } else {
        setCrawlError(response.error || 'Failed to start crawl');
      }
    } catch (err: any) {
      setCrawlError(err.message || 'Failed to start crawl');
    } finally {
      setCrawling(false);
    }
  };

  const handleBuildKB = async () => {
    if (!business) return;
    // useKnowledgeBaseBuildJob already guards against duplicate submissions
    // while a build is starting or actively running.
    await kbJob.startBuild();
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!business) return;

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
    const interval = setInterval(() => {
      if (business?.crawlStatus === 'IN_PROGRESS') {
        fetchBusiness();
      }
    }, 3000);

    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const getCrawlStatusInfo = (status: string) => {
    switch (status) {
      case 'NOT_STARTED':
        return { color: 'gray' as const, icon: Clock, text: 'Not started' };
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

  const statusInfo = business ? getCrawlStatusInfo(business.crawlStatus) : null;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <Link
          to="/businesses"
          className="inline-flex items-center gap-2 text-sm text-zinc-400 hover:text-zinc-100"
        >
          <ArrowLeft size={18} />
          <span>Back to Businesses</span>
        </Link>
        {business && (
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={handleStartCrawl}
              disabled={crawling || business.crawlStatus === 'IN_PROGRESS'}
              className="btn-primary"
            >
              <Play size={16} />
              {crawling ? 'Crawling…' : 'Start Crawl'}
            </button>
            <button
              onClick={handleBuildKB}
              disabled={kbJob.starting || kbJob.isBuildActive || !documents.length}
              className="btn-secondary"
            >
              <Database size={16} />
              {kbJob.starting || kbJob.isBuildActive
                ? `Building… ${kbJob.job ? `${kbJob.job.progressPercentage}%` : ''}`
                : 'Build Knowledge Base'}
            </button>
            <button onClick={() => navigate(`/businesses/${business.id}/chat`)} className="btn-success">
              <MessageCircle size={16} />
              <span>Test Chat</span>
            </button>
            <button onClick={() => navigate(`/businesses/${business.id}/edit`)} className="btn-secondary">
              <Edit size={16} />
              <span>Edit</span>
            </button>
            <button onClick={handleDelete} className="btn-danger">
              <Trash2 size={16} />
              <span>Delete</span>
            </button>
          </div>
        )}
      </div>

      {crawlError && (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">{crawlError}</div>
      )}
      {kbJob.error && (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">{kbJob.error}</div>
      )}
      {kbJob.job && kbJob.isBuildActive && (
        <div className="rounded-xl border border-primary-500/30 bg-primary-500/10 p-4 text-primary-200">
          Knowledge base build in progress: <strong>{kbJob.job.status}</strong> (
          {kbJob.job.progressPercentage}%). The backend accepted this job and continues processing even if
          this page is refreshed.
        </div>
      )}
      {kbJob.job && kbJob.job.status === 'COMPLETED' && (
        <div className="rounded-xl border border-[#22C55E]/30 bg-[#22C55E]/10 p-4 text-[#4ade80]">
          Knowledge base built: {kbJob.job.chunksCreated} chunks, {kbJob.job.embeddingsCreated} embeddings.
        </div>
      )}
      {kbJob.job && kbJob.job.status === 'PARTIAL' && (
        <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-amber-300">
          Knowledge base build finished with warnings: {kbJob.job.errorMessage || 'Some content could not be processed.'}
        </div>
      )}
      {kbJob.job && kbJob.job.status === 'FAILED' && (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">
          Knowledge base build failed: {kbJob.job.errorMessage || 'An unexpected error occurred.'}
        </div>
      )}

      {loading && <LoadingState label="Loading business…" />}

      {error && (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">{error}</div>
      )}

      {!loading && !error && !business && (
        <Card className="p-12 text-center">
          <FileText size={40} className="mx-auto mb-4 text-zinc-600" />
          <p className="text-zinc-400">Business not found</p>
        </Card>
      )}

      {!loading && !error && business && (
        <div className="space-y-6">
          {/* Header Card */}
          <Card className="p-6">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <h2 className="text-2xl font-bold text-white">{business.name}</h2>
                <a
                  href={business.websiteUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-2 inline-flex items-center gap-1 text-primary-300 hover:underline"
                >
                  <Globe size={16} />
                  <span className="max-w-md truncate">{business.websiteUrl}</span>
                </a>
              </div>
              {statusInfo && (
                <Badge color={statusInfo.color} className="gap-1.5 !py-1">
                  <statusInfo.icon
                    size={14}
                    className={business.crawlStatus === 'IN_PROGRESS' ? 'animate-spin' : ''}
                  />
                  {statusInfo.text}
                </Badge>
              )}
            </div>
          </Card>

          {/* RAG Search Section */}
          <Card className="p-6">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="flex items-center gap-2 text-lg font-semibold text-white">
                <Sparkles size={18} className="text-primary-400" />
                RAG Search
              </h3>
              <span className="text-sm text-zinc-500">{documents.length} documents available</span>
            </div>

            {!documents.length ? (
              <div className="py-8 text-center text-zinc-500">
                <Database size={30} className="mx-auto mb-2 text-zinc-600" />
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
                    <button type="submit" disabled={searching} className="btn-primary">
                      <Search size={16} />
                      {searching ? 'Searching…' : 'Search'}
                    </button>
                    {(searchResults.length > 0 || searchQuery) && (
                      <button type="button" onClick={clearSearch} className="btn-ghost">
                        <X size={16} />
                      </button>
                    )}
                  </div>
                </form>

                {searchError && (
                  <div className="mb-4 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">
                    {searchError}
                  </div>
                )}

                {searching && <LoadingState label="Generating answer…" />}

                {/* AI Answer card (shown first) */}
                {!searching && aiAnswer && (
                  <div className="mb-6 rounded-2xl border border-primary-500/30 bg-primary-500/[0.07] p-5">
                    <div className="mb-3 flex items-center gap-2">
                      <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-gradient-to-br from-[#8B5CF6] to-[#3B82F6]">
                        <Sparkles size={14} className="text-white" />
                      </span>
                      <h4 className="font-semibold text-white">AI Answer</h4>
                      <Badge color="purple">AI Agent</Badge>
                      {aiStatus && aiStatus !== 'COMPLETED' && <Badge color="amber">{aiStatus}</Badge>}
                    </div>
                    <p className="whitespace-pre-wrap leading-relaxed text-zinc-100">{aiAnswer}</p>
                    {aiSources.length > 0 && (
                      <div className="mt-4 border-t border-white/[0.08] pt-3">
                        <p className="mb-2 text-xs font-medium uppercase tracking-wide text-zinc-500">Sources</p>
                        <div className="flex flex-wrap gap-2">
                          {aiSources.map((src) => (
                            <a
                              key={src}
                              href={src}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="inline-flex max-w-[420px] items-center gap-1 rounded-full border border-white/[0.1] bg-white/[0.04] px-3 py-1 text-xs text-blue-300 transition-colors hover:bg-white/[0.08]"
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
                    <p className="text-sm font-medium text-zinc-400">
                      Retrieved Knowledge Chunks ({searchResults.length}) · for "{searchQuery}"
                    </p>
                    {searchResults.map((result) => (
                      <div
                        key={result.chunkId}
                        className="rounded-xl border border-white/[0.06] bg-black/30 p-4 transition-colors hover:border-white/[0.12]"
                      >
                        <div className="mb-2 flex items-center gap-2">
                          <Badge color="blue">#{result.rank}</Badge>
                          {result.similarity !== null && (
                            <Badge color="cyan">{Math.round(result.similarity * 100)}% match</Badge>
                          )}
                        </div>
                        <p className="mb-2 line-clamp-3 text-sm text-zinc-300">
                          {(() => {
                            const clean = stripMarkdown(result.content);
                            return clean.length > 400 ? clean.substring(0, 400) + '…' : clean;
                          })()}
                        </p>
                        <a
                          href={result.sourceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-1 text-sm text-primary-300 hover:underline"
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
                  <div className="py-8 text-center text-zinc-500">
                    <Search size={30} className="mx-auto mb-2 text-zinc-600" />
                    <p>No results found for "{searchQuery}"</p>
                    <p className="mt-1 text-sm">Try different keywords or build the knowledge base</p>
                  </div>
                )}

                {!searching && !searched && searchResults.length === 0 && (
                  <div className="py-8 text-center text-zinc-500">
                    <Search size={30} className="mx-auto mb-2 text-zinc-600" />
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
              <h3 className="text-lg font-semibold text-white">Crawled Documents</h3>
              <span className="text-sm text-zinc-500">{documents.length} pages</span>
            </div>
            {documents.length === 0 ? (
              <div className="py-8 text-center text-zinc-500">
                <FileText size={30} className="mx-auto mb-2 text-zinc-600" />
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
                    className="block rounded-xl border border-white/[0.06] p-3 transition-colors hover:border-white/[0.12] hover:bg-white/[0.03]"
                  >
                    <div className="truncate font-medium text-zinc-100">{doc.title}</div>
                    <div className="truncate text-sm text-zinc-500">{doc.url}</div>
                  </a>
                ))}
              </div>
            )}
          </Card>

          {/* Details Cards */}
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
            <Card className="p-6">
              <h3 className="mb-4 text-lg font-semibold text-white">Contact Information</h3>
              <div className="space-y-3">
                {business.contactEmail && (
                  <div>
                    <p className="text-sm text-zinc-500">Email</p>
                    <p className="text-zinc-100">{business.contactEmail}</p>
                  </div>
                )}
                {business.contactPhone && (
                  <div>
                    <p className="text-sm text-zinc-500">Phone</p>
                    <p className="text-zinc-100">{business.contactPhone}</p>
                  </div>
                )}
                {!business.contactEmail && !business.contactPhone && (
                  <p className="italic text-zinc-500">No contact information provided</p>
                )}
              </div>
            </Card>

            <Card className="p-6">
              <h3 className="mb-4 text-lg font-semibold text-white">Business Details</h3>
              <div className="space-y-3">
                <div>
                  <p className="text-sm text-zinc-500">Industry</p>
                  <p className="text-zinc-100">{business.industry || '-'}</p>
                </div>
                <div>
                  <p className="text-sm text-zinc-500">Description</p>
                  <p className="text-zinc-100">{business.description || 'No description provided'}</p>
                </div>
              </div>
            </Card>

            <Card className="p-6 md:col-span-2">
              <h3 className="mb-4 text-lg font-semibold text-white">Timeline</h3>
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <Calendar size={18} className="text-zinc-500" />
                  <div>
                    <p className="text-sm text-zinc-500">Created</p>
                    <p className="text-zinc-100">{new Date(business.createdAt).toLocaleString()}</p>
                  </div>
                </div>
                {business.updatedAt && (
                  <div className="flex items-center gap-3">
                    <Clock size={18} className="text-zinc-500" />
                    <div>
                      <p className="text-sm text-zinc-500">Last Updated</p>
                      <p className="text-zinc-100">{new Date(business.updatedAt).toLocaleString()}</p>
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
