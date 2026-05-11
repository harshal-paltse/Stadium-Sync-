package com.stadiumsync.app.presentation.screen.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import javax.inject.Inject

data class AnalyticsUiState(val summary: AnalyticsSummary = AnalyticsSummary(), val actions: List<TransitAction> = emptyList())

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getSummary: GetAnalyticsSummaryUseCase, private val getActions: GetTransitActionsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(AnalyticsUiState(summary = getSummary()))
    val state: StateFlow<AnalyticsUiState> = _state
    init { viewModelScope.launch { getActions().collect { a -> _state.update { it.copy(actions = a) } } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBack: () -> Unit = {}, vm: AnalyticsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val sum = s.summary

    Scaffold(topBar = {
        TopAppBar(title = { Text("Analytics") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {

            // ═══ HERO BANNER ══════════════════════════════
            Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Gunmetal, Slate))).padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("32K", style = MaterialTheme.typography.displaySmall.copy(color = ElectricCyan, fontWeight = FontWeight.Black))
                        Text("PASSENGERS", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${sum.totalCrowdPressure.toInt()}%", style = MaterialTheme.typography.displaySmall.copy(color = if (sum.totalCrowdPressure > 70) HotCoral else SolarAmber, fontWeight = FontWeight.Black))
                        Text("CROWD LOAD", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${sum.syncHealthPercent}%", style = MaterialTheme.typography.displaySmall.copy(color = ElectricCyan, fontWeight = FontWeight.Black))
                        Text("SYS HEALTH", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    }
                }
            }

            Column(Modifier.padding(20.dp)) {
                // ═══ METRIC GRID ══════════════════════════
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBlock("${sum.transportActionsCount}", "Actions", ElectricCyan, Icons.Filled.PlayCircle, Modifier.weight(1f))
                    StatBlock("${sum.notificationDeliveryRate.toInt()}%", "Notif Rate", VividBlue, Icons.Filled.NotificationsActive, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBlock("${sum.avgResponseTimeMs}ms", "Latency", DeepViolet, Icons.Filled.Speed, Modifier.weight(1f))
                    StatBlock("${sum.offlineQueueCount}", "Queue", SolarAmber, Icons.Filled.Queue, Modifier.weight(1f))
                }

                // ═══ CROWD TREND ══════════════════════════
                SectionLabel("Crowd Pressure Trend")
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                    Column {
                        WaveChart(listOf(12f, 18f, 25f, 40f, 52f, 68f, 82f, 95f, 88f, 72f, 48f, 22f), VividBlue)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("18:00", "19:30", "21:00", "22:30").forEach {
                                Text(it, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MutedText)
                            }
                        }
                    }
                }

                // ═══ TRANSIT MODE BARS ════════════════════
                SectionLabel("Transit Utilization")
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                    val modes = listOf("Metro" to 0.75f, "Bus" to 0.62f, "Shuttle" to 0.83f, "Ride" to 0.45f)
                    val colors = listOf(VividBlue, ElectricCyan, DeepViolet, SolarAmber)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        modes.forEachIndexed { i, (name, pct) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(60.dp))
                                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors[i].copy(alpha = 0.1f))) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(pct).clip(RoundedCornerShape(4.dp)).background(colors[i]))
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = colors[i], modifier = Modifier.width(34.dp))
                            }
                        }
                    }
                }

                // ═══ AUDIT TRAIL ══════════════════════════
                SectionLabel("Audit Trail")
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.actions.isEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.CheckCircle, null, tint = ElectricCyan.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("No actions recorded", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                    }
                    s.actions.take(8).forEach { action ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(32.dp).clip(RoundedCornerShape(2.dp)).background(VividBlue))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(action.actionType.name.replace("_", " "), style = MaterialTheme.typography.titleSmall)
                                Text(action.description.ifEmpty { action.routeName }, style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                            SignalTag(action.status.name)
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
