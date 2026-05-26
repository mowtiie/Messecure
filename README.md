# Messecure

A native Android messaging application built for STI College Balayan students. Messages are end-to-end encrypted, self-destruct after being read, and leave no permanent record on the server.

---

## Tech Stack

- **Android** — Java, Material Design 3, BiometricPrompt API
- **Database** — Firebase Cloud Firestore
- **Authentication** — Firebase Auth (email/password)
- **Backend** — Firebase Cloud Functions (Node.js)
- **Security** — Android Keystore System (AES-256), FLAG_SECURE

---

## Features

- Biometric lock on every app open and resume
- Screenshot prevention via FLAG_SECURE
- AES-256 message encryption using Android Keystore
- Self-destructing messages with configurable timers
- Verified-only access restricted to @sti.edu.ph accounts
- Remote wipe via authenticated Cloud Function
- Stealth push notifications that never expose message content

---

## Project Structure

```
app/
  src/main/java/com/sti/messecure/
    activities/       # LoginActivity, BiometricActivity, ChatActivity
    fragments/        # ChatsFragment, ContactsFragment, ProfileFragment, SettingsFragment
    adapters/         # ConversationAdapter, MessageAdapter
    models/           # Conversation, Message, User
    utils/            # KeystoreHelper, NetworkUtils
  res/
    layout/           # XML layouts
    menu/             # main_menu.xml
functions/
  index.js            # Cloud Functions: self-destruct, FCM, remote wipe
```

---

## Setup

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- A Firebase project on the Blaze plan
- Node.js 18+ (for Cloud Functions)

### Steps

1. Clone the repository
2. Create a Firebase project and enable Authentication (Email/Password) and Firestore
3. Download `google-services.json` from the Firebase Console and place it in `/app`
4. Open the project in Android Studio and sync Gradle
5. To deploy Cloud Functions:

```bash
cd functions
npm install
firebase deploy --only functions
```

6. Build and run on a physical device (biometric features do not work on emulators)

---

## Security Rules

Firestore is locked so users can only read and write their own data. The rules are stored in `firestore.rules` at the project root.

---

## KPIs

| Metric | Target |
|---|---|
| Message deletion after timer | 100% |
| Unencrypted messages stored on server | 0 |
| Average message latency | under 500ms |

---

## Author

Vrixzandro Cunamay Eliponga — STI College Balayan

---

## License

For academic use only.
