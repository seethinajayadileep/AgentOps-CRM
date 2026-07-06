-- Add lead capture fields to conversations table
-- This supports multi-step lead capture flow where we don't create leads immediately

ALTER TABLE conversations 
ADD COLUMN lead_capture_status VARCHAR(50),
ADD COLUMN pending_lead_name VARCHAR(255),
ADD COLUMN pending_lead_email VARCHAR(255),
ADD COLUMN pending_lead_phone VARCHAR(50),
ADD COLUMN pending_lead_requirement TEXT;

-- Add comment explaining the lead_capture_status values
COMMENT ON COLUMN conversations.lead_capture_status IS 'Lead capture flow state: null (normal), AWAITING_DETAILS (asked for details), COLLECTING_DETAILS (gathering info)';
