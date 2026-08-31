import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import Login from './Login';

const login = vi.fn();

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({
    status: 'unauthenticated',
    user: null,
    login,
    signup: vi.fn(),
    logout: vi.fn(),
  }),
}));

describe('Login sample access', () => {
  it('keeps typed email and password in the visible inputs', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );
    const email = screen.getByLabelText('Email');
    const password = screen.getByLabelText('Password');
    await user.type(email, 'user@example.com');
    await user.type(password, 'VisiblePass1');
    expect(email).toHaveValue('user@example.com');
    expect(password).toHaveValue('VisiblePass1');
    expect((email as HTMLInputElement).value).toBe('user@example.com');
    expect((password as HTMLInputElement).value).toBe('VisiblePass1');
    expect(login).not.toHaveBeenCalled();
  });

  it('shows sample credentials and fills the form without submitting', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Sample access' })).toBeInTheDocument();
    expect(screen.getByText('demo@agentcrm.app')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveValue('');
    expect(screen.getByLabelText('Password')).toHaveValue('');
    await user.click(screen.getByRole('button', { name: 'Fill credentials' }));
    const email = screen.getByLabelText('Email') as HTMLInputElement;
    const password = screen.getByLabelText('Password') as HTMLInputElement;
    expect(email.value).toBe('demo@agentcrm.app');
    expect(password.value).toBe('Demo@123');
    expect(login).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeEnabled();
  });

  it('clears field errors when sample credentials are filled', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );
    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(screen.getByText('Enter your email.')).toBeInTheDocument();
    expect(screen.getByText('Enter your password.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Fill credentials' }));
    expect(screen.queryByText('Enter your email.')).not.toBeInTheDocument();
    expect(screen.queryByText('Enter your password.')).not.toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });
});
