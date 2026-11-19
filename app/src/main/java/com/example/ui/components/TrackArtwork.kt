package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.media.toArtworkRequest
import com.example.model.Song
import com.example.ui.icons.MikaIcons

@Composable
fun TrackArtwork(
    song: Song?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    isCurrentTrack: Boolean = false,
    isPlaying: Boolean = false,
    crossfade: Boolean = false,
    bitmapSizePx: Int = 200,
    preserveAspectRatio: Boolean = false
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (preserveAspectRatio) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (song != null) {
            val imageRequest = remember(song.id, song.dateModifiedSec, crossfade, bitmapSizePx, preserveAspectRatio) {
                ImageRequest.Builder(context)
                    .data(song.toArtworkRequest())
                    .size(if (preserveAspectRatio) Size.ORIGINAL else Size(bitmapSizePx, bitmapSizePx))
                    .crossfade(crossfade)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = "${song.title} artwork",
                contentScale = if (preserveAspectRatio) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = MikaIcons.Disc,
                    contentDescription = "Music artwork placeholder",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (isCurrentTrack) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                EqualizerWaveOverlay(isAnimating = isPlaying)
            }
        }
    }
}

@Composable
fun EqualizerWaveOverlay(
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = Color.White
) {
    val transition = rememberInfiniteTransition(label = "eq_wave")
    val bar1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val bar4 by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(390, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    val heights = if (isAnimating) listOf(bar1, bar2, bar3, bar4) else listOf(0.45f, 0.7f, 0.5f, 0.65f)

    Row(
        modifier = modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun PlaylistCollageArtwork(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            songs.isEmpty() -> {
                Icon(
                    imageVector = MikaIcons.Playlist,
                    contentDescription = "Empty playlist artwork",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
            songs.size < 4 -> {
                TrackArtwork(
                    song = songs.first(),
                    cornerRadius = 0.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        TrackArtwork(
                            song = songs[0],
                            cornerRadius = 0.dp,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        TrackArtwork(
                            song = songs[1],
                            cornerRadius = 0.dp,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        TrackArtwork(
                            song = songs[2],
                            cornerRadius = 0.dp,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        TrackArtwork(
                            song = songs[3],
                            cornerRadius = 0.dp,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}
