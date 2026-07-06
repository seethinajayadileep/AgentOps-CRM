-- Add new fields to voice_calls table for Vapi integration

-- Add provider column (default to 'vapi')
ALTER TABLE voice_calls ADD COLUMN provider VARCHAR(50) DEFAULT 'vapi';

-- Add outcome column (CallOutcome enum)
ALTER TABLE voice_calls ADD COLUMN outcome VARCHAR(50);

-- Add failure_reason column
ALTER TABLE voice_calls ADD COLUMN failure_reason VARCHAR(1000);

-- Add index for outcome filtering
CREATE INDEX idx_voice_calls_outcome ON voice_calls(outcome);

-- Add index for provider filtering  
CREATE INDEX idx_voice_calls_provider ON voice_calls(provider);
