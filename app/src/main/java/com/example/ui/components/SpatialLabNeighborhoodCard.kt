package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PropertyEntity
import com.example.ui.theme.LodgistOrange
import com.example.ui.theme.LodgistTeal
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CampusTarget(
    val id: String,
    val name: String,
    val shortName: String,
    val latitude: Double,
    val longitude: Double
)

val OWERRI_CAMPUSES = listOf(
    CampusTarget("futo", "Federal University of Technology Owerri", "FUTO (Ihiagwa)", 5.3850, 6.9930),
    CampusTarget("imsu", "Imo State University", "IMSU (Aladinma)", 5.5036, 7.0425),
    CampusTarget("poly", "Federal Polytechnic Nekede", "Poly Nekede", 5.4380, 7.0260)
)

/**
 * Calculates straight-line distance in kilometers using the Haversine formula.
 */
fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

/**
 * Approximate center coordinates for recognized Owerri pilot neighborhoods.
 */
fun getNeighborhoodCoordinates(neighborhood: String, city: String): Pair<Double, Double> {
    val lower = neighborhood.lowercase()
    return when {
        lower.contains("nekede") || lower.contains("poly") -> Pair(5.4380, 7.0260)
        lower.contains("aladinma") || lower.contains("imsu") -> Pair(5.5036, 7.0425)
        lower.contains("world bank") || lower.contains("new owerri") -> Pair(5.4750, 7.0080)
        lower.contains("ihiagwa") || lower.contains("futo") -> Pair(5.3900, 6.9950)
        lower.contains("trans-amadi") || lower.contains("odili") -> Pair(4.8156, 7.0498)
        lower.contains("lekki") || lower.contains("yaba") -> Pair(6.4698, 3.5852)
        lower.contains("gwarinpa") -> Pair(9.1099, 7.4042)
        else -> Pair(5.4850, 7.0250) // Default Owerri central
    }
}

/**
 * Spatial Lab Neighborhood View integration for eligible public listings.
 *
 * Implements strict Spatial Lab product rules:
 * - Shows published neighborhood area or no pin (no private interior plotting).
 * - Allows campus choice and displays straight-line distance to the campus center.
 * - Prominently displays: "Map imagery does not prove ownership, condition, availability, or safety."
 */
@Composable
fun SpatialLabNeighborhoodCard(
    property: PropertyEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCampus by remember { mutableStateOf(OWERRI_CAMPUSES.first { it.id == "poly" || it.id == "imsu" }) }

    val (propLat, propLon) = getNeighborhoodCoordinates(property.neighborhood, property.city)
    val distanceKm = calculateDistanceKm(propLat, propLon, selectedCampus.latitude, selectedCampus.longitude)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spatial_lab_neighborhood_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(LodgistTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Spatial Lab Map",
                            tint = LodgistTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Spatial Lab • Campus Distance",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Published Area • ${property.neighborhood}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "NO PRIVATE PIN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LodgistTeal,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Campus Selector Chips
            Text(
                text = "Select Campus Center:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OWERRI_CAMPUSES.forEach { campus ->
                    val isSelected = campus.id == selectedCampus.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCampus = campus },
                        label = {
                            Text(
                                text = campus.shortName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) LodgistOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LodgistOrange.copy(alpha = 0.15f),
                            selectedLabelColor = LodgistOrange
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Straight-line Distance Result Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = LodgistOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Straight-line Distance (Flight Path)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "To ${selectedCampus.name} (Not road distance; varies by local transit)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "${"%.1f".format(distanceKm)} km",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LodgistOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mandatory Spatial Lab Product Disclaimer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Disclaimer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Map imagery does not prove ownership, condition, availability, or safety.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Spatial Lab preview link
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lodgist.online/spatial-lab/preview"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_spatial_lab_preview_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "View Spatial Lab Web Preview",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
