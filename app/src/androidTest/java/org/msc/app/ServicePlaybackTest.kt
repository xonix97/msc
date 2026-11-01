package org.msc.app

import android.content.ComponentName
import android.net.Uri
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.msc.app.playback.*
import java.util.concurrent.TimeUnit

@UnstableApi
@RunWith(AndroidJUnit4::class)
class ServicePlaybackTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun serviceDecodesPausesSeeksKeepsSessionWhenDiscChangesAndPlaysInBackground() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = compose.activity.application as MscApplication
        val files = listOf("tone-a.wav", "tone-b.wav").map { name ->
            java.io.File(app.cacheDir, "service-$name").also { file -> instrumentation.context.assets.open(name).use { input -> file.outputStream().use(input::copyTo) } }
        }
        val tracks = files.map { app.library.importUri(Uri.fromFile(it)) }; app.library.refresh()
        val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java))).buildAsync()
        val controller = future.get(30, TimeUnit.SECONDS)
        fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
        fun await(description: String, check: () -> Boolean) {
            val deadline = android.os.SystemClock.elapsedRealtime() + 30000
            var done = false
            while (!done && android.os.SystemClock.elapsedRealtime() < deadline) {
                main { assertNull("Player error during $description", controller.playerError); done = check() }
                if (!done) Thread.sleep(100)
            }
            assertTrue(description, done)
        }
        try {
            main { controller.setMediaItems(tracks.map { it.mediaItem() }); controller.prepare(); controller.play() }
            await("real service playback advances") { controller.isPlaying && controller.currentPosition >= 400 }
            main { controller.pause() }
            await("pause reaches session") { !controller.isPlaying }
            main { controller.seekTo(1000) }
            await("seek reaches actual player") { controller.currentPosition in 900..1200 }
            compose.onNodeWithText("Player").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("CD").performClick()
            main { assertEquals(tracks.first().uri, controller.currentMediaItem?.mediaId); assertFalse(controller.playWhenReady); assertTrue(controller.currentPosition in 900..1200) }
            compose.onNodeWithText("Vinyl").performClick()
            main {
                controller.moveMediaItem(1, 0); assertEquals(tracks[1].uri, controller.getMediaItemAt(0).mediaId)
                controller.shuffleModeEnabled = true; controller.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                controller.seekTo(0, 0); controller.play()
            }
            await("second track plays") { controller.currentMediaItem?.mediaId == tracks[1].uri && controller.currentPosition >= 300 }
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            await("background playback continues") { controller.isPlaying && controller.currentPosition >= 800 }
            main { controller.pause() }
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        } finally {
            main { controller.stop(); controller.clearMediaItems() }
            main { MediaController.releaseFuture(future) }
            tracks.forEach { app.library.removeFromLibrary(it.uri) }; files.forEach { it.delete() }
        }
    }
}
