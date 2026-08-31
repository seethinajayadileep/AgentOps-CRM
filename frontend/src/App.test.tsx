import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import App from './App';
import Layout from './components/Layout';
import { AuthProvider } from './auth/AuthContext';
import { APP_VERSION } from './config/version';
import packageJson from '../package.json';

vi.mock('./api/approvalsApi', () => ({
  getAllApprovals: vi.fn().mockResolvedValue([]),
}));

vi.mock('./api/authApi', () => ({
  TOKEN_STORAGE_KEY: 'auth_token',
  authApi: {
    me: vi.fn().mockRejectedValue(new Error('unauthenticated')),
    logout: vi.fn(),
  },
}));

describe('404 routing', () => {
  it('renders the not-found page for unknown paths without redirecting', async () => {
    window.history.pushState({}, '', '/not-a-route');
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Return to Dashboard' })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/not-a-route');
  });
});

describe('layout accessibility', () => {
  it('keeps the skip link and mobile navigation control', () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <Layout />
        </AuthProvider>
      </MemoryRouter>
    );
    expect(screen.getByRole('link', { name: 'Skip to main content' })).toHaveAttribute(
      'href',
      '#main-content'
    );
    expect(screen.getByRole('button', { name: 'Open navigation' })).toBeInTheDocument();
  });
});

describe('version consistency', () => {
  it('uses one frontend version for sidebar and package.json', () => {
    expect(APP_VERSION).toBe(packageJson.version);
    expect(APP_VERSION).toBe('0.2.0');
  });
});
