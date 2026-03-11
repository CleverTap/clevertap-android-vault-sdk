package com.clevertap.android.zeropii.sdk.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        networkProvider = NetworkProvider("https://api.test.com/")
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

        // Assert - Should return same instances
        assertNotNull(tokenRetrofit1)
        assertSame("Tokenization Retrofit should be singleton", tokenRetrofit1, tokenRetrofit2)
    }
}

// ====================================
// Configuration Tests
// ====================================
class NetworkProviderConfigurationTest {
    private lateinit var networkProvider: NetworkProvider

    @Before
    fun setUp() {
        networkProvider = NetworkProvider("https://api.test.com/")
    }

    @Test
    fun shouldConfigureOkHttpClientWithCorrectTimeouts() {
        // Act
        val client = networkProvider.okHttpClient

        // Assert - Check timeout configuration
        assertEquals("Connect timeout should be 15 seconds", 15000, client.connectTimeoutMillis())
        assertEquals("Read timeout should be 15 seconds", 15000, client.readTimeoutMillis())
        assertEquals("Write timeout should be 15 seconds", 15000, client.writeTimeoutMillis())
    }

    @Test
    fun shouldConfigureTokenizationRetrofitWithCorrectBaseUrl() {
        // Arrange
        val expectedBaseUrl = "https://api.test.com/"
        val provider = NetworkProvider(expectedBaseUrl)

        // Act
        val retrofit = provider.tokenizationRetrofit

        // Assert
        assertEquals(
            "Tokenization Retrofit should have correct base URL",
            expectedBaseUrl, retrofit.baseUrl().toString()
        )
    }

    @Test
    fun shouldUseOkHttpClientForRetrofitInstance() {
        // Act
        val tokenRetrofit = networkProvider.tokenizationRetrofit
        val directClient = networkProvider.okHttpClient

        // Assert - Retrofit instance should use the shared OkHttpClient
        assertSame(
            "Tokenization Retrofit should use the shared client",
            directClient, tokenRetrofit.callFactory()
        )
    }
}

// ====================================
// API Creation Tests
// ====================================
@RunWith(Parameterized::class)
class NetworkProviderApiCreationTest(
    private val apiUrl: String,
    private val description: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should create APIs for: {1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    "https://api.example.com/",
                    "Standard HTTPS URL"
                ),
                arrayOf(
                    "http://localhost:8080/api/",
                    "Localhost with port"
                ),
                arrayOf(
                    "https://api.test-env.company.com/v1/",
                    "Complex URL with path"
                ),
                arrayOf(
                    "http://192.168.1.100:3000/",
                    "IP address with port"
                )
            )
        }
    }

    private lateinit var networkProvider: NetworkProvider

    @Before
    fun setUp() {
        networkProvider = NetworkProvider(apiUrl)
    }

    @Test
    fun shouldCreateTokenizationApiSuccessfully() {
        // Act
        val tokenizationApi = networkProvider.tokenizationApi

        // Assert
        assertNotNull("TokenizationApi should be created for: $description", tokenizationApi)
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

        // Act
        val networkProvider = NetworkProvider(apiUrl)

        // Assert - Complete network stack should be properly configured
        val tokenizationApi = networkProvider.tokenizationApi
        val okHttpClient = networkProvider.okHttpClient
        val tokenRetrofit = networkProvider.tokenizationRetrofit

        // All components should be created
        assertNotNull("TokenizationApi should be created", tokenizationApi)
        assertNotNull("OkHttpClient should be created", okHttpClient)
        assertNotNull("Tokenization Retrofit should be created", tokenRetrofit)

        // Components should be properly configured
        assertEquals("Tokenization Retrofit base URL", apiUrl, tokenRetrofit.baseUrl().toString())
        assertSame("Client should be the same instance", okHttpClient, tokenRetrofit.callFactory())
        assertEquals("Client connect timeout", 15000, okHttpClient.connectTimeoutMillis())
        assertEquals("Client read timeout", 15000, okHttpClient.readTimeoutMillis())
        assertEquals("Client write timeout", 15000, okHttpClient.writeTimeoutMillis())
    }

}
