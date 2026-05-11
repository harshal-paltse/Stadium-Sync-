package com.stadiumsync.app.data.repository

import com.stadiumsync.app.core.datastore.UserPreferences
import com.stadiumsync.app.core.network.NetworkMonitor
import com.stadiumsync.app.data.local.dao.*
import com.stadiumsync.app.data.local.entity.*
import com.stadiumsync.app.data.remote.mock.*
import com.stadiumsync.app.domain.model.*
import com.stadiumsync.app.domain.repository.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MatchRepositoryImpl @Inject constructor(
    private val matchDao: MatchDao, private val predictionDao: PredictionDao,
    private val mockMatch: MockMatchService, private val networkMonitor: NetworkMonitor
) : MatchRepository {
    override fun observeLiveMatch(): Flow<Match?> =
        if (networkMonitor.isOnline.value) mockMatch.getLiveMatch().onEach { m -> matchDao.upsert(m.toEntity()); predictionDao.upsert(mockMatch.getPrediction(m).toEntity(m.id)) }
        else matchDao.observeLive().map { it?.toDomain() }
    override fun observePrediction(matchId: String) = predictionDao.observe(matchId).map { it?.toDomain() }
    override suspend fun refreshMatch() {}
}

@Singleton
class CrowdRepositoryImpl @Inject constructor(
    private val crowdDao: CrowdDataDao, private val mockCrowd: MockCrowdService, private val networkMonitor: NetworkMonitor
) : CrowdRepository {
    override fun observeCrowdPressure() = crowdDao.observeAll().map { l -> l.map { it.toDomain() } }
    override fun getHeatmapPoints() = mockCrowd.getHeatmapPoints()
    override suspend fun refreshCrowdData(matchPhase: MatchPhase) { crowdDao.upsertAll(mockCrowd.getCrowdPressure(matchPhase).map { it.toEntity() }) }
}

@Singleton
class TransitRepositoryImpl @Inject constructor(
    private val routeDao: TransitRouteDao, private val actionDao: TransitActionDao,
    private val auditDao: AuditLogDao, private val syncQueueDao: SyncQueueDao,
    private val mockTransit: MockTransitService, private val networkMonitor: NetworkMonitor
) : TransitRepository {
    override fun observeRoutes() = flow {
        while (true) {
            val routes = mockTransit.getTransitRoutes().map { r ->
                r.copy(currentLoad = (r.currentLoad + Random.nextInt(-20, 30)).coerceIn(0, r.capacity),
                    estimatedDelayMin = if (r.status == TransitRouteStatus.DELAYED) (r.estimatedDelayMin + Random.nextInt(-2, 4)).coerceAtLeast(0) else r.estimatedDelayMin)
            }
            routeDao.upsertAll(routes.map { it.toEntity() })
            emit(routes)
            delay(15_000)
        }
    }
    override fun observeActions() = actionDao.observeAll().map { l -> l.map { it.toDomain() } }
    override fun observeVehicles(): Flow<List<TransitVehicle>> = flow {
        val vehicles = mockTransit.getTransitVehicles().toMutableList()
        while (true) {
            val updated = vehicles.mapIndexed { i, v ->
                v.copy(
                    latitude = (v.latitude + Random.nextFloat() * 0.008f - 0.004f).coerceIn(0f, 1f),
                    longitude = (v.longitude + Random.nextFloat() * 0.008f - 0.004f).coerceIn(0f, 1f),
                    passengerCount = (v.passengerCount + Random.nextInt(-5, 10)).coerceIn(0, v.capacity),
                    speedKmh = if (v.status == VehicleStatus.AT_STOP) 0f else (v.speedKmh + Random.nextFloat() * 10f - 5f).coerceIn(0f, 90f),
                    etaMinutes = (v.etaMinutes + Random.nextInt(-1, 2)).coerceAtLeast(0),
                    lastPing = System.currentTimeMillis()
                )
            }
            emit(updated)
            delay(5_000)
        }
    }
    override fun observeIncidents(): Flow<List<TransitIncident>> = flow {
        while (true) { emit(mockTransit.getIncidents()); delay(30_000) }
    }
    override fun getStations() = mockTransit.getStations()
    override suspend fun executeAction(action: TransitAction) {
        actionDao.insert(action.copy(status = ActionStatus.EXECUTED).toEntity())
        auditDao.insert(AuditLogEntity(action = action.actionType.name, details = action.description, operatorName = action.operatorName))
        if (!networkMonitor.isOnline.value) syncQueueDao.insert(SyncQueueEntity(actionType = "TRANSIT_ACTION", payload = action.id))
    }
    override fun getRecommendations(crowdPressure: List<CrowdPressure>) = mockTransit.getRecommendations(crowdPressure)
    override fun getRouteSuggestions() = mockTransit.getRouteSuggestions()
    override fun getSmartSuggestions(from: String, to: String, type: TransitType?) = mockTransit.getSmartSuggestions(from, to, type)
    override fun getRouteETA(routeId: String) = mockTransit.getRouteETA(routeId)
    override suspend fun refreshRoutes() { routeDao.upsertAll(mockTransit.getTransitRoutes().map { it.toEntity() }) }
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val prefs: UserPreferences,
    private val authService: MockAuthService
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<User> {
        val result = authService.login(email, password)
        result.onSuccess { user -> prefs.saveUserSession(user.token, user.name, user.role.name, user.email, user.badgeId, user.department) }
        return result
    }
    override suspend fun register(name: String, email: String, password: String, role: UserRole, badgeId: String, department: String): Result<User> {
        val result = authService.register(name, email, password, role, badgeId, department)
        result.onSuccess { user -> prefs.saveUserSession(user.token, user.name, user.role.name, user.email, user.badgeId, user.department) }
        return result
    }
    override suspend fun forgotPassword(email: String) = authService.forgotPassword(email)
    override suspend fun logout() { prefs.clearSession() }
    override fun isLoggedIn() = prefs.preferences.map { it.cachedUserToken.isNotEmpty() }
    override fun getCurrentUser() = prefs.preferences.map { p ->
        if (p.cachedUserToken.isNotEmpty()) User(name = p.cachedUserName, email = p.cachedUserEmail, role = runCatching { UserRole.valueOf(p.cachedUserRole) }.getOrDefault(UserRole.OPERATOR), token = p.cachedUserToken, badgeId = p.cachedUserBadgeId, department = p.cachedUserDepartment) else null
    }
    override fun getFailedAttempts(email: String) = authService.failedCount(email)
    override fun isAccountLocked(email: String) = authService.isLocked(email)
    override fun getLockoutRemainingSeconds(email: String) = authService.lockoutRemaining(email)
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(private val dao: NotificationDao) : NotificationRepository {
    override fun observeNotifications() = dao.observeAll().map { l -> l.map { it.toDomain() } }
    override fun observeUnreadCount() = dao.observeUnreadCount()
    override suspend fun addNotification(alert: StadiumAlert) = dao.insert(alert.toEntity())
    override suspend fun markRead(id: String) = dao.markRead(id)
    override suspend fun markAllRead() = dao.markAllRead()
    override suspend fun clearAll() = dao.clearAll()
}

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncQueueDao: SyncQueueDao, private val prefs: UserPreferences, private val networkMonitor: NetworkMonitor
) : SyncRepository {
    override fun observeSyncStatus() = combine(syncQueueDao.observePendingCount(), prefs.preferences, networkMonitor.isOnline) { pending, p, online ->
        SyncStatus(pending, p.lastSyncTimestamp, online, if (online && pending == 0) 100 else if (online) 80 else 50)
    }
    override suspend fun queueAction(actionType: String, payload: String) = syncQueueDao.insert(SyncQueueEntity(actionType = actionType, payload = payload))
    override suspend fun processPendingSync(): Result<Int> {
        val pending = syncQueueDao.getPending()
        pending.forEach { syncQueueDao.updateStatus(it.id, "SYNCED") }
        prefs.setLastSync(System.currentTimeMillis()); syncQueueDao.clearSynced()
        return Result.success(pending.size)
    }
    override suspend fun getLastSyncTime() = 0L
}

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(private val mock: MockAnalyticsService, private val actionDao: TransitActionDao) : AnalyticsRepository {
    override fun getAnalyticsSummary() = mock.getSummary()
    override fun observeAuditLog() = actionDao.observeAll().map { l -> l.map { it.toDomain() } }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(private val prefs: UserPreferences) : SettingsRepository {
    override val isDarkMode = prefs.isDarkMode
    override suspend fun setDarkMode(enabled: Boolean) { prefs.setDarkMode(enabled) }
    override suspend fun setNotificationsEnabled(enabled: Boolean) { prefs.setNotifications(enabled) }
    override suspend fun setSyncInterval(minutes: Int) { prefs.setSyncInterval(minutes) }
}

// ─── Mappers ─────────────────────────────────────────────
fun Match.toEntity() = MatchEntity(id,team1,team2,venue,score1,score2,overs1,overs2,currentRunRate,requiredRunRate,matchPhase.name,status.name,format.name,startTime,lastUpdated)
fun MatchEntity.toDomain() = Match(id,team1,team2,venue,score1,score2,overs1,overs2,currentRunRate,requiredRunRate,MatchPhase.valueOf(matchPhase),MatchStatus.valueOf(status),MatchFormat.valueOf(format),startTime,lastUpdated)
fun MatchPrediction.toEntity(matchId: String) = PredictionEntity(matchId,estimatedEndTime,confidencePercent,crowdReleaseScore,transitUrgencyScore,estimatedMinutesLeft)
fun PredictionEntity.toDomain() = MatchPrediction(estimatedEndTime,confidencePercent,crowdReleaseScore,transitUrgencyScore,estimatedMinutesLeft)
fun CrowdPressure.toEntity() = CrowdDataEntity(gateId,gateName,pressureLevel.name,densityPercent,trend.name,estimatedPeople)
fun CrowdDataEntity.toDomain() = CrowdPressure(gateId,gateName,PressureLevel.valueOf(pressureLevel),densityPercent,PressureTrend.valueOf(trend),estimatedPeople)
fun TransitRoute.toEntity() = TransitRouteEntity(id,name,type.name,status.name,capacity,currentLoad,estimatedDelayMin)
fun TransitRouteEntity.toDomain() = TransitRoute(id,name,TransitType.valueOf(type),TransitRouteStatus.valueOf(status),capacity,currentLoad,estimatedDelayMin,lastUpdated=lastUpdated)
fun TransitAction.toEntity() = TransitActionEntity(id,actionType.name,routeId,routeName,timestamp,operatorName,status.name,description,isAutoTriggered)
fun TransitActionEntity.toDomain() = TransitAction(id,TransitActionType.valueOf(actionType),routeId,routeName,timestamp,operatorName,ActionStatus.valueOf(status),description,isAutoTriggered)
fun StadiumAlert.toEntity() = NotificationEntity(id,title,message,priority.name,timestamp,isRead,type.name,isDelivered)
fun NotificationEntity.toDomain() = StadiumAlert(id,title,message,AlertPriority.valueOf(priority),timestamp,isRead,AlertType.valueOf(type),isDelivered)
fun Ticket.toEntity() = TicketEntity(ticketId,holderName,pavilion.name,section,seatRow,seatNumber,gate,matchId,status.name,scannedAt)
fun TicketEntity.toDomain() = Ticket(ticketId,holderName,Pavilion.valueOf(pavilion),section,seatRow,seatNumber,gate,matchId,TicketStatus.valueOf(status),scannedAt)

@Singleton
class TicketRepositoryImpl @Inject constructor(private val ticketDao: TicketDao, private val mockTicket: MockTicketService) : TicketRepository {
    override suspend fun scanTicket(ticketCode: String): Result<Ticket> = try {
        val t = mockTicket.scanTicket(ticketCode); ticketDao.insert(t.toEntity()); Result.success(t)
    } catch (e: Exception) { Result.failure(e) }
    override fun observeScannedTickets() = ticketDao.observeAll().map { l -> l.map { it.toDomain() } }
    override suspend fun getTicketByCode(code: String) = ticketDao.getByCode(code)?.toDomain()
    override fun getStadiumSections() = mockTicket.getStadiumSections()
}
