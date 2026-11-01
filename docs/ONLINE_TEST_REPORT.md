# msc 0.3.0-online-alpha verification — 2026-11-02

This debug alpha adds native JioSaavn search/streaming and the glass UI. **6 JVM tests and 14 Android instrumentation cases passed**, with no failures, errors or skips in the successful full run. This is not proof that the public unofficial service works from this cloud worker: its live search request returned HTTP 403.

## Build and device

Temurin JDK 21.0.9+10, checksum-pinned Gradle 9.8.2, AGP 9.5.0, Compose BOM 2026.03.01 and Media3 1.11.1. SDK/target API 36, minimum API 26. The device is the API 28 AOSP x86_64 Pixel 2 emulator running without KVM, with software graphics, 1536 MiB RAM and two virtual cores. System animation scale was disabled for deterministic instrumentation. Neither those tests nor static screenshots establish physical-device frame rates.

Build/check command: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`. Lint reports **0 errors and 20 warnings**. Warnings are retained; they are not represented as a completed production audit.

Runtime command: remove only generated `app/build/outputs/androidTest-results/connected`, run `./gradlew :app:connectedDebugAndroidTest`, then `python3 scripts/verify_android_results.py`. The verifier requires the exact 14 methods and rejects missing, empty, failed or skipped results. Actual logs/XML and the APK hash are in [evidence/online](evidence/online/).

## Test coverage

| Suite | Passed | Observed behavior |
| --- | ---: | --- |
| LrcParserTest / LibraryRulesTest | 6 JVM | Timestamp parsing, local metadata search/sorting and playlist validation |
| FoundationTest | 1 Android | Real app launch, glass Home/Library/Search navigation, JioSaavn selection and return to phone search |
| LibraryPersistenceTest | 1 Android | Real audio metadata, favorites/history, playlists and lyrics survive repository reopen |
| PlaybackFixtureTest | 3 Android | Actual WAV, FLAC and MP3 decoding and advancing media time |
| ProviderContractTest | 1 Android | Existing ClashFLAC catalog contract and HTTPS validation |
| ServicePlaybackTest | 1 Android | Real MediaSession playback, pause, seek, queue reorder, repeat/shuffle, disc switching and activity backgrounding |
| JioSaavnTest | 4 Android | Documented response parsing, safe quality selection, metadata round trip, unavailable-song handling, destination validation, real local HTTP requests, query encoding, cancellation and HTTP 429 |
| OnlineUiTest | 2 Android | Native song-row play/queue actions, disabled unavailable results and working offline retry |
| NetworkPlaybackTest | 1 Android | Real HTTP AAC/MP4 decoding through the music service, pause/seek, actual position advancement after the Activity stops, and exclusion of expiring online URLs from saved local queues |

The HTTP test serves only an original CC0 AAC/MP4 tone on `127.0.0.1`. It is not a provider song or a live CDN test. Test audio is confined to the test APK; the user APK was checked to contain no audio fixtures.

The first 14-case run had one failure: the three-second HTTP fixture ended before the slow emulator completed its Activity transition. Its log/XML are retained. The fixture was extended to 15 seconds with explicit repeat, and the assertion was strengthened to require at least 400 ms of new playback progress after the Activity reached its stopped state. All 14 then passed. No failed case was removed or skipped.

## UI and artifact

Screenshot review caught inherited black text on the first transparent panels. The scaffold and glass surfaces now explicitly use the theme's foreground color. The updated Home, local library/search, online search and disc views were captured from the real app using original test material. The online screenshot is an empty search prompt; it does not fabricate a working live catalog.

Application ID `org.msc.app`, label **msc**, version code **3**, version name **0.3.0-online-alpha**. The APK is [msc-0.3.0-online-alpha-debug.apk](../releases/msc-0.3.0-online-alpha-debug.apk); its SHA-256 is in [SHA256SUMS](../releases/SHA256SUMS). It uses the retained debug certificate, SHA-256 `1942279f5e31f46d6a28f318d10c099f96d34589547f26759ce75657d212d3fe`, matching 0.2.0. A real 0.2.0 installation followed by an in-place 0.3.0 update succeeded. No signing key is committed. This is not a production-signed release or a GitHub Releases publication.

## Limits

- Live JioSaavn catalog access, actual provider song playback, regional restrictions and reported CDN quality remain unverified. The unofficial API can change or become unavailable. Contract tests and localhost playback must not be labeled live-provider success.
- Online favorites, persistent online playlists, downloads and YouTube Music are not implemented. Online queues last for the active service session; expiring links are not restored as a saved local queue.
- Crossfade, measured gapless timing, acoustic/bit-perfect output, physical audio effects, Android 13+ permissions, latest runtime API behavior, Bluetooth/call interruption, large-font/landscape/accessibility matrices and device performance need further validation.
- The embedded ClashFLAC experiment remains on its separate branch and is not bundled in this APK. Hosted CI results and fresh cloud snapshot restoration have not been observed.
