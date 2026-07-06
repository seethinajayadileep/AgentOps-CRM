# F-008 Database Status Constraint Fix

## Issue Summary
Voice call initiation was failing with a database constraint violation error:
```
status "PENDING" violates check constraint "voice_calls_status_check"
```

## Root Cause
The database check constraint on `voice_calls.status` column did not include the `PENDING` status value from the [`VoiceCallStatus`](backend/src/main/java/com/agentopscrm/entity/enums/VoiceCallStatus.java) enum.

## VoiceCallStatus Enum Values
The Java enum ([`VoiceCallStatus.java`](backend/src/main/java/com/agentopscrm/entity/enums/VoiceCallStatus.java)) defines these statuses:
- `PENDING` - Initial status when call is created
- `SCHEDULED` - Call is scheduled for future
- `STARTED` - Call has been initiated/ringing
- `IN_PROGRESS` - Call is actively in progress
- `COMPLETED` - Call completed successfully
- `FAILED` - Call failed to connect
- `NO_ANSWER` - Call rang but was not answered
- `BUSY` - Line was busy
- `VOICEMAIL` - Reached voicemail
- `CANCELLED` - Call was cancelled

## Fixes Applied

### 1. Database Migration (V6)
Created [`V6__fix_voice_call_status_constraint.sql`](backend/src/main/resources/migration/V6__fix_voice_call_status_constraint.sql):

```sql
-- Drop existing constraint if it exists
ALTER TABLE voice_calls DROP CONSTRAINT IF EXISTS voice_calls_status_check;

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
```

**Key Features:**
- Uses `DROP CONSTRAINT IF EXISTS` for safe execution
- Includes all 7 status values from the enum
- Does not delete existing data
- Adds helpful comment explaining the constraint

### 2. Improved Error Handling
Updated [`VoiceCallService.startCall()`](backend/src/main/java/com/agentopscrm/service/VoiceCallService.java:71):

**Changes:**
- Wrap database operations in try-catch blocks
- Catch database-specific errors separately
- Return user-friendly error messages instead of raw SQL errors
- Proper cleanup if voice call creation fails
- Better logging for debugging

**Example Error Messages:**
| Before | After |
|--------|-------|
| `ERROR: new row for relation "voice_calls" violates check constraint "voice_calls_status_check"` | `Failed to create voice call record. Please contact support if this persists.` |
| `Vapi API client error: 400 Bad Request: {...}` | `Failed to start voice call: Vapi API client error: 400 Bad Request: {...}` |

### 3. Updated VoiceCallStatus Enum
Added new statuses to [`VoiceCallStatus.java`](backend/src/main/java/com/agentopscrm/entity/enums/VoiceCallStatus.java):
- `NO_ANSWER` - Call rang but was not answered
- `BUSY` - Line was busy
- `VOICEMAIL` - Reached voicemail

### 4. Updated Status Mapping
Modified [`VoiceCallService.mapVapiStatus()`](backend/src/main/java/com/agentopscrm/service/VoiceCallService.java:270) to properly map:
- `busy` → `BUSY`
- `no-answer`/`no_answer` → `NO_ANSWER`
- `voicemail` → `VOICEMAIL`

### 5. Verified Enum-Database Alignment
Confirmed that all `VoiceCallStatus` enum values are now in the database constraint:

| Java Enum | Database Constraint | Status |
|-----------|---------------------|--------|
| PENDING | ✅ PENDING | Fixed |
| SCHEDULED | ✅ SCHEDULED | OK |
| STARTED | ✅ STARTED | OK |
| IN_PROGRESS | ✅ IN_PROGRESS | OK |
| COMPLETED | ✅ COMPLETED | OK |
| FAILED | ✅ FAILED | OK |
| NO_ANSWER | ✅ NO_ANSWER | New |
| BUSY | ✅ BUSY | New |
| VOICEMAIL | ✅ VOICEMAIL | New |
| CANCELLED | ✅ CANCELLED | OK |

## Testing

### Before Fix
```bash
POST /api/voice-calls/leads/{leadId}/start
Response: 500 Internal Server Error
{
  "error": "new row for relation \"voice_calls\" violates check constraint \"voice_calls_status_check\""
}
```

### After Fix
```bash
POST /api/voice-calls/leads/{leadId}/start
Response: 200 OK
{
  "id": "uuid",
  "phoneNumber": "+919876543210",
  "status": "STARTED",
  "vapiCallId": "vapi-call-id",
  ...
}
```

## Migration Safety

The migration is safe to run because:
1. Uses `DROP CONSTRAINT IF EXISTS` - won't fail if constraint doesn't exist
2. Does not modify existing data
3. Only adds constraint, doesn't change column type
4. Includes all previously valid values plus new ones

## Files Modified

1. [`backend/src/main/resources/migration/V6__fix_voice_call_status_constraint.sql`](backend/src/main/resources/migration/V6__fix_voice_call_status_constraint.sql) - New migration
2. [`backend/src/main/java/com/agentopscrm/service/VoiceCallService.java`](backend/src/main/java/com/agentopscrm/service/VoiceCallService.java) - Improved error handling

## Deployment Steps

1. **Stop Backend** (if running):
   ```bash
   ./stop.sh
   ```

2. **Start Backend** (migration runs automatically):
   ```bash
   ./run.sh
   ```

3. **Verify Migration**:
   Check backend logs for:
   ```
   Flyway: Migrating schema "public" to version "6 - fix voice call status constraint"
   ```

4. **Test Voice Call**:
   - Navigate to a lead in the CRM
   - Click "Start Voice Call"
   - Verify call initiates successfully

## Related Documentation

- [F008_VAPI_VOICE_STATUS_REPORT.md](F008_VAPI_VOICE_STATUS_REPORT.md) - Original feature
- [VAPI_API_FIX.md](VAPI_API_FIX.md) - API integration fixes
- [`VoiceCallStatus.java`](backend/src/main/java/com/agentopscrm/entity/enums/VoiceCallStatus.java) - Enum definition

## Notes

- The original constraint was likely created automatically by Hibernate/JPA based on an incomplete enum
- This fix ensures manual control over the constraint definition
- Future enum additions will require updating both the enum and this constraint
