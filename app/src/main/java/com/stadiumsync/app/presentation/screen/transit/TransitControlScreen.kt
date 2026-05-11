package com.stadiumsync.app.presentation.screen.transit

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stadiumsync.app.domain.model.*
import com.stadiumsync.app.domain.usecase.*
import com.stadiumsync.app.presentation.components.*
import com.stadiumsync.app.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TransitUiState(
    val routes: List<TransitRoute> = emptyList(),
    val actions: List<TransitAction> = emptyList(),
    val vehicles: List<TransitVehicle> = emptyList(),
    val incidents: List<TransitIncident> = emptyList(),
    val recommendations: List<TransitRecommendation> = emptyList(),
    val smartSuggestions: List<TransitSuggestion> = emptyList(),
    val showDialog: TransitRecommendation? = null,
    val selectedFrom: String = "Gate A",
    val selectedTo: String = "Churchgate",
    val filterType: TransitType? = null,
    val lastRefresh: Long = 0L
)

@HiltViewModel
class TransitControlViewModel @Inject constructor(
    private val getRoutes: GetTransitRoutesUseCase,
    private val getActions: GetTransitActionsUseCase,
    private val getVehicles: GetTransitVehiclesUseCase,
    private val getIncidents: GetTransitIncidentsUseCase,
    private val getRecommendations: GetTransitRecommendationsUseCase,
    private val getSmartSuggestions: GetSmartSuggestionsUseCase,
    private val getCrowdPressure: GetCrowdPressureUseCase,
    private val executeAction: ExecuteTransitActionUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(TransitUiState())
    val state: StateFlow<TransitUiState> = _state

    init {
        viewModelScope.launch {
            launch { getRoutes().collect { r -> _state.update { it.copy(routes = r, lastRefresh = System.currentTimeMillis()) } } }
            launch { getActions().collect { a -> _state.update { it.copy(actions = a) } } }
            launch { getVehicles().collect { v -> _state.update { it.copy(vehicles = v) } } }
            launch { getIncidents().collect { i -> _state.update { it.copy(incidents = i) } } }
            launch { getCrowdPressure().collect { cp -> _state.update { it.copy(recommendations = getRecommendations(cp)) } } }
        }
        refreshSmartSuggestions()
    }

    fun showDialog(rec: TransitRecommendation) = _state.update { it.copy(showDialog = rec) }
    fun dismissDialog() = _state.update { it.copy(showDialog = null) }
    fun setFrom(v: String) { _state.update { it.copy(selectedFrom = v) }; refreshSmartSuggestions() }
    fun setTo(v: String) { _state.update { it.copy(selectedTo = v) }; refreshSmartSuggestions() }
    fun setFilter(t: TransitType?) { _state.update { it.copy(filterType = t) }; refreshSmartSuggestions() }
    fun executeRec(rec: TransitRecommendation) = viewModelScope.launch {
        executeAction(TransitAction(UUID.randomUUID().toString(), rec.actionType, rec.routeId, rec.routeName, description = rec.reason, operatorName = "Operator", isAutoTriggered = rec.autoTriggered))
        dismissDialog()
    }
    private fun refreshSmartSuggestions() {
        val s = _state.value
        _state.update { it.copy(smartSuggestions = getSmartSuggestions(s.selectedFrom, s.selectedTo, s.filterType)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitControlScreen(onBack: () -> Unit = {}, vm: TransitControlViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Routes", "Live Map", "Incidents", "AI Suggest", "Smart Routes")

    s.showDialog?.let { rec ->
        AlertDialog(onDismissRequest = vm::dismissDialog,
            title = { Text(rec.actionType.name.replace("_", " "), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(rec.reason, style = MaterialTheme.typography.bodyMedium)
                    if (rec.estimatedImpact.isNotEmpty()) {
                        Text("Impact: ${rec.estimatedImpact}", style = MaterialTheme.typography.bodySmall, color = ElectricCyan)
                    }
                    Text("Confidence: ${rec.confidence}%  |  Affected: ${rec.affectedPassengers} pax",
                        style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
            },
            confirmButton = {
                val c = when(rec.priority){ AlertPriority.CRITICAL -> HotCoral; AlertPriority.WARNING -> SolarAmber; else -> VividBlue }
                Button(onClick = { vm.executeRec(rec) }, colors = ButtonDefaults.buttonColors(containerColor = c)) { Text("Execute") }
            },
            dismissButton = { TextButton(onClick = vm::dismissDialog) { Text("Cancel") } })
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Transit Control", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
            actions = {
                val incCount = s.incidents.count { !it.isResolved && it.severity == IncidentSeverity.CRITICAL }
                if (incCount > 0) Badge(containerColor = HotCoral) { Text("$incCount") }
                IconButton(onClick = {}) { Icon(Icons.Filled.Refresh, null) }
            }
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp, divider = {}) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i },
                        text = { Text(label, style = MaterialTheme.typography.labelMedium) })
                }
            }
            when (tab) {
                0 -> RoutesTab(s.routes)
                1 -> LiveMapTab(s.vehicles, s.incidents)
                2 -> IncidentsTab(s.incidents)
                3 -> AIRecommendTab(s.recommendations, vm::showDialog)
                4 -> SmartRoutesTab(s, vm::setFrom, vm::setTo, vm::setFilter)
            }
        }
    }
}

@Composable
private fun RoutesTab(routes: List<TransitRoute>) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(routes) { route ->
            val loadPct = if (route.capacity > 0) route.currentLoad * 100 / route.capacity else 0
            val statusColor = when(route.status) { TransitRouteStatus.ACTIVE -> ElectricCyan; TransitRouteStatus.DELAYED -> SolarAmber; TransitRouteStatus.HELD -> HotCoral; else -> MutedText }
            val typeIcon = when(route.type) { TransitType.METRO -> "🚇"; TransitType.BUS -> "🚌"; TransitType.SHUTTLE -> "🚐"; TransitType.RIDE_PICKUP -> "🚖" }
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(typeIcon, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(route.name, style = MaterialTheme.typography.titleSmall)
                            Text(route.operatorName, style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                            Text(route.status.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall, color = statusColor)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { loadPct / 100f },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (loadPct > 85) HotCoral else if (loadPct > 60) SolarAmber else ElectricCyan,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${route.currentLoad}/${route.capacity} pax ($loadPct%)", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        if (route.estimatedDelayMin > 0) Text("+${route.estimatedDelayMin} min delay", style = MaterialTheme.typography.bodySmall, color = SolarAmber)
                    }
                    if (route.nextArrivalMin > 0 || route.platform.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (route.platform.isNotEmpty()) InfoChip("🟦 ${route.platform}")
                            if (route.nextArrivalMin > 0) InfoChip("⏱ ${route.nextArrivalMin} min")
                            if (route.vehicleCount > 0) InfoChip("🚗 ${route.vehicleCount} vehicles")
                            InfoChip("🔄 Every ${route.frequencyMin}m")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveMapTab(vehicles: List<TransitVehicle>, incidents: List<TransitIncident>) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Stats row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("${vehicles.count { it.status == VehicleStatus.EN_ROUTE }} Moving", ElectricCyan, Modifier.weight(1f))
            StatPill("${vehicles.count { it.status == VehicleStatus.DELAYED }} Delayed", SolarAmber, Modifier.weight(1f))
            StatPill("${incidents.count { !it.isResolved }} Incidents", HotCoral, Modifier.weight(1f))
        }
        // Vehicle list
        Text("Live Vehicles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(vehicles) { v ->
                val dot = when(v.status) { VehicleStatus.EN_ROUTE -> ElectricCyan; VehicleStatus.DELAYED -> SolarAmber; VehicleStatus.BREAKDOWN -> HotCoral; VehicleStatus.AT_STOP -> VividBlue; else -> MutedText }
                Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, tonalElevation = 1.dp) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(8.dp).background(dot, CircleShape))
                        Column(Modifier.weight(1f)) {
                            Text(v.vehicleNumber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("${v.routeName}  →  ${v.nextStopName}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${v.passengerCount}/${v.capacity}", style = MaterialTheme.typography.labelSmall)
                            if (v.etaMinutes > 0) Text("ETA ${v.etaMinutes}m", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                            if (v.speedKmh > 0) Text("${v.speedKmh.toInt()} km/h", style = MaterialTheme.typography.labelSmall, color = MutedText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentsTab(incidents: List<TransitIncident>) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (incidents.isEmpty()) item {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No active incidents", color = MutedText)
            }
        }
        items(incidents) { inc ->
            val sevColor = when(inc.severity) { IncidentSeverity.CRITICAL -> HotCoral; IncidentSeverity.HIGH -> SolarAmber; IncidentSeverity.MEDIUM -> VividBlue; else -> MutedText }
            val typeEmoji = when(inc.type) { IncidentType.METRO_FAULT -> "🚇"; IncidentType.ROAD_BLOCK -> "🚧"; IncidentType.CROWD_OVERFLOW -> "👥"; IncidentType.ACCIDENT -> "🚨"; IncidentType.WEATHER -> "🌧️"; else -> "⚠️" }
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
                Row(Modifier.padding(14.dp)) {
                    Box(Modifier.width(3.dp).height(70.dp).clip(RoundedCornerShape(2.dp)).background(sevColor))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(typeEmoji, fontSize = 16.sp)
                            Text(inc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Surface(shape = RoundedCornerShape(4.dp), color = sevColor.copy(0.15f)) {
                                Text(inc.severity.name, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = sevColor)
                            }
                        }
                        Text(inc.description, style = MaterialTheme.typography.bodySmall, color = MutedText)
                        Text("📍 ${inc.location}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoChip("Est. resolve: ${inc.estimatedResolutionMin}m")
                            InfoChip("Routes: ${inc.affectedRoutes.size}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AIRecommendTab(recs: List<TransitRecommendation>, onExecute: (TransitRecommendation) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(recs) { rec ->
            val accent = when(rec.priority) { AlertPriority.CRITICAL -> HotCoral; AlertPriority.WARNING -> SolarAmber; else -> VividBlue }
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
                Row(Modifier.padding(14.dp)) {
                    Box(Modifier.width(3.dp).height(80.dp).clip(RoundedCornerShape(2.dp)).background(accent))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(4.dp), color = accent.copy(0.15f)) {
                                Text(rec.priority.name, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(rec.actionType.name.replace("_", " "), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${rec.confidence}%", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                        }
                        Text(rec.reason, style = MaterialTheme.typography.bodySmall, color = MutedText)
                        if (rec.estimatedImpact.isNotEmpty()) Text(rec.estimatedImpact, style = MaterialTheme.typography.bodySmall, color = ElectricCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (rec.affectedPassengers > 0) InfoChip("👥 ${rec.affectedPassengers} pax")
                            if (rec.autoTriggered) InfoChip("🤖 Auto")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = { onExecute(rec) }, colors = ButtonDefaults.buttonColors(containerColor = accent)) { Text("Execute") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartRoutesTab(s: TransitUiState, setFrom: (String) -> Unit, setTo: (String) -> Unit, setFilter: (TransitType?) -> Unit) {
    val fromOptions = listOf("Gate A","Gate B","Gate C","Gate D","Gate E","Gate F")
    val toOptions = listOf("Churchgate","Dadar Station","Andheri Metro","BKC Hub","Marine Drive","Bandra Stn")
    var expandFrom by remember { mutableStateOf(false) }
    var expandTo by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        // From/To pickers
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { expandFrom = true }, Modifier.fillMaxWidth()) {
                    Text("From: ${s.selectedFrom}", style = MaterialTheme.typography.labelMedium)
                }
                DropdownMenu(expanded = expandFrom, onDismissRequest = { expandFrom = false }) {
                    fromOptions.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { setFrom(it); expandFrom = false }) }
                }
            }
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { expandTo = true }, Modifier.fillMaxWidth()) {
                    Text("To: ${s.selectedTo}", style = MaterialTheme.typography.labelMedium)
                }
                DropdownMenu(expanded = expandTo, onDismissRequest = { expandTo = false }) {
                    toOptions.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { setTo(it); expandTo = false }) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val filters = listOf(null to "All") + TransitType.entries.map { it to it.name.replace("_"," ") }
            filters.forEach { (t, label) ->
                FilterChip(selected = s.filterType == t, onClick = { setFilter(t) }, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
            }
        }
        Spacer(Modifier.height(8.dp))
        // Suggestions list
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(s.smartSuggestions.sortedByDescending { it.aiScore }) { sg ->
                SmartSuggestionCard(sg)
            }
            if (s.smartSuggestions.isEmpty()) item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No routes found for this filter", color = MutedText)
                }
            }
        }
    }
}

@Composable
private fun SmartSuggestionCard(sg: TransitSuggestion) {
    var expanded by remember { mutableStateOf(false) }
    val crowdColor = when(sg.crowdLevel) { PressureLevel.LOW -> ElectricCyan; PressureLevel.MODERATE -> SolarAmber; PressureLevel.HIGH -> HotCoral; PressureLevel.CRITICAL -> HotCoral }
    val typeEmoji = when(sg.transitType) { TransitType.METRO -> "🚇"; TransitType.BUS -> "🚌"; TransitType.SHUTTLE -> "🚐"; TransitType.RIDE_PICKUP -> "🚖" }

    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
        Column(Modifier.padding(14.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(typeEmoji, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(sg.routeName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${sg.fromLocation}  →  ${sg.toLocation}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${sg.aiScore}", style = MaterialTheme.typography.headlineSmall.copy(color = if(sg.aiScore>=85) ElectricCyan else if(sg.aiScore>=70) SolarAmber else MutedText), fontWeight = FontWeight.Black)
                    Text("AI Score", style = MaterialTheme.typography.labelSmall, color = MutedText)
                }
            }
            // Tags
            if (sg.tags.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                sg.tags.forEach { tag ->
                    val c = when(tag) { "Fastest" -> VividBlue; "Eco" -> ElectricCyan; "Recommended" -> DeepViolet; "Budget" -> SolarAmber; else -> MutedText }
                    Surface(shape = RoundedCornerShape(4.dp), color = c.copy(0.15f)) {
                        Text(tag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = c)
                    }
                }
            }
            // Key stats row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("⏱", "${sg.estimatedTimeMin}m", "Time")
                StatItem("🚶", "${sg.walkingTimeMin}m walk", "Walk")
                StatItem("₹", "${sg.fareRupees}", "Fare")
                StatItem("🌱", "${sg.co2GramsPerKm}g", "CO₂/km")
                StatItem("✅", "${sg.reliabilityPercent}%", "Reliable")
            }
            // Next departure
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("Next: ${if(sg.nextDepartureMin==0) "Now" else "${sg.nextDepartureMin}m"}")
                InfoChip("Every ${sg.headwayMin}m")
                InfoChip(sg.crowdLevel.name, crowdColor)
            }
            // Expandable waypoints
            if (sg.waypoints.isNotEmpty()) {
                TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                    Text(if(expanded) "Hide Route ▲" else "Show Route ▼", style = MaterialTheme.typography.labelSmall, color = VividBlue)
                }
                if (expanded) {
                    Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        sg.waypoints.forEach { wp ->
                            val wpColor = when(wp.type) { WaypointType.START -> ElectricCyan; WaypointType.DESTINATION -> HotCoral; WaypointType.TRANSFER -> SolarAmber; else -> MutedText }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(8.dp).background(wpColor, CircleShape))
                                Text(wp.label, style = MaterialTheme.typography.bodySmall)
                                if (wp.etaFromStart > 0) Text("+${wp.etaFromStart}m", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                if (wp.walkingDistanceM > 0) Text("🚶${wp.walkingDistanceM}m", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$emoji $value", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedText)
    }
}

@Composable
private fun StatPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = color.copy(0.12f)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoChip(text: String, color: Color = MutedText) {
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(0.1f)) {
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color)
    }
}
