package org.msc.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.msc.app.playback.mediaItem
import org.msc.app.playback.track
import org.msc.app.providers.JioSaavn
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CancellationException

@RunWith(AndroidJUnit4::class)
class JioSaavnTest {
    private val fixture = """{"success":true,"data":{"total":1,"start":0,"results":[{
        "id":"original-fixture","name":"Vinyl &amp; Rain","duration":3,"album":{"name":"msc Tests"},
        "artists":{"primary":[{"name":"msc"}]},"image":[{"quality":"500x500","url":"https://c.saavncdn.com/test.jpg"}],
        "downloadUrl":[{"quality":"320kbps","url":"http://aac.saavncdn.com/insecure.mp4"},
            {"quality":"160kbps","url":"https://aac.saavncdn.com/original_160.mp4"},
            {"quality":"96kbps","url":"https://aac.saavncdn.com/original_96.mp4"}]}]}}"""

    @Test fun parsesContractChoosesSafeStreamAndPreservesSessionMetadata() {
        val song = JioSaavn().parse(fixture).single()
        assertEquals("Vinyl & Rain", song.title)
        assertEquals("160kbps", song.quality)
        assertEquals(3000L, song.durationMs)
        val track = song.track()!!
        val restored = track.mediaItem().track()
        assertEquals(track.uri, restored.uri); assertEquals("jiosaavn", restored.source)
        assertEquals(song.id, restored.fileName); assertEquals(track.artworkUri, restored.artworkUri)
        assertEquals(track.durationMs, restored.durationMs)
    }
    @Test fun unavailableSongsNeverAcquireFabricatedPlayableUrls() {
        val song = JioSaavn().parse("""{"success":true,"data":{"results":[{"id":"none","name":"Unavailable","downloadUrl":[]}]}}""").single()
        assertNull(song.streamUrl); assertNull(song.track())
        assertThrows(IllegalStateException::class.java) { JioSaavn().parse("""{"success":false,"data":null}""") }
    }
    @Test fun rejectsUntrustedStreamAndArtworkDestinations() {
        listOf("http://aac.saavncdn.com/a", "https://saavncdn.com.attacker.invalid/a", "https://user:password@aac.saavncdn.com/a",
            "file:///storage/emulated/0/Music/a", "https://127.0.0.1/a", "https://aac.saavncdn.com:8443/a", "not a URL").forEach {
            assertNull(it, JioSaavn.trustedCdnUrl(it))
        }
        assertNotNull(JioSaavn.trustedCdnUrl("https://aac.saavncdn.com/a.mp4?token=test"))
    }
    @Test fun realHttpSearchEncodesQueryAndHandlesRateLimitAndCancellation() {
        FixtureHttpServer(fixture.toByteArray(), "application/json").use { server ->
            var requested = ""
            val client = JioSaavn { address -> requested = address; URI(server.url).toURL().openConnection() as HttpURLConnection }
            assertEquals(1, client.search("a & b").size)
            assertTrue(requested.contains("query=a+%26+b")); assertEquals(1, server.requests.get())
            assertThrows(CancellationException::class.java) { client.search("cancel me") { true } }
            assertEquals(1, server.requests.get())
        }
        FixtureHttpServer("{}".toByteArray(), "application/json", 429).use { server ->
            val client = JioSaavn { URI(server.url).toURL().openConnection() as HttpURLConnection }
            val error = assertThrows(IllegalStateException::class.java) { client.search("busy") }
            assertTrue(error.message.orEmpty().contains("busy"))
        }
    }
}
