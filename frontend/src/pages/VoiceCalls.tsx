import { useEffect, useState } from 'react';
import { Phone } from 'lucide-react';
import { voiceCallsApi } from '../api/voiceCallsApi';
import { safeClientError } from '../util/safeClientError';
import type { VoiceCall, VoiceCallStatus, CallOutcome } from '../types/voiceCall';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import EmptyState from '../components/ui/EmptyState';
import LoadingState from '../components/ui/LoadingState';
import StatusBadge from '../components/ui/StatusBadge';
import Modal from '../components/ui/Modal';

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

  const filtersActive = statusFilter !== 'ALL' || outcomeFilter !== 'ALL';

  const clearFilters = () => {
    setStatusFilter('ALL');
    setOutcomeFilter('ALL');
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
            <label htmlFor="voice-status-filter" className="label-dark">
              Status
            </label>
            <select
              id="voice-status-filter"
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
            <label htmlFor="voice-outcome-filter" className="label-dark">
              Outcome
            </label>
            <select
              id="voice-outcome-filter"
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
        <div className="mb-6 rounded-sm border border-frost bg-mist p-4 text-ink">Error: {error}</div>
      )}

      {filteredCalls.length === 0 ? (
        <EmptyState
          icon={<Phone size={26} />}
          title={
            filtersActive
              ? 'No voice calls match the selected filters.'
              : 'Voice calls will appear here…'
          }
          description={
            filtersActive
              ? 'Try a different status or outcome, or clear filters to see all calls.'
              : 'Voice calls will appear here after they are started.'
          }
          action={
            filtersActive ? (
              <button type="button" className="btn-secondary" onClick={clearFilters}>
                Clear Filters
              </button>
            ) : undefined
          }
        />
      ) : (
        <div className="table-card">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="border-b border-frost bg-mist">
                <tr>
                  {['Lead', 'Phone', 'Status', 'Outcome', 'Summary', 'Duration', 'Date', ''].map((h, i) => (
                    <th
                      key={h || `col-${i}`}
                      className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate"
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
                    className="border-b border-frost transition-colors duration-200 hover:bg-mist"
                  >
                    <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-ink">
                      {call.leadName || 'Unknown'}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate">{call.phoneNumber}</td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <StatusBadge status={call.status} />
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      {call.outcome ? <StatusBadge status={call.outcome} /> : <span className="text-silver">-</span>}
                    </td>
                    <td className="max-w-sm px-6 py-4 text-sm text-slate">
                      {getCallDescription(call) ? (
                        <span className="line-clamp-2" title={call.summary || call.transcript || ''}>
                          {getCallDescription(call)}
                        </span>
                      ) : (
                        <span className="text-silver">-</span>
                      )}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate">
                      {formatDuration(call.durationSeconds)}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate">
                      {new Date(call.createdAt).toLocaleString()}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm">
                      <button
                        onClick={() => setSelectedCall(call)}
                        className="rounded-lg border border-frost px-3 py-1.5 text-xs font-medium text-indigo-300 transition-colors hover:bg-indigo-500/10"
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
    <Modal title={`Call with ${call.leadName || 'Unknown'}`} onClose={onClose} className="max-w-2xl p-0">
      <div className="flex max-h-[85vh] flex-col overflow-hidden">
        <div className="flex-1 space-y-6 overflow-y-auto p-6">
          {call.phoneNumber && <p className="text-sm text-slate">{call.phoneNumber}</p>}
          {/* Meta grid */}
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div>
              <p className="mb-1 text-xs uppercase tracking-wider text-slate">Status</p>
              <StatusBadge status={call.status} />
            </div>
            <div>
              <p className="mb-1 text-xs uppercase tracking-wider text-slate">Outcome</p>
              {call.outcome ? <StatusBadge status={call.outcome} /> : <span className="text-silver">-</span>}
            </div>
            <div>
              <p className="mb-1 text-xs uppercase tracking-wider text-slate">Duration</p>
              <p className="text-sm text-ink">{formatDuration(call.durationSeconds)}</p>
            </div>
            <div>
              <p className="mb-1 text-xs uppercase tracking-wider text-slate">Date</p>
              <p className="text-sm text-ink">{new Date(call.createdAt).toLocaleString()}</p>
            </div>
          </div>

          {call.failureReason && (
            <div className="rounded-lg border border-frost bg-mist p-3 text-sm text-ink">
              <span className="font-medium">Failure reason: </span>
              {safeClientError(call.failureReason, 'This call failed. Check Voice settings and try again.')}
            </div>
          )}

          {/* AI summary */}
          <div>
            <p className="mb-2 text-xs uppercase tracking-wider text-slate">Summary</p>
            {call.summary ? (
              <p className="rounded-lg bg-mist p-3 text-sm leading-relaxed text-ink">
                {call.summary}
              </p>
            ) : (
              <p className="text-sm text-slate">No summary available for this call.</p>
            )}
          </div>

          {/* Full conversation transcript */}
          <div>
            <p className="mb-2 text-xs uppercase tracking-wider text-slate">Conversation</p>
            {transcriptLines.length > 0 ? (
              <div className="space-y-2 rounded-lg bg-mist p-3">
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
                                ? 'font-medium text-slate'
                                : 'font-medium text-ink'
                            }
                          >
                            {speaker.trim()}:
                          </span>{' '}
                          <span className="text-ink">{body}</span>
                        </>
                      ) : (
                        <span className="text-ink">{line}</span>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="text-sm text-slate">No transcript available for this call.</p>
            )}
          </div>

          {/* Recording */}
          {call.recordingUrl && <RecordingPlayer callId={call.id} />}
        </div>
      </div>
    </Modal>
  );
}

const RECORDING_UNAVAILABLE =
  'Recording could not be played. The file may still be processing or is no longer available.';

/**
 * Loads the recording through the CRM API as a blob so the player uses a
 * same-origin object URL. A cross-origin audio src stays at readyState 0.
 */
function RecordingPlayer({ callId }: { callId: string }) {
  const [playbackSrc, setPlaybackSrc] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [readyState, setReadyState] = useState(0);

  useEffect(() => {
    let cancelled = false;
    let objectUrl: string | null = null;

    setPlaybackSrc(null);
    setLoading(true);
    setError(null);
    setReadyState(0);

    voiceCallsApi
      .getRecording(callId)
      .then((blob) => {
        if (cancelled) {
          return;
        }
        if (!blob || blob.size === 0) {
          setError(RECORDING_UNAVAILABLE);
          setLoading(false);
          return;
        }
        objectUrl = URL.createObjectURL(blob);
        setPlaybackSrc(objectUrl);
        setLoading(false);
      })
      .catch(() => {
        if (cancelled) {
          return;
        }
        setError(RECORDING_UNAVAILABLE);
        setLoading(false);
      });

    return () => {
      cancelled = true;
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [callId]);

  return (
    <div>
      <p className="mb-2 text-xs uppercase tracking-wider text-slate">Recording</p>
      {loading && <p className="text-sm text-slate">Loading recording…</p>}
      {error && <p className="text-sm text-ink">{error}</p>}
      {playbackSrc && !error && (
        <audio
          controls
          preload="auto"
          src={playbackSrc}
          className="w-full"
          data-testid="voice-recording-player"
          data-ready-state={readyState}
          onLoadedMetadata={(event) => setReadyState(event.currentTarget.readyState)}
          onLoadedData={(event) => setReadyState(event.currentTarget.readyState)}
          onCanPlay={(event) => setReadyState(event.currentTarget.readyState)}
          onError={() => {
            setPlaybackSrc(null);
            setError(RECORDING_UNAVAILABLE);
          }}
        />
      )}
      {readyState > 0 && (
        <p className="sr-only" data-testid="voice-recording-ready">
          Recording ready
        </p>
      )}
    </div>
  );
}
