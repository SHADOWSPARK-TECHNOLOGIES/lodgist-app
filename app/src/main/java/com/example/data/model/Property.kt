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
)
