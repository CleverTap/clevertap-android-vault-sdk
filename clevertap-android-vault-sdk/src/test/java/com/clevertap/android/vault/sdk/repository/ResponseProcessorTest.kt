package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.model.BatchDetokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeSummary
import com.clevertap.android.vault.sdk.model.BatchTokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeSummary
import com.clevertap.android.vault.sdk.model.DetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.DetokenizeResponse
import com.clevertap.android.vault.sdk.model.TokenizeRepoResult
import com.clevertap.android.vault.sdk.model.TokenizeResponse
import com.clevertap.android.vault.sdk.util.VaultLogger
import io.mockk.mockk
import io.mockk.verify
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.Response

// ====================================
// Single Tokenize Response Tests
// ====================================
@RunWith(Parameterized::class)
class ResponseProcessorTokenizeSuccessTest(
    private val token: String,
    private val exists: Boolean,
    private val newlyCreated: Boolean,
    private val dataType: String?,
    private val description: String
) {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should process tokenize success: {4}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf("token-123", false, true, "string", "Newly created string token"),
                arrayOf("token-456", true, false, "string", "Existing string token"),
                arrayOf("987654", false, true, "integer", "Newly created integer token"),
                arrayOf("false", true, false, "boolean", "Existing boolean token"),
                arrayOf("🚀", false, true, "string", "Unicode token"),
                arrayOf("", true, false, "string", "Empty token"),
                arrayOf("token-null", false, true, null, "Token with null dataType"),
                arrayOf(
                    "token-both",
                    true,
                    true,
                    "string",
                    "Token with both exists and newlyCreated true"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldProcessSuccessfulTokenizeResponse() {
        // Arrange
        val tokenizeResponse = TokenizeResponse(token, exists, newlyCreated, dataType)
        val response = Response.success(tokenizeResponse)

        // Act
        val result = responseProcessor.processTokenizeResponse(response)

        // Assert
        assertTrue("Should return success result", result is TokenizeRepoResult.Success)
        val successResult = result as TokenizeRepoResult.Success
        assertEquals("Token should match", token, successResult.token)
        assertEquals("Exists should match", exists, successResult.exists)
        assertEquals("NewlyCreated should match", newlyCreated, successResult.newlyCreated)
        assertEquals("DataType should match", dataType, successResult.dataType)
    }
}

@RunWith(Parameterized::class)
class ResponseProcessorTokenizeErrorTest(
    private val httpStatus: Int,
    private val errorBody: String,
    private val description: String
) {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should process tokenize error: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(400, "Bad Request", "400 Bad Request"),
                arrayOf(401, "Unauthorized", "401 Unauthorized"),
                arrayOf(403, "Forbidden", "403 Forbidden"),
                arrayOf(404, "Not Found", "404 Not Found"),
                arrayOf(500, "Internal Server Error", "500 Internal Server Error"),
                arrayOf(502, "Bad Gateway", "502 Bad Gateway"),
                arrayOf(503, "Service Unavailable", "503 Service Unavailable"),
                arrayOf(422, "", "422 with null error body")
            )
        }
    }

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldProcessErrorTokenizeResponse() {
        // Arrange
        val response = Response.error<TokenizeResponse>(httpStatus, errorBody.toResponseBody())

        // Act
        val result = responseProcessor.processTokenizeResponse(response)

        // Assert
        assertTrue("Should return error result", result is TokenizeRepoResult.Error)
        val errorResult = result as TokenizeRepoResult.Error

        val expectedMessage = "Tokenization failed: $httpStatus - $errorBody"
        assertEquals("Error message should match", expectedMessage, errorResult.message)
    }
}

class ResponseProcessorTokenizeEdgeCaseTest {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldHandleSuccessfulResponseWithNullBody() {
        // Arrange - Successful response but null body
        val response = Response.success<TokenizeResponse>(null)

        // Act
        val result = responseProcessor.processTokenizeResponse(response)

        // Assert
        assertTrue("Should return error result", result is TokenizeRepoResult.Error)
        val errorResult = result as TokenizeRepoResult.Error
        assertTrue(
            "Error message should indicate tokenization failure",
            errorResult.message.contains("Tokenization failed")
        )
    }
}

// ====================================
// Single Detokenize Response Tests
// ====================================
@RunWith(Parameterized::class)
class ResponseProcessorDetokenizeSuccessTest(
    private val value: String?,
    private val exists: Boolean,
    private val dataType: String?,
    private val description: String
) {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should process detokenize success: {3}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf("555-12-3456", true, "string", "Existing SSN value"),
                arrayOf("john@example.com", true, "string", "Existing email value"),
                arrayOf("123456", true, "integer", "Existing integer value"),
                arrayOf("true", true, "boolean", "Existing boolean value"),
                arrayOf(null, false, null, "Token not found"),
                arrayOf("", true, "string", "Empty value"),
                arrayOf("🚀", true, "string", "Unicode value"),
                arrayOf("value", true, null, "Value with null dataType")
            )
        }
    }

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldProcessSuccessfulDetokenizeResponse() {
        // Arrange
        val detokenizeResponse = DetokenizeResponse(value, exists, dataType)
        val response = Response.success(detokenizeResponse)

        // Act
        val result = responseProcessor.processDetokenizeResponse(response)

        // Assert
        assertTrue("Should return success result", result is DetokenizeRepoResult.Success)
        val successResult = result as DetokenizeRepoResult.Success
        assertEquals("Value should match", value, successResult.value)
        assertEquals("Exists should match", exists, successResult.exists)
        assertEquals("DataType should match", dataType, successResult.dataType)
    }
}

@RunWith(Parameterized::class)
class ResponseProcessorDetokenizeErrorTest(
    private val httpStatus: Int,
    private val errorBody: String,
    private val description: String
) {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should process detokenize error: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(400, "Invalid token format", "400 Invalid token"),
                arrayOf(401, "Unauthorized access", "401 Unauthorized"),
                arrayOf(403, "Token access denied", "403 Forbidden"),
                arrayOf(404, "Token not found", "404 Token not found"),
                arrayOf(500, "Server error", "500 Server error"),
                arrayOf(419, "Backend decryption failed", "419 Decryption failed")
            )
        }
    }

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldProcessErrorDetokenizeResponse() {
        // Arrange
        val response = Response.error<DetokenizeResponse>(httpStatus, errorBody.toResponseBody())

        // Act
        val result = responseProcessor.processDetokenizeResponse(response)

        // Assert
        assertTrue("Should return error result", result is DetokenizeRepoResult.Error)
        val errorResult = result as DetokenizeRepoResult.Error
        val expectedMessage = "Detokenization failed: $httpStatus - $errorBody"
        assertEquals("Error message should match", expectedMessage, errorResult.message)
    }
}

class ResponseProcessorDetokenizeEdgeCaseTest {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldHandleSuccessfulResponseWithNullBody() {
        // Arrange - Successful response but null body
        val response = Response.success<DetokenizeResponse>(null)

        // Act
        val result = responseProcessor.processDetokenizeResponse(response)

        // Assert
        assertTrue("Should return error result", result is DetokenizeRepoResult.Error)
        val errorResult = result as DetokenizeRepoResult.Error
        assertTrue(
            "Error message should indicate tokenization failure",
            errorResult.message.contains("Detokenization failed")
        )
    }
}

// ====================================
// Batch Tokenize Response Tests
// ====================================
@RunWith(Parameterized::class)
class ResponseProcessorBatchTokenizeSuccessTest(
    private val apiResults: List<BatchTokenItemResponse>,
    private val cachedResults: List<BatchTokenItemResponse>,
    private val expectedTotal: Int,
    private val expectedExisting: Int,
    private val expectedNewly: Int,
    private val description: String
) {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should process batch tokenize: {5}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // API only results
                arrayOf(
                    listOf(
                        BatchTokenItemResponse("value1", "token1", false, true, "string"),
                        BatchTokenItemResponse("value2", "token2", false, true, "string")
                    ),
                    emptyList<BatchTokenItemResponse>(),
                    2, 0, 2,
                    "API only - all newly created"
                ),
                // Cache only results
                arrayOf(
                    emptyList<BatchTokenItemResponse>(),
                    listOf(
                        BatchTokenItemResponse("value1", "token1", true, false, "string"),
                        BatchTokenItemResponse("value2", "token2", true, false, "string")
                    ),
                    2, 2, 0,
                    "Cache only - all existing"
                ),
                // Mixed API and cache results
                arrayOf(
                    listOf(
                        BatchTokenItemResponse("value3", "token3", false, true, "string"),
                        BatchTokenItemResponse("value4", "token4", false, true, "integer")
                    ),
                    listOf(
                        BatchTokenItemResponse("value1", "token1", true, false, "string"),
                        BatchTokenItemResponse("value2", "token2", true, false, "string")
                    ),
                    4, 2, 2,
                    "Mixed - cached existing and API newly created"
                ),
                // null data type and empty value/token
                arrayOf(
                    listOf(
                        BatchTokenItemResponse("value3", "", false, true, null),
                        BatchTokenItemResponse("", "token4", false, true, null)
                    ),
                    listOf(
                        BatchTokenItemResponse("", "token1", true, false, null),
                        BatchTokenItemResponse("value2", "", true, false, null)
                    ),
                    4, 2, 2,
                    "Mixed - null data type and empty value/token"
                ),
                // Mixed flags in API results
                arrayOf(
                    listOf(
                        BatchTokenItemResponse(
                            "value1",
                            "token1",
                            true,
                            false,
                            "string"
                        ),   // existing
                        BatchTokenItemResponse(
                            "value2",
                            "token2",
                            false,
                            true,
                            "string"
                        ),  // newly created
                        BatchTokenItemResponse(
                            "value3",
                            "token3",
                            true,
                            true,
                            "integer"
                        )   // both flags true
                    ),
                    emptyList<BatchTokenItemResponse>(),
                    3, 2, 2,
                    "API with mixed exists/newlyCreated flags"
                ),
                // Empty results
                arrayOf(
                    emptyList<BatchTokenItemResponse>(),
                    emptyList<BatchTokenItemResponse>(),
                    0, 0, 0,
                    "Empty results"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldProcessSuccessfulBatchTokenizeResponse() {
        // Arrange
        val batchResponse = BatchTokenizeResponse(
            results = apiResults,
            summary = BatchTokenizeSummary(apiResults.size, 0, 0) // Original summary (ignored)
        )
        val response = Response.success(batchResponse)

        // Act
        val result = responseProcessor.processBatchTokenizeResponse(response, cachedResults)

        // Assert
        assertTrue("Should return success result", result is BatchTokenizeRepoResult.Success)
        val successResult = result as BatchTokenizeRepoResult.Success

        assertEquals("Total results should match", expectedTotal, successResult.results.size)
        assertEquals(
            "Summary total should match",
            expectedTotal,
            successResult.summary.processedCount
        )
        assertEquals(
            "Summary existing should match",
            expectedExisting,
            successResult.summary.existingCount
        )
        assertEquals(
            "Summary newly created should match",
            expectedNewly,
            successResult.summary.newlyCreatedCount
        )

        // Verify merged results order: cachedResults + apiResults
        val expectedResults = cachedResults + apiResults
        assertEquals("Results should be merged correctly", expectedResults, successResult.results)
    }
}

@RunWith(Parameterized::class)
class ResponseProcessorBatchTokenizeErrorTest(
    private val httpStatus: Int,
    private val errorBody: String,
    private val cachedResults: List<BatchTokenItemResponse>,
    private val description: String
) {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should process batch tokenize error: {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    400, "Invalid batch request",
                    emptyList<BatchTokenItemResponse>(),
                    "400 with empty cache"
                ),
                arrayOf(
                    500, "Internal server error",
                    listOf(BatchTokenItemResponse("cached", "token", true, false, "string")),
                    "500 with cached items"
                ),
                arrayOf(
                    413, "Batch too large",
                    emptyList<BatchTokenItemResponse>(),
                    "413 Batch size error"
                ),
                arrayOf(
                    429, "Rate limit exceeded",
                    emptyList<BatchTokenItemResponse>(),
                    "429 Rate limit"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldProcessErrorBatchTokenizeResponse() {
        // Arrange
        val response = Response.error<BatchTokenizeResponse>(httpStatus, errorBody.toResponseBody())

        // Act
        val result = responseProcessor.processBatchTokenizeResponse(response, cachedResults)

        // Assert
        assertTrue("Should return error result", result is BatchTokenizeRepoResult.Error)
        val errorResult = result as BatchTokenizeRepoResult.Error
        val expectedMessage = "Batch tokenization failed: $httpStatus - $errorBody"
        assertEquals("Error message should match", expectedMessage, errorResult.message)
    }
}

// ====================================
// Batch Tokenize Edge Case Tests
// ====================================
class ResponseProcessorBatchTokenizeEdgeCaseTest {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldHandleSuccessfulResponseWithNullBody() {
        // Arrange - Successful response but null body
        val response = Response.success<BatchTokenizeResponse>(null)
        val cachedResults = listOf(
            BatchTokenItemResponse("cached", "token", true, false, "string")
        )

        // Act
        val result = responseProcessor.processBatchTokenizeResponse(response, cachedResults)

        // Assert
        assertTrue("Should return error result", result is BatchTokenizeRepoResult.Error)
        val errorResult = result as BatchTokenizeRepoResult.Error
        assertTrue(
            "Error message should indicate batch tokenization failure",
            errorResult.message.contains("Batch tokenization failed")
        )
    }

    @Test
    fun shouldHandleExtremelyLargeCachedResults() {
        // Arrange - Large cached results with small API response
        val cachedResults = (1..5000).map { i ->
            BatchTokenItemResponse("cached$i", "token$i", true, false, "string")
        }
        val apiResults = listOf(
            BatchTokenItemResponse("api1", "apitoken1", false, true, "string")
        )
        val batchResponse =
            BatchTokenizeResponse(results = apiResults, summary = BatchTokenizeSummary(1, 0, 1))
        val response = Response.success(batchResponse)

        // Act
        val result = responseProcessor.processBatchTokenizeResponse(response, cachedResults)

        // Assert
        assertTrue("Should handle large cached results", result is BatchTokenizeRepoResult.Success)
        val successResult = result as BatchTokenizeRepoResult.Success
        assertEquals("Should merge all results", 5001, successResult.results.size)
        assertEquals(
            "Should have correct existing count",
            5000,
            successResult.summary.existingCount
        )
        assertEquals(
            "Should have correct newly created count",
            1,
            successResult.summary.newlyCreatedCount
        )

        // Verify order: cached first, then API
        assertEquals(
            "First result should be cached",
            "cached1",
            successResult.results.first().originalValue
        )
        assertEquals(
            "Last result should be API",
            "api1",
            successResult.results.last().originalValue
        )
    }

}

// ====================================
// Batch Detokenize Response Tests
// ====================================
@RunWith(Parameterized::class)
class ResponseProcessorBatchDetokenizeSuccessTest(
    private val apiResults: List<BatchDetokenItemResponse>,
    private val cachedResults: List<BatchDetokenItemResponse>,
    private val expectedTotal: Int,
    private val expectedFound: Int,
    private val expectedNotFound: Int,
    private val description: String
) {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should process batch detokenize: {5}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // API only results - all found
                arrayOf(
                    listOf(
                        BatchDetokenItemResponse("token1", "value1", true, "string"),
                        BatchDetokenItemResponse("token2", "value2", true, "string")
                    ),
                    emptyList<BatchDetokenItemResponse>(),
                    2, 2, 0,
                    "API only - all found"
                ),
                // API only results - some not found
                arrayOf(
                    listOf(
                        BatchDetokenItemResponse("token1", "value1", true, "string"),
                        BatchDetokenItemResponse("token2", null, false, null),
                        BatchDetokenItemResponse("token3", "value3", true, "integer")
                    ),
                    emptyList<BatchDetokenItemResponse>(),
                    3, 2, 1,
                    "API only - mixed found/not found"
                ),
                // Cache only results
                arrayOf(
                    emptyList<BatchDetokenItemResponse>(),
                    listOf(
                        BatchDetokenItemResponse("token1", "value1", true, "string"),
                        BatchDetokenItemResponse("token2", "value2", true, "string")
                    ),
                    2, 2, 0,
                    "Cache only - all found"
                ),
                // Mixed cache and API results
                arrayOf(
                    listOf(
                        BatchDetokenItemResponse("token3", "value3", true, "string"),
                        BatchDetokenItemResponse("token4", null, false, null)
                    ),
                    listOf(
                        BatchDetokenItemResponse("token1", "value1", true, "string"),
                        BatchDetokenItemResponse("token2", "value2", true, "integer")
                    ),
                    4, 3, 1,
                    "Mixed - cached found and API mixed"
                ),
                // All not found
                arrayOf(
                    listOf(
                        BatchDetokenItemResponse("invalid1", null, false, null),
                        BatchDetokenItemResponse("invalid2", null, false, null)
                    ),
                    emptyList<BatchDetokenItemResponse>(),
                    2, 0, 2,
                    "API only - all not found"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldProcessSuccessfulBatchDetokenizeResponse() {
        // Arrange
        val batchResponse = BatchDetokenizeResponse(
            results = apiResults,
            summary = BatchDetokenizeSummary(apiResults.size, 0, 0) // Original summary (ignored)
        )
        val response = Response.success(batchResponse)

        // Act
        val result = responseProcessor.processBatchDetokenizeResponse(response, cachedResults)

        // Assert
        assertTrue("Should return success result", result is BatchDetokenizeRepoResult.Success)
        val successResult = result as BatchDetokenizeRepoResult.Success

        assertEquals("Total results should match", expectedTotal, successResult.results.size)
        assertEquals(
            "Summary total should match",
            expectedTotal,
            successResult.summary.processedCount
        )
        assertEquals("Summary found should match", expectedFound, successResult.summary.foundCount)
        assertEquals(
            "Summary not found should match",
            expectedNotFound,
            successResult.summary.notFoundCount
        )

        // Verify merged results order: cachedResults + apiResults
        val expectedResults = cachedResults + apiResults
        assertEquals("Results should be merged correctly", expectedResults, successResult.results)
    }
}

@RunWith(Parameterized::class)
class ResponseProcessorBatchDetokenizeErrorTest(
    private val httpStatus: Int,
    private val errorBody: String,
    private val cachedResults: List<BatchDetokenItemResponse>,
    private val description: String
) {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should process batch detokenize error: {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    400, "Invalid tokens in batch",
                    emptyList<BatchDetokenItemResponse>(),
                    "400 with empty cache"
                ),
                arrayOf(
                    403, "Access denied to tokens",
                    listOf(BatchDetokenItemResponse("cached", "value", true, "string")),
                    "403 with cached items"
                ),
                arrayOf(
                    500, "Internal server error",
                    emptyList<BatchDetokenItemResponse>(),
                    "500 server error"
                ),
                arrayOf(
                    413, "Too many tokens",
                    emptyList<BatchDetokenItemResponse>(),
                    "413 Batch size error"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldProcessErrorBatchDetokenizeResponse() {
        // Arrange
        val response =
            Response.error<BatchDetokenizeResponse>(httpStatus, errorBody.toResponseBody())

        // Act
        val result = responseProcessor.processBatchDetokenizeResponse(response, cachedResults)

        // Assert
        assertTrue("Should return error result", result is BatchDetokenizeRepoResult.Error)
        val errorResult = result as BatchDetokenizeRepoResult.Error
        val expectedMessage = "Batch detokenization failed: $httpStatus - $errorBody"
        assertEquals("Error message should match", expectedMessage, errorResult.message)
    }
}

// ====================================
// Batch Detokenize Edge Case Tests
// ====================================
class ResponseProcessorBatchDetokenizeEdgeCaseTest {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldHandleSuccessfulResponseWithNullBody() {
        // Arrange - Successful response but null body
        val response = Response.success<BatchDetokenizeResponse>(null)
        val cachedResults = listOf(
            BatchDetokenItemResponse("cached", "value", true, "string")
        )

        // Act
        val result = responseProcessor.processBatchDetokenizeResponse(response, cachedResults)

        // Assert
        assertTrue("Should return error result", result is BatchDetokenizeRepoResult.Error)
        val errorResult = result as BatchDetokenizeRepoResult.Error
        assertTrue("Error message should indicate batch detokenization failure",
            errorResult.message.contains("Batch detokenization failed"))
    }

    @Test
    fun shouldHandleEmptyResultsWithLargeCachedResults() {
        // Arrange - Large cached results with empty API response
        val cachedResults = (1..3000).map { i ->
            BatchDetokenItemResponse("token$i", "value$i", true, "string")
        }
        val batchResponse = BatchDetokenizeResponse(
            results = emptyList(),
            summary = BatchDetokenizeSummary(0, 0, 0)
        )
        val response = Response.success(batchResponse)

        // Act
        val result = responseProcessor.processBatchDetokenizeResponse(response, cachedResults)

        // Assert
        assertTrue("Should handle large cached results", result is BatchDetokenizeRepoResult.Success)
        val successResult = result as BatchDetokenizeRepoResult.Success
        assertEquals("Should contain only cached results", 3000, successResult.results.size)
        assertEquals("Should have all found from cache", 3000, successResult.summary.foundCount)
        assertEquals("Should have none not found", 0, successResult.summary.notFoundCount)
        assertEquals("Results should match cached", cachedResults, successResult.results)
    }
}

// ====================================
// Edge Cases
// ====================================
class ResponseProcessorEdgeCaseTest {
    private lateinit var mockLogger: VaultLogger
    private lateinit var responseProcessor: ResponseProcessor

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        responseProcessor = ResponseProcessor(mockLogger)
    }

    @Test
    fun shouldHandleUnicodeInResponseData() {
        // Arrange
        val unicodeToken = "🚀🌟🎯"
        val unicodeValue = "💯🔥⚡"
        val tokenizeResponse = TokenizeResponse(unicodeToken, false, true, "string")
        val detokenizeResponse = DetokenizeResponse(unicodeValue, true, "string")

        // Act
        val tokenizeResult =
            responseProcessor.processTokenizeResponse(Response.success(tokenizeResponse))
        val detokenizeResult =
            responseProcessor.processDetokenizeResponse(Response.success(detokenizeResponse))

        // Assert
        assertTrue(
            "Should handle unicode in tokenize",
            tokenizeResult is TokenizeRepoResult.Success
        )
        assertTrue(
            "Should handle unicode in detokenize",
            detokenizeResult is DetokenizeRepoResult.Success
        )

        val tokenizeSuccess = tokenizeResult as TokenizeRepoResult.Success
        val detokenizeSuccess = detokenizeResult as DetokenizeRepoResult.Success

        assertEquals("Should preserve unicode token", unicodeToken, tokenizeSuccess.token)
        assertEquals("Should preserve unicode value", unicodeValue, detokenizeSuccess.value)
    }
}