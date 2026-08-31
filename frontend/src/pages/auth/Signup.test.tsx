import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import Signup from './Signup';

const signup = vi.fn();

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({
    status: 'unauthenticated',
    user: null,
    login: vi.fn(),
    signup,
    logout: vi.fn(),
  }),
}));

describe('Signup validation', () => {
  it('keeps name, email, and passwords visible after typing a normal address', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <Signup />
      </MemoryRouter>
    );
    const fullName = screen.getByLabelText('Full name');
    const email = screen.getByLabelText('Email');
    const password = screen.getByLabelText('Password');
    const confirm = screen.getByLabelText('Confirm password');
    await user.type(fullName, 'Ada Lovelace');
    await user.type(email, 'user@example.com');
    await user.type(password, 'VisiblePass1');
    await user.type(confirm, 'VisiblePass1');
    expect(fullName).toHaveValue('Ada Lovelace');
    expect(email).toHaveValue('user@example.com');
    expect(password).toHaveValue('VisiblePass1');
    expect(confirm).toHaveValue('VisiblePass1');
    expect((email as HTMLInputElement).value).toBe('user@example.com');
    expect(signup).not.toHaveBeenCalled();
  });

  it('rejects mismatched passwords without calling the API', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <Signup />
      </MemoryRouter>
    );
    await user.type(screen.getByLabelText('Full name'), 'New User');
    await user.type(screen.getByLabelText('Email'), 'new@agentcrm.app');
    await user.type(screen.getByLabelText('Password'), 'Password1');
    await user.type(screen.getByLabelText('Confirm password'), 'Password2');
    await user.click(screen.getByLabelText(/terms of use/));
    await user.click(screen.getByRole('button', { name: 'Create account' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Passwords do not match.');
    expect(signup).not.toHaveBeenCalled();
  });
});
