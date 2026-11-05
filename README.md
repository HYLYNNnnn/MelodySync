# MelodySync

An Android music application built with Kotlin, Jetpack Compose, Navigation, Hilt, Retrofit, Room and Media3 ExoPlayer, with a Kotlin/Ktor music service.

## Features

- Browse the home feed and open an album's track list.
- Add/remove favorite albums and keep them in a local Room database across app restarts.
- Play tracks through an Activity-shared mini player, pause/resume, switch songs and retry failures.
- Keep the player while navigating between pages or rotating the screen. Playback pauses when the app leaves the foreground.
- Loading, empty and failure states with retry controls; local artwork when a remote cover cannot load.
- Typed JSON APIs, validated catalog resources and audio HEAD/byte-range responses.

## Repository layout

| Directory | Purpose |
| --- | --- |
| `android/` | Native Android client; open this directory in Android Studio |
| `backend/` | Ktor service, catalog JSON and bundled demonstration audio |

This repository contains the client and service together. Add the **root `melodysync` folder** to GitHub Desktop when publishing source. SDKs, local paths, dependencies and build output do not belong in the repository.

## Start the music service

Use JDK 17. From `backend/`:

```sh
./gradlew run
```

The default address is `http://127.0.0.1:8080`; `/health` returns the service status. Start this service before opening the Android app. If the app was opened first, press Retry on its home page.

Backend tests:

```sh
./gradlew test
```

The service uses Ktor, rather than json-server. More details are in [backend/README.md](backend/README.md).

## Run Android

1. Open `android/` in Android Studio.
2. Use JDK 17 for Gradle and install Android SDK Platform 35. Let Gradle synchronize dependencies.
3. Start an Android emulator, then run the `app` configuration.
4. The debug build connects to `http://10.0.2.2:8080/`, which is the emulator's address for the computer hosting the service.

The verified build uses Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 1.9.25 and Compose compiler 1.5.15. These follow the documented [AGP requirements](https://developer.android.com/build/releases/agp-8-7-0-release-notes) and [Compose/Kotlin pairing](https://developer.android.com/jetpack/androidx/releases/compose-kotlin). Playback uses the [AndroidX Media3 ExoPlayer library](https://developer.android.com/media/media3/exoplayer).

From `android/`, command-line checks are:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The generated debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Its backend must be running for online music browsing and streaming. Favorite albums remain available locally.

## Configure another server

The base URL is a build setting and must end with `/`:

```sh
./gradlew :app:assembleDebug -PAPI_BASE_URL=https://your-music-server.example/
```

For a USB-connected Android phone, forward its local port:

```sh
adb reverse tcp:8080 tcp:8080
./gradlew :app:assembleDebug -PAPI_BASE_URL=http://127.0.0.1:8080/
```

Install that debug APK on the phone. The default emulator address is not a physical phone's address. Debug builds permit local HTTP; release builds require HTTPS. A release build needs a real HTTPS backend URL and your own signing configuration before distribution. No server credentials are embedded in the app.

## Validation and scope

The repair includes 22 Android unit/Robolectric tests covering player behavior, network contracts, state races, Room close/reopen persistence and production Hilt Activity/navigation startup. The backend has 11 tests for catalog integrity, JSON, errors and audio ranges. Android debug compilation and lint are part of verification; lint has no errors, with development/resource warnings documented in the repair notes.

Playback is foreground-only. Background services, notification/lock-screen controls, account login, automatic next-track queues and offline audio downloads are outside this implementation. Covers still use the original remote URLs, with local placeholders if unavailable. Catalog durations are display metadata; audio bytes are streamed from the bundled resources.

Observed Android 15 emulator checks include real audio processing, pause, shared playback, rotation and favorites surviving a process restart. Screenshots: [Home](docs/images/home.png), [Playlist/player](docs/images/playlist-playing.png).

Repair notes and observed emulator results: [修复说明.zh-CN.md](修复说明.zh-CN.md).
