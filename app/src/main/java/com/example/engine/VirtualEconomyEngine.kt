package com.example.engine

import com.example.domain.model.GiftEvent
import com.example.domain.model.GiftTier
import com.example.domain.model.GiftType
import com.example.domain.model.LedgerEntry
import com.example.domain.model.SupporterRank
import com.example.domain.model.ViewerIdentity
import kotlin.random.Random

/**
 * Virtual economy & gift scheduling engine.
 * - Stochastic hazard based on active audience, engagement, generosity, and cooldowns.
 * - Enforces per-donor and global cooldowns, suppresses gifts during prolonged silence.
 * - Maintains an idempotent Ledger keyed by eventId with exact reconciliation.
 */
class VirtualEconomyEngine(
    private val sessionId: String,
    seed: Long = System.nanoTime()
) {
    private val random = Random(seed)
    private val ledger = LinkedHashMap<String, LedgerEntry>()
    private val donorLastGiftTick = HashMap<String, Int>()
    private val donorTotals = HashMap<String, SupporterRank>()

    private var lastGlobalLargeGiftTick = -100
    private var lastGlobalSmallGiftTick = -100
    private var eventSeq = 0L

    var totalGiftsCount: Int = 0
        private set

    fun maybeGenerateGift(
        tickIndex: Int,
        activeViewers: Int,
        engagementScore: Float,
        generosityFactor: Float,
        isSilent: Boolean,
        candidateDonors: List<ViewerIdentity>,
        timestampMs: Long = System.currentTimeMillis()
    ): GiftEvent? {
        if (activeViewers <= 0 || candidateDonors.isEmpty()) return null

        // Avoid constant gifts during silence
        if (isSilent && random.nextFloat() > 0.08f) return null

        // Global rate guard: prevent gift spam (e.g., never 100 large gifts/minute)
        val ticksSinceSmall = tickIndex - lastGlobalSmallGiftTick
        if (ticksSinceSmall < 2) return null

        val audienceScale = kotlin.math.ln(1.0 + activeViewers.toDouble()).toFloat() / 12f
        val hazard = (0.04f + 0.22f * engagementScore * generosityFactor * audienceScale.coerceIn(0.15f, 1.1f))
            .coerceIn(0.01f, 0.36f)

        if (random.nextFloat() > hazard) return null

        val donor = candidateDonors
            .filter { (tickIndex - (donorLastGiftTick[it.viewerId] ?: -999)) >= 8 }
            .maxByOrNull { it.generosity * (0.5f + random.nextFloat()) }
            ?: return null

        val ticksSinceLarge = tickIndex - lastGlobalLargeGiftTick
        val canSendLarge = ticksSinceLarge >= 10 && engagementScore >= 0.45f

        val giftType = selectGiftType(
            donorGenerosity = donor.generosity,
            sessionGenerosity = generosityFactor,
            allowLarge = canSendLarge
        )

        val count = if (!giftType.isFullScreenEvent && random.nextFloat() < 0.25f) {
            1 + random.nextInt(3) // e.g., x2 or x3 Rose/Heart combo
        } else 1

        val eventId = "gift_${sessionId}_${++eventSeq}_${donor.viewerId}"
        val giftEvent = GiftEvent(
            eventId = eventId,
            sessionId = sessionId,
            viewerId = donor.viewerId,
            username = donor.username,
            initials = donor.initials,
            avatarSeed = donor.avatarSeed,
            gift = giftType,
            count = count,
            timestampMs = timestampMs
        )

        recordGiftEvent(giftEvent)
        donorLastGiftTick[donor.viewerId] = tickIndex
        lastGlobalSmallGiftTick = tickIndex
        if (giftType.isFullScreenEvent) {
            lastGlobalLargeGiftTick = tickIndex
        }
        return giftEvent
    }

    /**
     * Idempotent ledger recording: duplicate eventIds are ignored without double-counting.
     */
    fun recordGiftEvent(event: GiftEvent): Boolean {
        if (ledger.containsKey(event.eventId)) return false

        val entry = LedgerEntry(
            eventId = event.eventId,
            sessionId = event.sessionId,
            viewerId = event.viewerId,
            viewerUsername = event.username,
            giftId = event.gift.id,
            giftName = event.gift.displayName,
            virtualCoins = event.totalVirtualCoins,
            tier = event.gift.tier,
            timestampMs = event.timestampMs
        )
        ledger[event.eventId] = entry
        totalGiftsCount += event.count

        val existing = donorTotals[event.viewerId]
        donorTotals[event.viewerId] = SupporterRank(
            viewerId = event.viewerId,
            username = event.username,
            initials = event.initials,
            avatarSeed = event.avatarSeed,
            totalCoins = (existing?.totalCoins ?: 0) + event.totalVirtualCoins
        )
        return true
    }

    fun getLedgerEntries(): List<LedgerEntry> = ledger.values.toList()

    fun reconcileTotalCoins(): Int = ledger.values.sumOf { it.virtualCoins }

    fun getTopSupporters(limit: Int = 5): List<SupporterRank> =
        donorTotals.values.sortedByDescending { it.totalCoins }.take(limit)

    private fun selectGiftType(
        donorGenerosity: Float,
        sessionGenerosity: Float,
        allowLarge: Boolean
    ): GiftType {
        val roll = random.nextFloat() * (0.65f + 0.55f * donorGenerosity * sessionGenerosity)
        return when {
            allowLarge && roll > 1.12f -> GiftType.COSMIC_CELEBRATION
            allowLarge && roll > 1.03f -> GiftType.LION_MASCOT
            allowLarge && roll > 0.94f -> GiftType.CROWN
            allowLarge && roll > 0.84f -> GiftType.GALAXY
            roll > 0.68f -> GiftType.FIREWORKS
            roll > 0.45f -> GiftType.COFFEE
            roll > 0.22f -> GiftType.HEART
            else -> GiftType.FLOWER
        }
    }
}

/**
 * Coalesces overlapping small gift banners and queues full-screen animations cleanly.
 */
class GiftOverlayQueue {
    private val largeQueue = ArrayDeque<GiftEvent>()
    private val smallQueue = ArrayDeque<GiftEvent>()

    fun enqueue(event: GiftEvent) {
        if (event.gift.isFullScreenEvent) {
            // Cap backlog to prevent stale animation pileup
            if (largeQueue.size < 4) largeQueue.addLast(event)
        } else {
            val last = smallQueue.lastOrNull()
            if (last != null && last.viewerId == event.viewerId && last.gift == event.gift) {
                smallQueue.removeLast()
                smallQueue.addLast(last.copy(count = last.count + event.count))
            } else {
                if (smallQueue.size >= 6) smallQueue.removeFirst()
                smallQueue.addLast(event)
            }
        }
    }

    fun popNextLarge(): GiftEvent? = if (largeQueue.isEmpty()) null else largeQueue.removeFirst()
    fun popNextSmall(): GiftEvent? = if (smallQueue.isEmpty()) null else smallQueue.removeFirst()
    fun largeQueueSize(): Int = largeQueue.size
    fun smallQueueSize(): Int = smallQueue.size
    fun clear() {
        largeQueue.clear()
        smallQueue.clear()
    }
}
