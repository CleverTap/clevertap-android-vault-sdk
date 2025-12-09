package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.api.TokenizationApi
import com.clevertap.android.vault.sdk.cache.TokenCache
import com.clevertap.android.vault.sdk.encryption.EncryptionManager
import com.clevertap.android.vault.sdk.model.BatchTokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeSummary
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
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.Response

// ====================================
// Batch Size Validation Tests
// ====================================
@RunWith(Parameterized::class)
class BatchTokenizeOperationValidationTest(
    private val inputValues: List<String>,
    private val shouldThrowException: Boolean,
    private val expectedErrorMessage: String,
    private val description: String
) {
    private lateinit var operation: BatchTokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should validate batch: {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    emptyList<String>(),
                    true,
                    "Batch tokenize request contains no values",
                    "Empty list"
                ),
                arrayOf(listOf("single-value"), false, "", "Single value"),
                arrayOf((1..100).map { "value-$it" }, false, "", "100 values"),
                arrayOf((1..1000).map { "value-$it" }, false, "", "Maximum allowed values (1000)"),
                arrayOf(
                    (1..1001).map { "value-$it" },
                    true,
                    "Batch size exceeds the maximum limit of 1000 values",
                    "Over maximum limit"
                ),
                arrayOf(
                    (1..5000).map { "value-$it" },
                    true,
                    "Batch size exceeds the maximum limit of 1000 values",
                    "Way over maximum limit"
                )
            )
        }
    }

    @Before
    fun setUp() {
        operation = createTestBatchTokenizeOperation()
    }

    @Test
    fun shouldValidateBatchSizeCorrectly() {
        if (shouldThrowException) {
            try {
                operation.validateRequest(inputValues)
                Assert.fail("Expected IllegalArgumentException but none was thrown")
            } catch (e: IllegalArgumentException) {
                assertEquals("Error message should match", expectedErrorMessage, e.message)
            }
        } else {
            // Should not throw exception
            operation.validateRequest(inputValues)
            // Test passes if no exception is thrown
        }
    }
}

// ====================================
// Cache State Check Tests for BatchTokenize
// ====================================
@RunWith(Parameterized::class)
class BatchTokenizeOperationCacheCheckTest(
    private val inputValues: List<String>,
    private val cachedTokens: Map<String, Pair<String, String>>, // value -> (token, dataType)
    private val expectedCacheResult: String,
    private val description: String
) {
    private lateinit var mockCacheManager: CacheManager
    private lateinit var operation: BatchTokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle cache check: {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // All values cached
                arrayOf(
                    listOf("555-12-3456", "john@example.com"),
                    mapOf(
                        "555-12-3456" to ("555-67-8901" to "string"),
                        "john@example.com" to ("abc@example.com" to "string")
                    ),
                    "CompleteFromCache",
                    "All values found in cache"
                ),
                // No values cached
                arrayOf(
                    listOf("new-value-1", "new-value-2"),
                    emptyMap<String, Pair<String, String>>(),
                    "PartialFromCache",
                    "No values found in cache"
                ),
                // Partial cache hit
                arrayOf(
                    listOf("555-12-3456", "new-value", "john@example.com"),
                    mapOf(
                        "555-12-3456" to ("555-67-8901" to "string")
                    ),
                    "PartialFromCache",
                    "Some values found in cache"
                ),
                // Mixed data types
                arrayOf(
                    listOf("123456", "true", "555-12-3456"),
                    mapOf(
                        "123456" to ("987654" to "integer"),
                        "true" to ("false" to "boolean")
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
        operation = createTestBatchTokenizeOperation(cacheManager = mockCacheManager)
    }

    @Test
    fun shouldCheckCacheStateCorrectly() {
        // Arrange
        val batchCacheResult = createMockBatchCacheResult(inputValues, cachedTokens)
        every { mockCacheManager.getBatchTokensFromCache(inputValues) } returns batchCacheResult

        // Act
        val result = operation.checkCacheWithState(inputValues)

        // Assert
        when (expectedCacheResult) {
            "CompleteFromCache" -> {
                assertTrue(
                    "Should be CompleteFromCache",
                    result is CacheCheckResult.CompleteFromCache
                )
                val completeResult =
                    result as CacheCheckResult.CompleteFromCache<List<String>, BatchTokenizeRepoResult>
                assertEquals(
                    "Original request should match",
                    inputValues,
                    completeResult.originalRequest
                )

                val batchResult = completeResult.result as BatchTokenizeRepoResult.Success
                assertEquals(
                    "Should have correct number of results",
                    inputValues.size,
                    batchResult.results.size
                )
                assertEquals(
                    "All should be existing",
                    inputValues.size,
                    batchResult.summary.existingCount
                )
                assertEquals(
                    "None should be newly created",
                    0,
                    batchResult.summary.newlyCreatedCount
                )
            }

            "PartialFromCache" -> {
                assertTrue(
                    "Should be PartialFromCache",
                    result is CacheCheckResult.PartialFromCache
                )
                val partialResult =
                    result as CacheCheckResult.PartialFromCache<List<String>, BatchTokenizeRepoResult>
                assertEquals(
                    "Original request should match",
                    inputValues,
                    partialResult.originalRequest
                )

                val cachedItems = partialResult.cachedItems as List<BatchTokenItemResponse>
                assertEquals(
                    "Should have correct number of cached items",
                    cachedTokens.size,
                    cachedItems.size
                )

                val uncachedValues = partialResult.uncachedRequest
                assertEquals(
                    "Should have correct number of uncached items",
                    inputValues.size - cachedTokens.size,
                    uncachedValues.size
                )
            }
        }

        verify(exactly = 1) { mockCacheManager.getBatchTokensFromCache(inputValues) }
    }

    private fun createMockBatchCacheResult(
        values: List<String>,
        cachedTokens: Map<String, Pair<String, String>>
    ): BatchCacheResult {
        val cached = mutableListOf<BatchTokenItemResponse>()
        val uncached = mutableListOf<String>()

        values.forEach { value ->
            val cachedPair = cachedTokens[value]
            if (cachedPair != null) {
                cached.add(
                    BatchTokenItemResponse(
                        originalValue = value,
                        token = cachedPair.first,
                        exists = true,
                        newlyCreated = false,
                        dataType = cachedPair.second
                    )
                )
            } else {
                uncached.add(value)
            }
        }

        return BatchCacheResult(cached, uncached)
    }
}

// ====================================
// Response Processing Tests for BatchTokenize
// ====================================
class BatchTokenizeOperationResponseProcessingTest {
    private lateinit var mockResponseProcessor: ResponseProcessor
    private lateinit var mockEncryptionStrategy: WithEncryptionStrategy
    private lateinit var operation: BatchTokenizeOperation

    @Before
    fun setUp() {
        mockResponseProcessor = mockk()
        mockEncryptionStrategy = mockk()
        operation = createTestBatchTokenizeOperation(
            responseProcessor = mockResponseProcessor,
            encryptionStrategy = mockEncryptionStrategy
        )
    }

    @Test
    fun shouldProcessRegularBatchTokenizeResponseCorrectly() {
        // Arrange
        val apiResults = listOf(
            BatchTokenItemResponse("value1", "token1", false, true, "string"),
            BatchTokenItemResponse("value2", "token2", false, true, "string")
        )
        val cachedResults = listOf(
            BatchTokenItemResponse("value3", "token3", true, false, "string")
        )
        val batchResponse = BatchTokenizeResponse(
            results = apiResults,
            summary = BatchTokenizeSummary(2, 0, 2)
        )
        val response = Response.success(batchResponse)
        val cacheResult = CacheCheckResult.PartialFromCache<List<String>, BatchTokenizeRepoResult>(
            originalRequest = listOf("value1", "value2", "value3"),
            cachedItems = cachedResults,
            uncachedRequest = listOf("value1", "value2")
        )

        val expectedResult = BatchTokenizeRepoResult.Success(
            results = cachedResults + apiResults,
            summary = BatchTokenizeSummary(3, 1, 2)
        )

        every {
            mockResponseProcessor.processBatchTokenizeResponse(
                response,
                cachedResults
            )
        } returns expectedResult

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return success result", result is BatchTokenizeRepoResult.Success)
        val successResult = result as BatchTokenizeRepoResult.Success
        assertEquals("Should have correct total results", 3, successResult.results.size)
        assertEquals("Should have correct summary", 3, successResult.summary.processedCount)

        verify(exactly = 1) {
            mockResponseProcessor.processBatchTokenizeResponse(
                response,
                cachedResults
            )
        }
    }

    @Test
    fun shouldProcessBatchTokenizeResponseWithEmptyCacheCorrectly() {
        // Arrange
        val apiResults = listOf(
            BatchTokenItemResponse("value1", "token1", false, true, "string"),
            BatchTokenItemResponse("value2", "token2", false, true, "string"),
            BatchTokenItemResponse("value3", "token3", false, true, "integer")
        )
        val cachedResults = emptyList<BatchTokenItemResponse>()  // ✅ Empty cache scenario

        val batchResponse = BatchTokenizeResponse(
            results = apiResults,
            summary = BatchTokenizeSummary(3, 0, 3)  // total=3, existing=0, newlyCreated=3
        )
        val response = Response.success(batchResponse)

        val cacheResult = CacheCheckResult.NothingFromCache<List<String>, BatchTokenizeRepoResult>(
            originalRequest = listOf("value1", "value2", "value3"),
            uncachedRequest = listOf("value1", "value2", "value3")  // All items uncached
        )

        val expectedResult = BatchTokenizeRepoResult.Success(
            results = apiResults,  // Only API results, no cached items
            summary = BatchTokenizeSummary(3, 0, 3)  // All newly created
        )

        every {
            mockResponseProcessor.processBatchTokenizeResponse(
                response,
                cachedResults  // Empty list
            )
        } returns expectedResult

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return success result", result is BatchTokenizeRepoResult.Success)
        val successResult = result as BatchTokenizeRepoResult.Success

        assertEquals("Should have correct total results", 3, successResult.results.size)
        assertEquals("Should contain only API results", apiResults, successResult.results)
        assertEquals("Should have correct summary - total", 3, successResult.summary.processedCount)
        assertEquals(
            "Should have correct summary - existing",
            0,
            successResult.summary.existingCount
        )
        assertEquals(
            "Should have correct summary - newly created",
            3,
            successResult.summary.newlyCreatedCount
        )

        // Verify processor was called with empty cached results
        verify(exactly = 1) {
            mockResponseProcessor.processBatchTokenizeResponse(
                response,
                emptyList()
            )
        }
    }

    @Test
    fun shouldProcessEncryptedBatchTokenizeResponseCorrectly() {
        // Arrange
        val encryptedResponse = EncryptedResponse("encrypted-payload", "iv")
        val response = Response.success(encryptedResponse)
        val cachedResults = listOf(
            BatchTokenItemResponse("cached-value", "cached-token", true, false, "string")
        )
        val cacheResult = CacheCheckResult.PartialFromCache<List<String>, BatchTokenizeRepoResult>(
            originalRequest = listOf("cached-value", "new-value"),
            cachedItems = cachedResults,
            uncachedRequest = listOf("new-value")
        )

        val decryptedApiResults = listOf(
            BatchTokenItemResponse("new-value", "new-token", false, true, "string")
        )
        val decryptedResponse = BatchTokenizeResponse(
            results = decryptedApiResults,
            summary = BatchTokenizeSummary(1, 0, 1)
        )

        every {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                BatchTokenizeResponse::class.java
            )
        } returns decryptedResponse

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return success result", result is BatchTokenizeRepoResult.Success)
        val successResult = result as BatchTokenizeRepoResult.Success
        assertEquals("Should have combined results", 2, successResult.results.size)
        assertEquals(
            "Should have cached result first",
            "cached-value",
            successResult.results[0].originalValue
        )
        assertEquals(
            "Should have API result second",
            "new-value",
            successResult.results[1].originalValue
        )

        verify(exactly = 1) {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                BatchTokenizeResponse::class.java
            )
        }
    }

    @Test
    fun shouldHandleEncryptedResponseDecryptionFailure() {
        // Arrange
        val encryptedResponse = EncryptedResponse("encrypted-payload", "iv")
        val response = Response.success(encryptedResponse)
        val cacheResult = CacheCheckResult.NothingFromCache<List<String>, BatchTokenizeRepoResult>(
            originalRequest = listOf("value1"),
            uncachedRequest = listOf("value1")
        )

        every {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                BatchTokenizeResponse::class.java
            )
        } returns null

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is BatchTokenizeRepoResult.Error)
        val errorResult = result as BatchTokenizeRepoResult.Error
        assertEquals("Failed to decrypt response", errorResult.message)

        verify(exactly = 1) {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                BatchTokenizeResponse::class.java
            )
        }
    }

    @Test
    fun shouldHandleEncryptionStrategyMismatch() {
        // Arrange
        val encryptedResponse = EncryptedResponse("encrypted-payload", "iv")
        val response = Response.success(encryptedResponse)
        val cacheResult = CacheCheckResult.NothingFromCache<List<String>, BatchTokenizeRepoResult>(
            originalRequest = listOf("value1", "value2"),
            uncachedRequest = listOf("value1", "value2")
        )

        // Use NoEncryptionStrategy instead of WithEncryptionStrategy
        val operationWithWrongStrategy = createTestBatchTokenizeOperation(
            encryptionStrategy = mockk<NoEncryptionStrategy>()
        )

        // Act
        val result = operationWithWrongStrategy.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is BatchTokenizeRepoResult.Error)
        val errorResult = result as BatchTokenizeRepoResult.Error
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
        val cacheResult = CacheCheckResult.NothingFromCache<List<String>, BatchTokenizeRepoResult>(
            originalRequest = listOf("value1", "value2"),
            uncachedRequest = listOf("value1", "value2")
        )

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is BatchTokenizeRepoResult.Error)
        val errorResult = result as BatchTokenizeRepoResult.Error
        assertTrue(
            "Error message should indicate operation failure",
            errorResult.message.contains("Batch Tokenize failed")
        )
    }

    @Test
    fun shouldHandleHttpErrorResponse() {
        // Arrange
        val errorBody = ResponseBody.create(null, "Server Error")
        val response = Response.error<BatchTokenizeResponse>(500, errorBody)
        val cacheResult = CacheCheckResult.NothingFromCache<List<String>, BatchTokenizeRepoResult>(
            originalRequest = listOf("value1"),
            uncachedRequest = listOf("value1")
        )

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is BatchTokenizeRepoResult.Error)
        val errorResult = result as BatchTokenizeRepoResult.Error
        assertTrue("Error message should contain HTTP code", errorResult.message.contains("500"))
        assertTrue(
            "Error message should contain operation type",
            errorResult.message.contains("Batch Tokenize")
        )
    }
}

// ====================================
// Cache Update Tests for BatchTokenize
// ====================================
class BatchTokenizeOperationCacheUpdateTest {
    private lateinit var mockCacheManager: CacheManager
    private lateinit var operation: BatchTokenizeOperation

    @Before
    fun setUp() {
        mockCacheManager = mockk(relaxed = true)
        operation = createTestBatchTokenizeOperation(cacheManager = mockCacheManager)
    }

    @Test
    fun shouldUpdateCacheOnSuccessfulBatchResult() {
        // Arrange
        val inputValues = listOf("value1", "value2")
        val results = listOf(
            BatchTokenItemResponse("value1", "token1", false, true, "string"),
            BatchTokenItemResponse("value2", "token2", true, false, "string")
        )
        val successResult = BatchTokenizeRepoResult.Success(
            results = results,
            summary = BatchTokenizeSummary(2, 1, 1)
        )

        // Act
        operation.updateCache(inputValues, successResult)

        // Assert
        verify(exactly = 1) { mockCacheManager.storeBatchTokensInCache(results) }
    }

    @Test
    fun shouldNotUpdateCacheOnErrorResult() {
        // Arrange
        val inputValues = listOf("value1", "value2")
        val errorResult = BatchTokenizeRepoResult.Error("Batch operation failed")

        // Act
        operation.updateCache(inputValues, errorResult)

        // Assert
        verify(exactly = 0) { mockCacheManager.storeBatchTokensInCache(any()) }
    }
}

// ====================================
// Error Result Creation Tests
// ====================================
@RunWith(Parameterized::class)
class BatchTokenizeOperationErrorResultTest(
    private val errorMessage: String,
    private val description: String
) {
    private lateinit var operation: BatchTokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should create error result: {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("Network connection failed", "Network error"),
                arrayOf("Server internal error", "Server error"),
                arrayOf("Batch size exceeds limit", "Batch size error"),
                arrayOf("", "Empty error message"),
            )
        }
    }

    @Before
    fun setUp() {
        operation = createTestBatchTokenizeOperation()
    }

    @Test
    fun shouldCreateErrorResultCorrectly() {
        // Act
        val result = operation.createErrorResult(errorMessage)

        // Assert
        assertTrue("Should return error result", result is BatchTokenizeRepoResult.Error)
        val errorResult = result as BatchTokenizeRepoResult.Error
        assertEquals("Error message should match", errorMessage, errorResult.message)
    }

    private fun createTestBatchTokenizeOperation(): BatchTokenizeOperation {
        return BatchTokenizeOperation(
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
class BatchTokenizeOperationApiCallTest(
    private val inputValues: List<String>,
    private val accessToken: String,
    private val description: String
) {
    private lateinit var mockEncryptionStrategy: EncryptionStrategy
    private lateinit var mockTokenizationApi: TokenizationApi
    private lateinit var mockNetworkProvider: NetworkProvider
    private lateinit var operation: BatchTokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle API call: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    listOf("555-12-3456", "john@example.com"),
                    "bearer-token-123",
                    "Multiple PII values"
                ),
                arrayOf(
                    listOf("🚀", "🌟", "🎯"),
                    "bearer-token-unicode",
                    "Unicode values"
                ),
                arrayOf(
                    listOf(""),
                    "bearer-token-empty",
                    "Empty value"
                ),
                arrayOf(
                    (1..100).map { "value-$it" },
                    "bearer-token-large",
                    "Large batch (100 items)"
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

        operation = createTestBatchTokenizeOperation(
            networkProvider = mockNetworkProvider,
            encryptionStrategy = mockEncryptionStrategy
        )
    }

    @Test
    fun shouldDelegateToEncryptionStrategyCorrectly() = runTest {
        // Arrange
        val expectedResponse = Response.success(
            BatchTokenizeResponse(
                results = inputValues.map { value ->
                    BatchTokenItemResponse(value, "token-$value", false, true, "string")
                },
                summary = BatchTokenizeSummary(inputValues.size, 0, inputValues.size)
            )
        )

        coEvery {
            mockEncryptionStrategy.batchTokenize(
                mockTokenizationApi,
                accessToken,
                inputValues
            )
        } returns expectedResponse

        // Act
        val result = operation.makeApiCall(inputValues, accessToken)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        coVerify(exactly = 1) {
            mockEncryptionStrategy.batchTokenize(
                mockTokenizationApi,
                accessToken,
                inputValues
            )
        }
        verify(exactly = 1) { mockNetworkProvider.tokenizationApi }
    }

    private fun createTestBatchTokenizeOperation(
        networkProvider: NetworkProvider = mockk(relaxed = true),
        encryptionStrategy: EncryptionStrategy = mockk(relaxed = true)
    ): BatchTokenizeOperation {
        return BatchTokenizeOperation(
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


// ====================================
// Helper Functions
// ====================================

private fun createTestBatchTokenizeOperation(
    tokenCache: TokenCache = mockk(relaxed = true),
    authRepository: AuthRepository = mockk(relaxed = true),
    networkProvider: NetworkProvider = mockk(relaxed = true),
    encryptionManager: EncryptionManager = mockk(relaxed = true),
    logger: VaultLogger = mockk(relaxed = true),
    retryHandler: RetryHandler = mockk(relaxed = true),
    cacheManager: CacheManager = mockk(relaxed = true),
    responseProcessor: ResponseProcessor = mockk(relaxed = true),
    encryptionStrategy: EncryptionStrategy = mockk(relaxed = true)
): BatchTokenizeOperation {
    return BatchTokenizeOperation(
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
