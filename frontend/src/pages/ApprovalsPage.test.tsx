import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ApprovalsPage from './ApprovalsPage';

vi.mock('../api/approvalsApi', () => ({
  getAllApprovals: vi.fn(),
}));

import { getAllApprovals } from '../api/approvalsApi';

describe('Approvals filter combinations', () => {
  beforeEach(() => {
    vi.mocked(getAllApprovals).mockReset();
  });

  it('treats Approved + Outbound Call with zero rows as empty, not an error', async () => {
    const user = userEvent.setup();
    vi.mocked(getAllApprovals).mockResolvedValue([]);
    render(
      <MemoryRouter>
        <ApprovalsPage />
      </MemoryRouter>
    );
    await screen.findByLabelText('Status');
    await user.selectOptions(screen.getByLabelText('Status'), 'APPROVED');
    await user.selectOptions(screen.getByLabelText('Type'), 'VOICE_CALL');
    await waitFor(() => {
      expect(getAllApprovals).toHaveBeenCalledWith({
        status: 'APPROVED',
        type: 'VOICE_CALL',
      });
    });
    expect(await screen.findByText('No approvals found')).toBeInTheDocument();
    expect(screen.queryByText('Failed to load approvals.')).not.toBeInTheDocument();
  });
});
