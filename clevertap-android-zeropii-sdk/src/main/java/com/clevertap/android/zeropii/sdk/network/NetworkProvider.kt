package com.clevertap.android.zeropii.sdk.network

import com.clevertap.android.zeropii.sdk.api.TokenizationApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Provides network-related services for the ZeroPii SDK.
 *
 * This class is responsible for creating and configuring Retrofit instances
 * for tokenization operations. It encapsulates the network configuration
 * details such as timeouts and base URLs.
 *
 * @property apiUrl The base URL for tokenization API endpoints
 *
 * @constructor Creates a NetworkProvider with specified API URL
 */
class NetworkProvider(
    private val apiUrl: String
) {
    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 15L
        private const val WRITE_TIMEOUT_SECONDS = 15L
    }

    /**
     * Lazily initialized TokenizationApi instance (created only once)
     */
    internal val tokenizationApi: TokenizationApi by lazy {
        tokenizationRetrofit.create(TokenizationApi::class.java)
    }

    /**
     * Lazily initialized OkHttpClient with configured timeouts
     */
    internal val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Lazily initialized Retrofit instance for tokenization operations
     */
    internal val tokenizationRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(/*gson*/))
            .build()
    }
}
