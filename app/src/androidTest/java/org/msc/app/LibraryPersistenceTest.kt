package org.msc.app

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.msc.app.data.*

@RunWith(AndroidJUnit4::class)
class LibraryPersistenceTest {
    @Test fun realAudioMetadataFavoritesPlaylistAndLyricsSurviveReopen() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "msc-test-${System.nanoTime()}.db"
        val file = java.io.File(context.cacheDir, "msc-library-fixture.wav")
        InstrumentationRegistry.getInstrumentation().context.assets.open("tone-a.wav").use { input -> file.outputStream().use(input::copyTo) }
        var repository = LibraryRepository(context, MusicDatabase(context, name))
        try {
            val track = repository.importUri(Uri.fromFile(file))
            repository.importUri(Uri.fromFile(file)); repository.refresh()
            assertEquals(1, repository.tracks.value.size)
            assertTrue(track.durationMs in 2900..3100)
            repository.toggleFavorite(track.uri); repository.recordPlayed(track.uri)
            val id = repository.createPlaylist(" Evening ")
            repository.setPlaylistTracks(id, listOf(track.uri, track.uri))
            repository.renamePlaylist(id, "Night")
            repository.saveLyrics(track.uri, "[00:00]Original test line", 250)
            repository.close(); repository = LibraryRepository(context, MusicDatabase(context, name)); repository.refresh()
            assertTrue(repository.tracks.value.single().favorite)
            assertEquals(1L, repository.tracks.value.single().playCount)
            assertEquals("Night", repository.playlists.value.single().name)
            assertEquals(listOf(track.uri), repository.playlists.value.single().tracks)
            assertEquals("[00:00]Original test line" to 250L, repository.lyrics(track.uri))
            repository.deletePlaylist(id); assertTrue(repository.playlists.value.isEmpty())
        } finally { repository.close(); context.deleteDatabase(name); file.delete() }
    }
}
