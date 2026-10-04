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
    val status: String = "HELD_IN_ESCROW", // "HELD_IN_ESCROW", "RELEASED_TO_LANDLORD", "REFUNDED"
    val timestamp: Long = System.currentTimeMillis()
)
