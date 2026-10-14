package com.example.data.repository

import com.example.data.local.PropertyListingDao
import com.example.data.model.Property
import com.example.data.remote.api.LodgistApiService
import com.example.data.remote.model.PropertyDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository class that abstracts data operations between the Lodgist backend API,
 * Room local cache DAO, and the ViewModel.
 *
 * Implements an Offline-First caching strategy:
 * - Emits cached data from Room immediately via reactive [Flow]s.
 * - Synchronizes with the Lodgist website backend API when online.
 * - Caches fetched listings locally for offline resilience.
 *
 * @param propertyDao The Room Data Access Object for local caching.
 * @param apiService Optional Retrofit service for network calls to Lodgist backend.
 * @param ioDispatcher Coroutine dispatcher for disk & network I/O (defaults to [Dispatchers.IO]).
 */
class PropertyRepository(
    private val propertyDao: PropertyListingDao,
    private val apiService: LodgistApiService? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Cold [Flow] observing all cached property listings, ordered by ascending price.
     * Emits a new list whenever the underlying Room database table changes.
     */
    val allCachedProperties: Flow<List<Property>> = propertyDao.getAllCachedProperties()

    /**
     * Cold [Flow] observing all properties that have been marked as verified.
     */
    val verifiedProperties: Flow<List<Property>> = propertyDao.getVerifiedProperties()

    // --- REMOTE API SYNC & CACHE METHODS ---

    /**
     * Fetches property listings from the Lodgist website backend API and caches them in Room.
     *
     * @param city Optional city filter (e.g. "Owerri", "Lagos", "Port Harcourt").
     * @param verified Optional verification flag filter.
     * @return [Result] containing the updated list of properties on success, or exception on error.
     */
    suspend fun fetchAndCachePropertiesFromApi(
        city: String? = null,
        verified: Boolean? = null
    ): Result<List<Property>> = withContext(ioDispatcher) {
        val service = apiService ?: return@withContext Result.failure(
            IllegalStateException("LodgistApiService is not configured.")
        )

        try {
            // First attempt live Railway endpoint GET /api/listings
            val listingsResponse = try {
                service.getListings()
            } catch (_: Exception) {
                null
            }

            if (listingsResponse != null && listingsResponse.isSuccessful && listingsResponse.body() != null) {
                val dtos = listingsResponse.body()!!.data
                val properties = dtos.map { it.toProperty() }
                if (properties.isNotEmpty()) {
                    propertyDao.insertProperties(properties)
                }
                return@withContext Result.success(properties)
            }

            // Fallback to legacy getProperties if /api/listings was unavailable
            val response = service.getProperties(city = city, verified = verified)
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body() ?: emptyList()
                val properties = dtos.map { it.toProperty() }
                if (properties.isNotEmpty()) {
                    propertyDao.insertProperties(properties)
                }
                Result.success(properties)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submits an advertiser lead for the Owerri campus pilot.
     * Note: Form submission is a lead, not an automatically published home.
     */
    suspend fun submitAdvertiserLead(
        name: String,
        phone: String,
        email: String,
        message: String,
        authority: String = "AUTHORIZED",
        photoPermission: Boolean = true,
        availability: String = "AVAILABLE_NOW"
    ): Result<Boolean> = withContext(ioDispatcher) {
        val service = apiService ?: return@withContext Result.failure(
            IllegalStateException("LodgistApiService is not configured.")
        )
        try {
            val payload = mapOf(
                "name" to name,
                "phone" to phone,
                "email" to email,
                "message" to message,
                "authority" to authority,
                "photoPermission" to photoPermission.toString(),
                "availability" to availability,
                "isTest" to "true"
            )
            val response = service.submitAdvertiseLead(payload)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches details of a specific property from the remote API and updates local Room cache.
     *
     * @param id The unique identifier of the property.
     * @return [Result] with the fetched and cached [Property], or error.
     */
    suspend fun fetchAndCachePropertyByIdFromApi(id: String): Result<Property?> = withContext(ioDispatcher) {
        val service = apiService ?: return@withContext Result.failure(
            IllegalStateException("LodgistApiService is not configured.")
        )

        try {
            val response = service.getPropertyById(id)
            if (response.isSuccessful && response.body() != null) {
                val property = response.body()!!.toProperty()
                propertyDao.insertProperty(property)
                Result.success(property)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submits a newly created or edited property to the Lodgist backend, and updates the local cache.
     *
     * @param property The [Property] entity to upload.
     */
    suspend fun submitPropertyListing(property: Property): Result<Property> = withContext(ioDispatcher) {
        val service = apiService ?: run {
            // If remote service is unavailable, cache locally as fallback
            propertyDao.insertProperty(property)
            return@withContext Result.success(property)
        }

        try {
            val dto = PropertyDto.fromProperty(property)
            val response = service.createPropertyListing(dto)
            if (response.isSuccessful && response.body() != null) {
                val created = response.body()!!.toProperty()
                propertyDao.insertProperty(created)
                Result.success(created)
            } else {
                // Fallback to local cache even if remote upload returns an error
                propertyDao.insertProperty(property)
                Result.success(property)
            }
        } catch (e: Exception) {
            // Store locally offline
            propertyDao.insertProperty(property)
            Result.success(property)
        }
    }

    // --- LOCAL ROOM DATABASE OPERATIONS ---

    /**
     * Queries a cached property by its unique ID.
     *
     * @param id The unique identifier of the property.
     * @return The [Property] entity if found in the cache, or null otherwise.
     */
    suspend fun getPropertyById(id: String): Property? {
        return withContext(ioDispatcher) {
            propertyDao.getPropertyById(id)
        }
    }

    /**
     * Observes a single property by its ID as a reactive stream.
     *
     * @param id The unique identifier of the property.
     */
    fun observePropertyById(id: String): Flow<Property?> = flow {
        emit(propertyDao.getPropertyById(id))
    }.flowOn(ioDispatcher)

    /**
     * Caches a single property listing into the local Room database.
     * Replaces any existing record with the same ID.
     *
     * @param property The [Property] entity to insert or update.
     */
    suspend fun insertProperty(property: Property) {
        withContext(ioDispatcher) {
            propertyDao.insertProperty(property)
        }
    }

    /**
     * Caches a batch of property listings into the local Room database.
     * Replaces existing records on conflict.
     *
     * @param properties The list of [Property] entities to insert.
     */
    suspend fun insertProperties(properties: List<Property>) {
        withContext(ioDispatcher) {
            propertyDao.insertProperties(properties)
        }
    }

    /**
     * Updates an existing cached property in the database.
     *
     * @param property The [Property] with updated fields.
     */
    suspend fun updateProperty(property: Property) {
        withContext(ioDispatcher) {
            propertyDao.updateProperty(property)
        }
    }

    /**
     * Removes a specific property from the local cache.
     *
     * @param property The [Property] entity to delete.
     */
    suspend fun deleteProperty(property: Property) {
        withContext(ioDispatcher) {
            propertyDao.deleteProperty(property)
        }
    }

    /**
     * Removes a cached property by its unique ID.
     *
     * @param id The ID of the property to delete.
     */
    suspend fun deletePropertyById(id: String) {
        withContext(ioDispatcher) {
            propertyDao.deletePropertyById(id)
        }
    }

    /**
     * Clears all cached property records from the database table.
     */
    suspend fun clearCache() {
        withContext(ioDispatcher) {
            propertyDao.clearCache()
        }
    }

    /**
     * Retrieves the total count of properties currently cached.
     *
     * @return The number of cached properties.
     */
    suspend fun getCachedCount(): Int {
        return withContext(ioDispatcher) {
            propertyDao.getCachedCount()
        }
    }
}
