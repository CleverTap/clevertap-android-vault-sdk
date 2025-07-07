package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.cache.TokenCache
import com.clevertap.android.vault.sdk.encryption.DecryptionSuccess
import com.clevertap.android.vault.sdk.encryption.EncryptionManager
import com.clevertap.android.vault.sdk.encryption.EncryptionSuccess
import com.clevertap.android.vault.sdk.model.BatchDetokenItem
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRequest
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeSummary
import com.clevertap.android.vault.sdk.model.BatchTokenItem
import com.clevertap.android.vault.sdk.model.BatchTokenizeRequest
import com.clevertap.android.vault.sdk.model.BatchTokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeSummary
import com.clevertap.android.vault.sdk.model.DetokenizeRequest
import com.clevertap.android.vault.sdk.model.DetokenizeResponse
import com.clevertap.android.vault.sdk.model.DetokenizeResult
import com.clevertap.android.vault.sdk.model.EncryptedRequest
import com.clevertap.android.vault.sdk.model.TokenizeRequest
import com.clevertap.android.vault.sdk.model.TokenizeResponse
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
    private val encryptionManager: EncryptionManager,
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
     * Flag to track if encryption should be temporarily disabled due to backend decryption failures
     */
    @Volatile
    private var encryptionDisabledDueToFailure = false

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
     * Tokenizes a single sensitive value with encryption over transit.
     *
     * This method provides enhanced security by encrypting the data before transmission:
     * 1. Checking the cache for an existing token (if caching is enabled)
     * 2. If not in cache, encrypting the value using AES-GCM encryption
     * 3. Making an API call with the encrypted payload
     * 4. Handling 419 status code by falling back to non-encrypted tokenization
     * 5. Decrypting the response
     * 6. Storing the result in cache (if caching is enabled)
     *
     * @param value The sensitive value to tokenize
     * @return A [TokenizeResult] containing either the token or an error message
     */
    override suspend fun tokenizeWithEncryptionOverTransit(value: String): TokenizeResult {
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

        // Check if encryption is enabled and not disabled due to failure
        if (!encryptionManager.isEnabled() || encryptionDisabledDueToFailure) {
            logger.d("Encryption is disabled or failed, falling back to non-encrypted tokenization")
            return tokenize(value)
        }

        // Not in cache, call the API with encryption
        return try {
            val accessToken = authRepository.getAccessToken()
            val apiService = networkProvider.getTokenizationApi()

            val encryptionResult = encryptionManager.encrypt(value)
            if (encryptionResult !is EncryptionSuccess) {
                logger.e("Encryption failed, falling back to non-encrypted tokenization")
                return tokenize(value)
            }

            val encryptedRequest = EncryptedRequest(
                itp = encryptionResult.encryptedPayload,
                itk = encryptionResult.sessionKey,
                iv = encryptionResult.iv
            )

            val response = executeWithRetryForEncryption(
                encryptedCall = {
                    apiService.tokenizeEncrypted("Bearer $accessToken", true, encryptedRequest)
                },
                fallbackCall = {
                    tokenize(value)
                }
            )

            // Handle the response based on its type
            return when (response) {
                is EncryptedApiResponse -> {
                    // Decrypt the response and parse into TokenizeResponse
                    val tokenResponse = decryptAndParseResponse(
                        response.encryptedPayload,
                        response.iv,
                        TokenizeResponse::class.java
                    )

                    if (tokenResponse == null) {
                        return TokenizeResult.Error("Failed to decrypt or parse response")
                    }

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
                }
                is FallbackResponse -> {
                    response.result as TokenizeResult
                }
                is ErrorResponse -> {
                    TokenizeResult.Error(response.message)
                }
            }
        } catch (e: Exception) {
            logger.e("Error during encrypted tokenization", e)
            TokenizeResult.Error("Error during encrypted tokenization: ${e.message}")
        }
    }

    /**
     * Detokenizes a single token with encryption over transit.
     *
     * This method provides enhanced security by encrypting the request and response:
     * 1. Checking the cache for the original value (if caching is enabled)
     * 2. If not in cache, encrypting the request using AES-GCM encryption
     * 3. Making an API call with the encrypted payload
     * 4. Handling 419 status code by falling back to non-encrypted detokenization
     * 5. Decrypting the response
     * 6. Storing the result in cache (if caching is enabled)
     *
     * @param token The token to detokenize
     * @return A [DetokenizeResult] containing either the original value or an error message
     */
    override suspend fun detokenizeWithEncryptionOverTransit(token: String): DetokenizeResult {
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

        // Check if encryption is enabled and not disabled due to failure
        if (!encryptionManager.isEnabled() || encryptionDisabledDueToFailure) {
            logger.d("Encryption is disabled or failed, falling back to non-encrypted detokenization")
            return detokenize(token)
        }

        // Not in cache, call the API with encryption
        try {
            val accessToken = authRepository.getAccessToken()
            val apiService = networkProvider.getTokenizationApi()

            // Encrypt the detokenize request
            val request = DetokenizeRequest(token)
            val gson = com.google.gson.Gson()
            val requestJson = gson.toJson(request)

            val encryptionResult = encryptionManager.encrypt(requestJson)
            if (encryptionResult !is EncryptionSuccess) {
                logger.e("Encryption failed, falling back to non-encrypted detokenization")
                return detokenize(token)
            }

            val encryptedRequest = EncryptedRequest(
                itp = encryptionResult.encryptedPayload,
                itk = encryptionResult.sessionKey,
                iv = encryptionResult.iv
            )

            val response = executeWithRetryForEncryption(
                encryptedCall = {
                    apiService.detokenizeEncrypted("Bearer $accessToken", true, encryptedRequest)
                },
                fallbackCall = {
                    detokenize(token)
                }
            )

            return when (response) {
                is EncryptedApiResponse -> {
                    // Decrypt and parse the response
                    val detokenizeResponse = decryptAndParseResponse(
                        response.encryptedPayload,
                        response.iv,
                        DetokenizeResponse::class.java
                    )

                    if (detokenizeResponse == null) {
                        return DetokenizeResult.Error("Failed to decrypt or parse response")
                    }

                    // Cache the result if enabled and exists
                    if (tokenCache.isEnabled() && detokenizeResponse.exists && detokenizeResponse.value != null) {
                        tokenCache.putValue(token, detokenizeResponse.value)
                    }

                    DetokenizeResult.Success(
                        value = detokenizeResponse.value,
                        exists = detokenizeResponse.exists,
                        dataType = detokenizeResponse.dataType
                    )
                }
                is FallbackResponse -> {
                    response.result as DetokenizeResult
                }
                is ErrorResponse -> {
                    DetokenizeResult.Error(response.message)
                }
            }
        } catch (e: Exception) {
            logger.e("Error during encrypted detokenization", e)
            return DetokenizeResult.Error("Error during encrypted detokenization: ${e.message}")
        }
    }

    /**
     * Tokenizes multiple sensitive values in a batch operation with encryption over transit.
     *
     * This method provides enhanced security for batch operations by:
     * 1. Validating the batch size against the maximum limit
     * 2. Checking the cache for existing tokens (if caching is enabled)
     * 3. Encrypting the entire batch request
     * 4. Making a batch API call with the encrypted payload
     * 5. Handling 419 status code by falling back to non-encrypted batch tokenization
     * 6. Decrypting the response
     * 7. Updating the cache with new results
     * 8. Combining cached and new results
     *
     * @param values The list of sensitive values to tokenize
     * @return A [BatchTokenizeResult] containing the results or an error message
     */
    override suspend fun batchTokenizeWithEncryptionOverTransit(values: List<String>): BatchTokenizeResult {
        // Validate batch size
        if (values.isEmpty()) {
            return BatchTokenizeResult.Error("Batch tokenize request contains no values")
        }

        if (values.size > MAX_BATCH_SIZE_TOKENIZE) {
            return BatchTokenizeResult.Error("Batch size exceeds the maximum limit of $MAX_BATCH_SIZE_TOKENIZE values")
        }

        // Check if encryption is enabled and not disabled due to failure
        if (!encryptionManager.isEnabled() || encryptionDisabledDueToFailure) {
            logger.d("Encryption is disabled or failed, falling back to non-encrypted batch tokenization")
            return batchTokenize(values)
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
        try {
            val accessToken = authRepository.getAccessToken()
            val apiService = networkProvider.getTokenizationApi()

            // Batch tokenize by encrypting the entire batch request
            val batchRequest = BatchTokenizeRequest(uncachedValues)
            val gson = com.google.gson.Gson()
            val requestJson = gson.toJson(batchRequest)

            // Encrypt the batch request JSON
            val encryptionResult = encryptionManager.encrypt(requestJson)
            if (encryptionResult !is EncryptionSuccess) {
                logger.e("Encryption failed for batch request, falling back to non-encrypted")
                return batchTokenize(values)
            }

            val encryptedRequest = EncryptedRequest(
                itp = encryptionResult.encryptedPayload,
                itk = encryptionResult.sessionKey,
                iv = encryptionResult.iv
            )

            val response = executeWithRetryForEncryption(
                encryptedCall = {
                    apiService.batchTokenizeEncrypted("Bearer $accessToken", true, encryptedRequest)
                },
                fallbackCall = {
                    batchTokenize(values)
                }
            )

            return when (response) {
                is EncryptedApiResponse -> {
                    // Decrypt the response
                    val batchResponse = decryptAndParseResponse(
                        response.encryptedPayload,
                        response.iv,
                        BatchTokenizeResponse::class.java
                    )

                    if (batchResponse == null) {
                        return BatchTokenizeResult.Error("Failed to decrypt or parse batch response")
                    }

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
                }
                is FallbackResponse -> {
                    response.result as BatchTokenizeResult
                }
                is ErrorResponse -> {
                    BatchTokenizeResult.Error(response.message)
                }
            }
        } catch (e: Exception) {
            logger.e("Error during encrypted batch tokenization", e)
            return BatchTokenizeResult.Error("Error during encrypted batch tokenization: ${e.message}")
        }
    }

    /**
     * Detokenizes multiple tokens in a batch operation with encryption over transit.
     *
     * This method provides enhanced security for batch detokenization by:
     * 1. Validating the batch size against the maximum limit
     * 2. Checking the cache for existing values (if caching is enabled)
     * 3. Encrypting the entire batch request
     * 4. Making a batch API call with the encrypted payload
     * 5. Handling 419 status code by falling back to non-encrypted batch detokenization
     * 6. Decrypting the response
     * 7. Updating the cache with new results
     * 8. Combining cached and new results
     *
     * @param tokens The list of tokens to detokenize
     * @return A [BatchDetokenizeResult] containing the results or an error message
     */
    override suspend fun batchDetokenizeWithEncryptionOverTransit(tokens: List<String>): BatchDetokenizeResult {
        // Validate batch size
        if (tokens.isEmpty()) {
            return BatchDetokenizeResult.Error("Batch detokenize request contains no tokens")
        }

        if (tokens.size > MAX_BATCH_SIZE_DETOKENIZE) {
            return BatchDetokenizeResult.Error("Batch size exceeds the maximum limit of $MAX_BATCH_SIZE_DETOKENIZE tokens")
        }

        // Check if encryption is enabled and not disabled due to failure
        if (!encryptionManager.isEnabled() || encryptionDisabledDueToFailure) {
            logger.d("Encryption is disabled or failed, falling back to non-encrypted batch detokenization")
            return batchDetokenize(tokens)
        }

        // First check cache for all tokens if enabled
        val results = mutableListOf<BatchDetokenItem>()
        val uncachedTokens = mutableListOf<String>()

        if (tokenCache.isEnabled()) {
            for (token in tokens) {
                val cachedValue = tokenCache.getValue(token)
                if (cachedValue != null) {
                    logger.d("Value found in cache for token in batch")
                    results.add(
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
                    results = results,
                    summary = BatchDetokenizeSummary(
                        processedCount = results.size,
                        foundCount = results.size,
                        notFoundCount = 0
                    )
                )
            }
        } else {
            // Cache not enabled, process all tokens
            uncachedTokens.addAll(tokens)
        }

        // Now handle the tokens not found in cache
        try {
            val accessToken = authRepository.getAccessToken()
            val apiService = networkProvider.getTokenizationApi()

            // Batch detokenize by encrypting the entire batch request
            val batchRequest = BatchDetokenizeRequest(uncachedTokens)
            val gson = com.google.gson.Gson()
            val requestJson = gson.toJson(batchRequest)

            // Encrypt the batch request JSON
            val encryptionResult = encryptionManager.encrypt(requestJson)
            if (encryptionResult !is EncryptionSuccess) {
                logger.e("Encryption failed for batch request, falling back to non-encrypted")
                return batchDetokenize(tokens)
            }

            val encryptedRequest = EncryptedRequest(
                itp = encryptionResult.encryptedPayload,
                itk = encryptionResult.sessionKey,
                iv = encryptionResult.iv
            )

            val response = executeWithRetryForEncryption(
                encryptedCall = {
                    apiService.batchDetokenizeEncrypted("Bearer $accessToken", true, encryptedRequest)
                },
                fallbackCall = {
                    batchDetokenize(tokens)
                }
            )

            return when (response) {
                is EncryptedApiResponse -> {
                    // Decrypt the response
                    val batchResponse = decryptAndParseResponse(
                        response.encryptedPayload,
                        response.iv,
                        BatchDetokenizeResponse::class.java
                    )

                    if (batchResponse == null) {
                        return BatchDetokenizeResult.Error("Failed to decrypt or parse batch response")
                    }

                    // Cache the results if enabled
                    if (tokenCache.isEnabled()) {
                        batchResponse.results.forEach { item ->
                            if (item.exists && item.value != null) {
                                tokenCache.putValue(item.token, item.value)
                            }
                        }
                    }

                    // Combine cached results with new results
                    results.addAll(batchResponse.results)

                    BatchDetokenizeResult.Success(
                        results = results,
                        summary = BatchDetokenizeSummary(
                            processedCount = results.size,
                            foundCount = results.count { it.exists },
                            notFoundCount = results.count { !it.exists }
                        )
                    )
                }
                is FallbackResponse -> {
                    response.result as BatchDetokenizeResult
                }
                is ErrorResponse -> {
                    BatchDetokenizeResult.Error(response.message)
                }
            }
        } catch (e: Exception) {
            logger.e("Error during encrypted batch detokenization", e)
            return BatchDetokenizeResult.Error("Error during encrypted batch detokenization: ${e.message}")
        }
    }

    /**
     * Sealed class to represent different types of responses from encrypted API calls
     */
    private sealed class EncryptedCallResponse
    private data class EncryptedApiResponse(val encryptedPayload: String, val iv: String) : EncryptedCallResponse()
    private data class FallbackResponse(val result: Any) : EncryptedCallResponse()
    private data class ErrorResponse(val message: String) : EncryptedCallResponse()

    /**
     * Executes an encrypted API call with fallback to non-encrypted version on 419 error.
     *
     * @param encryptedCall The encrypted API call to execute
     * @param fallbackCall The fallback non-encrypted call to execute on 419 error
     * @return The response from either the encrypted call or the fallback call
     */
    private suspend fun executeWithRetryForEncryption(
        encryptedCall: suspend () -> retrofit2.Response<com.clevertap.android.vault.sdk.model.EncryptedResponse>,
        fallbackCall: suspend () -> Any
    ): EncryptedCallResponse {
        var retryCount = 0
        var lastException: Exception? = null

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                val response = encryptedCall()

                when (response.code()) {
                    419 -> {
                        // Decryption failure on backend, disable encryption and fallback
                        logger.w("Received 419 - Backend decryption failure, disabling encryption and falling back to non-encrypted requests")
                        encryptionDisabledDueToFailure = true
                        val fallbackResult = fallbackCall()
                        return FallbackResponse(fallbackResult)
                    }
                    401 -> {
                        // Unauthorized, refresh token and retry
                        logger.d("Received 401 Unauthorized, refreshing token and retrying")
                        authRepository.refreshAccessToken()
                        retryCount++
                        delay(INITIAL_RETRY_DELAY_MS * (1 shl retryCount))
                        continue
                    }
                    else -> {
                        if (response.isSuccessful && response.body() != null) {
                            val encryptedResponse = response.body()!!
                            return EncryptedApiResponse(
                                encryptedResponse.encryptedPayload,
                                encryptedResponse.iv
                            )
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown error"
                            logger.e("Encrypted API call failed: ${response.code()} - $errorBody")
                            return ErrorResponse("Encrypted API call failed: ${response.code()} - $errorBody")
                        }
                    }
                }
            } catch (e: HttpException) {
                if (e.code() == 419) {
                    // Handle 419 in exception case as well
                    logger.w("Received 419 HttpException - Backend decryption failure, disabling encryption and falling back")
                    encryptionDisabledDueToFailure = true
                    val fallbackResult = fallbackCall()
                    return FallbackResponse(fallbackResult)
                }
                lastException = e
                logger.e("HTTP exception during encrypted API call, retrying", e)
            } catch (e: IOException) {
                lastException = e
                logger.e("IO exception during encrypted API call, retrying", e)
            } catch (e: Exception) {
                return ErrorResponse("Unexpected error during encrypted API call: ${e.message}")
            }

            retryCount++
            if (retryCount < MAX_RETRY_COUNT) {
                delay(INITIAL_RETRY_DELAY_MS * (1 shl retryCount))
            }
        }

        return ErrorResponse("Failed after $MAX_RETRY_COUNT retries: ${lastException?.message}")
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
                    delay(INITIAL_RETRY_DELAY_MS * (1 shl retryCount)) // TODO remove delay
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

    /**
     * Utility function to decrypt and parse encrypted API responses.
     *
     * This method:
     * 1. Decrypts the encrypted payload using the provided IV
     * 2. Parses the decrypted JSON into the specified response class
     *
     * @param encryptedPayload The encrypted response payload (Base64-encoded)
     * @param iv The initialization vector used for decryption (Base64-encoded)
     * @param responseClass The class to parse the decrypted JSON into
     * @return The parsed response object, or null if decryption or parsing fails
     */
    private fun <T> decryptAndParseResponse(
        encryptedPayload: String,
        iv: String,
        responseClass: Class<T>
    ): T? {
        val decryptionResult = encryptionManager.decrypt(encryptedPayload, iv)

        if (decryptionResult !is DecryptionSuccess) {
            logger.e("Failed to decrypt response")
            return null
        }

        return try {
            val gson = com.google.gson.Gson()
            gson.fromJson(decryptionResult.data, responseClass)
        } catch (e: Exception) {
            logger.e("Failed to parse decrypted response", e)
            null
        }
    }
}