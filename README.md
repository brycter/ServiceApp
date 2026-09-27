# Service Ops Mobile

An offline-first Android operations app for Service Ops LLC.

## Included in version 0.1

- Dashboard with open-task, unpaid-invoice, location, and service-log totals
- Customer/location records
- Dated service logs tied to a location
- Task planning with priorities and completion tracking
- Invoice records with paid/unpaid tracking
- Searchable reference notes for parts, pricing, procedures, and contracts
- Local on-device persistence through Android SharedPreferences

## Open and run

1. Install a current Android Studio release with Android SDK 37 and JDK 17.
2. Open the `ServiceOpsMobile` folder in Android Studio.
3. Let Android Studio sync the Gradle project.
4. If Android Studio requests a Gradle wrapper, select Gradle 9.4.1 or newer compatible with Android Gradle Plugin 9.2.1.
5. Run on an Android 7.0 (API 24) or newer emulator/device.

## Current data behavior

All data stays in the app's private local storage. Uninstalling the app clears it. Cloud sync, file attachments, PDF invoice output, automated backups, and biometric locking are intentionally reserved for the next phase.

## Suggested next phase

1. Replace SharedPreferences with Room and add schema migrations.
2. Add equipment records under each location (RTUs, hood fans, belts, filters, serial numbers).
3. Attach photos to service logs.
4. Generate and share branded PDF invoices.
5. Add calendar reminders and recurring PM schedules.
6. Add encrypted backup/export and biometric locking.
