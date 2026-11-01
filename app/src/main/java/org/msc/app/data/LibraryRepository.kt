package org.msc.app.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** All database and metadata methods are called on an IO dispatcher. User audio is never modified. */
class LibraryRepository(private val context: Context, private val helper: MusicDatabase = MusicDatabase(context)) {
    private val mutableTracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks = mutableTracks.asStateFlow()
    private val mutablePlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = mutablePlaylists.asStateFlow()
    private val db get() = helper.writableDatabase

    @Synchronized fun refresh() {
        val list = mutableListOf<Track>()
        db.rawQuery("SELECT * FROM tracks ORDER BY title COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) list += Track(
                c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4),
                c.getString(5), c.getString(6), c.getString(7), c.getLong(8), c.getString(9),
                c.getInt(10) != 0, c.getLong(11), c.getLong(12),
            )
        }
        mutableTracks.value = list
        val lists = mutableListOf<Playlist>()
        db.rawQuery("SELECT id,name FROM playlists ORDER BY name COLLATE NOCASE", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val entries = mutableListOf<String>()
                db.rawQuery("SELECT uri FROM playlist_tracks WHERE playlist_id=? ORDER BY position", arrayOf(id.toString())).use { e ->
                    while (e.moveToNext()) entries += e.getString(0)
                }
                lists += Playlist(id, c.getString(1), entries)
            }
        }
        mutablePlaylists.value = lists
    }

    @Synchronized fun upsert(track: Track) {
        val values = ContentValues().apply {
            put("uri", track.uri); put("title", track.title); put("artist", track.artist); put("album", track.album)
            put("duration", track.durationMs); put("filename", track.fileName); put("folder", track.folder)
            put("genre", track.genre); put("added", track.addedAt); put("source", track.source)
        }
        // Updating metadata must preserve favorites, history and original import time.
        if (db.update("tracks", ContentValues(values).apply { remove("uri"); remove("added") }, "uri=?", arrayOf(track.uri)) == 0) {
            db.insertOrThrow("tracks", null, values)
        }
    }

    fun importUri(uri: Uri): Track {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Untitled audio"
        if (uri.scheme == "content") context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) name = it.getString(0) ?: name
        }
        val reader = MediaMetadataRetriever()
        try {
            reader.setDataSource(context, uri)
            val title = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
                ?: name.substringBeforeLast('.', name)
            val track = Track(uri.toString(), title,
                reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() } ?: "Unknown artist",
                reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() } ?: "Unknown album",
                reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                name, genre = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).orEmpty())
            require(track.durationMs > 0) { "$name could not be read as supported audio." }
            upsert(track)
            return track
        } finally { reader.release() }
    }

    fun scanDevice(): Int {
        val found = mutableSetOf<String>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATE_ADDED)
        val folderColumn = if (android.os.Build.VERSION.SDK_INT >= 29) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
        context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection + folderColumn,
            "${MediaStore.Audio.Media.DURATION}>0", null, null)?.use { c ->
            while (c.moveToNext()) {
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(0)).toString()
                found += uri
                val path = c.getString(7).orEmpty()
                upsert(Track(uri, c.getString(1)?.takeIf { it.isNotBlank() } ?: c.getString(5).orEmpty(),
                    c.getString(2)?.takeIf { it != "<unknown>" } ?: "Unknown artist",
                    c.getString(3)?.takeIf { it != "<unknown>" } ?: "Unknown album", c.getLong(4), c.getString(5).orEmpty(),
                    if (android.os.Build.VERSION.SDK_INT >= 29) path.trimEnd('/') else path.substringBeforeLast('/', "Device music"),
                    addedAt = c.getLong(6) * 1000, source = "device"))
            }
        } ?: error("Android could not open the media library. Check your music permission.")
        // Only remove entries from this fully successful MediaStore scan. SAF imports are independent.
        tracks.value.filter { it.source == "device" && it.uri !in found }.forEach {
            db.delete("tracks", "uri=? AND source='device'", arrayOf(it.uri))
        }
        refresh()
        return found.size
    }

    @Synchronized fun toggleFavorite(uri: String) {
        db.execSQL("UPDATE tracks SET favorite=1-favorite WHERE uri=?", arrayOf(uri)); refresh()
    }
    @Synchronized fun recordPlayed(uri: String) {
        db.execSQL("UPDATE tracks SET last_played=?,play_count=play_count+1 WHERE uri=?", arrayOf<Any>(System.currentTimeMillis(), uri)); refresh()
    }
    @Synchronized fun removeFromLibrary(uri: String) {
        db.delete("tracks", "uri=?", arrayOf(uri)); refresh()
    }
    @Synchronized fun createPlaylist(name: String): Long {
        val id = db.insertOrThrow("playlists", null, ContentValues().apply { put("name", LibraryRules.playlistName(name)) })
        refresh(); return id
    }
    @Synchronized fun renamePlaylist(id: Long, name: String) {
        db.update("playlists", ContentValues().apply { put("name", LibraryRules.playlistName(name)) }, "id=?", arrayOf(id.toString())); refresh()
    }
    @Synchronized fun deletePlaylist(id: Long) { db.delete("playlists", "id=?", arrayOf(id.toString())); refresh() }
    @Synchronized fun setPlaylistTracks(id: Long, uris: List<String>) {
        db.beginTransaction()
        try {
            db.delete("playlist_tracks", "playlist_id=?", arrayOf(id.toString()))
            uris.distinct().forEachIndexed { index, uri ->
                db.insertOrThrow("playlist_tracks", null, ContentValues().apply { put("playlist_id", id); put("uri", uri); put("position", index) })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        refresh()
    }
    @Synchronized fun lyrics(uri: String): Pair<String, Long> {
        db.rawQuery("SELECT body,offset_ms FROM lyrics WHERE uri=?", arrayOf(uri)).use {
            return if (it.moveToFirst()) it.getString(0) to it.getLong(1) else "" to 0L
        }
    }
    @Synchronized fun saveLyrics(uri: String, body: String, offsetMs: Long = 0) {
        db.insertWithOnConflict("lyrics", null, ContentValues().apply { put("uri", uri); put("body", body); put("offset_ms", offsetMs) }, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun close() = helper.close()
}
