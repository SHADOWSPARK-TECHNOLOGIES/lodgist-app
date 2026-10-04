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

    @Query("SELECT SUM(amount) FROM escrow_payments WHERE status = 'HELD_IN_ESCROW'")
    fun getTotalEscrowVolume(): Flow<Long?>

    @Query("SELECT SUM(amount) FROM escrow_payments WHERE purpose = 'INSPECTION_TOKEN' OR purpose = 'EXPRESS_VERIFICATION'")
    fun getTotalPlatformRevenue(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: EscrowPaymentEntity): Long

    @Query("UPDATE escrow_payments SET status = :newStatus WHERE id = :paymentId")
    suspend fun updateStatus(paymentId: Long, newStatus: String)
}
