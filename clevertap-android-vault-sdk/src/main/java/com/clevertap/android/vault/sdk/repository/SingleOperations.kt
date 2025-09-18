package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.cache.TokenCache
import com.clevertap.android.vault.sdk.encryption.EncryptionManager
import com.clevertap.android.vault.sdk.model.DetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.DetokenizeResponse
import com.clevertap.android.vault.sdk.model.EncryptedResponse
import com.clevertap.android.vault.sdk.model.TokenizeRepoResult
import com.clevertap.android.vault.sdk.model.TokenizeResponse
import com.clevertap.android.vault.sdk.network.NetworkProvider
import com.clevertap.android.vault.sdk.util.VaultLogger
import retrofit2.Response

/**
 * Concrete implementation for single value tokenization operations
 */
class SingleTokenizeOperation(
    tokenCache: TokenCache,
    authRepository: AuthRepository,
    networkProvider: NetworkProvider,
    encryptionManager: EncryptionManager,
    logger: VaultLogger,
    retryHandler: RetryHandler,
    cacheManager: CacheManager,
    responseProcessor: ResponseProcessor,
    private val encryptionStrategy: EncryptionStrategy
) : BaseTokenOperation<String, TokenizeRepoResult>(
    tokenCache, authRepository, networkProvider, encryptionManager, logger,
    retryHandler, cacheManager, responseProcessor
) {

    override fun getOperationType(): String = "Single Tokenize"

    override fun checkCacheWithState(request: String): CacheCheckResult<String, TokenizeRepoResult> {
        val cacheResult = cacheManager.getTokenFromCache(request)

        return if (cacheResult is CacheResult.TokenResult) {
            val result = TokenizeRepoResult.Success(
                token = cacheResult.token,
                exists = true,
                newlyCreated = false,
                dataType = cacheResult.dataType
            )
            CacheCheckResult.CompleteFromCache(request, result)
        } else {
            CacheCheckResult.NothingFromCache(request, request)
        }
    }

    override fun processResponseWithCacheData(
        response: Response<*>,
        cacheResult: CacheCheckResult<String, TokenizeRepoResult>
    ): TokenizeRepoResult {
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
                createErrorResult(errorMessage)
            }
        }
    }

    override suspend fun makeApiCall(request: String, accessToken: String): Response<*> {
        val api = networkProvider.tokenizationApi
        return encryptionStrategy.tokenize(api, accessToken, request)
    }

    override fun updateCache(request: String, result: TokenizeRepoResult) {
        if (result is TokenizeRepoResult.Success) {
            cacheManager.storeTokenInCache(request, result.token, result.dataType)
        }
    }

    override fun createErrorResult(message: String): TokenizeRepoResult {
        return TokenizeRepoResult.Error(message)
    }

    private fun processEncryptedResponse(response: Response<EncryptedResponse>): TokenizeRepoResult {
        if (!isSuccessfulResponse(response)) {
            return createErrorResult(createErrorResponse(response, getOperationType()))
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

/**
 * Concrete implementation for single token detokenization operations
 */
class SingleDetokenizeOperation(
    tokenCache: TokenCache,
    authRepository: AuthRepository,
    networkProvider: NetworkProvider,
    encryptionManager: EncryptionManager,
    logger: VaultLogger,
    retryHandler: RetryHandler,
    cacheManager: CacheManager,
    responseProcessor: ResponseProcessor,
    private val encryptionStrategy: EncryptionStrategy
) : BaseTokenOperation<String, DetokenizeRepoResult>(
    tokenCache, authRepository, networkProvider, encryptionManager, logger,
    retryHandler, cacheManager, responseProcessor
) {

    override fun getOperationType(): String = "Single Detokenize"

    override fun checkCacheWithState(request: String): CacheCheckResult<String, DetokenizeRepoResult> {
        val cacheResult = cacheManager.getValueFromCache(request)
        return if (cacheResult is CacheResult.ValueResult) {
            val result = DetokenizeRepoResult.Success(
                value = cacheResult.value,
                exists = true,
                dataType = cacheResult.dataType
            )
            CacheCheckResult.CompleteFromCache(
                originalRequest = request,
                result = result
            )
        } else {
            CacheCheckResult.NothingFromCache(
                originalRequest = request,
                uncachedRequest = request
            )
        }
    }

    override fun processResponseWithCacheData(
        response: Response<*>,
        cacheResult: CacheCheckResult<String, DetokenizeRepoResult>
    ): DetokenizeRepoResult {

        return when {
            // Handle encrypted response
            response.body() is EncryptedResponse -> {
                processEncryptedResponse(response as Response<EncryptedResponse>)
            }
            // Handle regular response
            response.body() is DetokenizeResponse -> {
                responseProcessor.processDetokenizeResponse(response)
            }
            // Handle error
            else -> {
                val errorMessage = createErrorResponse(response, getOperationType())
                logger.e(errorMessage)
                createErrorResult(errorMessage)
            }
        }
    }

    override suspend fun makeApiCall(request: String, accessToken: String): Response<*> {
        val api = networkProvider.tokenizationApi
        return encryptionStrategy.detokenize(api, accessToken, request)
    }

    override fun updateCache(request: String, result: DetokenizeRepoResult) {
        if (result is DetokenizeRepoResult.Success && result.exists && result.value != null) {
            cacheManager.storeValueInCache(request, result.value, result.dataType)
        }
    }

    override fun createErrorResult(message: String): DetokenizeRepoResult {
        return DetokenizeRepoResult.Error(message)
    }

    private fun processEncryptedResponse(response: Response<EncryptedResponse>): DetokenizeRepoResult {
        if (!isSuccessfulResponse(response)) {
            return createErrorResult(createErrorResponse(response, getOperationType()))
        }

        val encryptedResponse = response.body()!!
        val encryptionStrategy = this.encryptionStrategy as? WithEncryptionStrategy
            ?: return createErrorResult("Encryption strategy mismatch")

        val decryptedResponse =
            encryptionStrategy.decryptResponse(encryptedResponse, DetokenizeResponse::class.java)
                ?: return createErrorResult("Failed to decrypt response")

        return DetokenizeRepoResult.Success(
            value = decryptedResponse.value,
            exists = decryptedResponse.exists,
            dataType = decryptedResponse.dataType
        )
    }
}