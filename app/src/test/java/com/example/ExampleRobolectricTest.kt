package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.LodgistDatabase
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
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, LodgistDatabase::class.java)
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
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, LodgistDatabase::class.java)
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

  @Test
  fun testPropertyViewModel() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val repository = com.example.data.repository.PropertyRepository(
      propertyDao = inMemoryDb.propertyListingDao(),
      ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
    )

    val viewModel = com.example.ui.viewmodel.PropertyViewModel(repository)

    val prop1 = Property(
      id = "vm_prop_01",
      title = "2-Bedroom Luxury Suite",
      price = 1500000.0,
      location = "Lekki, Lagos",
      description = "Ocean view with 24/7 security",
      verified = true
    )

    // Synchronously insert into repository
    repository.insertProperty(prop1)

    // Select property via ViewModel
    viewModel.selectProperty(prop1)
    assertEquals("vm_prop_01", viewModel.selectedProperty.value?.id)
    assertEquals("2-Bedroom Luxury Suite", viewModel.selectedProperty.value?.title)

    // Search query filtering test
    viewModel.onSearchQueryChanged("Lekki")
    assertEquals("Lekki", viewModel.searchQuery.value)

    // Location filter test
    viewModel.onLocationFilterChanged("Lagos")
    assertEquals("Lagos", viewModel.locationFilter.value)

    // Price range filter test
    viewModel.onPriceRangeChanged(500000.0, 2500000.0)
    assertEquals(500000.0, viewModel.minPriceFilter.value, 0.001)
    assertEquals(2500000.0, viewModel.maxPriceFilter.value, 0.001)

    // Number of rooms filter test
    viewModel.onRoomsFilterChanged(2)
    assertEquals(2, viewModel.roomsFilter.value)

    // Property category filter test (houses vs flats)
    viewModel.onPropertyCategoryChanged("Houses")
    assertEquals("Houses", viewModel.propertyCategoryFilter.value)
    viewModel.onPropertyCategoryChanged("Flats")
    assertEquals("Flats", viewModel.propertyCategoryFilter.value)

    // Convenience methods
    viewModel.filterByKeywords("self contain")
    assertEquals("self contain", viewModel.searchQuery.value)

    viewModel.filterByLocation("Owerri")
    assertEquals("Owerri", viewModel.locationFilter.value)

    // Clear search
    viewModel.clearSearch()
    assertEquals("", viewModel.searchQuery.value)
    assertEquals("", viewModel.locationFilter.value)
    assertEquals(0.0, viewModel.minPriceFilter.value, 0.001)
    assertEquals(Double.MAX_VALUE, viewModel.maxPriceFilter.value, 0.001)
    assertEquals(0, viewModel.roomsFilter.value)
    assertEquals("All", viewModel.propertyCategoryFilter.value)

    // Verification toggle
    viewModel.toggleVerifiedOnly(true)
    assertTrue(viewModel.verifiedOnlyFilter.value)

    viewModel.toggleVerifiedOnly(false)
    assertFalse(viewModel.verifiedOnlyFilter.value)

    // Clear selection
    viewModel.selectProperty(null)
    assertEquals(null, viewModel.selectedProperty.value)

    inMemoryDb.close()
  }

  @Test
  fun testPropertyDtoAndRetrofitApiIntegration() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    // 1. Verify PropertyDto mapping
    val dto = com.example.data.remote.model.PropertyDto(
      id = "api_prop_01",
      title = "Smart 2-Bed Serviced Apartment",
      price = 1200000.0,
      location = "Ikenegbu Layout, Owerri",
      description = "Fast fiber optic internet, 24-hour solar backup power, verified title.",
      verified = true,
      propertyType = "2 Bedroom Flat",
      bedrooms = 2,
      bathrooms = 2,
      city = "Owerri",
      neighborhood = "Ikenegbu"
    )

    val mappedProperty = dto.toProperty()
    assertEquals("api_prop_01", mappedProperty.id)
    assertEquals("Smart 2-Bed Serviced Apartment", mappedProperty.title)
    assertEquals(1200000.0, mappedProperty.price, 0.01)
    assertEquals("Ikenegbu Layout, Owerri", mappedProperty.location)
    assertTrue(mappedProperty.verified)

    // 2. Mock LodgistApiService
    val mockApiService = object : com.example.data.remote.api.LodgistApiService {
      override suspend fun getProperties(
        city: String?,
        verified: Boolean?,
        limit: Int
      ): retrofit2.Response<List<com.example.data.remote.model.PropertyDto>> {
        return retrofit2.Response.success(listOf(dto))
      }

      override suspend fun getPropertiesWrapped(
        city: String?
      ): retrofit2.Response<com.example.data.remote.model.PropertyListResponseDto> {
        return retrofit2.Response.success(
          com.example.data.remote.model.PropertyListResponseDto(
            status = "success",
            total = 1,
            data = listOf(dto)
          )
        )
      }

      override suspend fun getPropertyById(
        propertyId: String
      ): retrofit2.Response<com.example.data.remote.model.PropertyDto> {
        return retrofit2.Response.success(dto)
      }

      override suspend fun getFeaturedProperties(): retrofit2.Response<List<com.example.data.remote.model.PropertyDto>> {
        return retrofit2.Response.success(listOf(dto))
      }

      override suspend fun getListings(): retrofit2.Response<com.example.data.remote.model.PropertyListResponseDto> {
        return retrofit2.Response.success(
          com.example.data.remote.model.PropertyListResponseDto(
            success = true,
            data = listOf(dto)
          )
        )
      }

      override suspend fun submitAdvertiseLead(
        lead: Map<String, String>
      ): retrofit2.Response<Map<String, Any>> {
        return retrofit2.Response.success(mapOf("status" to "received", "leadId" to "lead_123"))
      }

      override suspend fun createPropertyListing(
        property: com.example.data.remote.model.PropertyDto
      ): retrofit2.Response<com.example.data.remote.model.PropertyDto> {
        return retrofit2.Response.success(property)
      }
    }

    // 3. Inject mock API service into PropertyRepository
    val repository = com.example.data.repository.PropertyRepository(
      propertyDao = inMemoryDb.propertyListingDao(),
      apiService = mockApiService,
      ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
    )

    // Initially Room cache is empty
    assertEquals(0, repository.getCachedCount())

    // 4. Fetch from API and assert automatic Room caching
    val result = repository.fetchAndCachePropertiesFromApi(city = "Owerri", verified = true)
    assertTrue(result.isSuccess)
    val fetchedList = result.getOrNull()
    assertNotNull(fetchedList)
    assertEquals(1, fetchedList?.size)
    assertEquals("api_prop_01", fetchedList?.first()?.id)

    // Verify Room database now has 1 cached item
    assertEquals(1, repository.getCachedCount())
    val fromLocalDb = repository.getPropertyById("api_prop_01")
    assertNotNull(fromLocalDb)
    assertEquals("Smart 2-Bed Serviced Apartment", fromLocalDb?.title)
    assertTrue(fromLocalDb?.verified == true)

    inMemoryDb.close()
  }

  @Test
  fun testSpatialLabCampusDistanceCalculationAndDisclaimers() {
    // Test Haversine formula calculation between Nekede and Federal Poly Nekede
    val (nekedeLat, nekedeLon) = com.example.ui.components.getNeighborhoodCoordinates("Nekede", "Owerri")
    val polyCampus = com.example.ui.components.OWERRI_CAMPUSES.first { it.id == "poly" }
    val distToPoly = com.example.ui.components.calculateDistanceKm(
      nekedeLat, nekedeLon, polyCampus.latitude, polyCampus.longitude
    )
    assertTrue("Distance within campus neighborhood should be close (< 2km)", distToPoly < 2.0)

    // Test distance from Nekede to FUTO (Ihiagwa)
    val futoCampus = com.example.ui.components.OWERRI_CAMPUSES.first { it.id == "futo" }
    val distToFuto = com.example.ui.components.calculateDistanceKm(
      nekedeLat, nekedeLon, futoCampus.latitude, futoCampus.longitude
    )
    assertTrue("Distance from Nekede to FUTO should be positive (> 2km)", distToFuto > 2.0)

    // Test Owerri campus list integrity
    assertEquals(3, com.example.ui.components.OWERRI_CAMPUSES.size)
    assertTrue(com.example.ui.components.OWERRI_CAMPUSES.any { it.id == "futo" })
    assertTrue(com.example.ui.components.OWERRI_CAMPUSES.any { it.id == "imsu" })
    assertTrue(com.example.ui.components.OWERRI_CAMPUSES.any { it.id == "poly" })
  }

  @Test
  fun testEmptyLiveInventoryHandling() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val emptyApiService = object : com.example.data.remote.api.LodgistApiService {
      override suspend fun getListings(): retrofit2.Response<com.example.data.remote.model.PropertyListResponseDto> {
        return retrofit2.Response.success(
          com.example.data.remote.model.PropertyListResponseDto(
            success = true,
            data = emptyList()
          )
        )
      }

      override suspend fun submitAdvertiseLead(
        lead: Map<String, String>
      ): retrofit2.Response<Map<String, Any>> = retrofit2.Response.success(emptyMap())

      override suspend fun getProperties(
        city: String?, verified: Boolean?, limit: Int
      ): retrofit2.Response<List<com.example.data.remote.model.PropertyDto>> =
        retrofit2.Response.success(emptyList())

      override suspend fun getPropertiesWrapped(
        city: String?
      ): retrofit2.Response<com.example.data.remote.model.PropertyListResponseDto> =
        retrofit2.Response.success(com.example.data.remote.model.PropertyListResponseDto(data = emptyList()))

      override suspend fun getPropertyById(
        propertyId: String
      ): retrofit2.Response<com.example.data.remote.model.PropertyDto> =
        retrofit2.Response.error(404, okhttp3.ResponseBody.create(null, "Not found"))

      override suspend fun getFeaturedProperties(): retrofit2.Response<List<com.example.data.remote.model.PropertyDto>> =
        retrofit2.Response.success(emptyList())

      override suspend fun createPropertyListing(
        property: com.example.data.remote.model.PropertyDto
      ): retrofit2.Response<com.example.data.remote.model.PropertyDto> =
        retrofit2.Response.success(property)
    }

    val repository = com.example.data.repository.PropertyRepository(
      propertyDao = inMemoryDb.propertyListingDao(),
      apiService = emptyApiService,
      ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
    )

    val result = repository.fetchAndCachePropertiesFromApi(city = "Owerri")
    assertTrue(result.isSuccess)
    val properties = result.getOrNull()
    assertNotNull(properties)
    assertEquals("Should return empty list without inventing fake inventory", 0, properties?.size)
    assertEquals(0, repository.getCachedCount())

    inMemoryDb.close()
  }

  @Test
  fun testInspectionEligibilityEnforcedAtRepositoryLayer() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, LodgistDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val repository = com.example.data.repository.LodgistRepository(
      propertyDao = inMemoryDb.propertyDao(),
      inspectionDao = inMemoryDb.inspectionDao(),
      landlordListingDao = inMemoryDb.landlordListingDao(),
      escrowDao = inMemoryDb.escrowDao()
    )

    // 1. Insert an unverified (PENDING) property
    val pendingProperty = PropertyEntity(
      id = "prop_pending_01",
      title = "Unverified 2-Bed",
      propertyType = "2 Bedroom Flat",
      city = "Owerri",
      neighborhood = "Nekede",
      state = "Imo",
      address = "Near Poly Gate",
      annualRent = 450000L,
      cautionDeposit = 50000L,
      serviceCharge = 0L,
      bedrooms = 2,
      bathrooms = 1,
      truthScore = 40,
      verificationStatus = "PENDING",
      titleDocumentChecked = false,
      landlordIdentityChecked = false,
      physicalInspectionPassed = false,
      disputeFree = false,
      fairPriceIndexed = false,
      description = "Pending verification audit",
      amenities = "Water",
      landlordName = "Mr. Test",
      landlordPhone = "08012345678",
      drawableResName = "building"
    )
    inMemoryDb.propertyDao().insertProperty(pendingProperty)

    // 2. Insert a fully verified property
    val verifiedProperty = pendingProperty.copy(
      id = "prop_verified_01",
      title = "Audited 2-Bed Flat",
      verificationStatus = "VERIFIED",
      truthScore = 95
    )
    inMemoryDb.propertyDao().insertProperty(verifiedProperty)

    // 3. Attempt booking inspection on the pending unverified property -> MUST FAIL
    val bookingOnPending = com.example.data.model.InspectionBookingEntity(
      propertyId = "prop_pending_01",
      propertyTitle = "Unverified 2-Bed",
      propertyLocation = "Nekede, Owerri",
      clientName = "Chidi Student",
      clientPhone = "08033333333",
      preferredDate = "Tomorrow",
      preferredTimeSlot = "Morning",
      notes = "Test unverified booking"
    )
    val pendingBookingResult = repository.bookInspection(bookingOnPending)
    assertTrue("Inspection on unverified property must be rejected by backend/repository", pendingBookingResult.isFailure)

    // 4. Booking inspection on verified property -> MUST SUCCEED
    val bookingOnVerified = bookingOnPending.copy(propertyId = "prop_verified_01")
    val verifiedBookingResult = repository.bookInspection(bookingOnVerified)
    assertTrue("Inspection on verified property should succeed", verifiedBookingResult.isSuccess)

    inMemoryDb.close()
  }

  @Test
  fun testMoveInCostCalculationIntegrity() {
    val property = PropertyEntity(
      id = "cost_test_01",
      title = "Transparent Cost Home",
      propertyType = "Self-Contain",
      city = "Owerri",
      neighborhood = "Ihiagwa",
      state = "Imo",
      address = "FUTO Road",
      annualRent = 300000L,
      cautionDeposit = 30000L,
      serviceCharge = 20000L,
      bedrooms = 1,
      bathrooms = 1,
      truthScore = 90,
      verificationStatus = "VERIFIED",
      titleDocumentChecked = true,
      landlordIdentityChecked = true,
      physicalInspectionPassed = true,
      disputeFree = true,
      fairPriceIndexed = true,
      description = "Honest fees",
      amenities = "Water, Prepaid Meter",
      landlordName = "Engr. Obi",
      landlordPhone = "08099887766",
      drawableResName = "living"
    )

    // Total move in cost must exactly match annualRent + cautionDeposit + serviceCharge with zero hidden fees
    assertEquals(350000L, property.totalMoveInCost)
  }

  @Test
  fun testSearchPropertyFilteringByLocationPriceAndRooms() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val repository = com.example.data.repository.PropertyRepository(
      propertyDao = inMemoryDb.propertyListingDao(),
      ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
    )
    val viewModel = com.example.ui.viewmodel.PropertyViewModel(repository)

    val flat1 = Property(
      id = "flat_ow_01",
      title = "2-Bedroom Luxury Serviced Flat",
      price = 1200000.0,
      location = "Ikenegbu, Owerri",
      description = "Spacious modern flat with solar inverter and borehole water.",
      verified = true,
      bedrooms = 2,
      bathrooms = 2,
      propertyType = "2 Bedroom Flat"
    )

    val house1 = Property(
      id = "house_ow_01",
      title = "4-Bedroom Detached Duplex",
      price = 4500000.0,
      location = "New Owerri, Owerri",
      description = "Executive duplex with gate security and private compound.",
      verified = true,
      bedrooms = 4,
      bathrooms = 4,
      propertyType = "House / Duplex"
    )

    val flat2 = Property(
      id = "flat_lag_01",
      title = "1-Bedroom Studio Flat",
      price = 700000.0,
      location = "Yaba, Lagos",
      description = "Student studio flat near tech hub.",
      verified = false,
      bedrooms = 1,
      bathrooms = 1,
      propertyType = "1 Bedroom Flat"
    )

    repository.insertProperty(flat1)
    repository.insertProperty(house1)
    repository.insertProperty(flat2)

    // 1. Verify initial state loads properties
    val allCached = repository.allCachedProperties
    assertNotNull(allCached)

    // 2. Filter by Category: Houses
    viewModel.onPropertyCategoryChanged("Houses")
    assertEquals("Houses", viewModel.propertyCategoryFilter.value)

    // 3. Filter by Location: Owerri
    viewModel.onLocationFilterChanged("Owerri")
    assertEquals("Owerri", viewModel.locationFilter.value)

    // 4. Filter by Price Range: ₦1M to ₦5M
    viewModel.onPriceRangeChanged(1000000.0, 5000000.0)
    assertEquals(1000000.0, viewModel.minPriceFilter.value, 0.001)
    assertEquals(5000000.0, viewModel.maxPriceFilter.value, 0.001)

    // 5. Filter by Number of Rooms: 4
    viewModel.onRoomsFilterChanged(4)
    assertEquals(4, viewModel.roomsFilter.value)

    // 6. Test toPropertyEntity conversion
    val convertedEntity = flat1.toPropertyEntity()
    assertEquals("flat_ow_01", convertedEntity.id)
    assertEquals("2-Bedroom Luxury Serviced Flat", convertedEntity.title)
    assertEquals(1200000L, convertedEntity.annualRent)
    assertEquals(2, convertedEntity.bedrooms)
    assertTrue(convertedEntity.verified)

    // 7. Reset filters and verify restoration
    viewModel.clearSearch()
    assertEquals("", viewModel.searchQuery.value)
    assertEquals("", viewModel.locationFilter.value)
    assertEquals(0, viewModel.roomsFilter.value)
    assertEquals("All", viewModel.propertyCategoryFilter.value)
    assertEquals(0.0, viewModel.minPriceFilter.value, 0.001)
    assertEquals(Double.MAX_VALUE, viewModel.maxPriceFilter.value, 0.001)

    inMemoryDb.close()
  }

  @Test
  fun testLodgistViewModelSearchAndFilterScreen() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, LodgistDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val repository = com.example.data.repository.LodgistRepository(
      propertyDao = inMemoryDb.propertyDao(),
      inspectionDao = inMemoryDb.inspectionDao(),
      landlordListingDao = inMemoryDb.landlordListingDao(),
      escrowDao = inMemoryDb.escrowDao()
    )

    // Insert test properties: Houses and Flats in different locations with different room counts and rents
    val flatOwerri = PropertyEntity(
      id = "prop_flat_ow",
      title = "Modern 2-Bed Serviced Flat",
      propertyType = "2 Bedroom Flat",
      city = "Owerri",
      neighborhood = "Ikenegbu",
      state = "Imo",
      address = "Plot 12 Ikenegbu Layout",
      annualRent = 1200000L,
      cautionDeposit = 100000L,
      serviceCharge = 50000L,
      bedrooms = 2,
      bathrooms = 2,
      truthScore = 92,
      verificationStatus = "VERIFIED",
      titleDocumentChecked = true,
      landlordIdentityChecked = true,
      physicalInspectionPassed = true,
      disputeFree = true,
      fairPriceIndexed = true,
      description = "Verified serviced flat in Owerri",
      amenities = "Water, Security, Inverter",
      landlordName = "Barrister Okeke",
      landlordPhone = "08031234567",
      drawableResName = "building"
    )

    val houseLagos = PropertyEntity(
      id = "prop_house_lg",
      title = "Luxury 4-Bedroom Duplex House",
      propertyType = "Duplex House",
      city = "Lagos",
      neighborhood = "Lekki Phase 1",
      state = "Lagos",
      address = "Admiralty Way, Lekki",
      annualRent = 6500000L,
      cautionDeposit = 500000L,
      serviceCharge = 200000L,
      bedrooms = 4,
      bathrooms = 4,
      truthScore = 88,
      verificationStatus = "VERIFIED",
      titleDocumentChecked = true,
      landlordIdentityChecked = true,
      physicalInspectionPassed = true,
      disputeFree = true,
      fairPriceIndexed = true,
      description = "Detached family house with compound in Lagos",
      amenities = "Swimming Pool, 24/7 Security",
      landlordName = "Chief Adeleke",
      landlordPhone = "08029876543",
      drawableResName = "home"
    )

    val selfContainNekede = PropertyEntity(
      id = "prop_sc_nek",
      title = "Student Self-Contain Studio",
      propertyType = "Self-Contain",
      city = "Nekede",
      neighborhood = "Poly Back Gate",
      state = "Imo",
      address = "Nekede Poly Road",
      annualRent = 350000L,
      cautionDeposit = 30000L,
      serviceCharge = 10000L,
      bedrooms = 1,
      bathrooms = 1,
      truthScore = 85,
      verificationStatus = "VERIFIED",
      titleDocumentChecked = true,
      landlordIdentityChecked = true,
      physicalInspectionPassed = true,
      disputeFree = true,
      fairPriceIndexed = true,
      description = "Compact self-contained room near campus",
      amenities = "Borehole Water",
      landlordName = "Madam Ngozi",
      landlordPhone = "08051239876",
      drawableResName = "apartment"
    )

    inMemoryDb.propertyDao().insertProperties(listOf(flatOwerri, houseLagos, selfContainNekede))

    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.LodgistViewModel(app, repository)

    // 1. Verify navigation to SEARCH_PROPERTIES
    assertEquals(com.example.ui.viewmodel.ScreenDestination.EXPLORE, viewModel.currentScreen.value)
    viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.SEARCH_PROPERTIES)
    assertEquals(com.example.ui.viewmodel.ScreenDestination.SEARCH_PROPERTIES, viewModel.currentScreen.value)

    // 2. Filter by Location: "Owerri"
    viewModel.updateCityFilter("Owerri")
    assertEquals("Owerri", viewModel.filterState.value.selectedCity)

    // 3. Filter by Category: "Houses"
    viewModel.updateCityFilter("All")
    viewModel.updateCategoryFilter("Houses")
    assertEquals("Houses", viewModel.filterState.value.selectedCategory)

    // 4. Filter by Category: "Flats"
    viewModel.updateCategoryFilter("Flats")
    assertEquals("Flats", viewModel.filterState.value.selectedCategory)

    // 5. Filter by Category: "Self-Contain"
    viewModel.updateCategoryFilter("Self-Contain")
    assertEquals("Self-Contain", viewModel.filterState.value.selectedCategory)

    // 6. Filter by Price Range (₦300k to ₦1.5M)
    viewModel.updateCategoryFilter("All")
    viewModel.updatePriceRange(300000L, 1500000L)
    assertEquals(300000L, viewModel.filterState.value.minRentNaira)
    assertEquals(1500000L, viewModel.filterState.value.maxRentNaira)

    // 7. Filter by Number of Rooms: 2 rooms
    viewModel.updatePriceRange(0L, 10000000L)
    viewModel.updateRoomsFilter(2)
    assertEquals(2, viewModel.filterState.value.selectedRooms)

    // 8. Filter by Number of Rooms: 4+ rooms
    viewModel.updateRoomsFilter(4)
    assertEquals(4, viewModel.filterState.value.selectedRooms)

    // 9. Reset filters
    viewModel.resetFilters()
    assertEquals("All", viewModel.filterState.value.selectedCity)
    assertEquals("All", viewModel.filterState.value.selectedCategory)
    assertEquals(0L, viewModel.filterState.value.minRentNaira)
    assertEquals(10000000L, viewModel.filterState.value.maxRentNaira)
    assertEquals(0, viewModel.filterState.value.selectedRooms)

    // 10. Navigate back to Explore
    viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.EXPLORE)
    assertEquals(com.example.ui.viewmodel.ScreenDestination.EXPLORE, viewModel.currentScreen.value)

    inMemoryDb.close()
  }

  @Test
  fun testBackStackNavigation() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = Room.inMemoryDatabaseBuilder(context, LodgistDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repository = com.example.data.repository.LodgistRepository(
      propertyDao = inMemoryDb.propertyDao(),
      inspectionDao = inMemoryDb.inspectionDao(),
      landlordListingDao = inMemoryDb.landlordListingDao(),
      escrowDao = inMemoryDb.escrowDao()
    )
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.LodgistViewModel(app, repository)

    // Initial screen is EXPLORE
    assertEquals(com.example.ui.viewmodel.ScreenDestination.EXPLORE, viewModel.currentScreen.value)

    // Navigate to SEARCH_PROPERTIES
    viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.SEARCH_PROPERTIES)
    assertEquals(com.example.ui.viewmodel.ScreenDestination.SEARCH_PROPERTIES, viewModel.currentScreen.value)

    // Select a property: goes to PROPERTY_DETAIL
    viewModel.selectProperty("lodg_ow_01")
    assertEquals(com.example.ui.viewmodel.ScreenDestination.PROPERTY_DETAIL, viewModel.currentScreen.value)
    assertEquals("lodg_ow_01", viewModel.selectedPropertyId.value)

    // Back from property detail: returns to SEARCH_PROPERTIES
    viewModel.clearSelectedProperty()
    assertEquals(com.example.ui.viewmodel.ScreenDestination.SEARCH_PROPERTIES, viewModel.currentScreen.value)
    assertEquals(null, viewModel.selectedPropertyId.value)

    // Pop back from SEARCH_PROPERTIES: returns to EXPLORE
    val popped = viewModel.popBack()
    assertTrue(popped)
    assertEquals(com.example.ui.viewmodel.ScreenDestination.EXPLORE, viewModel.currentScreen.value)

    // Navigate to INSPECTIONS then pop back
    viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.INSPECTIONS)
    assertEquals(com.example.ui.viewmodel.ScreenDestination.INSPECTIONS, viewModel.currentScreen.value)
    viewModel.popBack()
    assertEquals(com.example.ui.viewmodel.ScreenDestination.EXPLORE, viewModel.currentScreen.value)

    // Navigate to ESCROW_VAULT then pop back
    viewModel.navigateTo(com.example.ui.viewmodel.ScreenDestination.ESCROW_VAULT)
    assertEquals(com.example.ui.viewmodel.ScreenDestination.ESCROW_VAULT, viewModel.currentScreen.value)
    viewModel.popBack()
    assertEquals(com.example.ui.viewmodel.ScreenDestination.EXPLORE, viewModel.currentScreen.value)

    inMemoryDb.close()
  }
}
