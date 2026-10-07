package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PropertyEntity
import com.example.ui.components.InspectionBookingSheet
import com.example.ui.components.LodgistBottomBar
import com.example.ui.components.LodgistTopBar
import com.example.ui.components.PaymentCheckoutSheet
import com.example.ui.screens.EscrowRevenueScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.InspectionsScreen
import com.example.ui.screens.ListPropertyScreen
import com.example.ui.screens.PropertyDetailScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.screens.TrustSafetyScreen
import com.example.ui.theme.LodgistTheme
import com.example.ui.viewmodel.LodgistViewModel
import com.example.ui.viewmodel.ScreenDestination

class MainActivity : ComponentActivity() {
    private val viewModel: LodgistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LodgistTheme {
                LodgistApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LodgistApp(viewModel: LodgistViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val properties by viewModel.filteredProperties.collectAsStateWithLifecycle()
    val savedProperties by viewModel.favoriteProperties.collectAsStateWithLifecycle()
    val inspectionBookings by viewModel.inspectionBookings.collectAsStateWithLifecycle()
    val landlordSubmissions by viewModel.landlordSubmissions.collectAsStateWithLifecycle()
    val escrowPayments by viewModel.escrowPayments.collectAsStateWithLifecycle()
    val totalEscrow by viewModel.totalEscrowVolume.collectAsStateWithLifecycle()
    val totalRevenue by viewModel.totalPlatformRevenue.collectAsStateWithLifecycle()
    val selectedProperty by viewModel.selectedProperty.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var propertyForBooking by remember { mutableStateOf<PropertyEntity?>(null) }
    var propertyForEscrowPayment by remember { mutableStateOf<PropertyEntity?>(null) }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentScreen != ScreenDestination.PROPERTY_DETAIL) {
                LodgistTopBar(
                    title = when (currentScreen) {
                        ScreenDestination.EXPLORE -> "Lodgist"
                        ScreenDestination.INSPECTIONS -> "My Inspections"
                        ScreenDestination.SAVED -> "Saved Properties"
                        ScreenDestination.ESCROW_VAULT -> "Escrow Vault"
                        ScreenDestination.TRUST_SAFETY -> "Trust & Safety"
                        ScreenDestination.LIST_PROPERTY -> "List Property"
                        ScreenDestination.PROPERTY_DETAIL -> "Property Details"
                    },
                    subtitle = when (currentScreen) {
                        ScreenDestination.EXPLORE -> "Published homes"
                        ScreenDestination.INSPECTIONS -> "Saved on this phone"
                        ScreenDestination.SAVED -> "Saved homes"
                        ScreenDestination.ESCROW_VAULT -> "Payments unavailable"
                        ScreenDestination.TRUST_SAFETY -> "What Lodgist checks"
                        ScreenDestination.LIST_PROPERTY -> "Landlord & Owner Portal"
                        ScreenDestination.PROPERTY_DETAIL -> ""
                    },
                    canNavigateBack = false,
                    savedCount = savedProperties.size,
                    onSavedClick = { viewModel.navigateTo(ScreenDestination.SAVED) }
                )
            }
        },
        bottomBar = {
            if (currentScreen != ScreenDestination.PROPERTY_DETAIL) {
                LodgistBottomBar(
                    currentDestination = currentScreen,
                    onNavigate = { destination ->
                        viewModel.navigateTo(destination)
                    },
                    activeInspectionCount = inspectionBookings.size
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (currentScreen) {
            ScreenDestination.EXPLORE -> {
                ExploreScreen(
                    properties = properties,
                    filterState = filterState,
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    onCityChange = { viewModel.updateCityFilter(it) },
                    onTypeChange = { viewModel.updateTypeFilter(it) },
                    onMaxRentChange = { viewModel.updateMaxRentFilter(it) },
                    onToggleHighTruthScore = { viewModel.toggleHighTruthScoreFilter() },
                    onResetFilters = { viewModel.resetFilters() },
                    onPropertyClick = { viewModel.selectProperty(it.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onBookInspection = { propertyForBooking = it },
                    onReload = { viewModel.refreshPublishedHomes() },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenDestination.PROPERTY_DETAIL -> {
                selectedProperty?.let { prop ->
                    PropertyDetailScreen(
                        property = prop,
                        onBack = { viewModel.clearSelectedProperty() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onBookInspection = { propertyForBooking = it },
                        onPayEscrow = { viewModel.payRentIntoEscrow(it, "", "", "", "") }
                    )
                } ?: run {
                    viewModel.clearSelectedProperty()
                }
            }

            ScreenDestination.INSPECTIONS -> {
                InspectionsScreen(
                    bookings = inspectionBookings,
                    onCancelBooking = { viewModel.cancelInspection(it) },
                    onExploreClick = { viewModel.navigateTo(ScreenDestination.EXPLORE) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenDestination.SAVED -> {
                SavedScreen(
                    savedProperties = savedProperties,
                    onPropertyClick = { viewModel.selectProperty(it.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onBookInspection = { propertyForBooking = it },
                    onExploreClick = { viewModel.navigateTo(ScreenDestination.EXPLORE) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenDestination.ESCROW_VAULT -> {
                EscrowRevenueScreen(
                    payments = escrowPayments,
                    totalEscrow = totalEscrow,
                    totalRevenue = totalRevenue,
                    onReleaseEscrow = { viewModel.releaseEscrowToLandlord(it) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenDestination.TRUST_SAFETY -> {
                TrustSafetyScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }

            ScreenDestination.LIST_PROPERTY -> {
                ListPropertyScreen(
                    submissions = landlordSubmissions,
                    onSubmitProperty = { title, type, state, city, neighborhood, rent, beds, baths, ownerName, ownerPhone, hasTitle, hasNIN, hasWater, hasMeter ->
                        viewModel.submitLandlordProperty(
                            title = title,
                            propertyType = type,
                            state = state,
                            city = city,
                            neighborhood = neighborhood,
                            annualRent = rent,
                            bedrooms = beds,
                            bathrooms = baths,
                            ownerFullName = ownerName,
                            ownerPhone = ownerPhone,
                            hasTitleDeed = hasTitle,
                            hasNINVerified = hasNIN,
                            hasBoreholeWater = hasWater,
                            hasPrepaidMeter = hasMeter
                        )
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // Inspection Booking Bottom Sheet
        propertyForBooking?.let { prop ->
            InspectionBookingSheet(
                property = prop,
                onDismiss = { propertyForBooking = null },
                onConfirmBooking = { name, phone, date, slot, notes ->
                    viewModel.bookInspection(
                        property = prop,
                        clientName = name,
                        clientPhone = phone,
                        preferredDate = date,
                        preferredTimeSlot = slot,
                        notes = notes
                    )
                    propertyForBooking = null
                }
            )
        }

        // Annual Rent Escrow Payment Bottom Sheet
        propertyForEscrowPayment?.let { prop ->
            PaymentCheckoutSheet(
                title = "Annual Rent Escrow Protection",
                subtitle = prop.title,
                amountNaira = prop.totalMoveInCost,
                purpose = "ANNUAL_RENT_ESCROW",
                propertyId = prop.id,
                payerName = "Verified Tenant",
                payerPhone = "08012345678",
                onDismiss = { propertyForEscrowPayment = null },
                onPaymentSuccess = { method, ref ->
                    viewModel.payRentIntoEscrow(
                        property = prop,
                        tenantName = "Verified Tenant",
                        tenantPhone = "08012345678",
                        method = method,
                        ref = ref
                    )
                    propertyForEscrowPayment = null
                }
            )
        }
    }
}
