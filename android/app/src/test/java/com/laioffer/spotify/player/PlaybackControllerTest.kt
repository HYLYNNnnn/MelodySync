package com.laioffer.spotify.player

import com.laioffer.spotify.datamodel.Album
import com.laioffer.spotify.datamodel.Song
import org.junit.Assert.*
import org.junit.Test

class PlaybackControllerTest {
    private val album = Album(1, "Album one", "2025", "", "An artist", "A description")
    private val first = Song("First", "", "http://10.0.2.2:8080/songs/first.mp3", "1:00")
    private val second = Song("Second", "", "https://example.test/second.mp3", "2:00")

    @Test fun selectingAnAlreadyLoadedTrackKeepsItsPositionAndPlayback() {
        val engine = FakeEngine()
        val controller = PlaybackController(engine)
        controller.load(first, album)
        controller.play()
        controller.load(first, album)
        assertEquals(listOf(first), engine.loaded)
        assertTrue(controller.uiState.value.isPlaying)
        assertEquals(first, controller.uiState.value.song)
        assertEquals(album, controller.uiState.value.album)
    }

    @Test fun selectingAnotherTrackSwitchesItsMetadataAndPlayback() {
        val engine = FakeEngine()
        val controller = PlaybackController(engine)
        controller.load(first, album)
        controller.play()
        val nextAlbum = album.copy(id = 2, name = "Another album")
        controller.load(second, nextAlbum)
        assertFalse(controller.uiState.value.isPlaying)
        assertEquals(second, controller.uiState.value.song)
        assertEquals(nextAlbum, controller.uiState.value.album)
        controller.play()
        assertTrue(controller.uiState.value.isPlaying)
        assertEquals(listOf(first, second), engine.loaded)
    }

    @Test fun bufferingCanBePausedBeforeItStartsPlaying() {
        val engine = FakeEngine()
        val controller = PlaybackController(engine)
        controller.load(first, album)
        engine.emit(PlaybackStatus(playWhenReady = true, isLoading = true))
        controller.togglePlayback()
        assertEquals(1, engine.pauseCalls)
        assertFalse(controller.uiState.value.playWhenReady)
    }

    @Test fun finishedTrackReplaysFromTheBeginning() {
        val engine = FakeEngine()
        val controller = PlaybackController(engine)
        controller.load(first, album)
        engine.emit(PlaybackStatus(isEnded = true))
        controller.play()
        assertEquals(1, engine.seekCalls)
        assertTrue(controller.uiState.value.isPlaying)
    }

    @Test fun failedPlaybackCanReloadTheSameTrackAndRecover() {
        val engine = FakeEngine()
        val controller = PlaybackController(engine)
        controller.load(first, album)
        engine.emit(PlaybackStatus(error = "Track unavailable"))
        assertEquals("Track unavailable", controller.uiState.value.error)
        controller.retry()
        assertEquals(listOf(first, first), engine.loaded)
        assertNull(controller.uiState.value.error)
        assertTrue(controller.uiState.value.isPlaying)
    }

    @Test fun invalidAddressesDoNotReachThePlayerAndCanBeReplaced() {
        val engine = FakeEngine()
        val controller = PlaybackController(engine)
        for (address in listOf("file:///private/audio.mp3", "https:///missing-host.mp3", "not a url")) {
            controller.load(first.copy(src = address), album)
            controller.play()
            assertNotNull(controller.uiState.value.error)
        }
        assertTrue(engine.loaded.isEmpty())
        controller.load(first, album)
        controller.play()
        assertTrue(controller.uiState.value.isPlaying)
    }

    @Test fun commandFailuresBecomeSafeErrorsAndRetryCanRecover() {
        val engine = FakeEngine()
        val controller = PlaybackController(engine)
        controller.load(first, album)
        engine.failPlay = true
        controller.play()
        assertFalse(controller.uiState.value.isPlaying)
        assertNotNull(controller.uiState.value.error)
        assertFalse(controller.uiState.value.error!!.contains("private"))
        engine.failPlay = false
        controller.retry()
        assertTrue(controller.uiState.value.isPlaying)
        assertNull(controller.uiState.value.error)
    }

    @Test fun closingTrackAndClearingOwnerStopAndReleaseExactlyOnce() {
        val engine = FakeEngine()
        val controller = PlaybackController(engine)
        controller.load(first, album)
        controller.play()
        controller.stop()
        assertNull(controller.uiState.value.song)
        assertFalse(controller.uiState.value.isPlaying)
        controller.load(second, album)
        val staleListener = engine.listener
        controller.close()
        controller.close()
        staleListener?.invoke(PlaybackStatus(isPlaying = true))
        controller.play()
        controller.load(first, album)
        assertNull(engine.listener)
        assertEquals(1, engine.releaseCalls)
        assertNull(controller.uiState.value.song)
        assertEquals(listOf(first, second), engine.loaded)
    }

    private class FakeEngine : PlaybackEngine {
        override var listener: ((PlaybackStatus) -> Unit)? = null
        override var status = PlaybackStatus()
        val loaded = mutableListOf<Song>()
        var pauseCalls = 0
        var seekCalls = 0
        var releaseCalls = 0
        var failPlay = false

        fun emit(value: PlaybackStatus) { status = value; listener?.invoke(value) }
        override fun load(song: Song, album: Album) { loaded += song; emit(PlaybackStatus()) }
        override fun play() {
            if (failPlay) error("private internal error")
            emit(PlaybackStatus(isPlaying = true, playWhenReady = true))
        }
        override fun pause() { pauseCalls++; emit(PlaybackStatus()) }
        override fun seekToStart() { seekCalls++; emit(PlaybackStatus()) }
        override fun stop() { emit(PlaybackStatus()) }
        override fun release() { releaseCalls++ }
    }
}
