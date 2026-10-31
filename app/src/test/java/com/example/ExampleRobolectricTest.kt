package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.EscrowPaymentEntity
import com.example.data.model.Property
import com.example.data.model.PropertyEntity
import com.example.ui.components.formatNaira
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Lodgist", appName)
  }

  @Test
  fun testNairaFormatting() {
    val formatted = formatNaira(1200000L)
    assertEquals("₦1,200,000", formatted)
  }

  @Test
  fun testPropertyTotalMoveInCost() {
    val prop = PropertyEntity(
      id = "test_01",
      title = "Test Flat",
      propertyType = "2 Bedroom Flat",
      city = "Owerri",
      neighborhood = "Nekede",
      state = "Imo State",
      address = "Test Street",
      annualRent = 500000L,
      cautionDeposit = 50000L,
      serviceCharge = 30000L,
      bedrooms = 2,
      bathrooms = 2,
      truthScore = 95,
      verificationStatus = "VERIFIED",
      titleDocumentChecked = true,
      landlordIdentityChecked = true,
      physicalInspectionPassed = true,
      disputeFree = true,
      fairPriceIndexed = true,
      description = "Test description",
      amenities = "Solar,Borehole",
      landlordName = "Owner",
      landlordPhone = "08012345678",
      drawableResName = "building"
    )

    assertEquals(580000L, prop.totalMoveInCost)
    assertTrue(prop.amenityList().contains("Solar"))
    assertTrue(prop.amenityList().contains("Borehole"))
    assertTrue(prop.verified)
    assertEquals("Nekede, Owerri", prop.location)
  }

  @Test
  fun testEscrowPaymentTracking() {
    val inspectionToken = EscrowPaymentEntity(
      id = 1L,
      propertyId = "lodg_ow_01",
      propertyTitle = "Executive 2-Bedroom Serviced Flat",
      payerName = "Chidi Okafor",
      payerPhone = "08012345678",
      amount = 3500L,
      purpose = "INSPECTION_TOKEN",
      paymentMethod = "DEBIT_CARD",
      transactionRef = "LDG-INS-0001",
      status = "HELD_IN_ESCROW"
    )

    assertEquals(3500L, inspectionToken.amount)
    assertEquals("HELD_IN_ESCROW", inspectionToken.status)
    assertEquals("LDG-INS-0001", inspectionToken.transactionRef)
  }

  @Test
  fun testPropertyListingDataClassAndRoomCache() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val property = Property(
      id = "prop_nekede_01",
      title = "Modern Self-Contain Student Apartment",
      price = 380000.0,
      location = "Nekede, Owerri, Imo State",
      description = "Newly built self-contain apartment with solar inverter, prepaid meter, and verified borehole water.",
      verified = true
    )

    assertEquals("prop_nekede_01", property.id)
    assertEquals("Modern Self-Contain Student Apartment", property.title)
    assertEquals(380000.0, property.price, 0.01)
    assertEquals("Nekede, Owerri, Imo State", property.location)
    assertEquals("Newly built self-contain apartment with solar inverter, prepaid meter, and verified borehole water.", property.description)
    assertTrue(property.verified)

    // Test Room cache insertion and retrieval
    val dao = inMemoryDb.propertyListingDao()
    dao.insertProperty(property)

    val cached = dao.getPropertyById("prop_nekede_01")
    assertNotNull(cached)
    assertEquals(property.id, cached?.id)
    assertEquals(property.title, cached?.title)
    assertEquals(property.price, cached?.price ?: 0.0, 0.01)
    assertEquals(property.location, cached?.location)
    assertEquals(property.description, cached?.description)
    assertEquals(true, cached?.verified)

    // Test UPDATE
    val updatedProperty = property.copy(price = 350000.0, title = "Discounted Student Apartment")
    dao.updateProperty(updatedProperty)
    val afterUpdate = dao.getPropertyById("prop_nekede_01")
    assertEquals(350000.0, afterUpdate?.price ?: 0.0, 0.01)
    assertEquals("Discounted Student Apartment", afterUpdate?.title)

    // Test COUNT
    assertEquals(1, dao.getCachedCount())

    // Test DELETE
    dao.deleteProperty(updatedProperty)
    val afterDelete = dao.getPropertyById("prop_nekede_01")
    assertEquals(null, afterDelete)
    assertEquals(0, dao.getCachedCount())

    // Test BATCH INSERT and CLEAR
    dao.insertProperties(listOf(
      property.copy(id = "prop_1"),
      property.copy(id = "prop_2")
    ))
    assertEquals(2, dao.getCachedCount())
    dao.clearCache()
    assertEquals(0, dao.getCachedCount())

    inMemoryDb.close()
  }

  @Test
  fun testPropertyRepository() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val repository = com.example.data.repository.PropertyRepository(
      propertyDao = inMemoryDb.propertyListingDao(),
      ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
    )

    val prop = Property(
      id = "repo_prop_01",
      title = "Furnished 1 Bedroom Flat",
      price = 750000.0,
      location = "World Bank Area, Owerri",
      description = "Full solar inverter, borehole water, 24/7 security guard.",
      verified = true
    )

    // Test Repository Insert
    repository.insertProperty(prop)
    assertEquals(1, repository.getCachedCount())

    // Test Repository GetById
    val fetched = repository.getPropertyById("repo_prop_01")
    assertNotNull(fetched)
    assertEquals("Furnished 1 Bedroom Flat", fetched?.title)
    assertEquals(750000.0, fetched?.price ?: 0.0, 0.01)
    assertEquals(true, fetched?.verified)

    // Test Repository Update
    repository.updateProperty(prop.copy(price = 700000.0))
    val updated = repository.getPropertyById("repo_prop_01")
    assertEquals(700000.0, updated?.price ?: 0.0, 0.01)

    // Test Repository Delete
    repository.deletePropertyById("repo_prop_01")
    assertEquals(0, repository.getCachedCount())

    inMemoryDb.close()
  }

  @Test
  fun testDatabaseModuleProvider() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.di.DatabaseModule.resetForTests()

    val database = com.example.di.DatabaseModule.provideAppDatabase(context)
    assertNotNull(database)

    val propertyListingDao = com.example.di.DatabaseModule.providePropertyListingDao(database)
    assertNotNull(propertyListingDao)

    val propertyDao = com.example.di.DatabaseModule.providePropertyDao(database)
    assertNotNull(propertyDao)

    val inspectionDao = com.example.di.DatabaseModule.provideInspectionDao(database)
    assertNotNull(inspectionDao)

    val landlordListingDao = com.example.di.DatabaseModule.provideLandlordListingDao(database)
    assertNotNull(landlordListingDao)

    val escrowDao = com.example.di.DatabaseModule.provideEscrowDao(database)
    assertNotNull(escrowDao)

    val propertyRepo = com.example.di.DatabaseModule.providePropertyRepository(database)
    assertNotNull(propertyRepo)

    val lodgistRepo = com.example.di.DatabaseModule.provideLodgistRepository(database)
    assertNotNull(lodgistRepo)

    com.example.di.DatabaseModule.resetForTests()
  }
}
