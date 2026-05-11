package com.stadiumsync.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stadiumsync.app.domain.model.*
import com.stadiumsync.app.presentation.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/* ═══════════════════════════════════════════════════════════
   LIVE BEACON — pulsing dot with expanding ring
   ═══════════════════════════════════════════════════════════ */
@Composable
fun LiveBeacon(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "lb")
    val ring by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "r")
    val dot by inf.animateFloat(0.7f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "d")
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(18.dp)) {
            drawCircle(HotCoral.copy(alpha = (1f - ring) * 0.4f), radius = size.minDimension / 2 * ring)
            drawCircle(HotCoral.copy(alpha = dot), radius = 5f)
        }
        Spacer(Modifier.width(6.dp))
        Text("LIVE", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Black), color = HotCoral)
    }
}

/* ═══════════════════════════════════════════════════════════
   SIGNAL TAG — replaces generic "chip"
   ═══════════════════════════════════════════════════════════ */
@Composable
fun SignalTag(label: String, level: PressureLevel? = null, modifier: Modifier = Modifier) {
    val color = when (level) {
        PressureLevel.CRITICAL -> HotCoral; PressureLevel.HIGH -> SolarAmber
        PressureLevel.MODERATE -> VividBlue; PressureLevel.LOW -> ElectricCyan
        null -> when (label.uppercase()) {
            "LIVE", "ACTIVE", "SYNCED", "ONLINE" -> ElectricCyan
            "DELAYED", "HELD", "WARNING", "MODERATE" -> SolarAmber
            "CRITICAL", "HIGH CROWD", "EMERGENCY", "DIVERTED" -> HotCoral
            "OFFLINE" -> Ash
            else -> VividBlue
        }
    }
    Box(modifier.border(1.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/* ═══════════════════════════════════════════════════════════
   BIG NUMBER — the hero stat display
   ═══════════════════════════════════════════════════════════ */
@Composable
fun BigNumber(value: String, label: String, color: Color = MaterialTheme.colorScheme.onBackground, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.displayMedium, color = color)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedText)
    }
}

/* ═══════════════════════════════════════════════════════════
   ARC GAUGE — custom drawn radial gauge
   ═══════════════════════════════════════════════════════════ */
@Composable
fun ArcGauge(percent: Int, label: String, color: Color = ElectricCyan, size: Dp = 100.dp, modifier: Modifier = Modifier) {
    val anim by animateFloatAsState(percent.toFloat(), tween(1200, easing = FastOutSlowInEasing), label = "ag")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                val stroke = 8f
                drawArc(Color.Gray.copy(alpha = 0.1f), 135f, 270f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(
                    Brush.sweepGradient(listOf(color.copy(alpha = 0.3f), color)),
                    135f, 270f * anim / 100f, false, style = Stroke(stroke, cap = StrokeCap.Round)
                )
                // Needle dot
                val angle = Math.toRadians((135.0 + 270.0 * anim / 100.0))
                val r = this.size.minDimension / 2 - stroke
                val nx = center.x + r * cos(angle).toFloat()
                val ny = center.y + r * sin(angle).toFloat()
                drawCircle(Color.White, 5f, Offset(nx, ny))
                drawCircle(color, 3f, Offset(nx, ny))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$percent", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black), color = color)
                Text("%", style = MaterialTheme.typography.labelSmall, color = MutedText)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedText)
    }
}

/* ═══════════════════════════════════════════════════════════
   STAT BLOCK — compact metric with accent bar
   ═══════════════════════════════════════════════════════════ */
@Composable
fun StatBlock(value: String, label: String, accent: Color = VividBlue, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .drawBehind { drawRoundRect(accent, Offset.Zero, Size(4.dp.toPx(), size.height), CornerRadius(4f)) }
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedText)
        }
    }
}

/* ═══════════════════════════════════════════════════════════
   PRESSURE STRIP — horizontal gate pressure indicator
   ═══════════════════════════════════════════════════════════ */
@Composable
fun PressureStrip(gate: CrowdPressure, modifier: Modifier = Modifier) {
    val color = when (gate.pressureLevel) {
        PressureLevel.CRITICAL -> HotCoral; PressureLevel.HIGH -> SolarAmber
        PressureLevel.MODERATE -> VividBlue; PressureLevel.LOW -> ElectricCyan
    }
    val anim by animateFloatAsState(gate.densityPercent / 100f, tween(900), label = "ps")
    Row(modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically) {
        // Accent bar
        Box(Modifier.width(4.dp).fillMaxHeight().background(color))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(gate.gateName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("~${gate.estimatedPeople} people", style = MaterialTheme.typography.bodySmall, color = MutedText)
        }
        // Mini bar
        Box(Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.1f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(anim).clip(RoundedCornerShape(3.dp)).background(color))
        }
        Spacer(Modifier.width(10.dp))
        Text("${gate.densityPercent}%", style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.width(34.dp))
    }
}

/* ═══════════════════════════════════════════════════════════
   TRANSIT TILE — horizontal scrollable route card
   ═══════════════════════════════════════════════════════════ */
@Composable
fun TransitTile(route: TransitRoute, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val accent = when (route.type) { TransitType.METRO -> VividBlue; TransitType.BUS -> ElectricCyan; TransitType.SHUTTLE -> DeepViolet; TransitType.RIDE_PICKUP -> SolarAmber }
    val icon = when (route.type) { TransitType.METRO -> Icons.Filled.Train; TransitType.BUS -> Icons.Filled.DirectionsBus; TransitType.SHUTTLE -> Icons.Filled.AirportShuttle; TransitType.RIDE_PICKUP -> Icons.Filled.LocalTaxi }
    val loadPct = if (route.capacity > 0) route.currentLoad.toFloat() / route.capacity else 0f

    Column(modifier.width(200.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            SignalTag(route.status.name)
        }
        Spacer(Modifier.height(14.dp))
        Text(route.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
        Spacer(Modifier.height(10.dp))
        // Load indicator
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(accent.copy(alpha = 0.1f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(loadPct).clip(RoundedCornerShape(2.dp)).background(if (loadPct > 0.85f) HotCoral else accent))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${route.currentLoad}/${route.capacity}", style = MaterialTheme.typography.bodySmall, color = MutedText)
            if (route.estimatedDelayMin > 0) Text("+${route.estimatedDelayMin}m", style = MaterialTheme.typography.labelMedium, color = SolarAmber)
        }
    }
}

// Keep these for backwards compat
@Composable fun StatusChip(label: String, level: PressureLevel? = null, modifier: Modifier = Modifier) = SignalTag(label, level, modifier)
@Composable fun TransitStatusRow(route: TransitRoute, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) = TransitTile(route, onAction, modifier.width(IntrinsicSize.Max))

/* ═══════════════════════════════════════════════════════════
   SECTION DIVIDER — replaces card-based sections
   ═══════════════════════════════════════════════════════════ */
@Composable
fun SectionLabel(title: String, action: String = "", onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(VividBlue))
        Spacer(Modifier.width(10.dp))
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp), color = MutedText, modifier = Modifier.weight(1f))
        if (action.isNotEmpty()) TextButton(onClick = onAction) { Text(action, style = MaterialTheme.typography.labelMedium, color = VividBlue) }
    }
}

// Legacy aliases
@Composable fun SectionHeader(title: String, action: String = "", onAction: () -> Unit = {}) = SectionLabel(title, action, onAction)
@Composable fun StadiumCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) { Column(Modifier.padding(18.dp)) { content() } }
}
@Composable fun MetricCard(title: String, value: String, subtitle: String = "", icon: ImageVector = Icons.Filled.Analytics, accentColor: Color = VividBlue, gradient: Brush? = null, modifier: Modifier = Modifier) = StatBlock(value, title, accentColor, icon, modifier)
@Composable fun ConfidenceGauge(percent: Int, label: String = "Confidence", size: Int = 90, modifier: Modifier = Modifier) = ArcGauge(percent, label, ElectricCyan, size.dp, modifier)
@Composable fun CrowdPressureBar(gate: CrowdPressure, modifier: Modifier = Modifier) = PressureStrip(gate, modifier)
@Composable fun OfflineBanner(isVisible: Boolean, pendingCount: Int = 0) { if (!isVisible) return; Surface(Modifier.fillMaxWidth(), color = SolarAmber) { Row(Modifier.padding(10.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.CloudOff, null, tint = Obsidian, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(8.dp)); Text("OFFLINE", style = MaterialTheme.typography.labelSmall, color = Obsidian); if (pendingCount > 0) Text(" · $pendingCount queued", style = MaterialTheme.typography.labelSmall, color = Obsidian.copy(alpha = 0.7f)); Spacer(Modifier.weight(1f)); Text("cached data", style = MaterialTheme.typography.labelSmall, color = Obsidian.copy(alpha = 0.5f)) } } }
@Composable fun NotificationItem(alert: StadiumAlert, onClick: () -> Unit = {}) { val c = when (alert.priority) { AlertPriority.CRITICAL -> HotCoral; AlertPriority.WARNING -> SolarAmber; AlertPriority.INFO -> VividBlue }; Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (!alert.isRead) c.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(14.dp)) { Box(Modifier.width(3.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(c)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(alert.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (!alert.isRead) FontWeight.Bold else FontWeight.Normal)); Text(alert.message, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 2, overflow = TextOverflow.Ellipsis) }; SignalTag(alert.priority.name) } } }
@Composable fun LoadingScreen(message: String = "Loading...") { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = VividBlue, strokeWidth = 2.dp); Spacer(Modifier.height(16.dp)); Text(message, style = MaterialTheme.typography.bodySmall, color = MutedText) } } }
@Composable fun ErrorState(message: String, onRetry: () -> Unit = {}) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.ErrorOutline, null, Modifier.size(48.dp), HotCoral.copy(alpha = 0.6f)); Spacer(Modifier.height(12.dp)); Text(message, style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.height(16.dp)); OutlinedButton(onClick = onRetry) { Text("Retry") } } } }

/* ═══════════════════════════════════════════════════════════
   RADAR CHART — stadium-shaped crowd overview
   ═══════════════════════════════════════════════════════════ */
@Composable
fun StadiumRadar(gates: List<CrowdPressure>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(220.dp)) {
        val cx = size.width / 2; val cy = size.height / 2; val r = size.minDimension / 2 * 0.8f
        val n = gates.size.coerceAtLeast(1)
        // Background rings
        for (ring in 1..4) {
            val rr = r * ring / 4
            drawCircle(Color.Gray.copy(alpha = 0.06f), rr, Offset(cx, cy), style = Stroke(1f))
        }
        // Radar polygon
        val path = Path()
        gates.forEachIndexed { i, g ->
            val angle = 2 * PI * i / n - PI / 2
            val gr = r * g.densityPercent / 100f
            val x = cx + gr * cos(angle).toFloat()
            val y = cy + gr * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, Brush.radialGradient(listOf(ElectricCyan.copy(alpha = 0.3f), VividBlue.copy(alpha = 0.05f)), Offset(cx, cy), r))
        drawPath(path, ElectricCyan.copy(alpha = 0.6f), style = Stroke(2f))
        // Gate dots
        gates.forEachIndexed { i, g ->
            val angle = 2 * PI * i / n - PI / 2
            val gr = r * g.densityPercent / 100f
            val x = cx + gr * cos(angle).toFloat(); val y = cy + gr * sin(angle).toFloat()
            val dotColor = when (g.pressureLevel) { PressureLevel.CRITICAL -> HotCoral; PressureLevel.HIGH -> SolarAmber; PressureLevel.MODERATE -> VividBlue; PressureLevel.LOW -> ElectricCyan }
            drawCircle(Color.White, 5f, Offset(x, y)); drawCircle(dotColor, 3.5f, Offset(x, y))
            // Label position
            val lx = cx + (r + 18f) * cos(angle).toFloat(); val ly = cy + (r + 18f) * sin(angle).toFloat()
            drawCircle(dotColor.copy(alpha = 0.15f), 12f, Offset(lx, ly))
        }
    }
}

/* ═══════════════════════════════════════════════════════════
   WAVE CHART — smooth crowd wave visualization
   ═══════════════════════════════════════════════════════════ */
@Composable
fun WaveChart(data: List<Float>, color: Color = ElectricCyan, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        val w = size.width; val h = size.height; val max = data.maxOrNull() ?: 1f
        val area = Path().apply {
            moveTo(0f, h)
            data.forEachIndexed { i, v ->
                val x = w * i / (data.size - 1).coerceAtLeast(1)
                val y = h - (v / max * h * 0.85f)
                if (i == 0) lineTo(x, y)
                else { val prev = w * (i - 1) / (data.size - 1).coerceAtLeast(1); val prevY = h - (data[i - 1] / max * h * 0.85f); cubicTo(prev + (x - prev) / 2, prevY, prev + (x - prev) / 2, y, x, y) }
            }
            lineTo(w, h); close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent)))
        val line = Path().apply {
            data.forEachIndexed { i, v ->
                val x = w * i / (data.size - 1).coerceAtLeast(1); val y = h - (v / max * h * 0.85f)
                if (i == 0) moveTo(x, y)
                else { val prev = w * (i - 1) / (data.size - 1).coerceAtLeast(1); val prevY = h - (data[i - 1] / max * h * 0.85f); cubicTo(prev + (x - prev) / 2, prevY, prev + (x - prev) / 2, y, x, y) }
            }
        }
        drawPath(line, color, style = Stroke(3f, cap = StrokeCap.Round))
    }
}

/* ═══════════════════════════════════════════════════════════
   STADIUM 3D VIEW — isometric perspective stadium
   ═══════════════════════════════════════════════════════════ */
@Composable
fun Stadium3DView(
    highlightPavilion: Pavilion? = null,
    sections: List<StadiumSection> = emptyList(),
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "s3d")
    val pulse by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "p3d")

    Canvas(modifier.fillMaxWidth().height(280.dp)) {
        val cx = size.width / 2; val cy = size.height / 2
        val ovalW = size.width * 0.85f; val ovalH = size.height * 0.55f

        // Shadow
        drawOval(Color.Black.copy(alpha = 0.15f), Offset(cx - ovalW / 2, cy - ovalH / 2 + 20f), Size(ovalW, ovalH))

        // Base ring (field)
        val fieldW = ovalW * 0.5f; val fieldH = ovalH * 0.5f
        drawOval(Color(0xFF1B5E20).copy(alpha = 0.3f), Offset(cx - fieldW / 2, cy - fieldH / 2), Size(fieldW, fieldH))
        drawOval(Color(0xFF2E7D32), Offset(cx - fieldW / 2, cy - fieldH / 2), Size(fieldW, fieldH), style = Stroke(2f))

        // Pitch rectangle
        val pitchW = fieldW * 0.12f; val pitchH = fieldH * 0.5f
        drawRoundRect(Color(0xFFE8D5A3), Offset(cx - pitchW / 2, cy - pitchH / 2), Size(pitchW, pitchH), CornerRadius(4f))

        // Stadium sections (8 pavilions around the oval)
        val pavilions = Pavilion.entries
        pavilions.forEachIndexed { i, pav ->
            val angle = 2 * PI * i / pavilions.size - PI / 2
            val innerR = Offset((ovalW * 0.3f * cos(angle)).toFloat(), (ovalH * 0.3f * sin(angle)).toFloat())
            val outerR = Offset((ovalW * 0.47f * cos(angle)).toFloat(), (ovalH * 0.47f * sin(angle)).toFloat())
            val secW = ovalW * 0.18f; val secH = ovalH * 0.18f
            val secCenter = Offset(cx + (innerR.x + outerR.x) / 2, cy + (innerR.y + outerR.y) / 2)
            val isHighlighted = pav == highlightPavilion
            val secData = sections.filter { it.pavilion == pav }
            val occupancy = if (secData.isNotEmpty()) secData.sumOf { it.occupiedSeats }.toFloat() / secData.sumOf { it.totalSeats } else 0.5f

            val baseColor = when {
                isHighlighted -> ElectricCyan
                occupancy > 0.85f -> HotCoral
                occupancy > 0.6f -> SolarAmber
                else -> VividBlue
            }
            val alpha = if (isHighlighted) pulse else 0.6f

            // Section arc block
            drawRoundRect(baseColor.copy(alpha = alpha * 0.4f), Offset(secCenter.x - secW / 2, secCenter.y - secH / 2), Size(secW, secH), CornerRadius(6f))
            drawRoundRect(baseColor.copy(alpha = alpha), Offset(secCenter.x - secW / 2, secCenter.y - secH / 2), Size(secW, secH), CornerRadius(6f), style = Stroke(if (isHighlighted) 3f else 1.5f))

            // 3D depth effect
            if (i < pavilions.size / 2) {
                drawRoundRect(baseColor.copy(alpha = alpha * 0.15f), Offset(secCenter.x - secW / 2, secCenter.y + secH / 2 - 2f), Size(secW, 8f), CornerRadius(3f))
            }
        }

        // Center circle on field
        drawCircle(Color(0xFF2E7D32), fieldW * 0.12f, Offset(cx, cy), style = Stroke(1.5f))

        // Outer boundary
        drawOval(Brush.sweepGradient(listOf(VividBlue.copy(alpha = 0.4f), ElectricCyan.copy(alpha = 0.2f), VividBlue.copy(alpha = 0.4f))),
            Offset(cx - ovalW / 2, cy - ovalH / 2), Size(ovalW, ovalH), style = Stroke(2f))
    }
}

/* ═══════════════════════════════════════════════════════════
   PAVILION MAP VIEW — top-down stadium map with labels
   ═══════════════════════════════════════════════════════════ */
@Composable
fun PavilionMapView(
    highlightPavilion: Pavilion? = null,
    highlightSection: String = "",
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "pmap")
    val glow by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "glow")

    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Text("PAVILION MAP", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp), color = MutedText)
        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(200.dp)) {
            val cx = size.width / 2; val cy = size.height / 2
            val rX = size.width * 0.42f; val rY = size.height * 0.42f

            // Field
            drawOval(Color(0xFF1B5E20).copy(alpha = 0.2f), Offset(cx - rX * 0.5f, cy - rY * 0.5f), Size(rX, rY))

            val pavilions = Pavilion.entries
            pavilions.forEachIndexed { i, pav ->
                val angle = 2 * PI * i / pavilions.size - PI / 2
                val dist = 0.78f
                val px = cx + (rX * dist * cos(angle)).toFloat()
                val py = cy + (rY * dist * sin(angle)).toFloat()
                val isHL = pav == highlightPavilion
                val col = if (isHL) ElectricCyan else VividBlue.copy(alpha = 0.4f)
                val rad = if (isHL) 24f else 18f

                drawCircle(col.copy(alpha = if (isHL) glow * 0.3f else 0.1f), rad + 8f, Offset(px, py))
                drawCircle(col, rad, Offset(px, py))
                drawCircle(if (isHL) Color.White else col.copy(alpha = 0.6f), rad, Offset(px, py), style = Stroke(2f))
            }
        }
        // Legend
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Pavilion.entries.take(4).forEach { pav ->
                val isHL = pav == highlightPavilion
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(if (isHL) ElectricCyan else VividBlue.copy(alpha = 0.3f)))
                    Text(pav.shortName, style = MaterialTheme.typography.labelSmall, color = if (isHL) ElectricCyan else MutedText)
                }
            }
        }
        if (highlightPavilion != null) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ElectricCyan.copy(alpha = 0.1f)).padding(8.dp)) {
                Text("📍 ${highlightPavilion.displayName} — Section $highlightSection", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════
   SEAT NAVIGATOR — interactive section seat grid
   ═══════════════════════════════════════════════════════════ */
@Composable
fun SeatNavigator(
    pavilion: Pavilion,
    section: String,
    highlightRow: String = "",
    highlightSeat: Int = -1,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(ElectricCyan))
            Spacer(Modifier.width(8.dp))
            Text("SEAT NAVIGATOR", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp), color = MutedText)
            Spacer(Modifier.weight(1f))
            SignalTag(pavilion.shortName)
        }
        Spacer(Modifier.height(12.dp))

        // Stage/pitch indicator
        Box(Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(Color(0xFF1B5E20).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Text("⬇ PITCH SIDE ⬇", style = MaterialTheme.typography.labelSmall, color = MutedText)
        }
        Spacer(Modifier.height(8.dp))

        // Seat grid
        val rows = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        val seatsPerRow = 10
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Text(row, style = MaterialTheme.typography.labelSmall, color = MutedText, modifier = Modifier.width(16.dp))
                (1..seatsPerRow).forEach { seat ->
                    val isHighlighted = row == highlightRow && seat == highlightSeat
                    val isOccupied = (row.hashCode() + seat) % 3 != 0
                    val color = when {
                        isHighlighted -> ElectricCyan
                        isOccupied -> HotCoral.copy(alpha = 0.4f)
                        else -> Color.Gray.copy(alpha = 0.15f)
                    }
                    Box(
                        Modifier.size(if (isHighlighted) 22.dp else 18.dp).clip(RoundedCornerShape(3.dp)).background(color)
                            .then(if (isHighlighted) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(3.dp)) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isHighlighted) Text("$seat", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp), color = Color.White)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(ElectricCyan)); Spacer(Modifier.width(4.dp)); Text("Your Seat", style = MaterialTheme.typography.labelSmall, color = MutedText) }
            Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(HotCoral.copy(alpha = 0.4f))); Spacer(Modifier.width(4.dp)); Text("Occupied", style = MaterialTheme.typography.labelSmall, color = MutedText) }
            Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color.Gray.copy(alpha = 0.15f))); Spacer(Modifier.width(4.dp)); Text("Available", style = MaterialTheme.typography.labelSmall, color = MutedText) }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//   LIVE SCORE STRIP — compact live match score
// ═══════════════════════════════════════════════════════════
@Composable
fun LiveScoreStrip(match: Match?, modifier: Modifier = Modifier) {
    if (match == null) return
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Gunmetal, Slate))).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        LiveBeacon(Modifier)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${match.team1} vs ${match.team2}", style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${match.score1} (${match.overs1} ov)", style = MaterialTheme.typography.bodySmall, color = ElectricCyan)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(match.matchPhase.name.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = SolarAmber)
            Text("RR ${String.format("%.1f", match.currentRunRate)}", style = MaterialTheme.typography.labelSmall, color = MutedText)
        }
    }
}


// ═══════════════════════════════════════════════════════════
//   STADIUM MAP — interactive map with gates, food, exits
// ═══════════════════════════════════════════════════════════
@Composable
fun StadiumMapView(
    pois: List<StadiumPOI>,
    highlightPavilion: Pavilion? = null,
    selectedFilter: POIType? = null,
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "smap")
    val pulse by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "mp")

    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(VividBlue))
            Spacer(Modifier.width(8.dp))
            Text("STADIUM MAP", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp), color = MutedText)
        }
        Spacer(Modifier.height(12.dp))

        Canvas(Modifier.fillMaxWidth().height(300.dp)) {
            val cx = size.width / 2; val cy = size.height / 2
            val rX = size.width * 0.42f; val rY = size.height * 0.40f

            // Outer boundary
            drawOval(Color.Gray.copy(alpha = 0.08f), Offset(cx - rX - 20, cy - rY - 20), Size((rX + 20) * 2, (rY + 20) * 2))
            drawOval(VividBlue.copy(alpha = 0.2f), Offset(cx - rX, cy - rY), Size(rX * 2, rY * 2), style = Stroke(2f))

            // Field
            val fX = rX * 0.5f; val fY = rY * 0.5f
            drawOval(Color(0xFF1B5E20).copy(alpha = 0.25f), Offset(cx - fX, cy - fY), Size(fX * 2, fY * 2))
            drawOval(Color(0xFF2E7D32), Offset(cx - fX, cy - fY), Size(fX * 2, fY * 2), style = Stroke(1.5f))
            // Pitch
            drawRoundRect(Color(0xFFE8D5A3), Offset(cx - 8f, cy - fY * 0.35f), Size(16f, fY * 0.7f), CornerRadius(3f))
            drawCircle(Color(0xFF2E7D32), fX * 0.15f, Offset(cx, cy), style = Stroke(1f))

            // Pavilion sections
            Pavilion.entries.forEachIndexed { i, pav ->
                val angle = 2 * PI * i / Pavilion.entries.size - PI / 2
                val secDist = 0.72f
                val px = cx + (rX * secDist * cos(angle)).toFloat()
                val py = cy + (rY * secDist * sin(angle)).toFloat()
                val isHL = pav == highlightPavilion
                val col = if (isHL) ElectricCyan else VividBlue.copy(alpha = 0.3f)
                drawRoundRect(col.copy(alpha = if (isHL) pulse * 0.5f else 0.15f), Offset(px - 22f, py - 12f), Size(44f, 24f), CornerRadius(6f))
                drawRoundRect(col, Offset(px - 22f, py - 12f), Size(44f, 24f), CornerRadius(6f), style = Stroke(if (isHL) 2f else 1f))
            }

            // POI markers
            val filteredPois = if (selectedFilter != null) pois.filter { it.type == selectedFilter } else pois
            filteredPois.forEach { poi ->
                val angle = Math.toRadians(poi.angleDeg.toDouble())
                val px = cx + (rX * poi.distanceRatio * cos(angle)).toFloat()
                val py = cy + (rY * poi.distanceRatio * sin(angle)).toFloat()
                val poiColor = when (poi.type) {
                    POIType.GATE -> SolarAmber
                    POIType.FOOD_COURT -> Color(0xFFFF6B35)
                    POIType.EXIT -> ElectricCyan
                    POIType.RESTROOM -> VividBlue
                    POIType.FIRST_AID -> HotCoral
                    POIType.MERCHANDISE -> DeepViolet
                    POIType.ATM -> Color(0xFF4CAF50)
                    POIType.PARKING -> Ash
                }
                // Marker background
                drawCircle(poiColor.copy(alpha = 0.2f), 16f, Offset(px, py))
                drawCircle(poiColor, 11f, Offset(px, py))
                drawCircle(Color.White, 11f, Offset(px, py), style = Stroke(1.5f))
            }
        }

        // POI Legend grid
        Spacer(Modifier.height(12.dp))
        val types = POIType.entries
        for (row in types.chunked(4)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { type ->
                    val isSelected = selectedFilter == type
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val col = when (type) {
                            POIType.GATE -> SolarAmber; POIType.FOOD_COURT -> Color(0xFFFF6B35)
                            POIType.EXIT -> ElectricCyan; POIType.RESTROOM -> VividBlue
                            POIType.FIRST_AID -> HotCoral; POIType.MERCHANDISE -> DeepViolet
                            POIType.ATM -> Color(0xFF4CAF50); POIType.PARKING -> Ash
                        }
                        Box(Modifier.size(8.dp).clip(CircleShape).background(col))
                        Spacer(Modifier.width(4.dp))
                        Text("${type.emoji} ${type.label}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = if (isSelected) col else MutedText)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//   TICKET CARD — animated scanned ticket display
// ═══════════════════════════════════════════════════════════
@Composable
fun TicketCard(ticket: Ticket, modifier: Modifier = Modifier) {
    val statusColor = when (ticket.status) {
        TicketStatus.VALID -> Color(0xFF4CAF50)
        TicketStatus.ALREADY_SCANNED -> SolarAmber
        TicketStatus.EXPIRED -> HotCoral
        TicketStatus.INVALID -> HotCoral
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Gunmetal, Color(0xFF1A1A2E), Slate)
                )
            )
            .border(1.dp, VividBlue.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("STADIUM SYNC", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp), color = MutedText)
            SignalTag(ticket.status.name.replace("_", " "))
        }
        Spacer(Modifier.height(14.dp))
        Text(ticket.holderName, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(12.dp))
        // Dotted divider
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            for (x in 0..size.width.toInt() step 8) {
                drawCircle(MutedText.copy(alpha = 0.3f), 1.5f, Offset(x.toFloat(), 0f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("PAVILION", style = MaterialTheme.typography.labelSmall, color = MutedText)
                Text(ticket.pavilion.displayName, style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
            }
            Column(Modifier.weight(1f)) {
                Text("SECTION", style = MaterialTheme.typography.labelSmall, color = MutedText)
                Text(ticket.section, style = MaterialTheme.typography.titleSmall, color = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("ROW / SEAT", style = MaterialTheme.typography.labelSmall, color = MutedText)
                Text("${ticket.seatRow}-${ticket.seatNumber}", style = MaterialTheme.typography.titleSmall, color = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text("GATE", style = MaterialTheme.typography.labelSmall, color = MutedText)
                Text(ticket.gate, style = MaterialTheme.typography.titleSmall, color = SolarAmber)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.1f)).padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ticket.status == TicketStatus.VALID) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    null, tint = statusColor, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("ID: ${ticket.ticketId.take(12)}...", style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
        }
    }
}
