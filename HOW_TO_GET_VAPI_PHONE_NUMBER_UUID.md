# How to Get Your Vapi Phone Number UUID

## Current Error
```
Failed to start voice call: Vapi API client error: 400 Bad Request: 
{"message":["phoneNumberId must be a UUID"],"error":"Bad Request","statusCode":400}
```

This error occurs because `VAPI_PHONE_NUMBER_ID` in your [`.env`](.env#L18) file is set to `REPLACE_WITH_YOUR_VAPI_PHONE_NUMBER_UUID` instead of an actual UUID.

## Steps to Fix

### Option 1: Using Vapi Dashboard (Recommended)

1. **Log in to Vapi Dashboard**
   - Go to: https://dashboard.vapi.ai
   - Sign in with your credentials

2. **Navigate to Phone Numbers**
   - Click on "Phone Numbers" in the left sidebar
   - You should see your purchased phone number (e.g., +19802657069)

3. **Get the Phone Number ID**
   - Click on your phone number
   - Look for the "Phone Number ID" field
   - Copy the UUID (format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)

4. **Update Your .env Files**
   
   Edit [`.env`](.env):
   ```bash
   VAPI_PHONE_NUMBER_ID=paste-your-uuid-here
   ```
   
   Edit [`backend/.env`](backend/.env):
   ```bash
   VAPI_PHONE_NUMBER_ID=paste-your-uuid-here
   ```

5. **Restart the Backend**
   ```bash
   ./stop.sh
   ./run.sh
   ```

### Option 2: Using Vapi API

If you can't access the dashboard, use the Vapi API to list your phone numbers:

```bash
curl -X GET https://api.vapi.ai/phone-number \
  -H "Authorization: Bearer b09e1e0a-bf6e-42e3-a786-96c140684555"
```

This will return a list of your phone numbers with their IDs:
```json
[
  {
    "id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "number": "+19802657069",
    "provider": "twilio",
    ...
  }
]
```

Copy the `"id"` value and update your `.env` files.

### Option 3: Make Phone Number ID Optional (Temporary Workaround)

If you're still having trouble, I can modify the code to make the phone number ID optional and use your assistant's default configuration. However, this is not recommended as it may limit functionality.

## Important Notes

- **VAPI_PHONE_NUMBER_ID** is the UUID that identifies your phone number resource in Vapi
- It is **NOT** the actual phone number (like +19802657069)
- This UUID is used by Vapi to know which phone number to use as the caller ID
- You've already purchased this number from Vapi, you just need to find its UUID

## Verification

After updating, your `.env` should look like this:

```bash
VAPI_ENABLED=true
VAPI_API_KEY=b09e1e0a-bf6e-42e3-a786-96c140684555
VAPI_ASSISTANT_ID=951d3c5e-97c2-420f-a73c-00d36ba85a4e
VAPI_PHONE_NUMBER_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx  # ← Real UUID here
VAPI_WEBHOOK_SECRET=vapi_webhook_token_9f0c3e2e7f8f4f7bb0e5d52b8c8a1d7a6a0b9c1d2e3f4a5b6c7d8e9f0a1b2c3d
```

The UUID should be 36 characters long (32 hex digits + 4 hyphens) in the format `8-4-4-4-12`.

## Need Help?

If you're still unable to find the UUID, let me know and I can:
1. Help you make the API call to retrieve it
2. Modify the code to work without it (with limited functionality)
3. Guide you through the Vapi dashboard step-by-step
