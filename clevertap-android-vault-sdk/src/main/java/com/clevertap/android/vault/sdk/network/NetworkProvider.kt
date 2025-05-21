package com.clevertap.android.vault.sdk.network

import com.clevertap.android.vault.sdk.api.AuthApi
import com.clevertap.android.vault.sdk.api.TokenizationApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Provides network-related services for the Vault SDK.
 *
 * This class is responsible for creating and configuring Retrofit instances
 * for both tokenization and authentication operations. It encapsulates the
 * network configuration details such as timeouts and base URLs.
 *
 * @property apiUrl The base URL for tokenization API endpoints
 * @property authUrl The base URL for authentication API endpoints
 *
 * @constructor Creates a NetworkProvider with specified API and auth URLs
 */
class NetworkProvider(
    private val apiUrl: String,
    private val authUrl: String
) {
    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 15L // TODO final value
        private const val READ_TIMEOUT_SECONDS = 30L // TODO final value
        private const val WRITE_TIMEOUT_SECONDS = 30L // TODO final value
    }

    /**
     * Lazily initialized OkHttpClient with configured timeouts
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Lazily initialized Retrofit instance for tokenization operations
     */
    private val tokenizationRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Lazily initialized Retrofit instance for authentication operations
     */
    private val authRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(authUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Gets the API interface for authentication operations
     *
     * @return The AuthApi interface
     */
    fun getAuthApi(): AuthApi {
        return authRetrofit.create(AuthApi::class.java)
    }

    /**
     * Gets the API interface for tokenization operations
     *
     * @return The TokenizationApi interface
     */
    fun getTokenizationApi(): TokenizationApi {
        return tokenizationRetrofit.create(TokenizationApi::class.java)
    }

}
