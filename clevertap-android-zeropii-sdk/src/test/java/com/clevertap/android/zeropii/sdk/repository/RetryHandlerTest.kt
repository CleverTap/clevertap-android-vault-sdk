package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.retry.DefaultRetryPolicy
import com.clevertap.android.zeropii.sdk.retry.RetryPolicy
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.Response
import java.io.IOException

// ====================================
// Successful API Call Tests (No Retry Needed)
// ====================================
@RunWith(Parameterized::class)
class RetryHandlerSuccessfulCallTest(
    private val httpStatus: Int,
    private val responseBody: String,
    private val description: String
) {
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockLogger: ZeroPiiLogger
    private lateinit var retryHandler: RetryHandler

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle successful response: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(200, "Success response", "HTTP 200 OK"),
                arrayOf(201, "Created response", "HTTP 201 Created"),
                arrayOf(202, "Accepted response", "HTTP 202 Accepted"),
                arrayOf(204, "", "HTTP 204 No Content")
            )
        }
    }

    @Before
    fun setUp() {
        mockAuthRepository = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        retryHandler = RetryHandler(mockAuthRepository, mockLogger, DefaultRetryPolicy(maxRetries = 2))
    }

    @Test
    fun shouldReturnSuccessfulResponseWithoutRetry() = runTest {
        // Arrange
        val expectedResponse = Response.success(httpStatus, responseBody)
        val apiCall: suspend () -> Response<String> = mockk()
        coEvery { apiCall() } returns expectedResponse

        // Act
        val result = retryHandler.executeWithRetry(apiCall)

        // Assert
        assertEquals("Should return the successful response", expectedResponse, result)
        coVerify(exactly = 1) { apiCall() }
        verify { mockAuthRepository wasNot Called }
    }
}

// ====================================
// 401 Unauthorized Tests (Token Refresh Scenario)
// ====================================
class RetryHandler401Test {
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockLogger: ZeroPiiLogger
    private lateinit var retryHandler: RetryHandler

    @Before
    fun setUp() {
        mockAuthRepository = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        retryHandler = RetryHandler(mockAuthRepository, mockLogger, DefaultRetryPolicy(maxRetries = 2))
    }

    @Test
    fun shouldRefreshTokenAndRetryOn401() = runTest {
        // Arrange
        val unauthorizedResponse = Response.error<String>(401, ResponseBody.create(null,"Unauthorized"))
        val successResponse = Response.success("Success after token refresh")
        val apiCall: suspend () -> Response<String> = mockk()

        coEvery { apiCall() } returnsMany listOf(unauthorizedResponse, successResponse)
        coEvery { mockAuthRepository.refreshAccessToken() } returns "access-token"

        // Act
        val result = retryHandler.executeWithRetry(apiCall)

        // Assert
        assertEquals("Should return success after token refresh", successResponse, result)
        coVerify(exactly = 2) { apiCall() }
        coVerify(exactly = 1) { mockAuthRepository.refreshAccessToken() }
    }

    @Test
    fun shouldReturnErrorAfterSingleTokenRefreshOn401() = runTest {
        // Arrange: both attempts return 401
        val unauthorizedResponse = Response.error<String>(401, ResponseBody.create(null,"Unauthorized"))
        val apiCall: suspend () -> Response<String> = mockk()

        coEvery { apiCall() } returns unauthorizedResponse
        coEvery { mockAuthRepository.refreshAccessToken() } returns "access-token"

        // Act
        val result = retryHandler.executeWithRetry(apiCall)

        // Assert: Only one token refresh per executeWithRetry call
        assertEquals(
            "Should return 401 after single token refresh attempt",
            unauthorizedResponse,
            result
        )
        coVerify(exactly = 2) { apiCall() } // Initial + 1 retry after refresh
        coVerify(exactly = 1) { mockAuthRepository.refreshAccessToken() }
    }

    @Test
    fun shouldHandleTokenRefreshFailure() = runTest {
        // Arrange
        val unauthorizedResponse = Response.error<String>(401, ResponseBody.create(null,"Unauthorized"))
        val apiCall: suspend () -> Response<String> = mockk()

        coEvery { apiCall() } returns unauthorizedResponse
        coEvery { mockAuthRepository.refreshAccessToken() } throws RuntimeException("Token refresh failed")

        // Act & Assert
        try {
            retryHandler.executeWithRetry(apiCall)
            fail("Expected exception was not thrown")
        } catch (e: Exception) {
            assertTrue(
                "Should throw exception on token refresh failure",
                e.message?.contains("Token refresh failed") == true
            )
        }

        coVerify(exactly = 1) { apiCall() }
        coVerify(exactly = 1) { mockAuthRepository.refreshAccessToken() }
    }
}

// ====================================
// Non-Retryable Exception Tests
// ====================================
@RunWith(Parameterized::class)
class RetryHandlerNonRetryableExceptionTest(
    private val exception: Exception,
    private val description: String
) {
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockLogger: ZeroPiiLogger
    private lateinit var retryHandler: RetryHandler

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should not retry on: {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(IllegalArgumentException("Invalid argument"), "IllegalArgumentException"),
                arrayOf(NullPointerException("Null pointer"), "NullPointerException"),
                arrayOf(ClassCastException("Class cast error"), "ClassCastException"),
                arrayOf(RuntimeException("Runtime error"), "RuntimeException"),
                arrayOf(SecurityException("Security violation"), "SecurityException")
            )
        }
    }

    @Before
    fun setUp() {
        mockAuthRepository = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        retryHandler = RetryHandler(mockAuthRepository, mockLogger, DefaultRetryPolicy(maxRetries = 2))
    }

    @Test
    fun shouldNotRetryNonRetryableException() = runTest {
        // Arrange
        val apiCall: suspend () -> Response<String> = mockk()
        coEvery { apiCall() } throws exception

        // Act & Assert
        try {
            retryHandler.executeWithRetry(apiCall)
            fail("Expected exception was not thrown")
        } catch (e: Exception) {
            assertEquals("Should throw the original exception", exception, e)
        }

        coVerify(exactly = 1) { apiCall() } // Should not retry
        verify { mockAuthRepository wasNot Called }
    }
}

// ====================================
// Complex Scenario Tests
// ====================================
class RetryHandlerComplexScenarioTest {
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockLogger: ZeroPiiLogger
    private lateinit var retryHandler: RetryHandler

    @Before
    fun setUp() {
        mockAuthRepository = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        retryHandler =
            RetryHandler(mockAuthRepository, mockLogger, DefaultRetryPolicy(maxRetries = 3))
    }

    @Test
    fun shouldHandle401FollowedByServerError() = runTest {
        // Arrange
        val unauthorizedResponse = Response.error<String>(401, ResponseBody.create(null,"Unauthorized"))
        val serverErrorResponse = Response.error<String>(500, ResponseBody.create(null,"Server Error"))
        val successResponse = Response.success("Final success")
        val apiCall: suspend () -> Response<String> = mockk()

        coEvery { apiCall() } returnsMany listOf(
            unauthorizedResponse,
            serverErrorResponse,
            successResponse
        )
        coEvery { mockAuthRepository.refreshAccessToken() } returns "access-token"

        // Act
        val result = retryHandler.executeWithRetry(apiCall)

        // Assert
        assertEquals("Should succeed after handling both 401 and 500", successResponse, result)
        coVerify(exactly = 3) { apiCall() }
        coVerify(exactly = 1) { mockAuthRepository.refreshAccessToken() }
    }

    @Test
    fun shouldHandleNetworkErrorFollowedBy401() = runTest {
        // Arrange
        val networkException = IOException("Connection failed")
        val unauthorizedResponse = Response.error<String>(401, ResponseBody.create(null,"Unauthorized"))
        val successResponse = Response.success("Final success")
        val apiCall: suspend () -> Response<String> = mockk()

        coEvery { apiCall() } throwsMany listOf(networkException) andThenMany listOf(
            unauthorizedResponse,
            successResponse
        )
        coEvery { mockAuthRepository.refreshAccessToken() } returns "access-token"

        // Act
        val result = retryHandler.executeWithRetry(apiCall)

        // Assert
        assertEquals("Should succeed after network error and 401", successResponse, result)
        coVerify(exactly = 3) { apiCall() }
        coVerify(exactly = 1) { mockAuthRepository.refreshAccessToken() }
    }
}

// ====================================
// Edge Cases and Error Conditions
// ====================================
class RetryHandlerEdgeCaseTest {
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockLogger: ZeroPiiLogger

    @Before
    fun setUp() {
        mockAuthRepository = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
    }

    @Test
    fun shouldHandleZeroMaxRetries() = runTest {
        // Arrange
        val retryHandler = RetryHandler(mockAuthRepository, mockLogger, DefaultRetryPolicy(maxRetries = 0))
        val errorResponse = Response.error<String>(500, ResponseBody.create(null,"Server Error"))
        val apiCall: suspend () -> Response<String> = mockk()

        coEvery { apiCall() } returns errorResponse

        // Act
        val result = retryHandler.executeWithRetry(apiCall)

        // Assert
        assertEquals("Should return error immediately with 0 retries", errorResponse, result)
        coVerify(exactly = 1) { apiCall() } // Only initial call, no retries
    }

}


// ====================================
// Server Error Tests with Mocked Delay
// ====================================
@RunWith(Parameterized::class)
class RetryHandlerServerErrorDelayTest(
    private val errorCode: Int,
    private val initialDelayMs: Long,
    private val maxRetries: Int,
    private val expectedDelays: List<Long>,
    private val description: String
) {
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockLogger: ZeroPiiLogger
    private lateinit var retryHandler: RetryHandler

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should calculate delays correctly: {4}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    500, 1000L, 2,
                    listOf(2000L, 4000L), // 1000 * (1 << 1), 1000 * (1 << 2)
                    "500 error with 1000ms initial delay"
                ),
                arrayOf(
                    502, 500L, 3,
                    listOf(
                        1000L,
                        2000L,
                        4000L
                    ), // 500 * (1 << 1), 500 * (1 << 2), 500 * (1 << 3)
                    "502 error with 500ms initial delay"
                ),
                arrayOf(
                    429, 250L, 2,
                    listOf(500L, 1000L), // 250 * (1 << 1), 250 * (1 << 2)
                    "429 error with 250ms initial delay"
                ),
                arrayOf(
                    503, 100L, 1,
                    listOf(200L), // 100 * (1 << 1)
                    "503 error with single retry"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockAuthRepository = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)

        // Use a custom RetryPolicy that mirrors the parameterized initialDelayMs
        val customPolicy = object : RetryPolicy {
            override fun shouldRetry(attempt: Int, httpStatusCode: Int?): Boolean {
                if (attempt >= maxRetries) return false
                return httpStatusCode == null || httpStatusCode in setOf(500, 502, 503, 504, 429)
            }
            override fun retryDelayMs(attempt: Int): Long {
                return initialDelayMs * (1 shl (attempt + 1))
            }
        }
        retryHandler = RetryHandler(mockAuthRepository, mockLogger, customPolicy)

        // Mock the delay function to avoid actual waiting
        mockkStatic("kotlinx.coroutines.DelayKt")
        coEvery { kotlinx.coroutines.delay(any(Long::class)) } just Runs
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.DelayKt")
    }

    @Test
    fun shouldCalculateExponentialBackoffDelaysCorrectly() = runTest {
        // Arrange
        val errorResponse = Response.error<String>(errorCode, ResponseBody.create(null,"Server Error"))
        val apiCall: suspend () -> Response<String> = mockk()
        coEvery { apiCall() } returns errorResponse

        // Act
        val result = retryHandler.executeWithRetry(apiCall)

        // Assert
        assertEquals("Should return error response after retries", errorResponse, result)
        coVerify(exactly = maxRetries + 1) { apiCall() } // Initial + retries

        // Verify delay was called with correct exponential backoff values
        expectedDelays.forEach { expectedDelay ->
            coVerify { kotlinx.coroutines.delay(expectedDelay) }
        }

        // Verify total number of delay calls
        coVerify(exactly = expectedDelays.size) { kotlinx.coroutines.delay(any(Long::class)) }

        // Verify no token refresh attempted for server errors
        verify { mockAuthRepository wasNot Called }
    }
}


// ====================================
// Network Error Tests with Mocked Delay
// ====================================
class RetryHandlerNetworkErrorDelayTest {
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockLogger: ZeroPiiLogger
    private lateinit var retryHandler: RetryHandler

    @Before
    fun setUp() {
        mockAuthRepository = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        retryHandler =
            RetryHandler(mockAuthRepository, mockLogger, DefaultRetryPolicy(maxRetries = 2))

        mockkStatic("kotlinx.coroutines.DelayKt")
        coEvery { kotlinx.coroutines.delay(any(Long::class)) } just Runs
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.DelayKt")
    }

    @Test
    fun shouldRetryNetworkErrorWithCorrectDelays() = runTest {
        // Arrange
        val networkException = IOException("Network connection failed")
        val successResponse = Response.success("Success after network retry")
        val apiCall: suspend () -> Response<String> = mockk()

        coEvery { apiCall() } throwsMany listOf(networkException) andThen successResponse

        // Act
        val result = retryHandler.executeWithRetry(apiCall)

        // Assert
        assertEquals("Should return success after network retry", successResponse, result)
        coVerify(exactly = 2) { apiCall() }

        // Verify delay called with correct exponential backoff: 1000 * (1 << 1) = 2000ms
        coVerify(exactly = 1) { kotlinx.coroutines.delay(2000L) }
    }

    @Test
    fun shouldCalculateMultipleNetworkErrorDelays() = runTest {
        // Arrange
        val networkException = IOException("Network connection failed")
        val apiCall: suspend () -> Response<String> = mockk()
        coEvery { apiCall() } throws networkException

        // Act & Assert
        try {
            retryHandler.executeWithRetry(apiCall)
            fail("Expected exception was not thrown")
        } catch (e: Exception) {
            // Expected
        }

        coVerify(exactly = 3) { apiCall() } // Initial + 2 retries

        // Verify delays: 2000ms (1000 * 2^1), 4000ms (1000 * 2^2)
        coVerify(exactly = 1) { kotlinx.coroutines.delay(2000L) }
        coVerify(exactly = 1) { kotlinx.coroutines.delay(4000L) }
        coVerify(exactly = 2) { kotlinx.coroutines.delay(any(Long::class)) }
    }
}

// ====================================
// 401 Tests (Should NOT Call Delay)
// ====================================
class RetryHandler401NoDelayTest {
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockLogger: ZeroPiiLogger
    private lateinit var retryHandler: RetryHandler

    @Before
    fun setUp() {
        mockAuthRepository = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        retryHandler = RetryHandler(mockAuthRepository, mockLogger, DefaultRetryPolicy(maxRetries = 2))

        mockkStatic("kotlinx.coroutines.DelayKt")
        coEvery { kotlinx.coroutines.delay(any(Long::class)) } just Runs
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.DelayKt")
    }

    @Test
    fun shouldNotDelayOn401Errors() = runTest {
        // Arrange
        val unauthorizedResponse = Response.error<String>(401, ResponseBody.create(null,"Unauthorized"))
        val successResponse = Response.success("Success after token refresh")
        val apiCall: suspend () -> Response<String> = mockk()

        coEvery { apiCall() } returnsMany listOf(unauthorizedResponse, successResponse)
        coEvery { mockAuthRepository.refreshAccessToken() } returns "access-token"

        // Act
        val result = retryHandler.executeWithRetry(apiCall)

        // Assert
        assertEquals("Should return success after token refresh", successResponse, result)
        coVerify(exactly = 2) { apiCall() }
        coVerify(exactly = 1) { mockAuthRepository.refreshAccessToken() }

        // Critical: Verify delay was NEVER called for 401 errors
        coVerify(exactly = 0) { kotlinx.coroutines.delay(any(Long::class)) }

    }

    @Test
    fun shouldNotDelayOn401EvenWhenBothAttemptsReturn401() = runTest {
        // Arrange
        val unauthorizedResponse = Response.error<String>(401, ResponseBody.create(null,"Unauthorized"))
        val apiCall: suspend () -> Response<String> = mockk()

        coEvery { apiCall() } returns unauthorizedResponse
        coEvery { mockAuthRepository.refreshAccessToken() } returns "access-token"

        // Act
        val result = retryHandler.executeWithRetry(apiCall)

        // Assert: Only one token refresh per executeWithRetry call
        assertEquals("Should return 401 after single token refresh", unauthorizedResponse, result)
        coVerify(exactly = 2) { apiCall() } // Initial + 1 retry after refresh
        coVerify(exactly = 1) { mockAuthRepository.refreshAccessToken() }

        // Still no delay calls for 401s
        coVerify(exactly = 0) { kotlinx.coroutines.delay(any(Long::class)) }
    }
}

// ====================================
// tokenRefreshed Reset Tests (Bug Fix Verification)
// ====================================
class RetryHandlerTokenRefreshResetTest {
    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var mockLogger: ZeroPiiLogger
    private lateinit var retryHandler: RetryHandler

    @Before
    fun setUp() {
        mockAuthRepository = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        retryHandler = RetryHandler(mockAuthRepository, mockLogger, DefaultRetryPolicy(maxRetries = 1))
    }

    @Test
    fun shouldRefreshTokenOnSecondExecuteWithRetryCallAfterFirst401() = runTest {
        // Arrange: First call gets 401 then succeeds
        val unauthorizedResponse = Response.error<String>(401, ResponseBody.create(null, "Unauthorized"))
        val successResponse = Response.success("Success")

        coEvery { mockAuthRepository.refreshAccessToken() } returns "new-token"

        // First executeWithRetry call: 401 → refresh → success
        val apiCall1: suspend () -> Response<String> = mockk()
        coEvery { apiCall1() } returnsMany listOf(unauthorizedResponse, successResponse)

        val result1 = retryHandler.executeWithRetry(apiCall1)
        assertEquals(successResponse, result1)
        coVerify(exactly = 1) { mockAuthRepository.refreshAccessToken() }

        // Second executeWithRetry call: also gets 401 — tokenRefreshed should be reset
        val apiCall2: suspend () -> Response<String> = mockk()
        coEvery { apiCall2() } returnsMany listOf(unauthorizedResponse, successResponse)

        val result2 = retryHandler.executeWithRetry(apiCall2)
        assertEquals(successResponse, result2)

        // Verify token was refreshed AGAIN for the second call
        coVerify(exactly = 2) { mockAuthRepository.refreshAccessToken() }
    }
}
