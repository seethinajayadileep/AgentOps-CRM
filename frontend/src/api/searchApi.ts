import { apiClient } from './axios';

export interface SearchHit {
  id: string;
  title: string;
  subtitle: string;
  href: string;
}

export interface GlobalSearchResponse {
  businesses: SearchHit[];
  leads: SearchHit[];
  conversations: SearchHit[];
}

export async function searchCrm(query: string): Promise<GlobalSearchResponse> {
  const response = await apiClient.get<GlobalSearchResponse>('/search', {
    params: { q: query },
  });
  return response.data;
}
