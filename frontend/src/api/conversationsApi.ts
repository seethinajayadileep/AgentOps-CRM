import apiClient from './axios';
import type {
  ConversationListItem,
  ConversationDetail,
  ConversationMessage,
  ConversationSummary,
  ConversationStatusUpdateRequest,
  ConversationFilters,
  PaginatedResponse,
} from '../types/conversation';

/**
 * Conversations API client.
 *
 * @version 0.3.0
 * Feature: F-009 - Conversations Admin Page
 */

/**
 * Get paginated list of conversations with filters
 */
export const getAllConversations = async (
  filters: ConversationFilters = {}
): Promise<PaginatedResponse<ConversationListItem>> => {
  const params = new URLSearchParams();

  if (filters.search) params.append('search', filters.search);
  if (filters.businessId) params.append('businessId', filters.businessId);
  if (filters.status) params.append('status', filters.status);
  if (filters.channel) params.append('channel', filters.channel);
  if (filters.leadCaptureStatus) params.append('leadCaptureStatus', filters.leadCaptureStatus);
  if (filters.startDate) params.append('startDate', filters.startDate);
  if (filters.endDate) params.append('endDate', filters.endDate);
  if (filters.page !== undefined) params.append('page', filters.page.toString());
  if (filters.size !== undefined) params.append('size', filters.size.toString());
  if (filters.sort) params.append('sort', filters.sort);

  const response = await apiClient.get<PaginatedResponse<ConversationListItem>>(
    `/conversations?${params.toString()}`
  );
  return response.data;
};

/**
 * Get conversation details by ID
 */
export const getConversationDetails = async (id: string): Promise<ConversationDetail> => {
  const response = await apiClient.get<ConversationDetail>(`/conversations/${id}`);
  return response.data;
};

/**
 * Get messages for a conversation
 */
export const getConversationMessages = async (
  id: string,
  page: number = 0,
  size: number = 50
): Promise<PaginatedResponse<ConversationMessage>> => {
  const response = await apiClient.get<PaginatedResponse<ConversationMessage>>(
    `/conversations/${id}/messages?page=${page}&size=${size}`
  );
  return response.data;
};

/**
 * Update conversation status
 */
export const updateConversationStatus = async (
  id: string,
  statusUpdate: ConversationStatusUpdateRequest
): Promise<ConversationDetail> => {
  const response = await apiClient.patch<ConversationDetail>(
    `/conversations/${id}/status`,
    statusUpdate
  );
  return response.data;
};

/**
 * Get conversation summary statistics
 */
export const getConversationSummary = async (): Promise<ConversationSummary> => {
  const response = await apiClient.get<ConversationSummary>('/conversations/summary');
  return response.data;
};

export const conversationsApi = {
  getAllConversations,
  getConversationDetails,
  getConversationMessages,
  updateConversationStatus,
  getConversationSummary,
};

export default conversationsApi;
