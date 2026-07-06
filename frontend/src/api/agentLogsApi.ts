import axios from './axios';
import { AgentLog, AgentLogSummary, AgentActionStatus } from '../types/agentLog';

/**
 * API client for agent logs.
 *
 * @version 0.3.0
 * Feature: F-012 - Agent Logs Observability
 */

export interface AgentLogsFilters {
  search?: string;
  agentName?: string;
  action?: string;
  status?: AgentActionStatus;
  businessId?: string;
  startDate?: string; // ISO 8601 format
  endDate?: string; // ISO 8601 format
  page?: number;
  size?: number;
  sort?: string;
}

export const agentLogsApi = {
  /**
   * Get all agent logs with filtering and pagination.
   */
  async getAllLogs(filters: AgentLogsFilters = {}): Promise<{
    items: AgentLog[];
    total: number;
    page: number;
    pageSize: number;
    totalPages: number;
  }> {
    const params: any = {
      page: filters.page ?? 0,
      size: filters.size ?? 20,
    };

    if (filters.search) params.search = filters.search;
    if (filters.agentName) params.agentName = filters.agentName;
    if (filters.action) params.action = filters.action;
    if (filters.status) params.status = filters.status;
    if (filters.businessId) params.businessId = filters.businessId;
    if (filters.startDate) params.startDate = filters.startDate;
    if (filters.endDate) params.endDate = filters.endDate;
    if (filters.sort) params.sort = filters.sort;

    const response = await axios.get('/agent-logs', { params });
    
    const items = response.data.items || [];
    const pagination = response.data.pagination || {};
    
    return {
      items: Array.isArray(items) ? items : [],
      total: pagination.total || 0,
      page: pagination.page || 0,
      pageSize: pagination.size || filters.size || 20,
      totalPages: pagination.totalPages || 0,
    };
  },

  /**
   * Get a specific agent log by ID.
   */
  async getLogById(id: string): Promise<AgentLog> {
    const response = await axios.get(`/agent-logs/${id}`);
    return response.data.data;
  },

  /**
   * Get summary statistics.
   */
  async getSummary(): Promise<AgentLogSummary> {
    const response = await axios.get('/agent-logs/summary');
    return response.data.data;
  },
};
