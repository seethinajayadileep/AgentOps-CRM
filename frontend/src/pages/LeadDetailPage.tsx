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
    } catch (err: any) {
      alert('Failed to update status: ' + err.message);
    } finally {
      setUpdating(false);
    }
  };

  const handleGenerateFollowUps = async () => {
    if (!lead || !id || generatingFollowUps) return;

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
    if (!lead || !id || !lead.phone) {
      alert('Lead must have a phone number to start a voice call');
      return;
    }

    try {
      setStartingCall(true);
      await voiceCallsApi.startCall(id, { phoneNumber: lead.phone });
      alert('Voice call started successfully!');
      loadRecentCalls(id);
    } catch (err: any) {
      alert('Failed to start voice call: ' + err.message);
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
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">
          Error: {error || 'Lead not found'}
        </div>
        <button onClick={() => navigate('/leads')} className="mt-4 text-primary-300 hover:text-primary-200">
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
              className="inline-flex items-center gap-2 text-sm text-zinc-400 hover:text-zinc-100"
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
              <h2 className="text-xl font-semibold text-white">{lead.name}</h2>
              <p className="mt-1 text-sm text-zinc-500">Created {new Date(lead.createdAt).toLocaleString()}</p>
            </div>
            <div className="flex items-center gap-3">
              <div className="text-center">
                <p className="mb-1 text-xs uppercase tracking-wide text-zinc-500">Score</p>
                <LeadScoreBadge score={lead.leadScore} />
              </div>
              <div className="text-center">
                <p className="mb-1 text-xs uppercase tracking-wide text-zinc-500">Status</p>
                <LeadStatusBadge status={lead.status} />
              </div>
            </div>
          </div>
        </Card>

        {/* Info cards */}
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <Card className="p-6">
            <h3 className="mb-4 text-sm font-medium text-zinc-300">Contact Information</h3>
            <div className="space-y-3 text-sm">
              <div>
                <p className="text-xs text-zinc-500">Email</p>
                <p className="text-zinc-100">{lead.email || '-'}</p>
              </div>
              <div>
                <p className="text-xs text-zinc-500">Phone</p>
                <p className="text-zinc-100">{lead.phone || '-'}</p>
              </div>
              <div>
                <p className="text-xs text-zinc-500">Business</p>
                <p className="text-zinc-100">{lead.businessName || '-'}</p>
              </div>
            </div>
          </Card>

          <Card className="p-6">
            <h3 className="mb-4 text-sm font-medium text-zinc-300">Details</h3>
            <div className="space-y-3 text-sm">
              <div>
                <p className="text-xs text-zinc-500">Budget</p>
                <p className="text-zinc-100">{lead.budget || '-'}</p>
              </div>
              <div>
                <p className="text-xs text-zinc-500">Urgency</p>
                <p className="text-zinc-100">{lead.urgency || '-'}</p>
              </div>
              <div>
                <p className="text-xs text-zinc-500">Timeline</p>
                <p className="text-zinc-100">{lead.timeline || '-'}</p>
              </div>
            </div>
          </Card>
        </div>

        {(lead.requirementText || lead.summary) && (
          <Card className="p-6">
            {lead.requirementText && (
              <div className="mb-4">
                <h3 className="mb-2 text-sm font-medium text-zinc-300">Requirement</h3>
                <p className="text-sm text-zinc-100">{lead.requirementText}</p>
              </div>
            )}
            {lead.summary && (
              <div>
                <h3 className="mb-2 text-sm font-medium text-zinc-300">Summary</h3>
                <p className="text-sm text-zinc-100">{lead.summary}</p>
              </div>
            )}
          </Card>
        )}

        {/* Status Update */}
        <Card className="p-6">
          <h3 className="mb-3 text-sm font-medium text-zinc-300">Update Status</h3>
          <div className="flex flex-wrap gap-2">
            {Object.values(LeadStatus).map((status) => (
              <button
                key={status}
                onClick={() => handleStatusUpdate(status)}
                disabled={updating || lead.status === status}
                className={
                  lead.status === status
                    ? 'btn cursor-not-allowed bg-white/5 text-zinc-500'
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
          <h3 className="mb-2 flex items-center gap-2 text-sm font-medium text-zinc-300">
            <Sparkles size={16} className="text-primary-400" /> Follow-up Messages
          </h3>
          <p className="mb-4 text-sm text-zinc-400">
            Generate AI-powered follow-up messages for this lead. Messages require approval before use.
          </p>
          <button onClick={handleGenerateFollowUps} disabled={generatingFollowUps} className="btn-primary">
            <Sparkles size={16} />
            {generatingFollowUps ? 'Generating…' : 'Generate Follow-up Messages'}
          </button>
        </Card>

        {/* Voice Calls */}
        <Card className="p-6">
          <h3 className="mb-2 flex items-center gap-2 text-sm font-medium text-zinc-300">
            <Phone size={16} className="text-[#06B6D4]" /> Voice Calls
          </h3>
          <p className="mb-4 text-sm text-zinc-400">Start an AI-powered voice call to this lead using Vapi.</p>
          <button onClick={handleStartVoiceCall} disabled={startingCall || !lead.phone} className="btn-success">
            <Phone size={16} />
            {startingCall ? 'Starting Call…' : 'Start Voice Call'}
          </button>
          {!lead.phone && <p className="mt-2 text-xs text-red-400">Phone number required to start call</p>}

          {recentCalls.length > 0 && (
            <div className="mt-5">
              <h4 className="mb-2 text-xs font-medium text-zinc-400">Recent Calls</h4>
              <div className="space-y-2">
                {recentCalls.map((call) => (
                  <div
                    key={call.id}
                    className="flex items-center justify-between rounded-xl border border-white/[0.06] bg-black/30 p-3 text-xs"
                  >
                    <div className="flex items-center gap-2">
                      <StatusBadge status={call.status} />
                      {call.outcome && <span className="text-zinc-400">Outcome: {call.outcome}</span>}
                    </div>
                    <span className="text-zinc-500">{new Date(call.createdAt).toLocaleDateString()}</span>
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
            <h2 className="mb-4 text-lg font-semibold text-white">Generated Follow-up Messages</h2>
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
