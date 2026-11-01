# Foundation build and release status

Use JDK 21, SDK 36, build tools 36.0.0 and the pinned Gradle wrapper. `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` builds and validates the foundation. Then boot an API 35 device and run `./gradlew :app:connectedDebugAndroidTest`.

The GitHub Actions foundation workflow provisions an accelerated emulator. It has been written locally; no hosted run has been observed. It is not full release validation. Its test scope is only the baseline launch gate. The comprehensive acceptance plan remains pending.

Debug builds use Android's generated local debug key, which must not be committed. APK location: app/build/outputs/apk/debug/app-debug.apk. The application ID can be changed with -Pmsc.applicationId=your.package. For production, establish name clearance, a release version and authorized signing outside source control, then implement and pass all feature and runtime acceptance tests. No release workflow, signed production APK or published release is claimed yet.

After a fresh connected test run, execute `python3 scripts/verify_android_results.py`; the current runner can return zero even when the device failed and no tests ran. Delete only the generated `app/build/outputs/androidTest-results/connected` directory before rerunning to avoid stale results.
