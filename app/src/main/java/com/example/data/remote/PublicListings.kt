package com.example.data.remote

import com.example.data.model.PropertyEntity
import com.squareup.moshi.JsonClass

/** The label the API uses when a home has been evidence-checked. */
const val VERIFIED_LABEL = "Verified by Lodgist"

/** Everything else, including a missing label, reads as not verified. */
const val NOT_VERIFIED_LABEL = "Not verified by Lodgist"

@JsonClass(generateAdapter = true)
data class PublicListingsResponse(
    val success: Boolean = false,
    val data: List<PublicListingDto> = emptyList(),
    val pagination: PublicPaginationDto? = null
)

@JsonClass(generateAdapter = true)
data class PublicPaginationDto(
    val total: Int? = null,
    val page: Int? = null,
    val pages: Int? = null,
    val totalPages: Int? = null,
    val limit: Int? = null,
    val pageSize: Int? = null
)

@JsonClass(generateAdapter = true)
data class PublicListingDto(
    val id: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    val status: String? = null,
    val property: PublicPropertyDto? = null,
    val verificationLabel: String? = null,
    val evidenceVerified: Boolean? = null,
    val actions: PublicListingActionsDto? = null,
    val images: List<PublicListingImageDto> = emptyList()
) {
    fun toEntity(): PropertyEntity? {
        val listingId = id?.toString() ?: return null
        val label = verificationLabel ?: NOT_VERIFIED_LABEL
        val evidenceVerified = label == VERIFIED_LABEL
        val place = property
        val city = place?.city?.takeIf { it.isNotBlank() } ?: ""
        val area = place?.lga?.takeIf { it.isNotBlank() } ?: ""
        val rent = price?.toLong() ?: 0L
        return PropertyEntity(
            id = listingId,
            title = title?.takeIf { it.isNotBlank() } ?: "Untitled listing",
            propertyType = place?.propertyType?.takeIf { it.isNotBlank() } ?: "Home",
            city = city,
            neighborhood = area,
            state = place?.state?.takeIf { it.isNotBlank() } ?: "",
            address = "",
            annualRent = rent,
            cautionDeposit = 0L,
            serviceCharge = 0L,
            bedrooms = place?.bedrooms ?: 0,
            bathrooms = place?.bathrooms ?: 0,
            truthScore = 0,
            verificationStatus = if (evidenceVerified) "VERIFIED" else "NOT_VERIFIED",
            titleDocumentChecked = false,
            landlordIdentityChecked = false,
            physicalInspectionPassed = false,
            disputeFree = false,
            fairPriceIndexed = false,
            description = description ?: "",
            amenities = "",
            landlordName = "",
            landlordPhone = "",
            supportWhatsapp = "",
            drawableResName = "none",
            verified = evidenceVerified
        )
    }
}

@JsonClass(generateAdapter = true)
data class PublicPropertyDto(
    val city: String? = null,
    val state: String? = null,
    val lga: String? = null,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val propertyType: String? = null
)

/** Server-decided affordances. The client mirrors them; it does not invent them. */
@JsonClass(generateAdapter = true)
data class PublicListingActionsDto(
    val enquiry: Boolean? = null,
    val viewing: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class PublicListingImageDto(
    val id: Long? = null,
    val url: String? = null,
    val altText: String? = null,
    val isPrimary: Boolean? = null,
    val order: Int? = null
)
