package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.EscrowPaymentEntity
import com.example.data.model.InspectionBookingEntity
import com.example.data.model.LandlordListingEntity
import com.example.data.model.PropertyEntity

/**
 * Main Room database for the application.
 *
 * Houses cached property listings, inspection walkthrough appointments,
 * landlord onboarding submissions, and secure escrow transaction records.
 */
@Database(
    entities = [
        PropertyEntity::class,
        InspectionBookingEntity::class,
        LandlordListingEntity::class,
        EscrowPaymentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun landlordListingDao(): LandlordListingDao
    abstract fun escrowDao(): EscrowDao

    companion object {
        const val DATABASE_NAME = "lodgist_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of [AppDatabase].
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
