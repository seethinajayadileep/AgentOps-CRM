import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import GlobalSearch from '../components/GlobalSearch';

vi.mock('../api/searchApi', () => ({
  searchCrm: vi.fn(),
}));

import { searchCrm } from '../api/searchApi';

describe('global search', () => {
  beforeEach(() => {
    vi.mocked(searchCrm).mockReset();
  });

  it('queries the search API and lists accessible results', async () => {
    const user = userEvent.setup();
    vi.mocked(searchCrm).mockResolvedValue({
      businesses: [{ id: 'b1', title: 'Acme', subtitle: 'Ads', href: '/businesses/b1' }],
      leads: [],
      conversations: [],
    });

    render(
      <MemoryRouter>
        <GlobalSearch />
      </MemoryRouter>
    );

    const input = screen.getByRole('combobox', { name: 'Search the CRM' });
    await user.type(input, 'Ac');
    expect(await screen.findByRole('option', { name: /Acme/ })).toBeInTheDocument();
    expect(searchCrm).toHaveBeenCalledWith('Ac');
  });

  it('navigates to the active result on Enter and supports arrow keys', async () => {
    const user = userEvent.setup();
    vi.mocked(searchCrm).mockResolvedValue({
      businesses: [
        { id: 'b1', title: 'Acme', subtitle: 'Ads', href: '/businesses/b1' },
        { id: 'b2', title: 'Beta', subtitle: 'Media', href: '/businesses/b2' },
      ],
      leads: [],
      conversations: [],
    });

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<GlobalSearch />} />
          <Route path="/businesses/:id" element={<div>Opened business</div>} />
        </Routes>
      </MemoryRouter>
    );

    const input = screen.getByRole('combobox', { name: 'Search the CRM' });
    await user.type(input, 'Ac');
    expect(await screen.findByRole('option', { name: /Acme/ })).toBeInTheDocument();
    await user.keyboard('{ArrowDown}{Enter}');
    expect(await screen.findByText('Opened business')).toBeInTheDocument();
  });
});
