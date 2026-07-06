import { useEffect, useState } from 'react';
import { Phone, X } from 'lucide-react';
import { voiceCallsApi } from '../api/voiceCallsApi';
import type { VoiceCall, VoiceCallStatus, CallOutcome } from '../types/voiceCall';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import EmptyState from '../components/ui/EmptyState';
import LoadingState from '../components/ui/LoadingState';
import StatusBadge from '../components/ui/StatusBadge';

/**
 * Voice calls history page.
 *
 * @version 0.3.0
 * Feature: F-008 - Vapi Voice Call System
 */
export default function VoiceCalls() {
  const [calls, setCalls] = useState<VoiceCall[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<VoiceCallStatus | 'ALL'>('ALL');
  const [outcomeFilter, setOutcomeFilter] = useState<CallOutcome | 'ALL'>('ALL');
  // The call currently opened in the details modal (null = modal closed).
  const [selectedCall, setSelectedCall] = useState<VoiceCall | null>(null);

  useEffect(() => {
    loadCalls();
    // Poll periodically so in-progress calls update to their final status
    // (the backend syncs non-terminal calls from Vapi on each fetch).
    const interval = setInterval(() => loadCalls(true), 5000);
    return () => clearInterval(interval);
  }, []);

  const loadCalls = async (silent = false) => {
    try {
      if (!silent) setLoading(true);
      // Fetch calls across ALL businesses so the newest call always shows,
      // regardless of which business/lead it belongs to.
      const response = await voiceCallsApi.getAllCalls(0, 100);
      if (response && response.items) {
        setCalls(response.items);
      } else {
        setCalls([]);
      }
      setError(null);
    } catch (err: any) {
      console.error('Error loading calls:', err);
      setError(err.message || 'Failed to load voice calls');
      if (!silent) setCalls([]);
    } finally {
      if (!silent) setLoading(false);
    }
  };

  const filteredCalls = calls.filter((call) => {
    if (statusFilter !== 'ALL' && call.status !== statusFilter) return false;
    if (outcomeFilter !== 'ALL' && call.outcome !== outcomeFilter) return false;
    return true;
  });

  // Build a short, human-readable description of what happened on the call so
  // the CRM maintainer can understand it at a glance. Prefer the AI summary;
  // fall back to a snippet of the transcript.
  const getCallDescription = (call: VoiceCall): string => {
    const text = (call.summary || call.transcript || '').replace(/\s+/g, ' ').trim();
    if (!text) return '';
    const max = 140;
    return text.length > max ? `${text.slice(0, max)}…` : text;
  };

  const formatDuration = (seconds?: number): string =>
    seconds ? `${Math.floor(seconds / 60)}m ${seconds % 60}s` : '-';

  if (loading) {
    return <LoadingState label="Loading voice calls…" />;
  }

  return (
    <div>
      <PageHeader title="Voice Calls" subtitle="View AI voice call history and transcripts" />

      {/* Filters */}
      <Card className="mb-6 p-4">
        <div className="flex flex-wrap gap-4">
          <div>
            <label className="label-dark">Status</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as VoiceCallStatus | 'ALL')}
              className="input-dark w-44"
            >
              <option value="ALL">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="STARTED">Started</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="COMPLETED">Completed</option>
              <option value="FAILED">Failed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
          <div>
            <label className="label-dark">Outcome</label>
            <select
              value={outcomeFilter}
              onChange={(e) => setOutcomeFilter(e.target.value as CallOutcome | 'ALL')}
              className="input-dark w-44"
            >
              <option value="ALL">All Outcomes</option>
              <option value="ANSWERED">Answered</option>
              <option value="NO_ANSWER">No Answer</option>
              <option value="BUSY">Busy</option>
              <option value="VOICEMAIL">Voicemail</option>
              <option value="FAILED">Failed</option>
            </select>
          </div>
        </div>
      </Card>

      {error && (
        <div className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-red-300">Error: {error}</div>
      )}

      {filteredCalls.length === 0 ? (
        <EmptyState
          icon={<Phone size={26} />}
          title="No voice calls found"
          description="Voice calls will appear here after they are started."
        />
      ) : (
        <div className="table-card">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="border-b border-white/[0.06] bg-white/[0.02]">
                <tr>
                  {['Lead', 'Phone', 'Status', 'Outcome', 'Summary', 'Duration', 'Date', ''].map((h, i) => (
                    <th
                      key={h || `col-${i}`}
                      className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-zinc-400"
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filteredCalls.map((call) => (
                  <tr
                    key={call.id}
                    className="border-b border-white/[0.04] transition-colors duration-200 hover:bg-white/[0.03]"
                  >
                    <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-zinc-100">
                      {call.leadName || 'Unknown'}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-zinc-400">{call.phoneNumber}</td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <StatusBadge status={call.status} />
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      {call.outcome ? <StatusBadge status={call.outcome} /> : <span className="text-zinc-600">-</span>}
                    </td>
                    <td className="max-w-sm px-6 py-4 text-sm text-zinc-400">
                      {getCallDescription(call) ? (
                        <span className="line-clamp-2" title={call.summary || call.transcript || ''}>
                          {getCallDescription(call)}
                        </span>
                      ) : (
                        <span className="text-zinc-600">-</span>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-zinc-400">
                      {formatDuration(call.durationSeconds)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-zinc-400">
                      {new Date(call.createdAt).toLocaleString()}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm">
                      <button
                        onClick={() => setSelectedCall(call)}
                        className="rounded-lg border border-white/10 px-3 py-1.5 text-xs font-medium text-indigo-300 transition-colors hover:bg-indigo-500/10"
                      >
                        View
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Call details modal */}
      {selectedCall && (
        <CallDetailsModal
          call={selectedCall}
          onClose={() => setSelectedCall(null)}
          formatDuration={formatDuration}
        />
      )}
    </div>
  );
}

/**
 * Modal showing the full details of a voice call: status, outcome, timing,
 * the AI-generated summary, the full conversation transcript, and a link to
 * the recording when available.
 */
function CallDetailsModal({
  call,
  onClose,
  formatDuration,
}: {
  call: VoiceCall;
  onClose: () => void;
  formatDuration: (s?: number) => string;
}) {
  // Turn the raw transcript into styled conversation lines when it follows the
  // "Speaker: text" format that Vapi produces (AI: … / User: …).
  const transcriptLines = (call.transcript || '')
    .split('\n')
    .map((l) => l.trim())
    .filter(Boolean);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={onClose}
    >
      <div
        className="flex max-h-[85vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl border border-white/10 bg-zinc-900 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between border-b border-white/[0.06] p-6">
          <div>
            <h2 className="text-lg font-semibold text-zinc-100">
              Call with {call.leadName || 'Unknown'}
            </h2>
            <p className="mt-1 text-sm text-zinc-400">{call.phoneNumber}</p>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-zinc-400 transition-colors hover:bg-white/10 hover:text-zinc-100"
            aria-label="Close"
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 space-y-6 overflow-y-auto p-6">
          {/* Meta grid */}
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div>
              <p className="mb-1 text-xs uppercase tracking-wider text-zinc-500">Status</p>
              <StatusBadge status={call.status} />
            </div>
            <div>
              <p className="mb-1 text-xs uppercase tracking-wider text-zinc-500">Outcome</p>
              {call.outcome ? <StatusBadge status={call.outcome} /> : <span className="text-zinc-600">-</span>}
            </div>
            <div>
              <p className="mb-1 text-xs uppercase tracking-wider text-zinc-500">Duration</p>
              <p className="text-sm text-zinc-200">{formatDuration(call.durationSeconds)}</p>
            </div>
            <div>
              <p className="mb-1 text-xs uppercase tracking-wider text-zinc-500">Date</p>
              <p className="text-sm text-zinc-200">{new Date(call.createdAt).toLocaleString()}</p>
            </div>
          </div>

          {call.failureReason && (
            <div className="rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-300">
              <span className="font-medium">Failure reason: </span>
              {call.failureReason}
            </div>
          )}

          {/* AI summary */}
          <div>
            <p className="mb-2 text-xs uppercase tracking-wider text-zinc-500">Summary</p>
            {call.summary ? (
              <p className="rounded-lg bg-white/[0.03] p-3 text-sm leading-relaxed text-zinc-200">
                {call.summary}
              </p>
            ) : (
              <p className="text-sm text-zinc-500">No summary available for this call.</p>
            )}
          </div>

          {/* Full conversation transcript */}
          <div>
            <p className="mb-2 text-xs uppercase tracking-wider text-zinc-500">Conversation</p>
            {transcriptLines.length > 0 ? (
              <div className="space-y-2 rounded-lg bg-white/[0.03] p-3">
                {transcriptLines.map((line, idx) => {
                  const isAI = /^(ai|assistant|bot)\s*:/i.test(line);
                  const isUser = /^(user|customer|human)\s*:/i.test(line);
                  const [speaker, ...rest] = line.split(':');
                  const body = rest.join(':').trim();
                  return (
                    <div key={idx} className="text-sm leading-relaxed">
                      {body ? (
                        <>
                          <span
                            className={
                              isAI
                                ? 'font-medium text-indigo-300'
                                : isUser
                                ? 'font-medium text-emerald-300'
                                : 'font-medium text-zinc-300'
                            }
                          >
                            {speaker.trim()}:
                          </span>{' '}
                          <span className="text-zinc-200">{body}</span>
                        </>
                      ) : (
                        <span className="text-zinc-200">{line}</span>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="text-sm text-zinc-500">No transcript available for this call.</p>
            )}
          </div>

          {/* Recording */}
          {call.recordingUrl && (
            <div>
              <p className="mb-2 text-xs uppercase tracking-wider text-zinc-500">Recording</p>
              <audio controls src={call.recordingUrl} className="w-full">
                Your browser does not support audio playback.{' '}
                <a href={call.recordingUrl} target="_blank" rel="noreferrer" className="text-indigo-300 underline">
                  Open recording
                </a>
              </audio>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end border-t border-white/[0.06] p-4">
          <button
            onClick={onClose}
            className="rounded-lg border border-white/10 px-4 py-2 text-sm font-medium text-zinc-200 transition-colors hover:bg-white/10"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
