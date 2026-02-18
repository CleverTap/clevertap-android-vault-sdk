package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.encryption.EncryptionManager
import com.clevertap.android.zeropii.sdk.network.NetworkProvider
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger
import retrofit2.Response

/**
 * Abstract base class implementing the Template Method pattern for token operations.
 *
 * This class defines the common algorithm structure for all token operations:
 * 1. Validate the request
 * 2. Make API call with retry logic
 * 3. Process the API response
 * 4. Return result
 *
 * Subclasses implement specific steps for their operation type.
 */
abstract class BaseTokenOperation<TRequest, TResponse>(
    protected val authRepository: AuthRepository,
    protected val networkProvider: NetworkProvider,
    protected val encryptionManager: EncryptionManager,
    protected val logger: ZeroPiiLogger,
    private val retryHandler: RetryHandler,
    protected val responseProcessor: ResponseProcessor
) {

    /**
     * Template method defining the algorithm structure.
     * This is the main entry point that orchestrates the operation flow.
     */
    suspend fun execute(request: TRequest): TResponse {
        logger.d("Starting ${getOperationType()} operation")

        return try {
            // Step 1: Validate request
            validateRequest(request)

            // Step 2: Execute API call with retry logic
            val apiResponse = retryHandler.executeWithRetry {
                val accessToken = authRepository.getAccessToken()
                makeApiCall(request, accessToken)
            }

            // Step 3: Process response
            val result = processResponse(apiResponse)
            logger.d("${getOperationType()} operation completed successfully")
            result

        } catch (e: Exception) {
            logger.e("Error during ${getOperationType()}", e)
            createErrorResult("Error during ${getOperationType()}: ${e.message}")
        }
    }

    // Abstract methods to be implemented by subclasses
    internal abstract fun processResponse(response: Response<*>): TResponse
    protected abstract fun getOperationType(): String
    internal open fun validateRequest(request: TRequest) {}
    internal abstract suspend fun makeApiCall(request: TRequest, accessToken: String): Response<*>
    internal abstract fun createErrorResult(message: String): TResponse

    // Helper methods available to subclasses

    protected fun isSuccessfulResponse(response: Response<*>): Boolean {
        return response.isSuccessful && response.body() != null
    }

    protected fun createErrorResponse(response: Response<*>, operation: String): String {
        val errorBody = response.errorBody()?.string() ?: "Unknown error"
        return "$operation failed: ${response.code()} - $errorBody"
    }

    protected fun handleException(e: Exception, operation: String): String {
        logger.e("Error during $operation", e)
        return "Error during $operation: ${e.message}"
    }
}
