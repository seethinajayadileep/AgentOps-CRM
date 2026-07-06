import { Bell, Search } from 'lucide-react';

interface HeaderProps {
  title: string;
}

/**
 * Header component with search and notifications (dark glass).
 *
 * @version 0.2.0
 */
export default function Header({ title }: HeaderProps) {
  return (
    <header className="border-b border-white/[0.06] bg-black/30 px-4 py-4 backdrop-blur-xl sm:px-6">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-xl font-semibold text-white">{title}</h2>

        <div className="flex items-center gap-3">
          {/* Search */}
          <div className="relative hidden sm:block">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
            <input
              type="text"
              placeholder="Search…"
              className="w-56 rounded-xl border border-white/[0.08] bg-white/[0.04] py-2 pl-9 pr-4 text-sm text-zinc-100 placeholder-zinc-500 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary-500/60"
            />
          </div>

          {/* Notifications */}
          <button className="relative rounded-xl p-2 text-zinc-400 transition-colors duration-200 hover:bg-white/5 hover:text-zinc-100">
            <Bell size={18} />
            <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-[#8B5CF6]" />
          </button>

          {/* User Avatar */}
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-[#8B5CF6] to-[#3B82F6] text-sm font-semibold text-white">
            AD
          </div>
        </div>
      </div>
    </header>
  );
}
