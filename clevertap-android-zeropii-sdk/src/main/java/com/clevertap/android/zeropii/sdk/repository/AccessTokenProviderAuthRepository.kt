package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.auth.AccessTokenCallback
import com.clevertap.android.zeropii.sdk.auth.AccessTokenInfo
import com.clevertap.android.zeropii.sdk.auth.AccessTokenProvider
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of [AuthRepository] that delegates token acquisition
 * to an app-provided [AccessTokenProvider].
 *
 * This class handles:
 * - Caching valid tokens to avoid unnecessary provider calls
 * - Refreshing expired tokens automatically (with a 30-second safety buffer)
 * - Thread-safe token management using mutex locks
 * - Bridging the callback-based [AccessTokenProvider] to coroutines
 *
 * @property tokenProvider The app-provided token provider
 * @property logger Logger utility for recording authentication events and errors
 */
class AccessTokenProviderAuthRepository(
    private val tokenProvider: AccessTokenProvider,
    private val logger: ZeroPiiLogger
) : AuthRepository {

    private val mutex = Mutex()
    private var accessToken: String? = null
    private var accessTokenExpiration: Long = 0

    override suspend fun getAccessToken(): String {
        mutex.withLock {
            if (isTokenValid()) {
                logger.d("Using existing auth token")
                return accessToken!!
            }

            logger.d("Requesting new auth token")
            return refreshAccessToken()
        }
    }

    override suspend fun refreshAccessToken(): String {
        val tokenInfo = fetchTokenFromProvider()
        processTokenInfo(tokenInfo)
        return accessToken!!
    }

    override fun isTokenValid(): Boolean {
        val currentTime = getNowInMillis()
        return accessToken != null && currentTime < accessTokenExpiration - TimeUnit.SECONDS.toMillis(30)
    }

    private suspend fun fetchTokenFromProvider(): AccessTokenInfo {
        return suspendCancellableCoroutine { continuation ->
            try {
                tokenProvider.fetchToken(object : AccessTokenCallback {
                    override fun onSuccess(tokenInfo: AccessTokenInfo) {
                        if (continuation.isActive) {
                            continuation.resume(tokenInfo)
                        }
                    }

                    override fun onFailure(error: Exception) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
                })
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    private fun processTokenInfo(tokenInfo: AccessTokenInfo) {
        accessToken = tokenInfo.token
        accessTokenExpiration =
            getNowInMillis() + TimeUnit.SECONDS.toMillis(tokenInfo.expiresInSeconds)
        logger.d("Auth token refreshed, expires in ${tokenInfo.expiresInSeconds} seconds")
    }

    internal fun getNowInMillis() = System.currentTimeMillis()
}
