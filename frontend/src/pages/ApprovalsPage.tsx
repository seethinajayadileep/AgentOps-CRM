import React, { useState, useEffect } from 'react';
import { CheckCircle } from 'lucide-react';
import { Approval, ApprovalStatus, ApprovalType } from '../types/approval';
import { getAllApprovals } from '../api/approvalsApi';
import ApprovalCard from '../components/approvals/ApprovalCard';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import EmptyState from '../components/ui/EmptyState';
import LoadingState from '../components/ui/LoadingState';

const ApprovalsPage: React.FC = () => {
  const [approvals, setApprovals] = useState<Approval[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<ApprovalStatus | 'ALL'>('ALL');
  const [typeFilter, setTypeFilter] = useState<ApprovalType | 'ALL'>('ALL');

  const fetchApprovals = async () => {
    setLoading(true);
    setError(null);
    try {
      const params: any = {};
      if (statusFilter !== 'ALL') {
        params.status = statusFilter;
      }
      if (typeFilter !== 'ALL') {
        params.type = typeFilter;
      }
      const data = await getAllApprovals(params);
      setApprovals(data);
    } catch (err) {
      console.error('Failed to fetch approvals:', err);
      setError('Failed to load approvals. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApprovals();
  }, [statusFilter, typeFilter]);

  const handleUpdate = (updatedApproval: Approval) => {
    setApprovals((prev) =>
      prev.map((approval) =>
        approval.approvalId === updatedApproval.approvalId ? updatedApproval : approval
      )
    );
  };

  return (
    <div>
      <PageHeader
        title="Follow-up Approvals"
        subtitle="Review and manage pending follow-up messages and other approval requests."
        action={
          <button onClick={fetchApprovals} className="btn-primary">
            Refresh
          </button>
        }
      />

      {/* Filters */}
      <Card className="mb-6 p-4">
        <div className="flex flex-wrap gap-4">
          <div>
            <label className="label-dark">Status</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as ApprovalStatus | 'ALL')}
              className="input-dark w-48"
            >
              <option value="ALL">All Statuses</option>
              <option value={ApprovalStatus.PENDING}>Pending</option>
              <option value={ApprovalStatus.APPROVED}>Approved</option>
              <option value={ApprovalStatus.REJECTED}>Rejected</option>
            </select>
          </div>

          <div>
            <label className="label-dark">Type</label>
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value as ApprovalType | 'ALL')}
              className="input-dark w-48"
            >
              <option value="ALL">All Types</option>
              <option value={ApprovalType.FOLLOW_UP_MESSAGE}>Follow-up Message</option>
              <option value={ApprovalType.OUTBOUND_CALL}>Outbound Call</option>
              <option value={ApprovalType.OUTREACH_MESSAGE}>Outreach Message</option>
            </select>
          </div>
        </div>
      </Card>

      {loading && <LoadingState label="Loading approvals…" />}

      {error && !loading && (
        <div className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">{error}</div>
      )}

      {!loading && !error && approvals.length === 0 && (
        <EmptyState
          icon={<CheckCircle size={26} />}
          title="No approvals found"
          description={
            statusFilter !== 'ALL' || typeFilter !== 'ALL'
              ? 'Try changing your filters.'
              : 'Generate follow-up messages from the Leads page to create approvals.'
          }
        />
      )}

      {!loading && !error && approvals.length > 0 && (
        <>
          <div className="space-y-4">
            {approvals.map((approval) => (
              <ApprovalCard key={approval.approvalId} approval={approval} onUpdate={handleUpdate} />
            ))}
          </div>
          <div className="mt-6 text-center text-sm text-zinc-500">
            Showing {approvals.length} approval{approvals.length !== 1 ? 's' : ''}
          </div>
        </>
      )}
    </div>
  );
};

export default ApprovalsPage;
