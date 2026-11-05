package com.laioffer.spotify.player

import com.laioffer.spotify.datamodel.Album
import com.laioffer.spotify.datamodel.Song

/** Small player boundary so track changes and failures can be tested without a device. */
interface PlaybackEngine {
    var listener: ((PlaybackStatus) -> Unit)?
    val status: PlaybackStatus
    fun load(song: Song, album: Album)
    fun play()
    fun pause()
    fun seekToStart()
    fun stop()
    fun release()
}

data class PlaybackStatus(
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val isLoading: Boolean = false,
    val isEnded: Boolean = false,
    val error: String? = null
)
