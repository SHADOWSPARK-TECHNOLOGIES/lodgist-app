package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PropertyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties ORDER BY isFeatured DESC, truthScore DESC")
    fun getAllProperties(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE isFavorite = 1 ORDER BY truthScore DESC")
    fun getFavoriteProperties(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE id = :propertyId LIMIT 1")
    fun getPropertyById(propertyId: String): Flow<PropertyEntity?>

    @Query("SELECT * FROM properties WHERE id = :propertyId LIMIT 1")
    suspend fun getPropertyByIdDirect(propertyId: String): PropertyEntity?

    @Query("SELECT * FROM properties WHERE city = :city ORDER BY truthScore DESC")
    fun getPropertiesByCity(city: String): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE isStudentFriendly = 1 ORDER BY truthScore DESC")
    fun getStudentProperties(): Flow<List<PropertyEntity>>

    @Query("SELECT COUNT(*) FROM properties")
    suspend fun getPropertyCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperties(properties: List<PropertyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: PropertyEntity)

    @Update
    suspend fun updateProperty(property: PropertyEntity)

    @androidx.room.Delete
    suspend fun deleteProperty(property: PropertyEntity)

    @Query("DELETE FROM properties WHERE id = :propertyId")
    suspend fun deletePropertyById(propertyId: String)

    @Query("UPDATE properties SET isFavorite = :isFav WHERE id = :propertyId")
    suspend fun updateFavoriteStatus(propertyId: String, isFav: Boolean)
}
