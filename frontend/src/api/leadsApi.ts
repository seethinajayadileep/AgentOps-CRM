import axios from 'axios';
import type { Lead, LeadQualificationRequest, LeadQualificationResponse, LeadStatusUpdateRequest } from '../types/lead';

const API_BASE_URL = 'http://localhost:8080/api';

export const leadsApi = {
  // Qualify a lead from a message
  qualifyLead: async (request: LeadQualificationRequest): Promise<LeadQualificationResponse> => {
    const response = await axios.post<LeadQualificationResponse>(
      `${API_BASE_URL}/leads/qualify`,
      request
    );
    return response.data;
  },

  // Get all leads
  getAllLeads: async (): Promise<Lead[]> => {
    const response = await axios.get<Lead[]>(`${API_BASE_URL}/leads`);
    return response.data;
  },

  // Get a single lead by ID
  getLeadById: async (id: string): Promise<Lead> => {
    const response = await axios.get<Lead>(`${API_BASE_URL}/leads/${id}`);
    return response.data;
  },

  // Update lead status
  updateLeadStatus: async (id: string, request: LeadStatusUpdateRequest): Promise<Lead> => {
    const response = await axios.put<Lead>(
      `${API_BASE_URL}/leads/${id}/status`,
      request
    );
    return response.data;
  },

  // Get leads for a specific business
  getLeadsByBusiness: async (businessId: string): Promise<Lead[]> => {
    const response = await axios.get<Lead[]>(`${API_BASE_URL}/leads/business/${businessId}`);
    return response.data;
  },
};
