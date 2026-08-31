import { FormEvent, useId, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { MIN_PASSWORD_LENGTH } from '../../auth/sampleAccess';
import { safeClientError } from '../../util/safeClientError';
import AuthPasswordField from '../../components/auth/AuthPasswordField';

interface FieldErrors {
  fullName?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
  accepted?: string;
}

export default function Signup() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const passwordHintId = useId();
  const nameErrorId = useId();
  const emailErrorId = useId();
  const passwordErrorId = useId();
  const confirmErrorId = useId();
  const termsErrorId = useId();
  const formErrorId = useId();
  const successId = useId();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [accepted, setAccepted] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const validate = (values = { fullName, email, password, confirmPassword, accepted }): FieldErrors => {
    const next: FieldErrors = {};
    if (!values.fullName.trim()) next.fullName = 'Enter your full name.';
    if (!values.email.trim()) {
      next.email = 'Enter your email.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
      next.email = 'Enter a valid email address.';
    }
    if (!values.password) {
      next.password = 'Enter a password.';
    } else if (values.password.length < MIN_PASSWORD_LENGTH) {
      next.password = `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`;
    }
    if (!values.confirmPassword) {
      next.confirmPassword = 'Confirm your password.';
    } else if (values.password !== values.confirmPassword) {
      next.confirmPassword = 'Passwords do not match.';
    }
    if (!values.accepted) {
      next.accepted = 'Accept the terms to continue.';
    }
    return next;
  };

  const showFieldError = (field: keyof FieldErrors, values?: Parameters<typeof validate>[0]) => {
    const next = validate(values);
    setFieldErrors((current) => ({ ...current, [field]: next[field] }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const next = {
      fullName: String(data.get('fullName') ?? ''),
      email: String(data.get('email') ?? ''),
      password: String(data.get('password') ?? ''),
      confirmPassword: String(data.get('confirmPassword') ?? ''),
      accepted: data.get('accepted') != null,
    };
    setFullName(next.fullName);
    setEmail(next.email);
    setPassword(next.password);
    setConfirmPassword(next.confirmPassword);
    setAccepted(next.accepted);
    const nextErrors = validate(next);
    setFieldErrors(nextErrors);
    setError(null);
    setSuccess(null);
    if (Object.keys(nextErrors).length > 0) {
      setError(nextErrors.confirmPassword === 'Passwords do not match.' ? 'Passwords do not match.' : 'Review the highlighted fields.');
      return;
    }

    setSubmitting(true);
    try {
      await signup(next.fullName.trim(), next.email.trim(), next.password);
      setSuccess('Account created. Opening the AgentOps CRM workspace…');
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setError(safeClientError(err, 'Unable to create the account.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-card mx-auto max-w-lg">
      <h1 className="font-serif text-4xl text-ink">Create an account</h1>
      <p className="mt-3 max-w-xl text-copy">
        Create your account and start using AgentOps CRM.
      </p>
      <form onSubmit={handleSubmit} className="mt-8 space-y-5" noValidate>
        <div>
          <label htmlFor="signup-full-name" className="market-label">
            Full name
          </label>
          <input
            id="signup-full-name"
            name="fullName"
            type="text"
            autoComplete="name"
            onChange={(event) => setFullName(event.target.value)}
            onBlur={() => showFieldError('fullName')}
            className="market-input"
            aria-invalid={Boolean(fieldErrors.fullName)}
            aria-describedby={fieldErrors.fullName ? nameErrorId : undefined}
          />
          {fieldErrors.fullName && (
            <p id={nameErrorId} className="mt-2 text-sm text-error">
              {fieldErrors.fullName}
            </p>
          )}
        </div>
        <div>
          <label htmlFor="signup-email" className="market-label">
            Email
          </label>
          <input
            id="signup-email"
            name="email"
            type="email"
            autoComplete="email"
            inputMode="email"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            onChange={(event) => setEmail(event.target.value)}
            onBlur={() => showFieldError('email')}
            className="market-input"
            aria-invalid={Boolean(fieldErrors.email)}
            aria-describedby={fieldErrors.email ? emailErrorId : undefined}
          />
          {fieldErrors.email && (
            <p id={emailErrorId} className="mt-2 text-sm text-error">
              {fieldErrors.email}
            </p>
          )}
        </div>
        <AuthPasswordField
          id="signup-password"
          name="password"
          label="Password"
          autoComplete="new-password"
          aria-invalid={Boolean(fieldErrors.password)}
          aria-describedby={`${passwordHintId}${fieldErrors.password ? ` ${passwordErrorId}` : ''}`}
          disabled={submitting}
          onChange={(event) => setPassword(event.target.value)}
          onBlur={() => showFieldError('password')}
        />
        <p id={passwordHintId} className="-mt-3 text-sm text-copy">
          At least {MIN_PASSWORD_LENGTH} characters.
        </p>
        {fieldErrors.password && (
          <p id={passwordErrorId} className="-mt-3 text-sm text-error">
            {fieldErrors.password}
          </p>
        )}
        <AuthPasswordField
          id="signup-confirm-password"
          name="confirmPassword"
          label="Confirm password"
          autoComplete="new-password"
          aria-invalid={Boolean(fieldErrors.confirmPassword)}
          aria-describedby={fieldErrors.confirmPassword ? confirmErrorId : undefined}
          disabled={submitting}
          onChange={(event) => setConfirmPassword(event.target.value)}
          onBlur={() => showFieldError('confirmPassword')}
        />
        {fieldErrors.confirmPassword && (
          <p id={confirmErrorId} className="-mt-3 text-sm text-error">
            {fieldErrors.confirmPassword}
          </p>
        )}
        <div className="flex min-h-11 items-start gap-3">
          <input
            id="signup-terms"
            name="accepted"
            type="checkbox"
            checked={accepted}
            onChange={(event) => setAccepted(event.target.checked)}
            onBlur={() => showFieldError('accepted')}
            className="mt-2 h-5 w-5"
            aria-invalid={Boolean(fieldErrors.accepted)}
            aria-describedby={fieldErrors.accepted ? termsErrorId : undefined}
          />
          <label htmlFor="signup-terms" className="pt-1 text-sm text-copy">
            I agree to the AgentOps CRM terms of use.
          </label>
        </div>
        {fieldErrors.accepted && (
          <p id={termsErrorId} className="-mt-3 text-sm text-error">
            {fieldErrors.accepted}
          </p>
        )}
        {error && (
          <p id={formErrorId} role="alert" className="text-sm text-error">
            {error}
          </p>
        )}
        {success && (
          <p id={successId} role="status" className="text-sm text-success">
            {success}
          </p>
        )}
        <button type="submit" disabled={submitting} className="market-btn-primary w-full">
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="mt-6 text-sm text-copy">
        Already have an account?{' '}
        <Link to="/login" className="inline-flex min-h-11 items-center text-market-accent">
          Sign in
        </Link>
      </p>
    </div>
  );
}
