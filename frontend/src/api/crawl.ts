import { apiClient } from './axios';

export interface CrawlStatusPayload {
  status: string;
  message: string;
  startedAt?: string | null;
  finishedAt?: string | null;
  error?: string | null;
  pagesSaved?: number;
  pagesTotal?: number;
  elapsedSeconds?: number | null;
}

export interface Document {
  id: string;
  url: string;
  title: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

export const crawlApi = {
  async startCrawl(id: string): Promise<ApiResponse<CrawlStatusPayload>> {
    const response = await apiClient.post<ApiResponse<CrawlStatusPayload>>(
      `/businesses/${id}/crawl`
    );
    return response.data;
  },

  async getCrawlStatus(id: string): Promise<ApiResponse<CrawlStatusPayload>> {
    const response = await apiClient.get<ApiResponse<CrawlStatusPayload>>(
      `/businesses/${id}/crawl-status`
    );
    return response.data;
  },

  async getDocuments(id: string): Promise<ApiResponse<Document[]>> {
    const response = await apiClient.get<ApiResponse<Document[]>>(
      `/businesses/${id}/documents`
    );
    return response.data;
  },
};
