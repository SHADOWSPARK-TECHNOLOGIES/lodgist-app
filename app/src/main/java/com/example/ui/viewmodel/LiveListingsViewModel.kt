package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.PublicListingDto
import com.example.data.repository.ListingDetailResult
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
 * What the detail screen is currently able to show. [NotFound] is a distinct,
 * expected answer — the API returns it for homes that are not publicly eligible.
 */
sealed interface ListingDetailUiState {
    data object Loading : ListingDetailUiState
    data class Content(val home: PublicListingDto) : ListingDetailUiState
    data object NotFound : ListingDetailUiState
    data object Offline : ListingDetailUiState
    data object Unauthorized : ListingDetailUiState
    data class Failed(val httpCode: Int) : ListingDetailUiState
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

    /** Null means no detail is open; the list is showing. */
    private val _detailState = MutableStateFlow<ListingDetailUiState?>(null)
    val detailState: StateFlow<ListingDetailUiState?> = _detailState.asStateFlow()

    private var openDetailId: Long? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = LiveListingsUiState.Loading
            _uiState.value = repository.load().toUiState()
        }
    }

    fun openDetail(id: Long) {
        openDetailId = id
        loadDetail()
    }

    fun retryDetail() {
        loadDetail()
    }

    fun closeDetail() {
        openDetailId = null
        _detailState.value = null
    }

    private fun loadDetail() {
        val id = openDetailId ?: return
        viewModelScope.launch {
            _detailState.value = ListingDetailUiState.Loading
            _detailState.value = repository.loadDetail(id).toUiState()
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

internal fun ListingDetailResult.toUiState(): ListingDetailUiState = when (this) {
    is ListingDetailResult.Loaded -> ListingDetailUiState.Content(home)
    ListingDetailResult.NotFound -> ListingDetailUiState.NotFound
    ListingDetailResult.Offline -> ListingDetailUiState.Offline
    ListingDetailResult.Unauthorized -> ListingDetailUiState.Unauthorized
    is ListingDetailResult.Failed -> ListingDetailUiState.Failed(httpCode)
}
