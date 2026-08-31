import { FormEvent, useId, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { SAMPLE_ACCESS } from '../../auth/sampleAccess';
import { safeInternalPath } from '../../auth/safeRedirect';
import { safeClientError } from '../../util/safeClientError';
import AuthPasswordField from '../../components/auth/AuthPasswordField';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const emailErrorId = useId();
  const passwordErrorId = useId();
  const formErrorId = useId();
  const copyStatusId = useId();
  const [emailError, setEmailError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [copyNotice, setCopyNotice] = useState('');
  const emailRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);

  const validateEmail = (value: string) => {
    if (!value.trim()) return 'Enter your email.';
    return null;
  };

  const validatePassword = (value: string) => {
    if (!value) return 'Enter your password.';
    return null;
  };

  const copyValue = async (label: 'email' | 'password', value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      setCopyNotice(`${label === 'email' ? 'Email' : 'Password'} copied.`);
    } catch {
      setCopyNotice(`Unable to copy ${label}.`);
    }
  };

  const fillCredentials = () => {
    if (emailRef.current) {
      emailRef.current.value = SAMPLE_ACCESS.email;
    }
    if (passwordRef.current) {
      passwordRef.current.value = SAMPLE_ACCESS.password;
    }
    setEmailError(null);
    setPasswordError(null);
    setError(null);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const nextEmail = String(data.get('email') ?? '');
    const nextPassword = String(data.get('password') ?? '');
    const nextEmailError = validateEmail(nextEmail);
    const nextPasswordError = validatePassword(nextPassword);
    setEmailError(nextEmailError);
    setPasswordError(nextPasswordError);
    setError(null);
    if (nextEmailError || nextPasswordError) return;

    setSubmitting(true);
    try {
      await login(nextEmail.trim(), nextPassword);
      navigate(safeInternalPath(params.get('redirect')), { replace: true });
    } catch (err) {
      setError(safeClientError(err, 'Email or password is incorrect.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_20rem] lg:gap-8">
      <section className="auth-card">
        <h1 className="font-serif text-4xl text-ink">Sign in</h1>
        <p className="mt-3 max-w-xl text-copy">
          Use your AgentOps CRM account or the sample access credentials.
        </p>
        <form onSubmit={handleSubmit} className="mt-8 space-y-5" noValidate>
          <div>
            <label htmlFor="login-email" className="market-label">
              Email
            </label>
            <input
              ref={emailRef}
              id="login-email"
              name="email"
              type="email"
              autoComplete="username"
              inputMode="email"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              defaultValue={params.get('email') ?? undefined}
              onChange={(event) => {
                if (emailError) setEmailError(validateEmail(event.target.value));
              }}
              onBlur={(event) => setEmailError(validateEmail(event.target.value))}
              className="market-input"
              aria-invalid={Boolean(emailError)}
              aria-describedby={emailError ? emailErrorId : undefined}
            />
            {emailError && (
              <p id={emailErrorId} className="mt-2 text-sm text-error">
                {emailError}
              </p>
            )}
          </div>
          <AuthPasswordField
            ref={passwordRef}
            id="login-password"
            name="password"
            label="Password"
            autoComplete="current-password"
            aria-invalid={Boolean(passwordError)}
            aria-describedby={passwordError ? passwordErrorId : undefined}
            disabled={submitting}
            onChange={(event) => {
              if (passwordError) setPasswordError(validatePassword(event.target.value));
            }}
            onBlur={(event) => setPasswordError(validatePassword(event.target.value))}
          />
          {passwordError && (
            <p id={passwordErrorId} className="-mt-3 text-sm text-error">
              {passwordError}
            </p>
          )}
          {error && (
            <p id={formErrorId} role="alert" className="text-sm text-error">
              {error}
            </p>
          )}
          <button type="submit" disabled={submitting} className="market-btn-primary w-full">
            {submitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
        <div className="mt-6 flex flex-wrap gap-4 text-sm">
          <Link to="/forgot-password" className="inline-flex min-h-11 items-center text-copy hover:text-ink">
            Forgot password
          </Link>
          <Link to="/signup" className="inline-flex min-h-11 items-center text-market-accent">
            Create an account
          </Link>
        </div>
      </section>

      <aside className="sample-card h-fit" aria-labelledby="sample-access-heading">
        <h2 id="sample-access-heading" className="text-lg font-semibold text-ink">
          Sample access
        </h2>
        <p className="mt-2 text-sm text-copy">Want to explore the product immediately? Use these credentials:</p>
        <dl className="mt-4 space-y-4 text-sm">
          <div>
            <dt className="text-xs font-semibold uppercase tracking-classic text-copy">Email</dt>
            <dd className="mt-2 flex items-center justify-between gap-2 text-ink">
              <code className="min-w-0 break-all font-mono text-sm">{SAMPLE_ACCESS.email}</code>
              <button
                type="button"
                className="inline-flex min-h-11 min-w-[44px] shrink-0 items-center justify-center px-2 text-sm text-market-accent hover:text-ink"
                onClick={() => void copyValue('email', SAMPLE_ACCESS.email)}
              >
                Copy email
              </button>
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase tracking-classic text-copy">Password</dt>
            <dd className="mt-2 flex items-center justify-between gap-2 text-ink">
              <code className="min-w-0 break-all font-mono text-sm">{SAMPLE_ACCESS.password}</code>
              <button
                type="button"
                className="inline-flex min-h-11 min-w-[44px] shrink-0 items-center justify-center px-2 text-sm text-market-accent hover:text-ink"
                onClick={() => void copyValue('password', SAMPLE_ACCESS.password)}
              >
                Copy password
              </button>
            </dd>
          </div>
        </dl>
        <button
          type="button"
          className="market-btn-secondary mt-5 w-full"
          onClick={fillCredentials}
        >
          Fill credentials
        </button>
        <p id={copyStatusId} className="sr-only" aria-live="polite">
          {copyNotice}
        </p>
        {copyNotice && (
          <p className="mt-3 text-xs text-market-teal" aria-hidden="true">
            {copyNotice}
          </p>
        )}
      </aside>
    </div>
  );
}
