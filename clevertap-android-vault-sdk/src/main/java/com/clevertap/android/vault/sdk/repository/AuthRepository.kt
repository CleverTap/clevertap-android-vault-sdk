package com.clevertap.android.vault.sdk.repository

/**
 * Repository interface for managing authentication with the Vault API
 */
interface AuthRepository {
    /**
     * Gets a valid authentication token, refreshing if necessary
     *
     * @return A valid access token
     */
    suspend fun getAccessToken(): String

    /**
     * Requests a new access token from the authentication server
     *
     * @return A new access token
     */
    suspend fun refreshAccessToken(): String

    /**
     * Checks if the current token is valid
     *
     * @return True if the token is valid, false otherwise
     */
    fun isTokenValid(): Boolean
}
