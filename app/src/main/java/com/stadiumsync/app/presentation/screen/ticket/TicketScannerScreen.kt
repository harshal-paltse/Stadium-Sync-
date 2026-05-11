package com.stadiumsync.app.presentation.screen.ticket

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.stadiumsync.app.domain.model.*
import com.stadiumsync.app.domain.usecase.*
import com.stadiumsync.app.notification.NotificationRulesEngine
import com.stadiumsync.app.presentation.components.*
import com.stadiumsync.app.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TicketUiState(
    val scannedTicket: Ticket? = null,
    val ticketHistory: List<Ticket> = emptyList(),
    val sections: List<StadiumSection> = emptyList(),
    val match: Match? = null,
    val isScanning: Boolean = false,
    val ticketCode: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class TicketScannerViewModel @Inject constructor(
    private val scanTicket: ScanTicketUseCase,
    private val getScannedTickets: GetScannedTicketsUseCase,
    private val getStadiumSections: GetStadiumSectionsUseCase,
    private val getLiveMatch: GetLiveMatchUseCase,
    private val rulesEngine: NotificationRulesEngine
) : ViewModel() {
    private val _state = MutableStateFlow(TicketUiState())
    val state: StateFlow<TicketUiState> = _state

    init {
        _state.update { it.copy(sections = getStadiumSections()) }
        viewModelScope.launch {
            getScannedTickets().collect { tickets ->
                _state.update { it.copy(ticketHistory = tickets) }
            }
        }
        viewModelScope.launch {
            getLiveMatch().collect { match ->
                _state.update { it.copy(match = match) }
            }
        }
    }

    fun updateTicketCode(code: String) {
        _state.update { it.copy(ticketCode = code, errorMessage = null) }
    }

    fun scanTicket() {
        val code = _state.value.ticketCode.ifBlank { "TKT-${UUID.randomUUID().toString().take(8).uppercase()}" }
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true, errorMessage = null) }
            kotlinx.coroutines.delay(1200) // Simulate scan delay
            scanTicket(code).onSuccess { ticket ->
                _state.update { it.copy(scannedTicket = ticket, isScanning = false, ticketCode = "") }
                rulesEngine.onTicketScanned(ticket)
            }.onFailure { e ->
                _state.update { it.copy(isScanning = false, errorMessage = e.message ?: "Scan failed") }
            }
        }
    }

    fun quickScan() {
        _state.update { it.copy(ticketCode = "") }
        scanTicket()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScannerScreen(
    onBack: () -> Unit = {},
    vm: TicketScannerViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Ticket Scanner") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
            actions = {
                IconButton(onClick = {}) { Icon(Icons.Filled.QrCodeScanner, "Scan", tint = ElectricCyan) }
            }
        )
    }) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ═══ LIVE SCORE ══════════════════════════════
            item {
                LiveScoreStrip(s.match)
            }

            // ═══ SCAN SECTION ════════════════════════════
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(20.dp)
                ) {
                    Text("SCAN TICKET", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp), color = MutedText)
                    Spacer(Modifier.height(16.dp))

                    // Manual code input
                    OutlinedTextField(
                        value = s.ticketCode,
                        onValueChange = vm::updateTicketCode,
                        label = { Text("Ticket Code (optional)") },
                        placeholder = { Text("e.g. TKT-A1B2C3D4") },
                        leadingIcon = { Icon(Icons.Filled.ConfirmationNumber, null, tint = VividBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))

                    // Scan buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = vm::quickScan,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VividBlue),
                            enabled = !s.isScanning
                        ) {
                            if (s.isScanning) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.QrCodeScanner, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Quick Scan", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        OutlinedButton(
                            onClick = vm::scanTicket,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !s.isScanning && s.ticketCode.isNotBlank()
                        ) {
                            Icon(Icons.Filled.Search, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Lookup", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    s.errorMessage?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Text(err, style = MaterialTheme.typography.bodySmall, color = HotCoral)
                    }
                }
            }

            // ═══ SCANNED TICKET RESULT ═══════════════════
            s.scannedTicket?.let { ticket ->
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                        TicketCard(ticket)
                    }
                }

                // ═══ 3D STADIUM VIEW ═════════════════════
                item {
                    SectionLabel("3D Stadium View")
                    Stadium3DView(
                        highlightPavilion = ticket.pavilion,
                        sections = s.sections,
                        modifier = Modifier.padding(horizontal = 0.dp)
                    )
                    // Pavilion labels below 3D view
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Pavilion.entries.take(4).forEach { pav ->
                            val isHL = pav == ticket.pavilion
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(if (isHL) ElectricCyan else Ash.copy(alpha = 0.3f)))
                                Text(pav.shortName, style = MaterialTheme.typography.labelSmall,
                                    color = if (isHL) ElectricCyan else MutedText, fontWeight = if (isHL) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // ═══ PAVILION MAP ════════════════════════
                item {
                    SectionLabel("Your Pavilion")
                    PavilionMapView(highlightPavilion = ticket.pavilion, highlightSection = ticket.section)
                }

                // ═══ SEAT NAVIGATOR ══════════════════════
                item {
                    SectionLabel("Find Your Seat")
                    SeatNavigator(
                        pavilion = ticket.pavilion,
                        section = ticket.section,
                        highlightRow = ticket.seatRow,
                        highlightSeat = ticket.seatNumber.coerceIn(1, 10)
                    )
                }

                // ═══ METRO CONTACT INFO ══════════════════
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(DeepViolet.copy(alpha = 0.15f), VividBlue.copy(alpha = 0.1f))))
                        .padding(16.dp)
                    ) {
                        Text("🚇 METRO COORDINATION", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = DeepViolet)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Phone, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("9359947410", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Email, null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("harshalpaltse@gmail.com", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Contact for metro schedule & transport coordination", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    }
                }
            }

            // ═══ STADIUM SECTIONS OVERVIEW ═══════════════
            if (s.scannedTicket == null) {
                item {
                    SectionLabel("Stadium Sections")
                    Stadium3DView(sections = s.sections)
                }
            }

            // ═══ SCAN HISTORY ════════════════════════════
            if (s.ticketHistory.isNotEmpty()) {
                item {
                    SectionLabel("Scan History")
                }
                items(s.ticketHistory.take(10), key = { "${it.ticketId}_${it.scannedAt}" }) { ticket ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(ElectricCyan.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center) {
                            Text(ticket.pavilion.shortName, style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ticket.holderName, style = MaterialTheme.typography.titleSmall)
                            Text("${ticket.pavilion.displayName} · ${ticket.section} · Row ${ticket.seatRow}-${ticket.seatNumber}",
                                style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        SignalTag(ticket.status.name.replace("_", " "))
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
