# Enable app contacts

This stage adds private invitation codes and accepted app-contact connections. **It does not send app notifications yet. SOS still sends SMS only to the saved phone numbers.**

## First: Firebase console

1. Open the same Firebase project already used for Authentication.
2. Open **Firestore Database → Create database**. Choose **Standard edition**, database ID **(default)**, and a region appropriate for your users. Use **Production mode**, not open test-mode rules. If this project's default database already exists, use it instead of creating another one.
3. Open the database's **Rules** tab. For this project's new database, replace the starter rules with the complete contents of `firestore.rules` in the project root, then click **Publish**. If there are existing rules/data for other features, review and merge the rules first rather than overwriting them.
4. Keep Firebase Authentication's Email/Password provider enabled. No Cloud Functions, Cloud Storage, Analytics or messaging configuration is used by this stage.

No users/collections need to be created by hand. When a verified user with a profile name opens App contacts, the app creates or updates their own private profile. Queries use single-field indexes, which Firestore provides by default.

Local rule tests use `demo-safesignal` in an emulator and do not access your Firebase project. Publishing rules is a separate configuration step.

## Then: the installed app

1. Sign in, verify the email, and save a profile name under **Settings → Manage account**.
2. Open **Edit contacts → Manage app contacts**. Tap **Refresh contacts** if you just published the rules. Wait for “App contacts are up to date.”
3. Tap **Create invitation**, then **Copy private code**. Share that code directly with the person you intend to invite, through your normal private communication channel. The app does not automatically send it anywhere.
4. The other person signs into their own verified account in the same app, saves a name, opens App contacts, pastes the code and taps **Preview invitation**.
5. After checking the name and who supplied the code, they tap **Accept** or **Decline**.
6. An accepted invitation appears under **People you’ve chosen** for the sender and **People you support** for the recipient. Both screens update while open.
7. Either person can select **Remove** and confirm. The connection disappears from active lists on both accounts. A sender can also remove a pending invitation to invalidate its code.

The connection is directional: if Alice invites Bob and Bob accepts, Bob agrees to be a contact for Alice. Alice is not automatically made a contact for Bob. Bob can send a separate invitation for the reverse direction.

For testing on one phone, copy the code somewhere private before signing out, then sign in with the second account. Phone-local SMS contacts and PIN settings still belong to the installation, not the signed-in account. Do not press SOS during an invitation-only test.

## Privacy and data model

- `users/{uid}` contains only `displayName` and a server `updatedAt`. Only that verified user can read or write it; profiles cannot be listed or searched.
- `contactInvitations/{randomCode}` contains participant IDs, display-name snapshots, status and timestamps. There are no email addresses, phone numbers, locations, or PINs in these records.
- Codes are UUIDv4 values rendered as 32 hexadecimal characters (122 random bits). A code is a **bearer invitation**: any verified account that knows a still-pending code can preview and claim it. Share it only with the intended person. A name is self-chosen, not proof of identity.
- Pending codes last about six days. The server rules reject expiry beyond seven days and reject expired acceptance. Used or revoked codes cannot be claimed again.
- Sender-only and recipient-only queries can list records; a public invitation directory is denied. A stranger cannot read an accepted, declined or revoked invitation, even with its old code.
- All mutations use server transactions. Two recipients racing for the same code cannot both accept. Offline actions cannot silently queue an acceptance and report it as confirmed.
- Lists are shown as confirmed only after server snapshots arrive. Losing listener access clears the list; leaving the page or changing accounts cancels listeners and invalidates late callbacks.
- Firestore uses an in-memory cache, not a persistent contact database on disk. Only the account-specific queries are displayed. Firebase Authentication manages its own sign-in session.
- Name snapshots remain as they were at invitation creation/acceptance. Updating an Auth profile name does not rewrite old invitations.
- Removal marks a record `revoked`, retaining it for the two participants. Retention cleanup and account deletion remain future work.

Duplicate accepted invitations from the same sender are prevented in the normal app preview flow, not globally across simultaneous/malicious clients. Before notification delivery is added, enforce a canonical unique relationship per sender/recipient and test revocation against it. Abuse/rate limits and App Check also remain release work.

## Local security tests

Install Node.js and a JDK compatible with the Firebase emulator, then from `firebase-tests`:

```text
pnpm install --frozen-lockfile
pnpm test
```

The test command starts only the local Firestore emulator with a demo project, loads the checked-in rules and runs adversarial tests. It never publishes the rules. Keep the generated `pnpm-lock.yaml` for repeatable dependencies.

References: [Firestore security conditions](https://firebase.google.com/docs/firestore/security/rules-conditions), [rules emulator testing](https://firebase.google.com/docs/firestore/security/test-rules-emulator), [transactions](https://firebase.google.com/docs/firestore/manage-data/transactions).
