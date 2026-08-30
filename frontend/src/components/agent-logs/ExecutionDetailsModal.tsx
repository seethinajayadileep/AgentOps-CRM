import React, { useState } from 'react';
import { Copy, CheckCircle, ExternalLink } from 'lucide-react';
import { AgentLog, AgentActionStatus } from '../../types/agentLog';
import StatusBadge from '../ui/StatusBadge';
import Modal from '../ui/Modal';
import { Link } from 'react-router-dom';

/**
  * Modal for displaying agent execution details.
  *
  * @version 0.3.0
  * Feature: F-012 - Agent Logs Observability
  */

interface ExecutionDetailsModalProps {
  log: AgentLog;
  onClose: () => void;
}

const ExecutionDetailsModal: React.FC<ExecutionDetailsModalProps> = ({ log, onClose }) => {
  const [activeTab, setActiveTab] = useState<'overview' | 'input' | 'output' | 'error'>('overview');
  const [copied, setCopied] = useState(false);

  const copyExecutionId = () => {
    navigator.clipboard.writeText(log.id);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: true,
    });
  };

  const formatDuration = (ms?: number) => {
    if (!ms) return 'N/A';
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  };

  const formatActionLabel = (action: string) => {
    return action
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());
  };

  const tryFormatJson = (jsonString?: string) => {
    if (!jsonString) return null;
    try {
      const parsed = JSON.parse(jsonString);
      return JSON.stringify(parsed, null, 2);
    } catch {
      // Not valid JSON, return as plain text
      return jsonString;
    }
  };

  const hasError = log.status === AgentActionStatus.ERROR || log.status === AgentActionStatus.FAILED;

  return (
    <Modal title="Execution Details" onClose={onClose} className="max-w-4xl p-0">
      <div className="relative max-h-[90vh] overflow-hidden">
        <p className="border-b border-frost px-6 py-2 text-sm text-slate">{formatActionLabel(log.action)}</p>
        <div className="overflow-y-auto max-h-[calc(90vh-80px)]">
          {/* Tabs */}
          <div className="sticky top-0 z-10 flex gap-1 border-b border-frost bg-snow px-6 pt-4">
            <button
              onClick={() => setActiveTab('overview')}
              className={`px-4 py-2 text-sm font-medium rounded-t-lg transition-all ${
                activeTab === 'overview'
                  ? 'bg-mist text-ink border-b-2 border-ink'
                  : 'text-slate hover:text-ink hover:bg-mist'
              }`}
            >
              Overview
            </button>
            <button
              onClick={() => setActiveTab('input')}
              className={`px-4 py-2 text-sm font-medium rounded-t-lg transition-all ${
                activeTab === 'input'
                  ? 'bg-mist text-ink border-b-2 border-ink'
                  : 'text-slate hover:text-ink hover:bg-mist'
              }`}
            >
              Input
            </button>
            <button
              onClick={() => setActiveTab('output')}
              className={`px-4 py-2 text-sm font-medium rounded-t-lg transition-all ${
                activeTab === 'output'
                  ? 'bg-mist text-ink border-b-2 border-ink'
                  : 'text-slate hover:text-ink hover:bg-mist'
              }`}
            >
              Output
            </button>
            {hasError && (
              <button
                onClick={() => setActiveTab('error')}
                className={`px-4 py-2 text-sm font-medium rounded-t-lg transition-all ${
                  activeTab === 'error'
                    ? 'bg-mist text-ink border-b-2 border-red-500'
                    : 'text-ink hover:text-ink hover:bg-mist'
                }`}
              >
                Error
              </button>
            )}
          </div>

          {/* Tab Content */}
          <div className="p-6">
            {activeTab === 'overview' && (
              <div className="space-y-6">
                {/* Execution ID */}
                <div>
                  <label className="label-dark">Execution ID</label>
                  <div className="flex items-center gap-2 mt-1">
                    <code className="flex-1 text-sm text-ink bg-snow px-3 py-2 rounded-lg font-mono">
                      {log.id}
                    </code>
                    <button
                      onClick={copyExecutionId}
                      className="btn-secondary flex items-center gap-2"
                      title="Copy ID"
                    >
                      {copied ? <CheckCircle size={16} /> : <Copy size={16} />}
                    </button>
                  </div>
                </div>

                {/* Grid Layout */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="label-dark">Timestamp</label>
                    <p className="mt-1 text-[16px] text-ink">{formatTimestamp(log.createdAt)}</p>
                  </div>
                  <div>
                    <label className="label-dark">Duration</label>
                    <p className="mt-1 text-[16px] text-ink">{formatDuration(log.durationMs)}</p>
                  </div>
                  <div>
                    <label className="label-dark">Agent</label>
                    <p className="mt-1 text-[16px] text-ink">{log.agentName}</p>
                  </div>
                  <div>
                    <label className="label-dark">Action</label>
                    <p className="mt-1 text-[16px] text-ink">{formatActionLabel(log.action)}</p>
                  </div>
                  <div>
                    <label className="label-dark">Status</label>
                    <div className="mt-1">
                      <StatusBadge status={log.status} />
                    </div>
                  </div>
                </div>

                {/* Related Items */}
                {(log.businessId || log.leadId || log.conversationId) && (
                  <div>
                    <label className="label-dark">Related Items</label>
                    <div className="mt-2 space-y-2">
                      {log.businessId && (
                        <Link
                          to={`/businesses/${log.businessId}`}
                          className="flex items-center gap-2 text-sm text-ink hover:text-slate transition-colors"
                        >
                          <ExternalLink size={14} />
                          Business: {log.businessName || log.businessId}
                        </Link>
                      )}
                      {log.leadId && (
                        <Link
                          to={`/leads/${log.leadId}`}
                          className="flex items-center gap-2 text-sm text-ink hover:text-slate transition-colors"
                        >
                          <ExternalLink size={14} />
                          Lead: {log.leadName || log.leadId}
                        </Link>
                      )}
                      {log.conversationId && (
                        <p className="flex items-center gap-2 text-sm text-slate">
                          <ExternalLink size={14} />
                          Conversation: {log.conversationId}
                        </p>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}

            {activeTab === 'input' && (
              <div>
                <label className="label-dark mb-2">Input Data</label>
                {log.inputJson ? (
                  <pre className="text-xs text-ink bg-snow p-4 rounded-lg overflow-auto max-h-96 font-mono whitespace-pre-wrap break-words">
                    {tryFormatJson(log.inputJson) || 'No input data'}
                  </pre>
                ) : (
                  <p className="text-sm text-slate italic">No input data recorded</p>
                )}
              </div>
            )}

            {activeTab === 'output' && (
              <div>
                <label className="label-dark mb-2">Output Data</label>
                {log.outputJson ? (
                  <pre className="text-xs text-ink bg-snow p-4 rounded-lg overflow-auto max-h-96 font-mono whitespace-pre-wrap break-words">
                    {tryFormatJson(log.outputJson) || 'No output data'}
                  </pre>
                ) : (
                  <p className="text-sm text-slate italic">No output data recorded</p>
                )}
              </div>
            )}

            {activeTab === 'error' && hasError && (
              <div>
                <label className="label-dark mb-2">Error Details</label>
                {log.errorMessage ? (
                  <div className="bg-mist border border-frost rounded-lg p-4">
                    <p className="text-sm text-ink whitespace-pre-wrap font-mono">{log.errorMessage}</p>
                    {log.errorCategory && (
                      <p className="mt-3 text-sm text-slate">Category: {log.errorCategory}</p>
                    )}
                    {log.correlationId && (
                      <p className="mt-1 text-sm text-slate">Reference: {log.correlationId}</p>
                    )}
                    {log.recommendedAction && (
                      <p className="mt-2 text-sm text-ink">{log.recommendedAction}</p>
                    )}
                  </div>
                ) : (
                  <p className="text-sm text-slate italic">No error message available</p>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </Modal>
  );
};

export default ExecutionDetailsModal;
