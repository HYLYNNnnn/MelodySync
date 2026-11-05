package com.laioffer.spotify.data

import com.laioffer.spotify.network.MediaUrlResolver
import org.junit.Assert.*
import org.junit.Test

class MediaUrlResolverTest {
    private val resolver = MediaUrlResolver("http://192.168.1.20:8080/")

    @Test fun resolvesRelativeUnicodeAndEscapedSongPathsOnce() {
        assertEquals("http://192.168.1.20:8080/songs/%E8%8F%8A%E8%8A%B1%E5%8F%B0.mp3", resolver.resolve("/songs/菊花台.mp3"))
        assertEquals("http://192.168.1.20:8080/songs/%E8%8F%8A%E8%8A%B1%E5%8F%B0.mp3", resolver.resolve("/songs/%E8%8F%8A%E8%8A%B1%E5%8F%B0.mp3"))
        assertEquals("http://192.168.1.20:8080/songs/a%20song.mp3", resolver.resolve("songs/a song.mp3"))
    }

    @Test fun rewritesOnlyLegacyEmulatorHostsAndPreservesRemoteAudio() {
        assertEquals("http://192.168.1.20:8080/songs/test.mp3?x=1", resolver.resolve("http://10.0.2.2:8080/songs/test.mp3?x=1"))
        assertEquals("https://audio.example/song.mp3", resolver.resolve("https://audio.example/song.mp3"))
    }

    @Test fun rejectsNonHttpCredentialsAndAmbiguousAddresses() {
        for (value in listOf("file:///secret", "javascript:alert(1)", "//other.example/a.mp3", "https://user:pass@audio.example/a.mp3", "", "\n/songs/a.mp3")) {
            assertThrows("Should reject $value", Exception::class.java) { resolver.resolve(value) }
        }
    }
}
