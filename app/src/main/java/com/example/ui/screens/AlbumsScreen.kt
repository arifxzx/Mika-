package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Album
import com.example.model.Song
import com.example.model.TrackSwipeAction
import com.example.ui.components.MikaVerticalFastScroller
import com.example.ui.components.MikaVerticalGridFastScroller
import com.example.ui.components.SongRow
import com.example.ui.components.TrackArtwork
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily

@Composable
fun AlbumsScreen(
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = MikaIcons.Disc,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("No albums discovered yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("albums_screen")) {
        val columns = if (maxWidth >= 600.dp) 3 else 2
        val gridState = rememberLazyGridState()
        MikaVerticalGridFastScroller(
            gridState = gridState,
            totalItems = albums.size,
            columns = columns,
            sectionLabelProvider = { idx -> albums.getOrNull(idx)?.name?.trim()?.firstOrNull()?.uppercase() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(albums, key = { "${it.id}_${it.name}" }) { album ->
                    Surface(
                        onClick = { onAlbumClick(album.id) },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            TrackArtwork(
                                song = album.representativeSong,
                                cornerRadius = 14.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${album.artist} · ${album.trackCount} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: Album?,
    albumSongs: List<Song>,
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
    androidx.compose.runtime.LaunchedEffect(album == null) {
        if (album == null) onBack()
    }
    if (album == null) return

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = album.name,
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
            totalItems = albumSongs.size + 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TrackArtwork(
                            song = album.representativeSong,
                            cornerRadius = 22.dp,
                            modifier = Modifier.size(180.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${album.artist} · ${albumSongs.size} tracks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { onPlayAll(albumSongs, false) }) {
                                Icon(MikaIcons.PlayBold, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Play Album", fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(onClick = { onPlayAll(albumSongs, true) }) {
                                Icon(MikaIcons.Shuffle, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Shuffle", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(albumSongs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isCurrentTrack = song.id == currentSongId,
                        isPlaying = isPlaying,
                        isSelectionMode = false,
                        isSelected = false,
                        showLyricsBadge = showLyricsBadges,
                        onClick = { onPlaySong(song, albumSongs) },
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
