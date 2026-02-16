package com.clevertap.android.zeropii.sdk.auth

/**
 * Provider interface for supplying access tokens to the ZeroPii SDK.
 *
 * Implement this interface to supply authentication tokens from your
 * application's auth system. The SDK calls [fetchToken] when it needs
 * a new or refreshed token.
 *
 * **Important:** Do not hold Activity/Fragment references in your
 * implementation — use Application-scoped dependencies to avoid leaks.
 *
 * **Contract:** Implementors MUST call exactly one of
 * [AccessTokenCallback.onSuccess] or [AccessTokenCallback.onFailure].
 * Never call both, and never call neither (SDK will hang indefinitely).
 */
interface AccessTokenProvider {
    fun fetchToken(callback: AccessTokenCallback)
}

interface AccessTokenCallback {
    fun onSuccess(tokenInfo: AccessTokenInfo)
    fun onFailure(error: Exception)
}

data class AccessTokenInfo(
    val token: String,
    val expiresInSeconds: Long
)
