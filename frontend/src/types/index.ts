/**
 * Common TypeScript types for the AgentOps CRM frontend.
 *
 * @version 0.1.0
 */

// Health Response
export interface HealthResponse {
  status: string;
  timestamp: string;
  services: Record<string, ServiceStatus>;
  version: string;
}

export interface ServiceStatus {
  status: string;
  message: string;
}

// Business
export interface Business {
  id: string;
  name: string;
  websiteUrl: string;
  industry?: string;
  description?: string;
  contactEmail?: string;
  contactPhone?: string;
  crawlStatus: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  updatedAt?: string;
}

// Create/Update Business DTOs
export interface CreateBusinessRequest {
  name: string;
  websiteUrl: string;
  industry?: string;
  description?: string;
  contactEmail?: string;
  contactPhone?: string;
}

export interface UpdateBusinessRequest {
  name?: string;
  websiteUrl?: string;
  industry?: string;
  description?: string;
  contactEmail?: string;
  contactPhone?: string;
}

// Lead
export interface Lead {
  id: string;
  businessId: string;
  customerEmail?: string;
  customerPhone?: string;
  score: number;
  status: 'NEW' | 'WARM' | 'HOT' | 'QUALIFIED' | 'CONVERTED';
  createdAt: string;
  updatedAt: string;
}

// Conversation
export interface Conversation {
  id: string;
  businessId: string;
  customerEmail?: string;
  status: 'ACTIVE' | 'CLOSED';
  messages: Message[];
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
}

// Voice Call
export interface VoiceCall {
  id: string;
  leadId: string;
  phoneNumber: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  transcript?: string;
  summary?: string;
  duration?: number;
  createdAt: string;
  completedAt?: string;
}

// Approval
export interface Approval {
  id: string;
  leadId: string;
  lead: Lead;
  phoneNumber: string;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
  reviewedAt?: string;
  reviewedBy?: string;
}

// Agent Log
export interface AgentLog {
  id: string;
  agentType: string;
  action: string;
  input?: string;
  output?: string;
  status: 'SUCCESS' | 'ERROR' | 'PARTIAL';
  errorMessage?: string;
  timestamp: string;
  duration?: number;
}

// API Response Wrapper
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

// Pagination
export interface PaginationMeta {
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface PaginatedResponse<T> {
  items: T[];
  pagination: PaginationMeta;
}