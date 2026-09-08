# Connect SafeSignal accounts to Firebase

Accounts are optional. Follow these steps to connect your own Firebase project; the SMS/BLE prototype works without it.

## One-time setup

1. Open the [Firebase console](https://console.firebase.google.com/) using your Google account and create a project. Choose a name such as SafeSignal. Google Analytics is optional and is not used by this app.
2. Add an **Android app**. Enter the package name exactly: `com.example.emergencybutton`. Keep the package name unchanged so installing an update preserves the existing Android app data.
3. Download **google-services.json**. Place it at `app/google-services.json` inside this project, alongside `app/build.gradle.kts`. Do not rename it or put it in `app/src/main`.
4. Open **Authentication**, get started, and enable the **Email/Password** sign-in provider. The email-link/passwordless option is not used.
5. In Authentication settings, set the password policy to require at least **8 characters**, matching the registration form. Firebase still enforces any additional requirements on the server. Enable email enumeration protection if it is not already enabled.
6. Review the verification and password-reset email templates in the Firebase console. Keep the default hosted links for this prototype. This step is console setup only; verification in the app comes after installation.
7. Rebuild and install the app as an update. Settings → Manage account should now enable sign-in and registration.

There is no need to enable Firestore, Cloud Storage, Analytics or phone-number authentication for this stage. Do not download a service-account private key: an Android client uses `google-services.json`, never an Admin SDK credential.

The Google Services plugin is already declared. It is applied when `app/google-services.json` exists. The file contains Firebase client configuration, not an administrator credential, and is ignored by this project's `.gitignore` to keep individual development projects separate.

## Build and install

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --gradle-user-home .gradle-user :app:assembleDebug :app:testDebugUnitTest
```

Install `app/build/outputs/apk/debug/app-debug.apk` as an update, or use Android Studio Run. Do not uninstall the existing app first.

## Test against your project

1. Use an email address you control. Create an account and verify that it appears in Firebase Authentication's Users list.
2. Tap **Send verification email**. Open the link, then tap **Check verification** in the app. The status should change to Email verified.
3. Save a profile name. Close/reopen the app and confirm the signed-in account and name remain.
4. Sign out, then sign in with the correct password. Try an incorrect password and check that an understandable error appears.
5. Sign out, open Forgot password, request a reset link, and complete the reset. Check that the new password works. The UI deliberately does not reveal whether an email is registered.
6. Disconnect internet and attempt an account request. It should show an error and allow retry. Navigating back to the SMS dashboard must remain possible while a request is running.
7. Confirm contacts and the cancellation PIN remain unchanged after sign-in and sign-out. They are phone settings at this stage, not account-specific cloud data.

Automated tests use fakes and do not prove Firebase project configuration, real email delivery, or live session persistence. Those require this checklist after setup.

## Scope of this stage

Firebase Auth provides a stable user ID, email, display name and verification status. Optional contact invitations are implemented separately in Firestore; see [app-contact setup](APP_CONTACTS_SETUP.md). App notifications and shared audio/location are postponed. Local SMS data is not uploaded. Account deletion and shared-phone account isolation remain planned.

## Official references

- [Add Firebase to Android](https://firebase.google.com/docs/android/setup)
- [Email/password authentication and password policies](https://firebase.google.com/docs/auth/android/password-auth)
- [Profile updates, verification and password-reset emails](https://firebase.google.com/docs/auth/android/manage-users)
