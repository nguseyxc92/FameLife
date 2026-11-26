package com.example.domain.model

enum class LivePlatformMode(
    val id: String,
    val displayName: String,
    val inspirationLabel: String,
    val subtitle: String,
    val badgeTag: String,
    val defaultLandscape: Boolean
) {
    PHOTO_LIVE(
        id = "photo_live",
        displayName = "Photo Live",
        inspirationLabel = "Inspired by Instagram Live",
        subtitle = "Floating hearts, join notices, Q&A pill, badges & pinned comments",
        badgeTag = "PHOTO LIVE",
        defaultLandscape = false
    ),
    SHORT_LIVE(
        id = "short_live",
        displayName = "Short Live",
        inspirationLabel = "Inspired by TikTok LIVE",
        subtitle = "Vertical full-screen, rapid chat, supporter ranks & animated gift tray",
        badgeTag = "SHORT LIVE",
        defaultLandscape = false
    ),
    VIDEO_LIVE(
        id = "video_live",
        displayName = "Video Live",
        inspirationLabel = "Inspired by YouTube Live",
        subtitle = "Highlighted Super Chats, memberships, dedicated chat & mod controls",
        badgeTag = "VIDEO LIVE",
        defaultLandscape = false
    ),
    GAMING_LIVE(
        id = "gaming_live",
        displayName = "Gaming Live",
        inspirationLabel = "Inspired by Twitch",
        subtitle = "Screen-share stage, emotes, Bits, subs, raids & stream summary",
        badgeTag = "GAMING LIVE",
        defaultLandscape = true
    );

    companion object {
        fun fromId(id: String): LivePlatformMode = entries.firstOrNull { it.id == id } ?: SHORT_LIVE
    }
}

enum class FamePreset(
    val id: String,
    val title: String,
    val minTarget: Int,
    val maxTarget: Int,
    val growthPace: Float,
    val baseEngagement: Float,
    val giftGenerosity: Float,
    val rangeLabel: String
) {
    NEW_CREATOR("new_creator", "New Creator", 10, 100, 0.35f, 0.55f, 0.25f, "10 – 100"),
    RISING_STAR("rising_star", "Rising Star", 100, 1_000, 0.50f, 0.65f, 0.45f, "100 – 1,000"),
    INFLUENCER("influencer", "Influencer", 1_000, 5_000, 0.65f, 0.72f, 0.60f, "1,000 – 5,000"),
    INTERNET_CELEBRITY("internet_celebrity", "Internet Celebrity", 5_000, 15_000, 0.78f, 0.78f, 0.72f, "5,000 – 15,000"),
    SUPERSTAR("superstar", "Superstar", 15_000, 40_000, 0.88f, 0.84f, 0.85f, "15,000 – 40,000"),
    GLOBAL_ICON("global_icon", "Global Icon", 40_000, 150_000, 0.96f, 0.90f, 0.95f, "40,000 – 150,000"),
    CUSTOM("custom", "Custom Fame", 50, 25_000, 0.60f, 0.70f, 0.55f, "Configurable");

    companion object {
        fun fromId(id: String): FamePreset = entries.firstOrNull { it.id == id } ?: RISING_STAR
    }
}

data class CustomFameConfig(
    val targetAudience: Int = 2_500,
    val growthSpeed: Float = 0.6f,     // 0.1f..1.0f
    val engagement: Float = 0.7f,      // 0.1f..1.0f
    val generosity: Float = 0.55f      // 0.0f..1.0f
)

enum class AudienceTone(val id: String, val label: String, val description: String) {
    SUPPORTIVE("supportive", "Supportive", "Warm encouragement, helpful feedback & friendly banter"),
    BALANCED("balanced", "Balanced", "Realistic mix of fans, curious newcomers, jokes & mild critics"),
    LIVELY("lively", "Lively / Chaotic", "High-energy hype, memes, rapid debates & bold reactions");

    companion object {
        fun fromId(id: String): AudienceTone = entries.firstOrNull { it.id == id } ?: BALANCED
    }
}

enum class AudienceLanguage(val id: String, val label: String, val promptHint: String) {
    ENGLISH("en", "English", "English"),
    AMHARIC("am", "Amharic (አማርኛ)", "Amharic (mix of Ethiopic script and natural Latin-transliterated Ethiopian slang where realistic)"),
    BILINGUAL("en_am", "English + Amharic", "Natural mix of English and Amharic");

    companion object {
        fun fromId(id: String): AudienceLanguage = entries.firstOrNull { it.id == id } ?: ENGLISH
    }
}

enum class StreamSourceMode(val id: String, val label: String) {
    FRONT_CAMERA("front_cam", "Front Camera"),
    REAR_CAMERA("rear_cam", "Rear Camera"),
    SCREEN_SHARE("screen_share", "Screen Share"),
    AUDIO_ONLY("audio_only", "Audio Only")
}
