# Embedded ClashFLAC experiment

This branch ports the actual [ClashFLAC backend](https://github.com/ajisth69/clashflac/tree/0897400decfa510d8addd56ab23b25710fc99463) into msc. Its FastAPI provider routes run in an optional Python runtime in a separate Android `:clashflac` process. The existing local music player remains the default build.

**No embedded APK has been built or published.** The cloud network currently denies `chaquo.com`, which supplies Chaquopy's Android Python wheels. A saved environment configuration draft adds this domain; saving a draft does not activate it. Android wheel availability and compatibility still need validation after access is applied.

## Behavior

- Start and stop the server from msc Settings. A foreground notification provides a stop action. It does not start at boot or silently restart after being killed.
- Bind to a random `127.0.0.1` port. A fresh 256-bit session token is shared only through the app's private Binder connection and sent in an HTTP header. The service is not exported. Foreign browser origins and traversal requests are rejected.
- Keep configuration, temporary downloads and credentials in app-private storage. No provider accounts, shared client secrets, device keys or music are bundled.
- Invoke an Android-native FFmpeg executable extracted into the installed APK's native library directory. Desktop binaries and writing executable code into app data are not used.
- The current Android UI consumes catalog search only. Provider login, online playback and downloads still need an Android user flow and live validation with a configured account. The presence of the upstream routes does not establish that these features work on a phone.

The service's process is terminated after graceful shutdown so Python and native allocations can be reclaimed without stopping the music player. Android controls foreground-service lifetime and notification visibility. API 26+ is targeted; Android 13+ notification permission and recent Android service restrictions still need device validation.

## Build

Use JDK 21, Android SDK 36/build tools 36.0.0, NDK 27.2.12479018, and a host Python 3.11. Set `ANDROID_HOME`, `JAVA_HOME`, and optionally `MSC_BUILD_PYTHON` to the Python 3.11 executable. Keep the existing platform proxy and CA trust enabled. `chaquo.com` must be allowed by the environment's network policy.

Build the native audio tools from the pinned official source:

```sh
git clone --branch n8.0.1 --depth 1 https://github.com/FFmpeg/FFmpeg.git /workspace/setup/ffmpeg-8.0.1
export MSC_FFMPEG_SOURCE=/workspace/setup/ffmpeg-8.0.1
scripts/build_clash_ffmpeg.sh
./gradlew -Pmsc.embedClash=true :app:assembleDebug :app:lintDebug
```

The script checks the exact Git commit and a clean source tree, builds ARM64, ARMv7 and x86_64 PIE executables with 16 KB ELF segment alignment, and saves a corresponding FFmpeg source archive outside the repository. Generated `.so` files are ignored. Python requirements are pinned in [requirements-android.txt](requirements-android.txt), but their Android wheels have not yet resolved. Do not substitute a successful Linux pip install for this step.

The optional build uses version `0.3.0-embedded-alpha`, version code 3. Its application ID remains `org.msc.app`. Keep the same private debug keystore as prior releases if testing an upgrade; never commit that key. Do not replace the published local-player APK until the embedded runtime and provider setup have been tested on Android.

## Tests

The host suite starts the real vendored ASGI app and makes real local HTTP requests. It verifies readiness, private-session access, origin/path checks, duplicate starts and honest signed-out provider behavior, with no provider network calls:

```sh
python3.11 -m venv /workspace/setup/clash-host311
/workspace/setup/clash-host311/bin/pip install -r embedded-clash/requirements-android.txt
/workspace/setup/clash-host311/bin/python -m unittest discover -s embedded-clash/tests -v
```

The independent Android probe packages and executes the same native FFmpeg binaries. It checks the version and a WAV → FLAC → WAV round trip against every original PCM sample. It does not include or test Python:

```sh
./gradlew -p embedded-clash/native-probe connectedDebugAndroidTest
```

The normal player is checked separately with `:app:testDebugUnitTest` and `:app:lintDebug`. See [verification evidence](../docs/evidence/embedded-clash/README.md) for observed results and remaining checks.

## Provider configuration and licensing

The private runtime can read `files/clashflac/embedded-env.json`, a JSON object restricted to provider configuration names listed in `clash_runtime.py`. There is currently no Android configuration-import screen. Do not put secrets into source code, URLs, screenshots or log output. Arbitrary pickle/credential-file import is not exposed. Shared upstream client credential defaults have been removed; the original provider API routes remain present.

Upstream ClashFLAC source is MIT. Its `pywidevine==1.8.0` dependency is GPL-3.0-only. A combined embedded APK needs a GPL-compatible distribution with required notices and corresponding source, including msc changes and the native FFmpeg source/build instructions. FFmpeg is built without GPL/nonfree components and retains its LGPL requirements. The license files here record these obligations; they do not by themselves complete binary-distribution compliance. No combined embedded APK is distributed by this branch. See [UPSTREAM.md](UPSTREAM.md).
