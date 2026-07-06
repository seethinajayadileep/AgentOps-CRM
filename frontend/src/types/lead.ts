// Lead status enum matching backend
export enum LeadStatus {
  NEW = 'NEW',
  QUALIFIED = 'QUALIFIED',
  HOT = 'HOT',
  COLD = 'COLD',
  FOLLOWED_UP = 'FOLLOWED_UP',
  CLOSED = 'CLOSED',
}

// Lead interface
export interface Lead {
  id: string;
  businessId: string;
  businessName?: string;
  conversationId?: string;
  name: string;
  email?: string;
  phone?: string;
  requirementText?: string;
  budget?: number;
  urgency?: string;
  timeline?: string;
  leadScore?: number;
  summary?: string;
  status: LeadStatus;
  createdAt: string;
  updatedAt: string;
}

// Lead qualification request
export interface LeadQualificationRequest {
  businessId: string;
  conversationId?: string;
  message: string;
}

// Lead qualification response
export interface LeadQualificationResponse {
  leadId: string;
  name?: string;
  email?: string;
  phone?: string;
  requirementText?: string;
  budget?: string;
  urgency?: string;
  timeline?: string;
  leadScore?: number;
  status: LeadStatus;
  summary?: string;
}

// Lead status update request
export interface LeadStatusUpdateRequest {
  status: LeadStatus;
}

// Extended chat response with lead detection
export interface ChatResponseWithLead {
  conversationId: string;
  answer: string;
  sources: string[];
  confidenceScore: number;
  leadDetected?: boolean;
  leadId?: string;
}
