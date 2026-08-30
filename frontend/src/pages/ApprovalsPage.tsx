import React, { useEffect, useRef, useState } from 'react';
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
  const fetchGeneration = useRef(0);

  const fetchApprovals = async () => {
    const generation = ++fetchGeneration.current;
    setLoading(true);
    setError(null);
    try {
      const params: { status?: ApprovalStatus; type?: ApprovalType } = {};
      if (statusFilter !== 'ALL') {
        params.status = statusFilter;
      }
      if (typeFilter !== 'ALL') {
        params.type = typeFilter;
      }
      const data = await getAllApprovals(params);
      if (generation !== fetchGeneration.current) return;
      setApprovals(data);
    } catch (err) {
      if (generation !== fetchGeneration.current) return;
      console.error('Failed to fetch approvals:', err);
      setError('Failed to load approvals. Please try again.');
    } finally {
      if (generation === fetchGeneration.current) {
        setLoading(false);
      }
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
            <label htmlFor="approval-status-filter" className="label-dark">
              Status
            </label>
            <select
              id="approval-status-filter"
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
            <label htmlFor="approval-type-filter" className="label-dark">
              Type
            </label>
            <select
              id="approval-type-filter"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value as ApprovalType | 'ALL')}
              className="input-dark w-48"
            >
              <option value="ALL">All Types</option>
              <option value={ApprovalType.FOLLOW_UP_MESSAGE}>Follow-up Message</option>
              <option value={ApprovalType.VOICE_CALL}>Outbound Call</option>
              <option value={ApprovalType.REPORT_GENERATION}>Outreach Message</option>
            </select>
          </div>
        </div>
      </Card>

      {loading && <LoadingState label="Loading approvals…" />}

      {error && !loading && (
        <div className="mb-6 rounded-sm border border-frost bg-mist p-4 text-ink">{error}</div>
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
          <div className="mt-6 text-center text-sm text-slate">
            Showing {approvals.length} approval{approvals.length !== 1 ? 's' : ''}
          </div>
        </>
      )}
    </div>
  );
};

export default ApprovalsPage;
