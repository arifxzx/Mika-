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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GenreItem
import com.example.model.GenreSortField
import com.example.model.Song
import com.example.model.SortDirection
import com.example.model.TrackSwipeAction
import com.example.ui.components.MikaVerticalFastScroller
import com.example.ui.components.SongRow
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TypedSortBottomSheet
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenresScreen(
    genres: List<GenreItem>,
    sortField: GenreSortField,
    sortDirection: SortDirection,
    onUpdateSort: (GenreSortField, SortDirection) -> Unit,
    onBack: () -> Unit,
    onGenreClick: (String) -> Unit
) {
    var showSortSheet by remember { mutableStateOf(false) }

    if (showSortSheet) {
        TypedSortBottomSheet(
            title = "Sort Genres",
            fields = GenreSortField.entries,
            currentField = sortField,
            currentDirection = sortDirection,
            labelProvider = { it.label },
            onSelect = onUpdateSort,
            onDismiss = { showSortSheet = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Genres",
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
                actions = {
                    TextButton(onClick = { showSortSheet = true }) {
                        Icon(MikaIcons.Sort, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(sortField.label, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (genres.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = MikaIcons.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("No genres indexed yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        } else {
            val listState = rememberLazyListState()
            MikaVerticalFastScroller(
                listState = listState,
                totalItems = genres.size,
                sectionLabelProvider = { idx -> genres.getOrNull(idx)?.name?.trim()?.firstOrNull()?.uppercase() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("genres_screen"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                items(genres, key = { it.name }) { genre ->
                    Surface(
                        onClick = { onGenreClick(genre.name) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackArtwork(
                                song = genre.representativeSong,
                                cornerRadius = 14.dp,
                                bitmapSizePx = 150,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = genre.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${genre.songCount} tracks · ${Formatters.formatDuration(genre.totalDurationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = MikaIcons.ArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreDetailScreen(
    genreName: String,
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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = genreName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = BrandDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
            if (songs.isNotEmpty()) {
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
