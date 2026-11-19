package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CollectionType
import com.example.model.Song
import com.example.model.TrackSwipeAction
import com.example.ui.components.MikaVerticalFastScroller
import com.example.ui.components.SongRow
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    collectionType: CollectionType,
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    showLyricsBadges: Boolean,
    onBack: () -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onLyricsBadgeLongClick: ((Song) -> Unit)? = null,
    showLikeButton: Boolean = false,
    swipeLeftAction: TrackSwipeAction = TrackSwipeAction.NONE,
    swipeRightAction: TrackSwipeAction = TrackSwipeAction.NONE,
    onToggleFavorite: (Song) -> Unit = {},
    onExecuteSwipeAction: (Song, TrackSwipeAction) -> Unit = { _, _ -> },
    onRateAndMood: (Song) -> Unit = {}
) {
    val icon = when (collectionType) {
        CollectionType.FAVORITES -> MikaIcons.HeartBold
        CollectionType.LYRICS -> MikaIcons.Lyrics
        CollectionType.RECENTLY_PLAYED -> MikaIcons.Clock
        CollectionType.MOST_PLAYED -> MikaIcons.Chart
        CollectionType.RECENTLY_ADDED -> MikaIcons.MusicNote
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = collectionType.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = BrandDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(MikaIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (songs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp)
                    .testTag("collection_screen_empty"),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(58.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Nothing in ${collectionType.title}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = collectionType.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val listState = rememberLazyListState()
            MikaVerticalFastScroller(
                listState = listState,
                totalItems = songs.size + 1,
                sectionLabelProvider = { idx ->
                    songs.getOrNull((idx - 1).coerceAtLeast(0))?.title?.trim()?.firstOrNull()?.uppercase()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("collection_screen"),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onPlayAll(songs, false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(MikaIcons.PlayBold, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { onPlayAll(songs, true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(MikaIcons.Shuffle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(songs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isCurrentTrack = song.id == currentSongId,
                        isPlaying = isPlaying,
                        isSelectionMode = false,
                        isSelected = false,
                        showLyricsBadge = showLyricsBadges,
                        onClick = { onPlaySong(song, songs) },
                        onLongClick = { onSongMoreClick(song) },
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
