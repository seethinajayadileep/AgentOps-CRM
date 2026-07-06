import axios from './axios';
import { HealthResponse } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

/**
 * Health check API functions.
 *
 * @version 0.1.0
 */
export const healthApi = {
  async getHealth(): Promise<HealthResponse> {
    const response = await axios.get<HealthResponse>(`${API_BASE_URL}/health`);
    return response.data;
  },
};
