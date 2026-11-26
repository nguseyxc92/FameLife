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
    private val assignedFaceByViewerId = HashMap<String, String>(256)
    private var shuffledFaceDeck: List<String> = AudienceFaceAssets.FACE_FILES.shuffled(Random(System.nanoTime()))
    private var faceDeckCursor = 0
    private var uniqueMessageEvents = 0L

    private val firstNames = arrayOf(
        "daniel", "sarah", "hana", "dawit", "mimi", "yonas", "tigist", "bereket", "meron", "robel",
        "mahlet", "selam", "abebe", "elena", "omar", "tariq", "amara", "kofi", "leila", "zack",
        "maya", "lucas", "nina", "chloe", "noah", "liam", "sofia", "mateo", "aisha", "fatima",
        "kwame", "chidi", "tunde", "nadia", "yusuf", "samira", "jordan", "taylor", "alex", "camila",
        "diego", "kenji", "yuki", "sora", "minho", "priya", "arjun", "zara", "noel", "elias",
        "sara", "betty", "helen", "rahel", "eyob", "natnael", "lidya", "samuel", "nathan", "kaleb",
        "rediet", "bezawit", "biniyam", "mussie", "henok", "kalkidan", "mekdes", "tsion", "feven", "solomon",
        "nebiyat", "semira", "yohannes", "kidist", "temesgen", "michael", "david", "rachel", "hannah", "emma",
        "olivia", "ethan", "mason", "logan", "jayden", "caleb", "marcus", "trevor", "kevin", "brandon",
        "justin", "tyler", "brianna", "jasmine", "vanessa", "natalie", "claire", "audrey", "grace", "lily",
        "victoria", "sophie", "amir", "karim", "hassan", "layla", "yasmin", "ali", "farah", "diana",
        "valerie", "carlos", "luis", "andrea", "paula", "marco", "luca", "giulia", "chiara", "leo",
        "oscar", "felix", "hugo", "clara", "alice", "iris", "ruby", "jade", "ava", "mila",
        "zoe", "aria", "ezra", "theo", "kai", "adrian", "gabriel", "isaac", "julian", "sienna"
    )

    private val handleTokens = arrayOf(
        "m", "k", "t", "b", "s", "a", "d", "j", "r", "g",
        "live", "tv", "irl", "gg", "vibes", "daily", "creates", "music", "plays", "world",
        "studio", "xo", "official", "99", "01", "02", "03", "04", "05", "07",
        "98", "97", "24", "251", "addis", "habesha", "london", "nyc", "la", "toronto",
        "atl", "dubai", "tokyo", "paris", "berlin", "reads", "cooks", "draws", "fits", "lifts",
        "beats", "cam", "films", "travels", "notes", "edits", "chill", "flow", "wave", "spark",
        "mint", "glow", "hype", "zone", "hub", "club", "crew", "clan", "fam", "room",
        "den", "nest", "loop", "byte", "code", "dev", "tech", "pixel", "frame", "reel",
        "clips", "watch", "stream", "cast", "fm", "radio", "cafe", "buna", "latte", "tea",
        "roast", "star", "moon", "sun", "sky", "rain", "ocean", "coast", "hill", "peak",
        "view", "core", "style", "aura", "mode", "flex", "real", "true", "pure", "ace",
        "pro", "hq", "art", "vlog", "snap", "chat", "talk", "now", "here", "one",
        "x", "xx", "jr", "sr", "ii"
    )

    private val handlePrefixes = arrayOf(
        "its", "hey", "im", "real", "the", "not", "only", "just", "lil", "big",
        "young", "sunny", "night", "late", "cozy", "golden", "velvet", "silver", "cosmic", "urban",
        "chill", "happy", "lucky", "wild", "quiet", "soft", "fresh", "retro", "cyber", "pixel",
        "neon", "coffee", "midnight", "morning", "sunday", "friday", "summer", "winter", "addis", "buna",
        "abyssinia", "blue", "green", "red", "tiny", "super", "ultra", "turbo", "smooth", "sharp",
        "brave", "clever", "calm", "kind", "swift", "sweet", "bold", "bright", "clear", "vivid",
        "deep", "cool", "warm", "true", "pure", "dj", "mc", "mr", "ms", "dr"
    )

    private val handleNouns = arrayOf(
        "fox", "wolf", "lion", "bear", "tiger", "hawk", "eagle", "owl", "cat", "panda",
        "koala", "lynx", "raven", "swan", "dove", "star", "moon", "comet", "orbit", "nova",
        "nebula", "aurora", "cloud", "storm", "breeze", "river", "ocean", "wave", "canyon", "forest",
        "cedar", "palm", "rose", "lotus", "sage", "mint", "ember", "flame", "spark", "prism",
        "echo", "pulse", "beat", "chord", "lyric", "melody", "tempo", "vinyl", "sketch", "canvas",
        "lens", "shutter", "focus", "frame", "pixel", "atlas", "compass", "voyager", "nomad", "ranger",
        "knight", "pilot", "gamer", "coder", "maker", "chef", "baker", "barista", "drifter", "dreamer",
        "wanderer", "thinker", "seeker", "runner", "rider", "skater", "vibe", "sparkle", "horizon", "crest"
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
        assignedFaceByViewerId.clear()
        shuffledFaceDeck = AudienceFaceAssets.FACE_FILES.shuffled(Random(System.nanoTime()))
        faceDeckCursor = 0
        uniqueMessageEvents = 0L
    }

    private fun resolveSessionAvatarUri(
        viewerId: String,
        safeIdx: Int,
        hasProfilePicture: Boolean,
        allocateUnique: Boolean
    ): String? {
        if (!hasProfilePicture) return null
        assignedFaceByViewerId[viewerId]?.let { return it }
        val uri = if (allocateUnique && assignedFaceByViewerId.size < 2000) {
            val file = shuffledFaceDeck[faceDeckCursor % shuffledFaceDeck.size]
            faceDeckCursor++
            AudienceFaceAssets.assetUri(file).also { assignedFaceByViewerId[viewerId] = it }
        } else {
            val idx = ((safeIdx.toLong() * 31L) % shuffledFaceDeck.size).toInt().let {
                if (it < 0) it + shuffledFaceDeck.size else it
            }
            AudienceFaceAssets.assetUri(shuffledFaceDeck[idx])
        }
        return uri
    }

    private fun generateRealisticUsername(safeIdx: Int, h1: Long, h2: Long): String {
        val patternABase = firstNames.size * handleTokens.size          // 140 * 125 = 17,500
        val patternBBase = handlePrefixes.size * firstNames.size        // 70 * 140 = 9,800
        val patternCBase = handlePrefixes.size * handleNouns.size       // 70 * 80 = 5,600
        val totalNaturalCombos = patternABase + patternBBase + patternCBase // 32,900 unique natural handles

        // Permute low indices bijectively (gcd(11003, 32900) == 1) so first 32,900 handles are 100% distinct & natural
        val slot = if (safeIdx < totalNaturalCombos) {
            ((safeIdx.toLong() * 11003L) % totalNaturalCombos.toLong()).toInt().let {
                if (it < 0) it + totalNaturalCombos else it
            }
        } else {
            safeIdx
        }

        val sep = when ((h2 ushr 20 and 3L).toInt()) {
            0 -> "_"
            1 -> "."
            2 -> ""
            else -> "_"
        }

        return when {
            slot < patternABase -> {
                val f = slot / handleTokens.size
                val t = slot % handleTokens.size
                "${firstNames[f]}$sep${handleTokens[t]}"
            }
            slot < patternABase + patternBBase -> {
                val rem = slot - patternABase
                val p = rem / firstNames.size
                val f = rem % firstNames.size
                "${handlePrefixes[p]}$sep${firstNames[f]}"
            }
            slot < totalNaturalCombos -> {
                val rem = slot - patternABase - patternBBase
                val p = rem / handleNouns.size
                val n = rem % handleNouns.size
                "${handlePrefixes[p]}$sep${handleNouns[n]}"
            }
            else -> {
                val rem = slot - totalNaturalCombos
                val f = ((h1 ushr 16) % firstNames.size).toInt().let { if (it < 0) it + firstNames.size else it }
                val t = ((h1 ushr 32) % handleTokens.size).toInt().let { if (it < 0) it + handleTokens.size else it }
                val num = 10 + ((rem * 37 + ((h2 ushr 8) % 89).toInt().let { if (it < 0) it + 89 else it }) % 890)
                "${firstNames[f]}$sep${handleTokens[t]}$num"
            }
        }
    }

    fun getViewerByIndex(index: Int, memoryEnabled: Boolean): ViewerIdentity {
        val safeIdx = ((index % populationSize) + populationSize) % populationSize
        val viewerId = formatViewerId(safeIdx)
        val mem = if (memoryEnabled) returningMemory[viewerId] else null

        val h1 = splitMix64(safeIdx.toLong() + 0x9E3779B97F4A7C15UL.toLong())
        val h2 = splitMix64(h1 xor 0xBF58476D1CE4E5B9UL.toLong())
        val h3 = splitMix64(h2 xor 0x94D049BB133111EBUL.toLong())

        val generatedUsername = generateRealisticUsername(safeIdx, h1, h2)
        val username = mem?.username ?: generatedUsername
        val initials = username.filter { it.isLetterOrDigit() }.take(2).uppercase().ifEmpty { "FL" }
        val avatarSeed = (h3 and 0x7FFFFFFFL).toInt()

        // 90% probability of having a profile picture, 10% no profile picture
        val avatarProbRoll = ((h3 ushr 40) % 100L).toInt().let { if (it < 0) it + 100 else it }
        val hasProfilePicture = avatarProbRoll < 90

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
        val avatarUri = resolveSessionAvatarUri(
            viewerId = viewerId,
            safeIdx = safeIdx,
            hasProfilePicture = hasProfilePicture,
            allocateUnique = false
        )

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
            totalGiftedCoins = mem?.totalGiftsCoins ?: 0,
            hasProfilePicture = hasProfilePicture,
            avatarUri = avatarUri
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

            val allocated = viewer.copy(
                avatarUri = resolveSessionAvatarUri(
                    viewerId = viewer.viewerId,
                    safeIdx = viewer.numericIndex,
                    hasProfilePicture = viewer.hasProfilePicture,
                    allocateUnique = true
                )
            )
            pickedIndices.add(idx)
            lastSpokeTickByViewer[idx] = currentTick
            result.add(allocated)
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
