package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.cache.TokenCache
import com.clevertap.android.vault.sdk.encryption.EncryptionManager
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.vault.sdk.model.DetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.TokenizeRepoResult
import com.clevertap.android.vault.sdk.network.NetworkProvider
import com.clevertap.android.vault.sdk.util.VaultLogger

class TokenRepositoryImpl(
    private val networkProvider: NetworkProvider,
    private val authRepository: AuthRepository,
    private val encryptionManager: EncryptionManager,
    private val tokenCache: TokenCache,
    private val logger: VaultLogger
) : TokenRepository {

    // Utility components initialized lazily
    private val retryHandler by lazy {
        RetryHandler(authRepository, logger)
    }

    private val cacheManager by lazy {
        CacheManager(tokenCache, logger)
    }

    private val responseProcessor by lazy {
        ResponseProcessor(logger)
    }

    // Strategy for handling encryption/non-encryption
    private val encryptionStrategyFactory by lazy {
        EncryptionStrategyFactory()
    }

    // Template method operations for different scenarios
    private val singleTokenizeOperation by lazy {
        createSingleTokenizeOperation(NoEncryptionStrategy(logger))
    }

    private val singleTokenizeWithEncryptionOperation by lazy {
        createSingleTokenizeOperation(
            encryptionStrategyFactory.createStrategy(
                encryptionManager,
                logger
            )
        )
    }

    private val singleDetokenizeOperation by lazy {
        createSingleDetokenizeOperation(NoEncryptionStrategy(logger))
    }

    private val singleDetokenizeWithEncryptionOperation by lazy {
        createSingleDetokenizeOperation(
            encryptionStrategyFactory.createStrategy(
                encryptionManager,
                logger
            )
        )
    }

    private val batchTokenizeOperation by lazy {
        createBatchTokenizeOperation(NoEncryptionStrategy(logger))
    }

    private val batchTokenizeWithEncryptionOperation by lazy {
        createBatchTokenizeOperation(
            encryptionStrategyFactory.createStrategy(
                encryptionManager,
                logger
            )
        )
    }

    private val batchDetokenizeOperation by lazy {
        createBatchDetokenizeOperation(NoEncryptionStrategy(logger))
    }

    private val batchDetokenizeWithEncryptionOperation by lazy {
        createBatchDetokenizeOperation(
            encryptionStrategyFactory.createStrategy(
                encryptionManager,
                logger
            )
        )
    }

    // ========================================
    // Public TokenRepository Interface
    // ========================================

    override suspend fun tokenize(value: String): TokenizeRepoResult {
        return singleTokenizeOperation.execute(value)
    }

    override suspend fun detokenize(token: String): DetokenizeRepoResult {
        return singleDetokenizeOperation.execute(token)
    }

    override suspend fun batchTokenize(values: List<String>): BatchTokenizeRepoResult {
        return batchTokenizeOperation.execute(values)
    }

    override suspend fun batchDetokenize(tokens: List<String>): BatchDetokenizeRepoResult {
        return batchDetokenizeOperation.execute(tokens)
    }

    override suspend fun tokenizeWithEncryptionOverTransit(value: String): TokenizeRepoResult {
        return singleTokenizeWithEncryptionOperation.execute(value)
    }

    override suspend fun detokenizeWithEncryptionOverTransit(token: String): DetokenizeRepoResult {
        return singleDetokenizeWithEncryptionOperation.execute(token)
    }

    override suspend fun batchTokenizeWithEncryptionOverTransit(values: List<String>): BatchTokenizeRepoResult {
        return batchTokenizeWithEncryptionOperation.execute(values)
    }

    override suspend fun batchDetokenizeWithEncryptionOverTransit(tokens: List<String>): BatchDetokenizeRepoResult {
        return batchDetokenizeWithEncryptionOperation.execute(tokens)
    }

    // ========================================
    // Private Factory Methods
    // ========================================

    internal fun createSingleTokenizeOperation(strategy: EncryptionStrategy): SingleTokenizeOperation {
        return SingleTokenizeOperation(
            tokenCache = tokenCache,
            authRepository = authRepository,
            networkProvider = networkProvider,
            encryptionManager = encryptionManager,
            logger = logger,
            retryHandler = retryHandler,
            cacheManager = cacheManager,
            responseProcessor = responseProcessor,
            encryptionStrategy = strategy
        )
    }

    internal fun createSingleDetokenizeOperation(strategy: EncryptionStrategy): SingleDetokenizeOperation {
        return SingleDetokenizeOperation(
            tokenCache = tokenCache,
            authRepository = authRepository,
            networkProvider = networkProvider,
            encryptionManager = encryptionManager,
            logger = logger,
            retryHandler = retryHandler,
            cacheManager = cacheManager,
            responseProcessor = responseProcessor,
            encryptionStrategy = strategy
        )
    }

    internal fun createBatchTokenizeOperation(strategy: EncryptionStrategy): BatchTokenizeOperation {
        return BatchTokenizeOperation(
            tokenCache = tokenCache,
            authRepository = authRepository,
            networkProvider = networkProvider,
            encryptionManager = encryptionManager,
            logger = logger,
            retryHandler = retryHandler,
            cacheManager = cacheManager,
            responseProcessor = responseProcessor,
            encryptionStrategy = strategy
        )
    }

    internal fun createBatchDetokenizeOperation(strategy: EncryptionStrategy): BatchDetokenizeOperation {
        return BatchDetokenizeOperation(
            tokenCache = tokenCache,
            authRepository = authRepository,
            networkProvider = networkProvider,
            encryptionManager = encryptionManager,
            logger = logger,
            retryHandler = retryHandler,
            cacheManager = cacheManager,
            responseProcessor = responseProcessor,
            encryptionStrategy = strategy
        )
    }
}