package com.laioffer.spotify.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laioffer.spotify.datamodel.Album
import com.laioffer.spotify.datamodel.Song
import com.laioffer.spotify.repository.FavoriteAlbumRepository
import com.laioffer.spotify.repository.PlaylistRepository
import com.laioffer.spotify.repository.readableDataError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val favoriteAlbumRepository: FavoriteAlbumRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaylistUiState(Album.empty()))
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()
    private var songsJob: Job? = null
    private var favoriteObserver: Job? = null
    private val favoriteMutations = mutableMapOf<Int, Job>()
    private var requestVersion = 0L

    fun fetchPlaylist(album: Album) {
        val version = ++requestVersion
        songsJob?.cancel()
        favoriteObserver?.cancel()
        _uiState.value = PlaylistUiState(album = album, isLoading = true,
            isSavingFavorite = favoriteMutations[album.id]?.isActive == true)
        songsJob = viewModelScope.launch {
            try {
                val playlist = playlistRepository.getPlaylist(album.id)
                if (version == requestVersion) _uiState.value = _uiState.value.copy(playlist = playlist.songs, isLoading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version == requestVersion) _uiState.value = _uiState.value.copy(isLoading = false,
                    error = readableDataError(error, "Could not load this album. Please retry."))
            }
        }
        favoriteObserver = viewModelScope.launch {
            try {
                favoriteAlbumRepository.isFavoriteAlbum(album.id).collect { favorite ->
                    if (version == requestVersion) _uiState.value = _uiState.value.copy(isFavorite = favorite)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version == requestVersion) _uiState.value = _uiState.value.copy(
                    favoriteError = "Could not read saved favorites. Please retry.")
            }
        }
    }

    fun retry() = fetchPlaylist(_uiState.value.album)

    fun toggleFavorite(isFavorite: Boolean) {
        if (_uiState.value.album.id < 1 || favoriteMutations[_uiState.value.album.id]?.isActive == true) return
        val album = _uiState.value.album
        _uiState.value = _uiState.value.copy(isSavingFavorite = true, favoriteError = null)
        favoriteMutations[album.id] = viewModelScope.launch {
            try {
                if (isFavorite) favoriteAlbumRepository.favoriteAlbum(album)
                else favoriteAlbumRepository.unFavoriteAlbum(album)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (_uiState.value.album.id == album.id) _uiState.value = _uiState.value.copy(
                    favoriteError = "Could not update favorites. Please retry.")
            } finally {
                if (_uiState.value.album.id == album.id) _uiState.value = _uiState.value.copy(isSavingFavorite = false)
            }
        }
    }
}

data class PlaylistUiState(
    val album: Album,
    val isFavorite: Boolean = false,
    val playlist: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSavingFavorite: Boolean = false,
    val favoriteError: String? = null,
)
