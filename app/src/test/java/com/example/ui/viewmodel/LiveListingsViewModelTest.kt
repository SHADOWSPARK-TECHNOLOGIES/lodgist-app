package com.example.ui.viewmodel

import com.example.data.remote.PublicListingDetailResponse
import com.example.data.remote.PublicListingDto
import com.example.data.remote.PublicListingsResponse
import com.example.data.remote.api.LodgistApiService
import com.example.data.repository.PublicListingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

private class FakeApi(
    private val answer: () -> Response<PublicListingsResponse> = { error("no list answer") },
    private val detailAnswer: (Long) -> Response<PublicListingDetailResponse> = { error("no detail answer") }
) : LodgistApiService {
    override suspend fun getPublicListings(limit: Int) = answer()
    override suspend fun getListingDetail(id: Long) = detailAnswer(id)
}

@OptIn(ExperimentalCoroutinesApi::class)
class LiveListingsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(answer: () -> Response<PublicListingsResponse>) =
        LiveListingsViewModel(PublicListingsRepository(FakeApi(answer)))

    @Test
    fun `starts loading then shows content`() = runTest {
        val vm = viewModel { Response.success(PublicListingsResponse(true, listOf(PublicListingDto(id = 1)))) }
        val state = vm.uiState.value
        assertTrue("expected Content but was $state", state is LiveListingsUiState.Content)
        assertEquals(1, (state as LiveListingsUiState.Content).homes.size)
    }

    @Test
    fun `empty catalogue is not an error`() = runTest {
        val vm = viewModel { Response.success(PublicListingsResponse(true, emptyList())) }
        assertEquals(LiveListingsUiState.Empty, vm.uiState.value)
    }

    @Test
    fun `offline is its own state`() = runTest {
        val vm = viewModel { throw java.io.IOException("offline") }
        assertEquals(LiveListingsUiState.Offline, vm.uiState.value)
    }

    @Test
    fun `unauthorized is its own state`() = runTest {
        val vm = viewModel {
            Response.error(401, "{}".toResponseBody("application/json".toMediaType()))
        }
        assertEquals(LiveListingsUiState.Unauthorized, vm.uiState.value)
    }

    @Test
    fun `failure carries the code`() = runTest {
        val vm = viewModel {
            Response.error(502, "{}".toResponseBody("application/json".toMediaType()))
        }
        assertEquals(LiveListingsUiState.Failed(502), vm.uiState.value)
    }

    @Test
    fun `retry recovers from offline to content`() = runTest {
        var offline = true
        val vm = viewModel {
            if (offline) throw java.io.IOException("offline")
            Response.success(PublicListingsResponse(true, listOf(PublicListingDto(id = 5))))
        }

        assertEquals(LiveListingsUiState.Offline, vm.uiState.value)

        offline = false
        vm.refresh()

        val state = vm.uiState.value
        assertTrue("expected Content but was $state", state is LiveListingsUiState.Content)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ListingDetailViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(detail: (Long) -> Response<PublicListingDetailResponse>) =
        LiveListingsViewModel(PublicListingsRepository(FakeApi(detailAnswer = detail)))

    @Test
    fun `no detail is open until asked`() = runTest {
        val vm = viewModel { Response.success(PublicListingDetailResponse(true, PublicListingDto(id = 1))) }
        assertEquals(null, vm.detailState.value)
    }

    @Test
    fun `opening a home loads it, closing returns to the list`() = runTest {
        val vm = viewModel { id ->
            Response.success(PublicListingDetailResponse(true, PublicListingDto(id = id, title = "Home")))
        }

        vm.openDetail(41)
        val state = vm.detailState.value
        assertTrue("expected Content but was $state", state is ListingDetailUiState.Content)
        assertEquals(41L, (state as ListingDetailUiState.Content).home.id)

        vm.closeDetail()
        assertEquals(null, vm.detailState.value)
    }

    @Test
    fun `a 404 shows NotFound rather than a failure`() = runTest {
        val vm = viewModel {
            Response.error(404, "{}".toResponseBody("application/json".toMediaType()))
        }
        vm.openDetail(999999)
        assertEquals(ListingDetailUiState.NotFound, vm.detailState.value)
    }

    @Test
    fun `offline detail is its own state`() = runTest {
        val vm = viewModel { throw java.io.IOException("offline") }
        vm.openDetail(41)
        assertEquals(ListingDetailUiState.Offline, vm.detailState.value)
    }

    @Test
    fun `retry re-requests the same home`() = runTest {
        var offline = true
        var calls = 0
        val vm = viewModel { id ->
            calls += 1
            if (offline) throw java.io.IOException("offline")
            Response.success(PublicListingDetailResponse(true, PublicListingDto(id = id)))
        }

        vm.openDetail(41)
        assertEquals(ListingDetailUiState.Offline, vm.detailState.value)

        offline = false
        vm.retryDetail()

        assertTrue(vm.detailState.value is ListingDetailUiState.Content)
        assertEquals(2, calls)
    }
}
