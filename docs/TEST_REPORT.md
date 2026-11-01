# Foundation test report — 2026-11-01

## Scope and status

Phase A only. This is not a functional music player, completed application, or verified release. Feature phases B–F have not been implemented. All their acceptance tests remain unrun. No hosted CI run or publication has occurred.

## Environment

Debian GNU/Linux 13, x86_64. Temurin JDK 21.0.9+10 (download checksum verified), Gradle 9.8.2 (SHA-256 verified), AGP 9.5.0 with bundled Kotlin 2.2.10 and matching Compose compiler, Compose BOM 2026.03.01, Media3 1.11.1. Android SDK platform 36, build tools 36.0.0, emulator 37.2.13 build 16740729. API 35 Google APIs x86_64 image, Pixel 6 AVD named discnest-api35, 2048 MiB RAM, two virtual cores.

No /dev/kvm or CPU vmx/svm acceleration is available. `emulator -accel-check` reports code 3: KVM requires a CPU that supports vmx or svm. Software emulator was started with `-no-window -no-audio -no-boot-anim -gpu swiftshader -accel off -no-snapshot -memory 2048 -cores 2`. ADB reached device state; Android package service was found, but repeated boot-completion probes did not return 1. Runtime readiness is unresolved.

## Executed checks

- `git ls-remote origin HEAD`: succeeds through the configured proxy outside the command sandbox; remote has no refs. Existing checkout is unborn and initially contained no source.
- `./gradlew wrapper --gradle-version 9.8.2 --distribution-type bin --gradle-distribution-sha256-sum ...`: successful, full checksum is in wrapper properties.
- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`: successful after compatibility correction, 82 actionable tasks, 2m31s. See evidence/foundation-build.log.
- LRC unit suite: **3 passed, 0 failures, 0 errors, 0 skipped**. Covers repeated timestamps, fractions, invalid timestamps/metadata/overflow, and position-based selection after seeking. XML is in evidence/.
- Android lint: successful exit, **0 errors and 7 warnings**. Warning rules: DataExtractionRules, GradleDependency, MissingApplicationIcon, ObsoleteLintCustomCheck and OldTargetApi. These foundation warnings are not approved production-release exceptions. Full report: app/build/reports/lint-results-debug.html.
- `/workspace/setup/install.sh`: successful repeat execution, builds the added real-decoder instrumentation test, 82 actionable tasks, 6s. See evidence/install-repeat.log. Unit tests were up-to-date on repeat; the earlier three-test run provides execution evidence.
- Fixture generation: original 3-second 44.1kHz stereo 16-bit WAV tones at 440/660Hz; CC0 dedication, plus original LRC fixture. Fixture creation does not establish decoding or acoustic output.

## Corrections

Initial sandbox network calls could not reach the proxy. Network access works with supported command escalation; no destination bypass or credential extraction was used. Java initially failed TLS dependency resolution; the platform public proxy CA was imported into a local truststore, retaining certificate verification. Initial Compose BOM 2026.09.00 requires API 37, but the SDK installer reports that platform unavailable. Selected stable BOM 2026.03.01, compatible with API 36, and rebuilt successfully.

## Artifacts and pending checks

Debug APK: app/build/outputs/apk/debug/app-debug.apk, SHA-256 `90343a65fd16933f5ce812415afa441f053f99b4e8cd17ed91e2ae9263e9d81a`. This is a foundation APK displaying an explicit implementation-status message. It has no music-library or playback UI functionality. The generated debug key is not committed. No signed production release exists.

Compose foundation launch and real Media3 PCM decoding/position/pause smoke tests compile into the instrumentation APK. **Zero instrumentation tests have completed; none is marked passed or skipped.** Non-streamed `adb install --no-streaming -r ...` reported Success (exit 0). Launch via `adb shell am start -W -n org.discnest.app/.MainActivity` failed with Error type 3, activity does not exist; `aapt dump badging` confirms the correct launchable activity in the APK. The Android package/activity services subsequently became unavailable. `./gradlew :app:connectedDebugAndroidTest` reported a device installer RuntimeException, missing package/activity services, and zero completed tests, although its process and result-exit-code file returned zero. XML confirms tests=0, failures=0, errors=0, skipped=0; that is not a passing runtime suite. `python3 scripts/verify_android_results.py` correctly returns exit 1 for these results. CI now explicitly verifies both required test classes completed successfully after deleting stale generated results. Installation is observed but launch and runtime functionality remain unverified. See evidence/instrumentation.log, launch.log, install-apk-nostream.log and android-runtime-results.xml. The emulator is not a usable validated Android execution environment; an accelerated Android runner is needed to clear this gate. No screenshots or acoustic output verification exist. The decoder test, even if passed later, would not establish audible or bit-perfect output.

CI is configured for an accelerated API 35 emulator; it is unexecuted on GitHub. All comprehensive local library, permissions, background playback, vinyl/CD, palettes, playlists, lyrics UI, effects, gapless/crossfade, download, offline and robustness tests remain unrun. ClashFLAC is preliminarily audited and disabled; no provider integration is verified.

The install script and startup instructions are saved to the environment configuration draft. Saving does not publish or validate a fresh-task restore. Runtime foundation validation remains required before progressing beyond the requested Phase A gate.
