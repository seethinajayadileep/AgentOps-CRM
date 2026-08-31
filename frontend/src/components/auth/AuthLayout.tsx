import { Link, Outlet, useLocation } from 'react-router-dom';
import BrandMark from '../brand/BrandMark';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';

const pageTitles: Record<string, string> = {
  '/login': 'Sign in · AgentOps CRM',
  '/signup': 'Create account · AgentOps CRM',
  '/forgot-password': 'Forgot password · AgentOps CRM',
};

export default function AuthLayout() {
  const location = useLocation();
  useDocumentTitle(pageTitles[location.pathname] ?? 'AgentOps CRM');

  return (
    <div className="site-shell min-h-screen overflow-x-hidden">
      <a href="#main-content" className="skip-link">
        Skip to main content
      </a>
      <header className="border-b border-[var(--border)] bg-[var(--surface)]">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4 sm:px-6">
          <BrandMark />
          <Link to="/" className="inline-flex min-h-11 items-center text-sm text-copy hover:text-ink">
            Back to site
          </Link>
        </div>
      </header>
      <main id="main-content" tabIndex={-1} className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
        <Outlet />
      </main>
    </div>
  );
}
