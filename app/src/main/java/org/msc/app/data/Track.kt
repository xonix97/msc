package org.msc.app.data

import java.text.Normalizer
import java.util.Locale

data class Track(
    val uri: String,
    val title: String,
    val artist: String = "Unknown artist",
    val album: String = "Unknown album",
    val durationMs: Long = 0,
    val fileName: String = title,
    val folder: String = "Imported files",
    val genre: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val source: String = "import",
    val favorite: Boolean = false,
    val lastPlayed: Long = 0,
    val playCount: Long = 0,
)

data class Playlist(val id: Long, val name: String, val tracks: List<String>)
enum class TrackSort(val label: String) {
    TITLE("Title"), ARTIST("Artist"), ALBUM("Album"), FILE_NAME("File name"), DURATION("Duration"), RECENT("Recently added")
}

object LibraryRules {
    fun key(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT).trim()

    fun search(tracks: List<Track>, query: String): List<Track> {
        val words = key(query).split(Regex("\\s+")).filter(String::isNotEmpty)
        return tracks.filter { track ->
            val text = key("${track.title} ${track.artist} ${track.album} ${track.fileName} ${track.folder}")
            words.all(text::contains)
        }
    }

    fun sorted(tracks: List<Track>, by: TrackSort): List<Track> = when (by) {
        TrackSort.TITLE -> tracks.sortedBy { key(it.title) }
        TrackSort.ARTIST -> tracks.sortedWith(compareBy({ key(it.artist) }, { key(it.title) }))
        TrackSort.ALBUM -> tracks.sortedWith(compareBy({ key(it.album) }, { key(it.title) }))
        TrackSort.FILE_NAME -> tracks.sortedBy { key(it.fileName) }
        TrackSort.DURATION -> tracks.sortedBy { it.durationMs }
        TrackSort.RECENT -> tracks.sortedByDescending { it.addedAt }
    }

    fun playlistName(input: String): String = input.trim().also {
        require(it.isNotEmpty() && it.length <= 80) { "Use a playlist name between 1 and 80 characters." }
    }
}
