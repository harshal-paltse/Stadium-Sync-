package com.stadiumsync.app.domain.repository

import com.stadiumsync.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    fun observeLiveMatch(): Flow<Match?>
    fun observePrediction(matchId: String): Flow<MatchPrediction?>
    suspend fun refreshMatch()
}

interface CrowdRepository {
    fun observeCrowdPressure(): Flow<List<CrowdPressure>>
    fun getHeatmapPoints(): List<CrowdHeatmapPoint>
    suspend fun refreshCrowdData(matchPhase: MatchPhase)
}

interface TransitRepository {
    fun observeRoutes(): Flow<List<TransitRoute>>
    fun observeActions(): Flow<List<TransitAction>>
    fun observeVehicles(): Flow<List<TransitVehicle>>
    fun observeIncidents(): Flow<List<TransitIncident>>
    fun getStations(): List<TransitStation>
    suspend fun executeAction(action: TransitAction)
    fun getRecommendations(crowdPressure: List<CrowdPressure>): List<TransitRecommendation>
    fun getRouteSuggestions(): List<RouteSuggestion>
    fun getSmartSuggestions(from: String, to: String, type: TransitType?): List<TransitSuggestion>
    fun getRouteETA(routeId: String): RouteETA
    suspend fun refreshRoutes()
}

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, password: String, role: UserRole, badgeId: String, department: String): Result<User>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun logout()
    fun isLoggedIn(): Flow<Boolean>
    fun getCurrentUser(): Flow<User?>
    fun getFailedAttempts(email: String): Int
    fun isAccountLocked(email: String): Boolean
    fun getLockoutRemainingSeconds(email: String): Int
}

interface NotificationRepository {
    fun observeNotifications(): Flow<List<StadiumAlert>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun addNotification(alert: StadiumAlert)
    suspend fun markRead(id: String)
    suspend fun markAllRead()
    suspend fun clearAll()
}

interface SyncRepository {
    fun observeSyncStatus(): Flow<SyncStatus>
    suspend fun queueAction(actionType: String, payload: String)
    suspend fun processPendingSync(): Result<Int>
    suspend fun getLastSyncTime(): Long
}

interface AnalyticsRepository {
    fun getAnalyticsSummary(): AnalyticsSummary
    fun observeAuditLog(): Flow<List<TransitAction>>
}

interface SettingsRepository {
    val isDarkMode: Flow<Boolean>
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setSyncInterval(minutes: Int)
}

interface TicketRepository {
    suspend fun scanTicket(ticketCode: String): Result<Ticket>
    fun observeScannedTickets(): Flow<List<Ticket>>
    suspend fun getTicketByCode(code: String): Ticket?
    fun getStadiumSections(): List<StadiumSection>
}
