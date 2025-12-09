package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.api.AuthApi
import com.clevertap.android.vault.sdk.model.AuthTokenResponse
import com.clevertap.android.vault.sdk.network.NetworkProvider
import com.clevertap.android.vault.sdk.util.VaultLogger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

// ====================================
// Constructor and Initialization Tests
// ====================================
class AuthRepositoryImplConstructorTest {
    private lateinit var mockNetworkProvider: NetworkProvider
    private lateinit var mockLogger: VaultLogger
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setUp() {
        mockNetworkProvider = mockk()
        mockLogger = mockk(relaxed = true)

        authRepository = AuthRepositoryImpl(
            networkProvider = mockNetworkProvider,
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            logger = mockLogger
        )
    }

    @Test
    fun shouldCreateAuthRepositoryWithValidParameters() {
        // Assert
        assertNotNull("AuthRepository should be created", authRepository)
    }

    @Test
    fun shouldInitializeWithEmptyTokenState() {
        // Assert
        assertFalse("Token should initially be invalid", authRepository.isTokenValid())
    }
}

// ====================================
// Token Validation Tests with Time Control
// ====================================
@RunWith(Parameterized::class)
class AuthRepositoryTokenValidationTest(
    private val currentTime: Long,
    private val tokenExpiration: Long,
    private val hasToken: Boolean,
    private val expectedValid: Boolean,
    private val description: String
) {
    private lateinit var mockNetworkProvider: NetworkProvider
    private lateinit var mockAuthApi: AuthApi
    private lateinit var mockLogger: VaultLogger
    private lateinit var authRepository: AuthRepositoryImpl

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
        mockNetworkProvider = mockk()
        mockAuthApi = mockk()
        mockLogger = mockk(relaxed = true)

        every { mockNetworkProvider.authApi } returns mockAuthApi

        // Create spy to mock internal getNowInMillis function
        val realRepository = AuthRepositoryImpl(
            networkProvider = mockNetworkProvider,
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            logger = mockLogger
        )
        authRepository = spyk(realRepository)

        // Mock time function
        every { authRepository.getNowInMillis() } returns currentTime
    }

    @Test
    fun shouldValidateTokenCorrectly() = runTest {
        if (hasToken) {
            // Arrange - Set up a token with specific expiration
            val tokenResponse = AuthTokenResponse(
                accessToken = "test-token",
                expiresIn = ((tokenExpiration - currentTime) / 1000).toInt(),
                refreshExpiresIn = 1800,
                tokenType = "bearer",
                scope = "vault.access"
            )

            coEvery { mockAuthApi.getToken(any()) } returns Response.success(tokenResponse)

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
class AuthRepositoryTokenAcquisitionTest {
    private lateinit var mockNetworkProvider: NetworkProvider
    private lateinit var mockAuthApi: AuthApi
    private lateinit var mockLogger: VaultLogger
    private lateinit var authRepository: AuthRepositoryImpl

    private val testClientId = "test-client-id"
    private val testClientSecret = "test-client-secret"
    private val testAccessToken = "test-access-token-12345"
    private val testExpiresIn = 3600 // 1 hour

    @Before
    fun setUp() {
        mockNetworkProvider = mockk()
        mockAuthApi = mockk()
        mockLogger = mockk(relaxed = true)

        every { mockNetworkProvider.authApi } returns mockAuthApi

        authRepository = AuthRepositoryImpl(
            networkProvider = mockNetworkProvider,
            clientId = testClientId,
            clientSecret = testClientSecret,
            logger = mockLogger
        )
    }

    @Test
    fun shouldAcquireNewTokenWhenNoneExists() = runTest {
        // Arrange
        val expectedResponse = AuthTokenResponse(
            accessToken = testAccessToken,
            expiresIn = testExpiresIn,
            refreshExpiresIn = 1800,
            tokenType = "bearer",
            scope = "vault.access"
        )

        val expectedParams = mapOf(
            "grant_type" to "client_credentials",
            "client_id" to testClientId,
            "client_secret" to testClientSecret
        )

        coEvery { mockAuthApi.getToken(expectedParams) } returns Response.success(expectedResponse)

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
        val authRepositorySpy = spyk(authRepository)
        every { authRepositorySpy.getNowInMillis() } returns baseTime

        val tokenResponse = AuthTokenResponse(
            accessToken = testAccessToken,
            expiresIn = testExpiresIn,
            refreshExpiresIn = 1800,
            tokenType = "bearer",
            scope = "vault.access"
        )

        coEvery { mockAuthApi.getToken(any()) } returns Response.success(tokenResponse)

        // First call to set up token
        authRepositorySpy.getAccessToken()

        // Act - Second call should reuse token
        val token = authRepositorySpy.getAccessToken()

        // Assert
        assertEquals("Should reuse existing token", testAccessToken, token)
        // Should only make one API call
        io.mockk.coVerify(exactly = 1) { mockAuthApi.getToken(any()) }
    }

    @Test
    fun shouldRefreshExpiredToken() = runTest {
        // Arrange
        val baseTime = 1000000L
        val authRepositorySpy = spyk(authRepository)

        val oldToken = "old-expired-token"
        val newToken = "new-refreshed-token"

        // Set up expired token first
        val expiredResponse = AuthTokenResponse(
            accessToken = oldToken,
            expiresIn = 60, // 1 minute
            refreshExpiresIn = 120,
            tokenType = "bearer",
            scope = "vault.access"
        )

        val newResponse = AuthTokenResponse(
            accessToken = newToken,
            expiresIn = testExpiresIn,
            refreshExpiresIn = 1800,
            tokenType = "bearer",
            scope = "vault.access"
        )

        coEvery { mockAuthApi.getToken(any()) } returnsMany listOf(
            Response.success(expiredResponse),
            Response.success(newResponse)
        )

        // Mock time progression
        every { authRepositorySpy.getNowInMillis() } returns baseTime

        // Get initial token
        val firstToken = authRepositorySpy.getAccessToken()
        assertEquals("Should get first token", oldToken, firstToken)

        // Move time forward to expire token (past buffer zone)
        every { authRepositorySpy.getNowInMillis() } returns baseTime + TimeUnit.MINUTES.toMillis(2)

        // Act - Should refresh expired token
        val refreshedToken = authRepositorySpy.getAccessToken()

        // Assert
        assertEquals("Should return new refreshed token", newToken, refreshedToken)
        assertTrue("New token should be valid", authRepositorySpy.isTokenValid())
    }

    @Test
    fun shouldUseCorrectParametersForTokenRequest() = runTest {
        // Arrange
        val expectedResponse = AuthTokenResponse(
            accessToken = testAccessToken,
            expiresIn = testExpiresIn,
            refreshExpiresIn = 1800,
            tokenType = "bearer",
            scope = "vault.access"
        )

        val expectedParams = mapOf(
            "grant_type" to "client_credentials",
            "client_id" to testClientId,
            "client_secret" to testClientSecret
        )

        coEvery { mockAuthApi.getToken(expectedParams) } returns Response.success(expectedResponse)

        // Act
        authRepository.getAccessToken()

        // Assert
        io.mockk.coVerify(exactly = 1) { mockAuthApi.getToken(expectedParams) }
    }

    @Test
    fun shouldRefreshTokenExplicitly() = runTest {
        // Arrange
        val newToken = "new-refreshed-token"
        val expectedResponse = AuthTokenResponse(
            accessToken = newToken,
            expiresIn = testExpiresIn,
            refreshExpiresIn = 1800,
            tokenType = "bearer",
            scope = "vault.access"
        )

        coEvery { mockAuthApi.getToken(any()) } returns Response.success(expectedResponse)

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
class AuthRepositoryPreciseTimeTest {
    private lateinit var mockNetworkProvider: NetworkProvider
    private lateinit var mockAuthApi: AuthApi
    private lateinit var mockLogger: VaultLogger
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setUp() {
        mockNetworkProvider = mockk()
        mockAuthApi = mockk()
        mockLogger = mockk(relaxed = true)

        every { mockNetworkProvider.authApi } returns mockAuthApi

        val realRepository = AuthRepositoryImpl(
            networkProvider = mockNetworkProvider,
            clientId = "test-client",
            clientSecret = "test-secret",
            logger = mockLogger
        )
        authRepository = spyk(realRepository)
    }

    @Test
    fun shouldValidateTokenWithPreciseTimeControl() = runTest {
        // Arrange
        val baseTime = 1000000L
        val tokenResponse = AuthTokenResponse(
            accessToken = "time-test-token",
            expiresIn = 3600, // 1 hour
            refreshExpiresIn = 1800,
            tokenType = "bearer",
            scope = "vault.access"
        )

        coEvery { mockAuthApi.getToken(any()) } returns Response.success(tokenResponse)

        // Mock the time function to return specific times
        every { authRepository.getNowInMillis() } returns baseTime

        // Act - Get token at baseTime
        authRepository.getAccessToken()

        // Assert - Token should be valid immediately
        assertTrue("Token should be valid at acquisition time", authRepository.isTokenValid())

        // Move time forward to just before buffer zone (31 seconds before expiration)
        every { authRepository.getNowInMillis() } returns baseTime + TimeUnit.HOURS.toMillis(1) - TimeUnit.SECONDS.toMillis(
            31
        )
        assertTrue(
            "Token should be valid 31 seconds before expiration",
            authRepository.isTokenValid()
        )

        // Move time to exactly at buffer zone (30 seconds before expiration)
        every { authRepository.getNowInMillis() } returns baseTime + TimeUnit.HOURS.toMillis(1) - TimeUnit.SECONDS.toMillis(
            30
        )
        assertFalse("Token should be invalid at 30 second buffer", authRepository.isTokenValid())

        // Move time to within buffer zone (15 seconds before expiration)
        every { authRepository.getNowInMillis() } returns baseTime + TimeUnit.HOURS.toMillis(1) - TimeUnit.SECONDS.toMillis(
            15
        )
        assertFalse("Token should be invalid within buffer zone", authRepository.isTokenValid())

        // Move time past expiration
        every { authRepository.getNowInMillis() } returns baseTime + TimeUnit.HOURS.toMillis(1) + TimeUnit.SECONDS.toMillis(
            1
        )
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
            val baseTime = 2000000L
            val expirationTime = baseTime + TimeUnit.HOURS.toMillis(1)
            val testTime = expirationTime - TimeUnit.SECONDS.toMillis(secondsBeforeExpiration)

            val tokenResponse = AuthTokenResponse(
                accessToken = "boundary-test-token",
                expiresIn = 3600,
                refreshExpiresIn = 1800,
                tokenType = "bearer",
                scope = "vault.access"
            )

            // Create fresh repository for each test case
            val freshRepository = AuthRepositoryImpl(
                networkProvider = mockNetworkProvider,
                clientId = "test-client",
                clientSecret = "test-secret",
                logger = mockLogger
            )
            val repositorySpy = spyk(freshRepository)

            coEvery { mockAuthApi.getToken(any()) } returns Response.success(tokenResponse)

            // Set up token at base time
            every { repositorySpy.getNowInMillis() } returns baseTime
            repositorySpy.getAccessToken()

            // Move to test time
            every { repositorySpy.getNowInMillis() } returns testTime

            // Act & Assert
            val isValid = repositorySpy.isTokenValid()
            assertEquals(description, expectedValid, isValid)
        }
    }

}

// ====================================
// Error Handling Tests
// ====================================
@RunWith(Parameterized::class)
class AuthRepositoryErrorHandlingTest(
    private val httpCode: Int,
    private val errorBody: String,
    private val description: String
) {
    private lateinit var mockNetworkProvider: NetworkProvider
    private lateinit var mockAuthApi: AuthApi
    private lateinit var mockLogger: VaultLogger
    private lateinit var authRepository: AuthRepositoryImpl

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle error: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(400, "Bad Request", "400 Bad Request"),
                arrayOf(401, "Unauthorized", "401 Unauthorized"),
                arrayOf(403, "Forbidden", "403 Forbidden"),
                arrayOf(404, "Not Found", "404 Not Found"),
                arrayOf(500, "Internal Server Error", "500 Internal Server Error"),
                arrayOf(502, "Bad Gateway", "502 Bad Gateway"),
                arrayOf(503, "Service Unavailable", "503 Service Unavailable")
            )
        }
    }

    @Before
    fun setUp() {
        mockNetworkProvider = mockk()
        mockAuthApi = mockk()
        mockLogger = mockk(relaxed = true)

        every { mockNetworkProvider.authApi } returns mockAuthApi

        authRepository = AuthRepositoryImpl(
            networkProvider = mockNetworkProvider,
            clientId = "test-client",
            clientSecret = "test-secret",
            logger = mockLogger
        )
    }

    @Test
    fun shouldThrowHttpErrorsCorrectly() = runTest {
        // Arrange
        val errorResponseBody = mockk<ResponseBody>(relaxed = true)
        every { errorResponseBody.string() } returns errorBody

        val errorResponse = Response.error<AuthTokenResponse>(httpCode, errorResponseBody)
        coEvery { mockAuthApi.getToken(any()) } returns errorResponse

        // Act & Assert
        try {
            authRepository.refreshAccessToken()
            org.junit.Assert.fail("Expected exception but none was thrown")
        } catch (e: Exception) {
            assertTrue(
                "Error message should contain HTTP code",
                e.message?.contains(httpCode.toString()) == true
            )
            assertTrue(
                "Error message should contain error body",
                e.message?.contains(errorBody) == true
            )
        }
    }
}

class AuthRepositoryNetworkErrorTest {
    private lateinit var mockNetworkProvider: NetworkProvider
    private lateinit var mockAuthApi: AuthApi
    private lateinit var mockLogger: VaultLogger
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setUp() {
        mockNetworkProvider = mockk()
        mockAuthApi = mockk()
        mockLogger = mockk(relaxed = true)

        every { mockNetworkProvider.authApi } returns mockAuthApi

        authRepository = AuthRepositoryImpl(
            networkProvider = mockNetworkProvider,
            clientId = "test-client",
            clientSecret = "test-secret",
            logger = mockLogger
        )
    }

    @Test
    fun shouldThrowNetworkIOException() = runTest {
        // Arrange
        coEvery { mockAuthApi.getToken(any()) } throws IOException("Network connection failed")

        // Act & Assert
        try {
            authRepository.refreshAccessToken()
            org.junit.Assert.fail("Expected IOException but none was thrown")
        } catch (e: IOException) {
            assertEquals("Network connection failed", e.message)
        }
    }

    @Test
    fun shouldThrowGenericException() = runTest {
        // Arrange
        coEvery { mockAuthApi.getToken(any()) } throws RuntimeException("Unexpected error")

        // Act & Assert
        try {
            authRepository.refreshAccessToken()
            org.junit.Assert.fail("Expected RuntimeException but none was thrown")
        } catch (e: RuntimeException) {
            assertEquals("Unexpected error", e.message)
        }
    }

    @Test
    fun shouldHandleNullResponseBody() = runTest {
        // Arrange
        val errorResponse = Response.error<AuthTokenResponse>(
            400,
            ResponseBody.create(null, "")
        )
        coEvery { mockAuthApi.getToken(any()) } returns errorResponse

        // Act & Assert
        try {
            authRepository.refreshAccessToken()
            org.junit.Assert.fail("Expected exception but none was thrown")
        } catch (e: Exception) {
            assertTrue(
                "Should handle null response body gracefully",
                e.message?.contains("400") == true
            )
        }
    }
}

// ====================================
// Response Parsing Tests
// ====================================
@RunWith(Parameterized::class)
class AuthRepositoryResponseParsingTest(
    private val accessToken: String,
    private val expiresIn: Int,
    private val refreshExpiresIn: Int,
    private val tokenType: String,
    private val scope: String,
    private val description: String
) {
    private lateinit var mockNetworkProvider: NetworkProvider
    private lateinit var mockAuthApi: AuthApi
    private lateinit var mockLogger: VaultLogger
    private lateinit var authRepository: AuthRepositoryImpl

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should parse response: {5}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("token123", 3600, 1800, "bearer", "vault.access", "Standard response"),
                arrayOf(
                    "very-long-token-with-special-chars-!@#$%",
                    7200,
                    3600,
                    "Bearer",
                    "vault.read vault.write",
                    "Long token with special chars"
                ),
                arrayOf("short", 60, 120, "bearer", "vault", "Minimal response"),
                arrayOf("", 3600, 1800, "bearer", "vault.access", "Empty token"),
                arrayOf(
                    "token",
                    31,
                    60,
                    "bearer",
                    "",
                    "Short expiration time"
                ), // 31 seconds - just over buffer
                arrayOf(
                    "unicode-token-ñáéíóú",
                    86400,
                    43200,
                    "bearer",
                    "vault.admin",
                    "Unicode token"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockNetworkProvider = mockk()
        mockAuthApi = mockk()
        mockLogger = mockk(relaxed = true)

        every { mockNetworkProvider.authApi } returns mockAuthApi

        authRepository = AuthRepositoryImpl(
            networkProvider = mockNetworkProvider,
            clientId = "test-client",
            clientSecret = "test-secret",
            logger = mockLogger
        )
    }

    @Test
    fun shouldParseTokenResponseCorrectly() = runTest {
        // Arrange
        val tokenResponse = AuthTokenResponse(
            accessToken = accessToken,
            expiresIn = expiresIn,
            refreshExpiresIn = refreshExpiresIn,
            tokenType = tokenType,
            scope = scope
        )

        coEvery { mockAuthApi.getToken(any()) } returns Response.success(tokenResponse)

        // Act
        val resultToken = authRepository.getAccessToken()

        // Assert
        assertEquals(
            "Should return correct access token for: $description",
            accessToken,
            resultToken
        )

        // Verify token validity based on expiration time
        val expectedValid = expiresIn > 30 // Should be valid if expires in more than 30 seconds
        assertEquals(
            "Token validity should match expiration time for: $description",
            expectedValid,
            authRepository.isTokenValid()
        )
    }
}