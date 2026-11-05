package com.laioffer.spotify.data

import androidx.lifecycle.viewModelScope
import com.laioffer.spotify.database.DatabaseDao
import com.laioffer.spotify.datamodel.*
import com.laioffer.spotify.network.MediaUrlResolver
import com.laioffer.spotify.repository.*
import com.laioffer.spotify.ui.favorite.FavoriteViewModel
import com.laioffer.spotify.ui.home.HomeViewModel
import com.laioffer.spotify.ui.playlist.PlaylistViewModel
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }
    private fun album(id: Int) = Album(id, "Album $id", "2025", "", "Artist", "")
    private fun playlist(api: FakeApi, dao: FakeDao) = PlaylistViewModel(PlaylistRepository(api, MediaUrlResolver("http://localhost:8080/")), FavoriteAlbumRepository(dao))

    @Test fun homeShowsAnErrorThenCanRetrySuccessfully() = runTest(dispatcher) {
        val api = FakeApi()
        api.feed = { throw IOException("connection reset") }
        val vm = HomeViewModel(HomeRepository(api))
        try {
            vm.fetchHomeScreen(); advanceUntilIdle()
            assertFalse(vm.uiState.value.isLoading)
            assertTrue(vm.uiState.value.error!!.contains("connection"))
            api.feed = { Response.success(listOf(Section("Mix", listOf(album(1))))) }
            vm.fetchHomeScreen(); advanceUntilIdle()
            assertNull(vm.uiState.value.error)
            assertEquals("Mix", vm.uiState.value.feed.single().sectionTitle)
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun changingAlbumsCancelsOldRequestsAndOnlyOneFavoriteObserverRemains() = runTest(dispatcher) {
        val api = FakeApi(); val dao = FakeDao()
        var firstCancelled = false
        api.playlist = { id ->
            if (id == 1) try { awaitCancellation() } finally { firstCancelled = true }
            Response.success(Playlist(id, listOf(Song("Song $id", "", "/songs/$id.mp3", "0:10"))))
        }
        val vm = playlist(api, dao)
        try {
            vm.fetchPlaylist(album(1)); runCurrent()
            vm.fetchPlaylist(album(2)); advanceUntilIdle()
            assertTrue(firstCancelled)
            assertEquals(2, vm.uiState.value.album.id)
            assertEquals("Song 2", vm.uiState.value.playlist.single().name)
            assertEquals(1, dao.activeObservers)
            dao.favoriteAlbum(album(1)); runCurrent()
            assertFalse(vm.uiState.value.isFavorite)
            dao.favoriteAlbum(album(2)); runCurrent()
            assertTrue(vm.uiState.value.isFavorite)
        } finally { vm.viewModelScope.cancel(); runCurrent() }
        assertEquals(0, dao.activeObservers)
    }

    @Test fun savingFavoritesIsDeduplicatedAndOtherAlbumsRemainIndependent() = runTest(dispatcher) {
        val api = FakeApi(); val dao = FakeDao()
        val gate = CompletableDeferred<Unit>()
        dao.beforeWrite = { gate.await() }
        val vm = playlist(api, dao)
        try {
            vm.fetchPlaylist(album(1)); advanceUntilIdle()
            vm.toggleFavorite(true); vm.toggleFavorite(true); runCurrent()
            assertTrue(vm.uiState.value.isSavingFavorite)
            vm.fetchPlaylist(album(2)); runCurrent()
            assertFalse(vm.uiState.value.isSavingFavorite)
            gate.complete(Unit); advanceUntilIdle()
            assertEquals(1, dao.writes)
            assertFalse(vm.uiState.value.isFavorite)
            vm.toggleFavorite(true); advanceUntilIdle()
            assertTrue(vm.uiState.value.isFavorite)
            vm.toggleFavorite(false); advanceUntilIdle()
            assertFalse(vm.uiState.value.isFavorite)
        } finally { vm.viewModelScope.cancel(); runCurrent() }
    }

    @Test fun favoriteObservationFailureCanBeRetried() = runTest(dispatcher) {
        val dao = FakeDao(); dao.failListing = true
        val vm = FavoriteViewModel(FavoriteAlbumRepository(dao))
        try {
            advanceUntilIdle()
            assertNotNull(vm.uiState.value.error)
            dao.failListing = false
            dao.favoriteAlbum(album(1))
            vm.retry(); advanceUntilIdle()
            assertNull(vm.uiState.value.error)
            assertEquals(1, vm.uiState.value.albums.single().id)
        } finally { vm.viewModelScope.cancel(); runCurrent() }
    }
}

internal class FakeDao : DatabaseDao {
    private val albums = MutableStateFlow<List<Album>>(emptyList())
    var beforeWrite: suspend () -> Unit = {}
    var writes = 0
    var activeObservers = 0
    var failListing = false
    override suspend fun favoriteAlbum(album: Album) {
        beforeWrite(); writes++
        albums.value = albums.value.filterNot { it.id == album.id } + album
    }
    override suspend fun unFavoriteAlbum(album: Album) { albums.value = albums.value.filterNot { it.id == album.id } }
    override fun isFavoriteAlbum(id: Int): Flow<Boolean> = flow {
        activeObservers++
        try { emitAll(albums.map { current -> current.any { it.id == id } }) }
        finally { activeObservers-- }
    }
    override fun fetchFavoriteAlbums(): Flow<List<Album>> = flow {
        if (failListing) throw IOException("Database unavailable")
        emitAll(albums)
    }
}
