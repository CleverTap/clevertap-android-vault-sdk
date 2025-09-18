package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.api.TokenizationApi
import com.clevertap.android.vault.sdk.encryption.DecryptionSuccess
import com.clevertap.android.vault.sdk.encryption.EncryptionManager
import com.clevertap.android.vault.sdk.encryption.EncryptionSuccess
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRequest
import com.clevertap.android.vault.sdk.model.BatchTokenizeRequest
import com.clevertap.android.vault.sdk.model.DetokenizeRequest
import com.clevertap.android.vault.sdk.model.EncryptedRequest
import com.clevertap.android.vault.sdk.model.EncryptedResponse
import com.clevertap.android.vault.sdk.model.TokenizeRequest
import com.clevertap.android.vault.sdk.util.VaultLogger
import com.google.gson.Gson
import retrofit2.Response

/**
 * Strategy interface for handling different encryption approaches
 */
interface EncryptionStrategy {
    suspend fun tokenize(
        api: TokenizationApi,
        accessToken: String,
        value: String
    ): Response<*>

    suspend fun detokenize(
        api: TokenizationApi,
        accessToken: String,
        token: String
    ): Response<*>

    suspend fun batchTokenize(
        api: TokenizationApi,
        accessToken: String,
        values: List<String>
    ): Response<*>

    suspend fun batchDetokenize(
        api: TokenizationApi,
        accessToken: String,
        tokens: List<String>
    ): Response<*>
}

/**
 * Strategy for operations without encryption
 */
class NoEncryptionStrategy(
    private val logger: VaultLogger
) : EncryptionStrategy {

    override suspend fun tokenize(
        api: TokenizationApi,
        accessToken: String,
        value: String
    ): Response<*> {
        logger.d("Making non-encrypted tokenize call")
        return api.tokenize("Bearer $accessToken", TokenizeRequest(value))
    }

    override suspend fun detokenize(
        api: TokenizationApi,
        accessToken: String,
        token: String
    ): Response<*> {
        logger.d("Making non-encrypted detokenize call")
        return api.detokenize("Bearer $accessToken", DetokenizeRequest(token))
    }

    override suspend fun batchTokenize(
        api: TokenizationApi,
        accessToken: String,
        values: List<String>
    ): Response<*> {
        logger.d("Making non-encrypted batch tokenize call")
        return api.batchTokenize("Bearer $accessToken", BatchTokenizeRequest(values))
    }

    override suspend fun batchDetokenize(
        api: TokenizationApi,
        accessToken: String,
        tokens: List<String>
    ): Response<*> {
        logger.d("Making non-encrypted batch detokenize call")
        return api.batchDetokenize("Bearer $accessToken", BatchDetokenizeRequest(tokens))
    }
}

/**
 * Strategy for operations with encryption over transit
 */
class WithEncryptionStrategy(
    private val encryptionManager: EncryptionManager,
    private val logger: VaultLogger,
    private val fallbackStrategy: NoEncryptionStrategy,
    private val gson: Gson = Gson()
) : EncryptionStrategy {

    @Volatile
    private var encryptionDisabledDueToFailure = false

    override suspend fun tokenize(
        api: TokenizationApi,
        accessToken: String,
        value: String
    ): Response<*> {
        if (encryptionDisabledDueToFailure || !encryptionManager.isEnabled()) {
            logger.d("Encryption is disabled or failed, falling back to non-encrypted tokenization")
            return fallbackStrategy.tokenize(api, accessToken, value)
        }

        logger.d("Making encrypted tokenize call")

        // FALLBACK 1: If encryption of request fails
        val encryptedRequest = createEncryptedRequest(TokenizeRequest(value))
        if (encryptedRequest == null) {
            logger.e("Encryption failed, falling back to non-encrypted tokenization")
            return fallbackStrategy.tokenize(api, accessToken, value)
        }

        // FALLBACK 2: If API call fails (especially 419 error)
        return executeWithRetryForEncryption(
            encryptedCall = {
                api.tokenizeEncrypted("Bearer $accessToken", true, encryptedRequest)
            },
            fallbackCall = {
                fallbackStrategy.tokenize(api, accessToken, value)
            }
        )
    }

    override suspend fun detokenize(
        api: TokenizationApi,
        accessToken: String,
        token: String
    ): Response<*> {
        if (encryptionDisabledDueToFailure || !encryptionManager.isEnabled()) {
            logger.d("Encryption is disabled or failed, falling back to non-encrypted detokenization")
            return fallbackStrategy.detokenize(api, accessToken, token)
        }

        logger.d("Making encrypted detokenize call")

        // FALLBACK 1: If encryption of request fails
        val encryptedRequest = createEncryptedRequest(DetokenizeRequest(token))
        if (encryptedRequest == null) {
            logger.e("Encryption failed, falling back to non-encrypted detokenization")
            return fallbackStrategy.detokenize(api, accessToken, token)
        }

        // FALLBACK 2: If API call fails (especially 419 error)
        return executeWithRetryForEncryption(
            encryptedCall = {
                api.detokenizeEncrypted("Bearer $accessToken", true, encryptedRequest)
            },
            fallbackCall = {
                fallbackStrategy.detokenize(api, accessToken, token)
            }
        )
    }

    override suspend fun batchTokenize(
        api: TokenizationApi,
        accessToken: String,
        values: List<String>
    ): Response<*> {
        if (encryptionDisabledDueToFailure || !encryptionManager.isEnabled()) {
            logger.d("Encryption is disabled or failed, falling back to non-encrypted batch tokenization")
            return fallbackStrategy.batchTokenize(api, accessToken, values)
        }

        logger.d("Making encrypted batch tokenize call")

        // FALLBACK 1: If encryption of request fails
        val encryptedRequest = createEncryptedRequest(BatchTokenizeRequest(values))
        if (encryptedRequest == null) {
            logger.e("Encryption failed, falling back to non-encrypted batch tokenization")
            return fallbackStrategy.batchTokenize(api, accessToken, values)
        }

        // FALLBACK 2: If API call fails (especially 419 error)
        return executeWithRetryForEncryption(
            encryptedCall = {
                api.batchTokenizeEncrypted("Bearer $accessToken", true, encryptedRequest)
            },
            fallbackCall = {
                fallbackStrategy.batchTokenize(api, accessToken, values)
            }
        )
    }

    override suspend fun batchDetokenize(
        api: TokenizationApi,
        accessToken: String,
        tokens: List<String>
    ): Response<*> {
        if (encryptionDisabledDueToFailure || !encryptionManager.isEnabled()) {
            logger.d("Encryption is disabled or failed, falling back to non-encrypted batch detokenization")
            return fallbackStrategy.batchDetokenize(api, accessToken, tokens)
        }

        logger.d("Making encrypted batch detokenize call")

        // FALLBACK 1: If encryption of request fails
        val encryptedRequest = createEncryptedRequest(BatchDetokenizeRequest(tokens))
        if (encryptedRequest == null) {
            logger.e("Encryption failed, falling back to non-encrypted batch detokenization")
            return fallbackStrategy.batchDetokenize(api, accessToken, tokens)
        }

        // FALLBACK 2: If API call fails (especially 419 error)
        return executeWithRetryForEncryption(
            encryptedCall = {
                api.batchDetokenizeEncrypted("Bearer $accessToken", true, encryptedRequest)
            },
            fallbackCall = {
                fallbackStrategy.batchDetokenize(api, accessToken, tokens)
            }
        )
    }

    /**
     * Executes an encrypted API call with fallback to non-encrypted version on 419 error.
     * This is the CORE FALLBACK MECHANISM that handles backend decryption failures.
     */
    private suspend fun executeWithRetryForEncryption(
        encryptedCall: suspend () -> Response<EncryptedResponse>,
        fallbackCall: suspend () -> Response<*>
    ): Response<*> {
        return try {
            val response = encryptedCall()

            when (response.code()) {
                419 -> {
                    logger.w("Received 419 - Backend decryption failure, disabling encryption and falling back to non-encrypted requests")
                    encryptionDisabledDueToFailure = true
                    fallbackCall() // This calls the fallback strategy
                }

                else -> response
            }
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 419) {
                logger.w("Received 419 HttpException - Backend decryption failure, disabling encryption and falling back")
                encryptionDisabledDueToFailure = true
                fallbackCall()
            } else {
                throw e // Let other HTTP exceptions bubble up
            }
        }
    }

    /**
     * Creates an encrypted request from any request object
     */
    private fun createEncryptedRequest(request: Any): EncryptedRequest? {
        val requestJson = gson.toJson(request)
        logger.d("request before encryption: $requestJson")
        val encryptionResult = encryptionManager.encrypt(requestJson)

        return if (encryptionResult is EncryptionSuccess) {
            EncryptedRequest(
                itp = encryptionResult.encryptedPayload,
                itk = encryptionResult.sessionKey,
                itv = encryptionResult.iv
            )
        } else {
            logger.e("Encryption failed: $encryptionResult")
            null
        }
    }

    /**
     * Decrypts an encrypted response into the specified type
     */
    fun <T> decryptResponse(response: EncryptedResponse, responseClass: Class<T>): T? {
        val decryptionResult = encryptionManager.decrypt(response.itp, response.itv)

        return if (decryptionResult is DecryptionSuccess) {
            try {
                gson.fromJson(decryptionResult.data, responseClass)
            } catch (e: Exception) {
                logger.e("Failed to parse decrypted response", e)
                null
            }
        } else {
            logger.e("Failed to decrypt response")
            null
        }
    }

    private fun createErrorResponse(message: String): Response<Nothing> {
        return Response.error(500, okhttp3.ResponseBody.create(null, message))
    }
}

/**
 * Factory for creating appropriate encryption strategy with fallback support
 */
class EncryptionStrategyFactory {
    fun createStrategy(
        encryptionManager: EncryptionManager,
        logger: VaultLogger
    ): EncryptionStrategy {
        return if (encryptionManager.isEnabled()) {
            val fallbackStrategy = NoEncryptionStrategy(logger)
            WithEncryptionStrategy(encryptionManager, logger, fallbackStrategy)
        } else {
            NoEncryptionStrategy(logger)
        }
    }
}