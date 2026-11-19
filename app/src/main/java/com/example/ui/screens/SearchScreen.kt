package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Song
import com.example.model.TrackSwipeAction
import com.example.model.VideoItem
import com.example.ui.components.PlaylistCollageArtwork
import com.example.ui.components.SongRow
import com.example.ui.components.TrackArtwork
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.mikaScaledIcon
import com.example.ui.viewmodel.SearchResultsState
import com.example.util.Formatters

private enum class SearchScopeFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    GENRES("Genres"),
    PLAYLISTS("Playlists"),
    FOLDERS("Folders"),
    VIDEOS("Videos")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String,
    results: SearchResultsState,
    history: List<String>,
    currentSongId: Long?,
    isPlaying: Boolean,
    showLyricsBadges: Boolean,
    onQueryChange: (String) -> Unit,
    onCommitQuery: (String) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onSongMoreClick: (Song) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (Long) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenGenre: (String) -> Unit = {},
    onPlayVideo: (VideoItem) -> Unit = {},
    showLikeButton: Boolean = false,
    swipeLeftAction: TrackSwipeAction = TrackSwipeAction.NONE,
    swipeRightAction: TrackSwipeAction = TrackSwipeAction.NONE,
    onToggleFavorite: (Song) -> Unit = {},
    onExecuteSwipeAction: (Song, TrackSwipeAction) -> Unit = { _, _ -> },
    onRateAndMood: (Song) -> Unit = {}
) {
    var confirmClearHistory by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(SearchScopeFilter.ALL) }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear Search History?", fontWeight = FontWeight.Bold) },
            text = { Text("All saved search queries will be cleared.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearHistory = false
                        onClearHistory()
                    }
                ) {
                    Text("Clear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearHistory = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(MikaIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(mikaScaledIcon(24.dp)))
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Search songs, videos, artists...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        leadingIcon = { Icon(MikaIcons.Search, contentDescription = null, modifier = Modifier.size(22.dp)) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(MikaIcons.Close, contentDescription = "Clear search", modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onCommitQuery(query) }),
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .testTag("search_input")
                    )
                }

                if (query.isNotBlank() && !results.isEmpty) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchScopeFilter.entries.forEach { scope ->
                            val count = when (scope) {
                                SearchScopeFilter.ALL -> results.songs.size + results.artists.size + results.albums.size +
                                    results.genres.size + results.playlists.size + results.folders.size + results.videos.size
                                SearchScopeFilter.SONGS -> results.songs.size
                                SearchScopeFilter.ARTISTS -> results.artists.size
                                SearchScopeFilter.ALBUMS -> results.albums.size
                                SearchScopeFilter.GENRES -> results.genres.size
                                SearchScopeFilter.PLAYLISTS -> results.playlists.size
                                SearchScopeFilter.FOLDERS -> results.folders.size
                                SearchScopeFilter.VIDEOS -> results.videos.size
                            }
                            if (scope == SearchScopeFilter.ALL || count > 0) {
                                FilterChip(
                                    selected = activeFilter == scope,
                                    onClick = { activeFilter = scope },
                                    label = {
                                        Text(
                                            text = if (scope == SearchScopeFilter.ALL) "All" else "${scope.label} ($count)",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (query.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
            ) {
                if (history.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { confirmClearHistory = true }) {
                            Text("Clear all")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        history.forEach { item ->
                            InputChip(
                                selected = false,
                                onClick = { onQueryChange(item) },
                                label = { Text(item) },
                                leadingIcon = {
                                    Icon(MikaIcons.Clock, null, modifier = Modifier.size(16.dp))
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = MikaIcons.Close,
                                        contentDescription = "Remove $item",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onRemoveHistoryItem(item) }
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = MikaIcons.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Global Media Search",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Find songs, artists, albums, genres, playlists, folders & videos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (results.isEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No matches for \"$query\"", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Try a different title, artist, genre, mood, or video name.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (results.songs.isNotEmpty() && (activeFilter == SearchScopeFilter.ALL || activeFilter == SearchScopeFilter.SONGS)) {
                    item { GroupTitle("Songs (${results.songs.size})") }
                    items(results.songs, key = { "s_${it.id}" }) { song ->
                        SongRow(
                            song = song,
                            isCurrentTrack = song.id == currentSongId,
                            isPlaying = isPlaying,
                            isSelectionMode = false,
                            isSelected = false,
                            showLyricsBadge = showLyricsBadges,
                            onClick = {
                                onCommitQuery(query)
                                onPlaySong(song, results.songs)
                            },
                            onLongClick = { onSongMoreClick(song) },
                            onMoreClick = { onSongMoreClick(song) },
                            showLikeButton = showLikeButton,
                            swipeLeftAction = swipeLeftAction,
                            swipeRightAction = swipeRightAction,
                            onToggleFavorite = onToggleFavorite,
                            onExecuteSwipeAction = onExecuteSwipeAction,
                            onRateAndMood = onRateAndMood
                        )
                    }
                }

                if (results.artists.isNotEmpty() && (activeFilter == SearchScopeFilter.ALL || activeFilter == SearchScopeFilter.ARTISTS)) {
                    item { GroupTitle("Artists (${results.artists.size})") }
                    items(results.artists, key = { "ar_${it.id}_${it.name}" }) { artist ->
                        Surface(
                            onClick = {
                                onCommitQuery(query)
                                onOpenArtist(artist.id)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(MikaIcons.User, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(artist.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "${artist.songCount} songs · ${artist.albumCount} albums",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (results.albums.isNotEmpty() && (activeFilter == SearchScopeFilter.ALL || activeFilter == SearchScopeFilter.ALBUMS)) {
                    item { GroupTitle("Albums (${results.albums.size})") }
                    items(results.albums, key = { "al_${it.id}_${it.name}" }) { album ->
                        Surface(
                            onClick = {
                                onCommitQuery(query)
                                onOpenAlbum(album.id)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrackArtwork(song = album.representativeSong, modifier = Modifier.size(46.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(album.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${album.artist} · ${album.trackCount} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (results.genres.isNotEmpty() && (activeFilter == SearchScopeFilter.ALL || activeFilter == SearchScopeFilter.GENRES)) {
                    item { GroupTitle("Genres (${results.genres.size})") }
                    items(results.genres, key = { "gn_${it.name}" }) { genre ->
                        Surface(
                            onClick = {
                                onCommitQuery(query)
                                onOpenGenre(genre.name)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(MikaIcons.Category, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(genre.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "${genre.songCount} tracks · ${Formatters.formatDuration(genre.totalDurationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (results.playlists.isNotEmpty() && (activeFilter == SearchScopeFilter.ALL || activeFilter == SearchScopeFilter.PLAYLISTS)) {
                    item { GroupTitle("Playlists (${results.playlists.size})") }
                    items(results.playlists, key = { "pl_${it.id}" }) { pl ->
                        Surface(
                            onClick = {
                                onCommitQuery(query)
                                onOpenPlaylist(pl.id)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlaylistCollageArtwork(songs = pl.songs, modifier = Modifier.size(46.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(pl.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${pl.songCount} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (results.folders.isNotEmpty() && (activeFilter == SearchScopeFilter.ALL || activeFilter == SearchScopeFilter.FOLDERS)) {
                    item { GroupTitle("Folders (${results.folders.size})") }
                    items(results.folders, key = { "fd_${it.path}" }) { folder ->
                        Surface(
                            onClick = {
                                onCommitQuery(query)
                                onOpenFolder(folder.path)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(MikaIcons.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(folder.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(folder.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                if (results.videos.isNotEmpty() && (activeFilter == SearchScopeFilter.ALL || activeFilter == SearchScopeFilter.VIDEOS)) {
                    item { GroupTitle("Videos (${results.videos.size})") }
                    items(results.videos, key = { "vd_${it.id}_${it.contentUri}" }) { video ->
                        Surface(
                            onClick = {
                                onCommitQuery(query)
                                onPlayVideo(video)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(MikaIcons.VideoBold, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(video.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${video.folderName} · ${Formatters.formatDuration(video.durationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    )
}
