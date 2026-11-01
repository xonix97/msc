package org.msc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.msc.app.OnlineUi
import org.msc.app.providers.OnlineSong

@Composable internal fun OnlineSearchResults(state: OnlineUi, query: String, onRetry: () -> Unit,
    onPlay: (OnlineSong) -> Unit, onQueue: (OnlineSong) -> Unit, artwork: @Composable (OnlineSong) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Text("JioSaavn · Unofficial API · Internet required", Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        when {
            query.trim().length < 2 -> item { Text("Search for a song, artist or album to listen online.", Modifier.padding(24.dp)) }
            state.loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp)) }
            state.error != null -> item { Column(Modifier.padding(24.dp)) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Retry search") }
            } }
            state.results.isEmpty() -> item { Text("No songs found. Try another name.", Modifier.padding(24.dp)) }
        }
        items(state.results, key = { it.id }) { song ->
            val available = song.streamUrl != null
            val interaction = remember { MutableInteractionSource() }
            GlassPanel(Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth().liquidPress(interaction)) {
            ListItem(modifier = Modifier.clickable(enabled = available, interactionSource = interaction, indication = LocalIndication.current) { onPlay(song) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { artwork(song) },
                headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Column {
                    Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (available) "${timeLabel(song.durationMs)} · ${song.quality}" else "Unavailable to stream", style = MaterialTheme.typography.labelSmall)
                } },
                trailingContent = { IconButton(enabled = available, onClick = { onQueue(song) }) { AppIcon("add", "Queue ${song.title}") } })
            }
        }
    }
}
