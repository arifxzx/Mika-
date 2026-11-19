package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MoodPresets
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.ui.icons.MikaIcons
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionsSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditTags: () -> Unit,
    onShowInfo: () -> Unit,
    onShare: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDeletePermanently: () -> Unit,
    onHideSong: () -> Unit = {},
    onRateAndMood: (() -> Unit)? = null
) {
    var confirmPermanentDelete by remember { mutableStateOf(false) }

    if (confirmPermanentDelete) {
        AlertDialog(
            onDismissRequest = { confirmPermanentDelete = false },
            title = { Text("Delete Permanently?", fontWeight = FontWeight.Bold) },
            text = {
                Text("\"${song.title}\" will be permanently deleted from Mika Player and your device storage. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmPermanentDelete = false
                        onDismiss()
                        onDeletePermanently()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmPermanentDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtwork(
                    song = song,
                    cornerRadius = 12.dp,
                    bitmapSizePx = 180,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = Formatters.formatArtistAndAlbum(song.artist, song.album),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            ActionSheetItem(MikaIcons.PlayBold, "Play now") { onPlayNow(); onDismiss() }
            ActionSheetItem(MikaIcons.PlayNext, "Play next") { onPlayNext(); onDismiss() }
            ActionSheetItem(MikaIcons.Queue, "Add to queue") { onAddToQueue(); onDismiss() }
            ActionSheetItem(
                if (song.isFavorite) MikaIcons.HeartBold else MikaIcons.HeartLinear,
                if (song.isFavorite) "Remove from Favorites" else "Add to Favorites"
            ) { onToggleFavorite(); onDismiss() }
            if (onRateAndMood != null) {
                ActionSheetItem(MikaIcons.Mood, "Mark mood & rating") {
                    onDismiss()
                    onRateAndMood()
                }
            }
            ActionSheetItem(MikaIcons.PlaylistAdd, "Add to playlist") {
                onDismiss()
                onAddToPlaylist()
            }
            ActionSheetItem(MikaIcons.TagEdit, "Edit tags & cover") {
                onDismiss()
                onEditTags()
            }
            ActionSheetItem(MikaIcons.InfoCircle, "Song information") {
                onDismiss()
                onShowInfo()
            }
            ActionSheetItem(MikaIcons.Share, "Share audio") {
                onDismiss()
                onShare()
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            ActionSheetItem(MikaIcons.EyeSlash, "Hide song") {
                onDismiss()
                onHideSong()
            }
            ActionSheetItem(MikaIcons.Trash, "Move to Trash") {
                onDismiss()
                onMoveToTrash()
            }
            ActionSheetItem(
                icon = MikaIcons.Trash,
                label = "Delete permanently",
                tint = MaterialTheme.colorScheme.error
            ) {
                confirmPermanentDelete = true
            }
        }
    }
}

@Composable
private fun ActionSheetItem(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SongInfoDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Audio Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Title", song.title)
                InfoRow("Artist", song.artist)
                if (song.album.isNotBlank() &&
                    !song.album.equals("<unknown>", ignoreCase = true) &&
                    !song.album.equals("unknown", ignoreCase = true) &&
                    !song.album.equals("unknown album", ignoreCase = true) &&
                    !song.album.equals(song.artist, ignoreCase = true)
                ) {
                    InfoRow("Album", song.album)
                }
                if (song.genre.isNotBlank()) InfoRow("Genre", song.genre)
                if (song.year > 0) InfoRow("Year", "${song.year}")
                InfoRow("Duration", Formatters.formatDuration(song.durationMs))
                InfoRow("Format", song.mimeType)
                InfoRow("File Size", Formatters.formatFileSize(song.sizeBytes))
                InfoRow("Play Count", "${song.playCount}")
                if (song.lastPlayedAt > 0L) {
                    InfoRow("Last Played", Formatters.formatTimestamp(song.lastPlayedAt))
                }
                InfoRow("Folder", song.folderName)
                InfoRow("Path", song.filePath.ifBlank { song.contentUri })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> TypedSortBottomSheet(
    title: String,
    fields: List<T>,
    currentField: T,
    currentDirection: SortDirection,
    labelProvider: (T) -> String,
    onSelect: (T, SortDirection) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("sort_bottom_sheet")
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SortDirection.entries.forEach { dir ->
                    val selected = dir == currentDirection
                    FilterChip(
                        selected = selected,
                        onClick = { onSelect(currentField, dir) },
                        label = { Text(dir.label, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (dir == SortDirection.ASCENDING) {
                                    MikaIcons.ArrowUp
                                } else {
                                    MikaIcons.ArrowDown
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            fields.forEach { field ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(field, currentDirection)
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = field == currentField,
                        onClick = {
                            onSelect(field, currentDirection)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = labelProvider(field),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (field == currentField) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun SortBottomSheet(
    currentField: SortField,
    currentDirection: SortDirection,
    onSelect: (SortField, SortDirection) -> Unit,
    onDismiss: () -> Unit
) {
    TypedSortBottomSheet(
        title = "Sort Tracks",
        fields = SortField.entries,
        currentField = currentField,
        currentDirection = currentDirection,
        labelProvider = { it.label },
        onSelect = onSelect,
        onDismiss = onDismiss
    )
}

@Composable
fun CreateOrRenamePlaylistDialog(
    title: String,
    initialName: String = "",
    confirmLabel: String = "Save",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_input")
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("playlist_confirm_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    songIdsToAdd: List<Long>,
    onSelectPlaylist: (Long) -> Unit,
    onCreateNewPlaylist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }

    if (showCreate) {
        CreateOrRenamePlaylistDialog(
            title = "New Playlist",
            confirmLabel = "Create & Add",
            onConfirm = { name ->
                onCreateNewPlaylist(name)
                onDismiss()
            },
            onDismiss = { showCreate = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
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
                    text = if (songIdsToAdd.size == 1) "Add 1 track to Playlist" else "Add ${songIdsToAdd.size} tracks to Playlist",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { showCreate = true }) {
                    Icon(MikaIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (playlists.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No playlists yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showCreate = true }) {
                        Text("Create your first playlist")
                    }
                }
            } else {
                LazyColumn {
                    items(playlists, key = { it.id }) { pl ->
                        ListItem(
                            headlineContent = { Text(pl.name, fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("${pl.songCount} tracks") },
                            leadingContent = {
                                PlaylistCollageArtwork(
                                    songs = pl.songs,
                                    cornerRadius = 10.dp,
                                    modifier = Modifier.size(46.dp)
                                )
                            },
                            trailingContent = {
                                if (pl.songs.any { it.id in songIdsToAdd }) {
                                    Icon(
                                        imageVector = MikaIcons.Check,
                                        contentDescription = "Already contains track",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                onSelectPlaylist(pl.id)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RateAndMoodDialog(
    titleLabel: String,
    initialRating: Int,
    initialMood: String,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onSave: (rating: Int, mood: String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember(initialRating) { mutableStateOf(initialRating.coerceIn(0, 5)) }
    var mood by remember(initialMood) { mutableStateOf(initialMood) }
    val accent = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Mood & Rating",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = titleLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Interactive 5-Star Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        val filled = star <= rating
                        IconButton(
                            onClick = { rating = if (rating == star) 0 else star },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("rate_star_$star")
                        ) {
                            Icon(
                                imageVector = if (filled) MikaIcons.StarFilled else MikaIcons.Star,
                                contentDescription = "Rate $star stars",
                                tint = if (filled) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    if (isFavorite != null && onToggleFavorite != null) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) MikaIcons.HeartBold else MikaIcons.HeartLinear,
                                contentDescription = "Toggle Favorite",
                                tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Mood Preset Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MoodPresets.entries.forEach { preset ->
                        val selected = mood.equals(preset, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { mood = if (selected) "" else preset },
                            label = {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = mood,
                    onValueChange = { mood = it },
                    label = { Text("Custom mood tag") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mood_custom_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(rating, mood.trim())
                    onDismiss()
                },
                modifier = Modifier.testTag("rate_mood_save")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
