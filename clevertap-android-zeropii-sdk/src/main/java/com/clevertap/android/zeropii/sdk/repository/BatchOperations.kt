package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.encryption.EncryptionManager
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeResponse
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeSummary
import com.clevertap.android.zeropii.sdk.model.EncryptedResponse
import com.clevertap.android.zeropii.sdk.network.NetworkProvider
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger
import retrofit2.Response

/**
 * Concrete implementation for batch tokenization operations
 */
class BatchTokenizeOperation(
    authRepository: AuthRepository,
    networkProvider: NetworkProvider,
    encryptionManager: EncryptionManager,
    logger: ZeroPiiLogger,
    retryHandler: RetryHandler,
    responseProcessor: ResponseProcessor,
    private val encryptionStrategy: EncryptionStrategy
) : BaseTokenOperation<List<String>, BatchTokenizeRepoResult>(
    authRepository, networkProvider, encryptionManager, logger,
    retryHandler, responseProcessor
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

    override fun processResponse(response: Response<*>): BatchTokenizeRepoResult {
        return when {
            // Handle encrypted response
            response.body() is EncryptedResponse -> {
                processEncryptedResponse(response as Response<EncryptedResponse>)
            }
            // Handle regular response
            response.body() is BatchTokenizeResponse -> {
                responseProcessor.processBatchTokenizeResponse(response)
            }
            // Handle error
            else -> {
                val errorMessage = createErrorResponse(response, getOperationType())
                logger.e(errorMessage)
                createErrorResult(message = errorMessage, httpStatusCode = response.code())
            }
        }
    }

    override suspend fun makeApiCall(request: List<String>, accessToken: String): Response<*> {
        val api = networkProvider.tokenizationApi
        return encryptionStrategy.batchTokenize(api, accessToken, request)
    }

    override fun createErrorResult(message: String, httpStatusCode: Int?): BatchTokenizeRepoResult {
        return BatchTokenizeRepoResult.Error(message = message, httpStatusCode = httpStatusCode)
    }

    private fun processEncryptedResponse(
        response: Response<EncryptedResponse>
    ): BatchTokenizeRepoResult {
        if (!isSuccessfulResponse(response)) {
            return createErrorResult(message = createErrorResponse(response, getOperationType()), httpStatusCode = response.code())
        }

        val encryptedResponse = response.body()!!
        val encryptionStrategy = this.encryptionStrategy as? WithEncryptionStrategy
            ?: return createErrorResult("Encryption strategy mismatch")

        val decryptedResponse =
            encryptionStrategy.decryptResponse(encryptedResponse, BatchTokenizeResponse::class.java)
                ?: return createErrorResult("Failed to decrypt response")

        val results = decryptedResponse.results
        return BatchTokenizeRepoResult.Success(
            results = results,
            summary = BatchTokenizeSummary(
                processedCount = results.size,
                existingCount = results.count { it.exists },
                newlyCreatedCount = results.count { it.newlyCreated }
            )
        )
    }
}
