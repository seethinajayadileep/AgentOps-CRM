import React, { useState } from 'react';
import { Copy, Check, ThumbsUp, ThumbsDown } from 'lucide-react';
import { Approval, ApprovalStatus } from '../../types/approval';
import ApprovalStatusBadge from './ApprovalStatusBadge';
import Badge from '../ui/Badge';
import { approveApproval, rejectApproval } from '../../api/approvalsApi';

interface ApprovalCardProps {
  approval: Approval;
  onUpdate?: (updatedApproval: Approval) => void;
}

const ApprovalCard: React.FC<ApprovalCardProps> = ({ approval, onUpdate }) => {
  const [isLoading, setIsLoading] = useState(false);
  const [copySuccess, setCopySuccess] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(approval.content);
    setCopySuccess(true);
    setTimeout(() => setCopySuccess(false), 2000);
  };

  const handleApprove = async () => {
    if (isLoading) return;
    setIsLoading(true);
    try {
      const updatedApproval = await approveApproval(approval.approvalId);
      if (onUpdate) onUpdate(updatedApproval);
    } catch (error) {
      console.error('Failed to approve approval:', error);
      alert('Failed to approve. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleReject = async () => {
    if (isLoading) return;
    setIsLoading(true);
    try {
      const updatedApproval = await rejectApproval(approval.approvalId);
      if (onUpdate) onUpdate(updatedApproval);
    } catch (error) {
      console.error('Failed to reject approval:', error);
      alert('Failed to reject. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const formatDate = (dateString: string) => new Date(dateString).toLocaleString();

  return (
    <div className="glass-card glass-card-hover p-6">
      <div className="mb-4 flex items-start justify-between gap-4">
        <div>
          <div className="mb-2 flex flex-wrap items-center gap-2">
            <h3 className="text-base font-semibold text-white">{approval.type}</h3>
            <ApprovalStatusBadge status={approval.status} />
            {approval.style && <Badge color="purple">{approval.style}</Badge>}
          </div>
          {approval.leadName && (
            <p className="text-sm text-zinc-400">
              Lead: <span className="text-zinc-200">{approval.leadName}</span>
            </p>
          )}
          {approval.businessName && (
            <p className="text-sm text-zinc-400">
              Business: <span className="text-zinc-200">{approval.businessName}</span>
            </p>
          )}
        </div>
        <div className="text-right text-xs text-zinc-500">
          <div>Created: {formatDate(approval.createdAt)}</div>
        </div>
      </div>

      <div className="mb-5">
        <div className="mb-2 text-xs font-medium uppercase tracking-wide text-zinc-500">
          Message Content
        </div>
        <div className="whitespace-pre-wrap rounded-xl border border-white/[0.06] bg-black/30 p-4 text-sm leading-relaxed text-zinc-200">
          {approval.content}
        </div>
      </div>

      <div className="flex flex-wrap gap-2">
        <button onClick={handleCopy} className="btn-secondary">
          {copySuccess ? <Check size={16} /> : <Copy size={16} />}
          {copySuccess ? 'Copied!' : 'Copy'}
        </button>

        {approval.status === ApprovalStatus.PENDING && (
          <>
            <button onClick={handleApprove} disabled={isLoading} className="btn-success">
              <ThumbsUp size={16} />
              {isLoading ? 'Processing…' : 'Approve'}
            </button>
            <button onClick={handleReject} disabled={isLoading} className="btn-danger">
              <ThumbsDown size={16} />
              {isLoading ? 'Processing…' : 'Reject'}
            </button>
          </>
        )}

        {approval.status === ApprovalStatus.APPROVED && (
          <span className="inline-flex items-center gap-1.5 text-sm font-medium text-[#22C55E]">
            <Check size={16} /> Approved
          </span>
        )}
      </div>
    </div>
  );
};

export default ApprovalCard;
