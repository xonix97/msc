# Provider audit — preliminary

Inspected ajisth69/clashflac at commit 0897400decfa510d8addd56ab23b25710fc99463 on 2026-11-01. Read README.md, LICENSE, documentation.md and route declarations in server.py. Did not run its backend or copy its implementation into msc.

The project uses an MIT code license and a Python FastAPI backend plus Vite frontend. It documents Qobuz, Amazon and Tidal engines and Spotify metadata. Documented routes include GET /api/search, POST /api/resolve, POST /api/download, Tidal device authentication, auth status, and download status. Runtime needs Python dependencies and provider-specific account/configuration; these are not msc requirements while the integration is disabled.

The README explicitly advertises native Widevine decryption for Amazon downloads. server.py documents download/decryption behavior. Such functionality will not be integrated. The README's educational/personal-use notice and MIT code license do not grant music catalog streaming or download rights. Documentation describes account tokens; none were requested, inspected or extracted. No access-control bypass was attempted.

Status: unavailable/disabled pending a documented permitted API contract and track-level rights. No live search, streaming, downloading, authorization, rate-limit or operational-maintenance guarantees were verified. Source inspection alone does not establish provider compatibility. Other unofficial services, Jamendo and Internet Archive are not yet audited or enabled. No mock or real provider currently claims functionality in this foundation build.

## msc 0.2.0 adapter

The app now includes a configurable HTTPS ClashFLAC **catalog metadata** adapter. It uses the inspected GET /api/search endpoint with q and limit parameters and maps asin/title/artist/album/duration_sec. Optional catalog URLs are not mapped to playable or downloadable audio. User-provided server settings are disabled by default; no arbitrary hosted instance, account or credentials are fabricated. The adapter validates URLs, uses default TLS verification, bounds time and response size, and handles HTTP auth/rate-limit/error statuses with manual retry. Android contract tests parse the real documented schema and reject insecure/credential-bearing server URLs. These are contract tests, not a live provider integration result. No server endpoint, catalog entitlement, streaming authorization or download permission has been supplied or independently established. Streaming, downloading and DRM-related routes remain unimplemented and unavailable.
