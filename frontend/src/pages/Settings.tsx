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
        <div className="flex gap-2 border-b border-white/[0.06]">
          {(['overview', 'integrations', 'models', 'rag', 'voice', 'agents', 'system'] as TabName[]).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`whitespace-nowrap px-4 py-3 text-sm font-medium transition-colors ${
                activeTab === tab
                  ? 'border-b-2 border-purple-500 text-white'
                  : 'text-zinc-400 hover:text-zinc-200'
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
          <h2 className="text-xl font-semibold text-white">System Health Overview</h2>
         <p className="mt-1 text-sm text-zinc-400">
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
                <span className="ml-3 font-medium capitalize text-white">{name}</span>
              </div>
              <ReadinessBadge status={status} />
            </div>
          </Card>
        ))}
      </div>

      {/* System Info */}
      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-white">System Information</h3>
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
        <h2 className="text-xl font-semibold text-white">Integration Readiness</h2>
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
                  <h3 className="text-lg font-semibold text-white">{integration.name}</h3>
                  <p className="mt-1 text-sm text-zinc-400">{integration.purpose}</p>
                </div>
                <ReadinessBadge status={integration.status} />
              </div>

              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-zinc-400">Configured:</span>
                  <span className="text-white">{integration.configured ? 'Yes' : 'No'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-400">Enabled:</span>
                  <span className="text-white">{integration.enabled ? 'Yes' : 'No'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-400">Status:</span>
                  <span className="text-white">{integration.message}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-400">Last Checked:</span>
                  <span className="text-white">{formatDateTime(integration.lastChecked)}</span>
                </div>
              </div>

              <div className="mt-4 rounded-lg bg-white/[0.02] p-3">
                <p className="text-xs text-zinc-500">{integration.configDetails}</p>
              </div>

              {integration.configured && (
                <Button
                  variant="ghost"
                  onClick={() => handleTest(integration)}
                  disabled={isTesting}
                  className="mt-4 w-full"
                >
                  {isTesting ? 'Testing...' : 'Test Connection'}
                </Button>
              )}

              {testResult && (
                <div
                  className={`mt-4 rounded-lg p-3 ${
                    testResult.success ? 'bg-emerald-500/10' : 'bg-red-500/10'
                  }`}
                >
                  <div className="flex items-center text-sm">
                    {testResult.success ? (
                      <CheckCircle size={16} className="mr-2 text-emerald-400" />
                    ) : (
                      <XCircle size={16} className="mr-2 text-red-400" />
                    )}
                    <span className={testResult.success ? 'text-emerald-200' : 'text-red-200'}>
                      {testResult.message}
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-zinc-500">
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
        <h2 className="text-xl font-semibold text-white">AI Models Configuration</h2>
        <Button variant="secondary" onClick={loadData}>
          <RefreshCw size={16} className="mr-2" />
          Refresh
        </Button>
      </div>

      <Card className="p-6">
        <div className="mb-4 rounded-lg bg-blue-500/10 p-4">
          <p className="text-sm text-blue-200">{data.configNote}</p>
        </div>

        <h3 className="mb-4 text-lg font-semibold text-white">Agent Models</h3>
        <div className="space-y-3">
          <InfoRow label="RAG Answer Model" value={data.ragAnswerModel} />
          <InfoRow label="Evaluation Agent Model" value={data.evaluationModel} />
          <InfoRow label="Lead Qualification Model" value={data.leadQualificationModel} />
          <InfoRow label="Follow-up Agent Model" value={data.followUpModel} />
        </div>

        <h3 className="mb-4 mt-6 text-lg font-semibold text-white">Embeddings</h3>
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
        <h2 className="text-xl font-semibold text-white">Knowledge & RAG Configuration</h2>
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
        <h3 className="mb-4 text-lg font-semibold text-white">Configuration</h3>
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
        <h3 className="mb-4 text-lg font-semibold text-white">Knowledge Base Metrics</h3>
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
    if (data) {
      const fullUrl = window.location.origin + data.webhookEndpoint;
      navigator.clipboard.writeText(fullUrl);
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
        <div className="rounded-lg border border-zinc-700 bg-zinc-800/50 p-4">
          <div className="flex items-start">
            <AlertTriangle size={20} className="mr-3 mt-0.5 text-zinc-400" />
            <div>
              <h4 className="font-semibold text-zinc-200">Voice calling is disabled</h4>
              <p className="mt-1 text-sm text-zinc-400">{data.statusMessage}</p>
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
        <div className="rounded-lg border border-emerald-500/20 bg-emerald-500/10 p-4">
          <div className="flex items-start">
            <CheckCircle size={20} className="mr-3 mt-0.5 text-emerald-400" />
            <div>
              <h4 className="font-semibold text-emerald-200">Voice AI is ready</h4>
              <p className="mt-1 text-sm text-emerald-300">{data.statusMessage}</p>
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
        <div className="rounded-lg border border-red-500/20 bg-red-500/10 p-4">
          <div className="flex items-start">
            <XCircle size={20} className="mr-3 mt-0.5 text-red-400" />
            <div className="flex-1">
              <h4 className="font-semibold text-red-200">Service error</h4>
              <p className="mt-1 text-sm text-red-300">{data.statusMessage}</p>
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
        <h2 className="text-xl font-semibold text-white">Voice AI Configuration</h2>
        <Button variant="secondary" onClick={loadData} disabled={retrying}>
          <RefreshCw size={16} className="mr-2" />
          Refresh
        </Button>
      </div>

      {renderStatusBanner()}

      <Card className="p-6">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-white">Vapi Readiness</h3>
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
          <label className="mb-2 block text-sm font-medium text-zinc-400">Webhook Endpoint</label>
          <div className="flex items-center gap-2">
            <input
              type="text"
              value={window.location.origin + data.webhookEndpoint}
              readOnly
              className="input-dark flex-1"
            />
            <Button variant="ghost" onClick={copyWebhookUrl}>
              <Copy size={16} />
            </Button>
          </div>
          {copied && <p className="mt-1 text-xs text-emerald-400">Copied to clipboard!</p>}
        </div>
      </Card>

      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-white">Voice Call Metrics</h3>
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
        <h2 className="text-xl font-semibold text-white">Agents & Safety</h2>
        <Button variant="secondary" onClick={loadData}>
          <RefreshCw size={16} className="mr-2" />
          Refresh
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {data.agents.map((agent) => (
          <Card key={agent.name} className="p-6">
            <div className="mb-4 flex items-start justify-between">
              <div>
                <h3 className="text-lg font-semibold text-white">{agent.name}</h3>
                <p className="mt-1 text-sm text-zinc-400">{agent.message}</p>
              </div>
              <ReadinessBadge status={agent.status} />
            </div>

            <div className="space-y-2 text-sm">
              {agent.requiredIntegration && (
                <InfoRow label="Required Integration" value={agent.requiredIntegration} />
              )}
              {agent.currentModel && <InfoRow label="Current Model" value={agent.currentModel} />}
              {agent.fallbackAvailable !== undefined && (
                <InfoRow
                  label="Fallback Available"
                  value={agent.fallbackAvailable ? 'Yes' : 'No'}
                />
              )}
            </div>
          </Card>
       ))}
      </div>

      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-white">Safety Configuration</h3>
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
        <h2 className="text-xl font-semibold text-white">System Diagnostics</h2>
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
        <h3 className="mb-4 text-lg font-semibold text-white">Application</h3>
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
        <h3 className="mb-4 text-lg font-semibold text-white">Infrastructure</h3>
        <div className="space-y-3">
          <InfoRow label="Database Type" value={data.databaseType} />
          <InfoRow label="Redis Configured" value={data.redisConfigured ? 'Yes' : 'No'} />
          <InfoRow label="Vector Store Strategy" value={data.vectorStoreStrategy} />
        </div>
      </Card>

      <Card className="p-6">
        <h3 className="mb-4 text-lg font-semibold text-white">Database Management</h3>
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
    return <CheckCircle size={20} className="text-emerald-400" />;
  }
  if (status === 'CONFIGURED') {
    return <CheckCircle size={20} className="text-blue-400" />;
  }
  if (status === 'ERROR' || status === 'DEGRADED') {
    return <XCircle size={20} className="text-red-400" />;
  }
  return <AlertTriangle size={20} className="text-zinc-500" />;
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between border-b border-white/[0.06] py-2">
      <span className="text-sm text-zinc-400">{label}</span>
      <span className="text-sm font-medium text-white">{value}</span>
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
    <div className="rounded-lg bg-white/[0.02] p-4">
      <p className="text-sm text-zinc-400">{label}</p>
      <p
        className={`mt-2 text-2xl font-bold ${
          color === 'green'
            ? 'text-emerald-400'
            : color === 'red'
            ? 'text-red-400'
            : 'text-white'
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
      <XCircle size={48} className="mx-auto text-red-400" />
      <h3 className="mt-4 text-lg font-semibold text-white">Error Loading Data</h3>
      <p className="mt-2 text-sm text-zinc-400">{message}</p>
      <Button variant="primary" onClick={onRetry} className="mt-6">
        <RefreshCw size={16} className="mr-2" />
        Retry
      </Button>
    </Card>
  );
}

function formatDateTime(isoString: string): string {
  try {
    return new Date(isoString).toLocaleString();
  } catch {
    return isoString;
  }
}
