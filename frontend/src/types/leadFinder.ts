// F-010 Apify Lead Finder types

export type LeadSourceRunStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export type DiscoveredLeadStatus = 'NEW' | 'REVIEWED' | 'IMPORTED' | 'REJECTED';

export interface LeadSourceRun {
  id: string;
  searchName: string;
  industry?: string;
  location?: string;
  keywords?: string;
  actorId?: string;
  apifyRunId?: string;
  maxResults?: number;
  status: LeadSourceRunStatus;
  totalResults: number;
  importedCount: number;
  failureReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DiscoveredLead {
  id: string;
  leadSourceRunId: string;
  businessName?: string;
  websiteUrl?: string;
  contactName?: string;
  email?: string;
  phone?: string;
  location?: string;
  industry?: string;
  sourceUrl?: string;
  rawDataJson?: string;
  score?: number;
  status: DiscoveredLeadStatus;
  importedLeadId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface StartLeadFinderRunRequest {
  searchName: string;
  industry?: string;
  location?: string;
  keywords?: string;
  actorId?: string;
  maxResults?: number;
}

export interface BulkImportResult {
  requested: number;
  imported: number;
  skippedDuplicates: number;
  failed: number;
  messages: string[];
}

export interface LeadFinderConfig {
  apifyConfigured: boolean;
}
