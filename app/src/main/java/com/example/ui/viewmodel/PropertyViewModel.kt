package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Property
import com.example.data.repository.PropertyRepository
import com.example.di.DatabaseModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing and exposing property data to the UI.
 *
 * Supports keyword and location-based filtering, verified listings toggling,
 * and seamless offline-first caching via [PropertyRepository].
 *
 * @param propertyRepository Repository handling Room DAO caching operations and backend API calls.
 */
class PropertyViewModel(
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    /**
     * Secondary constructor for standard Android ViewModel instantiation via [DatabaseModule].
     */
    constructor(application: Application) : this(
        DatabaseModule.providePropertyRepository(
            DatabaseModule.provideAppDatabase(application)
        )
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _locationFilter = MutableStateFlow("")
    val locationFilter: StateFlow<String> = _locationFilter.asStateFlow()

    private val _verifiedOnlyFilter = MutableStateFlow(false)
    val verifiedOnlyFilter: StateFlow<Boolean> = _verifiedOnlyFilter.asStateFlow()

    private val _selectedProperty = MutableStateFlow<Property?>(null)
    val selectedProperty: StateFlow<Property?> = _selectedProperty.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Complete list of all cached properties exposed as a reactive [StateFlow].
     */
    val allProperties: StateFlow<List<Property>> = propertyRepository.allCachedProperties
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * List of verified properties exposed as a reactive [StateFlow].
     */
    val verifiedProperties: StateFlow<List<Property>> = propertyRepository.verifiedProperties
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Comprehensive UI state that dynamically combines cached listings with active
     * search queries (location or keywords) and verification filters.
     */
    val uiState: StateFlow<PropertyUiState> = combine(
        propertyRepository.allCachedProperties,
        _searchQuery,
        _locationFilter,
        _verifiedOnlyFilter,
        _selectedProperty
    ) { properties, query, location, verifiedOnly, selected ->
        val queryKeywords = query.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }

        val filtered = properties.filter { property ->
            // Verification check
            val matchesVerification = if (verifiedOnly) property.verified else true

            // Location filter check
            val matchesLocation = if (location.isBlank() || location.equals("All", ignoreCase = true)) {
                true
            } else {
                property.location.contains(location, ignoreCase = true)
            }

            // Keyword filter check (matches across title, location, description, or propertyType)
            val matchesKeywords = if (queryKeywords.isEmpty()) {
                true
            } else {
                val searchableText = listOf(
                    property.title,
                    property.location,
                    property.description,
                    property.propertyType
                ).joinToString(" ").lowercase()

                queryKeywords.all { keyword -> searchableText.contains(keyword) }
            }

            matchesVerification && matchesLocation && matchesKeywords
        }

        PropertyUiState.Success(
            properties = filtered,
            verifiedOnly = verifiedOnly,
            searchQuery = query,
            locationFilter = location,
            selectedProperty = selected,
            totalCount = filtered.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PropertyUiState.Loading
    )

    /**
     * Updates the user's active search query (location or keywords).
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Updates the selected location filter (e.g. "Owerri", "Lagos", "Abuja", "Nekede", or "All").
     */
    fun onLocationFilterChanged(location: String) {
        _locationFilter.value = if (location.equals("All", ignoreCase = true)) "" else location
    }

    /**
     * Convenience method to filter listings by keywords.
     */
    fun filterByKeywords(keywords: String) {
        onSearchQueryChanged(keywords)
    }

    /**
     * Convenience method to filter listings by location.
     */
    fun filterByLocation(location: String) {
        onLocationFilterChanged(location)
    }

    /**
     * Clears both the search query and location filter.
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _locationFilter.value = ""
    }

    /**
     * Toggles or sets the verified-only listing filter.
     */
    fun toggleVerifiedOnly(enabled: Boolean? = null) {
        _verifiedOnlyFilter.value = enabled ?: !_verifiedOnlyFilter.value
    }

    /**
     * Sets the currently selected property for detail inspection.
     */
    fun selectProperty(property: Property?) {
        _selectedProperty.value = property
    }

    /**
     * Refreshes listings by querying the Lodgist website backend API and updating the Room cache.
     */
    fun refreshFromBackendApi(city: String? = null, verified: Boolean? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = propertyRepository.fetchAndCachePropertiesFromApi(city, verified)
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Fetches and selects a property by its ID from the repository cache.
     */
    fun loadPropertyById(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _selectedProperty.value = propertyRepository.getPropertyById(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load property $id"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Inserts a new property listing into the local cache.
     */
    fun insertProperty(property: Property) {
        viewModelScope.launch {
            try {
                propertyRepository.insertProperty(property)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to save property"
            }
        }
    }

    /**
     * Updates an existing cached property.
     */
    fun updateProperty(property: Property) {
        viewModelScope.launch {
            try {
                propertyRepository.updateProperty(property)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update property"
            }
        }
    }

    /**
     * Deletes a property from the local cache.
     */
    fun deleteProperty(property: Property) {
        viewModelScope.launch {
            try {
                propertyRepository.deleteProperty(property)
                if (_selectedProperty.value?.id == property.id) {
                    _selectedProperty.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete property"
            }
        }
    }

    /**
     * Deletes a property by its unique ID.
     */
    fun deletePropertyById(id: String) {
        viewModelScope.launch {
            try {
                propertyRepository.deletePropertyById(id)
                if (_selectedProperty.value?.id == id) {
                    _selectedProperty.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete property"
            }
        }
    }

    /**
     * Dismisses the current error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = DatabaseModule.provideAppDatabase(application)
                    val repo = DatabaseModule.providePropertyRepository(db)
                    return PropertyViewModel(repo) as T
                }
            }
    }
}
