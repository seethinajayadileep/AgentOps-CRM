import { apiClient } from './axios';

export interface BuildResponse {
  businessId: string;
  success: boolean;
  status: string;
  documentsProcessed: number;
  chunksCreated: number;
  embeddingsCreated: number;
  skipped: number;
  message: string;
}

export interface RagResultItem {
  chunkId: string;
  content: string;
  sourceUrl: string;
  documentTitle: string | null;
  rank: number;
  similarity: number | null;
}

export interface SearchResponse {
  query: string;
  totalResults: number;
  items: RagResultItem[];
}

export interface AnswerResponse {
  businessId: string;
  query: string;
  answer: string;
  sources: string[];
  results: RagResultItem[];
  topK: number;
  status: string;
}

export interface SearchRequest {
  businessId: string;
  query: string;
  topK?: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

/**
 * Status of an asynchronous knowledge-base build job (Bug 2 fix).
 * Mirrors backend KnowledgeBaseJobStatus.
 */
export type KnowledgeBaseJobStatus =
  | 'QUEUED'
  | 'CRAWLING'
  | 'CHUNKING'
  | 'EMBEDDING'
  | 'COMPLETED'
  | 'PARTIAL'
  | 'FAILED';

export interface KnowledgeBaseJob {
  jobId: string;
  businessId: string;
  status: KnowledgeBaseJobStatus;
  progressPercentage: number;
  documentsTotal: number;
  documentsProcessed: number;
  chunksCreated: number;
  embeddingsCreated: number;
  errorMessage?: string | null;
  startedAt: string;
  updatedAt: string;
  completedAt?: string | null;
}

/** Terminal (non-active) knowledge-base job statuses. */
export const KB_JOB_TERMINAL_STATUSES: KnowledgeBaseJobStatus[] = ['COMPLETED', 'PARTIAL', 'FAILED'];

export const ragApi = {
  /**
   * Start an asynchronous knowledge-base build job. The backend responds
   * immediately (202 Accepted) with a QUEUED job - callers must poll
   * {@link getKnowledgeBaseJob} for progress rather than waiting on this call.
   */
  async buildKnowledgeBase(id: string): Promise<ApiResponse<KnowledgeBaseJob>> {
    const response = await apiClient.post<ApiResponse<KnowledgeBaseJob>>(
      `/businesses/${id}/knowledge-base/build`
    );
    return response.data;
  },

  /** Poll the status/progress of a knowledge-base build job. */
  async getKnowledgeBaseJob(businessId: string, jobId: string): Promise<ApiResponse<KnowledgeBaseJob>> {
    const response = await apiClient.get<ApiResponse<KnowledgeBaseJob>>(
      `/businesses/${businessId}/knowledge-base/jobs/${jobId}`
    );
    return response.data;
  },

  /** Restore the active (in-progress) build job for a business, if any. */
  async getActiveKnowledgeBaseJob(businessId: string): Promise<ApiResponse<KnowledgeBaseJob | null>> {
    const response = await apiClient.get<ApiResponse<KnowledgeBaseJob | null>>(
      `/businesses/${businessId}/knowledge-base/jobs/active`
    );
    return response.data;
  },

  async searchKnowledgeBase(request: SearchRequest): Promise<ApiResponse<SearchResponse>> {
    const response = await apiClient.post<ApiResponse<SearchResponse>>(
      '/rag/search',
      request
    );
    return response.data;
  },

  async answer(request: SearchRequest): Promise<ApiResponse<AnswerResponse>> {
    const response = await apiClient.post<ApiResponse<AnswerResponse>>(
      '/rag/answer',
      request
    );
    return response.data;
  },
};
