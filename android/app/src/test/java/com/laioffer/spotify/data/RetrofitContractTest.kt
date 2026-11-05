package com.laioffer.spotify.data

import com.laioffer.spotify.network.MediaUrlResolver
import com.laioffer.spotify.network.NetworkApi
import com.laioffer.spotify.repository.HomeRepository
import com.laioffer.spotify.repository.PlaylistRepository
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitContractTest {
    @Test fun parsesActualBackendFeedAndPlaylistShapesWithoutARealNetworkCall() = runTest {
        val requests = mutableListOf<String>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            synchronized(requests) { requests += request.url.encodedPath }
            val json = when (request.url.encodedPath) {
                "/feed" -> """[{"section_title":"Made for you","albums":[{"id":2,"album":"A title","year":"2025","cover":"https://image.example/a.jpg","artists":"An artist","description":"Description"}]}]"""
                "/playlist/2" -> """{"id":2,"songs":[{"name":"A song","lyric":"An artist","src":"/songs/%E8%8F%8A%E8%8A%B1%E5%8F%B0.mp3","length":"0:30"}]}"""
                else -> throw AssertionError("Unexpected request: ${request.url}")
            }
            okhttp3.Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body(json.toResponseBody("application/json".toMediaType())).build()
        }.build()
        try {
            val api = Retrofit.Builder().baseUrl("http://localhost:8080/").client(client)
                .addConverterFactory(GsonConverterFactory.create()).build().create(NetworkApi::class.java)
            val feed = HomeRepository(api).getHomeSections()
            assertEquals("Made for you", feed.single().sectionTitle)
            assertEquals("A title", feed.single().albums.single().name)
            val songs = PlaylistRepository(api, MediaUrlResolver("http://localhost:8080/")).getPlaylist(2)
            assertEquals(2, songs.albumId)
            assertEquals("http://localhost:8080/songs/%E8%8F%8A%E8%8A%B1%E5%8F%B0.mp3", songs.songs.single().src)
            assertEquals(listOf("/feed", "/playlist/2"), requests)
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }
}
