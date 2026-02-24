package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeResponse
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeSummary
import com.clevertap.android.zeropii.sdk.model.TokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.TokenizeResponse
import com.clevertap.android.zeropii.sdk.retry.RetryPolicy
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger
import kotlinx.coroutines.delay
import retrofit2.Response
import java.io.IOException

/**
 * Handles retry logic for network operations, delegating retry decisions to [RetryPolicy].
 *
 * 401 Unauthorized is handled internally (token refresh + one immediate retry) and is
 * never passed to the [RetryPolicy].
 */
class RetryHandler(
    private val authRepository: AuthRepository,
    private val logger: ZeroPiiLogger,
    private val retryPolicy: RetryPolicy
) {

    /**
     * Executes an API call with retry logic driven by the injected [RetryPolicy].
     */
    suspend fun <T> executeWithRetry(apiCall: suspend () -> Response<T>): Response<T> {
        var attempt = 0
        var tokenRefreshed = false

        while (true) {
            try {
                val response = apiCall()

                when (response.code()) {
                    401 -> {
                        // Authentication error — SDK handles internally: refresh token + immediate retry.
                        // Not delegated to RetryPolicy. Only one token refresh per executeWithRetry call.
                        if (!tokenRefreshed) {
                            logger.d("401 Unauthorized - refreshing token and retrying immediately (no delay)")
                            authRepository.refreshAccessToken()
                            tokenRefreshed = true
                            continue
                        }
                    }

                    else -> {
                        if (!response.isSuccessful && retryPolicy.shouldRetry(attempt, response.code())) {
                            val delayMs = retryPolicy.retryDelayMs(attempt)
                            logger.w("Server error ${response.code()} - retrying after ${delayMs}ms")
                            delay(delayMs)
                            attempt++
                            continue
                        }
                    }
                }

                return response

            } catch (e: IOException) {
                // Network error — null status code signals no HTTP response was received.
                if (retryPolicy.shouldRetry(attempt, null)) {
                    val delayMs = retryPolicy.retryDelayMs(attempt)
                    logger.w("Network error - retrying after ${delayMs}ms", e)
                    delay(delayMs)
                    attempt++
                    continue
                }
                throw Exception("Network error, retries exhausted", e)
            } catch (e: Exception) {
                // Non-retryable exception — throw immediately.
                logger.e("Non-retryable exception occurred: ${e.message}", e)
                throw e
            }
        }
    }
}

/**
 * Processes API responses into appropriate result formats
 */
class ResponseProcessor(
    private val logger: ZeroPiiLogger
) {
    /**
     * Processes tokenize response
     */
    fun processTokenizeResponse(response: Response<*>): TokenizeRepoResult {
        return if (response.isSuccessful && response.body() != null) {
            val tokenResponse = response.body() as TokenizeResponse
            TokenizeRepoResult.Success(
                token = tokenResponse.token,
                exists = tokenResponse.exists,
                newlyCreated = tokenResponse.newlyCreated,
                dataType = tokenResponse.dataType
            )
        } else {
            val errorMessage = getErrorMessage(response, "Tokenization")
            logger.e(errorMessage)
            TokenizeRepoResult.Error(errorMessage, httpStatusCode = response.code())
        }
    }

    /**
     * Processes batch tokenize response
     */
    fun processBatchTokenizeResponse(response: Response<*>): BatchTokenizeRepoResult {
        return if (response.isSuccessful && response.body() != null) {
            val batchResponse = response.body() as BatchTokenizeResponse
            val results = batchResponse.results

            BatchTokenizeRepoResult.Success(
                results = results,
                summary = BatchTokenizeSummary(
                    processedCount = results.size,
                    existingCount = results.count { it.exists },
                    newlyCreatedCount = results.count { it.newlyCreated }
                )
            )
        } else {
            val errorMessage = getErrorMessage(response, "Batch tokenization")
            logger.e(errorMessage)
            BatchTokenizeRepoResult.Error(errorMessage, httpStatusCode = response.code())
        }
    }

    private fun getErrorMessage(response: Response<*>, operation: String): String {
        val errorBody = response.errorBody()?.string() ?: "Unknown error"
        return "$operation failed: ${response.code()} - $errorBody"
    }
}
