# Build and release

JDK 21, SDK platform 36/build tools 36.0.0 and the checksum-pinned Gradle wrapper are required. Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`. Boot a device and run `./gradlew :app:connectedDebugAndroidTest`, followed by `python3 scripts/verify_android_results.py`. Empty/failed/skipped runs cannot satisfy the runtime gate. Remove only generated connected results before each fresh run.

Install `app/build/outputs/apk/debug/app-debug.apk` using `adb install --no-streaming -r ...`; launch org.msc.app/.MainActivity. The same debug key and increasing version code allow upgrading the earlier msc build. Never commit that key. Production releases require separately authorized signing credentials and the full outstanding acceptance matrix.

The GitHub Actions workflow builds, lints and runs the instrumented suite on an accelerated emulator, preserving APKs and reports as artifacts. Hosted results must be observed before claiming CI passed. Local software-emulator results and remote CI results are distinct.

The repository releases/ folder carries the current debug APK, notes, checksums and evidence summary. It is not a GitHub Releases publication or a signed production release. Release checksums must correspond to the exact copied APK, and the test report must describe that build's actual verification and remaining limits.

In this cloud environment, keep ANDROID_USER_HOME=/workspace/android-home as in the saved startup instructions. It selects the retained debug key used by the earlier APK. A different Android user directory can select a different debug key and make an in-place update fail. Do not commit either key.
