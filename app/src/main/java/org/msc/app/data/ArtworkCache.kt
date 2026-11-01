package org.msc.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import org.msc.app.providers.JioSaavn
import java.net.HttpURLConnection
import java.net.URI

data class Artwork(val bitmap: Bitmap?, val accent: Int = 0xffd7bc87.toInt())

class ArtworkCache(private val context: Context) {
    private val cache = object : LruCache<String, Artwork>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Artwork): Int = (value.bitmap?.allocationByteCount ?: 64)
    }
    fun load(uri: String, artworkUri: String = ""): Artwork {
        val key = artworkUri.ifBlank { uri }
        cache.get(key)?.let { return it }
        val bitmap = try {
            val bytes = if (artworkUri.isNotBlank()) remoteImage(artworkUri) else if (Uri.parse(uri).scheme in listOf("https", "http")) null else {
                val retriever = MediaMetadataRetriever()
                try { retriever.setDataSource(context, Uri.parse(uri)); retriever.embeddedPicture }
                finally { retriever.release() }
            }
            bytes?.takeIf { it.size <= 12 * 1024 * 1024 }?.let {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth !in 1..8192 || bounds.outHeight !in 1..8192) return@let null
                val options = BitmapFactory.Options().apply { inSampleSize = (maxOf(bounds.outWidth, bounds.outHeight) / 384).coerceAtLeast(1) }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        } catch (_: Exception) { null /* Missing, offline, unsupported or revoked artwork uses the disc fallback. */ }
        var accent = 0xffd7bc87.toInt()
        bitmap?.let {
            var r = 0L; var g = 0L; var b = 0L; var count = 0
            for (x in 0 until it.width step maxOf(1, it.width / 12)) for (y in 0 until it.height step maxOf(1, it.height / 12)) {
                val pixel = it.getPixel(x, y); r += android.graphics.Color.red(pixel); g += android.graphics.Color.green(pixel); b += android.graphics.Color.blue(pixel); count++
            }
            if (count > 0) {
                val hsv = FloatArray(3)
                android.graphics.Color.RGBToHSV((r/count).toInt(), (g/count).toInt(), (b/count).toInt(), hsv)
                hsv[1] = hsv[1].coerceIn(.15f, .5f); hsv[2] = .88f
                accent = android.graphics.Color.HSVToColor(hsv)
            }
        }
        return Artwork(bitmap, accent).also { if (bitmap != null || artworkUri.isBlank()) cache.put(key, it) }
    }
    private fun remoteImage(url: String): ByteArray? {
        val safe = JioSaavn.trustedCdnUrl(url) ?: return null
        val connection = URI(safe).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 4000; connection.readTimeout = 4000; connection.instanceFollowRedirects = false
        return try {
            if (connection.responseCode != 200) return null
            val output = java.io.ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer); if (count < 0) break
                    if (output.size() + count > 2 * 1024 * 1024) return null
                    output.write(buffer, 0, count)
                }
            }
            output.toByteArray()
        } finally { connection.disconnect() }
    }
}
