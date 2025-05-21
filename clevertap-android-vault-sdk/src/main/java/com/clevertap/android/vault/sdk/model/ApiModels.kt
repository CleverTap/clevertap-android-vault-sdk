package com.clevertap.android.vault.sdk.model

/**
 * Authentication token response
 */
data class AuthTokenResponse(
    val accessToken: String,
    val expiresIn: Int,
    val refreshExpiresIn: Int,
    val tokenType: String,
    val scope: String
)

