package com.example.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.example.model.PlaybackIconStyle

/**
 * Iconsax Broken & Bold vector icon system sourced from iconsax_broken_icon.zip (24dp viewport).
 */
object MikaIcons {

    private fun brokenIcon(name: String, paths: List<String>, strokeWidth: Float = 1.85f): ImageVector =
        ImageVector.Builder(
            name = "IconsaxBroken.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()

    private fun filledIcon(name: String, paths: List<String>, fillType: PathFillType = PathFillType.NonZero): ImageVector =
        ImageVector.Builder(
            name = "IconsaxBold.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = SolidColor(Color.Black),
                    pathFillType = fillType
                )
            }
        }.build()

    private inline fun linearIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name = "IconsaxLinear.$name", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply(block).build()
    private inline fun boldIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name = "IconsaxBold.$name", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply(block).build()
    private fun ImageVector.Builder.linearStroke(width: Float = 1.9f, block: PathBuilder.() -> Unit) {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = block)
    }
    private fun ImageVector.Builder.boldFill(fillType: PathFillType = PathFillType.NonZero, block: PathBuilder.() -> Unit) {
        path(fill = SolidColor(Color.Black), pathFillType = fillType, pathBuilder = block)
    }

    val Menu: ImageVector by lazy { brokenIcon("Menu", listOf("M10.05 4.5H21m-18 0h2.98M3 9.5h9.47M3 14.5h18m-18 5h9.47")) }
    val HomeLinear: ImageVector by lazy { brokenIcon("HomeLinear", listOf("M22 10.5c0-1.21-.81-2.76-1.8-3.45l-6.18-4.33c-1.4-.98-3.65-.93-5 .12l-5.39 4.2C2.73 7.74 2 9.23 2 10.36v7.41c0 2.32 1.89 4.22 4.21 4.22h11.58c2.32 0 4.21-1.9 4.21-4.21v-3.1m-10 3.31v-3")) }
    val Search: ImageVector by lazy { brokenIcon("Search", listOf("M11.5 2c5.25 0 9.5 4.25 9.5 9.5S16.75 21 11.5 21 2 16.75 2 11.5c0-3.7 2.11-6.9 5.2-8.47M22 22l-2-2")) }
    val Chart: ImageVector by lazy { brokenIcon("Chart", listOf("M2 13.01V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9", "M17.5 11.57v4.93a2 2 0 1 1-4 0v-9c0-1.1.9-2 2-2s2 .9 2 2m-9 11c1.1 0 2-.9 2-2V13c0-1.1-.9-2-2-2s-2 .9-2 2v3.5a2 2 0 0 0 2 2")) }
    val Settings: ImageVector by lazy { brokenIcon("Settings", listOf("M15 12c0-1.66-1.34-3-3-3s-3 1.34-3 3a2.996 2.996 0 0 0 4.17 2.76", "m6.88 20.58 1.09.63c.79.47 1.81.19 2.28-.6l.11-.19c.9-1.57 2.38-1.57 3.29 0l.11.19c.47.79 1.49 1.07 2.28.6l1.73-.99c.91-.52 1.22-1.69.7-2.59-.91-1.57-.17-2.85 1.64-2.85 1.04 0 1.9-.85 1.9-1.9v-1.76c0-1.04-.85-1.9-1.9-1.9-1.01 0-1.69-.4-1.93-1.03-.19-.49-.11-1.13.29-1.82.52-.91.21-2.07-.7-2.59l-.81-.46m-3.32.26c-.9 1.57-2.38 1.57-3.29 0l-.11-.19a1.655 1.655 0 0 0-2.27-.6l-1.73.99c-.91.52-1.22 1.69-.7 2.6.91 1.56.17 2.84-1.64 2.84-1.04 0-1.9.85-1.9 1.9v1.76c0 1.04.85 1.9 1.9 1.9 1.81 0 2.55 1.28 1.64 2.85")) }
    val MusicNote: ImageVector by lazy { brokenIcon("MusicNote", listOf("M6.28 22a3.12 3.12 0 1 0 0-6.24 3.12 3.12 0 0 0 0 6.24", "M20.84 7.96V4.6c0-2.6-1.63-2.96-3.28-2.51l-6.24 1.7C10.18 4.1 9.4 5 9.4 6.3v12.57m11.44-2.07V12m-3.12 7.92a3.12 3.12 0 1 0 0-6.24 3.12 3.12 0 0 0 0 6.24M9.4 9.52 20.84 6.4")) }
    val Playlist: ImageVector by lazy { brokenIcon("Playlist", listOf("M2 12c0-3.5 2-5 5-5h10c3 0 5 1.5 5 5v5c0 3.5-2 5-5 5H7c-3 0-5-1.5-5-5v-.83M16 4.5h2m-12 0h7.17M9 2h6", "M8.89 19.11a1.37 1.37 0 1 0 0-2.74 1.37 1.37 0 0 0 0 2.74", "M15.28 16.83v-5.35c0-1.14-.71-1.3-1.44-1.1l-2.74.75c-.5.14-.84.53-.84 1.1v5.51", "M13.91 18.2a1.37 1.37 0 1 0 0-2.74 1.37 1.37 0 0 0 0 2.74m-3.65-4.37 5.02-1.37")) }
    val Folder: ImageVector by lazy { brokenIcon("Folder", listOf("M8 2h9q3 0 3 3v1.38", "M2 13.02V7c0-4 1-5 5-5h1.5c1.5 0 1.83.44 2.4 1.2l1.5 2c.38.5.6.8 1.6.8h3c4 0 5 1 5 5m0 3.99V17c0 4-1 5-5 5H7c-4 0-5-1-5-5")) }
    val Disc: ImageVector by lazy { brokenIcon("Disc", listOf("M2.58 8.67a10 10 0 0 1 3.14-4.44M2 12c0 1.17.21 2.29.58 3.33m6.51 6.24c.92.28 1.9.43 2.91.43 5.52 0 10-4.48 10-10 0-.6-.06-1.19-.16-1.76M5.72 19.75c-.47-.38-.92-.8-1.32-1.26M20.24 6.34A9.98 9.98 0 0 0 12 2c-1.01 0-1.99.15-2.91.43m-.5 14.68a1.59 1.59 0 1 0 0-3.18 1.59 1.59 0 0 0 0 3.18", "M16 14.46V8.25c0-1.32-.83-1.51-1.67-1.28l-3.18.87c-.58.16-.98.61-.98 1.28v6.4", "M14.41 16.05a1.59 1.59 0 1 0 0-3.18 1.59 1.59 0 0 0 0 3.18m-4.23-5.29L16 9.17")) }
    val User: ImageVector by lazy { brokenIcon("User", listOf("M15.02 3.01A4.94 4.94 0 0 0 12 2C9.24 2 7 4.24 7 7s2.24 5 5 5 5-2.24 5-5m3.59 15c0-3.87-3.85-7-8.59-7s-8.59 3.13-8.59 7")) }
    val Category: ImageVector by lazy { brokenIcon("Category", listOf("M5 10h2q3 0 3-3V5q0-3-3-3H5Q2 2 2 5v2q0 3 3 3m14-8h-2q-3 0-3 3v2q0 3 3 3h2q3 0 3-3V5m-5 17h2q3 0 3-3v-2q0-3-3-3h-2q-3 0-3 3v2q0 3 3 3M5 22h2q3 0 3-3v-2q0-3-3-3H5q-3 0-3 3v2")) }
    val HeartLinear: ImageVector by lazy { brokenIcon("HeartLinear", listOf("M20.59 4.97c.88.99 1.41 2.29 1.41 3.72 0 7-6.48 11.13-9.38 12.13-.34.12-.9.12-1.24 0C8.48 19.82 2 15.69 2 8.69 2 5.6 4.49 3.1 7.56 3.1c1.82 0 3.43.88 4.44 2.24a5.53 5.53 0 0 1 4.44-2.24")) }
    val PlayLinear: ImageVector by lazy { brokenIcon("PlayLinear", listOf("M17.13 7.98c3.83 2.21 3.83 5.83 0 8.04l-3.09 1.78-3.09 1.78C7.13 21.79 4 19.98 4 15.56V8.44c0-4.42 3.13-6.23 6.96-4.02l2.25 1.3")) }
    val PauseLinear: ImageVector by lazy { brokenIcon("PauseLinear", listOf("M5.01 3C3.57 3 3 3.54 3 4.89v14.22C3 20.46 3.57 21 5.01 21h3.63c1.43 0 2.01-.54 2.01-1.89V4.89c0-1.35-.57-1.89-2.01-1.89m10.35 18c1.43 0 2.01-.54 2.01-1.89V4.89C21 3.54 20.43 3 18.99 3h-3.63c-1.43 0-2.01.54-2.01 1.89v14.22c0 1.35.57 1.89 2.01 1.89")) }
    val PrevLinear: ImageVector by lazy { brokenIcon("PrevLinear", listOf("M12.27 7.39 16.42 5c1.7-.98 3.83.24 3.83 2.21v9.57c0 1.96-2.13 3.19-3.83 2.21l-4.15-2.39-4.15-2.4c-1.7-.98-1.7-3.43 0-4.41m-4.36 8.39V17m0-3V5.82")) }
    val NextLinear: ImageVector by lazy { brokenIcon("NextLinear", listOf("M11.73 7.39 7.58 5c-1.7-.98-3.83.24-3.83 2.21v9.57c0 1.96 2.13 3.19 3.83 2.21l4.15-2.39 4.15-2.4c1.7-.98 1.7-3.43 0-4.41m4.36 8.39V17m0-3V5.82")) }
    val Shuffle: ImageVector by lazy { brokenIcon("Shuffle", listOf("m16.45 6 4.55.02M3 17.98l2.55.01c.91 0 1.76-.45 2.26-1.2l1.18-1.77.76-1.14L13.67 8M19 19.98l2-2M8.89 8.62l-1.08-1.5A2.68 2.68 0 0 0 5.61 6L3 6.01m9.97 9.37 1.22 1.57c.51.66 1.31 1.05 2.15 1.05l4.67-.02M21 6.02l-2-2")) }
    val Repeat: ImageVector by lazy { brokenIcon("Repeat", listOf("M17.42 5.16c1.66 0 3 1.34 3 3v3.32M3.58 5.16h9.41M6.74 2 3.58 5.16l3.16 3.16m13.68 10.52H6.58c-1.66 0-3-1.34-3-3v-3.32", "m17.26 22 3.16-3.16-3.16-3.16")) }
    val RepeatOne: ImageVector by lazy { brokenIcon("RepeatOne", listOf("M8.5 5.32C4.93 5.32 2 8.25 2 11.84c0 1.79.73 3.42 1.91 4.6M14 3l2.44 2.34-3.51-.01M10 21l-2.44-2.34 7.95.02c3.57 0 6.5-2.93 6.5-6.52 0-1.79-.73-3.42-1.91-4.6", "M12.25 14.67V9.33L10.75 11")) }
    val SleepTimer: ImageVector by lazy { brokenIcon("SleepTimer", listOf("M12 8v5", "M9 2h6", "M5 8a8.7 8.7 0 0 0-1.75 5.25C3.25 18.08 7.17 22 12 22s8.75-3.92 8.75-8.75S16.83 4.5 12 4.5c-1.26 0-2.45.26-3.53.74")) }
    val Speedometer: ImageVector by lazy { brokenIcon("Speedometer", listOf("M4.84 19.48A9.97 9.97 0 0 1 2 12.49C2 8.8 4 5.58 6.97 3.84c.31-.18.63-.35.97-.49m11.2 16.15a9.96 9.96 0 0 0 2.86-7c0-5.52-4.48-10-10-10M8.62 18.12c0 1.86 1.51 3.38 3.38 3.38s3.38-1.51 3.38-3.38-1.51-3.38-3.38-3.38a3.38 3.38 0 0 0-3.38 3.38", "M15.25 12H16c.82 0 1.5-.67 1.5-1.5 0-.82-.68-1.5-1.5-1.5s-1.5.67-1.5 1.5v.75c0 .41.34.75.75.75")) }
    val Equalizer: ImageVector by lazy { brokenIcon("Equalizer", listOf("M6.5 22v-7m0-10V2m11 20v-3m0-10V2m-8 9.02V13c0 1.1-.5 2-2 2h-2c-1.5 0-2-.9-2-2V7c0-1.1.5-2 2-2h2c1.5 0 2 .9 2 2m11 4v6c0 1.1-.5 2-2 2h-2c-1.5 0-2-.9-2-2v-6c0-1.1.5-2 2-2h2c1.5 0 2 .9 2 2")) }
    val Queue: ImageVector by lazy { brokenIcon("Queue", listOf("M2 3h3m3 0h14M2 9h9m-9 6h6m-6 6h4", "M11.84 22a2.18 2.18 0 1 0 0-4.36 2.18 2.18 0 0 0 0 4.36", "M22 18.37V9.86c0-1.81-1.14-2.06-2.29-1.75L15.36 9.3c-.79.22-1.34.84-1.34 1.75v8.77", "M19.82 20.55a2.18 2.18 0 1 0 0-4.36 2.18 2.18 0 0 0 0 4.36m.55-8.68 1.63-.45m-7.98 2.18 2.35-.64")) }
    val Lyrics: ImageVector by lazy { brokenIcon("Lyrics", listOf("M2 12.99V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9m15.5 8.08h-1.85m-2.68 0H6.5m11-3.76h-5.53m-2.7 0H6.5")) }
    val Trash: ImageVector by lazy { brokenIcon("Trash", listOf("M21 5.98c-3.33-.33-6.68-.5-10.02-.5q-2.97 0-5.94.3L3 5.98m5.5-1.01.22-1.31C8.88 2.71 9 2 10.69 2h2.62c1.69 0 1.82.75 1.97 1.67l.22 1.3M15.21 22H8.79C6 22 5.91 20.78 5.8 19.21L5.15 9.14m13.7 0-.65 10.07m-7.87-2.71h3.33m-.84-4h1.68m-5 0h.83")) }
    val InfoCircle: ImageVector by lazy { brokenIcon("InfoCircle", listOf("M12 8v5M4 6c-1.25 1.67-2 3.75-2 6 0 5.52 4.48 10 10 10s10-4.48 10-10S17.52 2 12 2c-1.43 0-2.8.3-4.03.85", "M11.995 16h.009")) }
    val BackupUpload: ImageVector by lazy { brokenIcon("BackupUpload", listOf("M9 13.65V11l2 2m-2 4v-1m0-5-2 2M2 9c0-5 2-7 7-7h5", "M22 10v5c0 5-2 7-7 7H9c-5 0-7-2-7-7v-2.02M18 10c-3 0-4-1-4-4V2l8 8")) }
    val BackupDownload: ImageVector by lazy { brokenIcon("BackupDownload", listOf("M9 14.35V17l2-2m-2-4v1m0 5-2-2M2 9c0-5 2-7 7-7h5", "M22 10v5c0 5-2 7-7 7H9c-5 0-7-2-7-7v-2.02M18 10c-3 0-4-1-4-4V2l8 8")) }
    val Sort: ImageVector by lazy { brokenIcon("Sort", listOf("M10 7h11M3 7h3m0 5h12m-8 5h4")) }
    val ArrowDown: ImageVector by lazy { brokenIcon("ArrowDown", listOf("m16.01 12.85-2.62 2.62c-.77.77-2.03.77-2.8 0L4.08 8.95m15.84 0-1.04 1.04")) }
    val ArrowLeft: ImageVector by lazy { brokenIcon("ArrowLeft", listOf("m11.19 7.94-2.62 2.62c-.77.77-.77 2.03 0 2.8l6.52 6.52m0-15.84-1.04 1.04")) }
    val ArrowRight: ImageVector by lazy { brokenIcon("ArrowRight", listOf("m12.9 7.94 2.62 2.62c.77.77.77 2.03 0 2.8L9 19.87M9 4.04l1.04 1.04")) }
    val ArrowUp: ImageVector by lazy { brokenIcon("ArrowUp", listOf("M16.01 11.15 13.4 8.53c-.77-.77-2.03-.77-2.8 0l-6.52 6.52m15.84 0-1.04-1.04")) }
    val MoreVert: ImageVector by lazy { brokenIcon("MoreVert", listOf("M12 7a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z", "M12 14a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z", "M10 19a2 2 0 1 0 4 0 2 2 0 0 0-2-2")) }
    val Refresh: ImageVector by lazy { brokenIcon("Refresh", listOf("M18.01 19.99A9.96 9.96 0 0 1 12 22c-5.52 0-8.89-5.56-8.89-5.56m0 0h4.52m-4.52 0v5M22 12c0 1.82-.49 3.53-1.34 5M6.03 3.97A9.92 9.92 0 0 1 12 2c6.67 0 10 5.56 10 5.56m0 0v-5m0 5h-4.44M2 12c0-1.82.48-3.53 1.33-5")) }
    val Add: ImageVector by lazy { brokenIcon("Add", listOf("M12 18V6m4 6h2M6 12h5.66m.34 6V6")) }
    val AddCircle: ImageVector by lazy { brokenIcon("AddCircle", listOf("M12 16V8m2.99 4H16m-8 0h3.81m.19 4V8M4 6c-1.25 1.67-2 3.75-2 6 0 5.52 4.48 10 10 10s10-4.48 10-10S17.52 2 12 2c-1.43 0-2.8.3-4.03.85")) }
    val Copy: ImageVector by lazy { brokenIcon("Copy", listOf("M2 12.9C2 9.4 3.4 8 6.9 8h4.2c3.5 0 4.9 1.4 4.9 4.9v4.2c0 3.5-1.4 4.9-4.9 4.9H6.9C3.4 22 2 20.6 2 17.1", "M22 11.1c0 3.5-1.4 4.9-4.9 4.9H16v-3.1C16 9.4 14.6 8 11.1 8H8V6.9C8 3.4 9.4 2 12.9 2h4.2C20.6 2 22 3.4 22 6.9")) }
    val Edit: ImageVector by lazy { brokenIcon("Edit", listOf("m17.37 10.17 1.34-1.42c1.42-1.5 2.06-3.21-.15-5.3-2.21-2.08-3.88-1.35-5.3.15l-8.21 8.69c-.31.33-.61.98-.67 1.43l-.37 3.24c-.13 1.17.71 1.97 1.87 1.77l3.22-.55c.45-.08 1.08-.41 1.39-.75l3.95-4.18", "M11.89 5.05a6.126 6.126 0 0 0 5.45 5.15M3 22h11m4 0h3")) }
    val Clock: ImageVector by lazy { brokenIcon("Clock", listOf("m15.71 15.18-3.1-1.85c-.54-.32-.98-1.09-.98-1.72v-4.1", "M4 6c-1.25 1.67-2 3.75-2 6 0 5.52 4.48 10 10 10s10-4.48 10-10S17.52 2 12 2c-1.43 0-2.8.3-4.03.85")) }
    val Close: ImageVector by lazy { brokenIcon("Close", listOf("m13.99 10.01.84-.84m-5.66 5.66 2.75-2.75m2.91 2.75L9.17 9.17M4 6c-1.25 1.67-2 3.75-2 6 0 5.52 4.48 10 10 10s10-4.48 10-10S17.52 2 12 2c-1.43 0-2.8.3-4.03.85")) }
    val Remove: ImageVector by lazy { brokenIcon("Remove", listOf("M16 12h2M6 12h5.66")) }
    val Check: ImageVector by lazy { brokenIcon("Check", listOf("m5 12.5 4.2 4.2 3.9-3.9", "m15.4 10.5 3.6-3.6")) }
    val Share: ImageVector by lazy { brokenIcon("Share", listOf("M16.96 6.17c2 1.39 3.38 3.6 3.66 6.15m-17.13.05a8.6 8.6 0 0 1 3.6-6.15m1.1 14.72c1.16.59 2.48.92 3.87.92 1.34 0 2.6-.3 3.73-.85M9.28 4.92a2.78 2.78 0 1 0 2.78-2.78M4.83 19.92a2.78 2.78 0 1 0 0-5.56 2.78 2.78 0 0 0 0 5.56m17.11-2.78a2.78 2.78 0 1 0-2.78 2.78")) }
    val SelectAll: ImageVector by lazy { brokenIcon("SelectAll", listOf("M2 12.96V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9m13 1.38 1.12-1.13", "m7.88 12 2.74 2.75 2.55-2.54")) }
    val Restore: ImageVector by lazy { brokenIcon("Restore", listOf("M2 16.75C2 20.5 3.5 22 7.25 22h4.5C15.5 22 17 20.5 17 16.75v-4.5C17 8.5 15.5 7 11.75 7h-4.5C3.5 7 2 8.5 2 12.25M22 9c0-3.87-3.13-7-7-7l1.05 1.75")) }
    val Shield: ImageVector by lazy { brokenIcon("Shield", listOf("m9.05 11.87 1.61 1.61 4.3-4.3", "M20.59 7.12c0-1.23-.94-2.59-2.09-3.02l-4.99-1.87c-.83-.31-2.19-.31-3.02 0L5.5 4.11c-1.15.43-2.09 1.79-2.09 3.01v7.43c0 1.18.78 2.73 1.73 3.44l4.3 3.21c1.41 1.06 3.73 1.06 5.14 0l4.3-3.21c.95-.71 1.73-2.26 1.73-3.44v-3.52")) }
    val AudioFile: ImageVector by lazy { brokenIcon("AudioFile", listOf("M2 12.99V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9", "M8.42 17.11a1.57 1.57 0 1 0 0-3.14 1.57 1.57 0 0 0 0 3.14", "M15.75 14.49V8.35c0-1.31-.82-1.49-1.65-1.26l-3.14.86c-.57.16-.96.61-.96 1.26v6.34", "M14.18 16.06a1.57 1.57 0 1 0 0-3.14 1.57 1.57 0 0 0 0 3.14m-4.19-5.02 5.76-1.57")) }
    val PlaylistAdd: ImageVector by lazy { brokenIcon("PlaylistAdd", listOf("M2 8.65C2 3.9 3.9 2 8.65 2h5.7C19.1 2 21 3.9 21 8.65v5.7c0 .34-.01.67-.03.98A3.94 3.94 0 0 0 18 14c-2.21 0-4 1.79-4 4 0 .75.21 1.46.58 2.06.2.34.46.65.76.91q-.465.03-.99.03h-5.7C3.9 21 2 19.1 2 14.35v-1.38", "M6.72 14.42a1.45 1.45 0 1 0 0-2.9 1.45 1.45 0 0 0 0 2.9", "M13.48 12V6.34c0-1.21-.76-1.37-1.52-1.16l-2.89.79c-.52.14-.89.56-.89 1.16v5.84", "M12.03 13.45a1.45 1.45 0 1 0 0-2.9 1.45 1.45 0 0 0 0 2.9M8.17 8.83l5.31-1.45", "M22 18c0 .75-.21 1.46-.58 2.06A3.97 3.97 0 0 1 18 22a3.97 3.97 0 0 1-3.42-1.94A3.9 3.9 0 0 1 14 18c0-2.21 1.79-4 4-4s4 1.79 4 4m-2.51-.02h-2.98M18 16.52v2.99")) }
    val PlayNext: ImageVector by lazy { brokenIcon("PlayNext", listOf("M6.73 14.97c-.08-.4-.12-.85-.12-1.36v-3.23c0-2.69 1.08-3.77 3.77-3.77h3.23c2.69 0 3.77 1.08 3.77 3.77v3.23c0 2.69-1.08 3.77-3.77 3.77h-3.23", "M22 5.77V9c0 2.69-1.08 3.77-3.77 3.77h-.85v-2.38c0-2.69-1.08-3.77-3.77-3.77h-2.38v-.85C11.23 3.08 12.31 2 15 2h3.23C20.92 2 22 3.08 22 5.77m-9.23 11.61v.85C12.77 20.92 11.69 22 9 22H5.77C3.08 22 2 20.92 2 18.23V15c0-2.69 1.08-3.77 3.77-3.77h.85")) }
    val Cleaning: ImageVector by lazy { brokenIcon("Cleaning", listOf("M9.87 5.67 6.45 7.75 4.89 5.19a2.01 2.01 0 0 1 .67-2.75 2.01 2.01 0 0 1 2.75.67zM9.2 20.44c.66 1.35 2.26 1.82 3.54 1.03l6.43-3.91c1.29-.78 1.6-2.41.71-3.62l-2.77-3.74c-1.2-1.61-3.46-2.16-5.29-1.04l-3.16 1.92a3.98 3.98 0 0 0-1.51 5.18", "m10.756 5.098-5.124 3.12 2.08 3.417 5.125-3.12zM14.31 16.81l1.65 2.71m-4.21-1.15 1.65 2.71m3.47-5.83 1.65 2.71")) }
    val BroomBold: ImageVector by lazy { filledIcon("BroomBold", listOf("M8.82 4.9 7.54 2.78C7.06 2 6.11 1.56 5.22 1.82c-1.34.4-1.86 1.91-1.19 3.02l1.32 2.17c.15.23.46.31.69.17l2.61-1.59c.24-.15.32-.46.17-.69m10.92 9.31-2.81-3.8c-.98-1.32-2.68-1.94-4.28-1.51 0-.01-.01-.01-.01-.02l-1.66-2.73c-.3-.47-.92-.62-1.39-.33L6.12 7.93c-.48.28-.63.91-.34 1.39l1.65 2.73s0 .01.01.01a4.05 4.05 0 0 0-.61 4.51l2.08 4.24c.64 1.31 2.15 1.81 3.43 1.15.1-.05.13-.17.07-.27l-1.56-2.58a.765.765 0 0 1 .25-1.05c.36-.21.82-.1 1.05.25l1.57 2.58c.06.09.18.12.27.07l.96-.58c.09-.06.12-.18.07-.27l-1.57-2.58c-.21-.36-.1-.82.25-1.05.37-.21.83-.1 1.05.25l1.57 2.58c.06.09.18.12.27.07l.96-.58c.09-.06.12-.18.07-.27l-1.57-2.58c-.21-.36-.1-.82.25-1.05.37-.21.83-.1 1.05.25l1.58 2.57c.06.09.18.12.27.06 1.16-.83 1.42-2.39.54-3.57")) }
    val Palette: ImageVector by lazy { brokenIcon("Palette", listOf("M2 4.5C2 3 3 2 4.5 2h3C9 2 10 3 10 4.5V18c0 1.08-.44 2.07-1.14 2.79l-.04.04c-.09.09-.19.18-.28.25-.3.26-.64.46-.99.6-.11.05-.22.09-.33.13-.39.13-.81.19-1.22.19-.27 0-.54-.03-.8-.08-.13-.03-.26-.06-.39-.1-.16-.05-.31-.1-.46-.17 0-.01 0-.01-.01 0-.28-.14-.55-.3-.8-.49l-.01-.01c-.13-.1-.25-.2-.36-.32s-.22-.24-.33-.37c-.19-.25-.35-.52-.49-.8.01-.01.01-.01 0-.01 0 0 0-.01-.01-.02-.06-.14-.11-.29-.16-.44a6 6 0 0 1-.1-.39c-.05-.26-.08-.53-.08-.8V8.57", "M22 16.5v3c0 1.5-1 2.5-2.5 2.5H6c.41 0 .83-.06 1.22-.19.11-.04.22-.08.33-.13.35-.14.69-.34.99-.6.09-.07.19-.16.28-.25l.04-.04 6.8-6.79h3.84c1.5 0 2.5 1 2.5 2.5M4.81 21.82c-.6-.18-1.17-.51-1.64-.99-.48-.47-.81-1.04-.99-1.64a4.02 4.02 0 0 0 2.63 2.63", "M18.37 11.29 15.66 14l-6.8 6.79C9.56 20.07 10 19.08 10 18V8.34l2.71-2.71c1.06-1.06 2.48-1.06 3.54 0l2.12 2.12c1.06 1.06 1.06 2.48 0 3.54M6 19a1 1 0 1 0 0-2 1 1 0 0 0 0 2")) }
    val Replay: ImageVector by lazy { brokenIcon("Replay", listOf("M10.02 4.47 12 2M3.99 16.96A8.88 8.88 0 0 0 12 22a8.89 8.89 0 0 0 8.89-8.89A8.89 8.89 0 0 0 12 4.22c-.68 0-1.34.09-1.98.24M4.91 7.8c-1.11 1.48-1.8 3.31-1.8 5.31", "M9.54 15.92v-5.34l-1.5 1.67M14 10.58c1.1 0 2 .9 2 2v1.35c0 1.1-.9 2-2 2s-2-.9-2-2v-1.35a2 2 0 0 1 2-2")) }
    val Forward: ImageVector by lazy { brokenIcon("Forward", listOf("M13.98 4.47 12 2m8.01 14.96A8.88 8.88 0 0 1 12 22a8.89 8.89 0 0 1-8.89-8.89A8.89 8.89 0 0 1 12 4.22c.68 0 1.34.09 1.98.24m5.11 3.34c1.11 1.48 1.8 3.31 1.8 5.31", "M9.54 15.92v-5.34l-1.5 1.67M14 10.58c1.1 0 2 .9 2 2v1.35c0 1.1-.9 2-2 2s-2-.9-2-2v-1.35a2 2 0 0 1 2-2")) }
    val FolderExclude: ImageVector by lazy { brokenIcon("FolderExclude", listOf("M14.43 14.55h-5M2 13.02V7c0-4 1-5 5-5h1.5c1.5 0 1.83.44 2.4 1.2l1.5 2c.38.5.6.8 1.6.8h3c4 0 5 1 5 5m0 3.99V17c0 4-1 5-5 5H7c-4 0-5-1-5-5")) }
    val FolderAdd: ImageVector by lazy { brokenIcon("FolderAdd", listOf("M12.06 16.5v-5M14.5 14h-5M2 13.02V7c0-4 1-5 5-5h1.5c1.5 0 1.83.44 2.4 1.2l1.5 2c.38.5.6.8 1.6.8h3c4 0 5 1 5 5m0 3.99V17c0 4-1 5-5 5H7c-4 0-5-1-5-5")) }
    val Sun: ImageVector by lazy { brokenIcon("Sun", listOf("M7 7.85a6.5 6.5 0 1 0 5-2.35", "m19.14 19.14-.13-.13m0-14.02.13-.13zM4.86 19.14l.13-.13zM12 2.08V2zM12 22v-.08zM2.08 12H2zM22 12h-.08zM4.99 4.99l-.13-.13z")) }
    val Moon: ImageVector by lazy { brokenIcon("Moon", listOf("M4.18 5.38a10.15 10.15 0 0 0-2.15 7.04c.36 5.15 4.73 9.34 9.96 9.57 3.69.16 6.99-1.56 8.97-4.27.82-1.11.38-1.85-.99-1.6-.67.12-1.36.17-2.08.14C13 16.06 9 11.97 8.98 7.14c-.01-1.3.26-2.53.75-3.65.54-1.24-.11-1.83-1.36-1.3")) }
    val SystemTheme: ImageVector by lazy { brokenIcon("SystemTheme", listOf("M2 6.74c0-3.37.84-4.21 4.21-4.21h10.53c3.37 0 4.21.84 4.21 4.21M10 16.95H6.21c-3.37 0-4.21-.84-4.21-4.21v-1.89m8 10.62v-4.52m-8-4h8m-3.26 8.52H10m12-8.67c0-2.37-.59-2.96-2.96-2.96h-3.55c-2.37 0-2.96.59-2.96 2.96v5.71c0 2.37.59 2.96 2.96 2.96h3.55c2.37 0 2.96-.59 2.96-2.96v-1.57", "M17.245 18.25h.008")) }
    val DragHandle: ImageVector by lazy { brokenIcon("DragHandle", listOf("M17.54 8.31a2.46 2.46 0 1 0 0-4.92 2.46 2.46 0 0 0 0 4.92M8.92 5.85c0-1.36-1.1-2.46-2.46-2.46S4 4.49 4 5.85s1.1 2.46 2.46 2.46m11.08 12.311c1.36 0 2.46-1.1 2.46-2.46s-1.1-2.46-2.46-2.46-2.46 1.1-2.46 2.46m-8.62 2.45a2.46 2.46 0 1 0 0-4.92 2.46 2.46 0 0 0 0 4.92")) }
    val TagEdit: ImageVector by lazy { brokenIcon("TagEdit", listOf("M19.83 15.45a4.78 4.78 0 0 0 0-6.75L15.3 4.17a4.75 4.75 0 0 0-3.6-1.39l-5 .24c-2 .09-3.59 1.68-3.69 3.67l-.24 5c-.06 1.34.44 2.65 1.39 3.6l4.53 4.53a4.78 4.78 0 0 0 6.75 0l1.52-1.52", "M7 9.5a2.5 2.5 0 0 0 5 0A2.5 2.5 0 0 0 9.5 7")) }
    val LocateCurrent: ImageVector by lazy { brokenIcon("LocateCurrent", listOf("M8.5 5.365A7.5 7.5 0 1 1 5.365 8.5", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6m0-11V2M4 12H2m10 8v2m8-10h2")) }
    val Star: ImageVector by lazy { brokenIcon("Star", listOf("M20.1 8.61c2.04.34 2.52 1.82 1.05 3.28l-2.48 2.48c-.42.42-.65 1.23-.52 1.81l.71 3.07c.56 2.43-.73 3.37-2.88 2.1l-2.99-1.77c-.54-.32-1.43-.32-1.98 0l-2.99 1.77c-2.14 1.27-3.44.32-2.88-2.1l.71-3.07c.13-.58-.1-1.39-.52-1.81l-2.48-2.48c-1.46-1.46-.99-2.94 1.05-3.28l3.19-.53c.53-.09 1.17-.56 1.41-1.05l1.76-3.52c.95-1.91 2.51-1.91 3.47 0l1.76 3.52c.1.21.28.42.49.59")) }
    val Code: ImageVector by lazy { brokenIcon("Code", listOf("m8 10-2 2 2 2m8-4 2 2-2 2m-3-4.33-2 4.66", "M4 6c-1.25 1.67-2 3.75-2 6 0 5.52 4.48 10 10 10s10-4.48 10-10S17.52 2 12 2c-1.43 0-2.8.3-4.03.85")) }
    val Guide: ImageVector by lazy { brokenIcon("Guide", listOf("M22 11.56v1.87c0 3-2 5-5 5h-4l-4.45 2.96A.997.997 0 0 1 7 20.56v-2.13c-3 0-5-2-5-5v-6c0-3 2-5 5-5h10c3 0 5 2 5 5", "M12 11.36v-.21c0-.68.42-1.04.84-1.33.41-.28.82-.64.82-1.3 0-.92-.74-1.66-1.66-1.66s-1.66.74-1.66 1.66m1.656 5.23h.008")) }
    val Changelog: ImageVector by lazy { brokenIcon("Changelog", listOf("M6.88 18.15v-2.07M12 18.15v-4.14m5.12 4.14v-6.22m0-6.08-.46.54a18.9 18.9 0 0 1-9.78 6.04", "M14.19 5.85h2.93v2.92", "M2 12.97V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9")) }
    val DocumentText: ImageVector by lazy { brokenIcon("DocumentText", listOf("M3 7c0-3 1.5-5 5-5h8c3.5 0 5 2 5 5v10c0 3-1.5 5-5 5H8c-3.5 0-5-2-5-5v-5.98", "M14.5 4.5v2c0 1.1.9 2 2 2h2M8 13h4m-4 4h8")) }
    val Cpu: ImageVector by lazy { brokenIcon("Cpu", listOf("M9.6 20h4.8c4 0 5.6-1.6 5.6-5.6V9.6c0-4-1.6-5.6-5.6-5.6H9.6C5.6 4 4 5.6 4 9.6v4.8c0 4 1.6 5.6 5.6 5.6", "M7 10.5v3C7 16 8 17 10.5 17h3c2.5 0 3.5-1 3.5-3.5v-3C17 8 16 7 13.5 7h-3M8.01 4V2M12 4V2m4 2V2m4 6h2m-2 4h2m-2 4h2m-6 4v2m-3.99-2v2m-4-2v2M2 8h2m-2 4h2m-2 4h2")) }
    val VideoLinear: ImageVector by lazy { brokenIcon("VideoLinear", listOf("M22 15V9c0-5-2-7-7-7H9C4 2 2 4 2 9v6c0 5 2 7 7 7h6c5 0 7-2 7-7M2.52 7.11h18.96m-12.96-5v4.86m6.96-4.86v4.41", "M9.75 14.45v-1.2c0-1.54 1.09-2.17 2.42-1.4l1.04.6 1.04.6c1.33.77 1.33 2.03 0 2.8l-1.04.6-1.04.6c-1.33.77-2.42.14-2.42-1.4z")) }
    val Lock: ImageVector by lazy { brokenIcon("Lock", listOf("M6 10V8c0-3.31 1-6 6-6s6 2.69 6 6v2m4 7v-2c0-4-1-5-5-5H7c-4 0-5 1-5 5v2c0 4 1 5 5 5h10c1.76 0 2.94-.19 3.71-.75", "M15.997 16h.008m-4.009 0h.008m-4.009 0h.008")) }
    val Unlock: ImageVector by lazy { brokenIcon("Unlock", listOf("M9.5 16a2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0-2.5-2.5", "M22 17v-2c0-4-1-5-5-5H7c-4 0-5 1-5 5v2c0 4 1 5 5 5h10c1.76 0 2.94-.19 3.71-.75M6 10V8c0-3.31 1-6 6-6 4.5 0 6 2 6 5")) }
    val Fullscreen: ImageVector by lazy { brokenIcon("Fullscreen", listOf("M21 5V3h-6m6 6V7.97M3 19v2h6m-6-6v1.03M21 3l-7.5 7.5m-3 3L3 21")) }
    val AspectRatio: ImageVector by lazy { brokenIcon("AspectRatio", listOf("M22 11V9c0-5-2-7-7-7H9C4 2 2 4 2 9v6c0 5 2 7 7 7h6c5 0 7-2 7-7M9 2l4.95 20m-2.42-9.78L2 15")) }
    val GridView: ImageVector by lazy { brokenIcon("GridView", listOf("M3.96 3.32C2.6 4.43 2 6.27 2 9v6c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9m3 0v20M2 12h20")) }
    val ListView: ImageVector by lazy { brokenIcon("ListView", listOf("M2 19.77C2 21.36 2.6 22 4.1 22h15.8c1.5 0 2.1-.64 2.1-2.23v-4.04c0-1.59-.6-2.23-2.1-2.23H4.1c-1.5 0-2.1.64-2.1 2.23m20-11.5C22 2.64 21.4 2 19.9 2H4.1C2.6 2 2 2.64 2 4.23v4.04c0 1.59.6 2.23 2.1 2.23h15.8c1.5 0 2.1-.64 2.1-2.23")) }
    val VolumeHigh: ImageVector by lazy { brokenIcon("VolumeHigh", listOf("M15 7.41c0-2.98-2.07-4.12-4.59-2.54L7.49 6.7c-.32.19-.69.3-1.06.3H5q-3 0-3 3v4q0 3 3 3h1.43c.37 0 .74.11 1.06.3l2.92 1.83c2.52 1.58 4.59.43 4.59-2.54v-5.12M18 8a6.66 6.66 0 0 1 0 8m1.83 2.5C21.28 16.57 22 14.29 22 12m-2.17-6.5c.59.78 1.05 1.62 1.4 2.5")) }
    val VolumeMute: ImageVector by lazy { brokenIcon("VolumeMute", listOf("M2 14q0 3 3 3h1.43c.37 0 .74.11 1.06.3l2.92 1.83c2.52 1.58 4.59.43 4.59-2.54V7.41c0-2.98-2.07-4.12-4.59-2.54L7.49 6.7c-.32.19-.69.3-1.06.3H5q-3 0-3 3", "M22 13.96 18.04 10m1.85 2.11L18 14m3.96-3.96-.54.54")) }
    val RotateScreen: ImageVector by lazy { brokenIcon("RotateScreen", listOf("m6.7 9.26 5.3 3.07 5.26-3.05M12 17.77v-5.45", "M6.23 10.3c0-.83.59-1.84 1.32-2.24l3.2-1.78c.68-.38 1.8-.38 2.49 0l3.2 1.78c.72.4 1.32 1.41 1.32 2.24v3.39c0 .83-.59 1.84-1.32 2.24l-3.2 1.78c-.68.38-1.8.38-2.49 0l-3.2-1.78c-.36-.2-.69-.55-.93-.96M22 15c0 3.87-3.13 7-7 7l1.05-1.75M2 9c0-3.87 3.13-7 7-7L7.95 3.75")) }
    val StyleTune: ImageVector by lazy { brokenIcon("StyleTune", listOf("M22 6.5h-6m-10 0H2m11.5 0c0 1.93-1.57 3.5-3.5 3.5S6.5 8.43 6.5 6.5a3.504 3.504 0 0 1 4.48-3.36M22 17.5h-4m-10 0H2M14 21a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7")) }
    val Eye: ImageVector by lazy { brokenIcon("Eye", listOf("M9.03 14c-.39-.57-.61-1.26-.61-2 0-1.98 1.6-3.58 3.58-3.58s3.58 1.6 3.58 3.58-1.6 3.58-3.58 3.58", "M17.56 5.58c-1.69-1.2-3.59-1.85-5.56-1.85-3.53 0-6.82 2.08-9.11 5.68-.9 1.41-.9 3.78 0 5.19 2.29 3.6 5.58 5.68 9.11 5.68s6.82-2.08 9.11-5.68c.9-1.41.9-3.78 0-5.19")) }
    val EyeSlash: ImageVector by lazy { brokenIcon("EyeSlash", listOf("m14.53 9.47-5.06 5.06a3.576 3.576 0 1 1 5.06-5.06M5.6 17.76c-1-.86-1.91-1.92-2.71-3.17-.9-1.41-.9-3.78 0-5.19 1.18-1.85 2.62-3.3 4.23-4.27m10.7.64C16.07 4.45 14.07 3.73 12 3.73m-3.58 15.8c1.14.48 2.35.74 3.58.74 3.53 0 6.82-2.08 9.11-5.68.9-1.41.9-3.78 0-5.19-.33-.52-.69-1.01-1.06-1.47", "M15.51 12.7a3.565 3.565 0 0 1-2.82 2.82m-3.22-.99L2 22M22 2l-7.47 7.47")) }
    val Filter: ImageVector by lazy { brokenIcon("Filter", listOf("M18.6 2.1c1.1 0 2 .9 2 2v2.2c0 .8-.5 1.8-1 2.3l-4.3 3.8c-.6.5-1 1.5-1 2.3V19c0 .6-.4 1.4-.9 1.7l-1.4.9c-1.3.8-3.1-.1-3.1-1.7v-5.3c0-.7-.4-1.6-.8-2.1l-3.8-4c-.5-.5-.9-1.4-.9-2V4.2c0-1.2.9-2.1 2-2.1h9m-3.47 0L6 10")) }
    val SoundControl: ImageVector by lazy { brokenIcon("SoundControl", listOf("M19 18v-7m0 11v-1M5 18v-7m0 11v-1M19 7V2m-7 20v-5m0-4V2M5 7V2m-2 9h4m10 0h4m-11 2h4")) }
    val Waveform: ImageVector by lazy { brokenIcon("Waveform", listOf("M3 8.25v7.5m4.5-10v12.5M12 9.96v10.79m0-17.5v2.72m4.5-.22v12.5m4.5-10v7.5")) }
    val Link: ImageVector by lazy { brokenIcon("Link", listOf("M13.5 12c0 3.18-2.57 5.75-5.75 5.75S2 15.18 2 12s2.57-5.75 5.75-5.75M22 12c0 3.31-2.69 6-6 6", "M10 12c0-3.31 2.69-6 6-6 1.53 0 2.93.57 3.98 1.51")) }
    val Telegram: ImageVector by lazy { brokenIcon("Telegram", listOf("M15.89 3.49c3.81-1.27 5.88.81 4.62 4.62l-2.83 8.49c-1.9 5.71-5.02 5.71-6.92 0l-.84-2.52-2.52-.84c-5.71-1.9-5.71-5.01 0-6.92L12 4.79m-1.89 8.86 3.58-3.59")) }
    val Security: ImageVector by lazy { brokenIcon("Security", listOf("M12 8.5c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2m-2 2v3", "M20.59 7.12c0-1.23-.94-2.59-2.09-3.02l-4.99-1.87c-.83-.31-2.19-.31-3.02 0L5.5 4.11c-1.15.43-2.09 1.79-2.09 3.01v7.43c0 1.18.78 2.73 1.73 3.44l4.3 3.21c1.41 1.06 3.73 1.06 5.14 0l4.3-3.21c.95-.71 1.73-2.26 1.73-3.44v-3.52")) }
    val BoxAdd: ImageVector by lazy { brokenIcon("BoxAdd", listOf("M23 18c0 .75-.21 1.46-.58 2.06-.21.36-.48.68-.79.94-.7.63-1.62 1-2.63 1-1.22 0-2.31-.55-3.03-1.41-.02-.03-.05-.05-.07-.08-.12-.14-.23-.29-.32-.45A3.9 3.9 0 0 1 15 18c0-1.26.58-2.39 1.5-3.12.69-.55 1.56-.88 2.5-.88 1 0 1.9.36 2.6.97.12.09.23.2.33.31C22.59 16 23 16.95 23 18m-2.51-.02h-2.98M19 16.52v2.99", "M3.17 7.44 12 12.55l8.77-5.08M12 21.61v-9.07", "M2.39 14.83V9.17c0-1.38.99-3.06 2.2-3.73l5.34-2.96c1.14-.64 3-.64 4.14 0l5.34 2.96c1.21.67 2.2 2.35 2.2 3.73v5.66c0 .05 0 .09-.01.14-.7-.61-1.6-.97-2.6-.97-.94 0-1.81.33-2.5.88A3.97 3.97 0 0 0 15 18c0 .75.21 1.46.58 2.06.09.16.2.31.32.45l-1.83 1.01c-1.14.64-3 .64-4.14 0l-5.34-2.96")) }
    val Save: ImageVector by lazy { brokenIcon("Save", listOf("M22 5.11v11.36c0 1.45-1.04 2.06-2.31 1.36L16 15.77V8.99c0-1.71-1.4-3.11-3.11-3.11H8v-.77C8 3.4 9.4 2 11.11 2h7.78C20.6 2 22 3.4 22 5.11", "M2 20.35c0 1.45 1.04 2.06 2.31 1.36l3.93-2.19c.42-.23 1.1-.23 1.52 0l3.93 2.19c1.27.7 2.31.09 2.31-1.36V8.99c0-1.71-1.4-3.11-3.11-3.11H5.11A3.12 3.12 0 0 0 2 8.99v6.52")) }
    val Hierarchy: ImageVector by lazy { brokenIcon("Hierarchy", listOf("M5 15V8m.25 14a3.25 3.25 0 1 0 0-6.5 3.25 3.25 0 0 0 0 6.5M5 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6m14 0a3 3 0 1 0 0-6 3 3 0 0 0 0 6M5.13 15a4.06 4.06 0 0 1 3.94-3.04l3.43.01c.85 0 1.67-.17 2.4-.49")) }
    val ForwardSkip: ImageVector by lazy { brokenIcon("ForwardSkip", listOf("M2 8.34v7.32c0 1.5 1.63 2.44 2.93 1.69l3.17-1.82 3.17-1.83c.2-.12.36-.25.49-.41v-2.56c-.13-.16-.29-.29-.49-.41L8.1 8.49 4.93 6.67C3.63 5.9 2 6.84 2 8.34", "M14.68 6.65c-1.3-.75-2.93.19-2.93 1.69v7.32c0 1.5 1.63 2.44 2.93 1.69l3.17-1.82 3.17-1.83c1.3-.75 1.3-2.62 0-3.38l-2.18-1.26")) }
    val Zoom: ImageVector by lazy { brokenIcon("Zoom", listOf("M2 13.03V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9m10 3-6 6M18 6l-2.99 2.99", "M18 10V6h-4m-8 8v4h4")) }
    val SliderHorizontal: ImageVector by lazy { brokenIcon("SliderHorizontal", listOf("M2 19.5v-15M22 19V4M5 12.45V7.5c0-.62.02-1.17.09-1.66C5.38 3.21 6.62 2.5 10 2.5h4c3.38 0 4.62.71 4.91 3.34.07.49.09 1.04.09 1.66v9c0 .62-.02 1.17-.09 1.66-.29 2.63-1.53 3.34-4.91 3.34h-4c-3.38 0-4.62-.71-4.91-3.34-.07-.49-.09-1.04-.09-1.66")) }
    val MagicPen: ImageVector by lazy { brokenIcon("MagicPen", listOf("M16.98 10.02 19.5 7.5c.83-.83.83-2.17 0-3s-2.17-.83-3 0l-13 13c-.83.83-.83 2.17 0 3s2.17.83 3 0l8-8m3.51-3.51-3-3", "M9.56 3.5 10 5l-1.5-.44L7 5l.44-1.5L7 2l1.5.44m-4 6L6 8l-.44 1.5L6 11l-1.5-.44L3 11l.44-1.5L3 8zm13.94 6.06L18 13l1.5.44L21 13l-.44 1.5L21 16l-1.5-.44")) }
    val Brush: ImageVector by lazy { brokenIcon("Brush", listOf("M18.02 3.01c.67-.36 1.33-.67 1.94-.91.59-.22 1.16-.06 1.52.31.38.38.56.95.32 1.54-1.54 3.84-5.4 9.06-8.63 11.65l-1.97 1.58c-.25.18-.5.34-.78.45 0-.18-.01-.38-.04-.57-.11-.84-.49-1.62-1.16-2.29-.68-.68-1.51-1.08-2.36-1.19-.2-.01-.4-.03-.6-.01.11-.31.28-.6.49-.84l1.56-1.97c1.35-1.69 3.44-3.56 5.66-5.18", "M10.43 17.62c0 1.1-.42 2.15-1.21 2.95-.61.61-1.44 1.03-2.43 1.16L4.33 22c-1.34.15-2.49-.99-2.33-2.35l.27-2.46c.24-2.19 2.07-3.59 4.01-3.63.2-.01.41 0 .6.01.85.11 1.68.5 2.36 1.19.67.67 1.05 1.45 1.16 2.29.01.19.03.38.03.57m3.81-3.15c0-2.61-2.12-4.73-4.73-4.73")) }
    val NoteFavorite: ImageVector by lazy { brokenIcon("NoteFavorite", listOf("M8 2v3m8-3v3M3 8.5c0-3 1.5-5 5-5h8c3.5 0 5 2 5 5v5.13a5.2 5.2 0 0 0-3.25-1.13c-1.23 0-2.38.43-3.28 1.16a5.19 5.19 0 0 0-1.97 4.09c0 .98.28 1.92.76 2.7.37.61.85 1.14 1.42 1.55H8c-3.5 0-5-2-5-5v-4.43M7 11h6m-6 5h2.62", "M23 17.75c0 .98-.28 1.92-.76 2.7-.28.48-.63.9-1.04 1.24-.92.82-2.12 1.31-3.45 1.31-1.15 0-2.21-.37-3.07-1-.57-.41-1.05-.94-1.42-1.55-.48-.78-.76-1.72-.76-2.7 0-1.65.76-3.14 1.97-4.09.9-.73 2.05-1.16 3.28-1.16s2.36.42 3.25 1.13c1.22.96 2 2.45 2 4.12", "M17.75 20.25a2.5 2.5 0 0 1 2.5-2.5 2.5 2.5 0 0 1-2.5-2.5 2.5 2.5 0 0 1-2.5 2.5 2.5 2.5 0 0 1 2.5 2.5")) }
    val AudioSquare: ImageVector by lazy { brokenIcon("AudioSquare", listOf("M2 12.99V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9", "M9.62 17.3a2.12 2.12 0 1 0 0-4.24 2.12 2.12 0 0 0 0 4.24m2.12-2.12V7.77m1.39-1 2.34.78c.57.19 1.03.83 1.03 1.43v.62c0 .81-.63 1.26-1.39 1l-2.34-.78c-.57-.19-1.03-.83-1.03-1.43v-.62c0-.8.62-1.26 1.39-1")) }
    val TickCircle: ImageVector by lazy { brokenIcon("TickCircle", listOf("M4 6c-1.25 1.67-2 3.75-2 6 0 5.52 4.48 10 10 10s10-4.48 10-10S17.52 2 12 2c-1.43 0-2.8.3-4.03.85M15 10.38l1.12-1.13", "m7.88 12 2.74 2.75 2.55-2.54")) }
    val TickSquare: ImageVector by lazy { brokenIcon("TickSquare", listOf("M2 12.96V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9m13 1.38 1.12-1.13", "m7.88 12 2.74 2.75 2.55-2.54")) }
    val Cup: ImageVector by lazy { brokenIcon("Cup", listOf("M12.15 16.5v2.1", "M7.15 22h10v-1c0-1.1-.9-2-2-2h-6c-1.1 0-2 .9-2 2z", "M6.15 22h12m-9.64-6.93A6.99 6.99 0 0 1 5 9V6c0-2.21 1.79-4 4-4h6c2.21 0 4 1.79 4 4v3c0 3.82-3.05 6.92-6.85 7H12", "M5.47 11.65c-.75-.24-1.41-.68-1.93-1.2-.9-1-1.5-2.2-1.5-3.6s1.1-2.5 2.5-2.5h.65c-.2.46-.3.97-.3 1.5v3c0 1 .21 1.94.58 2.8m13.06 0c.75-.24 1.41-.68 1.93-1.2.9-1 1.5-2.2 1.5-3.6s-1.1-2.5-2.5-2.5h-.65c.2.46.3.97.3 1.5v3c0 1-.21 1.94-.58 2.8")) }
    val Award: ImageVector by lazy { brokenIcon("Award", listOf("M18.02 18.96c1.72-1.16 1.72-1.16 1.72-2.98v-4.97c0-1.82 0-1.82-1.72-2.98L13.29 5.3c-.71-.41-1.87-.41-2.58 0L5.98 8.04C4.26 9.2 4.26 9.2 4.26 11.02v4.97c0 1.82 0 1.82 1.72 2.98l4.73 2.73c.71.41 1.87.41 2.58 0l1.52-.88", "M17.5 7.63V5q0-3-3-3h-5q-3 0-3 3v2.56m5.78 8.86a.86.86 0 0 0-.55 0l-.98.39c-.6.24-1.06-.1-1.02-.74l.06-1.05c.01-.16-.07-.4-.17-.52l-.67-.81c-.41-.5-.24-1.04.39-1.2l1.02-.26c.16-.04.36-.19.44-.32l.57-.89c.35-.55.92-.55 1.27 0l.57.89c.09.14.29.28.44.32l1.02.26c.63.16.8.7.39 1.2l-.67.81c-.1.13-.18.36-.17.52")) }
    val Calendar: ImageVector by lazy { brokenIcon("Calendar", listOf("M8 2v3m8-3v3M3.5 9.09h17M3 13.01V8.5c0-3 1.5-5 5-5h8c3.5 0 5 2 5 5V17c0 3-1.5 5-5 5H8c-3.5 0-5-2-5-5", "M15.695 13.7h.009m-.009 3h.009m-3.708-3h.008m-.008 3h.008m-3.71-3h.01m-.01 3h.01")) }
    val StatusUp: ImageVector by lazy { brokenIcon("StatusUp", listOf("M6.88 18.15v-2.07M12 18.15v-4.14m5.12 4.14v-6.22m0-6.08-.46.54a18.9 18.9 0 0 1-9.78 6.04", "M14.19 5.85h2.93v2.92", "M2 12.97V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9")) }
    val PresentionChart: ImageVector by lazy { brokenIcon("PresentionChart", listOf("M21 10V2H3v12.1c0 .71.14 1.3.42 1.76.46.76 1.3 1.15 2.49 1.15H18.1c1.9 0 2.9-1 2.9-2.9M2 2h20", "m8 22 4-2v-3m4 5-4-2m-4.5-9 3.15-2.63c.25-.21.58-.15.75.13l1.2 2c.17.28.5.33.75.13L16.5 8")) }
    val TrendUp: ImageVector by lazy { brokenIcon("TrendUp", listOf("M2 12.98V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9", "m11.17 12-.47-.7-3.2 3.2m9-5-1.51 1.51L14 12m.5-2.5h2v2")) }
    val Radar: ImageVector by lazy { brokenIcon("Radar", listOf("M12 12 3.33 7.02C2.49 8.49 2 10.19 2 12c0 5.52 4.48 10 10 10s10-4.48 10-10c0-2.049-.62-3.96-1.68-5.54M6 4a9.99 9.99 0 0 1 11.13-.58", "M6.83 8.96A5.9 5.9 0 0 0 6 12c0 3.31 2.69 6 6 6s6-2.69 6-6-2.69-6-6-6c-.91 0-1.78.2-2.55.57")) }
    val FavoriteChart: ImageVector by lazy { brokenIcon("FavoriteChart", listOf("M22 13V9c0-5-2-7-7-7H9C4 2 2 4 2 9v6c0 5 2 7 7 7h4", "m7.33 14.49 2.38-3.09c.34-.44.97-.52 1.41-.18l1.83 1.44c.44.34 1.07.26 1.41-.17l2.31-2.98m3.88 7.47c-.3-.05-.66-.32-.8-.6l-.28-.57c-.54-1.09-1.42-1.09-1.95 0l-.28.57c-.14.28-.49.54-.8.6l-.38.06c-1.14.19-1.41 1.03-.59 1.86l.35.35c.23.24.36.7.29 1.02l-.05.21c-.31 1.38.42 1.91 1.62 1.19l.26-.15c.31-.18.81-.18 1.12 0l.26.15c1.21.73 1.94.19 1.62-1.19l-.05-.21")) }
    val MusicLibrary: ImageVector by lazy { brokenIcon("MusicLibrary", listOf("M2 13c0-2.65 1.15-4.15 3-4.72C5.6 8.09 6.27 8 7 8h10c.73 0 1.4.09 2 .28 1.85.57 3 2.07 3 4.72v4c0 3.5-2 5-5 5H7c-3 0-5-1.5-5-5", "M19 7v1.28c-.6-.19-1.27-.28-2-.28H7c-.73 0-1.4.09-2 .28V7c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2m-3-3.49V5H8V3.51C8 2.68 8.68 2 9.51 2h4.98c.83 0 1.51.68 1.51 1.51", "M9.07 19.45a1.32 1.32 0 1 0 0-2.64 1.32 1.32 0 0 0 0 2.64", "M15.25 17.25v-5.17c0-1.1-.69-1.26-1.39-1.06l-2.65.72c-.48.13-.81.51-.81 1.06v5.33", "M13.93 18.57a1.32 1.32 0 1 0 0-2.64 1.32 1.32 0 0 0 0 2.64m-3.53-4.22 4.85-1.32")) }
    val VideoPlay: ImageVector by lazy { brokenIcon("VideoPlay", listOf("M22 15V9c0-5-2-7-7-7H9C4 2 2 4 2 9v6c0 5 2 7 7 7h6c5 0 7-2 7-7M2.52 7.11h18.96m-12.96-5v4.86m6.96-4.86v4.41", "M9.75 14.45v-1.2c0-1.54 1.09-2.17 2.42-1.4l1.04.6 1.04.6c1.33.77 1.33 2.03 0 2.8l-1.04.6-1.04.6c-1.33.77-2.42.14-2.42-1.4z")) }
    val Headphone: ImageVector by lazy { brokenIcon("Headphone", listOf("M2 12.22C1.89 6.6 6.33 2.05 11.95 2.05S22 6.6 22 12.11v6.16c0 1.95-1.62 3.57-3.57 3.57s-3.57-1.62-3.57-3.57v-2.81c0-.97.76-1.84 1.84-1.84.97 0 1.84.76 1.84 1.84v3.03", "M5.46 18.49v-2.92c0-.97.76-1.84 1.84-1.84.97 0 1.84.76 1.84 1.84v2.81c0 1.95-1.62 3.57-3.57 3.57S2 20.32 2 18.38V16")) }
    val MusicPlay: ImageVector by lazy { brokenIcon("MusicPlay", listOf("M2.02 12.22C1.91 6.6 6.35 2.05 11.97 2.05S22.02 6.6 22.02 12.11v6.16c0 1.95-1.62 3.57-3.57 3.57s-3.57-1.62-3.57-3.57v-2.81c0-.97.76-1.84 1.84-1.84.97 0 1.84.76 1.84 1.84v3.03", "M5.48 18.49v-2.92c0-.97.76-1.84 1.84-1.84.97 0 1.84.76 1.84 1.84v2.81c0 1.95-1.62 3.57-3.57 3.57s-3.57-1.63-3.57-3.57v-2.37m9.52-7.82-.46-.92c-.11-.21-.41-.22-.52-.01l-.84 1.55c-.1.19-.3.31-.52.31h-.73m7.06 0h-.81a.61.61 0 0 0-.53.32l-.75 1.5c-.11.22-.42.22-.53 0")) }
    val SwapHorizontal: ImageVector by lazy { brokenIcon("SwapHorizontal", listOf("m3.5 9.01 5.01-5.02m4.5 5.02H3.5m17 0H17m3.5 5.98-5.01 5.02m-4.5-5.02h9.51m-17 0H7")) }
    val Sound: ImageVector by lazy { brokenIcon("Sound", listOf("M3 8.25v7.5m4.5-10v12.5M12 9.96v10.79m0-17.5v2.72m4.5-.22v12.5m4.5-10v7.5")) }

    val HeartBold: ImageVector by lazy {
        boldIcon("HeartBold") {
            boldFill {
                moveTo(12.62f, 20.81f)
                curveTo(12.28f, 20.93f, 11.72f, 20.93f, 11.38f, 20.81f)
                curveTo(8.48f, 19.82f, 2f, 15.69f, 2f, 8.69f)
                curveTo(2f, 5.6f, 4.49f, 3.1f, 7.56f, 3.1f)
                curveTo(9.38f, 3.1f, 10.99f, 3.98f, 12f, 5.34f)
                curveTo(13.01f, 3.98f, 14.63f, 3.1f, 16.44f, 3.1f)
                curveTo(19.51f, 3.1f, 22f, 5.6f, 22f, 8.69f)
                curveTo(22f, 15.69f, 15.52f, 19.82f, 12.62f, 20.81f)
                close()
            }
        }
    }

    val PlayBold: ImageVector by lazy {
        boldIcon("PlayBold") {
            boldFill {
                moveTo(6.5f, 6.2f)
                curveTo(6.5f, 4.4f, 8.5f, 3.3f, 10.05f, 4.25f)
                lineTo(19.35f, 9.95f)
                curveTo(20.85f, 10.88f, 20.85f, 13.12f, 19.35f, 14.05f)
                lineTo(10.05f, 19.75f)
                curveTo(8.5f, 20.7f, 6.5f, 19.6f, 6.5f, 17.8f)
                close()
            }
        }
    }

    val PauseBold: ImageVector by lazy {
        boldIcon("PauseBold") {
            boldFill {
                moveTo(7.4f, 4.2f)
                curveTo(8.45f, 4.2f, 9.3f, 5.05f, 9.3f, 6.1f)
                verticalLineTo(17.9f)
                curveTo(9.3f, 18.95f, 8.45f, 19.8f, 7.4f, 19.8f)
                curveTo(6.35f, 19.8f, 5.5f, 18.95f, 5.5f, 17.9f)
                verticalLineTo(6.1f)
                curveTo(5.5f, 5.05f, 6.35f, 4.2f, 7.4f, 4.2f)
                close()
                moveTo(16.6f, 4.2f)
                curveTo(17.65f, 4.2f, 18.5f, 5.05f, 18.5f, 6.1f)
                verticalLineTo(17.9f)
                curveTo(18.5f, 18.95f, 17.65f, 19.8f, 16.6f, 19.8f)
                curveTo(15.55f, 19.8f, 14.7f, 18.95f, 14.7f, 17.9f)
                verticalLineTo(6.1f)
                curveTo(14.7f, 5.05f, 15.55f, 4.2f, 16.6f, 4.2f)
                close()
            }
        }
    }

    val PreviousBold: ImageVector by lazy {
        boldIcon("PreviousBold") {
            boldFill {
                moveTo(4.9f, 5.0f)
                curveTo(5.56f, 5.0f, 6.1f, 5.54f, 6.1f, 6.2f)
                verticalLineTo(17.8f)
                curveTo(6.1f, 18.46f, 5.56f, 19.0f, 4.9f, 19.0f)
                curveTo(4.24f, 19.0f, 3.7f, 18.46f, 3.7f, 17.8f)
                verticalLineTo(6.2f)
                curveTo(3.7f, 5.54f, 4.24f, 5.0f, 4.9f, 5.0f)
                close()
                moveTo(18.8f, 6.5f)
                verticalLineTo(17.5f)
                curveTo(18.8f, 18.9f, 17.22f, 19.72f, 16.04f, 18.94f)
                lineTo(8.35f, 13.44f)
                curveTo(7.28f, 12.74f, 7.28f, 11.26f, 8.35f, 10.56f)
                lineTo(16.04f, 5.06f)
                curveTo(17.22f, 4.28f, 18.8f, 5.1f, 18.8f, 6.5f)
                close()
            }
        }
    }

    val NextBold: ImageVector by lazy {
        boldIcon("NextBold") {
            boldFill {
                moveTo(19.1f, 5.0f)
                curveTo(19.76f, 5.0f, 20.3f, 5.54f, 20.3f, 6.2f)
                verticalLineTo(17.8f)
                curveTo(20.3f, 18.46f, 19.76f, 19.0f, 19.1f, 19.0f)
                curveTo(18.44f, 19.0f, 17.9f, 18.46f, 17.9f, 17.8f)
                verticalLineTo(6.2f)
                curveTo(17.9f, 5.54f, 18.44f, 5.0f, 19.1f, 5.0f)
                close()
                moveTo(5.2f, 6.5f)
                verticalLineTo(17.5f)
                curveTo(5.2f, 18.9f, 6.78f, 19.72f, 7.96f, 18.94f)
                lineTo(15.65f, 13.44f)
                curveTo(16.72f, 12.74f, 16.72f, 11.26f, 15.65f, 10.56f)
                lineTo(7.96f, 5.06f)
                curveTo(6.78f, 4.28f, 5.2f, 5.1f, 5.2f, 6.5f)
                close()
            }
        }
    }

    val HomeBold: ImageVector by lazy {
        boldIcon("HomeBold") {
            boldFill {
                moveTo(9.02f, 2.84f)
                lineTo(3.63f, 7.04f)
                curveTo(2.73f, 7.74f, 2f, 9.23f, 2f, 10.36f)
                verticalLineTo(17.77f)
                curveTo(2f, 20.09f, 3.89f, 21.99f, 6.21f, 21.99f)
                horizontalLineTo(17.79f)
                curveTo(20.11f, 21.99f, 22f, 20.09f, 22f, 17.78f)
                verticalLineTo(10.5f)
                curveTo(22f, 9.29f, 21.19f, 7.74f, 20.2f, 7.05f)
                lineTo(14.02f, 2.72f)
                curveTo(12.62f, 1.74f, 10.37f, 1.79f, 9.02f, 2.84f)
                close()
            }
        }
    }

    val FolderBold: ImageVector by lazy {
        boldIcon("FolderBold") {
            boldFill {
                moveTo(22f, 11f); verticalLineTo(17f)
                curveTo(22f, 21f, 21f, 22f, 17f, 22f)
                horizontalLineTo(7f)
                curveTo(3f, 22f, 2f, 21f, 2f, 17f)
                verticalLineTo(7f)
                curveTo(2f, 3f, 3f, 2f, 7f, 2f)
                horizontalLineTo(8.5f)
                curveTo(10f, 2f, 10.33f, 2.44f, 10.9f, 3.2f)
                lineTo(12.4f, 5.2f)
                curveTo(12.78f, 5.7f, 13f, 6f, 14f, 6f)
                horizontalLineTo(17f)
                curveTo(21f, 6f, 22f, 7f, 22f, 11f)
                close()
            }
        }
    }

    val MusicNoteBold: ImageVector by lazy {
        boldIcon("MusicNoteBold") {
            boldFill {
                moveTo(18.6f, 2.25f)
                lineTo(11.3f, 4.2f)
                curveTo(9.8f, 4.6f, 8.8f, 5.75f, 8.8f, 7.35f)
                verticalLineTo(14.7f)
                curveTo(8.1f, 14.3f, 7.25f, 14.1f, 6.35f, 14.15f)
                curveTo(4.35f, 14.25f, 2.8f, 15.85f, 2.8f, 17.85f)
                curveTo(2.8f, 19.9f, 4.45f, 21.55f, 6.5f, 21.55f)
                curveTo(8.55f, 21.55f, 10.2f, 19.9f, 10.2f, 17.85f)
                verticalLineTo(10.1f)
                lineTo(19.6f, 7.55f)
                verticalLineTo(12.9f)
                curveTo(18.9f, 12.5f, 18.05f, 12.3f, 17.15f, 12.35f)
                curveTo(15.15f, 12.45f, 13.6f, 14.05f, 13.6f, 16.05f)
                curveTo(13.6f, 18.1f, 15.25f, 19.75f, 17.3f, 19.75f)
                curveTo(19.35f, 19.75f, 21.0f, 18.1f, 21.0f, 16.05f)
                verticalLineTo(4.4f)
                curveTo(21.0f, 2.75f, 19.95f, 1.9f, 18.6f, 2.25f)
                close()
            }
        }
    }

    val VideoBold: ImageVector by lazy {
        boldIcon("VideoBold") {
            boldFill(PathFillType.EvenOdd) {
                moveTo(9f, 2f); horizontalLineTo(15f)
                curveTo(20f, 2f, 22f, 4f, 22f, 9f)
                verticalLineTo(15f)
                curveTo(22f, 20f, 20f, 22f, 15f, 22f)
                horizontalLineTo(9f)
                curveTo(4f, 22f, 2f, 20f, 2f, 15f)
                verticalLineTo(9f)
                curveTo(2f, 4f, 4f, 2f, 9f, 2f)
                close()
                moveTo(9.5f, 9.5f)
                curveTo(9.5f, 8.2f, 10.9f, 7.4f, 12.05f, 8.05f)
                lineTo(16.0f, 10.35f)
                curveTo(17.1f, 11.0f, 17.1f, 12.6f, 16.0f, 13.25f)
                lineTo(12.05f, 15.55f)
                curveTo(10.9f, 16.2f, 9.5f, 15.4f, 9.5f, 14.1f)
                close()
            }
        }
    }

    val FullscreenExit: ImageVector by lazy {
        linearIcon("FullscreenExit") {
            linearStroke {
                moveTo(9f, 22f); horizontalLineTo(15f)
                curveTo(20f, 22f, 22f, 20f, 22f, 15f)
                verticalLineTo(9f)
                curveTo(22f, 4f, 20f, 2f, 15f, 2f)
                horizontalLineTo(9f)
                curveTo(4f, 2f, 2f, 4f, 2f, 9f)
                verticalLineTo(15f)
                curveTo(2f, 20f, 4f, 22f, 9f, 22f)
                close()
                moveTo(13.8f, 10.2f); lineTo(17.2f, 6.8f)
                moveTo(13.8f, 10.2f); horizontalLineTo(16.8f)
                moveTo(13.8f, 10.2f); verticalLineTo(7.2f)
                moveTo(10.2f, 13.8f); lineTo(6.8f, 17.2f)
                moveTo(10.2f, 13.8f); horizontalLineTo(7.2f)
                moveTo(10.2f, 13.8f); verticalLineTo(16.8f)
            }
        }
    }

    val Discord: ImageVector by lazy {
        linearIcon("Discord") {
            linearStroke(1.8f) {
                moveTo(9.1f, 5.5f)
                curveTo(10.95f, 5.15f, 13.05f, 5.15f, 14.9f, 5.5f)
                lineTo(16.1f, 4.5f)
                curveTo(18.6f, 5.25f, 20.55f, 7.1f, 21.15f, 14.8f)
                curveTo(19.8f, 16.95f, 17.8f, 18.1f, 15.85f, 18.6f)
                lineTo(14.85f, 16.9f)
                moveTo(14.9f, 5.5f); curveTo(13.05f, 5.15f, 10.95f, 5.15f, 9.1f, 5.5f)
                lineTo(7.9f, 4.5f)
                curveTo(5.4f, 5.25f, 3.45f, 7.1f, 2.85f, 14.8f)
                curveTo(4.2f, 16.95f, 6.2f, 18.1f, 8.15f, 18.6f)
                lineTo(9.15f, 16.9f)
                moveTo(7.2f, 15.6f)
                curveTo(10.1f, 17.1f, 13.9f, 17.1f, 16.8f, 15.6f)
                moveTo(9.1f, 12.3f); curveTo(9.5f, 12.3f, 9.8f, 12.0f, 9.8f, 11.6f)
                curveTo(9.8f, 11.2f, 9.5f, 10.9f, 9.1f, 10.9f)
                curveTo(8.7f, 10.9f, 8.4f, 11.2f, 8.4f, 11.6f)
                curveTo(8.4f, 12.0f, 8.7f, 12.3f, 9.1f, 12.3f); close()
                moveTo(14.9f, 12.3f); curveTo(15.3f, 12.3f, 15.6f, 12.0f, 15.6f, 11.6f)
                curveTo(15.6f, 11.2f, 15.3f, 10.9f, 14.9f, 10.9f)
                curveTo(14.5f, 10.9f, 14.2f, 11.2f, 14.2f, 11.6f)
                curveTo(14.2f, 12.0f, 14.5f, 12.3f, 14.9f, 12.3f); close()
            }
        }
    }

    val Facebook: ImageVector by lazy {
        brokenIcon(
            "Facebook",
            listOf(
                "M14 9.3v2.95h2.63c.19 0 .33.17.29.36l-.38 1.9c-.03.14-.15.24-.29.24H14V22h-3v-7.25H9.3c-.17 0-.3-.13-.3-.3v-1.9c0-.17.13-.3.3-.3H11V9c0-1.66 1.34-3 3-3h2.7c.17 0 .3.13.3.3v2.4c0 .17-.13.3-.3.3h-2.4c-.17 0-.3.13-.3.3Z",
                "M2 12.83V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9"
            )
        )
    }

    val Donate: ImageVector by lazy {
        brokenIcon(
            "Donate",
            listOf(
                "M8.67 14.33c0 1.29.99 2.33 2.22 2.33h2.51c1.07 0 1.94-.91 1.94-2.03 0-1.22-.53-1.65-1.32-1.93l-4.03-1.4c-.79-.28-1.32-.71-1.32-1.93 0-1.12.87-2.03 1.94-2.03h2.51c1.23 0 2.22 1.04 2.22 2.33M12 6v12",
                "M4 6c-1.25 1.67-2 3.75-2 6 0 5.52 4.48 10 10 10s10-4.48 10-10S17.52 2 12 2c-1.43 0-2.8.3-4.03.85"
            )
        )
    }

    val DeveloperAvatar: ImageVector by lazy {
        brokenIcon(
            "DeveloperAvatar",
            listOf(
                "M15.02 3.01A4.94 4.94 0 0 0 12 2C9.24 2 7 4.24 7 7s2.24 5 5 5 5-2.24 5-5",
                "m19.21 15.74-3.54 3.54c-.14.14-.27.4-.3.59l-.19 1.35c-.07.49.27.83.76.76l1.35-.19c.19-.03.46-.16.59-.3l3.54-3.54c.61-.61.9-1.32 0-2.22-.89-.89-1.6-.6-2.21.01",
                "M18.7 16.25c.3 1.08 1.14 1.92 2.22 2.22",
                "M3.41 22c0-3.87 3.85-7 8.59-7 1.04 0 2.04.15 2.97.43"
            )
        )
    }

    val ArtworkDownload: ImageVector by lazy {
        brokenIcon(
            "ArtworkDownload",
            listOf(
                "M9 14.35V17l2-2m-2-4v1m0 5-2-2M2 9c0-5 2-7 7-7h5",
                "M22 10v5c0 5-2 7-7 7H9c-5 0-7-2-7-7v-2.02M18 10c-3 0-4-1-4-4V2l8 8"
            )
        )
    }

    val Mood: ImageVector by lazy {
        brokenIcon(
            "Mood",
            listOf(
                "M2 12.96V15c0 5 2 7 7 7h6c5 0 7-2 7-7V9c0-5-2-7-7-7H9C4 2 2 4 2 9",
                "M15.5 9.75a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3m-7 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3m-.1 3.55h7.2c.5 0 .9.4.9.9 0 2.49-2.01 4.5-4.5 4.5s-4.5-2.01-4.5-4.5c0-.5.4-.9.9-.9"
            )
        )
    }

    val StarFilled: ImageVector by lazy {
        filledIcon(
            "StarFilled",
            listOf(
                "M12 2.8l2.58 5.23 5.77.84-4.18 4.07.99 5.75L12 15.97l-5.16 2.72.99-5.75L3.65 8.87l5.77-.84L12 2.8z"
            )
        )
    }

    val PlayOutlineClean: ImageVector by lazy {
        linearIcon("PlayOutlineClean") {
            linearStroke(2.1f) {
                moveTo(7.2f, 5.4f)
                lineTo(18.6f, 12f)
                lineTo(7.2f, 18.6f)
                close()
            }
        }
    }

    val PauseOutlineClean: ImageVector by lazy {
        linearIcon("PauseOutlineClean") {
            linearStroke(2.1f) {
                moveTo(8f, 5.2f); verticalLineTo(18.8f)
                moveTo(16f, 5.2f); verticalLineTo(18.8f)
            }
        }
    }

    val PrevOutlineClean: ImageVector by lazy {
        linearIcon("PrevOutlineClean") {
            linearStroke(2.0f) {
                moveTo(5.6f, 5.5f); verticalLineTo(18.5f)
                moveTo(18.2f, 5.8f); lineTo(8.8f, 12f); lineTo(18.2f, 18.2f); close()
            }
        }
    }

    val NextOutlineClean: ImageVector by lazy {
        linearIcon("NextOutlineClean") {
            linearStroke(2.0f) {
                moveTo(18.4f, 5.5f); verticalLineTo(18.5f)
                moveTo(5.8f, 5.8f); lineTo(15.2f, 12f); lineTo(5.8f, 18.2f); close()
            }
        }
    }

    val PlayCapsule: ImageVector by lazy {
        boldIcon("PlayCapsule") {
            boldFill {
                moveTo(8.0f, 5.2f)
                curveTo(8.0f, 4.0f, 9.35f, 3.25f, 10.4f, 3.9f)
                lineTo(19.6f, 9.7f)
                curveTo(20.6f, 10.35f, 20.6f, 11.65f, 19.6f, 12.3f)
                lineTo(10.4f, 18.1f)
                curveTo(9.35f, 18.75f, 8.0f, 18.0f, 8.0f, 16.8f)
                close()
            }
        }
    }

    val PauseCapsule: ImageVector by lazy {
        boldIcon("PauseCapsule") {
            boldFill {
                moveTo(6.2f, 4.5f)
                curveTo(7.4f, 4.5f, 8.4f, 5.5f, 8.4f, 6.7f)
                verticalLineTo(17.3f)
                curveTo(8.4f, 18.5f, 7.4f, 19.5f, 6.2f, 19.5f)
                curveTo(5.0f, 19.5f, 4.0f, 18.5f, 4.0f, 17.3f)
                verticalLineTo(6.7f)
                curveTo(4.0f, 5.5f, 5.0f, 4.5f, 6.2f, 4.5f)
                close()
                moveTo(17.8f, 4.5f)
                curveTo(19.0f, 4.5f, 20.0f, 5.5f, 20.0f, 6.7f)
                verticalLineTo(17.3f)
                curveTo(20.0f, 18.5f, 19.0f, 19.5f, 17.8f, 19.5f)
                curveTo(16.6f, 19.5f, 15.6f, 18.5f, 15.6f, 17.3f)
                verticalLineTo(6.7f)
                curveTo(15.6f, 5.5f, 16.6f, 4.5f, 17.8f, 4.5f)
                close()
            }
        }
    }

    val PrevMinimal: ImageVector by lazy {
        linearIcon("PrevMinimal") {
            linearStroke(2.1f) {
                moveTo(14.5f, 6.5f); lineTo(9.0f, 12.0f); lineTo(14.5f, 17.5f)
                moveTo(6.5f, 7.0f); verticalLineTo(17.0f)
            }
        }
    }

    val NextMinimal: ImageVector by lazy {
        linearIcon("NextMinimal") {
            linearStroke(2.1f) {
                moveTo(9.5f, 6.5f); lineTo(15.0f, 12.0f); lineTo(9.5f, 17.5f)
                moveTo(17.5f, 7.0f); verticalLineTo(17.0f)
            }
        }
    }

    data class PlaybackTransportIcons(
        val previous: ImageVector,
        val play: ImageVector,
        val pause: ImageVector,
        val next: ImageVector
    )

    fun transportIcons(style: PlaybackIconStyle): PlaybackTransportIcons = when (style) {
        PlaybackIconStyle.NORMAL -> PlaybackTransportIcons(
            previous = PreviousBold,
            play = PlayBold,
            pause = PauseBold,
            next = NextBold
        )
        PlaybackIconStyle.BROKEN -> PlaybackTransportIcons(
            previous = PrevLinear,
            play = PlayLinear,
            pause = PauseLinear,
            next = NextLinear
        )
        PlaybackIconStyle.OUTLINE -> PlaybackTransportIcons(
            previous = PrevOutlineClean,
            play = PlayOutlineClean,
            pause = PauseOutlineClean,
            next = NextOutlineClean
        )
        PlaybackIconStyle.CAPSULE -> PlaybackTransportIcons(
            previous = PreviousBold,
            play = PlayCapsule,
            pause = PauseCapsule,
            next = NextBold
        )
        PlaybackIconStyle.MINIMAL -> PlaybackTransportIcons(
            previous = PrevMinimal,
            play = PlayOutlineClean,
            pause = PauseOutlineClean,
            next = NextMinimal
        )
    }
}
