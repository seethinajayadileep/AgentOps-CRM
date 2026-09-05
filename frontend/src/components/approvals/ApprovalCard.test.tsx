import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ApprovalCard from './ApprovalCard';
import { ApprovalStatus, ApprovalType, type Approval } from '../../types/approval';

vi.mock('../../api/approvalsApi', () => ({
  approveApproval: vi.fn(),
  rejectApproval: vi.fn(),
}));

import { approveApproval } from '../../api/approvalsApi';

function baseApproval(overrides: Partial<Approval> = {}): Approval {
  return {
    approvalId: 'a1',
    type: ApprovalType.FOLLOW_UP_MESSAGE,
    status: ApprovalStatus.PENDING,
    style: 'PROFESSIONAL',
    content: 'Hello from Stripe',
    leadName: 'Ada',
    leadEmail: 'ada@example.com',
    businessName: 'Stripe',
    createdAt: '2026-09-02T10:00:00Z',
    updatedAt: '2026-09-02T10:00:00Z',
    ...overrides,
  };
}

describe('ApprovalCard Resend send states', () => {
  it('shows Approve & send for email styles when Resend is on', () => {
    render(<ApprovalCard approval={baseApproval()} emailSendEnabled />);
    expect(screen.getByRole('button', { name: /approve & send/i })).toBeInTheDocument();
  });

  it('keeps Approve for WhatsApp drafts even when Resend is on', () => {
    render(
      <ApprovalCard
        approval={baseApproval({ style: 'SHORT_WHATSAPP' })}
        emailSendEnabled
      />
    );
    expect(screen.getByRole('button', { name: /^approve$/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /approve & send/i })).not.toBeInTheDocument();
  });

  it('shows the sent line after a successful send', () => {
    render(
      <ApprovalCard
        approval={baseApproval({
          status: ApprovalStatus.APPROVED,
          sentTo: 'ada@example.com',
          sentAt: '2026-09-02T10:05:00Z',
          resendMessageId: 'msg_1',
        })}
      />
    );
    expect(screen.getByText(/sent to ada@example.com/i)).toBeInTheDocument();
  });

  it('lets a failed send be retried', async () => {
    const user = userEvent.setup();
    vi.mocked(approveApproval).mockResolvedValue(
      baseApproval({
        status: ApprovalStatus.APPROVED,
        sentTo: 'ada@example.com',
        sentAt: '2026-09-02T10:06:00Z',
        resendMessageId: 'msg_2',
      })
    );
    render(
      <ApprovalCard
        approval={baseApproval({
          status: ApprovalStatus.SEND_FAILED,
          sendError: 'Failed to send email: invalid from',
        })}
        emailSendEnabled
      />
    );
    expect(screen.getByText(/invalid from/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /approve & send/i }));
    expect(approveApproval).toHaveBeenCalledWith('a1');
  });
});
