export enum ApprovalType {
  FOLLOW_UP_MESSAGE = 'FOLLOW_UP_MESSAGE',
  OUTBOUND_CALL = 'OUTBOUND_CALL',
  OUTREACH_MESSAGE = 'OUTREACH_MESSAGE',
}

export enum ApprovalStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

export interface Approval {
  approvalId: string;
  type: ApprovalType;
  status: ApprovalStatus;
  style?: string; // PROFESSIONAL, FRIENDLY, SHORT_WHATSAPP
  content: string;
  leadId?: string;
  leadName?: string;
  businessId?: string;
  businessName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface FollowUpGenerateRequest {
  tone: string; // ALL, PROFESSIONAL, FRIENDLY, SHORT_WHATSAPP
}

export interface FollowUpGenerateResponse {
  leadId: string;
  approvals: Approval[];
}

export interface ApprovalStatusUpdateRequest {
  status: ApprovalStatus;
}
