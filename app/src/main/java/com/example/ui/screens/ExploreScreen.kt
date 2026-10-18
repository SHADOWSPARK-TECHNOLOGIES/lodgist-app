package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PropertyEntity
import com.example.ui.components.PropertyCard
import com.example.ui.components.PropertySearchBar
import com.example.ui.components.formatNaira
import com.example.ui.theme.LodgistOrange
import com.example.ui.theme.LodgistTeal
import com.example.ui.viewmodel.FilterState

@Composable
fun ExploreScreen(
    properties: List<PropertyEntity>,
    filterState: FilterState,
    onSearchChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onMaxRentChange: (Long) -> Unit,
    onToggleHighTruthScore: () -> Unit,
    onResetFilters: () -> Unit,
    onPropertyClick: (PropertyEntity) -> Unit,
    onToggleFavorite: (PropertyEntity) -> Unit,
    onBookInspection: (PropertyEntity) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showFilterPanel by remember { mutableStateOf(false) }

    val cities = listOf("All", "Owerri", "Nekede", "Lagos", "Port Harcourt", "Abuja")
    val propertyTypes = listOf("All", "Self-Contain", "1 Bedroom Flat", "2 Bedroom Flat", "3 Bedroom Flat", "Studio Apartment")

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Search bar at the top of the UI to filter listings by location or keywords
        PropertySearchBar(
            searchQuery = filterState.searchQuery,
            onSearchQueryChange = onSearchChange,
            selectedLocation = filterState.selectedCity,
            onLocationSelected = onCityChange,
            verifiedOnly = filterState.onlyHighTruthScore,
            onToggleVerified = onToggleHighTruthScore
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .testTag("explore_screen_list"),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
        // Hero Brand Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_lodgist_hero),
                    contentDescription = "Lodgist Hero Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Text Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(LodgistOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LODGIST TRUST SYSTEM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LodgistOrange,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Find a place in Nigeria, and check before you pay.",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Pay only to the details Lodgist staff send you on WhatsApp. Your payment counts only once Lodgist staff confirm they received it.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Filter Options Toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${properties.size} Properties Available",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showFilterPanel = !showFilterPanel },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("toggle_filters_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filters",
                            modifier = Modifier.size(16.dp),
                            tint = if (showFilterPanel) LodgistOrange else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showFilterPanel) "Hide" else "Filters",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    androidx.compose.material3.FilledTonalButton(
                        onClick = onNavigateToSearch,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("open_search_screen_button")
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Search,
                            contentDescription = "Filter Houses & Flats",
                            modifier = Modifier.size(16.dp),
                            tint = LodgistOrange
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Search Screen",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Expandable Filter Controls
        item {
            AnimatedVisibility(visible = showFilterPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Filter by Property Type",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            propertyTypes.forEach { type ->
                                FilterChip(
                                    selected = filterState.selectedType == type,
                                    onClick = { onTypeChange(type) },
                                    label = { Text(type, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LodgistOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Max Budget
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Max Annual Budget:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatNaira(filterState.maxRentNaira),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LodgistOrange
                            )
                        }

                        Slider(
                            value = filterState.maxRentNaira.toFloat(),
                            onValueChange = { onMaxRentChange(it.toLong()) },
                            valueRange = 300000f..6000000f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = LodgistOrange,
                                activeTrackColor = LodgistOrange
                            )
                        )

                        // TruthScore 90%+ Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Only 90%+ TruthScore™",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Based on listing details; ask Lodgist staff what was checked",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = filterState.onlyHighTruthScore,
                                onCheckedChange = { onToggleHighTruthScore() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LodgistOrange,
                                    checkedTrackColor = LodgistOrange.copy(alpha = 0.35f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onResetFilters,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset All Filters", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // City Tabs
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cities) { city ->
                    val isSelected = filterState.selectedCity == city
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) LodgistOrange else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) LodgistOrange else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onCityChange(city) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("city_tab_$city")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (city == "Nekede") {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else LodgistOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (city == "Nekede") "Nekede (Students)" else city,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Listings Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${properties.size} Places Found",
                    modifier = Modifier.semantics { heading() },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (filterState.selectedCity != "All" || filterState.searchQuery.isNotEmpty()) {
                    Text(
                        text = "Clear filter",
                        fontSize = 12.sp,
                        color = LodgistOrange,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onResetFilters() }
                    )
                }
            }
        }

        // Empty state or list
        if (properties.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "No results",
                            tint = LodgistOrange,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Public Listings Found",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Only Lodgist-reviewed, non-sample homes appear in public inventory. Unreviewed submissions or sample fixtures are excluded from public search.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Owerri Campus Pilot (FUTO, IMSU, Nekede): If you have a rental to list or need enquiry assistance, reach our official field team.",
                            fontSize = 11.sp,
                            color = LodgistTeal,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onResetFilters,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LodgistOrange)
                            ) {
                                Text("Reset Filters", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            items(properties, key = { it.id }) { property ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PropertyCard(
                        property = property,
                        onClick = { onPropertyClick(property) },
                        onToggleFavorite = { onToggleFavorite(property) },
                        onBookInspection = { onBookInspection(property) }
                    )
                }
            }
        }
    }
}
}
