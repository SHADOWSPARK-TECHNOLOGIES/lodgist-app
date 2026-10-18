package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LodgistDatabase
import com.example.data.model.EscrowPaymentEntity
import com.example.data.model.InspectionBookingEntity
import com.example.data.model.PropertyEntity
import com.example.data.repository.LodgistRepository
import com.example.ui.components.buildWhatsAppPaymentRequestMessage
import com.example.ui.components.verificationBadgeLabel
import com.example.ui.viewmodel.LodgistViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * LM-01 honesty regressions: the app must never mark anything confirmed, paid or held in escrow
 * without a server response, and seeded sample data must never be shown as verified.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HonestyRegressionTest {

  private lateinit var db: LodgistDatabase
  private lateinit var repository: LodgistRepository

  private val forbiddenStatuses = setOf("CONFIRMED", "HELD_IN_ESCROW", "PAID", "RELEASED_TO_LANDLORD", "REFUNDED")

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, LodgistDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = LodgistRepository(
      propertyDao = db.propertyDao(),
      inspectionDao = db.inspectionDao(),
      landlordListingDao = db.landlordListingDao(),
      escrowDao = db.escrowDao()
    )
  }

  @After
  fun tearDown() {
    db.close()
  }

  private fun apiVerifiedProperty() = PropertyEntity(
    id = "api_verified_01",
    title = "API Verified Flat",
    propertyType = "2 Bedroom Flat",
    city = "Owerri",
    neighborhood = "Ikenegbu",
    state = "Imo",
    address = "Test address",
    annualRent = 1000000L,
    cautionDeposit = 100000L,
    serviceCharge = 50000L,
    bedrooms = 2,
    bathrooms = 2,
    truthScore = 90,
    verificationStatus = "VERIFIED",
    titleDocumentChecked = true,
    landlordIdentityChecked = true,
    physicalInspectionPassed = true,
    disputeFree = true,
    fairPriceIndexed = true,
    description = "From API",
    amenities = "Water",
    landlordName = "Owner",
    landlordPhone = "",
    drawableResName = "building"
  )

  private fun waitUntil(condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + 5_000
    while (!condition() && System.currentTimeMillis() < deadline) {
      ShadowLooper.idleMainLooper()
      Thread.sleep(10)
    }
    assertTrue("Timed out waiting for ViewModel work", condition())
  }

  @Test
  fun entityDefaultsArePendingNotConfirmedOrEscrow() {
    val booking = InspectionBookingEntity(
      propertyId = "p", propertyTitle = "t", propertyLocation = "l",
      clientName = "c", clientPhone = "", preferredDate = "d", preferredTimeSlot = "s"
    )
    assertEquals("INSPECTION_PENDING", booking.status)
    assertFalse(booking.assignedAgent.contains("Chidi"))

    val payment = EscrowPaymentEntity(
      propertyId = "p", propertyTitle = "t", payerName = "n", payerPhone = "",
      amount = 1L, purpose = "ANNUAL_RENT_ESCROW", paymentMethod = "WHATSAPP_MANUAL", transactionRef = "r"
    )
    assertEquals("PENDING_PAYMENT", payment.status)
  }

  @Test
  fun bookInspectionWithoutServerResponseStaysPending() {
    runBlocking { db.propertyDao().insertProperty(apiVerifiedProperty()) }
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = LodgistViewModel(app, repository)

    viewModel.bookInspection(apiVerifiedProperty(), "Tester", "", "Tomorrow", "Morning", "")
    waitUntil { runBlocking { db.escrowDao().getAllPayments().first().isNotEmpty() } && viewModel.userMessage.value != null }

    val bookings = runBlocking { db.inspectionDao().getAllBookings().first() }
    val payments = runBlocking { db.escrowDao().getAllPayments().first() }
    assertEquals(1, bookings.size)
    assertEquals("INSPECTION_PENDING", bookings.single().status)
    assertTrue(payments.none { it.status in forbiddenStatuses })
    assertEquals("PENDING_PAYMENT", payments.single().status)

    val message = viewModel.userMessage.value!!
    assertFalse(message.contains("Escrow", ignoreCase = true))
    assertFalse(message.contains("Officer assigned", ignoreCase = true))
    assertTrue(message.contains("Lodgist staff"))

    // Pending requests are never counted as revenue.
    assertEquals(null, runBlocking { db.escrowDao().getTotalPlatformRevenue().first() })
  }

  @Test
  fun rentRequestWithoutServerResponseStaysPending() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = LodgistViewModel(app, repository)

    viewModel.payRentIntoEscrow(apiVerifiedProperty(), "Not provided", "", "WHATSAPP_MANUAL", "LDG-REQ-000001")
    waitUntil { runBlocking { db.escrowDao().getAllPayments().first().isNotEmpty() } && viewModel.userMessage.value != null }

    val payment = runBlocking { db.escrowDao().getAllPayments().first() }.single()
    assertEquals("PENDING_PAYMENT", payment.status)
    assertFalse(viewModel.userMessage.value!!.contains("secured", ignoreCase = true))
    assertTrue(viewModel.userMessage.value!!.contains("Awaiting Lodgist staff confirmation"))
  }

  @Test
  fun seedDataIsSampleAndNeverVerified() = runBlocking {
    LodgistDatabase.populateInitialData(db.propertyDao())
    val seeded = db.propertyDao().getAllProperties().first()
    assertEquals(LodgistDatabase.SAMPLE_PROPERTY_IDS.toSet(), seeded.map { it.id }.toSet())
    seeded.forEach {
      assertEquals("SAMPLE", it.verificationStatus)
      assertFalse(it.verified)
      assertFalse(it.titleDocumentChecked || it.landlordIdentityChecked || it.physicalInspectionPassed)
      assertFalse(it.description.contains("verified", ignoreCase = true))
      assertEquals("Sample listing", verificationBadgeLabel(it.verificationStatus))
    }
  }

  @Test
  fun legacyVerifiedSeedRowsAreRelabelledAsSampleKeepingFavourites() = runBlocking {
    val legacy = apiVerifiedProperty().copy(id = "lodg_ow_01", isFavorite = true)
    db.propertyDao().insertProperty(legacy)
    repository.checkAndSeedInitialData()
    val row = db.propertyDao().getPropertyByIdDirect("lodg_ow_01")
    assertNotNull(row)
    assertEquals("SAMPLE", row!!.verificationStatus)
    assertFalse(row.verified)
    assertTrue(row.isFavorite)
  }

  @Test
  fun badgeSaysVerifiedOnlyForApiVerifiedStatus() {
    assertEquals("Verified by Lodgist", verificationBadgeLabel("VERIFIED"))
    assertEquals("Sample listing", verificationBadgeLabel("SAMPLE"))
    assertEquals("Not yet verified", verificationBadgeLabel("LODGIST_REVIEWED"))
    assertEquals("Not yet verified", verificationBadgeLabel("PENDING"))
  }

  @Test
  fun whatsAppHandoffCarriesListingReferenceAndNoPaymentDetails() {
    val message = buildWhatsAppPaymentRequestMessage(
      title = "Annual rent and move-in costs",
      listingTitle = "Sample Flat",
      propertyId = "lodg_ow_01",
      amountNaira = 1380000L,
      requestRef = "LDG-REQ-123456"
    )
    assertTrue(message.contains("lodg_ow_01"))
    assertTrue(message.contains("LDG-REQ-123456"))
    listOf("card", "cvv", "account number", "bank", "escrow", "guarantee", "paid successfully").forEach {
      assertFalse("Message must not contain '$it'", message.contains(it, ignoreCase = true))
    }
  }
}
