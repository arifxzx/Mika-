package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.media.LyricsParser
import com.example.model.LyricsData
import com.example.model.PlaybackProgressState
import com.example.model.PlaybackUiState
import com.example.ui.components.MikaSleekSlider
import com.example.ui.components.TrackArtwork
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.LocalMikaColors
import com.example.util.Formatters
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsScreen(
    playbackState: PlaybackUiState,
    lyrics: LyricsData?,
    progressFlow: StateFlow<PlaybackProgressState>,
    artworkAccentPair: Pair<Color, Color>?,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onImportLyrics: () -> Unit,
    onSaveCustomLyrics: (String) -> Unit,
    onClearCustomLyrics: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val song = playbackState.currentSong
    val progress by progressFlow.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val secondaryAccent = LocalMikaColors.current.accentSecondary
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var fontSizeSp by remember { mutableFloatStateOf(20f) }
    var centeredAlign by remember { mutableStateOf(true) }
    var autoScrollEnabled by remember { mutableStateOf(true) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var showRevertConfirm by remember { mutableStateOf(false) }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val currentLineIndex by remember(lyrics, progress.positionMs) {
        derivedStateOf {
            if (lyrics == null || !lyrics.isSynced || lyrics.lines.isEmpty()) -1
            else lyrics.lines.indexOfLast { it.timestampMs <= progress.positionMs }
        }
    }

    LaunchedEffect(currentLineIndex, autoScrollEnabled) {
        if (autoScrollEnabled && currentLineIndex >= 0) {
            listState.animateScrollToItem((currentLineIndex - 2).coerceAtLeast(0))
        }
    }

    if (showEditorDialog && song != null) {
        var draftText by remember(lyrics, song.id) {
            mutableStateOf(
                lyrics?.lines?.joinToString("\n") { line ->
                    if (lyrics.isSynced && line.timestampMs > 0L) {
                        "${LyricsParser.formatLrcTimestamp(line.timestampMs)}${line.text}"
                    } else {
                        line.text
                    }
                }.orEmpty()
            )
        }
        var editorTab by remember { mutableIntStateOf(0) } // 0 = Raw / LRC, 1 = Live Sync
        var syncPointer by remember { mutableIntStateOf(0) }
        val draftLines = remember(draftText) {
            draftText.replace("\r\n", "\n").replace('\r', '\n').lines()
        }
        val tagStripRegex = remember { Regex("""^\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""") }

        Dialog(
            onDismissRequest = { showEditorDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxHeight(0.84f)
                    .testTag("lyrics_studio_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lyrics Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { showEditorDialog = false }, modifier = Modifier.size(40.dp)) {
                            Icon(MikaIcons.Close, contentDescription = "Close editor", modifier = Modifier.size(20.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = editorTab == 0,
                            onClick = { editorTab = 0 },
                            label = { Text("Raw / LRC", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("lyrics_studio_tab_raw")
                        )
                        FilterChip(
                            selected = editorTab == 1,
                            onClick = { editorTab = 1 },
                            label = { Text("Live Sync", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("lyrics_studio_tab_sync")
                        )
                    }

                    if (editorTab == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { draftText = LyricsParser.shiftLrcTimestamps(draftText, -500L) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-0.5s", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { draftText = LyricsParser.shiftLrcTimestamps(draftText, 500L) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+0.5s", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { draftText = LyricsParser.stripLrcTimestamps(draftText) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Strip", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { draftText = it },
                            placeholder = { Text("[00:12.40]Write or paste lyrics here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("lyrics_editor_input")
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = accent.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = Formatters.formatDuration(progress.positionMs),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = accent,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "Tap Stamp as lines play",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Surface(
                                    onClick = onTogglePlayPause,
                                    shape = CircleShape,
                                    color = accent,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (playbackState.isPlaying) MikaIcons.PauseBold else MikaIcons.PlayBold,
                                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (draftLines.all { it.isBlank() }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Add lyric lines in Raw / LRC first, then sync timestamps here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(draftLines) { idx, rawLine ->
                                    val match = tagStripRegex.find(rawLine)
                                    val cleanText = if (match != null) rawLine.removeRange(match.range).trimStart() else rawLine
                                    val stampedTag = match?.value
                                    val isCurrentTarget = idx == syncPointer.coerceIn(0, draftLines.lastIndex)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isCurrentTarget) {
                                            accent.copy(alpha = 0.14f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                syncPointer = idx
                                                if (match != null) {
                                                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                                                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                                                    val frac = match.groupValues[3]
                                                    val ms = when (frac.length) {
                                                        1 -> (frac.toLongOrNull() ?: 0L) * 100L
                                                        2 -> (frac.toLongOrNull() ?: 0L) * 10L
                                                        3 -> frac.toLongOrNull() ?: 0L
                                                        else -> 0L
                                                    }
                                                    onSeekTo(min * 60_000L + sec * 1000L + ms)
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = stampedTag ?: "[--:--.--]",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (stampedTag != null) accent else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = cleanText.ifBlank { "(blank line)" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (cleanText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilledTonalButton(
                                                onClick = {
                                                    draftText = LyricsParser.stampLineTimestamp(draftText, idx, progress.positionMs)
                                                    val nextNonBlank = ((idx + 1)..draftLines.lastIndex).firstOrNull {
                                                        draftLines[it].isNotBlank()
                                                    } ?: (idx + 1).coerceAtMost(draftLines.lastIndex)
                                                    syncPointer = nextNonBlank
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("lyrics_stamp_line_$idx")
                                            ) {
                                                Text("Stamp", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditorDialog = false }) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showEditorDialog = false
                                onSaveCustomLyrics(draftText)
                            },
                            modifier = Modifier.testTag("lyrics_editor_save")
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showRevertConfirm) {
        AlertDialog(
            onDismissRequest = { showRevertConfirm = false },
            title = { Text("Revert Saved Lyrics?", fontWeight = FontWeight.Bold) },
            text = { Text("Remove custom saved lyrics for this track and fall back to embedded audio tags or local .lrc file.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRevertConfirm = false
                        onClearCustomLyrics()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Revert", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val bgStart = (artworkAccentPair?.first ?: accent).copy(alpha = 0.22f)
    val bgEnd = (artworkAccentPair?.second ?: secondaryAccent).copy(alpha = 0.10f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            bgStart,
                            bgEnd,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("lyrics_screen")
        ) {
            // Enhanced Frosted Glass Top Track Header
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LocalMikaColors.current.glassSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, LocalMikaColors.current.glassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("lyrics_back_button")
                    ) {
                        Icon(MikaIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(22.dp))
                    }
                    if (song != null) {
                        TrackArtwork(
                            song = song,
                            cornerRadius = 12.dp,
                            bitmapSizePx = 144,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (lyrics != null && lyrics.sourceLabel.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = lyrics.sourceLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                        IconButton(
                            onClick = onOpenPlayer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(MikaIcons.Disc, contentDescription = "Open player", modifier = Modifier.size(22.dp))
                        }
                    } else {
                        Text(
                            text = "Lyrics Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Studio Controls Toolbar
            if (song != null) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                IconButton(
                                    onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(15f) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(MikaIcons.Remove, contentDescription = "Decrease font size", modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "${fontSizeSp.toInt()}sp",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                IconButton(
                                    onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(28f) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(MikaIcons.Add, contentDescription = "Increase font size", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    item {
                        FilterChip(
                            selected = centeredAlign,
                            onClick = { centeredAlign = !centeredAlign },
                            label = { Text(if (centeredAlign) "Centered" else "Left Align", fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (lyrics?.isSynced == true) {
                        item {
                            FilterChip(
                                selected = autoScrollEnabled,
                                onClick = { autoScrollEnabled = !autoScrollEnabled },
                                label = { Text("Auto-Sync", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    item {
                        FilterChip(
                            selected = false,
                            onClick = { showEditorDialog = true },
                            leadingIcon = { Icon(MikaIcons.Edit, null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Edit Lyrics", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("lyrics_edit_button")
                        )
                    }

                    item {
                        FilterChip(
                            selected = false,
                            onClick = onImportLyrics,
                            leadingIcon = { Icon(MikaIcons.BackupDownload, null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Import .lrc", fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (lyrics != null && lyrics.lines.isNotEmpty()) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val allText = lyrics.lines.joinToString("\n") { it.text }
                                    clipboard.setText(AnnotatedString(allText))
                                    copiedNotice = "Copied all lyrics"
                                },
                                leadingIcon = { Icon(MikaIcons.Copy, null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Copy All", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    if (lyrics?.sourceLabel == "Saved Lyrics") {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { showRevertConfirm = true },
                                leadingIcon = { Icon(MikaIcons.Restore, null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Revert", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }

            copiedNotice?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(1800)
                    copiedNotice = null
                }
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
            }

            // Main Lyrics Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    song == null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(MikaIcons.Lyrics, null, tint = accent, modifier = Modifier.size(54.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No Track Playing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Play a track to open Lyrics Studio.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    lyrics == null || lyrics.lines.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(MikaIcons.Lyrics, null, tint = accent, modifier = Modifier.size(36.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("No Lyrics Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Write, sync, or import a .lrc file.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { showEditorDialog = true }) {
                                    Icon(MikaIcons.Edit, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Write / Paste", fontWeight = FontWeight.Bold)
                                }
                                FilledTonalButton(onClick = onImportLyrics) {
                                    Icon(MikaIcons.BackupDownload, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Import .lrc", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(lyrics.lines, key = { idx, line -> "${idx}_${line.timestampMs}" }) { index, line ->
                                val isActive = lyrics.isSynced && index == currentLineIndex
                                val targetColor by animateColorAsState(
                                    targetValue = when {
                                        isActive -> MaterialTheme.colorScheme.onBackground
                                        lyrics.isSynced -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                                        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.90f)
                                    },
                                    animationSpec = tween(220),
                                    label = "lyrics_line_color"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isActive) accent.copy(alpha = 0.14f) else Color.Transparent
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                if (lyrics.isSynced && line.timestampMs >= 0L) {
                                                    onSeekTo(line.timestampMs)
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                clipboard.setText(AnnotatedString(line.text))
                                                copiedNotice = "Copied line to clipboard"
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = line.text,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = if (isActive) (fontSizeSp + 2f).sp else fontSizeSp.sp,
                                            lineHeight = ((if (isActive) fontSizeSp + 2f else fontSizeSp) * 1.38f).sp,
                                            fontWeight = if (isActive || !lyrics.isSynced) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = targetColor,
                                        textAlign = if (centeredAlign) TextAlign.Center else TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Docked Frosted Glass Player Transport (never covered by MiniPlayer)
            if (song != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = LocalMikaColors.current.surfaceElevated.copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LocalMikaColors.current.glassBorder),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val safeDuration = progress.durationMs.coerceAtLeast(1L)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MikaSleekSlider(
                            value = progress.positionMs.toFloat().coerceIn(0f, safeDuration.toFloat()),
                            onValueChange = { onSeekTo(it.toLong()) },
                            valueRange = 0f..safeDuration.toFloat(),
                            accentColor = accent,
                            secondaryColor = secondaryAccent,
                            trackHeight = 5.dp,
                            thumbRadius = 7.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Formatters.formatDuration(progress.positionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                IconButton(onClick = onSkipPrevious, modifier = Modifier.size(44.dp)) {
                                    Icon(MikaIcons.PreviousBold, contentDescription = "Previous", modifier = Modifier.size(24.dp))
                                }
                                Surface(
                                    onClick = onTogglePlayPause,
                                    shape = CircleShape,
                                    color = accent,
                                    modifier = Modifier.size(50.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (playbackState.isPlaying) MikaIcons.PauseBold else MikaIcons.PlayBold,
                                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = onSkipNext, modifier = Modifier.size(44.dp)) {
                                    Icon(MikaIcons.NextBold, contentDescription = "Next", modifier = Modifier.size(24.dp))
                                }
                            }
                            Text(
                                text = Formatters.formatDuration(progress.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
