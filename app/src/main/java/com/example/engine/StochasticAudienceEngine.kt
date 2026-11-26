package com.example.engine

import com.example.domain.model.CustomFameConfig
import com.example.domain.model.FamePreset
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

data class AudienceTickSnapshot(
    val tickIndex: Int,
    val elapsedSec: Int,
    val activeViewers: Int,
    val peakViewers: Int,
    val arrivals: Int,
    val departures: Int,
    val engagementScore: Float,
    val latentDemand: Float,
    val viralEventTriggered: Boolean,
    val newFollowers: Int,
    val newLikes: Int
)

/**
 * Stochastic population & engagement dynamics engine.
 * - Never jumps instantly to target.
 * - Uses Poisson/Binomial draws for arrivals & departures: N(t+1) = max(0, N(t) + arrivals - departures).
 * - Responds organically to creator speech, silence decay, time-of-day, session age, and rare viral spikes.
 */
class StochasticAudienceEngine(
    private val preset: FamePreset,
    private val customConfig: CustomFameConfig = CustomFameConfig(),
    private val returningCoreSize: Int = 0,
    seed: Long = System.nanoTime(),
    private val hourOfDay: Int = 20
) {
    private val random = Random(seed)

    var currentTick: Int = 0
        private set
    var elapsedSec: Int = 0
        private set
    var activeViewers: Int = returningCoreSize.coerceAtLeast(0)
        private set
    var peakViewers: Int = activeViewers
        private set

    private var silenceSeconds: Int = 0
    private var contentMomentum: Float = 0.25f
    private var correlatedNoise: Float = 0f
    private var viralTicksRemaining: Int = 0
    private var viralMultiplier: Float = 1f
    private var cumulativeViewersSum: Long = 0L

    val targetMidpoint: Int
        get() = if (preset == FamePreset.CUSTOM) {
            customConfig.targetAudience.coerceIn(10, 150_000)
        } else {
            ((preset.minTarget + preset.maxTarget) / 2f).roundToInt()
        }

    val growthPace: Float
        get() = if (preset == FamePreset.CUSTOM) customConfig.growthSpeed.coerceIn(0.1f, 1.0f) else preset.growthPace

    val baseEngagement: Float
        get() = if (preset == FamePreset.CUSTOM) customConfig.engagement.coerceIn(0.1f, 1.0f) else preset.baseEngagement

    val generosityFactor: Float
        get() = if (preset == FamePreset.CUSTOM) customConfig.generosity.coerceIn(0.0f, 1.0f) else preset.giftGenerosity

    private val timeOfDayFactor: Float = when (hourOfDay) {
        in 18..23 -> 1.15f
        in 12..17 -> 1.0f
        in 7..11 -> 0.85f
        else -> 0.72f
    }

    /**
     * Call whenever the creator speaks, reacts, pins a comment, or triggers a visual scene change.
     */
    fun notifyCreatorActivity(intensity: Float = 0.45f) {
        silenceSeconds = 0
        contentMomentum = (contentMomentum + intensity).coerceIn(0f, 1.4f)
    }

    fun triggerViralShareBoost(multiplier: Float = 2.2f, durationTicks: Int = 6) {
        viralMultiplier = max(viralMultiplier, multiplier)
        viralTicksRemaining = max(viralTicksRemaining, durationTicks)
    }

    fun step(tickSeconds: Int = 2, hadCreatorActivityThisTick: Boolean = false): AudienceTickSnapshot {
        currentTick++
        elapsedSec += tickSeconds

        if (hadCreatorActivityThisTick) {
            notifyCreatorActivity(0.35f)
        } else {
            silenceSeconds += tickSeconds
            // Decay momentum faster when silent
            val decay = if (silenceSeconds > 12) 0.08f else 0.03f
            contentMomentum = (contentMomentum - decay).coerceAtLeast(0.05f)
        }

        // Correlated Ornstein-Uhlenbeck style noise so viewer waves ebb and flow naturally
        val shock = (random.nextFloat() * 2f - 1f) * 0.14f
        correlatedNoise = (0.78f * correlatedNoise + shock).coerceIn(-0.45f, 0.45f)

        // Gradual ramp curve based on session age and growth pace (no instant jump to target)
        val rampMinutes = (4.5f - growthPace * 2.5f).coerceAtLeast(1.2f)
        val sessionMinutes = elapsedSec / 60f
        val rampFactor = (1f - exp(-sessionMinutes / rampMinutes)).coerceIn(0.03f, 1.0f)

        // Rare viral / recommendation spike
        var viralEventTriggered = false
        if (viralTicksRemaining <= 0 && contentMomentum > 0.55f && random.nextFloat() < 0.018f * growthPace) {
            viralMultiplier = 1.45f + random.nextFloat() * 0.9f
            viralTicksRemaining = 5 + random.nextInt(5)
            viralEventTriggered = true
        } else if (viralTicksRemaining > 0) {
            viralTicksRemaining--
            if (viralTicksRemaining == 0) viralMultiplier = 1f
        }

        // Silence penalty: even huge creators bleed viewers when silent for 18+ seconds
        val silencePenalty = when {
            silenceSeconds < 12 -> 1.0f
            silenceSeconds < 28 -> 0.82f
            silenceSeconds < 50 -> 0.58f
            else -> 0.35f
        }

        val engagementScore = (baseEngagement * 0.45f + contentMomentum * 0.45f + (correlatedNoise * 0.2f))
            .times(silencePenalty)
            .coerceIn(0.05f, 1.0f)

        val latentDemand = (targetMidpoint *
            rampFactor *
            timeOfDayFactor *
            viralMultiplier *
            (0.45f + 0.75f * engagementScore) *
            (1f + correlatedNoise * 0.35f))
            .coerceAtLeast(0f)

        // Arrival & departure rates bounded relative to demand gap and active count
        val gap = (latentDemand - activeViewers).coerceAtLeast(0f)
        val baseArrivalRate = (gap * (0.045f + 0.05f * growthPace) + (activeViewers * 0.012f * engagementScore))
            .coerceIn(0f, max(5f, targetMidpoint * 0.14f))

        val boredomPressure = if (silenceSeconds > 16) 0.045f else 0.012f
        val overshootPressure = if (activeViewers > latentDemand) {
            (activeViewers - latentDemand) * 0.08f
        } else 0f
        val baseDepartureRate = (activeViewers * (boredomPressure + 0.015f * (1f - engagementScore)) + overshootPressure)
            .coerceIn(0f, activeViewers.toFloat())

        val arrivals = samplePoisson(baseArrivalRate.toDouble(), random)
        val departures = min(activeViewers, samplePoisson(baseDepartureRate.toDouble(), random))

        activeViewers = max(0, activeViewers + arrivals - departures)
        if (activeViewers > peakViewers) {
            peakViewers = activeViewers
        }
        cumulativeViewersSum += activeViewers

        val followerProb = (0.02f + 0.08f * engagementScore) * (arrivals.coerceAtLeast(1) / 8f).coerceIn(0.1f, 3f)
        val newFollowers = if (activeViewers > 0) samplePoisson(followerProb.toDouble().coerceAtMost(45.0), random) else 0
        val likeLambda = (activeViewers * 0.025f * engagementScore).coerceIn(0f, 350f)
        val newLikes = if (activeViewers > 0) samplePoisson(likeLambda.toDouble(), random) else 0

        return AudienceTickSnapshot(
            tickIndex = currentTick,
            elapsedSec = elapsedSec,
            activeViewers = activeViewers,
            peakViewers = peakViewers,
            arrivals = arrivals,
            departures = departures,
            engagementScore = engagementScore,
            latentDemand = latentDemand,
            viralEventTriggered = viralEventTriggered,
            newFollowers = newFollowers,
            newLikes = newLikes
        )
    }

    fun getAverageViewers(): Int {
        return if (currentTick <= 0) activeViewers else (cumulativeViewersSum / currentTick).toInt()
    }

    private fun samplePoisson(lambda: Double, rng: Random): Int {
        if (lambda <= 0.0) return 0
        if (lambda > 35.0) {
            // Fast normal approximation for large concurrent streams (up to 150,000 viewers)
            val std = sqrt(lambda)
            // Box-Muller
            val u1 = rng.nextDouble().coerceAtLeast(1e-9)
            val u2 = rng.nextDouble()
            val z = sqrt(-2.0 * ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
            return max(0, (lambda + std * z).roundToInt())
        }
        val l = exp(-lambda)
        var k = 0
        var p = 1.0
        do {
            k++
            p *= rng.nextDouble()
        } while (p > l)
        return k - 1
    }
}
