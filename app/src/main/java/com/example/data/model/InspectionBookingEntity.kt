package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspection_bookings")
data class InspectionBookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: String,
    val propertyTitle: String,
    val propertyLocation: String,
    val clientName: String,
    val clientPhone: String,
    val preferredDate: String,      // e.g. "Tomorrow, Oct 5"
    val preferredTimeSlot: String,  // e.g. "Morning (10:00 AM - 12:00 PM)"
    val notes: String = "",
    val assignedAgent: String = "Lodgist Field Inspector (Chidi O.)",
    val status: String = "CONFIRMED", // "CONFIRMED", "INSPECTION_PENDING", "COMPLETED", "CANCELLED"
    val bookingTimestamp: Long = System.currentTimeMillis()
)
