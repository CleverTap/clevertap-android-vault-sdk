package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.api.TokenizationApi
import com.clevertap.android.vault.sdk.model.EncryptedResponse
import com.clevertap.android.vault.sdk.model.TokenizeRepoResult
import com.clevertap.android.vault.sdk.model.TokenizeResponse
import com.clevertap.android.vault.sdk.network.NetworkProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
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
class SingleTokenizeOperationCacheCheckTest(
    private val inputValue: String,
    private val cachedToken: String?,
    private val cachedDataType: String?,
    private val expectedCacheResult: String, // "CompleteFromCache" or "NothingFromCache"
    private val description: String
) {
    private lateinit var mockCacheManager: CacheManager
    private lateinit var operation: SingleTokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle cache check: {4}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf(
                    "555-12-3456",
                    "555-67-8901",
                    "string",
                    "CompleteFromCache",
                    "SSN found in cache"
                ),
                arrayOf(
                    "john@example.com",
                    "xyz@example.com",
                    "string",
                    "CompleteFromCache",
                    "Email found in cache"
                ),
                arrayOf(
                    "123456",
                    "987654",
                    "integer",
                    "CompleteFromCache",
                    "Integer found in cache"
                ),
                arrayOf("new-value", null, null, "NothingFromCache", "Value not in cache"),
                arrayOf(
                    "",
                    "empty-token",
                    "string",
                    "CompleteFromCache",
                    "Empty value found in cache"
                ),
                arrayOf("🚀", "🌟", "string", "CompleteFromCache", "Unicode value found in cache")
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
        val cacheResult = if (cachedToken != null) {
            CacheResult.TokenResult(cachedToken, cachedDataType ?: "string")
        } else {
            null
        }

        every { mockCacheManager.getTokenFromCache(inputValue) } returns cacheResult

        // Act
        val result = operation.checkCacheWithState(inputValue)

        // Assert
        when (expectedCacheResult) {
            "CompleteFromCache" -> {
                assertTrue(
                    "Should be CompleteFromCache",
                    result is CacheCheckResult.CompleteFromCache
                )
                val completeResult =
                    result as CacheCheckResult.CompleteFromCache<String, TokenizeRepoResult>
                assertEquals(
                    "Original request should match",
                    inputValue,
                    completeResult.originalRequest
                )

                val tokenizeResult = completeResult.result as TokenizeRepoResult.Success
                assertEquals("Cached token should match", cachedToken, tokenizeResult.token)
                assertTrue("Should indicate token exists", tokenizeResult.exists)
                assertFalse("Should not indicate newly created", tokenizeResult.newlyCreated)
                assertEquals(
                    "Data type should match",
                    cachedDataType ?: "string",
                    tokenizeResult.dataType
                )
            }

            "NothingFromCache" -> {
                assertTrue(
                    "Should be NothingFromCache",
                    result is CacheCheckResult.NothingFromCache
                )
                val nothingResult =
                    result as CacheCheckResult.NothingFromCache<String, TokenizeRepoResult>
                assertEquals(
                    "Original request should match",
                    inputValue,
                    nothingResult.originalRequest
                )
                assertEquals(
                    "Uncached request should match",
                    inputValue,
                    nothingResult.uncachedRequest
                )
            }
        }

        verify(exactly = 1) { mockCacheManager.getTokenFromCache(inputValue) }
    }

    private fun createTestOperation(cacheManager: CacheManager): SingleTokenizeOperation {
        return SingleTokenizeOperation(
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
class SingleTokenizeOperationResponseProcessingTest {
    private lateinit var mockResponseProcessor: ResponseProcessor
    private lateinit var mockEncryptionStrategy: WithEncryptionStrategy
    private lateinit var operation: SingleTokenizeOperation

    private val testToken = "test-token-123"
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
    fun shouldProcessRegularTokenizeResponseCorrectly() {
        // Arrange
        val tokenizeResponse = TokenizeResponse(
            token = testToken,
            exists = false,
            newlyCreated = true,
            dataType = testDataType
        )
        val response = Response.success(tokenizeResponse)
        val cacheResult = CacheCheckResult.NothingFromCache<String, TokenizeRepoResult>(
            originalRequest = "test-value",
            uncachedRequest = "test-value"
        )
        val expectedResult = TokenizeRepoResult.Success(testToken, false, true, testDataType)

        every { mockResponseProcessor.processTokenizeResponse(response) } returns expectedResult

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return success result", result is TokenizeRepoResult.Success)
        val successResult = result as TokenizeRepoResult.Success
        assertEquals("Token should match", testToken, successResult.token)
        assertFalse("Should not exist", successResult.exists)
        assertTrue("Should be newly created", successResult.newlyCreated)
        assertEquals("Data type should match", testDataType, successResult.dataType)

        verify(exactly = 1) { mockResponseProcessor.processTokenizeResponse(response) }
    }

    @Test
    fun shouldProcessEncryptedResponseCorrectly() {
        // Arrange
        val encryptedResponse = EncryptedResponse(
            itp = "encrypted-payload",
            itv = "initialization-vector"
        )
        val response = Response.success(encryptedResponse)
        val cacheResult = CacheCheckResult.NothingFromCache<String, TokenizeRepoResult>(
            originalRequest = "test-value",
            uncachedRequest = "test-value"
        )
        val decryptedTokenizeResponse = TokenizeResponse(testToken, false, true, testDataType)

        every {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                TokenizeResponse::class.java
            )
        } returns decryptedTokenizeResponse

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return success result", result is TokenizeRepoResult.Success)
        val successResult = result as TokenizeRepoResult.Success
        assertEquals("Token should match", testToken, successResult.token)
        assertEquals("Data type should match", testDataType, successResult.dataType)

        verify(exactly = 1) {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                TokenizeResponse::class.java
            )
        }
    }

    @Test
    fun shouldHandleEncryptedResponseDecryptionFailure() {
        // Arrange
        val encryptedResponse = EncryptedResponse("encrypted-payload", "iv")
        val response = Response.success(encryptedResponse)
        val cacheResult = CacheCheckResult.NothingFromCache<String, TokenizeRepoResult>(
            originalRequest = "test-value",
            uncachedRequest = "test-value"
        )

        every {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                TokenizeResponse::class.java
            )
        } returns null

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is TokenizeRepoResult.Error)
        val errorResult = result as TokenizeRepoResult.Error
        assertEquals(
            "Error message should indicate decryption failure",
            "Failed to decrypt response",
            errorResult.message
        )

        verify(exactly = 1) {
            mockEncryptionStrategy.decryptResponse(
                encryptedResponse,
                TokenizeResponse::class.java
            )
        }
    }

    @Test
    fun shouldHandleUnknownResponseType() {
        // Arrange
        val unknownResponse = "unknown-response-type"
        val response = Response.success(unknownResponse)
        val cacheResult = CacheCheckResult.NothingFromCache<String, TokenizeRepoResult>(
            originalRequest = "test-value",
            uncachedRequest = "test-value"
        )

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is TokenizeRepoResult.Error)
        val errorResult = result as TokenizeRepoResult.Error
        assertTrue(
            "Error message should indicate operation failure",
            errorResult.message.contains("Single Tokenize failed")
        )
    }

    @Test
    fun shouldHandleHttpErrorResponse() {
        // Arrange
        val errorBody = "Server Error".toResponseBody()
        val response = Response.error<TokenizeResponse>(500, errorBody)
        val cacheResult = CacheCheckResult.NothingFromCache<String, TokenizeRepoResult>(
            originalRequest = "test-value",
            uncachedRequest = "test-value"
        )

        // Act
        val result = operation.processResponseWithCacheData(response, cacheResult)

        // Assert
        assertTrue("Should return error result", result is TokenizeRepoResult.Error)
        val errorResult = result as TokenizeRepoResult.Error
        assertTrue("Error message should contain HTTP code", errorResult.message.contains("500"))
        assertTrue(
            "Error message should contain operation type",
            errorResult.message.contains("Single Tokenize")
        )
    }

    private fun createTestOperation(
        responseProcessor: ResponseProcessor = mockk(relaxed = true),
        encryptionStrategy: EncryptionStrategy = mockk(relaxed = true)
    ): SingleTokenizeOperation {
        return SingleTokenizeOperation(
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
class SingleTokenizeOperationApiCallTest(
    private val inputValue: String,
    private val accessToken: String,
) {
    private lateinit var mockEncryptionStrategy: EncryptionStrategy
    private lateinit var mockTokenizationApi: TokenizationApi
    private lateinit var mockNetworkProvider: NetworkProvider
    private lateinit var operation: SingleTokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle API call: {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    "555-12-3456",
                    "bearer-token-123",
                ),
                arrayOf(
                    "🚀",
                    "bearer-token-unicode",
                ),
                arrayOf(
                    "",
                    "bearer-token-empty",
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
        val expectedResponse = Response.success(TokenizeResponse("token", false, true, "string"))
        coEvery {
            mockEncryptionStrategy.tokenize(
                mockTokenizationApi,
                accessToken,
                inputValue
            )
        } returns expectedResponse

        // Act
        val result = operation.makeApiCall(inputValue, accessToken)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        coVerify(exactly = 1) {
            mockEncryptionStrategy.tokenize(
                mockTokenizationApi,
                accessToken,
                inputValue
            )
        }
        verify(exactly = 1) { mockNetworkProvider.tokenizationApi }
    }

    private fun createTestOperation(
        networkProvider: NetworkProvider = mockk(relaxed = true),
        encryptionStrategy: EncryptionStrategy = mockk(relaxed = true)
    ): SingleTokenizeOperation {
        return SingleTokenizeOperation(
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
class SingleTokenizeOperationCacheUpdateTest {
    private lateinit var mockCacheManager: CacheManager
    private lateinit var operation: SingleTokenizeOperation

    @Before
    fun setUp() {
        mockCacheManager = mockk(relaxed = true)
        operation = createTestOperation(cacheManager = mockCacheManager)
    }

    @Test
    fun shouldUpdateCacheOnSuccessfulResult() {
        // Arrange
        val inputValue = "test-value"
        val successResult = TokenizeRepoResult.Success(
            token = "test-token",
            exists = false,
            newlyCreated = true,
            dataType = "string"
        )

        // Act
        operation.updateCache(inputValue, successResult)

        // Assert
        verify(exactly = 1) {
            mockCacheManager.storeTokenInCache(inputValue, "test-token", "string")
        }
    }

    @Test
    fun shouldNotUpdateCacheOnErrorResult() {
        // Arrange
        val inputValue = "test-value"
        val errorResult = TokenizeRepoResult.Error("Some error occurred")

        // Act
        operation.updateCache(inputValue, errorResult)

        // Assert
        verify(exactly = 0) {
            mockCacheManager.storeTokenInCache(any(), any(), any())
        }
    }

    @Test
    fun shouldUpdateCacheWithNullDataType() {
        // Arrange
        val inputValue = "test-value"
        val successResult = TokenizeRepoResult.Success(
            token = "test-token",
            exists = false,
            newlyCreated = true,
            dataType = null
        )

        // Act
        operation.updateCache(inputValue, successResult)

        // Assert
        verify(exactly = 1) {
            mockCacheManager.storeTokenInCache(inputValue, "test-token", null)
        }
    }

    private fun createTestOperation(cacheManager: CacheManager): SingleTokenizeOperation {
        return SingleTokenizeOperation(
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
@RunWith(Parameterized::class)
class SingleTokenizeOperationErrorResultTest(
    private val errorMessage: String,
    private val description: String
) {
    private lateinit var operation: SingleTokenizeOperation

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should create error result: {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("Network connection failed", "Network error"),
                arrayOf("Server internal error", "Server error"),
                arrayOf("", "Empty error message"),
                arrayOf("Error with special characters: !@#$%", "Special characters error")
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
        assertTrue("Should return error result", result is TokenizeRepoResult.Error)
        val errorResult = result as TokenizeRepoResult.Error
        assertEquals("Error message should match", errorMessage, errorResult.message)
    }

    private fun createTestOperation(): SingleTokenizeOperation {
        return SingleTokenizeOperation(
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