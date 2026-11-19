package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.ui.components.TrackArtwork
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    trashedSongs: List<Song>,
    onBack: () -> Unit,
    onRestoreSong: (Song) -> Unit,
    onRestoreAll: () -> Unit,
    onPermanentDeleteSongs: (List<Song>) -> Unit
) {
    var pendingDeleteSongs by remember { mutableStateOf<List<Song>?>(null) }
    val accent = MaterialTheme.colorScheme.primary
    val dangerColor = MaterialTheme.colorScheme.error

    pendingDeleteSongs?.let { targets ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSongs = null },
            title = {
                Text(
                    text = if (targets.size == 1) "Delete Permanently?" else "Empty Trash?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val msg = if (targets.size == 1) {
                    "\"${targets.first().title}\" will be permanently removed from Mika Player and your device storage. This cannot be undone."
                } else {
                    "All ${targets.size} tracks in Trash will be permanently deleted from Mika Player and device storage. This cannot be undone."
                }
                Text(msg)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteSongs = null
                        onPermanentDeleteSongs(targets)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = dangerColor)
                ) {
                    Text("Delete Forever", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSongs = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trash Bin",
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
                        Icon(
                            imageVector = MikaIcons.ArrowLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    if (trashedSongs.isNotEmpty()) {
                        TextButton(onClick = onRestoreAll) {
                            Text("Restore All", fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { pendingDeleteSongs = trashedSongs },
                            modifier = Modifier.testTag("empty_trash_button")
                        ) {
                            Icon(
                                imageVector = MikaIcons.Trash,
                                contentDescription = "Empty trash",
                                tint = dangerColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (trashedSongs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp)
                    .testTag("trash_screen_empty"),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MikaIcons.Trash,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Trash is Empty",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tracks you move to Trash will appear here so you can restore them or permanently free up storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("trash_screen"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onRestoreAll,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(MikaIcons.Restore, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore All", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { pendingDeleteSongs = trashedSongs },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = dangerColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(MikaIcons.Trash, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Empty Trash", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(trashedSongs, key = { it.id }) { song ->
                    Surface(
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
                                song = song,
                                cornerRadius = 12.dp,
                                bitmapSizePx = 150,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.artist} · ${Formatters.formatDuration(song.durationMs)} · ${Formatters.formatFileSize(song.sizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onRestoreSong(song) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = MikaIcons.Restore,
                                    contentDescription = "Restore ${song.title}",
                                    tint = accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = { pendingDeleteSongs = listOf(song) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = MikaIcons.Trash,
                                    contentDescription = "Delete ${song.title} permanently",
                                    tint = dangerColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
