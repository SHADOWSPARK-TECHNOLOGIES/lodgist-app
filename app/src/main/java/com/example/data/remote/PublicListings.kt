package com.example.data.remote

import com.example.data.model.PropertyEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Read of GET /api/listings on the Lodgist API.
 * Owned by the web/API project. This client only adapts the published shape.
 */
object PublicListings {
    const val VERIFIED_LABEL = "Verified by Lodgist"
    const val NOT_VERIFIED_LABEL = "Not verified by Lodgist"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(PublicListingsResponse::class.java)

    fun read(httpCode: Int, body: String?): ListingsRead {
        if (httpCode == 404) return ListingsRead.Unknown
        if (httpCode != 200 || body.isNullOrBlank()) return ListingsRead.Failed(httpCode)
        return try {
            val parsed = adapter.fromJson(body) ?: return ListingsRead.Failed(httpCode)
            if (!parsed.success) return ListingsRead.Failed(httpCode)
            ListingsRead.Loaded(parsed.data.mapNotNull { it.toEntity() })
        } catch (_: Exception) {
            ListingsRead.Failed(httpCode)
        }
    }

    fun isDemonstrationId(id: String): Boolean = id.startsWith("lodg_")
}

sealed class ListingsRead {
    data class Loaded(val homes: List<PropertyEntity>) : ListingsRead()
    data object Unknown : ListingsRead()
    data class Failed(val httpCode: Int) : ListingsRead()
}

@JsonClass(generateAdapter = true)
data class PublicListingsResponse(
    val success: Boolean = false,
    val data: List<PublicListingDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PublicListingDto(
    val id: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val status: String? = null,
    val property: PublicPropertyDto? = null,
    val verificationLabel: String? = null
) {
    fun toEntity(): PropertyEntity? {
        val listingId = id?.toString() ?: return null
        val label = verificationLabel ?: PublicListings.NOT_VERIFIED_LABEL
        val evidenceVerified = label == PublicListings.VERIFIED_LABEL
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
