package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.EscrowPaymentEntity
import com.example.data.model.InspectionBookingEntity
import com.example.data.model.LandlordListingEntity
import com.example.data.model.Property
import com.example.data.model.PropertyEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Room database for the application.
 *
 * Houses cached property listings, inspection walkthrough appointments,
 * landlord onboarding submissions, and local payment request records (staff confirm manually).
 */
@Database(
    entities = [
        PropertyEntity::class,
        InspectionBookingEntity::class,
        LandlordListingEntity::class,
        EscrowPaymentEntity::class,
        Property::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun landlordListingDao(): LandlordListingDao
    abstract fun escrowDao(): EscrowDao
    abstract fun propertyListingDao(): PropertyListingDao

    companion object {
        const val DATABASE_NAME = "lodgist_database"

        /** Ids of the built-in sample listings. Sample data is labelled SAMPLE and never VERIFIED. */
        val SAMPLE_PROPERTY_IDS = listOf(
            "lodg_ow_01", "lodg_ow_02", "lodg_lag_01", "lodg_lag_02", "lodg_ph_01", "lodg_abj_01", "lodg_ow_03"
        )

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of [AppDatabase].
         */
        fun getDatabase(context: Context, scope: CoroutineScope? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()

                if (scope != null) {
                    builder.addCallback(AppDatabaseCallback(scope))
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return getDatabase(context)
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.propertyDao(), database.propertyListingDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(
            propertyDao: PropertyDao,
            propertyListingDao: PropertyListingDao? = null
        ) {
            val initialProperties = listOf(
                PropertyEntity(
                    id = "lodg_ow_01",
                    title = "Executive 2-Bedroom Serviced Flat",
                    propertyType = "2 Bedroom Flat",
                    city = "Owerri",
                    neighborhood = "New Owerri (World Bank Area)",
                    state = "Imo State",
                    address = "Plot 14, Ring Road, Off Port Harcourt Road, New Owerri",
                    annualRent = 1200000L,
                    cautionDeposit = 100000L,
                    serviceCharge = 80000L,
                    bedrooms = 2,
                    bathrooms = 2,
                    truthScore = 96,
                    verificationStatus = "SAMPLE",
                    titleDocumentChecked = false,
                    landlordIdentityChecked = false,
                    physicalInspectionPassed = false,
                    disputeFree = true,
                    fairPriceIndexed = true,
                    description = "Sample listing for illustration only. Modern 2-bedroom flat with Italian vitrified floor tiles, POP ceiling, water heater in all bathrooms, dedicated prepaid meter, borehole supply, and 24/7 security outpost.",
                    amenities = "24/7 Solar Backup,Borehole Water,Fenced & Gated,Prepaid Meter,Dedicated Parking,POP Ceiling,Water Heater,Security Guard",
                    landlordName = "Chief Emeka N.",
                    landlordPhone = "+2348032001199",
                    supportWhatsapp = "2349136094315",
                    isFavorite = true,
                    drawableResName = "building",
                    isFeatured = true,
                    isStudentFriendly = false,
                    totalAvailableUnits = 2
                ),
                PropertyEntity(
                    id = "lodg_ow_02",
                    title = "Modern Self-Contain Student / Tech Hub",
                    propertyType = "Self-Contain",
                    city = "Owerri",
                    neighborhood = "Nekede (Polytechnic Junction)",
                    state = "Imo State",
                    address = "8 Poly Back Gate Road, Nekede, Owerri",
                    annualRent = 380000L,
                    cautionDeposit = 30000L,
                    serviceCharge = 25000L,
                    bedrooms = 1,
                    bathrooms = 1,
                    truthScore = 98,
                    verificationStatus = "SAMPLE",
                    titleDocumentChecked = false,
                    landlordIdentityChecked = false,
                    physicalInspectionPassed = false,
                    disputeFree = true,
                    fairPriceIndexed = true,
                    description = "Top-tier student and young professional studio in Nekede. Water pump runs on solar inverter, solid perimeter wall with electric fencing, spacious kitchen with cabinets, and private balcony. Sample listing for illustration only.",
                    amenities = "Solar Inverter for Water/Light,Borehole Water,Prepaid Meter,Electric Fence,Kitchen Cabinets,Tiled Floor,Motorcycle/Car Park",
                    landlordName = "Engr. Patrick O.",
                    landlordPhone = "+2348123456789",
                    supportWhatsapp = "2349136094315",
                    isFavorite = false,
                    drawableResName = "living",
                    isFeatured = true,
                    isStudentFriendly = true,
                    totalAvailableUnits = 3
                ),
                PropertyEntity(
                    id = "lodg_lag_01",
                    title = "Luxury 2-Bed Contemporary Apartment",
                    propertyType = "2 Bedroom Flat",
                    city = "Lagos",
                    neighborhood = "Lekki Phase 1 (Admiralty Way)",
                    state = "Lagos State",
                    address = "Block 4, Freedom Way Corridor, Lekki Phase 1, Lagos",
                    annualRent = 4500000L,
                    cautionDeposit = 350000L,
                    serviceCharge = 500000L,
                    bedrooms = 2,
                    bathrooms = 3,
                    truthScore = 95,
                    verificationStatus = "SAMPLE",
                    titleDocumentChecked = false,
                    landlordIdentityChecked = false,
                    physicalInspectionPassed = false,
                    disputeFree = true,
                    fairPriceIndexed = true,
                    description = "Premium finished 2-bedroom home in prime Lekki Phase 1. Features 24/7 central estate power, high-speed fiber readiness, gymnasium, fitted gas burner, and access card security.",
                    amenities = "24/7 Estate Generator,Treated Water Plant,Swimming Pool,Gym,CCTV & Uniformed Security,Elevator,Fitted Kitchen",
                    landlordName = "Mrs. Folashade Adeyemi",
                    landlordPhone = "+2348023344556",
                    supportWhatsapp = "2349136094315",
                    isFavorite = true,
                    drawableResName = "hero",
                    isFeatured = true,
                    isStudentFriendly = false,
                    totalAvailableUnits = 1
                ),
                PropertyEntity(
                    id = "lodg_lag_02",
                    title = "Spacious Mini-Flat (1 Bedroom)",
                    propertyType = "1 Bedroom Flat",
                    city = "Lagos",
                    neighborhood = "Yaba (Commercial Avenue / Unilag Axis)",
                    state = "Lagos State",
                    address = "22 Commercial Avenue, Sabo Yaba, Lagos",
                    annualRent = 1800000L,
                    cautionDeposit = 150000L,
                    serviceCharge = 120000L,
                    bedrooms = 1,
                    bathrooms = 1,
                    truthScore = 91,
                    verificationStatus = "SAMPLE",
                    titleDocumentChecked = false,
                    landlordIdentityChecked = false,
                    physicalInspectionPassed = false,
                    disputeFree = true,
                    fairPriceIndexed = true,
                    description = "Conveniently situated mini flat in Yaba tech corridor. Cross-ventilation, separate parlor and bedroom, individual prepaid meter, clean borehole tap, gated close with resident security patrol.",
                    amenities = "Borehole Water,Prepaid Meter,Gated Compound,Security Guard,Paved Compound,Wardrobe Included",
                    landlordName = "Barrister Babatunde O.",
                    landlordPhone = "+2348055667788",
                    supportWhatsapp = "2349136094315",
                    isFavorite = false,
                    drawableResName = "living",
                    isFeatured = false,
                    isStudentFriendly = true,
                    totalAvailableUnits = 1
                ),
                PropertyEntity(
                    id = "lodg_ph_01",
                    title = "Classy 3-Bedroom Flat with Penthouse Balcony",
                    propertyType = "3 Bedroom Flat",
                    city = "Port Harcourt",
                    neighborhood = "Peter Odili Road (Trans-Amadi Axis)",
                    state = "Rivers State",
                    address = "Plot 19, Peter Odili Express Link, Port Harcourt",
                    annualRent = 2800000L,
                    cautionDeposit = 200000L,
                    serviceCharge = 180000L,
                    bedrooms = 3,
                    bathrooms = 3,
                    truthScore = 93,
                    verificationStatus = "SAMPLE",
                    titleDocumentChecked = false,
                    landlordIdentityChecked = false,
                    physicalInspectionPassed = false,
                    disputeFree = true,
                    fairPriceIndexed = true,
                    description = "Spacious all-ensuite 3-bedroom flat in serene Peter Odili neighborhood. Sample listing for illustration only.",
                    amenities = "24h Water Storage,Estate Security,Interlocking Pavements,Dedicated Transformer,Balcony,Visitor Parking",
                    landlordName = "Dr. Ken Saro-W.",
                    landlordPhone = "+2348098877665",
                    supportWhatsapp = "2349136094315",
                    isFavorite = false,
                    drawableResName = "building",
                    isFeatured = false,
                    isStudentFriendly = false,
                    totalAvailableUnits = 1
                ),
                PropertyEntity(
                    id = "lodg_abj_01",
                    title = "Executive 2-Bed Terrace Duplex",
                    propertyType = "2 Bedroom Flat",
                    city = "Abuja",
                    neighborhood = "Gwarinpa Estate (1st Avenue)",
                    state = "Abuja FCT",
                    address = "House 12, 1st Avenue Close, Gwarinpa, Abuja",
                    annualRent = 3200000L,
                    cautionDeposit = 250000L,
                    serviceCharge = 200000L,
                    bedrooms = 2,
                    bathrooms = 3,
                    truthScore = 97,
                    verificationStatus = "SAMPLE",
                    titleDocumentChecked = false,
                    landlordIdentityChecked = false,
                    physicalInspectionPassed = false,
                    disputeFree = true,
                    fairPriceIndexed = true,
                    description = "Quiet and secure duplex apartment in Gwarinpa Estate. Sample listing for illustration only. Solar inverter backup, pop designs, fenced with security house.",
                    amenities = "Solar Power System,Borehole Water,Fenced & Gated,Car Port,POP Ceiling",
                    landlordName = "Alhaji Bello M.",
                    landlordPhone = "+2348037788990",
                    supportWhatsapp = "2349136094315",
                    isFavorite = false,
                    drawableResName = "hero",
                    isFeatured = true,
                    isStudentFriendly = false,
                    totalAvailableUnits = 1
                ),
                PropertyEntity(
                    id = "lodg_ow_03",
                    title = "Furnished 1-Bedroom Studio (IMSU / Aladinma)",
                    propertyType = "Studio Apartment",
                    city = "Owerri",
                    neighborhood = "Aladinma Housing Estate (Near IMSU)",
                    state = "Imo State",
                    address = "Road 5, Aladinma Extension, Owerri",
                    annualRent = 650000L,
                    cautionDeposit = 50000L,
                    serviceCharge = 40000L,
                    bedrooms = 1,
                    bathrooms = 1,
                    truthScore = 90,
                    verificationStatus = "SAMPLE",
                    titleDocumentChecked = false,
                    landlordIdentityChecked = false,
                    physicalInspectionPassed = false,
                    disputeFree = true,
                    fairPriceIndexed = true,
                    description = "Well-maintained furnished studio flat close to Imo State University and FMC Owerri. Steady municipal light supplemented by solar panels, treated borehole, clean kitchen, and quiet residential neighborhood.",
                    amenities = "Solar Lighting,Prepaid Meter,Borehole Water,Furnished,Fenced Compound,Night Watchman",
                    landlordName = "Lady Ngozi I.",
                    landlordPhone = "+2348145566778",
                    supportWhatsapp = "2349136094315",
                    isFavorite = false,
                    drawableResName = "living",
                    isFeatured = false,
                    isStudentFriendly = true,
                    totalAvailableUnits = 2
                )
            )
            propertyDao.insertProperties(initialProperties)
            propertyListingDao?.insertProperties(initialProperties.map { it.toProperty() })
        }
    }
}

/**
 * Backward compatibility alias for [AppDatabase].
 */
typealias LodgistDatabase = AppDatabase
