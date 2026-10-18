package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a Property listing with Room database schema annotations for local caching.
 *
 * @param id Unique identifier for the property listing
 * @param title Headline or title of the property
 * @param price Price of the property (in Naira or local currency)
 * @param location Physical location, neighborhood, or city
 * @param description Detailed description of the property
 * @param verified Boolean flag indicating if the property has been audited and verified
 */
@Entity(tableName = "cached_properties")
data class Property(
    @PrimaryKey val id: String,
    val title: String,
    val price: Double,
    val location: String,
    val description: String,
    val verified: Boolean = false,
    val bedrooms: Int = 1,
    val bathrooms: Int = 1,
    val propertyType: String = "Flat",
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toPropertyEntity(): PropertyEntity {
        val locParts = location.split(",").map { it.trim() }
        val neigh = locParts.firstOrNull() ?: location
        val city = if (locParts.size > 1) locParts.last() else "Owerri"
        return PropertyEntity(
            id = id,
            title = title,
            propertyType = propertyType,
            city = city,
            neighborhood = neigh,
            state = "Imo",
            address = location,
            annualRent = price.toLong(),
            cautionDeposit = (price * 0.1).toLong(),
            serviceCharge = (price * 0.05).toLong(),
            bedrooms = bedrooms,
            bathrooms = bathrooms,
            // Map only what the API returns. The cached API model carries a single `verified` flag and
            // no TruthScore or per-check results, so none are invented: 0 = no audit data (hidden in UI).
            truthScore = 0,
            verificationStatus = if (verified) "VERIFIED" else "NOT_REVIEWED",
            titleDocumentChecked = false,
            landlordIdentityChecked = false,
            physicalInspectionPassed = false,
            disputeFree = false,
            fairPriceIndexed = false,
            description = description,
            amenities = "Water, Electricity, Security",
            landlordName = "Lodgist listing host",
            landlordPhone = "09136094315",
            drawableResName = if (propertyType.contains("House", ignoreCase = true) || propertyType.contains("Duplex", ignoreCase = true)) "building" else "living",
            price = price,
            location = location,
            verified = verified
        )
    }
}
