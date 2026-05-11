package com.stadiumsync.app.data.remote.mock

import com.stadiumsync.app.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MockMatchService @Inject constructor() {
    private var currentOver = 12.3; private var score = 98; private var wickets = 3
    fun getLiveMatch(): Flow<Match> = flow {
        while (true) {
            currentOver += 0.1
            if (currentOver.toString().split(".").last().toIntOrNull() ?: 0 > 5) currentOver = currentOver.toInt() + 1.0
            score += Random.nextInt(0, 7)
            if (Random.nextFloat() < 0.05f) wickets = (wickets + 1).coerceAtMost(10)
            val phase = when { currentOver <= 6.0 -> MatchPhase.POWERPLAY; currentOver <= 15.0 -> MatchPhase.MIDDLE_OVERS; currentOver <= 20.0 -> MatchPhase.DEATH_OVERS; else -> MatchPhase.COMPLETED }
            emit(Match("match_001","Mumbai Indians","Chennai Super Kings","Wankhede Stadium, Mumbai","$score/$wickets","",String.format("%.1f",currentOver.coerceAtMost(20.0)),"",if(currentOver>0) score/currentOver else 0.0,if(20-currentOver>0)(180-score)/(20-currentOver) else 0.0,phase,if(currentOver>=20.0) MatchStatus.COMPLETED else MatchStatus.LIVE,MatchFormat.T20,System.currentTimeMillis()-5400000L))
            delay(3000)
        }
    }
    fun getPrediction(match: Match): MatchPrediction {
        val ol = 20.0 - (match.overs1.toDoubleOrNull() ?: 0.0); val ml = (ol * 4.2).toInt()
        return MatchPrediction(System.currentTimeMillis()+ml*60_000L,if(ol<=2)95 else if(ol<=5)85 else if(ol<=10)70 else 55,((20.0-ol)/20.0*100).coerceIn(0.0,100.0),if(ol<=5)85.0 else if(ol<=10)50.0 else 20.0,ml,listOf(0.1,0.15,0.25,0.3,0.15,0.05))
    }
}

@Singleton
class MockCrowdService @Inject constructor() {
    private val gates = listOf("Gate A - North","Gate B - East","Gate C - South","Gate D - West","Gate E - VIP","Gate F - Metro Exit")
    fun getCrowdPressure(phase: MatchPhase): List<CrowdPressure> {
        val base = when(phase){ MatchPhase.DEATH_OVERS->75; MatchPhase.COMPLETED->95; MatchPhase.MIDDLE_OVERS->40; else->25 }
        return gates.mapIndexed { i,g -> val d=(base+Random.nextInt(-15,20)).coerceIn(0,100); CrowdPressure("gate_$i",g,if(d>=80)PressureLevel.CRITICAL else if(d>=60)PressureLevel.HIGH else if(d>=35)PressureLevel.MODERATE else PressureLevel.LOW,d,if(phase==MatchPhase.DEATH_OVERS||phase==MatchPhase.COMPLETED)PressureTrend.RISING else PressureTrend.STABLE,d*50) }
    }
    fun getHeatmapPoints(): List<CrowdHeatmapPoint> = buildList { for(i in 0..20) for(j in 0..20){ val d=Random.nextFloat()*0.4f+if(i in 8..12&&j in 8..12)0.6f else 0f; add(CrowdHeatmapPoint(i/20f,j/20f,d.coerceAtMost(1f))) } }
}

@Singleton
class MockTransitService @Inject constructor() {

    fun getTransitRoutes(): List<TransitRoute> = listOf(
        TransitRoute("metro_1","Metro Blue Line – Churchgate",TransitType.METRO,TransitRouteStatus.ACTIVE,2400,1800,0,3,7,"Platform 1","Churchgate","MMRC",12),
        TransitRoute("metro_2","Metro Line 2A – Andheri West",TransitType.METRO,TransitRouteStatus.HELD,2000,1950,8,11,10,"Platform 3","Andheri","MMRC",8),
        TransitRoute("metro_3","Metro Line 7 – Dahisar E",TransitType.METRO,TransitRouteStatus.ACTIVE,2000,1100,0,5,12,"Platform 2","Dahisar E","MMRC",6),
        TransitRoute("bus_1","BEST 83 – Worli Sea Face",TransitType.BUS,TransitRouteStatus.ACTIVE,60,45,0,4,15,"Bay 3","Worli","BEST",2),
        TransitRoute("bus_2","BEST 124 – Dadar TT",TransitType.BUS,TransitRouteStatus.DELAYED,60,58,12,16,20,"Bay 5","Dadar","BEST",2),
        TransitRoute("bus_3","BEST 302 – Bandra Stn",TransitType.BUS,TransitRouteStatus.ACTIVE,60,30,0,6,18,"Bay 1","Bandra","BEST",3),
        TransitRoute("shuttle_1","Shuttle A – Parking P1",TransitType.SHUTTLE,TransitRouteStatus.ACTIVE,30,25,0,2,8,"Bay S1","P1 Parking","Stadium",4),
        TransitRoute("shuttle_2","Shuttle B – Parking P2",TransitType.SHUTTLE,TransitRouteStatus.ACTIVE,30,10,0,6,8,"Bay S2","P2 Parking","Stadium",3),
        TransitRoute("shuttle_3","Shuttle C – VIP Drop",TransitType.SHUTTLE,TransitRouteStatus.ACTIVE,20,18,0,1,6,"Bay VIP","VIP Gate","Stadium",2),
        TransitRoute("ride_1","Pickup Alpha – Gate A",TransitType.RIDE_PICKUP,TransitRouteStatus.ACTIVE,200,120,3,0,0,"Zone A","Gate A","OLA/Uber",0),
        TransitRoute("ride_2","Pickup Beta – Gate D",TransitType.RIDE_PICKUP,TransitRouteStatus.ACTIVE,200,80,0,0,0,"Zone D","Gate D","OLA/Uber",0)
    )

    fun getTransitVehicles(): List<TransitVehicle> = listOf(
        TransitVehicle("v1","MH-01-BL-1121","metro_1","Metro Blue Line",TransitType.METRO,VehicleStatus.EN_ROUTE,320,400,72f,0.18f,0.55f,"Wankhede Station",3,"Suresh K."),
        TransitVehicle("v2","MH-01-BL-1122","metro_1","Metro Blue Line",TransitType.METRO,VehicleStatus.AT_STOP,400,400,0f,0.35f,0.55f,"Marine Lines",0,"Anil P."),
        TransitVehicle("v3","MH-01-2A-2201","metro_2","Metro Line 2A",TransitType.METRO,VehicleStatus.DELAYED,380,400,18f,0.62f,0.3f,"DN Nagar",9,"Ramesh V."),
        TransitVehicle("v4","MH-01-2A-2202","metro_2","Metro Line 2A",TransitType.METRO,VehicleStatus.EN_ROUTE,200,400,55f,0.78f,0.22f,"Andheri W",14,"Kavita M."),
        TransitVehicle("v5","MH-01-7-3301","metro_3","Metro Line 7",TransitType.METRO,VehicleStatus.EN_ROUTE,150,400,65f,0.5f,0.72f,"Aarey",5,"Deepak S."),
        TransitVehicle("v6","MH-04-GH-8831","bus_1","BEST 83",TransitType.BUS,VehicleStatus.EN_ROUTE,42,60,38f,0.2f,0.8f,"Worli Naka",4,"Mohan R."),
        TransitVehicle("v7","MH-04-GH-8832","bus_1","BEST 83",TransitType.BUS,VehicleStatus.AT_STOP,55,60,0f,0.08f,0.68f,"Gate A Bus Stop",0,"Pradeep T."),
        TransitVehicle("v8","MH-04-GH-1241","bus_2","BEST 124",TransitType.BUS,VehicleStatus.DELAYED,58,60,8f,0.42f,0.45f,"Dadar TT",16,"Santosh B."),
        TransitVehicle("v9","MH-04-GH-3021","bus_3","BEST 302",TransitType.BUS,VehicleStatus.EN_ROUTE,22,60,42f,0.65f,0.6f,"Bandra Stn",6,"Vijay N."),
        TransitVehicle("v10","SS-SHUT-A1","shuttle_1","Shuttle A",TransitType.SHUTTLE,VehicleStatus.EN_ROUTE,24,30,22f,0.88f,0.5f,"Parking P1",2,"Kiran D."),
        TransitVehicle("v11","SS-SHUT-A2","shuttle_1","Shuttle A",TransitType.SHUTTLE,VehicleStatus.AT_STOP,30,30,0f,0.94f,0.5f,"Gate A Drop",0,"Nitin P."),
        TransitVehicle("v12","SS-SHUT-B1","shuttle_2","Shuttle B",TransitType.SHUTTLE,VehicleStatus.IDLE,4,30,0f,0.9f,0.62f,"Parking P2",8,"Sanjay K."),
        TransitVehicle("v13","SS-VIP-C1","shuttle_3","Shuttle C VIP",TransitType.SHUTTLE,VehicleStatus.EN_ROUTE,18,20,25f,0.82f,0.42f,"VIP Gate",1,"Rajesh A."),
        TransitVehicle("v14","OLA-ALPHA-01","ride_1","Pickup Alpha",TransitType.RIDE_PICKUP,VehicleStatus.AT_STOP,1,4,0f,0.95f,0.38f,"Zone A",0,"Amit C."),
        TransitVehicle("v15","OLA-BETA-07","ride_2","Pickup Beta",TransitType.RIDE_PICKUP,VehicleStatus.EN_ROUTE,1,4,28f,0.8f,0.7f,"Zone D",3,"Ravi S.")
    )

    fun getIncidents(): List<TransitIncident> {
        val now = System.currentTimeMillis()
        return listOf(
            TransitIncident("inc1","Metro 2A Signal Fault","Automatic signaling system failure between DN Nagar and Azad Nagar. Single-line operation in effect.",IncidentSeverity.HIGH,"DN Nagar – Azad Nagar",listOf("metro_2"),now-1800000,25,false,IncidentType.METRO_FAULT),
            TransitIncident("inc2","Road Block – LBS Marg","Water pipeline burst causing partial road closure. BEST Route 124 diverted via Sion.",IncidentSeverity.MEDIUM,"LBS Marg, Kurla",listOf("bus_2"),now-3600000,45,false,IncidentType.ROAD_BLOCK),
            TransitIncident("inc3","Crowd Overflow – Gate A","Crowd density exceeding safe threshold at Gate A North. Flow control activated.",IncidentSeverity.CRITICAL,"Gate A – North Stand",listOf("shuttle_1","ride_1"),now-600000,15,false,IncidentType.CROWD_OVERFLOW),
            TransitIncident("inc4","Vehicle Breakdown – Shuttle A2","Shuttle A2 engine fault. Replacement vehicle dispatched from depot.",IncidentSeverity.LOW,"Parking P1 Entry",listOf("shuttle_1"),now-900000,20,false,IncidentType.VEHICLE_BREAKDOWN),
            TransitIncident("inc5","Heavy Rain – Pickup Zones","Waterlogging at pickup zones Beta and Gamma. Capacity reduced 40%.",IncidentSeverity.MEDIUM,"Gate D Pickup Zone",listOf("ride_2"),now-2700000,60,false,IncidentType.WEATHER)
        )
    }

    fun getStations(): List<TransitStation> = listOf(
        TransitStation("st1","Wankhede Metro Station",TransitType.METRO,3,600,480,3,"Metro Blue Line",200,3,0.35f,0.55f),
        TransitStation("st2","Marine Lines Station",TransitType.METRO,2,500,310,8,"Metro Blue Line",850,12,0.18f,0.55f),
        TransitStation("st3","DN Nagar Station",TransitType.METRO,2,400,380,11,"Metro Line 2A",1200,16,0.62f,0.3f),
        TransitStation("st4","Gate A Bus Stop",TransitType.BUS,1,200,155,4,"BEST 83",80,1,0.08f,0.68f),
        TransitStation("st5","Dadar TT Bus Stand",TransitType.BUS,4,800,620,16,"BEST 124",3200,22,0.42f,0.45f),
        TransitStation("st6","Parking P1 Shuttle Bay",TransitType.SHUTTLE,2,120,95,2,"Shuttle A",500,7,0.88f,0.5f),
        TransitStation("st7","Pickup Zone Alpha",TransitType.RIDE_PICKUP,1,300,118,0,"OLA/Uber",100,1,0.95f,0.38f),
        TransitStation("st8","VIP Gate Drop",TransitType.SHUTTLE,1,50,18,1,"Shuttle C VIP",150,2,0.82f,0.42f)
    )

    fun getSmartSuggestions(from: String, to: String, type: TransitType?): List<TransitSuggestion> {
        val all = listOf(
            TransitSuggestion("sg1",from.ifBlank{"Gate A"},to.ifBlank{"Churchgate"},TransitType.METRO,"Metro Blue Line",12,3,PressureLevel.LOW,true,94,10,28,96,7,3,listOf(RouteWaypoint("Gate A",WaypointType.START,0,0),RouteWaypoint("Wankhede Metro",WaypointType.TRANSIT_STOP,350,3),RouteWaypoint("Marine Lines",WaypointType.TRANSIT_STOP,0,8),RouteWaypoint("Churchgate",WaypointType.DESTINATION,200,12)),listOf("Fastest","Eco","Recommended")),
            TransitSuggestion("sg2",from.ifBlank{"Gate C"},to.ifBlank{"Dadar Station"},TransitType.BUS,"BEST Route 83",28,5,PressureLevel.MODERATE,false,71,8,82,78,15,4,listOf(RouteWaypoint("Gate C",WaypointType.START,0,0),RouteWaypoint("Worli Naka",WaypointType.TRANSIT_STOP,450,5),RouteWaypoint("Dadar Station",WaypointType.DESTINATION,300,28)),listOf("Budget")),
            TransitSuggestion("sg3",from.ifBlank{"Gate D"},to.ifBlank{"BKC Hub"},TransitType.SHUTTLE,"Stadium Shuttle B",18,2,PressureLevel.LOW,true,88,0,0,92,8,6,listOf(RouteWaypoint("Gate D",WaypointType.START,0,0),RouteWaypoint("Shuttle Bay S2",WaypointType.TRANSIT_STOP,180,2),RouteWaypoint("BKC Hub",WaypointType.DESTINATION,250,18)),listOf("Comfortable","Free")),
            TransitSuggestion("sg4",from.ifBlank{"Gate B"},to.ifBlank{"Andheri Metro"},TransitType.METRO,"Metro Line 2A → 7",38,4,PressureLevel.HIGH,false,62,20,28,72,10,11,listOf(RouteWaypoint("Gate B",WaypointType.START,0,0),RouteWaypoint("DN Nagar",WaypointType.TRANSIT_STOP,600,8),RouteWaypoint("Andheri W",WaypointType.TRANSFER,100,22),RouteWaypoint("Andheri Metro",WaypointType.DESTINATION,200,38)),listOf("Delayed Risk")),
            TransitSuggestion("sg5",from.ifBlank{"Gate A"},to.ifBlank{"Marine Drive"},TransitType.RIDE_PICKUP,"OLA/Uber Pickup Alpha",8,1,PressureLevel.MODERATE,false,78,180,0,70,0,0,listOf(RouteWaypoint("Gate A",WaypointType.START,0,0),RouteWaypoint("Pickup Zone Alpha",WaypointType.TRANSIT_STOP,100,1),RouteWaypoint("Marine Drive",WaypointType.DESTINATION,0,8)),listOf("Door-to-Door"))
        )
        return if (type != null) all.filter { it.transitType == type } else all
    }

    fun getRecommendations(crowdPressure: List<CrowdPressure>): List<TransitRecommendation> {
        val recs = mutableListOf<TransitRecommendation>()
        val maxP = crowdPressure.maxByOrNull { it.densityPercent }
        val avgDensity = if (crowdPressure.isEmpty()) 0 else crowdPressure.sumOf { it.densityPercent } / crowdPressure.size
        if (maxP != null && maxP.densityPercent > 70) {
            recs.add(TransitRecommendation("rec_1",TransitActionType.HOLD_METRO,"Crowd surge at ${maxP.gateName} (${maxP.densityPercent}%). Hold Metro Blue Line 5 min to stagger passenger wave.",AlertPriority.CRITICAL,"metro_1","Metro Blue Line",true,"Reduces gate pressure by ~35%",maxP.estimatedPeople,92))
            recs.add(TransitRecommendation("rec_2",TransitActionType.DISPATCH_BUS,"Dispatch 3 reserve BEST buses to Gate A and Gate C to absorb overflow.",AlertPriority.WARNING,"bus_1","BEST 83",false,"Handles ~180 additional passengers",180,85))
        }
        if (avgDensity > 50) {
            recs.add(TransitRecommendation("rec_3",TransitActionType.OPEN_ALTERNATE_ZONE,"Activate Pickup Zone Gamma (Gate F) to distribute rideshare demand.",AlertPriority.WARNING,"ride_2","Pickup Zone Beta",false,"Reduces Zone A wait by ~40%",320,78))
            recs.add(TransitRecommendation("rec_4",TransitActionType.DIVERT_ROUTE,"Divert BEST 124 via Hindmata to bypass LBS Marg road block.",AlertPriority.WARNING,"bus_2","BEST 124",true,"Saves 12 min delay for 58 passengers",58,88))
        }
        recs.add(TransitRecommendation("rec_5",TransitActionType.SEND_CROWD_ALERT,"Broadcast crowd advisory via app to encourage early departure from South Stand.",AlertPriority.INFO,"","All Routes",false,"Expected 15% stagger in crowd release",2000,70))
        return recs
    }

    fun getRouteSuggestions(): List<RouteSuggestion> = listOf(
        RouteSuggestion("r1","Gate A","Churchgate Station",TransitType.METRO,12,PressureLevel.LOW,true),
        RouteSuggestion("r2","Gate C","Dadar Station",TransitType.BUS,25,PressureLevel.MODERATE,false),
        RouteSuggestion("r3","Gate D","BKC Hub",TransitType.SHUTTLE,18,PressureLevel.LOW,true),
        RouteSuggestion("r4","Gate B","Marine Drive Pickup",TransitType.RIDE_PICKUP,8,PressureLevel.HIGH,false),
        RouteSuggestion("r5","Gate F","Andheri Metro",TransitType.METRO,30,PressureLevel.MODERATE,true)
    )

    fun getRouteETA(routeId: String): RouteETA {
        val platform = when(routeId){ "metro_1"->"Platform 1"; "metro_2"->"Platform 3"; else->"Bay 1" }
        return RouteETA(routeId, platform, listOf(
            ArrivalInfo("v_next1", 3, 400, 280, platform),
            ArrivalInfo("v_next2", 13, 400, 150, platform),
            ArrivalInfo("v_next3", 23, 400, 50, platform)
        ))
    }
}

@Singleton
class MockAnalyticsService @Inject constructor() {
    fun getSummary(): AnalyticsSummary = AnalyticsSummary(67.5,32000,14,94,3,97.2,"21:45",5200,340L)
}

@Singleton
class MockTicketService @Inject constructor() {
    private val scannedCodes = mutableSetOf<String>()
    private val names = listOf("Rahul Sharma","Priya Patel","Amit Kumar","Sneha Desai","Vikram Singh","Ananya Gupta","Rohan Mehta","Kavita Joshi","Arjun Nair","Meera Reddy","Suresh Iyer","Deepika Rao","Rajesh Verma","Pooja Mishra","Karan Malhotra","Neha Kapoor")
    fun scanTicket(code: String): Ticket {
        val already = code in scannedCodes; scannedCodes.add(code)
        val h = code.hashCode().let { if(it<0)-it else it }
        val p = Pavilion.entries[h % Pavilion.entries.size]
        val gate = when(p){ Pavilion.NORTH_STAND->"Gate A - North"; Pavilion.SOUTH_STAND->"Gate C - South"; Pavilion.EAST_PAVILION->"Gate B - East"; Pavilion.WEST_PAVILION->"Gate D - West"; Pavilion.VIP_ENCLOSURE,Pavilion.CORPORATE_BOX->"Gate E - VIP"; Pavilion.MEMBERS_STAND->"Gate F - Members"; Pavilion.GENERAL_STAND->"Gate A - North" }
        return Ticket(code,names[h%names.size],p,"${p.shortName}-${(h%4)+1}","${('A'+(h%26))}",(h%40)+1,gate,"match_001",if(already)TicketStatus.ALREADY_SCANNED else TicketStatus.VALID,System.currentTimeMillis())
    }
    fun getStadiumSections(): List<StadiumSection> = listOf(
        StadiumSection(Pavilion.NORTH_STAND,"N-1","North Stand A",2500,2100,"Gate A"),
        StadiumSection(Pavilion.NORTH_STAND,"N-2","North Stand B",2500,1800,"Gate A"),
        StadiumSection(Pavilion.SOUTH_STAND,"S-1","South Stand A",2500,2300,"Gate C"),
        StadiumSection(Pavilion.SOUTH_STAND,"S-2","South Stand B",2500,1950,"Gate C"),
        StadiumSection(Pavilion.EAST_PAVILION,"E-1","East Pavilion A",3000,2700,"Gate B"),
        StadiumSection(Pavilion.WEST_PAVILION,"W-1","West Pavilion A",3000,2850,"Gate D"),
        StadiumSection(Pavilion.VIP_ENCLOSURE,"VIP-1","VIP Box",500,420,"Gate E"),
        StadiumSection(Pavilion.CORPORATE_BOX,"CB-1","Corporate Suite",300,280,"Gate E"),
        StadiumSection(Pavilion.MEMBERS_STAND,"M-1","Members Enclosure",2000,1700,"Gate F"),
        StadiumSection(Pavilion.GENERAL_STAND,"G-1","General Gallery",4000,3600,"Gate A")
    )
}

// ─── Auth Service ─────────────────────────────────────────
@Singleton
class MockAuthService @Inject constructor() {
    private val failedAttempts = mutableMapOf<String, Int>()
    private val lockoutUntil = mutableMapOf<String, Long>()
    private val registeredUsers = mutableListOf(
        AuthCredential("admin@stadiumsync.in", hash("Admin@123"), UserRole.ADMIN, "Admin User", "ADM-001", "Management"),
        AuthCredential("operator@stadiumsync.in", hash("Op3r@t0r"), UserRole.OPERATOR, "Ops Controller", "OPS-042", "Operations"),
        AuthCredential("transit@stadiumsync.in", hash("Tr4ns!t"), UserRole.TRANSIT_OFFICER, "Transit Officer", "TRN-007", "Transit"),
        AuthCredential("viewer@stadiumsync.in", hash("View3r#"), UserRole.VIEWER, "Stadium Viewer", "VWR-100", "Public")
    )

    fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isLocked(email: String): Boolean {
        val until = lockoutUntil[email] ?: return false
        return System.currentTimeMillis() < until
    }

    fun lockoutRemaining(email: String): Int {
        val until = lockoutUntil[email] ?: return 0
        val rem = (until - System.currentTimeMillis()) / 1000
        return if (rem > 0) rem.toInt() else 0
    }

    fun failedCount(email: String) = failedAttempts[email] ?: 0

    fun login(email: String, password: String): Result<User> {
        if (isLocked(email)) return Result.failure(Exception("LOCKED:${lockoutRemaining(email)}"))
        val cred = registeredUsers.find { it.email.equals(email, ignoreCase = true) }
            ?: return Result.failure(Exception("INVALID_CREDENTIALS"))
        return if (cred.passwordHash == hash(password)) {
            failedAttempts.remove(email); lockoutUntil.remove(email)
            val token = "tok_${UUID.randomUUID()}"
            Result.success(User(UUID.randomUUID().toString(), cred.name, cred.email, cred.role, token, cred.department, cred.badgeId))
        } else {
            val attempts = (failedAttempts[email] ?: 0) + 1
            failedAttempts[email] = attempts
            if (attempts >= 3) lockoutUntil[email] = System.currentTimeMillis() + 30_000L
            Result.failure(Exception("INVALID_CREDENTIALS:$attempts"))
        }
    }

    fun register(name: String, email: String, password: String, role: UserRole, badgeId: String, department: String): Result<User> {
        if (registeredUsers.any { it.email.equals(email, ignoreCase = true) })
            return Result.failure(Exception("EMAIL_ALREADY_EXISTS"))
        if (!isValidEmail(email)) return Result.failure(Exception("VALIDATION_ERROR:Invalid email"))
        if (!isStrongPassword(password)) return Result.failure(Exception("VALIDATION_ERROR:Password must be 8+ chars with uppercase, digit and symbol"))
        val cred = AuthCredential(email.lowercase(), hash(password), role, name, badgeId, department)
        registeredUsers.add(cred)
        return Result.success(User(UUID.randomUUID().toString(), name, email, role, "tok_${UUID.randomUUID()}", department, badgeId))
    }

    fun forgotPassword(email: String): Result<Unit> {
        val exists = registeredUsers.any { it.email.equals(email, ignoreCase = true) }
        // Always return success to avoid email enumeration
        return Result.success(Unit)
    }

    private fun isValidEmail(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    private fun isStrongPassword(pwd: String) = pwd.length >= 8 && pwd.any { it.isUpperCase() } && pwd.any { it.isDigit() } && pwd.any { !it.isLetterOrDigit() }
}
