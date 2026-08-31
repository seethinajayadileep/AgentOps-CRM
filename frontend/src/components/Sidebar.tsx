import { Link } from 'react-router-dom';
import {
  LayoutDashboard,
  Building2,
  Users,
  Search,
  MessageSquare,
  Phone,
  CheckCircle,
  FileText,
  Settings as SettingsIcon,
} from 'lucide-react';
import { APP_VERSION } from '../config/version';

interface SidebarProps {
  currentPath: string;
  open?: boolean;
  onClose?: () => void;
}

const navItems = [
  { path: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { path: '/businesses', icon: Building2, label: 'Businesses' },
  { path: '/leads', icon: Users, label: 'Leads' },
  { path: '/lead-finder', icon: Search, label: 'Lead Finder' },
  { path: '/conversations', icon: MessageSquare, label: 'Conversations' },
  { path: '/voice-calls', icon: Phone, label: 'Voice Calls' },
  { path: '/approvals', icon: CheckCircle, label: 'Approvals' },
  { path: '/agent-logs', icon: FileText, label: 'Agent Logs' },
  { path: '/settings', icon: SettingsIcon, label: 'Settings' },
];

/**
 * Fixed sidebar with a 3px primary-blue active indicator and 44px touch targets.
 */
export default function Sidebar({ currentPath, open = false, onClose }: SidebarProps) {
  const isActivePath = (path: string) =>
    currentPath === path || currentPath.startsWith(path + '/');

  return (
    <aside
      id="app-sidebar"
      className={`
        fixed inset-y-0 left-0 z-40 flex w-64 flex-shrink-0 flex-col border-r border-frost shadow-sidebar
        transition-transform duration-200 lg:static lg:translate-x-0
        ${open ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
      `}
      style={{ backgroundColor: 'var(--app-sidebar)' }}
      aria-label="Primary"
    >
      <div className="border-b border-frost px-6 py-6">
        <Link to="/dashboard" aria-label="AgentOps CRM" className="inline-block">
          <span className="block text-[11px] font-semibold uppercase tracking-classic text-copy">AGENTOPS</span>
          <span className="mt-1 block font-serif text-[28px] leading-none text-ink">CRM</span>
        </Link>
        <span className="mt-2 block h-0.5 w-8 bg-gold" aria-hidden="true" />
        <p className="mt-2 text-sm font-medium text-copy">Agentic revenue operations platform</p>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-4" aria-label="Main">
        <ul className="space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = isActivePath(item.path);

            return (
              <li key={item.path}>
                <Link
                  to={item.path}
                  aria-current={isActive ? 'page' : undefined}
                  onClick={onClose}
                  className={`
                    group flex min-h-[44px] items-center gap-3 border-l-[3px] px-4 text-[15px]
                    transition-colors duration-150
                    ${
                      isActive
                        ? 'nav-link-active'
                        : 'border-transparent font-medium text-copy hover:bg-page hover:text-navy'
                    }
                  `}
                >
                  <Icon
                    size={18}
                    strokeWidth={isActive ? 2 : 1.75}
                    className={isActive ? 'text-[var(--primary)]' : 'text-copy group-hover:text-navy'}
                    aria-hidden="true"
                  />
                  <span>{item.label}</span>
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>

      <div className="border-t border-frost px-6 py-4">
        <p className="text-[11px] font-semibold uppercase tracking-classic text-copy">Version {APP_VERSION}</p>
        <p className="mt-1 text-xs text-copy">© 2026 AgentOps CRM</p>
      </div>
    </aside>
  );
}
