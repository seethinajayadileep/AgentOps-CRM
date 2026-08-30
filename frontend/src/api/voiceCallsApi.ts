import axios from './axios';
import { VoiceCall, VoiceCallStartRequest } from '../types/voiceCall';

export const voiceCallsApi = {
  /**
   * Start a new voice call for a lead.
   */
  async startCall(leadId: string, request: VoiceCallStartRequest): Promise<VoiceCall> {
    const response = await axios.post(
      `/leads/${leadId}/voice-calls/start`,
      request
    );
    return response.data.data;
  },

  /**
   * Get all voice calls for a lead.
   */
  async getLeadCalls(leadId: string, page: number = 0, size: number = 20): Promise<{
    items: VoiceCall[];
    total: number;
    page: number;
    pageSize: number;
    totalPages: number;
  }> {
    const response = await axios.get(
      `/leads/${leadId}/voice-calls`,
      { params: { page, size } }
    );
    // Backend returns {items: [], pagination: {page, size, total, totalPages}}
    const items = response.data.items || [];
    const pagination = response.data.pagination || {};
    return {
      items: Array.isArray(items) ? items : [],
      total: pagination.total || 0,
      page: pagination.page || 0,
      pageSize: pagination.size || size,
      totalPages: pagination.totalPages || 0,
    };
  },

  /**
   * Get all voice calls for a business.
   */
  async getBusinessCalls(businessId: string, page: number = 0, size: number = 20): Promise<{
    items: VoiceCall[];
    total: number;
    page: number;
    pageSize: number;
    totalPages: number;
  }> {
    const response = await axios.get(
      `/businesses/${businessId}/voice-calls`,
      { params: { page, size } }
    );
    // Backend returns {items: [], pagination: {page, size, total, totalPages}}
    const items = response.data.items || [];
    const pagination = response.data.pagination || {};
    return {
      items: Array.isArray(items) ? items : [],
      total: pagination.total || 0,
      page: pagination.page || 0,
      pageSize: pagination.size || size,
      totalPages: pagination.totalPages || 0,
    };
  },

  /**
   * Get all voice calls across every business (global Voice Calls page).
   */
  async getAllCalls(page: number = 0, size: number = 20): Promise<{
    items: VoiceCall[];
    total: number;
    page: number;
    pageSize: number;
    totalPages: number;
  }> {
    const response = await axios.get(`/voice-calls`, { params: { page, size } });
    // Backend returns {items: [], pagination: {page, size, total, totalPages}}
    const items = response.data.items || [];
    const pagination = response.data.pagination || {};
    return {
      items: Array.isArray(items) ? items : [],
      total: pagination.total || 0,
      page: pagination.page || 0,
      pageSize: pagination.size || size,
      totalPages: pagination.totalPages || 0,
    };
  },

  /**
   * Get a specific voice call by ID.
   */
  async getCall(callId: string): Promise<VoiceCall> {
    const response = await axios.get(`/voice-calls/${callId}`);
    return response.data.data;
  },

  /**
   * Download a call recording through the CRM proxy as a playable blob.
   * Never expose the provider URL to the audio element.
   */
  async getRecording(callId: string): Promise<Blob> {
    const response = await axios.get(`/voice-calls/${callId}/recording`, {
      responseType: 'blob',
      timeout: 90000,
      headers: {
        Accept: 'audio/*,*/*',
      },
    });
    const rawType = response.headers['content-type'];
    const type = (typeof rawType === 'string' ? rawType.split(';')[0] : '') || 'audio/mpeg';
    const data = response.data;
    if (!(data instanceof Blob) || data.size === 0) {
      throw new Error('Recording could not be retrieved');
    }
    if (type.includes('application/json') || type.includes('text/')) {
      throw new Error('Recording could not be retrieved');
    }
    return data.type && data.type !== 'application/octet-stream'
      ? data
      : new Blob([data], { type });
  },
};
