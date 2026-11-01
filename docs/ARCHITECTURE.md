# Architecture decision — foundation

Native Kotlin, Jetpack Compose, and AndroidX Media3 are selected for Android-only development. Kotlin provides direct MediaStore, SAF, audio-focus, effects and foreground-service integration. Compose supports accessible custom disc drawing without a cross-framework bridge. Media3 will own one background MediaSessionService; presentation mode changes must never recreate its player.

Flutter offers portable drawing but requires Android plugins for these core workflows. React Native similarly adds a JavaScript/native lifecycle boundary. Neither portability benefit is needed here. Native Android gives the simplest media lifecycle and instrumentation path.

Planned boundaries: library indexing on IO dispatchers, persistent personal-library repositories, one playback service, controller-backed observable UI, capability-based provider adapters, LRC parsing and position-based highlighting, independently managed audio effects. Room and DataStore will be evaluated when persistence is implemented. No remote provider is enabled yet.

Minimum API 26; compile and target API 36. Tool versions were checked against official Google/Maven metadata. AGP 9.5.0 pins Kotlin 2.2.10, so the Compose compiler plugin matches it. Dependency compatibility must be established by an actual build.

The app name is msc, as selected by the user. org.discnest.app remains the configurable application identifier. applicationId is overrideable with -Pdiscnest.applicationId=your.package.

This repository currently contains only the Phase A foundation. It is not the requested complete player or a release candidate. Feature implementation follows the runtime foundation gate.

Compatibility finding: Compose BOM 2026.09.00 requires API 37, which the official SDK installer does not provide in this environment. BOM 2026.03.01 is selected for API 36 compatibility; it maps UI to 1.10.6.
