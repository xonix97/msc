# Provider audit — preliminary

Inspected ajisth69/clashflac at commit 0897400decfa510d8addd56ab23b25710fc99463 on 2026-11-01. Read README.md, LICENSE, documentation.md and route declarations in server.py. Did not run its backend or copy its implementation into DiscNest.

The project uses an MIT code license and a Python FastAPI backend plus Vite frontend. It documents Qobuz, Amazon and Tidal engines and Spotify metadata. Documented routes include GET /api/search, POST /api/resolve, POST /api/download, Tidal device authentication, auth status, and download status. Runtime needs Python dependencies and provider-specific account/configuration; these are not DiscNest requirements while the integration is disabled.

The README explicitly advertises native Widevine decryption for Amazon downloads. server.py documents download/decryption behavior. Such functionality will not be integrated. The README's educational/personal-use notice and MIT code license do not grant music catalog streaming or download rights. Documentation describes account tokens; none were requested, inspected or extracted. No access-control bypass was attempted.

Status: unavailable/disabled pending a documented permitted API contract and track-level rights. No live search, streaming, downloading, authorization, rate-limit or operational-maintenance guarantees were verified. Source inspection alone does not establish provider compatibility. Other unofficial services, Jamendo and Internet Archive are not yet audited or enabled. No mock or real provider currently claims functionality in this foundation build.
