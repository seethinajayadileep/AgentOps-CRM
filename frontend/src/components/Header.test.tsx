import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Header from './Header';

vi.mock('../api/approvalsApi', () => ({
  getAllApprovals: vi.fn(),
}));

vi.mock('./GlobalSearch', () => ({
  default: () => <div>Search</div>,
}));

import { getAllApprovals } from '../api/approvalsApi';

describe('header notification and profile menus', () => {
  beforeEach(() => {
    vi.mocked(getAllApprovals).mockResolvedValue([
      { approvalId: 'a1' },
      { approvalId: 'a2' },
    ] as never);
  });

  it('opens notifications and links to approvals', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <Header title="Dashboard" />
      </MemoryRouter>
    );
    const bell = await screen.findByRole('button', { name: /Notifications, 2 pending approvals/ });
    await user.click(bell);
    expect(screen.getByRole('menu', { name: 'Notifications' })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /2 pending approvals/ })).toHaveAttribute(
      'href',
      '/approvals'
    );
  });

  it('opens the account menu with a Settings destination', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <Header title="Dashboard" />
      </MemoryRouter>
    );
    await user.click(screen.getByRole('button', { name: 'Account menu for Alex Drake' }));
    expect(screen.getByRole('menu', { name: 'Account' })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: 'Settings' })).toHaveAttribute('href', '/settings');
  });
});
