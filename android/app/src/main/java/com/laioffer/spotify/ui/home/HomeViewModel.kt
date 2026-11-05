package com.laioffer.spotify.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laioffer.spotify.datamodel.Section
import com.laioffer.spotify.repository.HomeRepository
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
class HomeViewModel @Inject constructor(private val repository: HomeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var loading: Job? = null
    private var requestVersion = 0L

    fun fetchHomeScreen() {
        val version = ++requestVersion
        loading?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loading = viewModelScope.launch {
            try {
                val feed = repository.getHomeSections()
                if (version == requestVersion) _uiState.value = HomeUiState(feed = feed, isLoading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version == requestVersion) _uiState.value = _uiState.value.copy(isLoading = false,
                    error = readableDataError(error, "Could not load music. Please retry."))
            }
        }
    }
}

data class HomeUiState(
    val feed: List<Section> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
