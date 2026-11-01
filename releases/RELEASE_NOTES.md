# msc 0.3.0-online-alpha — 2026-11-02

Download [msc-0.3.0-online-alpha-debug.apk](msc-0.3.0-online-alpha-debug.apk). Application ID `org.msc.app`, version code 3, minimum Android 8/API 26. This is a debug-signed alpha. The previous [0.2.0 local-player APK](msc-0.2.0-local-debug.apk) remains available.

- New liquid glass presentation: translucent panels, artwork-colored light, floating navigation with a spring selection capsule, press feedback and short page fades. Reduced motion is respected.
- New **Home → Explore JioSaavn** and **Search → JioSaavn**. The native unofficial API client parses song metadata and safe HTTPS stream URLs, then uses the existing Media3 player for streaming, queueing, pause/seek and background playback.
- Clear loading, offline, unavailable-song and retry states. No bundled API key, account, Python server or music.
- Local file import, scanning, playlists, favorites, lyrics and vinyl/CD views remain available. Online favorites/playlists/downloads and YouTube Music are not implemented.

**Live JioSaavn access is unverified:** a public search request from this cloud worker returned HTTP 403. The successful tests use the documented contract and original audio over localhost; they do not establish live catalog availability or real provider-song playback. Service/region availability and expiring stream links can affect playback.

Six JVM cases and 14 Android cases passed on the API 28 software emulator. Lint has zero errors and 20 warnings. Read [TEST_REPORT.md](TEST_REPORT.md) for exact scope and remaining device/performance checks. These results do not certify acoustic output, frame rates, latest-Android behavior or production readiness.

The signing certificate matches the earlier APK, and an in-place update was tested. Verify files against [SHA256SUMS](SHA256SUMS). Install this APK over msc; uninstalling deletes app data. The `releases/` folder is a repository artifact location, not a GitHub Releases publication.
