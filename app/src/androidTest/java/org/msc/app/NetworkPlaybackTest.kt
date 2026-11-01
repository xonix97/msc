package org.msc.app

import android.content.ComponentName
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.msc.app.data.Track
import org.msc.app.playback.*
import java.util.concurrent.TimeUnit

@UnstableApi
@RunWith(AndroidJUnit4::class)
class NetworkPlaybackTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun realHttpAudioUsesMediaSessionSeeksAndContinuesInBackground() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val audio = instrumentation.context.assets.open("tone-a.m4a").use { it.readBytes() }
        val app = compose.activity.application
        FixtureHttpServer(audio, "audio/mp4").use { server ->
            val track = Track(server.url, "Original HTTP fixture", "msc", "Network tests", 15000, source = "jiosaavn")
            val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync()
            val controller = future.get(30, TimeUnit.SECONDS)
            fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
            fun await(label: String, check: () -> Boolean) {
                val deadline = android.os.SystemClock.elapsedRealtime() + 30000
                var done = false
                var diagnostic = ""
                while (!done && android.os.SystemClock.elapsedRealtime() < deadline) {
                    main {
                        assertNull(controller.playerError); done = check()
                        diagnostic = "position=${controller.currentPosition}, duration=${controller.duration}, playing=${controller.isPlaying}, state=${controller.playbackState}"
                    }
                    if (!done) Thread.sleep(100)
                }
                assertTrue("$label ($diagnostic)", done)
            }
            try {
                main { controller.setMediaItem(track.mediaItem()); controller.repeatMode = Player.REPEAT_MODE_ONE; controller.prepare(); controller.play() }
                await("HTTP audio decodes and advances") { controller.isPlaying && controller.currentPosition >= 300 }
                assertTrue(server.requests.get() > 0)
                main { assertEquals("jiosaavn", controller.currentMediaItem?.track()?.source); controller.pause() }
                await("stream pauses") { !controller.isPlaying }
                main { controller.seekTo(1000) }
                await("stream seeks") { controller.currentPosition in 900..1200 }
                main { controller.play() }
                compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
                var backgroundPosition = 0L
                main { assertTrue("Playback remains active after backgrounding", controller.isPlaying); backgroundPosition = controller.currentPosition }
                await("HTTP music advances while Activity is stopped") {
                    controller.isPlaying && ((controller.currentPosition - backgroundPosition + controller.duration) % controller.duration.coerceAtLeast(1)) >= 400
                }
                assertFalse(PlaybackStore(app).read().uris.any { it.startsWith("http") })
            } finally {
                main { controller.stop(); controller.clearMediaItems(); controller.repeatMode = Player.REPEAT_MODE_OFF; MediaController.releaseFuture(future) }
                compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            }
        }
    }
}
