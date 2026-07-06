-- Fix voice call status constraint to include all valid status values
-- This fixes the issue where PENDING, NO_ANSWER, BUSY, VOICEMAIL statuses were not allowed

-- Drop existing constraint if it exists
ALTER TABLE public.voice_calls DROP CONSTRAINT IF EXISTS voice_calls_status_check;

-- Add the correct constraint with all valid VoiceCallStatus enum values
ALTER TABLE public.voice_calls ADD CONSTRAINT voice_calls_status_check
    CHECK (
        status IN (
            'PENDING',
            'SCHEDULED',
            'STARTED',
            'IN_PROGRESS',
            'COMPLETED',
            'FAILED',
            'NO_ANSWER',
            'BUSY',
            'VOICEMAIL',
            'CANCELLED'
        )
    );

-- Add comment explaining the constraint
COMMENT ON CONSTRAINT voice_calls_status_check ON public.voice_calls IS
    'Ensures status column matches VoiceCallStatus enum values: PENDING, SCHEDULED, STARTED, IN_PROGRESS, COMPLETED, FAILED, NO_ANSWER, BUSY, VOICEMAIL, CANCELLED';
