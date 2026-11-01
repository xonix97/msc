package org.msc.app.clash

import android.content.*
import android.os.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.msc.app.BuildConfig

data class LocalClashState(val state: String = "STOPPED", val port: Int = 0, val token: String = "", val error: String? = null)

/** Same-UID Binder channel; the loopback token is never put in URLs, notifications or logs. */
class ClashControl(private val context: Context) {
    private val mutable = MutableStateFlow(LocalClashState())
    val state = mutable.asStateFlow()
    private var bound = false
    private var remote: Messenger? = null
    private val listener = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            val b = message.data
            mutable.value = LocalClashState(b.getString("state") ?: "STOPPED", b.getInt("port"), b.getString("token").orEmpty(), b.getString("error"))
        }
    })
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            remote = Messenger(binder)
            try { remote?.send(Message.obtain(null, EmbeddedClashService.GET_STATE).apply { replyTo = listener }) }
            catch (_: RemoteException) { remote = null; mutable.value = LocalClashState("ERROR", error = "The local engine disconnected.") }
        }
        override fun onServiceDisconnected(name: ComponentName) { remote = null; mutable.value = LocalClashState() }
    }
    fun connect() {
        if (BuildConfig.EMBEDDED_CLASH && !bound) bound = context.bindService(Intent(context, EmbeddedClashService::class.java), connection, Context.BIND_AUTO_CREATE)
    }
    fun start() {
        check(BuildConfig.EMBEDDED_CLASH) { "This APK does not include the embedded backend" }
        connect(); ContextCompat.startForegroundService(context, Intent(context, EmbeddedClashService::class.java))
    }
    fun stop() { context.startService(Intent(context, EmbeddedClashService::class.java).setAction(EmbeddedClashService.STOP)) }
    fun close() { if (bound) context.unbindService(connection); bound = false; remote = null }
}
