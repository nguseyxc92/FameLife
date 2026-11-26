package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "creator_profile")
data class CreatorProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val handle: String,
    val bio: String,
    val isGuest: Boolean,
    val virtualFollowers: Int,
    val totalStreamsCount: Int,
    val avatarUri: String? = null
)

@Entity(tableName = "stream_sessions")
data class StreamSessionEntity(
    @PrimaryKey val sessionId: String,
    val platformModeId: String,
    val famePresetId: String,
    val startedAtMs: Long,
    val durationSec: Int,
    val peakViewers: Int,
    val avgViewers: Int,
    val uniqueParticipants: Int,
    val totalMessages: Int,
    val newFollowers: Int,
    val totalLikes: Int,
    val totalGifts: Int,
    val totalVirtualCoins: Int,
    val retentionPercent: Int,
    val timelineJson: String
)

@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey val eventId: String,
    val sessionId: String,
    val viewerId: String,
    val viewerUsername: String,
    val giftId: String,
    val giftName: String,
    val virtualCoins: Int,
    val tierName: String,
    val timestampMs: Long
)

@Entity(tableName = "returning_fans")
data class ReturningFanEntity(
    @PrimaryKey val viewerId: String,
    val username: String,
    val archetypeName: String,
    val loyaltyScore: Float,
    val totalGiftsCoins: Int,
    val lastTopic: String?,
    val joinCount: Int,
    val lastSeenAtMs: Long
)

@Dao
interface CreatorProfileDao {
    @Query("SELECT * FROM creator_profile WHERE id = 1")
    fun observeProfile(): Flow<CreatorProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: CreatorProfileEntity)

    @Query("DELETE FROM creator_profile")
    suspend fun clearProfile()
}

@Dao
interface StreamSessionDao {
    @Query("SELECT * FROM stream_sessions ORDER BY startedAtMs DESC")
    fun observeAllSessions(): Flow<List<StreamSessionEntity>>

    @Query("SELECT * FROM stream_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): StreamSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StreamSessionEntity)

    @Query("DELETE FROM stream_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM stream_sessions")
    suspend fun clearAllSessions()
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries ORDER BY timestampMs DESC")
    fun observeAllEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT COALESCE(SUM(virtualCoins), 0) FROM ledger_entries")
    fun observeTotalCoins(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIdempotent(entries: List<LedgerEntryEntity>)

    @Query("DELETE FROM ledger_entries")
    suspend fun clearAllLedger()
}

@Dao
interface ReturningFanDao {
    @Query("SELECT * FROM returning_fans ORDER BY loyaltyScore DESC, lastSeenAtMs DESC LIMIT 100")
    fun observeTopFans(): Flow<List<ReturningFanEntity>>

    @Query("SELECT * FROM returning_fans ORDER BY lastSeenAtMs DESC LIMIT 40")
    suspend fun getRecentFansSnapshot(): List<ReturningFanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFans(fans: List<ReturningFanEntity>)

    @Query("DELETE FROM returning_fans WHERE viewerId = :viewerId")
    suspend fun deleteFan(viewerId: String)

    @Query("DELETE FROM returning_fans")
    suspend fun clearAllFans()
}

@Database(
    entities = [
        CreatorProfileEntity::class,
        StreamSessionEntity::class,
        LedgerEntryEntity::class,
        ReturningFanEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FameLifeDatabase : RoomDatabase() {
    abstract fun creatorProfileDao(): CreatorProfileDao
    abstract fun streamSessionDao(): StreamSessionDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun returningFanDao(): ReturningFanDao

    companion object {
        @Volatile
        private var INSTANCE: FameLifeDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE creator_profile ADD COLUMN avatarUri TEXT")
            }
        }

        fun getInstance(context: Context): FameLifeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FameLifeDatabase::class.java,
                    "famelife_studio.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
