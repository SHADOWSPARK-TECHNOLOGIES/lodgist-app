package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.viewmodel.LiveListingsUiState

/**
 * Homes published by the Lodgist API right now.
 *
 * This screen renders server state and nothing else. It never upgrades a home
 * to "verified" on its own: the badge comes from `verificationLabel` /
 * `evidenceVerified` exactly as the API sent them, and a missing label reads as
 * not verified.
 */
@Composable
fun LiveListingsScreen(
    uiState: LiveListingsUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDetail: (Long) -> Unit = {}
) {
    when (uiState) {
        LiveListingsUiState.Loading -> StatusMessage(
            tag = "live_listings_loading",
            title = "Loading published homes",
            body = "Asking the Lodgist API for what is live right now.",
            showSpinner = true,
            modifier = modifier
        )

        LiveListingsUiState.Empty -> StatusMessage(
            tag = "live_listings_empty",
            title = "No published homes yet",
            body = "The API answered. Nothing is published at the moment, so there is nothing to show.",
            showSpinner = false,
            actionLabel = "Check again",
            onAction = onRetry,
            modifier = modifier
        )

        LiveListingsUiState.Offline -> StatusMessage(
            tag = "live_listings_offline",
            title = "You are offline",
            body = "We could not reach the Lodgist API. This is a connection problem, not an empty catalogue.",
            showSpinner = false,
            icon = Icons.Filled.CloudOff,
            actionLabel = "Try again",
            onAction = onRetry,
            modifier = modifier
        )

        LiveListingsUiState.Unauthorized -> StatusMessage(
            tag = "live_listings_unauthorized",
            title = "Not allowed",
            body = "The API refused this request. Sign in again before browsing published homes.",
            showSpinner = false,
            icon = Icons.Filled.Lock,
            actionLabel = "Try again",
            onAction = onRetry,
            modifier = modifier
        )

        is LiveListingsUiState.Failed -> StatusMessage(
            tag = "live_listings_error",
            title = "The API could not answer",
            body = "The request failed with HTTP ${uiState.httpCode}. Nothing was invented to fill the gap.",
            showSpinner = false,
            actionLabel = "Retry",
            onAction = onRetry,
            modifier = modifier
        )

        is LiveListingsUiState.Content -> LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("live_listings_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Live from the Lodgist API",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("live_listings_source")
                )
            }
            items(uiState.homes, key = { it.id ?: 0L }) { home ->
                ListingRow(home, onOpenDetail)
            }
        }
    }
}

@Composable
private fun ListingRow(home: PublicListingDto, onOpenDetail: (Long) -> Unit) {
    val place = home.property
    val area = listOfNotNull(
        place?.city?.takeIf { it.isNotBlank() },
        place?.lga?.takeIf { it.isNotBlank() },
        place?.state?.takeIf { it.isNotBlank() }
    ).distinct().joinToString(", ")

    // Server's word only. Absent label means not verified — never the reverse.
    val verified = home.evidenceVerified == true
    val label = home.verificationLabel ?: "Not verified by Lodgist"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = home.id != null) { home.id?.let(onOpenDetail) }
            .testTag("live_listing_${home.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = home.title?.takeIf { it.isNotBlank() } ?: "Untitled listing",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (area.isNotBlank()) area else "Area not published",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatNaira(home.price?.toLong() ?: 0L),
                    fontWeight = FontWeight.SemiBold,
                    color = LodgistOrange
                )
                Text(
                    text = " / year",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (verified) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = LodgistTeal
                    )
                }
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (verified) LodgistTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("live_listing_label_${home.id}")
                )
            }
        }
    }
}

@Composable
private fun StatusMessage(
    tag: String,
    title: String,
    body: String,
    showSpinner: Boolean,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showSpinner) {
            CircularProgressIndicator(color = LodgistOrange)
            Spacer(Modifier.height(16.dp))
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = LodgistOrange)
            Spacer(Modifier.height(12.dp))
        }
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.testTag("${tag}_action")
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text(text = actionLabel)
            }
        }
    }
}
