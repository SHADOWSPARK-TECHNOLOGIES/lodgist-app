package com.example.data.repository

import com.example.data.remote.PublicListingDetailResponse
import com.example.data.remote.PublicListingDto
import com.example.data.remote.PublicListingsResponse
import com.example.data.remote.api.LodgistApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Fake service that answers `GET /api/listings` however the test needs. */
private class FakeApi(
    private val answer: () -> Response<PublicListingsResponse> = { error("no list answer") },
    private val detailAnswer: (Long) -> Response<PublicListingDetailResponse> = { error("no detail answer") }
) : LodgistApiService {
    override suspend fun getPublicListings(limit: Int): Response<PublicListingsResponse> = answer()
    override suspend fun getListingDetail(id: Long): Response<PublicListingDetailResponse> = detailAnswer(id)
}

private fun home(id: Long) = PublicListingDto(id = id, title = "Home $id", price = 250000.0)

private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

private val CONTRACT_FIXTURE = """
{
  "success": true,
  "data": [
    {
      "id": 41,
      "propertyId": 7,
      "ownerId": 3,
      "title": "Self contained in Nekede",
      "description": "Close to the poly gate.",
      "price": 250000,
      "currency": "NGN",
      "category": "RESIDENTIAL",
      "status": "VERIFIED",
      "isFeatured": false,
      "views": 12,
      "isFlaggedForFraud": false,
      "riskState": "CLEAR",
      "truthIndex": 61,
      "reviewStatus": "APPROVED",
      "evidenceVerified": true,
      "verifiedAt": "2026-09-30T10:00:00.000Z",
      "totalMoveInCost": { "base_rent": 250000, "agency_fee": 25000, "legal_fee": 25000, "caution_fee": 25000, "total": 325000, "currency": "NGN" },
      "images": [ { "id": 1, "url": "https://cdn.example/1.jpg", "altText": "Room", "isPrimary": true, "order": 0 } ],
      "property": { "id": 7, "city": "Owerri", "state": "Imo", "lga": "Nekede", "bedrooms": 2, "bathrooms": 1, "propertyType": "Apartment" },
      "owner": { "id": 3, "name": "Ada", "truthIndex": 40, "isVerified": true },
      "reviewLabel": "Lodgist-reviewed",
      "verificationLabel": "Verified by Lodgist",
      "sourceLabel": "Direct submission",
      "actions": { "enquiry": true, "viewing": true }
    }
  ],
  "pagination": { "total": 1, "page": 1, "pages": 1, "totalPages": 1, "limit": 12, "pageSize": 12 }
}
""".trimIndent()

class PublicListingsRepositoryTest {

    private fun repository(answer: () -> Response<PublicListingsResponse>) =
        PublicListingsRepository(FakeApi(answer))

    @Test
    fun `parses the real contract fixture`() {
        val parsed = moshi.adapter(PublicListingsResponse::class.java).fromJson(CONTRACT_FIXTURE)!!
        assertEquals(true, parsed.success)
        assertEquals(1, parsed.data.size)
        val home = parsed.data.first()
        assertEquals("Verified by Lodgist", home.verificationLabel)
        assertEquals(true, home.evidenceVerified)
        assertEquals("Nekede", home.property?.lga)
        assertEquals(true, home.actions?.viewing)
        assertEquals(12, parsed.pagination?.limit)
    }

    @Test
    fun `loaded when the api returns published homes`() = kotlinx.coroutines.test.runTest {
        val result = repository {
            Response.success(PublicListingsResponse(true, listOf(home(1), home(2))))
        }.load()

        assertTrue(result is PublicListingsResult.Loaded)
        assertEquals(2, (result as PublicListingsResult.Loaded).homes.size)
    }

    @Test
    fun `empty when the api answers successfully with nothing published`() =
        kotlinx.coroutines.test.runTest {
            val result = repository { Response.success(PublicListingsResponse(true, emptyList())) }.load()
            assertEquals(PublicListingsResult.Empty, result)
        }

    @Test
    fun `offline on IO failure, never empty`() = kotlinx.coroutines.test.runTest {
        val result = repository { throw IOException("no route to host") }.load()
        assertEquals(PublicListingsResult.Offline, result)
    }

    @Test
    fun `offline on timeout`() = kotlinx.coroutines.test.runTest {
        val result = repository { throw SocketTimeoutException("timeout") }.load()
        assertEquals(PublicListingsResult.Offline, result)
    }

    @Test
    fun `unauthorized on 401`() = kotlinx.coroutines.test.runTest {
        val result = repository {
            Response.error(401, "{}".toResponseBody("application/json".toMediaType()))
        }.load()
        assertEquals(PublicListingsResult.Unauthorized, result)
    }

    @Test
    fun `unauthorized on 403`() = kotlinx.coroutines.test.runTest {
        val result = repository {
            Response.error(403, "{}".toResponseBody("application/json".toMediaType()))
        }.load()
        assertEquals(PublicListingsResult.Unauthorized, result)
    }

    @Test
    fun `failed carries the http code for a server error`() = kotlinx.coroutines.test.runTest {
        val result = repository {
            Response.error(500, "{}".toResponseBody("application/json".toMediaType()))
        }.load()
        assertEquals(PublicListingsResult.Failed(500), result)
    }

    @Test
    fun `success false is a failure, not an empty catalogue`() = kotlinx.coroutines.test.runTest {
        val result = repository { Response.success(PublicListingsResponse(false, emptyList())) }.load()
        assertTrue(result is PublicListingsResult.Failed)
    }

    @Test
    fun `entries without an id are dropped rather than rendered blank`() =
        kotlinx.coroutines.test.runTest {
            val result = repository {
                Response.success(PublicListingsResponse(true, listOf(PublicListingDto(id = null), home(9))))
            }.load()

            assertTrue(result is PublicListingsResult.Loaded)
            assertEquals(listOf(9L), (result as PublicListingsResult.Loaded).homes.map { it.id })
        }

    @Test
    fun `empty body is a failure`() = kotlinx.coroutines.test.runTest {
        val result = repository { Response.success(null) }.load()
        assertTrue(result is PublicListingsResult.Failed)
    }
}

private val DETAIL_FIXTURE = """
{
  "success": true,
  "data": {
    "id": 41,
    "title": "Self contained in Nekede",
    "description": "Close to the poly gate.",
    "price": 250000,
    "currency": "NGN",
    "status": "VERIFIED",
    "evidenceVerified": true,
    "reviewStatus": "APPROVED",
    "reviewLabel": "Lodgist-reviewed",
    "verificationLabel": "Verified by Lodgist",
    "sourceLabel": "Direct submission",
    "reviewCount": 3,
    "averageRating": 4.5,
    "images": [],
    "property": { "id": 7, "city": "Owerri", "state": "Imo", "lga": "Nekede", "bedrooms": 2, "bathrooms": 1, "propertyType": "Apartment", "size": 40 },
    "owner": { "id": 3, "name": "Ada", "truthIndex": 40, "isVerified": true },
    "approxLocation": { "lat": 5.43, "lng": 7.03, "radius_m": 500 },
    "actions": { "enquiry": true, "viewing": true }
  }
}
""".trimIndent()

class ListingDetailRepositoryTest {

    private fun repository(detail: (Long) -> retrofit2.Response<com.example.data.remote.PublicListingDetailResponse>) =
        PublicListingsRepository(FakeApi(detailAnswer = detail))

    @Test
    fun `parses the detail contract fixture including approx location`() {
        val parsed = moshi.adapter(com.example.data.remote.PublicListingDetailResponse::class.java)
            .fromJson(DETAIL_FIXTURE)!!
        val home = parsed.data!!
        assertEquals("Verified by Lodgist", home.verificationLabel)
        assertEquals(500, home.approxLocation?.radius_m)
        assertEquals("Ada", home.owner?.name)
        assertEquals("Lodgist-reviewed", home.reviewLabel)
        assertEquals(3, home.reviewCount)
    }

    @Test
    fun `loaded when the api returns one home`() = kotlinx.coroutines.test.runTest {
        val result = repository { id ->
            Response.success(
                com.example.data.remote.PublicListingDetailResponse(
                    success = true,
                    data = PublicListingDto(id = id, title = "Home")
                )
            )
        }.loadDetail(41)

        assertTrue(result is ListingDetailResult.Loaded)
        assertEquals(41L, (result as ListingDetailResult.Loaded).home.id)
    }

    @Test
    fun `404 is NotFound, not a generic failure`() = kotlinx.coroutines.test.runTest {
        val result = repository {
            Response.error(404, """{"error":"Listing not found"}""".toResponseBody("application/json".toMediaType()))
        }.loadDetail(999999)
        assertEquals(ListingDetailResult.NotFound, result)
    }

    @Test
    fun `400 for an invalid id is a failure, not NotFound`() = kotlinx.coroutines.test.runTest {
        val result = repository {
            Response.error(400, """{"error":"Invalid listing ID"}""".toResponseBody("application/json".toMediaType()))
        }.loadDetail(1)
        assertEquals(ListingDetailResult.Failed(400), result)
    }

    @Test
    fun `offline on io failure`() = kotlinx.coroutines.test.runTest {
        val result = repository { throw IOException("offline") }.loadDetail(41)
        assertEquals(ListingDetailResult.Offline, result)
    }

    @Test
    fun `unauthorized on 401`() = kotlinx.coroutines.test.runTest {
        val result = repository {
            Response.error(401, "{}".toResponseBody("application/json".toMediaType()))
        }.loadDetail(41)
        assertEquals(ListingDetailResult.Unauthorized, result)
    }

    @Test
    fun `success false is a failure`() = kotlinx.coroutines.test.runTest {
        val result = repository {
            Response.success(com.example.data.remote.PublicListingDetailResponse(success = false))
        }.loadDetail(41)
        assertTrue(result is ListingDetailResult.Failed)
    }

    @Test
    fun `missing data is a failure`() = kotlinx.coroutines.test.runTest {
        val result = repository {
            Response.success(com.example.data.remote.PublicListingDetailResponse(success = true, data = null))
        }.loadDetail(41)
        assertTrue(result is ListingDetailResult.Failed)
    }
}
