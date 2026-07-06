# Vapi API Integration Fix - F-008

## Issue Summary
Fixed the 400 Bad Request error when starting voice calls through Vapi API.

## Error Details
```
Failed to start voice call: Vapi API client error: 400 Bad Request: 
{
  "message": [
    "phoneNumberId must be a UUID",
    "phoneNumber must be an object",
    "nested property phoneNumber must be either object or array"
  ],
  "error": "Bad Request",
  "statusCode": 400
}
```

## Root Causes

### 1. Incorrect Request Structure
The original [`VapiClient.java`](backend/src/main/java/com/agentopscrm/client/VapiClient.java) was sending:
```json
{
  "phoneNumber": "+19802657069",
  "assistantId": "uuid",
  "phoneNumberId": "uuid"
}
```

But Vapi API v2 expects:
```json
{
  "customer": {
    "number": "+19802657069"
  },
  "assistantId": "uuid",
  "phoneNumberId": "uuid"
}
```

### 2. Invalid Phone Number ID Configuration
The `.env` files had `VAPI_PHONE_NUMBER_ID=+19802657069` (an actual phone number) instead of a UUID identifier.

## Changes Made

### 1. Updated VapiClient.java
- Modified [`VapiCallRequest`](backend/src/main/java/com/agentopscrm/client/VapiClient.java:153) to use nested `customer` object
- Added new [`CustomerInfo`](backend/src/main/java/com/agentopscrm/client/VapiClient.java:173) class to properly structure customer data
- Updated [`VapiCallResponse`](backend/src/main/java/com/agentopscrm/client/VapiClient.java:183) to match API response structure
- Added helper method `getPhoneNumber()` for backward compatibility

**Before:**
```java
public static class VapiCallRequest {
    @JsonProperty("phoneNumber")
    public String phoneNumber;
    
    @JsonProperty("assistantId")
    public String assistantId;
    
    @JsonProperty("phoneNumberId")
    public String phoneNumberId;
}
```

**After:**
```java
public static class VapiCallRequest {
    @JsonProperty("customer")
    public CustomerInfo customer;
    
    @JsonProperty("assistantId")
    public String assistantId;
    
    @JsonProperty("phoneNumberId")
    public String phoneNumberId;
    
    public VapiCallRequest(String phoneNumber, String assistantId, String phoneNumberId) {
        this.customer = new CustomerInfo(phoneNumber);
        this.assistantId = assistantId;
        this.phoneNumberId = phoneNumberId;
    }
}

public static class CustomerInfo {
    @JsonProperty("number")
    public String number;
    
    public CustomerInfo(String number) {
        this.number = number;
    }
}
```

### 2. Updated Environment Configuration
Updated all `.env` files to clarify that `VAPI_PHONE_NUMBER_ID` must be a UUID:

- [`.env`](.env#L10-L15)
- [`backend/.env`](backend/.env#L26-L31)
- [`backend/.env.example`](backend/.env.example#L27-L32)

Added clear comments explaining:
- This should be the UUID of your Vapi phone number resource
- Find it in Vapi dashboard: Phone Numbers → Click on your number → Copy the ID
- This is NOT the actual phone number but the UUID identifier

## How to Fix Your Configuration

### Step 1: Get Your Vapi Phone Number UUID
1. Log in to your Vapi dashboard at https://dashboard.vapi.ai
2. Navigate to **Phone Numbers** section
3. Click on your phone number (e.g., +19802657069)
4. Copy the **Phone Number ID** (it will be a UUID format like `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)

### Step 2: Update Your .env Files
Replace the placeholder value in both `.env` and `backend/.env`:

```bash
# Before (INCORRECT)
VAPI_PHONE_NUMBER_ID=+19802657069

# After (CORRECT)
VAPI_PHONE_NUMBER_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

### Step 3: Restart the Backend
```bash
# Stop the backend if running
./stop.sh

# Restart the backend
./run.sh
```

## Phone Number Formatting

The system now automatically formats phone numbers with the +91 (India) country code:

### Formatting Rules
- If number already has `+` prefix: Used as-is
- If number starts with `91`: Adds `+` prefix → `+91xxxxxxxxxx`
- If number starts with `0`: Removes `0` and adds `+91` → `+91xxxxxxxxxx`
- Otherwise: Adds `+91` prefix → `+91xxxxxxxxxx`

### Examples
| Input | Output |
|-------|--------|
| `9876543210` | `+919876543210` |
| `09876543210` | `+919876543210` |
| `919876543210` | `+919876543210` |
| `+919876543210` | `+919876543210` |
| `+14155551234` | `+14155551234` |

This formatting is applied automatically in [`VoiceCallService.formatPhoneNumber()`](backend/src/main/java/com/agentopscrm/service/VoiceCallService.java:310) when initiating calls.

## API Request Example

### Correct Request Format
```bash
curl -X POST https://api.vapi.ai/call/phone \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumberId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "customer": {
      "number": "+19802657069"
    },
    "assistantId": "951d3c5e-97c2-420f-a73c-00d36ba85a4e"
  }'
```

## Testing

After making these changes, test the voice call feature:

1. Navigate to a lead in the CRM
2. Click "Start Voice Call"
3. The call should now initiate successfully
4. Check the backend logs for confirmation

## Files Modified

1. [`backend/src/main/java/com/agentopscrm/client/VapiClient.java`](backend/src/main/java/com/agentopscrm/client/VapiClient.java) - Updated request/response structure
2. [`.env`](.env) - Added configuration guidance
3. [`backend/.env`](backend/.env) - Added configuration guidance
4. [`backend/.env.example`](backend/.env.example) - Added configuration guidance

## Related Documentation

- [Vapi API Documentation](https://docs.vapi.ai/)
- [F008_VAPI_VOICE_STATUS_REPORT.md](F008_VAPI_VOICE_STATUS_REPORT.md) - Original feature documentation

## Notes

- The `VoiceCallService` doesn't need changes as it already uses the correct constructor
- The change is backward compatible for existing code that uses `VapiCallRequest`
- The response objects now match the actual Vapi API response structure
