import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Businesses from './Businesses';

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    status: 'authenticated',
    user: { externalActionsDisabled: true },
    login: vi.fn(),
    signup: vi.fn(),
    logout: vi.fn(),
  }),
}));

vi.mock('../api/business', () => ({
  businessApi: {
    getAllBusinesses: vi.fn(),
    searchBusinesses: vi.fn(),
    deleteBusiness: vi.fn(),
    getDependencies: vi.fn(),
  },
}));

import { businessApi } from '../api/business';

const allPage = {
  items: [
    { id: 'b1', name: 'Acme', websiteUrl: 'https://acme.test', crawlStatus: 'COMPLETED', createdAt: '2026-01-01' },
    { id: 'b2', name: 'ogilvy', websiteUrl: 'https://ogilvy.test', crawlStatus: 'COMPLETED', createdAt: '2026-01-01' },
  ],
  pagination: { page: 0, size: 20, total: 2, totalPages: 1 },
};

const ogilvyPage = {
  items: [allPage.items[1]],
  pagination: { page: 0, size: 20, total: 1, totalPages: 1 },
};

function lastCall<T extends (...args: never[]) => unknown>(fn: T): Parameters<T> | undefined {
  const mocked = vi.mocked(fn);
  if (mocked.mock.calls.length === 0) return undefined;
  return mocked.mock.calls[mocked.mock.calls.length - 1] as Parameters<T>;
}

describe('Business search reset', () => {
  beforeEach(() => {
    vi.mocked(businessApi.getAllBusinesses).mockResolvedValue({ success: true, data: allPage } as never);
    vi.mocked(businessApi.searchBusinesses).mockResolvedValue({ success: true, data: ogilvyPage } as never);
  });

  it('searches normal text, resets on empty Search/Enter/Clear, and caps at 200 characters', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <Businesses />
      </MemoryRouter>
    );
    const input = await screen.findByLabelText('Search businesses');
    expect(await screen.findByText('Acme')).toBeInTheDocument();
    vi.mocked(businessApi.getAllBusinesses).mockClear();
    vi.mocked(businessApi.searchBusinesses).mockClear();

    await user.type(input, 'ogilvy');
    await user.click(screen.getByRole('button', { name: 'Search' }));
    await waitFor(() => {
      expect(lastCall(businessApi.searchBusinesses)?.[0]).toBe('ogilvy');
    });
    expect(screen.queryByText('Acme')).not.toBeInTheDocument();
    expect(screen.getByText('ogilvy')).toBeInTheDocument();

    await user.clear(input);
    expect(input).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Clear' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Search' }));
    await waitFor(() => {
      expect(businessApi.getAllBusinesses).toHaveBeenCalled();
    });
    expect(lastCall(businessApi.searchBusinesses)?.[0]).toBe('ogilvy');
    expect(await screen.findByText('Acme')).toBeInTheDocument();
    expect(screen.getByText('ogilvy')).toBeInTheDocument();
    expect(screen.getByText('2 businesses')).toBeInTheDocument();

    await user.type(input, 'ogilvy');
    await user.click(screen.getByRole('button', { name: 'Search' }));
    await waitFor(() => {
      expect(screen.queryByText('Acme')).not.toBeInTheDocument();
    });
    await user.clear(input);
    await user.type(input, '   ');
    await user.keyboard('{Enter}');
    expect(await screen.findByText('Acme')).toBeInTheDocument();

    await user.clear(input);
    await user.type(input, 'ogilvy');
    await user.click(screen.getByRole('button', { name: 'Search' }));
    await waitFor(() => {
      expect(screen.queryByText('Acme')).not.toBeInTheDocument();
    });
    await user.click(screen.getByRole('button', { name: 'Clear' }));
    expect(await screen.findByText('Acme')).toBeInTheDocument();

    expect(input).toHaveAttribute('maxLength', '200');

    await user.clear(input);
    await user.type(input, 'Acme & Co <test>');
    await user.click(screen.getByRole('button', { name: 'Search' }));
    await waitFor(() => {
      expect(lastCall(businessApi.searchBusinesses)?.[0]).toBe('Acme & Co <test>');
    });
  });
});
