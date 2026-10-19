package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LodgistOrange
import com.example.ui.theme.LodgistTeal

/**
 * Top-level search and filter bar for property listings.
 *
 * Allows users to filter properties by location (e.g. Owerri, Lagos, Abuja, Nekede)
 * or keywords (e.g. self-contain, solar inverter, borehole, Lekki).
 *
 * @param searchQuery Current search query string.
 * @param onSearchQueryChange Callback when search text changes.
 * @param selectedLocation Currently selected quick-filter location.
 * @param onLocationSelected Callback when a location chip is selected.
 * @param verifiedOnly Whether verified-only filter is active.
 * @param onToggleVerified Callback to toggle verified-only flag.
 * @param modifier Composable modifier.
 */
@Composable
fun PropertySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedLocation: String = "",
    onLocationSelected: (String) -> Unit = {},
    verifiedOnly: Boolean = false,
    onToggleVerified: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val quickLocations = listOf("All", "Owerri", "Nekede", "Lagos", "Abuja", "Port Harcourt")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("property_search_bar_container"),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Main Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search location or keywords (e.g. Owerri, self contain, Lekki)...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = LodgistOrange,
                        modifier = Modifier.size(22.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("property_search_clear_button")
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
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LodgistOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("property_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Filter Chips (Locations). Verified-only chip hidden until real verification data exists (LM-03).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Location Chips
                quickLocations.forEach { loc ->
                    val isSelected = if (loc == "All") {
                        selectedLocation.isEmpty() || selectedLocation.equals("All", ignoreCase = true)
                    } else {
                        selectedLocation.equals(loc, ignoreCase = true)
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newLoc = if (loc == "All") "" else loc
                            onLocationSelected(newLoc)
                        },
                        label = {
                            Text(
                                text = loc,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (loc != "All") {
                            {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) LodgistOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LodgistOrange.copy(alpha = 0.18f),
                            selectedLabelColor = LodgistOrange
                        ),
                        modifier = Modifier.testTag("filter_chip_location_$loc")
                    )
                }
            }
        }
    }
}
