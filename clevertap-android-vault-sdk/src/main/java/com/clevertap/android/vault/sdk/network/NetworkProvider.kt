package com.clevertap.android.vault.sdk.network

import com.clevertap.android.vault.sdk.api.AuthApi
import com.clevertap.android.vault.sdk.api.TokenizationApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 15L
        private const val WRITE_TIMEOUT_SECONDS = 15L
    }

    val logging : HttpLoggingInterceptor= HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

    /**
     * Lazily initialized AuthApi instance (created only once)
     */
    internal val authApi: AuthApi by lazy {
        authRetrofit.create(AuthApi::class.java)
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
            .addInterceptor(logging)
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

    /**
     * Lazily initialized Retrofit instance for authentication operations
     */
    internal val authRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(authUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

}
