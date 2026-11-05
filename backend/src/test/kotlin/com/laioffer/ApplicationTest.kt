package com.laioffer

import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.core.readBytes
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun healthAndFeedUseTypedJson() = testApplication {
        application { module() }
        val health = client.get("/health")
        assertEquals(HttpStatusCode.OK, health.status)
        assertEquals(ContentType.Application.Json, health.contentType()?.withoutParameters())
        val healthJson = Json.parseToJsonElement(health.bodyAsText()).jsonObject
        assertEquals("ok", healthJson["status"]?.jsonPrimitive?.content)
        assertEquals("MelodySync", healthJson["service"]?.jsonPrimitive?.content)
        val response = client.get("/feed")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
        val feed = Json.decodeFromString<List<Section>>(response.bodyAsText())
        assertTrue(feed.isNotEmpty())
        assertTrue(feed.all { it.albums.isNotEmpty() })
    }

    @Test
    fun playlistCollectionMatchesIndividualPlaylistAndUsesRelativeAudioUrls() = testApplication {
        application { module() }
        val response = client.get("/playlists")
        val playlists = Json.decodeFromString<List<Playlist>>(response.bodyAsText())
        assertTrue(playlists.isNotEmpty())
        for (playlist in playlists) {
            val single = client.get("/playlist/${playlist.id}")
            assertEquals(HttpStatusCode.OK, single.status)
            assertEquals(playlist, Json.decodeFromString<Playlist>(single.bodyAsText()))
            assertTrue(playlist.songs.all { it.src.startsWith("/songs/") && !it.src.contains("10.0.2.2") })
        }
    }

    @Test
    fun missingMalformedAndUnknownPlaylistIdsReturnJsonErrors() = testApplication {
        application { module() }
        for (path in listOf("/playlist", "/playlist/", "/playlist/nope", "/playlist/0", "/playlist/-1", "/playlist/9999999999999999")) {
            val response = client.get(path)
            assertEquals(HttpStatusCode.BadRequest, response.status, path)
            assertEquals("INVALID_PLAYLIST_ID", Json.decodeFromString<ApiError>(response.bodyAsText()).code)
        }
        val unknown = client.get("/playlist/99999")
        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals("NOT_FOUND", Json.decodeFromString<ApiError>(unknown.bodyAsText()).code)
    }

    @Test
    fun audioSupportsHeadAndByteRanges() = testApplication {
        application { module() }
        val expected = javaClass.classLoader.getResourceAsStream("static/songs/solo.mp3")!!.use { it.readNBytes(128) }
        val head = client.head("/songs/solo.mp3")
        assertEquals(HttpStatusCode.OK, head.status)
        assertEquals("audio/mpeg", head.contentType()?.withoutParameters()?.toString())
        assertTrue(assertNotNull(head.headers[HttpHeaders.ContentLength]).toLong() > expected.size)
        assertEquals("", head.bodyAsText())
        val partial = client.get("/songs/solo.mp3") { headers.append(HttpHeaders.Range, "bytes=0-127") }
        assertEquals(HttpStatusCode.PartialContent, partial.status)
        assertTrue(assertNotNull(partial.headers[HttpHeaders.ContentRange]).startsWith("bytes 0-127/"))
        assertContentEquals(expected, partial.bodyAsChannel().readRemaining().readBytes())
        assertEquals("nosniff", partial.headers["X-Content-Type-Options"])
    }

    @Test
    fun encodedUnicodeAudioPathsResolveAndUnsatisfiableRangeIsRejected() = testApplication {
        application { module() }
        val src = MusicCatalog.load().playlists.flatMap { it.songs }.first { it.name == "夜的第七章" }.src
        assertTrue(src.contains("%"))
        val response = client.head(src)
        assertEquals(HttpStatusCode.OK, response.status)
        val invalidRange = client.get("/songs/solo.mp3") { headers.append(HttpHeaders.Range, "bytes=999999999-9999999999") }
        assertEquals(HttpStatusCode.RequestedRangeNotSatisfiable, invalidRange.status)
    }

    @Test
    fun missingSongsAndOtherClasspathResourcesStayPrivate() = testApplication {
        application { module() }
        for (path in listOf("/songs/missing.mp3", "/songs/feed.json", "/feed.json", "/playlists.json", "/logback.xml", "/songs/..%2Ffeed.json", "/unknown")) {
            val response = client.get(path)
            assertEquals(HttpStatusCode.NotFound, response.status, path)
            val body = response.bodyAsText()
            assertEquals("NOT_FOUND", Json.decodeFromString<ApiError>(body).code)
            assertFalse(body.contains("static/songs"))
        }
    }
}
