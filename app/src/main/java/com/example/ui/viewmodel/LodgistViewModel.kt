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
    val maxRentNaira: Long = 10000000L,
    val onlyHighTruthScore: Boolean = false // TruthScore >= 90
)

class LodgistViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DatabaseModule.provideAppDatabase(application)
    private val repository = DatabaseModule.provideLodgistRepository(database)

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

            val matchesRent = prop.annualRent <= filter.maxRentNaira
            val matchesTruthScore = !filter.onlyHighTruthScore || prop.truthScore >= 90

            matchesQuery && matchesCity && matchesType && matchesRent && matchesTruthScore
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

    fun navigateTo(destination: ScreenDestination) {
        _currentScreen.value = destination
    }

    fun selectProperty(propertyId: String) {
        _selectedPropertyId.value = propertyId
        _currentScreen.value = ScreenDestination.PROPERTY_DETAIL
    }

    fun clearSelectedProperty() {
        _selectedPropertyId.value = null
        _currentScreen.value = ScreenDestination.EXPLORE
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
            val bookingId = repository.bookInspection(booking)

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
                status = "UNDER_VERIFICATION"
            )
            val submissionId = repository.submitLandlordProperty(submission)

            // Automate landlord express audit revenue stream: Record ₦15,000 audit fee
            val auditPayment = EscrowPaymentEntity(
                propertyId = "landlord_listing_$submissionId",
                propertyTitle = title,
                payerName = ownerFullName,
                payerPhone = ownerPhone,
                amount = 15000L,
                purpose = "EXPRESS_VERIFICATION",
                paymentMethod = "DIRECT_BANK_TRANSFER",
                transactionRef = "LDG-AUD-${submissionId.toString().padStart(4, '0')}",
                status = "HELD_IN_ESCROW"
            )
            repository.processEscrowPayment(auditPayment)

            _userMessage.value = "Property submitted! 24h TruthScore field audit initiated."
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
