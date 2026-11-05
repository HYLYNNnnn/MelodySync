package com.laioffer.spotify.repository

import com.laioffer.spotify.database.DatabaseDao
import com.laioffer.spotify.datamodel.Album
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class FavoriteAlbumRepository @Inject constructor(private val databaseDao: DatabaseDao) {
    fun isFavoriteAlbum(id: Int): Flow<Boolean> = databaseDao.isFavoriteAlbum(id).distinctUntilChanged()

    suspend fun favoriteAlbum(album: Album) {
        require(album.id > 0) { "Select an album before saving it." }
        databaseDao.favoriteAlbum(album)
    }

    suspend fun unFavoriteAlbum(album: Album) = databaseDao.unFavoriteAlbum(album)

    fun fetchFavoriteAlbums(): Flow<List<Album>> = databaseDao.fetchFavoriteAlbums().distinctUntilChanged()
}
