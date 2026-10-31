package com.example.data.repository

import com.example.data.local.PropertyListingDao
import com.example.data.model.Property
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository class that abstracts data operations between the local Room cache,
 * the DAO, and the ViewModel.
 *
 * This repository is local-only by design. It used to synchronise with the
 * backend over `api/v1/properties*`; those routes do not exist, and the one
 * write path silently reported success after caching locally. Remote reads
 * belong to [PublicListingsRepository], which talks to the route that is
 * actually served.
 *
 * @param propertyDao The Room Data Access Object for local caching.
 * @param ioDispatcher Coroutine dispatcher for disk I/O (defaults to [Dispatchers.IO]).
 */
class PropertyRepository(
    private val propertyDao: PropertyListingDao,
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
