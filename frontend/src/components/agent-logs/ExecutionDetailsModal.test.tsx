import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import ExecutionDetailsModal from './ExecutionDetailsModal';
import { AgentActionStatus, type AgentLog } from '../../types/agentLog';

const log: AgentLog = {
  id: 'log-1',
  agentName: 'Crawler',
  action: 'CRAWL_FAILED',
  status: AgentActionStatus.ERROR,
  createdAt: '2026-08-30T00:00:00',
  errorMessage: 'The integration could not be reached. Reference ERR-ABC12345.',
  errorCategory: 'CONNECTIVITY',
  correlationId: 'ERR-ABC12345',
  recommendedAction: 'Verify outbound TLS and network access, then retry.',
};

describe('Execution Details dialog', () => {
  it('has a single heading, labelled close, Escape, and sanitized error fields', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(
      <MemoryRouter>
        <ExecutionDetailsModal log={log} onClose={onClose} />
      </MemoryRouter>
    );
    expect(screen.getAllByRole('heading', { name: 'Execution Details' })).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'Close Execution Details' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Error' }));
    expect(screen.getAllByText(/ERR-ABC12345/).length).toBeGreaterThan(0);
    expect(screen.getByText(/CONNECTIVITY/)).toBeInTheDocument();
    expect(screen.queryByText(/PKIX/i)).not.toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalled();
  });
});
