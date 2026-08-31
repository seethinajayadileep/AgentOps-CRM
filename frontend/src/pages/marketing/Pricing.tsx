import { Link } from 'react-router-dom';

export default function Pricing() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-16 sm:px-6">
      <h1 className="font-serif text-4xl text-ink">Pricing</h1>
      <p className="mt-4 max-w-2xl text-copy">
        Billing is not part of this build yet.
      </p>
      <div className="mt-8 border border-[var(--border)] bg-[var(--surface)] p-6 shadow-[var(--shadow-card)]">
        <h2 className="text-xl font-semibold text-ink">Get started</h2>
        <p className="mt-2 text-sm text-copy">
          Sign in with the sample credentials on the login page, or create an account. Crawl, voice,
          and lead search run when those providers are configured.
        </p>
        <Link to="/login" className="mt-6 inline-flex min-h-11 items-center text-sm text-market-accent">
          Go to login
        </Link>
      </div>
    </div>
  );
}
