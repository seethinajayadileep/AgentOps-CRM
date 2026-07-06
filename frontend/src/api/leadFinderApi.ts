import axios from './axios';
import type {
  LeadSourceRun,
  DiscoveredLead,
  StartLeadFinderRunRequest,
  BulkImportResult,
  LeadFinderConfig,
} from '../types/leadFinder';

/**
 * API client for the Apify Lead Finder (F-010).
 * The Apify token lives only on the backend; it is never referenced here.
 */
export const leadFinderApi = {
  // Report whether Apify is configured (drives the "Apify not configured" UI state).
  async getConfig(): Promise<LeadFinderConfig> {
    const response = await axios.get('/lead-finder/config');
    return response.data.data;
  },

  // Start a new lead discovery run.
  async startRun(request: StartLeadFinderRunRequest): Promise<LeadSourceRun> {
    const response = await axios.post('/lead-finder/runs', request);
    return response.data.data;
  },

  // List all runs.
  async listRuns(): Promise<LeadSourceRun[]> {
    const response = await axios.get('/lead-finder/runs');
    return response.data.data || [];
  },

  // Get one run.
  async getRun(id: string): Promise<LeadSourceRun> {
    const response = await axios.get(`/lead-finder/runs/${id}`);
    return response.data.data;
  },

  // Get discovered leads for a run.
  async getResults(id: string): Promise<DiscoveredLead[]> {
    const response = await axios.get(`/lead-finder/runs/${id}/results`);
    return response.data.data || [];
  },

  // Sync/fetch results from Apify.
  async syncRun(id: string): Promise<LeadSourceRun> {
    const response = await axios.post(`/lead-finder/runs/${id}/sync`);
    return response.data.data;
  },

  // Import a single discovered lead.
  async importLead(id: string, targetBusinessId?: string): Promise<DiscoveredLead> {
    const response = await axios.post(`/lead-finder/discovered-leads/${id}/import`, {
      targetBusinessId: targetBusinessId ?? null,
    });
    return response.data.data;
  },

  // Import multiple discovered leads.
  async importBulk(discoveredLeadIds: string[], targetBusinessId?: string): Promise<BulkImportResult> {
    const response = await axios.post('/lead-finder/discovered-leads/import-bulk', {
      discoveredLeadIds,
      targetBusinessId: targetBusinessId ?? null,
    });
    return response.data.data;
  },

  // Reject a discovered lead.
  async rejectLead(id: string): Promise<DiscoveredLead> {
    const response = await axios.post(`/lead-finder/discovered-leads/${id}/reject`);
    return response.data.data;
  },
};
