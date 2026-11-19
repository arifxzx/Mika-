package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.compose.ui.graphics.toArgb
import com.example.MainActivity
import com.example.MikaApplication
import com.example.R
import com.example.media.ArtworkManager
import com.example.model.AppSettings
import com.example.model.Song
import com.example.model.WidgetArtworkEffect
import com.example.model.WidgetBackgroundStyle
import com.example.model.WidgetConfig
import com.example.model.WidgetTapAction
import com.example.model.WidgetThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class WidgetLayoutPreset(@LayoutRes val layoutRes: Int) {
    CLASSIC(R.layout.widget_mika_player),
    COMPACT(R.layout.widget_mika_compact),
    EXPANDED(R.layout.widget_mika_expanded)
}

open class MikaMusicWidgetProvider : AppWidgetProvider() {

    open val preset: WidgetLayoutPreset = WidgetLayoutPreset.CLASSIC

    companion object {
        const val ACTION_PLAY_PAUSE = "dev.sagor.mika.WIDGET_PLAY_PAUSE"
        const val ACTION_NEXT = "dev.sagor.mika.WIDGET_NEXT"
        const val ACTION_PREV = "dev.sagor.mika.WIDGET_PREV"
        const val ACTION_STOP = "dev.sagor.mika.WIDGET_STOP"
        const val ACTION_FAVORITE = "dev.sagor.mika.WIDGET_FAVORITE"
        const val ACTION_REPEAT = "dev.sagor.mika.WIDGET_REPEAT"
        const val ACTION_SHUFFLE = "dev.sagor.mika.WIDGET_SHUFFLE"

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun requestWidgetUpdate(context: Context) {
            widgetScope.launch {
                updateAllWidgetsInternal(context.applicationContext)
            }
        }

        private suspend fun updateAllWidgetsInternal(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val classicIds = manager.getAppWidgetIds(ComponentName(context, MikaMusicWidgetProvider::class.java))
            val compactIds = manager.getAppWidgetIds(ComponentName(context, MikaCompactWidgetProvider::class.java))
            val expandedIds = manager.getAppWidgetIds(ComponentName(context, MikaExpandedWidgetProvider::class.java))
            if (classicIds.isEmpty() && compactIds.isEmpty() && expandedIds.isEmpty()) return

            val app = context.applicationContext as? MikaApplication ?: return
            val settings = runCatching { app.settingsDataStore.settingsFlow.first() }
                .getOrDefault(AppSettings())
            val playback = app.playbackController.playbackState.value
            val song = playback.currentSong
            val config = settings.widgetConfig

            val rawArt = song?.let {
                runCatching { ArtworkManager.getOrExtractBitmap(context, it, maxDimension = 256) }.getOrNull()
            }
            val colorBundle = song?.let {
                runCatching { ArtworkManager.extractArtworkColorBundle(context, it) }.getOrNull()
            }

            fun updateGroup(ids: IntArray, preset: WidgetLayoutPreset, defaultW: Int, defaultH: Int) {
                ids.forEach { appWidgetId ->
                    val options = manager.getAppWidgetOptions(appWidgetId)
                    val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, defaultW).coerceAtLeast(110)
                    val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, defaultH).coerceAtLeast(52)
                    val views = buildRemoteViews(
                        context = context,
                        preset = preset,
                        config = config,
                        song = song,
                        isPlaying = playback.isPlaying,
                        rawArt = rawArt,
                        artStartArgb = colorBundle?.deepPair?.first?.toArgb(),
                        artEndArgb = colorBundle?.deepPair?.second?.toArgb(),
                        minWidthDp = minWidthDp,
                        minHeightDp = minHeightDp
                    )
                    manager.updateAppWidget(appWidgetId, views)
                }
            }

            updateGroup(classicIds, WidgetLayoutPreset.CLASSIC, 250, 90)
            updateGroup(compactIds, WidgetLayoutPreset.COMPACT, 160, 58)
            updateGroup(expandedIds, WidgetLayoutPreset.EXPANDED, 280, 150)
        }

        private fun buildRemoteViews(
            context: Context,
            preset: WidgetLayoutPreset,
            config: WidgetConfig,
            song: Song?,
            isPlaying: Boolean,
            rawArt: Bitmap?,
            artStartArgb: Int?,
            artEndArgb: Int?,
            minWidthDp: Int = 250,
            minHeightDp: Int = 90
        ): RemoteViews {
            val isCompactWidget = preset == WidgetLayoutPreset.COMPACT
            val isExpandedWidget = preset == WidgetLayoutPreset.EXPANDED
            val isCompactWidth = isCompactWidget || minWidthDp < 210
            val isUltraCompact = !isCompactWidget && minWidthDp < 165
            val isShortHeight = isCompactWidget || minHeightDp < 78
            val views = RemoteViews(context.packageName, preset.layoutRes)
            val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val isDark = when (config.theme) {
                WidgetThemeMode.DARK -> true
                WidgetThemeMode.LIGHT -> false
                WidgetThemeMode.SYSTEM -> isSystemDark
            }

            val baseStart = when {
                config.colorsFromArtwork && artStartArgb != null -> artStartArgb
                isDark -> Color.rgb(18, 22, 34)
                else -> Color.rgb(241, 245, 249)
            }
            val baseEnd = when {
                config.colorsFromArtwork && artEndArgb != null -> artEndArgb
                isDark -> Color.rgb(34, 42, 64)
                else -> Color.rgb(220, 230, 242)
            }

            val bgBitmap = renderBackgroundBitmap(
                config = config,
                isDark = isDark,
                startColor = baseStart,
                endColor = baseEnd,
                rawArt = rawArt,
                targetWidthPx = (minWidthDp * 2).coerceIn(220, 720),
                targetHeightPx = (minHeightDp * 2).coerceIn(104, 380)
            )
            views.setImageViewBitmap(R.id.widget_bg_image, bgBitmap)

            val fgColor = if (isDark || (config.colorsFromArtwork && artStartArgb != null)) {
                Color.WHITE
            } else {
                Color.rgb(20, 26, 36)
            }
            val subFgColor = Color.argb(
                205,
                Color.red(fgColor),
                Color.green(fgColor),
                Color.blue(fgColor)
            )

            views.setTextColor(R.id.widget_title, fgColor)
            views.setTextColor(R.id.widget_subtitle, subFgColor)

            if (config.showArtwork && !isUltraCompact) {
                views.setViewVisibility(R.id.widget_artwork, View.VISIBLE)
                val artScale = (config.artworkSizePct / 100f).coerceIn(0.6f, 1.3f)
                val basePx = when {
                    isExpandedWidget -> 188f
                    isCompactWidget -> 120f
                    isShortHeight -> 110f
                    else -> 160f
                }
                val artBitmapPx = (basePx * artScale).toInt().coerceIn(88, 230)
                val artBmp = renderRoundedArtworkBitmap(config, rawArt, baseEnd, artBitmapPx)
                views.setImageViewBitmap(R.id.widget_artwork, artBmp)
            } else {
                views.setViewVisibility(R.id.widget_artwork, View.GONE)
            }

            val title = song?.title ?: context.getString(R.string.app_name)
            val subtitle = when {
                song == null -> "Tap to open library"
                song.artist.equals(song.album, ignoreCase = true) -> song.artist
                else -> "${song.artist} · ${song.album}"
            }

            views.setViewVisibility(R.id.widget_title, if (config.showTitle) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_title, title)
            val showSub = when {
                !config.showSubtitle -> false
                isExpandedWidget -> true
                isCompactWidget -> true
                else -> !isShortHeight && !isUltraCompact
            }
            views.setViewVisibility(R.id.widget_subtitle, if (showSub) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_subtitle, subtitle)

            val contentScale = ((config.contentSizePct / 100f) * (if (isCompactWidth) 0.92f else 1f)).coerceIn(0.75f, 1.25f)
            views.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, (if (isExpandedWidget) 16f else 15f) * contentScale)
            views.setTextViewTextSize(R.id.widget_subtitle, android.util.TypedValue.COMPLEX_UNIT_SP, (if (isExpandedWidget) 13f else 12f) * contentScale)

            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )
            views.setInt(R.id.widget_btn_play_pause, "setColorFilter", fgColor)
            views.setInt(R.id.widget_btn_next, "setColorFilter", fgColor)
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, broadcastIntent(context, ACTION_PLAY_PAUSE, 104))
            views.setOnClickPendingIntent(R.id.widget_btn_next, broadcastIntent(context, ACTION_NEXT, 105))

            if (isCompactWidget) {
                views.setViewVisibility(R.id.widget_btn_play_pause, if (config.showPlayPause) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widget_btn_next, if (config.showNext) View.VISIBLE else View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_btn_favorite, if (config.showFavourite && (!isCompactWidth || isExpandedWidget)) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widget_btn_shuffle, if (config.showShuffle && (!isCompactWidth || isExpandedWidget)) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widget_btn_prev, if (config.showPrevious) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widget_btn_play_pause, if (config.showPlayPause) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widget_btn_next, if (config.showNext) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widget_btn_repeat, if (config.showRepeat && (!isCompactWidth || isExpandedWidget)) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widget_btn_stop, if (config.showStop && (!isUltraCompact || isExpandedWidget)) View.VISIBLE else View.GONE)

                views.setImageViewResource(
                    R.id.widget_btn_favorite,
                    if (song?.isFavorite == true) R.drawable.ic_notif_heart_filled else R.drawable.ic_notif_heart_outline
                )
                listOf(
                    R.id.widget_btn_favorite,
                    R.id.widget_btn_shuffle,
                    R.id.widget_btn_prev,
                    R.id.widget_btn_repeat,
                    R.id.widget_btn_stop
                ).forEach { id ->
                    views.setInt(id, "setColorFilter", fgColor)
                }

                views.setOnClickPendingIntent(R.id.widget_btn_favorite, broadcastIntent(context, ACTION_FAVORITE, 101))
                views.setOnClickPendingIntent(R.id.widget_btn_shuffle, broadcastIntent(context, ACTION_SHUFFLE, 102))
                views.setOnClickPendingIntent(R.id.widget_btn_prev, broadcastIntent(context, ACTION_PREV, 103))
                views.setOnClickPendingIntent(R.id.widget_btn_repeat, broadcastIntent(context, ACTION_REPEAT, 106))
                views.setOnClickPendingIntent(R.id.widget_btn_stop, broadcastIntent(context, ACTION_STOP, 107))
            }

            applyTapAction(context, views, R.id.widget_root, config.widgetTapAction, 201)
            applyTapAction(context, views, R.id.widget_artwork, config.artworkTapAction, 202)
            return views
        }

        private fun applyTapAction(
            context: Context,
            views: RemoteViews,
            viewId: Int,
            action: WidgetTapAction,
            requestCode: Int
        ) {
            val pi = when (action) {
                WidgetTapAction.OPEN_APP -> {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    PendingIntent.getActivity(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                WidgetTapAction.PLAY_PAUSE -> broadcastIntent(context, ACTION_PLAY_PAUSE, requestCode)
                WidgetTapAction.NOTHING -> null
            }
            if (pi != null) {
                views.setOnClickPendingIntent(viewId, pi)
            }
        }

        private fun broadcastIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MikaMusicWidgetProvider::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun renderBackgroundBitmap(
            config: WidgetConfig,
            isDark: Boolean,
            startColor: Int,
            endColor: Int,
            rawArt: Bitmap?,
            targetWidthPx: Int = 460,
            targetHeightPx: Int = 160
        ): Bitmap {
            val width = targetWidthPx
            val height = targetHeightPx
            val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val alpha = (config.backgroundOpacity.coerceIn(0f, 1f) * 255).toInt()
            val radius = (config.cornerRadiusDp.toFloat() * 1.4f).coerceAtLeast(0f)
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

            val path = android.graphics.Path().apply {
                addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(path)

            when (config.backgroundStyle) {
                WidgetBackgroundStyle.SOLID -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = startColor
                        this.alpha = alpha
                    }
                    canvas.drawRect(rect, p)
                }
                WidgetBackgroundStyle.GRADIENT -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            0f, 0f, width.toFloat(), height.toFloat(),
                            startColor, endColor, Shader.TileMode.CLAMP
                        )
                        this.alpha = alpha
                    }
                    canvas.drawRect(rect, p)
                }
                WidgetBackgroundStyle.BLURRED_ART -> {
                    if (rawArt != null) {
                        val tiny = Bitmap.createScaledBitmap(rawArt, 24, 12, true)
                        val blurred = Bitmap.createScaledBitmap(tiny, width, height, true)
                        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            this.alpha = alpha
                            colorFilter = PorterDuffColorFilter(
                                if (isDark) Color.argb(120, 10, 12, 18) else Color.argb(90, 255, 255, 255),
                                PorterDuff.Mode.SRC_ATOP
                            )
                        }
                        canvas.drawBitmap(blurred, 0f, 0f, p)
                    } else {
                        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), startColor, endColor, Shader.TileMode.CLAMP)
                            this.alpha = alpha
                        }
                        canvas.drawRect(rect, p)
                    }
                }
            }
            canvas.restore()
            return out
        }

        private fun renderRoundedArtworkBitmap(
            config: WidgetConfig,
            rawArt: Bitmap?,
            accentColor: Int,
            targetSizePx: Int = 160
        ): Bitmap {
            val size = targetSizePx
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val pad = if (config.artworkEffect != WidgetArtworkEffect.NONE) 8f else 0f
            val rect = RectF(pad, pad, size - pad, size - pad)
            val radius = rect.width() * (config.artworkRoundingPct.coerceIn(0, 50) / 100f)

            if (config.artworkEffect == WidgetArtworkEffect.GLOW) {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    alpha = 140
                    setShadowLayer(12f, 0f, 0f, accentColor)
                }
                canvas.drawRoundRect(rect, radius, radius, glowPaint)
            } else if (config.artworkEffect == WidgetArtworkEffect.SHADOW) {
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(110, 0, 0, 0)
                    setShadowLayer(10f, 0f, 5f, Color.argb(150, 0, 0, 0))
                }
                canvas.drawRoundRect(rect, radius, radius, shadowPaint)
            }

            val clipPath = android.graphics.Path().apply {
                addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            if (rawArt != null) {
                canvas.drawBitmap(rawArt, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            } else {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, size.toFloat(), size.toFloat(),
                        accentColor, Color.rgb(24, 30, 48), Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(rect, bgPaint)
                val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(200, 255, 255, 255)
                    textSize = 58f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("♪", size / 2f, size / 2f + 20f, notePaint)
            }
            canvas.restore()
            return out
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        requestWidgetUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        requestWidgetUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val app = context.applicationContext as? MikaApplication ?: return
        val controller = app.playbackController
        when (intent.action) {
            ACTION_PLAY_PAUSE -> controller.togglePlayPause()
            ACTION_NEXT -> controller.skipNext()
            ACTION_PREV -> controller.skipPrevious()
            ACTION_STOP -> controller.stopPlaybackAndClear()
            ACTION_REPEAT -> controller.cycleRepeatMode()
            ACTION_SHUFFLE -> controller.toggleShuffle()
            ACTION_FAVORITE -> {
                val song = controller.playbackState.value.currentSong ?: return
                val nextFav = !song.isFavorite
                controller.updateSongFavoriteInQueue(song.id, nextFav)
                widgetScope.launch {
                    app.repository.setFavorite(song.id, nextFav)
                    updateAllWidgetsInternal(context.applicationContext)
                }
            }
        }
        if (intent.action in setOf(
                ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREV, ACTION_STOP,
                ACTION_REPEAT, ACTION_SHUFFLE
            )
        ) {
            requestWidgetUpdate(context)
        }
    }
}

class MikaCompactWidgetProvider : MikaMusicWidgetProvider() {
    override val preset: WidgetLayoutPreset = WidgetLayoutPreset.COMPACT
}

class MikaExpandedWidgetProvider : MikaMusicWidgetProvider() {
    override val preset: WidgetLayoutPreset = WidgetLayoutPreset.EXPANDED
}
