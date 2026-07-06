import { apiClient } from './axios';
import type { Lead, LeadQualificationRequest, LeadQualificationResponse, LeadStatusUpdateRequest } from '../types/lead';

export const leadsApi = {
  // Qualify a lead from a message
  qualifyLead: async (request: LeadQualificationRequest): Promise<LeadQualificationResponse> => {
    const response = await apiClient.post<LeadQualificationResponse>(
      '/leads/qualify',
      request
    );
    return response.data;
  },

  // Get all leads
  getAllLeads: async (): Promise<Lead[]> => {
    const response = await apiClient.get<Lead[]>('/leads');
    return response.data;
  },

  // Get a single lead by ID
  getLeadById: async (id: string): Promise<Lead> => {
    const response = await apiClient.get<Lead>(`/leads/${id}`);
    return response.data;
  },

  // Update lead status
  updateLeadStatus: async (id: string, request: LeadStatusUpdateRequest): Promise<Lead> => {
    const response = await apiClient.put<Lead>(
      `/leads/${id}/status`,
      request
    );
    return response.data;
  },

  // Get leads for a specific business
  getLeadsByBusiness: async (businessId: string): Promise<Lead[]> => {
    const response = await apiClient.get<Lead[]>(`/leads/business/${businessId}`);
    return response.data;
  },
};
