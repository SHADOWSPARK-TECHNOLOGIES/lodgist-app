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
    val assignedAgent: String = AGENT_AWAITING_ASSIGNMENT,
    // Local requests start as INSPECTION_PENDING. Other values ("CONFIRMED", "COMPLETED", "CANCELLED")
    // must come from Lodgist staff/server. Kotlin default only: no SQL column default, schema (v3) unchanged.
    val status: String = STATUS_INSPECTION_PENDING,
    val bookingTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_INSPECTION_PENDING = "INSPECTION_PENDING"
        const val AGENT_AWAITING_ASSIGNMENT = "Awaiting Lodgist staff assignment"
    }
}
