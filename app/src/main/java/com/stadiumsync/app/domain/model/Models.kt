package com.stadiumsync.app.domain.model

import kotlinx.serialization.Serializable

// ─── Match ───────────────────────────────────────────────
@Serializable
data class Match(
    val id: String,
    val team1: String,
    val team2: String,
    val venue: String,
    val score1: String = "0/0",
    val score2: String = "",
    val overs1: String = "0.0",
    val overs2: String = "",
    val currentRunRate: Double = 0.0,
    val requiredRunRate: Double = 0.0,
    val matchPhase: MatchPhase = MatchPhase.PRE_MATCH,
    val status: MatchStatus = MatchStatus.UPCOMING,
    val format: MatchFormat = MatchFormat.T20,
    val startTime: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
enum class MatchPhase { PRE_MATCH, POWERPLAY, MIDDLE_OVERS, DEATH_OVERS, INNINGS_BREAK, SECOND_INNINGS, COMPLETED }

@Serializable
enum class MatchStatus { UPCOMING, LIVE, COMPLETED, DELAYED, ABANDONED }

@Serializable
enum class MatchFormat { T20, ODI, TEST }

// ─── Prediction ──────────────────────────────────────────
@Serializable
data class MatchPrediction(
    val estimatedEndTime: Long = 0L,
    val confidencePercent: Int = 0,
    val crowdReleaseScore: Double = 0.0,
    val transitUrgencyScore: Double = 0.0,
    val estimatedMinutesLeft: Int = 0,
    val probabilityDistribution: List<Double> = emptyList()
)

// ─── Crowd ───────────────────────────────────────────────
@Serializable
data class CrowdPressure(
    val gateId: String,
    val gateName: String,
    val pressureLevel: PressureLevel = PressureLevel.LOW,
    val densityPercent: Int = 0,
    val trend: PressureTrend = PressureTrend.STABLE,
    val estimatedPeople: Int = 0
)

@Serializable
data class CrowdHeatmapPoint(val x: Float, val y: Float, val density: Float)

@Serializable
enum class PressureLevel { LOW, MODERATE, HIGH, CRITICAL }

@Serializable
enum class PressureTrend { RISING, STABLE, FALLING }

// ─── Transit ─────────────────────────────────────────────
@Serializable
data class TransitRoute(
    val id: String,
    val name: String,
    val type: TransitType,
    val status: TransitRouteStatus = TransitRouteStatus.ACTIVE,
    val capacity: Int = 0,
    val currentLoad: Int = 0,
    val estimatedDelayMin: Int = 0,
    val nextArrivalMin: Int = 0,
    val frequencyMin: Int = 10,
    val platform: String = "",
    val terminalStation: String = "",
    val operatorName: String = "",
    val vehicleCount: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
enum class TransitType { METRO, BUS, SHUTTLE, RIDE_PICKUP }

@Serializable
enum class TransitRouteStatus { ACTIVE, DELAYED, HELD, DIVERTED, OFFLINE }

@Serializable
data class TransitAction(
    val id: String,
    val actionType: TransitActionType,
    val routeId: String,
    val routeName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val operatorName: String = "",
    val status: ActionStatus = ActionStatus.PENDING,
    val description: String = "",
    val isAutoTriggered: Boolean = false
)

@Serializable
enum class TransitActionType { HOLD_METRO, DISPATCH_BUS, OPEN_ALTERNATE_ZONE, DIVERT_ROUTE, SEND_CROWD_ALERT }

@Serializable
enum class ActionStatus { PENDING, EXECUTED, FAILED, CANCELLED, SYNCED }

@Serializable
data class TransitRecommendation(
    val id: String,
    val actionType: TransitActionType,
    val reason: String,
    val priority: AlertPriority = AlertPriority.INFO,
    val routeId: String = "",
    val routeName: String = "",
    val autoTriggered: Boolean = false,
    val estimatedImpact: String = "",
    val affectedPassengers: Int = 0,
    val confidence: Int = 85
)

// ─── Live Transit Vehicle ─────────────────────────────────
@Serializable
data class TransitVehicle(
    val id: String,
    val vehicleNumber: String,
    val routeId: String,
    val routeName: String,
    val type: TransitType,
    val status: VehicleStatus,
    val passengerCount: Int,
    val capacity: Int,
    val speedKmh: Float,
    val latitude: Float,   // 0..1 normalized for schematic
    val longitude: Float,  // 0..1 normalized for schematic
    val nextStopName: String,
    val etaMinutes: Int,
    val driverName: String = "",
    val lastPing: Long = System.currentTimeMillis()
)

@Serializable
enum class VehicleStatus { EN_ROUTE, AT_STOP, DELAYED, BREAKDOWN, IDLE }

// ─── Transit Incident ─────────────────────────────────────
@Serializable
data class TransitIncident(
    val id: String,
    val title: String,
    val description: String,
    val severity: IncidentSeverity,
    val location: String,
    val affectedRoutes: List<String>,
    val reportedAt: Long,
    val estimatedResolutionMin: Int,
    val isResolved: Boolean = false,
    val type: IncidentType
)

@Serializable
enum class IncidentSeverity { LOW, MEDIUM, HIGH, CRITICAL }

@Serializable
enum class IncidentType { ROAD_BLOCK, METRO_FAULT, CROWD_OVERFLOW, ACCIDENT, WEATHER, VEHICLE_BREAKDOWN }

// ─── Transit Station ─────────────────────────────────────
@Serializable
data class TransitStation(
    val id: String,
    val name: String,
    val type: TransitType,
    val platformCount: Int,
    val platformCapacity: Int,
    val currentOccupancy: Int,
    val nextArrivalMin: Int,
    val nextArrivalRoute: String,
    val distanceFromStadiumM: Int,
    val walkingTimeMin: Int,
    val latitude: Float,
    val longitude: Float
)

// ─── Smart Transit Suggestion ──────────────────────────────
@Serializable
data class TransitSuggestion(
    val id: String,
    val fromLocation: String,
    val toLocation: String,
    val transitType: TransitType,
    val routeName: String,
    val estimatedTimeMin: Int,
    val walkingTimeMin: Int,
    val crowdLevel: PressureLevel,
    val isRecommended: Boolean,
    val aiScore: Int,                // 0-100
    val fareRupees: Int,
    val co2GramsPerKm: Int,
    val reliabilityPercent: Int,
    val headwayMin: Int,             // minutes between vehicles
    val nextDepartureMin: Int,
    val waypoints: List<RouteWaypoint>,
    val tags: List<String>           // "Fastest", "Eco", "Budget", etc.
)

@Serializable
data class RouteWaypoint(
    val label: String,
    val type: WaypointType,
    val walkingDistanceM: Int = 0,
    val etaFromStart: Int = 0
)

@Serializable
enum class WaypointType { START, TRANSIT_STOP, TRANSFER, DESTINATION }

// ─── Route ETA ────────────────────────────────────────────
@Serializable
data class RouteETA(
    val routeId: String,
    val platform: String,
    val arrivals: List<ArrivalInfo>
)

@Serializable
data class ArrivalInfo(
    val vehicleId: String,
    val etaMinutes: Int,
    val capacity: Int,
    val currentLoad: Int,
    val platform: String
)

// ─── Route Suggestion (legacy compat) ────────────────────
@Serializable
data class RouteSuggestion(
    val id: String,
    val fromLocation: String,
    val toLocation: String,
    val transitType: TransitType,
    val estimatedTimeMin: Int,
    val crowdLevel: PressureLevel,
    val isRecommended: Boolean = false,
    val waypoints: List<RoutePoint> = emptyList()
)

@Serializable
data class RoutePoint(val lat: Double, val lng: Double, val label: String = "")

// ─── Alerts & Notifications ─────────────────────────────
@Serializable
data class StadiumAlert(
    val id: String,
    val title: String,
    val message: String,
    val priority: AlertPriority = AlertPriority.INFO,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: AlertType = AlertType.GENERAL,
    val isDelivered: Boolean = true
)

@Serializable
enum class AlertPriority { CRITICAL, WARNING, INFO }

@Serializable
enum class AlertType { CROWD_SURGE, MATCH_ENDING, TRANSPORT_DELAY, NETWORK_FAILURE, SYNC_PENDING, GENERAL, WICKET, MILESTONE, RAIN_DELAY }

// ─── User ────────────────────────────────────────────────
@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.OPERATOR,
    val token: String = "",
    val department: String = "",
    val badgeId: String = "",
    val phoneNumber: String = ""
)

@Serializable
enum class UserRole { ADMIN, OPERATOR, VIEWER, TRANSIT_OFFICER }

// ─── Auth ─────────────────────────────────────────────────
enum class AuthError { INVALID_CREDENTIALS, ACCOUNT_LOCKED, NETWORK_ERROR, VALIDATION_ERROR, EMAIL_ALREADY_EXISTS }

data class AuthCredential(
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val name: String,
    val badgeId: String = "",
    val department: String = ""
)

// ─── Sync & Analytics ────────────────────────────────────
@Serializable
data class SyncStatus(
    val pendingCount: Int = 0,
    val lastSyncTime: Long = 0L,
    val isOnline: Boolean = true,
    val syncHealthPercent: Int = 100
)

@Serializable
data class AnalyticsSummary(
    val totalCrowdPressure: Double = 0.0,
    val predictedPassengerWave: Int = 0,
    val transportActionsCount: Int = 0,
    val syncHealthPercent: Int = 100,
    val offlineQueueCount: Int = 0,
    val notificationDeliveryRate: Double = 100.0,
    val peakCrowdTime: String = "",
    val totalTransitCapacity: Int = 0,
    val avgResponseTimeMs: Long = 0L
)

// ─── Ticket & Seat Navigator ────────────────────────────
@Serializable
enum class Pavilion(val displayName: String, val shortName: String) {
    NORTH_STAND("North Stand", "N"),
    SOUTH_STAND("South Stand", "S"),
    EAST_PAVILION("East Pavilion", "E"),
    WEST_PAVILION("West Pavilion", "W"),
    VIP_ENCLOSURE("VIP Enclosure", "VIP"),
    CORPORATE_BOX("Corporate Box", "CB"),
    MEMBERS_STAND("Members Stand", "M"),
    GENERAL_STAND("General Stand", "G")
}

@Serializable
enum class TicketStatus { VALID, ALREADY_SCANNED, EXPIRED, INVALID }

@Serializable
data class Ticket(
    val ticketId: String,
    val holderName: String,
    val pavilion: Pavilion,
    val section: String,
    val seatRow: String,
    val seatNumber: Int,
    val gate: String,
    val matchId: String = "match_001",
    val status: TicketStatus = TicketStatus.VALID,
    val scannedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SeatInfo(
    val pavilion: Pavilion,
    val section: String,
    val row: String,
    val seatNumber: Int,
    val isOccupied: Boolean = false,
    val isSelected: Boolean = false
)

@Serializable
data class StadiumSection(
    val pavilion: Pavilion,
    val sectionId: String,
    val sectionName: String,
    val totalSeats: Int,
    val occupiedSeats: Int,
    val gateAccess: String
)

// ─── Stadium Map POI ────────────────────────────────────
@Serializable
enum class POIType(val emoji: String, val label: String) {
    GATE("🚪", "Gate"),
    FOOD_COURT("🍔", "Food Court"),
    EXIT("🚶", "Exit"),
    RESTROOM("🚻", "Restroom"),
    FIRST_AID("🏥", "First Aid"),
    MERCHANDISE("🛍️", "Merch Shop"),
    ATM("🏧", "ATM"),
    PARKING("🅿️", "Parking")
}

@Serializable
data class StadiumPOI(
    val id: String,
    val name: String,
    val type: POIType,
    val angleDeg: Float,
    val distanceRatio: Float = 0.85f,
    val nearestPavilion: Pavilion
)
