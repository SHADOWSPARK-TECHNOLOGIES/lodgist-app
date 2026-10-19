package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "escrow_payments")
data class EscrowPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: String,
    val propertyTitle: String,
    val payerName: String,
    val payerPhone: String,
    val amount: Long,           // in Naira (₦)
    val purpose: String,        // "INSPECTION_TOKEN" (₦3,500), "ANNUAL_RENT_ESCROW", "EXPRESS_VERIFICATION" (₦15,000)
    val paymentMethod: String,  // "DEBIT_CARD", "DIRECT_BANK_TRANSFER", "USSD"
    val transactionRef: String,
    // Local requests are PENDING_PAYMENT / AWAITING_PROOF only. PAID etc. must come from Lodgist staff/server.
    // Legacy rows may still hold "HELD_IN_ESCROW", "RELEASED_TO_LANDLORD" or "REFUNDED" (read-only, not written).
    // Kotlin default only: Room has no SQL column default here, so the schema (v3) is unchanged.
    val status: String = STATUS_PENDING_PAYMENT,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING_PAYMENT = "PENDING_PAYMENT"
        const val STATUS_AWAITING_PROOF = "AWAITING_PROOF"
    }
}
