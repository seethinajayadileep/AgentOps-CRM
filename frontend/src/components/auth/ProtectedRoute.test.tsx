import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import ProtectedRoute from './ProtectedRoute';

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({
    status: 'unauthenticated',
    user: null,
    login: vi.fn(),
    signup: vi.fn(),
    logout: vi.fn(),
  }),
}));

describe('ProtectedRoute', () => {
  it('redirects logged-out users to login with an internal redirect', () => {
    render(
      <MemoryRouter initialEntries={['/leads']}>
        <Routes>
          <Route
            path="/leads"
            element={
              <ProtectedRoute>
                <div>Leads secret</div>
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>
    );
    expect(screen.getByText('Login page')).toBeInTheDocument();
    expect(screen.queryByText('Leads secret')).not.toBeInTheDocument();
  });
});
