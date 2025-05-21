package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.model.AuthTokenResponse
import com.clevertap.android.vault.sdk.network.NetworkProvider
import com.clevertap.android.vault.sdk.util.VaultLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/**
 * Implementation of the [AuthRepository] interface that manages authentication with the Vault API.
 *
 * This class handles OAuth2 client credentials authentication flow, including:
 * - Retrieving access tokens from the authentication server
 * - Caching valid tokens to avoid unnecessary network requests
 * - Refreshing expired tokens automatically
 * - Thread-safe token management using mutex locks
 *
 * The implementation maintains a single access token in memory with its expiration time,
 * and handles refreshing it before it expires (with a 30-second safety buffer).
 *
 * @property networkProvider Provider for network-related services, used to create the authentication API client
 * @property clientId The OAuth2 client ID used for authentication
 * @property clientSecret The OAuth2 client secret used for authentication
 * @property logger Logger utility for recording authentication events and errors
 *
 * @constructor Creates an AuthRepository implementation with the specified dependencies
 */
class AuthRepositoryImpl(
    private val networkProvider: NetworkProvider,
    private val clientId: String,
    private val clientSecret: String,
    private val logger: VaultLogger
) : AuthRepository {
    /**
     * Mutex lock for thread-safe access token operations.
     * Ensures that only one token refresh operation can occur at a time,
     * preventing multiple simultaneous network requests when multiple threads
     * need a valid token.
     */
    private val mutex = Mutex()
    private var accessToken: String? = null
    private var accessTokenExpiration: Long = 0 // Timestamp when the token expires

    /**
     * Gets a valid authentication token, refreshing it if necessary.
     *
     * This method ensures a valid token is available by:
     * 1. Checking if the current token is still valid (with a 30-second buffer)
     * 2. Using the existing token if it's valid
     * 3. Requesting a new token if none exists or the current one is expired/near expiration
     *
     * The method is thread-safe, using a mutex to prevent multiple simultaneous
     * token refresh operations when called from different threads.
     *
     * @return A valid access token to use in API requests
     * @throws Exception If token acquisition fails
     */
    override suspend fun getAccessToken(): String {
        mutex.withLock {
            val currentTime = System.currentTimeMillis()

            // Check if token is still valid (with a 30-second buffer)
            if (accessToken != null && currentTime < accessTokenExpiration - TimeUnit.SECONDS.toMillis(
                    30
                )
            ) {
                logger.d("Using existing auth token")
                return accessToken!!
            }

            logger.d("Requesting new auth token")
            return refreshAccessToken()
        }
    }

    /**
     * Requests a new access token from the authentication server.
     *
     * This method performs the actual network request to obtain a fresh OAuth2 token
     * using the client credentials flow. It:
     * 1. Creates the authentication API client
     * 2. Prepares the request parameters (grant_type, client_id, client_secret)
     * 3. Makes the network request to the token endpoint
     * 4. Processes the response, extracting and storing the new token
     *
     * @return A fresh access token
     * @throws Exception If the network request fails or returns an error response
     */
    override suspend fun refreshAccessToken(): String {
        val authApi = networkProvider.getAuthApi()

        val requestParams = mapOf(
            "grant_type" to "client_credentials",
            "client_id" to clientId,
            "client_secret" to clientSecret
        )

        val response = authApi.getToken(requestParams)

        if (response.isSuccessful && response.body() != null) {
            val tokenResponse = response.body()!!
            processTokenResponse(tokenResponse)
            return accessToken!!
        } else {
            val errorBody = response.errorBody()?.string() ?: "Unknown error"
            logger.e("Authentication failed: ${response.code()} - $errorBody")
            throw Exception("Authentication failed: ${response.code()} - $errorBody")
        }
    }

    /**
     * Processes and stores the token response
     *
     * @param tokenResponse The authentication token response
     */
    private fun processTokenResponse(tokenResponse: AuthTokenResponse) {
        accessToken = tokenResponse.accessToken

        // Calculate expiration time (current time + expiresIn seconds)
        accessTokenExpiration =
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(tokenResponse.expiresIn.toLong())

        logger.d("Auth token refreshed, expires in ${tokenResponse.expiresIn} seconds")
    }

    /**
     * Checks if the current token is valid and not near expiration.
     *
     * This method determines if the current token can be used for API requests by:
     * 1. Checking if a token exists
     * 2. Verifying that it's not expired or about to expire (using a 30-second buffer)
     *
     * The 30-second buffer helps avoid scenarios where a token might expire
     * during a network request.
     *
     * @return True if the token is valid and not near expiration, false otherwise
     */
    override fun isTokenValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        return accessToken != null && currentTime < accessTokenExpiration - TimeUnit.SECONDS.toMillis(
            30
        )
    }
}
