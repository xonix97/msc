package org.msc.app.providers

import android.text.Html
import org.json.JSONArray
import org.json.JSONObject
import org.msc.app.data.Track
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.CancellationException

data class OnlineSong(val id: String, val title: String, val artist: String, val album: String,
    val durationMs: Long, val artworkUrl: String, val streamUrl: String?, val quality: String) {
    fun track(): Track? = streamUrl?.let {
        Track(it, title, artist, album, durationMs, fileName = id, folder = "JioSaavn",
            source = "jiosaavn", artworkUri = artworkUrl)
    }
}

/** Client for the documented sumitkolhe/jiosaavn-api song-search contract. */
class JioSaavn(private val connect: (String) -> HttpURLConnection = { URI(it).toURL().openConnection() as HttpURLConnection }) {
    fun search(query: String, cancelled: () -> Boolean = { Thread.currentThread().isInterrupted }): List<OnlineSong> {
        val text = query.trim().take(200)
        if (text.length < 2) return emptyList()
        val address = "https://saavn.dev/api/search/songs?query=${URLEncoder.encode(text, "UTF-8")}&page=0&limit=20"
        val connection = connect(address)
        connection.connectTimeout = 8000; connection.readTimeout = 12000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        try {
            if (cancelled()) throw CancellationException()
            check(connection.responseCode == 200) { when (connection.responseCode) {
                403 -> "JioSaavn is unavailable from this connection or region. Try again later."
                429 -> "JioSaavn is busy. Wait a moment and retry."
                else -> "JioSaavn is temporarily unavailable (HTTP ${connection.responseCode})."
            } }
            val output = java.io.ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    if (cancelled()) throw CancellationException()
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 1024 * 1024) { "JioSaavn returned too much data. Try a different search." }
                    output.write(buffer, 0, count)
                }
            }
            return parse(output.toString("UTF-8"))
        } finally { connection.disconnect() }
    }

    fun parse(json: String): List<OnlineSong> {
        val body = JSONObject(json)
        check(body.optBoolean("success")) { "JioSaavn could not complete this search. Try again later." }
        val results = body.getJSONObject("data").getJSONArray("results")
        return (0 until minOf(results.length(), 100)).mapNotNull { index ->
            val song = results.optJSONObject(index) ?: return@mapNotNull null
            val id = song.optString("id").take(200)
            val title = text(song.optString("name"))
            if (id.isBlank() || title.isBlank() || id == "null") return@mapNotNull null
            val primary = song.optJSONObject("artists")?.optJSONArray("primary") ?: JSONArray()
            val artist = (0 until minOf(primary.length(), 8)).mapNotNull { primary.optJSONObject(it)?.optString("name") }
                .map(::text).filter(String::isNotBlank).joinToString(", ").ifBlank { "Unknown artist" }
            val streams = links(song.optJSONArray("downloadUrl"))
            val best = streams.maxByOrNull { qualityNumber(it.first) }
            val image = links(song.optJSONArray("image")).maxByOrNull { qualityNumber(it.first) }?.second.orEmpty()
            OnlineSong(id, title, artist, text(song.optJSONObject("album")?.optString("name").orEmpty()).ifBlank { "Unknown album" },
                song.optLong("duration").coerceIn(0, 24 * 60 * 60) * 1000, image, best?.second, best?.first.orEmpty())
        }.distinctBy { it.id }
    }

    private fun links(array: JSONArray?): List<Pair<String, String>> = (0 until minOf(array?.length() ?: 0, 20)).mapNotNull { i ->
        val item = array?.optJSONObject(i) ?: return@mapNotNull null
        val url = trustedCdnUrl(item.optString("url")) ?: return@mapNotNull null
        item.optString("quality").take(30) to url
    }
    private fun qualityNumber(value: String): Int = value.takeWhile(Char::isDigit).toIntOrNull() ?: 0
    private fun text(value: String): String = if (value == "null") "" else
        Html.fromHtml(value.take(1024), Html.FROM_HTML_MODE_LEGACY).toString().trim()

    companion object {
        fun trustedCdnUrl(value: String): String? = try {
            val uri = URI(value)
            val host = uri.host?.lowercase(java.util.Locale.ROOT).orEmpty()
            value.takeIf { uri.scheme == "https" && (host == "saavncdn.com" || host.endsWith(".saavncdn.com")) &&
                uri.userInfo == null && uri.fragment == null && uri.port in listOf(-1, 443) && value.length <= 4096 }
        } catch (_: Exception) { null }
    }
}
