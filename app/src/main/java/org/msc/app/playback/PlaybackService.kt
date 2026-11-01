package org.msc.app.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import org.msc.app.MainActivity
import org.msc.app.MscApplication

@UnstableApi
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession
    private lateinit var store: PlaybackStore
    private lateinit var effects: AudioEffects
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var restoring = true
    private var recordedUri: String? = null
    override fun onCreate() {
        super.onCreate()
        store = PlaybackStore(this); effects = AudioEffects(this)
        player = ExoPlayer.Builder(this).setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true).setHandleAudioBecomingNoisy(true).build()
        player.setWakeMode(C.WAKE_MODE_LOCAL)
        session = MediaSession.Builder(this, player).setSessionActivity(PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(s: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    val result = super.onConnect(s, controller)
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(s)
                        .setAvailableSessionCommands(result.availableSessionCommands.buildUpon()
                            .add(SessionCommand(GET_EFFECTS, Bundle.EMPTY)).add(SessionCommand(SET_EFFECTS, Bundle.EMPTY)).build()).build()
                }
                override fun onCustomCommand(s: MediaSession, controller: MediaSession.ControllerInfo, command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                    return when (command.customAction) {
                        GET_EFFECTS -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, effects.snapshot()))
                        SET_EFFECTS -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, effects.configure(args)))
                        else -> super.onCustomCommand(s, controller, command, args)
                    }
                }
            }).build()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val network = mediaItem?.localConfiguration?.uri?.scheme in listOf("https", "http")
                player.setWakeMode(if (network) C.WAKE_MODE_NETWORK else C.WAKE_MODE_LOCAL)
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) { effects.attach(audioSessionId) }
            override fun onEvents(p: Player, events: Player.Events) {
                if (!restoring) persist()
                if (p.isPlaying && p.currentMediaItem?.mediaId != recordedUri) {
                    recordedUri = p.currentMediaItem?.mediaId
                    recordedUri?.let { uri -> scope.launch(Dispatchers.IO) { (application as MscApplication).library.recordPlayed(uri) } }
                }
            }
        })
        scope.launch {
            val repository = (application as MscApplication).library
            val saved = withContext(Dispatchers.IO) { repository.refresh(); store.read() }
            // A controller may have already received a user play command while disk restoration ran.
            if (player.mediaItemCount == 0) {
                val tracks = repository.tracks.value.associateBy { it.uri }
                val items = saved.uris.mapNotNull { tracks[it]?.mediaItem() }
                if (items.isNotEmpty()) {
                    player.setMediaItems(items, saved.index.coerceIn(0, items.lastIndex), saved.position.coerceAtLeast(0))
                    player.shuffleModeEnabled = saved.shuffle
                    player.repeatMode = saved.repeat.coerceIn(0, 2)
                    player.prepare() // Restore paused; never start sound without a new user action.
                }
            }
            restoring = false
            while (isActive) { delay(3000); persist() }
        }
    }
    private fun persist() {
        // Public stream links can expire. Keep online queues in the active session only.
        val all = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        val local = all.filter { it.localConfiguration?.uri?.scheme !in listOf("http", "https") }
        val current = player.currentMediaItem
        val index = local.indexOfFirst { it.mediaId == current?.mediaId }
        store.save(SavedPlayback(local.map { it.mediaId }, index.coerceAtLeast(0),
            if (index >= 0) player.currentPosition else 0, player.shuffleModeEnabled, player.repeatMode))
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = session
    override fun onDestroy() {
        persist(); scope.cancel(); effects.release(); session.release(); player.release(); super.onDestroy()
    }
    companion object { const val GET_EFFECTS = "msc.GET_EFFECTS"; const val SET_EFFECTS = "msc.SET_EFFECTS" }
}
