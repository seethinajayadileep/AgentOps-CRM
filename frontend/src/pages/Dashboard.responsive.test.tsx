import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import Dashboard from './Dashboard';

vi.mock('../api/axios', () => ({
  default: {
    get: vi.fn().mockResolvedValue({
      data: {
        activeBusinesses: 1,
        totalLeads: 1,
        conversations: 1,
        voiceCalls: 0,
        pendingApprovals: 0,
        agentActionsToday: 1,
        recentActivity: [],
      },
    }),
  },
}));

describe('Dashboard responsive metric grid', () => {
  it.each([390, 768, 1280])('renders key metrics at %spx', async (width) => {
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: width });
    render(<Dashboard />);
    expect(await screen.findByLabelText('Key metrics')).toBeInTheDocument();
    expect(screen.getByText('Active Businesses')).toBeInTheDocument();
  });
});
