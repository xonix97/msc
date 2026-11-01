package org.msc.app

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real decoder smoke test. Advancing media time is not evidence of acoustic output. */
@RunWith(AndroidJUnit4::class)
class PlaybackFixtureTest {
    @Test fun decodesOriginalPcmAndAdvancesMediaTime() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val fixture = java.io.File(context.cacheDir, "tone-a.wav")
        InstrumentationRegistry.getInstrumentation().context.assets.open("tone-a.wav").use { source ->
            fixture.outputStream().use { source.copyTo(it) }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var player: ExoPlayer
        instrumentation.runOnMainSync { player = ExoPlayer.Builder(context).build() }
        try {
            instrumentation.runOnMainSync {
                player.setMediaItem(MediaItem.fromUri(Uri.fromFile(fixture)))
                player.prepare()
                player.play()
            }
            val deadline = android.os.SystemClock.elapsedRealtime() + 30000
            var position = 0L
            while (position < 500 && android.os.SystemClock.elapsedRealtime() < deadline) {
                instrumentation.runOnMainSync {
                    assertNull("Decoder failed", player.playerError)
                    position = player.currentPosition
                }
                if (position < 500) Thread.sleep(100)
            }
            assertTrue("Media position did not advance", position >= 500)
            instrumentation.runOnMainSync {
                player.pause()
                assertFalse(player.playWhenReady)
                assertTrue(player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_ENDED)
            }
        } finally {
            instrumentation.runOnMainSync { player.release() }
            fixture.delete()
        }
    }
}
