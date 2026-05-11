package com.stadiumsync.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val team1: String,
    val team2: String,
    val venue: String,
    val score1: String = "0/0",
    val score2: String = "",
    val overs1: String = "0.0",
    val overs2: String = "",
    val currentRunRate: Double = 0.0,
    val requiredRunRate: Double = 0.0,
    val matchPhase: String = "PRE_MATCH",
    val status: String = "UPCOMING",
    val format: String = "T20",
    val startTime: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "crowd_data")
data class CrowdDataEntity(
    @PrimaryKey val gateId: String,
    val gateName: String,
    val pressureLevel: String = "LOW",
    val densityPercent: Int = 0,
    val trend: String = "STABLE",
    val estimatedPeople: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "transit_routes")
data class TransitRouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val status: String = "ACTIVE",
    val capacity: Int = 0,
    val currentLoad: Int = 0,
    val estimatedDelayMin: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "transit_actions")
data class TransitActionEntity(
    @PrimaryKey val id: String,
    val actionType: String,
    val routeId: String,
    val routeName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val operatorName: String = "",
    val status: String = "PENDING",
    val description: String = "",
    val isAutoTriggered: Boolean = false
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val priority: String = "INFO",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "GENERAL",
    val isDelivered: Boolean = true
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "PENDING"
)

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val details: String = "",
    val operatorName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Entity(tableName = "predictions")
data class PredictionEntity(
    @PrimaryKey val matchId: String,
    val estimatedEndTime: Long = 0L,
    val confidencePercent: Int = 0,
    val crowdReleaseScore: Double = 0.0,
    val transitUrgencyScore: Double = 0.0,
    val estimatedMinutesLeft: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val ticketId: String,
    val holderName: String,
    val pavilion: String,
    val section: String,
    val seatRow: String,
    val seatNumber: Int,
    val gate: String,
    val matchId: String = "",
    val status: String = "VALID",
    val scannedAt: Long = System.currentTimeMillis()
)

