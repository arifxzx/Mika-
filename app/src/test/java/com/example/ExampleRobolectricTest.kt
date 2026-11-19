package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.text.font.FontWeight
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupPlaylistDto
import com.example.data.backup.BackupPlaylistTrackRefDto
import com.example.data.backup.BackupSettingsDto
import com.example.data.backup.BackupSongUserStateDto
import com.example.data.backup.MikaBackupPayload
import com.example.data.db.MikaDatabase
import com.example.data.db.SongEntity
import com.example.data.db.TrackTagOverrideEntity
import com.example.data.prefs.SettingsDataStore
import com.example.data.repository.MusicRepository
import com.example.media.LyricsParser
import com.example.media.MediaScanner
import com.example.media.SongFallbackArtworkGenerator
import com.example.media.TrackArtworkRequest
import com.example.model.AccentPalette
import com.example.model.AlbumSortField
import com.example.model.AppSettings
import com.example.model.ArtistSortField
import com.example.model.CollectionType
import com.example.model.FolderSortField
import com.example.model.GenreSortField
import com.example.model.MainTab
import com.example.model.PlaylistSortField
import com.example.model.SleepTimerState
import com.example.model.ThemeMode
import com.example.playback.AudioSpectrumAnalyzer
import com.example.ui.theme.mikaTypography
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name is Mika Player`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Mika Player", appName)
    }

    @Test
    fun `exact navigation tab order is preserved`() {
        assertEquals(10, MainTab.entries.size)
        assertFalse(MainTab.entries.any { it.name == "HOME" })
        assertFalse(MainTab.entries.any { it.name == "FOLDERS" })
        assertEquals(
            listOf(
                "Songs",
                "Playlists",
                "Albums",
                "Artists",
                "Genres",
                "Favorites",
                "Most Played",
                "Recent",
                "New Added",
                "History"
            ),
            MainTab.entries.map { it.label }
        )
    }

    @Test
    fun `theme modes and accent colors match spec counts`() {
        assertEquals(3, ThemeMode.entries.size)
        assertEquals(10, AccentPalette.options.size)
    }

    @Test
    fun `lrc parser parses synchronized timestamps accurately`() {
        val lrc = """
            [00:12.50]First line
            [01:05.00]Second line
        """.trimIndent()
        val parsed = LyricsParser.parseRawText(lrc, "Test LRC")
        assertTrue(parsed.isSynced)
        assertEquals(2, parsed.lines.size)
        assertEquals(12_500L, parsed.lines[0].timestampMs)
        assertEquals("First line", parsed.lines[0].text)
        assertEquals(65_000L, parsed.lines[1].timestampMs)
    }

    @Test
    fun `embedded ID3v2 parser finds USLT even after 600KB APIC frame`() {
        val lyricsText = "[00:05.00]Streaming ID3 lyrics\n[00:10.00]Beyond 600KB artwork"
        val apicSize = 600 * 1024
        val usltPayload = ByteArrayOutputStream().apply {
            write(3) // UTF-8
            write("eng".toByteArray(StandardCharsets.US_ASCII))
            write(0) // empty descriptor terminator
            write(lyricsText.toByteArray(StandardCharsets.UTF_8))
        }.toByteArray()

        val tagBodySize = (10 + apicSize) + (10 + usltPayload.size)
        val out = ByteArrayOutputStream()
        // ID3v2.4 header
        out.write("ID3".toByteArray(StandardCharsets.US_ASCII))
        out.write(4)
        out.write(0)
        out.write(0)
        out.write(encodeSyncSafe(tagBodySize))

        // Large APIC frame
        out.write("APIC".toByteArray(StandardCharsets.US_ASCII))
        out.write(encodeSyncSafe(apicSize))
        out.write(0)
        out.write(0)
        out.write(ByteArray(apicSize))

        // USLT frame after APIC
        out.write("USLT".toByteArray(StandardCharsets.US_ASCII))
        out.write(encodeSyncSafe(usltPayload.size))
        out.write(0)
        out.write(0)
        out.write(usltPayload)

        val extracted = LyricsParser.extractEmbeddedLyricsFromStream(
            BufferedInputStream(ByteArrayInputStream(out.toByteArray())),
            "mp3"
        )
        assertEquals(lyricsText, extracted)
    }

    @Test
    fun `embedded ID3v2 parser reads TXXX LYRICS frame`() {
        val lyricsText = "Line one of TXXX\nLine two of TXXX"
        val payload = ByteArrayOutputStream().apply {
            write(3) // UTF-8
            write("LYRICS".toByteArray(StandardCharsets.UTF_8))
            write(0)
            write(lyricsText.toByteArray(StandardCharsets.UTF_8))
        }.toByteArray()

        val tagBodySize = 10 + payload.size
        val out = ByteArrayOutputStream()
        out.write("ID3".toByteArray(StandardCharsets.US_ASCII))
        out.write(3) // ID3v2.3
        out.write(0)
        out.write(0)
        out.write(encodeSyncSafe(tagBodySize))

        out.write("TXXX".toByteArray(StandardCharsets.US_ASCII))
        out.write(encodeBigEndianInt(payload.size))
        out.write(0)
        out.write(0)
        out.write(payload)

        val extracted = LyricsParser.extractEmbeddedLyricsFromStream(
            BufferedInputStream(ByteArrayInputStream(out.toByteArray())),
            "mp3"
        )
        assertEquals(lyricsText, extracted)
    }

    @Test
    fun `embedded FLAC VorbisComment parser reads LYRICS field`() {
        val lyricsText = "FLAC verse line 1\nFLAC verse line 2"
        val vendor = "MikaTest".toByteArray(StandardCharsets.UTF_8)
        val comment = "LYRICS=$lyricsText".toByteArray(StandardCharsets.UTF_8)

        val vorbisBlock = ByteArrayOutputStream().apply {
            write(encodeLittleEndianInt(vendor.size))
            write(vendor)
            write(encodeLittleEndianInt(1))
            write(encodeLittleEndianInt(comment.size))
            write(comment)
        }.toByteArray()

        val flacStream = ByteArrayOutputStream().apply {
            write("fLaC".toByteArray(StandardCharsets.US_ASCII))
            write(0x80 or 4) // last block + VorbisComment type 4
            write((vorbisBlock.size ushr 16) and 0xFF)
            write((vorbisBlock.size ushr 8) and 0xFF)
            write(vorbisBlock.size and 0xFF)
            write(vorbisBlock)
        }.toByteArray()

        val extracted = LyricsParser.extractEmbeddedLyricsFromStream(
            BufferedInputStream(ByteArrayInputStream(flacStream)),
            "flac"
        )
        assertEquals(lyricsText, extracted)
    }

    @Test
    fun `embedded MP4 atom parser reads ©lyr inside moov udta meta ilst`() {
        val lyricsText = "M4A embedded lyric line"
        val dataAtomPayload = ByteArrayOutputStream().apply {
            write(byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0)) // version/type UTF-8 + locale
            write(lyricsText.toByteArray(StandardCharsets.UTF_8))
        }.toByteArray()
        val dataAtom = wrapAtom("data", dataAtomPayload)
        val lyrAtom = wrapAtom("\u00A9lyr", dataAtom)
        val ilstAtom = wrapAtom("ilst", lyrAtom)
        val metaPayload = ByteArrayOutputStream().apply {
            write(byteArrayOf(0, 0, 0, 0)) // FullBox flags
            write(ilstAtom)
        }.toByteArray()
        val metaAtom = wrapAtom("meta", metaPayload)
        val udtaAtom = wrapAtom("udta", metaAtom)
        val moovAtom = wrapAtom("moov", udtaAtom)
        val ftypAtom = wrapAtom("ftyp", "M4A \u0000\u0000\u0000\u0000".toByteArray(StandardCharsets.ISO_8859_1))

        val mp4Bytes = ftypAtom + moovAtom
        val extracted = LyricsParser.extractEmbeddedLyricsFromStream(
            BufferedInputStream(ByteArrayInputStream(mp4Bytes)),
            "m4a"
        )
        assertEquals(lyricsText, extracted)
    }

    @Test
    fun `database migration 1 to 2 adds trash columns cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test.db"
        context.deleteDatabase(dbName)
        val callback = object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS songs (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        )
        val db = helper.writableDatabase
        MikaDatabase.MIGRATION_1_2.migrate(db)
        db.execSQL("INSERT INTO songs (id, title, isTrashed, trashedAt) VALUES (42, 'Song', 1, 9999)")
        val cursor = db.query("SELECT isTrashed, trashedAt FROM songs WHERE id = 42")
        assertNotNull(cursor)
        assertTrue(cursor.moveToFirst())
        assertEquals(1, cursor.getInt(0))
        assertEquals(9999L, cursor.getLong(1))
        cursor.close()
        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `database migration 2 to 3 adds genre column while preserving rows`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_2_3_test.db"
        context.deleteDatabase(dbName)
        val callback = object : SupportSQLiteOpenHelper.Callback(3) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS songs (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL, isTrashed INTEGER NOT NULL DEFAULT 0, trashedAt INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("INSERT INTO songs (id, title, isTrashed, trashedAt) VALUES (7, 'Ambient Drift', 0, 0)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        )
        val db = helper.writableDatabase
        MikaDatabase.MIGRATION_2_3.migrate(db)
        db.execSQL("UPDATE songs SET genre = 'Electronic' WHERE id = 7")
        val cursor = db.query("SELECT title, genre FROM songs WHERE id = 7")
        assertNotNull(cursor)
        assertTrue(cursor.moveToFirst())
        assertEquals("Ambient Drift", cursor.getString(0))
        assertEquals("Electronic", cursor.getString(1))
        cursor.close()
        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `database migration 3 to 4 creates track_tag_overrides table and persists overrides`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_3_4_test.db"
        context.deleteDatabase(dbName)
        val callback = object : SupportSQLiteOpenHelper.Callback(4) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS songs (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        )
        val db = helper.writableDatabase
        MikaDatabase.MIGRATION_3_4.migrate(db)
        db.execSQL(
            """
            INSERT INTO track_tag_overrides (songId, title, artist, album, genre, year, trackNumber, customArtworkPath, artworkCleared, updatedAtMs)
            VALUES (99, 'Custom Title', 'Custom Artist', 'Custom Album', 'Lo-Fi', 2025, 3, '/tmp/cover_99.jpg', 0, 123456)
            """.trimIndent()
        )
        val cursor = db.query("SELECT title, artist, genre, year, trackNumber, customArtworkPath FROM track_tag_overrides WHERE songId = 99")
        assertNotNull(cursor)
        assertTrue(cursor.moveToFirst())
        assertEquals("Custom Title", cursor.getString(0))
        assertEquals("Custom Artist", cursor.getString(1))
        assertEquals("Lo-Fi", cursor.getString(2))
        assertEquals(2025, cursor.getInt(3))
        assertEquals(3, cursor.getInt(4))
        assertEquals("/tmp/cover_99.jpg", cursor.getString(5))
        cursor.close()
        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `fallback artwork generator produces distinct visual signatures per song`() {
        val trackA = TrackArtworkRequest(1L, "content://1", "/a.mp3", 100L, "Alpha Echo", "Artist A")
        val trackB = TrackArtworkRequest(2L, "content://2", "/b.mp3", 100L, "Midnight Pulse", "Artist B")
        val sigA = SongFallbackArtworkGenerator.deriveVisualSignature(trackA)
        val sigB = SongFallbackArtworkGenerator.deriveVisualSignature(trackB)
        assertFalse("Songs with different metadata must not share identical fallback signature", sigA == sigB)
        val bmpA = SongFallbackArtworkGenerator.generate(trackA, 64)
        val bmpB = SongFallbackArtworkGenerator.generate(trackB, 64)
        assertEquals(64, bmpA.width)
        assertEquals(64, bmpB.width)
    }

    @Test
    fun `typography scales cleanly and uses bold main weights`() {
        val normal = mikaTypography(1.0f)
        val large = mikaTypography(1.20f)
        assertEquals(FontWeight.Bold, normal.titleMedium.fontWeight)
        assertEquals(FontWeight.Bold, normal.headlineMedium.fontWeight)
        assertEquals(FontWeight.Bold, normal.bodyLarge.fontWeight)
        assertEquals(FontWeight.SemiBold, normal.bodyMedium.fontWeight)
        assertEquals(FontWeight.SemiBold, normal.bodySmall.fontWeight)
        assertEquals(FontWeight.Bold, normal.labelMedium.fontWeight)
        assertTrue(large.bodyLarge.fontSize.value > normal.bodyLarge.fontSize.value)
    }

    @Test
    fun `app settings defaults include core 4 library tabs, fade out sleep timer, mika backup path, and auto backup Off`() {
        val settings = AppSettings()
        assertEquals(
            listOf(MainTab.SONGS, MainTab.PLAYLISTS, MainTab.ALBUMS, MainTab.ARTISTS),
            settings.enabledTabs
        )
        assertEquals(MainTab.SONGS, settings.defaultTab)
        assertEquals(1.0f, settings.fontScale, 0.001f)
        assertEquals(1.0f, settings.appIconScale, 0.001f)
        assertEquals(16, settings.cornerRadiusDp)
        assertEquals(com.example.model.SeekBarStyle.SLEEK_CAPSULE, settings.seekBarStyle)
        assertEquals(com.example.model.PlaybackIconStyle.NORMAL, settings.playbackIconStyle)
        assertEquals("/mika/backup", settings.backupLocationLabel)
        assertEquals(0, settings.minDurationSec)
        assertEquals(0, settings.minFileSizeKb)
        assertEquals(0, settings.autoBackupIntervalDays)
        assertFalse(settings.allowSimultaneousPlayback)
        assertTrue(settings.obeyNoMedia)
        assertTrue(settings.autoScanOnStartup)
        assertTrue(settings.swipeToOpenDrawer)
        assertTrue(SleepTimerState().fadeOutEnabled)
    }

    @Test
    fun `audio spectrum analyzer maintains 28 bands and resets cleanly`() {
        AudioSpectrumAnalyzer.reset()
        assertEquals(28, AudioSpectrumAnalyzer.BAND_COUNT)
        assertEquals(28, AudioSpectrumAnalyzer.bandsFlow.value.size)

        val signal = FloatArray(AudioSpectrumAnalyzer.BAND_COUNT) { idx ->
            if (idx % 2 == 0) 0.75f else 0.35f
        }
        AudioSpectrumAnalyzer.publishSpectrum(signal, 0.8f)
        assertTrue(AudioSpectrumAnalyzer.bandsFlow.value[0] > 0f)
        assertTrue(AudioSpectrumAnalyzer.beatEnergyFlow.value > 0f)

        AudioSpectrumAnalyzer.reset()
        assertTrue(AudioSpectrumAnalyzer.bandsFlow.value.all { it == 0f })
        assertEquals(0f, AudioSpectrumAnalyzer.beatEnergyFlow.value, 0.0001f)
    }

    @Test
    fun `per-tab sort enums and collection types cover all views`() {
        assertEquals(5, PlaylistSortField.entries.size)
        assertEquals(4, FolderSortField.entries.size)
        assertEquals(4, AlbumSortField.entries.size)
        assertEquals(3, ArtistSortField.entries.size)
        assertEquals(3, GenreSortField.entries.size)
        assertEquals(
            setOf("FAVORITES", "LYRICS", "RECENTLY_PLAYED", "MOST_PLAYED", "RECENTLY_ADDED"),
            CollectionType.entries.map { it.name }.toSet()
        )
    }

    @Test
    fun `backup manager round trip creates local snapshot and restores user data`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, MikaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.mikaDao()
        val dataStore = SettingsDataStore(context)

        // Clean any old backup files first
        BackupManager.getBackupDirectory(context).listFiles()?.forEach { File(it.absolutePath).delete() }

        dao.upsertSongs(
            listOf(
                SongEntity(
                    id = 101L,
                    title = "Night Drive",
                    artist = "Mika Artist",
                    album = "Midnight",
                    albumId = 10L,
                    artistId = 20L,
                    durationMs = 180_000L,
                    contentUri = "content://media/external/audio/media/101",
                    filePath = "/storage/emulated/0/Music/night_drive.mp3",
                    folderPath = "/storage/emulated/0/Music",
                    folderName = "Music",
                    mimeType = "audio/mpeg",
                    sizeBytes = 4_000_000L,
                    dateAddedSec = 1_700_000_000L,
                    dateModifiedSec = 1_700_000_000L,
                    genre = "Synthwave",
                    playCount = 0,
                    isFavorite = false
                )
            )
        )

        val payload = MikaBackupPayload(
            settings = BackupSettingsDto(
                accentColorId = "custom",
                customAccentArgb = 0xFFFF4081L,
                autoColoring = true,
                enabledTabs = listOf(
                    MainTab.FAVORITES.name,
                    MainTab.SONGS.name,
                    MainTab.GENRES.name,
                    MainTab.PLAYLISTS.name
                ),
                defaultTab = MainTab.FAVORITES.name,
                appIconScale = 1.15f,
                seekBarStyle = com.example.model.SeekBarStyle.WAVEFORM_BARS.name,
                autoBackupIntervalDays = 7,
                allowSimultaneousPlayback = true
            ),
            songUserStates = listOf(
                BackupSongUserStateDto(
                    songId = 101L,
                    filePath = "/storage/emulated/0/Music/night_drive.mp3",
                    title = "Night Drive",
                    artist = "Mika Artist",
                    playCount = 5,
                    lastPlayedAt = 1_700_005_000L,
                    isFavorite = true,
                    hasLyrics = true
                )
            ),
            playlists = listOf(
                BackupPlaylistDto(
                    name = "Late Night Mix",
                    createdAt = 1_700_001_000L,
                    tracks = listOf(
                        BackupPlaylistTrackRefDto(
                            songId = 101L,
                            filePath = "/storage/emulated/0/Music/night_drive.mp3",
                            title = "Night Drive"
                        )
                    )
                )
            )
        )

        val summary = BackupManager.restorePayload(payload, dao, dataStore)
        assertEquals(1, summary.restoredPlaylists)
        assertEquals(1, summary.restoredSongStates)

        val restoredSettings = dataStore.settingsFlow.first()
        assertEquals("custom", restoredSettings.accentColorId)
        assertEquals(0xFFFF4081L, restoredSettings.customAccentArgb)
        assertTrue(restoredSettings.autoColoring)
        assertEquals(
            listOf(MainTab.FAVORITES, MainTab.SONGS, MainTab.GENRES, MainTab.PLAYLISTS),
            restoredSettings.enabledTabs
        )
        assertEquals(MainTab.FAVORITES, restoredSettings.defaultTab)
        assertEquals(1.15f, restoredSettings.appIconScale, 0.001f)
        assertEquals(com.example.model.SeekBarStyle.WAVEFORM_BARS, restoredSettings.seekBarStyle)
        assertEquals(7, restoredSettings.autoBackupIntervalDays)
        assertTrue(restoredSettings.allowSimultaneousPlayback)

        val restoredSongs = dao.getAllSongsOnce()
        assertTrue(restoredSongs.first { it.id == 101L }.isFavorite)
        assertEquals(5, restoredSongs.first { it.id == 101L }.playCount)

        val snapshot = BackupManager.createInternalBackup(context, dao, dataStore)
        assertTrue(snapshot.file.exists())
        val parsed = BackupManager.parseFromFile(snapshot.file)
        assertEquals("Mika Player", parsed.appName)
        assertEquals(1, parsed.playlists.size)
        assertEquals("Late Night Mix", parsed.playlists.first().name)
        assertEquals(0xFFFF4081L, parsed.settings.customAccentArgb)
        assertTrue(parsed.settings.autoColoring)
        assertTrue(BackupManager.listLocalBackups(context).any { it.name == snapshot.name })

        snapshot.file.delete()
        db.close()
    }

    @Test
    fun `settings datastore persists custom tab ordering, custom color, and auto coloring`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = SettingsDataStore(context)
        val orderedTabs = listOf(MainTab.GENRES, MainTab.SONGS, MainTab.MOST_PLAYED, MainTab.PLAYLISTS)

        dataStore.updateEnabledTabs(orderedTabs)
        dataStore.updateDefaultTab(MainTab.GENRES)
        dataStore.updateCustomAccentColor(0xFF00E5FFL)
        dataStore.updateAutoColoring(true)
        dataStore.updateAppIconScale(1.22f)
        dataStore.updateSeekBarStyle(com.example.model.SeekBarStyle.WAVY_PULSE)

        val updated = dataStore.settingsFlow.first()
        assertEquals(orderedTabs, updated.enabledTabs)
        assertEquals(MainTab.GENRES, updated.defaultTab)
        assertEquals("custom", updated.accentColorId)
        assertEquals(0xFF00E5FFL, updated.customAccentArgb)
        assertTrue(updated.autoColoring)
        assertEquals(1.22f, updated.appIconScale, 0.001f)
        assertEquals(com.example.model.SeekBarStyle.WAVY_PULSE, updated.seekBarStyle)

        val resolved = AccentPalette.resolve(updated.accentColorId, updated.customAccentArgb)
        assertEquals("Custom Color", resolved.name)
    }

    @Test
    fun `seek bar styles cover all 7 presets with distinct labels`() {
        val styles = com.example.model.SeekBarStyle.entries
        assertEquals(7, styles.size)
        assertEquals(
            setOf(
                "SLEEK_CAPSULE",
                "WAVEFORM_BARS",
                "REAL_WAVEFORM",
                "LIVE_FLOW_WAVEFORM",
                "WAVY_PULSE",
                "MINIMAL_GLOW",
                "TICK_RULER"
            ),
            styles.map { it.name }.toSet()
        )
        assertEquals(7, styles.map { it.label }.toSet().size)
    }

    @Test
    fun `application id matches dev sagor hsnmse and version is 1_3`() {
        assertEquals("dev.sagor.hsnmse", BuildConfig.APPLICATION_ID)
        assertEquals("1.3", BuildConfig.VERSION_NAME)
    }

    @Test
    fun `database migration 4 to 5 creates performance indices and passes Room schema validation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_4_5_test.db"
        context.deleteDatabase(dbName)

        val callback = object : SupportSQLiteOpenHelper.Callback(4) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `songs` (
                        `id` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `albumId` INTEGER NOT NULL,
                        `artistId` INTEGER NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `contentUri` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `folderPath` TEXT NOT NULL,
                        `folderName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `dateAddedSec` INTEGER NOT NULL,
                        `dateModifiedSec` INTEGER NOT NULL,
                        `trackNumber` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL,
                        `genre` TEXT NOT NULL,
                        `playCount` INTEGER NOT NULL,
                        `lastPlayedAt` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `hasLyrics` INTEGER NOT NULL,
                        `isTrashed` INTEGER NOT NULL,
                        `trashedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_songs_contentUri` ON `songs` (`contentUri`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_albumId` ON `songs` (`albumId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_artistId` ON `songs` (`artistId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_folderPath` ON `songs` (`folderPath`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playlists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `lastPlayedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playlist_songs` (
                        `playlistId` INTEGER NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`playlistId`, `songId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId` ON `playlist_songs` (`playlistId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_songId` ON `playlist_songs` (`songId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `play_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `playedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE TABLE IF NOT EXISTS `search_history` (`query` TEXT NOT NULL, `searchedAt` INTEGER NOT NULL, PRIMARY KEY(`query`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `saved_queue` (`positionIndex` INTEGER NOT NULL, `songId` INTEGER NOT NULL, PRIMARY KEY(`positionIndex`))")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `track_tag_overrides` (
                        `songId` INTEGER NOT NULL,
                        `title` TEXT,
                        `artist` TEXT,
                        `album` TEXT,
                        `genre` TEXT,
                        `year` INTEGER,
                        `trackNumber` INTEGER,
                        `customArtworkPath` TEXT,
                        `artworkCleared` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`songId`)
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }

        val v4Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        )
        v4Helper.writableDatabase
        v4Helper.close()

        val roomDb = androidx.room.Room.databaseBuilder(context, MikaDatabase::class.java, dbName)
            .addMigrations(MikaDatabase.MIGRATION_4_5, MikaDatabase.MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = roomDb.mikaDao()
            dao.upsertSongs(
                listOf(
                    SongEntity(
                        id = 55L,
                        title = "Indexed Track",
                        artist = "Mika",
                        album = "Vault",
                        albumId = 1L,
                        artistId = 1L,
                        durationMs = 120_000L,
                        contentUri = "content://media/external/audio/media/55",
                        filePath = "/music/indexed.mp3",
                        folderPath = "/music",
                        folderName = "music",
                        mimeType = "audio/mpeg",
                        sizeBytes = 1024L,
                        dateAddedSec = 100L,
                        dateModifiedSec = 100L,
                        isFavorite = true
                    )
                )
            )
            assertEquals(1, dao.getAllSongsOnce().size)

            val sqliteDb = roomDb.openHelper.writableDatabase
            val indexNames = mutableSetOf<String>()
            sqliteDb.query("SELECT name FROM sqlite_master WHERE type='index'").use { cursor ->
                while (cursor.moveToNext()) {
                    indexNames += cursor.getString(0)
                }
            }
            assertTrue(
                indexNames.containsAll(
                    listOf(
                        "index_songs_isTrashed",
                        "index_songs_isHidden",
                        "index_songs_isFavorite",
                        "index_songs_lastPlayedAt",
                        "index_songs_playCount",
                        "index_songs_dateAddedSec",
                        "index_play_history_playedAt"
                    )
                )
            )
        } finally {
            roomDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `loadSavedQueue preserves custom tag overrides`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, MikaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.mikaDao()
            val repo = MusicRepository(context, dao, SettingsDataStore(context))
            dao.upsertSongs(
                listOf(
                    SongEntity(
                        id = 202L,
                        title = "Raw Title",
                        artist = "Raw Artist",
                        album = "Raw Album",
                        albumId = 2L,
                        artistId = 3L,
                        durationMs = 210_000L,
                        contentUri = "content://media/external/audio/media/202",
                        filePath = "/music/track_202.mp3",
                        folderPath = "/music",
                        folderName = "music",
                        mimeType = "audio/mpeg",
                        sizeBytes = 2048L,
                        dateAddedSec = 500L,
                        dateModifiedSec = 500L
                    )
                )
            )
            dao.upsertTagOverrides(
                listOf(
                    TrackTagOverrideEntity(
                        songId = 202L,
                        title = "Edited Title",
                        artist = "Edited Artist",
                        album = "Edited Album",
                        updatedAtMs = 900_000L
                    )
                )
            )
            dao.saveQueue(listOf(202L))

            val restoredQueue = repo.loadSavedQueue()
            assertEquals(1, restoredQueue.size)
            assertEquals("Edited Title", restoredQueue.first().title)
            assertEquals("Edited Artist", restoredQueue.first().artist)
            assertEquals("Edited Album", restoredQueue.first().album)
        } finally {
            db.close()
        }
    }

    @Test
    fun `bottom navigation destinations cover Home Folders Library and Videos`() {
        assertEquals(
            listOf("HOME", "FOLDERS", "LIBRARY", "VIDEOS"),
            com.example.model.BottomNavDest.entries.map { it.name }
        )
        assertTrue(AppSettings().showBottomNavBar)
    }

    @Test
    fun `settings datastore persists optional bottom navigation bar toggle`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = SettingsDataStore(context)

        dataStore.updateShowBottomNavBar(false)
        assertFalse(dataStore.settingsFlow.first().showBottomNavBar)

        dataStore.updateShowBottomNavBar(true)
        assertTrue(dataStore.settingsFlow.first().showBottomNavBar)
    }

    @Test
    fun `media and video scanners support wide range of formats`() {
        assertTrue(
            MediaScanner.supportedExtensions.containsAll(
                listOf("mp3", "m4a", "flac", "opus", "alac", "aiff", "wav", "dsf", "ogg", "wma")
            )
        )
        assertTrue(
            com.example.media.VideoScanner.videoExtensions.containsAll(
                listOf("mp4", "mkv", "webm", "mov", "avi", "m4v", "ts")
            )
        )
    }

    @Test
    fun `lyrics parser studio helpers format shift strip and stamp timestamps cleanly`() {
        assertEquals("[01:05.50]", LyricsParser.formatLrcTimestamp(65_500L))

        val raw = "[00:10.00]Line one\nLine two"
        val stamped = LyricsParser.stampLineTimestamp(raw, 1, 14_250L)
        assertEquals("[00:10.00]Line one\n[00:14.25]Line two", stamped)

        val shifted = LyricsParser.shiftLrcTimestamps(stamped, -500L)
        assertEquals("[00:09.50]Line one\n[00:13.75]Line two", shifted)

        val stripped = LyricsParser.stripLrcTimestamps(shifted)
        assertEquals("Line one\nLine two", stripped)
    }

    @Test
    fun `manifest avoids restricted READ_MEDIA_VIDEO and READ_MEDIA_IMAGES permissions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = pkgInfo.requestedPermissions?.toSet().orEmpty()
        assertFalse(android.Manifest.permission.READ_MEDIA_VIDEO in requested)
        assertFalse(android.Manifest.permission.READ_MEDIA_IMAGES in requested)
    }

    @Test
    fun `customization defaults, widget config, and gesture enums cover all screenshot options`() {
        val defaults = AppSettings()
        assertTrue(defaults.enableFoldersHierarchy)
        assertTrue(defaults.skipSingleSubfolder)
        assertFalse(defaults.skipSilence)
        assertEquals(com.example.model.CrossfadeStyle.DEFAULT, defaults.crossfadeStyle)
        assertEquals(com.example.model.BackgroundEffect.NONE, defaults.backgroundEffect)
        assertEquals(com.example.model.PlayerBackgroundMode.MESH_GRADIENT, defaults.playerBackgroundMode)
        assertEquals(com.example.model.ArtworkGestureAction.ARTWORK_OPEN, defaults.artworkTapAction)
        assertEquals(com.example.model.ArtworkGestureAction.INFO, defaults.artworkLongPressAction)
        assertEquals(com.example.model.WidgetPreviewSize.WIDE, defaults.widgetConfig.previewSize)
        assertEquals(9, com.example.model.ArtworkGestureAction.entries.size)
        assertEquals(8, com.example.model.BackgroundEffect.entries.size)
        assertEquals(6, com.example.model.PlayerBackgroundMode.entries.size)
    }

    @Test
    fun `settings datastore and backup manager persist extended customization and widget config`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = SettingsDataStore(context)
        val widgetCfg = com.example.model.WidgetConfig(
            previewSize = com.example.model.WidgetPreviewSize.LARGE,
            theme = com.example.model.WidgetThemeMode.DARK,
            backgroundStyle = com.example.model.WidgetBackgroundStyle.BLURRED_ART,
            artworkEffect = com.example.model.WidgetArtworkEffect.GLOW,
            backgroundOpacity = 0.72f,
            cornerRadiusDp = 18
        )

        dataStore.updateFoldersHierarchy(enableHierarchy = false, skipSingleSubfolder = false)
        dataStore.updateSkipSilence(true)
        dataStore.updateCrossfadeEnabled(true)
        dataStore.updateCrossfadeDurationMs(2400)
        dataStore.updateCrossfadeTriggerLastSec(8)
        dataStore.updateCrossfadeStyle(com.example.model.CrossfadeStyle.EQUAL_POWER)
        dataStore.updateUsePitchBlack(true)
        dataStore.updateForceMiniPlayerTrackColors(true)
        dataStore.updateVisualEffects(blur = true, glow = false, parallax = true, backgroundEffect = com.example.model.BackgroundEffect.SAKURA)
        dataStore.updateArtworkGestures(
            doubleTapToToggleLyrics = false,
            tapAction = com.example.model.ArtworkGestureAction.PLAY_PAUSE,
            longPressAction = com.example.model.ArtworkGestureAction.SHARE
        )
        dataStore.updateCornerRadiusDp(22)
        dataStore.updatePlaybackIconStyle(com.example.model.PlaybackIconStyle.BROKEN)
        dataStore.updateExtensionBlacklist(setOf(".amr", "mid"))
        dataStore.updateColorTargets(onApp = false, onPlayer = true)
        dataStore.updateWidgetConfig(widgetCfg)

        val saved = dataStore.settingsFlow.first()
        assertFalse(saved.enableFoldersHierarchy)
        assertFalse(saved.skipSingleSubfolder)
        assertTrue(saved.skipSilence)
        assertTrue(saved.crossfadeEnabled)
        assertEquals(2400, saved.crossfadeDurationMs)
        assertEquals(8, saved.crossfadeTriggerLastSec)
        assertEquals(com.example.model.CrossfadeStyle.EQUAL_POWER, saved.crossfadeStyle)
        assertTrue(saved.usePitchBlack)
        assertTrue(saved.forceMiniPlayerTrackColors)
        assertTrue(saved.enableBlurEffect)
        assertFalse(saved.enableGlowEffect)
        assertTrue(saved.enableParallaxEffect)
        assertEquals(com.example.model.BackgroundEffect.SAKURA, saved.backgroundEffect)
        assertFalse(saved.doubleTapToToggleLyrics)
        assertEquals(com.example.model.ArtworkGestureAction.PLAY_PAUSE, saved.artworkTapAction)
        assertEquals(com.example.model.ArtworkGestureAction.SHARE, saved.artworkLongPressAction)
        assertEquals(22, saved.cornerRadiusDp)
        assertEquals(com.example.model.PlaybackIconStyle.BROKEN, saved.playbackIconStyle)
        assertEquals(setOf("amr", "mid"), saved.extensionBlacklist)
        assertFalse(saved.applyColorOnApp)
        assertTrue(saved.applyColorOnPlayer)
        assertEquals(widgetCfg, saved.widgetConfig)
    }

    @Test
    fun `database migration 6 to 7 adds mood and rating columns cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_6_7_test.db"
        context.deleteDatabase(dbName)
        val callback = object : SupportSQLiteOpenHelper.Callback(6) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS songs (id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL)")
                db.execSQL("INSERT INTO songs (id, title) VALUES (11, 'Evening Breeze')")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        )
        val db = helper.writableDatabase
        MikaDatabase.MIGRATION_6_7.migrate(db)
        db.execSQL("UPDATE songs SET mood = 'Chill', rating = 5 WHERE id = 11")
        val cursor = db.query("SELECT title, mood, rating FROM songs WHERE id = 11")
        assertNotNull(cursor)
        assertTrue(cursor.moveToFirst())
        assertEquals("Evening Breeze", cursor.getString(0))
        assertEquals("Chill", cursor.getString(1))
        assertEquals(5, cursor.getInt(2))
        cursor.close()
        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `repository detects duplicate tracks and persists mood and rating`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, MikaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.mikaDao()
            val repo = MusicRepository(context, dao, SettingsDataStore(context))
            dao.upsertSongs(
                listOf(
                    SongEntity(
                        id = 1L,
                        title = "Same Track",
                        artist = "Same Artist",
                        album = "Album",
                        albumId = 1L,
                        artistId = 1L,
                        durationMs = 200_000L,
                        contentUri = "content://media/external/audio/media/1",
                        filePath = "/music/a/same_track.mp3",
                        folderPath = "/music/a",
                        folderName = "a",
                        mimeType = "audio/mpeg",
                        sizeBytes = 5_000_000L,
                        dateAddedSec = 100L,
                        dateModifiedSec = 100L
                    ),
                    SongEntity(
                        id = 2L,
                        title = "Same Track",
                        artist = "Same Artist",
                        album = "Album",
                        albumId = 1L,
                        artistId = 1L,
                        durationMs = 200_200L,
                        contentUri = "content://media/external/audio/media/2",
                        filePath = "/music/b/same_track_copy.mp3",
                        folderPath = "/music/b",
                        folderName = "b",
                        mimeType = "audio/mpeg",
                        sizeBytes = 5_000_000L,
                        dateAddedSec = 200L,
                        dateModifiedSec = 200L
                    )
                )
            )
            repo.setSongMood(1L, "Late Night")
            repo.setSongRating(1L, 4)

            val updated = dao.getSongById(1L)
            assertNotNull(updated)
            assertEquals("Late Night", updated?.mood)
            assertEquals(4, updated?.rating)

            val duplicates = repo.findDuplicateGroups()
            assertEquals(1, duplicates.size)
            assertEquals(2, duplicates.first().songs.size)
            assertTrue(duplicates.first().reclaimableBytes >= 5_000_000L)
        } finally {
            db.close()
        }
    }

    private fun encodeSyncSafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte()
    )

    private fun encodeBigEndianInt(value: Int): ByteArray = byteArrayOf(
        ((value ushr 24) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte()
    )

    private fun encodeLittleEndianInt(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte()
    )

    private fun wrapAtom(type: String, payload: ByteArray): ByteArray {
        val size = 8 + payload.size
        return ByteArrayOutputStream().apply {
            write(encodeBigEndianInt(size))
            write(type.toByteArray(StandardCharsets.ISO_8859_1))
            write(payload)
        }.toByteArray()
    }
}

