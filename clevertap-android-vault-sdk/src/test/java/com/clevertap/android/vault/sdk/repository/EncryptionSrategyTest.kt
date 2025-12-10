package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.api.TokenizationApi
import com.clevertap.android.vault.sdk.encryption.DecryptionFailure
import com.clevertap.android.vault.sdk.encryption.DecryptionSuccess
import com.clevertap.android.vault.sdk.encryption.EncryptionFailure
import com.clevertap.android.vault.sdk.encryption.EncryptionManager
import com.clevertap.android.vault.sdk.encryption.EncryptionSuccess
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRequest
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeSummary
import com.clevertap.android.vault.sdk.model.BatchTokenizeRequest
import com.clevertap.android.vault.sdk.model.BatchTokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeSummary
import com.clevertap.android.vault.sdk.model.DetokenizeRequest
import com.clevertap.android.vault.sdk.model.DetokenizeResponse
import com.clevertap.android.vault.sdk.model.EncryptedRequest
import com.clevertap.android.vault.sdk.model.EncryptedResponse
import com.clevertap.android.vault.sdk.model.TokenizeRequest
import com.clevertap.android.vault.sdk.model.TokenizeResponse
import com.clevertap.android.vault.sdk.util.VaultLogger
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.Response

// ====================================
// NoEncryptionStrategy Tests
// ====================================
@RunWith(Parameterized::class)
class NoEncryptionStrategyTest(
    private val inputValue: String,
    private val accessToken: String,
    private val description: String
) {
    private lateinit var mockTokenizationApi: TokenizationApi
    private lateinit var mockLogger: VaultLogger
    private lateinit var noEncryptionStrategy: NoEncryptionStrategy

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle non-encrypted operations: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("john.doe@example.com", "bearer-token-123", "Email tokenization"),
                arrayOf("555-12-3456", "bearer-token-456", "SSN tokenization"),
                arrayOf("4111111111111111", "bearer-token-789", "Credit card tokenization"),
                arrayOf("🌟🚀💯", "bearer-token-unicode", "Unicode content"),
                arrayOf("", "bearer-token-empty", "Empty value"),
                arrayOf("very-long-value-" + "x".repeat(1000), "bearer-token-long", "Long value"),
                arrayOf("multi\nline\tvalue", "bearer-token-multiline", "Multiline content")
            )
        }
    }

    @Before
    fun setUp() {
        mockTokenizationApi = mockk()
        mockLogger = mockk(relaxed = true)
        noEncryptionStrategy = NoEncryptionStrategy(mockLogger)
    }

    @Test
    fun shouldMakeNonEncryptedTokenizeCall() = runBlocking {
        // Arrange
        val expectedResponse = Response.success(
            TokenizeResponse("token-123", false, true, "string")
        )
        coEvery {
            mockTokenizationApi.tokenize("Bearer $accessToken", TokenizeRequest(inputValue))
        } returns expectedResponse

        // Act
        val result = noEncryptionStrategy.tokenize(mockTokenizationApi, accessToken, inputValue)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        coVerify(exactly = 1) {
            mockTokenizationApi.tokenize("Bearer $accessToken", TokenizeRequest(inputValue))
        }
    }

    @Test
    fun shouldMakeNonEncryptedDetokenizeCall() = runBlocking {
        // Arrange
        val expectedResponse = Response.success(
            DetokenizeResponse("decrypted-value", true, "string")
        )
        coEvery {
            mockTokenizationApi.detokenize("Bearer $accessToken", DetokenizeRequest(inputValue))
        } returns expectedResponse

        // Act
        val result = noEncryptionStrategy.detokenize(mockTokenizationApi, accessToken, inputValue)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        coVerify(exactly = 1) {
            mockTokenizationApi.detokenize("Bearer $accessToken", DetokenizeRequest(inputValue))
        }
    }

    @Test
    fun shouldMakeNonEncryptedBatchTokenizeCall() = runBlocking {
        // Arrange
        val values = listOf(inputValue, "additional-value")
        val expectedResponse = Response.success(
            BatchTokenizeResponse(
                results = emptyList(),
                summary = BatchTokenizeSummary(2, 0, 2)
            )
        )
        coEvery {
            mockTokenizationApi.batchTokenize("Bearer $accessToken", BatchTokenizeRequest(values))
        } returns expectedResponse

        // Act
        val result = noEncryptionStrategy.batchTokenize(mockTokenizationApi, accessToken, values)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        coVerify(exactly = 1) {
            mockTokenizationApi.batchTokenize("Bearer $accessToken", BatchTokenizeRequest(values))
        }
    }

    @Test
    fun shouldMakeNonEncryptedBatchDetokenizeCall() = runBlocking {
        // Arrange
        val tokens = listOf(inputValue, "additional-token")
        val expectedResponse = Response.success(
            BatchDetokenizeResponse(
                results = emptyList(),
                summary = BatchDetokenizeSummary(2, 2, 0)
            )
        )
        coEvery {
            mockTokenizationApi.batchDetokenize(
                "Bearer $accessToken",
                BatchDetokenizeRequest(tokens)
            )
        } returns expectedResponse

        // Act
        val result = noEncryptionStrategy.batchDetokenize(mockTokenizationApi, accessToken, tokens)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        coVerify(exactly = 1) {
            mockTokenizationApi.batchDetokenize(
                "Bearer $accessToken",
                BatchDetokenizeRequest(tokens)
            )
        }
    }
}

// ====================================
// WithEncryptionStrategy Tests
// ====================================
class WithEncryptionStrategyTest {
    private lateinit var mockEncryptionManager: EncryptionManager
    private lateinit var mockLogger: VaultLogger
    private lateinit var mockFallbackStrategy: NoEncryptionStrategy
    private lateinit var mockApi: TokenizationApi
    private lateinit var strategy: WithEncryptionStrategy

    private val accessToken = "test-access-token"
    private val bearerToken = "Bearer $accessToken"
    private val gson = Gson()

    @Before
    fun setUp() {
        mockEncryptionManager = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        mockFallbackStrategy = mockk(relaxed = true)
        mockApi = mockk(relaxed = true)
        strategy = WithEncryptionStrategy(mockEncryptionManager, mockLogger, mockFallbackStrategy)
    }

    @Test
    fun shouldTokenizeWithEncryptionSuccess() = runTest {
        // Arrange
        val testValue = "test-value-123"
        val encryptedRequest = EncryptedRequest("encrypted-payload", "session-key", "iv")
        val encryptedResponse = EncryptedResponse("encrypted-response", "response-iv")
        val expectedResponse = Response.success(encryptedResponse)
        val tokenizeRequestJson = gson.toJson(TokenizeRequest(testValue))

        every {
            mockEncryptionManager.encrypt(tokenizeRequestJson)
        } returns EncryptionSuccess("encrypted-payload", "session-key", "iv")

        every { mockEncryptionManager.isEnabled() } returns true

        coEvery {
            mockApi.tokenizeEncrypted(bearerToken, true, encryptedRequest)
        } returns expectedResponse

        // Act
        val result = strategy.tokenize(mockApi, accessToken, testValue)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        verify(exactly = 1) { mockEncryptionManager.encrypt(tokenizeRequestJson) }
        coVerify(exactly = 1) {
            mockApi.tokenizeEncrypted(bearerToken, true, encryptedRequest)
        }
    }

    @Test
    fun shouldDetokenizeWithEncryptionSuccess() = runTest {
        // Arrange
        val testToken = "test-token-456"
        val encryptedRequest = EncryptedRequest("encrypted-payload", "session-key", "iv")
        val encryptedResponse = EncryptedResponse("encrypted-response", "response-iv")
        val expectedResponse = Response.success(encryptedResponse)
        val detokenizeRequestJson = gson.toJson(DetokenizeRequest(testToken))

        every { mockEncryptionManager.isEnabled() } returns true
        every {
            mockEncryptionManager.encrypt(detokenizeRequestJson)
        } returns EncryptionSuccess("encrypted-payload", "session-key", "iv")

        coEvery {
            mockApi.detokenizeEncrypted(bearerToken, true, encryptedRequest)
        } returns expectedResponse

        // Act
        val result = strategy.detokenize(mockApi, accessToken, testToken)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        verify(exactly = 1) { mockEncryptionManager.encrypt(detokenizeRequestJson) }
        coVerify(exactly = 1) {
            mockApi.detokenizeEncrypted(bearerToken, true, encryptedRequest)
        }
    }

    @Test
    fun shouldBatchTokenizeWithEncryptionSuccess() = runTest {
        // Arrange
        val testValues = listOf("value1", "value2", "value3")
        val encryptedRequest = EncryptedRequest("encrypted-payload", "session-key", "iv")
        val encryptedResponse = EncryptedResponse("encrypted-response", "response-iv")
        val expectedResponse = Response.success(encryptedResponse)
        val batchRequestJson = gson.toJson(BatchTokenizeRequest(testValues))

        every { mockEncryptionManager.isEnabled() } returns true
        every {
            mockEncryptionManager.encrypt(batchRequestJson)
        } returns EncryptionSuccess("encrypted-payload", "session-key", "iv")

        coEvery {
            mockApi.batchTokenizeEncrypted(bearerToken, true, encryptedRequest)
        } returns expectedResponse

        // Act
        val result = strategy.batchTokenize(mockApi, accessToken, testValues)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        verify(exactly = 1) { mockEncryptionManager.encrypt(batchRequestJson) }
        coVerify(exactly = 1) {
            mockApi.batchTokenizeEncrypted(bearerToken, true, encryptedRequest)
        }
    }

    @Test
    fun shouldBatchDetokenizeWithEncryptionSuccess() = runTest {
        // Arrange
        val testTokens = listOf("token1", "token2", "token3")
        val encryptedRequest = EncryptedRequest("encrypted-payload", "session-key", "iv")
        val encryptedResponse = EncryptedResponse("encrypted-response", "response-iv")
        val expectedResponse = Response.success(encryptedResponse)
        val batchRequestJson = gson.toJson(BatchDetokenizeRequest(testTokens))

        every { mockEncryptionManager.isEnabled() } returns true
        every {
            mockEncryptionManager.encrypt(batchRequestJson)
        } returns EncryptionSuccess("encrypted-payload", "session-key", "iv")

        coEvery {
            mockApi.batchDetokenizeEncrypted(bearerToken, true, encryptedRequest)
        } returns expectedResponse

        // Act
        val result = strategy.batchDetokenize(mockApi, accessToken, testTokens)

        // Assert
        assertEquals("Should return expected response", expectedResponse, result)
        verify(exactly = 1) { mockEncryptionManager.encrypt(batchRequestJson) }
        coVerify(exactly = 1) {
            mockApi.batchDetokenizeEncrypted(bearerToken, true, encryptedRequest)
        }
    }
}

// ====================================
// WithEncryptionStrategy Fallback Level 1 Tests
// ====================================
@RunWith(Parameterized::class)
class WithEncryptionStrategyFallbackLevel1Test(
    private val operationType: String,
    private val description: String
) {
    private lateinit var mockEncryptionManager: EncryptionManager
    private lateinit var mockLogger: VaultLogger
    private lateinit var mockFallbackStrategy: NoEncryptionStrategy
    private lateinit var mockApi: TokenizationApi
    private lateinit var strategy: WithEncryptionStrategy

    private val accessToken = "test-access-token"

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should fallback for: {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("tokenize", "Single tokenization"),
                arrayOf("detokenize", "Single detokenization"),
                arrayOf("batchTokenize", "Batch tokenization"),
                arrayOf("batchDetokenize", "Batch detokenization")
            )
        }
    }

    @Before
    fun setUp() {
        mockEncryptionManager = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        mockFallbackStrategy = mockk(relaxed = true)
        mockApi = mockk(relaxed = true)
        strategy = WithEncryptionStrategy(mockEncryptionManager, mockLogger, mockFallbackStrategy)
    }

    @Test
    fun shouldFallbackWhenEncryptionManagerDisabled() = runTest {
        // Arrange
        val fallbackResponse = createMockResponse()
        every { mockEncryptionManager.isEnabled() } returns false
        setupFallbackMock(fallbackResponse)

        // Act
        val result = executeOperation()

        // Assert
        assertEquals("Should return fallback response", fallbackResponse, result)
        verifyFallbackCalled()
        verify(exactly = 0) { mockEncryptionManager.encrypt(any()) }
    }

    @Test
    fun shouldFallbackWhenEncryptionDisabledDueToFailure() = runTest {
        // Arrange - First, disable encryption due to a 419 error
        every { mockEncryptionManager.isEnabled() } returns true
        every {
            mockEncryptionManager.encrypt(any())
        } returns EncryptionSuccess("encrypted", "key", "iv")

        val error419Response = Response.error<EncryptedResponse>(
            419, ResponseBody.create(null, "Backend decryption failed")
        )
        val fallbackResponse = createMockResponse()

        setupEncryptedApiMock(error419Response)
        setupFallbackMock(fallbackResponse)

        // Act - First call should trigger 419 and set encryptionDisabledDueToFailure = true
        val firstResult = executeOperation()

        // Second call should immediately use fallback without attempting encryption
        val secondResult = executeOperation()

        // Assert
        assertEquals("First call should return fallback response", fallbackResponse, firstResult)
        assertEquals(
            "Second call should also return fallback response",
            fallbackResponse,
            secondResult
        )

        // Verify fallback strategy was called multiple times
        verifyFallbackCalledAtLeast(2)
        verify(exactly = 1) { mockEncryptionManager.encrypt(any()) }
    }

    private fun createMockResponse(): Response<*> {
        return when (operationType) {
            "tokenize" -> Response.success(
                TokenizeResponse(
                    "fallback-token",
                    false,
                    true,
                    "string"
                )
            )

            "detokenize" -> Response.success(DetokenizeResponse("fallback-value", true, "string"))
            "batchTokenize" -> Response.success(
                BatchTokenizeResponse(
                    emptyList(),
                    BatchTokenizeSummary(0, 0, 0)
                )
            )

            "batchDetokenize" -> Response.success(
                BatchDetokenizeResponse(
                    emptyList(),
                    BatchDetokenizeSummary(0, 0, 0)
                )
            )

            else -> throw IllegalArgumentException("Unknown operation type: $operationType")
        }
    }

    private fun setupFallbackMock(response: Response<*>) {
        when (operationType) {
            "tokenize" -> coEvery {
                mockFallbackStrategy.tokenize(mockApi, accessToken, "test-value")
            } returns response

            "detokenize" -> coEvery {
                mockFallbackStrategy.detokenize(mockApi, accessToken, "test-token")
            } returns response

            "batchTokenize" -> coEvery {
                mockFallbackStrategy.batchTokenize(mockApi, accessToken, listOf("value1", "value2"))
            } returns response

            "batchDetokenize" -> coEvery {
                mockFallbackStrategy.batchDetokenize(
                    mockApi,
                    accessToken,
                    listOf("token1", "token2")
                )
            } returns response
        }
    }

    private fun setupEncryptedApiMock(response: Response<EncryptedResponse>) {
        when (operationType) {
            "tokenize" -> coEvery {
                mockApi.tokenizeEncrypted(any(), any(), any())
            } returns response

            "detokenize" -> coEvery {
                mockApi.detokenizeEncrypted(any(), any(), any())
            } returns response

            "batchTokenize" -> coEvery {
                mockApi.batchTokenizeEncrypted(any(), any(), any())
            } returns response

            "batchDetokenize" -> coEvery {
                mockApi.batchDetokenizeEncrypted(any(), any(), any())
            } returns response
        }
    }

    private suspend fun executeOperation(): Response<*> {
        return when (operationType) {
            "tokenize" -> strategy.tokenize(mockApi, accessToken, "test-value")
            "detokenize" -> strategy.detokenize(mockApi, accessToken, "test-token")
            "batchTokenize" -> strategy.batchTokenize(
                mockApi,
                accessToken,
                listOf("value1", "value2")
            )

            "batchDetokenize" -> strategy.batchDetokenize(
                mockApi,
                accessToken,
                listOf("token1", "token2")
            )

            else -> throw IllegalArgumentException("Unknown operation type: $operationType")
        }
    }

    private fun verifyFallbackCalled() {
        when (operationType) {
            "tokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.tokenize(mockApi, accessToken, "test-value")
            }

            "detokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.detokenize(mockApi, accessToken, "test-token")
            }

            "batchTokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.batchTokenize(mockApi, accessToken, listOf("value1", "value2"))
            }

            "batchDetokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.batchDetokenize(
                    mockApi,
                    accessToken,
                    listOf("token1", "token2")
                )
            }
        }
    }

    private fun verifyFallbackCalledAtLeast(times: Int) {
        when (operationType) {
            "tokenize" -> coVerify(atLeast = times) {
                mockFallbackStrategy.tokenize(mockApi, accessToken, "test-value")
            }

            "detokenize" -> coVerify(atLeast = times) {
                mockFallbackStrategy.detokenize(mockApi, accessToken, "test-token")
            }

            "batchTokenize" -> coVerify(atLeast = times) {
                mockFallbackStrategy.batchTokenize(mockApi, accessToken, listOf("value1", "value2"))
            }

            "batchDetokenize" -> coVerify(atLeast = times) {
                mockFallbackStrategy.batchDetokenize(
                    mockApi,
                    accessToken,
                    listOf("token1", "token2")
                )
            }
        }
    }
}

// ====================================
// WithEncryptionStrategy Fallback Level 2 Tests
// ====================================
@RunWith(Parameterized::class)
class WithEncryptionStrategyFallbackLevel2Test(
    private val operationType: String,
    private val description: String
) {
    private lateinit var mockEncryptionManager: EncryptionManager
    private lateinit var mockLogger: VaultLogger
    private lateinit var mockFallbackStrategy: NoEncryptionStrategy
    private lateinit var mockApi: TokenizationApi
    private lateinit var strategy: WithEncryptionStrategy

    private val accessToken = "test-access-token"

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should fallback when encryption fails for: {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("tokenize", "Single tokenization"),
                arrayOf("detokenize", "Single detokenization"),
                arrayOf("batchTokenize", "Batch tokenization"),
                arrayOf("batchDetokenize", "Batch detokenization")
            )
        }
    }

    @Before
    fun setUp() {
        mockEncryptionManager = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        mockFallbackStrategy = mockk(relaxed = true)
        mockApi = mockk(relaxed = true)
        strategy = WithEncryptionStrategy(mockEncryptionManager, mockLogger, mockFallbackStrategy)
    }

    @Test
    fun shouldFallbackWhenEncryptionFails() = runTest {
        // Arrange
        val fallbackResponse = createMockResponse()

        every { mockEncryptionManager.isEnabled() } returns true
        every {
            mockEncryptionManager.encrypt(any())
        } returns EncryptionFailure("Encryption failed")

        setupFallbackMock(fallbackResponse)

        // Act
        val result = executeOperation()

        // Assert
        assertEquals("Should return fallback response", fallbackResponse, result)
        verify(exactly = 1) { mockEncryptionManager.encrypt(any()) }
        verifyFallbackCalled()
        verifyNoEncryptedApiCalled()
    }

    private fun createMockResponse(): Response<*> {
        return when (operationType) {
            "tokenize" -> Response.success(
                TokenizeResponse(
                    "fallback-token",
                    false,
                    true,
                    "string"
                )
            )

            "detokenize" -> Response.success(DetokenizeResponse("fallback-value", true, "string"))
            "batchTokenize" -> Response.success(
                BatchTokenizeResponse(
                    emptyList(),
                    BatchTokenizeSummary(0, 0, 0)
                )
            )

            "batchDetokenize" -> Response.success(
                BatchDetokenizeResponse(
                    emptyList(),
                    BatchDetokenizeSummary(0, 0, 0)
                )
            )

            else -> throw IllegalArgumentException("Unknown operation type: $operationType")
        }
    }

    private fun setupFallbackMock(response: Response<*>) {
        when (operationType) {
            "tokenize" -> coEvery {
                mockFallbackStrategy.tokenize(mockApi, accessToken, "test-value")
            } returns response

            "detokenize" -> coEvery {
                mockFallbackStrategy.detokenize(mockApi, accessToken, "test-token")
            } returns response

            "batchTokenize" -> coEvery {
                mockFallbackStrategy.batchTokenize(mockApi, accessToken, listOf("value1", "value2"))
            } returns response

            "batchDetokenize" -> coEvery {
                mockFallbackStrategy.batchDetokenize(
                    mockApi,
                    accessToken,
                    listOf("token1", "token2")
                )
            } returns response
        }
    }

    private suspend fun executeOperation(): Response<*> {
        return when (operationType) {
            "tokenize" -> strategy.tokenize(mockApi, accessToken, "test-value")
            "detokenize" -> strategy.detokenize(mockApi, accessToken, "test-token")
            "batchTokenize" -> strategy.batchTokenize(
                mockApi,
                accessToken,
                listOf("value1", "value2")
            )

            "batchDetokenize" -> strategy.batchDetokenize(
                mockApi,
                accessToken,
                listOf("token1", "token2")
            )

            else -> throw IllegalArgumentException("Unknown operation type: $operationType")
        }
    }

    private fun verifyFallbackCalled() {
        when (operationType) {
            "tokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.tokenize(mockApi, accessToken, "test-value")
            }

            "detokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.detokenize(mockApi, accessToken, "test-token")
            }

            "batchTokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.batchTokenize(mockApi, accessToken, listOf("value1", "value2"))
            }

            "batchDetokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.batchDetokenize(
                    mockApi,
                    accessToken,
                    listOf("token1", "token2")
                )
            }
        }
    }

    private fun verifyNoEncryptedApiCalled() {
        when (operationType) {
            "tokenize" -> coVerify(exactly = 0) { mockApi.tokenizeEncrypted(any(), any(), any()) }
            "detokenize" -> coVerify(exactly = 0) {
                mockApi.detokenizeEncrypted(
                    any(),
                    any(),
                    any()
                )
            }

            "batchTokenize" -> coVerify(exactly = 0) {
                mockApi.batchTokenizeEncrypted(
                    any(),
                    any(),
                    any()
                )
            }

            "batchDetokenize" -> coVerify(exactly = 0) {
                mockApi.batchDetokenizeEncrypted(
                    any(),
                    any(),
                    any()
                )
            }
        }
    }
}

// ====================================
// WithEncryptionStrategy Fallback Level 3 Tests
// ====================================
@RunWith(Parameterized::class)
class WithEncryptionStrategyFallbackLevel3Test(
    private val operationType: String,
    private val errorCode: Int,
    private val errorType: String,
    private val description: String
) {
    private lateinit var mockEncryptionManager: EncryptionManager
    private lateinit var mockLogger: VaultLogger
    private lateinit var mockFallbackStrategy: NoEncryptionStrategy
    private lateinit var mockApi: TokenizationApi
    private lateinit var strategy: WithEncryptionStrategy

    private val accessToken = "test-access-token"
    private val bearerToken = "Bearer $accessToken"

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle {0} with: {3}")
        fun data(): Collection<Array<Any>> {
            val operations = listOf("tokenize", "detokenize", "batchTokenize", "batchDetokenize")
            val errorScenarios = listOf(
                arrayOf(419, "response", "419 error code in response"),
                arrayOf(419, "exception", "419 HttpException"),
                arrayOf(500, "exception_no_fallback", "Non-419 HttpException should not fallback")
            )

            val result = mutableListOf<Array<Any>>()
            for (operation in operations) {
                for (errorScenario in errorScenarios) {
                    result.add(
                        arrayOf(
                            operation,
                            errorScenario[0], // errorCode
                            errorScenario[1], // errorType
                            "$operation - ${errorScenario[2]}" // description
                        )
                    )
                }
            }
            return result
        }
    }

    @Before
    fun setUp() {
        mockEncryptionManager = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        mockFallbackStrategy = mockk(relaxed = true)
        mockApi = mockk(relaxed = true)
        strategy = WithEncryptionStrategy(mockEncryptionManager, mockLogger, mockFallbackStrategy)
    }

    @Test
    fun shouldHandle419ErrorsCorrectly() = runTest {
        // Arrange
        val encryptedRequest = EncryptedRequest("encrypted", "key", "iv")
        val fallbackResponse = createMockResponse()

        every { mockEncryptionManager.isEnabled() } returns true
        every {
            mockEncryptionManager.encrypt(any())
        } returns EncryptionSuccess("encrypted", "key", "iv")

        setupFallbackMock(fallbackResponse)

        when (errorType) {
            "response" -> {
                val errorResponse = Response.error<EncryptedResponse>(
                    errorCode, ResponseBody.create(null, "Backend decryption failed")
                )
                setupEncryptedApiMock(errorResponse)

                // Act
                val result = executeOperation()

                // Assert
                assertEquals("Should return fallback response", fallbackResponse, result)
                verifyEncryptedApiCalled()
                verifyFallbackCalled()
            }

            "exception" -> {
                setupEncryptedApiException(
                    retrofit2.HttpException(
                        Response.error<Any>(
                            errorCode,
                            ResponseBody.create(null, "Backend decryption failed")
                        )
                    )
                )

                // Act
                val result = executeOperation()

                // Assert
                assertEquals("Should return fallback response", fallbackResponse, result)
                verifyEncryptedApiCalled()
                verifyFallbackCalled()
            }

            "exception_no_fallback" -> {
                val nonFallbackException = retrofit2.HttpException(
                    Response.error<Any>(errorCode, ResponseBody.create(null, "Server error"))
                )

                setupEncryptedApiException(nonFallbackException)

                // Act & Assert
                try {
                    executeOperation()
                    fail("Expected HttpException to be thrown")
                } catch (e: retrofit2.HttpException) {
                    assertEquals("Should propagate non-419 HTTP exceptions", errorCode, e.code())
                }

                verifyNoFallbackCalled()
            }
        }
    }

    private fun createMockResponse(): Response<*> {
        return when (operationType) {
            "tokenize" -> Response.success(
                TokenizeResponse(
                    "fallback-token",
                    false,
                    true,
                    "string"
                )
            )

            "detokenize" -> Response.success(DetokenizeResponse("fallback-value", true, "string"))
            "batchTokenize" -> Response.success(
                BatchTokenizeResponse(
                    emptyList(),
                    BatchTokenizeSummary(0, 0, 0)
                )
            )

            "batchDetokenize" -> Response.success(
                BatchDetokenizeResponse(
                    emptyList(),
                    BatchDetokenizeSummary(0, 0, 0)
                )
            )

            else -> throw IllegalArgumentException("Unknown operation type: $operationType")
        }
    }

    private fun setupFallbackMock(response: Response<*>) {
        when (operationType) {
            "tokenize" -> coEvery {
                mockFallbackStrategy.tokenize(mockApi, accessToken, any())
            } returns response

            "detokenize" -> coEvery {
                mockFallbackStrategy.detokenize(mockApi, accessToken, any())
            } returns response

            "batchTokenize" -> coEvery {
                mockFallbackStrategy.batchTokenize(mockApi, accessToken, any())
            } returns response

            "batchDetokenize" -> coEvery {
                mockFallbackStrategy.batchDetokenize(mockApi, accessToken, any())
            } returns response
        }
    }

    private fun setupEncryptedApiMock(response: Response<EncryptedResponse>) {
        when (operationType) {
            "tokenize" -> coEvery {
                mockApi.tokenizeEncrypted(bearerToken, true, any())
            } returns response

            "detokenize" -> coEvery {
                mockApi.detokenizeEncrypted(bearerToken, true, any())
            } returns response

            "batchTokenize" -> coEvery {
                mockApi.batchTokenizeEncrypted(bearerToken, true, any())
            } returns response

            "batchDetokenize" -> coEvery {
                mockApi.batchDetokenizeEncrypted(bearerToken, true, any())
            } returns response
        }
    }

    private fun setupEncryptedApiException(exception: Exception) {
        when (operationType) {
            "tokenize" -> coEvery {
                mockApi.tokenizeEncrypted(bearerToken, true, any())
            } throws exception

            "detokenize" -> coEvery {
                mockApi.detokenizeEncrypted(bearerToken, true, any())
            } throws exception

            "batchTokenize" -> coEvery {
                mockApi.batchTokenizeEncrypted(bearerToken, true, any())
            } throws exception

            "batchDetokenize" -> coEvery {
                mockApi.batchDetokenizeEncrypted(bearerToken, true, any())
            } throws exception
        }
    }

    private suspend fun executeOperation(): Response<*> {
        return when (operationType) {
            "tokenize" -> strategy.tokenize(mockApi, accessToken, "test-value")
            "detokenize" -> strategy.detokenize(mockApi, accessToken, "test-token")
            "batchTokenize" -> strategy.batchTokenize(
                mockApi,
                accessToken,
                listOf("value1", "value2")
            )

            "batchDetokenize" -> strategy.batchDetokenize(
                mockApi,
                accessToken,
                listOf("token1", "token2")
            )

            else -> throw IllegalArgumentException("Unknown operation type: $operationType")
        }
    }

    private fun verifyEncryptedApiCalled() {
        when (operationType) {
            "tokenize" -> coVerify(exactly = 1) {
                mockApi.tokenizeEncrypted(bearerToken, true, any())
            }

            "detokenize" -> coVerify(exactly = 1) {
                mockApi.detokenizeEncrypted(bearerToken, true, any())
            }

            "batchTokenize" -> coVerify(exactly = 1) {
                mockApi.batchTokenizeEncrypted(bearerToken, true, any())
            }

            "batchDetokenize" -> coVerify(exactly = 1) {
                mockApi.batchDetokenizeEncrypted(bearerToken, true, any())
            }
        }
    }

    private fun verifyFallbackCalled() {
        when (operationType) {
            "tokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.tokenize(mockApi, accessToken, any())
            }

            "detokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.detokenize(mockApi, accessToken, any())
            }

            "batchTokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.batchTokenize(mockApi, accessToken, any())
            }

            "batchDetokenize" -> coVerify(exactly = 1) {
                mockFallbackStrategy.batchDetokenize(mockApi, accessToken, any())
            }
        }
    }

    private fun verifyNoFallbackCalled() {
        coVerify(exactly = 0) { mockFallbackStrategy.tokenize(any(), any(), any()) }
        coVerify(exactly = 0) { mockFallbackStrategy.detokenize(any(), any(), any()) }
        coVerify(exactly = 0) { mockFallbackStrategy.batchTokenize(any(), any(), any()) }
        coVerify(exactly = 0) { mockFallbackStrategy.batchDetokenize(any(), any(), any()) }
    }
}

// ====================================
// WithEncryptionStrategy DecryptResponse Tests
// ====================================
@RunWith(Parameterized::class)
class WithEncryptionStrategyDecryptResponseTest(
    private val decryptionResult: String,
    private val expectedOutcome: String,
    private val description: String
) {
    private lateinit var mockEncryptionManager: EncryptionManager
    private lateinit var mockLogger: VaultLogger
    private lateinit var strategy: WithEncryptionStrategy
    private val gson = Gson()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("success", "parsed_object", "Successful decryption and parsing"),
                arrayOf("decryption_failure", "null", "Decryption failure"),
                arrayOf("json_parse_failure", "null", "JSON parsing failure")
            )
        }
    }

    @Before
    fun setUp() {
        mockEncryptionManager = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        strategy = WithEncryptionStrategy(mockEncryptionManager, mockLogger, mockk())
    }

    @Test
    fun shouldDecryptResponseCorrectly() {
        // Arrange
        val encryptedResponse = EncryptedResponse("encrypted-payload", "iv")

        when (decryptionResult) {
            "success" -> {
                val decryptedData =
                    gson.toJson(TokenizeResponse("decrypted-token", false, true, "string"))
                every {
                    mockEncryptionManager.decrypt("encrypted-payload", "iv")
                } returns DecryptionSuccess(decryptedData)

                // Act
                val result =
                    strategy.decryptResponse(encryptedResponse, TokenizeResponse::class.java)

                // Assert
                assertEquals(
                    "Should return decrypted TokenizeResponse",
                    "decrypted-token",
                    result?.token
                )
                assertEquals("Should have correct exists value", false, result?.exists)
                assertEquals("Should have correct newlyCreated value", true, result?.newlyCreated)
            }

            "decryption_failure" -> {
                every {
                    mockEncryptionManager.decrypt("encrypted-payload", "iv")
                } returns DecryptionFailure("Decryption failed")

                // Act
                val result =
                    strategy.decryptResponse(encryptedResponse, TokenizeResponse::class.java)

                // Assert
                assertEquals("Should return null on decryption failure", null, result)
            }

            "json_parse_failure" -> {
                val invalidJson = "{ invalid json structure"
                every {
                    mockEncryptionManager.decrypt("encrypted-payload", "iv")
                } returns DecryptionSuccess(invalidJson)

                // Act
                val result =
                    strategy.decryptResponse(encryptedResponse, TokenizeResponse::class.java)

                // Assert
                assertEquals("Should return null on JSON parsing failure", null, result)
            }
        }

        verify(exactly = 1) { mockEncryptionManager.decrypt("encrypted-payload", "iv") }
    }
}

// ====================================
// EncryptionStrategyFactory Tests
// ====================================
@RunWith(Parameterized::class)
class EncryptionStrategyFactoryTest(
    private val encryptionEnabled: Boolean,
    private val expectedStrategyClass: Class<*>,
    private val description: String
) {
    private lateinit var mockEncryptionManager: EncryptionManager
    private lateinit var mockLogger: VaultLogger
    private lateinit var factory: EncryptionStrategyFactory

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should create strategy: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    true,
                    WithEncryptionStrategy::class.java,
                    "WithEncryptionStrategy when enabled"
                ),
                arrayOf(
                    false,
                    NoEncryptionStrategy::class.java,
                    "NoEncryptionStrategy when disabled"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockEncryptionManager = mockk()
        mockLogger = mockk(relaxed = true)
        factory = EncryptionStrategyFactory()
    }

    @Test
    fun shouldCreateCorrectStrategy() {
        // Arrange
        every { mockEncryptionManager.isEnabled() } returns encryptionEnabled

        // Act
        val strategy = factory.createStrategy(mockEncryptionManager, mockLogger)

        // Assert
        assertEquals(
            "Should create correct strategy class",
            expectedStrategyClass,
            strategy::class.java
        )
    }
}