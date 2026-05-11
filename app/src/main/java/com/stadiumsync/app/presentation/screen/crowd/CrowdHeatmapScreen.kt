package com.stadiumsync.app.presentation.screen.crowd

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.*

data class CrowdUiState(val pressure: List<CrowdPressure> = emptyList(), val heatmap: List<CrowdHeatmapPoint> = emptyList())

@HiltViewModel
class CrowdHeatmapViewModel @Inject constructor(
    private val getCrowdPressure: GetCrowdPressureUseCase, private val getHeatmap: GetCrowdHeatmapUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(CrowdUiState())
    val state: StateFlow<CrowdUiState> = _state
    init { viewModelScope.launch { _state.update { it.copy(heatmap = getHeatmap()) }; getCrowdPressure().collect { cp -> _state.update { it.copy(pressure = cp) } } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrowdHeatmapScreen(onBack: () -> Unit = {}, vm: CrowdHeatmapViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val inf = rememberInfiniteTransition(label = "h")
    val pulse by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "p")

    Scaffold(topBar = {
        TopAppBar(title = { Text("Crowd Intelligence") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
            actions = { LiveBeacon(Modifier.padding(end = 12.dp)) })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {

            // ═══ STADIUM HEATMAP ══════════════════════════
            Box(Modifier.fillMaxWidth().height(300.dp).background(Obsidian)) {
                Canvas(Modifier.fillMaxSize().padding(20.dp)) {
                    val w = size.width; val h = size.height; val cx = w / 2; val cy = h / 2
                    // Stadium shape — rounded rectangle / oval
                    drawOval(Slate.copy(alpha = 0.4f), Offset(cx - w * 0.42f, cy - h * 0.42f), androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.84f))
                    drawOval(Zinc.copy(alpha = 0.3f), Offset(cx - w * 0.42f, cy - h * 0.42f), androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.84f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                    // Inner pitch
                    drawOval(Color(0xFF1B3A2B).copy(alpha = 0.6f), Offset(cx - w * 0.18f, cy - h * 0.18f), androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.36f))

                    // Heat dots with glow
                    s.heatmap.forEach { pt ->
                        val px = w * 0.08f + pt.x * w * 0.84f
                        val py = h * 0.08f + pt.y * h * 0.84f
                        val dotColor = when {
                            pt.density > 0.75f -> HotCoral
                            pt.density > 0.5f -> SolarAmber
                            pt.density > 0.25f -> ElectricCyan.copy(alpha = 0.7f)
                            else -> ElectricCyan.copy(alpha = 0.2f)
                        }
                        // Glow
                        if (pt.density > 0.5f) {
                            drawCircle(dotColor.copy(alpha = 0.12f * pulse), radius = 20f + pt.density * 15f, center = Offset(px, py))
                        }
                        drawCircle(dotColor.copy(alpha = pt.density * pulse), radius = 3f + pt.density * 7f, center = Offset(px, py))
                    }

                    // Gate markers
                    val gates = listOf("N" to Offset(cx, cy - h * 0.45f), "E" to Offset(cx + w * 0.45f, cy), "S" to Offset(cx, cy + h * 0.45f), "W" to Offset(cx - w * 0.45f, cy))
                    gates.forEach { (_, pos) -> drawCircle(Color.White, 3f, pos); drawCircle(Color.White.copy(alpha = 0.2f), 8f, pos) }
                }

                // Legend
                Row(Modifier.align(Alignment.BottomStart).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DotLabel(ElectricCyan, "Low"); DotLabel(SolarAmber, "High"); DotLabel(HotCoral, "Critical")
                }
                // Compass
                Column(Modifier.align(Alignment.TopEnd).padding(20.dp)) {
                    listOf("N", "E", "S", "W").forEachIndexed { i, d ->
                        Text(d, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.4f)))
                    }
                }
            }

            Column(Modifier.padding(20.dp)) {
                // ═══ SUMMARY METRICS ══════════════════════
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBlock("${s.pressure.maxOfOrNull { it.densityPercent } ?: 0}%", "Peak", HotCoral, Icons.Filled.TrendingUp, Modifier.weight(1f))
                    StatBlock("${s.pressure.map { it.densityPercent }.average().toInt()}%", "Average", VividBlue, Icons.Filled.BarChart, Modifier.weight(1f))
                }

                // ═══ GATE PRESSURE STRIPS ═════════════════
                SectionLabel("Gate Breakdown")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { s.pressure.forEach { PressureStrip(it) } }

                // ═══ RADAR VIEW ═══════════════════════════
                if (s.pressure.isNotEmpty()) {
                    SectionLabel("Pressure Radar")
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)) {
                        StadiumRadar(s.pressure)
                    }
                }

                // ═══ EXIT WAVE ═════════════════════════════
                SectionLabel("Predicted Exit Wave")
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                    Column {
                        WaveChart(listOf(5f, 12f, 28f, 55f, 82f, 95f, 85f, 58f, 30f, 12f), HotCoral)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Now", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            Text("▲ Peak +12m", style = MaterialTheme.typography.labelSmall, color = HotCoral)
                            Text("+30m", style = MaterialTheme.typography.labelSmall, color = MutedText)
                        }
                    }
                }

                // ═══ RANKED GATES ══════════════════════════
                SectionLabel("Critical Zones")
                s.pressure.sortedByDescending { it.densityPercent }.take(3).forEachIndexed { i, g ->
                    val medal = when(i) { 0 -> HotCoral; 1 -> SolarAmber; else -> VividBlue }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(10.dp)).background(medal.copy(alpha = 0.06f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(medal.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = medal)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.gateName, style = MaterialTheme.typography.titleSmall)
                            Text("~${String.format("%,d", g.estimatedPeople)} people · ${g.trend.name}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        Text("${g.densityPercent}%", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = medal)
                    }
                }

                // ═══ SAFETY ═══════════════════════════════
                val critical = s.pressure.filter { it.pressureLevel == PressureLevel.CRITICAL || it.pressureLevel == PressureLevel.HIGH }
                if (critical.isNotEmpty()) {
                    SectionLabel("Safety Actions")
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(HotCoral.copy(alpha = 0.05f)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionItem("Divert traffic from ${critical.first().gateName}", HotCoral)
                        ActionItem("Deploy crowd marshals at exits", SolarAmber)
                        ActionItem("Open overflow lanes", SolarAmber)
                        ActionItem("Stagger metro boarding", VividBlue)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun DotLabel(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun ActionItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 5.dp).size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
