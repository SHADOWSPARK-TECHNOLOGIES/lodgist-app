package com.example.data.repository

import com.example.data.local.EscrowDao
import com.example.data.local.InspectionDao
import com.example.data.local.LandlordListingDao
import com.example.data.local.LodgistDatabase
import com.example.data.local.PropertyDao
import com.example.data.model.EscrowPaymentEntity
import com.example.data.model.InspectionBookingEntity
import com.example.data.model.LandlordListingEntity
import com.example.data.model.PropertyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LodgistRepository(
    private val propertyDao: PropertyDao,
    private val inspectionDao: InspectionDao,
    private val landlordListingDao: LandlordListingDao,
    private val escrowDao: EscrowDao
) {
    val allProperties: Flow<List<PropertyEntity>> = propertyDao.getAllProperties()
    val favoriteProperties: Flow<List<PropertyEntity>> = propertyDao.getFavoriteProperties()
    val inspectionBookings: Flow<List<InspectionBookingEntity>> = inspectionDao.getAllBookings()
    val landlordSubmissions: Flow<List<LandlordListingEntity>> = landlordListingDao.getAllSubmissions()
    val escrowPayments: Flow<List<EscrowPaymentEntity>> = escrowDao.getAllPayments()
    val totalEscrowVolume: Flow<Long?> = escrowDao.getTotalEscrowVolume()
    val totalPlatformRevenue: Flow<Long?> = escrowDao.getTotalPlatformRevenue()

    fun getPropertyById(id: String): Flow<PropertyEntity?> = propertyDao.getPropertyById(id)

    fun getPropertiesByCity(city: String): Flow<List<PropertyEntity>> = propertyDao.getPropertiesByCity(city)

    fun getStudentProperties(): Flow<List<PropertyEntity>> = propertyDao.getStudentProperties()

    suspend fun toggleFavorite(propertyId: String, currentStatus: Boolean) {
        withContext(Dispatchers.IO) {
            propertyDao.updateFavoriteStatus(propertyId, !currentStatus)
        }
    }

    suspend fun bookInspection(booking: InspectionBookingEntity): Result<Long> {
        return withContext(Dispatchers.IO) {
            val property = propertyDao.getPropertyByIdDirect(booking.propertyId)
            if (property != null && property.verificationStatus != "VERIFIED") {
                return@withContext Result.failure(
                    IllegalStateException("Inspection request rejected: Property ${booking.propertyId} is not verified. Viewing requests stay verified-only.")
                )
            }
            val id = inspectionDao.insertBooking(booking)
            Result.success(id)
        }
    }

    suspend fun cancelInspection(bookingId: Long) {
        withContext(Dispatchers.IO) {
            inspectionDao.cancelBooking(bookingId)
        }
    }

    suspend fun submitLandlordProperty(listing: LandlordListingEntity): Long {
        return withContext(Dispatchers.IO) {
            landlordListingDao.insertSubmission(listing)
        }
    }

    suspend fun processEscrowPayment(payment: EscrowPaymentEntity): Long {
        return withContext(Dispatchers.IO) {
            escrowDao.insertPayment(payment)
        }
    }

    suspend fun checkAndSeedInitialData() {
        withContext(Dispatchers.IO) {
            val count = propertyDao.getPropertyCount()
            if (count == 0) {
                LodgistDatabase.populateInitialData(propertyDao)
            } else {
                // Older installs seeded these sample rows as VERIFIED. Relabel them in place
                // (favourites and other columns are preserved). Sample data is never verified.
                propertyDao.markAsSample(LodgistDatabase.SAMPLE_PROPERTY_IDS)
            }
        }
    }
}
