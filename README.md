# FameLife — AI Fame & Livestream Simulator (Tebib Labs)

FameLife is a native Android (Kotlin + Jetpack Compose) simulated livestream studio where creators experience speaking to an intelligent, context-aware virtual audience across four distinct platform-inspired modes—without broadcasting to real platforms, real viewers, or real-money economies.

## Key Capabilities
- **Four Distinct Live Modes**:
  - **Photo Live** (Instagram-inspired): Full-bleed video, compact creator header, floating hearts column, join/badge events, Q&A drawer, and pinned comments.
  - **Short Live** (TikTok-inspired): Vertical full-screen stage, supporter rank pills, rapid badge-tagged comments, animated small gift banners, and skippable large gift overlays.
  - **Video Live** (YouTube-inspired): Adaptive portrait (stage + live chat panel) and landscape theater split, Super Chat ticker pills, highlighted Super Chat card, moderator delete/pin controls, and chat search.
  - **Gaming Live** (Twitch-inspired): Landscape-optimized screen-share stage, top virtual Bits leaderboard, Stream Summary card, and dense emote/badge chat.
- **Stochastic Audience & 100K+ Identity Engine**:
  - Lazy deterministic identity generation for `100,000+` fictional accounts (`IdentityPopulationEngine`).
  - Organic Poisson arrival/departure simulation (`StochasticAudienceEngine`) across 7 fame tiers (`New Creator` up to `Global Icon` `40,000–150,000` + `Custom Fame`) with silence decay, correlated noise, and viral spikes.
- **Context-Aware AI Audience Director (`Firebase AI Logic`)**:
  - Uses `gemini-3.8-flash` via `Firebase.ai` with App Check attestation and strict `Schema.obj(...)` structured JSON batches.
  - Combines real CameraX / `MediaProjection` sparse scene-change frame sampling and continuous `SpeechRecognizer` transcripts with deterministic local poll handlers (`"Put 1 in the chat"`), stale-context cancellation, moderation filters, and honest degraded/offline fallback.
- **Virtual Economy & Local Room Persistence**:
  - Eight custom vector/Canvas virtual gifts across 4 rarity tiers with cooldowns, idempotent `LedgerEntry` reconciliation, optional returning fan memory, session viewer growth charts, labeled stats export, and one-tap local data deletion.

## Directory Structure
- `app/src/main/java/com/example/ai/` — `AppCheckConfig.kt`, `AudienceDirector.kt`
- `app/src/main/java/com/example/domain/model/` — `StreamConfigModels.kt`, `AudienceModels.kt`
- `app/src/main/java/com/example/engine/` — `IdentityPopulationEngine.kt`, `StochasticAudienceEngine.kt`, `VirtualEconomyEngine.kt`
- `app/src/main/java/com/example/data/` — `FameLifeDatabase.kt` (Room), `FameLifeRepository.kt`, `SettingsDataStore.kt`
- `app/src/main/java/com/example/media/` — `CameraPreviewComposable.kt` (CameraX), `SpeechRecognitionAdapter.kt`, `ScreenCaptureService.kt` (MediaProjection)
- `app/src/main/java/com/example/ui/` — Theme, custom components, gift artwork, and all screens
- `app/src/test/java/com/example/` — Robolectric UI tests and engine acceptance tests

## Setup & Build
1. Firebase AI Logic & App Check are provisioned via `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` (no client-side `GEMINI_API_KEY` required).
2. Run unit & Robolectric acceptance tests:
   `gradle :app:testDebugUnitTest`
3. Build debug APK:
   `gradle :app:assembleDebug`
