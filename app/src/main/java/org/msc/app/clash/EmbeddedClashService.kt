package org.msc.app.clash

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.*
import org.msc.app.MainActivity
import org.msc.app.R
import java.security.SecureRandom

/** Private foreground service in its own process: stopping Python cannot stop the music player. */
class EmbeddedClashService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val subscribers = mutableSetOf<Messenger>()
    private var state = Bundle().apply { putString("state", "STOPPED") }
    private var starting: Job? = null
    private val messenger by lazy { Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            if (message.what == GET_STATE) {
                message.replyTo?.let { subscribers += it; send(it) }
            }
        }
    }) }
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "On-device ClashFLAC", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onBind(intent: Intent): IBinder = messenger.binder
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopEngine(); return START_NOT_STICKY }
        if (state.getString("state") in listOf("STARTING", "RUNNING")) return START_NOT_STICKY
        foreground("Starting on-device ClashFLAC")
        publish(Bundle().apply { putString("state", "STARTING") })
        starting = scope.launch {
            try {
                val token = ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
                val result = withContext(Dispatchers.IO) {
                    val runtime = Class.forName(RUNTIME)
                    runtime.getMethod("start", Context::class.java, String::class.java).invoke(null, applicationContext, token) as Bundle
                }
                publish(Bundle(result).apply { putString("state", "RUNNING") })
                foreground("On-device ClashFLAC is running")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                publish(Bundle().apply { putString("state", "ERROR"); putString("error", "The embedded engine could not start. Check the native runtime and provider setup.") })
                ServiceCompat.stopForeground(this@EmbeddedClashService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }
    private fun stopEngine() {
        starting?.cancel()
        // Enforce the stop deadline even if a native Python call cannot be interrupted.
        val handler = Handler(Looper.getMainLooper())
        val terminate = Runnable { Process.killProcess(Process.myPid()) }
        handler.postDelayed(terminate, 8000)
        scope.launch {
            withContext(Dispatchers.IO) {
                // A graceful shutdown is attempted before terminating only this isolated process.
                try { Class.forName(RUNTIME).getMethod("stop").invoke(null) } catch (_: ReflectiveOperationException) { }
            }
            publish(Bundle().apply { putString("state", "STOPPED") })
            ServiceCompat.stopForeground(this@EmbeddedClashService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            handler.removeCallbacks(terminate)
            handler.postDelayed(terminate, 300)
        }
    }
    private fun foreground(text: String) {
        ServiceCompat.startForeground(this, NOTIFICATION, notification(text),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
    }
    private fun notification(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, javaClass).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_msc).setContentTitle("msc · ClashFLAC")
            .setContentText(text).setContentIntent(open).setOngoing(true).addAction(0, "Stop server", stop).build()
    }
    private fun send(target: Messenger) {
        try { target.send(Message.obtain(null, GET_STATE).apply { data = Bundle(state) }) }
        catch (_: RemoteException) { subscribers.remove(target) }
    }
    private fun publish(value: Bundle) { state = value; subscribers.toList().forEach(::send) }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    companion object {
        const val GET_STATE = 1
        const val STOP = "org.msc.clash.STOP"
        private const val RUNTIME = "org.msc.embeddedclash.EmbeddedRuntime"
        private const val CHANNEL = "msc-clashflac"
        private const val NOTIFICATION = 7042
    }
}
