package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Song
import com.example.ui.components.MikaVerticalFastScroller
import com.example.ui.components.SongRow
import com.example.ui.icons.MikaIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    isRefreshing: Boolean,
    selectedSongIds: Set<Long>,
    showLyricsBadges: Boolean,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onImportAudio: () -> Unit,
    onRefresh: () -> Unit,
    onLyricsBadgeLongClick: ((Song) -> Unit)? = null,
    showLikeButton: Boolean = false,
    swipeLeftAction: com.example.model.TrackSwipeAction = com.example.model.TrackSwipeAction.NONE,
    swipeRightAction: com.example.model.TrackSwipeAction = com.example.model.TrackSwipeAction.NONE,
    onToggleFavorite: ((Song) -> Unit)? = null,
    onExecuteSwipeAction: ((Song, com.example.model.TrackSwipeAction) -> Unit)? = null,
    onRateAndMood: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSelectionMode = selectedSongIds.isNotEmpty()
    val listState = rememberLazyListState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        if (songs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = MikaIcons.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "No local audio tracks found",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Open an audio track or scan device folders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onImportAudio) {
                    Icon(MikaIcons.AudioFile, contentDescription = null, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Audio File", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            MikaVerticalFastScroller(
                listState = listState,
                totalItems = songs.size,
                sectionLabelProvider = { idx ->
                    songs.getOrNull(idx)?.title?.trim()?.firstOrNull()?.uppercase()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("songs_list"),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 172.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = songs,
                        key = { it.id },
                        contentType = { "song_row" }
                    ) { song ->
                        SongRow(
                            song = song,
                            isCurrentTrack = song.id == currentSongId,
                            isPlaying = isPlaying,
                            isSelectionMode = isSelectionMode,
                            isSelected = song.id in selectedSongIds,
                            showLyricsBadge = showLyricsBadges,
                            onClick = { onSongClick(song) },
                            onLongClick = { onSongLongClick(song) },
                            onMoreClick = { onSongMoreClick(song) },
                            onLyricsBadgeLongClick = onLyricsBadgeLongClick?.let { cb -> { cb(song) } },
                            showLikeButton = showLikeButton,
                            swipeLeftAction = swipeLeftAction,
                            swipeRightAction = swipeRightAction,
                            onToggleFavorite = onToggleFavorite,
                            onExecuteSwipeAction = onExecuteSwipeAction,
                            onRateAndMood = onRateAndMood
                        )
                    }
                }
            }
        }
    }
}
