import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LeadFinder from './LeadFinder';

vi.mock('../api/leadFinderApi', () => ({
  leadFinderApi: {
    getConfig: vi.fn(),
    listRuns: vi.fn(),
    startRun: vi.fn(),
    syncRun: vi.fn(),
  },
}));

import { leadFinderApi } from '../api/leadFinderApi';

describe('Lead Finder a11y and sanitized errors', () => {
  beforeEach(() => {
    vi.mocked(leadFinderApi.getConfig).mockResolvedValue({ apifyConfigured: true } as never);
    vi.mocked(leadFinderApi.listRuns).mockResolvedValue([
      {
        id: 'r1',
        searchName: 'QA run',
        status: 'FAILED',
        failureReason:
          'Lead discovery could not connect to the provider. Check the integration and try again. Reference ERR-ABC12345.',
      },
    ] as never);
  });

  it('labels the Max Results number input', async () => {
    render(
      <MemoryRouter>
        <LeadFinder />
      </MemoryRouter>
    );
    expect(await screen.findByLabelText('Max Results')).toBeInTheDocument();
  });

  it('shows the sanitized failure reason without internal exception details', async () => {
    render(
      <MemoryRouter>
        <LeadFinder />
      </MemoryRouter>
    );
    expect(
      await screen.findByText(/Lead discovery could not connect to the provider/)
    ).toBeInTheDocument();
    expect(screen.queryByText(/PKIX/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/javax.net/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/api\.apify\.com/i)).not.toBeInTheDocument();
  });
});
