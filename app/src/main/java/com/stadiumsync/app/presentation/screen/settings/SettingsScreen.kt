package com.stadiumsync.app.presentation.screen.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stadiumsync.app.core.datastore.UserPreferences
import com.stadiumsync.app.core.datastore.UserPreferencesData
import com.stadiumsync.app.domain.usecase.LogoutUseCase
import com.stadiumsync.app.presentation.components.*
import com.stadiumsync.app.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences, private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    val prefsData: StateFlow<UserPreferencesData> = prefs.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesData())
    fun setDarkMode(v: Boolean) { viewModelScope.launch { prefs.setDarkMode(v) } }
    fun setNotifications(v: Boolean) { viewModelScope.launch { prefs.setNotifications(v) } }
    fun setCriticalAlerts(v: Boolean) { viewModelScope.launch { prefs.setCriticalAlerts(v) } }
    fun setSyncInterval(v: Int) { viewModelScope.launch { prefs.setSyncInterval(v) } }
    fun logout(onDone: () -> Unit) { viewModelScope.launch { logoutUseCase(); onDone() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit = {}, onLogout: () -> Unit = {}, vm: SettingsViewModel = hiltViewModel()) {
    val p by vm.prefsData.collectAsState()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp)) {
            SectionLabel("Appearance")
            ToggleRow(Icons.Filled.DarkMode, "Dark Mode", "Switch theme", p.isDarkMode, vm::setDarkMode)

            SectionLabel("Notifications")
            ToggleRow(Icons.Filled.Notifications, "Push Notifications", "Transit & match alerts", p.notificationsEnabled, vm::setNotifications)
            ToggleRow(Icons.Filled.Warning, "Critical Alerts", "Always receive", p.criticalAlertsEnabled, vm::setCriticalAlerts)

            SectionLabel("Sync")
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Sync, null, tint = MutedText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text("Sync Interval", style = MaterialTheme.typography.titleSmall); Text("${p.syncIntervalMinutes} min", style = MaterialTheme.typography.bodySmall, color = MutedText) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1, 5, 15).forEach { m ->
                        FilterChip(selected = p.syncIntervalMinutes == m, onClick = { vm.setSyncInterval(m) }, label = { Text("${m}m") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VividBlue.copy(alpha = 0.15f), selectedLabelColor = VividBlue))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storage, null, tint = MutedText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Column { Text("API Endpoint", style = MaterialTheme.typography.titleSmall); Text(p.apiBaseUrl, style = MaterialTheme.typography.bodySmall, color = MutedText) }
            }

            SectionLabel("About")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoLine("Version", "1.0.0"); InfoLine("Build", "Production"); InfoLine("Operator", p.cachedUserName.ifEmpty { "—" }); InfoLine("Role", p.cachedUserRole)
            }
            Spacer(Modifier.height(24.dp))

            OutlinedButton(onClick = { vm.logout(onLogout) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HotCoral)) {
                Icon(Icons.Filled.Logout, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Sign Out")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MutedText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedText) }
        Switch(checked = checked, onCheckedChange = onChanged, colors = SwitchDefaults.colors(checkedTrackColor = ElectricCyan))
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedText)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
}
