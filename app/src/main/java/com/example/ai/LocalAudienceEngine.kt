package com.example.ai

import android.graphics.Bitmap
import com.example.domain.model.AudienceLanguage
import com.example.domain.model.AudienceTone
import com.example.domain.model.ChatEventType
import com.example.domain.model.LivePlatformMode
import com.example.domain.model.ViewerArchetype
import com.example.domain.model.ViewerIdentity
import kotlin.random.Random

/**
 * Rich, zero-quota, deterministic/stochastic local contextual NLP & rule-based
 * audience reaction engine. Replaces cloud LLM calls during live streams so creators
 * experience responsive, realistic chat without API quota limits or error interruptions.
 */
class LocalAlgorithmicDirectorClient(
    private val random: Random = Random.Default
) : AudienceAiClient {

    override suspend fun generateBatchJson(prompt: String, frameBitmap: Bitmap?): String {
        // Not used when LocalAlgorithmicDirectorClient is invoked directly via generateLocalReactions
        return """{"reactions":[]}"""
    }

    fun generateLocalReactions(
        context: ContextSnapshot,
        speakers: List<ViewerIdentity>,
        frameBitmap: Bitmap?,
        recentTexts: ArrayDeque<String>,
        rememberText: (String) -> Unit
    ): List<DirectedReaction> {
        if (speakers.isEmpty()) return emptyList()

        val rawTranscript = context.creatorTranscript?.trim().orEmpty()
        val lower = rawTranscript.lowercase()
        val visualHint = describeVisualFrame(frameBitmap)
        val keywords = extractKeywords(rawTranscript)
        val count = speakers.size.coerceIn(3, 6)

        val candidates = buildCandidatePool(
            context = context,
            lower = lower,
            rawTranscript = rawTranscript,
            keywords = keywords,
            visualHint = visualHint
        )

        val usedInBatch = HashSet<String>()
        val results = ArrayList<DirectedReaction>(count)

        for (i in 0 until count) {
            val speaker = speakers[i]
            val line = pickSpeakerReaction(
                speaker = speaker,
                index = i,
                context = context,
                lower = lower,
                keywords = keywords,
                candidates = candidates,
                usedInBatch = usedInBatch,
                recentTexts = recentTexts
            ) ?: continue

            val norm = line.lowercase()
            usedInBatch.add(norm)
            rememberText(norm)

            val eventType = when {
                line.endsWith("?") && speaker.archetype != ViewerArchetype.CRITIC -> ChatEventType.QUESTION
                speaker.joinCount > 1 && i == 0 && rawTranscript.isBlank() -> ChatEventType.JOIN
                else -> ChatEventType.CHAT
            }

            val optionalGiftId = maybePickSmallGift(speaker, lower, context.tone, i)

            results.add(
                DirectedReaction(
                    eventId = "loc_${context.contextId}_${i}_${speaker.viewerId}",
                    viewerId = speaker.viewerId,
                    viewerSlot = i,
                    type = eventType,
                    text = line,
                    delayMs = (320L + i * 480L + random.nextLong(120L, 420L)).coerceIn(250L, 4200L),
                    contextId = context.contextId,
                    optionalGiftId = optionalGiftId
                )
            )
        }

        return results
    }

    private fun buildCandidatePool(
        context: ContextSnapshot,
        lower: String,
        rawTranscript: String,
        keywords: List<String>,
        visualHint: String?
    ): List<String> {
        val amharic = context.language == AudienceLanguage.AMHARIC
        val bilingual = context.language == AudienceLanguage.BILINGUAL

        val isChoice = lower.contains(" or ") && (lower.contains("should") || lower.contains("?") || lower.contains("which") || lower.contains("pick"))
        val isDecision = lower.startsWith("should i") || lower.contains("should i ") ||
            lower.startsWith("do i ") || lower.contains("should we") ||
            lower.contains("open this door") || lower.contains("ask her out") || lower.contains("ask him out")
        val isQuestion = rawTranscript.endsWith("?") || lower.startsWith("what ") || lower.startsWith("why ") ||
            lower.startsWith("how ") || lower.startsWith("who ") || lower.startsWith("where ") ||
            lower.startsWith("which ") || lower.startsWith("can you ") || lower.startsWith("do you ")
        val isGreeting = lower.contains("hello") || lower.contains("hi ") || lower == "hi" ||
            lower.contains("hey") || lower.contains("what's up") || lower.contains("whats up") ||
            lower.contains("good morning") || lower.contains("good evening") || lower.contains("selam") ||
            lower.contains("ሰላም") || lower.contains("how is everyone")
        val isThanks = lower.contains("thank") || lower.contains("appreciate") ||
            lower.contains("shoutout") || lower.contains("አመሰግናለሁ") || !context.lastDonorUsername.isNullOrBlank()

        val list = ArrayList<String>()

        if (isThanks) {
            val donor = context.lastDonorUsername
            if (!donor.isNullOrBlank()) {
                list += listOf(
                    "Big W to $donor 🙌",
                    "Respect $donor for supporting!",
                    "$donor carrying the stream rn 🔥",
                    "You deserve every gift honestly ❤️"
                )
            } else {
                list += if (amharic) {
                    listOf("ይመችህ! ❤️", "እኛም እናመሰግናለን!", "ሁሌም ከጎንህ ነን 🙌", "ቀጥልበት!")
                } else {
                    listOf("You deserve it!! ❤️", "W creator always", "Happy to be here fam 🙌", "Best community on here")
                }
            }
        }

        if (isChoice) {
            val parts = lower.split(" or ", limit = 2)
            val optA = parts.getOrNull(0)?.trim()?.split(" ")?.takeLast(2)?.joinToString(" ")?.replace("?", "").orEmpty()
            val optB = parts.getOrNull(1)?.trim()?.split(" ")?.take(2)?.joinToString(" ")?.replace("?", "").orEmpty()
            if (optA.isNotBlank() && optB.isNotBlank()) {
                list += listOf(
                    "Definitely $optA!!",
                    "Team $optB all day 👀",
                    "Go with $optA trust me",
                    "Wait why not $optB 😂",
                    "50/50 ngl, pick $optA"
                )
            }
        }

        if (isDecision) {
            list += when {
                lower.contains("door") -> listOf(
                    "OPEN IT RIGHT NOW 👀",
                    "Wait what if someone's inside 😭",
                    "Do it bro!!",
                    "Nooo be careful 😂",
                    "I need to see what's behind it",
                    "Slowly crack it open first!"
                )
                lower.contains("ask") && (lower.contains("out") || lower.contains("her") || lower.contains("him")) -> listOf(
                    "Send the text right now!! 🔥",
                    "You only live once go for it",
                    "Wait check the vibe first 👀",
                    "We're all rooting for you ❤️",
                    "Tell us what happens after 😂",
                    "Keep it casual and confident"
                )
                else -> listOf(
                    "Do it!! Let's see how it goes 🔥",
                    "Wait think twice first 😭",
                    "100% go for it",
                    "If it goes wrong blame chat 😂",
                    "Hold on I'm actually nervous for you",
                    "Trust your gut on this one"
                )
            }
        }

        if (isGreeting) {
            list += if (amharic) {
                listOf(
                    "ሰላም! ❤️",
                    "እንዴት ነህ? ከአዲስ አበባ እያየን ነው!",
                    "ዛሬ ድምፁም ምስሉም ጽዱ ነው",
                    "ሰላም ለሁላችሁ 👋",
                    "አሁን ነው የገባሁት ምን አመለጠኝ?"
                )
            } else {
                listOf(
                    "Heyyy!! Just got the notification 👋",
                    "What's good! Checking in from Addis",
                    "Audio and camera look super clean today",
                    "Finally caught you live!! ❤️",
                    "Yo chat how are we doing tonight"
                )
            }
        }

        // Topic-specific matching
        when {
            containsAny(lower, "coffee", "buna", "eat", "food", "cook", "lunch", "dinner", "hungry", "tea") -> {
                list += listOf(
                    "Wait that looks so good 😭",
                    "Pour a cup for chat too ☕",
                    "Now I'm hungry watching this",
                    "Buna time is unbeatable honestly",
                    "What's the secret ingredient?"
                )
            }
            containsAny(lower, "music", "song", "sing", "guitar", "beat", "piano", "listen", "play") -> {
                list += listOf(
                    "Wait play that part again!! 🎶",
                    "Such a clean melody 🔥",
                    "What song is this?",
                    "This late-night acoustic vibe hits different",
                    "Drop this on streaming asap"
                )
            }
            containsAny(lower, "game", "win", "lose", "clutch", "rank", "match", "boss", "level", "stream") -> {
                list += listOf(
                    "CLUTCH IT UP!! 🔥",
                    "Watch the left side!!",
                    "Bro that was actually insane",
                    "GGs already 😂",
                    "What rank are we pushing today?"
                )
            }
            containsAny(lower, "walk", "outside", "city", "street", "travel", "car", "drive", "trip", "night") -> {
                list += listOf(
                    "Love the IRL walk streams 🙌",
                    "Whereabouts is this?",
                    "The street lights look cinematic",
                    "Stay safe out there!",
                    "Feels like we're walking right with you"
                )
            }
            containsAny(lower, "gym", "workout", "run", "fit", "tired", "sleep", "late") -> {
                list += listOf(
                    "Respect the consistency 💪",
                    "Get some water and keep pushing!",
                    "Late night grind never stops",
                    "How long have you been at it today?"
                )
            }
            containsAny(lower, "code", "app", "tech", "phone", "camera", "setup", "build", "work") -> {
                list += listOf(
                    "Clean setup!! What specs are you using?",
                    "Tech streams are underrated ngl",
                    "How long did that take to put together?",
                    "Looks super smooth on my end"
                )
            }
        }

        // Dynamic keyword weaving for arbitrary utterances
        if (keywords.isNotEmpty() && !isGreeting) {
            val kw = keywords.first()
            val kw2 = keywords.getOrNull(1)
            list += listOf(
                "Wait $kw?! Tell us more about that",
                "Actually love the $kw topic rn",
                "Hold up let's talk about $kw for a sec",
                if (kw2 != null) "$kw + $kw2 is elite combo 😂" else "Wait show us the $kw!"
            )
        }

        if (isQuestion && !isDecision && !isChoice) {
            list += listOf(
                "Good question honestly 👀",
                "Wait let me think about that one",
                "100% yes in my opinion",
                "Depends on the mood tbh",
                "Chat is gonna have mixed answers for sure 😂"
            )
        }

        if (visualHint != null) {
            list += visualHint
        }

        // Platform flavor & bilingual/Amharic accents
        if (amharic || (bilingual && random.nextFloat() < 0.45f)) {
            list += listOf(
                "በጣም አሪፍ ነው 🔥",
                "ቀጥልበት ወንድሜ 🙌",
                "ይሄ ላይቭ ይመቻል ❤️",
                "አሁንም እየተከታተልን ነው!",
                "ምን አዲስ ነገር አለ?"
            )
        }

        if (list.isEmpty() || context.isSilent) {
            list += when (context.platformMode) {
                LivePlatformMode.GAMING_LIVE -> listOf(
                    "Chat moving fast today 👀",
                    "Who else is lurking while working?",
                    "Vibes in here are unmatched",
                    "Wait what's the next move?",
                    "Let's hit the next milestone!"
                )
                LivePlatformMode.VIDEO_LIVE -> listOf(
                    "Checking in from the home feed!",
                    "Anyone else watching with headphones?",
                    "Audio quality is crisp today",
                    "What topic are we covering next?",
                    "Drop a wave if you're early 👋"
                )
                else -> listOf(
                    "Just popped into the live 👋",
                    "Love the energy in here ❤️",
                    "Where is everyone watching from?",
                    "Stay live a bit longer!",
                    "First time catching your stream!"
                )
            }
        }

        return list
    }

    private fun pickSpeakerReaction(
        speaker: ViewerIdentity,
        index: Int,
        context: ContextSnapshot,
        lower: String,
        keywords: List<String>,
        candidates: List<String>,
        usedInBatch: Set<String>,
        recentTexts: ArrayDeque<String>
    ): String? {
        // Returning fan memory shoutout
        if (!speaker.rememberedTopic.isNullOrBlank() && index == 0 && random.nextFloat() < 0.55f) {
            val memLine = "Still thinking about \"${speaker.rememberedTopic.take(28)}\" from last stream!"
            if (!recentTexts.contains(memLine.lowercase()) && !usedInBatch.contains(memLine.lowercase())) {
                return memLine
            }
        }

        // Archetype & tone flavored override ~30% of the time for natural diversity
        val archetypeLine: String? = when (speaker.archetype) {
            ViewerArchetype.MODERATOR -> if (index == candidates.lastIndex && random.nextFloat() < 0.4f) {
                listOf("Welcome everyone jumping in! Keep chat kind 🛡️", "Shoutout to all the regulars here today!").random(random)
            } else null
            ViewerArchetype.CRITIC -> if (context.tone != AudienceTone.SUPPORTIVE && random.nextFloat() < 0.4f) {
                val kw = keywords.firstOrNull()
                if (kw != null) "Hmm not fully convinced on $kw yet 👀"
                else listOf("Wait let's see if you actually follow through 👀", "Unpopular opinion maybe, idk about that").random(random)
            } else null
            ViewerArchetype.JOKER -> if (random.nextFloat() < 0.35f) {
                listOf("Chat has zero chill today 😂", "Plot twist incoming I can feel it 😭", "Why did that actually make me laugh").random(random)
            } else null
            else -> null
        }

        if (archetypeLine != null && !usedInBatch.contains(archetypeLine.lowercase()) && !recentTexts.contains(archetypeLine.lowercase())) {
            return archetypeLine
        }

        // Pick unused candidate
        val shuffled = candidates.shuffled(random)
        val picked = shuffled.firstOrNull { line ->
            val norm = line.lowercase()
            !usedInBatch.contains(norm) && !recentTexts.contains(norm)
        }
        if (picked != null) return picked

        // Varied fallback if everything in candidate pool was recently used
        val kw = keywords.firstOrNull()
        val fallbacks = if (kw != null) {
            listOf("Big facts about $kw", "Wait $kw tho 👀", "Keep talking about $kw!", "So real for saying that")
        } else {
            listOf("W stream today 🔥", "Love this vibe", "Wait say that again!", "Here for the whole live 🙌", "Watching from work lol")
        }
        return fallbacks.firstOrNull { !usedInBatch.contains(it.lowercase()) } ?: fallbacks.random(random)
    }

    private fun maybePickSmallGift(
        speaker: ViewerIdentity,
        lower: String,
        tone: AudienceTone,
        index: Int
    ): String? {
        if (index != 0 && speaker.archetype != ViewerArchetype.SUPPORTER) return null
        val chance = when (speaker.archetype) {
            ViewerArchetype.SUPPORTER -> 0.24f
            ViewerArchetype.LOYAL_FAN -> 0.12f
            else -> 0.03f
        } * if (tone == AudienceTone.LIVELY || lower.contains("thank")) 1.3f else 1.0f

        if (random.nextFloat() >= chance) return null
        return listOf("flower", "heart", "coffee").random(random)
    }

    private fun describeVisualFrame(bitmap: Bitmap?): String? {
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return null
        return try {
            val sampleX = bitmap.width / 2
            val sampleY = bitmap.height / 2
            val pixel = bitmap.getPixel(sampleX, sampleY)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luma = (0.299 * r + 0.587 * g + 0.114 * b)
            when {
                luma < 60 -> "Cozy low-light aesthetic rn ✨"
                luma > 185 -> "Super bright crisp lighting today!"
                r > b + 35 -> "Love the warm lighting tones in frame"
                else -> "Camera framing looks really clean"
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractKeywords(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val stopWords = setOf(
            "the", "and", "that", "this", "with", "from", "have", "what", "when", "where",
            "which", "your", "you", "for", "are", "was", "were", "just", "like", "about",
            "should", "could", "would", "going", "want", "need", "know", "think", "really",
            "here", "there", "they", "them", "been", "some", "more", "very", "guys", "chat",
            "everyone", "today", "tonight", "right", "now", "hello", "hey", "hi", "please"
        )
        return text.lowercase()
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length >= 4 && it !in stopWords }
            .distinct()
            .take(3)
    }

    private fun containsAny(text: String, vararg words: String): Boolean =
        words.any { text.contains(it) }
}
