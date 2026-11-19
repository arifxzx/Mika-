package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.model.AppIconPreset
import java.io.File
import java.io.FileOutputStream

object AppIconManager {

    private const val CUSTOM_ICON_FILE = "custom_app_icon.png"

    private val presetAliases: Map<AppIconPreset, String> = mapOf(
        AppIconPreset.AURORA_GLASS to "com.example.MainLauncherAurora",
        AppIconPreset.MONO_OUTLINE to "com.example.MainLauncherMono",
        AppIconPreset.VIOLET_LINE to "com.example.MainLauncherViolet",
        AppIconPreset.NEON_WAVE to "com.example.MainLauncherNeon",
        AppIconPreset.CLASSIC to "com.example.MainLauncherClassic"
    )

    fun customIconFile(context: Context): File = File(context.filesDir, CUSTOM_ICON_FILE)

    fun applyLauncherPreset(context: Context, preset: AppIconPreset): Boolean {
        val targetPreset = if (preset == AppIconPreset.CUSTOM) AppIconPreset.AURORA_GLASS else preset
        val targetAlias = presetAliases[targetPreset] ?: return false
        return runCatching {
            val pm = context.packageManager
            val pkg = context.packageName
            presetAliases.forEach { (_, className) ->
                val state = if (className == targetAlias) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, className),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }
            true
        }.getOrDefault(false)
    }

    fun saveAndPinCustomIcon(context: Context, imageUri: Uri): Result<String> = runCatching {
        val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: error("Unable to open image")
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Unsupported image format")

        val side = minOf(raw.width, raw.height).coerceAtLeast(1)
        val x = ((raw.width - side) / 2).coerceAtLeast(0)
        val y = ((raw.height - side) / 2).coerceAtLeast(0)
        val square = Bitmap.createBitmap(raw, x, y, side, side)
        val scaled = Bitmap.createScaledBitmap(square, 256, 256, true)

        val out = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val path = Path().apply {
            addRoundRect(RectF(0f, 0f, 256f, 256f), 58f, 58f, Path.Direction.CW)
        }
        canvas.clipPath(path)
        canvas.drawBitmap(scaled, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        val dest = customIconFile(context)
        FileOutputStream(dest).use { fos ->
            out.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        if (square !== raw) square.recycle()
        if (scaled !== square) scaled.recycle()

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val shortcut = ShortcutInfoCompat.Builder(context, "mika_custom_icon_${System.currentTimeMillis()}")
                .setShortLabel("Mika Player")
                .setLongLabel("Open Mika Player")
                .setIcon(IconCompat.createWithBitmap(out))
                .setIntent(launchIntent)
                .build()
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }
        dest.absolutePath
    }
}
