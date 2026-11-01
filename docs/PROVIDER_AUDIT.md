# Provider audit

## JioSaavn online source — 2026-11-02

Inspected sumitkolhe/jiosaavn-api at commit `fc6bd132193d1fc71c2ab3db373e78b92884f67c`, including its search controller, song model and song payload helper. The documented endpoint is `GET https://saavn.dev/api/search/songs?query=...&page=0&limit=20`, returning `success`, `data.results`, song metadata, primary artists, image links and quality-ranked `downloadUrl` links. msc uses these supplied links for streaming and does not implement a downloader. No upstream server, key extraction or media decryption implementation is copied into the app.

The client is native Kotlin. Searches are debounced and stop updating the UI when cancelled. Requests require HTTPS, have bounded connect/read timeouts and a 1 MiB response limit, do not follow redirects, and distinguish offline, timeout, HTTP 403/429 and malformed-response errors. Returned audio and artwork URLs must use HTTPS on the `saavncdn.com` domain or its subdomains, without embedded credentials, unusual ports or fragments. Unavailable songs stay unavailable; no URL is invented. Artwork requests are bounded to 2 MiB and decoded at thumbnail size.

One live search attempt from this cloud worker returned HTTP 403. Live catalog availability, regional behavior, real song playback, CDN quality and operational guarantees are therefore **unverified**. Synthetic contract/UI tests and actual localhost AAC/MP4 streaming tests are separate evidence, not successful access to the public service. No account or copyrighted song was used in validation. The API is unofficial and can change or disappear. No YouTube Music integration is included.

## ClashFLAC inspection and local adapter

Inspected ajisth69/clashflac at commit 0897400decfa510d8addd56ab23b25710fc99463 on 2026-11-01. Read README.md, LICENSE, documentation.md and route declarations in server.py. Did not run its backend or copy its implementation into msc.

The project uses an MIT code license and a Python FastAPI backend plus Vite frontend. It documents Qobuz, Amazon and Tidal engines and Spotify metadata. Documented routes include GET /api/search, POST /api/resolve, POST /api/download, Tidal device authentication, auth status, and download status. Runtime needs Python dependencies and provider-specific account/configuration; these are not msc requirements while the integration is disabled.

The upstream project includes Amazon download/decryption routes and provider account configuration. This main-branch client consumes ClashFLAC catalog metadata only. The separate embedded-server experiment is documented on its own branch. No provider accounts, credentials or device keys are bundled with this APK.

Status: the configured ClashFLAC adapter remains catalog-only and disabled by default. No live ClashFLAC search, streaming, downloading, authorization, rate-limit or operational-maintenance guarantees were verified. Source inspection alone does not establish provider compatibility. The experimental embedded server work is retained on the separate `embedded-clash` branch.

## msc 0.2.0 adapter

The app now includes a configurable HTTPS ClashFLAC **catalog metadata** adapter. It uses the inspected GET /api/search endpoint with q and limit parameters and maps asin/title/artist/album/duration_sec. Optional catalog URLs are not mapped to playable or downloadable audio. User-provided server settings are disabled by default; no arbitrary hosted instance, account or credentials are fabricated. The adapter validates URLs, uses default TLS verification, bounds time and response size, and handles HTTP auth/rate-limit/error statuses with manual retry. Android contract tests parse the real documented schema and reject insecure/credential-bearing server URLs. These are contract tests, not a live provider integration result. No server endpoint, catalog entitlement, streaming authorization or download permission has been supplied or independently established. Streaming, downloading and DRM-related routes remain unimplemented and unavailable.
