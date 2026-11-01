package org.msc.app

import org.junit.Assert.*
import org.junit.Test
import org.msc.app.data.*

class LibraryRulesTest {
    private val tracks = listOf(
        Track("content://one", "Écho", "Ana", "Night", durationMs = 3000),
        Track("content://two", "Dawn", "Béa", "Morning", durationMs = 1000),
    )
    @Test fun searchMatchesAllWordsAcrossMetadataWithoutAccents() {
        assertEquals(listOf(tracks[0]), LibraryRules.search(tracks, "ECHO ana"))
        assertTrue(LibraryRules.search(tracks, "echo morning").isEmpty())
    }
    @Test fun sortingUsesMetadataAndActualDuration() {
        assertEquals(listOf(tracks[1], tracks[0]), LibraryRules.sorted(tracks, TrackSort.TITLE))
        assertEquals(listOf(tracks[1], tracks[0]), LibraryRules.sorted(tracks, TrackSort.DURATION))
        assertEquals(tracks, LibraryRules.sorted(tracks, TrackSort.ARTIST))
    }
    @Test fun playlistNamesAreTrimmedAndValidated() {
        assertEquals("Evening", LibraryRules.playlistName("  Evening  "))
        assertThrows(IllegalArgumentException::class.java) { LibraryRules.playlistName(" ") }
        assertThrows(IllegalArgumentException::class.java) { LibraryRules.playlistName("x".repeat(81)) }
    }
}
