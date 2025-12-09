package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.cache.TokenCache
import com.clevertap.android.vault.sdk.encryption.EncryptionManager
import com.clevertap.android.vault.sdk.network.NetworkProvider
import com.clevertap.android.vault.sdk.util.VaultLogger
import retrofit2.Response

/**
 * Abstract base class implementing the Template Method pattern for token operations.
 *
 * This class defines the common algorithm structure for all token operations:
 * 1. Check cache for existing result
 * 2. If not cached, make API call with retry logic
 * 3. Process the API response
 * 4. Update cache with result
 * 5. Return final result
 *
 * Subclasses implement specific steps for their operation type.
 */
abstract class BaseTokenOperation<TRequest, TResponse>(
    protected val tokenCache: TokenCache,
    protected val authRepository: AuthRepository,
    protected val networkProvider: NetworkProvider,
    protected val encryptionManager: EncryptionManager,
    protected val logger: VaultLogger,
    private val retryHandler: RetryHandler,
    protected val cacheManager: CacheManager,
    protected val responseProcessor: ResponseProcessor
) {

    /**
     * Template method defining the algorithm structure.
     * This is the main entry point that orchestrates the operation flow.
     */
    suspend fun execute(request: TRequest): TResponse {
        logger.d("Starting ${getOperationType()} operation")

        return try {
            // Step 1: Check cache and get explicit state
            val cacheResult = checkCacheWithState(request)

            // Step 2: Return immediately if everything is cached
            if (cacheResult is CacheCheckResult.CompleteFromCache) {
                logger.d("All results found in cache for ${getOperationType()}")
                return cacheResult.result
            }

            // Step 3: Validate request
            validateRequest(request)

            // Step 4: Extract uncached request for API call
            val uncachedRequest = when (cacheResult) {
                is CacheCheckResult.PartialFromCache -> cacheResult.uncachedRequest
                is CacheCheckResult.NothingFromCache -> cacheResult.uncachedRequest
                else -> throw IllegalStateException("Unexpected cache result type")
            }

            // Step 5: Execute API call with retry logic
            val apiResponse = retryHandler.executeWithRetry {
                val accessToken = authRepository.getAccessToken()
                makeApiCall(uncachedRequest, accessToken)
            }

            // Step 6: Process response with explicit cache data
            val processedResult = processResponseWithCacheData(apiResponse, cacheResult)

            // Step 7: Update cache
            updateCache(request, processedResult)

            logger.d("${getOperationType()} operation completed successfully")
            processedResult

        } catch (e: Exception) {
            logger.e("Error during ${getOperationType()}", e)
            createErrorResult("Error during ${getOperationType()}: ${e.message}")
        }
    }

    // Abstract methods to be implemented by subclasses
    internal abstract fun checkCacheWithState(request: TRequest): CacheCheckResult<TRequest, TResponse>

    internal abstract fun processResponseWithCacheData(
        response: Response<*>,
        cacheResult: CacheCheckResult<TRequest, TResponse>
    ): TResponse
    protected abstract fun getOperationType(): String
    internal open fun validateRequest(request: TRequest) {}
    internal abstract suspend fun makeApiCall(request: TRequest, accessToken: String): retrofit2.Response<*>
    internal abstract fun updateCache(request: TRequest, result: TResponse)
    internal abstract fun createErrorResult(message: String): TResponse

    // Helper methods available to subclasses

    protected fun isSuccessfulResponse(response: retrofit2.Response<*>): Boolean {
        return response.isSuccessful && response.body() != null
    }

    protected fun createErrorResponse(response: retrofit2.Response<*>, operation: String): String {
        val errorBody = response.errorBody()?.string() ?: "Unknown error"
        return "$operation failed: ${response.code()} - $errorBody"
    }

    protected fun handleException(e: Exception, operation: String): String {
        logger.e("Error during $operation", e)
        return "Error during $operation: ${e.message}"
    }

}