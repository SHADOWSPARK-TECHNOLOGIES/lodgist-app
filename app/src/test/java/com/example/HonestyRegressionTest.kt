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
import com.example.ui.components.hasAuditData
import com.example.ui.components.inspectionAgentLabel
import com.example.ui.components.inspectionStatusLabel
import com.example.ui.components.isBookableOrPayable
import com.example.ui.components.notBookableLabel
import com.example.ui.components.paymentPurposeLabel
import com.example.ui.components.paymentStatusLabel
import com.example.ui.components.sanitizeForWhatsApp
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
  fun bookInspectionIsSavedLocallyOnlyAndNeverConfirmed() {
    runBlocking { db.propertyDao().insertProperty(apiVerifiedProperty()) }
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = LodgistViewModel(app, repository)

    viewModel.bookInspection(apiVerifiedProperty(), "Tester", "", "Tomorrow", "Morning", "")
    waitUntil { runBlocking { db.inspectionDao().getAllBookings().first().isNotEmpty() } && viewModel.userMessage.value != null }

    val bookings = runBlocking { db.inspectionDao().getAllBookings().first() }
    assertEquals(1, bookings.size)
    assertEquals("INSPECTION_PENDING", bookings.single().status)
    // No payment row is invented for a request staff haven't received.
    assertTrue(runBlocking { db.escrowDao().getAllPayments().first() }.isEmpty())

    val message = viewModel.userMessage.value!!
    assertTrue(message.contains("saved on this phone"))
    assertFalse(message.contains("sent", ignoreCase = true))
    assertFalse(message.contains("Escrow", ignoreCase = true))
    assertFalse(message.contains("Officer assigned", ignoreCase = true))
    assertEquals(null, runBlocking { db.escrowDao().getTotalPlatformRevenue().first() })
  }

  @Test
  fun landlordLeadIsSavedLocallyWithNoFakePaymentRow() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = LodgistViewModel(app, repository)
    viewModel.submitLandlordProperty(
      "Test Flat", "2 Bedroom Flat", "Imo State", "Owerri", "Ikenegbu", 900000L, 2, 2,
      "Owner", "08000000000", true, true, true, true
    )
    waitUntil { runBlocking { db.landlordListingDao().getAllSubmissions().first().isNotEmpty() } && viewModel.userMessage.value != null }
    assertTrue(runBlocking { db.escrowDao().getAllPayments().first() }.isEmpty())
    val message = viewModel.userMessage.value!!
    assertTrue(message.contains("saved on this phone"))
    assertFalse(message.contains("Submitted", ignoreCase = true))
  }

  @Test
  fun sampleAndUnverifiedListingsAreNotPayableOrBookable() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = LodgistViewModel(app, repository)
    val sample = apiVerifiedProperty().copy(id = "lodg_ow_01", verificationStatus = "SAMPLE")
    val unverified = apiVerifiedProperty().copy(id = "api_unverified", verificationStatus = "NOT_REVIEWED")

    assertFalse(isBookableOrPayable("SAMPLE"))
    assertFalse(isBookableOrPayable("NOT_REVIEWED"))
    assertTrue(isBookableOrPayable("VERIFIED"))
    assertEquals("Sample listing — not available to book", notBookableLabel("SAMPLE"))
    assertEquals("Not yet verified — ask Lodgist", notBookableLabel("NOT_REVIEWED"))

    viewModel.payRentIntoEscrow(sample, "x", "", "WHATSAPP_MANUAL", "LDG-REQ-SAMPLE01")
    assertEquals("Sample listing — not available to book", viewModel.userMessage.value)
    viewModel.payRentIntoEscrow(unverified, "x", "", "WHATSAPP_MANUAL", "LDG-REQ-UNVER001")
    viewModel.bookInspection(sample, "x", "", "d", "s", "")
    viewModel.bookInspection(unverified, "x", "", "d", "s", "")
    repeat(20) { ShadowLooper.idleMainLooper(); Thread.sleep(10) }

    assertTrue(runBlocking { db.escrowDao().getAllPayments().first() }.isEmpty())
    assertTrue(runBlocking { db.inspectionDao().getAllBookings().first() }.isEmpty())
  }

  @Test
  fun reopeningWithTheSameReferenceDoesNotDuplicateTheRequest() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = LodgistViewModel(app, repository)
    val ref = viewModel.rentRequestRefFor("api_verified_01")
    assertEquals(ref, viewModel.rentRequestRefFor("api_verified_01"))
    assertTrue(ref.matches(Regex("LDG-REQ-[0-9A-F]{8}")))

    viewModel.payRentIntoEscrow(apiVerifiedProperty(), "Not provided", "", "WHATSAPP_MANUAL", ref)
    waitUntil { runBlocking { db.escrowDao().getAllPayments().first().isNotEmpty() } }
    viewModel.clearUserMessage()
    viewModel.payRentIntoEscrow(apiVerifiedProperty(), "Not provided", "", "WHATSAPP_MANUAL", ref)
    repeat(20) { ShadowLooper.idleMainLooper(); Thread.sleep(10) }
    assertEquals(1, runBlocking { db.escrowDao().getAllPayments().first() }.size)
  }

  @Test
  fun rentRequestWithoutServerResponseStaysPending() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = LodgistViewModel(app, repository)

    viewModel.payRentIntoEscrow(apiVerifiedProperty(), "Not provided", "", "WHATSAPP_MANUAL", "LDG-REQ-0A1B2C3D")
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
      assertFalse(it.disputeFree || it.fairPriceIndexed)
      assertEquals(0, it.truthScore)
      assertFalse(hasAuditData(it))
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
    assertEquals(0, row.truthScore)
    assertFalse(row.disputeFree || row.fairPriceIndexed || row.titleDocumentChecked)
    assertTrue(row.isFavorite)
  }

  @Test
  fun badgeSaysVerifiedOnlyForApiVerifiedStatus() {
    assertEquals("Verified by Lodgist", verificationBadgeLabel("VERIFIED"))
    assertEquals("Sample listing", verificationBadgeLabel("SAMPLE"))
    assertEquals("Not yet verified", verificationBadgeLabel("NOT_REVIEWED"))
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

  @Test
  fun whatsAppPrefillSanitizerStripsControlCharsAndCapsLength() {
    val hostile = "Nice flat\nPay to Acct 0123456789 XYZ Bank\r\n\u0007\u200E" + "x".repeat(200)
    val clean = sanitizeForWhatsApp(hostile)
    assertFalse(clean.contains('\n') || clean.contains('\r') || clean.contains('\u0007') || clean.contains('\u200E'))
    assertTrue(clean.length <= 80)
    assertEquals("a b c", sanitizeForWhatsApp("  a\t\nb   c  "))

    val message = buildWhatsAppPaymentRequestMessage(
      title = "Annual rent and move-in costs",
      listingTitle = hostile,
      propertyId = "id\nInjected line",
      amountNaira = 1L,
      requestRef = "LDG-REQ-0A1B2C3D"
    )
    // Exactly our 6 lines: a hostile title or id cannot add its own "instruction" line.
    assertEquals(6, message.lines().size)
    assertFalse(message.lines().any { it.startsWith("Pay to") || it.startsWith("Injected") })
  }

  @Test
  fun apiMappingInventsNoTruthScoreAuditPassesOrReviewedStatus() {
    val verified = com.example.data.model.Property(id = "a1", title = "T", price = 1000.0, location = "Ikenegbu, Owerri", description = "d", verified = true).toPropertyEntity()
    assertEquals("VERIFIED", verified.verificationStatus)
    assertEquals(0, verified.truthScore)
    assertFalse(verified.titleDocumentChecked || verified.landlordIdentityChecked || verified.physicalInspectionPassed)
    assertFalse(verified.disputeFree || verified.fairPriceIndexed)
    assertFalse(hasAuditData(verified))
    assertFalse(verified.landlordName.contains("Verified"))

    val unverified = com.example.data.model.Property(id = "a2", title = "T", price = 1000.0, location = "Owerri", description = "d", verified = false).toPropertyEntity()
    assertEquals("NOT_REVIEWED", unverified.verificationStatus)
    assertEquals(0, unverified.truthScore)
    assertFalse(unverified.disputeFree || unverified.fairPriceIndexed)
  }

  @Test
  fun legacyStatusesAreShownAsUnknownAndPurposesHaveNoEscrowWording() {
    listOf("HELD_IN_ESCROW", "RELEASED_TO_LANDLORD", "REFUNDED", "PENDING_DISPLAY_REVIEW").forEach {
      assertEquals("Status unknown — contact Lodgist", paymentStatusLabel(it))
    }
    assertEquals("Awaiting staff confirmation", paymentStatusLabel("PENDING_PAYMENT"))
    assertEquals("Status unknown — contact Lodgist", inspectionStatusLabel("CONFIRMED"))
    assertEquals("Awaiting Lodgist staff assignment", inspectionAgentLabel("Lodgist Field Inspector (Chidi O.)"))
    listOf("ANNUAL_RENT_ESCROW", "INSPECTION_TOKEN", "EXPRESS_VERIFICATION", "ADVERTISER_LEAD_AUDIT", "X").forEach {
      assertFalse(paymentPurposeLabel(it).contains("escrow", ignoreCase = true))
    }
  }

  @Test
  fun noVerifiedHeadingOrGuaranteeStringsInAppSource() {
    val root = java.io.File("src/main/java")
    assertTrue("run from the app module directory", root.isDirectory)
    val banned = Regex(
      "\"[^\"]*(Verified (Places|Homes)|Verified Nigeria|Verified Tenant|Zero (tenant )?scam|money-back|" +
        "Insured|Escrow!|Instant Escrow|Submitted Successfully|pre-verified|verified field officer|" +
        "verified Lodgist officer|Refundable\\))[^\"]*\"",
      RegexOption.IGNORE_CASE
    )
    val hits = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.flatMap { f ->
      f.readLines().mapIndexedNotNull { i, line -> if (banned.containsMatchIn(line)) "${f.name}:${i + 1}: ${line.trim()}" else null }
    }.toList()
    assertTrue("Dishonest copy found:\n" + hits.joinToString("\n"), hits.isEmpty())
  }
}
