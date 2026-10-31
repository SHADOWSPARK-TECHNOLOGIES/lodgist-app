package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.PublicListingDto
import com.example.data.repository.PublicListingsRepository
import com.example.data.repository.PublicListingsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the live listings screen is currently able to show.
 *
 * Each failure mode stays distinct. In particular [Empty] means the API answered
 * and published nothing, while [Offline] means we could not ask — the two are
 * never merged into one "no results" screen.
 */
sealed interface LiveListingsUiState {
    data object Loading : LiveListingsUiState
    data object Empty : LiveListingsUiState
    data class Content(val homes: List<PublicListingDto>) : LiveListingsUiState
    data object Offline : LiveListingsUiState
    data object Unauthorized : LiveListingsUiState
    data class Failed(val httpCode: Int) : LiveListingsUiState
}

/**
 * Drives the live listings screen from `GET /api/listings`.
 *
 * The state is derived only from what the server returned. Nothing here can
 * mark a home as verified: verification is read from the server's own labels.
 */
class LiveListingsViewModel(
    private val repository: PublicListingsRepository = PublicListingsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LiveListingsUiState>(LiveListingsUiState.Loading)
    val uiState: StateFlow<LiveListingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = LiveListingsUiState.Loading
            _uiState.value = repository.load().toUiState()
        }
    }
}

internal fun PublicListingsResult.toUiState(): LiveListingsUiState = when (this) {
    is PublicListingsResult.Loaded -> LiveListingsUiState.Content(homes)
    PublicListingsResult.Empty -> LiveListingsUiState.Empty
    PublicListingsResult.Offline -> LiveListingsUiState.Offline
    PublicListingsResult.Unauthorized -> LiveListingsUiState.Unauthorized
    is PublicListingsResult.Failed -> LiveListingsUiState.Failed(httpCode)
}
