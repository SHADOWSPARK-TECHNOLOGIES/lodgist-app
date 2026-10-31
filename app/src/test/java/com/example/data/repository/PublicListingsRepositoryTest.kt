package com.example.data.repository

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
    private val answer: () -> Response<PublicListingsResponse>
) : LodgistApiService {
    override suspend fun getPublicListings(limit: Int): Response<PublicListingsResponse> = answer()




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
