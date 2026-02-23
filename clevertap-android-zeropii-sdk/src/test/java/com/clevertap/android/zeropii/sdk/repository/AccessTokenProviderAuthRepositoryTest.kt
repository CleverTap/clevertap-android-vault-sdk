package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.auth.AccessTokenCallback
import com.clevertap.android.zeropii.sdk.auth.AccessTokenInfo
import com.clevertap.android.zeropii.sdk.auth.AccessTokenProvider
import com.clevertap.android.zeropii.sdk.util.Clock
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit

// ====================================
// Helper functions
// ====================================
private fun successProvider(token: String, expiresIn: Long) = object : AccessTokenProvider {
    override fun fetchToken(callback: AccessTokenCallback) {
        callback.onSuccess(AccessTokenInfo(token, expiresIn))
    }
}

private fun failureProvider(error: Exception) = object : AccessTokenProvider {
    override fun fetchToken(callback: AccessTokenCallback) {
        callback.onFailure(error)
    }
}

/**
 * Test implementation of Clock that allows controlling time for testing.
 */
private class TestClock(private var currentTimeMillis: Long = 1_000_000L) : Clock {
    override fun currentTimeMillis(): Long = currentTimeMillis

    fun setCurrentTimeMillis(time: Long) {
        currentTimeMillis = time
    }

    fun advanceTimeBy(millis: Long) {
        currentTimeMillis += millis
    }
}

// ====================================
// Constructor and Initialization Tests
// ====================================
class AccessTokenProviderAuthRepositoryConstructorTest {
    private lateinit var mockLogger: ZeroPiiLogger
    private lateinit var authRepository: AccessTokenProviderAuthRepository

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
        authRepository = AccessTokenProviderAuthRepository(
            tokenProvider = successProvider("test-token", 3600),
            logger = mockLogger
        )
    }

    @Test
    fun shouldCreateAuthRepositoryWithValidParameters() {
        assertNotNull("AuthRepository should be created", authRepository)
    }

    @Test
    fun shouldInitializeWithEmptyTokenState() {
        assertFalse("Token should initially be invalid", authRepository.isTokenValid())
    }
}

// ====================================
// Token Validation Tests with Time Control
// ====================================
@RunWith(Parameterized::class)
class AccessTokenProviderTokenValidationTest(
    private val currentTime: Long,
    private val tokenExpiration: Long,
    private val hasToken: Boolean,
    private val expectedValid: Boolean,
    private val description: String
) {
    private lateinit var mockLogger: ZeroPiiLogger

    companion object {
        private const val THIRTY_SECONDS_MS = 30 * 1000L
        private const val ONE_MINUTE_MS = 60 * 1000L
        private const val BASE_TIME = 1_000_000L

        @JvmStatic
        @Parameterized.Parameters(name = "Should validate token: {4}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    BASE_TIME,
                    BASE_TIME + ONE_MINUTE_MS,
                    true,
                    true,
                    "Valid token with 1 minute remaining"
                ),
                arrayOf(
                    BASE_TIME,
                    BASE_TIME + THIRTY_SECONDS_MS + 1000,
                    true,
                    true,
                    "Valid token with 31 seconds remaining"
                ),
                arrayOf(
                    BASE_TIME,
                    BASE_TIME + THIRTY_SECONDS_MS,
                    true,
                    false,
                    "Token exactly at 30 second buffer"
                ),
                arrayOf(
                    BASE_TIME,
                    BASE_TIME + THIRTY_SECONDS_MS - 1000,
                    true,
                    false,
                    "Token within 30 second buffer"
                ),
                arrayOf(BASE_TIME, BASE_TIME - 1000, true, false, "Expired token"),
                arrayOf(BASE_TIME, BASE_TIME + ONE_MINUTE_MS, false, false, "No token available"),
                arrayOf(BASE_TIME, 0L, false, false, "No token with zero expiration"),
                arrayOf(
                    BASE_TIME,
                    BASE_TIME + (24 * 60 * 60 * 1000L),
                    true,
                    true,
                    "Token valid for 24 hours"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
    }

    @Test
    fun shouldValidateTokenCorrectly() = runTest {
        // Arrange
        val expiresInSeconds = ((tokenExpiration - currentTime) / 1000)
        val provider = successProvider("test-token", expiresInSeconds)
        val testClock = TestClock(currentTime)

        val authRepository = AccessTokenProviderAuthRepository(
            tokenProvider = provider,
            logger = mockLogger,
            clock = testClock
        )

        if (hasToken) {
            // Act - Get token to set it up
            authRepository.getAccessToken()
        }

        // Act & Assert
        val isValid = authRepository.isTokenValid()
        assertEquals(description, expectedValid, isValid)
    }
}

// ====================================
// Token Acquisition Tests
// ====================================
class AccessTokenProviderTokenAcquisitionTest {
    private lateinit var mockLogger: ZeroPiiLogger

    private val testAccessToken = "test-access-token-12345"
    private val testExpiresIn = 3600L

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
    }

    @Test
    fun shouldAcquireNewTokenWhenNoneExists() = runTest {
        // Arrange
        val provider = successProvider(testAccessToken, testExpiresIn)
        val authRepository = AccessTokenProviderAuthRepository(provider, mockLogger)

        // Act
        val token = authRepository.getAccessToken()

        // Assert
        assertEquals("Should return the access token", testAccessToken, token)
        assertTrue("Token should be valid after acquisition", authRepository.isTokenValid())
    }

    @Test
    fun shouldReuseValidExistingToken() = runTest {
        // Arrange
        val baseTime = 1_000_000L
        var callCount = 0
        val countingProvider = object : AccessTokenProvider {
            override fun fetchToken(callback: AccessTokenCallback) {
                callCount++
                callback.onSuccess(AccessTokenInfo(testAccessToken, testExpiresIn))
            }
        }
        val testClock = TestClock(baseTime)

        val authRepository = AccessTokenProviderAuthRepository(
            tokenProvider = countingProvider,
            logger = mockLogger,
            clock = testClock
        )

        // First call to set up token
        authRepository.getAccessToken()

        // Act - Second call should reuse token
        val token = authRepository.getAccessToken()

        // Assert
        assertEquals("Should reuse existing token", testAccessToken, token)
        assertEquals("Should only call provider once", 1, callCount)
    }

    @Test
    fun shouldRefreshExpiredToken() = runTest {
        // Arrange
        val baseTime = 1_000_000L
        val oldToken = "old-expired-token"
        val newToken = "new-refreshed-token"

        var callCount = 0
        val provider = object : AccessTokenProvider {
            override fun fetchToken(callback: AccessTokenCallback) {
                callCount++
                if (callCount == 1) {
                    callback.onSuccess(AccessTokenInfo(oldToken, 60))
                } else {
                    callback.onSuccess(AccessTokenInfo(newToken, testExpiresIn))
                }
            }
        }
        val testClock = TestClock(baseTime)

        val authRepository = AccessTokenProviderAuthRepository(
            tokenProvider = provider,
            logger = mockLogger,
            clock = testClock
        )

        // Get initial token
        val firstToken = authRepository.getAccessToken()
        assertEquals("Should get first token", oldToken, firstToken)

        // Move time forward to expire token (past buffer zone)
        testClock.setCurrentTimeMillis(baseTime + TimeUnit.MINUTES.toMillis(2))

        // Act - Should refresh expired token
        val refreshedToken = authRepository.getAccessToken()

        // Assert
        assertEquals("Should return new refreshed token", newToken, refreshedToken)
        assertTrue("New token should be valid", authRepository.isTokenValid())
    }

    @Test
    fun shouldRefreshTokenExplicitly() = runTest {
        // Arrange
        val newToken = "new-refreshed-token"
        val provider = successProvider(newToken, testExpiresIn)
        val authRepository = AccessTokenProviderAuthRepository(provider, mockLogger)

        // Act
        val token = authRepository.refreshAccessToken()

        // Assert
        assertEquals("Should return new refreshed token", newToken, token)
        assertTrue("New token should be valid", authRepository.isTokenValid())
    }
}

// ====================================
// Precise Time-Based Token Logic Tests
// ====================================
class AccessTokenProviderPreciseTimeTest {
    private lateinit var mockLogger: ZeroPiiLogger

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
    }

    @Test
    fun shouldValidateTokenWithPreciseTimeControl() = runTest {
        // Arrange
        val baseTime = 1_000_000L
        val provider = successProvider("time-test-token", 3600)
        val testClock = TestClock(baseTime)

        val authRepository = AccessTokenProviderAuthRepository(
            tokenProvider = provider,
            logger = mockLogger,
            clock = testClock
        )

        // Act - Get token at baseTime
        authRepository.getAccessToken()

        // Assert - Token should be valid immediately
        assertTrue("Token should be valid at acquisition time", authRepository.isTokenValid())

        // Move time forward to just before buffer zone (31 seconds before expiration)
        testClock.setCurrentTimeMillis(baseTime + TimeUnit.HOURS.toMillis(1) - TimeUnit.SECONDS.toMillis(31))
        assertTrue("Token should be valid 31 seconds before expiration", authRepository.isTokenValid())

        // Move time to exactly at buffer zone (30 seconds before expiration)
        testClock.setCurrentTimeMillis(baseTime + TimeUnit.HOURS.toMillis(1) - TimeUnit.SECONDS.toMillis(30))
        assertFalse("Token should be invalid at 30 second buffer", authRepository.isTokenValid())

        // Move time to within buffer zone (15 seconds before expiration)
        testClock.setCurrentTimeMillis(baseTime + TimeUnit.HOURS.toMillis(1) - TimeUnit.SECONDS.toMillis(15))
        assertFalse("Token should be invalid within buffer zone", authRepository.isTokenValid())

        // Move time past expiration
        testClock.setCurrentTimeMillis(baseTime + TimeUnit.HOURS.toMillis(1) + TimeUnit.SECONDS.toMillis(1))
        assertFalse("Token should be invalid after expiration", authRepository.isTokenValid())
    }

    @Test
    fun shouldHandleBufferZoneBoundaryConditions() = runTest {
        val testCases = listOf(
            Triple(31L, true, "31 seconds before expiration - should be valid"),
            Triple(30L, false, "30 seconds before expiration - should be invalid (exact buffer)"),
            Triple(29L, false, "29 seconds before expiration - should be invalid (within buffer)"),
            Triple(1L, false, "1 second before expiration - should be invalid"),
            Triple(0L, false, "At expiration time - should be invalid"),
            Triple(-1L, false, "1 second after expiration - should be invalid")
        )

        testCases.forEach { (secondsBeforeExpiration, expectedValid, description) ->
            // Arrange
            val baseTime = 2_000_000L
            val expirationTime = baseTime + TimeUnit.HOURS.toMillis(1)
            val testTime = expirationTime - TimeUnit.SECONDS.toMillis(secondsBeforeExpiration)

            val provider = successProvider("boundary-test-token", 3600)
            val testClock = TestClock(baseTime)

            val repository = AccessTokenProviderAuthRepository(
                tokenProvider = provider,
                logger = mockLogger,
                clock = testClock
            )

            // Set up token at base time
            repository.getAccessToken()

            // Move to test time
            testClock.setCurrentTimeMillis(testTime)

            // Act & Assert
            val isValid = repository.isTokenValid()
            assertEquals(description, expectedValid, isValid)
        }
    }
}

// ====================================
// Error Handling Tests
// ====================================
class AccessTokenProviderErrorHandlingTest {
    private lateinit var mockLogger: ZeroPiiLogger

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
    }

    @Test
    fun shouldThrowWhenProviderCallsOnFailure() = runTest {
        // Arrange
        val expectedError = Exception("Token provider failed")
        val provider = failureProvider(expectedError)
        val authRepository = AccessTokenProviderAuthRepository(provider, mockLogger)

        // Act & Assert
        try {
            authRepository.refreshAccessToken()
            fail("Expected exception but none was thrown")
        } catch (e: Exception) {
            assertEquals("Token provider failed", e.message)
        }
    }

    @Test
    fun shouldThrowWhenProviderThrowsSynchronously() = runTest {
        // Arrange
        val provider = object : AccessTokenProvider {
            override fun fetchToken(callback: AccessTokenCallback) {
                throw RuntimeException("Synchronous failure")
            }
        }
        val authRepository = AccessTokenProviderAuthRepository(provider, mockLogger)

        // Act & Assert
        try {
            authRepository.refreshAccessToken()
            fail("Expected exception but none was thrown")
        } catch (e: RuntimeException) {
            assertEquals("Synchronous failure", e.message)
        }
    }

    @Test
    fun shouldHandleEmptyToken() = runTest {
        // Arrange
        val provider = successProvider("", 3600)
        val authRepository = AccessTokenProviderAuthRepository(provider, mockLogger)

        // Act
        val token = authRepository.getAccessToken()

        // Assert - empty string is accepted (server will reject if invalid)
        assertEquals("Should return empty token", "", token)
    }
}

// ====================================
// Callback Bridging Tests
// ====================================
class AccessTokenProviderCallbackBridgingTest {
    private lateinit var mockLogger: ZeroPiiLogger

    @Before
    fun setUp() {
        mockLogger = mockk(relaxed = true)
    }

    @Test
    fun shouldHandleSynchronousCallback() = runTest {
        // Arrange - provider calls back synchronously (common pattern)
        val provider = successProvider("sync-token", 3600)
        val authRepository = AccessTokenProviderAuthRepository(provider, mockLogger)

        // Act
        val token = authRepository.getAccessToken()

        // Assert
        assertEquals("Should handle synchronous callback", "sync-token", token)
    }

    @Test
    fun shouldTokenRemainInvalidAfterFailure() = runTest {
        // Arrange
        val provider = failureProvider(Exception("auth error"))
        val authRepository = AccessTokenProviderAuthRepository(provider, mockLogger)

        // Act
        try {
            authRepository.getAccessToken()
        } catch (_: Exception) {
            // expected
        }

        // Assert
        assertFalse("Token should remain invalid after failure", authRepository.isTokenValid())
    }
}
