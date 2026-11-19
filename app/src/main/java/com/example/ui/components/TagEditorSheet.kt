package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.Song
import com.example.ui.icons.MikaIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorSheet(
    songs: List<Song>,
    onSave: (
        title: String?,
        artist: String?,
        album: String?,
        genre: String?,
        year: Int?,
        trackNumber: Int?,
        artworkUri: Uri?,
        clearArtwork: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    if (songs.isEmpty()) return
    val isBatch = songs.size > 1
    val singleSong = songs.first()
    val accent = MaterialTheme.colorScheme.primary
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun commonValue(selector: (Song) -> String): String {
        val values = songs.map { selector(it).trim() }.distinct()
        return if (values.size == 1) values.first() else ""
    }

    var title by remember(songs) { mutableStateOf(if (isBatch) "" else singleSong.title) }
    var artist by remember(songs) { mutableStateOf(if (isBatch) commonValue { it.artist } else singleSong.artist) }
    var album by remember(songs) { mutableStateOf(if (isBatch) commonValue { it.album } else singleSong.album) }
    var genre by remember(songs) { mutableStateOf(if (isBatch) commonValue { it.genre } else singleSong.genre) }
    var yearText by remember(songs) {
        mutableStateOf(
            if (isBatch) {
                val distinctYears = songs.map { it.year }.distinct()
                if (distinctYears.size == 1 && distinctYears.first() > 0) distinctYears.first().toString() else ""
            } else {
                if (singleSong.year > 0) singleSong.year.toString() else ""
            }
        )
    }
    var trackNumberText by remember(songs) {
        mutableStateOf(if (!isBatch && singleSong.trackNumber > 0) singleSong.trackNumber.toString() else "")
    }

    var pickedArtworkUri by remember(songs) { mutableStateOf<Uri?>(null) }
    var clearCustomArtwork by remember(songs) { mutableStateOf(false) }

    val artworkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedArtworkUri = uri
            clearCustomArtwork = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp)
                .testTag("tag_editor_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = MikaIcons.TagEdit,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isBatch) "Batch Tag Editor" else "Edit Tags & Cover",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBatch) {
                                "${songs.size} tracks selected · Blank fields stay unchanged"
                            } else {
                                singleSong.title
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(MikaIcons.Close, contentDescription = "Close tag editor")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Artwork Preview & Picker Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            pickedArtworkUri != null -> {
                                AsyncImage(
                                    model = pickedArtworkUri,
                                    contentDescription = "New cover preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            clearCustomArtwork -> {
                                Icon(
                                    imageVector = MikaIcons.MusicNote,
                                    contentDescription = "Default embedded artwork",
                                    tint = accent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            else -> {
                                TrackArtwork(
                                    song = singleSong,
                                    cornerRadius = 16.dp,
                                    bitmapSizePx = 240,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isBatch) "Batch Cover Artwork" else "Track Cover Artwork",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                pickedArtworkUri != null -> "New photo ready to apply"
                                clearCustomArtwork -> "Will revert to original audio cover"
                                else -> "Pick any image offline via Photo Picker"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    artworkPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("tag_pick_artwork_button")
                            ) {
                                Text("Change Cover", fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = {
                                    pickedArtworkUri = null
                                    clearCustomArtwork = true
                                },
                                modifier = Modifier.testTag("tag_reset_artwork_button")
                            ) {
                                Text("Reset", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (isBatch) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Individual song titles stay preserved during batch updates. Fill in any field below to apply it across all ${songs.size} tracks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            } else {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Track Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_title_input")
                )
            }

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Artist") },
                placeholder = if (isBatch) {
                    { Text("Leave blank to keep per-song artist") }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tag_artist_input")
            )

            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text("Album") },
                placeholder = if (isBatch) {
                    { Text("Leave blank to keep per-song album") }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tag_album_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre") },
                    placeholder = if (isBatch) {
                        { Text("Keep existing") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("tag_genre_input")
                )

                OutlinedTextField(
                    value = yearText,
                    onValueChange = { input ->
                        yearText = input.filter { it.isDigit() }.take(4)
                    },
                    label = { Text("Year") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tag_year_input")
                )

                if (!isBatch) {
                    OutlinedTextField(
                        value = trackNumberText,
                        onValueChange = { input ->
                            trackNumberText = input.filter { it.isDigit() }.take(3)
                        },
                        label = { Text("Track #") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("tag_track_input")
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val safeTitle = if (!isBatch) title.trim().ifBlank { null } else null
                        val safeArtist = artist.trim().ifBlank { null }
                        val safeAlbum = album.trim().ifBlank { null }
                        val safeGenre = genre.trim().ifBlank { null }
                        val safeYear = yearText.trim().toIntOrNull()?.takeIf { it > 0 }
                        val safeTrack = if (!isBatch) trackNumberText.trim().toIntOrNull()?.takeIf { it > 0 } else null
                        onSave(
                            safeTitle,
                            safeArtist,
                            safeAlbum,
                            safeGenre,
                            safeYear,
                            safeTrack,
                            pickedArtworkUri,
                            clearCustomArtwork
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("tag_save_button")
                ) {
                    Icon(MikaIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isBatch) "Apply to ${songs.size} Tracks" else "Save Changes",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
