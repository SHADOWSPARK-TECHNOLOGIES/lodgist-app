package com.example.data.remote.model

import com.example.data.model.Property
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data Transfer Object (DTO) representing a property listing from the Lodgist backend API.
 */
@JsonClass(generateAdapter = true)
data class PropertyDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "price") val price: Double,
    @Json(name = "location") val location: String,
    @Json(name = "description") val description: String,
    @Json(name = "verified") val verified: Boolean = false,
    @Json(name = "bedrooms") val bedrooms: Int? = 1,
    @Json(name = "bathrooms") val bathrooms: Int? = 1,
    @Json(name = "property_type") val propertyType: String? = "Flat",
    @Json(name = "city") val city: String? = null,
    @Json(name = "neighborhood") val neighborhood: String? = null,
    @Json(name = "annual_rent") val annualRent: Long? = null,
    @Json(name = "caution_deposit") val cautionDeposit: Long? = null,
    @Json(name = "service_charge") val serviceCharge: Long? = null,
    @Json(name = "truth_score") val truthScore: Int? = null,
    @Json(name = "verification_status") val verificationStatus: String? = null,
    @Json(name = "amenities") val amenities: List<String>? = null,
    @Json(name = "landlord_name") val landlordName: String? = null,
    @Json(name = "landlord_phone") val landlordPhone: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "created_at") val createdAt: Long? = null
) {
    /**
     * Maps the API DTO into the local Room database [Property] model for caching.
     */
    fun toProperty(): Property {
        val effectivePrice = if (price > 0) price else (annualRent?.toDouble() ?: 0.0)
        val effectiveLocation = if (location.isNotBlank()) {
            location
        } else {
            listOfNotNull(neighborhood, city).filter { it.isNotBlank() }.joinToString(", ")
        }
        val isVerified = verified || verificationStatus.equals("VERIFIED", ignoreCase = true)

        return Property(
            id = id,
            title = title,
            price = effectivePrice,
            location = effectiveLocation,
            description = description,
            verified = isVerified,
            bedrooms = bedrooms ?: 1,
            bathrooms = bathrooms ?: 1,
            propertyType = propertyType ?: "Flat",
            cachedAt = createdAt ?: System.currentTimeMillis()
        )
    }

    companion object {
        /**
         * Creates a [PropertyDto] from a local [Property] entity for syncing or remote uploads.
         */
        fun fromProperty(property: Property): PropertyDto {
            return PropertyDto(
                id = property.id,
                title = property.title,
                price = property.price,
                location = property.location,
                description = property.description,
                verified = property.verified,
                bedrooms = property.bedrooms,
                bathrooms = property.bathrooms,
                propertyType = property.propertyType,
                createdAt = property.cachedAt
            )
        }
    }
}

/**
 * Standard API response wrapper for paginated or envelope responses from Lodgist backend.
 */
@JsonClass(generateAdapter = true)
data class PropertyListResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "status") val status: String? = null,
    @Json(name = "total") val total: Int? = 0,
    @Json(name = "data") val data: List<PropertyDto> = emptyList()
)
