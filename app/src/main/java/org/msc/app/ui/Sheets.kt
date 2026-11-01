package org.msc.app.ui

import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.msc.app.LrcParser
import org.msc.app.LyricLine
import org.msc.app.MusicViewModel
import org.msc.app.BuildConfig
import org.msc.app.PlaybackUi
import org.msc.app.data.Playlist
import org.msc.app.data.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun QueueSheet(model: MusicViewModel, playback: PlaybackUi, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.8f)) {
            Text("Your queue · ${playback.queue.size}", Modifier.padding(24.dp), style = MaterialTheme.typography.headlineSmall)
            if (playback.queue.isEmpty()) Text("Play a track to start a queue.", Modifier.padding(24.dp))
            LazyColumn {
                itemsIndexed(playback.queue) { index, t ->
                    ListItem(headlineContent = { Text((if (index == playback.index) "• " else "") + t.title) },
                        supportingContent = { Text(t.artist) }, modifier = Modifier.clickable { model.queuePlay(index) },
                        trailingContent = { Row {
                            IconButton(enabled = index > 0, onClick = { model.queueMove(index, index - 1) }) { AppIcon("up", "Move ${t.title} up") }
                            IconButton(enabled = index < playback.queue.lastIndex, onClick = { model.queueMove(index, index + 1) }) { AppIcon("down", "Move ${t.title} down") }
                            IconButton(onClick = { model.queueRemove(index) }) { AppIcon("close", "Remove ${t.title} from queue") }
                        } })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun PlaylistSheet(model: MusicViewModel, playlist: Playlist, tracks: List<Track>, onDismiss: () -> Unit) {
    var rename by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.8f)) {
            Text(playlist.name, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
            val entries = playlist.tracks.mapNotNull { uri -> tracks.find { it.uri == uri } }
            Row(Modifier.padding(horizontal = 16.dp)) {
                TextButton(enabled = entries.isNotEmpty(), onClick = { model.play(entries.first(), entries) }) { Text("Play") }
                TextButton(enabled = entries.isNotEmpty(), onClick = { val queue = entries.shuffled(); model.play(queue.first(), queue) }) { Text("Shuffle") }
                TextButton(onClick = { entries.forEach(model::enqueue) }, enabled = entries.isNotEmpty()) { Text("Queue") }
                TextButton(onClick = { rename = true }) { Text("Rename") }
                TextButton(onClick = { delete = true }) { Text("Delete") }
            }
            if (playlist.tracks.isEmpty()) Text("Use a track’s menu to add music to this playlist.", Modifier.padding(24.dp))
            LazyColumn { itemsIndexed(playlist.tracks, key = { _, uri -> uri }) { index, uri ->
                val track = tracks.find { it.uri == uri }
                ListItem(headlineContent = { Text(track?.title ?: "Unavailable track") },
                    supportingContent = { Text(track?.artist ?: "File is no longer in your library") },
                    modifier = Modifier.clickable(enabled = track != null) { if (track != null) model.play(track, entries) },
                    trailingContent = { Row {
                        IconButton(enabled = index > 0, onClick = {
                            val copy = playlist.tracks.toMutableList(); java.util.Collections.swap(copy, index, index - 1); model.updatePlaylist(playlist, copy)
                        }) { AppIcon("up", "Move track up") }
                        IconButton(enabled = index < playlist.tracks.lastIndex, onClick = {
                            val copy = playlist.tracks.toMutableList(); java.util.Collections.swap(copy, index, index + 1); model.updatePlaylist(playlist, copy)
                        }) { AppIcon("down", "Move track down") }
                        IconButton(onClick = { model.updatePlaylist(playlist, playlist.tracks.filterNot { it == uri }) }) { AppIcon("close", "Remove track from playlist") }
                    } })
            } }
        }
    }
    if (rename) NameDialog("Rename playlist", playlist.name, { rename = false }, { model.renamePlaylist(playlist.id, it); rename = false })
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Delete ${playlist.name}?") },
        text = { Text("Your audio files will be kept.") }, confirmButton = { TextButton(onClick = { model.deletePlaylist(playlist.id); onDismiss() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { delete = false }) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun LyricsSheet(model: MusicViewModel, track: Track, position: Long, onDismiss: () -> Unit, onImport: () -> Unit) {
    val lyrics by model.lyrics.collectAsStateWithLifecycle()
    val lines by produceState<List<LyricLine>>(emptyList(), lyrics.body) { value = withContext(Dispatchers.Default) { LrcParser.parse(lyrics.body) } }
    val active = LrcParser.activeIndex(lines, position + lyrics.offset)
    val list = rememberLazyListState()
    var auto by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(active, auto) { if (auto && active >= 0 && !list.isScrollInProgress) list.animateScrollToItem(active) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.85f)) {
            Text("Lyrics", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
            Text(track.title, Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onImport) { Text("Import LRC / text") }
                Spacer(Modifier.weight(1f)); Text("Auto scroll", style = MaterialTheme.typography.labelSmall); Switch(auto, { auto = it })
            }
            if (lines.isNotEmpty()) Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { model.lyricsOffset(lyrics.offset - 250) }) { Text("−250 ms") }
                TextButton(onClick = { model.lyricsOffset(0) }) { Text("${lyrics.offset} ms · reset") }
                TextButton(onClick = { model.lyricsOffset(lyrics.offset + 250) }) { Text("+250 ms") }
            }
            if (lyrics.body.isBlank()) Text("Import a lyrics file for this track. Timestamped LRC lines follow the real playback position; tap a line to seek.", Modifier.padding(24.dp))
            else if (lines.isEmpty()) LazyColumn { item { Text(lyrics.body, Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge) } }
            else LazyColumn(state = list, contentPadding = PaddingValues(24.dp)) {
                itemsIndexed(lines) { index, line -> Text(line.text.ifBlank { "♪" }, Modifier.fillMaxWidth().clickable { model.seek(line.timeMs - lyrics.offset) }.padding(vertical = 15.dp),
                    color = if (index == active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = if (index == active) FontWeight.Bold else FontWeight.Normal) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun EffectsSheet(model: MusicViewModel, onDismiss: () -> Unit) {
    val effects by model.effects.collectAsStateWithLifecycle()
    val frequencies = effects.getIntArray("frequencies") ?: intArrayOf()
    val levels = effects.getIntArray("levels") ?: intArrayOf()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(contentPadding = PaddingValues(24.dp)) {
            item { Text("Shape your sound", style = MaterialTheme.typography.headlineSmall) }
            item { Text(effects.getString("message") ?: "Checking audio effects…", Modifier.padding(vertical = 12.dp)) }
            if (effects.getBoolean("available")) {
                item { Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable effects", Modifier.weight(1f)); Switch(effects.getBoolean("enabled"), { model.requestEffects(Bundle().apply { putBoolean("enabled", it) }) })
                } }
                item { Row {
                    listOf("Flat", "Bass", "Rock", "Vocal").forEach { name ->
                        TextButton(onClick = {
                            model.requestEffects(Bundle().apply { putIntArray("levels", IntArray(frequencies.size) { index ->
                                val hz = frequencies[index]
                                when(name) { "Bass" -> if (hz < 250) 200 else 0; "Rock" -> if (hz < 250 || hz > 4000) 150 else -100; "Vocal" -> if (hz in 500..4000) 200 else -100; else -> 0 }
                            }) })
                        }) { Text(name) }
                    }
                } }
                itemsIndexed(frequencies.toList()) { index, hz ->
                    val min = effects.getInt("min", -600).toFloat(); val max = effects.getInt("max", 300).toFloat()
                    EffectSlider("${hz} Hz", levels.getOrElse(index) { 0 }.toFloat(), min..max) { value ->
                        val copy = levels.copyOf(); if (index in copy.indices) { copy[index] = value.toInt(); model.requestEffects(Bundle().apply { putIntArray("levels", copy) }) }
                    }
                }
                if (effects.getBoolean("bassAvailable")) item { EffectSlider("Bass enhancement", effects.getInt("bass").toFloat(), 0f..500f) { model.requestEffects(Bundle().apply { putInt("bass", it.toInt()) }) } }
            } else item { TextButton(onClick = { model.requestEffects() }) { Text("Check again") } }
        }
    }
}

@Composable private fun EffectSlider(label: String, current: Float, range: ClosedFloatingPointRange<Float>, onFinish: (Float) -> Unit) {
    var value by remember(current) { mutableFloatStateOf(current.coerceIn(range.start, range.endInclusive)) }
    Text("$label · ${value.toInt()}", style = MaterialTheme.typography.labelLarge)
    Slider(value, { value = it }, valueRange = range, onValueChangeFinished = { onFinish(value) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun SettingsSheet(model: MusicViewModel, onDismiss: () -> Unit, onSoundSettings: () -> Unit, onImportFolder: () -> Unit, onStartClash: () -> Unit) {
    val reduced by model.reducedMotion.collectAsStateWithLifecycle()
    val provider by model.provider.collectAsStateWithLifecycle()
    val local by model.clashControl.state.collectAsStateWithLifecycle()
    var url by rememberSaveable { mutableStateOf(provider.url) }
    var enabled by rememberSaveable { mutableStateOf(provider.enabled) }
    var connectionMessage by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(contentPadding = PaddingValues(24.dp)) {
            item { Text("Make it yours", style = MaterialTheme.typography.headlineSmall) }
            item { Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Reduced motion"); Text("Keep the disc still", style = MaterialTheme.typography.bodySmall) }
                Switch(reduced, model::setReducedMotion)
            } }
            item { TextButton(onClick = onSoundSettings) { Text("System audio output and volume") } }
            item { TextButton(onClick = onImportFolder) { Text("Import music folder") } }
            item { TextButton(onClick = model::rescanFolders) { Text("Rescan saved folders") } }
            item { HorizontalDivider(Modifier.padding(vertical = 16.dp)); Text("ClashFLAC", style = MaterialTheme.typography.titleLarge) }
            if (BuildConfig.EMBEDDED_CLASH) {
                item { Text("On this phone · ${local.state.lowercase().replaceFirstChar { it.uppercase() }}", Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.titleMedium) }
                local.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                item { Text("The real ClashFLAC backend runs in a separate local process with a persistent notification. Provider accounts and your own client configuration are required. No credentials or device keys are bundled.", style = MaterialTheme.typography.bodySmall) }
                item { Row {
                    Button(enabled = local.state !in listOf("STARTING", "RUNNING"), onClick = onStartClash) { Text("Start server") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(enabled = local.state in listOf("STARTING", "RUNNING"), onClick = { model.clashControl.stop() }) { Text("Stop server") }
                } }
                item { Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Use on-device catalog", Modifier.weight(1f)); Switch(provider.onDevice, model::useOnDeviceClash)
                } }
            }
            item { Text("Connect a server you are authorized to use. This version supports its catalog search only. Streaming and downloads are unavailable.", Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium) }
            item { OutlinedTextField(url, { url = it }, label = { Text("ClashFLAC HTTPS server URL") }, placeholder = { Text("https://your-server.example") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Search this catalog", Modifier.weight(1f)); Switch(enabled, { enabled = it })
            } }
            item { Button(onClick = {
                try {
                    if (enabled || url.isNotBlank()) org.msc.app.providers.ClashFlacCatalog().validateBaseUrl(url)
                    model.configureProvider(url, enabled)
                    connectionMessage = "Settings saved. Server access is checked when you search."
                } catch (e: Exception) { connectionMessage = e.message ?: "Check your server address." }
            }) { Text("Save connection") } }
            connectionMessage?.let { message -> item { Text(message, Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall) } }
            item { HorizontalDivider(Modifier.padding(vertical = 16.dp)); Text("msc · ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium) }
            item { Text("Your files stay yours. Importing, favorites and playlists do not change or delete your original audio.", Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall) }
        }
    }
}
