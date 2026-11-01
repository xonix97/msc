# Contributing

Read docs/ARCHITECTURE.md and docs/TEST_REPORT.md. Keep one playback session, use content URIs and IO dispatchers for storage/network work, and never modify original user audio. Add deterministic tests for library and player behavior and execute real Android instrumentation for platform behavior. Run the build, lint, unit and connected commands in README.md; scripts/verify_android_results.py must confirm the intended nonzero cases completed.

Provider adapters must declare their actual capabilities. Do not treat a catalog URL as permitted playable/downloadable audio, bundle copyrighted songs, embed credentials, or implement DRM/access-control bypasses. Distinguish mocked/contract tests, live integration and hardware/acoustic evidence. Do not call the full product complete while acceptance items remain unverified.
