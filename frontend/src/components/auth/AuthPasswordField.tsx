import { forwardRef, useState, type InputHTMLAttributes } from 'react';
import { Eye, EyeOff } from 'lucide-react';

interface AuthPasswordFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  id: string;
  name: string;
  label: string;
}

const AuthPasswordField = forwardRef<HTMLInputElement, AuthPasswordFieldProps>(
  function AuthPasswordField(
    { id, name, label, value: _unusedValue, onChange, onBlur, className = 'market-input pr-12', ...rest },
    ref
  ) {
    void _unusedValue;
    const [show, setShow] = useState(false);

    return (
      <div>
        <label htmlFor={id} className="market-label">
          {label}
        </label>
        <div className="relative">
          <input
            {...rest}
            id={id}
            name={name}
            type={show ? 'text' : 'password'}
            onChange={onChange}
            onBlur={onBlur}
            ref={ref}
            className={className}
          />
          <button
            type="button"
            className="absolute inset-y-0 right-0 inline-flex w-11 items-center justify-center text-copy hover:text-ink"
            onClick={() => setShow((current) => !current)}
            aria-label={show ? `Hide ${label.toLowerCase()}` : `Show ${label.toLowerCase()}`}
            aria-pressed={show}
          >
            {show ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
          </button>
        </div>
      </div>
    );
  }
);

export default AuthPasswordField;
