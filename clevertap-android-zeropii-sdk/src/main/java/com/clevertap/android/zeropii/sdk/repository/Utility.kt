package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeResponse
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeSummary
import com.clevertap.android.zeropii.sdk.model.TokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.TokenizeResponse
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger
import kotlinx.coroutines.delay
import retrofit2.Response
import java.io.IOException

/**
 * Handles retry logic with exponential backoff for network operations
 */
class RetryHandler(
    private val authRepository: AuthRepository,
    private val logger: ZeroPiiLogger,
    private val maxRetries: Int = 1,
    private val initialDelayMs: Long = 1000L
) {
    /**
     * Executes an API call with retry logic and exponential backoff
     */
    suspend fun <T> executeWithRetry(apiCall: suspend () -> Response<T>): Response<T> {
        var attempt = 0

        while (attempt <= maxRetries) {
            try {
                val response = apiCall()

                when (response.code()) {
                    401 -> {
                        // Authentication error - refresh token and retry immediately
                        if (attempt < maxRetries) {
                            logger.d("401 Unauthorized - refreshing token and retrying immediately (no delay)")
                            authRepository.refreshAccessToken()
                            attempt++
                            continue // No delay for authentication issues
                        }
                    }

                    500, 502, 503, 504, 429 -> {
                        // Server errors or rate limiting - retry with delay
                        if (attempt < maxRetries) {
                            logger.w("Server error ${response.code()} - retrying after delay")
                            attempt = retryWithDelay(attempt)
                            continue
                        }
                    }
                }

                return response

            } catch (e: IOException) {
                // Network errors - retry with delay
                if (attempt < maxRetries) {
                    logger.w("Network error - retrying after delay", e)
                    attempt = retryWithDelay(attempt)
                    continue
                }
                throw Exception("Network error after $maxRetries retries", e)
            } catch (e: Exception) {
                // For other exceptions, don't retry - just throw
                logger.e("Non-retryable exception occurred: ${e.message}", e)
                throw e
            }
        }
        throw Exception("Failed after $maxRetries retries")
    }

    private suspend fun retryWithDelay(attempt: Int, exception: Exception? = null): Int {
        val newAttempt = attempt + 1
        val delayToApply = initialDelayMs * (1 shl newAttempt) // Exponential backoff

        if (exception != null) {
            logger.d("Waiting ${delayToApply}ms before retry attempt $newAttempt due to exception")
        } else {
            logger.d("Waiting ${delayToApply}ms before retry attempt $newAttempt")
        }

        delay(delayToApply)
        return newAttempt
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
            TokenizeRepoResult.Error(errorMessage)
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
            BatchTokenizeRepoResult.Error(errorMessage)
        }
    }

    private fun getErrorMessage(response: Response<*>, operation: String): String {
        val errorBody = response.errorBody()?.string() ?: "Unknown error"
        return "$operation failed: ${response.code()} - $errorBody"
    }
}
