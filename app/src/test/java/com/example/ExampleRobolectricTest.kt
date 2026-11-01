package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.EscrowPaymentEntity
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
  fun testDatabaseModuleProvider() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.di.DatabaseModule.resetForTests()

    val database = com.example.di.DatabaseModule.provideAppDatabase(context)
    assertNotNull(database)

    val propertyDao = com.example.di.DatabaseModule.providePropertyDao(database)
    assertNotNull(propertyDao)

    val inspectionDao = com.example.di.DatabaseModule.provideInspectionDao(database)
    assertNotNull(inspectionDao)

    val landlordListingDao = com.example.di.DatabaseModule.provideLandlordListingDao(database)
    assertNotNull(landlordListingDao)

    val escrowDao = com.example.di.DatabaseModule.provideEscrowDao(database)
    assertNotNull(escrowDao)

    val lodgistRepo = com.example.di.DatabaseModule.provideLodgistRepository(database)
    assertNotNull(lodgistRepo)

    com.example.di.DatabaseModule.resetForTests()
  }
}
