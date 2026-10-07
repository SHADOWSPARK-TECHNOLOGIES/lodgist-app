package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicListingsTest {
    @Test
    fun emptyInventoryIsAnEmptyList() {
        val read = PublicListings.read(200, """{"success":true,"data":[],"pagination":{"total":0}}""")
        assertTrue(read is ListingsRead.Loaded)
        assertTrue((read as ListingsRead.Loaded).homes.isEmpty())
    }

    @Test
    fun statusEnumDoesNotMakeAHomeVerified() {
        val json = """
            {"success":true,"data":[{
              "id": 9,
              "title": "Flat",
              "price": 500000,
              "status": "VERIFIED",
              "verificationLabel": "Not verified by Lodgist",
              "property": {"city": "Owerri", "state": "Imo", "lga": "Owerri Municipal", "bedrooms": 2, "bathrooms": 1, "propertyType": "Flat"}
            }]}
        """.trimIndent()
        val home = (PublicListings.read(200, json) as ListingsRead.Loaded).homes.single()
        assertEquals("9", home.id)
        assertEquals("Owerri", home.city)
        assertEquals(500000L, home.annualRent)
        assertFalse(home.verified)
        assertEquals("NOT_VERIFIED", home.verificationStatus)
        assertEquals(0, home.truthScore)
        assertEquals("", home.landlordPhone)
    }

    @Test
    fun evidenceLabelCanMarkVerified() {
        val json = """
            {"success":true,"data":[{"id": 3, "title": "Checked", "verificationLabel": "Verified by Lodgist"}]}
        """.trimIndent()
        val home = (PublicListings.read(200, json) as ListingsRead.Loaded).homes.single()
        assertTrue(home.verified)
    }

    @Test
    fun unknownPropertyAndNetworkFailureStayDistinct() {
        assertTrue(PublicListings.read(404, """{"error":"Listing not found"}""") is ListingsRead.Unknown)
        assertTrue(PublicListings.read(0, null) is ListingsRead.Failed)
        assertTrue(PublicListings.read(500, """{"success":false}""") is ListingsRead.Failed)
    }

    @Test
    fun demonstrationIdsAreTheSeededRows() {
        assertTrue(PublicListings.isDemonstrationId("lodg_ow_01"))
        assertFalse(PublicListings.isDemonstrationId("9"))
    }
}
