# msc 0.2.0-local — Debug local-player build

Replaces the foundation screen with a real local music player. Install this APK over the earlier msc build, then choose **Import files** or **Scan device**. The signing certificate is compatible with the old APK and an actual in-place upgrade was checked on Android.

- Home, Library, Search and Player, plus a mini player outside the immersive player.
- Local file/folder import, MediaStore scans, metadata/artwork, grouping, sorting and local search.
- Media3 service playback, seek/previous/next, queue editing, shuffle/repeat and background media session.
- Vinyl/CD views using the same engine, artwork-derived colors and reduced motion.
- Persistent favorites, playlists/history, LRC/text lyrics import and device-supported audio effects.
- Optional ClashFLAC HTTPS **catalog metadata** adapter. No hosted server is bundled; live streaming/downloads are unavailable without an established permitted contract.

Verification: 6 JVM tests and 7 real Android API 28 instrumentation tests passed; no skips or failures in the final suite. Lint has zero errors and 18 warnings. README screenshots are real emulator captures with original test audio/artwork, not bundled music.

This is debug signed, not a complete production release. Crossfade, authorized online playback/downloads, acoustic/gapless timing, effects hardware verification, the complete lyrics/picker/process-death/device matrix and newest-runtime checks remain outstanding. See TEST_REPORT.md for the exact scope and evidence. Advancing media time does not establish audible or bit-perfect output.
