package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlayHistoryEntity
import com.example.model.CollectionType
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.VideoItem
import com.example.ui.components.MikaGlassSurface
import com.example.ui.components.MikaVerticalFastScroller
import com.example.ui.components.PlaylistCollageArtwork
import com.example.ui.components.TrackArtwork
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class HomeMixDeck(
    val id: String,
    val title: String,
    val subtitle: String,
    val songs: List<Song>,
    val accent: Color
)

@Composable
fun HomeScreen(
    allSongs: List<Song>,
    recentlyPlayed: List<Song>,
    recentlyAdded: List<Song>,
    mostPlayed: List<Song>,
    favorites: List<Song>,
    recentPlaylists: List<Playlist>,
    playlistsCount: Int,
    trashedCount: Int,
    currentSong: Song?,
    isPlayingAudio: Boolean,
    currentVideo: VideoItem?,
    isPlayingVideo: Boolean,
    hasAudioPermission: Boolean,
    bottomNavVisible: Boolean,
    onRequestPermission: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleAudioPlayPause: () -> Unit,
    onOpenFullAudioPlayer: () -> Unit,
    onShuffleAll: () -> Unit,
    onPlayFavorites: () -> Unit,
    onNavigateToLibrarySongs: () -> Unit,
    onNavigateToLibraryPlaylists: () -> Unit,
    onNavigateToLibraryFolders: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onImportAudio: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenCollection: (CollectionType) -> Unit,
    videos: List<VideoItem>,
    onPlayVideo: (VideoItem) -> Unit,
    onToggleVideoPlayPause: () -> Unit,
    onExpandVideoPlayer: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenPlayerStyleStudio: () -> Unit,
    playHistory: List<PlayHistoryEntity> = emptyList(),
    onRateAndMood: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val mikaColors = LocalMikaColors.current
    val accent = MaterialTheme.colorScheme.primary
    val secondary = mikaColors.accentSecondary
    val listState = rememberLazyListState()
    val nowMs = remember(allSongs.size, playHistory.size, recentlyPlayed.size) { System.currentTimeMillis() }

    val topRecentDayWindows = remember { listOf(1, 3, 6, 9, 12) }
    var selectedTopDays by remember { mutableIntStateOf(3) }
    var selectedMoodFilter by remember { mutableStateOf("All") }

    val mixes = remember(allSongs, favorites, mostPlayed, recentlyAdded, accent, secondary) {
        buildList {
            if (allSongs.isNotEmpty()) {
                val recSongs = (mostPlayed.take(6) + favorites.take(4) + allSongs.shuffled())
                    .distinctBy { it.id }
                    .take(15)
                add(
                    HomeMixDeck(
                        id = "recommended",
                        title = "Recommended",
                        subtitle = recSongs.take(3).joinToString(" · ") { it.title },
                        songs = recSongs,
                        accent = accent
                    )
                )
            }
            if (favorites.isNotEmpty()) {
                add(
                    HomeMixDeck(
                        id = "favorites_mix",
                        title = "Favorites Mix",
                        subtitle = favorites.take(3).joinToString(" · ") { it.artist },
                        songs = favorites.take(18),
                        accent = Color(0xFFF43F5E)
                    )
                )
            }
            if (mostPlayed.isNotEmpty()) {
                add(
                    HomeMixDeck(
                        id = "heavy_rotation",
                        title = "On Repeat",
                        subtitle = mostPlayed.take(3).joinToString(" · ") { it.title },
                        songs = mostPlayed.take(18),
                        accent = secondary
                    )
                )
            }
            if (recentlyAdded.isNotEmpty()) {
                add(
                    HomeMixDeck(
                        id = "fresh_finds",
                        title = "Fresh Additions",
                        subtitle = recentlyAdded.take(3).joinToString(" · ") { it.title },
                        songs = recentlyAdded.take(18),
                        accent = Color(0xFF10B981)
                    )
                )
            }
        }
    }

    val topRecentsForWindow = remember(allSongs, playHistory, mostPlayed, selectedTopDays, nowMs) {
        val songMap = allSongs.associateBy { it.id }
        val cutoffMs = nowMs - selectedTopDays * 86_400_000L
        val fromHistory = playHistory
            .filter { it.playedAt >= cutoffMs }
            .groupBy { it.songId }
            .entries
            .mapNotNull { (id, entries) -> songMap[id]?.let { it to entries.size } }
            .sortedByDescending { it.second }
            .map { (song, cnt) -> song.copy(playCount = maxOf(song.playCount, cnt)) }

        if (fromHistory.isNotEmpty()) {
            fromHistory.take(15)
        } else {
            val playedInWindow = allSongs
                .filter { it.playCount > 0 && (it.lastPlayedAt == 0L || it.lastPlayedAt >= cutoffMs) }
                .sortedByDescending { it.playCount }
            playedInWindow.ifEmpty { mostPlayed }.take(15)
        }
    }

    val moodTagsAvailable = remember(allSongs) {
        val explicitMoods = allSongs.mapNotNull { it.mood.trim().takeIf(String::isNotEmpty) }.distinct()
        listOf("All") + explicitMoods.take(8)
    }
    val moodOrRatedSongs = remember(allSongs, selectedMoodFilter) {
        val filtered = when {
            selectedMoodFilter != "All" -> allSongs.filter { it.mood.equals(selectedMoodFilter, ignoreCase = true) }
            else -> allSongs.filter { it.rating > 0 || it.mood.isNotBlank() }
                .sortedWith(compareByDescending<Song> { it.rating }.thenByDescending { it.playCount })
        }
        filtered.take(15)
    }

    val bottomClearance = if (bottomNavVisible) 184.dp else 124.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        mikaColors.gradientStart.copy(alpha = if (mikaColors.isDark) 0.42f else 0.30f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        MikaVerticalFastScroller(
            listState = listState,
            totalItems = 9,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_screen"),
                contentPadding = PaddingValues(top = 0.dp, bottom = bottomClearance),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Uniform top bar matching Library header (no subheader)
                item(key = "global_home_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_drawer_button")
                        ) {
                            Icon(
                                imageVector = MikaIcons.Menu,
                                contentDescription = "Open navigation menu",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(mikaScaledIcon(24.dp))
                            )
                        }
                        Text(
                            text = "Mika Player",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = BrandDisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        )
                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_search_button")
                        ) {
                            Icon(
                                imageVector = MikaIcons.Search,
                                contentDescription = "Search media",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(mikaScaledIcon(24.dp))
                            )
                        }
                        IconButton(
                            onClick = onOpenStatistics,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_stats_button")
                        ) {
                            Icon(
                                imageVector = MikaIcons.Chart,
                                contentDescription = "Statistics",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(mikaScaledIcon(24.dp))
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = MikaIcons.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(mikaScaledIcon(24.dp))
                            )
                        }
                    }
                }

                // Permission Prompt Banner (only when library empty & ungranted)
                if (!hasAudioPermission && allSongs.isEmpty() && videos.isEmpty()) {
                    item(key = "home_permission_card") {
                        MikaGlassSurface(
                            shape = RoundedCornerShape(22.dp),
                            tintColor = accent,
                            tintStrength = 0.12f,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Unlock Your Offline Media",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Grant media access to auto-discover your local tracks, folders, and videos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onRequestPermission,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accent,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text("Allow Access", fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = onImportAudio,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text("Pick File", fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. Mixes Wide Card Carousel (matching screenshot)
                if (mixes.isNotEmpty()) {
                    item(key = "home_mixes_section") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HomeIconSectionHeader(
                                title = "Mixes",
                                icon = MikaIcons.Radar,
                                actionIcon = null,
                                onActionClick = null
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(mixes, key = { it.id }) { mix ->
                                    val leadSong = mix.songs.firstOrNull()
                                    Column(
                                        modifier = Modifier
                                            .width(248.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable {
                                                leadSong?.let { onPlaySong(it, mix.songs) }
                                            }
                                            .testTag("home_mix_${mix.id}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(138.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(mix.accent.copy(alpha = 0.22f))
                                        ) {
                                            if (leadSong != null) {
                                                TrackArtwork(
                                                    song = leadSong,
                                                    cornerRadius = 18.dp,
                                                    bitmapSizePx = 360,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(
                                                                Color.Transparent,
                                                                Color.Black.copy(alpha = 0.45f)
                                                            )
                                                        )
                                                    )
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color.Black.copy(alpha = 0.68f),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = MikaIcons.PlayBold,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = "${mix.songs.size}",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = mix.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = mix.subtitle,
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

                // 2. Recent Listens Carousel (with relative time badge in corner)
                if (recentlyPlayed.isNotEmpty()) {
                    item(key = "home_recent_listens") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HomeIconSectionHeader(
                                title = "Recent Listens",
                                icon = MikaIcons.Clock,
                                actionIcon = MikaIcons.ArrowRight,
                                onActionClick = { onOpenCollection(CollectionType.RECENTLY_PLAYED) }
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(recentlyPlayed.take(18), key = { it.id }) { song ->
                                    val badge = remember(song.lastPlayedAt, nowMs) {
                                        if (song.lastPlayedAt > 0L) formatRelativeBadge(nowMs - song.lastPlayedAt) else "recent"
                                    }
                                    HomeArtworkTileCard(
                                        song = song,
                                        isCurrent = song.id == currentSong?.id,
                                        isPlaying = isPlayingAudio,
                                        topCornerBadge = badge,
                                        bottomCircleCount = null,
                                        onClick = { onPlaySong(song, recentlyPlayed) },
                                        onLongClick = { onRateAndMood(song) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Recently Added Carousel (with relative days badge in corner)
                if (recentlyAdded.isNotEmpty()) {
                    item(key = "home_recently_added") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HomeIconSectionHeader(
                                title = "Recently Added",
                                icon = MikaIcons.AudioFile,
                                actionIcon = MikaIcons.ArrowRight,
                                onActionClick = { onOpenCollection(CollectionType.RECENTLY_ADDED) }
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(recentlyAdded.take(18), key = { it.id }) { song ->
                                    val addedAgoMs = (nowMs - song.dateAddedSec * 1000L).coerceAtLeast(0L)
                                    val badge = remember(addedAgoMs) { formatDaysAgoBadge(addedAgoMs) }
                                    HomeArtworkTileCard(
                                        song = song,
                                        isCurrent = song.id == currentSong?.id,
                                        isPlaying = isPlayingAudio,
                                        topCornerBadge = badge,
                                        bottomCircleCount = null,
                                        onClick = { onPlaySong(song, recentlyAdded) },
                                        onLongClick = { onRateAndMood(song) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Top Recents with Day Window Filter Chips + Circular Count Badge
                if (topRecentsForWindow.isNotEmpty()) {
                    item(key = "home_top_recents") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HomeIconSectionHeader(
                                title = "Top Recents",
                                icon = MikaIcons.Award,
                                actionIcon = MikaIcons.ArrowRight,
                                onActionClick = { onOpenCollection(CollectionType.MOST_PLAYED) }
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                topRecentDayWindows.forEach { days ->
                                    val selected = selectedTopDays == days
                                    Surface(
                                        onClick = { selectedTopDays = days },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) accent.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) accent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Text(
                                            text = if (days == 1) "1 Day" else "$days Days",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(topRecentsForWindow, key = { it.id }) { song ->
                                    HomeArtworkTileCard(
                                        song = song,
                                        isCurrent = song.id == currentSong?.id,
                                        isPlaying = isPlayingAudio,
                                        topCornerBadge = null,
                                        bottomCircleCount = song.playCount.takeIf { it > 0 },
                                        onClick = { onPlaySong(song, topRecentsForWindow) },
                                        onLongClick = { onRateAndMood(song) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Playlists Section
                if (recentPlaylists.isNotEmpty()) {
                    item(key = "home_playlists") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HomeIconSectionHeader(
                                title = "Playlists",
                                icon = MikaIcons.Playlist,
                                actionIcon = MikaIcons.ArrowRight,
                                onActionClick = onNavigateToLibraryPlaylists
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(recentPlaylists, key = { it.id }) { playlist ->
                                    Column(
                                        modifier = Modifier
                                            .width(136.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onOpenPlaylist(playlist.id) }
                                            .padding(bottom = 4.dp)
                                    ) {
                                        PlaylistCollageArtwork(
                                            songs = playlist.songs,
                                            cornerRadius = 16.dp,
                                            modifier = Modifier.size(136.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${playlist.songCount} tracks",
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

                // 6. Videos & Clips Carousel (directly follows, zero shortcut strip between Playlists and Videos)
                item(key = "home_videos_section") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeIconSectionHeader(
                            title = "Videos & Clips",
                            icon = MikaIcons.VideoBold,
                            actionIcon = MikaIcons.ArrowRight,
                            onActionClick = onOpenVideos
                        )
                        if (videos.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(videos.take(15), key = { it.id }) { video ->
                                    HomeVideoCard(
                                        video = video,
                                        accent = accent,
                                        onClick = { onPlayVideo(video) }
                                    )
                                }
                            }
                        } else {
                            MikaGlassSurface(
                                shape = RoundedCornerShape(18.dp),
                                tintColor = secondary,
                                tintStrength = 0.08f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable(onClick = onOpenVideos)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(secondary.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = MikaIcons.VideoBold,
                                            contentDescription = null,
                                            tint = secondary,
                                            modifier = Modifier.size(mikaScaledIcon(20.dp))
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Watch Offline Videos",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap to browse indexed video clips",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = MikaIcons.ArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(mikaScaledIcon(18.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Moods & Top Rated Section
                if (moodOrRatedSongs.isNotEmpty() || moodTagsAvailable.size > 1) {
                    item(key = "home_moods_rated") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HomeIconSectionHeader(
                                title = "Moods & Rated",
                                icon = MikaIcons.Mood,
                                actionIcon = null,
                                onActionClick = null
                            )
                            if (moodTagsAvailable.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    moodTagsAvailable.forEach { mood ->
                                        val selected = selectedMoodFilter == mood
                                        Surface(
                                            onClick = { selectedMoodFilter = mood },
                                            shape = RoundedCornerShape(50),
                                            color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = mood,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (moodOrRatedSongs.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(moodOrRatedSongs, key = { it.id }) { song ->
                                        val badge = when {
                                            song.mood.isNotBlank() -> song.mood
                                            song.rating > 0 -> "${song.rating}★"
                                            else -> null
                                        }
                                        HomeArtworkTileCard(
                                            song = song,
                                            isCurrent = song.id == currentSong?.id,
                                            isPlaying = isPlayingAudio,
                                            topCornerBadge = badge,
                                            bottomCircleCount = null,
                                            onClick = { onPlaySong(song, moodOrRatedSongs) },
                                            onLongClick = { onRateAndMood(song) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. Favorite Tracks Carousel
                if (favorites.isNotEmpty()) {
                    item(key = "home_favorites_audio") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HomeIconSectionHeader(
                                title = "Favorites",
                                icon = MikaIcons.HeartBold,
                                actionIcon = MikaIcons.ArrowRight,
                                onActionClick = { onOpenCollection(CollectionType.FAVORITES) }
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(favorites.take(18), key = { it.id }) { song ->
                                    HomeArtworkTileCard(
                                        song = song,
                                        isCurrent = song.id == currentSong?.id,
                                        isPlaying = isPlayingAudio,
                                        topCornerBadge = null,
                                        bottomCircleCount = null,
                                        onClick = { onPlaySong(song, favorites) },
                                        onLongClick = { onRateAndMood(song) }
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
private fun HomeIconSectionHeader(
    title: String,
    icon: ImageVector,
    actionIcon: ImageVector?,
    onActionClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(mikaScaledIcon(19.dp))
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (actionIcon != null && onActionClick != null) {
            IconButton(
                onClick = onActionClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = "View $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(mikaScaledIcon(18.dp))
                )
            }
        }
    }
}

@Composable
private fun HomeArtworkTileCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    topCornerBadge: String?,
    bottomCircleCount: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(118.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.size(118.dp)) {
            TrackArtwork(
                song = song,
                cornerRadius = 14.dp,
                isCurrentTrack = isCurrent,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize()
            )
            if (!topCornerBadge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 10.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = topCornerBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            if (bottomCircleCount != null && bottomCircleCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.74f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "$bottomCircleCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist.ifBlank { "Unknown" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoThumbImage(path: String?, contentDescription: String) {
    var thumbBitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        thumbBitmap = withContext(Dispatchers.IO) {
            path?.let { p ->
                runCatching { BitmapFactory.decodeFile(p)?.asImageBitmap() }.getOrNull()
            }
        }
    }
    val bmp = thumbBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Icon(
            imageVector = MikaIcons.VideoBold,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(mikaScaledIcon(28.dp))
        )
    }
}

@Composable
private fun HomeVideoCard(
    video: VideoItem,
    accent: Color,
    onClick: () -> Unit
) {
    MikaGlassSurface(
        shape = RoundedCornerShape(16.dp),
        tintColor = accent,
        tintStrength = 0.08f,
        modifier = Modifier
            .width(184.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                VideoThumbImage(
                    path = video.thumbnailPath,
                    contentDescription = video.title
                )
                if (video.durationMs > 0L) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = Formatters.formatDuration(video.durationMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = video.folderName.ifBlank { "Video" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatRelativeBadge(elapsedMs: Long): String {
    val sec = (elapsedMs / 1000L).coerceAtLeast(0L)
    return when {
        sec < 45L -> "a few seconds"
        sec < 90L -> "a minute"
        sec < 3600L -> "${sec / 60L} min"
        sec < 86_400L -> "${sec / 3600L} hours"
        else -> "${sec / 86_400L} days"
    }
}

private fun formatDaysAgoBadge(elapsedMs: Long): String {
    val days = (elapsedMs / 86_400_000L).coerceAtLeast(0L)
    return when {
        days == 0L -> "today"
        days == 1L -> "1 day"
        else -> "$days days"
    }
}
