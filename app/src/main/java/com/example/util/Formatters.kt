package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val hours = totalSec / 3600L
        val minutes = (totalSec % 3600L) / 60L
        val seconds = totalSec % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "Unknown"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatTimestamp(epochMillis: Long): String {
        if (epochMillis <= 0L) return "Never"
        val fmt = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        return fmt.format(Date(epochMillis))
    }

    fun formatFrequencyHz(hz: Int): String {
        return if (hz >= 1000) {
            val khz = hz / 1000f
            if (khz % 1f == 0f) "${khz.toInt()}kHz" else String.format(Locale.US, "%.1fkHz", khz)
        } else {
            "${hz}Hz"
        }
    }

    fun formatArtistAndAlbum(
        artist: String,
        album: String,
        separator: String = " · "
    ): String {
        val cleanArtist = artist.trim()
        val cleanAlbum = album.trim()
        val albumMeaningful = cleanAlbum.isNotBlank() &&
            !cleanAlbum.equals("<unknown>", ignoreCase = true) &&
            !cleanAlbum.equals("unknown", ignoreCase = true) &&
            !cleanAlbum.equals("unknown album", ignoreCase = true)
        return when {
            cleanArtist.isBlank() && albumMeaningful -> cleanAlbum
            !albumMeaningful -> cleanArtist.ifBlank { "Unknown Artist" }
            cleanArtist.equals(cleanAlbum, ignoreCase = true) -> cleanArtist
            else -> "$cleanArtist$separator$cleanAlbum"
        }
    }

    fun timeGreeting(hourOfDay: Int): Pair<String, String> {
        val h = ((hourOfDay % 24) + 24) % 24
        return when (h) {
            in 5..11 -> "Good Morning" to "Start your day with your favorite tracks"
            in 12..16 -> "Good Afternoon" to "Keep the rhythm flowing"
            in 17..20 -> "Good Evening" to "Unwind with your evening soundscape"
            else -> "Late Night Vibes" to "Deep, calm sessions in offline fidelity"
        }
    }
}
