package com.laioffer.spotify.network

import com.laioffer.spotify.datamodel.Playlist
import com.laioffer.spotify.datamodel.Section
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface NetworkApi {
    @GET("feed")
    suspend fun getHomeFeed(): Response<List<Section>>

    @GET("playlist/{id}")
    suspend fun getPlaylist(@Path("id") id: Int): Response<Playlist>
}
