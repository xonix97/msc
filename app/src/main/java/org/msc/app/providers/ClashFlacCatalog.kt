package org.msc.app.providers

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

data class CatalogTrack(val id: String, val title: String, val artist: String, val album: String, val durationSeconds: Int)

/** Catalog metadata only. Catalog URLs are not interpreted as licensed playable/downloadable audio. */
class ClashFlacCatalog {
    fun validateBaseUrl(input: String): String {
        val uri = URI(input.trim())
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Enter an HTTPS server address without credentials, query parameters or fragments."
        }
        return uri.toASCIIString().trimEnd('/')
    }
    fun search(baseUrl: String, query: String): List<CatalogTrack> {
        val base = validateBaseUrl(baseUrl)
        val url = URI("$base/api/search?q=${URLEncoder.encode(query, "UTF-8")}&limit=20").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 8000; connection.readTimeout = 10000
        connection.instanceFollowRedirects = false
        try {
            val code = connection.responseCode
            check(code == 200) { when(code) {
                401, 403 -> "This ClashFLAC server requires authorization. Configure its account on the server."
                429 -> "ClashFLAC rate limit reached. Try again later."
                else -> "ClashFLAC returned HTTP $code. Check the server and try again."
            } }
            val output = java.io.ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer); if (read < 0) break
                    require(output.size() + read <= 1024 * 1024) { "ClashFLAC response is too large." }
                    output.write(buffer, 0, read)
                }
            }
            return parse(output.toString("UTF-8"))
        } finally { connection.disconnect() }
    }
    fun parse(json: String): List<CatalogTrack> {
        val array = JSONArray(json)
        return (0 until minOf(array.length(), 100)).map { i ->
            val item = array.getJSONObject(i)
            CatalogTrack(item.getString("asin"), item.getString("title"), item.optString("artist", "Unknown artist"),
                item.optString("album", ""), item.optInt("duration_sec", 0).coerceAtLeast(0))
        }
    }
}
