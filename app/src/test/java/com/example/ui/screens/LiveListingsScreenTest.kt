package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.remote.PublicListingActionsDto
import com.example.data.remote.PublicListingDto
import com.example.data.remote.PublicPropertyDto
import com.example.ui.viewmodel.LiveListingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Renders each live-listings state and pins the two rules that matter:
 * every failure mode looks different, and the client never claims a home is
 * verified on its own authority.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveListingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(state: LiveListingsUiState) {
        compose.setContent {
            LiveListingsScreen(uiState = state, onRetry = {})
        }
    }

    private fun home(
        verified: Boolean,
        label: String?,
        title: String = "Self contained in Nekede"
    ) = PublicListingDto(
        id = 41,
        title = title,
        price = 250000.0,
        property = PublicPropertyDto(city = "Owerri", state = "Imo", lga = "Nekede"),
        verificationLabel = label,
        evidenceVerified = verified,
        actions = PublicListingActionsDto(enquiry = true, viewing = true)
    )

    @Test
    fun `loading state is shown`() {
        render(LiveListingsUiState.Loading)
        compose.onNodeWithTag("live_listings_loading").assertIsDisplayed()
    }

    @Test
    fun `empty state says the api answered with nothing, and is distinct from offline`() {
        render(LiveListingsUiState.Empty)
        compose.onNodeWithTag("live_listings_empty").assertIsDisplayed()
        compose.onNodeWithTag("live_listings_offline").assertDoesNotExist()
        compose.onNodeWithText("No published homes yet").assertIsDisplayed()
    }

    @Test
    fun `offline state never claims the catalogue is empty`() {
        render(LiveListingsUiState.Offline)
        compose.onNodeWithTag("live_listings_offline").assertIsDisplayed()
        compose.onNodeWithTag("live_listings_empty").assertDoesNotExist()
        compose.onNodeWithText("You are offline").assertIsDisplayed()
    }

    @Test
    fun `unauthorized state is its own screen`() {
        render(LiveListingsUiState.Unauthorized)
        compose.onNodeWithTag("live_listings_unauthorized").assertIsDisplayed()
        compose.onNodeWithTag("live_listings_empty").assertDoesNotExist()
    }

    @Test
    fun `failed state reports the http code`() {
        render(LiveListingsUiState.Failed(502))
        compose.onNodeWithTag("live_listings_error").assertIsDisplayed()
        compose.onNodeWithText("The request failed with HTTP 502. Nothing was invented to fill the gap.")
            .assertIsDisplayed()
    }

    @Test
    fun `content renders server-provided fields`() {
        render(
            LiveListingsUiState.Content(
                listOf(home(verified = true, label = "Verified by Lodgist"))
            )
        )
        compose.onNodeWithTag("live_listings_list").assertIsDisplayed()
        compose.onNodeWithText("Self contained in Nekede").assertIsDisplayed()
        compose.onNodeWithText("₦250,000").assertIsDisplayed()
        compose.onNodeWithText("Owerri, Nekede, Imo").assertIsDisplayed()
        compose.onNodeWithText("Verified by Lodgist").assertIsDisplayed()
    }

    @Test
    fun `client cannot claim verification the server did not send`() {
        // evidenceVerified false and no label: the only honest reading.
        render(LiveListingsUiState.Content(listOf(home(verified = false, label = null))))
        compose.onNodeWithTag("live_listing_label_41").assertIsDisplayed()
        compose.onNodeWithText("Not verified by Lodgist").assertIsDisplayed()
        compose.onNodeWithText("Verified by Lodgist").assertDoesNotExist()
    }

    @Test
    fun `client does not upgrade an unverified home even when the boolean is missing`() {
        render(
            LiveListingsUiState.Content(
                listOf(home(verified = false, label = "Not verified by Lodgist", title = "Room at Nekede"))
            )
        )
        compose.onNodeWithText("Not verified by Lodgist").assertIsDisplayed()
        compose.onNodeWithText("Room at Nekede").assertIsDisplayed()
    }
}
