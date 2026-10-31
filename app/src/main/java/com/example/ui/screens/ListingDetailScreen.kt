package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.PublicListingDto
import com.example.ui.components.formatNaira
import com.example.ui.theme.LodgistOrange
import com.example.ui.theme.LodgistTeal
import com.example.ui.viewmodel.ListingDetailUiState

/**
 * One published home, as the API describes it.
 *
 * The screen reports three things it must never embellish: the verification
 * label (server's word, absent means not verified), the location (an
 * approximate area with a published radius, never an address), and the
 * absence of photos when there are none.
 */
@Composable
fun ListingDetailScreen(
    uiState: ListingDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        ListingDetailUiState.Loading -> DetailStatus(
            tag = "detail_loading",
            title = "Loading this home",
            body = "Asking the Lodgist API for the published record.",
            loading = true,
            onBack = onBack,
            modifier = modifier
        )

        ListingDetailUiState.NotFound -> DetailStatus(
            tag = "detail_not_found",
            title = "This home is not published",
            body = "The API has no public listing under this reference. Unpublished homes answer the same way, so Lodgist cannot tell you whether it exists.",
            icon = Icons.Filled.SearchOff,
            onBack = onBack,
            modifier = modifier
        )

        ListingDetailUiState.Offline -> DetailStatus(
            tag = "detail_offline",
            title = "You are offline",
            body = "We could not reach the Lodgist API, so this home could not be loaded.",
            icon = Icons.Filled.CloudOff,
            retryLabel = "Try again",
            onRetry = onRetry,
            onBack = onBack,
            modifier = modifier
        )

        ListingDetailUiState.Unauthorized -> DetailStatus(
            tag = "detail_unauthorized",
            title = "Not allowed",
            body = "The API refused this request.",
            icon = Icons.Filled.Lock,
            retryLabel = "Try again",
            onRetry = onRetry,
            onBack = onBack,
            modifier = modifier
        )

        is ListingDetailUiState.Failed -> DetailStatus(
            tag = "detail_error",
            title = "The API could not answer",
            body = "The request failed with HTTP ${uiState.httpCode}.",
            retryLabel = "Retry",
            onRetry = onRetry,
            onBack = onBack,
            modifier = modifier
        )

        is ListingDetailUiState.Content -> DetailContent(home = uiState.home, onBack = onBack, modifier = modifier)
    }
}

@Composable
private fun DetailContent(home: PublicListingDto, onBack: () -> Unit, modifier: Modifier) {
    val place = home.property
    val area = listOfNotNull(
        place?.city?.takeIf { it.isNotBlank() },
        place?.lga?.takeIf { it.isNotBlank() },
        place?.state?.takeIf { it.isNotBlank() }
    ).distinct().joinToString(", ")

    val verified = home.evidenceVerified == true
    val label = home.verificationLabel ?: "Not verified by Lodgist"
    val approx = home.approxLocation

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("detail_content")
    ) {
        Text(
            text = home.title?.takeIf { it.isNotBlank() } ?: "Untitled listing",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (area.isNotBlank()) area else "Area not published",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatNaira(home.price?.toLong() ?: 0L),
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = LodgistOrange
            )
            Text(" / year", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (verified) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = LodgistTeal)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (verified) LodgistTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("detail_verification_label")
            )
        }

        if (place != null && (place.bedrooms != null || place.bathrooms != null || place.propertyType != null)) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = listOfNotNull(
                    place.propertyType?.takeIf { it.isNotBlank() },
                    place.bedrooms?.let { "$it bed" },
                    place.bathrooms?.let { "$it bath" }
                ).joinToString(" · "),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text(
            text = home.description?.takeIf { it.isNotBlank() } ?: "No description published.",
            fontSize = 14.sp
        )

        Spacer(Modifier.height(20.dp))
        Text("Location", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (approx?.lat != null && approx.lng != null) {
                "Approximate area only, about ${approx.radius_m ?: 0} m across. Lodgist publishes a nearby point, not the address."
            } else {
                "No approximate location published for this home."
            },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("detail_approx_location")
        )

        Spacer(Modifier.height(20.dp))
        Text("Photos", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (home.images.isEmpty()) {
                "No photo published. Do not treat this home as inspected."
            } else {
                "${home.images.size} photo(s) published by the advertiser."
            },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("detail_photos")
        )

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.testTag("detail_back_button")
        ) {
            Text("Back to listings")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailStatus(
    tag: String,
    title: String,
    body: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loading) {
            CircularProgressIndicator(color = LodgistOrange)
            Spacer(Modifier.height(16.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = LodgistOrange)
            Spacer(Modifier.height(12.dp))
        }
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        if (retryLabel != null && onRetry != null) {
            Button(onClick = onRetry, modifier = Modifier.testTag("${tag}_retry")) {
                Text(retryLabel)
            }
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.testTag("${tag}_back")) {
            Text("Back to listings")
        }
    }
}
