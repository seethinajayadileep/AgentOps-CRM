import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import BrandMark from '../brand/BrandMark';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';

const links = [
  { to: '/features', label: 'Features' },
  { to: '/how-it-works', label: 'How it works' },
  { to: '/pricing', label: 'Pricing' },
  { to: '/contact', label: 'Contact' },
];

const pageTitles: Record<string, string> = {
  '/': 'AgentOps CRM',
  '/features': 'Features · AgentOps CRM',
  '/how-it-works': 'How it works · AgentOps CRM',
  '/pricing': 'Pricing · AgentOps CRM',
  '/contact': 'Contact · AgentOps CRM',
};

export default function MarketingLayout() {
  const [open, setOpen] = useState(false);
  const location = useLocation();
  useDocumentTitle(pageTitles[location.pathname] ?? 'AgentOps CRM');

  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  return (
    <div className="site-shell min-h-screen overflow-x-hidden">
      <a href="#main-content" className="skip-link">
        Skip to main content
      </a>
      <header className="border-b border-[var(--border)] bg-[var(--surface)]">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4 sm:px-6">
          <BrandMark />
          <nav className="hidden items-center gap-6 md:flex" aria-label="Product">
            {links.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) =>
                  `inline-flex min-h-11 items-center text-sm ${
                    isActive ? 'font-semibold text-ink' : 'text-copy hover:text-ink'
                  }`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="hidden items-center gap-3 md:flex">
            <Link to="/login" className="inline-flex min-h-11 items-center text-sm text-copy hover:text-ink">
              Login
            </Link>
            <Link to="/signup" className="market-btn-primary">
              Get Started
            </Link>
          </div>
          <button
            type="button"
            className="inline-flex h-11 w-11 items-center justify-center text-ink md:hidden"
            aria-label={open ? 'Close menu' : 'Open menu'}
            aria-expanded={open}
            aria-controls="marketing-mobile-nav"
            onClick={() => setOpen((value) => !value)}
          >
            {open ? <X size={20} aria-hidden="true" /> : <Menu size={20} aria-hidden="true" />}
          </button>
        </div>
        {open && (
          <nav
            id="marketing-mobile-nav"
            className="border-t border-[var(--border)] bg-[var(--surface)] px-4 py-4 md:hidden"
            aria-label="Mobile"
          >
            <ul className="space-y-2">
              {links.map((link) => (
                <li key={link.to}>
                  <NavLink
                    to={link.to}
                    onClick={() => setOpen(false)}
                    className="block min-h-11 py-2 text-ink"
                  >
                    {link.label}
                  </NavLink>
                </li>
              ))}
              <li>
                <Link to="/login" onClick={() => setOpen(false)} className="block min-h-11 py-2 text-ink">
                  Login
                </Link>
              </li>
              <li>
                <Link to="/signup" onClick={() => setOpen(false)} className="block min-h-11 py-2 text-market-accent">
                  Get Started
                </Link>
              </li>
            </ul>
          </nav>
        )}
      </header>
      <main id="main-content" tabIndex={-1}>
        <Outlet />
      </main>
      <footer className="site-footer">
        <div className="mx-auto grid max-w-6xl gap-6 px-4 py-10 sm:grid-cols-3 sm:px-6">
          <div>
            <BrandMark inverted showSupport />
            <p className="mt-3 max-w-sm text-sm text-hero-muted">
              An accountable workspace for leads, conversations, approvals and AI-assisted follow-up.
            </p>
          </div>
          <div>
            <p className="text-xs font-semibold uppercase tracking-classic text-hero-muted">Product</p>
            <ul className="mt-3 space-y-2 text-sm">
              {links.map((link) => (
                <li key={link.to}>
                  <Link to={link.to} className="inline-flex min-h-11 items-center text-hero-muted hover:text-hero-text">
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
          <div>
            <p className="text-xs font-semibold uppercase tracking-classic text-hero-muted">Account</p>
            <ul className="mt-3 space-y-2 text-sm">
              <li>
                <Link to="/login" className="inline-flex min-h-11 items-center text-hero-muted hover:text-hero-text">
                  Login
                </Link>
              </li>
              <li>
                <Link to="/signup" className="inline-flex min-h-11 items-center text-hero-muted hover:text-hero-text">
                  Sign up
                </Link>
              </li>
            </ul>
          </div>
        </div>
      </footer>
    </div>
  );
}
