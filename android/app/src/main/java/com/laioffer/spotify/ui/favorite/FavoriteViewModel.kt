package com.laioffer.spotify.ui.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laioffer.spotify.datamodel.Album
import com.laioffer.spotify.repository.FavoriteAlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(private val favoriteAlbumRepository: FavoriteAlbumRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FavoriteUiState())
    val uiState: StateFlow<FavoriteUiState> = _uiState.asStateFlow()
    private var observer: Job? = null

    init { retry() }

    fun retry() {
        observer?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        observer = viewModelScope.launch {
            try {
                favoriteAlbumRepository.fetchFavoriteAlbums().collect { albums ->
                    _uiState.value = FavoriteUiState(albums = albums, isLoading = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Could not load saved favorites. Please retry.")
            }
        }
    }
}

data class FavoriteUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
