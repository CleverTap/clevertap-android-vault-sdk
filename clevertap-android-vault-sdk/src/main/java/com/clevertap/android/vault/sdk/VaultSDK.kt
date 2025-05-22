package com.clevertap.android.vault.sdk

import com.clevertap.android.vault.sdk.cache.TokenCache
import com.clevertap.android.vault.sdk.encryption.EncryptionManager
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeResult
import com.clevertap.android.vault.sdk.model.DetokenizeResult
import com.clevertap.android.vault.sdk.model.TokenizeResult
import com.clevertap.android.vault.sdk.network.NetworkProvider
import com.clevertap.android.vault.sdk.repository.AuthRepository
import com.clevertap.android.vault.sdk.repository.AuthRepositoryImpl
import com.clevertap.android.vault.sdk.repository.TokenRepository
import com.clevertap.android.vault.sdk.repository.TokenRepositoryImpl
import com.clevertap.android.vault.sdk.util.VaultLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The main entry point for the Vault SDK, which provides tokenization services
 * for sensitive data like PII (Personally Identifiable Information).
 */
class VaultSDK private constructor(
    private val clientId: String,
    private val clientSecret: String,
    private val apiUrl: String,
    private val authUrl: String,
    private val enableEncryption: Boolean,
    private val enableCache: Boolean,
    private val debugMode: Boolean
) {
    private val sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var tokenRepository: TokenRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var encryptionManager: EncryptionManager
    private lateinit var tokenCache: TokenCache
    private lateinit var logger: VaultLogger

    init {
        initialize()
    }

    private fun initialize() {
        logger = VaultLogger(debugMode)

        logger.d("Initializing VaultSDK")

        // Create network provider
        val networkProvider = NetworkProvider(apiUrl, authUrl)

        // Setup encryption manager if enabled
        encryptionManager = EncryptionManager(enableEncryption, logger)

        // Initialize token cache if enabled
        tokenCache = TokenCache(enableCache)

        // Create repositories
        authRepository = AuthRepositoryImpl(
            networkProvider,
            clientId,
            clientSecret,
            logger
        )

        tokenRepository = TokenRepositoryImpl(
            networkProvider,
            authRepository,
            encryptionManager,
            tokenCache,
            logger
        )

        logger.d("VaultSDK initialization complete")
    }

    /**
     * Tokenizes a single sensitive value, replacing it with a format-preserving token.
     * If encryption is enabled in the SDK configuration, it will use encryption over transit.
     *
     * @param value The sensitive value to tokenize
     * @param callback The callback to receive the result
     */
    fun tokenize(value: String, callback: (TokenizeResult) -> Unit) {
        sdkScope.launch {
            try {
                logger.d("Tokenizing single value")
                val result = if (enableEncryption) {
                    tokenRepository.tokenizeWithEncryptionOverTransit(value)
                } else {
                    tokenRepository.tokenize(value)
                }
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            } catch (e: Exception) {
                logger.e("Error tokenizing value", e)
                withContext(Dispatchers.Main) {
                    callback(TokenizeResult.Error(e.message ?: "Unknown error occurred"))
                }
            }
        }
    }

    /**
     * Retrieves the original value for a given token.
     * If encryption is enabled in the SDK configuration, it will use encryption over transit.
     *
     * @param token The token to detokenize
     * @param callback The callback to receive the result
     */
    fun detokenize(token: String, callback: (DetokenizeResult) -> Unit) {
        sdkScope.launch {
            try {
                logger.d("Detokenizing single token")
                val result = if (enableEncryption) {
                    tokenRepository.detokenizeWithEncryptionOverTransit(token)
                } else {
                    tokenRepository.detokenize(token)
                }
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            } catch (e: Exception) {
                logger.e("Error detokenizing token", e)
                withContext(Dispatchers.Main) {
                    callback(DetokenizeResult.Error(e.message ?: "Unknown error occurred"))
                }
            }
        }
    }


    /**
     * Tokenizes multiple sensitive values in a single batch operation.
     * If encryption is enabled in the SDK configuration, it will use encryption over transit.
     *
     * @param values The list of sensitive values to tokenize
     * @param callback The callback to receive the result
     */
    fun batchTokenize(values: List<String>, callback: (BatchTokenizeResult) -> Unit) {
        sdkScope.launch {
            try {
                logger.d("Tokenizing batch of ${values.size} values")
                val result = if (enableEncryption) {
                    tokenRepository.batchTokenizeWithEncryptionOverTransit(values)
                } else {
                    tokenRepository.batchTokenize(values)
                }
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            } catch (e: Exception) {
                logger.e("Error batch tokenizing values", e)
                withContext(Dispatchers.Main) {
                    callback(BatchTokenizeResult.Error(e.message ?: "Unknown error occurred"))
                }
            }
        }
    }

    /**
     * Retrieves the original values for multiple tokens in a single batch operation.
     * If encryption is enabled in the SDK configuration, it will use encryption over transit.
     *
     * @param tokens The list of tokens to detokenize
     * @param callback The callback to receive the result
     */
    fun batchDetokenize(tokens: List<String>, callback: (BatchDetokenizeResult) -> Unit) {
        sdkScope.launch {
            try {
                logger.d("Detokenizing batch of ${tokens.size} tokens")
                val result = if (enableEncryption) {
                    tokenRepository.batchDetokenizeWithEncryptionOverTransit(tokens)
                } else {
                    tokenRepository.batchDetokenize(tokens)
                }
                withContext(Dispatchers.Main) {
                    callback(result)
                }
            } catch (e: Exception) {
                logger.e("Error batch detokenizing tokens", e)
                withContext(Dispatchers.Main) {
                    callback(BatchDetokenizeResult.Error(e.message ?: "Unknown error occurred"))
                }
            }
        }
    }

    /**
     * Clears the token cache.
     */
    fun clearCache() {
        logger.d("Clearing token cache")
        tokenCache.clear()
    }

    companion object {
        @Volatile
        private var INSTANCE: VaultSDK? = null

        /**
         * Initializes and returns an instance of the VaultSDK.
         *
         * @param clientId The OAuth2 client ID
         * @param clientSecret The OAuth2 client secret
         * @param apiUrl The base URL for the Vault API
         * @param authUrl The URL for the authentication service
         * @param enableEncryption Whether to enable encryption for API requests/responses
         * @param enableCache Whether to enable caching of token mappings
         * @param debugMode Whether to enable debug logging
         * @return The VaultSDK instance
         */
        @JvmStatic
        fun initialize(
            clientId: String,
            clientSecret: String,
            apiUrl: String,
            authUrl: String,
            enableEncryption: Boolean = true,
            enableCache: Boolean = true,
            debugMode: Boolean = false
        ): VaultSDK {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VaultSDK(
                    clientId,
                    clientSecret,
                    apiUrl,
                    authUrl,
                    enableEncryption,
                    enableCache,
                    debugMode
                ).also { INSTANCE = it }
            }
        }

        /**
         * Gets the VaultSDK instance.
         *
         * @return The VaultSDK instance
         * @throws IllegalStateException if not initialized
         */
        @JvmStatic
        fun getInstance(): VaultSDK {
            return INSTANCE ?: throw IllegalStateException(
                "VaultSDK is not initialized. Call initialize() first."
            )
        }
    }
}
