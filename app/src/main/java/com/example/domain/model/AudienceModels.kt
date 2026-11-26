package com.example.domain.model

enum class ViewerArchetype(val label: String, val badgeLabel: String?) {
    SILENT("Silent Viewer", null),
    CASUAL("Casual", null),
    LOYAL_FAN("Loyal Fan", "FAN"),
    CRITIC("Critic", null),
    JOKER("Comedian", null),
    CURIOUS("Newcomer", "NEW"),
    MODERATOR("Moderator", "MOD"),
    SUPPORTER("Supporter", "TOP")
}

data class ViewerIdentity(
    val viewerId: String,
    val numericIndex: Int,
    val username: String,
    val avatarSeed: Int,
    val initials: String,
    val primaryLanguage: AudienceLanguage,
    val interest: String,
    val archetype: ViewerArchetype,
    val loyalty: Float,          // 0..1
    val activityRate: Float,     // 0..1
    val generosity: Float,       // 0..1
    val joinCount: Int = 1,
    val rememberedTopic: String? = null,
    val totalGiftedCoins: Int = 0,
    val hasProfilePicture: Boolean = true
)

enum class ChatEventType {
    CHAT,
    QUESTION,
    JOIN,
    FOLLOW,
    SUBSCRIPTION,
    SUPER_CHAT,
    BITS,
    RAID,
    GIFT_REACTION,
    SYSTEM_NOTICE
}

data class ChatEvent(
    val eventId: String,
    val sequence: Long,
    val timestampMs: Long,
    val sessionId: String,
    val viewerId: String,
    val username: String,
    val initials: String,
    val avatarSeed: Int,
    val archetype: ViewerArchetype,
    val type: ChatEventType,
    val text: String,
    val contextId: String,
    val giftId: String? = null,
    val virtualCoins: Int = 0,
    val isPinned: Boolean = false,
    val isReturningFan: Boolean = false,
    val hasProfilePicture: Boolean = true
)

enum class GiftTier(val label: String, val priority: Int) {
    COMMON("Common", 1),
    UNCOMMON("Uncommon", 2),
    RARE("Rare", 3),
    EXCEPTIONAL("Exceptional", 4)
}

enum class GiftType(
    val id: String,
    val displayName: String,
    val tier: GiftTier,
    val virtualCoins: Int,
    val isFullScreenEvent: Boolean
) {
    FLOWER("flower", "Desert Rose", GiftTier.COMMON, 5, false),
    HEART("heart", "Pulse Heart", GiftTier.COMMON, 10, false),
    COFFEE("coffee", "Buna Cup", GiftTier.COMMON, 25, false),
    FIREWORKS("fireworks", "Sky Fireworks", GiftTier.UNCOMMON, 100, false),
    GALAXY("galaxy", "Nebula Galaxy", GiftTier.UNCOMMON, 500, true),
    CROWN("crown", "Royal Crown", GiftTier.RARE, 1_200, true),
    LION_MASCOT("lion_mascot", "Addis Lion", GiftTier.RARE, 2_500, true),
    COSMIC_CELEBRATION("cosmic_celebration", "Cosmic Nova", GiftTier.EXCEPTIONAL, 5_000, true);

    companion object {
        fun fromId(id: String?): GiftType? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}

data class GiftEvent(
    val eventId: String,
    val sessionId: String,
    val viewerId: String,
    val username: String,
    val initials: String,
    val avatarSeed: Int,
    val gift: GiftType,
    val count: Int = 1,
    val timestampMs: Long,
    val message: String? = null,
    val hasProfilePicture: Boolean = true
) {
    val totalVirtualCoins: Int get() = gift.virtualCoins * count
}

data class LedgerEntry(
    val eventId: String,
    val sessionId: String,
    val viewerId: String,
    val viewerUsername: String,
    val giftId: String,
    val giftName: String,
    val virtualCoins: Int,
    val tier: GiftTier,
    val timestampMs: Long
)

data class ViewerTimePoint(
    val elapsedSec: Int,
    val viewerCount: Int
)

data class SupporterRank(
    val viewerId: String,
    val username: String,
    val initials: String,
    val avatarSeed: Int,
    val totalCoins: Int,
    val hasProfilePicture: Boolean = true
)
