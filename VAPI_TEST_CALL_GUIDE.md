# VAPI Test Call Guide

## How to Make a Test Call from VAPI Dashboard

Follow these steps to trigger a test call and verify the webhook fix:

### Method 1: Using VAPI Dashboard (Recommended for Testing Webhooks)

1. **Go to VAPI Dashboard**
   - Navigate to: https://dashboard.vapi.ai/

2. **Navigate to Assistants**
   - Click on "Assistants" in the left sidebar
   - Find your assistant: **"lead assistant"** (ID: `951d3c5e-97c2-420f-a73c-00d36ba85a4e`)

3. **Test the Assistant**
   - Click on the "lead assistant"
   - Look for the "Test" or "Try it" button (usually at the top right)
   - Click to open the test interface

4. **Make Web Test Call**
   - In the test interface, click "Start Call" or "Test Call"
   - This will initiate a web-based call
   - The call will trigger webhooks to your endpoint: `https://handful-reformer-smooth.ngrok-free.dev/api/webhooks/vapi`

5. **Interact with the Assistant**
   - Speak a few words or type responses
   - The AI will respond
   - Every interaction generates webhook events

6. **End the Call**
   - Click "End Call" or hang up
   - This triggers the "End Of Call Report" webhook

### Method 2: Using Phone Call (Full Integration Test)

1. **Get Your Phone Number from VAPI**
   - Go to "Phone Numbers" section
   - Your number: **+17543336507**

2. **Make a Real Call**
   - Call the number from your phone: +1 754-333-6507
   - Interact with the AI assistant

3. **Monitor the Webhooks**
   - Check VAPI Dashboard → Logs → Webhooks
   - All events should show HTTP 200 (not 500)

### Method 3: Using API Call from Terminal

Alternatively, you can use the voice call endpoint in your CRM system:

```bash
# First, get a lead ID from your database
curl http://localhost:8080/api/leads | jq '.data[0].id'

# Then initiate a call (replace LEAD_ID and PHONE_NUMBER)
curl -X POST http://localhost:8080/api/voice-calls/lead/LEAD_ID/start \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+919876543210"
  }'
```

## What to Monitor

### 1. VAPI Dashboard - Webhooks Tab
Navigate to: `https://dashboard.vapi.ai/` → Your Assistant → Logs → Webhooks

**Before Fix (❌ Broken):**
```
TIME                    TYPE                REQUEST DURATION    HTTP CODE
03 Jul 2026: 4:52 AM   End Of Call Report  2.41               500
03 Jul 2026: 4:52 AM   Status Update       2.5                500
03 Jul 2026: 4:52 AM   Speech Update       4.65               500
```

**After Fix (✅ Working):**
```
TIME                    TYPE                REQUEST DURATION    HTTP CODE
03 Jul 2026: 5:25 AM   End Of Call Report  0.15               200
03 Jul 2026: 5:25 AM   Status Update       0.12               200
03 Jul 2026: 5:25 AM   Speech Update       0.10               200
```

### 2. Backend Logs (Terminal)
The monitoring terminal should show:

**Success Messages:**
```
2026-07-03 10:55:23.456 [http-nio-8080-exec-5] INFO  c.a.c.VapiWebhookController - Received Vapi webhook
2026-07-03 10:55:23.457 [http-nio-8080-exec-5] INFO  c.a.c.VapiWebhookController - Parsed webhook event type: speech-update
2026-07-03 10:55:23.458 [http-nio-8080-exec-5] INFO  c.a.service.VoiceCallService - Processing Vapi webhook event type: speech-update
2026-07-03 10:55:23.459 [http-nio-8080-exec-5] INFO  c.a.service.VoiceCallService - Updated voice call abc123... from webhook
2026-07-03 10:55:23.460 [http-nio-8080-exec-5] INFO  c.a.c.VapiWebhookController - Successfully processed Vapi webhook event: speech-update
```

**What Each Log Means:**
- `Received Vapi webhook` - Controller received the request
- `Parsed webhook event type: X` - JSON parsing successful
- `Processing Vapi webhook event type: X` - Service processing started
- `Updated voice call X from webhook` - Database updated
- `Successfully processed Vapi webhook event: X` - Complete success

### 3. Database - Voice Calls Table
After the call completes, check if the voice call record was updated:

```bash
# Query voice calls in database
curl http://localhost:8080/api/voice-calls?page=0&size=10
```

**Expected fields to be populated:**
- `transcript` - Full conversation transcript
- `status` - COMPLETED, IN_PROGRESS, etc.
- `durationSeconds` - Call duration
- `recordingUrl` - Link to call recording
- `summary` - AI-generated call summary
- `outcome` - ANSWERED, NO_ANSWER, VOICEMAIL, etc.
- `endedAt` - Call end timestamp

## Verification Checklist

- [ ] VAPI Dashboard shows HTTP 200 for all webhook events
- [ ] Backend logs show "Successfully processed Vapi webhook event" messages
- [ ] No error stack traces in backend logs
- [ ] Voice call record in database has transcript populated
- [ ] Voice call record has recording URL
- [ ] Call status is updating correctly (STARTED → IN_PROGRESS → COMPLETED)

## Troubleshooting

### If Still Getting 500 Errors
1. Check if backend restarted successfully: `curl http://localhost:8080/api/health`
2. Verify ngrok is still running: Terminal 1 should show active tunnel
3. Check if webhook URL in VAPI matches: `https://handful-reformer-smooth.ngrok-free.dev/api/webhooks/vapi`

### If Getting 401 Unauthorized
- Webhook signature verification is enabled
- Check if `vapi.webhook-secret` is configured in `.env`
- Either add the secret or disable verification

### If Logs Show Parse Errors
- The webhook payload structure might have changed
- Share the full error stack trace for further investigation

## Current System Status

- **Backend**: Running on http://localhost:8080
- **NgrokTunnel**: https://handful-reformer-smooth.ngrok-free.dev
- **Webhook Endpoint**: https://handful-reformer-smooth.ngrok-free.dev/api/webhooks/vapi
- **Log Monitoring**: Active in Terminal 5

---

**Ready to Test!** 🚀

Make a test call using any of the methods above and watch the logs for confirmation.
