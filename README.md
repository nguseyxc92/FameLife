# FameLife — Livestream & Fame Simulator

**Developed by Tebib Labs**

FameLife is a native Android (Kotlin + Jetpack Compose) livestream and fame simulator where creators experience speaking to a responsive, context-aware virtual audience across four distinct platform-inspired modes—without broadcasting to real platforms, real viewers, or real-money economies.

## About Tebib Labs

Tebib Labs is an independent technology and software development company focused on artificial intelligence, computational engineering, scientific software and interactive digital experiences. We build innovative, intelligent and user-focused applications that connect engineering, technology and creativity.

FameLife is developed by Tebib Labs.

## Key Capabilities

- **Four Distinct Live Modes**:
  - **Photo Live**: Full-bleed video, compact creator header, floating hearts column, join/badge events, Q&A drawer, and pinned comments.
  - **Short Live**: Vertical full-screen stage, supporter rank pills, rapid badge-tagged comments, animated small gift banners, and skippable large gift overlays.
  - **Video Live**: Adaptive portrait (stage + live chat panel) and landscape theater split, Super Chat ticker pills, highlighted Super Chat card, moderator delete/pin controls, and chat search.
  - **Gaming Live**: Landscape-optimized screen-share stage, top virtual Bits leaderboard, Stream Summary card, and dense emote/badge chat.
- **Stochastic Audience & 32,900+ Identity Population Engine**:
  - Deterministic, collision-free natural username generation (`IdentityPopulationEngine`, including 5,000+ unique realistic names/handles in the first 5,000 viewers) with a 90% profile-picture / 10% initial-avatar distribution across live chat and rankings.
  - Organic Poisson arrival/departure simulation (`StochasticAudienceEngine`) across 7 fame tiers (`New Creator` up to `Global Icon` `40,000–150,000` + `Custom Fame`) with silence decay, correlated noise, and viral spikes.
- **Contextual Local Audience Engine**:
  - Zero-quota, offline-ready contextual NLP director (`LocalAlgorithmicDirectorClient`) that listens to creator speech (`SpeechRecognizer`) and sparse visual camera/screen scenes (CameraX / `MediaProjection`) to trigger topical viewer comments, polls (`"Put 1 in the chat"`), questions, and virtual gifts without showing distracting live speech captions on screen.
  - Studio notices route cleanly to the main-menu notification center without interrupting live broadcasts.
- **Creator Profile, Virtual Economy & Local Room Persistence**:
  - Local creator account with customizable name, handle, bio, and profile picture (`ActivityResultContracts.PickVisualMedia`).
  - Eight custom vector/Canvas virtual gifts across 4 rarity tiers, idempotent `LedgerEntry` reconciliation, returning fan memory, viewer growth charts, stats export, and one-tap local data deletion.

## Directory Structure

- `app/src/main/java/com/example/ai/` — `AudienceDirector.kt`, `LocalAudienceEngine.kt`, `AppCheckConfig.kt`
- `app/src/main/java/com/example/domain/model/` — `StreamConfigModels.kt`, `AudienceModels.kt`
- `app/src/main/java/com/example/engine/` — `IdentityPopulationEngine.kt`, `StochasticAudienceEngine.kt`, `VirtualEconomyEngine.kt`
- `app/src/main/java/com/example/data/` — `FameLifeDatabase.kt` (Room), `FameLifeRepository.kt`, `ProfilePhotoStorage.kt`, `SettingsDataStore.kt`
- `app/src/main/java/com/example/media/` — `CameraPreviewComposable.kt` (CameraX), `SpeechRecognitionAdapter.kt`, `ScreenCaptureService.kt` (MediaProjection)
- `app/src/main/java/com/example/ui/` — Theme, custom components, gift artwork, and all screens
- `app/src/test/java/com/example/` — Robolectric UI tests and engine acceptance tests

## Setup & Build

1. Run unit & Robolectric acceptance tests:
   `gradle :app:testDebugUnitTest`
2. Build debug APK:
   `gradle :app:assembleDebug`

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

© 2026 Tebib Labs. All rights reserved.
