package com.clevertap.android.vault.sdk.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


// ====================================
// Lazy Initialization Tests
// ====================================
class NetworkProviderLazyInitializationTest {
    private lateinit var networkProvider: NetworkProvider

    @Before
    fun setUp() {
        networkProvider = NetworkProvider(
            "https://api.test.com/",
            "https://auth.test.com/"
        )
    }

    @Test
    fun shouldLazilyInitializeTokenizationApi() {
        // Act - Access the API for the first time
        val api1 = networkProvider.tokenizationApi
        val api2 = networkProvider.tokenizationApi

        // Assert - Should return the same instance (lazy singleton)
        assertNotNull(api1)
        assertNotNull(api2)
        assertSame("Should return same instance on multiple calls", api1, api2)
    }

    @Test
    fun shouldLazilyInitializeAuthApi() {
        // Act - Access the API for the first time
        val api1 = networkProvider.authApi
        val api2 = networkProvider.authApi

        // Assert - Should return the same instance (lazy singleton)
        assertNotNull(api1)
        assertNotNull(api2)
        assertSame("Should return same instance on multiple calls", api1, api2)
    }

    @Test
    fun shouldLazilyInitializeOkHttpClient() {
        // Act - Access the client for the first time
        val client1 = networkProvider.okHttpClient
        val client2 = networkProvider.okHttpClient

        // Assert - Should return the same instance (lazy singleton)
        assertNotNull(client1)
        assertNotNull(client2)
        assertSame("Should return same instance on multiple calls", client1, client2)
    }

    @Test
    fun shouldLazilyInitializeRetrofitInstances() {
        // Act - Access Retrofit instances
        val tokenRetrofit1 = networkProvider.tokenizationRetrofit
        val tokenRetrofit2 = networkProvider.tokenizationRetrofit
        val authRetrofit1 = networkProvider.authRetrofit
        val authRetrofit2 = networkProvider.authRetrofit

        // Assert - Should return same instances
        assertNotNull(tokenRetrofit1)
        assertNotNull(authRetrofit1)
        assertSame("Tokenization Retrofit should be singleton", tokenRetrofit1, tokenRetrofit2)
        assertSame("Auth Retrofit should be singleton", authRetrofit1, authRetrofit2)
        assertNotSame("Should have different Retrofit instances", tokenRetrofit1, authRetrofit1)
    }
}

// ====================================
// Configuration Tests
// ====================================
class NetworkProviderConfigurationTest {
    private lateinit var networkProvider: NetworkProvider

    @Before
    fun setUp() {
        networkProvider = NetworkProvider(
            "https://api.test.com/",
            "https://auth.test.com/"
        )
    }

    @Test
    fun shouldConfigureOkHttpClientWithCorrectTimeouts() {
        // Act
        val client = networkProvider.okHttpClient

        // Assert - Check timeout configuration
        assertEquals("Connect timeout should be 15 seconds", 15000, client.connectTimeoutMillis)
        assertEquals("Read timeout should be 15 seconds", 15000, client.readTimeoutMillis)
        assertEquals("Write timeout should be 15 seconds", 15000, client.writeTimeoutMillis)
    }

    @Test
    fun shouldConfigureTokenizationRetrofitWithCorrectBaseUrl() {
        // Arrange
        val expectedBaseUrl = "https://api.test.com/"
        val provider = NetworkProvider(expectedBaseUrl, "https://auth.test.com/")

        // Act
        val retrofit = provider.tokenizationRetrofit

        // Assert
        assertEquals(
            "Tokenization Retrofit should have correct base URL",
            expectedBaseUrl, retrofit.baseUrl().toString()
        )
    }

    @Test
    fun shouldConfigureAuthRetrofitWithCorrectBaseUrl() {
        // Arrange
        val expectedBaseUrl = "https://auth.test.com/"
        val provider = NetworkProvider("https://api.test.com/", expectedBaseUrl)

        // Act
        val retrofit = provider.authRetrofit

        // Assert
        assertEquals(
            "Auth Retrofit should have correct base URL",
            expectedBaseUrl, retrofit.baseUrl().toString()
        )
    }

    @Test
    fun shouldUseSameOkHttpClientForBothRetrofitInstances() {
        // Act
        val tokenRetrofit = networkProvider.tokenizationRetrofit
        val authRetrofit = networkProvider.authRetrofit
        val directClient = networkProvider.okHttpClient

        // Assert - Both Retrofit instances should use the same OkHttpClient
        assertSame(
            "Tokenization Retrofit should use the shared client",
            directClient, tokenRetrofit.callFactory()
        )
        assertSame(
            "Auth Retrofit should use the shared client",
            directClient, authRetrofit.callFactory()
        )
        assertSame(
            "Both Retrofits should use the same client",
            tokenRetrofit.callFactory(), authRetrofit.callFactory()
        )
    }
}

// ====================================
// API Creation Tests
// ====================================
@RunWith(Parameterized::class)
class NetworkProviderApiCreationTest(
    private val apiUrl: String,
    private val authUrl: String,
    private val description: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should create APIs for: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    "https://api.example.com/",
                    "https://auth.example.com/",
                    "Standard HTTPS URLs"
                ),
                arrayOf(
                    "http://localhost:8080/api/",
                    "http://localhost:9000/auth/",
                    "Localhost with ports"
                ),
                arrayOf(
                    "https://api.test-env.company.com/v1/",
                    "https://auth.test-env.company.com/v1/",
                    "Complex URLs with paths"
                ),
                arrayOf(
                    "http://192.168.1.100:3000/",
                    "http://192.168.1.200:4000/",
                    "IP addresses with ports"
                )
            )
        }
    }

    private lateinit var networkProvider: NetworkProvider

    @Before
    fun setUp() {
        networkProvider = NetworkProvider(apiUrl, authUrl)
    }

    @Test
    fun shouldCreateTokenizationApiSuccessfully() {
        // Act
        val tokenizationApi = networkProvider.tokenizationApi

        // Assert
        assertNotNull("TokenizationApi should be created for: $description", tokenizationApi)
    }

    @Test
    fun shouldCreateAuthApiSuccessfully() {
        // Act
        val authApi = networkProvider.authApi

        // Assert
        assertNotNull("AuthApi should be created for: $description", authApi)
    }
}

// ====================================
// Integration Tests
// ====================================
class NetworkProviderIntegrationTest {

    @Test
    fun shouldCreateCompletelyConfiguredNetworkStack() {
        // Arrange
        val apiUrl = "https://api.example.com/"
        val authUrl = "https://auth.example.com/"

        // Act
        val networkProvider = NetworkProvider(apiUrl, authUrl)

        // Assert - Complete network stack should be properly configured
        val tokenizationApi = networkProvider.tokenizationApi
        val authApi = networkProvider.authApi
        val okHttpClient = networkProvider.okHttpClient
        val tokenRetrofit = networkProvider.tokenizationRetrofit
        val authRetrofit = networkProvider.authRetrofit

        // All components should be created
        assertNotNull("TokenizationApi should be created", tokenizationApi)
        assertNotNull("AuthApi should be created", authApi)
        assertNotNull("OkHttpClient should be created", okHttpClient)
        assertNotNull("Tokenization Retrofit should be created", tokenRetrofit)
        assertNotNull("Auth Retrofit should be created", authRetrofit)

        // Components should be properly configured
        assertEquals("Tokenization Retrofit base URL", apiUrl, tokenRetrofit.baseUrl().toString())
        assertEquals("Auth Retrofit base URL", authUrl, authRetrofit.baseUrl().toString())
        assertSame(
            "Retrofits should share the same client",
            tokenRetrofit.callFactory(),
            authRetrofit.callFactory()
        )
        assertSame("Client should be the same instance", okHttpClient, tokenRetrofit.callFactory())
        assertSame("Auth Retrofit should use configured client", okHttpClient, authRetrofit.callFactory())
        assertEquals("Client connect timeout", 15000, okHttpClient.connectTimeoutMillis)
        assertEquals("Client read timeout", 15000, okHttpClient.readTimeoutMillis)
        assertEquals("Client write timeout", 15000, okHttpClient.writeTimeoutMillis)
    }

}