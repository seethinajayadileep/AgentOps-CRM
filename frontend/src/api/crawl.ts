import axios from './axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

// Create a client with longer timeout for crawl operations
const crawlClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 300000, // 5 minutes for crawl operations
  headers: {
    'Content-Type': 'application/json',
  },
});

export interface CrawlResponse {
  status: string;
  message: string;
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
  async startCrawl(id: string): Promise<ApiResponse<CrawlResponse>> {
    const response = await crawlClient.post<ApiResponse<CrawlResponse>>(
      `/businesses/${id}/crawl`
    );
    return response.data;
  },

  async getDocuments(id: string): Promise<ApiResponse<Document[]>> {
    const response = await crawlClient.get<ApiResponse<Document[]>>(
      `/businesses/${id}/documents`
    );
    return response.data;
  },
};