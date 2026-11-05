package com.laioffer.spotify.player

import androidx.lifecycle.ViewModel
import com.laioffer.spotify.datamodel.Album
import com.laioffer.spotify.datamodel.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(engine: PlaybackEngine) : ViewModel() {
    private val controller = PlaybackController(engine)
    val uiState = controller.uiState

    fun load(song: Song, album: Album) = controller.load(song, album)
    fun play() = controller.play()
    fun pause() = controller.pause()
    fun togglePlayback() = controller.togglePlayback()
    fun retry() = controller.retry()
    fun stop() = controller.stop()

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }
}
