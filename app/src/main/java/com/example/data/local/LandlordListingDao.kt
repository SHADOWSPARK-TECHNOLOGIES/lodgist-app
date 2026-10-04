package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LandlordListingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LandlordListingDao {
    @Query("SELECT * FROM landlord_listings ORDER BY submittedAt DESC")
    fun getAllSubmissions(): Flow<List<LandlordListingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(listing: LandlordListingEntity): Long
}
