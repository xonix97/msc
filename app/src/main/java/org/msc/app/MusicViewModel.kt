package org.msc.app

import android.app.Application
import android.content.ComponentName
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.msc.app.data.*
import org.msc.app.playback.*
import org.msc.app.providers.*
import org.msc.app.clash.ClashControl

data class PlaybackUi(val connected: Boolean = false, val queue: List<Track> = emptyList(), val index: Int = -1,
    val position: Long = 0, val duration: Long = 0, val playing: Boolean = false, val buffering: Boolean = false,
    val shuffle: Boolean = false, val repeat: Int = 0, val error: String? = null) {
    val current get() = queue.getOrNull(index)
}
data class LyricsUi(val body: String = "", val offset: Long = 0)
data class ProviderUi(val enabled: Boolean = false, val url: String = "", val loading: Boolean = false,
    val results: List<CatalogTrack> = emptyList(), val error: String? = null, val onDevice: Boolean = false)

@OptIn(kotlinx.coroutines.FlowPreview::class)
@androidx.annotation.OptIn(UnstableApi::class)
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    val repository = (application as MscApplication).library
    val artwork = (application as MscApplication).artwork
    val tracks = repository.tracks
    val playlists = repository.playlists
    private val preferences = application.getSharedPreferences("msc-settings", 0)
    val mode = MutableStateFlow(preferences.getString("mode", "Vinyl") ?: "Vinyl")
    val reducedMotion = MutableStateFlow(preferences.getBoolean("reducedMotion", false))
    val busy = MutableStateFlow(false)
    val playback = MutableStateFlow(PlaybackUi())
    val query = MutableStateFlow("")
    val searchResults = combine(tracks, query.debounce(250)) { all, q -> LibraryRules.search(all, q) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lyrics = MutableStateFlow(LyricsUi())
    val effects = MutableStateFlow(Bundle())
    val provider = MutableStateFlow(ProviderUi(preferences.getBoolean("clashEnabled", false), preferences.getString("clashUrl", "").orEmpty(),
        onDevice = BuildConfig.EMBEDDED_CLASH && preferences.getBoolean("clashOnDevice", false)))
    val clashControl = ClashControl(application)
    private val catalog = ClashFlacCatalog()
    private val messages = Channel<String>(Channel.UNLIMITED)
    val events = messages.receiveAsFlow()
    private var controller: MediaController? = null
    private val future = MediaController.Builder(application, SessionToken(application, ComponentName(application, PlaybackService::class.java))).buildAsync()
    private var ticker: Job? = null
    private var scanJob: Job? = null
    private var remoteJob: Job? = null
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scanJob?.cancel(); scanJob = viewModelScope.launch { delay(800); scan(false) }
        }
    }
    init {
        viewModelScope.launch(Dispatchers.IO) { repository.refresh() }
        application.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        future.addListener({
            try {
                controller = future.get().also { it.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) { updatePlayer(true) }
                }) }
                updatePlayer(true)
            } catch (e: Exception) { notify("Audio service could not connect: ${e.cause?.message ?: e.message}") }
        }, ContextCompat.getMainExecutor(application))
        viewModelScope.launch {
            playback.map { it.current?.uri }.distinctUntilChanged().collectLatest { uri ->
                lyrics.value = if (uri == null) LyricsUi() else withContext(Dispatchers.IO) {
                    repository.lyrics(uri).let { LyricsUi(it.first, it.second) }
                }
            }
        }
        viewModelScope.launch { query.debounce(400).collectLatest { remoteSearch(it) } }
        if (BuildConfig.EMBEDDED_CLASH) {
            clashControl.connect()
            viewModelScope.launch { clashControl.state.map { it.state }.distinctUntilChanged().collect { if (provider.value.onDevice) remoteSearch() } }
        }
    }
    fun notify(message: String) { messages.trySend(message) }
    fun foreground(active: Boolean) {
        ticker?.cancel()
        if (active) ticker = viewModelScope.launch { while (isActive) { updatePlayer(); delay(250) } }
    }
    private fun updatePlayer(refreshQueue: Boolean = false) {
        val p = controller ?: return
        val queue = if (refreshQueue) (0 until p.mediaItemCount).map { p.getMediaItemAt(it).track() } else playback.value.queue
        playback.value = PlaybackUi(true, queue,
            p.currentMediaItemIndex, p.currentPosition.coerceAtLeast(0), p.duration.coerceAtLeast(0), p.isPlaying,
            p.playbackState == Player.STATE_BUFFERING, p.shuffleModeEnabled, p.repeatMode,
            p.playerError?.let { "Cannot play this file. It may be unavailable, unsupported or its permission was revoked. ${it.errorCodeName}" })
    }
    private fun player(block: (MediaController) -> Unit) {
        val p = controller
        if (p == null) { notify("Connecting to the audio service. Please try again."); return }
        block(p); updatePlayer(true)
    }
    fun play(track: Track, queue: List<Track> = tracks.value) = player { p ->
        val list = queue.ifEmpty { listOf(track) }; val index = list.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)
        p.setMediaItems(list.map { it.mediaItem() }, index, 0); p.prepare(); p.play()
    }
    fun togglePlay() = player { if (it.isPlaying) it.pause() else { it.prepare(); it.play() } }
    fun previous() = player { it.seekToPrevious() }
    fun next() = player { it.seekToNext() }
    fun seek(position: Long) = player { it.seekTo(position.coerceAtLeast(0)) }
    fun shuffle() = player { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun repeat() = player { it.repeatMode = (it.repeatMode + 1) % 3 }
    fun enqueue(track: Track) = player { it.addMediaItem(track.mediaItem()); notify("Added to queue") }
    fun queuePlay(index: Int) = player { it.seekTo(index, 0); it.prepare(); it.play() }
    fun queueMove(from: Int, to: Int) = player { if (to in 0 until it.mediaItemCount) it.moveMediaItem(from, to) }
    fun queueRemove(index: Int) = player { it.removeMediaItem(index) }
    fun setMode(value: String) { mode.value = value; preferences.edit().putString("mode", value).apply() }
    fun setReducedMotion(value: Boolean) { reducedMotion.value = value; preferences.edit().putBoolean("reducedMotion", value).apply() }
    fun favorite(track: Track) = task { repository.toggleFavorite(track.uri) }
    fun remove(track: Track) = task { repository.removeFromLibrary(track.uri) }
    fun createPlaylist(name: String, track: Track? = null) = task {
        val id = repository.createPlaylist(name); if (track != null) repository.setPlaylistTracks(id, listOf(track.uri))
    }
    fun renamePlaylist(id: Long, name: String) = task { repository.renamePlaylist(id, name) }
    fun deletePlaylist(id: Long) = task { repository.deletePlaylist(id) }
    fun addToPlaylist(playlist: Playlist, track: Track) = task { repository.setPlaylistTracks(playlist.id, playlist.tracks + track.uri) }
    fun updatePlaylist(playlist: Playlist, list: List<String>) = task { repository.setPlaylistTracks(playlist.id, list) }
    private fun task(block: () -> Unit) { viewModelScope.launch(Dispatchers.IO) {
        try { block() } catch (e: Exception) { if (e is CancellationException) throw e; notify(e.message ?: "This action could not be completed.") }
    } }
    fun importFiles(uris: List<Uri>) { viewModelScope.launch {
        if (busy.value) { notify("Wait for the current library update to finish."); return@launch }
        busy.value = true
        try {
            var imported = 0; var failed = 0
            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    ensureActive()
                    try { repository.importUri(uri); imported++ } catch (e: Exception) { if (e is CancellationException) throw e; failed++ }
                }
                repository.refresh()
            }
            notify("Imported $imported track(s)" + if (failed > 0) "; $failed could not be read as accessible audio." else "")
        } finally { busy.value = false }
    } }
    fun scan(manual: Boolean = true) { viewModelScope.launch {
        if (busy.value) return@launch
        val context = getApplication<Application>()
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_AUDIO else android.Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (manual) notify("Allow music access to scan, or import files without granting library access.")
            return@launch
        }
        busy.value = true
        try {
            val count = withContext(Dispatchers.IO) { repository.scanDevice() }
            if (manual) notify("Found $count device tracks")
        } catch (e: Exception) { if (e is CancellationException) throw e; if (manual) notify(e.message ?: "Music scan failed.") }
        finally { busy.value = false }
    } }
    fun importFolder(uri: Uri) { viewModelScope.launch {
        if (busy.value) { notify("Wait for the current library update to finish."); return@launch }
        busy.value = true
        try {
            val result = withContext(Dispatchers.IO) { FolderImporter(getApplication(), repository).scan(uri) }
            val folders = preferences.getStringSet("folders", emptySet()).orEmpty() + uri.toString()
            preferences.edit().putStringSet("folders", folders).apply()
            notify("Imported ${result.first} folder tracks; ${result.second} unreadable files.")
        } catch (e: Exception) { if (e is CancellationException) throw e; notify(e.message ?: "Folder import failed.") }
        finally { busy.value = false }
    } }
    fun rescanFolders() {
        val folders = preferences.getStringSet("folders", emptySet()).orEmpty()
        if (folders.isEmpty()) { notify("Import a folder first."); return }
        viewModelScope.launch {
            if (busy.value) return@launch
            busy.value = true
            try {
                withContext(Dispatchers.IO) { for (folder in folders) FolderImporter(getApplication(), repository).scan(Uri.parse(folder)) }
                notify("Saved folders refreshed")
            } catch (e: Exception) { if (e is CancellationException) throw e; notify(e.message ?: "A folder is unavailable. Grant access by selecting it again.") }
            finally { busy.value = false }
        }
    }
    fun importLyrics(uri: Uri, target: String) = task {
        val output = java.io.ByteArrayOutputStream()
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8192)
            while (true) { val n = input.read(buffer); if (n < 0) break; require(output.size() + n <= 2 * 1024 * 1024) { "Lyrics file is too large." }; output.write(buffer, 0, n) }
        } ?: error("The lyrics file could not be opened.")
        val body = output.toString("UTF-8").removePrefix("\uFEFF")
        repository.saveLyrics(target, body)
        if (playback.value.current?.uri == target) lyrics.value = LyricsUi(body)
        notify("Lyrics imported")
    }
    fun lyricsOffset(value: Long) {
        val uri = playback.value.current?.uri ?: return
        lyrics.value = lyrics.value.copy(offset = value)
        val snapshot = lyrics.value
        task { repository.saveLyrics(uri, snapshot.body, value) }
    }
    fun requestEffects(args: Bundle? = null) = player { p ->
        val future = p.sendCustomCommand(SessionCommand(if (args == null) PlaybackService.GET_EFFECTS else PlaybackService.SET_EFFECTS, Bundle.EMPTY), args ?: Bundle.EMPTY)
        future.addListener({ try { effects.value = future.get().extras } catch (e: Exception) { notify("Audio effects could not be queried: ${e.message}") } }, ContextCompat.getMainExecutor(getApplication()))
    }
    fun configureProvider(url: String, enabled: Boolean) {
        try {
            val validated = if (enabled || url.isNotBlank()) catalog.validateBaseUrl(url) else ""
            preferences.edit().putString("clashUrl", validated).putBoolean("clashEnabled", enabled).apply()
            preferences.edit().putBoolean("clashOnDevice", false).apply()
            provider.value = ProviderUi(enabled, validated); remoteSearch(query.value)
        } catch (e: Exception) { notify(e.message ?: "Invalid server address") }
    }
    fun useOnDeviceClash(enabled: Boolean) {
        check(BuildConfig.EMBEDDED_CLASH)
        preferences.edit().putBoolean("clashOnDevice", enabled).putBoolean("clashEnabled", enabled).apply()
        provider.value = provider.value.copy(onDevice = enabled, enabled = enabled, results = emptyList(), error = null)
        remoteSearch()
    }
    fun remoteSearch(q: String = query.value) {
        remoteJob?.cancel()
        val settings = provider.value
        if (!settings.enabled || q.trim().length < 2) { provider.value = settings.copy(loading = false, results = emptyList(), error = null); return }
        remoteJob = viewModelScope.launch {
            provider.value = settings.copy(loading = true, error = null)
            try {
                val results = withContext(Dispatchers.IO) {
                    if (settings.onDevice) {
                        val local = clashControl.state.value
                        check(local.state == "RUNNING") { "Start the on-device ClashFLAC server in Settings." }
                        catalog.searchLocal(local.port, local.token, q.trim())
                    } else catalog.search(settings.url, q.trim())
                }
                ensureActive(); provider.value = settings.copy(results = results)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                provider.value = settings.copy(error = e.message ?: "ClashFLAC is unavailable.")
            }
        }
    }
    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(observer)
        clashControl.close()
        MediaController.releaseFuture(future); super.onCleared()
    }
}
