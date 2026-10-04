package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "landlord_listings")
data class LandlordListingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val propertyType: String,
    val state: String,
    val city: String,
    val neighborhood: String,
    val annualRent: Long,
    val bedrooms: Int,
    val bathrooms: Int,
    val ownerFullName: String,
    val ownerPhone: String,
    val hasTitleDeed: Boolean,
    val hasNINVerified: Boolean,
    val hasBoreholeWater: Boolean,
    val hasPrepaidMeter: Boolean,
    val status: String = "UNDER_VERIFICATION", // "UNDER_VERIFICATION", "PHYSICAL_VISIT_SCHEDULED", "APPROVED_LISTED"
    val submittedAt: Long = System.currentTimeMillis()
)
