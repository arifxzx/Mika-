package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlayHistoryEntity
import com.example.model.Album
import com.example.model.LibraryStats
import com.example.model.Song
import com.example.model.VideoItem
import com.example.ui.components.DayHourMatrixHeatmap
import com.example.ui.components.DaysHeatmapGrid
import com.example.ui.components.DonutSegmentItem
import com.example.ui.components.ListenTimeGaugeCard
import com.example.ui.components.ListeningClockRadialChart
import com.example.ui.components.MonthsBarChart
import com.example.ui.components.MultiSegmentDonutCard
import com.example.ui.components.StatsGlassCard
import com.example.ui.components.StatsMetricPill
import com.example.ui.components.StatsSongRankRow
import com.example.ui.components.StatsSummaryTile
import com.example.ui.components.TotalListensAreaChart
import com.example.ui.components.WeekdayBarChart
import com.example.ui.components.YearDistributionChart
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon
import com.example.util.Formatters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class StatsPeriodFilter(val label: String, val days: Int?) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    THREE_MONTHS("3 Months", 90),
    SIX_MONTHS("6 Months", 180),
    YEAR("Year", 365),
    ALL("All", null)
}

private data class ComputedStatsSnapshot(
    val totalListens: Int,
    val totalListenMs: Long,
    val uniqueTrackCount: Int,
    val uniqueArtistCount: Int,
    val activeDaysCount: Int,
    val totalSpanDays: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val busiestDayText: String,
    val monthlyCounts: IntArray,
    val cumulativeCurve: List<Float>,
    val rangeStartLabel: String,
    val rangeMidLabel: String,
    val rangeEndLabel: String,
    val heatmapWeeks: List<IntArray>,
    val heatmapMonthHeaders: List<String>,
    val hourCounts: IntArray,
    val weekdayCounts: IntArray,
    val dayHourMatrix: Array<IntArray>,
    val peakHourLabel: String,
    val peakDayLabel: String,
    val repeatRatePct: Int,
    val tracksOver1: Int,
    val tracksOver10: Int,
    val libraryReachPct: Int,
    val unplayedCount: Int,
    val firstListenTimeLabel: String,
    val lastListenTimeLabel: String,
    val obsessionEntries: List<Pair<Song, Pair<Int, String>>>,
    val discoveryEntries: List<Pair<Song, Pair<Int, String>>>,
    val topTracks: List<Pair<Song, Int>>,
    val topArtists: List<Triple<String, Int, Int>>,
    val completedAlbums: List<Triple<String, Int, Int>>,
    val streakSong: Pair<Song, Int>?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    stats: LibraryStats,
    rawSongs: List<Song> = emptyList(),
    albums: List<Album> = emptyList(),
    playHistory: List<PlayHistoryEntity> = emptyList(),
    videos: List<VideoItem> = emptyList(),
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayTopMix: () -> Unit,
    onOpenGenres: () -> Unit,
    onOpenGenre: (String) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val secondaryAccent = LocalMikaColors.current.accentSecondary
    var selectedPeriod by remember { mutableStateOf(StatsPeriodFilter.ALL) }
    var showAllObsessions by remember { mutableStateOf(false) }
    var showAllTopTracks by remember { mutableStateOf(false) }
    var showAllArtists by remember { mutableStateOf(false) }
    var showAllAlbums by remember { mutableStateOf(false) }

    var chartReveal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chartReveal = true }
    val chartProgress by animateFloatAsState(
        targetValue = if (chartReveal) 1f else 0f,
        animationSpec = tween(durationMillis = 720),
        label = "stats_chart_reveal"
    )

    val snapshot = remember(rawSongs, albums, playHistory, stats, selectedPeriod) {
        buildStatsSnapshot(
            stats = stats,
            songs = rawSongs,
            albums = albums,
            history = playHistory,
            period = selectedPeriod
        )
    }

    val totalVideoDurationMs = remember(videos) { videos.sumOf { it.durationMs } }
    val totalVideoSizeBytes = remember(videos) { videos.sumOf { it.sizeBytes } }
    val overallListenMs = maxOf(
        snapshot.totalListenMs,
        if (stats.estimatedListeningMs > 0L) stats.estimatedListeningMs else 0L
    )
    val decadeCounts = remember(rawSongs) {
        val buckets = linkedMapOf("1990s" to 0, "2000s" to 0, "2010s" to 0, "2020s" to 0)
        rawSongs.forEach { song ->
            when (song.year) {
                in 1990..1999 -> buckets["1990s"] = (buckets["1990s"] ?: 0) + 1
                in 2000..2009 -> buckets["2000s"] = (buckets["2000s"] ?: 0) + 1
                in 2010..2019 -> buckets["2010s"] = (buckets["2010s"] ?: 0) + 1
                in 2020..2035 -> buckets["2020s"] = (buckets["2020s"] ?: 0) + 1
            }
        }
        buckets.entries.map { it.key to it.value }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = MikaIcons.ArrowLeft,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(mikaScaledIcon(24.dp))
                    )
                }
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = BrandDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("statistics_screen"),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Audio Formats Donut + Per-Format Bar Breakdown
            if (stats.formatBreakdown.isNotEmpty()) {
                val formatPalette = listOf(
                    accent,
                    secondaryAccent,
                    Color(0xFF10B981),
                    Color(0xFFF59E0B),
                    Color(0xFF8B5CF6)
                )
                val totalTracksForPct = stats.formatBreakdown.sumOf { it.trackCount }.coerceAtLeast(1)
                val segments = stats.formatBreakdown.take(5).mapIndexed { i, f ->
                    val pct = ((f.trackCount * 100f) / totalTracksForPct).toInt()
                    DonutSegmentItem(
                        label = f.format,
                        count = f.trackCount,
                        color = formatPalette[i % formatPalette.size],
                        detail = "$pct% (${f.trackCount})"
                    )
                }
                item {
                    MultiSegmentDonutCard(
                        title = "Audio Formats Donut Chart",
                        subtitle = "",
                        centerValue = "${stats.totalSongs}",
                        centerLabel = "tracks",
                        segments = segments,
                        icon = MikaIcons.AudioFile,
                        progress = chartProgress,
                        footerContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                stats.formatBreakdown.take(5).forEachIndexed { idx, fmt ->
                                    val barColor = formatPalette[idx % formatPalette.size]
                                    val ratio = (fmt.trackCount.toFloat() / totalTracksForPct.toFloat()).coerceIn(0.03f, 1f) * chartProgress
                                    val sizeMb = String.format(Locale.US, "%.1f MB", fmt.sizeBytes / (1024f * 1024f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(barColor.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = MikaIcons.AudioFile,
                                                contentDescription = fmt.format,
                                                tint = barColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = fmt.format,
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${fmt.trackCount} files · $sizeMb",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { ratio },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(50)),
                                                color = barColor,
                                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // 2. Library Health & Vault (3 compact tiles matching screenshot)
            item {
                StatsGlassCard(
                    title = "Library Health & Vault",
                    icon = MikaIcons.Shield,
                    accent = accent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsSummaryTile(
                            value = "${stats.hiddenCount}",
                            label = "Hidden",
                            icon = MikaIcons.EyeSlash,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                        StatsSummaryTile(
                            value = "${snapshot.unplayedCount}",
                            label = "Unplayed",
                            icon = MikaIcons.MusicNote,
                            tint = accent,
                            modifier = Modifier.weight(1f)
                        )
                        StatsSummaryTile(
                            value = "${stats.trashedCount}",
                            label = "Trashed",
                            icon = MikaIcons.Trash,
                            tint = Color(0xFFEF4444),
                            onClick = onOpenTrash,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Video Library Overview (3 compact tiles matching screenshot)
            item {
                StatsGlassCard(
                    title = "Video Library Overview",
                    icon = MikaIcons.VideoBold,
                    accent = accent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsSummaryTile(
                            value = "${videos.size}",
                            label = "Videos",
                            icon = MikaIcons.VideoBold,
                            tint = accent,
                            modifier = Modifier.weight(1f)
                        )
                        StatsSummaryTile(
                            value = formatHoursMinutes(totalVideoDurationMs),
                            label = "Duration",
                            icon = MikaIcons.Clock,
                            tint = secondaryAccent,
                            modifier = Modifier.weight(1f)
                        )
                        StatsSummaryTile(
                            value = Formatters.formatFileSize(totalVideoSizeBytes),
                            label = "Video Size",
                            icon = MikaIcons.Disc,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Compact Library Overview Card
            item {
                StatsGlassCard(
                    title = "Library Overview",
                    icon = MikaIcons.Chart,
                    accent = accent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsSummaryTile(
                            value = "${stats.totalSongs}",
                            label = "Tracks",
                            icon = MikaIcons.MusicNote,
                            tint = accent,
                            modifier = Modifier.weight(1f)
                        )
                        StatsSummaryTile(
                            value = "${stats.albumsCount}",
                            label = "Albums",
                            icon = MikaIcons.Disc,
                            tint = secondaryAccent,
                            modifier = Modifier.weight(1f)
                        )
                        StatsSummaryTile(
                            value = "${stats.artistsCount}",
                            label = "Artists",
                            icon = MikaIcons.User,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsMetricPill(
                            value = "${stats.genresCount}",
                            label = "Genres",
                            icon = MikaIcons.Category,
                            tint = Color(0xFF14B8A6),
                            onClick = onOpenGenres
                        )
                        StatsMetricPill(formatHoursMinutes(stats.totalDurationMs), "Audio Length", MikaIcons.Clock, accent)
                        StatsMetricPill(formatHoursMinutes(overallListenMs), "Listened", MikaIcons.PlayBold, Color(0xFF10B981))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (stats.topPlayedSongs.isNotEmpty() || rawSongs.isNotEmpty()) {
                            Button(
                                onClick = onPlayTopMix,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(MikaIcons.PlayBold, null, modifier = Modifier.size(mikaScaledIcon(16.dp)))
                                Spacer(Modifier.width(6.dp))
                                Text("Top Mix", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        FilledTonalButton(
                            onClick = onOpenBackup,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(MikaIcons.BackupUpload, null, modifier = Modifier.size(mikaScaledIcon(16.dp)))
                            Spacer(Modifier.width(6.dp))
                            Text("Backup", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // 2. Charts Period Filter & Activity KPI Pills
            item {
                StatsGlassCard(
                    title = "Charts",
                    subtitle = "${selectedPeriod.label} activity insights",
                    icon = MikaIcons.StatusUp,
                    accent = accent
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StatsPeriodFilter.entries) { period ->
                            val selected = selectedPeriod == period
                            Surface(
                                onClick = { selectedPeriod = period },
                                shape = RoundedCornerShape(50),
                                color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Text(
                                    text = period.label,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsMetricPill("${snapshot.totalListens}", "Total Listens", MikaIcons.PlayLinear, accent)
                        StatsMetricPill(formatHoursMinutes(snapshot.totalListenMs), "Listen Time", MikaIcons.Clock, secondaryAccent)
                        StatsMetricPill("${snapshot.uniqueTrackCount}", "Tracks", MikaIcons.MusicNote, Color(0xFF10B981))
                        StatsMetricPill("${snapshot.uniqueArtistCount}", "Artists", MikaIcons.User, Color(0xFF8B5CF6))
                        StatsMetricPill("${snapshot.activeDaysCount}/${snapshot.totalSpanDays}", "Days Active", MikaIcons.Changelog, Color(0xFFF59E0B))
                        StatsMetricPill("${snapshot.currentStreakDays} Days", "Streak", MikaIcons.HeartBold, Color(0xFFF43F5E))
                    }
                }
            }

            // 3. Months Bar Chart
            item {
                StatsGlassCard(
                    title = "Months",
                    subtitle = "${Calendar.getInstance().get(Calendar.YEAR)} monthly listening breakdown",
                    icon = MikaIcons.Chart,
                    accent = accent
                ) {
                    MonthsBarChart(
                        monthlyCounts = snapshot.monthlyCounts,
                        accent = accent,
                        secondaryAccent = secondaryAccent,
                        progress = chartProgress
                    )
                }
            }

            // 4. Total Listens Cumulative Curve
            item {
                StatsGlassCard(
                    title = "Total Listens",
                    subtitle = "${snapshot.rangeStartLabel} → ${snapshot.rangeEndLabel}",
                    icon = MikaIcons.Chart,
                    accent = accent
                ) {
                    TotalListensAreaChart(
                        points = snapshot.cumulativeCurve,
                        startLabel = snapshot.rangeStartLabel,
                        midLabel = snapshot.rangeMidLabel,
                        endLabel = snapshot.rangeEndLabel,
                        accent = accent,
                        secondaryAccent = secondaryAccent,
                        progress = chartProgress
                    )
                }
            }

            // 5. Days Activity Heatmap Grid
            item {
                StatsGlassCard(
                    title = "Days",
                    subtitle = "Longest Streak: ${snapshot.longestStreakDays}d · Busiest: ${snapshot.busiestDayText}",
                    icon = MikaIcons.Changelog,
                    accent = accent
                ) {
                    DaysHeatmapGrid(
                        weeks = snapshot.heatmapWeeks,
                        monthHeaders = snapshot.heatmapMonthHeaders,
                        accent = accent
                    )
                }
            }

            // 6. Listening Clock + Weekday Bars + 7x24 Activity Matrix
            item {
                StatsGlassCard(
                    title = "Listening Clock",
                    subtitle = "Peak at ${snapshot.peakHourLabel} on ${snapshot.peakDayLabel}",
                    icon = MikaIcons.Clock,
                    accent = accent
                ) {
                    ListeningClockRadialChart(
                        hourCounts = snapshot.hourCounts,
                        peakHourLabel = snapshot.peakHourLabel,
                        peakDayLabel = snapshot.peakDayLabel,
                        accent = accent,
                        secondaryAccent = secondaryAccent,
                        progress = chartProgress
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    WeekdayBarChart(
                        weekdayCounts = snapshot.weekdayCounts,
                        accent = accent,
                        secondaryAccent = secondaryAccent,
                        progress = chartProgress
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    DayHourMatrixHeatmap(
                        matrix = snapshot.dayHourMatrix,
                        accent = accent
                    )
                }
            }

            // 7. Listening Habits Card
            item {
                StatsGlassCard(
                    title = "Habits",
                    subtitle = "Replay & exploration depth",
                    icon = MikaIcons.Repeat,
                    accent = accent
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsMetricPill("${snapshot.repeatRatePct}%", "Repeat Rate", MikaIcons.Repeat, accent)
                        StatsMetricPill("${snapshot.tracksOver1}", "Listens > 1", MikaIcons.MusicNote, secondaryAccent)
                        StatsMetricPill("${snapshot.tracksOver10}", "Listens ≥ 10", MikaIcons.Star, Color(0xFFF59E0B))
                        StatsMetricPill("${snapshot.libraryReachPct}%", "Library Reach", MikaIcons.Chart, Color(0xFF10B981))
                        StatsMetricPill("${snapshot.unplayedCount}", "Unplayed Tracks", MikaIcons.EyeSlash, Color(0xFF8B5CF6))
                        StatsMetricPill(snapshot.firstListenTimeLabel, "First Listen", MikaIcons.Clock, accent)
                        StatsMetricPill(snapshot.lastListenTimeLabel, "Last Listen", MikaIcons.Clock, secondaryAccent)
                    }
                    snapshot.streakSong?.let { (song, plays) ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Text(
                            text = "Streak Favorite · ${snapshot.longestStreakDays} Days",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatsSongRankRow(
                            rank = null,
                            song = song,
                            trailingText = "$plays plays",
                            accent = accent,
                            onClick = { onPlaySong(song, rawSongs) }
                        )
                    }
                }
            }

            // 8. Obsessions (Most Replayed) Card
            if (snapshot.obsessionEntries.isNotEmpty()) {
                val visibleObsessions = if (showAllObsessions) snapshot.obsessionEntries else snapshot.obsessionEntries.take(5)
                item {
                    StatsGlassCard(
                        title = "Obsession",
                        subtitle = "Top repeated tracks",
                        icon = MikaIcons.HeartBold,
                        accent = Color(0xFFF43F5E)
                    ) {
                        visibleObsessions.forEachIndexed { idx, (song, meta) ->
                            StatsSongRankRow(
                                rank = null,
                                song = song,
                                trailingText = "${meta.first} · ${meta.second}",
                                accent = accent,
                                onClick = { onPlaySong(song, snapshot.obsessionEntries.map { it.first }) }
                            )
                            if (idx < visibleObsessions.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            }
                        }
                        if (snapshot.obsessionEntries.size > 5) {
                            TextButton(
                                onClick = { showAllObsessions = !showAllObsessions },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(if (showAllObsessions) "Show less" else "Show more", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 9. Discoveries Card
            if (snapshot.discoveryEntries.isNotEmpty()) {
                item {
                    StatsGlassCard(
                        title = "Discoveries",
                        subtitle = "Recent additions & newly spun tracks",
                        icon = MikaIcons.Star,
                        accent = secondaryAccent
                    ) {
                        snapshot.discoveryEntries.take(5).forEachIndexed { idx, (song, meta) ->
                            StatsSongRankRow(
                                rank = null,
                                song = song,
                                trailingText = "${meta.first} · ${meta.second}",
                                accent = secondaryAccent,
                                onClick = { onPlaySong(song, snapshot.discoveryEntries.map { it.first }) }
                            )
                            if (idx < 4 && idx < snapshot.discoveryEntries.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            }
                        }
                    }
                }
            }

            // 10. Top Tracks Ranked List (1..10 + Show More)
            if (snapshot.topTracks.isNotEmpty()) {
                val visibleTop = if (showAllTopTracks) snapshot.topTracks else snapshot.topTracks.take(10)
                item {
                    StatsGlassCard(
                        title = "Top Tracks",
                        subtitle = "Most played in library",
                        icon = MikaIcons.MusicNote,
                        accent = accent
                    ) {
                        visibleTop.forEachIndexed { idx, (song, count) ->
                            StatsSongRankRow(
                                rank = idx + 1,
                                song = song,
                                trailingText = "$count",
                                accent = accent,
                                onClick = { onPlaySong(song, snapshot.topTracks.map { it.first }) }
                            )
                            if (idx < visibleTop.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            }
                        }
                        if (snapshot.topTracks.size > 10) {
                            TextButton(
                                onClick = { showAllTopTracks = !showAllTopTracks },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(if (showAllTopTracks) "Show less" else "Show more", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 11. Most Played Spotlight Card (Top Artist / Top Album / Top Genre)
            item {
                val topArtist = snapshot.topArtists.firstOrNull()
                val topAlbum = snapshot.completedAlbums.firstOrNull()
                val topGenre = stats.topGenres.firstOrNull()
                StatsGlassCard(
                    title = "Most Played",
                    subtitle = "Top champions across your library",
                    icon = MikaIcons.Star,
                    accent = Color(0xFFF59E0B)
                ) {
                    SpotlightRow("Artist", topArtist?.first ?: "—", topArtist?.third ?: 0, MikaIcons.User, accent)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    SpotlightRow("Album", topAlbum?.first ?: "—", topAlbum?.third ?: 0, MikaIcons.Disc, secondaryAccent)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    SpotlightRow(
                        label = "Genre",
                        name = topGenre?.name ?: "—",
                        count = topGenre?.songCount ?: 0,
                        icon = MikaIcons.Category,
                        tint = Color(0xFF14B8A6),
                        onClick = topGenre?.let { { onOpenGenre(it.name) } }
                    )
                }
            }

            // 12. Top Artists Progress Bars
            if (snapshot.topArtists.isNotEmpty()) {
                val visibleArtists = if (showAllArtists) snapshot.topArtists else snapshot.topArtists.take(10)
                val maxArtistScore = snapshot.topArtists.maxOfOrNull { maxOf(it.third, it.second) }?.coerceAtLeast(1) ?: 1
                item {
                    StatsGlassCard(
                        title = "Top Artists",
                        subtitle = "${snapshot.topArtists.size} artists ranked",
                        icon = MikaIcons.User,
                        accent = accent
                    ) {
                        visibleArtists.forEachIndexed { idx, (name, trackCount, plays) ->
                            val score = if (plays > 0) plays else trackCount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (idx < 3) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(22.dp)
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                LinearProgressIndicator(
                                    progress = { (score.toFloat() / maxArtistScore.toFloat()).coerceIn(0.06f, 1f) * chartProgress },
                                    modifier = Modifier
                                        .width(86.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = accent,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                                )
                                Text(
                                    text = if (plays > 0) "$plays" else "$trackCount trk",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (snapshot.topArtists.size > 10) {
                            TextButton(
                                onClick = { showAllArtists = !showAllArtists },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(if (showAllArtists) "Show less" else "Show more", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 13. Completed Albums Card
            if (snapshot.completedAlbums.isNotEmpty()) {
                val visibleAlbums = if (showAllAlbums) snapshot.completedAlbums else snapshot.completedAlbums.take(8)
                item {
                    StatsGlassCard(
                        title = "Completed Albums",
                        subtitle = "${snapshot.completedAlbums.size} Albums",
                        icon = MikaIcons.Disc,
                        accent = secondaryAccent
                    ) {
                        visibleAlbums.forEachIndexed { idx, (albumName, tracks, plays) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(MikaIcons.Disc, null, tint = secondaryAccent, modifier = Modifier.size(mikaScaledIcon(18.dp)))
                                Text(
                                    text = albumName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$tracks Tracks · $plays",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (idx < visibleAlbums.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                        if (snapshot.completedAlbums.size > 8) {
                            TextButton(
                                onClick = { showAllAlbums = !showAllAlbums },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(if (showAllAlbums) "Show less" else "Show more", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 14. Genres Breakdown Donut Card
            if (stats.topGenres.isNotEmpty()) {
                val palette = listOf(accent, secondaryAccent, Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFF43F5E))
                val segments = stats.topGenres.take(6).mapIndexed { i, g ->
                    DonutSegmentItem(g.name, g.songCount, palette[i % palette.size])
                }
                item {
                    MultiSegmentDonutCard(
                        title = "Total Listens by Genre",
                        subtitle = "${stats.genresCount} Genres",
                        centerValue = "${stats.totalSongs}",
                        centerLabel = "Tracks",
                        segments = segments,
                        icon = MikaIcons.Category,
                        progress = chartProgress
                    )
                }
            }

            if (decadeCounts.any { it.second > 0 }) {
                item {
                    StatsGlassCard(
                        title = "Year",
                        subtitle = "Tracks by release decade",
                        icon = MikaIcons.Changelog,
                        accent = accent
                    ) {
                        YearDistributionChart(
                            decadeCounts = decadeCounts,
                            accent = accent,
                            secondaryAccent = secondaryAccent,
                            progress = chartProgress
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightRow(
    label: String,
    name: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(mikaScaledIcon(18.dp)))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = tint
        )
    }
}

private fun buildStatsSnapshot(
    stats: LibraryStats,
    songs: List<Song>,
    albums: List<Album>,
    history: List<PlayHistoryEntity>,
    period: StatsPeriodFilter
): ComputedStatsSnapshot {
    val nowMs = System.currentTimeMillis()
    val cutoffMs = period.days?.let { nowMs - it * 86_400_000L } ?: 0L
    val filteredHistory = history
        .filter { it.playedAt >= cutoffMs }
        .sortedBy { it.playedAt }

    val songMap = songs.associateBy { it.id }
    val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())
    val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dayKeyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val cal = Calendar.getInstance()
    val currentYear = cal.get(Calendar.YEAR)

    val monthlyCounts = IntArray(12)
    val hourCounts = IntArray(24)
    val weekdayCounts = IntArray(7)
    val dayHourMatrix = Array(7) { IntArray(24) }
    val dayPlayCounts = linkedMapOf<String, Int>()
    val songPlayCountsFromHistory = HashMap<Long, Int>()
    val songFirstPlayAt = HashMap<Long, Long>()

    filteredHistory.forEach { entry ->
        cal.timeInMillis = entry.playedAt
        if (cal.get(Calendar.YEAR) == currentYear || period != StatsPeriodFilter.ALL) {
            monthlyCounts[cal.get(Calendar.MONTH)]++
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dow = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceIn(0, 6)
        hourCounts[hour]++
        weekdayCounts[dow]++
        dayHourMatrix[dow][hour]++

        val dayKey = dayKeyFmt.format(Date(entry.playedAt))
        dayPlayCounts[dayKey] = (dayPlayCounts[dayKey] ?: 0) + 1
        songPlayCountsFromHistory[entry.songId] = (songPlayCountsFromHistory[entry.songId] ?: 0) + 1
        songFirstPlayAt.putIfAbsent(entry.songId, entry.playedAt)
    }

    // Fallback to Song.lastPlayedAt / playCount when play_history table is empty
    if (filteredHistory.isEmpty()) {
        songs.filter { it.playCount > 0 }.forEach { song ->
            val ts = if (song.lastPlayedAt > 0L) song.lastPlayedAt else song.dateAddedSec * 1000L
            cal.timeInMillis = ts
            monthlyCounts[cal.get(Calendar.MONTH)] += song.playCount
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val dow = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceIn(0, 6)
            hourCounts[hour] += song.playCount
            weekdayCounts[dow] += song.playCount
            dayHourMatrix[dow][hour] += song.playCount
            val dayKey = dayKeyFmt.format(Date(ts))
            dayPlayCounts[dayKey] = (dayPlayCounts[dayKey] ?: 0) + song.playCount
        }
    }

    // Build cumulative curve (12 buckets)
    val totalHistoryPlays = filteredHistory.size.takeIf { it > 0 } ?: maxOf(stats.totalPlayEvents, stats.totalPlayCountSum)
    val cumulativeCurve = if (filteredHistory.size >= 2) {
        val buckets = 12
        val minT = filteredHistory.first().playedAt
        val spanT = (filteredHistory.last().playedAt - minT).coerceAtLeast(1L)
        val counts = IntArray(buckets)
        filteredHistory.forEach { e ->
            val b = (((e.playedAt - minT).toDouble() / spanT) * (buckets - 1)).toInt().coerceIn(0, buckets - 1)
            counts[b]++
        }
        var running = 0f
        counts.map {
            running += it
            running
        }
    } else {
        listOf(0f, totalHistoryPlays * 0.2f, totalHistoryPlays * 0.55f, totalHistoryPlays.toFloat())
    }

    // Build 18-week trailing heatmap grid
    val weekCount = 18
    val heatmapCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val dowOffset = get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        add(Calendar.DAY_OF_YEAR, -dowOffset - (weekCount - 1) * 7)
    }
    val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
    var prevMonth = -1
    val monthHeaders = mutableListOf<String>()
    val heatmapWeeks = List(weekCount) {
        val m = heatmapCal.get(Calendar.MONTH)
        if (m != prevMonth) {
            monthHeaders.add(monthFmt.format(heatmapCal.time))
            prevMonth = m
        } else {
            monthHeaders.add("")
        }
        IntArray(7) {
            val key = dayKeyFmt.format(heatmapCal.time)
            val v = dayPlayCounts[key] ?: 0
            heatmapCal.add(Calendar.DAY_OF_YEAR, 1)
            v
        }
    }

    // Streaks
    val sortedDays = dayPlayCounts.keys.sorted()
    var longestStreak = if (sortedDays.isEmpty()) 0 else 1
    var runStreak = 1
    for (i in 1 until sortedDays.size) {
        val d1 = dayKeyFmt.parse(sortedDays[i - 1])?.time ?: 0L
        val d2 = dayKeyFmt.parse(sortedDays[i])?.time ?: 0L
        if (d2 - d1 in 80_000_000L..92_000_000L) {
            runStreak++
            if (runStreak > longestStreak) longestStreak = runStreak
        } else {
            runStreak = 1
        }
    }
    val currentStreak = runStreak.takeIf { sortedDays.isNotEmpty() } ?: 0
    val busiestEntry = dayPlayCounts.maxByOrNull { it.value }
    val busiestDayText = busiestEntry?.let { "${it.key} (${it.value})" } ?: "No sessions"

    val peakHour = hourCounts.indices.maxByOrNull { hourCounts[it] } ?: 20
    val peakHourLabel = when {
        peakHour == 0 -> "12 AM"
        peakHour < 12 -> "$peakHour AM"
        peakHour == 12 -> "12 PM"
        else -> "${peakHour - 12} PM"
    }
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val peakDow = weekdayCounts.indices.maxByOrNull { weekdayCounts[it] } ?: 4

    val topTrackPairs = if (songPlayCountsFromHistory.isNotEmpty()) {
        songPlayCountsFromHistory.entries
            .mapNotNull { (id, cnt) -> songMap[id]?.let { it to cnt } }
            .sortedByDescending { it.second }
    } else {
        songs.filter { it.playCount > 0 }
            .sortedByDescending { it.playCount }
            .map { it to it.playCount }
            .ifEmpty { stats.topPlayedSongs.map { it to it.playCount } }
    }

    val repeatTracks = songs.count { it.playCount > 1 }
    val heavyTracks = songs.count { it.playCount >= 10 }
    val totalPlaysSum = topTrackPairs.sumOf { it.second }.coerceAtLeast(stats.totalPlayCountSum)
    val repeatRate = if (totalPlaysSum > 0 && topTrackPairs.isNotEmpty()) {
        (((totalPlaysSum - topTrackPairs.size).coerceAtLeast(0).toFloat() / totalPlaysSum.toFloat()) * 100f).roundToInt()
    } else 0
    val libraryReach = if (songs.isNotEmpty()) {
        ((songs.count { it.playCount > 0 }.toFloat() / songs.size.toFloat()) * 100f).roundToInt()
    } else 0

    val artistStats = songs
        .groupBy { it.artist.ifBlank { "Unknown Artist" } }
        .map { (name, list) -> Triple(name, list.size, list.sumOf { it.playCount }) }
        .sortedByDescending { maxOf(it.third, it.second) }

    val albumStats = albums
        .map { alb ->
            val albumSongs = songs.filter { it.albumId == alb.id }
            Triple(alb.name.ifBlank { "Unknown Album" }, alb.trackCount, albumSongs.sumOf { it.playCount })
        }
        .sortedByDescending { maxOf(it.third, it.second) }

    val obsessionList = topTrackPairs.take(15).map { (song, cnt) ->
        val d = if (song.lastPlayedAt > 0L) dateFmt.format(Date(song.lastPlayedAt)) else "Library"
        song to (cnt to d)
    }
    val discoveryList = songs
        .sortedByDescending { songFirstPlayAt[it.id] ?: (it.dateAddedSec * 1000L) }
        .take(8)
        .map { song ->
            val ts = songFirstPlayAt[song.id] ?: (song.dateAddedSec * 1000L)
            val d = if (ts > 0L) dateFmt.format(Date(ts)) else "New"
            song to (song.playCount.coerceAtLeast(1) to d)
        }

    val startLabel = filteredHistory.firstOrNull()?.let { dateFmt.format(Date(it.playedAt)) } ?: "Start"
    val endLabel = filteredHistory.lastOrNull()?.let { dateFmt.format(Date(it.playedAt)) } ?: "Today"
    val midLabel = if (filteredHistory.size >= 2) {
        dateFmt.format(Date((filteredHistory.first().playedAt + filteredHistory.last().playedAt) / 2L))
    } else ""

    val listenMs = topTrackPairs.sumOf { (s, c) -> s.durationMs.coerceAtLeast(0L) * c }

    return ComputedStatsSnapshot(
        totalListens = totalPlaysSum,
        totalListenMs = listenMs,
        uniqueTrackCount = topTrackPairs.size.takeIf { it > 0 } ?: stats.songsPlayedCount,
        uniqueArtistCount = topTrackPairs.map { it.first.artist }.distinct().size.takeIf { it > 0 } ?: stats.artistsCount,
        activeDaysCount = dayPlayCounts.size,
        totalSpanDays = (period.days ?: 365).coerceAtLeast(dayPlayCounts.size),
        currentStreakDays = currentStreak,
        longestStreakDays = longestStreak,
        busiestDayText = busiestDayText,
        monthlyCounts = monthlyCounts,
        cumulativeCurve = cumulativeCurve,
        rangeStartLabel = startLabel,
        rangeMidLabel = midLabel,
        rangeEndLabel = endLabel,
        heatmapWeeks = heatmapWeeks,
        heatmapMonthHeaders = monthHeaders,
        hourCounts = hourCounts,
        weekdayCounts = weekdayCounts,
        dayHourMatrix = dayHourMatrix,
        peakHourLabel = peakHourLabel,
        peakDayLabel = dayNames[peakDow],
        repeatRatePct = repeatRate,
        tracksOver1 = repeatTracks,
        tracksOver10 = heavyTracks,
        libraryReachPct = libraryReach,
        unplayedCount = (stats.totalSongs - stats.songsPlayedCount).coerceAtLeast(0),
        firstListenTimeLabel = filteredHistory.firstOrNull()?.let { timeFmt.format(Date(it.playedAt)) } ?: "—",
        lastListenTimeLabel = filteredHistory.lastOrNull()?.let { timeFmt.format(Date(it.playedAt)) } ?: "—",
        obsessionEntries = obsessionList,
        discoveryEntries = discoveryList,
        topTracks = topTrackPairs,
        topArtists = artistStats,
        completedAlbums = albumStats,
        streakSong = topTrackPairs.firstOrNull()
    )
}

private fun formatHoursMinutes(durationMs: Long): String {
    val totalMin = (durationMs / 60_000L).coerceAtLeast(0L)
    val hrs = totalMin / 60L
    val mins = totalMin % 60L
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}
