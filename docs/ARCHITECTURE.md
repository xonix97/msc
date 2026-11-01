# Architecture

Native Kotlin, Jetpack Compose and AndroidX Media3 suit Android-only playback without a cross-framework lifecycle bridge. Flutter and React Native add native plugin boundaries for MediaStore, audio focus, effects and notifications without a required portability benefit.

MscApplication owns a small application graph. LibraryRepository uses Android SQLiteOpenHelper with typed models, explicit schema versioning and transactional playlist ordering. This small app uses platform SQLite directly rather than introducing an ORM/code-generation toolchain; instrumentation verifies metadata ingestion and persistence across a database reopen. Favorites, counts, history, playlist order and lyrics are local. Upserting metadata preserves personal state.

MediaStore scans and SAF import/folder traversal run on IO dispatchers. URI identity prevents duplicate entries. Only fully successful scans prune missing entries from their own source; audio files are never deleted. MediaMetadataRetriever extracts metadata and bounded artwork. A bounded LRU bitmap/palette cache avoids repeated decoding. Artwork failures have a normal visual fallback.

PlaybackService exclusively owns ExoPlayer, audio focus, becoming-noisy handling, foreground media notifications, MediaSession and Android audio effects. The UI uses a MediaController and observable state. Queue/repeat/shuffle/position are saved; restoration prepares a paused queue rather than starting sound automatically. Presentation changes never create another player. Disc rotation uses frame time only while visible, resumed, actually playing, and motion is enabled. The position ticker stops when the Activity stops; queue snapshots are refreshed on player events rather than copied every tick.

MusicViewModel coordinates commands, library flows, debounced local search and cancelable catalog searches. Source adapters do not supply executable code. ClashFlacCatalog validates HTTPS addresses, limits response size/time, handles auth/rate-limit/error responses and maps the inspected metadata contract. Catalog URLs are never treated as licensed audio streams. No online playback/download path is exposed.

Audio effects attach to the one ExoPlayer audio session, use real supported band limits, cap positive gain and bass, persist preferences and release native resources. This does not establish clipping-free, acoustic or bit-perfect output. LRC highlighting follows player position; plain text fallback and a stored offset are supported. Lyrics come only from user files in this build.

App/project name: msc. Namespace/application ID: org.msc.app; property msc.applicationId provides an override. SDK 36, minimum API 26, AGP 9.5.0/Kotlin 2.2.10, Compose BOM 2026.03.01 and Media3 1.11.1 are build-validated. API 37 is unavailable in this SDK environment, so the newer incompatible Compose BOM is not selected.
