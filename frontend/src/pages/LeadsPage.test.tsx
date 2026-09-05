import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LeadsPage from './LeadsPage';
import { LeadStatus, type Lead } from '../types/lead';

vi.mock('../api/leadsApi', () => ({
  leadsApi: {
    getAllLeads: vi.fn(),
  },
}));

import { leadsApi } from '../api/leadsApi';

function lead(overrides: Partial<Lead> = {}): Lead {
  return {
    id: overrides.id ?? 'l1',
    businessId: 'b1',
    businessName: 'Acme',
    name: 'Pat Lee',
    email: 'pat@example.com',
    phone: '+15551212',
    requirementText: 'Need a CRM demo',
    status: LeadStatus.HOT,
    leadScore: 82,
    createdAt: '2026-08-01T00:00:00',
    updatedAt: '2026-08-01T00:00:00',
    ...overrides,
  };
}

function renderLeads(path = '/leads') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/leads" element={<LeadsPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('Leads search and status filters', () => {
  beforeEach(() => {
    vi.mocked(leadsApi.getAllLeads).mockReset();
  });

  it('shows search and status controls even when the list is empty', async () => {
    vi.mocked(leadsApi.getAllLeads).mockResolvedValue([]);
    renderLeads();
    expect(await screen.findByPlaceholderText('Search leads...')).toHaveAccessibleName('Search leads');
    expect(screen.getByLabelText('Status')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Search leads' })).toBeInTheDocument();
    expect(await screen.findByText('No leads found')).toBeInTheDocument();
    expect(screen.queryByText('No leads match these filters')).not.toBeInTheDocument();
  });

  it('filters by search query and restores results when cleared', async () => {
    const user = userEvent.setup();
    vi.mocked(leadsApi.getAllLeads).mockResolvedValue([
      lead({ id: 'l1', name: 'Pat Lee' }),
      lead({ id: 'l2', name: 'Jordan Kim', email: 'jordan@example.com', status: LeadStatus.NEW }),
    ]);
    renderLeads();
    expect(await screen.findByText('Pat Lee')).toBeInTheDocument();
    expect(screen.getByText('Jordan Kim')).toBeInTheDocument();

    await user.type(screen.getByPlaceholderText('Search leads...'), 'Jordan');
    await user.click(screen.getByRole('button', { name: 'Search leads' }));
    expect(screen.getByText('Jordan Kim')).toBeInTheDocument();
    expect(screen.queryByText('Pat Lee')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Clear/ }));
    expect(screen.getByText('Pat Lee')).toBeInTheDocument();
    expect(screen.getByText('Jordan Kim')).toBeInTheDocument();
  });

  it.each(['NEW', 'QUALIFIED', 'HOT', 'COLD', 'FOLLOWED_UP', 'CLOSED'] as const)(
    'shows a filter empty state for status=%s and recovers after Clear Filters',
    async (status) => {
      const user = userEvent.setup();
      vi.mocked(leadsApi.getAllLeads).mockResolvedValue([
        lead({ id: 'l1', name: 'Pat Lee', status: LeadStatus.HOT }),
      ]);
      renderLeads();
      expect(await screen.findByText('Pat Lee')).toBeInTheDocument();
      await user.selectOptions(screen.getByLabelText('Status'), status);
      if (status === 'HOT') {
        expect(screen.getByText('Pat Lee')).toBeInTheDocument();
        expect(screen.queryByText('No leads match these filters')).not.toBeInTheDocument();
        return;
      }
      expect(screen.getByText('No leads match these filters')).toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: 'Clear Filters' }));
      expect(screen.getByText('Pat Lee')).toBeInTheDocument();
    }
  );

  it('applies search and status from the URL on load', async () => {
    vi.mocked(leadsApi.getAllLeads).mockResolvedValue([
      lead({ id: 'l1', name: 'Pat Lee', status: LeadStatus.HOT }),
      lead({ id: 'l2', name: 'Jordan Kim', status: LeadStatus.NEW }),
    ]);
    renderLeads('/leads?search=Pat&status=HOT');
    expect(await screen.findByText('Pat Lee')).toBeInTheDocument();
    expect(screen.queryByText('Jordan Kim')).not.toBeInTheDocument();
    expect(screen.getByPlaceholderText('Search leads...')).toHaveValue('Pat');
    expect(screen.getByLabelText('Status')).toHaveValue('HOT');
  });

  it('ignores an invalid status from the URL and recovers after clearing filters', async () => {
    const user = userEvent.setup();
    vi.mocked(leadsApi.getAllLeads).mockResolvedValue([lead({ name: 'Pat Lee' })]);
    renderLeads('/leads?status=Closed');
    expect(await screen.findByText('Pat Lee')).toBeInTheDocument();
    expect(screen.getByLabelText('Status')).toHaveValue('');
    await user.click(screen.getByRole('button', { name: /Clear/ }));
    expect(screen.getByText('Pat Lee')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Clear/ })).not.toBeInTheDocument();
  });

  it('caps search input at 200 characters and accepts special characters', async () => {
    const user = userEvent.setup();
    vi.mocked(leadsApi.getAllLeads).mockResolvedValue([
      lead({ name: 'Acme & Co <test>', requirementText: 'Need a CRM demo' }),
    ]);
    renderLeads();
    const search = await screen.findByPlaceholderText('Search leads...');
    await user.type(search, 'Acme & Co <test>');
    await user.click(screen.getByRole('button', { name: 'Search leads' }));
    expect(await screen.findByText('Acme & Co <test>')).toBeInTheDocument();
    expect(search).toHaveAttribute('maxLength', '200');
    await user.clear(search);
    await user.type(search, 'x'.repeat(210));
    expect((search as HTMLInputElement).value.length).toBeLessThanOrEqual(200);
  });

  it('retries a failed request and recovers when the service returns data', async () => {
    const user = userEvent.setup();
    vi.mocked(leadsApi.getAllLeads)
      .mockRejectedValueOnce(new Error('Failed to load leads'))
      .mockResolvedValue([lead({ name: 'Pat Lee' })]);
    renderLeads();
    expect(await screen.findByRole('button', { name: 'Retry' })).toBeInTheDocument();
    expect(screen.queryByText('No leads found')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByText('Pat Lee')).toBeInTheDocument();
  });
});
