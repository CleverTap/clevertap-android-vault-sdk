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
import com.clevertap.android.vault.sdk.repository.TokenRepositoryImpl
import com.clevertap.android.vault.sdk.repository.TokenRepository
import com.clevertap.android.vault.sdk.util.VaultLogger
import com.clevertap.android.vault.sdk.util.toPublicResult
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
    private val logLevel: Int
) {
    internal var sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    internal lateinit var tokenRepository: TokenRepository
    internal lateinit var authRepository: AuthRepository
    private lateinit var encryptionManager: EncryptionManager
    private lateinit var tokenCache: TokenCache
    private lateinit var logger: VaultLogger

    init {
        initialize()
    }

    private fun initialize() {
        logger = VaultLogger(logLevel)

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

        tokenRepository = TokenRepositoryImpl(networkProvider, authRepository, encryptionManager, tokenCache, logger)
        logger.d("VaultSDK initialization complete")
    }

    // ========================================
    // TOKENIZATION METHODS (Overloaded)
    // ========================================

    /**
     * Tokenizes a String value
     * @param value The String value to tokenize
     * @param callback The callback to receive the result
     */
    fun tokenize(value: String, callback: (TokenizeResult) -> Unit) {
        performTokenization(value, String::class.java, callback)
    }

    /**
     * Tokenizes an Int value
     * @param value The Int value to tokenize
     * @param callback The callback to receive the result
     */
    fun tokenize(value: Int, callback: (TokenizeResult) -> Unit) {
        performTokenization(value, Int::class.java, callback)
    }

    /**
     * Tokenizes a Long value
     * @param value The Long value to tokenize
     * @param callback The callback to receive the result
     */
    fun tokenize(value: Long, callback: (TokenizeResult) -> Unit) {
        performTokenization(value, Long::class.java, callback)
    }

    /**
     * Tokenizes a Float value
     * @param value The Float value to tokenize
     * @param callback The callback to receive the result
     */
    fun tokenize(value: Float, callback: (TokenizeResult) -> Unit) {
        performTokenization(value, Float::class.java, callback)
    }

    /**
     * Tokenizes a Double value
     * @param value The Double value to tokenize
     * @param callback The callback to receive the result
     */
    fun tokenize(value: Double, callback: (TokenizeResult) -> Unit) {
        performTokenization(value, Double::class.java, callback)
    }

    /**
     * Tokenizes a Boolean value
     * @param value The Boolean value to tokenize
     * @param callback The callback to receive the result
     */
    fun tokenize(value: Boolean, callback: (TokenizeResult) -> Unit) {
        performTokenization(value, Boolean::class.java, callback)
    }

    // ========================================
    // DETOKENIZATION METHODS (Type-Specific)
    // ========================================

    /**
     * Detokenizes a token and returns the result as String
     * @param token The token to detokenize
     * @param callback The callback to receive the result
     */
    fun deTokenizeAsString(token: String, callback: (DetokenizeResult<String>) -> Unit) {
        performDetokenization(token, String::class.java, callback)
    }

    /**
     * Detokenizes a token and returns the result as Int
     * @param token The token to detokenize
     * @param callback The callback to receive the result
     */
    fun deTokenizeAsInt(token: String, callback: (DetokenizeResult<Int>) -> Unit) {
        performDetokenization(token, Int::class.java, callback)
    }

    /**
     * Detokenizes a token and returns the result as Long
     * @param token The token to detokenize
     * @param callback The callback to receive the result
     */
    fun deTokenizeAsLong(token: String, callback: (DetokenizeResult<Long>) -> Unit) {
        performDetokenization(token, Long::class.java, callback)
    }

    /**
     * Detokenizes a token and returns the result as Float
     * @param token The token to detokenize
     * @param callback The callback to receive the result
     */
    fun deTokenizeAsFloat(token: String, callback: (DetokenizeResult<Float>) -> Unit) {
        performDetokenization(token, Float::class.java, callback)
    }

    /**
     * Detokenizes a token and returns the result as Double
     * @param token The token to detokenize
     * @param callback The callback to receive the result
     */
    fun deTokenizeAsDouble(token: String, callback: (DetokenizeResult<Double>) -> Unit) {
        performDetokenization(token, Double::class.java, callback)
    }

    /**
     * Detokenizes a token and returns the result as Boolean
     * @param token The token to detokenize
     * @param callback The callback to receive the result
     */
    fun deTokenizeAsBoolean(token: String, callback: (DetokenizeResult<Boolean>) -> Unit) {
        performDetokenization(token, Boolean::class.java, callback)
    }

    // ========================================
    // BATCH TOKENIZATION METHODS (Overloaded)
    // ========================================

    /**
     * Tokenizes multiple String values in a single batch operation.
     *
     * @param values The list of String values to tokenize
     * @param callback The callback to receive the result
     */
    fun batchTokenizeStringValues(values: List<String>, callback: (BatchTokenizeResult) -> Unit) {
        performBatchTokenization(values, String::class.java, callback)
    }

    /**
     * Tokenizes multiple Int values in a single batch operation.
     *
     * @param values The list of Int values to tokenize
     * @param callback The callback to receive the result
     */
    fun batchTokenizeIntValues(values: List<Int>, callback: (BatchTokenizeResult) -> Unit) {
        performBatchTokenization(values, Int::class.java, callback)
    }

    /**
     * Tokenizes multiple Long values in a single batch operation.
     *
     * @param values The list of Long values to tokenize
     * @param callback The callback to receive the result
     */
    fun batchTokenizeLongValues(values: List<Long>, callback: (BatchTokenizeResult) -> Unit) {
        performBatchTokenization(values, Long::class.java, callback)
    }

    /**
     * Tokenizes multiple Float values in a single batch operation.
     *
     * @param values The list of Float values to tokenize
     * @param callback The callback to receive the result
     */
    fun batchTokenizeFloatValues(values: List<Float>, callback: (BatchTokenizeResult) -> Unit) {
        performBatchTokenization(values, Float::class.java, callback)
    }

    /**
     * Tokenizes multiple Double values in a single batch operation.
     *
     * @param values The list of Double values to tokenize
     * @param callback The callback to receive the result
     */
    fun batchTokenizeDoubleValues(values: List<Double>, callback: (BatchTokenizeResult) -> Unit) {
        performBatchTokenization(values, Double::class.java, callback)
    }

    /**
     * Tokenizes multiple Boolean values in a single batch operation.
     *
     * @param values The list of Boolean values to tokenize
     * @param callback The callback to receive the result
     */
    fun batchTokenizeBooleanValues(values: List<Boolean>, callback: (BatchTokenizeResult) -> Unit) {
        performBatchTokenization(values, Boolean::class.java, callback)
    }

    // ========================================
    // BATCH DETOKENIZATION METHODS (Type-Specific)
    // ========================================

    /**
     * Detokenizes multiple tokens and returns the results as String values
     * @param tokens The list of tokens to detokenize
     * @param callback The callback to receive the result
     */
    fun batchDeTokenizeAsString(tokens: List<String>, callback: (BatchDetokenizeResult<String>) -> Unit) {
        performBatchDetokenization(tokens, String::class.java, callback)
    }

    /**
     * Detokenizes multiple tokens and returns the results as Int values
     * @param tokens The list of tokens to detokenize
     * @param callback The callback to receive the result
     */
    fun batchDeTokenizeAsInt(tokens: List<String>, callback: (BatchDetokenizeResult<Int>) -> Unit) {
        performBatchDetokenization(tokens, Int::class.java, callback)
    }

    /**
     * Detokenizes multiple tokens and returns the results as Long values
     * @param tokens The list of tokens to detokenize
     * @param callback The callback to receive the result
     */
    fun batchDeTokenizeAsLong(tokens: List<String>, callback: (BatchDetokenizeResult<Long>) -> Unit) {
        performBatchDetokenization(tokens, Long::class.java, callback)
    }

    /**
     * Detokenizes multiple tokens and returns the results as Float values
     * @param tokens The list of tokens to detokenize
     * @param callback The callback to receive the result
     */
    fun batchDeTokenizeAsFloat(tokens: List<String>, callback: (BatchDetokenizeResult<Float>) -> Unit) {
        performBatchDetokenization(tokens, Float::class.java, callback)
    }

    /**
     * Detokenizes multiple tokens and returns the results as Double values
     * @param tokens The list of tokens to detokenize
     * @param callback The callback to receive the result
     */
    fun batchDeTokenizeAsDouble(tokens: List<String>, callback: (BatchDetokenizeResult<Double>) -> Unit) {
        performBatchDetokenization(tokens, Double::class.java, callback)
    }

    /**
     * Detokenizes multiple tokens and returns the results as Boolean values
     * @param tokens The list of tokens to detokenize
     * @param callback The callback to receive the result
     */
    fun batchDeTokenizeAsBoolean(tokens: List<String>, callback: (BatchDetokenizeResult<Boolean>) -> Unit) {
        performBatchDetokenization(tokens, Boolean::class.java, callback)
    }

    /**
     * Clears the token cache.
     */
    fun clearCache() {
        logger.d("Clearing token cache")
        tokenCache.clear()
    }

    // ========================================
    // PRIVATE IMPLEMENTATION METHODS
    // ========================================

    /**
     * Internal method to perform tokenization with type conversion
     */
    private fun <T> performTokenization(
        value: T,
        type: Class<T>,
        callback: (TokenizeResult) -> Unit
    ) {
        sdkScope.launch {
            try {
                logger.d("Tokenizing ${type.simpleName} value")

                val converter = TypeConverterRegistry.getConverter(type)
                if (converter == null) {
                    withContext(Dispatchers.Main) {
                        callback(TokenizeResult.Error("Unsupported type: ${type.simpleName}"))
                    }
                    return@launch
                }

                // Convert typed value to string for repository layer
                val stringValue = converter.toString(value)

                // Call repository with string value
                val repoResult = if (enableEncryption) {
                    tokenRepository.tokenizeWithEncryptionOverTransit(stringValue)
                } else {
                    tokenRepository.tokenize(stringValue)
                }

                // Convert repository result to public result
                val publicResult = repoResult.toPublicResult()
                logger.d("tokenization result: ${type.simpleName} value: $publicResult")
                withContext(Dispatchers.Main) {
                    callback(publicResult)
                }
            } catch (e: Exception) {
                logger.e("Error tokenizing ${type.simpleName} value", e)
                withContext(Dispatchers.Main) {
                    callback(TokenizeResult.Error(e.message ?: "Unknown error occurred"))
                }
            }
        }
    }

    /**
     * Internal method to perform detokenization with type conversion
     */
    private fun <T> performDetokenization(
        token: String,
        type: Class<T>,
        callback: (DetokenizeResult<T>) -> Unit
    ) {
        sdkScope.launch {
            try {
                logger.d("Detokenizing token to ${type.simpleName}")

                val converter = TypeConverterRegistry.getConverter(type)
                if (converter == null) {
                    withContext(Dispatchers.Main) {
                        callback(DetokenizeResult.Error("Unsupported type: ${type.simpleName}"))
                    }
                    return@launch
                }

                // Call repository to get string result
                val repoResult = if (enableEncryption) {
                    tokenRepository.detokenizeWithEncryptionOverTransit(token)
                } else {
                    tokenRepository.detokenize(token)
                }

                // Convert repository result (string) to public result (typed) using converter
                val publicResult = repoResult.toPublicResult(converter)
                logger.d("de-tokenization result: ${type.simpleName} value: $publicResult")

                withContext(Dispatchers.Main) {
                    callback(publicResult)
                }
            } catch (e: Exception) {
                logger.e("Error detokenizing token to ${type.simpleName}", e)
                withContext(Dispatchers.Main) {
                    callback(DetokenizeResult.Error(e.message ?: "Unknown error occurred"))
                }
            }
        }
    }

    /**
     * Internal method to perform batch tokenization
     */
    private fun <T> performBatchTokenization(
        values: List<T>,
        type: Class<T>,
        callback: (BatchTokenizeResult) -> Unit
    ) {
        sdkScope.launch {
            try {
                if (values.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback(BatchTokenizeResult.Error("Batch tokenize request contains no values"))
                    }
                    return@launch
                }

                logger.d("Batch tokenizing ${values.size} ${type.simpleName} values")

                val converter = TypeConverterRegistry.getConverter(type)
                if (converter == null) {
                    withContext(Dispatchers.Main) {
                        callback(BatchTokenizeResult.Error("Unsupported type: ${type.simpleName}"))
                    }
                    return@launch
                }

                // Convert typed values to strings for repository layer
                val stringValues = values.map { converter.toString(it) }

                // Call repository with string values
                val repoResult = if (enableEncryption) {
                    tokenRepository.batchTokenizeWithEncryptionOverTransit(stringValues)
                } else {
                    tokenRepository.batchTokenize(stringValues)
                }

                // Convert repository result (strings) to public result (non-generic) using converter
                val publicResult = repoResult.toPublicResult()
                logger.d("batch tokenization result: ${type.simpleName} values: $publicResult")

                withContext(Dispatchers.Main) {
                    callback(publicResult)
                }
            } catch (e: Exception) {
                logger.e("Error batch tokenizing ${type.simpleName} values", e)
                withContext(Dispatchers.Main) {
                    callback(BatchTokenizeResult.Error(e.message ?: "Unknown error occurred"))
                }
            }
        }
    }

    /**
     * Internal method to perform batch detokenization
     */
    private fun <T> performBatchDetokenization(
        tokens: List<String>,
        type: Class<T>,
        callback: (BatchDetokenizeResult<T>) -> Unit
    ) {
        sdkScope.launch {
            try {
                if (tokens.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback(BatchDetokenizeResult.Error("Batch detokenize request contains no tokens"))
                    }
                    return@launch
                }

                logger.d("Batch detokenizing ${tokens.size} tokens to ${type.simpleName}")

                val converter = TypeConverterRegistry.getConverter(type)
                if (converter == null) {
                    withContext(Dispatchers.Main) {
                        callback(BatchDetokenizeResult.Error("Unsupported type: ${type.simpleName}"))
                    }
                    return@launch
                }

                // Call repository to get string results
                val repoResult = if (enableEncryption) {
                    tokenRepository.batchDetokenizeWithEncryptionOverTransit(tokens)
                } else {
                    tokenRepository.batchDetokenize(tokens)
                }

                // Convert repository result (strings) to public result (typed) using converter
                val publicResult = repoResult.toPublicResult(converter)
                logger.d("batch de-tokenization result: ${type.simpleName} tokens: $publicResult")

                withContext(Dispatchers.Main) {
                    callback(publicResult)
                }
            } catch (e: Exception) {
                logger.e("Error batch detokenizing tokens to ${type.simpleName}", e)
                withContext(Dispatchers.Main) {
                    callback(BatchDetokenizeResult.Error(e.message ?: "Unknown error occurred"))
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: VaultSDK? = null

        @JvmStatic
        fun initialize(
            clientId: String,
            clientSecret: String,
            apiUrl: String,
            authUrl: String,
            logLevel: VaultLogger.LogLevel = VaultLogger.LogLevel.OFF
        ): VaultSDK {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VaultSDK(
                    clientId,
                    clientSecret,
                    apiUrl,
                    authUrl,
                    enableEncryption = true,
                    enableCache = true,
                    logLevel = logLevel.intValue
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