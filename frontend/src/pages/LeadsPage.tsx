import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Users } from 'lucide-react';
import { leadsApi } from '../api/leadsApi';
import type { Lead } from '../types/lead';
import { LeadStatusBadge } from '../components/leads/LeadStatusBadge';
import { LeadScoreBadge } from '../components/leads/LeadScoreBadge';
import PageHeader from '../components/ui/PageHeader';
import EmptyState from '../components/ui/EmptyState';
import LoadingState from '../components/ui/LoadingState';

export default function LeadsPage() {
  const [leads, setLeads] = useState<Lead[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadLeads();
  }, []);

  const loadLeads = async () => {
    try {
      setLoading(true);
      const data = await leadsApi.getAllLeads();
      setLeads(data);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to load leads');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <LoadingState label="Loading leads…" />;
  }

  if (error) {
    return (
      <div className="rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">Error: {error}</div>
    );
  }

  return (
    <div>
      <PageHeader title="Leads" subtitle="Manage qualified leads from customer conversations" />

      {leads.length === 0 ? (
        <EmptyState
          icon={<Users size={26} />}
          title="No leads found"
          description="Leads will appear here when customers show buying intent"
        />
      ) : (
        <div className="table-card">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="border-b border-white/[0.06] bg-white/[0.02]">
                <tr>
                  {['Name', 'Requirement', 'Score', 'Status', 'Created', 'Actions'].map((h) => (
                    <th
                      key={h}
                      className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-zinc-400"
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {leads.map((lead) => (
                  <tr
                    key={lead.id}
                    className="border-b border-white/[0.04] transition-colors duration-200 hover:bg-white/[0.03]"
                  >
                    <td className="whitespace-nowrap px-6 py-4">
                      <div className="text-sm font-medium text-zinc-100">{lead.name}</div>
                      {lead.email && <div className="text-sm text-zinc-500">{lead.email}</div>}
                      {lead.phone && <div className="text-sm text-zinc-500">{lead.phone}</div>}
                    </td>
                    <td className="px-6 py-4">
                      <div className="max-w-xs truncate text-sm text-zinc-300">{lead.requirementText || '-'}</div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <LeadScoreBadge score={lead.leadScore} />
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <LeadStatusBadge status={lead.status} />
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-zinc-500">
                      {new Date(lead.createdAt).toLocaleDateString()}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm">
                      <Link to={`/leads/${lead.id}`} className="font-medium text-primary-300 hover:text-primary-200">
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
