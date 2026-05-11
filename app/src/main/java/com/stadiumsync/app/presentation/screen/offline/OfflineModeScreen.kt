package com.stadiumsync.app.presentation.screen.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

data class OfflineUiState(
    val isOnline: Boolean = true, val syncStatus: SyncStatus = SyncStatus(),
    val match: Match? = null, val prediction: MatchPrediction? = null,
    val isSyncing: Boolean = false, val lastSyncResult: String? = null
)

@HiltViewModel
class OfflineModeViewModel @Inject constructor(
    private val networkMonitor: NetworkMonitor, private val getSyncStatus: GetSyncStatusUseCase,
    private val syncOfflineData: SyncOfflineDataUseCase, private val getLiveMatch: GetLiveMatchUseCase,
    private val getPrediction: GetMatchPredictionUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(OfflineUiState())
    val state: StateFlow<OfflineUiState> = _state
    init { viewModelScope.launch {
        launch { networkMonitor.isOnline.collect { o -> _state.update { it.copy(isOnline = o) } } }
        launch { getSyncStatus().collect { s -> _state.update { it.copy(syncStatus = s) } } }
        launch { getLiveMatch().collect { m -> _state.update { it.copy(match = m) }; m?.let { launch { getPrediction(m.id).collect { p -> _state.update { it.copy(prediction = p) } } } } } }
    }}
    fun triggerSync() { viewModelScope.launch {
        _state.update { it.copy(isSyncing = true) }
        syncOfflineData().onSuccess { count ->
            _state.update { it.copy(isSyncing = false, lastSyncResult = "Synced $count items") }
        }.onFailure { error ->
            _state.update { it.copy(isSyncing = false, lastSyncResult = "Failed: ${error.message}") }
        }
    }}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineModeScreen(onBack: () -> Unit = {}, vm: OfflineModeViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Offline Mode") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp)) {
            // Network status hero
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (s.isOnline) ElectricCyan.copy(alpha = 0.08f) else HotCoral.copy(alpha = 0.08f)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (s.isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff, null, tint = if (s.isOnline) ElectricCyan else HotCoral, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (s.isOnline) "Connected" else "No Network", style = MaterialTheme.typography.headlineSmall)
                    Text(if (s.isOnline) "All services operational" else "Using cached data", style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
                SignalTag(if (s.isOnline) "Online" else "Offline")
            }
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBlock("${s.syncStatus.pendingCount}", "Queued", SolarAmber, Icons.Filled.Queue, Modifier.weight(1f))
                StatBlock("${s.syncStatus.syncHealthPercent}%", "Health", ElectricCyan, Icons.Filled.HealthAndSafety, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))

            // Sync button
            Button(onClick = vm::triggerSync, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = !s.isSyncing && s.isOnline,
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(if (s.isOnline) CyanGlow else Brush.linearGradient(listOf(Zinc, Zinc))),
                    contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (s.isSyncing) CircularProgressIndicator(Modifier.size(18.dp), color = Obsidian, strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Sync, null, tint = Obsidian, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (s.isSyncing) "Syncing..." else "Sync Now", color = Obsidian, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            s.lastSyncResult?.let { Spacer(Modifier.height(6.dp)); Text(it, style = MaterialTheme.typography.bodySmall, color = ElectricCyan) }

            // Cached match data
            SectionLabel("Cached Match Data")
            s.match?.let { m ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp)) {
                    Text("${m.team1} vs ${m.team2}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text("${m.score1} · ${m.overs1} ov", style = MaterialTheme.typography.bodyMedium)
                    s.prediction?.let { p -> Text("~${p.estimatedMinutesLeft} min left (${p.confidencePercent}% conf)", style = MaterialTheme.typography.bodySmall, color = ElectricCyan) }
                }
            } ?: Text("No cached data", style = MaterialTheme.typography.bodySmall, color = MutedText)

            SectionLabel("Sync History")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp)) {
                Text(if (s.syncStatus.lastSyncTime > 0) "Last: ${sdf.format(Date(s.syncStatus.lastSyncTime))}" else "Never synced", style = MaterialTheme.typography.bodySmall, color = MutedText)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
