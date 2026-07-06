import { apiClient } from './axios';
import type {
  ApiResponse,
  Business,
  CreateBusinessRequest,
  UpdateBusinessRequest,
  PaginatedResponse,
} from '../types/index';

export const businessApi = {
  async getAllBusinesses(params?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
  }): Promise<ApiResponse<PaginatedResponse<Business>>> {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Business>>>(
      '/businesses',
      { params }
    );
    return response.data;
  },

  async getBusinessById(id: string): Promise<ApiResponse<Business>> {
    const response = await apiClient.get<ApiResponse<Business>>(
      `/businesses/${id}`
    );
    return response.data;
  },

  async createBusiness(
    data: CreateBusinessRequest
  ): Promise<ApiResponse<Business>> {
    const response = await apiClient.post<ApiResponse<Business>>(
      '/businesses',
      data
    );
    return response.data;
  },

  async updateBusiness(
    id: string,
    data: UpdateBusinessRequest
  ): Promise<ApiResponse<Business>> {
    const response = await apiClient.put<ApiResponse<Business>>(
      `/businesses/${id}`,
      data
    );
    return response.data;
  },

  async deleteBusiness(id: string): Promise<ApiResponse<void>> {
    const response = await apiClient.delete<ApiResponse<void>>(
      `/businesses/${id}`
    );
    return response.data;
  },

  async searchBusinesses(term: string, params?: {
    page?: number;
    size?: number;
  }): Promise<ApiResponse<PaginatedResponse<Business>>> {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Business>>>(
      '/businesses/search',
      {
        params: { term, ...params },
      }
    );
    return response.data;
  },

  async getByCrawlStatus(
    _status: string,
    params?: { page?: number; size?: number }
  ): Promise<ApiResponse<PaginatedResponse<Business>>> {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Business>>>(
      '/businesses/crawl-status/\${status}',
      { params }
    );
    return response.data;
  },
};
