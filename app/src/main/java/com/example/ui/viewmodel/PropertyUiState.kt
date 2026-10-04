package com.example.ui.viewmodel

import com.example.data.model.Property

/**
 * UI State representation for the Property screen and components.
 */
sealed interface PropertyUiState {
    /**
     * Initial loading state while properties are being fetched or initialized.
     */
    data object Loading : PropertyUiState

    /**
     * Success state exposing cached property listings with filtering and selection.
     */
    data class Success(
        val properties: List<Property>,
        val verifiedOnly: Boolean = false,
        val searchQuery: String = "",
        val locationFilter: String = "",
        val selectedProperty: Property? = null,
        val totalCount: Int = properties.size
    ) : PropertyUiState

    /**
     * Error state conveying failure details.
     */
    data class Error(val message: String) : PropertyUiState
}
