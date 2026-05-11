package com.stadiumsync.app.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stadiumsync.app.core.network.NetworkMonitor
import com.stadiumsync.app.domain.model.*
import com.stadiumsync.app.domain.usecase.*
import com.stadiumsync.app.presentation.components.*
import com.stadiumsync.app.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val match: Match? = null, val prediction: MatchPrediction? = null,
    val crowdPressure: List<CrowdPressure> = emptyList(), val syncStatus: SyncStatus = SyncStatus(),
    val isOnline: Boolean = true, val unreadAlerts: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getLiveMatch: GetLiveMatchUseCase, private val getMatchPrediction: GetMatchPredictionUseCase,
    private val getCrowdPressure: GetCrowdPressureUseCase, private val refreshCrowdData: RefreshCrowdDataUseCase,
    private val getSyncStatus: GetSyncStatusUseCase, private val getUnreadCount: GetUnreadCountUseCase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state
    init { viewModelScope.launch {
        launch { getLiveMatch().collect { m -> _state.update { it.copy(match = m) }; m?.let { refreshCrowdData(m.matchPhase); launch { getMatchPrediction(m.id).collect { p -> _state.update { it.copy(prediction = p) } } } } } }
        launch { getCrowdPressure().collect { cp -> _state.update { it.copy(crowdPressure = cp) } } }
        launch { getSyncStatus().collect { ss -> _state.update { it.copy(syncStatus = ss) } } }
        launch { getUnreadCount().collect { c -> _state.update { it.copy(unreadAlerts = c) } } }
        launch { networkMonitor.isOnline.collect { o -> _state.update { it.copy(isOnline = o) } } }
    }}
}

@Composable
fun HomeScreen(
    onMatchClick: () -> Unit = {}, onCrowdClick: () -> Unit = {}, onTransitClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {}, onOfflineClick: () -> Unit = {}, onTicketClick: () -> Unit = {},
    vm: HomeViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        OfflineBanner(!s.isOnline, s.syncStatus.pendingCount)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // ═══ HEADER BAR ═══════════════════════════════
            Row(Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Stadium", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black))
                    Text("Sync", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, color = VividBlue))
                }
                if (s.isOnline) LiveBeacon() else SignalTag("Offline")
                Spacer(Modifier.width(12.dp))
                BadgedBox(badge = { if (s.unreadAlerts > 0) Badge(containerColor = HotCoral) { Text("${s.unreadAlerts}", fontSize = 9.sp) } }) {
                    IconButton(onClick = onAlertsClick) { Icon(Icons.Filled.Notifications, null) }
                }
            }

            // ═══ SCORE STRIP ══════════════════════════════
            s.match?.let { m ->
                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Gunmetal, Slate))).padding(20.dp)) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(m.format.name, style = MaterialTheme.typography.labelSmall, color = MutedText)
                            SignalTag(m.matchPhase.name.replace("_", " "))
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Text(m.team1, style = MaterialTheme.typography.titleSmall.copy(color = Color.White.copy(alpha = 0.6f)))
                                Text(m.score1, style = MaterialTheme.typography.displayLarge.copy(color = Color.White, fontWeight = FontWeight.Black))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${m.overs1} ov", style = MaterialTheme.typography.bodySmall.copy(color = MutedText))
                                    Spacer(Modifier.width(8.dp))
                                    Text("RR ${String.format("%.1f", m.currentRunRate)}", style = MaterialTheme.typography.labelSmall.copy(color = ElectricCyan))
                                }
                            }
                            // Prediction compact
                            s.prediction?.let { p ->
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${p.estimatedMinutesLeft}", style = MaterialTheme.typography.displayMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Black))
                                    Text("MIN LEFT", style = MaterialTheme.typography.labelSmall.copy(color = MutedText))
                                    Spacer(Modifier.height(4.dp))
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(ElectricCyan.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text("${p.confidencePercent}% conf", style = MaterialTheme.typography.labelSmall.copy(color = ElectricCyan))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // VS strip
                        Text("vs ${m.team2}", style = MaterialTheme.typography.bodySmall.copy(color = MutedText))
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(onClick = onMatchClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = 0.08f))) {
                            Text("Match Details →", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // ═══ GAUGES ROW ═══════════════════════════════
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ArcGauge(s.crowdPressure.maxOfOrNull { it.densityPercent } ?: 0, "Crowd", HotCoral, 90.dp)
                ArcGauge(s.prediction?.transitUrgencyScore?.toInt() ?: 0, "Transit", SolarAmber, 90.dp)
                ArcGauge(s.syncStatus.syncHealthPercent, "Sync", ElectricCyan, 90.dp)
            }

            // ═══ STAT BLOCKS ══════════════════════════════
            SectionLabel("Key Metrics")
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBlock("33K", "Capacity", DeepViolet, Icons.Filled.Stadium, Modifier.weight(1f))
                StatBlock("28°C", "Weather", SolarAmber, Icons.Filled.WbSunny, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBlock("${s.syncStatus.pendingCount}", "Queued", if (s.syncStatus.pendingCount > 0) SolarAmber else ElectricCyan, Icons.Filled.Queue, Modifier.weight(1f))
                StatBlock("340ms", "Latency", ElectricCyan, Icons.Filled.Speed, Modifier.weight(1f))
            }

            // ═══ CROWD RADAR ══════════════════════════════
            if (s.crowdPressure.isNotEmpty()) {
                SectionLabel("Crowd Radar", "Details", onCrowdClick)
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
                    StadiumRadar(s.crowdPressure)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    s.crowdPressure.take(4).forEach { g ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val c = when (g.pressureLevel) { PressureLevel.CRITICAL -> HotCoral; PressureLevel.HIGH -> SolarAmber; PressureLevel.MODERATE -> VividBlue; PressureLevel.LOW -> ElectricCyan }
                            Text("${g.densityPercent}%", style = MaterialTheme.typography.labelLarge, color = c)
                            Text(g.gateName.substringBefore(" -"), style = MaterialTheme.typography.labelSmall, color = MutedText)
                        }
                    }
                }
            }

            // ═══ CROWD WAVE ═══════════════════════════════
            SectionLabel("Predicted Exit Wave")
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                Column {
                    WaveChart(listOf(5f, 8f, 15f, 28f, 52f, 78f, 95f, 88f, 60f, 35f, 18f, 8f))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Now", style = MaterialTheme.typography.labelSmall, color = MutedText)
                        Text("Peak", style = MaterialTheme.typography.labelSmall, color = HotCoral)
                        Text("+30m", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    }
                }
            }

            // ═══ TRANSIT CAROUSEL ═════════════════════════
            SectionLabel("Transit Routes", "Control", onTransitClick)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val mockRoutes = listOf(
                    TransitRoute("m1", "Metro Blue Line", TransitType.METRO, TransitRouteStatus.ACTIVE, 2400, 1800, 0),
                    TransitRoute("b1", "BEST Bus 83", TransitType.BUS, TransitRouteStatus.DELAYED, 60, 52, 8),
                    TransitRoute("s1", "Shuttle A", TransitType.SHUTTLE, TransitRouteStatus.ACTIVE, 30, 22, 0),
                    TransitRoute("r1", "Pickup Alpha", TransitType.RIDE_PICKUP, TransitRouteStatus.ACTIVE, 200, 120, 3)
                )
                mockRoutes.forEach { TransitTile(it) }
            }

            // ═══ QUICK NAV ════════════════════════════════
            SectionLabel("Quick Access")
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NavPill(Icons.Filled.ConfirmationNumber, "Ticket", ElectricCyan, onTicketClick, Modifier.weight(1f))
                NavPill(Icons.Filled.Map, "Heatmap", VividBlue, onCrowdClick, Modifier.weight(1f))
                NavPill(Icons.Filled.CloudOff, "Offline", SolarAmber, onOfflineClick, Modifier.weight(1f))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NavPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.08f)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
