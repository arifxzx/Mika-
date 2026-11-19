package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Song
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.mikaScaledIcon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class DonutSegmentItem(
    val label: String,
    val count: Int,
    val color: Color,
    val detail: String = ""
)

@Composable
fun StatsGlassCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    MikaGlassSurface(
        shape = RoundedCornerShape(20.dp),
        tintColor = accent,
        tintStrength = 0.06f,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(mikaScaledIcon(16.dp))
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
fun StatsSummaryTile(
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(mikaScaledIcon(18.dp))
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatsMetricPill(
    value: String,
    label: String,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(mikaScaledIcon(14.dp))
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MonthsBarChart(
    monthlyCounts: IntArray,
    accent: Color,
    secondaryAccent: Color,
    progress: Float
) {
    val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val maxVal = (monthlyCounts.maxOrNull() ?: 0).coerceAtLeast(1)
    val trackBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
        ) {
            val count = 12
            val slotW = size.width / count
            val barW = (slotW * 0.56f).coerceIn(6.dp.toPx(), 20.dp.toPx())
            for (i in 0 until count) {
                val x = i * slotW + (slotW - barW) / 2f
                drawRoundRect(
                    color = trackBg,
                    topLeft = Offset(x, 4.dp.toPx()),
                    size = Size(barW, size.height - 4.dp.toPx()),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
                val valCount = monthlyCounts.getOrElse(i) { 0 }
                if (valCount > 0) {
                    val ratio = (valCount.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 1f) * progress
                    val barH = ((size.height - 4.dp.toPx()) * ratio).coerceAtLeast(6.dp.toPx())
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(secondaryAccent, accent)),
                        topLeft = Offset(x, size.height - barH),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { m ->
                Text(
                    text = m,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TotalListensAreaChart(
    points: List<Float>,
    startLabel: String,
    midLabel: String,
    endLabel: String,
    accent: Color,
    secondaryAccent: Color,
    progress: Float
) {
    val safePoints = if (points.size >= 2) points else listOf(0f, points.firstOrNull() ?: 1f)
    val maxVal = (safePoints.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val topPad = 10.dp.toPx()
            val bottomPad = 10.dp.toPx()
            val usableH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            for (g in 0..3) {
                val gy = topPad + usableH * (g / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, gy),
                    end = Offset(size.width, gy),
                    strokeWidth = 1.dp.toPx()
                )
            }
            val stepX = size.width / (safePoints.size - 1).coerceAtLeast(1)
            val offsets = safePoints.mapIndexed { idx, v ->
                val norm = (v / maxVal).coerceIn(0f, 1f) * progress
                Offset(idx * stepX, topPad + usableH * (1f - norm))
            }
            val linePath = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                for (i in 0 until offsets.lastIndex) {
                    val mx = (offsets[i].x + offsets[i + 1].x) / 2f
                    cubicTo(mx, offsets[i].y, mx, offsets[i + 1].y, offsets[i + 1].x, offsets[i + 1].y)
                }
            }
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(offsets.last().x, topPad + usableH)
                lineTo(offsets.first().x, topPad + usableH)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.34f), secondaryAccent.copy(alpha = 0.10f), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(listOf(accent, secondaryAccent)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            val endPt = offsets.last()
            drawCircle(color = accent.copy(alpha = 0.25f), radius = 8.dp.toPx(), center = endPt)
            drawCircle(color = accent, radius = 4.dp.toPx(), center = endPt)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(midLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(endLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DaysHeatmapGrid(
    weeks: List<IntArray>, // each week has 7 days (Sun..Sat)
    monthHeaders: List<String>,
    accent: Color
) {
    val maxDay = weeks.maxOfOrNull { w -> w.maxOrNull() ?: 0 }?.coerceAtLeast(1) ?: 1
    val emptyCell = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 18.dp, end = 4.dp)) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                    Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                        Text(d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            weeks.forEachIndexed { wIdx, weekCounts ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = monthHeaders.getOrElse(wIdx) { "" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.height(14.dp)
                    )
                    for (dayIdx in 0 until 7) {
                        val c = weekCounts.getOrElse(dayIdx) { 0 }
                        val levelAlpha = when {
                            c <= 0 -> 0f
                            else -> (0.25f + 0.75f * (c.toFloat() / maxDay.toFloat())).coerceIn(0.25f, 1f)
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (c <= 0) emptyCell else accent.copy(alpha = levelAlpha))
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            listOf(0f, 0.28f, 0.52f, 0.76f, 1f).forEach { a ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (a == 0f) emptyCell else accent.copy(alpha = a))
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ListeningClockRadialChart(
    hourCounts: IntArray, // 24 hours 0..23
    peakHourLabel: String,
    peakDayLabel: String,
    accent: Color,
    secondaryAccent: Color,
    progress: Float
) {
    val maxHour = (hourCounts.maxOrNull() ?: 0).coerceAtLeast(1)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("12 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "6 PM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 10.dp)
            )
            Box(modifier = Modifier.size(174.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val innerR = size.minDimension * 0.28f
                    val maxArm = size.minDimension * 0.21f
                    drawCircle(
                        color = trackColor,
                        radius = innerR + maxArm * 0.5f,
                        center = Offset(cx, cy),
                        style = Stroke(width = maxArm)
                    )
                    for (h in 0 until 24) {
                        val angleDeg = h * 15f - 90f
                        val rad = (angleDeg * PI / 180.0).toFloat()
                        val count = hourCounts.getOrElse(h) { 0 }
                        val ratio = if (count <= 0) 0.08f else (0.18f + 0.82f * (count.toFloat() / maxHour.toFloat())) * progress
                        val outerR = innerR + maxArm * ratio
                        val p1 = Offset(cx + cos(rad) * innerR, cy + sin(rad) * innerR)
                        val p2 = Offset(cx + cos(rad) * outerR, cy + sin(rad) * outerR)
                        drawLine(
                            color = if (count > 0) accent.copy(alpha = 0.45f + 0.55f * (count.toFloat() / maxHour)) else secondaryAccent.copy(alpha = 0.16f),
                            start = p1,
                            end = p2,
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = peakHourLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = peakDayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "6 AM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        Text("12 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WeekdayBarChart(
    weekdayCounts: IntArray, // 7 days Sun..Sat
    accent: Color,
    secondaryAccent: Color,
    progress: Float
) {
    val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val maxDay = (weekdayCounts.maxOrNull() ?: 0).coerceAtLeast(1)
    val trackBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
        ) {
            val slotW = size.width / 7f
            val barW = (slotW * 0.46f).coerceIn(10.dp.toPx(), 24.dp.toPx())
            for (i in 0 until 7) {
                val x = i * slotW + (slotW - barW) / 2f
                drawRoundRect(
                    color = trackBg,
                    topLeft = Offset(x, 2.dp.toPx()),
                    size = Size(barW, size.height - 2.dp.toPx()),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
                val v = weekdayCounts.getOrElse(i) { 0 }
                if (v > 0) {
                    val ratio = (v.toFloat() / maxDay.toFloat()).coerceIn(0.10f, 1f) * progress
                    val h = ((size.height - 2.dp.toPx()) * ratio).coerceAtLeast(6.dp.toPx())
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(secondaryAccent, accent)),
                        topLeft = Offset(x, size.height - h),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DayHourMatrixHeatmap(
    matrix: Array<IntArray>, // 7 rows x 24 cols
    accent: Color
) {
    val maxVal = matrix.maxOfOrNull { row -> row.maxOrNull() ?: 0 }?.coerceAtLeast(1) ?: 1
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 30.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "03", "06", "09", "12", "15", "18", "21").forEach { h ->
                Text(h, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        days.forEachIndexed { dIdx, dLabel ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = dLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp)
                )
                for (h in 0 until 24) {
                    val v = matrix.getOrNull(dIdx)?.getOrNull(h) ?: 0
                    val alpha = if (v <= 0) 0f else (0.24f + 0.76f * (v.toFloat() / maxVal.toFloat())).coerceIn(0.24f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (v <= 0) emptyColor else accent.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
fun MultiSegmentDonutCard(
    title: String,
    subtitle: String,
    centerValue: String,
    centerLabel: String,
    segments: List<DonutSegmentItem>,
    icon: ImageVector,
    progress: Float,
    footerContent: (@Composable () -> Unit)? = null
) {
    val total = segments.sumOf { it.count }.coerceAtLeast(1)
    val accent = segments.firstOrNull()?.color ?: MaterialTheme.colorScheme.primary

    StatsGlassCard(
        title = title,
        subtitle = subtitle,
        icon = icon,
        accent = accent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    var startAngle = -90f
                    segments.forEach { seg ->
                        val sweep = (seg.count.toFloat() / total.toFloat()) * 360f * progress
                        if (sweep > 1f) {
                            drawArc(
                                color = seg.color,
                                startAngle = startAngle,
                                sweepAngle = (sweep - 3f).coerceAtLeast(1f),
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = centerValue,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = centerLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                segments.take(6).forEach { seg ->
                    val pct = ((seg.count.toFloat() / total.toFloat()) * 100f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(seg.color)
                            )
                            Text(
                                text = seg.label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = if (seg.detail.isNotBlank()) seg.detail else "${pct.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        footerContent?.invoke()
    }
}

@Composable
fun ListenTimeGaugeCard(
    totalListenText: String,
    libraryDurationText: String,
    ratioPercent: Int,
    accent: Color,
    secondaryAccent: Color,
    progress: Float
) {
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val sweepFraction = (ratioPercent.coerceIn(5, 100) / 100f) * progress

    StatsGlassCard(
        title = "Total Listen Time",
        subtitle = "Library duration $libraryDurationText",
        icon = MikaIcons.Clock,
        accent = accent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(166.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(148.dp)) {
                val strokeW = 16.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(listOf(accent, secondaryAccent, accent)),
                    startAngle = 135f,
                    sweepAngle = 270f * sweepFraction,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalListenText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${ratioPercent}% of library",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun YearDistributionChart(
    decadeCounts: List<Pair<String, Int>>,
    accent: Color,
    secondaryAccent: Color,
    progress: Float
) {
    val maxVal = (decadeCounts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val trackBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            val count = decadeCounts.size.coerceAtLeast(1)
            val slotW = size.width / count
            val barW = (slotW * 0.42f).coerceIn(14.dp.toPx(), 34.dp.toPx())
            decadeCounts.forEachIndexed { idx, (_, value) ->
                val x = idx * slotW + (slotW - barW) / 2f
                drawRoundRect(
                    color = trackBg,
                    topLeft = Offset(x, 2.dp.toPx()),
                    size = Size(barW, size.height - 2.dp.toPx()),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                if (value > 0) {
                    val h = ((size.height - 2.dp.toPx()) * (value.toFloat() / maxVal) * progress).coerceAtLeast(8.dp.toPx())
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(secondaryAccent, accent)),
                        topLeft = Offset(x, size.height - h),
                        size = Size(barW, h),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            decadeCounts.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatsSongRankRow(
    rank: Int?,
    song: Song,
    trailingText: String,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (rank <= 3) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (rank <= 3) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        TrackArtwork(
            song = song,
            cornerRadius = 10.dp,
            bitmapSizePx = 128,
            modifier = Modifier.size(42.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist.ifBlank { "Unknown Artist" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = trailingText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = accent,
            maxLines = 1
        )
    }
}
