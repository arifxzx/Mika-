package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.model.TrackSwipeAction
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon
import com.example.util.Formatters
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    showLyricsBadge: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    onLyricsBadgeClick: (() -> Unit)? = null,
    onLyricsBadgeLongClick: (() -> Unit)? = null,
    showLikeButton: Boolean = false,
    swipeLeftAction: TrackSwipeAction = TrackSwipeAction.NONE,
    swipeRightAction: TrackSwipeAction = TrackSwipeAction.NONE,
    onToggleFavorite: ((Song) -> Unit)? = null,
    onExecuteSwipeAction: ((Song, TrackSwipeAction) -> Unit)? = null,
    onRateAndMood: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val mikaColors = LocalMikaColors.current
    val secondaryAccent = mikaColors.accentSecondary
    val shape = RoundedCornerShape(mikaColors.cornerRadiusDp.coerceIn(10, 20).dp)
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 82.dp.toPx() }
    val maxSwipePx = with(density) { 116.dp.toPx() }
    val offsetX = remember(song.id) { Animatable(0f) }

    val latestSong by rememberUpdatedState(song)
    val latestSwipeLeft by rememberUpdatedState(swipeLeftAction)
    val latestSwipeRight by rememberUpdatedState(swipeRightAction)
    val latestOnSwipe by rememberUpdatedState(onExecuteSwipeAction)
    val canSwipe = !isSelectionMode && onExecuteSwipeAction != null &&
        (swipeLeftAction != TrackSwipeAction.NONE || swipeRightAction != TrackSwipeAction.NONE)

    val rowBackground = when {
        isSelected -> Modifier.background(accent.copy(alpha = 0.16f))
        isCurrentTrack -> Modifier
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.22f),
                        secondaryAccent.copy(alpha = 0.08f)
                    )
                )
            )
            .border(1.3.dp, accent.copy(alpha = 0.65f), shape)
        else -> Modifier
    }

    val activeSwipeAction = when {
        offsetX.value > 18f -> swipeRightAction
        offsetX.value < -18f -> swipeLeftAction
        else -> TrackSwipeAction.NONE
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        if (canSwipe && activeSwipeAction != TrackSwipeAction.NONE) {
            val isRight = offsetX.value > 0f
            val ready = abs(offsetX.value) >= swipeThresholdPx
            val badgeTint = if (activeSwipeAction == TrackSwipeAction.TRASH) {
                MaterialTheme.colorScheme.error
            } else {
                accent
            }
            val actionIcon = when (activeSwipeAction) {
                TrackSwipeAction.PLAY_NEXT -> MikaIcons.PlayNext
                TrackSwipeAction.ADD_TO_QUEUE -> MikaIcons.Queue
                TrackSwipeAction.FAVORITE -> if (song.isFavorite) MikaIcons.HeartLinear else MikaIcons.HeartBold
                TrackSwipeAction.ADD_TO_PLAYLIST -> MikaIcons.PlaylistAdd
                TrackSwipeAction.RATE_MOOD -> MikaIcons.Mood
                TrackSwipeAction.HIDE -> MikaIcons.EyeSlash
                TrackSwipeAction.TRASH -> MikaIcons.Trash
                TrackSwipeAction.NONE -> MikaIcons.Check
            }
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .background(badgeTint.copy(alpha = if (ready) 0.22f else 0.10f), shape)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRight) Arrangement.Start else Arrangement.End
            ) {
                if (isRight) {
                    Icon(actionIcon, contentDescription = activeSwipeAction.label, tint = badgeTint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = activeSwipeAction.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = badgeTint
                    )
                } else {
                    Text(
                        text = activeSwipeAction.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = badgeTint
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(actionIcon, contentDescription = activeSwipeAction.label, tint = badgeTint, modifier = Modifier.size(20.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clip(shape)
                .then(rowBackground)
                .then(
                    if (canSwipe) {
                        Modifier.pointerInput(song.id, swipeLeftAction, swipeRightAction) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val x = offsetX.value
                                    val triggered = when {
                                        x >= swipeThresholdPx && latestSwipeRight != TrackSwipeAction.NONE -> latestSwipeRight
                                        x <= -swipeThresholdPx && latestSwipeLeft != TrackSwipeAction.NONE -> latestSwipeLeft
                                        else -> TrackSwipeAction.NONE
                                    }
                                    if (triggered != TrackSwipeAction.NONE) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        latestOnSwipe?.invoke(latestSong, triggered)
                                    }
                                    scope.launch {
                                        offsetX.animateTo(0f, spring())
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { offsetX.animateTo(0f, spring()) }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    val current = offsetX.value + dragAmount
                                    val clamped = when {
                                        current > 0f && latestSwipeRight == TrackSwipeAction.NONE -> 0f
                                        current < 0f && latestSwipeLeft == TrackSwipeAction.NONE -> 0f
                                        else -> current.coerceIn(-maxSwipePx, maxSwipePx)
                                    }
                                    scope.launch { offsetX.snapTo(clamped) }
                                }
                            )
                        }
                    } else Modifier
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(start = if (isCurrentTrack) 6.dp else 10.dp, end = 2.dp, top = 8.dp, bottom = 8.dp)
                .testTag("song_row_${song.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrentTrack && !isSelectionMode) {
                Box(
                    modifier = Modifier
                        .padding(end = 7.dp)
                        .width(3.5.dp)
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) accent else Color.Transparent)
                        .border(
                            width = 1.8.dp,
                            color = if (isSelected) accent else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = MikaIcons.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(mikaScaledIcon(16.dp))
                        )
                    }
                }
            }

            TrackArtwork(
                song = song,
                cornerRadius = 11.dp,
                isCurrentTrack = isCurrentTrack,
                isPlaying = isPlaying,
                modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isCurrentTrack) accent else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (showLyricsBadge && song.hasLyrics) {
                        val badgeMod = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .let { base ->
                                if (onLyricsBadgeClick != null || onLyricsBadgeLongClick != null) {
                                    base.combinedClickable(
                                        onClick = { onLyricsBadgeClick?.invoke() ?: onClick() },
                                        onLongClick = onLyricsBadgeLongClick
                                    )
                                } else {
                                    base
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                        Box(modifier = badgeMod) {
                            Text(
                                text = "LYRICS",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (song.isFavorite && !showLikeButton) {
                        Icon(
                            imageVector = MikaIcons.HeartBold,
                            contentDescription = "Favorite",
                            tint = accent,
                            modifier = Modifier.size(mikaScaledIcon(14.dp))
                        )
                    }

                    if (song.rating > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .then(
                                    if (onRateAndMood != null) Modifier.clickable { onRateAndMood(song) } else Modifier
                                )
                                .padding(horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = MikaIcons.StarFilled,
                                contentDescription = "Rated ${song.rating} stars",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${song.rating}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (song.mood.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent.copy(alpha = 0.14f),
                            modifier = if (onRateAndMood != null) Modifier.clickable { onRateAndMood(song) } else Modifier
                        ) {
                            Text(
                                text = song.mood,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold),
                                color = accent,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "${song.artist} · ${Formatters.formatDuration(song.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrentTrack) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isSelectionMode && showLikeButton && onToggleFavorite != null) {
                IconButton(
                    onClick = { onToggleFavorite(song) },
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("song_like_${song.id}")
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) MikaIcons.HeartBold else MikaIcons.HeartLinear,
                        contentDescription = if (song.isFavorite) "Unlike ${song.title}" else "Like ${song.title}",
                        tint = if (song.isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(mikaScaledIcon(19.dp))
                    )
                }
            }

            if (!isSelectionMode) {
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("song_more_${song.id}")
                ) {
                    Icon(
                        imageVector = MikaIcons.MoreVert,
                        contentDescription = "More options for ${song.title}",
                        tint = if (isCurrentTrack) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(mikaScaledIcon(22.dp))
                    )
                }
            }
        }
    }
}
