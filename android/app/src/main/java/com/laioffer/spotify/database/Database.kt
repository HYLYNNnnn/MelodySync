package com.laioffer.spotify.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laioffer.spotify.datamodel.Album
import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun favoriteAlbum(album: Album)

    @Query("SELECT EXISTS(SELECT 1 FROM Album WHERE id = :id)")
    fun isFavoriteAlbum(id: Int): Flow<Boolean>

    @Delete
    suspend fun unFavoriteAlbum(album: Album)

    @Query("SELECT * FROM Album ORDER BY name COLLATE NOCASE, id")
    fun fetchFavoriteAlbums(): Flow<List<Album>>
}
