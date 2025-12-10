package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.api.TokenizationApi
import com.clevertap.android.vault.sdk.model.DetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.DetokenizeResponse
import com.clevertap.android.vault.sdk.model.EncryptedResponse
import com.clevertap.android.vault.sdk.network.NetworkProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.Response

// ====================================
// Cache State Check Tests
// ====================================
@RunWith(Parameterized::class)
class SingleDetokenizeOperationCacheCheckTest(
    private val inputToken: String,
    private val cachedValue: String?,
    private val cachedDataType: String?,
    private val expectedCacheResult: String, // "CompleteFromCache" or "NothingFromCache"
    private val description: String
) {
    private lateinit var mockCacheManager: CacheManager
    private lateinit var operation: SingleDetokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle cache check: {4}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf(
                    "555-67-8901",
                    "555-12-3456",
                    "string",
                    "CompleteFromCache",
                    "SSN token found in cache"
                ),
                arrayOf(
                    "xyz@example.com",
                    "john@example.com",
                    "string",
                    "CompleteFromCache",
                    "Email token found in cache"
                ),
                arrayOf(
                    "987654",
                    "123456",
                    "integer",
                    "CompleteFromCache",
                    "Integer token found in cache"
                ),
                arrayOf("new-token", null, null, "NothingFromCache", "Token not in cache"),
                arrayOf(
                    "empty-token",
                    "",
                    "string",
                    "CompleteFromCache",
                    "Empty value found in cache"
                ),
                arrayOf("🌟", "🚀", "string", "CompleteFromCache", "Unicode token found in cache"),
                arrayOf(
                    "bool-token-true",
                    "true",
                    "boolean",
                    "CompleteFromCache",
                    "Boolean token found in cache"
                ),
                arrayOf(
                    "float-token-123",
                    "123.45",
                    "float",
                    "CompleteFromCache",
                    "Float token found in cache"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockCacheManager = mockk()
        operation = createTestOperation(cacheManager = mockCacheManager)
    }

    @Test
    fun shouldCheckCacheStateCorrectly() {
        // Arrange
        val cacheResult = if (cachedValue != null) {
            CacheResult.ValueResult(cachedValue, cachedDataType ?: "string")
        } else {
            null
        }

        every { mockCacheManager.getValueFromCache(inputToken) } returns cacheResult

        // Act
        val result = operation.checkCacheWithState(inputToken)

        // Assert
        when (expectedCacheResult) {
            "CompleteFromCache" -> {
                assertTrue(
                    "Should be CompleteFromCache",
                    result is CacheCheckResult.CompleteFromCache
                )
                val completeResult =
                    result as CacheCheckResult.CompleteFromCache<String, DetokenizeRepoResult>
                assertEquals(
                    "Original request should match",
                    inputToken,
                    completeResult.originalRequest
                )

                val detokenizeResult = completeResult.result as DetokenizeRepoResult.Success
                assertEquals("Cached value should match", cachedValue, detokenizeResult.value)
                assertTrue("Should indicate value exists", detokenizeResult.exists)
                assertEquals(
                    "Data type should match",
                    cachedDataType ?: "string",
                    detokenizeResult.dataType
                )
            }

            "NothingFromCache" -> {
                assertTrue(
                    "Should be NothingFromCache",
                    result is CacheCheckResult.NothingFromCache
                )
                val nothingResult =
                    result as CacheCheckResult.NothingFromCache<String, DetokenizeRepoResult>
                assertEquals(
                    "Original request should match",
                    inputToken,
                    nothingResult.originalRequest
                )
                assertEquals(
                    "Uncached request should match",
                    inputToken,
                    nothingResult.uncachedRequest
                )
            }
        }

        verify(exactly = 1) { mockCacheManager.getValueFromCache(inputToken) }
    }

    private fun createTestOperation(cacheManager: CacheManager): SingleDetokenizeOperation {
        return SingleDetokenizeOperation(
            tokenCache = mockk(relaxed = true),
            authRepository = mockk(relaxed = true),
            networkProvider = mockk(relaxed = true),
            encryptionManager = mockk(relaxed = true),
            logger = mockk(relaxed = true),
            retryHandler = mockk(relaxed = true),
            cacheManager = cacheManager,
            responseProcessor = mockk(relaxed = true),
            encryptionStrategy = mockk(relaxed = true)
        )
    }
}

// ====================================
// Response Processing Tests
// ====================================
class SingleDetokenizeOperationResponseProcessingTest {
    private lateinit var mockResponseProcessor: ResponseProcessor
    private lateinit var mockEncryptionStrategy: WithEncryptionStrategy
    private lateinit var operation: SingleDetokenizeOperation

    private val testToken = "test-token-123"
    private val testValue = "test-value-456"
    private val testDataType = "string"

    @Before
    fun setUp() {
        mockResponseProcessor = mockk()
        mockEncryptionStrategy = mockk()
        operation = createTestOperation(
            responseProcessor = mockResponseProcessor,
            encryptionStrategy = mockEncryptionStrategy
        )
    }

    @Test
    fun shouldProcessRegularDetokenizeResponseCorrectly() {
        // Arrange
        val detokenizeResponse = DetokenizeResponse(
            value = testValue,
            exists = true,
            dataType = testDataType
        )
        val response = Response.success(detokenizeResponse)
        val cacheResult = CacheCheckResult.NothingFromCache<String, DetokenizeRepoResult>(
            originalRequest = testToken,
            uncachedRequest = testToken
        )
        val expectedResult = DetokenizeRepoResult.Success(testValue, true, testDataType)

        every { mockResponseProcessor.processDetokenizeResponse(response) } returns expectedResult

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return success result", result is DetokenizeRepoResult.Success)
        val successResult = result as DetokenizeRepoResult.Success
        assertEquals("Value should match", testValue, successResult.value)
        assertTrue("Should exist", successResult.exists)
        assertEquals("Data type should match", testDataType, successResult.dataType)

        verify(exactly = 1) { mockResponseProcessor.processDetokenizeResponse(response) }
    }

    @Test
    fun shouldProcessEncryptedResponseCorrectly() {
        // Arrange
        val encryptedResponse = EncryptedResponse(
            itp = "encrypted-payload",
            itv = "initialization-vector"
        )
        val response = Response.success(encryptedResponse)
        val cacheResult = CacheCheckResult.NothingFromCache<String, DetokenizeRepoResult>(
            originalRequest = testToken,
            uncachedRequest = testToken
        )
        val decryptedDetokenizeResponse = DetokenizeResponse(testValue, true, testDataType)

        every {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                DetokenizeResponse::class.java
            )
        } returns decryptedDetokenizeResponse

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return success result", result is DetokenizeRepoResult.Success)
        val successResult = result as DetokenizeRepoResult.Success
        assertEquals("Value should match", testValue, successResult.value)
        assertTrue("Should exist", successResult.exists)
        assertEquals("Data type should match", testDataType, successResult.dataType)

        verify(exactly = 1) {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                DetokenizeResponse::class.java
            )
        }
    }

    @Test
    fun shouldProcessDetokenizeResponseWithNullValue() {
        // Arrange
        val detokenizeResponse = DetokenizeResponse(
            value = null,
            exists = false,
            dataType = null
        )
        val response = Response.success(detokenizeResponse)
        val cacheResult = CacheCheckResult.NothingFromCache<String, DetokenizeRepoResult>(
            originalRequest = testToken,
            uncachedRequest = testToken
        )
        val expectedResult = DetokenizeRepoResult.Success(null, false, null)

        every { mockResponseProcessor.processDetokenizeResponse(response) } returns expectedResult

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return success result", result is DetokenizeRepoResult.Success)
        val successResult = result as DetokenizeRepoResult.Success
        assertEquals("Value should be null", null, successResult.value)
        assertFalse("Should not exist", successResult.exists)
        assertEquals("Data type should be null", null, successResult.dataType)

        verify(exactly = 1) { mockResponseProcessor.processDetokenizeResponse(response) }
    }

    @Test
    fun shouldHandleEncryptedResponseDecryptionFailure() {
        // Arrange
        val encryptedResponse = EncryptedResponse("encrypted-payload", "iv")
        val response = Response.success(encryptedResponse)
        val cacheResult = CacheCheckResult.NothingFromCache<String, DetokenizeRepoResult>(
            originalRequest = testToken,
            uncachedRequest = testToken
        )

        every {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                DetokenizeResponse::class.java
            )
        } returns null

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is DetokenizeRepoResult.Error)
        val errorResult = result as DetokenizeRepoResult.Error
        assertEquals(
            "Error message should indicate decryption failure",
            "Failed to decrypt response",
            errorResult.message
        )

        verify(exactly = 1) {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                DetokenizeResponse::class.java
            )
        }
    }

    @Test
    fun shouldHandleEncryptionStrategyMismatch() {
        // Arrange
        val encryptedResponse = EncryptedResponse("encrypted-payload", "iv")
        val response = Response.success(encryptedResponse)
        val cacheResult = CacheCheckResult.NothingFromCache<String, DetokenizeRepoResult>(
            originalRequest = testToken,
            uncachedRequest = testToken
        )

        // Use regular encryption strategy instead of WithEncryptionStrategy
        val operationWithWrongStrategy = createTestOperation(
            encryptionStrategy = mockk<NoEncryptionStrategy>()
        )

        // Act
        val result = operationWithWrongStrategy.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is DetokenizeRepoResult.Error)
        val errorResult = result as DetokenizeRepoResult.Error
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
        val cacheResult = CacheCheckResult.NothingFromCache<String, DetokenizeRepoResult>(
            originalRequest = testToken,
            uncachedRequest = testToken
        )

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is DetokenizeRepoResult.Error)
        val errorResult = result as DetokenizeRepoResult.Error
        assertTrue(
            "Error message should indicate operation failure",
            errorResult.message.contains("Single Detokenize failed")
        )
    }

    @Test
    fun shouldHandleHttpErrorResponse() {
        // Arrange
        val errorBody = ResponseBody.create(null, "Token not found")
        val response = Response.error<DetokenizeResponse>(404, errorBody)
        val cacheResult = CacheCheckResult.NothingFromCache<String, DetokenizeRepoResult>(
            originalRequest = testToken,
            uncachedRequest = testToken
        )

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is DetokenizeRepoResult.Error)
        val errorResult = result as DetokenizeRepoResult.Error
        assertTrue("Error message should contain HTTP code", errorResult.message.contains("404"))
        assertTrue(
            "Error message should contain operation type",
            errorResult.message.contains("Single Detokenize")
        )
    }

    private fun createTestOperation(
        responseProcessor: ResponseProcessor = mockk(relaxed = true),
        encryptionStrategy: EncryptionStrategy = mockk(relaxed = true)
    ): SingleDetokenizeOperation {
        return SingleDetokenizeOperation(
            tokenCache = mockk(relaxed = true),
            authRepository = mockk(relaxed = true),
            networkProvider = mockk(relaxed = true),
            encryptionManager = mockk(relaxed = true),
            logger = mockk(relaxed = true),
            retryHandler = mockk(relaxed = true),
            cacheManager = mockk(relaxed = true),
            responseProcessor = responseProcessor,
            encryptionStrategy = encryptionStrategy
        )
    }
}

// ====================================
// API Call Delegation Tests
// ====================================
@RunWith(Parameterized::class)
class SingleDetokenizeOperationApiCallTest(
    private val inputToken: String,
    private val accessToken: String,
) {
    private lateinit var mockEncryptionStrategy: EncryptionStrategy
    private lateinit var mockTokenizationApi: TokenizationApi
    private lateinit var mockNetworkProvider: NetworkProvider
    private lateinit var operation: SingleDetokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle API call: {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    "555-67-8901",
                    "bearer-token-123",
                ),
                arrayOf(
                    "987654",
                    "bearer-token-789",
                ),
                arrayOf(
                    "🌟",
                    "bearer-token-unicode",
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

        operation = createTestOperation(
            networkProvider = mockNetworkProvider,
            encryptionStrategy = mockEncryptionStrategy
        )
    }

    @Test
    fun shouldDelegateToEncryptionStrategyCorrectly() = runTest {
        // Arrange
        val expectedResponse =
            Response.success(DetokenizeResponse("original-value", true, "string"))
        coEvery {
            mockEncryptionStrategy.detokenize(
                mockTokenizationApi,
                accessToken,
                inputToken
            )
        } returns expectedResponse

        // Act
        val result = operation.makeApiCall(inputToken, accessToken)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        coVerify(exactly = 1) {
            mockEncryptionStrategy.detokenize(
                mockTokenizationApi,
                accessToken,
                inputToken
            )
        }
        verify(exactly = 1) { mockNetworkProvider.tokenizationApi }
    }

    private fun createTestOperation(
        networkProvider: NetworkProvider = mockk(relaxed = true),
        encryptionStrategy: EncryptionStrategy = mockk(relaxed = true)
    ): SingleDetokenizeOperation {
        return SingleDetokenizeOperation(
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
// Cache Update Tests
// ====================================
class SingleDetokenizeOperationCacheUpdateTest {
    private lateinit var mockCacheManager: CacheManager
    private lateinit var operation: SingleDetokenizeOperation

    @Before
    fun setUp() {
        mockCacheManager = mockk(relaxed = true)
        operation = createTestOperation(cacheManager = mockCacheManager)
    }

    @Test
    fun shouldUpdateCacheOnSuccessfulResultWithExistingValue() {
        // Arrange
        val inputToken = "test-token"
        val successResult = DetokenizeRepoResult.Success(
            value = "test-value",
            exists = true,
            dataType = "string"
        )

        // Act
        operation.updateCache(inputToken, successResult)

        // Assert
        verify(exactly = 1) {
            mockCacheManager.storeValueInCache(inputToken, "test-value", "string")
        }
    }

    @Test
    fun shouldNotUpdateCacheOnSuccessfulResultWithNonExistingValue() {
        // Arrange
        val inputToken = "test-token"
        val successResult = DetokenizeRepoResult.Success(
            value = null,
            exists = false,
            dataType = null
        )

        // Act
        operation.updateCache(inputToken, successResult)

        // Assert
        verify(exactly = 0) {
            mockCacheManager.storeValueInCache(any(), any(), any())
        }
    }

    @Test
    fun shouldNotUpdateCacheOnSuccessfulResultWithNullValue() {
        // Arrange
        val inputToken = "test-token"
        val successResult = DetokenizeRepoResult.Success(
            value = null,
            exists = true, // exists but value is null
            dataType = "string"
        )

        // Act
        operation.updateCache(inputToken, successResult)

        // Assert
        verify(exactly = 0) {
            mockCacheManager.storeValueInCache(any(), any(), any())
        }
    }

    @Test
    fun shouldNotUpdateCacheOnErrorResult() {
        // Arrange
        val inputToken = "test-token"
        val errorResult = DetokenizeRepoResult.Error("Token not found")

        // Act
        operation.updateCache(inputToken, errorResult)

        // Assert
        verify(exactly = 0) {
            mockCacheManager.storeValueInCache(any(), any(), any())
        }
    }

    @Test
    fun shouldUpdateCacheWithNullDataType() {
        // Arrange
        val inputToken = "test-token"
        val successResult = DetokenizeRepoResult.Success(
            value = "test-value",
            exists = true,
            dataType = null
        )

        // Act
        operation.updateCache(inputToken, successResult)

        // Assert
        verify(exactly = 1) {
            mockCacheManager.storeValueInCache(inputToken, "test-value", null)
        }
    }

    @Test
    fun shouldUpdateCacheWithEmptyValue() {
        // Arrange
        val inputToken = "test-token"
        val successResult = DetokenizeRepoResult.Success(
            value = "",
            exists = true,
            dataType = "string"
        )

        // Act
        operation.updateCache(inputToken, successResult)

        // Assert
        verify(exactly = 1) {
            mockCacheManager.storeValueInCache(inputToken, "", "string")
        }
    }

    private fun createTestOperation(cacheManager: CacheManager): SingleDetokenizeOperation {
        return SingleDetokenizeOperation(
            tokenCache = mockk(relaxed = true),
            authRepository = mockk(relaxed = true),
            networkProvider = mockk(relaxed = true),
            encryptionManager = mockk(relaxed = true),
            logger = mockk(relaxed = true),
            retryHandler = mockk(relaxed = true),
            cacheManager = cacheManager,
            responseProcessor = mockk(relaxed = true),
            encryptionStrategy = mockk(relaxed = true)
        )
    }
}

// ====================================
// Error Result Creation Tests
// ====================================
@RunWith(Parameterized::class)
class SingleDetokenizeOperationErrorResultTest(
    private val errorMessage: String,
    private val description: String
) {
    private lateinit var operation: SingleDetokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should create error result: {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("Token not found", "Token not found error"),
                arrayOf("Network connection failed", "Network error"),
                arrayOf("Decryption failed", "Decryption error"),
                arrayOf("", "Empty error message"),
            )
        }
    }

    @Before
    fun setUp() {
        operation = createTestOperation()
    }

    @Test
    fun shouldCreateErrorResultCorrectly() {
        // Act
        val result = operation.createErrorResult(errorMessage)

        // Assert
        assertTrue("Should return error result", result is DetokenizeRepoResult.Error)
        val errorResult = result as DetokenizeRepoResult.Error
        assertEquals("Error message should match", errorMessage, errorResult.message)
    }

    private fun createTestOperation(): SingleDetokenizeOperation {
        return SingleDetokenizeOperation(
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