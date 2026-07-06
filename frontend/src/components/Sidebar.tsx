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
  Sparkles,
} from 'lucide-react';

interface SidebarProps {
  currentPath: string;
}

/**
 * Sidebar navigation component (dark glass).
 *
 * @version 0.2.0
 */
export default function Sidebar({ currentPath }: SidebarProps) {
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

  const isActivePath = (path: string) =>
    currentPath === path || currentPath.startsWith(path + '/');

  return (
    <aside className="flex w-64 flex-shrink-0 flex-col border-r border-white/[0.06] bg-black/40 backdrop-blur-xl">
      <div className="flex items-center gap-3 border-b border-white/[0.06] px-6 py-5">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-[#8B5CF6] to-[#3B82F6] shadow-[0_0_20px_rgba(139,92,246,0.35)]">
          <Sparkles size={20} className="text-white" />
        </div>
        <div>
          <h1 className="text-base font-bold text-white">AgentOps CRM</h1>
          <p className="text-xs text-zinc-500">Multi-Agent AI Platform</p>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto p-3">
        <ul className="space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = isActivePath(item.path);

            return (
              <li key={item.path}>
                <Link
                  to={item.path}
                  className={`
                    group flex items-center gap-3 rounded-xl px-4 py-2.5 text-sm font-medium
                    transition-all duration-200
                    ${
                      isActive
                        ? 'bg-gradient-to-r from-[#8B5CF6]/90 to-[#3B82F6]/90 text-white shadow-[0_0_18px_rgba(139,92,246,0.3)]'
                        : 'text-zinc-400 hover:bg-white/5 hover:text-zinc-100'
                    }
                  `}
                >
                  <Icon
                    size={18}
                    className={isActive ? 'text-white' : 'text-zinc-500 group-hover:text-zinc-200'}
                  />
                  <span>{item.label}</span>
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>

      <div className="border-t border-white/[0.06] p-4">
        <div className="text-xs text-zinc-600">
          <p>Version 0.2.0</p>
          <p className="mt-0.5">© 2026 AgentOps Team</p>
        </div>
      </div>
    </aside>
  );
}
