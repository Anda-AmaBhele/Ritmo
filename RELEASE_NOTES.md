# Ritmo – Release Notes

## v1.0.0 – Final PoE (November 2026)

This release lists every update made to the app **since the prototype (Part 2)**.

### Innovative features added in this version

| Feature | What it does |
|---|---|
| **Offline mode with sync** | Posts written without internet are stored in a local SQLite queue. A connectivity listener uploads them automatically when the phone is back online, keeping the original time they were written. A banner shows how many posts are waiting. |
| **Push notifications** | Firebase Cloud Messaging delivers notifications to the phone. The app asks for notification permission, creates a notification channel and saves the device token to the user's profile. |
| **Multi-language support** | The whole app can be switched between **English, isiZulu and isiXhosa** from Settings. The change applies immediately. |

### Other updates

**Settings and profile**
- Language selector now changes every screen, not just the setting.
- Settings screen scrolls so the Save button is always reachable on small screens.
- Language is saved on the device so it works even when offline.

**Quality and reliability**
- Input validation: blank routes, empty posts, empty login fields and failed photo loads show friendly messages instead of crashing.
- Fixed Firestore security rules that had expired and were blocking posting and saving.
- Created the Firestore composite indexes needed for room posts and the messages inbox, removing the "query requires an index" error.
- Signed-in users skip the login screen when the app restarts.

**Development and delivery**
- GitHub Actions workflow (**Android CI**) builds the app, runs unit tests and uploads the APK on every push.
- Removed a duplicate workflow file.
- New app icon and final image assets.
- Signed release APK prepared for the Google Play Store.
- README with design report, screenshots and publishing evidence.

### Carried over from the prototype
- Register and log in with email and password (verification link), Google Sign-In
- Home screen with route, time picker, current-location auto-fill and streak
- Route rooms with a live board (Lift, Accommodation, Job, Event) and photo posts
- Direct messages and notifications screen
- 24-hour post expiry

### Known limitations
- The microphone button in rooms is a UI prototype; live audio is planned.
- Translations should be reviewed by first-language speakers.
- Security rules are permissive for signed-in users and must be tightened before a public launch.

---

## v0.2.0 – Prototype (Part 2)

- Login and registration
- Home screen with route and time selection
- Route room with board and posting
- Direct messaging
- Streak counter
