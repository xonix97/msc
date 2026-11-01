# msc — foundation in progress

Android-only vinyl/CD music-player project. **This is a Phase A foundation, not a functional music player or verified release.** No online music providers are enabled, and no copyrighted music is included.

Requirements: JDK 21, Android SDK platform 36/build tools 36.0.0 and an Android emulator or device (API 26+). Build with `./gradlew :app:assembleDebug`, run `./gradlew :app:testDebugUnitTest :app:lintDebug`, then `./gradlew :app:connectedDebugAndroidTest` with a booted device. The debug APK is under `app/build/outputs/apk/debug/`. Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

See docs/ARCHITECTURE.md, docs/TEST_PLAN.md and docs/TEST_REPORT.md for scope and evidence. The project name has not been cleared for release. No publication or production signing has occurred.

After a fresh connected test run, execute `python3 scripts/verify_android_results.py`; the current runner can return zero even when the device failed and no tests ran. Delete only the generated `app/build/outputs/androidTest-results/connected` directory before rerunning to avoid stale results.
