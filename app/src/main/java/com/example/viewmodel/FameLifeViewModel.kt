package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AudienceDirector
import com.example.ai.ContextSnapshot
import com.example.ai.DirectedReaction
import com.example.ai.DirectorResult
import com.example.data.CreatorProfileEntity
import com.example.data.FameLifeDatabase
import com.example.data.FameLifeRepository
import com.example.data.LedgerEntryEntity
import com.example.data.ReturningFanEntity
import com.example.data.SettingsDataStore
import com.example.data.StreamSessionEntity
import com.example.data.UserSettings
import com.example.domain.model.AudienceLanguage
import com.example.domain.model.AudienceTone
import com.example.domain.model.ChatEvent
import com.example.domain.model.ChatEventType
import com.example.domain.model.CustomFameConfig
import com.example.domain.model.FamePreset
import com.example.domain.model.GiftEvent
import com.example.domain.model.GiftType
import com.example.domain.model.LivePlatformMode
import com.example.domain.model.StreamSourceMode
import com.example.domain.model.SupporterRank
import com.example.domain.model.ViewerArchetype
import com.example.domain.model.ViewerIdentity
import com.example.domain.model.ViewerTimePoint
import com.example.engine.GiftOverlayQueue
import com.example.engine.IdentityPopulationEngine
import com.example.engine.StochasticAudienceEngine
import com.example.engine.VirtualEconomyEngine
import com.example.media.LiveRecordingCoordinator
import com.example.media.ScreenCaptureBridge
import com.example.media.ScreenCaptureService
import com.example.media.SpeechEngineState
import com.example.media.SpeechRecognitionAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import kotlin.random.Random

private const val TAG = "FameLifeViewModel"

sealed interface AiStatus {
    data object Ready : AiStatus
    data object Thinking : AiStatus
    data class Active(val latencyMs: Long, val source: String) : AiStatus
    data class Degraded(val reason: String) : AiStatus
}

data class StudioNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class LiveSessionUiState(
    val isLive: Boolean = false,
    val isPaused: Boolean = false,
    val sessionId: String = "",
    val platformMode: LivePlatformMode = LivePlatformMode.SHORT_LIVE,
    val famePreset: FamePreset = FamePreset.RISING_STAR,
    val sourceMode: StreamSourceMode = StreamSourceMode.FRONT_CAMERA,
    val isMicMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isChatVisible: Boolean = true,
    val visualUnderstandingEnabled: Boolean = false,
    val elapsedSec: Int = 0,
    val currentViewers: Int = 0,
    val peakViewers: Int = 0,
    val avgViewers: Int = 0,
    val totalLikes: Int = 0,
    val newFollowers: Int = 0,
    val totalGiftsCount: Int = 0,
    val totalVirtualCoins: Int = 0,
    val uniqueParticipants: Int = 0,
    val totalMessagesCount: Int = 0,
    val engagementScore: Float = 0.5f,
    val viralBannerText: String? = null,
    val pinnedMessage: ChatEvent? = null,
    val latestSuperChat: ChatEvent? = null,
    val superChatTicker: List<ChatEvent> = emptyList(),
    val visibleChat: List<ChatEvent> = emptyList(),
    val searchableChatHistory: List<ChatEvent> = emptyList(),
    val topSupporters: List<SupporterRank> = emptyList(),
    val activeSmallGift: GiftEvent? = null,
    val activeLargeGift: GiftEvent? = null,
    val floatingHeartPulse: Long = 0L,
    val creatorPartialSpeech: String = "",
    val lastCreatorUtterance: String = "",
    val speechState: SpeechEngineState = SpeechEngineState.IDLE,
    val aiStatus: AiStatus = AiStatus.Ready,
    val viewerTimeline: List<ViewerTimePoint> = emptyList(),
    val apiCallsCount: Int = 0,
    val sessionReminderTriggered: Boolean = false,
    val recordLiveEnabled: Boolean = false
)

class FameLifeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FameLifeDatabase.getInstance(application)
    private val repository = FameLifeRepository(db)
    private val settingsStore = SettingsDataStore(application)
    private val populationEngine = IdentityPopulationEngine()
    private val audienceDirector = AudienceDirector()
    private val giftOverlayQueue = GiftOverlayQueue()

    val settings: StateFlow<UserSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val creatorProfile: StateFlow<CreatorProfileEntity?> = repository.creatorProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val streamHistory: StateFlow<List<StreamSessionEntity>> = repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ledgerEntries: StateFlow<List<LedgerEntryEntity>> = repository.ledgerEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val walletTotalCoins: StateFlow<Int> = repository.totalVirtualCoins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val returningFans: StateFlow<List<ReturningFanEntity>> = repository.returningFans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _liveState = MutableStateFlow(LiveSessionUiState())
    val liveState: StateFlow<LiveSessionUiState> = _liveState.asStateFlow()

    private val _lastCompletedSession = MutableStateFlow<StreamSessionEntity?>(null)
    val lastCompletedSession: StateFlow<StreamSessionEntity?> = _lastCompletedSession.asStateFlow()

    private val _preLiveSource = MutableStateFlow(StreamSourceMode.FRONT_CAMERA)
    val preLiveSource: StateFlow<StreamSourceMode> = _preLiveSource.asStateFlow()

    private val _studioNotifications = MutableStateFlow<List<StudioNotification>>(emptyList())
    val studioNotifications: StateFlow<List<StudioNotification>> = _studioNotifications.asStateFlow()

    private var audienceEngine: StochasticAudienceEngine? = null
    private var economyEngine: VirtualEconomyEngine? = null
    private var simulationJob: Job? = null
    private var scheduledReactionsJob: Job? = null
    private var speechAdapter: SpeechRecognitionAdapter? = null

    private var activeContextId: String = "ctx_0"
    private var contextSeq = 0
    private var chatSeq = 0L
    private var lastDonorUsername: String? = null
    private var lastDonorGiftName: String? = null
    private var lastCreatorSpeechAtMs = 0L
    private var latestVisualBitmap: Bitmap? = null
    private var lastVisualAnalyzedAtMs = 0L
    private var sessionStartedAtMs = 0L
    private val sessionRandom = Random(System.currentTimeMillis())
    private val sessionDonors = LinkedHashMap<String, ViewerIdentity>()
    private var lastDirectorTick = -10

    init {
        ScreenCaptureBridge.onFrameCaptured = { bmp ->
            onVisualFrameSampled(bmp)
        }
    }

    fun completeOnboarding(
        displayName: String,
        handle: String,
        bio: String,
        isGuest: Boolean,
        avatarUri: String? = null,
        isVerified: Boolean = false
    ) {
        viewModelScope.launch {
            repository.saveCreatorProfile(displayName, handle, bio, isGuest, avatarUri, isVerified)
            settingsStore.updateSettings { it.copy(onboardingCompleted = true) }
        }
    }

    fun updateCreatorProfile(
        displayName: String,
        handle: String,
        bio: String,
        avatarUri: String?,
        isVerified: Boolean = creatorProfile.value?.isVerified ?: false
    ) {
        viewModelScope.launch {
            repository.updateProfileDetails(
                existing = creatorProfile.value,
                displayName = displayName,
                handle = handle,
                bio = bio,
                avatarUri = avatarUri,
                isVerified = isVerified
            )
        }
    }

    fun dismissNotification(id: String) {
        _studioNotifications.update { list -> list.filterNot { it.id == id } }
    }

    fun clearAllNotifications() {
        _studioNotifications.value = emptyList()
    }

    private fun pushStudioNotification(title: String, message: String) {
        _studioNotifications.update { current ->
            if (current.any { it.title == title && it.message == message }) current
            else (listOf(StudioNotification(UUID.randomUUID().toString(), title, message)) + current).take(12)
        }
    }

    fun selectPlatform(mode: LivePlatformMode) {
        viewModelScope.launch {
            settingsStore.updateSettings { it.copy(selectedPlatform = mode) }
            if (mode == LivePlatformMode.GAMING_LIVE) {
                _preLiveSource.value = StreamSourceMode.SCREEN_SHARE
            } else if (_preLiveSource.value == StreamSourceMode.SCREEN_SHARE) {
                _preLiveSource.value = StreamSourceMode.FRONT_CAMERA
            }
        }
    }

    fun selectFamePreset(preset: FamePreset) {
        viewModelScope.launch {
            settingsStore.updateSettings { it.copy(selectedFamePreset = preset) }
        }
    }

    fun updateCustomFame(config: CustomFameConfig) {
        viewModelScope.launch {
            settingsStore.updateSettings {
                it.copy(selectedFamePreset = FamePreset.CUSTOM, customFameConfig = config)
            }
        }
    }

    fun updateLanguage(language: AudienceLanguage) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(language = language) } }
    }

    fun updateTone(tone: AudienceTone) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(tone = tone) } }
    }

    fun updateChatSpeed(speed: Float) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(chatSpeedLevel = speed) } }
    }

    fun updateGiftFrequency(freq: Float) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(giftFrequencyLevel = freq) } }
    }

    fun toggleVisualUnderstanding(enabled: Boolean) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(visualUnderstandingEnabled = enabled) } }
        _liveState.update { it.copy(visualUnderstandingEnabled = enabled) }
    }

    fun toggleFanMemory(enabled: Boolean) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(fanMemoryEnabled = enabled) } }
    }

    fun toggleReducedMotion(enabled: Boolean) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(reducedMotion = enabled) } }
    }

    fun toggleRecordLive(enabled: Boolean) {
        viewModelScope.launch { settingsStore.updateSettings { it.copy(recordLiveEnabled = enabled) } }
    }

    fun setPreLiveSource(source: StreamSourceMode) {
        _preLiveSource.value = source
    }

    fun startLiveStream(hasMicPermission: Boolean) {
        viewModelScope.launch {
            val currentSettings = settingsStore.settingsFlow.first()
            val sessionId = "ses_${UUID.randomUUID().toString().take(8)}"
            sessionStartedAtMs = System.currentTimeMillis()
            contextSeq = 0
            chatSeq = 0L
            lastDonorUsername = null
            lastDonorGiftName = null
            lastCreatorSpeechAtMs = System.currentTimeMillis()
            lastDirectorTick = -10
            sessionDonors.clear()
            giftOverlayQueue.clear()
            populationEngine.clearSessionTracking()

            val memories = if (currentSettings.fanMemoryEnabled) {
                repository.getReturningFanMemories()
            } else {
                emptyList()
            }
            populationEngine.setReturningMemories(memories)

            val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val returningCore = if (memories.isNotEmpty()) memories.size.coerceAtMost(8) else 0
            audienceEngine = StochasticAudienceEngine(
                preset = currentSettings.selectedFamePreset,
                customConfig = currentSettings.customFameConfig,
                returningCoreSize = returningCore,
                seed = System.nanoTime(),
                hourOfDay = hourOfDay
            )
            economyEngine = VirtualEconomyEngine(sessionId = sessionId, seed = System.nanoTime())

            val source = _preLiveSource.value
            val recordEnabled = currentSettings.recordLiveEnabled
            if (recordEnabled) {
                LiveRecordingCoordinator.beginSession(sessionId)
                if (source == StreamSourceMode.AUDIO_ONLY && hasMicPermission) {
                    LiveRecordingCoordinator.startAudioOnlyRecording(getApplication(), sessionId)
                }
            }

            _liveState.value = LiveSessionUiState(
                isLive = true,
                isPaused = false,
                sessionId = sessionId,
                platformMode = currentSettings.selectedPlatform,
                famePreset = currentSettings.selectedFamePreset,
                sourceMode = source,
                isMicMuted = !hasMicPermission,
                isCameraOff = source == StreamSourceMode.AUDIO_ONLY,
                visualUnderstandingEnabled = currentSettings.visualUnderstandingEnabled,
                currentViewers = returningCore,
                peakViewers = returningCore,
                aiStatus = AiStatus.Ready,
                recordLiveEnabled = recordEnabled
            )

            if (hasMicPermission) {
                startSpeechCapture(currentSettings.language)
            }

            // Emit initial returning fan greeting if memory exists, or welcome notice
            if (memories.isNotEmpty()) {
                val fan = memories.first()
                val viewer = populationEngine.getViewerByIndex(
                    fan.viewerId.removePrefix("v_").toIntOrNull() ?: 0,
                    memoryEnabled = true
                )
                val memoryNotice = if (!fan.lastTopic.isNullOrBlank()) {
                    "Back again! Remember last stream about \"${fan.lastTopic.take(36)}\"?"
                } else {
                    "Back for another live!!"
                }
                appendChatEvent(
                    viewer = viewer,
                    type = ChatEventType.JOIN,
                    text = memoryNotice,
                    contextId = activeContextId,
                    isReturningFan = true
                )
            }

            startSimulationLoop()
        }
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (_liveState.value.isLive) {
                delay(2_000L)
                val state = _liveState.value
                if (!state.isLive || state.isPaused) continue

                val engine = audienceEngine ?: continue
                val econ = economyEngine ?: continue
                val currentSettings = settings.value

                val now = System.currentTimeMillis()
                val isSilent = (now - lastCreatorSpeechAtMs) > 14_000L
                val tickSnap = engine.step(
                    tickSeconds = 2,
                    hadCreatorActivityThisTick = !isSilent
                )

                val timeline = (state.viewerTimeline + ViewerTimePoint(tickSnap.elapsedSec, tickSnap.activeViewers))
                    .takeLast(180)

                val viralText = when {
                    tickSnap.viralEventTriggered -> "Viral Surge! Viewers are sharing your live"
                    tickSnap.tickIndex % 9 == 0 -> null
                    else -> state.viralBannerText
                }

                // Sample potential gift if active viewers > 0
                val donorCandidates = populationEngine.sampleSpeakers(
                    activeViewers = tickSnap.activeViewers,
                    count = 5,
                    currentTick = tickSnap.tickIndex,
                    random = sessionRandom,
                    memoryEnabled = currentSettings.fanMemoryEnabled,
                    preferredLanguage = currentSettings.language
                )

                val giftEvent = econ.maybeGenerateGift(
                    tickIndex = tickSnap.tickIndex,
                    activeViewers = tickSnap.activeViewers,
                    engagementScore = tickSnap.engagementScore,
                    generosityFactor = engine.generosityFactor * (0.5f + currentSettings.giftFrequencyLevel),
                    isSilent = isSilent,
                    candidateDonors = donorCandidates,
                    timestampMs = now
                )

                if (giftEvent != null) {
                    handleNewGiftEvent(giftEvent, donorCandidates)
                }

                // Pop queued gift overlays cleanly
                val nextLarge = state.activeLargeGift ?: giftOverlayQueue.popNextLarge()
                val nextSmall = giftOverlayQueue.popNextSmall() ?: state.activeSmallGift

                // Trigger periodic ambient or visual reactions
                if (tickSnap.activeViewers > 0 && tickSnap.tickIndex - lastDirectorTick >= 4) {
                    val pendingBitmap = if (currentSettings.visualUnderstandingEnabled &&
                        latestVisualBitmap != null &&
                        (now - lastVisualAnalyzedAtMs) >= 9_000L
                    ) {
                        lastVisualAnalyzedAtMs = now
                        latestVisualBitmap
                    } else null

                    if (pendingBitmap != null || (isSilent && tickSnap.tickIndex % 6 == 0)) {
                        triggerAudienceDirector(
                            utterance = if (pendingBitmap != null) "[Visual scene update from creator camera/screen]" else null,
                            frameBitmap = pendingBitmap,
                            isSilent = isSilent && pendingBitmap == null
                        )
                    }
                }

                // Occasional join/follow micro-events for lively stream feel
                if (tickSnap.arrivals > 0 && donorCandidates.isNotEmpty() && sessionRandom.nextFloat() < 0.45f) {
                    val joiner = donorCandidates.first()
                    val joinMsg = when (state.platformMode) {
                        LivePlatformMode.VIDEO_LIVE -> if (tickSnap.newFollowers > 0) "Subscribed!" else "Joined the live chat"
                        LivePlatformMode.GAMING_LIVE -> if (tickSnap.viralEventTriggered) "Raid incoming! 🔥" else "Lurking & watching"
                        else -> "joined"
                    }
                    appendChatEvent(
                        viewer = joiner,
                        type = if (tickSnap.newFollowers > 0) ChatEventType.FOLLOW else ChatEventType.JOIN,
                        text = joinMsg,
                        contextId = activeContextId
                    )
                }

                val reminderHit = currentSettings.sessionReminderMinutes > 0 &&
                    tickSnap.elapsedSec >= currentSettings.sessionReminderMinutes * 60

                _liveState.update {
                    it.copy(
                        elapsedSec = tickSnap.elapsedSec,
                        currentViewers = tickSnap.activeViewers,
                        peakViewers = tickSnap.peakViewers,
                        avgViewers = engine.getAverageViewers(),
                        totalLikes = it.totalLikes + tickSnap.newLikes,
                        newFollowers = it.newFollowers + tickSnap.newFollowers,
                        totalGiftsCount = econ.totalGiftsCount,
                        totalVirtualCoins = econ.reconcileTotalCoins(),
                        uniqueParticipants = populationEngine.getDistinctParticipantCount(),
                        totalMessagesCount = populationEngine.getUniqueEventCount().toInt(),
                        engagementScore = tickSnap.engagementScore,
                        viralBannerText = viralText,
                        topSupporters = econ.getTopSupporters(5),
                        activeSmallGift = nextSmall,
                        activeLargeGift = nextLarge,
                        floatingHeartPulse = if (tickSnap.newLikes > 0) now else it.floatingHeartPulse,
                        viewerTimeline = timeline,
                        apiCallsCount = audienceDirector.apiRequestCount,
                        sessionReminderTriggered = reminderHit
                    )
                }
            }
        }
    }

    fun submitCreatorUtterance(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty() || !_liveState.value.isLive) return

        lastCreatorSpeechAtMs = System.currentTimeMillis()
        audienceEngine?.notifyCreatorActivity(0.60f)

        // Topic change cancels outdated queued reactions
        activeContextId = "ctx_${++contextSeq}"
        scheduledReactionsJob?.cancel()

        _liveState.update {
            it.copy(
                lastCreatorUtterance = cleaned,
                creatorPartialSpeech = ""
            )
        }

        val frame = if (_liveState.value.visualUnderstandingEnabled) latestVisualBitmap else null
        triggerAudienceDirector(utterance = cleaned, frameBitmap = frame, isSilent = false)
    }

    private fun triggerAudienceDirector(
        utterance: String?,
        frameBitmap: Bitmap?,
        isSilent: Boolean
    ) {
        val state = _liveState.value
        val engine = audienceEngine ?: return
        val currentSettings = settings.value
        lastDirectorTick = engine.currentTick

        val speakerCount = (3 + (currentSettings.chatSpeedLevel * 4).toInt()).coerceIn(3, 7)
        val speakers = populationEngine.sampleSpeakers(
            activeViewers = engine.activeViewers.coerceAtLeast(1),
            count = speakerCount,
            currentTick = engine.currentTick,
            random = sessionRandom,
            memoryEnabled = currentSettings.fanMemoryEnabled,
            preferredLanguage = currentSettings.language
        )

        val donorThanks = utterance?.lowercase()?.let {
            it.contains("thank") || it.contains("appreciate") || it.contains("shoutout") || it.contains("አመሰግናለሁ")
        } == true

        val ctx = ContextSnapshot(
            contextId = activeContextId,
            sessionId = state.sessionId,
            platformMode = state.platformMode,
            language = currentSettings.language,
            tone = currentSettings.tone,
            creatorTranscript = utterance,
            recentChatHistory = state.visibleChat.takeLast(6).map { "${it.username}: ${it.text}" },
            lastDonorUsername = if (donorThanks) lastDonorUsername else null,
            lastDonorGiftName = if (donorThanks) lastDonorGiftName else null,
            isSilent = isSilent
        )

        viewModelScope.launch {
            _liveState.update { it.copy(aiStatus = AiStatus.Thinking) }
            val result = withContext(Dispatchers.IO) {
                audienceDirector.directAudience(
                    context = ctx,
                    speakers = speakers,
                    frameBitmap = frameBitmap,
                    isOnline = isOnline()
                )
            }

            when (result) {
                is DirectorResult.Success -> {
                    _liveState.update {
                        it.copy(aiStatus = AiStatus.Active(result.latencyMs, result.source))
                    }
                    scheduleDirectedReactions(result.reactions, speakers, ctx.contextId)
                }
                is DirectorResult.Degraded -> {
                    // Never show live error banners; log quietly to main-menu notifications if needed
                    if (result.reason.isNotBlank() && !result.reason.contains("Waiting for audience", ignoreCase = true)) {
                        pushStudioNotification("Audience Engine Notice", result.reason)
                    }
                    _liveState.update {
                        it.copy(aiStatus = AiStatus.Ready)
                    }
                    scheduleDirectedReactions(result.ambientReactions, speakers, ctx.contextId)
                }
            }
        }
    }

    private fun scheduleDirectedReactions(
        reactions: List<DirectedReaction>,
        speakers: List<ViewerIdentity>,
        expectedContextId: String
    ) {
        scheduledReactionsJob?.cancel()
        scheduledReactionsJob = viewModelScope.launch {
            val sorted = reactions.sortedBy { it.delayMs }
            var prevDelay = 0L
            for (reaction in sorted) {
                val wait = (reaction.delayMs - prevDelay).coerceAtLeast(180L)
                prevDelay = reaction.delayMs
                delay(wait)

                if (!_liveState.value.isLive || _liveState.value.isPaused) break
                if (activeContextId != expectedContextId) break

                val speaker = speakers.getOrNull(reaction.viewerSlot) ?: continue
                appendChatEvent(
                    viewer = speaker,
                    type = reaction.type,
                    text = reaction.text,
                    contextId = expectedContextId,
                    isReturningFan = !speaker.rememberedTopic.isNullOrBlank()
                )

                // Optional AI-triggered small gift
                val gift = GiftType.fromId(reaction.optionalGiftId)
                if (gift != null && !gift.isFullScreenEvent) {
                    val econ = economyEngine
                    if (econ != null && _liveState.value.currentViewers > 0) {
                        val giftEvt = GiftEvent(
                            eventId = "aigift_${reaction.eventId}",
                            sessionId = _liveState.value.sessionId,
                            viewerId = speaker.viewerId,
                            username = speaker.username,
                            initials = speaker.initials,
                            avatarSeed = speaker.avatarSeed,
                            gift = gift,
                            count = 1,
                            timestampMs = System.currentTimeMillis(),
                            hasProfilePicture = speaker.hasProfilePicture,
                            avatarUri = speaker.avatarUri
                        )
                        if (econ.recordGiftEvent(giftEvt)) {
                            handleNewGiftEvent(giftEvt, speakers)
                        }
                    }
                }
            }
        }
    }

    private fun handleNewGiftEvent(event: GiftEvent, nearbyViewers: List<ViewerIdentity>) {
        lastDonorUsername = event.username
        lastDonorGiftName = event.gift.displayName
        giftOverlayQueue.enqueue(event)

        val donorIdentity = nearbyViewers.firstOrNull { it.viewerId == event.viewerId }
        if (donorIdentity != null) {
            sessionDonors[donorIdentity.viewerId] = donorIdentity.copy(
                totalGiftedCoins = donorIdentity.totalGiftedCoins + event.totalVirtualCoins
            )
        }

        val mode = _liveState.value.platformMode
        val chatType = when {
            mode == LivePlatformMode.VIDEO_LIVE && event.totalVirtualCoins >= 100 -> ChatEventType.SUPER_CHAT
            mode == LivePlatformMode.GAMING_LIVE -> ChatEventType.BITS
            else -> ChatEventType.GIFT_REACTION
        }

        val msgText = when (chatType) {
            ChatEventType.SUPER_CHAT -> "Super Chat (${event.totalVirtualCoins} virtual coins): Keep going!"
            ChatEventType.BITS -> "Cheered ${event.totalVirtualCoins} virtual Bits • ${event.gift.displayName}"
            else -> "Sent ${event.gift.displayName} x${event.count} (${event.totalVirtualCoins} coins)"
        }

        val chatEvent = ChatEvent(
            eventId = "chat_${event.eventId}",
            sequence = ++chatSeq,
            timestampMs = event.timestampMs,
            sessionId = event.sessionId,
            viewerId = event.viewerId,
            username = event.username,
            initials = event.initials,
            avatarSeed = event.avatarSeed,
            archetype = ViewerArchetype.SUPPORTER,
            type = chatType,
            text = msgText,
            contextId = activeContextId,
            giftId = event.gift.id,
            virtualCoins = event.totalVirtualCoins,
            hasProfilePicture = event.hasProfilePicture,
            avatarUri = event.avatarUri
        )

        populationEngine.recordParticipation(event.viewerId)

        _liveState.update { state ->
            val updatedVisible = (state.visibleChat + chatEvent).takeLast(150)
            val updatedHistory = (state.searchableChatHistory + chatEvent).takeLast(1500)
            val updatedTicker = if (chatType == ChatEventType.SUPER_CHAT) {
                (listOf(chatEvent) + state.superChatTicker).take(6)
            } else state.superChatTicker
            state.copy(
                visibleChat = updatedVisible,
                searchableChatHistory = updatedHistory,
                latestSuperChat = if (chatType == ChatEventType.SUPER_CHAT) chatEvent else state.latestSuperChat,
                superChatTicker = updatedTicker
            )
        }

        // Large gifts trigger natural audience awe from 1-2 nearby viewers
        if (event.gift.isFullScreenEvent && nearbyViewers.size >= 2) {
            val reactor = nearbyViewers.firstOrNull { it.viewerId != event.viewerId }
            if (reactor != null) {
                appendChatEvent(
                    viewer = reactor,
                    type = ChatEventType.CHAT,
                    text = "Whoa ${event.username} just dropped a ${event.gift.displayName}!! 🔥",
                    contextId = activeContextId
                )
            }
        }
    }

    private fun appendChatEvent(
        viewer: ViewerIdentity,
        type: ChatEventType,
        text: String,
        contextId: String,
        isReturningFan: Boolean = false
    ) {
        populationEngine.recordParticipation(viewer.viewerId)
        val event = ChatEvent(
            eventId = "msg_${_liveState.value.sessionId}_${++chatSeq}",
            sequence = chatSeq,
            timestampMs = System.currentTimeMillis(),
            sessionId = _liveState.value.sessionId,
            viewerId = viewer.viewerId,
            username = viewer.username,
            initials = viewer.initials,
            avatarSeed = viewer.avatarSeed,
            archetype = viewer.archetype,
            type = type,
            text = text,
            contextId = contextId,
            isReturningFan = isReturningFan,
            hasProfilePicture = viewer.hasProfilePicture,
            avatarUri = viewer.avatarUri
        )
        _liveState.update { state ->
            state.copy(
                visibleChat = (state.visibleChat + event).takeLast(150),
                searchableChatHistory = (state.searchableChatHistory + event).takeLast(1500)
            )
        }
    }

    fun onVisualFrameSampled(bitmap: Bitmap) {
        latestVisualBitmap = bitmap
        audienceEngine?.notifyCreatorActivity(0.18f)
    }

    fun dismissLargeGift() {
        val next = giftOverlayQueue.popNextLarge()
        _liveState.update { it.copy(activeLargeGift = next) }
    }

    fun dismissSmallGift() {
        val next = giftOverlayQueue.popNextSmall()
        _liveState.update { it.copy(activeSmallGift = next) }
    }

    fun togglePinMessage(event: ChatEvent) {
        _liveState.update { state ->
            val nextPinned = if (state.pinnedMessage?.eventId == event.eventId) null else event.copy(isPinned = true)
            state.copy(pinnedMessage = nextPinned)
        }
    }

    fun moderateRemoveMessage(eventId: String) {
        _liveState.update { state ->
            state.copy(
                visibleChat = state.visibleChat.filterNot { it.eventId == eventId },
                pinnedMessage = if (state.pinnedMessage?.eventId == eventId) null else state.pinnedMessage
            )
        }
    }

    fun triggerFloatingHeart() {
        _liveState.update {
            it.copy(
                totalLikes = it.totalLikes + 1,
                floatingHeartPulse = System.currentTimeMillis()
            )
        }
    }

    fun toggleMicMute(hasMicPermission: Boolean) {
        val newMuted = !_liveState.value.isMicMuted
        _liveState.update { it.copy(isMicMuted = newMuted) }
        if (newMuted || !hasMicPermission) {
            speechAdapter?.stopListening(isMuted = true)
        } else {
            startSpeechCapture(settings.value.language)
        }
    }

    fun toggleCameraOff() {
        _liveState.update { it.copy(isCameraOff = !it.isCameraOff) }
    }

    fun switchCameraFacing() {
        _liveState.update { state ->
            val nextSource = when (state.sourceMode) {
                StreamSourceMode.FRONT_CAMERA -> StreamSourceMode.REAR_CAMERA
                StreamSourceMode.REAR_CAMERA -> StreamSourceMode.FRONT_CAMERA
                else -> StreamSourceMode.FRONT_CAMERA
            }
            state.copy(sourceMode = nextSource, isCameraOff = false)
        }
    }

    fun switchSourceMode(mode: StreamSourceMode) {
        if (mode != StreamSourceMode.SCREEN_SHARE) {
            ScreenCaptureService.stop(getApplication())
        }
        _liveState.update {
            it.copy(
                sourceMode = mode,
                isCameraOff = mode == StreamSourceMode.AUDIO_ONLY
            )
        }
    }

    fun toggleChatVisibility() {
        _liveState.update { it.copy(isChatVisible = !it.isChatVisible) }
    }

    fun togglePauseLive() {
        val nextPaused = !_liveState.value.isPaused
        _liveState.update { it.copy(isPaused = nextPaused) }
        if (nextPaused) {
            speechAdapter?.stopListening(isMuted = _liveState.value.isMicMuted)
        } else if (!_liveState.value.isMicMuted) {
            startSpeechCapture(settings.value.language)
        }
    }

    fun endLiveStream(onCompleted: () -> Unit) {
        simulationJob?.cancel()
        scheduledReactionsJob?.cancel()
        speechAdapter?.release()
        speechAdapter = null
        ScreenCaptureService.stop(getApplication())
        latestVisualBitmap = null

        val state = _liveState.value
        val econ = economyEngine
        val ledger = econ?.getLedgerEntries().orEmpty()
        val totalCoins = econ?.reconcileTotalCoins() ?: state.totalVirtualCoins
        val retention = (state.engagementScore * 100).toInt().coerceIn(12, 99)

        val fansToRemember = if (settings.value.fanMemoryEnabled) {
            val topDonors = sessionDonors.values.take(6)
            val speakers = state.searchableChatHistory
                .distinctBy { it.viewerId }
                .takeLast(6)
                .map {
                    ReturningFanEntity(
                        viewerId = it.viewerId,
                        username = it.username,
                        archetypeName = it.archetype.name,
                        loyaltyScore = 0.72f,
                        totalGiftsCoins = sessionDonors[it.viewerId]?.totalGiftedCoins ?: 0,
                        lastTopic = state.lastCreatorUtterance.take(60).ifBlank { null },
                        joinCount = 2,
                        lastSeenAtMs = System.currentTimeMillis()
                    )
                }
            (speakers + topDonors.map { d ->
                ReturningFanEntity(
                    viewerId = d.viewerId,
                    username = d.username,
                    archetypeName = d.archetype.name,
                    loyaltyScore = 0.9f,
                    totalGiftsCoins = d.totalGiftedCoins,
                    lastTopic = state.lastCreatorUtterance.take(60).ifBlank { null },
                    joinCount = d.joinCount + 1,
                    lastSeenAtMs = System.currentTimeMillis()
                )
            }).distinctBy { it.viewerId }
        } else emptyList()

        viewModelScope.launch {
            val savedRecordingPath = if (state.recordLiveEnabled) {
                withContext(Dispatchers.IO) {
                    LiveRecordingCoordinator.stopAndAwaitFinalPath()
                }
            } else null

            val sessionEntity = StreamSessionEntity(
                sessionId = state.sessionId,
                platformModeId = state.platformMode.id,
                famePresetId = state.famePreset.id,
                startedAtMs = sessionStartedAtMs,
                durationSec = state.elapsedSec.coerceAtLeast(1),
                peakViewers = state.peakViewers,
                avgViewers = state.avgViewers,
                uniqueParticipants = state.uniqueParticipants,
                totalMessages = state.totalMessagesCount,
                newFollowers = state.newFollowers,
                totalLikes = state.totalLikes,
                totalGifts = state.totalGiftsCount,
                totalVirtualCoins = totalCoins,
                retentionPercent = retention,
                timelineJson = repository.serializeTimeline(state.viewerTimeline),
                recordingPath = savedRecordingPath
            )

            repository.persistCompletedSession(sessionEntity, ledger, fansToRemember)
            creatorProfile.value?.let { current ->
                repository.updateProfileEntity(
                    current.copy(
                        virtualFollowers = current.virtualFollowers + state.newFollowers,
                        totalStreamsCount = current.totalStreamsCount + 1
                    )
                )
            }
            _lastCompletedSession.value = sessionEntity
            _liveState.value = LiveSessionUiState(isLive = false)
            onCompleted()
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val recordingPath = streamHistory.value.firstOrNull { it.sessionId == sessionId }?.recordingPath
            withContext(Dispatchers.IO) {
                LiveRecordingCoordinator.deleteRecordingFile(recordingPath)
            }
            repository.deleteSession(sessionId)
        }
    }

    fun deleteReturningFan(viewerId: String) {
        viewModelScope.launch { repository.deleteReturningFan(viewerId) }
    }

    fun deleteAllDataAndReset() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                LiveRecordingCoordinator.deleteAllRecordings(getApplication())
            }
            repository.deleteAllLocalData()
            settingsStore.resetAllPreferences()
        }
    }

    fun parseTimeline(json: String): List<ViewerTimePoint> = repository.parseTimeline(json)

    fun buildExportReport(session: StreamSessionEntity): String =
        repository.buildExportSummaryText(session)

    private fun startSpeechCapture(language: AudienceLanguage) {
        val tag = when (language) {
            AudienceLanguage.AMHARIC -> "am-ET"
            else -> "en-US"
        }
        if (speechAdapter == null) {
            speechAdapter = SpeechRecognitionAdapter(
                context = getApplication(),
                onPartialTranscript = { partial ->
                    _liveState.update { it.copy(creatorPartialSpeech = partial) }
                },
                onFinalTranscript = { finalText ->
                    submitCreatorUtterance(finalText)
                },
                onStateChanged = { st ->
                    _liveState.update { it.copy(speechState = st) }
                    if (st == SpeechEngineState.UNAVAILABLE) {
                        pushStudioNotification(
                            title = "Voice Input Notice",
                            message = "On-device speech recognition was unavailable during your live stream. Typed prompts and quick phrases work anytime."
                        )
                    }
                }
            )
        }
        speechAdapter?.startListening(tag)
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.w(TAG, "Connectivity check failed", e)
            true
        }
    }

    override fun onCleared() {
        simulationJob?.cancel()
        scheduledReactionsJob?.cancel()
        speechAdapter?.release()
        ScreenCaptureService.stop(getApplication())
        super.onCleared()
    }
}
