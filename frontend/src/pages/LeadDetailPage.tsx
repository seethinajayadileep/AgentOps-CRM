import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Sparkles, Phone } from 'lucide-react';
import { leadsApi } from '../api/leadsApi';
import type { Lead } from '../types/lead';
import { LeadStatus } from '../types/lead';
import { LeadStatusBadge } from '../components/leads/LeadStatusBadge';
import { LeadScoreBadge } from '../components/leads/LeadScoreBadge';
import { generateFollowUpMessages } from '../api/approvalsApi';
import type { Approval } from '../types/approval';
import ApprovalCard from '../components/approvals/ApprovalCard';
import { voiceCallsApi } from '../api/voiceCallsApi';
import type { VoiceCall } from '../types/voiceCall';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import StatusBadge from '../components/ui/StatusBadge';
import LoadingState from '../components/ui/LoadingState';
import ToastContainer from '../components/ui/ToastContainer';
import { useToast } from '../hooks/useToast';
import { useIntegrations } from '../hooks/useIntegrations';

export default function LeadDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { toasts, showToast, closeToast } = useToast();
  const [lead, setLead] = useState<Lead | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [updating, setUpdating] = useState(false);
  const [generatingFollowUps, setGeneratingFollowUps] = useState(false);
  const [followUpApprovals, setFollowUpApprovals] = useState<Approval[]>([]);
  const [startingCall, setStartingCall] = useState(false);
  const [recentCalls, setRecentCalls] = useState<VoiceCall[]>([]);
  const integrations = useIntegrations();
  const openaiReady = integrations.ready('OpenAI');
  const vapiReady = integrations.ready('Vapi');

  useEffect(() => {
    if (id) {
      loadLead(id);
      loadRecentCalls(id);
    }
  }, [id]);

  const loadLead = async (leadId: string) => {
    try {
      setLoading(true);
      const data = await leadsApi.getLeadById(leadId);
      setLead(data);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to load lead');
    } finally {
      setLoading(false);
    }
  };

  const handleStatusUpdate = async (newStatus: LeadStatus) => {
    if (!lead || !id) return;

    try {
      setUpdating(true);
      const updatedLead = await leadsApi.updateLeadStatus(id, { status: newStatus });
      setLead(updatedLead);
      showToast('success', `Lead status updated to ${newStatus}`);
    } catch (err: any) {
      showToast('error', 'Failed to update status: ' + (err.message || 'Unknown error'));
    } finally {
      setUpdating(false);
    }
  };

  const handleGenerateFollowUps = async () => {
    if (!lead || !id || generatingFollowUps || !openaiReady) return;

    try {
      setGeneratingFollowUps(true);
      const response = await generateFollowUpMessages(id, { tone: 'ALL' });
      setFollowUpApprovals(response.approvals);
      showToast('success', `Successfully generated ${response.approvals.length} follow-up message${response.approvals.length !== 1 ? 's' : ''}!`);
    } catch (err: any) {
      showToast('error', `Failed to generate follow-up messages: ${err.message || 'Unknown error'}`);
    } finally {
      setGeneratingFollowUps(false);
    }
  };

  const handleApprovalUpdate = (updatedApproval: Approval) => {
    setFollowUpApprovals((prev) =>
      prev.map((approval) =>
        approval.approvalId === updatedApproval.approvalId ? updatedApproval : approval
      )
    );
  };

  const loadRecentCalls = async (leadId: string) => {
    try {
      const response = await voiceCallsApi.getLeadCalls(leadId, 0, 5);
      setRecentCalls(response.items);
    } catch (err: any) {
      console.error('Failed to load voice calls:', err);
    }
  };

  const handleStartVoiceCall = async () => {
    if (!vapiReady) {
      showToast('error', 'Voice calls require Vapi to be configured in Settings');
      return;
    }
    if (!lead || !id || !lead.phone) {
      showToast('error', 'Lead must have a phone number to start a voice call');
      return;
    }

    try {
      setStartingCall(true);
      await voiceCallsApi.startCall(id, { phoneNumber: lead.phone });
      showToast('success', 'Voice call started successfully!');
      loadRecentCalls(id);
    } catch (err: any) {
      showToast('error', 'Failed to start voice call: ' + (err.message || 'Unknown error'));
    } finally {
      setStartingCall(false);
    }
  };

  if (loading) {
    return <LoadingState label="Loading lead…" />;
  }

  if (error || !lead) {
    return (
      <div>
        <div className="rounded-sm border border-frost bg-mist p-4 text-ink">
          Error: {error || 'Lead not found'}
        </div>
        <button onClick={() => navigate('/leads')} className="mt-4 text-ink hover:text-ink">
          ← Back to Leads
        </button>
      </div>
    );
  }

  return (
    <>
      <ToastContainer toasts={toasts} onClose={closeToast} />
      <div>
        <PageHeader
          title="Lead Details"
          back={
            <button
              onClick={() => navigate('/leads')}
              className="inline-flex items-center gap-2 text-sm text-slate hover:text-ink"
            >
              <ArrowLeft size={18} />
              Back to Leads
            </button>
          }
        />

      <div className="space-y-6">
        {/* Header Card with prominent score/status */}
        <Card className="p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <h2 className="text-xl font-semibold text-ink">{lead.name}</h2>
              <p className="mt-1 text-sm text-slate">Created {new Date(lead.createdAt).toLocaleString()}</p>
            </div>
            <div className="flex items-center gap-3">
              <div className="text-center">
                <p className="mb-1 text-xs uppercase tracking-wide text-slate">Score</p>
                <LeadScoreBadge score={lead.leadScore} />
              </div>
              <div className="text-center">
                <p className="mb-1 text-xs uppercase tracking-wide text-slate">Status</p>
                <LeadStatusBadge status={lead.status} />
              </div>
            </div>
          </div>
        </Card>

        {/* Info cards */}
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <Card className="p-6">
            <h3 className="mb-4 text-sm font-medium text-ink">Contact Information</h3>
            <div className="space-y-3 text-sm">
              <div>
                <p className="text-xs text-slate">Email</p>
                <p className="text-ink">{lead.email || '-'}</p>
              </div>
              <div>
                <p className="text-xs text-slate">Phone</p>
                <p className="text-ink">{lead.phone || '-'}</p>
              </div>
              <div>
                <p className="text-xs text-slate">Business</p>
                <p className="text-ink">{lead.businessName || '-'}</p>
              </div>
            </div>
          </Card>

          <Card className="p-6">
            <h3 className="mb-4 text-sm font-medium text-ink">Details</h3>
            <div className="space-y-3 text-sm">
              <div>
                <p className="text-xs text-slate">Budget</p>
                <p className="text-ink">{lead.budget || '-'}</p>
              </div>
              <div>
                <p className="text-xs text-slate">Urgency</p>
                <p className="text-ink">{lead.urgency || '-'}</p>
              </div>
              <div>
                <p className="text-xs text-slate">Timeline</p>
                <p className="text-ink">{lead.timeline || '-'}</p>
              </div>
            </div>
          </Card>
        </div>

        {(lead.requirementText || lead.summary) && (
          <Card className="p-6">
            {lead.requirementText && (
              <div className="mb-4">
                <h3 className="mb-2 text-sm font-medium text-ink">Requirement</h3>
                <p className="text-sm text-ink">{lead.requirementText}</p>
              </div>
            )}
            {lead.summary && (
              <div>
                <h3 className="mb-2 text-sm font-medium text-ink">Summary</h3>
                <p className="text-sm text-ink">{lead.summary}</p>
              </div>
            )}
          </Card>
        )}

        {/* Status Update */}
        <Card className="p-6">
          <h3 className="mb-3 text-sm font-medium text-ink">Update Status</h3>
          <div className="flex flex-wrap gap-2">
            {Object.values(LeadStatus).map((status) => (
              <button
                key={status}
                onClick={() => handleStatusUpdate(status)}
                disabled={updating || lead.status === status}
                className={
                  lead.status === status
                    ? 'btn cursor-not-allowed bg-mist text-slate'
                    : 'btn-secondary'
                }
              >
                {status}
              </button>
            ))}
          </div>
        </Card>

        {/* Follow-up */}
        <Card className="p-6">
          <h3 className="mb-2 flex items-center gap-2 text-sm font-medium text-ink">
            <Sparkles size={16} className="text-slate" /> Follow-up Messages
          </h3>
          <p className="mb-4 text-sm text-slate">
            Generate AI-powered follow-up messages for this lead. Messages require approval before use.
          </p>
          {!openaiReady && (
            <p className="mb-3 text-sm text-ink">Follow-up generation requires OpenAI to be configured in Settings.</p>
          )}
          <button
            onClick={handleGenerateFollowUps}
            disabled={!openaiReady || generatingFollowUps}
            className="btn-primary"
          >
            <Sparkles size={16} />
            {generatingFollowUps ? 'Generating…' : 'Generate Follow-up Messages'}
          </button>
        </Card>

        {/* Voice Calls */}
        <Card className="p-6">
          <h3 className="mb-2 flex items-center gap-2 text-sm font-medium text-ink">
            <Phone size={16} className="text-slate" /> Voice Calls
          </h3>
          <p className="mb-4 text-sm text-slate">Start an AI-powered voice call to this lead using Vapi.</p>
          {!vapiReady && (
            <p className="mb-3 text-sm text-ink">Voice calls require Vapi to be configured in Settings.</p>
          )}
          <button
            onClick={handleStartVoiceCall}
            disabled={!vapiReady || startingCall || !lead.phone}
            className="btn-success"
          >
            <Phone size={16} />
            {startingCall ? 'Starting Call…' : 'Start Voice Call'}
          </button>
          {!lead.phone && <p className="mt-2 text-xs text-ink">Phone number required to start call</p>}

          {recentCalls.length > 0 && (
            <div className="mt-5">
              <h4 className="mb-2 text-xs font-medium text-slate">Recent Calls</h4>
              <div className="space-y-2">
                {recentCalls.map((call) => (
                  <div
                    key={call.id}
                    className="flex items-center justify-between rounded-sm border border-frost bg-snow p-3 text-xs"
                  >
                    <div className="flex items-center gap-2">
                      <StatusBadge status={call.status} />
                      {call.outcome && <span className="text-slate">Outcome: {call.outcome}</span>}
                    </div>
                    <span className="text-slate">{new Date(call.createdAt).toLocaleDateString()}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </Card>
      </div>

        {/* Follow-up Approvals */}
        {followUpApprovals.length > 0 && (
          <div className="mt-6">
            <h2 className="mb-4 text-lg font-semibold text-ink">Generated Follow-up Messages</h2>
            <div className="space-y-4">
              {followUpApprovals.map((approval) => (
                <ApprovalCard key={approval.approvalId} approval={approval} onUpdate={handleApprovalUpdate} />
              ))}
            </div>
          </div>
        )}
      </div>
    </>
  );
}
