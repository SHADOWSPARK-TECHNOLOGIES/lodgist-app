package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.InspectionBookingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspection_bookings ORDER BY bookingTimestamp DESC")
    fun getAllBookings(): Flow<List<InspectionBookingEntity>>

    @Query("SELECT * FROM inspection_bookings WHERE propertyId = :propertyId ORDER BY bookingTimestamp DESC")
    fun getBookingsForProperty(propertyId: String): Flow<List<InspectionBookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: InspectionBookingEntity): Long

    // No generic update/status writer on purpose (LM-01): only Lodgist staff/server may confirm a visit.

    @Query("DELETE FROM inspection_bookings WHERE id = :bookingId")
    suspend fun cancelBooking(bookingId: Long)
}
