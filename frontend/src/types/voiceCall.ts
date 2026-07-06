export enum VoiceCallStatus {
  PENDING = 'PENDING',
  SCHEDULED = 'SCHEDULED',
  STARTED = 'STARTED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

export enum CallOutcome {
  ANSWERED = 'ANSWERED',
  NO_ANSWER = 'NO_ANSWER',
  BUSY = 'BUSY',
  VOICEMAIL = 'VOICEMAIL',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

export interface VoiceCall {
  id: string;
  leadId: string;
  leadName?: string;
  phoneNumber: string;
  status: VoiceCallStatus;
  provider: string;
  outcome?: CallOutcome;
  failureReason?: string;
  vapiCallId?: string;
  transcript?: string;
  summary?: string;
  recordingUrl?: string;
  durationSeconds?: number;
  startedAt?: string;
  endedAt?: string;
  createdAt: string;
}

export interface VoiceCallStartRequest {
  phoneNumber: string;
  notes?: string;
}
