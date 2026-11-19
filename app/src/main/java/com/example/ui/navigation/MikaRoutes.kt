package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface MikaRoute {
    @Serializable data object Home : MikaRoute
    @Serializable data object Folders : MikaRoute
    @Serializable data object Library : MikaRoute
    @Serializable data object Search : MikaRoute
    @Serializable data object Settings : MikaRoute
    @Serializable data object Statistics : MikaRoute
    @Serializable data object Trash : MikaRoute
    @Serializable data object Hidden : MikaRoute
    @Serializable data object About : MikaRoute
    @Serializable data object Backup : MikaRoute
    @Serializable data object Genres : MikaRoute
    @Serializable data object Lyrics : MikaRoute
    @Serializable data object Videos : MikaRoute
    @Serializable data object WidgetSettings : MikaRoute
    @Serializable data object PlayerStyleSettings : MikaRoute
    @Serializable data class GenreDetail(val genreName: String) : MikaRoute
    @Serializable data class Collection(val collectionType: String) : MikaRoute
    @Serializable data class PlaylistDetail(val playlistId: Long) : MikaRoute
    @Serializable data class AlbumDetail(val albumId: Long) : MikaRoute
    @Serializable data class ArtistDetail(val artistId: Long) : MikaRoute
}
