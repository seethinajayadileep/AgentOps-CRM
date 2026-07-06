/**
 * Agent log types for observability.
 *
 * @version 0.3.0
 * Feature: F-012 - Agent Logs Observability
 */

export enum AgentActionStatus {
  SUCCESS = 'SUCCESS',
  PARTIAL = 'PARTIAL',
  ERROR = 'ERROR',
  FAILED = 'FAILED',
  FALLBACK_USED = 'FALLBACK_USED',
}

export interface AgentLog {
  id: string;
  agentName: string;
  action: string;
  status: AgentActionStatus;
  durationMs?: number;
  createdAt: string;
  // Related entities
  businessId?: string;
  businessName?: string;
  leadId?: string;
  leadName?: string;
  conversationId?: string;
  // Execution details
  inputJson?: string;
  outputJson?: string;
  errorMessage?: string;
}

export interface AgentLogSummary {
  executionsToday: number;
  successRate: number;
  errorCount: number;
  averageDurationMs: number;
}
