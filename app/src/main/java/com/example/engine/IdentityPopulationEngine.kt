package com.example.engine

import com.example.domain.model.AudienceLanguage
import com.example.domain.model.ViewerArchetype
import com.example.domain.model.ViewerIdentity
import kotlin.random.Random

data class ReturningFanMemory(
    val viewerId: String,
    val username: String,
    val archetype: ViewerArchetype,
    val loyaltyScore: Float,
    val totalGiftsCoins: Int,
    val lastTopic: String?,
    val joinCount: Int
)

/**
 * Deterministic, lazy-indexed virtual audience population supporting 100,000+ unique identities
 * and 40,000+ distinct active chat participants without per-viewer LLM calls or memory bloat.
 */
class IdentityPopulationEngine(
    val populationSize: Int = DEFAULT_POPULATION_SIZE
) {
    companion object {
        const val DEFAULT_POPULATION_SIZE = 120_000
    }

    private val returningMemory = LinkedHashMap<String, ReturningFanMemory>()
    private val lastSpokeTickByViewer = HashMap<Int, Int>(4096)
    private val distinctParticipantIds = HashSet<String>(4096)
    private var uniqueMessageEvents = 0L

    private val prefixes = arrayOf(
        "abyssinia", "addis", "luna", "ghost", "mimi", "daniel", "sarah", "king", "natalie", "selam",
        "dawit", "bereket", "tigist", "hana", "yonas", "pixel", "nova", "echo", "vibe", "turbo",
        "coffee", "buna", "zack", "leila", "kofi", "amara", "tarik", "sol", "maya", "rebel",
        "zen", "neon", "midnight", "cloud", "rift", "kairo", "eden", "samir", "nina", "lucas",
        "meron", "abebe", "fiker", "robel", "mahlet", "elena", "omar", "tariq", "juno", "rex",
        "sky", "frost", "blaze", "pulse", "orbit", "delta", "sage", "ivy", "milo", "chloe"
    )

    private val suffixes = arrayOf(
        "live", "x", "99", "tv", "plays", "official", "vibes", "hub", "gg", "studio",
        "irl", "251", "addis", "wave", "fam", "07", "24", "pro", "zone", "fx",
        "dev", "art", "music", "chill", "night", "cam", "cast", "star", "core", "hq",
        "clips", "daily", "watch", "room", "crew", "club", "line", "beam", "spark", "loop"
    )

    private val interests = arrayOf(
        "gaming", "music", "cooking", "tech", "comedy", "travel", "fitness",
        "art", "movies", "sports", "late-night chat", "reaction streams", "street walks"
    )

    fun setReturningMemories(memories: List<ReturningFanMemory>) {
        returningMemory.clear()
        memories.forEach { returningMemory[it.viewerId] = it }
    }

    fun clearSessionTracking() {
        lastSpokeTickByViewer.clear()
        distinctParticipantIds.clear()
        uniqueMessageEvents = 0L
    }

    fun getViewerByIndex(index: Int, memoryEnabled: Boolean): ViewerIdentity {
        val safeIdx = ((index % populationSize) + populationSize) % populationSize
        val viewerId = formatViewerId(safeIdx)
        val mem = if (memoryEnabled) returningMemory[viewerId] else null

        val h1 = splitMix64(safeIdx.toLong() + 0x9E3779B97F4A7C15UL.toLong())
        val h2 = splitMix64(h1 xor 0xBF58476D1CE4E5B9UL.toLong())
        val h3 = splitMix64(h2 xor 0x94D049BB133111EBUL.toLong())

        val pIdx = ((h1 ushr 16) % prefixes.size).toInt().let { if (it < 0) it + prefixes.size else it }
        val sIdx = ((h1 ushr 32) % suffixes.size).toInt().let { if (it < 0) it + suffixes.size else it }
        val disc = (safeIdx / (prefixes.size * suffixes.size))
        val tagNum = ((h2 ushr 8) % 97).toInt().let { if (it < 0) it + 97 else it }

        val separator = when ((h2 ushr 20 and 3L).toInt()) {
            0 -> "_"
            1 -> "."
            2 -> ""
            else -> "_"
        }
        val generatedUsername = if (disc == 0) {
            "${prefixes[pIdx]}$separator${suffixes[sIdx]}"
        } else {
            "${prefixes[pIdx]}$separator${suffixes[sIdx]}${disc}$tagNum"
        }

        val username = mem?.username ?: generatedUsername
        val initials = username.filter { it.isLetterOrDigit() }.take(2).uppercase().ifEmpty { "FL" }
        val avatarSeed = (h3 and 0x7FFFFFFFL).toInt()

        val langRoll = ((h2 ushr 24) and 0xFFL).toInt()
        val language = when {
            langRoll < 180 -> AudienceLanguage.ENGLISH
            langRoll < 225 -> AudienceLanguage.BILINGUAL
            else -> AudienceLanguage.AMHARIC
        }

        val archRoll = ((h3 ushr 12) % 1000L).toInt().let { if (it < 0) it + 1000 else it }
        val archetype = mem?.archetype ?: when {
            archRoll < 480 -> ViewerArchetype.SILENT       // 48% lurkers
            archRoll < 700 -> ViewerArchetype.CASUAL       // 22% casual
            archRoll < 810 -> ViewerArchetype.CURIOUS      // 11% newcomers
            archRoll < 890 -> ViewerArchetype.LOYAL_FAN    // 8% loyal fans
            archRoll < 935 -> ViewerArchetype.JOKER        // 4.5% comedians
            archRoll < 965 -> ViewerArchetype.CRITIC       // 3% critics
            archRoll < 992 -> ViewerArchetype.SUPPORTER    // 2.7% wealthy supporters
            else -> ViewerArchetype.MODERATOR              // 0.8% moderators
        }

        val baseLoyalty = mem?.loyaltyScore ?: (((h1 ushr 40) and 0xFFL) / 255f)
        val activityRate = when (archetype) {
            ViewerArchetype.SILENT -> 0.02f
            ViewerArchetype.CURIOUS -> 0.35f
            ViewerArchetype.CASUAL -> 0.45f
            ViewerArchetype.CRITIC -> 0.55f
            ViewerArchetype.JOKER -> 0.72f
            ViewerArchetype.LOYAL_FAN -> 0.78f
            ViewerArchetype.SUPPORTER -> 0.75f
            ViewerArchetype.MODERATOR -> 0.65f
        }
        val generosity = when (archetype) {
            ViewerArchetype.SUPPORTER -> 0.75f + (((h2 ushr 32) and 0x3FL) / 255f)
            ViewerArchetype.LOYAL_FAN -> 0.35f + (((h2 ushr 32) and 0x3FL) / 160f)
            ViewerArchetype.MODERATOR -> 0.30f
            ViewerArchetype.CASUAL -> (((h2 ushr 32) and 0x3FL) / 255f)
            else -> (((h2 ushr 32) and 0x1FL) / 255f)
        }.coerceIn(0f, 1f)

        val interestIdx = ((h3 ushr 28) % interests.size).toInt().let { if (it < 0) it + interests.size else it }

        return ViewerIdentity(
            viewerId = viewerId,
            numericIndex = safeIdx,
            username = username,
            avatarSeed = avatarSeed,
            initials = initials,
            primaryLanguage = language,
            interest = interests[interestIdx],
            archetype = archetype,
            loyalty = baseLoyalty.coerceIn(0f, 1f),
            activityRate = activityRate,
            generosity = generosity,
            joinCount = mem?.joinCount ?: 1,
            rememberedTopic = mem?.lastTopic,
            totalGiftedCoins = mem?.totalGiftsCoins ?: 0
        )
    }

    /**
     * Sample active speakers from the currently watching audience window [0, max(1, activeViewers)].
     * Respects speaker cooldowns, silent-viewer ratios, and returning fan priority.
     */
    fun sampleSpeakers(
        activeViewers: Int,
        count: Int,
        currentTick: Int,
        random: Random,
        memoryEnabled: Boolean,
        preferredLanguage: AudienceLanguage,
        allowSilentToSpeak: Boolean = false
    ): List<ViewerIdentity> {
        if (count <= 0) return emptyList()
        val window = activeViewers.coerceIn(1, populationSize)
        val result = ArrayList<ViewerIdentity>(count)
        val pickedIndices = HashSet<Int>(count * 2)

        // Chance to pull in a returning fan first if memory is enabled and they exist
        if (memoryEnabled && returningMemory.isNotEmpty() && random.nextFloat() < 0.35f) {
            val memories = returningMemory.values.toList()
            val candidate = memories[random.nextInt(memories.size)]
            val idx = parseIndexFromId(candidate.viewerId)
            if (idx != null) {
                val lastTick = lastSpokeTickByViewer[idx] ?: -999
                if (currentTick - lastTick >= 4) {
                    val viewer = getViewerByIndex(idx, memoryEnabled = true)
                    result.add(viewer)
                    pickedIndices.add(idx)
                    lastSpokeTickByViewer[idx] = currentTick
                }
            }
        }

        var attempts = 0
        val maxAttempts = count * 16 + 20
        while (result.size < count && attempts < maxAttempts) {
            attempts++
            val idx = random.nextInt(window)
            if (pickedIndices.contains(idx)) continue

            val lastTick = lastSpokeTickByViewer[idx] ?: -999
            if (currentTick - lastTick < 3) continue

            val viewer = getViewerByIndex(idx, memoryEnabled = memoryEnabled)
            if (!allowSilentToSpeak && viewer.archetype == ViewerArchetype.SILENT && attempts < maxAttempts - 8) {
                continue
            }

            // Soft language affinity filter unless almost out of attempts
            val langMatch = when (preferredLanguage) {
                AudienceLanguage.ENGLISH -> viewer.primaryLanguage != AudienceLanguage.AMHARIC || attempts > maxAttempts / 2
                AudienceLanguage.AMHARIC -> viewer.primaryLanguage != AudienceLanguage.ENGLISH || attempts > maxAttempts / 2
                AudienceLanguage.BILINGUAL -> true
            }
            if (!langMatch) continue

            pickedIndices.add(idx)
            lastSpokeTickByViewer[idx] = currentTick
            result.add(viewer)
        }

        return result
    }

    fun recordParticipation(viewerId: String) {
        distinctParticipantIds.add(viewerId)
        uniqueMessageEvents++
    }

    fun getDistinctParticipantCount(): Int = distinctParticipantIds.size

    fun getUniqueEventCount(): Long = uniqueMessageEvents

    fun formatViewerId(index: Int): String = "v_${index.toString().padStart(6, '0')}"

    private fun parseIndexFromId(viewerId: String): Int? =
        viewerId.removePrefix("v_").toIntOrNull()

    private fun splitMix64(x: Long): Long {
        var z = x + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }
}
