package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.model.LibraryStats
import com.example.ui.components.MikaGlassSurface
import com.example.ui.components.MikaVerticalFastScroller
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon

private const val MIKA_TELEGRAM_URL = "https://t.me/sagorxzx"
private const val MIKA_DISCORD_URL = "https://discord.gg"
private const val MIKA_FACEBOOK_URL = "https://www.facebook.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    stats: LibraryStats,
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val secondaryAccent = LocalMikaColors.current.accentSecondary
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()

    var showGuideDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }

    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            title = { Text("Quick Guide", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GuideBullet("Swipe Mini-Player Up/Down", "Drag the Mini-Player upward to expand Full Player, or swipe left/right to skip tracks.")
                    GuideBullet("Artwork Gestures & Swipes", "Tap artwork for details or lyrics, swipe horizontally to skip tracks, or customize swipe actions in Settings.")
                    GuideBullet("Smart DSP & Auto Coloring", "Customize 5-band Equalizer, Speed & Pitch, and Auto Coloring in Settings.")
                    GuideBullet("Offline Backup", "Back up playlists, favorites, play history, and settings locally to /mika/backup.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showGuideDialog = false }) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = { Text("Changelog · v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GuideBullet("Audio & Video Home", "Unified Home hub across audio mixes, recent listens, playlists, and videos.")
                    GuideBullet("Real Waveform & Player Styles", "Added real waveform seek bars, capsule/broken playback icon sets, and floating PiP video window.")
                    GuideBullet("Mood, Rating & History", "Track rating stars, mood tags, dedicated History tab, and customizable tab actions.")
                    GuideBullet("Smart Indexing & Backups", "Auto media discovery, .nomedia support, duplicate finder, and /mika/backup snapshots.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelogDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text("Open-Source Licenses", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GuideBullet("Jetpack Compose & Material 3", "Apache License 2.0 · UI Toolkit")
                    GuideBullet("AndroidX Media3 (ExoPlayer & MediaSession)", "Apache License 2.0 · Local audio & video playback")
                    GuideBullet("Room Database & DataStore", "Apache License 2.0 · Offline library persistence")
                    GuideBullet("AndroidX Palette & Kotlinx Serialization", "Apache License 2.0 · Color sampling & JSON backup")
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showDonateDialog) {
        AlertDialog(
            onDismissRequest = { showDonateDialog = false },
            title = { Text("Support Mika Player", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Thank you for using Mika Player! Every bit of support helps keep the app fast, ad-free, and offline-first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GuideBullet("Telegram Community", "Message @sagorxzx for direct support or sponsorship options.")
                    GuideBullet("Spread the Word", "Share playlists, rate tracks, and invite friends to our community.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDonateDialog = false
                        runCatching { uriHandler.openUri(MIKA_TELEGRAM_URL) }
                    }
                ) {
                    Text("Open Telegram", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDonateDialog = false }) {
                    Text("Close")
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
                        text = "About",
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
                        Icon(MikaIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(mikaScaledIcon(24.dp)))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        MikaVerticalFastScroller(
            listState = listState,
            totalItems = 6,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("about_screen"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Card with Overlapping Centered Circular Icon + Inline Developer Row
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        MikaGlassSurface(
                            shape = RoundedCornerShape(24.dp),
                            tintColor = accent,
                            tintStrength = 0.12f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 44.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                accent.copy(alpha = 0.14f),
                                                secondaryAccent.copy(alpha = 0.06f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(top = 54.dp, start = 18.dp, end = 18.dp, bottom = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Mika Player",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = BrandDisplayFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Offline Hi-Fi Audio & Video",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Spacer(Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(accent.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = MikaIcons.DeveloperAvatar,
                                            contentDescription = "Developer",
                                            tint = accent,
                                            modifier = Modifier.size(mikaScaledIcon(24.dp))
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Sagor Islam",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "App Developer",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(3.dp, MaterialTheme.colorScheme.background),
                            modifier = Modifier.size(88.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.mika_icon_fg),
                                contentDescription = "Mika Player icon",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            )
                        }
                    }
                }

                // Socials Card
                item {
                    MikaGlassSurface(
                        shape = RoundedCornerShape(22.dp),
                        tintColor = accent,
                        tintStrength = 0.08f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("about_socials_card")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = MikaIcons.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(mikaScaledIcon(22.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Socials",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "↳ Join us on our platforms for Updates, Tips, Discussion & Ideas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                AboutActionRow(
                                    icon = MikaIcons.Telegram,
                                    title = "Telegram",
                                    subtitle = "Updates & channel announcements",
                                    onClick = { runCatching { uriHandler.openUri(MIKA_TELEGRAM_URL) } }
                                )
                                AboutActionRow(
                                    icon = MikaIcons.Facebook,
                                    title = "Facebook",
                                    subtitle = "News, posts & community updates",
                                    onClick = { runCatching { uriHandler.openUri(MIKA_FACEBOOK_URL) } }
                                )
                                AboutActionRow(
                                    icon = MikaIcons.Discord,
                                    title = "Discord",
                                    subtitle = "Community chat & feature ideas",
                                    onClick = { runCatching { uriHandler.openUri(MIKA_DISCORD_URL) } }
                                )
                            }
                        }
                    }
                }

                // Donate / Support Section Card
                item {
                    AboutGroupedCard(
                        headerIcon = MikaIcons.Donate,
                        headerTitle = "Donate"
                    ) {
                        AboutActionRow(
                            icon = MikaIcons.Donate,
                            title = "Support Mika Player",
                            subtitle = "Help support independent offline app development",
                            onClick = { showDonateDialog = true }
                        )
                    }
                }

                // Development Section Card
                item {
                    AboutGroupedCard(
                        headerIcon = MikaIcons.Code,
                        headerTitle = "Development"
                    ) {
                        AboutActionRow(
                            icon = MikaIcons.Guide,
                            title = "Guide",
                            subtitle = "Gestures, lyrics sync & DSP tips",
                            onClick = { showGuideDialog = true }
                        )
                        AboutActionRow(
                            icon = MikaIcons.Changelog,
                            title = "Changelog",
                            subtitle = "What's new in v${BuildConfig.VERSION_NAME}",
                            onClick = { showChangelogDialog = true }
                        )
                    }
                }

                // Library Snapshot & Others Card
                item {
                    AboutGroupedCard(
                        headerIcon = MikaIcons.InfoCircle,
                        headerTitle = "App & Library"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AboutMetricBox("Tracks", "${stats.totalSongs}", Modifier.weight(1f))
                            AboutMetricBox("Albums", "${stats.albumsCount}", Modifier.weight(1f))
                            AboutMetricBox("Artists", "${stats.artistsCount}", Modifier.weight(1f))
                            AboutMetricBox("Genres", "${stats.genresCount}", Modifier.weight(1f))
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        AboutActionRow(
                            icon = MikaIcons.DocumentText,
                            title = "License",
                            subtitle = "Open-source libraries & agreements",
                            onClick = { showLicensesDialog = true }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = MikaIcons.Cpu,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App Version",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME} (Offline Hi-Fi Build)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = accent.copy(alpha = 0.14f)
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = MikaIcons.Check,
                                        contentDescription = "Up to date",
                                        tint = accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = MikaIcons.Shield,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline Privacy",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "No internet permission · artwork, lyrics & stats stay on device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AboutActionRow(
                            icon = MikaIcons.BackupUpload,
                            title = "Backup & Restore",
                            subtitle = "Export or restore playlists, favorites & preferences",
                            trailing = {
                                Icon(
                                    imageVector = MikaIcons.ArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = onOpenBackup
                        )

                        AboutActionRow(
                            icon = MikaIcons.Settings,
                            title = "Settings",
                            subtitle = "Customize appearance, navigation tabs & audio DSP",
                            trailing = {
                                Icon(
                                    imageVector = MikaIcons.ArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = onOpenSettings
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutGroupedCard(
    headerIcon: ImageVector,
    headerTitle: String,
    content: @Composable () -> Unit
) {
    MikaGlassSurface(
        shape = RoundedCornerShape(22.dp),
        tintStrength = 0.07f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = headerIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(mikaScaledIcon(22.dp))
                )
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            content()
        }
    }
}

@Composable
private fun AboutActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(mikaScaledIcon(22.dp))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun AboutMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GuideBullet(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
