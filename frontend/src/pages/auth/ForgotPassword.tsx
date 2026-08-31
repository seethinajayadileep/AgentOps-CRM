import { Link } from 'react-router-dom';

export default function ForgotPassword() {
  return (
    <div className="auth-card mx-auto max-w-lg">
      <h1 className="font-serif text-4xl text-ink">Forgot password</h1>
      <p className="mt-4 text-copy">
        Password reset is not available yet. Sign in with your account, use sample access on the login
        page, or create a new account. No email is sent.
      </p>
      <Link to="/login" className="mt-8 inline-flex min-h-11 items-center text-sm text-market-accent">
        Return to login
      </Link>
    </div>
  );
}
