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

export const ragApi = {
  async buildKnowledgeBase(id: string): Promise<ApiResponse<BuildResponse>> {
    const response = await apiClient.post<ApiResponse<BuildResponse>>(
      `/businesses/${id}/knowledge-base/build`
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
