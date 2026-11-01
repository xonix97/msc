# Embedded ClashFLAC verification — 2026-11-01

These checks apply to the experimental `embedded-clash` branch. They do not describe an embedded APK release. The published `0.2.0-local` APK is unchanged.

| Check | Observed result | Scope |
| --- | --- | --- |
| Real ClashFLAC private HTTP server | 11 passed | Linux host, Python 3.11.17, real ASGI app, no provider credentials |
| Exact host requirements | `pip check` passed | Linux compatibility only |
| Native FFmpeg build | ARM64, ARMv7, x86_64 built | Official n8.0.1 source; 16 KB ELF LOAD alignment checked |
| Android native executable | 2 passed, 0 failures/errors/skips | API 28 AOSP x86_64 software emulator; expected version and sample-exact WAV/FLAC/WAV conversion |
| Normal msc unit suite | 6 passed | Local-player build, embedded flag off |
| Normal msc lint | 0 errors | Embedded flag off; existing warnings retained |
| Optional module Gradle configuration | Passed | `-Pmsc.embedClash=true :embedded-clash:tasks`; configuration, not compilation |
| Complete embedded build | Blocked | CONNECT 403 for `chaquo.com/pypi-13.1`; no Android-native Python wheels resolved |

The dependency probe used upstream requirements and Python 3.12 before the port selected Python 3.11 and maintained host-compatible dependencies. Its recorded network denial establishes the infrastructure blocker, not compatibility of the port's final Android requirements. A Kotlin-only attempt excluding Python proxy generation also could not compile because the plugin requires that generated source task. No optional-runtime compilation or APK result is claimed.

Evidence files include the actual test logs/XML, normal build log, dependency failure excerpt, and native binary hashes/ELF reports. Generated binaries, keystores, provider credentials and music are not included. The host package versions are recorded for reproduction; they are not an Android wheel lockfile.

Still to validate: Android Python wheels and imports; embedded manifest and runtime compilation; service readiness, stop/restart, process isolation, battery/background behavior and notification permissions on Android; ARM execution; Android 13+ and recent SDK restrictions; secure provider setup in the native UI; real account login, search, playback and downloads. No live provider or DRM operation was tested. Full binary licensing/notices/source packaging must precede distribution.

The saved environment draft adds `chaquo.com`. Its configuration must be reviewed, saved and published through environment settings before retrying dependency resolution. Draft saving alone does not change this worker's network policy.
