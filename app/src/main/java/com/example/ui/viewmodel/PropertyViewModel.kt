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

    private val _minPriceFilter = MutableStateFlow(0.0)
    val minPriceFilter: StateFlow<Double> = _minPriceFilter.asStateFlow()

    private val _maxPriceFilter = MutableStateFlow(Double.MAX_VALUE)
    val maxPriceFilter: StateFlow<Double> = _maxPriceFilter.asStateFlow()

    private val _roomsFilter = MutableStateFlow(0) // 0 = Any, 1, 2, 3, 4+
    val roomsFilter: StateFlow<Int> = _roomsFilter.asStateFlow()

    private val _propertyCategoryFilter = MutableStateFlow("All") // "All", "Flats", "Houses", "Self-Contain"
    val propertyCategoryFilter: StateFlow<String> = _propertyCategoryFilter.asStateFlow()

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

    private data class QuadFilter(
        val minPrice: Double,
        val maxPrice: Double,
        val rooms: Int,
        val category: String
    )

    private data class FilterCriteria(
        val query: String,
        val location: String,
        val verifiedOnly: Boolean,
        val minPrice: Double,
        val maxPrice: Double,
        val rooms: Int,
        val category: String
    )

    private val _filterCriteria = combine(
        combine(_searchQuery, _locationFilter, _verifiedOnlyFilter) { q, loc, ver ->
            Triple(q, loc, ver)
        },
        combine(_minPriceFilter, _maxPriceFilter, _roomsFilter, _propertyCategoryFilter) { minP, maxP, r, cat ->
            QuadFilter(minP, maxP, r, cat)
        }
    ) { textFilters, priceAndRooms ->
        FilterCriteria(
            query = textFilters.first,
            location = textFilters.second,
            verifiedOnly = textFilters.third,
            minPrice = priceAndRooms.minPrice,
            maxPrice = priceAndRooms.maxPrice,
            rooms = priceAndRooms.rooms,
            category = priceAndRooms.category
        )
    }

    /**
     * Comprehensive UI state that dynamically combines cached listings with active
     * search queries (location or keywords), price range, rooms, category, and verification filters.
     */
    val uiState: StateFlow<PropertyUiState> = combine(
        propertyRepository.allCachedProperties,
        _filterCriteria,
        _selectedProperty
    ) { properties, filters, selected ->
        val queryKeywords = filters.query.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }

        val filtered = properties.filter { property ->
            // Verification check
            val matchesVerification = if (filters.verifiedOnly) property.verified else true

            // Location filter check
            val matchesLocation = if (filters.location.isBlank() || filters.location.equals("All", ignoreCase = true)) {
                true
            } else {
                property.location.contains(filters.location, ignoreCase = true)
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

            // Price range check
            val matchesPrice = property.price in filters.minPrice..filters.maxPrice

            // Rooms / bedrooms check
            val matchesRooms = when (filters.rooms) {
                0 -> true
                4 -> property.bedrooms >= 4
                else -> property.bedrooms == filters.rooms
            }

            // Category check (houses vs flats)
            val matchesCategory = when (filters.category) {
                "All" -> true
                "Flats" -> property.propertyType.contains("Flat", ignoreCase = true) ||
                           property.propertyType.contains("Apartment", ignoreCase = true)
                "Houses" -> property.propertyType.contains("House", ignoreCase = true) ||
                            property.propertyType.contains("Duplex", ignoreCase = true) ||
                            property.propertyType.contains("Bungalow", ignoreCase = true)
                "Self-Contain" -> property.propertyType.contains("Self-Contain", ignoreCase = true) ||
                                  property.propertyType.contains("Studio", ignoreCase = true)
                else -> true
            }

            matchesVerification && matchesLocation && matchesKeywords && matchesPrice && matchesRooms && matchesCategory
        }

        PropertyUiState.Success(
            properties = filtered,
            verifiedOnly = filters.verifiedOnly,
            searchQuery = filters.query,
            locationFilter = filters.location,
            minPrice = filters.minPrice,
            maxPrice = filters.maxPrice,
            rooms = filters.rooms,
            propertyCategory = filters.category,
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
     * Updates the price range filter.
     */
    fun onPriceRangeChanged(minPrice: Double, maxPrice: Double) {
        _minPriceFilter.value = minPrice
        _maxPriceFilter.value = maxPrice
    }

    /**
     * Updates the number of rooms / bedrooms filter (0 = Any, 1..4+).
     */
    fun onRoomsFilterChanged(rooms: Int) {
        _roomsFilter.value = rooms
    }

    /**
     * Updates the property category filter ("All", "Flats", "Houses", "Self-Contain").
     */
    fun onPropertyCategoryChanged(category: String) {
        _propertyCategoryFilter.value = category
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
     * Clears all search criteria and filter states.
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _locationFilter.value = ""
        _minPriceFilter.value = 0.0
        _maxPriceFilter.value = Double.MAX_VALUE
        _roomsFilter.value = 0
        _propertyCategoryFilter.value = "All"
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
