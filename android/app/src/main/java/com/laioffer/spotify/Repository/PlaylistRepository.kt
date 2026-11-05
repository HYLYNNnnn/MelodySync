package com.laioffer.spotify.repository

import com.laioffer.spotify.datamodel.Playlist
import com.laioffer.spotify.network.MediaUrlResolver
import com.laioffer.spotify.network.NetworkApi
import javax.inject.Inject

class PlaylistRepository @Inject constructor(
    private val networkApi: NetworkApi,
    private val mediaUrls: MediaUrlResolver,
) {
    suspend fun getPlaylist(id: Int): Playlist {
        if (id < 1) throw MusicDataException("Select an album before loading its songs.")
        val playlist = networkApi.getPlaylist(id).requireBody("album")
        if (playlist.albumId != id) throw MusicDataException("The music server returned a different album. Please retry.")
        return try {
            playlist.copy(songs = playlist.songs.map { it.copy(src = mediaUrls.resolve(it.src)) })
        } catch (error: IllegalArgumentException) {
            throw MusicDataException("This album contains an invalid audio address.", error)
        } catch (error: java.net.URISyntaxException) {
            throw MusicDataException("This album contains an invalid audio address.", error)
        }
    }
}
