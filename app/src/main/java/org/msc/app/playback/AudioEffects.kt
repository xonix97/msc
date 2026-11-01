package org.msc.app.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Bundle

/** Device-supported Android effects, attached only to the service player's audio session. */
class AudioEffects(context: Context) {
    private val prefs = context.getSharedPreferences("msc-effects", Context.MODE_PRIVATE)
    private var equalizer: Equalizer? = null
    private var bass: BassBoost? = null
    private var problem = "Start playback to check this audio output."
    fun attach(sessionId: Int) {
        release()
        if (sessionId <= 0) return
        try { equalizer = Equalizer(0, sessionId) } catch (_: RuntimeException) { problem = "Equalizer is unavailable on this audio output." }
        try { bass = BassBoost(0, sessionId) } catch (_: RuntimeException) { /* Bass support is optional and reported below. */ }
        applySaved()
    }
    private fun applySaved() {
        try {
            val enabled = prefs.getBoolean("enabled", false)
            equalizer?.let { eq ->
                val range = eq.bandLevelRange
                for (i in 0 until eq.numberOfBands.toInt()) eq.setBandLevel(i.toShort(), prefs.getInt("band$i", 0).coerceIn(maxOf(-600, range[0].toInt()), minOf(300, range[1].toInt())).toShort())
                eq.enabled = enabled
            }
            bass?.let { if (it.strengthSupported) it.setStrength(prefs.getInt("bass", 0).coerceIn(0, 500).toShort()); it.enabled = enabled }
        } catch (e: RuntimeException) { problem = "Effects could not be applied: ${e.javaClass.simpleName}"; release() }
    }
    fun configure(args: Bundle): Bundle {
        prefs.edit().apply {
            if (args.containsKey("enabled")) putBoolean("enabled", args.getBoolean("enabled"))
            if (args.containsKey("bass")) putInt("bass", args.getInt("bass").coerceIn(0, 500))
            args.getIntArray("levels")?.forEachIndexed { i, level -> putInt("band$i", level.coerceIn(-600, 300)) }
        }.apply()
        applySaved(); return snapshot()
    }
    fun snapshot(): Bundle = Bundle().apply {
        val eq = equalizer
        putBoolean("enabled", prefs.getBoolean("enabled", false)); putBoolean("available", eq != null)
        putBoolean("bassAvailable", bass?.strengthSupported == true); putInt("bass", prefs.getInt("bass", 0))
        putString("message", if (eq != null) "Effects can change the source signal. Gain is limited to reduce clipping." else problem)
        if (eq != null) try {
            putIntArray("frequencies", IntArray(eq.numberOfBands.toInt()) { eq.getCenterFreq(it.toShort()) / 1000 })
            putIntArray("levels", IntArray(eq.numberOfBands.toInt()) { eq.getBandLevel(it.toShort()).toInt() })
            putInt("min", maxOf(-600, eq.bandLevelRange[0].toInt())); putInt("max", minOf(300, eq.bandLevelRange[1].toInt()))
        } catch (_: RuntimeException) { putBoolean("available", false); putString("message", "Effects are no longer available on this output.") }
    }
    fun release() { equalizer?.release(); bass?.release(); equalizer = null; bass = null }
}
