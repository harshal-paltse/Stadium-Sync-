package com.stadiumsync.app.presentation.screen.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.stadiumsync.app.domain.model.*
import com.stadiumsync.app.domain.usecase.GetRouteSuggestionsUseCase
import com.stadiumsync.app.presentation.components.*
import com.stadiumsync.app.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RouteSuggestionViewModel @Inject constructor(private val getRouteSuggestions: GetRouteSuggestionsUseCase) : ViewModel() {
    private val _routes = MutableStateFlow(getRouteSuggestions())
    val routes: StateFlow<List<RouteSuggestion>> = _routes
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSuggestionScreen(onBack: () -> Unit = {}, vm: RouteSuggestionViewModel = hiltViewModel()) {
    val routes by vm.routes.collectAsState()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Route Suggestions") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(VividBlue.copy(alpha = 0.08f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Route, null, tint = VividBlue, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column { Text("Crowd Dispersal Routes", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)); Text("Based on real-time crowd density", style = MaterialTheme.typography.bodySmall, color = MutedText) }
                }
            }
            items(routes) { route ->
                val icon = when (route.transitType) { TransitType.METRO -> Icons.Filled.Train; TransitType.BUS -> Icons.Filled.DirectionsBus; TransitType.SHUTTLE -> Icons.Filled.AirportShuttle; TransitType.RIDE_PICKUP -> Icons.Filled.LocalTaxi }
                val accent = when (route.transitType) { TransitType.METRO -> VividBlue; TransitType.BUS -> ElectricCyan; TransitType.SHUTTLE -> DeepViolet; TransitType.RIDE_PICKUP -> SolarAmber }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.padding(16.dp)) {
                        Box(Modifier.width(3.dp).height(52.dp).background(accent))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(route.transitType.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                if (route.isRecommended) { Spacer(Modifier.width(8.dp)); SignalTag("Recommended") }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${route.fromLocation} → ${route.toLocation}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("${route.estimatedTimeMin} min", style = MaterialTheme.typography.labelLarge, color = accent)
                                SignalTag(route.crowdLevel.name, route.crowdLevel)
                            }
                        }
                    }
                }
            }
        }
    }
}
