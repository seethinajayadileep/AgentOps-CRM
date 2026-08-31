import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Header from './Header';
import { AuthProvider } from '../auth/AuthContext';

vi.mock('../api/approvalsApi', () => ({
  getAllApprovals: vi.fn(),
}));

vi.mock('../api/authApi', () => ({
  TOKEN_STORAGE_KEY: 'auth_token',
  authApi: {
    me: vi.fn().mockResolvedValue({
      id: 'u1',
      fullName: 'Alex Drake',
      email: 'alex@example.com',
      externalActionsDisabled: true,
      sharedWorkspace: true,
    }),
    logout: vi.fn().mockResolvedValue(undefined),
  },
}));

vi.mock('./GlobalSearch', () => ({
  default: () => <div>Search</div>,
}));

import { getAllApprovals } from '../api/approvalsApi';

function renderHeader() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <Header title="Dashboard" />
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('header notification and profile menus', () => {
  beforeEach(() => {
    localStorage.setItem('auth_token', 'test-token');
    vi.mocked(getAllApprovals).mockResolvedValue([
      { approvalId: 'a1' },
      { approvalId: 'a2' },
    ] as never);
  });

  it('opens notifications and links to approvals', async () => {
    const user = userEvent.setup();
    renderHeader();
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
    renderHeader();
    await user.click(await screen.findByRole('button', { name: 'Account menu for Alex Drake' }));
    expect(screen.getByRole('menu', { name: 'Account' })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: 'Settings' })).toHaveAttribute('href', '/settings');
  });
});
