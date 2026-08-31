import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { SAMPLE_ACCESS } from '../../auth/sampleAccess';

const login = vi.fn();

vi.mock('../../api/authApi', () => ({
  TOKEN_STORAGE_KEY: 'auth_token',
  authApi: {
    me: vi.fn().mockRejectedValue(new Error('unauthenticated')),
    login: (...args: unknown[]) => login(...args),
    signup: vi.fn(),
    logout: vi.fn().mockResolvedValue(undefined),
  },
}));

vi.mock('../../api/approvalsApi', () => ({
  getAllApprovals: vi.fn().mockResolvedValue([]),
}));

vi.mock('../../api/axios', () => ({
  API_BASE_URL: '/api',
  apiClient: {
    get: vi.fn().mockResolvedValue({ data: {} }),
    post: vi.fn().mockResolvedValue({ data: {} }),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
  default: {
    get: vi.fn().mockResolvedValue({
      data: {
        activeBusinesses: 0,
        totalLeads: 0,
        conversations: 0,
        voiceCalls: 0,
        pendingApprovals: 0,
        agentActionsToday: 0,
        recentActivity: [],
      },
    }),
  },
}));

vi.mock('../../components/GlobalSearch', () => ({
  default: () => <div>Search</div>,
}));

describe('Login Fill credentials browser flow', () => {
  beforeEach(() => {
    login.mockReset();
    login.mockResolvedValue({
      token: 'test-token',
      user: {
        id: 'demo',
        fullName: SAMPLE_ACCESS.name,
        email: SAMPLE_ACCESS.email,
        externalActionsDisabled: true,
        sharedWorkspace: true,
      },
    });
    localStorage.removeItem('auth_token');
    window.history.pushState({}, '', '/login');
  });

  it('fills visible input values without authenticating, then signs in from that form state', async () => {
    const user = userEvent.setup();
    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/login');

    const email = screen.getByLabelText('Email') as HTMLInputElement;
    const password = screen.getByLabelText('Password') as HTMLInputElement;
    expect(email.value).toBe('');
    expect(password.value).toBe('');

    await user.click(screen.getByRole('button', { name: 'Fill credentials' }));

    expect(email.value).toBe(SAMPLE_ACCESS.email);
    expect(password.value).toBe(SAMPLE_ACCESS.password);
    expect(email).toHaveValue(SAMPLE_ACCESS.email);
    expect(password).toHaveValue(SAMPLE_ACCESS.password);
    expect(window.location.pathname).toBe('/login');
    expect(login).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(login).toHaveBeenCalledTimes(1);
      expect(login).toHaveBeenCalledWith(SAMPLE_ACCESS.email, SAMPLE_ACCESS.password);
    });
    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/dashboard');
  });
});
