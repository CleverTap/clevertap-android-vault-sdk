package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.model.BatchTokenItemResponse
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeResponse
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeSummary
import com.clevertap.android.zeropii.sdk.model.TokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.TokenizeResponse
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger
import io.mockk.mockk
import okhttp3.ResponseBody
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
    private lateinit var mockLogger: ZeroPiiLogger
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
    private lateinit var mockLogger: ZeroPiiLogger
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
        val response = Response.error<TokenizeResponse>(httpStatus, ResponseBody.create(null, errorBody))

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
    private lateinit var mockLogger: ZeroPiiLogger
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
// Batch Tokenize Response Tests
// ====================================
@RunWith(Parameterized::class)
class ResponseProcessorBatchTokenizeSuccessTest(
    private val apiResults: List<BatchTokenItemResponse>,
    private val expectedTotal: Int,
    private val expectedExisting: Int,
    private val expectedNewly: Int,
    private val description: String
) {
    private lateinit var mockLogger: ZeroPiiLogger
    private lateinit var responseProcessor: ResponseProcessor

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should process batch tokenize: {4}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // All newly created
                arrayOf(
                    listOf(
                        BatchTokenItemResponse("value1", "token1", false, true, "string"),
                        BatchTokenItemResponse("value2", "token2", false, true, "string")
                    ),
                    2, 0, 2,
                    "All newly created"
                ),
                // All existing
                arrayOf(
                    listOf(
                        BatchTokenItemResponse("value1", "token1", true, false, "string"),
                        BatchTokenItemResponse("value2", "token2", true, false, "string")
                    ),
                    2, 2, 0,
                    "All existing"
                ),
                // null data type and empty value/token
                arrayOf(
                    listOf(
                        BatchTokenItemResponse("value3", "", false, true, null),
                        BatchTokenItemResponse("", "token4", false, true, null)
                    ),
                    2, 0, 2,
                    "Null data type and empty value/token"
                ),
                // Mixed flags in API results
                arrayOf(
                    listOf(
                        BatchTokenItemResponse("value1", "token1", true, false, "string"),
                        BatchTokenItemResponse("value2", "token2", false, true, "string"),
                        BatchTokenItemResponse("value3", "token3", true, true, "integer")
                    ),
                    3, 2, 2,
                    "Mixed exists/newlyCreated flags"
                ),
                // Empty results
                arrayOf(
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
        val result = responseProcessor.processBatchTokenizeResponse(response)

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
        assertEquals("Results should match API results", apiResults, successResult.results)
    }
}

@RunWith(Parameterized::class)
class ResponseProcessorBatchTokenizeErrorTest(
    private val httpStatus: Int,
    private val errorBody: String,
    private val description: String
) {
    private lateinit var mockLogger: ZeroPiiLogger
    private lateinit var responseProcessor: ResponseProcessor

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should process batch tokenize error: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(400, "Invalid batch request", "400 with empty cache"),
                arrayOf(500, "Internal server error", "500 server error"),
                arrayOf(413, "Batch too large", "413 Batch size error"),
                arrayOf(429, "Rate limit exceeded", "429 Rate limit")
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
        val response = Response.error<BatchTokenizeResponse>(httpStatus, ResponseBody.create(null, errorBody))

        // Act
        val result = responseProcessor.processBatchTokenizeResponse(response)

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
    private lateinit var mockLogger: ZeroPiiLogger
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

        // Act
        val result = responseProcessor.processBatchTokenizeResponse(response)

        // Assert
        assertTrue("Should return error result", result is BatchTokenizeRepoResult.Error)
        val errorResult = result as BatchTokenizeRepoResult.Error
        assertTrue(
            "Error message should indicate batch tokenization failure",
            errorResult.message.contains("Batch tokenization failed")
        )
    }

    @Test
    fun shouldHandleLargeBatchApiResults() {
        // Arrange
        val apiResults = (1..5001).map { i ->
            BatchTokenItemResponse("value$i", "token$i", i % 2 == 0, i % 2 != 0, "string")
        }
        val batchResponse = BatchTokenizeResponse(
            results = apiResults,
            summary = BatchTokenizeSummary(apiResults.size, 0, 0)
        )
        val response = Response.success(batchResponse)

        // Act
        val result = responseProcessor.processBatchTokenizeResponse(response)

        // Assert
        assertTrue("Should handle large batch results", result is BatchTokenizeRepoResult.Success)
        val successResult = result as BatchTokenizeRepoResult.Success
        assertEquals("Should return all results", 5001, successResult.results.size)
    }
}

// ====================================
// Edge Cases
// ====================================
class ResponseProcessorEdgeCaseTest {
    private lateinit var mockLogger: ZeroPiiLogger
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
        val tokenizeResponse = TokenizeResponse(unicodeToken, false, true, "string")

        // Act
        val tokenizeResult =
            responseProcessor.processTokenizeResponse(Response.success(tokenizeResponse))

        // Assert
        assertTrue(
            "Should handle unicode in tokenize",
            tokenizeResult is TokenizeRepoResult.Success
        )

        val tokenizeSuccess = tokenizeResult as TokenizeRepoResult.Success
        assertEquals("Should preserve unicode token", unicodeToken, tokenizeSuccess.token)
    }
}
