import apiClient from './axios';
import type {
  SystemHealthResponse,
  IntegrationsResponse,
  ModelsConfigResponse,
  RagConfigResponse,
  VoiceConfigResponse,
  AgentsResponse,
  SystemDiagnosticsResponse,
  IntegrationTestResult,
} from '../types/settings';

/**
 * API client for system settings and diagnostics.
 *
 * @version 0.1.0
 * Feature: Settings Page Implementation
 */

/**
 * Get system health overview.
 */
export async function getSystemHealth(): Promise<SystemHealthResponse> {
  const response = await apiClient.get<SystemHealthResponse>('/settings/overview');
  return response.data;
}

/**
 * Get all integrations status.
 */
export async function getIntegrations(): Promise<IntegrationsResponse> {
  const response = await apiClient.get<IntegrationsResponse>('/settings/integrations');
  return response.data;
}

/**
 * Get AI models configuration.
 */
export async function getModelsConfig(): Promise<ModelsConfigResponse> {
  const response = await apiClient.get<ModelsConfigResponse>('/settings/models');
  return response.data;
}

/**
 * Get RAG/Knowledge Base configuration and metrics.
 */
export async function getRagConfig(): Promise<RagConfigResponse> {
  const response = await apiClient.get<RagConfigResponse>('/settings/rag');
  return response.data;
}

/**
 * Get Voice AI (Vapi) configuration and metrics.
 */
export async function getVoiceConfig(): Promise<VoiceConfigResponse> {
  const response = await apiClient.get<VoiceConfigResponse>('/settings/voice');
  return response.data;
}

/**
 * Get agents readiness and safety configuration.
 */
export async function getAgentsConfig(): Promise<AgentsResponse> {
  const response = await apiClient.get<AgentsResponse>('/settings/agents');
  return response.data;
}

/**
 * Get system diagnostics.
 */
export async function getSystemDiagnostics(): Promise<SystemDiagnosticsResponse> {
  const response = await apiClient.get<SystemDiagnosticsResponse>('/settings/system');
  return response.data;
}

/**
 * Test an integration connection.
 *
 * @param integration - Integration name (e.g., "database", "openai", "firecrawl", "apify", "vapi", "redis")
 */
export async function testIntegration(integration: string): Promise<IntegrationTestResult> {
  const response = await apiClient.post<IntegrationTestResult>(
    `/settings/integrations/${integration}/test`
  );
  return response.data;
}
