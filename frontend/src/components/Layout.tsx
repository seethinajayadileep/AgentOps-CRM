import { useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import ErrorBoundary from './ErrorBoundary';
import Sidebar from './Sidebar';
import Header from './Header';
import { useDocumentTitle } from '../hooks/useDocumentTitle';

/**
 * Main layout: fixed sidebar, utility header, and a 1200px content column.
 */
export default function Layout() {
  const location = useLocation();
  const [navOpen, setNavOpen] = useState(false);

  useEffect(() => {
    setNavOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setNavOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const getPageTitle = () => {
    const path = location.pathname;
    if (path.startsWith('/businesses')) return 'Businesses';
    if (path.startsWith('/leads')) return 'Leads';
    if (path.startsWith('/lead-finder')) return 'Lead Finder';
    switch (path) {
      case '/dashboard':
        return 'Dashboard';
      case '/conversations':
        return 'Conversations';
      case '/voice-calls':
        return 'Voice Calls';
      case '/approvals':
        return 'Approvals';
      case '/agent-logs':
      case '/agentlogs':
        return 'Agent Logs';
      case '/settings':
        return 'Settings';
      default:
        return 'AgentOps CRM';
    }
  };

  const pageTitle = getPageTitle();
  useDocumentTitle(pageTitle === 'AgentOps CRM' ? pageTitle : `${pageTitle} · AgentOps CRM`);

  return (
    <div className="app-bg flex h-screen overflow-hidden">
      <a href="#main-content" className="skip-link">
        Skip to main content
      </a>

      <Sidebar
        currentPath={location.pathname}
        open={navOpen}
        onClose={() => setNavOpen(false)}
      />

      {navOpen && (
        <button
          type="button"
          aria-label="Close navigation"
          className="fixed inset-0 z-30 bg-ink/40 lg:hidden"
          onClick={() => setNavOpen(false)}
        />
      )}

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <Header
          title={pageTitle}
          navOpen={navOpen}
          onToggleNav={() => setNavOpen((open) => !open)}
        />
        <main
          id="main-content"
          tabIndex={-1}
          className="flex-1 overflow-y-auto px-4 py-6 sm:px-6 lg:px-8"
        >
          <div className="mx-auto w-full max-w-content animate-fade-in">
            <ErrorBoundary key={location.pathname}>
              <Outlet />
            </ErrorBoundary>
          </div>
        </main>
      </div>
    </div>
  );
}
