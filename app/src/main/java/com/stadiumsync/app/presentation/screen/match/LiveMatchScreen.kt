package com.stadiumsync.app.presentation.screen.match

import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class MatchUiState(val match: Match? = null, val prediction: MatchPrediction? = null, val isOnline: Boolean = true)

@HiltViewModel
class LiveMatchViewModel @Inject constructor(
    private val getLiveMatch: GetLiveMatchUseCase, private val getPrediction: GetMatchPredictionUseCase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    private val _state = MutableStateFlow(MatchUiState())
    val state: StateFlow<MatchUiState> = _state
    init { viewModelScope.launch {
        launch { getLiveMatch().collect { m -> _state.update { it.copy(match = m) }; m?.let { launch { getPrediction(m.id).collect { p -> _state.update { it.copy(prediction = p) } } } } } }
        launch { networkMonitor.isOnline.collect { o -> _state.update { it.copy(isOnline = o) } } }
    }}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMatchScreen(onBack: () -> Unit = {}, vm: LiveMatchViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val match = s.match
    val pred = s.prediction

    Scaffold(topBar = {
        TopAppBar(title = { Text("Live Match") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
            actions = { if (s.isOnline) LiveBeacon(Modifier.padding(end = 12.dp)) else SignalTag("Offline", modifier = Modifier.padding(end = 12.dp)) })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OfflineBanner(!s.isOnline)
            if (match == null) { LoadingScreen("Connecting..."); return@Scaffold }

            Column(Modifier.verticalScroll(rememberScrollState())) {
                // ═══ HERO SCORE ═══════════════════════════
                Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Gunmetal, Obsidian))).padding(24.dp)) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${match.format.name} · IPL 2024", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            SignalTag(match.matchPhase.name.replace("_", " "))
                        }
                        Spacer(Modifier.height(24.dp))
                        // Team 1
                        Text(match.team1, style = MaterialTheme.typography.titleSmall.copy(color = MutedText))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(match.score1, style = MaterialTheme.typography.displayLarge.copy(color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.padding(bottom = 8.dp)) {
                                Text("${match.overs1} ov", style = MaterialTheme.typography.bodySmall.copy(color = MutedText))
                                Text("CRR ${String.format("%.1f", match.currentRunRate)}", style = MaterialTheme.typography.labelSmall.copy(color = ElectricCyan))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.height(1.dp).weight(1f).background(Zinc))
                            Text("  VS  ", style = MaterialTheme.typography.labelSmall.copy(color = Zinc))
                            Box(Modifier.height(1.dp).weight(1f).background(Zinc))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(match.team2, style = MaterialTheme.typography.titleSmall.copy(color = MutedText))
                        if (match.score2.isNotEmpty()) Text(match.score2, style = MaterialTheme.typography.displaySmall.copy(color = Color.White))
                        else Text("Yet to bat", style = MaterialTheme.typography.bodySmall.copy(color = Zinc))

                        Spacer(Modifier.height(20.dp))
                        // Stats strip
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Slate.copy(alpha = 0.5f)).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MiniStat("REQ RR", String.format("%.1f", match.requiredRunRate), SolarAmber)
                            Divider(Modifier.width(1.dp).height(28.dp), color = Zinc)
                            MiniStat("VENUE", "33K", VividBlue)
                            Divider(Modifier.width(1.dp).height(28.dp), color = Zinc)
                            MiniStat("OVERS", "${match.overs1}", ElectricCyan)
                        }
                    }
                }

                Column(Modifier.padding(20.dp)) {
                    // ═══ PREDICTION PANEL ══════════════════
                    pred?.let { p ->
                        SectionLabel("AI Prediction")
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            ArcGauge(p.confidencePercent, "Confidence", ElectricCyan, 100.dp)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(sdf.format(Date(p.estimatedEndTime)), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black), color = VividBlue)
                                Text("EST. END", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                Spacer(Modifier.height(8.dp))
                                BigNumber("${p.estimatedMinutesLeft}", "min left", ElectricCyan)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Probability bars
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                            val probs = p.probabilityDistribution.ifEmpty { listOf(0.05, 0.1, 0.2, 0.35, 0.2, 0.1) }
                            val labels = listOf("5m", "10m", "15m", "20m", "25m", "30m")
                            probs.forEachIndexed { i, prob ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${(prob * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MutedText)
                                    Spacer(Modifier.height(4.dp))
                                    Box(Modifier.width(20.dp).height(((prob * 200).toFloat().coerceIn(8f, 80f)).dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (prob == probs.max()) ElectricCyan else VividBlue.copy(alpha = 0.4f)))
                                    Spacer(Modifier.height(4.dp))
                                    Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MutedText)
                                }
                            }
                        }
                    }

                    // ═══ IMPACT SCORES ═════════════════════
                    SectionLabel("Transit Impact")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatBlock("${pred?.crowdReleaseScore?.toInt() ?: 0}%", "Crowd Release", SolarAmber, Icons.Filled.DirectionsWalk, Modifier.weight(1f))
                        StatBlock("${pred?.transitUrgencyScore?.toInt() ?: 0}%", "Transit Urgency", HotCoral, Icons.Filled.Train, Modifier.weight(1f))
                    }

                    // ═══ RUN RATE WAVE ═════════════════════
                    SectionLabel("Run Rate Trend")
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                        WaveChart(listOf(6.2f, 7.1f, 5.8f, 8.4f, 9.2f, 7.6f, 10.1f, 8.8f, 9.5f, 11.2f), ElectricCyan)
                    }

                    // ═══ MATCH INFO ════════════════════════
                    SectionLabel("Match Info")
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoLine("Venue", match.venue)
                        InfoLine("Start", sdf.format(Date(match.startTime)))
                        InfoLine("Capacity", "33,000 (98%)")
                        InfoLine("Weather", "28°C Clear")
                        InfoLine("Source", if (s.isOnline) "Live Feed" else "Cached")
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedText)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedText)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
}
