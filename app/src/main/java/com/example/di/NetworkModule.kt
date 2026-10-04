package com.example.di

import com.example.data.remote.api.LodgistApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Dependency Injection Module providing Retrofit client and network services for Lodgist backend API.
 */
object NetworkModule {

    const val DEFAULT_BASE_URL = "https://api.lodgist.ng/"

    @Volatile
    private var apiServiceInstance: LodgistApiService? = null

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provides the singleton instance of [LodgistApiService].
     *
     * @param baseUrl Custom base URL for staging, testing, or mock environments.
     */
    fun provideLodgistApiService(baseUrl: String = DEFAULT_BASE_URL): LodgistApiService {
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(LodgistApiService::class.java)
                .also { apiServiceInstance = it }
        }
    }

    /**
     * Custom setter allowing test suites or environments to inject a mock API service.
     */
    fun setCustomApiServiceForTests(service: LodgistApiService?) {
        apiServiceInstance = service
    }

    /**
     * Resets network singletons.
     */
    fun resetForTests() {
        apiServiceInstance = null
    }
}
