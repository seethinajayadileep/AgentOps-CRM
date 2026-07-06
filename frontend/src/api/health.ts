import { apiClient } from './axios';
import { HealthResponse } from '../types';

/**
 * Health check API functions.
 *
 * @version 0.1.0
 */
export const healthApi = {
  async getHealth(): Promise<HealthResponse> {
    const response = await apiClient.get<HealthResponse>('/health');
    return response.data;
  },
};
