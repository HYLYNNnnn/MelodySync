package com.laioffer.spotify.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.laioffer.spotify.datamodel.Album
import com.laioffer.spotify.datamodel.Song

internal class Media3PlaybackEngine(private val player: ExoPlayer) : PlaybackEngine {
    override var listener: ((PlaybackStatus) -> Unit)? = null
    private var released = false
    private val events = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (!released) listener?.invoke(status)
        }
    }

    init { player.addListener(events) }

    override val status: PlaybackStatus
        get() = PlaybackStatus(
            isPlaying = !released && player.isPlaying,
            playWhenReady = !released && player.playWhenReady,
            isLoading = !released && player.playbackState == Player.STATE_BUFFERING,
            isEnded = !released && player.playbackState == Player.STATE_ENDED,
            error = if (released) null else player.playerError?.let(::readableError)
        )

    override fun load(song: Song, album: Album) {
        check(!released)
        player.playWhenReady = false
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(song.src)
                .setMediaId(song.src)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.name)
                        .setArtist(album.artists)
                        .setAlbumTitle(album.name)
                        .build()
                )
                .build()
        )
        player.prepare()
    }

    override fun play() { check(!released); player.play() }
    override fun pause() { if (!released) player.pause() }
    override fun seekToStart() { check(!released); player.seekTo(0) }
    override fun stop() {
        if (!released) {
            player.stop()
            player.clearMediaItems()
        }
    }

    override fun release() {
        if (released) return
        listener = null
        player.removeListener(events)
        released = true
        player.release()
    }

    private fun readableError(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "Cannot reach the music server. Check your connection and try again."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "This track is unavailable on the music server. Try again or select another track."
        else -> "This track could not be played. Try again or select another track."
    }
}
