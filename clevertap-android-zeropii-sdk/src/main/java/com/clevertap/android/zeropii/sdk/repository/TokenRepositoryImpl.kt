package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.encryption.EncryptionManager
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.TokenizeRepoResult
import com.clevertap.android.zeropii.sdk.network.NetworkProvider
import com.clevertap.android.zeropii.sdk.retry.RetryPolicy
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger

class TokenRepositoryImpl(
    private val networkProvider: NetworkProvider,
    private val authRepository: AuthRepository,
    private val encryptionManager: EncryptionManager,
    private val logger: ZeroPiiLogger,
    private val retryPolicy: RetryPolicy
) : TokenRepository {

    // Utility components initialized lazily
    private val retryHandler by lazy {
        RetryHandler(authRepository, logger, retryPolicy)
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

    // ========================================
    // Public TokenRepository Interface
    // ========================================

    override suspend fun tokenize(value: String): TokenizeRepoResult {
        return singleTokenizeOperation.execute(value)
    }

    override suspend fun batchTokenize(values: List<String>): BatchTokenizeRepoResult {
        return batchTokenizeOperation.execute(values)
    }

    override suspend fun tokenizeWithEncryptionOverTransit(value: String): TokenizeRepoResult {
        return singleTokenizeWithEncryptionOperation.execute(value)
    }

    override suspend fun batchTokenizeWithEncryptionOverTransit(values: List<String>): BatchTokenizeRepoResult {
        return batchTokenizeWithEncryptionOperation.execute(values)
    }

    // ========================================
    // Private Factory Methods
    // ========================================

    internal fun createSingleTokenizeOperation(strategy: EncryptionStrategy): SingleTokenizeOperation {
        return SingleTokenizeOperation(
            authRepository = authRepository,
            networkProvider = networkProvider,
            encryptionManager = encryptionManager,
            logger = logger,
            retryHandler = retryHandler,
            responseProcessor = responseProcessor,
            encryptionStrategy = strategy
        )
    }

    internal fun createBatchTokenizeOperation(strategy: EncryptionStrategy): BatchTokenizeOperation {
        return BatchTokenizeOperation(
            authRepository = authRepository,
            networkProvider = networkProvider,
            encryptionManager = encryptionManager,
            logger = logger,
            retryHandler = retryHandler,
            responseProcessor = responseProcessor,
            encryptionStrategy = strategy
        )
    }
}
