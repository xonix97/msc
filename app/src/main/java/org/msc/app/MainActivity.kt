package org.msc.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import org.msc.app.ui.MscApp

@androidx.annotation.OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {
    private val model: MusicViewModel by viewModels()
    private var lyricsTarget: String? = null
    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) { model.notify("This file picker granted temporary access only; reimport if access expires.") }
        }
        if (uris.isNotEmpty()) model.importFiles(uris)
    }
    private val lyricsPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = lyricsTarget
        if (uri != null && target != null) model.importLyrics(uri, target)
    }
    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                model.importFolder(uri)
            } catch (_: SecurityException) { model.notify("This folder did not grant persistent access. Select a different folder or import individual files.") }
        }
    }
    private val musicPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) model.scan() else model.notify("Music access was denied. You can still import files using the system picker.")
    }
    private val serverNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) model.notify("Server notifications are off. You can stop the engine from Settings or Android's active-app controls.")
        model.clashControl.start()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lyricsTarget = savedInstanceState?.getString("lyricsTarget")
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent { MscApp(model,
            onImport = { audioPicker.launch(arrayOf("audio/*", "application/ogg")) },
            onImportFolder = { folderPicker.launch(null) },
            onScan = {
                val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) model.scan()
                else musicPermission.launch(permission)
            },
            onImportLyrics = { uri -> lyricsTarget = uri; lyricsPicker.launch(arrayOf("*/*")) },
            onSoundSettings = { startActivity(Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)) },
            onStartClash = {
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    serverNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else model.clashControl.start()
            },
        ) }
    }
    override fun onStart() { super.onStart(); model.foreground(true) }
    override fun onStop() { model.foreground(false); super.onStop() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("lyricsTarget", lyricsTarget); super.onSaveInstanceState(outState) }
}
