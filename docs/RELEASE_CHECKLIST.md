# Release checklist

The project already targets Android API 36, disables cleartext traffic and cloud backup, has no sensitive SMS/phone permission, enables R8 for release, and builds an Android App Bundle.

Before a public Google Play launch:

1. Confirm ownership of the application ID `com.medikin.tracker`. Keep it unchanged after the first Play upload because Play application IDs are permanent.
2. Create the Play Console app, opt into Play App Signing, and create a private upload key. Never commit the keystore or passwords.
3. Put these entries in the user-level `~/.gradle/gradle.properties` (not this repository), then rebuild the bundle:

   ```properties
   MEDIKIN_STORE_FILE=/absolute/private/path/medikin-upload.jks
   MEDIKIN_STORE_PASSWORD=your_store_password
   MEDIKIN_KEY_ALIAS=your_upload_alias
   MEDIKIN_KEY_PASSWORD=your_key_password
   ```

   Upload the signed `app/build/outputs/bundle/release/app-release.aab`.
4. Host `PRIVACY_POLICY.md` at a public HTTPS URL and add the distributor's support contact.
5. Complete Data safety as no data collected/shared. Declare on-device health-related app data and notification use accurately.
6. Complete the Health apps declaration and Medical functionality form. Keep the non-medical-device disclaimer in the listing.
7. Upload the artwork from `store-assets`, phone screenshots, and listing copy from `PLAY_STORE_LISTING.md`.
8. Run `./gradlew clean testDebugUnitTest lintDebug bundleRelease` and test reminders on at least one Android 13+ physical device with notification permission allowed, alarm volume audible, and battery saver both on and off.
9. Verify the five-minute follow-up on OEMs with aggressive battery optimization. Android may defer inexact alarms while deeply idle; do not promise emergency-grade timing.
10. Increment `versionCode` for every subsequent upload and sign all releases through the same Play signing identity.

Silent automatic SMS is intentionally not implemented: it would add sensitive permission and Play-policy risk. The notification's Message family action opens a pre-filled draft for user review. Cross-device automatic family alerts should be added later through a consented backend and push-notification flow with account deletion and server-side privacy controls.
