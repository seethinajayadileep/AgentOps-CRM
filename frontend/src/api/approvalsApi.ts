import axios from 'axios';
import {
  Approval,
  ApprovalStatus,
  ApprovalType,
  FollowUpGenerateRequest,
  FollowUpGenerateResponse,
  ApprovalStatusUpdateRequest,
} from '../types/approval';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Generate follow-up messages for a lead
 */
export const generateFollowUpMessages = async (
  leadId: string,
  request: FollowUpGenerateRequest
): Promise<FollowUpGenerateResponse> => {
  const response = await axios.post<FollowUpGenerateResponse>(
    `${API_BASE_URL}/api/leads/${leadId}/follow-up/generate`,
    request
  );
  return response.data;
};

/**
 * Get all approvals with optional filters
 */
export const getAllApprovals = async (params?: {
  status?: ApprovalStatus;
  type?: ApprovalType;
  leadId?: string;
  businessId?: string;
}): Promise<Approval[]> => {
  const response = await axios.get<Approval[]>(`${API_BASE_URL}/api/approvals`, {
    params,
  });
  return response.data;
};

/**
 * Get a single approval by ID
 */
export const getApprovalById = async (id: string): Promise<Approval> => {
  const response = await axios.get<Approval>(`${API_BASE_URL}/api/approvals/${id}`);
  return response.data;
};

/**
 * Approve an approval
 */
export const approveApproval = async (id: string): Promise<Approval> => {
  const response = await axios.put<Approval>(`${API_BASE_URL}/api/approvals/${id}/approve`);
  return response.data;
};

/**
 * Reject an approval
 */
export const rejectApproval = async (id: string): Promise<Approval> => {
  const response = await axios.put<Approval>(`${API_BASE_URL}/api/approvals/${id}/reject`);
  return response.data;
};

/**
 * Update approval status
 */
export const updateApprovalStatus = async (
  id: string,
  request: ApprovalStatusUpdateRequest
): Promise<Approval> => {
  const response = await axios.put<Approval>(
    `${API_BASE_URL}/api/approvals/${id}/status`,
    request
  );
  return response.data;
};
