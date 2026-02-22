package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.encryption.EncryptionManager
import com.clevertap.android.zeropii.sdk.model.EncryptedResponse
import com.clevertap.android.zeropii.sdk.model.TokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.TokenizeResponse
import com.clevertap.android.zeropii.sdk.network.NetworkProvider
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger
import retrofit2.Response

/**
 * Concrete implementation for single value tokenization operations
 */
class SingleTokenizeOperation(
    authRepository: AuthRepository,
    networkProvider: NetworkProvider,
    encryptionManager: EncryptionManager,
    logger: ZeroPiiLogger,
    retryHandler: RetryHandler,
    responseProcessor: ResponseProcessor,
    private val encryptionStrategy: EncryptionStrategy
) : BaseTokenOperation<String, TokenizeRepoResult>(
    authRepository, networkProvider, encryptionManager, logger,
    retryHandler, responseProcessor
) {

    override fun getOperationType(): String = "Single Tokenize"

    override fun processResponse(response: Response<*>): TokenizeRepoResult {
        return when {
            // Handle encrypted response
            response.body() is EncryptedResponse -> {
                processEncryptedResponse(response as Response<EncryptedResponse>)
            }
            // Handle regular response
            response.body() is TokenizeResponse -> {
                responseProcessor.processTokenizeResponse(response)
            }
            // Handle error
            else -> {
                val errorMessage = createErrorResponse(response, getOperationType())
                logger.e(errorMessage)
                createErrorResult(message = errorMessage, httpStatusCode = response.code())
            }
        }
    }

    override suspend fun makeApiCall(request: String, accessToken: String): Response<*> {
        val api = networkProvider.tokenizationApi
        return encryptionStrategy.tokenize(api, accessToken, request)
    }

    override fun createErrorResult(message: String, httpStatusCode: Int?): TokenizeRepoResult {
        return TokenizeRepoResult.Error(message = message, httpStatusCode = httpStatusCode)
    }

    private fun processEncryptedResponse(response: Response<EncryptedResponse>): TokenizeRepoResult {
        if (!isSuccessfulResponse(response)) {
            return createErrorResult(message = createErrorResponse(response, getOperationType()), httpStatusCode = response.code())
        }

        val encryptedResponse = response.body()!!
        val encryptionStrategy = this.encryptionStrategy as? WithEncryptionStrategy
            ?: return createErrorResult("Encryption strategy mismatch")

        val decryptedResponse =
            encryptionStrategy.decryptResponse(encryptedResponse, TokenizeResponse::class.java)
                ?: return createErrorResult("Failed to decrypt response")

        return TokenizeRepoResult.Success(
            token = decryptedResponse.token,
            exists = decryptedResponse.exists,
            newlyCreated = decryptedResponse.newlyCreated,
            dataType = decryptedResponse.dataType
        )
    }
}
