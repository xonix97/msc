# msc 0.2.0-local test report — 2026-11-01

## Result and scope

The foundation status screen has been replaced with a local music player. **6 JVM tests and 7 real Android instrumentation tests passed; 0 failures, errors or skips in the final run.** This is a debug local-player build, not the fully completed original product release. Remaining capabilities and verification gaps are listed below.

## Environment and commands

Debian 13 x86_64; Temurin JDK 21.0.9+10; checksum-pinned Gradle 9.8.2; AGP 9.5.0/Kotlin 2.2.10; Compose BOM 2026.03.01; Media3 1.11.1; SDK/target API 36, minimum API 26; build tools 36.0.0. Android emulator 37.2.13, AOSP Android 9/API 28 x86_64, Pixel 2 profile, AVD msc-api28, 1536 MiB RAM and two virtual cores. No /dev/kvm is available. The smaller official AOSP image booted with software emulation after the earlier API 35 attempt was unstable.

Boot command: `emulator @msc-api28 -no-window -no-audio -no-boot-anim -gpu swiftshader -accel off -no-snapshot -memory 1536 -cores 2`. ADB and `sys.boot_completed=1` established runtime readiness. System animations were disabled for deterministic UI tests. Software startup is slow; a standalone `am start -W` reported its wait timeout, but the app subsequently rendered and was exercised. An initial System UI ANR on this software emulator was dismissed; no application crash was observed in the successful test run. This does not establish a complete ANR/performance matrix.

Build: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`. Final layout build succeeded in 20s. Lint: **0 errors, 18 warnings** (11 UseKtx suggestions, 3 dependency updates, OldTargetApi, ModifierParameter, ExportedService and DataExtractionRules). The exported media session service is intentional for system media controls. Warnings are recorded, not presented as a completed production audit.

Android: remove only generated `app/build/outputs/androidTest-results/connected`, run `./gradlew :app:connectedDebugAndroidTest`, then `python3 scripts/verify_android_results.py`. The verifier requires all seven exact cases and rejects missing, empty, failed or skipped results. See evidence/player-build.log, android-runtime.log and the current XML reports. The native runner reports aggregate XML time as zero; use the runner log for wall time rather than interpreting that as execution duration.

## Executed tests

| Suite | Passed | Behavior exercised |
| --- | ---: | --- |
| LrcParserTest | 3 | Fractional/repeated timestamps, malformed/overflow input, position-based seeking selection |
| LibraryRulesTest | 3 | Accent-insensitive multiword metadata search, sorting, playlist-name validation |
| FoundationTest | 1 | Actual app launch, removal of the old message, Home/Library/Search navigation |
| LibraryPersistenceTest | 1 | Native WAV metadata import, duplicate handling, favorites/history/counts, playlist rename/entries/delete and lyrics/offset persistence across database reopen |
| PlaybackFixtureTest | 3 | Actual ExoPlayer WAV, FLAC and MP3 preparation/decoding, advancing media time, pause and error checks |
| ProviderContractTest | 1 | Documented ClashFLAC metadata schema and HTTPS/server-address validation; no live provider access |
| ServicePlaybackTest | 1 | Real MediaSessionService playback, advancing position, pause, seek, queue reorder, shuffle/repeat setting, second-track playback, vinyl/CD switching without position/session reset, and playback while Activity is backgrounded |

The first Android attempt had four passes and one service-test failure caused by test cleanup calling MediaController.releaseFuture off its application thread. Cleanup was corrected to run on Main. The expanded suite then passed all seven cases, and passed again after layout changes. The failure log is retained in evidence/first-runtime-attempt.log. No assertion was disabled and an empty runner result was never accepted.

## Standalone UI and artifact

A real permission-denial flow left import usable and displayed the appropriate explanation. After granting storage access, Scan device found two original test tracks through MediaStore. Actual artwork, local search, the mini player, both disc views and provider settings were inspected on Android. Player artwork was resized after screenshots showed that it pushed transport controls below the viewport. Final screenshots in docs/screenshots/ come from the real emulator using original generated tones/artwork. These samples are not bundled in the user APK.

APK: releases/msc-0.2.0-local-debug.apk. Application ID org.msc.app; versionCode 2; label msc. SHA-256 `cc419db62ad84655d07e38bc7ee966c014d8eebf3513f5467ce81db05c203bb0`. APK signature verification succeeds with v2 debug signing. Two Android user directories initially selected different debug keys; the final build uses the retained original key under ANDROID_USER_HOME=/workspace/android-home. Certificate compatibility was verified, and every ZIP payload entry matches the runtime-tested APK; only signing differs. A real installation of the old foundation APK followed by `adb install --no-streaming -r` of this APK both reported Success. Android confirms versionCode 2/versionName 0.2.0-local; see the upgrade and artifact-verification evidence. No key is committed and this is not production signing.

## Remaining limits

- ClashFLAC catalog settings and adapter are implemented, disabled without an authorized HTTPS server. Its parser contract is tested; no live server/catalog access is verified. No permitted streaming/download contract or hosted endpoint has been established. Streaming, downloading and DRM-related routes are unavailable.
- Crossfade and authorized downloads are not implemented. Queue transitions use Media3; acoustic gapless/crossfade timing and bit-perfect output are not verified. Advancing media timestamps and host-muted emulation do not prove audible output.
- Audio effects use real Android Equalizer/BassBoost APIs with capability handling and stored preferences. Their audible response, full attachment/removal/persistence matrix and physical output routes have not been separately verified.
- LRC parsing and lyrics persistence are tested; the complete import/highlight/auto-scroll/tap-to-seek UI matrix and embedded-lyrics extraction are not verified. Embedded extraction is not implemented.
- SAF file/folder import, missing-file handling, live storage observation, paused queue restoration and playlist editing are implemented, but the complete picker/revocation/process-death/large-library/playlist UI matrix has not run. The persistence and service tests cover the narrower behaviors listed above.
- API 26, Android 13+ permission behavior, latest runtime API, Bluetooth/headset/call/focus interruptions, lock-screen controls, large fonts, landscape, accessibility/performance profiling and the full offline acceptance matrix need further device/CI execution. Local playback has no network dependency, but a full network-disabled UI matrix has not run.
- GitHub Actions is configured; hosted CI results have not been observed. The releases/ folder is a Git repository artifact location, not a GitHub Releases publication. Cloud install/start instructions are saved in a draft; review/save and publish the environment settings to retain the prepared setup. A fresh-task snapshot restore has not been independently tested.
