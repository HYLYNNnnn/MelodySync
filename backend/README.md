# MelodySync music service

Kotlin / Ktor service for the Android app. Requires JDK 17 or newer. The checked-in Gradle wrapper uses Gradle 8.9.

```sh
./gradlew test
./gradlew run
```

The default address is `http://127.0.0.1:8080`. `HOST` and `PORT` environment variables can override it. The Android emulator accesses the computer through `http://10.0.2.2:8080/`; the Android client's API base URL is configured separately.

## API

| Method | Route | Result |
| --- | --- | --- |
| GET | `/health` | JSON status and service name |
| GET | `/feed` | Home sections and album metadata |
| GET | `/playlists` | All playlists |
| GET | `/playlist/{id}` | One playlist and its songs |
| GET / HEAD | `/songs/{filename}` | Catalog audio; supports byte-range requests |

Feed fields and song fields retain the original Android contract. Playlist IDs are integers. Song `src` values are relative, percent-encoded paths such as `/songs/solo.mp3`; the client resolves them against its API base URL.

Missing or malformed playlist IDs return 400, unknown IDs and songs return 404, and unexpected failures return 500. Errors use JSON `{ "code": "...", "message": "..." }`. Only audio explicitly referenced in the validated catalog is exposed; other classpath resources are not downloadable.

At startup, the service loads and validates `feed.json` and `playlists.json`, checks matching IDs and audio references, and fails clearly if required resources are missing or invalid. It does not require a database or external music API.

Tests cover the real bundled catalog, JSON responses, error statuses, Unicode filenames, HEAD, audio byte ranges and resource isolation. Run results and Android verification are recorded in the repository's repair notes.
