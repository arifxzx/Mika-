package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.AppSettings
import com.example.model.BackgroundEffect
import com.example.model.PlayerBackgroundMode
import com.example.model.Song
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.mikaScaledIcon
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun rememberParallaxOffset(enabled: Boolean): State<Offset> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(enabled) {
        if (!enabled) {
            state.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || sensor == null) {
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val nx = (event.values.getOrNull(0) ?: 0f).coerceIn(-6f, 6f) / 6f
                val ny = ((event.values.getOrNull(1) ?: 0f) - 4f).coerceIn(-6f, 6f) / 6f
                val target = Offset(-nx * 18f, ny * 18f)
                state.value = Offset(
                    x = state.value.x * 0.8f + target.x * 0.2f,
                    y = state.value.y * 0.8f + target.y * 0.2f
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    return state
}

@Composable
fun PlayerBackgroundLayer(
    song: Song,
    settings: AppSettings,
    artworkAccentPair: Pair<Color, Color>?,
    baseBackgroundBrush: Brush,
    isExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val c1 = if (settings.playerBackgroundColorWhenExpanded && isExpanded && artworkAccentPair != null) {
        artworkAccentPair.first
    } else {
        MaterialTheme.colorScheme.primary
    }
    val c2 = if (settings.playerBackgroundColorWhenExpanded && isExpanded && artworkAccentPair != null) {
        artworkAccentPair.second
    } else {
        MaterialTheme.colorScheme.secondary
    }

    val animPhase = if (settings.playerBackgroundAnimated || settings.backgroundEffect != BackgroundEffect.NONE) {
        val transition = rememberInfiniteTransition(label = "player_bg_anim")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "bg_phase"
        ).value
    } else {
        0.2f
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Base background mode
        when (settings.playerBackgroundMode) {
            PlayerBackgroundMode.NONE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
            PlayerBackgroundMode.ARTWORK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseBackgroundBrush)
                )
                TrackArtwork(
                    song = song,
                    cornerRadius = 0.dp,
                    bitmapSizePx = 512,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.36f)
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(28.dp) else Modifier
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f))
                )
            }
            PlayerBackgroundMode.CUSTOM -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseBackgroundBrush)
                )
                if (settings.playerBackgroundCustomUri.isNotBlank()) {
                    AsyncImage(
                        model = Uri.parse(settings.playerBackgroundCustomUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.58f)
                            .then(
                                if (settings.enableBlurEffect && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.blur(20.dp)
                                } else Modifier
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.38f))
                )
            }
            PlayerBackgroundMode.MESH_GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseBackgroundBrush)
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val a1 = animPhase * 2f * PI.toFloat()
                    val a2 = animPhase * 4f * PI.toFloat()
                    val a3 = animPhase * 6f * PI.toFloat()
                    val cx1 = size.width * (0.26f + 0.16f * cos(a1) + 0.05f * sin(a2))
                    val cy1 = size.height * (0.24f + 0.12f * sin(a1) + 0.04f * cos(a3))
                    val cx2 = size.width * (0.76f - 0.16f * sin(a1) + 0.04f * cos(a2))
                    val cy2 = size.height * (0.68f - 0.12f * cos(a1) + 0.05f * sin(a3))
                    val cx3 = size.width * (0.48f + 0.14f * sin(a2 + 1.1f))
                    val cy3 = size.height * (0.48f + 0.14f * cos(a2 + 0.7f))

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c1.copy(alpha = 0.44f), c1.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(cx1, cy1),
                            radius = size.width * 0.90f
                        ),
                        radius = size.width * 0.90f,
                        center = Offset(cx1, cy1)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c2.copy(alpha = 0.40f), c2.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(cx2, cy2),
                            radius = size.width * 0.92f
                        ),
                        radius = size.width * 0.92f,
                        center = Offset(cx2, cy2)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c1.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(cx3, cy3),
                            radius = size.width * 0.78f
                        ),
                        radius = size.width * 0.78f,
                        center = Offset(cx3, cy3)
                    )
                }
            }
            PlayerBackgroundMode.AMBIENT_LIGHTS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val a1 = animPhase * 2f * PI.toFloat()
                    val a2 = animPhase * 4f * PI.toFloat()
                    val orb1 = Offset(
                        size.width * (0.22f + 0.24f * sin(a1) + 0.06f * cos(a2)),
                        size.height * (0.20f + 0.08f * cos(a1))
                    )
                    val orb2 = Offset(
                        size.width * (0.78f - 0.22f * cos(a1) + 0.05f * sin(a2)),
                        size.height * (0.50f + 0.10f * sin(a1))
                    )
                    val orb3 = Offset(
                        size.width * (0.48f + 0.16f * cos(a2)),
                        size.height * (0.82f + 0.08f * sin(a1))
                    )
                    listOf(
                        Triple(orb1, c1.copy(alpha = 0.48f), size.width * 0.62f),
                        Triple(orb2, c2.copy(alpha = 0.42f), size.width * 0.58f),
                        Triple(orb3, c1.copy(alpha = 0.30f), size.width * 0.68f)
                    ).forEach { (center, col, rad) ->
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(col, col.copy(alpha = col.alpha * 0.35f), Color.Transparent),
                                center = center,
                                radius = rad
                            ),
                            radius = rad,
                            center = center
                        )
                    }
                }
            }
            PlayerBackgroundMode.BOKEH -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseBackgroundBrush)
                )
                val bokehSeeds = remember {
                    val rnd = Random(42)
                    List(16) {
                        Triple(rnd.nextFloat(), rnd.nextFloat(), 26f + rnd.nextFloat() * 72f)
                    }
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    bokehSeeds.forEachIndexed { idx, (bx, by, rDp) ->
                        val driftY = if (settings.playerBackgroundAnimated) {
                            ((by - animPhase * (0.25f + (idx % 3) * 0.10f) + 1f) % 1f)
                        } else by
                        val swayX = if (settings.playerBackgroundAnimated) {
                            0.04f * sin((animPhase * 2f * PI + idx * 0.8f).toFloat())
                        } else 0f
                        val center = Offset(((bx + swayX).mod(1f)) * size.width, driftY * size.height)
                        val color = if (idx % 2 == 0) c1 else c2
                        val rPx = rDp.dp.toPx()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = 0.20f), color.copy(alpha = 0.08f), Color.Transparent),
                                center = center,
                                radius = rPx
                            ),
                            radius = rPx,
                            center = center
                        )
                        if (idx % 3 == 0) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.10f),
                                radius = rPx * 0.86f,
                                center = center,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // 2. Seasonal / Particle Background Effect
        val resolvedEffect = remember(settings.backgroundEffect) {
            if (settings.backgroundEffect == BackgroundEffect.AUTO_SEASON) {
                when (Calendar.getInstance().get(Calendar.MONTH)) {
                    Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> BackgroundEffect.SNOW
                    Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> BackgroundEffect.SAKURA
                    Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> BackgroundEffect.FIREFLIES
                    else -> BackgroundEffect.AUTUMN_LEAVES
                }
            } else {
                settings.backgroundEffect
            }
        }

        if (resolvedEffect != BackgroundEffect.NONE) {
            val particles = remember(resolvedEffect) {
                val rnd = Random(resolvedEffect.ordinal * 97 + 7)
                List(30) {
                    val x = rnd.nextFloat()
                    val y = rnd.nextFloat()
                    val speed = 0.35f + rnd.nextFloat() * 0.95f
                    val sizeFactor = 0.65f + rnd.nextFloat() * 0.75f
                    floatArrayOf(x, y, speed, sizeFactor)
                }
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEachIndexed { idx, p ->
                    val px = p[0]
                    val py = p[1]
                    val speedScale = p[2]
                    val sizeScale = p[3]
                    val phaseOffset = idx * 0.43f
                    val yFrac = (py + animPhase * speedScale) % 1f
                    val xSway = sin((animPhase * 2f * PI + phaseOffset).toDouble()).toFloat() * (12f + (idx % 3) * 6f).dp.toPx()
                    val pos = Offset(
                        x = ((px * size.width) + xSway).mod(size.width),
                        y = yFrac * size.height
                    )
                    when (resolvedEffect) {
                        BackgroundEffect.SNOW -> {
                            val r = (1.8f + (idx % 4) * 0.9f) * sizeScale
                            val alpha = 0.28f + 0.34f * absSin(animPhase * 2f + phaseOffset)
                            drawCircle(
                                color = Color.White.copy(alpha = alpha * 0.35f),
                                radius = (r * 1.9f).dp.toPx(),
                                center = pos
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = r.dp.toPx(),
                                center = pos
                            )
                        }
                        BackgroundEffect.SAKURA -> {
                            val rot = animPhase * 360f * (if (idx % 2 == 0) 1f else -1f) + idx * 35f
                            val petalR = (5.2f * sizeScale).dp.toPx()
                            val petalColor = if (idx % 3 == 0) Color(0xFFFFD6EB) else Color(0xFFFFA8CE)
                            rotate(degrees = rot, pivot = pos) {
                                val petalPath = Path().apply {
                                    moveTo(pos.x, pos.y - petalR)
                                    cubicTo(
                                        pos.x + petalR * 0.95f, pos.y - petalR * 0.55f,
                                        pos.x + petalR * 0.72f, pos.y + petalR * 0.68f,
                                        pos.x, pos.y + petalR * 0.85f
                                    )
                                    cubicTo(
                                        pos.x - petalR * 0.72f, pos.y + petalR * 0.68f,
                                        pos.x - petalR * 0.95f, pos.y - petalR * 0.55f,
                                        pos.x, pos.y - petalR * 0.45f
                                    )
                                    close()
                                }
                                drawPath(path = petalPath, color = petalColor.copy(alpha = 0.68f))
                                drawCircle(
                                    color = Color(0xFFFFF0F6).copy(alpha = 0.55f),
                                    radius = petalR * 0.22f,
                                    center = Offset(pos.x, pos.y + petalR * 0.15f)
                                )
                            }
                        }
                        BackgroundEffect.RAIN -> {
                            val rainY = ((py + animPhase * (speedScale * 2.2f)) % 1f) * size.height
                            val rainPos = Offset(px * size.width, rainY)
                            val len = (14f + (idx % 4) * 6f).dp.toPx()
                            drawLine(
                                color = Color(0xFF93C5FD).copy(alpha = 0.36f),
                                start = rainPos,
                                end = Offset(rainPos.x - len * 0.22f, rainPos.y + len),
                                strokeWidth = 1.4.dp.toPx()
                            )
                            if (rainY > size.height * 0.85f) {
                                val rippleProgress = ((rainY - size.height * 0.85f) / (size.height * 0.15f)).coerceIn(0f, 1f)
                                drawCircle(
                                    color = Color(0xFFBAE6FD).copy(alpha = (1f - rippleProgress) * 0.32f),
                                    radius = (4.dp.toPx() + rippleProgress * 12.dp.toPx()),
                                    center = Offset(rainPos.x - 3.dp.toPx(), size.height * 0.96f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                        BackgroundEffect.FIREFLIES -> {
                            val fx = ((px + 0.06f * sin(animPhase * 2f * PI.toFloat() + idx)) * size.width).coerceIn(0f, size.width)
                            val fy = ((py + 0.05f * cos(animPhase * 2f * PI.toFloat() * 0.7f + idx * 1.3f)) * size.height).coerceIn(0f, size.height)
                            val centerPos = Offset(fx, fy)
                            val pulse = 0.25f + 0.75f * absSin(animPhase * 4.5f + idx)
                            val coreR = (2.2f * sizeScale).dp.toPx()
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFEF08A).copy(alpha = pulse * 0.68f), Color.Transparent),
                                    center = centerPos,
                                    radius = coreR * 4.8f
                                ),
                                radius = coreR * 4.8f,
                                center = centerPos
                            )
                            drawCircle(
                                color = Color(0xFFFEF9C3).copy(alpha = pulse),
                                radius = coreR,
                                center = centerPos
                            )
                        }
                        BackgroundEffect.AUTUMN_LEAVES -> {
                            val leafColor = when (idx % 3) {
                                0 -> Color(0xFFF97316)
                                1 -> Color(0xFFEA580C)
                                else -> Color(0xFFF59E0B)
                            }
                            val r = (6.8f * sizeScale).dp.toPx()
                            val leafRot = animPhase * 220f * (if (idx % 2 == 0) 1f else -1f) +
                                32f * sin((animPhase * 4f * PI + idx).toFloat()) + idx * 27f
                            rotate(degrees = leafRot, pivot = pos) {
                                val leafPath = Path().apply {
                                    moveTo(pos.x, pos.y - r)
                                    quadraticTo(pos.x + r * 0.95f, pos.y, pos.x, pos.y + r)
                                    quadraticTo(pos.x - r * 0.95f, pos.y, pos.x, pos.y - r)
                                    close()
                                }
                                drawPath(path = leafPath, color = leafColor.copy(alpha = 0.62f))
                                drawLine(
                                    color = Color(0xFFFEF3C7).copy(alpha = 0.48f),
                                    start = Offset(pos.x, pos.y - r * 0.72f),
                                    end = Offset(pos.x, pos.y + r * 0.85f),
                                    strokeWidth = 1.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                        BackgroundEffect.STARS -> {
                            val starCenter = Offset(px * size.width, py * size.height)
                            val twinkle = 0.20f + 0.78f * absSin(animPhase * 5.2f + idx * 0.75f)
                            val r = (1.6f + (idx % 3) * 0.7f).dp.toPx()
                            drawCircle(
                                color = Color.White.copy(alpha = twinkle * 0.32f),
                                radius = r * 2.4f,
                                center = starCenter
                            )
                            drawLine(
                                color = Color.White.copy(alpha = twinkle * 0.8f),
                                start = Offset(starCenter.x - r * 2.2f, starCenter.y),
                                end = Offset(starCenter.x + r * 2.2f, starCenter.y),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = Color.White.copy(alpha = twinkle * 0.8f),
                                start = Offset(starCenter.x, starCenter.y - r * 2.2f),
                                end = Offset(starCenter.x, starCenter.y + r * 2.2f),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = twinkle),
                                radius = r,
                                center = starCenter
                            )
                        }
                        else -> {}
                    }
                }
            }
        }

        // 3. Vignette overlay
        if (settings.playerBackgroundVignette && settings.playerBackgroundMode != PlayerBackgroundMode.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f)),
                            radius = 1400f
                        )
                    )
            )
        }
    }
}

private fun absSin(x: Float): Float = kotlin.math.abs(sin(x))

@Composable
fun PlayerBackgroundDialog(
    settings: AppSettings,
    onSelectMode: (PlayerBackgroundMode) -> Unit,
    onPickCustomImage: () -> Unit,
    onToggleVignette: (Boolean) -> Unit,
    onToggleAnimated: (Boolean) -> Unit,
    onToggleColorWhenExpanded: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val modes = PlayerBackgroundMode.entries
    val modeRows = remember(modes) { modes.chunked(3) }

    Dialog(onDismissRequest = onDismiss) {
        MikaGlassSurface(
            shape = RoundedCornerShape(22.dp),
            tintColor = accent,
            tintStrength = 0.11f,
            frostedAlpha = 1f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("player_background_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MikaIcons.Palette,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(mikaScaledIcon(16.dp))
                            )
                        }
                        Text(
                            text = "Player background",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        color = accent,
                        modifier = Modifier
                            .heightIn(min = 30.dp)
                            .testTag("player_bg_done_button")
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Compact 3x2 Grid of Background Modes
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    modeRows.forEach { rowModes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            rowModes.forEach { mode ->
                                val selected = settings.playerBackgroundMode == mode
                                val modeIcon = when (mode) {
                                    PlayerBackgroundMode.NONE -> MikaIcons.Close
                                    PlayerBackgroundMode.ARTWORK -> MikaIcons.MusicNote
                                    PlayerBackgroundMode.CUSTOM -> MikaIcons.Palette
                                    PlayerBackgroundMode.MESH_GRADIENT -> MikaIcons.Disc
                                    PlayerBackgroundMode.AMBIENT_LIGHTS -> MikaIcons.Sun
                                    PlayerBackgroundMode.BOKEH -> MikaIcons.Star
                                }
                                Surface(
                                    onClick = {
                                        onSelectMode(mode)
                                        if (mode == PlayerBackgroundMode.CUSTOM && settings.playerBackgroundCustomUri.isBlank()) {
                                            onPickCustomImage()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) {
                                        accent.copy(alpha = 0.18f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                                    },
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp, vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = mode.label,
                                            tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(mikaScaledIcon(16.dp))
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (rowModes.size < 3) {
                                repeat(3 - rowModes.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (settings.playerBackgroundMode == PlayerBackgroundMode.CUSTOM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (settings.playerBackgroundCustomUri.isNotBlank()) {
                            AsyncImage(
                                model = Uri.parse(settings.playerBackgroundCustomUri),
                                contentDescription = "Custom background preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onPickCustomImage)
                            )
                        }
                        OutlinedButton(
                            onClick = onPickCustomImage,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Text(
                                text = if (settings.playerBackgroundCustomUri.isBlank()) "Choose Photo" else "Change Photo",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                DialogSwitchRow(
                    icon = MikaIcons.Shield,
                    label = "Vignette",
                    checked = settings.playerBackgroundVignette,
                    onCheckedChange = onToggleVignette
                )
                DialogSwitchRow(
                    icon = MikaIcons.Changelog,
                    label = "Animated",
                    checked = settings.playerBackgroundAnimated,
                    onCheckedChange = onToggleAnimated
                )
                DialogSwitchRow(
                    icon = MikaIcons.Palette,
                    label = "Color when expanded",
                    checked = settings.playerBackgroundColorWhenExpanded,
                    onCheckedChange = onToggleColorWhenExpanded
                )
            }
        }
    }
}

@Composable
private fun DialogSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(mikaScaledIcon(16.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
