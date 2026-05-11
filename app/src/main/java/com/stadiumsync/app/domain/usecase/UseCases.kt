package com.stadiumsync.app.domain.usecase

import com.stadiumsync.app.domain.model.*
import com.stadiumsync.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLiveMatchUseCase @Inject constructor(private val repo: MatchRepository) {
    operator fun invoke(): Flow<Match?> = repo.observeLiveMatch()
}

class GetMatchPredictionUseCase @Inject constructor(private val repo: MatchRepository) {
    operator fun invoke(matchId: String): Flow<MatchPrediction?> = repo.observePrediction(matchId)
}

class GetCrowdPressureUseCase @Inject constructor(private val repo: CrowdRepository) {
    operator fun invoke(): Flow<List<CrowdPressure>> = repo.observeCrowdPressure()
}

class GetCrowdHeatmapUseCase @Inject constructor(private val repo: CrowdRepository) {
    operator fun invoke(): List<CrowdHeatmapPoint> = repo.getHeatmapPoints()
}

class RefreshCrowdDataUseCase @Inject constructor(private val repo: CrowdRepository) {
    suspend operator fun invoke(phase: MatchPhase) = repo.refreshCrowdData(phase)
}

class GetTransitRoutesUseCase @Inject constructor(private val repo: TransitRepository) {
    operator fun invoke(): Flow<List<TransitRoute>> = repo.observeRoutes()
}

class GetTransitActionsUseCase @Inject constructor(private val repo: TransitRepository) {
    operator fun invoke(): Flow<List<TransitAction>> = repo.observeActions()
}

class GetTransitVehiclesUseCase @Inject constructor(private val repo: TransitRepository) {
    operator fun invoke(): Flow<List<TransitVehicle>> = repo.observeVehicles()
}

class GetTransitIncidentsUseCase @Inject constructor(private val repo: TransitRepository) {
    operator fun invoke(): Flow<List<TransitIncident>> = repo.observeIncidents()
}

class GetTransitStationsUseCase @Inject constructor(private val repo: TransitRepository) {
    operator fun invoke(): List<TransitStation> = repo.getStations()
}

class ExecuteTransitActionUseCase @Inject constructor(private val repo: TransitRepository) {
    suspend operator fun invoke(action: TransitAction) = repo.executeAction(action)
}

class GetTransitRecommendationsUseCase @Inject constructor(private val repo: TransitRepository) {
    operator fun invoke(pressure: List<CrowdPressure>): List<TransitRecommendation> =
        repo.getRecommendations(pressure)
}

class GetRouteSuggestionsUseCase @Inject constructor(private val repo: TransitRepository) {
    operator fun invoke(): List<RouteSuggestion> = repo.getRouteSuggestions()
}

class GetSmartSuggestionsUseCase @Inject constructor(private val repo: TransitRepository) {
    operator fun invoke(from: String, to: String, type: TransitType? = null): List<TransitSuggestion> =
        repo.getSmartSuggestions(from, to, type)
}

class GetRouteETAUseCase @Inject constructor(private val repo: TransitRepository) {
    operator fun invoke(routeId: String): RouteETA = repo.getRouteETA(routeId)
}

class LoginUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        repo.login(email, password)
}

class RegisterUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(
        name: String, email: String, password: String,
        role: UserRole, badgeId: String, department: String
    ): Result<User> = repo.register(name, email, password, role, badgeId, department)
}

class ForgotPasswordUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> = repo.forgotPassword(email)
}

class LogoutUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.logout()
}

class GetCurrentUserUseCase @Inject constructor(private val repo: AuthRepository) {
    operator fun invoke(): Flow<User?> = repo.getCurrentUser()
}

class GetNotificationsUseCase @Inject constructor(private val repo: NotificationRepository) {
    operator fun invoke(): Flow<List<StadiumAlert>> = repo.observeNotifications()
}

class GetUnreadCountUseCase @Inject constructor(private val repo: NotificationRepository) {
    operator fun invoke(): Flow<Int> = repo.observeUnreadCount()
}

class GetSyncStatusUseCase @Inject constructor(private val repo: SyncRepository) {
    operator fun invoke(): Flow<SyncStatus> = repo.observeSyncStatus()
}

class SyncOfflineDataUseCase @Inject constructor(private val repo: SyncRepository) {
    suspend operator fun invoke(): Result<Int> = repo.processPendingSync()
}

class GetAnalyticsSummaryUseCase @Inject constructor(private val repo: AnalyticsRepository) {
    operator fun invoke(): AnalyticsSummary = repo.getAnalyticsSummary()
}

class ScanTicketUseCase @Inject constructor(private val repo: TicketRepository) {
    suspend operator fun invoke(ticketCode: String): Result<Ticket> = repo.scanTicket(ticketCode)
}

class GetScannedTicketsUseCase @Inject constructor(private val repo: TicketRepository) {
    operator fun invoke(): Flow<List<Ticket>> = repo.observeScannedTickets()
}

class GetStadiumSectionsUseCase @Inject constructor(private val repo: TicketRepository) {
    operator fun invoke(): List<StadiumSection> = repo.getStadiumSections()
}
