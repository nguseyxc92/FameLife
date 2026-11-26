package com.example.data

import com.example.domain.model.LedgerEntry
import com.example.domain.model.ViewerArchetype
import com.example.domain.model.ViewerTimePoint
import com.example.engine.ReturningFanMemory
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class FameLifeRepository(private val db: FameLifeDatabase) {
    val creatorProfile: Flow<CreatorProfileEntity?> = db.creatorProfileDao().observeProfile()
    val sessions: Flow<List<StreamSessionEntity>> = db.streamSessionDao().observeAllSessions()
    val ledgerEntries: Flow<List<LedgerEntryEntity>> = db.ledgerDao().observeAllEntries()
    val totalVirtualCoins: Flow<Int> = db.ledgerDao().observeTotalCoins()
    val returningFans: Flow<List<ReturningFanEntity>> = db.returningFanDao().observeTopFans()

    suspend fun saveCreatorProfile(
        displayName: String,
        handle: String,
        bio: String,
        isGuest: Boolean,
        avatarUri: String? = null
    ) {
        val current = db.creatorProfileDao()
        current.upsertProfile(
            CreatorProfileEntity(
                id = 1,
                displayName = displayName.ifBlank { "Creator" },
                handle = handle.ifBlank { "famelife_creator" }.removePrefix("@"),
                bio = bio,
                isGuest = isGuest,
                virtualFollowers = if (isGuest) 0 else 120,
                totalStreamsCount = 0,
                avatarUri = avatarUri
            )
        )
    }

    suspend fun updateProfileDetails(
        existing: CreatorProfileEntity?,
        displayName: String,
        handle: String,
        bio: String,
        avatarUri: String?
    ) {
        val cleanedHandle = handle.trim().removePrefix("@").ifBlank { "famelife_creator" }
        val cleanedName = displayName.trim().ifBlank { "Creator" }
        val updated = existing?.copy(
            displayName = cleanedName,
            handle = cleanedHandle,
            bio = bio.trim(),
            isGuest = false,
            avatarUri = avatarUri
        ) ?: CreatorProfileEntity(
            id = 1,
            displayName = cleanedName,
            handle = cleanedHandle,
            bio = bio.trim(),
            isGuest = false,
            virtualFollowers = 120,
            totalStreamsCount = 0,
            avatarUri = avatarUri
        )
        db.creatorProfileDao().upsertProfile(updated)
    }

    suspend fun updateProfileEntity(entity: CreatorProfileEntity) {
        db.creatorProfileDao().upsertProfile(entity)
    }

    suspend fun getReturningFanMemories(): List<ReturningFanMemory> {
        return db.returningFanDao().getRecentFansSnapshot().map { entity ->
            ReturningFanMemory(
                viewerId = entity.viewerId,
                username = entity.username,
                archetype = ViewerArchetype.entries.firstOrNull { it.name == entity.archetypeName }
                    ?: ViewerArchetype.LOYAL_FAN,
                loyaltyScore = entity.loyaltyScore,
                totalGiftsCoins = entity.totalGiftsCoins,
                lastTopic = entity.lastTopic,
                joinCount = entity.joinCount
            )
        }
    }

    suspend fun persistCompletedSession(
        session: StreamSessionEntity,
        ledger: List<LedgerEntry>,
        fansToRemember: List<ReturningFanEntity>
    ) {
        db.streamSessionDao().insertSession(session)
        if (ledger.isNotEmpty()) {
            db.ledgerDao().insertAllIdempotent(
                ledger.map {
                    LedgerEntryEntity(
                        eventId = it.eventId,
                        sessionId = it.sessionId,
                        viewerId = it.viewerId,
                        viewerUsername = it.viewerUsername,
                        giftId = it.giftId,
                        giftName = it.giftName,
                        virtualCoins = it.virtualCoins,
                        tierName = it.tier.name,
                        timestampMs = it.timestampMs
                    )
                }
            )
        }
        if (fansToRemember.isNotEmpty()) {
            db.returningFanDao().upsertFans(fansToRemember)
        }
    }

    suspend fun deleteSession(sessionId: String) = db.streamSessionDao().deleteSession(sessionId)

    suspend fun deleteReturningFan(viewerId: String) = db.returningFanDao().deleteFan(viewerId)

    suspend fun deleteAllLocalData() {
        db.streamSessionDao().clearAllSessions()
        db.ledgerDao().clearAllLedger()
        db.returningFanDao().clearAllFans()
        db.creatorProfileDao().clearProfile()
    }

    fun serializeTimeline(points: List<ViewerTimePoint>): String {
        val arr = JSONArray()
        points.forEach { p ->
            arr.put(JSONObject().apply {
                put("t", p.elapsedSec)
                put("n", p.viewerCount)
            })
        }
        return arr.toString()
    }

    fun parseTimeline(json: String): List<ViewerTimePoint> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ViewerTimePoint(o.optInt("t", 0), o.optInt("n", 0))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun buildExportSummaryText(session: StreamSessionEntity): String {
        return """
[FAMELIFE SIMULATED STREAM REPORT — NOT REAL SOCIAL MEDIA DATA]
Session ID: ${session.sessionId}
Studio Mode: ${session.platformModeId}
Fame Preset: ${session.famePresetId}
Duration: ${session.durationSec}s
Peak Viewers (Simulated): ${session.peakViewers}
Average Viewers (Simulated): ${session.avgViewers}
Unique Chat Participants (Simulated): ${session.uniqueParticipants}
Total Messages (Simulated): ${session.totalMessages}
New Virtual Followers: ${session.newFollowers}
Total Virtual Likes: ${session.totalLikes}
Total Virtual Gifts: ${session.totalGifts}
Total Virtual Coins (Non-redeemable): ${session.totalVirtualCoins}
Retention Score: ${session.retentionPercent}%
Generated by FameLife (Tebib Labs)
        """.trimIndent()
    }
}
