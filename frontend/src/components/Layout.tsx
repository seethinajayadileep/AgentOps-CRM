import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';

/**
 * Main layout component with sidebar and header.
 *
 * @version 0.2.0
 */
export default function Layout() {
  const location = useLocation();

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

  return (
    <div className="app-bg flex h-screen overflow-hidden">
      <Sidebar currentPath={location.pathname} />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header title={getPageTitle()} />
        <main className="flex-1 overflow-y-auto px-4 py-6 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-7xl animate-fade-in">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
