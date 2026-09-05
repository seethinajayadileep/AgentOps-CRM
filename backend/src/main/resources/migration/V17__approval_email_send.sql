-- Track Resend delivery on follow-up approvals.
ALTER TABLE approvals ADD COLUMN IF NOT EXISTS sent_to VARCHAR(255);
ALTER TABLE approvals ADD COLUMN IF NOT EXISTS sent_subject VARCHAR(500);
ALTER TABLE approvals ADD COLUMN IF NOT EXISTS resend_message_id VARCHAR(100);
ALTER TABLE approvals ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP;
ALTER TABLE approvals ADD COLUMN IF NOT EXISTS send_error TEXT;
