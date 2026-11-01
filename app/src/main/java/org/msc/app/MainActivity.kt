package org.msc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Foundation runtime gate. Music features are deliberately not advertised yet. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Scaffold { inset ->
                    Column(Modifier.padding(inset).padding(24.dp)) {
                        Text("msc", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(16.dp))
                        Text("Android foundation validation", style = MaterialTheme.typography.titleLarge)
                        Text("Music playback is not implemented in this foundation build.")
                    }
                }
            }
        }
    }
}
