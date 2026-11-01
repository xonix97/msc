# msc foundation test report — 2026-11-01

This is a Phase A foundation, not a functional music player or a verified production release. Music feature phases and their acceptance scenarios remain unimplemented and unrun.

## Current build

App/project name: msc. Namespace and application ID: org.msc.app. Sources and tests use app/src/<variant>/java/org/msc/app. The application ID can be overridden with -Pmsc.applicationId=your.package. The configured API 35 test device is msc-api35.

Environment: Debian 13 x86_64, Temurin JDK 21.0.9+10, checksum-pinned Gradle 9.8.2, AGP 9.5.0 with Kotlin/Compose compiler 2.2.10, Compose BOM 2026.03.01, Media3 1.11.1, compile/target SDK 36, minimum SDK 26, build tools 36.0.0. The newer Compose BOM required unavailable API 37; the selected stable BOM builds against SDK 36. Java uses the platform public proxy CA with TLS verification enabled.

`./gradlew :app:clean` followed by the renamed reusable install script executed `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`. Build succeeded in 15s; all 82 tasks executed. This freshly ran the renamed LRC unit suite: **3 passed, 0 failures, 0 errors, 0 skipped**. Tests cover repeated timestamps/fractions, malformed metadata and timestamps/overflow, and seeking selection. Lint completed with **0 errors and 7 warnings** (not approved production exceptions). Both the debug and instrumentation APKs build. Evidence: evidence/msc-package-build.log and evidence/TEST-org.msc.app.LrcParserTest.xml.

The APK manifest reports msc, org.msc.app and launchable component org.msc.app.MainActivity. APK signature verification succeeds with v2 debug signing; it is not production signing. Both APK archives were inspected for the former project name and have no matches.

Debug APK: app/build/outputs/apk/debug/app-debug.apk. SHA-256: `5f54365a447074e2ca560d646ace4b7cccab9b1ac0c09e0e66d767c59afca474`. Release bundle filename: msc-0.1.0-foundation-debug.apk. The app displays an explicit foundation status; it does not expose music-library or playback UI features. Original mathematically generated stereo WAV fixtures are CC0; no copyrighted songs or signing keys are included.

## Android runtime remains blocked

This machine has no /dev/kvm or CPU vmx/svm acceleration. The emulator acceleration check failed. An earlier software-emulated API 35 Pixel 6 device exposed ADB and temporarily exposed the package service, but never established stable boot readiness. Non-streamed APK installation reported success; launcher resolution failed and package/activity services subsequently became unavailable. The connected test runner failed before executing test cases, although Gradle returned zero. Its XML reported zero cases, which does not establish a passing suite.

The renamed APK and package have **not been installed or launched**. Foundation UI and real-decoder instrumentation tests compile; **zero instrumented tests have completed**. No runtime feature, audible output, screenshots, hardware effect, gapless/crossfade transition, permission flow or offline player behavior is verified. Old logs are retained in previous Git commits and in the local setup archive; current evidence files belong to this freshly renamed build.

CI provisions an accelerated API 35 emulator and runs both instrumented classes. scripts/verify_android_results.py rejects empty/failed/skipped suites and missing required classes. Before a connected run, remove only the generated app/build/outputs/androidTest-results/connected directory to prevent stale results. Hosted CI has not been observed or validated from this environment.

ClashFLAC remains disabled after preliminary source audit; no online provider or authorized download path is implemented or verified. The app's full feature acceptance plan remains pending. The install/start instructions are saved in the environment draft; saving is not publication or fresh-task restore validation. The current release bundle is prepared locally; no GitHub release has been created because the current network policy blocks the API and upload domains.
