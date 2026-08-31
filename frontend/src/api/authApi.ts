import { apiClient } from './axios';
import type { AuthSession, AuthUser } from '../types/auth';

export const TOKEN_STORAGE_KEY = 'auth_token';

export const authApi = {
  async login(email: string, password: string): Promise<AuthSession> {
    const response = await apiClient.post<AuthSession>('/auth/login', { email, password });
    return response.data;
  },

  async signup(fullName: string, email: string, password: string): Promise<AuthSession> {
    const response = await apiClient.post<AuthSession>('/auth/signup', { fullName, email, password });
    return response.data;
  },

  async me(): Promise<AuthUser> {
    const response = await apiClient.get<AuthUser>('/auth/me');
    return response.data;
  },

  async logout(): Promise<void> {
    await apiClient.post('/auth/logout');
  },
};
