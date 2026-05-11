package com.stadiumsync.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stadiumsync.app.data.local.dao.*
import com.stadiumsync.app.data.local.entity.*

@Database(
    entities = [
        MatchEntity::class,
        CrowdDataEntity::class,
        TransitRouteEntity::class,
        TransitActionEntity::class,
        NotificationEntity::class,
        SyncQueueEntity::class,
        AuditLogEntity::class,
        PredictionEntity::class,
        TicketEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class StadiumSyncDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun crowdDataDao(): CrowdDataDao
    abstract fun transitRouteDao(): TransitRouteDao
    abstract fun transitActionDao(): TransitActionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun predictionDao(): PredictionDao
    abstract fun ticketDao(): TicketDao
}
