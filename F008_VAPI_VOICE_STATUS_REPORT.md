# F-008: Vapi Voice Call System - Status Report

## Date: 2026-07-02

## Current Status: READY TO IMPLEMENT

---

## Existing Infrastructure

### ✅ COMPLETE - Database Entity Layer
- **VoiceCall.java**: EXISTS
  - Has: id, business, lead, conversation, vapiCallId, phoneNumber, status, transcript, summary, recordingUrl, durationSeconds, startedAt, endedAt, createdAt, updatedAt
  - **Missing**: provider, outcome, failureReason
  
- **VoiceCallStatus.java**: EXISTS (needs updates)
  - Current values: SCHEDULED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
  - **Missing**: PENDING, STARTED (requirements specify these)
  
- **VoiceCallRepository.java**: EXISTS ✅
  - Has all necessary query methods
  - Ready to use

### ⏸️ PARTIALLY COMPLETE - Frontend
- **VoiceCalls.tsx**: EXISTS (stub only)
  - Currently just a placeholder page
  - Needs full implementation

### ❌ MISSING - Backend Components
- **VapiClient**: MISSING - needs creation
- **VoiceCallService**: MISSING - needs creation
- **VoiceCallController**: MISSING - needs creation  
- **VapiWebhookController**: MISSING - needs creation
- **Voice Call DTOs**: MISSING - needs creation
- **CallOutcome enum**: MISSING - needs creation

### ❌ MISSING - Configuration
- **Vapi config in application.yml**: MISSING
  - Need to add: VAPI_API_KEY, VAPI_ASSISTANT_ID, VAP I_PHONE_NUMBER_ID, VAPI_WEBHOOK_SECRET, VAPI_ENABLED

### ❌ MISSING - Frontend Components
- **Voice Calls API client**: MISSING
- **Voice Calls page implementation**: MISSING
- **Lead Detail voice call section**: MISSING
- **Voice Call types**: MISSING

### ❌ MISSING - Database
- **Migration for new fields**: MISSING
  - Need migration for: provider, outcome, failureReason columns

---

## Files That Will Be Created

### Backend (13 files):
1. `backend/src/main/java/com/agentopscrm/entity/enums/CallOutcome.java` - NEW
2. `backend/src/main/java/com/agentopscrm/client/VapiClient.java` - NEW
3. `backend/src/main/java/com/agentopscrm/service/VoiceCallService.java` - NEW
4. `backend/src/main/java/com/agentopscrm/controller/VoiceCallController.java` - NEW
5. `backend/src/main/java/com/agentopscrm/controller/VapiWebhookController.java` - NEW
6. `backend/src/main/java/com/agentopscrm/dto/VoiceCallStartRequest.java` - NEW
7. `backend/src/main/java/com/agentopscrm/dto/VoiceCallResponse.java` - NEW
8. `backend/src/main/java/com/agentopscrm/dto/VapiWebhookEvent.java` - NEW
9. `backend/src/main/resources/migration/V5__add_voice_call_fields.sql` - NEW
10. `backend/src/main/resources/prompts/voice-call-agent.md` - NEW (optional)

### Backend (4 files to modify):
1. `backend/src/main/java/com/agentopscrm/entity/VoiceCall.java` - ADD provider, outcome, failureReason
2. `backend/src/main/java/com/agentopscrm/entity/enums/VoiceCallStatus.java` - ADD PENDING, STARTED
3. `backend/src/main/resources/application.yml` - ADD Vapi config
4. `backend/src/main/resources/application-dev.yml` - ADD Vapi config

### Frontend (4 files):
1. `frontend/src/api/voiceCallsApi.ts` - NEW
2. `frontend/src/types/voiceCall.ts` - NEW
3. `frontend/src/pages/VoiceCalls.tsx` - MODIFY (Replace stub)
4. `frontend/src/pages/LeadDetailPage.tsx` - MODIFY (Add voice call section)

### Documentation (6 files):
1. `docs/FEATURE_CHECKLIST.md` - UPDATE
2. `docs/API_CONTRACT.md` - UPDATE
3. `docs/FILE_MAP.md` - UPDATE
4. `docs/CHANGELOG.md` - UPDATE
5. `docs/ENVIRONMENT.md` - UPDATE
6. `docs/TEST_PLAN.md` - UPDATE

---

## Implementation Plan

### Phase 1: Backend Foundation (Steps 2-6)
1. Add missing enum: CallOutcome
2. Update VoiceCallStatus enum
3. Add missing fields to VoiceCall entity
4. Create database migration V5
5. Add Vapi configuration to application.yml

### Phase 2: Backend Services (Steps 7-9)
1. Create VapiClient with safe error handling
2. Create Voice Call DTOs
3. Create VoiceCallService

### Phase 3: Backend APIs (Steps 10-11)
1. Create VoiceCallController
2. Create VapiWebhookController

### Phase 4: Frontend (Steps 12-14)
1. Create voice call types and API client
2. Update Lead Detail page with voice call section
3. Update Voice Calls page with full implementation

### Phase 5: Documentation & Testing (Steps 15-17)
1. Update all documentation
2. Test backend compilation
3. Test frontend compilation

---

## Expected API Endpoints

1. **POST /api/leads/{leadId}/voice-calls/start** - Start voice call
2. **GET /api/leads/{leadId}/voice-calls** - Get lead's voice calls
3. **GET /api/voice-calls** - List all voice calls
4. **GET /api/voice-calls/{id}** - Get voice call details
5. **POST /api/webhooks/vapi** - Vapi webhook receiver

---

## Safety Features

1. ✅ Vapi config can be disabled (VAPI_ENABLED=false)
2. ✅ No hardcoded API keys
3. ✅ Clean error messages when Vapi not configured
4. ✅ Manual call initiation only (no auto-dialing)
5. ✅ Phone number validation
6. ✅ Lead/business validation before call
7. ✅ AgentLog for all actions
8. ✅ Webhook signature verification

---

## Ready to Proceed

All prerequisites checked. No existing voice call service/controller to conflict with. Can safely proceed with implementation.
