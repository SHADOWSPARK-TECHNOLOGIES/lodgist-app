package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.remote.PublicApproxLocationDto
import com.example.data.remote.PublicListingDto
import com.example.data.remote.PublicListingImageDto
import com.example.data.remote.PublicPropertyDto
import com.example.ui.viewmodel.ListingDetailUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the detail screen's honesty rules: the verification label is the
 * server's word, the location is approximate with a published radius, and a
 * home with no photo says so instead of looking inspected.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ListingDetailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(state: ListingDetailUiState) {
        compose.setContent {
            ListingDetailScreen(uiState = state, onBack = {}, onRetry = {})
        }
    }

    private fun home(
        verified: Boolean = false,
        label: String? = null,
        images: List<PublicListingImageDto> = emptyList(),
        approx: PublicApproxLocationDto? = null
    ) = PublicListingDto(
        id = 41,
        title = "Self contained in Nekede",
        description = "Close to the poly gate.",
        price = 250000.0,
        property = PublicPropertyDto(city = "Owerri", state = "Imo", lga = "Nekede", bedrooms = 2, bathrooms = 1, propertyType = "Apartment"),
        verificationLabel = label,
        evidenceVerified = verified,
        images = images,
        approxLocation = approx
    )

    @Test
    fun `loading state renders`() {
        render(ListingDetailUiState.Loading)
        compose.onNodeWithTag("detail_loading").assertIsDisplayed()
    }

    @Test
    fun `not found explains that unpublished and missing look identical`() {
        render(ListingDetailUiState.NotFound)
        compose.onNodeWithTag("detail_not_found").assertIsDisplayed()
        compose.onNodeWithText("This home is not published").assertIsDisplayed()
        compose.onNodeWithTag("detail_content").assertDoesNotExist()
    }

    @Test
    fun `offline detail is distinct from not found`() {
        render(ListingDetailUiState.Offline)
        compose.onNodeWithTag("detail_offline").assertIsDisplayed()
        compose.onNodeWithTag("detail_not_found").assertDoesNotExist()
    }

    @Test
    fun `unauthorized state renders`() {
        render(ListingDetailUiState.Unauthorized)
        compose.onNodeWithTag("detail_unauthorized").assertIsDisplayed()
    }

    @Test
    fun `failure carries the http code`() {
        render(ListingDetailUiState.Failed(500))
        compose.onNodeWithTag("detail_error").assertIsDisplayed()
        compose.onNodeWithText("The request failed with HTTP 500.").assertIsDisplayed()
    }

    @Test
    fun `content shows the server verification label verbatim`() {
        render(ListingDetailUiState.Content(home(verified = true, label = "Verified by Lodgist")))
        compose.onNodeWithTag("detail_content").assertIsDisplayed()
        compose.onNodeWithText("Verified by Lodgist").assertIsDisplayed()
    }

    @Test
    fun `an unlabelled home reads as not verified`() {
        render(ListingDetailUiState.Content(home(verified = false, label = null)))
        compose.onNodeWithTag("detail_verification_label").assertIsDisplayed()
        compose.onNodeWithText("Not verified by Lodgist").assertIsDisplayed()
        compose.onNodeWithText("Verified by Lodgist").assertDoesNotExist()
    }

    @Test
    fun `location is presented as an approximate area with its radius`() {
        render(
            ListingDetailUiState.Content(
                home(approx = PublicApproxLocationDto(lat = 5.43, lng = 7.03, radius_m = 500))
            )
        )
        compose.onNodeWithTag("detail_approx_location").assertIsDisplayed()
        compose.onNodeWithText(
            "Approximate area only, about 500 m across. Lodgist publishes a nearby point, not the address."
        ).assertIsDisplayed()
    }

    @Test
    fun `a home without photos says so instead of implying inspection`() {
        render(ListingDetailUiState.Content(home(images = emptyList())))
        compose.onNodeWithText("No photo published. Do not treat this home as inspected.")
            .assertIsDisplayed()
    }

    @Test
    fun `published photos are counted`() {
        render(
            ListingDetailUiState.Content(
                home(
                    images = listOf(
                        PublicListingImageDto(id = 1, url = "https://cdn.example/1.jpg"),
                        PublicListingImageDto(id = 2, url = "https://cdn.example/2.jpg")
                    )
                )
            )
        )
        compose.onNodeWithText("2 photo(s) published by the advertiser.").assertIsDisplayed()
    }
}
