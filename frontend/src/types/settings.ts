/**
 * Settings-related types for system configuration and diagnostics.
 *
 * @version 0.1.0
 * Feature: Settings Page Implementation
 */

/**
 * Readiness status for integrations and system components.
 */
export enum ReadinessStatus {
  HEALTHY = 'HEALTHY',
  CONFIGURED = 'CONFIGURED',
  NOT_CONFIGURED = 'NOT_CONFIGURED',
  DISABLED = 'DISABLED',
  DEGRADED = 'DEGRADED',
  ERROR = 'ERROR',
  UNKNOWN = 'UNKNOWN',
}

/**
 * System health overview response.
 */
export interface SystemHealthResponse {
  applicationName: string;
  applicationVersion: string;
  activeProfile: string;
  environment: string;
  serverTime: string;
  lastHealthCheck: string;
  components: Record<string, ReadinessStatus>;
}

/**
 * Individual integration status.
 */
export interface IntegrationStatus {
  name: string;
  purpose: string;
  status: ReadinessStatus;
  configured: boolean;
  enabled: boolean;
  message: string;
  lastChecked: string;
  configDetails: string;
}

/**
 * All integrations overview.
 */
export interface IntegrationsResponse {
  integrations: IntegrationStatus[];
}

/**
 * AI models configuration.
 */
export interface ModelsConfigResponse {
  ragAnswerModel: string;
  evaluationModel: string;
  leadQualificationModel: string;
  followUpModel: string;
  embeddingProvider: string;
  embeddingModel: string;
  embeddingDimension: number;
  configNote: string;
}

/**
 * RAG/Knowledge Base configuration and metrics.
 */
export interface RagConfigResponse {
  embeddingProvider: string;
  embeddingModel: string;
  embeddingDimension: number;
  vectorStoreStrategy: string;
  defaultTopK: number;
  maxTopK: number;
  totalBusinesses: number;
  businessesWithDocuments: number;
  businessesWithKnowledge: number;
  totalDocuments: number;
  totalKnowledgeChunks: number;
  vectorStoreWarning?: string;
}

/**
 * Voice AI (Vapi) configuration and metrics.
 */
export interface VoiceConfigResponse {
  enabled: boolean;
  apiKeyConfigured: boolean;
  assistantIdConfigured: boolean;
  phoneNumberIdConfigured: boolean;
  webhookSecretConfigured: boolean;
  webhookEndpoint: string;
  status: ReadinessStatus;
  statusMessage: string;
  totalCalls: number;
  successfulCalls: number;
  failedCalls: number;
  lastSuccessfulCall?: string;
  lastFailedCall?: string;
}

/**
 * Individual agent status.
 */
export interface AgentStatus {
  name: string;
  status: ReadinessStatus;
  message: string;
  requiredIntegration?: string;
  currentModel?: string;
  fallbackAvailable?: boolean;
}

/**
 * Safety configuration settings.
 */
export interface SafetyConfig {
  evaluationEnabled: boolean;
  unsafeAnswerBlocking: boolean;
  fallbackAnswerAvailable: boolean;
  humanApprovalEnabled: boolean;
  humanApprovalForVoice: boolean;
  leadCaptureBehavior: string;
}

/**
 * Agents readiness and safety configuration.
 */
export interface AgentsResponse {
  agents: AgentStatus[];
  safetyConfig: SafetyConfig;
}

/**
 * System warning.
 */
export interface SystemWarning {
  type: string;
  title: string;
  recommendation: string;
}

/**
 * System diagnostics response.
 */
export interface SystemDiagnosticsResponse {
  applicationName: string;
  applicationVersion: string;
  backendVersion: string;
  activeProfile: string;
  apiBasePath: string;
  serverTimezone: string;
  databaseType: string;
  redisConfigured: boolean;
  flywayEnabled: boolean;
  hibernateSchemaMode: string;
  vectorStoreStrategy: string;
  warnings: SystemWarning[];
}

/**
 * Integration test result.
 */
export interface IntegrationTestResult {
  integration: string;
  success: boolean;
  status: ReadinessStatus;
  message: string;
  testedAt: string;
  durationMs: number;
}
