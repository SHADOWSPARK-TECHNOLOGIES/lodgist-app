package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mapping guarantee that matters: a home is verified only when the server's
 * own label says so. Nothing else in the payload can promote it.
 */
class PublicListingsTest {

    private fun dto(status: String?, label: String?) = PublicListingDto(
        id = 9,
        title = "Flat",
        price = 500000.0,
        status = status,
        verificationLabel = label
    )

    @Test
    fun statusEnumDoesNotMakeAHomeVerified() {
        val home = dto(status = "VERIFIED", label = NOT_VERIFIED_LABEL).toEntity()
        assertEquals("NOT_VERIFIED", home?.verificationStatus)
        assertFalse(home?.verified == true)
    }

    @Test
    fun evidenceLabelCanMarkVerified() {
        val home = dto(status = "VERIFIED", label = VERIFIED_LABEL).toEntity()
        assertEquals("VERIFIED", home?.verificationStatus)
        assertTrue(home?.verified == true)
    }

    @Test
    fun missingLabelReadsAsNotVerified() {
        val home = dto(status = "VERIFIED", label = null).toEntity()
        assertEquals("NOT_VERIFIED", home?.verificationStatus)
        assertFalse(home?.verified == true)
    }

    @Test
    fun aHomeWithoutAnIdIsDropped() {
        assertEquals(null, PublicListingDto(id = null, title = "Flat").toEntity())
    }
}
