package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.EscrowDao
import com.example.data.local.InspectionDao
import com.example.data.local.LandlordListingDao
import com.example.data.local.PropertyDao
import com.example.data.local.PropertyListingDao
import com.example.data.repository.LodgistRepository
import com.example.data.repository.PropertyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dependency Injection Module providing the Room [AppDatabase] instance and DAOs.
 *
 * Implements a clean, decoupled Dependency Injection pattern for database
 * components, allowing seamless provision, testing, and lifecycle management.
 */
object DatabaseModule {

    @Volatile
    private var databaseInstance: AppDatabase? = null

    /**
     * Provides the singleton instance of the Room [AppDatabase].
     *
     * @param context Application context used to build the Room database.
     * @return Thread-safe singleton instance of [AppDatabase].
     */
    fun provideAppDatabase(context: Context): AppDatabase {
        return databaseInstance ?: synchronized(this) {
            databaseInstance ?: AppDatabase.getDatabase(context).also {
                databaseInstance = it
            }
        }
    }

    /**
     * Provides the [PropertyListingDao] from the given [AppDatabase].
     */
    fun providePropertyListingDao(database: AppDatabase): PropertyListingDao {
        return database.propertyListingDao()
    }

    /**
     * Provides the [PropertyDao] from the given [AppDatabase].
     */
    fun providePropertyDao(database: AppDatabase): PropertyDao {
        return database.propertyDao()
    }

    /**
     * Provides the [InspectionDao] from the given [AppDatabase].
     */
    fun provideInspectionDao(database: AppDatabase): InspectionDao {
        return database.inspectionDao()
    }

    /**
     * Provides the [LandlordListingDao] from the given [AppDatabase].
     */
    fun provideLandlordListingDao(database: AppDatabase): LandlordListingDao {
        return database.landlordListingDao()
    }

    /**
     * Provides the [EscrowDao] from the given [AppDatabase].
     */
    fun provideEscrowDao(database: AppDatabase): EscrowDao {
        return database.escrowDao()
    }

    /**
     * Factory method providing [PropertyRepository] with its DAO and IO dispatcher.
     *
     * @param database The [AppDatabase] providing the [PropertyListingDao].
     * @param ioDispatcher The [CoroutineDispatcher] for disk I/O (defaults to [Dispatchers.IO]).
     */
    fun providePropertyRepository(
        database: AppDatabase,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): PropertyRepository {
        return PropertyRepository(
            propertyDao = providePropertyListingDao(database),
            ioDispatcher = ioDispatcher
        )
    }

    /**
     * Factory method providing [LodgistRepository] with all required DAOs.
     *
     * @param database The [AppDatabase] providing the DAOs.
     */
    fun provideLodgistRepository(database: AppDatabase): LodgistRepository {
        return LodgistRepository(
            propertyDao = providePropertyDao(database),
            inspectionDao = provideInspectionDao(database),
            landlordListingDao = provideLandlordListingDao(database),
            escrowDao = provideEscrowDao(database),
            apiService = NetworkModule.provideLodgistApiService()
        )
    }

    /**
     * Resets the cached database instance (primarily for automated tests).
     */
    fun resetForTests() {
        databaseInstance = null
    }
}
