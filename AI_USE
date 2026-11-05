# Use of AI Tools – Ritmo (OPSC6312 Part 3)

I used **Claude** (Anthropic, https://claude.ai) as an assistant while completing Part 3. I decided what the app should do, designed the screens and tested everything myself. The AI helped me write and fix code faster and explained steps I was unsure about. I reviewed, ran and adjusted everything it produced.

## Where I used it

**1. Code snippets**
- Offline mode: the SQLite helper (`OfflineDb`) and the connectivity listener (`SyncManager`).
- Push notifications: `RitmoMessagingService` and the permission request in `MainActivity`.
- Multi-language support: `LocaleHelper` and converting hard-coded text into string resources.
- Small UI changes such as a scrollable settings screen and blank-route validation.

**2. Debugging errors**
- Posting and saving failed with `PERMISSION_DENIED`. Claude helped me see that my Firestore test-mode rules had expired, and I updated them in the Firebase console.
- A "query requires an index" error on the board. I created the composite indexes in Firestore.
- A build error (`Unresolved reference 'categoryLabel'`) caused by a file that had not been replaced.
- Connecting BlueStacks to Android Studio through ADB, and troubleshooting why push notifications did not arrive on the emulator.

**3. Translations**
- First drafts of the isiZulu and isiXhosa text. I understand that AI translation can be wrong, so these need review by first-language speakers.

**4. Guidance**
- Step-by-step help with GitHub Actions, signing the APK, and preparing the README and release notes.

## How I checked the AI's work
- I ran every change on a device and fixed anything that did not work.
- I read the code so I could explain it in my demo video.
- I added comments and references in the code where I used documentation.
- Automated unit tests run in GitHub Actions on every push.

## What I did without AI
- The app idea, the research and design from Part 1, and the Canva mockups.
- Setting up Firebase, GitHub, and the testing on my physical phone.
- Recording and narrating the demo video.

## Citation

Anthropic (2026) *Claude* [Large language model]. Available at: https://claude.ai (Accessed 5 November 2026).
