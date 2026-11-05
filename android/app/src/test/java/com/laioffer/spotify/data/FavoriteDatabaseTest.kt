package com.laioffer.spotify.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.laioffer.spotify.database.AppDatabase
import com.laioffer.spotify.datamodel.Album
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
// Match UiStartupTest's SDK so one test process uses one native SQLite ABI.
@Config(sdk = [34], application = Application::class)
class FavoriteDatabaseTest {
    @Test fun favoritesSurviveDatabaseReopenAndCanBeRemoved() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "favorite-test-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val album = Album(1, "Album", "2025", "", "Artist", "Description")
            db.databaseDao().favoriteAlbum(album)
            db.databaseDao().favoriteAlbum(album.copy(name = "Updated album"))
            assertEquals(1, withTimeout(3000) { db.databaseDao().fetchFavoriteAlbums().first() }.size)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val saved = withTimeout(3000) { db.databaseDao().fetchFavoriteAlbums().first() }.single()
            assertEquals("Updated album", saved.name)
            assertTrue(withTimeout(3000) { db.databaseDao().isFavoriteAlbum(1).first() })
            db.databaseDao().unFavoriteAlbum(saved)
            assertFalse(withTimeout(3000) { db.databaseDao().isFavoriteAlbum(1).first() })
            assertTrue(withTimeout(3000) { db.databaseDao().fetchFavoriteAlbums().first() }.isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
