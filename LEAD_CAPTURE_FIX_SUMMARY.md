# Lead Capture Flow - Multi-Step Enhancement Summary

## Problem Fixed
Leads were being created with `name="Unknown"` immediately when buying intent was detected, resulting in incomplete and unusable lead data in the CRM.

## Solution Implemented
Implemented a **multi-step conversational lead capture flow** that collects complete information before creating leads.

---

## Changes Made

### 1. Database Schema Changes
**File:** [`backend/src/main/resources/migration/V3__add_lead_capture_fields.sql`](backend/src/main/resources/migration/V3__add_lead_capture_fields.sql)

Added new columns to the `conversations` table:
- `lead_capture_status` - Tracks the state of lead capture (null, AWAITING_DETAILS, COLLECTING_DETAILS)
- `pending_lead_name` - Temporarily stores name during collection
- `pending_lead_email` - Temporarily stores email during collection
- `pending_lead_phone` - Temporarily stores phone during collection
- `pending_lead_requirement` - Stores the original requirement message

### 2. Entity Updates
**File:** [`backend/src/main/java/com/agentopscrm/entity/Conversation.java`](backend/src/main/java/com/agentopscrm/entity/Conversation.java:61-74)

Added fields and getters/setters for pending lead data storage per conversation.

### 3. Multi-Step Flow Implementation
**File:** [`backend/src/main/java/com/agentopscrm/service/ChatService.java`](backend/src/main/java/com/agentopscrm/service/ChatService.java)

#### Key Changes:
- **Replaced immediate lead creation** with multi-step flow (lines 154-183)
- **Added `handleLeadCapture()` method** (lines 295-378) that implements:
  - **Step 1:** Detect buying intent → Set status to AWAITING_DETAILS → Ask for details
  - **Step 2:** Extract details from user messages → Store in pending fields
  - **Step 3:** Validate completeness → Create lead only when minimum fields present
  - **Step 4:** Send confirmation message with user's name

#### Helper Methods Added:
- `buildLeadMessage()` - Combines pending data into lead message
- `getLeadConfirmationMessage()` - Generates personalized confirmation
- `clearPendingLeadData()` - Cleans up after lead creation

### 4. Validation Rules
**File:** [`backend/src/main/java/com/agentopscrm/service/LeadQualificationService.java`](backend/src/main/java/com/agentopscrm/service/LeadQualificationService.java:166-217)

#### Strict Validation in `createNewLead()`:
- ❌ **Reject leads with `name="Unknown"` or blank**
- ❌ **Reject leads with no `email` AND no `phone`**
- ✅ **Only create leads with complete minimum required fields**
- Pulls data from `conversation.pendingLead*` fields if available

### 5. AI Prompt Update
**File:** [`backend/src/main/resources/prompts/support-agent.md`](backend/src/main/resources/prompts/support-agent.md:73-99)

Added Lead Capture Flow section documenting:
- The 3-step process for AI to follow
- What to ask at each step
- How to handle missing information
- Confirmation message format

---

## How It Works Now

### Flow Diagram
```
User: "I'm interested in your service"
  ↓
AI detects buying intent (keywords: interested, need it, want, contact me, call me, buy)
  ↓
Set conversation.leadCaptureStatus = "AWAITING_DETAILS"
Store requirement in conversation.pendingLeadRequirement
  ↓
AI: "Great! Please share your name, phone number, and email so our team can contact you."
  ↓
User: "My name is Rahul, phone 9876543210"
  ↓
Extract & store in conversation.pendingLeadName, .pendingLeadPhone
Check if minimum fields present (name + email OR phone)
  ↓
✅ Has name + phone → Create Lead
  ↓
AI: "Thanks Rahul, your details have been saved. Our team will contact you soon."
  ↓
Clear pending data, set leadCaptureStatus = null
```

### Validation Rules
A lead is **ONLY** created when:
1. ✅ `name` is present AND NOT "Unknown" or blank
2. ✅ `email` OR `phone` is present (at least one)
3. ✅ `requirement/message` exists (from original buying intent)

If any validation fails, the system:
- Continues in COLLECTING_DETAILS mode
- Asks for missing fields
- Does NOT create an incomplete lead

---

## Testing Guide

### Test Case 1: User Provides Complete Info Immediately ✅
```
1. User: "I need it"
   Expected: "Great! Please share your name, phone number, and email..."
   
2. User: "My name is Rahul, email rahul@test.com, phone 9876543210"
   Expected: "Thanks Rahul, your details have been saved..."
   Result: Lead created with complete data
```

### Test Case 2: User Provides Info Gradually ✅
```
1. User: "I want to buy"
   Expected: "Great! Please share your name, phone number, and email..."
   
2. User: "My name is Priya"
   Expected: System stores name, still collecting
   
3. User: "My email is priya@test.com"
   Expected: "Thanks Priya, your details have been saved..."
   Result: Lead created with name + email
```

### Test Case 3: No "Unknown" Leads ✅
```
Before: User says "interested" → Lead created immediately with name="Unknown"
After: User says "interested" → AI asks for details → No lead created until complete info provided
```

### Test Case 4: Validation Works ✅
```
If user provides:
- Only name, no contact → Lead NOT created, AI asks for email/phone
- Only email, no name → Lead NOT created, AI asks for name
- Name + email → Lead created ✅
- Name + phone → Lead created ✅
```

---

## Verification Steps

### 1. Check Database Migration Applied
```bash
# After starting the application
psql -h localhost -U postgres -d agentopscrm
\d conversations
# Should show new columns: lead_capture_status, pending_lead_*
```

### 2. Test in Application
```bash
# 1. Set OPENAI_API_KEY
export OPENAI_API_KEY=sk-your-key

# 2. Start application
bash run.sh

# 3. Open frontend: http://localhost:5173

# 4. Test conversation:
- Say "I need it"
- Verify AI asks for details
- Provide name + phone
- Verify confirmation message
- Check Leads page - no "Unknown" leads
```

### 3. Verify Lead Data
```bash
# Check leads table
psql -h localhost -U postgres -d agentopscrm
SELECT name, email, phone FROM leads ORDER BY created_at DESC LIMIT 5;

# Should NOT see any:
- name = 'Unknown'
- name IS NULL
- (email IS NULL AND phone IS NULL)
```

---

## Files Modified

1. ✅ [`backend/src/main/java/com/agentopscrm/entity/Conversation.java`](backend/src/main/java/com/agentopscrm/entity/Conversation.java)
2. ✅ [`backend/src/main/resources/migration/V3__add_lead_capture_fields.sql`](backend/src/main/resources/migration/V3__add_lead_capture_fields.sql) (NEW)
3. ✅ [`backend/src/main/java/com/agentopscrm/service/ChatService.java`](backend/src/main/java/com/agentopscrm/service/ChatService.java)
4. ✅ [`backend/src/main/java/com/agentopscrm/service/LeadQualificationService.java`](backend/src/main/java/com/agentopscrm/service/LeadQualificationService.java)
5. ✅ [`backend/src/main/resources/prompts/support-agent.md`](backend/src/main/resources/prompts/support-agent.md)

## Compilation Status
✅ **BUILD SUCCESS** - No compilation errors

---

## Next Steps

1. **Set OpenAI API Key** (required for testing):
   ```bash
   export OPENAI_API_KEY=sk-your-actual-key
   ```

2. **Start Application**:
   ```bash
   bash run.sh
   ```

3. **Run Test Scenarios** (listed above)

4. **Verify Results**:
   - No "Unknown" leads in database
   - All leads have valid name
   - All leads have email OR phone
   - Multi-step flow works correctly

---

## Benefits

✅ **No more "Unknown" leads** - All leads have real user data  
✅ **Better lead quality** - Minimum required fields enforced  
✅ **Improved user experience** - Conversational, multi-step flow  
✅ **Data integrity** - Validation prevents incomplete leads  
✅ **Trackable state** - Lead capture status per conversation  

---

## Backward Compatibility

- Existing conversations continue to work normally
- New field columns are nullable
- No breaking changes to API
- Flyway migration handles schema updates automatically
