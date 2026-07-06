import axios from './axios';
import type {
  ApiResponse,
  Business,
  CreateBusinessRequest,
  UpdateBusinessRequest,
  PaginatedResponse,
} from '../types/index';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export const businessApi = {
  async getAllBusinesses(params?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
  }): Promise<ApiResponse<PaginatedResponse<Business>>> {
    const response = await axios.get<ApiResponse<PaginatedResponse<Business>>>(
      `${API_BASE_URL}/businesses`,
      { params }
    );
    return response.data;
  },

  async getBusinessById(id: string): Promise<ApiResponse<Business>> {
    const response = await axios.get<ApiResponse<Business>>(
      `${API_BASE_URL}/businesses/${id}`
    );
    return response.data;
  },

  async createBusiness(
    data: CreateBusinessRequest
  ): Promise<ApiResponse<Business>> {
    const response = await axios.post<ApiResponse<Business>>(
      `${API_BASE_URL}/businesses`,
      data
    );
    return response.data;
  },

  async updateBusiness(
    id: string,
    data: UpdateBusinessRequest
  ): Promise<ApiResponse<Business>> {
    const response = await axios.put<ApiResponse<Business>>(
      `${API_BASE_URL}/businesses/${id}`,
      data
    );
    return response.data;
  },

  async deleteBusiness(id: string): Promise<ApiResponse<void>> {
    const response = await axios.delete<ApiResponse<void>>(
      `${API_BASE_URL}/businesses/${id}`
    );
    return response.data;
  },

  async searchBusinesses(term: string, params?: {
    page?: number;
    size?: number;
  }): Promise<ApiResponse<PaginatedResponse<Business>>> {
    const response = await axios.get<ApiResponse<PaginatedResponse<Business>>>(
      `${API_BASE_URL}/businesses/search`,
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
    const response = await axios.get<ApiResponse<PaginatedResponse<Business>>>(
      `${API_BASE_URL}/businesses/crawl-status/\${status}`,
      { params }
    );
    return response.data;
  },
};
