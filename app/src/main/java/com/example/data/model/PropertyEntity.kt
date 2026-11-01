package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey val id: String,
    val title: String,
    val propertyType: String, // "Self-Contain", "1 Bedroom Flat", "2 Bedroom Flat", "3 Bedroom Duplex", "Studio Apartment"
    val city: String,         // "Owerri", "Lagos", "Port Harcourt", "Abuja", "Ibadan"
    val neighborhood: String, // "Nekede / Poly Road", "Lekki Phase 1", "Peter Odili Road", "Gwarinpa Estate", etc.
    val state: String,        // "Imo", "Lagos", "Rivers", "Abuja FCT", "Oyo"
    val address: String,
    val annualRent: Long,     // In Naira (₦)
    val cautionDeposit: Long,
    val serviceCharge: Long,
    val bedrooms: Int,
    val bathrooms: Int,
    val truthScore: Int,      // 0 - 100
    val verificationStatus: String, // "VERIFIED", "LODGIST_REVIEWED", "PENDING"
    val titleDocumentChecked: Boolean,
    val landlordIdentityChecked: Boolean,
    val physicalInspectionPassed: Boolean,
    val disputeFree: Boolean,
    val fairPriceIndexed: Boolean,
    val description: String,
    val amenities: String,    // Comma-separated
    val landlordName: String,
    val landlordPhone: String,
    val supportWhatsapp: String = "2349136094315",
    val isFavorite: Boolean = false,
    val drawableResName: String, // "hero", "living", "building"
    val isFeatured: Boolean = false,
    val isStudentFriendly: Boolean = false, // e.g. Nekede campus area
    val totalAvailableUnits: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val price: Double = annualRent.toDouble(),
    val location: String = "$neighborhood, $city",
    val verified: Boolean = (verificationStatus == "VERIFIED")
) {
    val totalMoveInCost: Long
        get() = annualRent + cautionDeposit + serviceCharge

    fun amenityList(): List<String> {
        return amenities.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
