package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.EscrowPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EscrowDao {
    @Query("SELECT * FROM escrow_payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<EscrowPaymentEntity>>

    // Amount of requests still awaiting Lodgist staff confirmation (nothing here is held or paid).
    @Query("SELECT SUM(amount) FROM escrow_payments WHERE status IN ('PENDING_PAYMENT', 'AWAITING_PROOF')")
    fun getTotalEscrowVolume(): Flow<Long?>

    // Only staff-confirmed (PAID) records count as revenue. The app never writes PAID locally.
    @Query("SELECT SUM(amount) FROM escrow_payments WHERE status = 'PAID' AND (purpose = 'INSPECTION_TOKEN' OR purpose = 'EXPRESS_VERIFICATION')")
    fun getTotalPlatformRevenue(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: EscrowPaymentEntity): Long

    @Query("SELECT COUNT(*) FROM escrow_payments WHERE transactionRef = :ref")
    suspend fun countByRef(ref: String): Int

    // No generic status writer on purpose (LM-01): only Lodgist staff/server may change a payment status.
}
