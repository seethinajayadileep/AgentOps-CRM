import { useEffect, useState } from 'react';
import {
  Building2,
  Users,
  MessageSquare,
  Phone,
  CheckCircle,
  Clock,
  Activity,
} from 'lucide-react';
import axios from '../api/axios';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import Badge, { type BadgeColor } from '../components/ui/Badge';
import EmptyState from '../components/ui/EmptyState';

/**
 * Dashboard overview: live KPIs and a recent-activity timeline.
 */
interface ActivityItem {
  agentName: string;
  action: string;
  status: string | null;
  createdAt: string | null;
}

interface Trend {
  direction: 'up' | 'down' | 'flat' | 'alert';
  label: string;
}

interface DashboardStats {
  activeBusinesses: number;
  totalLeads: number;
  conversations: number;
  voiceCalls: number;
  pendingApprovals: number;
  agentActionsToday: number;
  businessesTrend?: Trend;
  leadsTrend?: Trend;
  conversationsTrend?: Trend;
  voiceCallsTrend?: Trend;
  pendingApprovalsTrend?: Trend;
  agentActionsTrend?: Trend;
  recentActivity: ActivityItem[];
}

const EMPTY_STATS: DashboardStats = {
  activeBusinesses: 0,
  totalLeads: 0,
  conversations: 0,
  voiceCalls: 0,
  pendingApprovals: 0,
  agentActionsToday: 0,
  recentActivity: [],
};

function formatRelativeTime(iso: string | null): string {
  if (!iso) return 'Unknown time';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  const diffMs = Date.now() - date.getTime();
  const minutes = Math.max(0, Math.round(diffMs / 60000));
  if (minutes < 1) return 'Just now';
  if (minutes === 1) return '1 min ago';
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours === 1) return '1 hr ago';
  if (hours < 24) return `${hours} hr ago`;
  const days = Math.round(hours / 24);
  if (days === 1) return 'Yesterday';
  if (days < 7) return `${days} days ago`;
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

function formatAction(action: string): string {
  const sentence = action.replace(/_/g, ' ').toLowerCase();
  return sentence.charAt(0).toUpperCase() + sentence.slice(1);
}

function activityStatus(
  action: string,
  status: string | null
): { label: string; color: BadgeColor } {
  const a = (action || '').toUpperCase();
  const s = (status || '').toUpperCase();

  if (a.includes('UNSAFE') || a.includes('BLOCKED')) {
    return { label: 'Protected', color: 'gold' };
  }
  if (a.includes('FALLBACK') || s.includes('FALLBACK')) {
    return { label: 'Fallback used', color: 'amber' };
  }
  if (a.includes('FAILED') || a.includes('ERROR') || s === 'ERROR' || s === 'FAILED') {
    return { label: 'Failed', color: 'red' };
  }
  if (s === 'PENDING' || s === 'STARTED' || s === 'RUNNING' || s === 'IN_PROGRESS') {
    return { label: 'In progress', color: 'amber' };
  }
  if (s === 'SUCCESS' || s === 'COMPLETED' || s === 'APPROVED') {
    return { label: 'Succeeded', color: 'green' };
  }
  if (!status) return { label: 'Logged', color: 'gray' };
  return { label: formatAction(status), color: 'gray' };
}

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadStats();
    const interval = setInterval(loadStats, 5000);
    return () => clearInterval(interval);
  }, []);

  const loadStats = async () => {
    try {
      const response = await axios.get('/dashboard/stats');
      setStats({ ...EMPTY_STATS, ...response.data });
      setError(null);
    } catch (err) {
      setError('Dashboard metrics could not be loaded. Counts are hidden until the API responds.');
    } finally {
      setLoading(false);
    }
  };

  const pending = stats?.pendingApprovals ?? 0;
  const fallback = (direction: Trend['direction'], label: string): Trend => ({ direction, label });
  const metrics = stats
    ? [
        {
          label: 'Active Businesses',
          value: stats.activeBusinesses,
          icon: Building2,
          caption: 'On the platform',
          trend: stats.businessesTrend ?? fallback('flat', 'No comparison yet'),
        },
        {
          label: 'Total Leads',
          value: stats.totalLeads,
          icon: Users,
          caption: 'Qualified and new',
          trend: stats.leadsTrend ?? fallback('flat', 'No comparison yet'),
        },
        {
          label: 'Conversations',
          value: stats.conversations,
          icon: MessageSquare,
          caption: 'All channels',
          trend: stats.conversationsTrend ?? fallback('flat', 'No comparison yet'),
        },
        {
          label: 'Voice Calls',
          value: stats.voiceCalls,
          icon: Phone,
          caption: 'Completed and queued',
          trend: stats.voiceCallsTrend ?? fallback('flat', 'No comparison yet'),
        },
        {
          label: 'Pending Approvals',
          value: pending,
          icon: CheckCircle,
          caption: pending > 0 ? 'Needs review' : 'All clear',
          trend:
            stats.pendingApprovalsTrend ??
            fallback(pending > 0 ? 'alert' : 'flat', pending > 0 ? `${pending} awaiting action` : 'None awaiting'),
        },
        {
          label: 'Agent Actions Today',
          value: stats.agentActionsToday,
          icon: Clock,
          caption: 'Since midnight',
          trend: stats.agentActionsTrend ?? fallback('flat', 'No comparison yet'),
        },
      ]
    : [];

  return (
    <div>
      <PageHeader
        title="Dashboard"
        subtitle="Monitor agent performance, pipeline volume, and work that needs your attention."
      />

      {error && (
        <div className="mb-4 rounded-sm border border-frost bg-mist p-4 text-ink" role="alert">
          {error}
        </div>
      )}

      <section aria-label="Key metrics">
        <div className="grid grid-cols-1 gap-4 min-[480px]:grid-cols-2 lg:grid-cols-3">
          {loading && !stats &&
            ['Active Businesses', 'Total Leads', 'Conversations', 'Voice Calls', 'Pending Approvals', 'Agent Actions Today'].map(
              (label) => (
                <Card key={label} className="px-4 py-3 sm:p-5">
                  <p className="text-[12px] font-semibold uppercase tracking-classic text-copy">{label}</p>
                  <div
                    className="mt-3 h-8 w-16 animate-pulse rounded-sm bg-frost"
                    data-testid="dashboard-metric-skeleton"
                    aria-hidden="true"
                  />
                  <p className="sr-only">Loading {label}</p>
                </Card>
              )
            )}
          {metrics.map((metric) => {
            const Icon = metric.icon;
            const trendClass =
              metric.trend.direction === 'up'
                ? 'trend-up'
                : metric.trend.direction === 'down'
                ? 'trend-down'
                : metric.trend.direction === 'alert'
                ? 'trend-alert'
                : 'trend-flat';
            const trendPrefix =
              metric.trend.direction === 'up'
                ? '↑ '
                : metric.trend.direction === 'down'
                ? '↓ '
                : '';

            return (
              <Card key={metric.label} className="px-4 py-3 sm:p-5">
                <div className="flex items-start justify-between gap-3">
                  <p className="text-[12px] font-semibold uppercase tracking-classic text-copy sm:text-[13px]">
                    {metric.label}
                  </p>
                  <span
                    className="flex h-7 w-7 flex-shrink-0 items-center justify-center border border-frost bg-pale-navy text-navy sm:h-10 sm:w-10"
                    aria-hidden="true"
                  >
                    <Icon className="h-3.5 w-3.5 sm:h-4 sm:w-4" strokeWidth={1.75} />
                  </span>
                </div>
                <p
                  className={`tabular mt-1.5 font-sans text-[26px] font-semibold leading-none sm:mt-3 sm:text-[32px] ${
                    metric.trend.direction === 'alert' ? 'metric-gold' : 'text-ink'
                  }`}
                >
                  {metric.value.toLocaleString()}
                </p>
                <p className={`mt-1.5 text-[13px] font-semibold tabular sm:mt-2 sm:text-sm ${trendClass}`}>
                  {trendPrefix}
                  {metric.trend.label}
                </p>
                <p className="mt-0.5 text-[13px] text-copy sm:mt-1 sm:text-sm">{metric.caption}</p>
              </Card>
            );
          })}
        </div>
      </section>

      <section className="mt-6" aria-labelledby="recent-activity-heading">
        <Card className="p-0">
          <div className="flex items-end justify-between border-b border-frost px-5 py-4">
            <div>
              <h2 id="recent-activity-heading" className="font-serif text-2xl text-ink">
                Recent Activity
              </h2>
              <p className="mt-1 text-sm text-copy">Latest agent actions across the workspace.</p>
            </div>
          </div>

          {loading && !stats ? (
            <div className="px-5 py-8" data-testid="dashboard-activity-skeleton">
              <div className="h-4 w-48 animate-pulse rounded-sm bg-frost" />
            </div>
          ) : !stats || stats.recentActivity.length === 0 ? (
            <EmptyState
              icon={<Activity size={22} strokeWidth={1.75} />}
              title="No recent activity"
              description="Agent actions will appear here as conversations, qualifications, and calls run."
            />
          ) : (
            <ol className="divide-y divide-frost">
              {stats.recentActivity.map((item, idx) => {
                const status = activityStatus(item.action, item.status);
                return (
                  <li key={`${item.createdAt}-${idx}`} className="flex gap-4 px-5 py-4">
                    <div className="flex w-8 flex-shrink-0 flex-col items-center" aria-hidden="true">
                      <span
                        className={`mt-1.5 h-2.5 w-2.5 rounded-full ${
                          status.color === 'green'
                            ? 'bg-success'
                            : status.color === 'red'
                            ? 'bg-error'
                            : status.color === 'gold'
                            ? 'bg-gold'
                            : status.color === 'amber'
                            ? 'bg-warning'
                            : 'bg-slate'
                        }`}
                      />
                      {idx < stats.recentActivity.length - 1 && (
                        <span className="mt-2 w-px flex-1 bg-frost" />
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <p className="font-medium text-ink">{item.agentName}</p>
                        <time
                          className="text-sm font-medium text-copy"
                          dateTime={item.createdAt || undefined}
                          title={item.createdAt ? new Date(item.createdAt).toLocaleString() : undefined}
                        >
                          {formatRelativeTime(item.createdAt)}
                        </time>
                      </div>
                      <p className="mt-1 text-[15px] text-copy">{formatAction(item.action)}</p>
                      <div className="mt-2">
                        <Badge color={status.color} className="normal-case tracking-normal">
                          {status.label}
                        </Badge>
                      </div>
                    </div>
                  </li>
                );
              })}
            </ol>
          )}
        </Card>
      </section>
    </div>
  );
}
