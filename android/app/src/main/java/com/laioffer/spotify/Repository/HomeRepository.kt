package com.laioffer.spotify.repository

import com.laioffer.spotify.datamodel.Section
import com.laioffer.spotify.network.NetworkApi
import javax.inject.Inject

class HomeRepository @Inject constructor(private val networkApi: NetworkApi) {
    suspend fun getHomeSections(): List<Section> = networkApi.getHomeFeed().requireBody("music feed")
}
