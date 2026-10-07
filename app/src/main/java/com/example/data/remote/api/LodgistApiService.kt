package com.example.data.remote.api

import com.example.data.remote.model.PropertyDto
import com.example.data.remote.model.PropertyListResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.data.remote.PublicListingsResponse

/**
 * Retrofit API service interface for communicating with the Lodgist website backend API.
 */
interface LodgistApiService {

    /**
     * Public browse. Owned by the Lodgist API. Empty data means no published homes.
     */
    @GET("api/listings")
    suspend fun getPublicListings(
        @Query("limit") limit: Int = 12
    ): Response<PublicListingsResponse>

    /**
     * Fetches property listings from Lodgist with optional city and verification filters.
     *
     * @param city Optional city filter (e.g., "Owerri", "Lagos", "Abuja").
     * @param verified Optional filter for only verified listings.
     * @param limit Maximum number of listings to return.
     */
    @GET("api/v1/properties")
    suspend fun getProperties(
        @Query("city") city: String? = null,
        @Query("verified") verified: Boolean? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<PropertyDto>>

    /**
     * Fetches wrapped list of properties if the backend uses an envelope response.
     */
    @GET("api/v1/properties/all")
    suspend fun getPropertiesWrapped(
        @Query("city") city: String? = null
    ): Response<PropertyListResponseDto>

    /**
     * Fetches details of a specific property listing by ID.
     *
     * @param propertyId Unique ID of the property.
     */
    @GET("api/v1/properties/{id}")
    suspend fun getPropertyById(
        @Path("id") propertyId: String
    ): Response<PropertyDto>

    /**
     * Fetches curated and verified featured properties.
     */
    @GET("api/v1/properties/featured")
    suspend fun getFeaturedProperties(): Response<List<PropertyDto>>

    /**
     * Submits a new property listing for verification and auditing.
     *
     * @param property Payload of the listing to create.
     */
    @POST("api/v1/properties")
    suspend fun createPropertyListing(
        @Body property: PropertyDto
    ): Response<PropertyDto>
}
