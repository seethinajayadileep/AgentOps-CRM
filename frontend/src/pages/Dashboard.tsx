import { useEffect, useState } from 'react';
import {
  Building2,
  Users,
  MessageSquare,
  Phone,
  CheckCircle,
  Clock,
} from 'lucide-react';
import axios from '../api/axios';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';

/**
 * Dashboard page with live overview metrics.
 *
 * @version 0.3.0
 * Feature: F-000 - Project Foundation
 */
interface ActivityItem {
  agentName: string;
  action: string;
  status: string | null;
  createdAt: string | null;
}

interface DashboardStats {
  activeBusinesses: number;
  totalLeads: number;
  conversations: number;
  voiceCalls: number;
  pendingApprovals: number;
  agentActionsToday: number;
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

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats>(EMPTY_STATS);

  useEffect(() => {
    loadStats();
    // Refresh periodically so the dashboard stays live.
    const interval = setInterval(loadStats, 5000);
    return () => clearInterval(interval);
  }, []);

  const loadStats = async () => {
    try {
      const response = await axios.get('/dashboard/stats');
      setStats({ ...EMPTY_STATS, ...response.data });
    } catch (err) {
      console.error('Error loading dashboard stats:', err);
    }
  };

  const metrics = [
    { label: 'Active Businesses', value: stats.activeBusinesses, icon: Building2, gradient: 'from-[#8B5CF6] to-[#6D28D9]' },
    { label: 'Total Leads', value: stats.totalLeads, icon: Users, gradient: 'from-[#22C55E] to-[#16A34A]' },
    { label: 'Conversations', value: stats.conversations, icon: MessageSquare, gradient: 'from-[#3B82F6] to-[#2563EB]' },
    { label: 'Voice Calls', value: stats.voiceCalls, icon: Phone, gradient: 'from-[#06B6D4] to-[#0891B2]' },
    { label: 'Pending Approvals', value: stats.pendingApprovals, icon: CheckCircle, gradient: 'from-[#F59E0B] to-[#D97706]' },
    { label: 'Agent Actions Today', value: stats.agentActionsToday, icon: Clock, gradient: 'from-[#EC4899] to-[#DB2777]' },
  ];

  const formatActivityTime = (iso: string | null): string => {
    if (!iso) return '';
    try {
      return new Date(iso).toLocaleString();
    } catch {
      return iso;
    }
  };

  return (
    <div>
      <PageHeader title="Overview" subtitle="Monitor your AI agent performance" />

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
        {metrics.map((metric) => {
          const Icon = metric.icon;
          return (
            <Card key={metric.label} hover className="p-6">
              <div className="flex items-center justify-between">
                <div className={`flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br ${metric.gradient} shadow-[0_0_16px_rgba(139,92,246,0.25)]`}>
                  <Icon size={22} className="text-white" />
                </div>
              </div>
              <div className="mt-5">
                <p className="text-3xl font-bold text-white">{metric.value}</p>
                <p className="mt-1 text-sm text-zinc-400">{metric.label}</p>
              </div>
            </Card>
          );
        })}
      </div>

      {/* Recent Activity */}
      <Card className="mt-5 p-6">
        <h4 className="mb-4 text-lg font-semibold text-white">Recent Activity</h4>
        {stats.recentActivity.length === 0 ? (
          <p className="text-zinc-500">No recent activity to display.</p>
        ) : (
          <ul className="divide-y divide-white/[0.06]">
            {stats.recentActivity.map((item, idx) => (
              <li key={idx} className="flex items-center justify-between py-3">
                <div className="flex items-center gap-3">
                  <span
                    className={`inline-block h-2 w-2 rounded-full ${
                      item.status === 'SUCCESS'
                        ? 'bg-emerald-400'
                        : item.status === 'ERROR'
                        ? 'bg-red-400'
                        : 'bg-zinc-500'
                    }`}
                  />
                  <div>
                    <p className="text-sm font-medium text-zinc-100">{item.agentName}</p>
                    <p className="text-xs text-zinc-400">{item.action}</p>
                  </div>
                </div>
                <span className="text-xs text-zinc-500">{formatActivityTime(item.createdAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
