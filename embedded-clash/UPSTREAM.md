# Embedded ClashFLAC source

Source: https://github.com/ajisth69/clashflac, commit 0897400decfa510d8addd56ab23b25710fc99463. Upstream MIT license is included at src/main/python/clashflac/LICENSE. No account files, device files, tokens, keys or copyrighted music are copied.

Android changes: run the real FastAPI app on an authenticated loopback socket; keep its working files in app-private storage; execute it in a dedicated Android process; remove bundled/shared client secrets and require user-owned provider configuration. The original routes remain present. Android requirements use a maintained Audible/httpx combination and Uvicorn's supported pure-Python asyncio/h11 mode. Native FFmpeg is built separately from official source and placed in the APK's read-only native library directory.

The upstream compiled web frontend is omitted: msc uses its native Android UI and does not expose a browser session for the private backend.

The pywidevine dependency is GPL-3.0-only. An embedded APK must not be represented as MIT-only: distribution requires GPL-compatible licensing, notices and corresponding source. This experimental branch has not produced or distributed an embedded APK. Native dependency resolution is currently blocked by environment policy for chaquo.com. No provider playback, download or DRM behavior is verified.
