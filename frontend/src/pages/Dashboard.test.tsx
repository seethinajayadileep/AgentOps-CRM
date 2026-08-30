import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Dashboard from './Dashboard';

vi.mock('../api/axios', () => ({
  default: {
    get: vi.fn(),
  },
}));

import axios from '../api/axios';

describe('Dashboard live trends', () => {
  beforeEach(() => {
    vi.mocked(axios.get).mockReset();
  });

  it('renders trend labels from the stats API instead of hardcoded percentages', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        activeBusinesses: 11,
        totalLeads: 17,
        conversations: 26,
        voiceCalls: 13,
        pendingApprovals: 15,
        agentActionsToday: 4,
        businessesTrend: { direction: 'up', label: '22% vs last month' },
        leadsTrend: { direction: 'down', label: '10% vs last month' },
        conversationsTrend: { direction: 'up', label: '8% vs last week' },
        voiceCallsTrend: { direction: 'flat', label: 'No change vs last week' },
        pendingApprovalsTrend: { direction: 'alert', label: '15 awaiting action' },
        agentActionsTrend: { direction: 'up', label: '33% vs yesterday' },
        recentActivity: [],
      },
    });

    render(<Dashboard />);

    expect(await screen.findByText(/22% vs last month/)).toBeInTheDocument();
    expect(screen.getByText(/10% vs last month/)).toBeInTheDocument();
    expect(screen.getByText(/8% vs last week/)).toBeInTheDocument();
    expect(screen.getByText(/No change vs last week/)).toBeInTheDocument();
    expect(screen.getByText(/15 awaiting action/)).toBeInTheDocument();
    expect(screen.getByText(/33% vs yesterday/)).toBeInTheDocument();
    expect(screen.queryByText(/12% from last month/)).not.toBeInTheDocument();
  });

  it('shows skeletons instead of zero metrics while the API is loading', () => {
    vi.mocked(axios.get).mockReturnValue(new Promise(() => undefined) as never);
    render(<Dashboard />);
    expect(screen.getAllByTestId('dashboard-metric-skeleton').length).toBeGreaterThan(0);
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  it('shows a real zero only after the API returns zero', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...{
          activeBusinesses: 0,
          totalLeads: 0,
          conversations: 0,
          voiceCalls: 0,
          pendingApprovals: 0,
          agentActionsToday: 0,
          recentActivity: [],
        },
      },
    });
    render(<Dashboard />);
    expect(await screen.findAllByText('0')).not.toHaveLength(0);
  });

  it('shows an error banner when the stats API fails', async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error('network'));
    render(<Dashboard />);
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not be loaded/i);
  });
});
