# MelodySync Android client

Open this directory in Android Studio. Use JDK 17, Android SDK Platform 35 and the checked-in Gradle wrapper. Start the repository's Ktor backend before running the app in an emulator.

Default debug API URL: `http://10.0.2.2:8080/`. Override it with `-PAPI_BASE_URL=https://your-server.example/` (trailing slash required). See the root README for USB phone forwarding and release HTTPS configuration.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Home and favorites are Fragment destinations containing Compose screens. Safe Args passes the selected album to its playlist. An Activity-owned Hilt ViewModel shares the Media3 player across pages and configuration changes; closing that owner releases the player. Leaving the app pauses foreground playback.

Favorites use Room and are stored on the device. Media URLs are resolved against the configured API server, including encoded Chinese paths. Network requests and favorite observers are cancelled/replaced when the selected album changes.

Local SDK paths, generated APKs, Gradle caches and signing files are ignored. The root README and Chinese repair notes contain the complete setup and validation results.
