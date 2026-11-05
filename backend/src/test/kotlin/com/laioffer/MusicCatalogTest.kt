package com.laioffer

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MusicCatalogTest {
    private val album = Album(1, "Demo", "2025", "https://example.com/cover.jpg", "Demo artist", "Demo album")
    private val song = Song("Demo song", "Demo artist", "/songs/demo.mp3", "1:23")

    private fun load(
        feed: List<Section> = listOf(Section("Mixes", listOf(album))),
        playlists: List<Playlist> = listOf(Playlist(1, listOf(song))),
        audioExists: (String) -> Boolean = { true },
    ): MusicCatalog = MusicCatalog.load(
        readText = { if (it == "feed.json") Json.encodeToString(feed) else Json.encodeToString(playlists) },
        audioExists = audioExists,
    )

    @Test
    fun bundledCatalogHasAllMatchingPlaylistsAndAudioResources() {
        val catalog = MusicCatalog.load()
        assertEquals(7, catalog.playlists.size)
        assertEquals(15, catalog.audioNames.size)
        assertTrue(catalog.feed.flatMap { it.albums }.all { catalog.playlist(it.id) != null })
        assertNull(catalog.playlist(99999))
    }

    @Test
    fun repeatedAlbumsAreAllowedWhenTheirMetadataAgrees() {
        val catalog = load(feed = listOf(Section("One", listOf(album)), Section("Two", listOf(album))))
        assertEquals(2, catalog.feed.size)
        assertEquals(setOf("demo.mp3"), catalog.audioNames)
    }

    @Test
    fun catalogRejectsMissingMalformedAndUnmatchedResources() {
        assertFailsWith<IllegalArgumentException> { MusicCatalog.load(readText = { null }, audioExists = { true }) }
        assertFailsWith<IllegalArgumentException> { MusicCatalog.load(readText = { "not JSON" }, audioExists = { true }) }
        assertFailsWith<IllegalArgumentException> { load(audioExists = { false }) }
        assertFailsWith<IllegalArgumentException> { load(playlists = listOf(Playlist(2, listOf(song)))) }
        assertFailsWith<IllegalArgumentException> { load(playlists = listOf(Playlist(1, listOf(song)), Playlist(1, listOf(song)))) }
        assertFailsWith<IllegalArgumentException> {
            load(feed = listOf(Section("One", listOf(album)), Section("Two", listOf(album.copy(name = "Different")))))
        }
    }

    @Test
    fun catalogRejectsRemoteOrEscapingAudioPathsAndInvalidLengths() {
        for (src in listOf("http://example.com/demo.mp3", "//example.com/demo.mp3", "/songs/../secret.mp3", "/songs/%2e%2e%2fsecret.mp3", "/songs/demo.mp3?key=secret", "/songs/demo.mp3#fragment", "/other/demo.mp3")) {
            assertFailsWith<IllegalArgumentException>(src) { load(playlists = listOf(Playlist(1, listOf(song.copy(src = src))))) }
        }
        assertFailsWith<IllegalArgumentException> { load(playlists = listOf(Playlist(1, listOf(song.copy(length = "4.53"))))) }
    }

    @Test
    fun serverConfigUsesExplicitValidatedEnvironmentValues() {
        assertEquals(ServerConfig("127.0.0.1", 8080), ServerConfig.fromEnvironment(emptyMap()))
        assertEquals(ServerConfig("0.0.0.0", 8090), ServerConfig.fromEnvironment(mapOf("HOST" to "0.0.0.0", "PORT" to "8090")))
        for (port in listOf("0", "65536", "eight", "-1")) {
            assertFailsWith<IllegalArgumentException> { ServerConfig.fromEnvironment(mapOf("PORT" to port)) }
        }
        assertFailsWith<IllegalArgumentException> { ServerConfig.fromEnvironment(mapOf("HOST" to "bad host")) }
    }
}
