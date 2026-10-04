package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Property
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) interface for the Room database.
 * Handles full CRUD operations for the [Property] entity, including inserting,
 * updating, deleting, and querying cached property listings.
 */
@Dao
interface PropertyListingDao {

    // --- CREATE / INSERT ---

    /**
     * Inserts a single property into the local cache. If it already exists, replaces it.
     * @param property The [Property] entity to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: Property)

    /**
     * Inserts a list of properties into the local cache. Replaces existing entries on conflict.
     * @param properties The list of [Property] entities to cache.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperties(properties: List<Property>)

    // --- READ / QUERY ---

    /**
     * Observes all cached property listings ordered by price.
     * @return A [Flow] emitting the updated list of cached properties.
     */
    @Query("SELECT * FROM cached_properties ORDER BY price ASC")
    fun getAllCachedProperties(): Flow<List<Property>>

    /**
     * Observes all verified property listings from the cache.
     * @return A [Flow] emitting properties where verified flag is true.
     */
    @Query("SELECT * FROM cached_properties WHERE verified = 1 ORDER BY price ASC")
    fun getVerifiedProperties(): Flow<List<Property>>

    /**
     * Queries a specific property by its unique identifier.
     * @param propertyId The unique string ID of the property.
     * @return The [Property] if found, null otherwise.
     */
    @Query("SELECT * FROM cached_properties WHERE id = :propertyId LIMIT 1")
    suspend fun getPropertyById(propertyId: String): Property?

    /**
     * Returns the total count of cached properties.
     * @return The number of items in the cached_properties table.
     */
    @Query("SELECT COUNT(*) FROM cached_properties")
    suspend fun getCachedCount(): Int

    // --- UPDATE ---

    /**
     * Updates an existing property in the local cache.
     * @param property The [Property] entity with updated values.
     */
    @Update
    suspend fun updateProperty(property: Property)

    // --- DELETE ---

    /**
     * Deletes a specific property entity from the cache.
     * @param property The [Property] entity to delete.
     */
    @Delete
    suspend fun deleteProperty(property: Property)

    /**
     * Deletes a property by its unique ID.
     * @param propertyId The ID of the property to delete.
     */
    @Query("DELETE FROM cached_properties WHERE id = :propertyId")
    suspend fun deletePropertyById(propertyId: String)

    /**
     * Clears all cached property listings from the table.
     */
    @Query("DELETE FROM cached_properties")
    suspend fun clearCache()
}
