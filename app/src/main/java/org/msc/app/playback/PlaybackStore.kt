package org.msc.app.playback

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.msc.app.data.Track

fun Track.mediaItem(): MediaItem = MediaItem.Builder().setMediaId(uri).setUri(Uri.parse(uri))
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album)
        .setArtworkUri(artworkUri.takeIf(String::isNotBlank)?.let(Uri::parse))
        .setExtras(Bundle().apply { putString("source", source); putString("fileName", fileName); putLong("durationMs", durationMs) }).build()).build()

fun MediaItem.track(): Track = Track(mediaId, mediaMetadata.title?.toString() ?: "Untitled audio",
    mediaMetadata.artist?.toString() ?: "Unknown artist", mediaMetadata.albumTitle?.toString() ?: "Unknown album",
    durationMs = mediaMetadata.extras?.getLong("durationMs") ?: 0,
    fileName = mediaMetadata.extras?.getString("fileName") ?: mediaId,
    source = mediaMetadata.extras?.getString("source") ?: "import", artworkUri = mediaMetadata.artworkUri?.toString().orEmpty())

data class SavedPlayback(val uris: List<String>, val index: Int, val position: Long, val shuffle: Boolean, val repeat: Int)
class PlaybackStore(context: Context) {
    private val prefs = context.getSharedPreferences("msc-playback", Context.MODE_PRIVATE)
    fun read(): SavedPlayback {
        val list = mutableListOf<String>()
        try {
            val json = JSONArray(prefs.getString("queue", "[]"))
            for (i in 0 until json.length()) list += json.getString(i)
        } catch (_: org.json.JSONException) { prefs.edit().remove("queue").apply() }
        return SavedPlayback(list, prefs.getInt("index", 0), prefs.getLong("position", 0), prefs.getBoolean("shuffle", false), prefs.getInt("repeat", 0))
    }
    fun save(state: SavedPlayback) {
        prefs.edit().putString("queue", JSONArray(state.uris).toString()).putInt("index", state.index)
            .putLong("position", state.position).putBoolean("shuffle", state.shuffle).putInt("repeat", state.repeat).apply()
    }
}
