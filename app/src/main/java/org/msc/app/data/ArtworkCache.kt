package org.msc.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache

data class Artwork(val bitmap: Bitmap?, val accent: Int = 0xffd7bc87.toInt())

class ArtworkCache(private val context: Context) {
    private val cache = object : LruCache<String, Artwork>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Artwork): Int = (value.bitmap?.allocationByteCount ?: 64)
    }
    @Synchronized fun load(uri: String): Artwork {
        cache.get(uri)?.let { return it }
        val retriever = MediaMetadataRetriever()
        val bitmap = try {
            retriever.setDataSource(context, Uri.parse(uri))
            retriever.embeddedPicture?.takeIf { it.size <= 12 * 1024 * 1024 }?.let { bytes ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val options = BitmapFactory.Options().apply { inSampleSize = (maxOf(bounds.outWidth, bounds.outHeight) / 384).coerceAtLeast(1) }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        } catch (_: RuntimeException) { null /* Missing, unsupported or revoked artwork is a normal fallback. */ }
        finally { retriever.release() }
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
        return Artwork(bitmap, accent).also { cache.put(uri, it) }
    }
}
