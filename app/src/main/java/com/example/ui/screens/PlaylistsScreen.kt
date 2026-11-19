package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CollectionType
import com.example.model.Playlist
import com.example.model.Song
import androidx.compose.foundation.layout.heightIn
import com.example.ui.components.CreateOrRenamePlaylistDialog
import com.example.ui.components.MikaGlassSurface
import com.example.ui.components.MikaVerticalFastScroller
import com.example.ui.components.PlaylistCollageArtwork
import com.example.ui.components.TrackArtwork
import com.example.ui.components.mikaGlassBorderBrush
import com.example.ui.components.mikaGlassFillBrush
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.mikaScaledIcon
import com.example.util.Formatters

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    favoritesCount: Int,
    lyricsCount: Int,
    mostPlayedCount: Int,
    recentlyPlayedCount: Int,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenCollection: (CollectionType) -> Unit,
    onPlayCollection: (CollectionType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var deletingPlaylist by remember { mutableStateOf<Playlist?>(null) }

    if (showCreateDialog) {
        CreateOrRenamePlaylistDialog(
            title = "Create Playlist",
            confirmLabel = "Create",
            onConfirm = onCreatePlaylist,
            onDismiss = { showCreateDialog = false }
        )
    }

    renamingPlaylist?.let { pl ->
        CreateOrRenamePlaylistDialog(
            title = "Rename Playlist",
            initialName = pl.name,
            confirmLabel = "Rename",
            onConfirm = { newName -> onRenamePlaylist(pl.id, newName) },
            onDismiss = { renamingPlaylist = null }
        )
    }

    deletingPlaylist?.let { pl ->
        AlertDialog(
            onDismissRequest = { deletingPlaylist = null },
            title = { Text("Delete Playlist?", fontWeight = FontWeight.Bold) },
            text = { Text("Delete playlist \"${pl.name}\"? Your audio files will remain safely in your library.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingPlaylist = null
                        onDeletePlaylist(pl.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlaylist = null }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val listState = rememberLazyListState()

    MikaVerticalFastScroller(
        listState = listState,
        totalItems = playlists.size + 3,
        sectionLabelProvider = { idx ->
            val plIdx = (idx - 3).coerceAtLeast(0)
            playlists.getOrNull(plIdx)?.name?.firstOrNull()?.uppercase()
        },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("playlists_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Smart Playlists",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                )
            }

            // Compact 2-Column Side-by-Side Grid of Default Smart Playlists
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("default_playlists_grid")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    SmartPlaylistGridCard(
                        title = "Favorites",
                        subtitle = "$favoritesCount tracks",
                        icon = MikaIcons.HeartBold,
                        tint = Color(0xFFF43F5E),
                        onCardClick = { onOpenCollection(CollectionType.FAVORITES) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("default_playlist_favorites")
                    )
                    SmartPlaylistGridCard(
                        title = "Lyrics List",
                        subtitle = "$lyricsCount tracks",
                        icon = MikaIcons.Lyrics,
                        tint = Color(0xFF06B6D4),
                        onCardClick = { onOpenCollection(CollectionType.LYRICS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("default_playlist_lyrics")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmartPlaylistGridCard(
                        title = "Most Played",
                        subtitle = "$mostPlayedCount tracks",
                        icon = MikaIcons.Chart,
                        tint = Color(0xFFF59E0B),
                        onCardClick = { onOpenCollection(CollectionType.MOST_PLAYED) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("default_playlist_most_played")
                    )
                    SmartPlaylistGridCard(
                        title = "Recently Played",
                        subtitle = "$recentlyPlayedCount tracks",
                        icon = MikaIcons.Clock,
                        tint = Color(0xFF10B981),
                        onCardClick = { onOpenCollection(CollectionType.RECENTLY_PLAYED) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("default_playlist_recently_played")
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${playlists.size} Custom Playlists",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Surface(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .testTag("create_playlist_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = MikaIcons.Add,
                            contentDescription = "New playlist",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(mikaScaledIcon(20.dp))
                        )
                        Text(
                            text = "New",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        if (playlists.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MikaIcons.Playlist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No custom playlists yet",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Tap New to build your own mix",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        items(playlists, key = { it.id }) { pl ->
            var menuExpanded by remember { mutableStateOf(false) }

            Surface(
                onClick = { onOpenPlaylist(pl.id) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlaylistCollageArtwork(
                        songs = pl.songs,
                        cornerRadius = 14.dp,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pl.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pl.songCount} tracks · ${Formatters.formatDuration(pl.totalDurationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = MikaIcons.MoreVert,
                                contentDescription = "Playlist options",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(MikaIcons.Edit, null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    renamingPlaylist = pl
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(MikaIcons.Trash, null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    deletingPlaylist = pl
                                }
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
fun PlaylistDetailScreen(
    playlist: Playlist?,
    allSongs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayAll: (List<Song>, Boolean) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onAddSongs: (List<Long>) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onReorderSongs: (List<Long>) -> Unit,
    onRenamePlaylist: (String) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(playlist == null) {
        if (playlist == null) onBack()
    }
    if (playlist == null) return

    var showAddPicker by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }

    if (showRename) {
        CreateOrRenamePlaylistDialog(
            title = "Rename Playlist",
            initialName = playlist.name,
            confirmLabel = "Save",
            onConfirm = onRenamePlaylist,
            onDismiss = { showRename = false }
        )
    }

    if (showAddPicker) {
        var pickedIds by remember { mutableStateOf(emptySet<Long>()) }
        ModalBottomSheet(onDismissRequest = { showAddPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Songs (${pickedIds.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(
                        onClick = {
                            onAddSongs(pickedIds.toList())
                            showAddPicker = false
                        },
                        enabled = pickedIds.isNotEmpty()
                    ) {
                        Text("Add")
                    }
                }
                LazyColumn(modifier = Modifier.height(380.dp)) {
                    items(allSongs, key = { it.id }) { song ->
                        val checked = song.id in pickedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pickedIds = if (checked) pickedIds - song.id else pickedIds + song.id
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(10.dp))
                            TrackArtwork(song = song, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = com.example.ui.theme.BrandDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = androidx.compose.ui.unit.TextUnit(22f, androidx.compose.ui.unit.TextUnitType.Sp)
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
                actions = {
                    TextButton(onClick = { isReorderMode = !isReorderMode }) {
                        Text(if (isReorderMode) "Done" else "Reorder")
                    }
                    IconButton(onClick = { showRename = true }) {
                        Icon(MikaIcons.Edit, contentDescription = "Rename playlist", modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { showAddPicker = true }) {
                        Icon(MikaIcons.Add, contentDescription = "Add songs", modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val detailListState = rememberLazyListState()
        MikaVerticalFastScroller(
            listState = detailListState,
            totalItems = playlist.songs.size + 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = detailListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlaylistCollageArtwork(
                        songs = playlist.songs,
                        cornerRadius = 22.dp,
                        modifier = Modifier.size(164.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "${playlist.songCount} tracks · ${Formatters.formatDuration(playlist.totalDurationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onPlayAll(playlist.songs, false) },
                            enabled = playlist.songs.isNotEmpty()
                        ) {
                            Icon(MikaIcons.PlayBold, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { onPlayAll(playlist.songs, true) },
                            enabled = playlist.songs.isNotEmpty()
                        ) {
                            Icon(MikaIcons.Shuffle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (playlist.songs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("This playlist is empty", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { showAddPicker = true }) {
                            Text("Add Songs")
                        }
                    }
                }
            } else {
                itemsIndexed(playlist.songs, key = { _, song -> song.id }) { index, song ->
                    val isCurrent = song.id == currentSongId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPlaySong(song, playlist.songs) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackArtwork(
                            song = song,
                            cornerRadius = 10.dp,
                            isCurrentTrack = isCurrent,
                            isPlaying = isPlaying,
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        if (isReorderMode) {
                            IconButton(
                                enabled = index > 0,
                                onClick = {
                                    val ids = playlist.songs.map { it.id }.toMutableList()
                                    val moved = ids.removeAt(index)
                                    ids.add(index - 1, moved)
                                    onReorderSongs(ids)
                                }
                            ) {
                                Icon(MikaIcons.ArrowUp, contentDescription = "Move up", modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                enabled = index < playlist.songs.lastIndex,
                                onClick = {
                                    val ids = playlist.songs.map { it.id }.toMutableList()
                                    val moved = ids.removeAt(index)
                                    ids.add(index + 1, moved)
                                    onReorderSongs(ids)
                                }
                            ) {
                                Icon(MikaIcons.ArrowDown, contentDescription = "Move down", modifier = Modifier.size(20.dp))
                            }
                        } else {
                            IconButton(onClick = { onRemoveSong(song.id) }) {
                                Icon(
                                    imageVector = MikaIcons.Remove,
                                    contentDescription = "Remove from playlist",
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
}

@Composable
private fun SmartPlaylistGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.1.dp, mikaGlassBorderBrush(accentTint = tint)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(mikaGlassFillBrush(tintColor = tint, tintStrength = 0.14f))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(tint.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(mikaScaledIcon(19.dp))
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

