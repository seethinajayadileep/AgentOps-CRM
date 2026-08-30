import { useEffect, useId, useRef, useState } from 'react';
import { Bell, Menu, User, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import GlobalSearch from './GlobalSearch';
import { getAllApprovals } from '../api/approvalsApi';
import { ApprovalStatus } from '../types/approval';

interface HeaderProps {
  title: string;
  navOpen?: boolean;
  onToggleNav?: () => void;
}

export default function Header({ title, navOpen = false, onToggleNav }: HeaderProps) {
  const [noticeOpen, setNoticeOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [pendingCount, setPendingCount] = useState(0);
  const noticeId = useId();
  const profileId = useId();
  const noticeRef = useRef<HTMLDivElement>(null);
  const profileRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let cancelled = false;
    getAllApprovals({ status: ApprovalStatus.PENDING })
      .then((rows) => {
        if (!cancelled) setPendingCount(rows.length);
      })
      .catch(() => {
        if (!cancelled) setPendingCount(0);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const onDoc = (event: MouseEvent) => {
      const target = event.target as Node;
      if (noticeRef.current && !noticeRef.current.contains(target)) setNoticeOpen(false);
      if (profileRef.current && !profileRef.current.contains(target)) setProfileOpen(false);
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setNoticeOpen(false);
        setProfileOpen(false);
      }
    };
    document.addEventListener('mousedown', onDoc);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDoc);
      document.removeEventListener('keydown', onKey);
    };
  }, []);

  return (
    <header className="border-b border-frost bg-snow px-4 sm:px-6">
      <div className="flex min-h-[64px] items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <button
            type="button"
            className="inline-flex h-11 w-11 items-center justify-center border border-frost text-navy hover:bg-pale-navy lg:hidden"
            aria-label={navOpen ? 'Close navigation' : 'Open navigation'}
            aria-expanded={navOpen}
            aria-controls="app-sidebar"
            onClick={onToggleNav}
          >
            {navOpen ? <X size={20} strokeWidth={1.75} /> : <Menu size={20} strokeWidth={1.75} />}
          </button>
          <p className="truncate font-serif text-xl text-ink lg:hidden">CRM</p>
          <span className="sr-only">Current page: {title}</span>
        </div>

        <div className="flex items-center gap-2 sm:gap-3">
          <GlobalSearch />

          <div className="relative" ref={noticeRef}>
            <button
              type="button"
              className="relative inline-flex h-11 w-11 items-center justify-center border border-frost text-navy hover:bg-pale-navy"
              aria-label={
                pendingCount > 0
                  ? `Notifications, ${pendingCount} pending approvals`
                  : 'Notifications, none pending'
              }
              aria-expanded={noticeOpen}
              aria-controls={noticeId}
              onClick={() => {
                setNoticeOpen((open) => !open);
                setProfileOpen(false);
              }}
            >
              <Bell size={18} strokeWidth={1.75} aria-hidden="true" />
              {pendingCount > 0 && (
                <span className="absolute right-2.5 top-2.5 h-2 w-2 rounded-full bg-gold" aria-hidden="true" />
              )}
            </button>
            {noticeOpen && (
              <div
                id={noticeId}
                role="menu"
                aria-label="Notifications"
                className="absolute right-0 z-50 mt-2 w-72 border border-frost bg-snow p-3 shadow-classic"
              >
                {pendingCount > 0 ? (
                  <Link
                    role="menuitem"
                    to="/approvals"
                    className="block px-2 py-2 text-sm text-ink hover:bg-pale-navy"
                    onClick={() => setNoticeOpen(false)}
                  >
                    {pendingCount} pending approval{pendingCount === 1 ? '' : 's'} need review
                  </Link>
                ) : (
                  <p className="px-2 py-2 text-sm text-copy">No notifications yet.</p>
                )}
              </div>
            )}
          </div>

          <div className="relative" ref={profileRef}>
            <button
              type="button"
              className="flex items-center gap-2 border border-frost p-0 hover:bg-pale-navy"
              aria-label="Account menu for Alex Drake"
              aria-expanded={profileOpen}
              aria-controls={profileId}
              aria-haspopup="menu"
              onClick={() => {
                setProfileOpen((open) => !open);
                setNoticeOpen(false);
              }}
            >
              <span
                className="flex h-11 w-11 flex-shrink-0 items-center justify-center text-xs font-semibold"
                style={{ backgroundColor: '#243B53', color: '#FFFEFC' }}
                aria-hidden="true"
              >
                AD
              </span>
              <span className="hidden pr-3 text-sm font-medium text-ink sm:inline">Alex Drake</span>
            </button>
            {profileOpen && (
              <div
                id={profileId}
                role="menu"
                aria-label="Account"
                className="absolute right-0 z-50 mt-2 w-56 border border-frost bg-snow py-2 shadow-classic"
              >
                <p className="px-3 py-1 text-xs uppercase tracking-classic text-slate">Signed in</p>
                <p className="px-3 pb-2 text-sm font-medium text-ink">Alex Drake</p>
                <Link
                  role="menuitem"
                  to="/settings"
                  className="flex items-center gap-2 px-3 py-2 text-sm text-ink hover:bg-pale-navy"
                  onClick={() => setProfileOpen(false)}
                >
                  <User size={14} aria-hidden="true" />
                  Settings
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
