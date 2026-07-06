/**
 * Conversation types matching backend DTOs.
 *
 * @version 0.3.0
 * Feature: F-009 - Conversations Admin Page
 */

// Conversation status enum
export enum ConversationStatus {
  ACTIVE = 'ACTIVE',
  PAUSED = 'PAUSED',
  CLOSED = 'CLOSED',
  ARCHIVED = 'ARCHIVED',
}

// Channel enum
export enum Channel {
  WEB_WIDGET = 'WEB_WIDGET',
  EMAIL = 'EMAIL',
  PHONE = 'PHONE',
  SMS = 'SMS',
  WHATSAPP = 'WHATSAPP',
}

// Lead capture status values
export type LeadCaptureStatus = 'AWAITING_DETAILS' | 'COLLECTING_DETAILS' | 'CAPTURED' | null;

// Message role enum
export enum MessageRole {
  USER = 'USER',
  ASSISTANT = 'ASSISTANT',
  SYSTEM = 'SYSTEM',
}

// Conversation list item
export interface ConversationListItem {
  id: string;
  businessId: string;
  businessName: string;
  customerName?: string;
  customerEmail?: string;
  customerPhone?: string;
  channel: Channel;
  status: ConversationStatus;
  summary?: string;
  leadCaptureStatus?: LeadCaptureStatus;
  messageCount: number;
  leadCount: number;
  latestMessagePreview?: string;
  latestMessageRole?: MessageRole;
  latestMessageAt?: string;
  createdAt: string;
  updatedAt: string;
}

// Related lead summary (in conversation detail)
export interface RelatedLead {
  id: string;
  name: string;
  email?: string;
  status: string;
  leadScore?: number;
}

// Conversation detail
export interface ConversationDetail {
  id: string;
  businessId: string;
  businessName: string;
  customerName?: string;
  customerEmail?: string;
  customerPhone?: string;
  channel: Channel;
  status: ConversationStatus;
  summary?: string;
  leadCaptureStatus?: LeadCaptureStatus;
  pendingLeadName?: string;
  pendingLeadEmail?: string;
  pendingLeadPhone?: string;
  pendingLeadRequirement?: string;
  relatedLeads: RelatedLead[];
  voiceCallCount: number;
  createdAt: string;
  updatedAt: string;
}

// Conversation message
export interface ConversationMessage {
  id: string;
  role: MessageRole;
  content: string;
  createdAt: string;
}

// Conversation summary stats
export interface ConversationSummary {
  totalConversations: number;
  activeConversations: number;
  conversationsToday: number;
  leadsCaptured: number;
  averageMessagesPerConversation: number;
}

// Paginated response
export interface PaginatedResponse<T> {
  items: T[];
  pagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// Status update request
export interface ConversationStatusUpdateRequest {
  status: ConversationStatus;
}

// Query filters for list endpoint
export interface ConversationFilters {
  search?: string;
  businessId?: string;
  status?: ConversationStatus;
  channel?: Channel;
  leadCaptureStatus?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}
