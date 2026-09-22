# Ritmo

## Purpose
Ritmo is an Android social app that lets users connect through themed "Rooms," 
post updates, and chat in real time. It was built as part of the OPSC6312 
Portfolio of Evidence, focusing on native Android development with 
[Jetpack Compose](https://developer.android.com/jetpack/compose) and 
[Firebase](https://firebase.google.com/) as the backend.

## Design Considerations
- **UI/UX:** Built entirely in Jetpack Compose for a modern, declarative UI. 
  A custom colour palette (RitmoCream, RitmoSage, RitmoDarkSage, RitmoBlack, 
  RitmoWhite, RitmoGray) was chosen for a warm, calm aesthetic.
- **Navigation:** A manual sealed-class `Screen` router directs users between 
  Login, Home, Room, Post, Messages, Chat, Notifications and Profile screens.
- **State handling:** The last visited Room is remembered so returning from 
  Messages/Chat lands users back where they left off.
- **Backend:** Firebase Authentication handles user login/signup, and 
  Firestore stores app data (rooms, posts, messages) in real time.

## Tech Stack
- [Kotlin](https://kotlinlang.org/) — primary programming language
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — UI toolkit
- [Android Studio](https://developer.android.com/studio) — IDE used for development
- [Firebase Authentication](https://firebase.google.com/products/auth) & 
  [Firestore](https://firebase.google.com/products/firestore) — backend
- Tested via [BlueStacks](https://www.bluestacks.com/) (Android emulator on Windows)

## GitHub & GitHub Actions
The project is version-controlled on GitHub with commits tracking incremental 
progress across each screen. A GitHub Actions workflow (`android-build.yml`) 
automatically builds the project with Gradle on every push and pull request, 
catching build errors early and confirming the codebase compiles cleanly 
before submission.

## Video Demonstration
Watch the full walkthrough here: [Ritmo Demo](https://youtu.be/ZBeU5Kr8lzg)

## AI Tools Used
AI tools (Claude) were used for code help and debugging during development — 
for example, working through issues with Compose state handling and Firebase 
integration. All code was reviewed and understood before being committed.

## Screenshots
<img width="1440" height="900" alt="Screenshot 2026-09-21 at 15 58 56" src="https://github.com/user-attachments/assets/34929c0b-3ceb-497b-85db-c76e6a048ecf" />
