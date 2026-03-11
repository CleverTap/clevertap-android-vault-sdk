package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.api.TokenizationApi
import com.clevertap.android.zeropii.sdk.encryption.EncryptionManager
import com.clevertap.android.zeropii.sdk.model.BatchTokenItemResponse
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeResponse
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeSummary
import com.clevertap.android.zeropii.sdk.model.EncryptedResponse
import com.clevertap.android.zeropii.sdk.network.NetworkProvider
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger
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
        val batchResponse = BatchTokenizeResponse(
            results = apiResults,
            summary = BatchTokenizeSummary(2, 0, 2)
        )
        val response = Response.success(batchResponse)

        val expectedResult = BatchTokenizeRepoResult.Success(
            results = apiResults,
            summary = BatchTokenizeSummary(2, 0, 2)
        )

        every {
            mockResponseProcessor.processBatchTokenizeResponse(response)
        } returns expectedResult

        // Act
        val result = operation.processResponse(response)

        // Assert
        assertTrue("Should return success result", result is BatchTokenizeRepoResult.Success)
        val successResult = result as BatchTokenizeRepoResult.Success
        assertEquals("Should have correct total results", 2, successResult.results.size)
        assertEquals("Should have correct summary", 2, successResult.summary.processedCount)

        verify(exactly = 1) {
            mockResponseProcessor.processBatchTokenizeResponse(response)
        }
    }

    @Test
    fun shouldProcessEncryptedBatchTokenizeResponseCorrectly() {
        // Arrange
        val encryptedResponse = EncryptedResponse("encrypted-payload", "iv")
        val response = Response.success(encryptedResponse)

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
        val result = operation.processResponse(response)

        // Assert
        assertTrue("Should return success result", result is BatchTokenizeRepoResult.Success)
        val successResult = result as BatchTokenizeRepoResult.Success
        assertEquals("Should have decrypted results", 1, successResult.results.size)
        assertEquals(
            "Should have API result",
            "new-value",
            successResult.results[0].originalValue
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

        every {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                BatchTokenizeResponse::class.java
            )
        } returns null

        // Act
        val result = operation.processResponse(response)

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

        // Use NoEncryptionStrategy instead of WithEncryptionStrategy
        val operationWithWrongStrategy = createTestBatchTokenizeOperation(
            encryptionStrategy = mockk<NoEncryptionStrategy>()
        )

        // Act
        val result = operationWithWrongStrategy.processResponse(response)

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

        // Act
        val result = operation.processResponse(response)

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

        // Act
        val result = operation.processResponse(response)

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
}


// ====================================
// Helper Functions
// ====================================

private fun createTestBatchTokenizeOperation(
    authRepository: AuthRepository = mockk(relaxed = true),
    networkProvider: NetworkProvider = mockk(relaxed = true),
    encryptionManager: EncryptionManager = mockk(relaxed = true),
    logger: ZeroPiiLogger = mockk(relaxed = true),
    retryHandler: RetryHandler = mockk(relaxed = true),
    responseProcessor: ResponseProcessor = mockk(relaxed = true),
    encryptionStrategy: EncryptionStrategy = mockk(relaxed = true)
): BatchTokenizeOperation {
    return BatchTokenizeOperation(
        authRepository = authRepository,
        networkProvider = networkProvider,
        encryptionManager = encryptionManager,
        logger = logger,
        retryHandler = retryHandler,
        responseProcessor = responseProcessor,
        encryptionStrategy = encryptionStrategy
    )
}
