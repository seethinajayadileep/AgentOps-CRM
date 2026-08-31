import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import GuestRoute from './GuestRoute';

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({
    status: 'authenticated',
    user: { id: '1', fullName: 'Ada', email: 'ada@example.com' },
    login: vi.fn(),
    signup: vi.fn(),
    logout: vi.fn(),
  }),
}));

describe('GuestRoute', () => {
  it('restores the original safe redirect when already signed in', () => {
    render(
      <MemoryRouter initialEntries={['/login?redirect=%2Fleads']}>
        <Routes>
          <Route
            path="/login"
            element={
              <GuestRoute>
                <div>Login page</div>
              </GuestRoute>
            }
          />
          <Route path="/leads" element={<div>Leads page</div>} />
          <Route path="/dashboard" element={<div>Dashboard page</div>} />
        </Routes>
      </MemoryRouter>
    );
    expect(screen.getByText('Leads page')).toBeInTheDocument();
    expect(screen.queryByText('Login page')).not.toBeInTheDocument();
    expect(screen.queryByText('Dashboard page')).not.toBeInTheDocument();
  });
});
