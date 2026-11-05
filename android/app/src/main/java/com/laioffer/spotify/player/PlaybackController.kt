package com.laioffer.spotify.player

import com.laioffer.spotify.datamodel.Album
import com.laioffer.spotify.datamodel.Song
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val song: Song? = null,
    val album: Album? = null,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val isLoading: Boolean = false,
    val isEnded: Boolean = false,
    val error: String? = null
)

/** Activity-scoped playback. The owning ViewModel retains this across configuration changes. */
class PlaybackController(private val engine: PlaybackEngine) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    private var closed = false
    private var loaded = false
    private var commandError: String? = null

    init {
        engine.listener = { status -> updateStatus(status) }
    }

    fun load(song: Song, album: Album) {
        if (closed) return
        if (loaded && _uiState.value.song?.src == song.src && _uiState.value.album?.id == album.id) return
        loadTrack(song, album)
    }

    private fun loadTrack(song: Song, album: Album) {
        loaded = false
        commandError = null
        _uiState.value = PlayerUiState(song = song, album = album, isLoading = true)
        val uri = runCatching { URI(song.src) }.getOrNull()
        if (uri == null || uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) {
            runCatching { engine.stop() }
            fail("This track has an invalid audio address. Select another track.")
            return
        }
        perform("This track could not be loaded. Please try again.") {
            engine.load(song, album)
            loaded = true
        }
    }

    fun play() {
        if (closed || !loaded) return
        if (_uiState.value.error != null) {
            retry()
            return
        }
        perform("Playback could not start. Please try again.") {
            if (engine.status.isEnded) engine.seekToStart()
            engine.play()
        }
    }

    fun pause() {
        if (closed || !loaded) return
        perform("Playback could not be paused. Select another track.") { engine.pause() }
    }

    fun togglePlayback() {
        if (_uiState.value.playWhenReady) pause() else play()
    }

    fun retry() {
        if (closed) return
        val song = _uiState.value.song ?: return
        val album = _uiState.value.album ?: return
        loadTrack(song, album)
        if (loaded && _uiState.value.error == null) play()
    }

    fun stop() {
        if (closed) return
        runCatching { engine.stop() }
        loaded = false
        commandError = null
        _uiState.value = PlayerUiState()
    }

    fun close() {
        if (closed) return
        closed = true
        loaded = false
        engine.listener = null
        runCatching { engine.release() }
        _uiState.value = PlayerUiState()
    }

    private inline fun perform(message: String, block: () -> Unit) {
        try {
            block()
            updateStatus(engine.status)
        } catch (_: Exception) {
            fail(message)
        }
    }

    private fun fail(message: String) {
        commandError = message
        runCatching { engine.pause() }
        _uiState.value = _uiState.value.copy(isPlaying = false, playWhenReady = false, isLoading = false, error = message)
    }

    private fun updateStatus(status: PlaybackStatus) {
        if (closed || _uiState.value.song == null) return
        _uiState.value = _uiState.value.copy(
            isPlaying = status.isPlaying,
            playWhenReady = status.playWhenReady,
            isLoading = status.isLoading,
            isEnded = status.isEnded,
            error = commandError ?: status.error
        )
    }
}
