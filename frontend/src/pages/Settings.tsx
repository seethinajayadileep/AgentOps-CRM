import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { RefreshCw, AlertTriangle, CheckCircle, XCircle, Copy, ExternalLink } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import LoadingState from '../components/ui/LoadingState';
import ReadinessBadge from '../components/settings/ReadinessBadge';
import {
  getSystemHealth,
  getIntegrations,
  getModelsConfig,
  getRagConfig,
  getVoiceConfig,
  getAgentsConfig,
  getSystemDiagnostics,
  testIntegration,
} from '../api/settingsApi';
import type {
  SystemHealthResponse,
  IntegrationsResponse,
  ModelsConfigResponse,
  RagConfigResponse,
  VoiceConfigResponse,
  AgentsResponse,
  SystemDiagnosticsResponse,
  IntegrationStatus,
  IntegrationTestResult,
  ReadinessStatus,
} from '../types/settings';
import { formatServerDateTime } from '../util/serverDate';

/**
  * Comprehensive Settings page for system configuration, integration readiness and diagnostics.
  * 
  * @version 0.1.0
  * Feature: F-013 - Production Settings Page
  */

type TabName = 'overview' | 'integrations' | 'models' | 'rag' | 'voice' | 'agents' | 'system';

export default function Settings() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = (searchParams.get('tab') as TabName) || 'overview';

  const setActiveTab = (tab: TabName) => {
    setSearchParams({ tab });
  };

  return (
    <div>
      <PageHeader 
        title="Settings" 
        subtitle="System configuration, integration readiness and diagnostics" 
      />

      {/* Tabs Navigation */}
      <div className="mb-6 overflow-x-auto">
        <div className="flex gap-2 border-b border-frost">
          {(['overview', 'integrations', 'models', 'rag', 'voice', 'agents', 'system'] as TabName[]).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`whitespace-nowrap px-4 py-3 text-sm font-medium transition-colors ${
                activeTab === tab
                  ? 'border-b-2 border-ink text-ink'
                  : 'text-slate hover:text-ink'
              }`}
            >
              {tab.charAt(0).toUpperCase() + tab.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {/* Tab Content */}
      {activeTab === 'overview' && <OverviewTab />}
      {activeTab === 'integrations' && <IntegrationsTab />}
      {activeTab === 'models' && <ModelsTab />}
      {activeTab === 'rag' && <RagTab />}
      {activeTab === 'voice' && <VoiceTab />}
      {activeTab === 'agents' && <AgentsTab />}
      {activeTab === 'system' && <SystemTab />}
    </div>
  );
}

// ===== Overview Tab =====

function OverviewTab() {
  const [data, setData] = useState<SystemHealthResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getSystemHealth();
      setData(response);
    } catch (err: any) {
      setError(err.message || 'Failed to load system health');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) return <LoadingState label="Loading system health..." />;
  if (error) return <ErrorState message={error} onRetry={loadData} />;
  if (!data) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-ink">System Health Overview</h2>
          <p className="mt-1 text-sm text-slate">
            {data.applicationName} v{data.applicationVersion} • {data.activeProfile} environment
          </p>
        </div>
        <Button variant="secondary" onClick={loadData}>
          <RefreshCw size={16} className="mr-2" />
          Refresh
        </Button>
      </div>

      {/* Health Cards Grid */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
        {Object.entries(data.components).map(([name, status]) => (
          <Card key={name} className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <StatusIcon status={status} />
                <span className="ml-3 font-medium capitalize text-ink">{name}</span>
              </div>
              <ReadinessBadge status={status} />
            </div>
          </Card>
        ))}
      </div>

      {/* System Info */}
      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-ink">System Information</h3>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <InfoRow label="Server Time" value={formatDateTime(data.serverTime)} />
          <InfoRow label="Last Health Check" value={formatDateTime(data.lastHealthCheck)} />
          <InfoRow label="Environment" value={data.environment} />
          <InfoRow label="Active Profile" value={data.activeProfile} />
        </div>
      </Card>
    </div>
  );
}

// ===== Integrations Tab =====

function IntegrationsTab() {
  const [data, setData] = useState<IntegrationsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [testResults, setTestResults] = useState<Record<string, IntegrationTestResult>>({});
  const [testing, setTesting] = useState<Record<string, boolean>>({});

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getIntegrations();
      setData(response);
    } catch (err: any) {
      setError(err.message || 'Failed to load integrations');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleTest = async (integration: IntegrationStatus) => {
    const key = integration.name.toLowerCase().replace(/\s+/g, '');
    setTesting((prev) => ({ ...prev, [key]: true }));
    try {
      const result = await testIntegration(key);
      setTestResults((prev) => ({ ...prev, [key]: result }));
    } catch (err: any) {
      setTestResults((prev) => ({
        ...prev,
        [key]: {
          integration: integration.name,
          success: false,
          status: 'ERROR' as ReadinessStatus,
          message: err.message || 'Test failed',
          testedAt: new Date().toISOString(),
          durationMs: 0,
        },
      }));
    } finally {
      setTesting((prev) => ({ ...prev, [key]: false }));
    }
  };

  if (loading) return <LoadingState label="Loading integrations..." />;
  if (error) return <ErrorState message={error} onRetry={loadData} />;
  if (!data) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-ink">Integration Readiness</h2>
        <Button variant="secondary" onClick={loadData}>
          <RefreshCw size={16} className="mr-2" />
          Refresh
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {data.integrations.map((integration) => {
          const key = integration.name.toLowerCase().replace(/\s+/g, '');
          const testResult = testResults[key];
          const isTesting = testing[key];

          return (
            <Card key={integration.name} className="p-6">
              <div className="mb-4 flex items-start justify-between">
                <div>
                  <h3 className="text-lg font-semibold text-ink">{integration.name}</h3>
                  <p className="mt-1 text-sm text-slate">{integration.purpose}</p>
                </div>
                <ReadinessBadge status={integration.status} />
              </div>

              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-slate">Configured:</span>
                  <span className="text-ink">{integration.configured ? 'Yes' : 'No'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate">Enabled:</span>
                  <span className="text-ink">{integration.enabled ? 'Yes' : 'No'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate">Status:</span>
                  <span className="text-ink">{integration.message}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate">Last Checked:</span>
                  <span className="text-ink">{formatDateTime(integration.lastChecked)}</span>
                </div>
              </div>

              <div className="mt-4 rounded-lg bg-mist p-3">
                <p className="text-xs text-slate">{integration.configDetails}</p>
              </div>

              {integration.configured && integration.name !== 'Redis' && (
                <Button
                  variant="ghost"
                  onClick={() => handleTest(integration)}
                  disabled={isTesting}
                  className="mt-4 w-full"
                >
                  {isTesting
                    ? 'Testing...'
                    : testResult?.checkType === 'CONFIGURATION_ONLY'
                      ? 'Check configuration'
                      : 'Test Connection'}
                </Button>
              )}

              {testResult && (
                <div className="mt-4 rounded-lg bg-mist p-3">
                  <div className="mb-2">
                    <ReadinessBadge status={testResult.status} />
                  </div>
                  <p className="text-sm text-ink">{testResult.message}</p>
                  {testResult.checkType === 'CONFIGURATION_ONLY' && (
                    <p className="mt-1 text-xs text-slate">Configuration check only — no live provider call.</p>
                  )}
                  <p className="mt-1 text-xs text-slate">
                    Duration: {testResult.durationMs}ms • {formatDateTime(testResult.testedAt)}
                  </p>
                </div>
              )}
            </Card>
          );
        })}
      </div>
    </div>
  );
}

// ===== AI Models Tab =====

function ModelsTab() {
  const [data, setData] = useState<ModelsConfigResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getModelsConfig();
      setData(response);
    } catch (err: any) {
      setError(err.message || 'Failed to load models configuration');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) return <LoadingState label="Loading AI models configuration..." />;
  if (error) return <ErrorState message={error} onRetry={loadData} />;
  if (!data) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-ink">AI Models Configuration</h2>
        <Button variant="secondary" onClick={loadData}>
          <RefreshCw size={16} className="mr-2" />
          Refresh
        </Button>
      </div>

      <Card className="p-6">
        <div className="mb-4 rounded-lg bg-mist p-4">
          <p className="text-sm text-blue-200">{data.configNote}</p>
        </div>

        <h3 className="mb-4 text-lg font-semibold text-ink">Agent Models</h3>
        <div className="space-y-3">
          <InfoRow label="RAG Answer Model" value={data.ragAnswerModel} />
          <InfoRow label="Evaluation Agent Model" value={data.evaluationModel} />
          <InfoRow label="Lead Qualification Model" value={data.leadQualificationModel} />
          <InfoRow label="Follow-up Agent Model" value={data.followUpModel} />
        </div>

        <h3 className="mb-4 mt-6 text-lg font-semibold text-ink">Embeddings</h3>
        <div className="space-y-3">
          <InfoRow label="Embedding Provider" value={data.embeddingProvider} />
          <InfoRow label="Embedding Model" value={data.embeddingModel} />
          <InfoRow label="Embedding Dimension" value={data.embeddingDimension.toString()} />
        </div>
      </Card>
    </div>
  );
}

// ===== RAG Tab =====

function RagTab() {
  const [data, setData] = useState<RagConfigResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getRagConfig();
      setData(response);
    } catch (err: any) {
      setError(err.message || 'Failed to load RAG configuration');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) return <LoadingState label="Loading RAG configuration..." />;
  if (error) return <ErrorState message={error} onRetry={loadData} />;
  if (!data) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-ink">Knowledge & RAG Configuration</h2>
        <Button variant="secondary" onClick={loadData}>
          <RefreshCw size={16} className="mr-2" />
          Refresh
        </Button>
      </div>

      {data.vectorStoreWarning && (
        <div className="rounded-lg border border-amber-500/20 bg-amber-500/10 p-4">
          <div className="flex items-start">
            <AlertTriangle size={20} className="mr-3 mt-0.5 text-amber-400" />
            <div>
              <h4 className="font-semibold text-amber-200">Vector Storage Warning</h4>
              <p className="mt-1 text-sm text-amber-300">{data.vectorStoreWarning}</p>
            </div>
          </div>
        </div>
      )}

      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-ink">Configuration</h3>
        <div className="space-y-3">
          <InfoRow label="Embedding Provider" value={data.embeddingProvider} />
          <InfoRow label="Embedding Model" value={data.embeddingModel} />
          <InfoRow label="Embedding Dimension" value={data.embeddingDimension.toString()} />
          <InfoRow label="Vector Store Strategy" value={data.vectorStoreStrategy} />
          <InfoRow label="Default Top-K" value={data.defaultTopK.toString()} />
          <InfoRow label="Maximum Top-K" value={data.maxTopK.toString()} />
        </div>
      </Card>

      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-ink">Knowledge Base Metrics</h3>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          <MetricCard label="Total Businesses" value={data.totalBusinesses} />
          <MetricCard label="With Documents" value={data.businessesWithDocuments} />
          <MetricCard label="With Knowledge" value={data.businessesWithKnowledge} />
          <MetricCard label="Total Documents" value={data.totalDocuments} />
          <MetricCard label="Knowledge Chunks" value={data.totalKnowledgeChunks} />
        </div>

        <div className="mt-6 flex gap-3">
          <Link to="/businesses">
            <Button variant="ghost">
              <ExternalLink size={16} className="mr-2" />
              View Businesses
            </Button>
          </Link>
          <Link to="/agent-logs">
            <Button variant="ghost">
              <ExternalLink size={16} className="mr-2" />
              View Agent Logs
            </Button>
          </Link>
        </div>
      </Card>
    </div>
  );
}

// ===== Voice AI Tab =====

function VoiceTab() {
  const [data, setData] = useState<VoiceConfigResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [retrying, setRetrying] = useState(false);

  const loadData = async () => {
    if (retrying) return; // Prevent duplicate requests
    
    setLoading(true);
    setError(null);
    setRetrying(true);
    
    try {
      const response = await getVoiceConfig();
      setData(response);
    } catch (err: any) {
      setError(err.message || 'Failed to load voice configuration');
    } finally {
      setLoading(false);
      setRetrying(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const copyWebhookUrl = () => {
    if (data && data.webhookUrl) {
      navigator.clipboard.writeText(data.webhookUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  if (loading) return <LoadingState label="Loading voice configuration..." />;
  if (error) return <ErrorState message={error} onRetry={loadData} />;
  if (!data) return null;

  // Helper to render state-specific information banner
  const renderStatusBanner = () => {
    if (data.status === 'DISABLED') {
      return (
        <div className="rounded-lg border border-frost bg-mist/50 p-4">
          <div className="flex items-start">
            <AlertTriangle size={20} className="mr-3 mt-0.5 text-slate" />
            <div>
              <h4 className="font-semibold text-ink">Voice calling is disabled</h4>
              <p className="mt-1 text-sm text-slate">{data.statusMessage}</p>
            </div>
          </div>
        </div>
      );
    }
    
    if (data.status === 'NOT_CONFIGURED') {
      return (
        <div className="rounded-lg border border-amber-500/20 bg-amber-500/10 p-4">
          <div className="flex items-start">
            <AlertTriangle size={20} className="mr-3 mt-0.5 text-amber-400" />
            <div>
              <h4 className="font-semibold text-amber-200">Configuration incomplete</h4>
              <p className="mt-1 text-sm text-amber-300">{data.statusMessage}</p>
            </div>
          </div>
        </div>
      );
    }
    
    if (data.status === 'CONFIGURED' || data.status === 'HEALTHY') {
      return (
        <div className="rounded-lg border border-frost bg-mist p-4">
          <div className="flex items-start">
            <CheckCircle size={20} className="mr-3 mt-0.5 text-ink" />
            <div>
              <h4 className="font-semibold text-ink">Voice AI is ready</h4>
              <p className="mt-1 text-sm text-slate">{data.statusMessage}</p>
            </div>
          </div>
        </div>
      );
    }
    
    if (data.status === 'DEGRADED') {
      return (
        <div className="rounded-lg border border-amber-500/20 bg-amber-500/10 p-4">
          <div className="flex items-start">
            <AlertTriangle size={20} className="mr-3 mt-0.5 text-amber-400" />
            <div>
              <h4 className="font-semibold text-amber-200">Service degraded</h4>
              <p className="mt-1 text-sm text-amber-300">{data.statusMessage}</p>
            </div>
          </div>
        </div>
      );
    }
    
    if (data.status === 'ERROR') {
      return (
        <div className="rounded-lg border border-red-500/20 bg-mist p-4">
          <div className="flex items-start">
            <XCircle size={20} className="mr-3 mt-0.5 text-ink" />
            <div className="flex-1">
              <h4 className="font-semibold text-ink">Service error</h4>
              <p className="mt-1 text-sm text-ink">{data.statusMessage}</p>
              <Button 
                variant="ghost" 
                onClick={loadData} 
                disabled={retrying}
                className="mt-3 text-sm"
              >
                <RefreshCw size={14} className="mr-2" />
                {retrying ? 'Retrying...' : 'Retry'}
              </Button>
            </div>
          </div>
        </div>
      );
    }
    
    return null;
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-ink">Voice AI Configuration</h2>
        <Button variant="secondary" onClick={loadData} disabled={retrying}>
          <RefreshCw size={16} className="mr-2" />
          Refresh
        </Button>
      </div>

      {renderStatusBanner()}

      <Card className="p-6">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-ink">Vapi Readiness</h3>
          <ReadinessBadge status={data.status} />
        </div>

        <div className="space-y-3">
          <InfoRow label="Enabled" value={data.enabled ? 'Yes' : 'No'} />
          <InfoRow label="API Key Configured" value={data.apiKeyConfigured ? 'Yes' : 'No'} />
          <InfoRow label="Assistant ID Configured" value={data.assistantIdConfigured ? 'Yes' : 'No'} />
          <InfoRow label="Phone Number ID Configured" value={data.phoneNumberIdConfigured ? 'Yes' : 'No'} />
          <InfoRow label="Webhook Secret Configured" value={data.webhookSecretConfigured ? 'Yes' : 'No'} />
        </div>

        <div className="mt-4">
          <label className="mb-2 block text-sm font-medium text-slate">Webhook Endpoint</label>
          <div className="flex items-center gap-2">
            <input
              type="text"
              value={data.webhookUrl || ''}
              readOnly
              className="input-dark flex-1"
            />
            <Button variant="ghost" onClick={copyWebhookUrl}>
              <Copy size={16} />
            </Button>
          </div>
          {copied && <p className="mt-1 text-xs text-slate">Copied to clipboard!</p>}
        </div>
      </Card>

      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-ink">Voice Call Metrics</h3>
        
        {!data.metricsAvailable && (
          <div className="mb-4 rounded-lg border border-amber-500/20 bg-amber-500/10 p-3">
            <div className="flex items-start">
              <AlertTriangle size={16} className="mr-2 mt-0.5 text-amber-400" />
              <p className="text-sm text-amber-300">{data.metricsMessage}</p>
            </div>
          </div>
        )}
        
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <MetricCard label="Total Calls" value={data.totalCalls} />
          <MetricCard label="Successful" value={data.successfulCalls} color="green" />
          <MetricCard label="Failed" value={data.failedCalls} color="red" />
        </div>

        {(data.lastSuccessfulCall || data.lastFailedCall) && (
          <div className="mt-6 space-y-3">
            {data.lastSuccessfulCall && (
              <InfoRow label="Last Successful Call" value={formatDateTime(data.lastSuccessfulCall)} />
            )}
            {data.lastFailedCall && (
              <InfoRow label="Last Failed Call" value={formatDateTime(data.lastFailedCall)} />
            )}
          </div>
        )}

        <div className="mt-6">
          <Link to="/voice-calls">
            <Button variant="ghost">
              <ExternalLink size={16} className="mr-2" />
              View Voice Calls
            </Button>
          </Link>
        </div>
      </Card>
    </div>
  );
}

// ===== Agents Tab =====

function AgentsTab() {
  const [data, setData] = useState<AgentsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getAgentsConfig();
      setData(response);
    } catch (err: any) {
      setError(err.message || 'Failed to load agents configuration');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) return <LoadingState label="Loading agents configuration..." />;
  if (error) return <ErrorState message={error} onRetry={loadData} />;
  if (!data) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-ink">Agents & Safety</h2>
        <Button variant="secondary" onClick={loadData}>
          <RefreshCw size={16} className="mr-2" />
          Refresh
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {data.agents.map((agent) => (
          <Card key={agent.name || agent.agentName} className="p-6">
            <div className="mb-4 flex items-start justify-between">
              <div>
                <h3 className="text-lg font-semibold text-ink">{agent.name || agent.agentName}</h3>
                <p className="mt-1 text-sm text-slate">{agent.message || agent.statusMessage || agent.purpose}</p>
              </div>
              <ReadinessBadge status={agent.status} />
            </div>

            <div className="space-y-2 text-sm">
              {agent.requiredIntegration && (
                <InfoRow label="Required Integration" value={agent.requiredIntegration} />
              )}
              <InfoRow label="Current Model" value={agent.currentModel || 'Not assigned'} />
              <InfoRow
                label="Fallback Available"
                value={agent.fallbackAvailable ? 'Yes' : 'No'}
              />
            </div>
          </Card>
        ))}
      </div>

      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-ink">Safety Configuration</h3>
        <div className="space-y-3">
          <InfoRow
            label="Evaluation Agent"
            value={data.safetyConfig.evaluationEnabled ? 'Enabled' : 'Disabled'}
          />
          <InfoRow
            label="Unsafe Answer Blocking"
            value={data.safetyConfig.unsafeAnswerBlocking ? 'Enabled' : 'Disabled'}
          />
          <InfoRow
            label="Fallback Answer"
            value={data.safetyConfig.fallbackAnswerAvailable ? 'Available' : 'Not Available'}
          />
          <InfoRow
            label="Human Approval for Follow-ups"
            value={data.safetyConfig.humanApprovalEnabled ? 'Required' : 'Not Required'}
          />
          <InfoRow
            label="Human Approval for Voice Calls"
            value={data.safetyConfig.humanApprovalForVoice ? 'Required' : 'Not Required'}
          />
          <InfoRow label="Lead Capture Behavior" value={data.safetyConfig.leadCaptureBehavior} />
        </div>

        <div className="mt-6">
          <Link to="/approvals">
            <Button variant="ghost">
              <ExternalLink size={16} className="mr-2" />
              View Approvals
            </Button>
          </Link>
        </div>
      </Card>
    </div>
  );
}

// ===== System Tab =====

function SystemTab() {
  const [data, setData] = useState<SystemDiagnosticsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getSystemDiagnostics();
      setData(response);
    } catch (err: any) {
      setError(err.message || 'Failed to load system diagnostics');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) return <LoadingState label="Loading system diagnostics..." />;
  if (error) return <ErrorState message={error} onRetry={loadData} />;
  if (!data) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-ink">System Diagnostics</h2>
        <Button variant="secondary" onClick={loadData}>
          <RefreshCw size={16} className="mr-2" />
          Refresh
        </Button>
      </div>

      {data.warnings.length > 0 && (
        <div className="space-y-3">
          {data.warnings.map((warning, index) => (
            <div key={index} className="rounded-lg border border-amber-500/20 bg-amber-500/10 p-4">
              <div className="flex items-start">
                <AlertTriangle size={20} className="mr-3 mt-0.5 text-amber-400" />
                <div>
                  <h4 className="font-semibold text-amber-200">{warning.title}</h4>
                  <p className="mt-1 text-sm text-amber-300">{warning.recommendation}</p>
                  <span className="mt-2 inline-block rounded bg-amber-500/20 px-2 py-1 text-xs text-amber-200">
                    {warning.type}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-ink">Application</h3>
        <div className="space-y-3">
          <InfoRow label="Application Name" value={data.applicationName} />
          <InfoRow label="Application Version" value={data.applicationVersion} />
          <InfoRow label="Backend Version" value={data.backendVersion} />
          <InfoRow label="Active Profile" value={data.activeProfile} />
          <InfoRow label="API Base Path" value={data.apiBasePath} />
          <InfoRow label="Server Timezone" value={data.serverTimezone} />
        </div>
      </Card>

      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-ink">Infrastructure</h3>
        <div className="space-y-3">
          <InfoRow label="Database Type" value={data.databaseType} />
          <InfoRow label="Redis Configured" value={data.redisConfigured ? 'Yes' : 'No'} />
          <InfoRow label="Vector Store Strategy" value={data.vectorStoreStrategy} />
        </div>
      </Card>

      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-ink">Database Management</h3>
        <div className="space-y-3">
          <InfoRow label="Flyway Migrations" value={data.flywayEnabled ? 'Enabled' : 'Disabled'} />
          <InfoRow label="Hibernate Schema Mode" value={data.hibernateSchemaMode} />
        </div>

        {(!data.flywayEnabled || data.hibernateSchemaMode === 'update') && (
          <div className="mt-4 rounded-lg bg-amber-500/10 p-3">
            <p className="text-sm text-amber-300">
              ⚠️ Production environments should use Flyway migrations with Hibernate ddl-auto=validate
            </p>
          </div>
        )}
      </Card>
    </div>
  );
}

// ===== Utility Components =====

function StatusIcon({ status }: { status: ReadinessStatus }) {
  if (status === 'HEALTHY') {
    return <CheckCircle size={20} className="text-ink" />;
  }
  if (status === 'CONFIGURED') {
    return <CheckCircle size={20} className="text-slate" />;
  }
  if (status === 'ERROR' || status === 'DEGRADED') {
    return <XCircle size={20} className="text-ink" />;
  }
  return <AlertTriangle size={20} className="text-slate" />;
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between border-b border-frost py-2">
      <span className="text-sm text-slate">{label}</span>
      <span className="text-sm font-medium text-ink">{value}</span>
    </div>
  );
}

function MetricCard({
  label,
  value,
  color,
}: {
  label: string;
  value: number;
  color?: 'green' | 'red';
}) {
  return (
    <div className="rounded-lg bg-mist p-4">
      <p className="text-sm text-slate">{label}</p>
      <p
        className={`mt-2 text-2xl font-bold ${
          color === 'green'
            ? 'text-ink'
            : color === 'red'
            ? 'text-ink'
            : 'text-ink'
        }`}
      >
        {value}
      </p>
    </div>
  );
}

function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <Card className="p-8 text-center">
      <XCircle size={48} className="mx-auto text-ink" />
      <h3 className="mt-4 text-lg font-semibold text-ink">Error Loading Data</h3>
      <p className="mt-2 text-sm text-slate">{message}</p>
      <Button variant="primary" onClick={onRetry} className="mt-6">
        <RefreshCw size={16} className="mr-2" />
        Retry
      </Button>
    </Card>
  );
}

function formatDateTime(isoString: string): string {
  return formatServerDateTime(isoString) || isoString;
}
