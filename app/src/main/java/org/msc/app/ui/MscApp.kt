package org.msc.app.ui

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.msc.app.MusicViewModel
import org.msc.app.PlaybackUi
import org.msc.app.data.*

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable fun MscApp(model: MusicViewModel, onImport: () -> Unit, onScan: () -> Unit,
    onImportFolder: () -> Unit, onImportLyrics: (String) -> Unit, onSoundSettings: () -> Unit) {
    val tracks by model.tracks.collectAsStateWithLifecycle()
    val playlists by model.playlists.collectAsStateWithLifecycle()
    val playback by model.playback.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val mode by model.mode.collectAsStateWithLifecycle()
    val reduced by model.reducedMotion.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf("Home") }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showEffects by rememberSaveable { mutableStateOf(false) }
    var addTrack by remember { mutableStateOf<Track?>(null) }
    var createPlaylist by rememberSaveable { mutableStateOf(false) }
    var selectedPlaylist by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val current = tracks.firstOrNull { it.uri == playback.current?.uri } ?: playback.current
    val art by produceState(Artwork(null), current?.uri, current?.artworkUri) {
        value = current?.let { withContext(Dispatchers.IO) { model.artwork.load(it.uri, it.artworkUri) } } ?: Artwork(null)
    }
    val accent by animateColorAsState(Color(art.accent), label = "Artwork accent")
    val angle = rememberSaveable { mutableFloatStateOf(0f) }
    val state by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val context = LocalContext.current
    val systemReduced = remember(context) { android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    LaunchedEffect(destination, playback.playing, reduced, systemReduced, state) {
        if (destination == "Player" && playback.playing && !reduced && !systemReduced && state.isAtLeast(Lifecycle.State.RESUMED)) {
            var previous = withFrameNanos { it }
            while (true) withFrameNanos { now ->
                angle.floatValue = (angle.floatValue + (now - previous) / 1_000_000_000f * 36f) % 360f
                previous = now
            }
        }
    }
    LaunchedEffect(model) { model.events.collect { snackbar.showSnackbar(it) } }
    BackHandler(destination != "Home") { destination = "Home" }
    CompositionLocalProvider(LocalLiquidMotion provides (!reduced && !systemReduced)) {
    MaterialTheme(colorScheme = darkColorScheme(primary = accent, onPrimary = Color(0xff111719),
        background = Color(0xff101719), surface = Color(0xff161f22), surfaceVariant = Color(0xff242e32),
        onSurface = Color(0xfff1f1e9), onSurfaceVariant = Color(0xffb9c3c6),
        secondaryContainer = accent.copy(alpha = .2f), onSecondaryContainer = accent)) {
        Box(Modifier.fillMaxSize()) {
        LiquidBackdrop(accent)
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface, bottomBar = {
            if (destination != "Player") Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (current != null) GlassPanel(Modifier.fillMaxWidth().clickable { destination = "Player" }, shape = RoundedCornerShape(25.dp)) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Art(model, current.uri, Modifier.size(44.dp), current.artworkUri)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(current.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                            Text(current.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = model::togglePlay) { AppIcon(if (playback.playing) "pause" else "play", if (playback.playing) "Pause" else "Play") }
                        IconButton(onClick = model::next) { AppIcon("next", "Next track") }
                    }
                }
                LiquidNavigation(destination) { destination = it }
            }
        }) { insets ->
            Column(Modifier.fillMaxSize().padding(insets)) {
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (destination == "Player") IconButton(onClick = { destination = "Home" }) { AppIcon("back", "Back to Home") }
                    Column(Modifier.weight(1f)) {
                        Text("msc", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        Text("MUSIC, IN YOUR HANDS", style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                    GlassPanel(shape = RoundedCornerShape(20.dp)) { IconButton(onClick = { showSettings = true }) { AppIcon("settings", "Settings") } }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Crossfade(destination, modifier = Modifier.weight(1f).fillMaxWidth(),
                    animationSpec = if (!reduced && !systemReduced) tween(180) else snap(), label = "Page flow") { page ->
                when (page) {
                    "Home" -> HomeScreen(model, tracks, current, onImport, onScan,
                        { track, queue ->
                            if (track.uri == playback.current?.uri) { if (!playback.playing) model.togglePlay() }
                            else model.play(track, queue)
                            destination = "Player"
                        }, { addTrack = it }, { model.useOnlineSearch(true); destination = "Search" })
                    "Library" -> LibraryScreen(model, tracks, playlists, onImport, onScan,
                        { track, queue -> model.play(track, queue) }, { addTrack = it }, { createPlaylist = true }, { selectedPlaylist = it })
                    "Search" -> SearchScreen(model, { track, queue -> model.play(track, queue) }, { addTrack = it }, { showSettings = true })
                    else -> PlayerScreen(model, playback, current, art, angle, mode, onImport, onScan,
                        { showQueue = true }, { showLyrics = true }, { model.requestEffects(); showEffects = true }, { addTrack = it })
                }
                }
            }
        }
        }
        if (showSettings) SettingsSheet(model, onDismiss = { showSettings = false }, onSoundSettings, onImportFolder)
        if (showQueue) QueueSheet(model, playback, onDismiss = { showQueue = false })
        if (showLyrics && current != null) LyricsSheet(model, current, playback.position, { showLyrics = false }, { onImportLyrics(current.uri) })
        if (showEffects) EffectsSheet(model) { showEffects = false }
        val playlist = playlists.firstOrNull { it.id == selectedPlaylist }
        if (playlist != null) PlaylistSheet(model, playlist, tracks, { selectedPlaylist = null })
        if (addTrack != null) AlertDialog(onDismissRequest = { addTrack = null }, title = { Text("Add to playlist") },
            text = {
                LazyColumn { items(playlists, key = { it.id }) { p -> TextButton(onClick = { model.addToPlaylist(p, addTrack!!); addTrack = null }) { Text(p.name) } } }
            },
            confirmButton = { TextButton(onClick = { createPlaylist = true }) { Text("New playlist") } },
            dismissButton = { TextButton(onClick = { addTrack = null }) { Text("Cancel") } })
        if (createPlaylist) NameDialog("New playlist", "", { createPlaylist = false }, { name ->
            model.createPlaylist(name, addTrack); createPlaylist = false; addTrack = null
        })
    }
    }
}

@Composable private fun ImportActions(onImport: () -> Unit, onScan: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onImport) { AppIcon("add"); Spacer(Modifier.width(8.dp)); Text("Import files") }
        OutlinedButton(onClick = onScan) { Text("Scan device") }
    }
}

@Composable private fun EmptyMusic(onImport: () -> Unit, onScan: () -> Unit, onOnline: (() -> Unit)? = null) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (onOnline != null) GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("A world of music.\nA different spin.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text("Find your next favourite with JioSaavn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onOnline) { AppIcon("search"); Spacer(Modifier.width(8.dp)); Text("Explore JioSaavn") }
            }
        } else Text("Your music.\nA different spin.", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
        Text("Your own files", style = MaterialTheme.typography.titleLarge)
        Text("Import your audio to listen offline. Your files stay on your phone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ImportActions(onImport, onScan)
        Text("Local playback needs no account or connection.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun HomeScreen(model: MusicViewModel, tracks: List<Track>, current: Track?, onImport: () -> Unit,
    onScan: () -> Unit, onPlay: (Track, List<Track>) -> Unit, onAdd: (Track) -> Unit, onOnline: () -> Unit) {
    if (tracks.isEmpty()) { EmptyMusic(onImport, onScan, onOnline); return }
    val recent = remember(tracks) { tracks.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(8) }
    val added = remember(tracks) { tracks.sortedByDescending { it.addedAt }.take(20).groupBy { it.album } }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Make yourself at home.", style = MaterialTheme.typography.headlineMedium)
            Text("${tracks.size} tracks · ${tracks.map { it.album }.distinct().size} albums", color = MaterialTheme.colorScheme.onSurfaceVariant)
            ImportActions(onImport, onScan)
        } }
        item { GlassPanel(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Something new to love.", style = MaterialTheme.typography.titleLarge)
                Text("Search songs and artists online.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOnline) { Text("Explore JioSaavn"); Spacer(Modifier.width(8.dp)); AppIcon("search") }
            }
        } }
        if (current != null) {
            item { SectionTitle("Continue listening") }
            item { TrackRow(model, current, { onPlay(current, tracks) }, { onAdd(current) }) }
        }
        item { SectionTitle("Recently added") }
        item { LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(added.entries.toList(), key = { it.key }) { (album, entries) ->
                Column(Modifier.width(140.dp).clickable { onPlay(entries.first(), tracks.filter { it.album == album }) }) {
                    Art(model, entries.first().uri, Modifier.size(140.dp))
                    Text(album, Modifier.padding(top = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text(entries.first().artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } }
        if (recent.isNotEmpty()) { item { SectionTitle("Recently played") }; items(recent, key = { "recent${it.uri}" }) { t -> TrackRow(model, t, { onPlay(t, recent) }, { onAdd(t) }) } }
        val favorites = tracks.filter { it.favorite }.take(8)
        if (favorites.isNotEmpty()) { item { SectionTitle("Your favorites") }; items(favorites, key = { "favorite${it.uri}" }) { t -> TrackRow(model, t, { onPlay(t, favorites) }, { onAdd(t) }) } }
    }
}

@Composable private fun SectionTitle(title: String) { Text(title, Modifier.padding(start = 24.dp, top = 24.dp, bottom = 10.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }

@Composable private fun LibraryScreen(model: MusicViewModel, tracks: List<Track>, playlists: List<Playlist>, onImport: () -> Unit,
    onScan: () -> Unit, onPlay: (Track, List<Track>) -> Unit, onAdd: (Track) -> Unit, onCreate: () -> Unit, onPlaylist: (Long) -> Unit) {
    var category by rememberSaveable { mutableStateOf("Songs") }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(TrackSort.TITLE.name) }
    var sortMenu by remember { mutableStateOf(false) }
    val visible = remember(tracks, category, group, sort) {
        val filtered = tracks.filter { t -> when(category) {
            "Favorites" -> t.favorite; "Albums" -> group == null || t.album == group
            "Artists" -> group == null || t.artist == group; "Folders" -> group == null || t.folder == group
            "Genres" -> group == null || t.genre.ifBlank { "Unspecified" } == group; else -> true
        } }
        LibraryRules.sorted(filtered, if (category == "Recent") TrackSort.RECENT else TrackSort.valueOf(sort))
    }
    BackHandler(group != null) { group = null }
    Column {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (group != null) IconButton(onClick = { group = null }) { AppIcon("back", "Back to groups") }
            Text(group ?: "Your library", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box {
                TextButton(onClick = { sortMenu = true }) { Text("Sort") }
                DropdownMenu(sortMenu, { sortMenu = false }) { TrackSort.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = { sort = option.name; sortMenu = false })
                } }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Songs", "Albums", "Artists", "Folders", "Genres", "Playlists", "Favorites", "Recent").forEach { name ->
                FilterChip(selected = category == name, onClick = { category = name; group = null }, label = { Text(name) })
            }
        }
        if (category == "Playlists") {
            TextButton(onClick = onCreate, modifier = Modifier.padding(horizontal = 16.dp)) { AppIcon("add"); Text("Create playlist") }
            LazyColumn { items(playlists, key = { it.id }) { p ->
                ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("${p.tracks.size} tracks") },
                    leadingContent = { AppIcon("queue") }, modifier = Modifier.clickable { onPlaylist(p.id) })
            } }
        } else if (tracks.isEmpty()) EmptyMusic(onImport, onScan)
        else if (group == null && category in listOf("Albums", "Artists", "Folders", "Genres")) {
            val groups = remember(visible, category) { visible.groupBy { when(category) { "Albums" -> it.album; "Artists" -> it.artist; "Folders" -> it.folder; else -> it.genre.ifBlank { "Unspecified" } } } }
            LazyColumn { items(groups.entries.toList(), key = { it.key }) { (name, entries) ->
                ListItem(headlineContent = { Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis) }, supportingContent = { Text("${entries.size} tracks") },
                    leadingContent = { Art(model, entries.first().uri, Modifier.size(54.dp)) }, modifier = Modifier.clickable { group = name })
            } }
        } else {
            if (visible.isEmpty()) Text("No tracks here yet.", Modifier.padding(24.dp))
            LazyColumn { items(visible, key = { it.uri }) { t -> TrackRow(model, t, { onPlay(t, visible) }, { onAdd(t) }) } }
        }
    }
}

@Composable private fun SearchScreen(model: MusicViewModel, onPlay: (Track, List<Track>) -> Unit, onAdd: (Track) -> Unit, onSettings: () -> Unit) {
    val query by model.query.collectAsStateWithLifecycle()
    val results by model.searchResults.collectAsStateWithLifecycle()
    val provider by model.provider.collectAsStateWithLifecycle()
    val online by model.online.collectAsStateWithLifecycle()
    val playlists by model.playlists.collectAsStateWithLifecycle()
    Column {
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!online.active, { model.useOnlineSearch(false) }, label = { Text("On your phone") })
            FilterChip(online.active, { model.useOnlineSearch(true) }, label = { Text("JioSaavn") })
        }
        OutlinedTextField(query, { model.query.value = it }, label = { Text("Search music") }, placeholder = { Text("Tracks, artists, albums, playlists") },
            leadingIcon = { AppIcon("search") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), shape = RoundedCornerShape(18.dp))
        if (online.active) {
            OnlineSearchResults(online, query, model::searchOnline,
                onPlay = { song -> song.track()?.let { track -> onPlay(track, online.results.mapNotNull { it.track() }) } },
                onQueue = { song -> song.track()?.let(model::enqueue) },
                artwork = { song -> Art(model, song.streamUrl.orEmpty(), Modifier.size(52.dp), song.artworkUrl) })
        } else if (query.isBlank()) Text("Find something in your collection, or choose JioSaavn to search online.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else LazyColumn {
            item { SectionTitle("On your phone · ${results.size}") }
            if (results.isEmpty()) item { Text("No local matches.", Modifier.padding(horizontal = 24.dp)) }
            items(results, key = { it.uri }) { t -> TrackRow(model, t, { onPlay(t, results) }, { onAdd(t) }) }
            val matching = playlists.filter { LibraryRules.key(it.name).contains(LibraryRules.key(query)) }
            if (matching.isNotEmpty()) {
                item { SectionTitle("Playlists") }
                items(matching, key = { "playlist${it.id}" }) { p ->
                    val tracks by model.tracks.collectAsStateWithLifecycle()
                    val entries = p.tracks.mapNotNull { uri -> tracks.find { it.uri == uri } }
                    ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("${entries.size} available tracks · Tap to play") },
                        modifier = Modifier.clickable(enabled = entries.isNotEmpty()) { onPlay(entries.first(), entries) })
                }
            }
            item { SectionTitle("ClashFLAC · Catalog") }
            if (!provider.enabled) item { TextButton(onClick = onSettings, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Connect a ClashFLAC server") } }
            else {
                item { Text("Catalog entries are metadata only. Streaming and downloads are unavailable.", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodySmall) }
                if (provider.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp)) }
                if (provider.error != null) item { Column(Modifier.padding(24.dp)) { Text(provider.error!!); TextButton(onClick = { model.remoteSearch() }) { Text("Retry") } } }
                if (!provider.loading && provider.error == null && provider.results.isEmpty()) item { Text("No catalog matches.", Modifier.padding(24.dp)) }
                items(provider.results, key = { "clash${it.id}" }) { t -> ListItem(headlineContent = { Text(t.title) }, supportingContent = { Text("${t.artist} · ${t.album}\nClashFLAC · Catalog only") }) }
            }
        }
    }
}

@Composable private fun PlayerScreen(model: MusicViewModel, playback: PlaybackUi, current: Track?, art: Artwork, angle: MutableFloatState,
    mode: String, onImport: () -> Unit, onScan: () -> Unit, onQueue: () -> Unit, onLyrics: () -> Unit, onEffects: () -> Unit, onAdd: (Track) -> Unit) {
    if (current == null) { EmptyMusic(onImport, onScan); return }
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val discSize = minOf(maxWidth - 48.dp, maxHeight * .37f, 320.dp)
    LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(mode == "Vinyl", { model.setMode("Vinyl") }, label = { Text("Vinyl") })
            FilterChip(mode == "CD", { model.setMode("CD") }, label = { Text("CD") })
        } }
        item { GlassPanel(Modifier.padding(vertical = 12.dp), RoundedCornerShape(44.dp)) {
            Disc(art, mode == "CD", angle, Modifier.padding(16.dp).size((discSize - 32.dp).coerceAtLeast(80.dp)))
        } }
        item { Text(current.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        item { Text(current.artist, Modifier.padding(top = 5.dp, bottom = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (playback.buffering) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        playback.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); TextButton(onClick = model::togglePlay) { Text("Retry playback") } } }
        item {
            val max = playback.duration.toFloat().coerceAtLeast(1f)
            Slider(value = (if (seeking) seekValue else playback.position.toFloat()).coerceIn(0f, max),
                onValueChange = { seeking = true; seekValue = it }, valueRange = 0f..max, enabled = playback.duration > 0,
                onValueChangeFinished = { model.seek(seekValue.toLong()); seeking = false })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(timeLabel(if (seeking) seekValue.toLong() else playback.position), style = MaterialTheme.typography.labelSmall)
                Text(timeLabel(playback.duration), style = MaterialTheme.typography.labelSmall)
            }
        }
        item { GlassPanel(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = model::shuffle) { Text(if (playback.shuffle) "Shuffle ✓" else "Shuffle", style = MaterialTheme.typography.labelSmall) }
            IconButton(onClick = model::previous) { AppIcon("previous", "Previous track") }
            FilledIconButton(onClick = model::togglePlay, modifier = Modifier.size(68.dp)) { AppIcon(if (playback.playing) "pause" else "play", if (playback.playing) "Pause" else "Play") }
            IconButton(onClick = model::next) { AppIcon("next", "Next track") }
            TextButton(onClick = model::repeat) { Text(when(playback.repeat) { 1 -> "Repeat 1"; 2 -> "Repeat all"; else -> "Repeat off" }, style = MaterialTheme.typography.labelSmall) }
        } }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (current.source != "jiosaavn") TextButton(onClick = { model.favorite(current) }) { Text(if (current.favorite) "Liked ♥" else "Like ♡") }
            TextButton(onClick = onQueue) { Text("Queue") }
            TextButton(onClick = onLyrics) { Text("Lyrics") }
            TextButton(onClick = onEffects) { Text("EQ") }
        } }
        if (current.source != "jiosaavn") item { TextButton(onClick = { onAdd(current) }) { Text("Add to playlist") } }
        else item { Text("JioSaavn · Online streaming", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
    }
    }
}

@Composable internal fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        OutlinedTextField(name, { name = it.take(80) }, label = { Text("Playlist name") }, singleLine = true)
    }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
