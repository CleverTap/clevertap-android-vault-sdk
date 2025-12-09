package com.clevertap.android.vault.sdk.repository


import com.clevertap.android.vault.sdk.api.TokenizationApi
import com.clevertap.android.vault.sdk.cache.TokenCache
import com.clevertap.android.vault.sdk.encryption.EncryptionManager
import com.clevertap.android.vault.sdk.model.BatchDetokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeSummary
import com.clevertap.android.vault.sdk.model.EncryptedResponse
import com.clevertap.android.vault.sdk.network.NetworkProvider
import com.clevertap.android.vault.sdk.util.VaultLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.Response

// ====================================
// Batch Size Validation Tests for BatchDetokenize
// ====================================
@RunWith(Parameterized::class)
class BatchDetokenizeOperationValidationTest(
    private val inputTokens: List<String>,
    private val shouldThrowException: Boolean,
    private val expectedErrorMessage: String,
    private val description: String
) {
    private lateinit var operation: BatchDetokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should validate batch: {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    emptyList<String>(),
                    true,
                    "Batch detokenize request contains no tokens",
                    "Empty list"
                ),
                arrayOf(listOf("single-token"), false, "", "Single token"),
                arrayOf((1..1000).map { "token-$it" }, false, "", "1000 tokens"),
                arrayOf(
                    (1..10000).map { "token-$it" },
                    false,
                    "",
                    "Maximum allowed tokens (10000)"
                ),
                arrayOf(
                    (1..10001).map { "token-$it" },
                    true,
                    "Batch size exceeds the maximum limit of 10000 tokens",
                    "Over maximum limit"
                ),
                arrayOf(
                    (1..50000).map { "token-$it" },
                    true,
                    "Batch size exceeds the maximum limit of 10000 tokens",
                    "Way over maximum limit"
                )
            )
        }
    }

    @Before
    fun setUp() {
        operation = createTestBatchDetokenizeOperation()
    }

    @Test
    fun shouldValidateBatchSizeCorrectly() {
        if (shouldThrowException) {
            try {
                operation.validateRequest(inputTokens)
                org.junit.Assert.fail("Expected IllegalArgumentException but none was thrown")
            } catch (e: IllegalArgumentException) {
                assertEquals("Error message should match", expectedErrorMessage, e.message)
            }
        } else {
            // Should not throw exception
            operation.validateRequest(inputTokens)
            // Test passes if no exception is thrown
        }
    }
}

// ====================================
// Cache State Check Tests for BatchDetokenize
// ====================================
@RunWith(Parameterized::class)
class BatchDetokenizeOperationCacheCheckTest(
    private val inputTokens: List<String>,
    private val cachedValues: Map<String, Pair<String, String>>, // token -> (value, dataType)
    private val expectedCacheResult: String,
    private val description: String
) {
    private lateinit var mockCacheManager: CacheManager
    private lateinit var operation: BatchDetokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle cache check: {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // All tokens cached
                arrayOf(
                    listOf("555-67-8901", "abc@example.com"),
                    mapOf(
                        "555-67-8901" to ("555-12-3456" to "string"),
                        "abc@example.com" to ("john@example.com" to "string")
                    ),
                    "CompleteFromCache",
                    "All tokens found in cache"
                ),
                // No tokens cached
                arrayOf(
                    listOf("new-token-1", "new-token-2"),
                    emptyMap<String, Pair<String, String>>(),
                    "PartialFromCache",
                    "No tokens found in cache"
                ),
                // Partial cache hit
                arrayOf(
                    listOf("555-67-8901", "new-token", "abc@example.com"),
                    mapOf(
                        "555-67-8901" to ("555-12-3456" to "string")
                    ),
                    "PartialFromCache",
                    "Some tokens found in cache"
                ),
                // Mixed data types
                arrayOf(
                    listOf("int-token-123", "bool-token-true", "float-token-456"),
                    mapOf(
                        "int-token-123" to ("987654" to "integer"),
                        "bool-token-true" to ("false" to "boolean")
                    ),
                    "PartialFromCache",
                    "Mixed data types with partial cache"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockCacheManager = mockk()
        operation = createTestBatchDetokenizeOperation(cacheManager = mockCacheManager)
    }

    @Test
    fun shouldCheckCacheStateCorrectly() {
        // Arrange
        val batchCacheResult = createMockBatchDetokenizeCacheResult(inputTokens, cachedValues)
        every { mockCacheManager.getBatchValuesFromCache(inputTokens) } returns batchCacheResult

        // Act
        val result = operation.checkCacheWithState(inputTokens)

        // Assert
        when (expectedCacheResult) {
            "CompleteFromCache" -> {
                assertTrue(
                    "Should be CompleteFromCache",
                    result is CacheCheckResult.CompleteFromCache
                )
                val completeResult =
                    result as CacheCheckResult.CompleteFromCache<List<String>, BatchDetokenizeRepoResult>
                assertEquals(
                    "Original request should match",
                    inputTokens,
                    completeResult.originalRequest
                )

                val batchResult = completeResult.result as BatchDetokenizeRepoResult.Success
                assertEquals(
                    "Should have correct number of results",
                    inputTokens.size,
                    batchResult.results.size
                )
                assertEquals(
                    "All should be found",
                    inputTokens.size,
                    batchResult.summary.foundCount
                )
                assertEquals("None should be not found", 0, batchResult.summary.notFoundCount)
            }

            "PartialFromCache" -> {
                assertTrue(
                    "Should be PartialFromCache",
                    result is CacheCheckResult.PartialFromCache
                )
                val partialResult =
                    result as CacheCheckResult.PartialFromCache<List<String>, BatchDetokenizeRepoResult>
                assertEquals(
                    "Original request should match",
                    inputTokens,
                    partialResult.originalRequest
                )

                val cachedItems = partialResult.cachedItems as List<BatchDetokenItemResponse>
                assertEquals(
                    "Should have correct number of cached items",
                    cachedValues.size,
                    cachedItems.size
                )

                val uncachedTokens = partialResult.uncachedRequest
                assertEquals(
                    "Should have correct number of uncached items",
                    inputTokens.size - cachedValues.size,
                    uncachedTokens.size
                )
            }
        }

        verify(exactly = 1) { mockCacheManager.getBatchValuesFromCache(inputTokens) }
    }

    private fun createMockBatchDetokenizeCacheResult(
        tokens: List<String>,
        cachedValues: Map<String, Pair<String, String>>
    ): BatchCacheResult {
        val cached = mutableListOf<BatchDetokenItemResponse>()
        val uncached = mutableListOf<String>()

        tokens.forEach { token ->
            val cachedPair = cachedValues[token]
            if (cachedPair != null) {
                cached.add(
                    BatchDetokenItemResponse(
                        token = token,
                        value = cachedPair.first,
                        exists = true,
                        dataType = cachedPair.second
                    )
                )
            } else {
                uncached.add(token)
            }
        }

        return BatchCacheResult(cached, uncached)
    }
}

// ====================================
// Response Processing Tests for BatchDetokenize
// ====================================
class BatchDetokenizeOperationResponseProcessingTest {
    private lateinit var mockResponseProcessor: ResponseProcessor
    private lateinit var mockEncryptionStrategy: WithEncryptionStrategy
    private lateinit var operation: BatchDetokenizeOperation

    @Before
    fun setUp() {
        mockResponseProcessor = mockk()
        mockEncryptionStrategy = mockk()
        operation = createTestBatchDetokenizeOperation(
            responseProcessor = mockResponseProcessor,
            encryptionStrategy = mockEncryptionStrategy
        )
    }

    @Test
    fun shouldProcessRegularBatchDetokenizeResponseCorrectly() {
        // Arrange
        val apiResults = listOf(
            BatchDetokenItemResponse("token1", "value1", true, "string"),
            BatchDetokenItemResponse("token2", null, false, null)
        )
        val cachedResults = listOf(
            BatchDetokenItemResponse("token3", "value3", true, "string")
        )
        val batchResponse = BatchDetokenizeResponse(
            results = apiResults,
            summary = BatchDetokenizeSummary(2, 1, 1)
        )
        val response = Response.success(batchResponse)
        val cacheResult =
            CacheCheckResult.PartialFromCache<List<String>, BatchDetokenizeRepoResult>(
                originalRequest = listOf("token1", "token2", "token3"),
                cachedItems = cachedResults,
                uncachedRequest = listOf("token1", "token2")
            )

        val expectedResult = BatchDetokenizeRepoResult.Success(
            results = cachedResults + apiResults,
            summary = BatchDetokenizeSummary(3, 2, 1)
        )

        every {
            mockResponseProcessor.processBatchDetokenizeResponse(
                response,
                cachedResults
            )
        } returns expectedResult

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return success result", result is BatchDetokenizeRepoResult.Success)
        val successResult = result as BatchDetokenizeRepoResult.Success
        assertEquals("Should have correct total results", 3, successResult.results.size)
        assertEquals("Should have correct summary", 3, successResult.summary.processedCount)

        verify(exactly = 1) {
            mockResponseProcessor.processBatchDetokenizeResponse(
                response,
                cachedResults
            )
        }
    }

    @Test
    fun shouldProcessBatchDetokenizeResponseWithEmptyCacheCorrectly() {
        // Arrange
        val apiResults = listOf(
            BatchDetokenItemResponse("token1", "value1", true, "string"),
            BatchDetokenItemResponse("token2", "value2", true, "string"),
            BatchDetokenItemResponse("token3", "123456", true, "integer")
        )
        val cachedResults = emptyList<BatchDetokenItemResponse>()  // ✅ Empty cache scenario

        val batchResponse = BatchDetokenizeResponse(
            results = apiResults,
            summary = BatchDetokenizeSummary(3, 3, 0)  // total=3, found=3, notFound=0
        )
        val response = Response.success(batchResponse)

        val cacheResult =
            CacheCheckResult.NothingFromCache<List<String>, BatchDetokenizeRepoResult>(
                originalRequest = listOf("token1", "token2", "token3"),
                uncachedRequest = listOf("token1", "token2", "token3")  // All tokens uncached
            )

        val expectedResult = BatchDetokenizeRepoResult.Success(
            results = apiResults,  // Only API results, no cached items
            summary = BatchDetokenizeSummary(3, 3, 0)  // All found via API
        )

        every {
            mockResponseProcessor.processBatchDetokenizeResponse(
                response,
                cachedResults  // Empty list
            )
        } returns expectedResult

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return success result", result is BatchDetokenizeRepoResult.Success)
        val successResult = result as BatchDetokenizeRepoResult.Success

        assertEquals("Should have correct total results", 3, successResult.results.size)
        assertEquals("Should contain only API results", apiResults, successResult.results)
        assertEquals("Should have correct summary - total", 3, successResult.summary.processedCount)
        assertEquals("Should have correct summary - found", 3, successResult.summary.foundCount)
        assertEquals(
            "Should have correct summary - not found",
            0,
            successResult.summary.notFoundCount
        )

        // Verify processor was called with empty cached results
        verify(exactly = 1) {
            mockResponseProcessor.processBatchDetokenizeResponse(
                response,
                emptyList()
            )
        }
    }

    @Test
    fun shouldProcessEncryptedBatchDetokenizeResponseCorrectly() {
        // Arrange
        val encryptedResponse = EncryptedResponse("encrypted-payload", "iv")
        val response = Response.success(encryptedResponse)
        val cachedResults = listOf(
            BatchDetokenItemResponse("cached-token", "cached-value", true, "string")
        )
        val cacheResult =
            CacheCheckResult.PartialFromCache<List<String>, BatchDetokenizeRepoResult>(
                originalRequest = listOf("cached-token", "new-token"),
                cachedItems = cachedResults,
                uncachedRequest = listOf("new-token")
            )

        val decryptedApiResults = listOf(
            BatchDetokenItemResponse("new-token", "new-value", true, "string")
        )
        val decryptedResponse = BatchDetokenizeResponse(
            results = decryptedApiResults,
            summary = BatchDetokenizeSummary(1, 1, 0)
        )

        every {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                BatchDetokenizeResponse::class.java
            )
        } returns decryptedResponse

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return success result", result is BatchDetokenizeRepoResult.Success)
        val successResult = result as BatchDetokenizeRepoResult.Success
        assertEquals("Should have combined results", 2, successResult.results.size)
        assertEquals(
            "Should have cached result first",
            "cached-token",
            successResult.results[0].token
        )
        assertEquals("Should have API result second", "new-token", successResult.results[1].token)

        verify(exactly = 1) {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                BatchDetokenizeResponse::class.java
            )
        }
    }

    @Test
    fun shouldHandleEncryptedResponseDecryptionFailure() {
        // Arrange
        val encryptedResponse = EncryptedResponse("encrypted-payload", "iv")
        val response = Response.success(encryptedResponse)
        val cacheResult =
            CacheCheckResult.NothingFromCache<List<String>, BatchDetokenizeRepoResult>(
                originalRequest = listOf("token1"),
                uncachedRequest = listOf("token1")
            )

        every {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                BatchDetokenizeResponse::class.java
            )
        } returns null

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is BatchDetokenizeRepoResult.Error)
        val errorResult = result as BatchDetokenizeRepoResult.Error
        assertEquals("Failed to decrypt response", errorResult.message)

        verify(exactly = 1) {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                BatchDetokenizeResponse::class.java
            )
        }
    }

    @Test
    fun shouldHandleEncryptionStrategyMismatch() {
        // Arrange
        val encryptedResponse = EncryptedResponse("encrypted-payload", "iv")
        val response = Response.success(encryptedResponse)
        val cacheResult =
            CacheCheckResult.NothingFromCache<List<String>, BatchDetokenizeRepoResult>(
                originalRequest = listOf("token1", "token2"),
                uncachedRequest = listOf("token1", "token2")
            )

        // Use NoEncryptionStrategy instead of WithEncryptionStrategy
        val operationWithWrongStrategy = createTestBatchDetokenizeOperation(
            encryptionStrategy = mockk<NoEncryptionStrategy>()
        )

        // Act
        val result = operationWithWrongStrategy.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is BatchDetokenizeRepoResult.Error)
        val errorResult = result as BatchDetokenizeRepoResult.Error
        assertEquals(
            "Error message should indicate strategy mismatch",
            "Encryption strategy mismatch",
            errorResult.message
        )
    }

    @Test
    fun shouldHandleUnknownResponseType() {
        // Arrange
        val unknownResponse = "unknown-response-type"
        val response = Response.success(unknownResponse)
        val cacheResult =
            CacheCheckResult.NothingFromCache<List<String>, BatchDetokenizeRepoResult>(
                originalRequest = listOf("token1", "token2"),
                uncachedRequest = listOf("token1", "token2")
            )

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is BatchDetokenizeRepoResult.Error)
        val errorResult = result as BatchDetokenizeRepoResult.Error
        assertTrue(
            "Error message should indicate operation failure",
            errorResult.message.contains("Batch Detokenize failed")
        )
    }

    @Test
    fun shouldHandleHttpErrorResponse() {
        // Arrange
        val errorBody = ResponseBody.create(null, "Tokens not found")
        val response = Response.error<BatchDetokenizeResponse>(404, errorBody)
        val cacheResult =
            CacheCheckResult.NothingFromCache<List<String>, BatchDetokenizeRepoResult>(
                originalRequest = listOf("token1"),
                uncachedRequest = listOf("token1")
            )

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is BatchDetokenizeRepoResult.Error)
        val errorResult = result as BatchDetokenizeRepoResult.Error
        assertTrue("Error message should contain HTTP code", errorResult.message.contains("404"))
        assertTrue(
            "Error message should contain operation type",
            errorResult.message.contains("Batch Detokenize")
        )
    }
}

// ====================================
// Cache Update Tests for BatchDetokenize
// ====================================
class BatchDetokenizeOperationCacheUpdateTest {
    private lateinit var mockCacheManager: CacheManager
    private lateinit var operation: BatchDetokenizeOperation

    @Before
    fun setUp() {
        mockCacheManager = mockk(relaxed = true)
        operation = createTestBatchDetokenizeOperation(cacheManager = mockCacheManager)
    }

    @Test
    fun shouldUpdateCacheOnSuccessfulBatchResult() {
        // Arrange
        val inputTokens = listOf("token1", "token2")
        val results = listOf(
            BatchDetokenItemResponse("token1", "value1", true, "string"),
            BatchDetokenItemResponse("token2", null, false, null)
        )
        val successResult = BatchDetokenizeRepoResult.Success(
            results = results,
            summary = BatchDetokenizeSummary(2, 1, 1)
        )

        // Act
        operation.updateCache(inputTokens, successResult)

        // Assert
        verify(exactly = 1) { mockCacheManager.storeBatchValuesInCache(results) }
    }

    @Test
    fun shouldNotUpdateCacheOnErrorResult() {
        // Arrange
        val inputTokens = listOf("token1", "token2")
        val errorResult = BatchDetokenizeRepoResult.Error("Batch operation failed")

        // Act
        operation.updateCache(inputTokens, errorResult)

        // Assert
        verify(exactly = 0) { mockCacheManager.storeBatchValuesInCache(any()) }
    }
}

// ====================================
// Error Result Creation Tests
// ====================================
@RunWith(Parameterized::class)
class BatchDetokenizeOperationErrorResultTest(
    private val errorMessage: String,
    private val description: String
) {
    private lateinit var operation: BatchDetokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should create error result: {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("Network connection failed", "Network error"),
                arrayOf("Batch size exceeds limit", "Batch size error"),
                arrayOf("", "Empty error message"),
                arrayOf("Special @#$% characters error", "Special characters error")
            )
        }
    }

    @Before
    fun setUp() {
        operation = createTestBatchDetokenizeOperation()
    }

    @Test
    fun shouldCreateErrorResultCorrectly() {
        // Act
        val result = operation.createErrorResult(errorMessage)

        // Assert
        assertTrue("Should return error result", result is BatchDetokenizeRepoResult.Error)
        val errorResult = result as BatchDetokenizeRepoResult.Error
        assertEquals("Error message should match", errorMessage, errorResult.message)
    }

    private fun createTestBatchDetokenizeOperation(): BatchDetokenizeOperation {
        return BatchDetokenizeOperation(
            tokenCache = mockk(relaxed = true),
            authRepository = mockk(relaxed = true),
            networkProvider = mockk(relaxed = true),
            encryptionManager = mockk(relaxed = true),
            logger = mockk(relaxed = true),
            retryHandler = mockk(relaxed = true),
            cacheManager = mockk(relaxed = true),
            responseProcessor = mockk(relaxed = true),
            encryptionStrategy = mockk(relaxed = true)
        )
    }
}

// ====================================
// API Call Delegation Tests
// ====================================
@RunWith(Parameterized::class)
class BatchDetokenizeOperationApiCallTest(
    private val inputTokens: List<String>,
    private val accessToken: String,
    private val description: String
) {
    private lateinit var mockEncryptionStrategy: EncryptionStrategy
    private lateinit var mockTokenizationApi: TokenizationApi
    private lateinit var mockNetworkProvider: NetworkProvider
    private lateinit var operation: BatchDetokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle API call: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    listOf("555-67-8901", "abc@example.com"),
                    "bearer-token-123",
                    "Multiple PII tokens"
                ),
                arrayOf(
                    listOf("🌟", "🎯", "🚀"),
                    "bearer-token-unicode",
                    "Unicode tokens"
                ),
                arrayOf(
                    (1..500).map { "token-$it" },
                    "bearer-token-large",
                    "Large batch (500 tokens)"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockEncryptionStrategy = mockk()
        mockTokenizationApi = mockk()
        mockNetworkProvider = mockk()
        every { mockNetworkProvider.tokenizationApi } returns mockTokenizationApi

        operation = createTestBatchDetokenizeOperation(
            networkProvider = mockNetworkProvider,
            encryptionStrategy = mockEncryptionStrategy
        )
    }

    @Test
    fun shouldDelegateToEncryptionStrategyCorrectly() = runTest {
        // Arrange
        val expectedResponse = Response.success(
            BatchDetokenizeResponse(
                results = inputTokens.map { token ->
                    BatchDetokenItemResponse(token, "value-$token", true, "string")
                },
                summary = BatchDetokenizeSummary(processedCount = inputTokens.size,
                    foundCount = inputTokens.size,
                    notFoundCount = 0
                )
            )
        )

        coEvery {
            mockEncryptionStrategy.batchDetokenize(
                mockTokenizationApi,
                accessToken,
                inputTokens
            )
        } returns expectedResponse

        // Act
        val result = operation.makeApiCall(inputTokens, accessToken)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        coVerify(exactly = 1) {
            mockEncryptionStrategy.batchDetokenize(
                mockTokenizationApi,
                accessToken,
                inputTokens
            )
        }
        verify(exactly = 1) { mockNetworkProvider.tokenizationApi }
    }

    private fun createTestBatchDetokenizeOperation(
        networkProvider: NetworkProvider = mockk(relaxed = true),
        encryptionStrategy: EncryptionStrategy = mockk(relaxed = true)
    ): BatchDetokenizeOperation {
        return BatchDetokenizeOperation(
            tokenCache = mockk(relaxed = true),
            authRepository = mockk(relaxed = true),
            networkProvider = networkProvider,
            encryptionManager = mockk(relaxed = true),
            logger = mockk(relaxed = true),
            retryHandler = mockk(relaxed = true),
            cacheManager = mockk(relaxed = true),
            responseProcessor = mockk(relaxed = true),
            encryptionStrategy = encryptionStrategy
        )
    }
}

private fun createTestBatchDetokenizeOperation(
    tokenCache: TokenCache = mockk(relaxed = true),
    authRepository: AuthRepository = mockk(relaxed = true),
    networkProvider: NetworkProvider = mockk(relaxed = true),
    encryptionManager: EncryptionManager = mockk(relaxed = true),
    logger: VaultLogger = mockk(relaxed = true),
    retryHandler: RetryHandler = mockk(relaxed = true),
    cacheManager: CacheManager = mockk(relaxed = true),
    responseProcessor: ResponseProcessor = mockk(relaxed = true),
    encryptionStrategy: EncryptionStrategy = mockk(relaxed = true)
): BatchDetokenizeOperation {
    return BatchDetokenizeOperation(
        tokenCache = tokenCache,
        authRepository = authRepository,
        networkProvider = networkProvider,
        encryptionManager = encryptionManager,
        logger = logger,
        retryHandler = retryHandler,
        cacheManager = cacheManager,
        responseProcessor = responseProcessor,
        encryptionStrategy = encryptionStrategy
    )
}