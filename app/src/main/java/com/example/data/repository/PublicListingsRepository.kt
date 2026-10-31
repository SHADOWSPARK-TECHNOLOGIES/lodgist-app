package com.example.data.repository

import com.example.data.remote.PublicListingDetailResponse
import com.example.data.remote.PublicListingDto
import com.example.data.remote.PublicListingsResponse
import com.example.data.remote.api.LodgistApiService
import com.example.di.NetworkModule
import java.io.IOException
import retrofit2.Response

/**
 * Outcome of reading `GET /api/listings`.
 *
 * Every failure mode is named separately so the UI can say what actually
 * happened. A network failure and an empty catalogue are different things and
 * must never collapse into the same screen.
 */
sealed interface PublicListingsResult {
    /** Server returned one or more published homes. */
    data class Loaded(val homes: List<PublicListingDto>) : PublicListingsResult

    /** Server answered successfully and published nothing. */
    data object Empty : PublicListingsResult

    /** No usable connection, DNS failure, or timeout. */
    data object Offline : PublicListingsResult

    /** 401/403 — the caller is not allowed to read this resource. */
    data object Unauthorized : PublicListingsResult

    /** Any other non-success response, or a body that did not match the contract. */
    data class Failed(val httpCode: Int) : PublicListingsResult
}

sealed interface ListingDetailResult {
    /** The API returned one published home. */
    data class Loaded(val home: PublicListingDto) : ListingDetailResult

    /**
     * 404. The API collapses "does not exist" and "not publicly eligible" into
     * this one answer on purpose, so the client must not try to tell them apart.
     */
    data object NotFound : ListingDetailResult

    data object Offline : ListingDetailResult
    data object Unauthorized : ListingDetailResult
    data class Failed(val httpCode: Int) : ListingDetailResult
}

/**
 * Reads published homes from the Lodgist API.
 *
 * This is a read-only client of an API owned elsewhere: it adapts the published
 * response and invents nothing. It never decides that a home is verified — the
 * server's `verificationLabel` / `evidenceVerified` are passed through verbatim.
 */
class PublicListingsRepository(
    private val api: LodgistApiService = NetworkModule.provideLodgistApiService()
) {

    suspend fun load(limit: Int = PAGE_SIZE): PublicListingsResult {
        val response: Response<PublicListingsResponse> = try {
            api.getPublicListings(limit = limit)
        } catch (_: IOException) {
            return PublicListingsResult.Offline
        } catch (_: Exception) {
            // Malformed payloads surface here from the converter.
            return PublicListingsResult.Failed(HTTP_UNKNOWN)
        }

        if (response.code() == HTTP_UNAUTHORIZED || response.code() == HTTP_FORBIDDEN) {
            return PublicListingsResult.Unauthorized
        }
        if (!response.isSuccessful) {
            return PublicListingsResult.Failed(response.code())
        }

        val body = response.body() ?: return PublicListingsResult.Failed(response.code())
        if (!body.success) {
            // 200 with success=false is a server-side refusal, not an empty catalogue.
            return PublicListingsResult.Failed(response.code())
        }

        val homes = body.data.filter { it.id != null }
        return if (homes.isEmpty()) PublicListingsResult.Empty
        else PublicListingsResult.Loaded(homes)
    }

    suspend fun loadDetail(id: Long): ListingDetailResult {
        val response: Response<PublicListingDetailResponse> = try {
            api.getListingDetail(id)
        } catch (_: IOException) {
            return ListingDetailResult.Offline
        } catch (_: Exception) {
            return ListingDetailResult.Failed(HTTP_UNKNOWN)
        }

        // 404 is a real answer here: the API hides non-public homes behind it.
        if (response.code() == HTTP_NOT_FOUND) return ListingDetailResult.NotFound
        if (response.code() == HTTP_UNAUTHORIZED || response.code() == HTTP_FORBIDDEN) {
            return ListingDetailResult.Unauthorized
        }
        if (!response.isSuccessful) return ListingDetailResult.Failed(response.code())

        val body = response.body() ?: return ListingDetailResult.Failed(response.code())
        if (!body.success) {
            // 200 with success=false is a refusal, not a missing home.
            return ListingDetailResult.Failed(response.code())
        }
        val home = body.data ?: return ListingDetailResult.Failed(response.code())
        return ListingDetailResult.Loaded(home)
    }

    companion object {
        const val PAGE_SIZE = 12
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_UNKNOWN = -1
    }
}
