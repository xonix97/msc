package org.msc.embeddedclash

import android.content.Context
import android.os.Bundle
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject
import java.io.File

/** Invoked only in the dedicated :clashflac process by the private app service. */
object EmbeddedRuntime {
    @JvmStatic fun start(context: Context, token: String): Bundle {
        if (!Python.isStarted()) Python.start(AndroidPlatform(context.applicationContext))
        val root = File(context.filesDir, "clashflac")
        val ffmpeg = File(context.applicationInfo.nativeLibraryDir, "libmsc_ffmpeg.so")
        val result = Python.getInstance().getModule("clash_runtime")
            .callAttr("start", root.absolutePath, token, ffmpeg.absolutePath).toString()
        val parsed = JSONObject(result)
        check(parsed.getBoolean("running")) { "ClashFLAC is not HTTP-ready" }
        return Bundle().apply { putInt("port", parsed.getInt("port")); putString("token", token) }
    }
    @JvmStatic fun stop() {
        if (Python.isStarted()) Python.getInstance().getModule("clash_runtime").callAttr("stop")
    }
}
