package com.stadiumsync.app.data.local.dao

import androidx.room.*
import com.stadiumsync.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY startTime DESC")
    fun observeAll(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE status = 'LIVE' LIMIT 1")
    fun observeLive(): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getById(id: String): MatchEntity?

    @Upsert
    suspend fun upsert(match: MatchEntity)

    @Upsert
    suspend fun upsertAll(matches: List<MatchEntity>)
}

@Dao
interface CrowdDataDao {
    @Query("SELECT * FROM crowd_data ORDER BY densityPercent DESC")
    fun observeAll(): Flow<List<CrowdDataEntity>>

    @Upsert
    suspend fun upsertAll(data: List<CrowdDataEntity>)

    @Query("DELETE FROM crowd_data")
    suspend fun clearAll()
}

@Dao
interface TransitRouteDao {
    @Query("SELECT * FROM transit_routes ORDER BY type, name")
    fun observeAll(): Flow<List<TransitRouteEntity>>

    @Upsert
    suspend fun upsertAll(routes: List<TransitRouteEntity>)

    @Query("SELECT * FROM transit_routes WHERE id = :id")
    suspend fun getById(id: String): TransitRouteEntity?
}

@Dao
interface TransitActionDao {
    @Query("SELECT * FROM transit_actions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransitActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: TransitActionEntity)

    @Query("SELECT COUNT(*) FROM transit_actions")
    suspend fun count(): Int
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<SyncQueueEntity>

    @Insert
    suspend fun insert(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearSynced()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT 100")
    fun observeRecent(): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insert(log: AuditLogEntity)

    @Query("SELECT * FROM audit_log WHERE synced = 0")
    suspend fun getUnsynced(): List<AuditLogEntity>

    @Query("UPDATE audit_log SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}

@Dao
interface PredictionDao {
    @Query("SELECT * FROM predictions WHERE matchId = :matchId")
    fun observe(matchId: String): Flow<PredictionEntity?>

    @Upsert
    suspend fun upsert(prediction: PredictionEntity)
}

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY scannedAt DESC")
    fun observeAll(): Flow<List<TicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ticket: TicketEntity)

    @Query("SELECT * FROM tickets WHERE ticketId = :ticketId")
    suspend fun getByCode(ticketId: String): TicketEntity?

    @Query("SELECT COUNT(*) FROM tickets")
    suspend fun count(): Int

    @Query("DELETE FROM tickets")
    suspend fun clearAll()
}

