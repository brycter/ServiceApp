# Service Ops Mobile

An offline-first Android operations app for Service Ops LLC.

## Included in version 0.2

- Dashboard with open-task, unpaid-invoice, location, and service-log totals
- Customer/location records
- Dated service logs tied to a location
- Task planning with priorities and completion tracking
- Invoice records with paid/unpaid tracking
- Searchable reference notes for parts, pricing, procedures, and contracts
- Location-aware chat with an OpenAI-compatible local LLM
- Local photo attachments and review-before-save service-history drafts
- Local on-device persistence through Android SharedPreferences

## Open and run

1. Install a current Android Studio release with Android SDK 37 and JDK 17 or newer.
2. Open the repository root in Android Studio.
3. Let Android Studio sync the included Gradle 9.4.1 wrapper.
4. Run on an Android 7.0 (API 24) or newer emulator/device.

To build from a terminal on Windows:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Current data behavior

All records, chat messages, and imported service photos stay in the app's private local storage. Uninstalling the app clears them. The chat sends the selected location context, recent history, current message, and current photo attachments only to the configured local LLM endpoint. Nothing is written to location history until **Save to location history** is tapped.

## Local model chat

The default endpoint is `http://10.0.2.2:8080/v1`, which lets the Android emulator reach an OpenAI-compatible server running on the same computer. For a physical phone, open Chat settings and replace `10.0.2.2` with the computer's LAN address; the phone and computer must be on the same trusted network and the server must listen on that interface.

A text-only model can organize typed field notes while keeping attached photos with the saved history entry. To interpret the actual image content, configure a vision-capable model and server that accept OpenAI-style `image_url` message content.

## Suggested next phase

1. Replace SharedPreferences with Room and add schema migrations.
2. Add equipment records under each location (RTUs, hood fans, belts, filters, serial numbers).
3. Generate and share branded PDF invoices.
4. Add calendar reminders and recurring PM schedules.
5. Add encrypted backup/export and biometric locking.
