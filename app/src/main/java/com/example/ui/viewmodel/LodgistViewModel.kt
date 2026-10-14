package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LodgistDatabase
import com.example.data.model.EscrowPaymentEntity
import com.example.data.model.InspectionBookingEntity
import com.example.data.model.LandlordListingEntity
import com.example.data.model.PropertyEntity
import com.example.data.repository.LodgistRepository
import com.example.di.DatabaseModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenDestination {
    EXPLORE,
    SEARCH_PROPERTIES,
    INSPECTIONS,
    SAVED,
    ESCROW_VAULT,
    TRUST_SAFETY,
    LIST_PROPERTY,
    PROPERTY_DETAIL
}

data class FilterState(
    val searchQuery: String = "",
    val selectedCity: String = "All", // "All", "Owerri", "Nekede", "Lagos", "Port Harcourt", "Abuja"
    val selectedType: String = "All", // "All", "Self-Contain", "1 Bedroom Flat", "2 Bedroom Flat", "3 Bedroom Duplex", "Studio Apartment"
    val minRentNaira: Long = 0L,
    val maxRentNaira: Long = 10000000L,
    val selectedRooms: Int = 0, // 0 = Any, 1, 2, 3, 4+
    val selectedCategory: String = "All", // "All", "Houses", "Flats", "Self-Contain"
    val onlyHighTruthScore: Boolean = false // TruthScore >= 90
)

class LodgistViewModel(
    application: android.app.Application,
    customRepository: com.example.data.repository.LodgistRepository?
) : androidx.lifecycle.AndroidViewModel(application) {

    constructor(application: android.app.Application) : this(application, null)

    private val database = com.example.di.DatabaseModule.provideAppDatabase(application)
    private val repository = customRepository ?: com.example.di.DatabaseModule.provideLodgistRepository(database)

    private val _currentScreen = MutableStateFlow(ScreenDestination.EXPLORE)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _selectedPropertyId = MutableStateFlow<String?>(null)
    val selectedPropertyId: StateFlow<String?> = _selectedPropertyId.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val allProperties: StateFlow<List<PropertyEntity>> = repository.allProperties
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteProperties: StateFlow<List<PropertyEntity>> = repository.favoriteProperties
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inspectionBookings: StateFlow<List<InspectionBookingEntity>> = repository.inspectionBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val landlordSubmissions: StateFlow<List<LandlordListingEntity>> = repository.landlordSubmissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val escrowPayments: StateFlow<List<EscrowPaymentEntity>> = repository.escrowPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalEscrowVolume: StateFlow<Long> = repository.totalEscrowVolume
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalPlatformRevenue: StateFlow<Long> = repository.totalPlatformRevenue
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val filteredProperties: StateFlow<List<PropertyEntity>> = combine(
        allProperties,
        _filterState
    ) { properties, filter ->
        properties.filter { prop ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                prop.title.contains(filter.searchQuery, ignoreCase = true) ||
                prop.neighborhood.contains(filter.searchQuery, ignoreCase = true) ||
                prop.city.contains(filter.searchQuery, ignoreCase = true) ||
                prop.propertyType.contains(filter.searchQuery, ignoreCase = true) ||
                prop.amenities.contains(filter.searchQuery, ignoreCase = true)

            val matchesCity = when (filter.selectedCity) {
                "All" -> true
                "Nekede" -> prop.neighborhood.contains("Nekede", ignoreCase = true) || prop.isStudentFriendly
                else -> prop.city.equals(filter.selectedCity, ignoreCase = true)
            }

            val matchesType = when (filter.selectedType) {
                "All" -> true
                else -> prop.propertyType.equals(filter.selectedType, ignoreCase = true)
            }

            val matchesRooms = when (filter.selectedRooms) {
                0 -> true
                4 -> prop.bedrooms >= 4
                else -> prop.bedrooms == filter.selectedRooms
            }

            val matchesCategory = when (filter.selectedCategory) {
                "All" -> true
                "Flats" -> prop.propertyType.contains("Flat", ignoreCase = true) ||
                           prop.propertyType.contains("Apartment", ignoreCase = true)
                "Houses" -> prop.propertyType.contains("House", ignoreCase = true) ||
                            prop.propertyType.contains("Duplex", ignoreCase = true) ||
                            prop.propertyType.contains("Bungalow", ignoreCase = true)
                "Self-Contain" -> prop.propertyType.contains("Self-Contain", ignoreCase = true) ||
                                  prop.propertyType.contains("Studio", ignoreCase = true)
                else -> true
            }

            val matchesRent = prop.annualRent in filter.minRentNaira..filter.maxRentNaira
            val matchesTruthScore = !filter.onlyHighTruthScore || prop.truthScore >= 90

            matchesQuery && matchesCity && matchesType && matchesRooms && matchesCategory && matchesRent && matchesTruthScore
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedProperty: StateFlow<PropertyEntity?> = combine(
        allProperties,
        _selectedPropertyId
    ) { properties, id ->
        if (id == null) null else properties.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    private val backStack = mutableListOf<ScreenDestination>()

    fun navigateTo(destination: ScreenDestination) {
        if (_currentScreen.value != destination) {
            backStack.add(_currentScreen.value)
            _currentScreen.value = destination
        }
    }

    fun selectProperty(propertyId: String) {
        backStack.add(_currentScreen.value)
        _selectedPropertyId.value = propertyId
        _currentScreen.value = ScreenDestination.PROPERTY_DETAIL
    }

    fun clearSelectedProperty() {
        _selectedPropertyId.value = null
        if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeAt(backStack.size - 1)
        } else {
            _currentScreen.value = ScreenDestination.EXPLORE
        }
    }

    fun popBack(): Boolean {
        if (_currentScreen.value == ScreenDestination.PROPERTY_DETAIL) {
            clearSelectedProperty()
            return true
        }
        if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeAt(backStack.size - 1)
            return true
        }
        if (_currentScreen.value != ScreenDestination.EXPLORE) {
            _currentScreen.value = ScreenDestination.EXPLORE
            return true
        }
        return false
    }

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateCityFilter(city: String) {
        _filterState.value = _filterState.value.copy(selectedCity = city)
    }

    fun updateTypeFilter(type: String) {
        _filterState.value = _filterState.value.copy(selectedType = type)
    }

    fun updateMaxRentFilter(maxRent: Long) {
        _filterState.value = _filterState.value.copy(maxRentNaira = maxRent)
    }

    fun updatePriceRange(minRent: Long, maxRent: Long) {
        _filterState.value = _filterState.value.copy(minRentNaira = minRent, maxRentNaira = maxRent)
    }

    fun updateRoomsFilter(rooms: Int) {
        _filterState.value = _filterState.value.copy(selectedRooms = rooms)
    }

    fun updateCategoryFilter(category: String) {
        _filterState.value = _filterState.value.copy(selectedCategory = category)
    }

    fun toggleHighTruthScoreFilter() {
        val current = _filterState.value.onlyHighTruthScore
        _filterState.value = _filterState.value.copy(onlyHighTruthScore = !current)
    }

    fun resetFilters() {
        _filterState.value = FilterState()
    }

    fun toggleFavorite(property: PropertyEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(property.id, property.isFavorite)
        }
    }

    fun bookInspection(
        property: PropertyEntity,
        clientName: String,
        clientPhone: String,
        preferredDate: String,
        preferredTimeSlot: String,
        notes: String
    ) {
        viewModelScope.launch {
            val booking = InspectionBookingEntity(
                propertyId = property.id,
                propertyTitle = property.title,
                propertyLocation = "${property.neighborhood}, ${property.city}",
                clientName = clientName,
                clientPhone = clientPhone,
                preferredDate = preferredDate,
                preferredTimeSlot = preferredTimeSlot,
                notes = notes,
                assignedAgent = "Lodgist Field Inspector (Chidi O.)",
                status = "CONFIRMED"
            )
            val bookingResult = repository.bookInspection(booking)
            if (bookingResult.isFailure) {
                _userMessage.value = bookingResult.exceptionOrNull()?.message ?: "Viewing request rejected: property is unverified."
                return@launch
            }
            val bookingId = bookingResult.getOrNull() ?: 1L

            // Automate platform revenue: Record the ₦3,500 inspection escort escrow token
            val payment = EscrowPaymentEntity(
                propertyId = property.id,
                propertyTitle = property.title,
                payerName = clientName,
                payerPhone = clientPhone,
                amount = 3500L,
                purpose = "INSPECTION_TOKEN",
                paymentMethod = "DEBIT_CARD",
                transactionRef = "LDG-INS-${bookingId.toString().padStart(4, '0')}",
                status = "HELD_IN_ESCROW"
            )
            repository.processEscrowPayment(payment)

            _userMessage.value = "Inspection booked! ₦3,500 token secured in Escrow. Officer assigned for $preferredDate."
            _currentScreen.value = ScreenDestination.INSPECTIONS
        }
    }

    fun cancelInspection(bookingId: Long) {
        viewModelScope.launch {
            repository.cancelInspection(bookingId)
            _userMessage.value = "Inspection visit cancelled & escrow token refunded."
        }
    }

    fun submitLandlordProperty(
        title: String,
        propertyType: String,
        state: String,
        city: String,
        neighborhood: String,
        annualRent: Long,
        bedrooms: Int,
        bathrooms: Int,
        ownerFullName: String,
        ownerPhone: String,
        hasTitleDeed: Boolean,
        hasNINVerified: Boolean,
        hasBoreholeWater: Boolean,
        hasPrepaidMeter: Boolean
    ) {
        viewModelScope.launch {
            val submission = LandlordListingEntity(
                title = title,
                propertyType = propertyType,
                state = state,
                city = city,
                neighborhood = neighborhood,
                annualRent = annualRent,
                bedrooms = bedrooms,
                bathrooms = bathrooms,
                ownerFullName = ownerFullName,
                ownerPhone = ownerPhone,
                hasTitleDeed = hasTitleDeed,
                hasNINVerified = hasNINVerified,
                hasBoreholeWater = hasBoreholeWater,
                hasPrepaidMeter = hasPrepaidMeter,
                status = "ADVERTISER_LEAD_PENDING_REVIEW"
            )
            val submissionId = repository.submitLandlordProperty(submission)

            // An advertise submission is a lead, not a published home. Payment never buys verification.
            val auditPayment = EscrowPaymentEntity(
                propertyId = "landlord_lead_$submissionId",
                propertyTitle = title,
                payerName = ownerFullName,
                payerPhone = ownerPhone,
                amount = 15000L,
                purpose = "ADVERTISER_LEAD_AUDIT",
                paymentMethod = "DIRECT_BANK_TRANSFER",
                transactionRef = "LDG-LEAD-${submissionId.toString().padStart(4, '0')}",
                status = "PENDING_DISPLAY_REVIEW"
            )
            repository.processEscrowPayment(auditPayment)

            _userMessage.value = "Advertiser lead submitted! Our Owerri field team will verify details before display approval."
            _currentScreen.value = ScreenDestination.ESCROW_VAULT
        }
    }

    fun payRentIntoEscrow(
        property: PropertyEntity,
        tenantName: String,
        tenantPhone: String,
        method: String,
        ref: String
    ) {
        viewModelScope.launch {
            val payment = EscrowPaymentEntity(
                propertyId = property.id,
                propertyTitle = property.title,
                payerName = tenantName,
                payerPhone = tenantPhone,
                amount = property.totalMoveInCost,
                purpose = "ANNUAL_RENT_ESCROW",
                paymentMethod = method,
                transactionRef = ref,
                status = "HELD_IN_ESCROW"
            )
            repository.processEscrowPayment(payment)
            _userMessage.value = "${formatNaira(property.totalMoveInCost)} rent secured in Lodgist Escrow! Safe until move-in."
            _currentScreen.value = ScreenDestination.ESCROW_VAULT
        }
    }

    fun releaseEscrowToLandlord(paymentId: Long) {
        viewModelScope.launch {
            repository.releaseEscrow(paymentId)
            _userMessage.value = "Escrow funds released to landlord. Move-in confirmed!"
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private fun formatNaira(amount: Long): String {
        return com.example.ui.components.formatNaira(amount)
    }
}
