import { apiClient } from './axios';

export interface AskRequest {
  businessId: string;
  conversationId: string | null;
  question: string;
}

export type HallucinationRisk = 'LOW' | 'MEDIUM' | 'HIGH';

/**
 * Evaluation Agent verdict (F-008) attached to a chat answer.
 * May be null/undefined for messages that were not evaluated (e.g. greetings).
 */
export interface EvaluationSummary {
  hallucinationRisk: HallucinationRisk;
  safeToSend: boolean;
  reason: string;
}

export interface AskResponse {
  conversationId: string;
  answer: string;
  sources: string[];
  confidenceScore: number;
  leadDetected?: boolean;
  leadId?: string | null;
  evaluation?: EvaluationSummary | null;
}

export interface MessageResponse {
  id: string;
  role: string;
  content: string;
  createdAt: string;
}

export interface ConversationHistoryResponse {
  conversationId: string;
  messages: MessageResponse[];
}

/**
 * Ask a question to the support agent
 */
export const askQuestion = async (request: AskRequest): Promise<AskResponse> => {
  const response = await apiClient.post<AskResponse>('/chat/ask', request);
  return response.data;
};

/**
 * Get conversation history
 */
export const getConversationHistory = async (conversationId: string): Promise<ConversationHistoryResponse> => {
  const response = await apiClient.get<ConversationHistoryResponse>(
    `/chat/conversations/${conversationId}/messages`
  );
  return response.data;
};
