package com.laioffer.spotify.data

import com.laioffer.spotify.datamodel.*
import com.laioffer.spotify.network.*
import com.laioffer.spotify.repository.*
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class RepositoryTest {
    private val urls = MediaUrlResolver("http://localhost:8080/")

    @Test fun rejectsHttpErrorAndEmptyBodiesInsteadOfReturningAnEmptySuccess() = runTest {
        val api = FakeApi()
        api.feed = { Response.error(503, "upstream private details".toResponseBody("text/plain".toMediaType())) }
        val repository = HomeRepository(api)
        try { repository.getHomeSections(); fail("Expected an error") }
        catch (error: MusicDataException) { assertTrue(error.message!!.contains("unavailable")); assertFalse(error.message!!.contains("private")) }
        api.feed = { Response.success(null) }
        try { repository.getHomeSections(); fail("Expected an error") }
        catch (error: MusicDataException) { assertTrue(error.message!!.contains("empty")) }
    }

    @Test fun playlistChecksIdentityAndNormalizesMediaAddresses() = runTest {
        val api = FakeApi()
        api.playlist = { id -> Response.success(Playlist(id, listOf(Song("Song", "Artist", "/songs/a.mp3", "0:10")))) }
        assertEquals("http://localhost:8080/songs/a.mp3", PlaylistRepository(api, urls).getPlaylist(2).songs.single().src)
        api.playlist = { Response.success(Playlist(99, emptyList())) }
        try { PlaylistRepository(api, urls).getPlaylist(2); fail("Expected identity mismatch") }
        catch (error: MusicDataException) { assertTrue(error.message!!.contains("different album")) }
    }

    @Test fun cancellationPropagatesAndInvalidAudioIsAReadableFailure() = runTest {
        val api = FakeApi()
        api.feed = { throw CancellationException("cancelled") }
        try { HomeRepository(api).getHomeSections(); fail("Expected cancellation") }
        catch (error: CancellationException) { assertEquals("cancelled", error.message) }
        api.playlist = { id -> Response.success(Playlist(id, listOf(Song("bad", "", "file:///secret", "0:00")))) }
        try { PlaylistRepository(api, urls).getPlaylist(1); fail("Expected rejection") }
        catch (error: MusicDataException) { assertTrue(error.message!!.contains("invalid audio")) }
    }
}

internal class FakeApi : NetworkApi {
    var feed: suspend () -> Response<List<Section>> = { Response.success(emptyList()) }
    var playlist: suspend (Int) -> Response<Playlist> = { Response.success(Playlist(it, emptyList())) }
    override suspend fun getHomeFeed() = feed()
    override suspend fun getPlaylist(id: Int) = playlist(id)
}
