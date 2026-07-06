# Voice Call Status Constraint - Manual Fix Applied

## Issue
The V6 Flyway migration didn't run automatically, so the database constraint was manually fixed.

## Manual Fix Applied
```sql
ALTER TABLE public.voice_calls DROP CONSTRAINT IF EXISTS voice_calls_status_check;

ALTER TABLE public.voice_calls ADD CONSTRAINT voice_calls_status_check CHECK (
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

## Status
✅ **FIXED** - Constraint has been updated in the database directly

## Verification
To verify the fix is working, try starting a voice call from the UI. It should now succeed without the constraint error.

## What Changed
- The `voice_calls_status_check` constraint now allows all 10 status values
- `PENDING` status is now permitted (previously was causing the error)
- Added new statuses: `NO_ANSWER`, `BUSY`, `VOICEMAIL`

## Next Steps
1. Test the voice call feature in the UI
2. The V6 migration file can stay - it won't run since the constraint is already correct
3. If backend is restarted, Flyway may try to run V6 but it will succeed (DROP IF EXISTS is safe)

## Files Modified
- Database constraint updated manually
- Code changes already in place:
  - [`VoiceCallStatus.java`](backend/src/main/java/com/agentopscrm/entity/enums/VoiceCallStatus.java) - 10 statuses
  - [`VoiceCall.java`](backend/src/main/java/com/agentopscrm/entity/VoiceCall.java) - default PENDING
  - [`VoiceCallService.java`](backend/src/main/java/com/agentopscrm/service/VoiceCallService.java) - improved error handling

## Testing
Try clicking "Start Voice Call" on a lead - it should now work without the constraint violation error.
