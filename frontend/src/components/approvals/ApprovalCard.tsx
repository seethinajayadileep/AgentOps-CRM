import React, { useState } from 'react';
import { Copy, Check, ThumbsUp, ThumbsDown } from 'lucide-react';
import { Approval, ApprovalStatus } from '../../types/approval';
import ApprovalStatusBadge from './ApprovalStatusBadge';
import Badge from '../ui/Badge';
import { approveApproval, rejectApproval } from '../../api/approvalsApi';
import { formatServerDateTime } from '../../util/serverDate';

interface ApprovalCardProps {
  approval: Approval;
  onUpdate?: (updatedApproval: Approval) => void;
  emailSendEnabled?: boolean;
}

function isEmailStyle(style?: string) {
  const normalized = (style || '').trim().toUpperCase();
  return normalized === 'PROFESSIONAL' || normalized === 'FRIENDLY';
}

const ApprovalCard: React.FC<ApprovalCardProps> = ({
  approval,
  onUpdate,
  emailSendEnabled = false,
}) => {
  const [isLoading, setIsLoading] = useState(false);
  const [copySuccess, setCopySuccess] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const willSend = emailSendEnabled && isEmailStyle(approval.style);
  const canAct =
    approval.status === ApprovalStatus.PENDING || approval.status === ApprovalStatus.SEND_FAILED;
  const missingEmail = willSend && !approval.leadEmail;

  const handleCopy = () => {
    navigator.clipboard.writeText(approval.content);
    setCopySuccess(true);
    setTimeout(() => setCopySuccess(false), 2000);
  };

  const handleApprove = async () => {
    if (isLoading) return;
    setIsLoading(true);
    setActionError(null);
    try {
      const updatedApproval = await approveApproval(approval.approvalId);
      if (onUpdate) onUpdate(updatedApproval);
    } catch (error) {
      console.error('Failed to approve approval:', error);
      setActionError('Failed to approve. Please try again.');
      setTimeout(() => setActionError(null), 5000);
    } finally {
      setIsLoading(false);
    }
  };

  const handleReject = async () => {
    if (isLoading) return;
    setIsLoading(true);
    setActionError(null);
    try {
      const updatedApproval = await rejectApproval(approval.approvalId);
      if (onUpdate) onUpdate(updatedApproval);
    } catch (error) {
      console.error('Failed to reject approval:', error);
      setActionError('Failed to reject. Please try again.');
      setTimeout(() => setActionError(null), 5000);
    } finally {
      setIsLoading(false);
    }
  };

  const formatDate = (dateString: string) => formatServerDateTime(dateString);

  return (
    <div className="glass-card glass-card-hover p-6">
      <div className="mb-4 flex items-start justify-between gap-4">
        <div>
          <div className="mb-2 flex flex-wrap items-center gap-2">
            <h3 className="text-[16px] font-semibold text-ink">{approval.type}</h3>
            <ApprovalStatusBadge status={approval.status} />
            {approval.style && <Badge color="purple">{approval.style}</Badge>}
          </div>
          {approval.leadName && (
            <p className="text-sm text-slate">
              Lead: <span className="text-ink">{approval.leadName}</span>
            </p>
          )}
          {approval.leadEmail && (
            <p className="text-sm text-slate">
              Email: <span className="text-ink">{approval.leadEmail}</span>
            </p>
          )}
          {approval.businessName && (
            <p className="text-sm text-slate">
              Business: <span className="text-ink">{approval.businessName}</span>
            </p>
          )}
        </div>
        <div className="text-right text-xs text-slate">
          <div>Created: {formatDate(approval.createdAt)}</div>
        </div>
      </div>

      <div className="mb-5">
        <div className="mb-2 text-xs font-medium uppercase tracking-wide text-slate">
          Message Content
        </div>
        <div className="whitespace-pre-wrap rounded-sm border border-frost bg-snow p-4 text-sm leading-relaxed text-ink">
          {approval.content}
        </div>
      </div>

      {approval.status === ApprovalStatus.APPROVED && approval.sentTo && approval.sentAt && (
        <p className="mb-3 text-sm text-ink">
          Sent to {approval.sentTo} at {formatDate(approval.sentAt)}
        </p>
      )}

      {(approval.sendError || actionError) && (
        <div className="mb-3 rounded-lg border border-frost bg-mist px-3 py-2 text-sm text-ink">
          {actionError || approval.sendError}
        </div>
      )}

      <div className="flex flex-wrap gap-2">
        <button onClick={handleCopy} className="btn-secondary">
          {copySuccess ? <Check size={16} /> : <Copy size={16} />}
          {copySuccess ? 'Copied!' : 'Copy'}
        </button>

        {canAct && (
          <>
            <button
              onClick={handleApprove}
              disabled={isLoading}
              className="btn-success"
            >
              <ThumbsUp size={16} />
              {isLoading
                ? willSend
                  ? 'Sending…'
                  : 'Processing…'
                : willSend
                  ? 'Approve & send'
                  : 'Approve'}
            </button>
            <button onClick={handleReject} disabled={isLoading} className="btn-danger">
              <ThumbsDown size={16} />
              {isLoading ? 'Processing…' : 'Reject'}
            </button>
          </>
        )}

        {approval.status === ApprovalStatus.APPROVED && !approval.sentTo && (
          <span className="inline-flex items-center gap-1.5 text-sm font-medium text-ink">
            <Check size={16} /> Approved
          </span>
        )}
      </div>

      {canAct && willSend && missingEmail && (
        <p className="mt-3 text-xs text-slate">This lead has no email. Add one on the lead before sending.</p>
      )}
    </div>
  );
};

export default ApprovalCard;
