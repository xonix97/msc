# Acceptance plan

Phase A: build the baseline, boot API 35, verify ADB, install, launch, run Compose instrumentation, run deterministic LRC tests. Generate original stereo PCM fixtures; never bundle copyrighted music. Unit tests alone do not satisfy the runtime gate.

Phase B: import fixtures through MediaStore and SAF; grant/deny/regrant media permission; scan/rescan/remove; metadata fallbacks; local search; actual ExoPlayer playback, progressing position, pause/resume, seek, queue mutation, shuffle/repeat, background notification and media controls, process recreation and offline use.

Phase C: verify vinyl/CD selection preserves the same player, position and queue. Test rotation pause/resume, visibility and lifecycle, reduced motion, large fonts, contrast, missing artwork and narrow displays. Capture real emulator screenshots.

Phase D: persist playlists, favorites, history and queue; validate LRC synchronization and tap-to-seek against playback; attach/release device-supported audio effects; measure gapless/crossfade transitions with deterministic fixtures and captured output where possible. Advancing timestamps alone do not establish audible output.

Phase E: audit ClashFLAC and alternatives before enabling any integration. Mock HTTP tests must cover auth, rate limiting, timeouts, malformed data, cancellation and disabled sources. Live authorized tests and licensed download-byte checks are separate required evidence.

Phase F: full instrumented regression, lint, unit suite, clean APK build/install, offline checks, accessibility, lifecycle robustness, screenshots and honest release documentation. API 26, 33+ and current stable API/device variations remain required before release.

Every acceptance item is pending unless a current test report records command, target and result. No provider, download, acoustic or release behavior is verified by the foundation tests.
