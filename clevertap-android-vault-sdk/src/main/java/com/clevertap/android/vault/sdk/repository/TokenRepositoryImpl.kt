package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.cache.TokenCache
import com.clevertap.android.vault.sdk.model.BatchDetokenItem
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRequest
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeSummary
import com.clevertap.android.vault.sdk.model.BatchTokenItem
import com.clevertap.android.vault.sdk.model.BatchTokenizeRequest
import com.clevertap.android.vault.sdk.model.BatchTokenizeResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeSummary
import com.clevertap.android.vault.sdk.model.DetokenizeRequest
import com.clevertap.android.vault.sdk.model.DetokenizeResult
import com.clevertap.android.vault.sdk.model.TokenizeRequest
import com.clevertap.android.vault.sdk.model.TokenizeResult
import com.clevertap.android.vault.sdk.network.NetworkProvider
import com.clevertap.android.vault.sdk.util.VaultLogger
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

/**
 * Implementation of the TokenRepository interface
 */
class TokenRepositoryImpl(
    private val networkProvider: NetworkProvider,
    private val authRepository: AuthRepository,
    private val tokenCache: TokenCache,
    private val logger: VaultLogger
) : TokenRepository {

    companion object {
        /**
         * Maximum number of retry attempts for API calls that fail due to
         * network issues or authentication errors.
         */
        private const val MAX_RETRY_COUNT = 3
        /**
         * Initial delay (in milliseconds) between retry attempts.
         * This value is used as the base for exponential backoff.
         */
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        /**
         * Maximum number of values that can be processed in a single batch tokenization request.
         * Requests exceeding this limit will be rejected with an error.
         */
        private const val MAX_BATCH_SIZE_TOKENIZE = 1000 // TODO: final value?
        /**
         * Maximum number of tokens that can be processed in a single batch detokenization request.
         * Requests exceeding this limit will be rejected with an error.
         */
        private const val MAX_BATCH_SIZE_DETOKENIZE = 10000// TODO: final value?
    }

    /**
     * Tokenizes a single sensitive value without encryption over transit.
     *
     * This method replaces a sensitive value with a format-preserving token by:
     * 1. Checking the cache for an existing token (if caching is enabled)
     * 2. If not in cache, making an API call to the tokenization service
     * 3. Storing the result in cache (if caching is enabled)
     *
     * @param value The sensitive value to tokenize
     * @return A [TokenizeResult] containing either the token or an error message
     */
    override suspend fun tokenize(value: String): TokenizeResult {
        // Check cache first if enabled
        if (tokenCache.isEnabled()) {
            val cachedToken = tokenCache.getToken(value)
            if (cachedToken != null) {
                logger.d("Token found in cache")
                return TokenizeResult.Success(
                    token = cachedToken,
                    exists = true,
                    newlyCreated = false,
                    dataType = "string" // assuming string as default for cached values
                )
            }
        }

        // Not in cache, call the API without encryption
        return try {
            val accessToken = authRepository.getAccessToken()
            val apiService = networkProvider.getTokenizationApi()

            val request = TokenizeRequest(value)

            val response = executeWithRetry {
                apiService.tokenize("Bearer $accessToken", request)
            }

            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!

                // Cache the result if enabled
                if (tokenCache.isEnabled()) {
                    tokenCache.putToken(value, tokenResponse.token)
                }

                TokenizeResult.Success(
                    token = tokenResponse.token,
                    exists = tokenResponse.exists,
                    newlyCreated = tokenResponse.newlyCreated,
                    dataType = tokenResponse.dataType
                )
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logger.e("Tokenization failed: ${response.code()} - $errorBody")
                TokenizeResult.Error("Tokenization failed: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            logger.e("Error during tokenization", e)
            TokenizeResult.Error("Error during tokenization: ${e.message}")
        }
    }

    /**
     * Detokenizes a single token to retrieve the original value.
     *
     * This method retrieves the original sensitive value for a token by:
     * 1. Checking the cache for the original value (if caching is enabled)
     * 2. If not in cache, making an API call to the detokenization service
     * 3. Storing the result in cache (if caching is enabled)
     *
     * @param token The token to detokenize
     * @return A [DetokenizeResult] containing either the original value or an error message
     */
    override suspend fun detokenize(token: String): DetokenizeResult {
        // Check cache first if enabled
        if (tokenCache.isEnabled()) {
            val cachedValue = tokenCache.getValue(token)
            if (cachedValue != null) {
                logger.d("Value found in cache")
                return DetokenizeResult.Success(
                    value = cachedValue,
                    exists = true,
                    dataType = "string" // assuming string as default for cached values
                )
            }
        }

        // Not in cache, call the API
        return try {
            val accessToken = authRepository.getAccessToken()
            val apiService = networkProvider.getTokenizationApi()

            val request = DetokenizeRequest(token)

            val response = executeWithRetry {
                apiService.detokenize("Bearer $accessToken", request)
            }

            if (response.isSuccessful && response.body() != null) {
                val detokenizeResponse = response.body()!!

                // Cache the result if enabled and exists
                if (tokenCache.isEnabled() && detokenizeResponse.exists && detokenizeResponse.value != null) {
                    tokenCache.putValue(token, detokenizeResponse.value)
                }

                DetokenizeResult.Success(
                    value = detokenizeResponse.value,
                    exists = detokenizeResponse.exists,
                    dataType = detokenizeResponse.dataType
                )
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logger.e("Detokenization failed: ${response.code()} - $errorBody")
                DetokenizeResult.Error("Detokenization failed: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            logger.e("Error during detokenization", e)
            DetokenizeResult.Error("Error during detokenization: ${e.message}")
        }
    }

    /**
     * Tokenizes multiple sensitive values in a batch operation.
     *
     * This method efficiently processes multiple values at once by:
     * 1. Validating the batch size against the maximum limit
     * 2. Checking the cache for existing tokens (if caching is enabled)
     * 3. Making a batch API call for values not found in cache
     * 4. Updating the cache with new results
     * 5. Combining cached and new results
     *
     * @param values The list of sensitive values to tokenize
     * @return A [BatchTokenizeResult] containing the results or an error message
     * @throws Exception If the batch size exceeds the maximum limit or if an error occurs
     */
    override suspend fun batchTokenize(values: List<String>): BatchTokenizeResult {
        // Validate batch size
        if (values.isEmpty()) {
            return BatchTokenizeResult.Error("Batch tokenize request contains no values")
        }

        if (values.size > MAX_BATCH_SIZE_TOKENIZE) {
            return BatchTokenizeResult.Error("Batch size exceeds the maximum limit of $MAX_BATCH_SIZE_TOKENIZE values")
        }

        // First check cache for all values if enabled
        val results = mutableListOf<BatchTokenItem>()
        val uncachedValues = mutableListOf<String>()

        if (tokenCache.isEnabled()) {
            for (value in values) {
                val cachedToken = tokenCache.getToken(value)
                if (cachedToken != null) {
                    logger.d("Token found in cache for value in batch")
                    results.add(
                        BatchTokenItem(
                            originalValue = value,
                            token = cachedToken,
                            exists = true,
                            newlyCreated = false,
                            dataType = "string" // assuming string as default for cached values
                        )
                    )
                } else {
                    uncachedValues.add(value)
                }
            }

            // If all values were in cache, return immediately
            if (uncachedValues.isEmpty()) {
                logger.d("All tokens found in cache")
                return BatchTokenizeResult.Success(
                    results = results,
                    summary = BatchTokenizeSummary(
                        processedCount = results.size,
                        existingCount = results.size,
                        newlyCreatedCount = 0
                    )
                )
            }
        } else {
            // Cache not enabled, process all values
            uncachedValues.addAll(values)
        }

        // Now handle the values not found in cache
        return try {
            val accessToken = authRepository.getAccessToken()
            val apiService = networkProvider.getTokenizationApi()

            val request = BatchTokenizeRequest(uncachedValues)

            val response = executeWithRetry {
                apiService.batchTokenize("Bearer $accessToken", request)
            }

            if (response.isSuccessful && response.body() != null) {
                val batchResponse = response.body()!!

                // Cache the results if enabled
                if (tokenCache.isEnabled()) {
                    batchResponse.results.forEach { item ->
                        if (item.exists || item.newlyCreated) {
                            tokenCache.putToken(item.originalValue, item.token)
                            tokenCache.putValue(item.token, item.originalValue)
                        }
                    }
                }

                // Combine cached results with new results
                results.addAll(batchResponse.results)

                BatchTokenizeResult.Success(
                    results = results,
                    summary = BatchTokenizeSummary(
                        processedCount = results.size,
                        existingCount = results.count { it.exists },
                        newlyCreatedCount = results.count { it.newlyCreated }
                    )
                )
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logger.e("Batch tokenization failed: ${response.code()} - $errorBody")
                BatchTokenizeResult.Error("Batch tokenization failed: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            logger.e("Error during batch tokenization", e)
            BatchTokenizeResult.Error("Error during batch tokenization: ${e.message}")
        }
    }

    /**
     * Detokenizes multiple tokens in a batch operation.
     *
     * This method efficiently retrieves multiple original values at once by:
     * 1. Validating the batch size against the maximum limit
     * 2. Checking the cache for existing values (if caching is enabled)
     * 3. Making a batch API call for tokens not found in cache
     * 4. Updating the cache with new results
     * 5. Combining cached and new results
     *
     * @param tokens The list of tokens to detokenize
     * @return A [BatchDetokenizeResult] containing the results or an error message
     */
    override suspend fun batchDetokenize(tokens: List<String>): BatchDetokenizeResult {
        // Validate batch size
        if (tokens.isEmpty()) {
            return BatchDetokenizeResult.Error("Batch detokenize request contains no tokens")
        }

        if (tokens.size > MAX_BATCH_SIZE_DETOKENIZE) {
            return BatchDetokenizeResult.Error("Batch size exceeds the maximum limit of $MAX_BATCH_SIZE_DETOKENIZE tokens")
        }

        val cachedResults = mutableListOf<BatchDetokenItem>()
        val uncachedTokens = mutableListOf<String>()
        // Check cache first if enabled
        if (tokenCache.isEnabled()) {

            // Try to get values from cache
            tokens.forEach { token ->
                val cachedValue = tokenCache.getValue(token)
                if (cachedValue != null) {
                    cachedResults.add(
                        BatchDetokenItem(
                            token = token,
                            value = cachedValue,
                            exists = true,
                            dataType = "string" // assuming string as default for cached values
                        )
                    )
                } else {
                    uncachedTokens.add(token)
                }
            }

            // If all tokens were in cache, return immediately
            if (uncachedTokens.isEmpty()) {
                logger.d("All values found in cache")
                return BatchDetokenizeResult.Success(
                    results = cachedResults,
                    summary = BatchDetokenizeSummary(
                        processedCount = cachedResults.size,
                        foundCount = cachedResults.size,
                        notFoundCount = 0
                    )
                )
            }
        } else {
            // Cache not enabled, process all values
            uncachedTokens.addAll(tokens)
        }

        // Otherwise, fetch the uncached tokens
        return try {
            val accessToken = authRepository.getAccessToken()
            val apiService = networkProvider.getTokenizationApi()

            val request = BatchDetokenizeRequest(uncachedTokens)

            val response = executeWithRetry {
                apiService.batchDetokenize("Bearer $accessToken", request)
            }

            if (response.isSuccessful && response.body() != null) {
                val batchResponse = response.body()!!

                // Cache the results
                batchResponse.results.forEach { item ->
                    if (item.exists && item.value != null) {
                        tokenCache.putValue(item.token, item.value)
                    }
                }

                // Combine cached and API results
                val allResults = cachedResults + batchResponse.results

                BatchDetokenizeResult.Success(
                    results = allResults,
                    summary = BatchDetokenizeSummary(
                        processedCount = allResults.size,
                        foundCount = allResults.count { it.exists },
                        notFoundCount = allResults.count { !it.exists }
                    )
                )
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logger.e("Batch detokenization failed: ${response.code()} - $errorBody")
                BatchDetokenizeResult.Error("Batch detokenization failed: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            logger.e("Error during batch detokenization", e)
            BatchDetokenizeResult.Error("Error during batch detokenization: ${e.message}")
        }
    }

    /**
     * Executes a network call with retry logic and exponential backoff.
     *
     * This utility method provides resilience for API calls by:
     * 1. Attempting the API call
     * 2. Handling authentication errors by refreshing the token and retrying
     * 3. Handling network errors with exponential backoff
     * 4. Limiting retries to a maximum count
     *
     * @param apiCall A suspend function representing the API call to execute
     * @return The response from the successful API call
     * @throws Exception If all retry attempts fail
     */
    private suspend fun <T> executeWithRetry(apiCall: suspend () -> retrofit2.Response<T>): retrofit2.Response<T> {
        var retryCount = 0
        var lastException: Exception? = null
        // TODO: what we want to retry exactly? Network connectivity issues (IOException) is for sure
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                val response = apiCall()

                // If unauthorized, refresh token and retry
                if (response.code() == 401) {
                    logger.d("Received 401 Unauthorized, refreshing token and retrying")
                    authRepository.refreshAccessToken()
                    retryCount++
                    // Exponential backoff
                    delay(INITIAL_RETRY_DELAY_MS * (1 shl retryCount))
                    continue
                }

                return response
            } catch (e: HttpException) {
                lastException = e
                logger.e("HTTP exception during API call, retrying", e)
            } catch (e: IOException) {
                lastException = e
                logger.e("IO exception during API call, retrying", e)
            } catch (e: Exception) {
                throw e // Rethrow unexpected exceptions
            }

            retryCount++
            if (retryCount < MAX_RETRY_COUNT) {
                // Exponential backoff
                delay(INITIAL_RETRY_DELAY_MS * (1 shl retryCount))
            }
        }

        throw lastException ?: Exception("Failed after $MAX_RETRY_COUNT retries")
    }
}
