package com.example

import com.example.ai.AudienceAiClient
import com.example.ai.AudienceDirector
import com.example.ai.ContextSnapshot
import com.example.ai.DirectorResult
import com.example.domain.model.AudienceLanguage
import com.example.domain.model.AudienceTone
import com.example.domain.model.CustomFameConfig
import com.example.domain.model.FamePreset
import com.example.domain.model.GiftEvent
import com.example.domain.model.GiftType
import com.example.domain.model.LivePlatformMode
import com.example.domain.model.ViewerArchetype
import com.example.engine.GiftOverlayQueue
import com.example.engine.IdentityPopulationEngine
import com.example.engine.ReturningFanMemory
import com.example.engine.StochasticAudienceEngine
import com.example.engine.VirtualEconomyEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FameLifeEngineAcceptanceTest {

    @Test
    fun `population engine generates non-negative gradual growth across all presets and supports 150k viewers`() {
        for (preset in FamePreset.entries) {
            val engine = StochasticAudienceEngine(
                preset = preset,
                customConfig = CustomFameConfig(targetAudience = 150_000, growthSpeed = 0.9f, engagement = 0.9f),
                returningCoreSize = 0,
                seed = 42L + preset.ordinal
            )
            // Must start at 0, not instantly at target
            assertEquals(0, engine.activeViewers)

            var maxSeen = 0
            repeat(60) {
                val snap = engine.step(tickSeconds = 2, hadCreatorActivityThisTick = true)
                assertTrue("Viewer count must remain non-negative", snap.activeViewers >= 0)
                if (snap.activeViewers > maxSeen) maxSeen = snap.activeViewers
            }
            assertTrue("Preset ${preset.title} should grow organically", maxSeen > 0)
        }

        // Stress test 150,000 target viewers over 300 ticks with zero AI calls
        val megaEngine = StochasticAudienceEngine(
            preset = FamePreset.CUSTOM,
            customConfig = CustomFameConfig(targetAudience = 150_000, growthSpeed = 1.0f, engagement = 1.0f),
            seed = 777L
        )
        repeat(300) {
            val snap = megaEngine.step(tickSeconds = 2, hadCreatorActivityThisTick = true)
            assertTrue(snap.activeViewers >= 0)
            assertTrue(snap.activeViewers <= 400_000)
        }
        assertTrue("Mega stream should reach tens of thousands", megaEngine.peakViewers > 40_000)
    }

    @Test
    fun `silence causes audience decline and trajectories vary by seed`() {
        val activeEngine = StochasticAudienceEngine(FamePreset.INFLUENCER, seed = 100L)
        repeat(45) { activeEngine.step(2, hadCreatorActivityThisTick = true) }
        val peakBeforeSilence = activeEngine.activeViewers
        assertTrue(peakBeforeSilence > 50)

        repeat(60) { activeEngine.step(2, hadCreatorActivityThisTick = false) }
        assertTrue(
            "Prolonged silence should reduce active viewers (before=$peakBeforeSilence, after=${activeEngine.activeViewers})",
            activeEngine.activeViewers < peakBeforeSilence
        )

        val runA = StochasticAudienceEngine(FamePreset.RISING_STAR, seed = 111L)
        val runB = StochasticAudienceEngine(FamePreset.RISING_STAR, seed = 222L)
        val trajA = (1..25).map { runA.step(2, true).activeViewers }
        val trajB = (1..25).map { runB.step(2, true).activeViewers }
        assertNotEquals("Different seeds must produce varied trajectories", trajA, trajB)
    }

    @Test
    fun `identity population produces 100k unique accounts and tracks 40k distinct participants and events`() {
        val pop = IdentityPopulationEngine(populationSize = 100_000)
        val ids = HashSet<String>(100_000)
        val usernames = HashSet<String>(100_000)

        for (i in 0 until 100_000) {
            val v = pop.getViewerByIndex(i, memoryEnabled = false)
            ids.add(v.viewerId)
            usernames.add(v.username)
        }
        assertEquals(100_000, ids.size)
        assertTrue("Expected vast majority unique usernames, got ${usernames.size}", usernames.size > 95_000)

        // Returning persona stability
        val fanMemory = ReturningFanMemory(
            viewerId = "v_000042",
            username = "selam_loyal",
            archetype = ViewerArchetype.LOYAL_FAN,
            loyaltyScore = 0.95f,
            totalGiftsCoins = 500,
            lastTopic = "Addis coffee walk",
            joinCount = 4
        )
        pop.setReturningMemories(listOf(fanMemory))
        val remembered = pop.getViewerByIndex(42, memoryEnabled = true)
        assertEquals("selam_loyal", remembered.username)
        assertEquals("Addis coffee walk", remembered.rememberedTopic)

        // Support >40,000 distinct chat participants & >40,000 unique message events
        for (i in 0 until 42_000) {
            pop.recordParticipation(pop.formatViewerId(i))
        }
        assertEquals(42_000, pop.getDistinctParticipantCount())
        assertEquals(42_000L, pop.getUniqueEventCount())
    }

    @Test
    fun `economy engine enforces cooldowns, idempotent ledger reconciliation, and overlay queue caps`() {
        val econ = VirtualEconomyEngine(sessionId = "ses_test", seed = 99L)
        val pop = IdentityPopulationEngine()
        val rng = Random(99L)

        var largeGifts = 0
        for (tick in 1..120) {
            val donors = pop.sampleSpeakers(10_000, 5, tick, rng, memoryEnabled = false, AudienceLanguage.ENGLISH)
            val evt = econ.maybeGenerateGift(
                tickIndex = tick,
                activeViewers = 10_000,
                engagementScore = 0.9f,
                generosityFactor = 1.0f,
                isSilent = false,
                candidateDonors = donors,
                timestampMs = tick * 2000L
            )
            if (evt != null && evt.gift.isFullScreenEvent) largeGifts++
        }

        // Cooldown prevents 100 large gifts per minute
        assertTrue("Large gifts must stay bounded by cooldowns, got $largeGifts", largeGifts <= 14)

        // Idempotent recording
        val duplicateEvent = GiftEvent(
            eventId = "fixed_evt_1",
            sessionId = "ses_test",
            viewerId = "v_000001",
            username = "tester",
            initials = "TE",
            avatarSeed = 1,
            gift = GiftType.CROWN,
            count = 1,
            timestampMs = 5000L
        )
        assertTrue(econ.recordGiftEvent(duplicateEvent))
        assertFalse("Duplicate eventId must be rejected idempotently", econ.recordGiftEvent(duplicateEvent))

        val sumFromEntries = econ.getLedgerEntries().sumOf { it.virtualCoins }
        assertEquals(sumFromEntries, econ.reconcileTotalCoins())

        // Queue coalescing & large cap
        val queue = GiftOverlayQueue()
        val small1 = duplicateEvent.copy(eventId = "s1", gift = GiftType.FLOWER, count = 1)
        val small2 = duplicateEvent.copy(eventId = "s2", gift = GiftType.FLOWER, count = 2)
        queue.enqueue(small1)
        queue.enqueue(small2)
        assertEquals(1, queue.smallQueueSize())
        assertEquals(3, queue.popNextSmall()?.count)
    }

    @Test
    fun `audience director handles poll waves, stale context rejection, safety filter, and offline degraded mode`() = runTest {
        val pop = IdentityPopulationEngine()
        val speakers = (0..4).map { pop.getViewerByIndex(it, memoryEnabled = false) }

        val mockAi = object : AudienceAiClient {
            var nextResponse: String = ""
            override suspend fun generateBatchJson(prompt: String, frameBitmap: android.graphics.Bitmap?): String = nextResponse
        }
        val director = AudienceDirector(aiClient = mockAi, random = Random(123L))

        // 1. Deterministic "Put 1 in the chat"
        val pollCtx = ContextSnapshot(
            contextId = "ctx_1",
            sessionId = "s1",
            platformMode = LivePlatformMode.SHORT_LIVE,
            language = AudienceLanguage.ENGLISH,
            tone = AudienceTone.BALANCED,
            creatorTranscript = "Put 1 in the chat",
            recentChatHistory = emptyList(),
            lastDonorUsername = null,
            lastDonorGiftName = null
        )
        val pollRes = director.directAudience(pollCtx, speakers, null, isOnline = true) as DirectorResult.Success
        assertEquals("LOCAL_INTENT", pollRes.source)
        assertTrue(pollRes.reactions.any { it.text.contains("1") })

        // 2. Stale contextId rejection + unsafe text filter + valid reactions
        mockAi.nextResponse = """
            {
              "reactions": [
                {"viewerSlot": 0, "type": "chat", "text": "OPEN IT right now!!", "delayMs": 600, "contextId": "ctx_2"},
                {"viewerSlot": 1, "type": "chat", "text": "Stale reaction", "delayMs": 800, "contextId": "ctx_OLD"},
                {"viewerSlot": 2, "type": "chat", "text": "kill yourself", "delayMs": 900, "contextId": "ctx_2"},
                {"viewerSlot": 99, "type": "chat", "text": "Bad slot", "delayMs": 1000, "contextId": "ctx_2"}
              ]
            }
        """.trimIndent()
        val doorCtx = pollCtx.copy(contextId = "ctx_2", creatorTranscript = "Should I open this door?")
        val doorRes = director.directAudience(doorCtx, speakers, null, isOnline = true) as DirectorResult.Success
        assertEquals(1, doorRes.reactions.size)
        assertEquals("OPEN IT right now!!", doorRes.reactions.first().text)

        // 3. Offline degraded mode honesty
        val offlineRes = director.directAudience(doorCtx, speakers, null, isOnline = false)
        assertTrue(offlineRes is DirectorResult.Degraded)
        val degraded = offlineRes as DirectorResult.Degraded
        assertTrue(degraded.reason.contains("Offline", ignoreCase = true))

        // 4. Malformed JSON graceful fallback
        mockAi.nextResponse = "{not valid json"
        val malformedRes = director.directAudience(doorCtx, speakers, null, isOnline = true)
        assertTrue(malformedRes is DirectorResult.Degraded)

        // 5. Default local algorithmic director works offline & online with zero quota/API calls
        val localDirector = AudienceDirector(random = Random(77L))
        val localOffline = localDirector.directAudience(doorCtx, speakers, null, isOnline = false)
        assertTrue(localOffline is DirectorResult.Success)
        val localSuccess = localOffline as DirectorResult.Success
        assertEquals("LOCAL_ENGINE", localSuccess.source)
        assertTrue(localSuccess.reactions.size >= 3)
        assertEquals(0, localDirector.apiRequestCount)
    }

    @Test
    fun `first 5000 usernames are realistic and unique with ~90 percent profile pictures`() {
        val pop = IdentityPopulationEngine(populationSize = 100_000)
        val first5k = HashSet<String>(5_000)
        var withPfp = 0
        val sampleSize = 10_000

        for (i in 0 until sampleSize) {
            val v = pop.getViewerByIndex(i, memoryEnabled = false)
            if (i < 5_000) {
                first5k.add(v.username)
            }
            if (v.hasProfilePicture) withPfp++
        }

        assertTrue("Expected >= 4990 unique usernames in first 5000, got ${first5k.size}", first5k.size >= 4_990)
        val pfpRatio = withPfp.toFloat() / sampleSize.toFloat()
        assertTrue("Expected ~90% profile pictures (0.87..0.93), got $pfpRatio", pfpRatio in 0.87f..0.93f)
    }
}
