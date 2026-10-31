package com.example.data.remote.api

import com.example.data.remote.PublicListingDetailResponse
import com.example.data.remote.PublicListingsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The endpoints this client may call.
 *
 * It declares exactly one route because the Lodgist API serves exactly one
 * public browse route. Earlier revisions also declared `api/v1/properties*`
 * for list/detail/featured/create; that API has never served those paths, so
 * they answered 404 and any screen built on them could only fail.
 *
 * Adding an endpoint here is a contract decision: confirm the route exists in
 * the backend before declaring it.
 */
interface LodgistApiService {

    /**
     * Public browse. Owned by the Lodgist API. An empty `data` array means no
     * home is published right now — it is a real answer, not an error.
     */
    @GET("api/listings")
    suspend fun getPublicListings(
        @Query("limit") limit: Int = 12
    ): Response<PublicListingsResponse>

    /**
     * Single published home. The API answers 404 for a listing that does not
     * exist *or* is not publicly eligible, deliberately collapsing both so a
     * stranger cannot probe unpublished homes.
     */
    @GET("api/listings/{id}")
    suspend fun getListingDetail(
        @Path("id") id: Long
    ): Response<PublicListingDetailResponse>
}
