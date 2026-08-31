import { createRef, useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import FormInput from './FormInput';

describe('FormInput', () => {
  it('forwards name, value, events, ref, id, type, and aria attributes', async () => {
    const user = userEvent.setup();
    const ref = createRef<HTMLInputElement>();
    const onChange = vi.fn();
    const onBlur = vi.fn();

    render(
      <FormInput
        ref={ref}
        id="login-email"
        name="email"
        type="email"
        value="user@example.com"
        onChange={onChange}
        onBlur={onBlur}
        aria-describedby="login-email-error"
        aria-invalid={true}
      />
    );

    const input = screen.getByRole('textbox');
    expect(input).toHaveAttribute('id', 'login-email');
    expect(input).toHaveAttribute('name', 'email');
    expect(input).toHaveAttribute('type', 'email');
    expect(input).toHaveValue('user@example.com');
    expect(input).toHaveAttribute('aria-describedby', 'login-email-error');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(ref.current).toBe(input);

    await user.type(input, 'x');
    expect(onChange).toHaveBeenCalled();
    expect(onChange.mock.calls[0][0].target).toBe(input);

    input.blur();
    expect(onBlur).toHaveBeenCalled();
  });

  it('keeps the typed value when the parent stores onChange on the same field', async () => {
    const user = userEvent.setup();

    function Harness() {
      const [email, setEmail] = useState('');
      return (
        <label>
          Email
          <FormInput
            name="email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
      );
    }

    render(<Harness />);
    const input = screen.getByLabelText('Email');
    await user.type(input, 'user@example.com');
    expect(input).toHaveValue('user@example.com');
  });
});
