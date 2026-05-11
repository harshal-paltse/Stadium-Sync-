package com.stadiumsync.app.presentation.screen.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

data class NotifUiState(val alerts: List<StadiumAlert> = emptyList(), val filter: AlertPriority? = null)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotifications: GetNotificationsUseCase,
    private val notifRepo: com.stadiumsync.app.domain.repository.NotificationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NotifUiState())
    val state: StateFlow<NotifUiState> = _state
    init { viewModelScope.launch { getNotifications().collect { a -> _state.update { it.copy(alerts = a) } } } }
    fun setFilter(f: AlertPriority?) { _state.update { it.copy(filter = f) } }
    fun markRead(id: String) { viewModelScope.launch { notifRepo.markRead(id) } }
    fun markAllRead() { viewModelScope.launch { notifRepo.markAllRead() } }
    fun clearAll() { viewModelScope.launch { notifRepo.clearAll() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit = {}, vm: NotificationsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val filtered = s.filter?.let { f -> s.alerts.filter { it.priority == f } } ?: s.alerts

    Scaffold(topBar = {
        TopAppBar(title = { Text("Notifications") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
            actions = {
                IconButton(onClick = vm::markAllRead) { Icon(Icons.Filled.DoneAll, "Mark all read", tint = ElectricCyan) }
                IconButton(onClick = vm::clearAll) { Icon(Icons.Filled.DeleteSweep, "Clear", tint = MutedText) }
            })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(s.filter == null, onClick = { vm.setFilter(null) }, label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VividBlue.copy(alpha = 0.15f), selectedLabelColor = VividBlue))
                FilterChip(s.filter == AlertPriority.CRITICAL, onClick = { vm.setFilter(AlertPriority.CRITICAL) }, label = { Text("Critical") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = HotCoral.copy(alpha = 0.15f), selectedLabelColor = HotCoral))
                FilterChip(s.filter == AlertPriority.WARNING, onClick = { vm.setFilter(AlertPriority.WARNING) }, label = { Text("Warning") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SolarAmber.copy(alpha = 0.15f), selectedLabelColor = SolarAmber))
                FilterChip(s.filter == AlertPriority.INFO, onClick = { vm.setFilter(AlertPriority.INFO) }, label = { Text("Info") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VividBlue.copy(alpha = 0.15f), selectedLabelColor = VividBlue))
            }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.NotificationsNone, null, Modifier.size(40.dp), MutedText)
                        Spacer(Modifier.height(12.dp))
                        Text("No notifications", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(filtered, key = { it.id }) { NotificationItem(it, onClick = { vm.markRead(it.id) }) }
                }
            }
        }
    }
}
