package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Property
import com.example.data.model.PropertyEntity
import com.example.ui.components.PropertyCard
import com.example.ui.components.formatNaira
import com.example.ui.theme.LodgistOrange
import com.example.ui.theme.LodgistTeal
import com.example.ui.viewmodel.PropertyUiState
import com.example.ui.viewmodel.PropertyViewModel

/**
 * Stateful Search Property Screen connected directly to [PropertyViewModel].
 * Allows users to search and filter houses and flats by location, price range, and number of rooms.
 */
@Composable
fun SearchPropertyScreen(
    viewModel: PropertyViewModel,
    onPropertyClick: (Property) -> Unit = {},
    onBookInspection: (Property) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val locationFilter by viewModel.locationFilter.collectAsStateWithLifecycle()
    val minPrice by viewModel.minPriceFilter.collectAsStateWithLifecycle()
    val maxPrice by viewModel.maxPriceFilter.collectAsStateWithLifecycle()
    val rooms by viewModel.roomsFilter.collectAsStateWithLifecycle()
    val category by viewModel.propertyCategoryFilter.collectAsStateWithLifecycle()
    val verifiedOnly by viewModel.verifiedOnlyFilter.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is PropertyUiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("search_property_loading"),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LodgistOrange)
            }
        }
        is PropertyUiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .testTag("search_property_error"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Unable to load properties",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.refreshFromBackendApi() },
                        colors = ButtonDefaults.buttonColors(containerColor = LodgistOrange)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        is PropertyUiState.Success -> {
            val propertyEntities = state.properties.map { it.toPropertyEntity() }
            SearchPropertyContent(
                properties = propertyEntities,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                locationFilter = locationFilter,
                onLocationFilterChange = { viewModel.onLocationFilterChanged(it) },
                categoryFilter = category,
                onCategoryFilterChange = { viewModel.onPropertyCategoryChanged(it) },
                minPrice = minPrice.toLong(),
                maxPrice = if (maxPrice >= 10000000.0) 10000000L else maxPrice.toLong(),
                onPriceRangeChange = { min, max -> viewModel.onPriceRangeChanged(min.toDouble(), max.toDouble()) },
                roomsFilter = rooms,
                onRoomsFilterChange = { viewModel.onRoomsFilterChanged(it) },
                verifiedOnly = verifiedOnly,
                onToggleVerifiedOnly = { viewModel.toggleVerifiedOnly() },
                onResetFilters = { viewModel.clearSearch() },
                onPropertyClick = { entity ->
                    val original = state.properties.firstOrNull { it.id == entity.id } ?: entity.toProperty()
                    viewModel.selectProperty(original)
                    onPropertyClick(original)
                },
                onBookInspection = { entity ->
                    val original = state.properties.firstOrNull { it.id == entity.id } ?: entity.toProperty()
                    onBookInspection(original)
                },
                modifier = modifier
            )
        }
    }
}

/**
 * Reusable, stateless Search Property Screen content layout.
 * Supports filtering houses and flats by location, price range (Naira), and number of rooms.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchPropertyContent(
    properties: List<PropertyEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    locationFilter: String,
    onLocationFilterChange: (String) -> Unit,
    categoryFilter: String, // "All", "Houses", "Flats", "Self-Contain"
    onCategoryFilterChange: (String) -> Unit,
    minPrice: Long,
    maxPrice: Long,
    onPriceRangeChange: (Long, Long) -> Unit,
    roomsFilter: Int, // 0 = Any, 1, 2, 3, 4+
    onRoomsFilterChange: (Int) -> Unit,
    verifiedOnly: Boolean,
    onToggleVerifiedOnly: () -> Unit,
    onResetFilters: () -> Unit,
    onPropertyClick: (PropertyEntity) -> Unit,
    onBookInspection: (PropertyEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvancedFilters by remember { mutableStateOf(true) }

    val locations = listOf("All", "Owerri", "Nekede", "Ikenegbu", "Aladinma", "Lagos", "Port Harcourt", "Abuja")
    val categories = listOf("All", "Flats", "Houses", "Self-Contain")
    val roomOptions = listOf(
        0 to "Any Rooms",
        1 to "1 Room",
        2 to "2 Rooms",
        3 to "3 Rooms",
        4 to "4+ Rooms"
    )

    val hasActiveFilters = searchQuery.isNotBlank() ||
            (locationFilter.isNotBlank() && !locationFilter.equals("All", ignoreCase = true)) ||
            (categoryFilter.isNotBlank() && !categoryFilter.equals("All", ignoreCase = true)) ||
            minPrice > 0L ||
            maxPrice < 10000000L ||
            roomsFilter > 0 ||
            verifiedOnly

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_property_screen"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Top Search Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(LodgistOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = LodgistOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Search Properties",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Filter Houses & Flats across Nigeria",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Toggle expand filter panel
                        IconButton(
                            onClick = { showAdvancedFilters = !showAdvancedFilters },
                            modifier = Modifier.testTag("toggle_search_filters_button")
                        ) {
                            Icon(
                                imageVector = if (showAdvancedFilters) Icons.Default.Tune else Icons.Default.FilterAlt,
                                contentDescription = "Toggle Filters",
                                tint = if (showAdvancedFilters) LodgistOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Keyword / Location Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_property_input"),
                        placeholder = {
                            Text(
                                "Search by keyword, address, or amenity...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = LodgistOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.testTag("clear_search_query_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LodgistOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }

        // 2. Expandable Filter Controls (Houses/Flats, Location, Price Range, Rooms)
        item {
            AnimatedVisibility(
                visible = showAdvancedFilters,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {

                        // SECTION A: Property Category (Houses vs Flats)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Apartment,
                                contentDescription = null,
                                tint = LodgistOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Property Type (Houses & Flats)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = if (cat == "All") {
                                    categoryFilter.isBlank() || categoryFilter.equals("All", ignoreCase = true)
                                } else {
                                    categoryFilter.equals(cat, ignoreCase = true)
                                }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onCategoryFilterChange(if (cat == "All") "All" else cat) },
                                    label = {
                                        Text(
                                            text = when (cat) {
                                                "Houses" -> "🏡 Houses"
                                                "Flats" -> "🏢 Flats"
                                                "Self-Contain" -> "🛋️ Self-Contain"
                                                else -> "All Types"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LodgistOrange,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("filter_category_${cat.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SECTION B: Location Filter
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = LodgistOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Location & Pilot Hub",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (locationFilter.isNotBlank() && !locationFilter.equals("All", ignoreCase = true)) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${locationFilter})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LodgistOrange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            locations.forEach { loc ->
                                val isSelected = if (loc == "All") {
                                    locationFilter.isBlank() || locationFilter.equals("All", ignoreCase = true)
                                } else {
                                    locationFilter.equals(loc, ignoreCase = true)
                                }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onLocationFilterChange(if (loc == "All") "" else loc) },
                                    label = {
                                        Text(
                                            text = loc,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LodgistOrange,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("filter_location_${loc.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SECTION C: Number of Rooms / Bedrooms Filter
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bed,
                                contentDescription = null,
                                tint = LodgistOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Number of Rooms (Bedrooms)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            roomOptions.forEach { (roomVal, label) ->
                                val isSelected = roomsFilter == roomVal

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onRoomsFilterChange(roomVal) },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LodgistOrange,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("filter_rooms_$roomVal")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SECTION D: Price Range (Annual Rent in Naira)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = LodgistOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Annual Price Range",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${formatNaira(minPrice)} - ${if (maxPrice >= 10000000L) "₦10M+" else formatNaira(maxPrice)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LodgistOrange
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Price Preset Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val pricePresets = listOf(
                                Triple(0L, 10000000L, "Any Price"),
                                Triple(0L, 500000L, "< ₦500k"),
                                Triple(500000L, 1500000L, "₦500k - ₦1.5M"),
                                Triple(1500000L, 3000000L, "₦1.5M - ₦3M"),
                                Triple(3000000L, 10000000L, "₦3M+")
                            )

                            pricePresets.forEach { (pMin, pMax, label) ->
                                val isSelected = minPrice == pMin && maxPrice == pMax
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onPriceRangeChange(pMin, pMax) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LodgistOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // SECTION E: Verified Only Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = LodgistTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Verified Listings Only",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Show homes with physical inspection passed",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = verifiedOnly,
                                onCheckedChange = { onToggleVerifiedOnly() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LodgistTeal,
                                    checkedTrackColor = LodgistTeal.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.testTag("verified_only_filter_switch")
                            )
                        }

                        // Reset All Filters Button
                        if (hasActiveFilters) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onResetFilters,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_all_search_filters_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset All Filters", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Results Header & Active Filter Pills
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${properties.size} ${if (properties.size == 1) "Property" else "Properties"} Found",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (hasActiveFilters) {
                        Text(
                            text = "Filtered",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LodgistOrange
                        )
                    }
                }

                // Active filter tags row
                if (hasActiveFilters) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (categoryFilter.isNotBlank() && !categoryFilter.equals("All", ignoreCase = true)) {
                            ActiveFilterBadge(
                                text = "Category: $categoryFilter",
                                onClear = { onCategoryFilterChange("All") }
                            )
                        }

                        if (locationFilter.isNotBlank() && !locationFilter.equals("All", ignoreCase = true)) {
                            ActiveFilterBadge(
                                text = "Location: $locationFilter",
                                onClear = { onLocationFilterChange("") }
                            )
                        }

                        if (roomsFilter > 0) {
                            ActiveFilterBadge(
                                text = if (roomsFilter == 4) "4+ Rooms" else "$roomsFilter Rooms",
                                onClear = { onRoomsFilterChange(0) }
                            )
                        }

                        if (minPrice > 0L || maxPrice < 10000000L) {
                            ActiveFilterBadge(
                                text = "Price: ${formatNaira(minPrice)} - ${if (maxPrice >= 10000000L) "Max" else formatNaira(maxPrice)}",
                                onClear = { onPriceRangeChange(0L, 10000000L) }
                            )
                        }

                        if (verifiedOnly) {
                            ActiveFilterBadge(
                                text = "Verified Only",
                                onClear = onToggleVerifiedOnly
                            )
                        }
                    }
                }
            }
        }

        // 4. Results List or Empty State
        if (properties.isEmpty()) {
            item {
                SearchEmptyState(
                    onResetFilters = onResetFilters,
                    hasActiveFilters = hasActiveFilters
                )
            }
        } else {
            items(
                items = properties,
                key = { it.id }
            ) { property ->
                PropertyCard(
                    property = property,
                    onClick = { onPropertyClick(property) },
                    onToggleFavorite = { /* Handled in detail/favorites */ },
                    onBookInspection = { onBookInspection(property) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterBadge(
    text: String,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LodgistOrange.copy(alpha = 0.12f),
        modifier = Modifier.clickable(onClick = onClear)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = LodgistOrange
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove filter",
                tint = LodgistOrange,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun SearchEmptyState(
    onResetFilters: () -> Unit,
    hasActiveFilters: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("search_empty_state_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(LodgistOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = LodgistOrange,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Properties Found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (hasActiveFilters) {
                    "No houses or flats match your current location, room count, or price filter. Try broadening your criteria or resetting filters."
                } else {
                    "No listings available at this time. Check back soon for new homes."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (hasActiveFilters) {
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onResetFilters,
                    colors = ButtonDefaults.buttonColors(containerColor = LodgistOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_state_reset_filters_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset All Filters", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
