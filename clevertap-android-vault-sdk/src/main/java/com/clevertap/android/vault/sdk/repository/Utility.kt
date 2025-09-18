package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.cache.TokenCache
import com.clevertap.android.vault.sdk.model.BatchDetokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeSummary
import com.clevertap.android.vault.sdk.model.BatchTokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeSummary
import com.clevertap.android.vault.sdk.model.DetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.DetokenizeResponse
import com.clevertap.android.vault.sdk.model.TokenizeRepoResult
import com.clevertap.android.vault.sdk.model.TokenizeResponse
import com.clevertap.android.vault.sdk.util.VaultLogger
import kotlinx.coroutines.delay
import retrofit2.Response
import java.io.IOException

/**
 * Handles retry logic with exponential backoff for network operations
 */
class RetryHandler(
    private val authRepository: AuthRepository,
    private val logger: VaultLogger,
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
        val delay = initialDelayMs * (1 shl newAttempt) // Exponential backoff

        if (exception != null) {
            logger.d("Waiting ${delay}ms before retry attempt $newAttempt due to exception")
        } else {
            logger.d("Waiting ${delay}ms before retry attempt $newAttempt")
        }

        delay(delay)
        return newAttempt
    }
}

/**
 * Manages cache operations for tokens and values
 */
class CacheManager(
    private val tokenCache: TokenCache,
    private val logger: VaultLogger
) {
    /**
     * Gets token from cache for a given value
     */
    fun getTokenFromCache(value: String): CacheResult? {
        if (!tokenCache.isEnabled()) return null

        val cachedTokenPair = tokenCache.getToken(value)
        return if (cachedTokenPair != null) {
            logger.d("Token found in cache")
            CacheResult.TokenResult(
                token = cachedTokenPair.first,
                dataType = cachedTokenPair.second
            )
        } else null
    }

    /**
     * Gets value from cache for a given token
     */
    fun getValueFromCache(token: String): CacheResult? {
        if (!tokenCache.isEnabled()) return null

        val cachedValuePair = tokenCache.getValue(token)
        return if (cachedValuePair != null) {
            logger.d("Value found in cache")
            CacheResult.ValueResult(
                value = cachedValuePair.first,
                dataType = cachedValuePair.second
            )
        } else null
    }

    /**
     * Gets multiple tokens from cache for batch operations
     */
    fun getBatchTokensFromCache(values: List<String>): BatchCacheResult {
        if (!tokenCache.isEnabled()) {
            return BatchCacheResult(emptyList(), values)
        }

        val cached = mutableListOf<BatchTokenItemResponse>()
        val uncached = mutableListOf<String>()

        values.forEach { value ->
            val cachedPair = tokenCache.getToken(value)
            if (cachedPair != null) {
                cached.add(
                    BatchTokenItemResponse(
                        originalValue = value,
                        token = cachedPair.first,
                        exists = true,
                        newlyCreated = false,
                        dataType = cachedPair.second
                    )
                )
            } else {
                uncached.add(value)
            }
        }

        return BatchCacheResult(cached, uncached)
    }

    /**
     * Gets multiple values from cache for batch operations
     */
    fun getBatchValuesFromCache(tokens: List<String>): BatchCacheResult {
        if (!tokenCache.isEnabled()) {
            return BatchCacheResult(emptyList(), tokens)
        }

        val cached = mutableListOf<BatchDetokenItemResponse>()
        val uncached = mutableListOf<String>()

        tokens.forEach { token ->
            val cachedPair = tokenCache.getValue(token)
            if (cachedPair != null) {
                cached.add(
                    BatchDetokenItemResponse(
                        token = token,
                        value = cachedPair.first,
                        exists = true,
                        dataType = cachedPair.second
                    )
                )
            } else {
                uncached.add(token)
            }
        }

        return BatchCacheResult(cached, uncached)
    }

    /**
     * Stores token in cache
     */
    fun storeTokenInCache(value: String, token: String, dataType: String?) {
        if (tokenCache.isEnabled()) {
            tokenCache.putToken(value, token, dataType)
            logger.d("Token stored in cache")
        }
    }

    /**
     * Stores value in cache
     */
    fun storeValueInCache(token: String, value: String, dataType: String?) {
        if (tokenCache.isEnabled()) {
            tokenCache.putValue(token, value, dataType)
            logger.d("Value stored in cache")
        }
    }

    /**
     * Stores batch tokenization results in cache
     */
    fun storeBatchTokensInCache(results: List<BatchTokenItemResponse>) {
        if (tokenCache.isEnabled()) {
            results.forEach { item ->
                if (item.exists || item.newlyCreated) {
                    tokenCache.putToken(item.originalValue, item.token, item.dataType)
                }
            }
            logger.d("Batch tokens stored in cache")
        }
    }

    /**
     * Stores batch detokenization results in cache
     */
    fun storeBatchValuesInCache(results: List<BatchDetokenItemResponse>) {
        if (tokenCache.isEnabled()) {
            results.forEach { item ->
                if (item.exists && item.value != null) {
                    tokenCache.putValue(item.token, item.value, item.dataType)
                }
            }
            logger.d("Batch values stored in cache")
        }
    }
}

/**
 * Processes API responses into appropriate result formats
 */
class ResponseProcessor(
    private val logger: VaultLogger
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
     * Processes detokenize response
     */
    fun processDetokenizeResponse(response: Response<*>): DetokenizeRepoResult {
        return if (response.isSuccessful && response.body() != null) {
            val detokenizeResponse = response.body() as DetokenizeResponse
            DetokenizeRepoResult.Success(
                value = detokenizeResponse.value,
                exists = detokenizeResponse.exists,
                dataType = detokenizeResponse.dataType
            )
        } else {
            val errorMessage = getErrorMessage(response, "Detokenization")
            logger.e(errorMessage)
            DetokenizeRepoResult.Error(errorMessage)
        }
    }

    /**
     * Processes batch tokenize response
     */
    fun processBatchTokenizeResponse(
        response: Response<*>,
        cachedResults: List<BatchTokenItemResponse>
    ): BatchTokenizeRepoResult {
        return if (response.isSuccessful && response.body() != null) {
            val batchResponse = response.body() as BatchTokenizeResponse
            val allResults = cachedResults + batchResponse.results

            BatchTokenizeRepoResult.Success(
                results = allResults,
                summary = BatchTokenizeSummary(
                    processedCount = allResults.size,
                    existingCount = allResults.count { it.exists },
                    newlyCreatedCount = allResults.count { it.newlyCreated }
                )
            )
        } else {
            val errorMessage = getErrorMessage(response, "Batch tokenization")
            logger.e(errorMessage)
            BatchTokenizeRepoResult.Error(errorMessage)
        }
    }

    /**
     * Processes batch detokenize response
     */
    fun processBatchDetokenizeResponse(
        response: Response<*>,
        cachedResults: List<BatchDetokenItemResponse>
    ): BatchDetokenizeRepoResult {
        return if (response.isSuccessful && response.body() != null) {
            val batchResponse = response.body() as BatchDetokenizeResponse
            val allResults = cachedResults + batchResponse.results

            BatchDetokenizeRepoResult.Success(
                results = allResults,
                summary = BatchDetokenizeSummary(
                    processedCount = allResults.size,
                    foundCount = allResults.count { it.exists },
                    notFoundCount = allResults.count { !it.exists }
                )
            )
        } else {
            val errorMessage = getErrorMessage(response, "Batch detokenization")
            logger.e(errorMessage)
            BatchDetokenizeRepoResult.Error(errorMessage)
        }
    }

    private fun getErrorMessage(response: Response<*>, operation: String): String {
        val errorBody = response.errorBody()?.string() ?: "Unknown error"
        return "$operation failed: ${response.code()} - $errorBody"
    }
}

/**
 * Sealed class representing cache results
 */
sealed class CacheResult {
    data class TokenResult(val token: String, val dataType: String) : CacheResult()
    data class ValueResult(val value: String, val dataType: String) : CacheResult()
}

/**
 * Data class for batch cache results
 */
data class BatchCacheResult(
    val cached: List<Any>, // Can be BatchTokenItemResponse or BatchDetokenItemResponse
    val uncached: List<String>
)