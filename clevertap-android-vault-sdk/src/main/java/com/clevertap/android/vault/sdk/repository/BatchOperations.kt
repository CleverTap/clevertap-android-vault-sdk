package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.cache.TokenCache
import com.clevertap.android.vault.sdk.encryption.EncryptionManager
import com.clevertap.android.vault.sdk.model.BatchDetokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeSummary
import com.clevertap.android.vault.sdk.model.BatchTokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeSummary
import com.clevertap.android.vault.sdk.model.EncryptedResponse
import com.clevertap.android.vault.sdk.network.NetworkProvider
import com.clevertap.android.vault.sdk.util.VaultLogger
import retrofit2.Response

/**
 * Concrete implementation for batch tokenization operations
 */
class BatchTokenizeOperation(
    tokenCache: TokenCache,
    authRepository: AuthRepository,
    networkProvider: NetworkProvider,
    encryptionManager: EncryptionManager,
    logger: VaultLogger,
    retryHandler: RetryHandler,
    cacheManager: CacheManager,
    responseProcessor: ResponseProcessor,
    private val encryptionStrategy: EncryptionStrategy
) : BaseTokenOperation<List<String>, BatchTokenizeRepoResult>(
    tokenCache, authRepository, networkProvider, encryptionManager, logger,
    retryHandler, cacheManager, responseProcessor
) {

    companion object {
        private const val MAX_BATCH_SIZE = 1000
    }

    override fun getOperationType(): String = "Batch Tokenize"

    override fun validateRequest(request: List<String>) {
        if (request.isEmpty()) {
            throw IllegalArgumentException("Batch tokenize request contains no values")
        }
        if (request.size > MAX_BATCH_SIZE) {
            throw IllegalArgumentException("Batch size exceeds the maximum limit of $MAX_BATCH_SIZE values")
        }
    }

    override fun checkCacheWithState(
        request: List<String>
    ): CacheCheckResult<List<String>, BatchTokenizeRepoResult> {

        val batchCacheResult = cacheManager.getBatchTokensFromCache(request)
        val batchState = createBatchTokenizeStateFromCacheResult(request, batchCacheResult)

        return if (batchState.isComplete) {
            CacheCheckResult.CompleteFromCache(
                originalRequest = request,
                result = batchState.createCompleteResult()
            )
        } else {
            CacheCheckResult.PartialFromCache(
                originalRequest = request,
                cachedItems = batchState.cachedResults,
                uncachedRequest = batchState.uncachedValues
            )
        }
    }

    override fun processResponseWithCacheData(
        response: Response<*>,
        cacheResult: CacheCheckResult<List<String>, BatchTokenizeRepoResult>
    ): BatchTokenizeRepoResult {

        // Extract cached data from the explicit parameter
        val cachedResults = when (cacheResult) {
            is CacheCheckResult.PartialFromCache ->
                cacheResult.cachedItems as List<BatchTokenItemResponse>

            else -> emptyList()
        }

        return when {
            // Handle encrypted response
            response.body() is EncryptedResponse -> {
                processEncryptedResponse(
                    response as Response<EncryptedResponse>,
                    cachedResults
                )
            }
            // Handle regular response
            response.body() is BatchTokenizeResponse -> {
                responseProcessor.processBatchTokenizeResponse(response, cachedResults)
            }
            // Handle error
            else -> {
                val errorMessage = createErrorResponse(response, getOperationType())
                logger.e(errorMessage)
                createErrorResult(errorMessage)
            }
        }
    }

    override suspend fun makeApiCall(request: List<String>, accessToken: String): Response<*> {
        val api = networkProvider.tokenizationApi
        return encryptionStrategy.batchTokenize(api, accessToken, request)
    }

    override fun updateCache(request: List<String>, result: BatchTokenizeRepoResult) {
        if (result is BatchTokenizeRepoResult.Success) {
            cacheManager.storeBatchTokensInCache(result.results)
        }
    }

    override fun createErrorResult(message: String): BatchTokenizeRepoResult {
        return BatchTokenizeRepoResult.Error(message)
    }

    private fun processEncryptedResponse(
        response: Response<EncryptedResponse>,
        cachedResults: List<BatchTokenItemResponse>
    ): BatchTokenizeRepoResult {
        if (!isSuccessfulResponse(response)) {
            return createErrorResult(createErrorResponse(response, getOperationType()))
        }

        val encryptedResponse = response.body()!!
        val encryptionStrategy = this.encryptionStrategy as? WithEncryptionStrategy
            ?: return createErrorResult("Encryption strategy mismatch")

        val decryptedResponse =
            encryptionStrategy.decryptResponse(encryptedResponse, BatchTokenizeResponse::class.java)
                ?: return createErrorResult("Failed to decrypt response")

        // Combine cached and new results
        val allResults = cachedResults + decryptedResponse.results

        return BatchTokenizeRepoResult.Success(
            results = allResults,
            summary = BatchTokenizeSummary(
                processedCount = allResults.size,
                existingCount = allResults.count { it.exists },
                newlyCreatedCount = allResults.count { it.newlyCreated }
            )
        )
    }

    private fun createBatchTokenizeStateFromCacheResult(
        originalValues: List<String>,
        cacheResult: BatchCacheResult
    ): BatchTokenizeState {
        return BatchTokenizeState(
            originalValues = originalValues,
            cachedResults = cacheResult.cached as List<BatchTokenItemResponse>,
            uncachedValues = cacheResult.uncached
        )
    }
}

/**
 * Concrete implementation for batch detokenization operations
 */
class BatchDetokenizeOperation(
    tokenCache: TokenCache,
    authRepository: AuthRepository,
    networkProvider: NetworkProvider,
    encryptionManager: EncryptionManager,
    logger: VaultLogger,
    retryHandler: RetryHandler,
    cacheManager: CacheManager,
    responseProcessor: ResponseProcessor,
    private val encryptionStrategy: EncryptionStrategy
) : BaseTokenOperation<List<String>, BatchDetokenizeRepoResult>(
    tokenCache, authRepository, networkProvider, encryptionManager, logger,
    retryHandler, cacheManager, responseProcessor
) {

    companion object {
        private const val MAX_BATCH_SIZE = 10000
    }

    override fun getOperationType(): String = "Batch Detokenize"

    override fun validateRequest(request: List<String>) {
        if (request.isEmpty()) {
            throw IllegalArgumentException("Batch detokenize request contains no tokens")
        }
        if (request.size > MAX_BATCH_SIZE) {
            throw IllegalArgumentException("Batch size exceeds the maximum limit of $MAX_BATCH_SIZE tokens")
        }
    }

    override fun checkCacheWithState(
        request: List<String>
    ): CacheCheckResult<List<String>, BatchDetokenizeRepoResult> {

        val batchCacheResult = cacheManager.getBatchValuesFromCache(request)
        val batchState = createBatchDetokenizeStateFromCacheResult(request, batchCacheResult)

        return if (batchState.isComplete) {
            CacheCheckResult.CompleteFromCache(
                originalRequest = request,
                result = batchState.createCompleteResult()
            )
        } else {
            CacheCheckResult.PartialFromCache(
                originalRequest = request,
                cachedItems = batchState.cachedResults,
                uncachedRequest = batchState.uncachedTokens
            )
        }
    }


    override fun processResponseWithCacheData(
        response: Response<*>,
        cacheResult: CacheCheckResult<List<String>, BatchDetokenizeRepoResult>
    ): BatchDetokenizeRepoResult {

        val cachedResults = when (cacheResult) {
            is CacheCheckResult.PartialFromCache ->
                cacheResult.cachedItems as List<BatchDetokenItemResponse>

            else -> emptyList()
        }

        return when {
            // Handle encrypted response
            response.body() is EncryptedResponse -> {
                processEncryptedResponse(
                    response as Response<EncryptedResponse>,
                    cachedResults
                )
            }
            // Handle regular response
            response.body() is BatchDetokenizeResponse -> {
                responseProcessor.processBatchDetokenizeResponse(response, cachedResults)
            }
            // Handle error
            else -> {
                val errorMessage = createErrorResponse(response, getOperationType())
                logger.e(errorMessage)
                createErrorResult(errorMessage)
            }
        }
    }

    override suspend fun makeApiCall(request: List<String>, accessToken: String): Response<*> {
        val api = networkProvider.tokenizationApi
        return encryptionStrategy.batchDetokenize(api, accessToken, request)
    }

    override fun updateCache(request: List<String>, result: BatchDetokenizeRepoResult) {
        if (result is BatchDetokenizeRepoResult.Success) {
            cacheManager.storeBatchValuesInCache(result.results)
        }
    }

    override fun createErrorResult(message: String): BatchDetokenizeRepoResult {
        return BatchDetokenizeRepoResult.Error(message)
    }

    private fun processEncryptedResponse(
        response: Response<EncryptedResponse>,
        cachedResults: List<BatchDetokenItemResponse>
    ): BatchDetokenizeRepoResult {
        if (!isSuccessfulResponse(response)) {
            return createErrorResult(createErrorResponse(response, getOperationType()))
        }

        val encryptedResponse = response.body()!!
        val encryptionStrategy = this.encryptionStrategy as? WithEncryptionStrategy
            ?: return createErrorResult("Encryption strategy mismatch")

        val decryptedResponse = encryptionStrategy.decryptResponse(
            encryptedResponse,
            BatchDetokenizeResponse::class.java
        ) ?: return createErrorResult("Failed to decrypt response")

        // Combine cached and new results
        val allResults = cachedResults + decryptedResponse.results

        return BatchDetokenizeRepoResult.Success(
            results = allResults,
            summary = BatchDetokenizeSummary(
                processedCount = allResults.size,
                foundCount = allResults.count { it.exists },
                notFoundCount = allResults.count { !it.exists }
            )
        )
    }

    private fun createBatchDetokenizeStateFromCacheResult(
        originalTokens: List<String>,
        cacheResult: BatchCacheResult
    ): BatchDetokenizeState {
        return BatchDetokenizeState(
            originalTokens = originalTokens,
            cachedResults = cacheResult.cached as List<BatchDetokenItemResponse>,
            uncachedTokens = cacheResult.uncached
        )
    }
}