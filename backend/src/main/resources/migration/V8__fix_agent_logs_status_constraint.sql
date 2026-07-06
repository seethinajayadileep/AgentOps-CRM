-- F-008 (F-026) Evaluation Agent
-- Fix agent_logs status constraint to include all valid AgentActionStatus enum values.
--
-- Background: hibernate.ddl-auto=update generates a CHECK constraint for the
-- STRING-mapped `status` enum when the table is first created, but it does NOT
-- alter that constraint when new enum values are added later. The Evaluation
-- Agent writes status=FALLBACK_USED (rule-based fallback / customer fallback
-- used), which the older generated constraint rejected. This mirrors the
-- earlier V6 fix for voice_calls_status_check.

-- Drop existing constraint if it exists
ALTER TABLE public.agent_logs DROP CONSTRAINT IF EXISTS agent_logs_status_check;

-- Add the correct constraint with all valid AgentActionStatus enum values
ALTER TABLE public.agent_logs ADD CONSTRAINT agent_logs_status_check
    CHECK (
        status IN (
            'SUCCESS',
            'PARTIAL',
            'ERROR',
            'FAILED',
            'FALLBACK_USED'
        )
    );

-- Add comment explaining the constraint
COMMENT ON CONSTRAINT agent_logs_status_check ON public.agent_logs IS
    'Ensures status column matches AgentActionStatus enum values: SUCCESS, PARTIAL, ERROR, FAILED, FALLBACK_USED';
