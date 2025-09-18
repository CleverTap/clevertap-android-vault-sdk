package com.clevertap.demo.ctvaultsdk.config

import android.util.Log

/**
 * Configuration class for Vault SDK testing scenarios
 *
 * Enable/disable different test cases by changing these flags.
 * When all flags are false, normal production initialization will be used.
 */
object VaultTestConfig {

    // Test Configuration Flags
    const val ENABLE_MULTI_THREAD_INIT_TEST = false
    const val ENABLE_GET_INSTANCE_BEFORE_INIT_TEST = true
    const val ENABLE_INVALID_CREDENTIALS_TEST = false
    const val ENABLE_INVALID_URL_TEST = false
    const val ENABLE_ENCRYPTION_DISABLED_TEST = false
    const val ENABLE_CACHE_DISABLED_TEST = false
    const val ENABLE_DEBUG_MODE_TEST = false

    // Test Configuration Details
    data class TestCredentials(
        val clientId: String,
        val clientSecret: String,
        val apiUrl: String,
        val authUrl: String,
        val enableEncryption: Boolean = true,
        val enableCache: Boolean = true,
        val debugMode: Boolean = true
    )

    // Valid configuration for normal operation
    val VALID_CONFIG = TestCredentials(
        clientId = "navin-client",
        clientSecret = "83EpGODDbeyepKaGRy1Srg4c4YVEFQG8",
        apiUrl = "http://3.35.104.51:8080/ct-vault/",
        authUrl = "https://auth-test.clevertap.net/auth/realms/master/"
    )

    // Invalid configurations for testing
    val INVALID_CREDENTIALS_CONFIG = VALID_CONFIG.copy(
        clientId = "invalid-client",
        clientSecret = "invalid-secret"
    )

    val INVALID_API_URL_CONFIG = VALID_CONFIG.copy(
        apiUrl = "http://invalid-api-url.com/"
    )

    val INVALID_AUTH_URL_CONFIG = VALID_CONFIG.copy(
        authUrl = "http://invalid-auth-url.com/"
    )

    val ENCRYPTION_DISABLED_CONFIG = VALID_CONFIG.copy(
        enableEncryption = false
    )

    val CACHE_DISABLED_CONFIG = VALID_CONFIG.copy(
        enableCache = false
    )

    val DEBUG_DISABLED_CONFIG = VALID_CONFIG.copy(
        debugMode = false
    )

    /**
     * Returns the configuration to use based on enabled test flags
     */
    fun getActiveTestConfig(): TestCredentials {
        return when {
            ENABLE_INVALID_CREDENTIALS_TEST -> {
                Log.w("VaultTestConfig", "🧪 TEST MODE: Invalid Credentials Test")
                INVALID_CREDENTIALS_CONFIG
            }
            ENABLE_INVALID_URL_TEST -> {
                Log.w("VaultTestConfig", "🧪 TEST MODE: Invalid URL Test")
                INVALID_API_URL_CONFIG
            }
            ENABLE_ENCRYPTION_DISABLED_TEST -> {
                Log.w("VaultTestConfig", "🧪 TEST MODE: Encryption Disabled Test")
                ENCRYPTION_DISABLED_CONFIG
            }
            ENABLE_CACHE_DISABLED_TEST -> {
                Log.w("VaultTestConfig", "🧪 TEST MODE: Cache Disabled Test")
                CACHE_DISABLED_CONFIG
            }
            ENABLE_DEBUG_MODE_TEST -> {
                Log.w("VaultTestConfig", "🧪 TEST MODE: Debug Mode Test")
                DEBUG_DISABLED_CONFIG
            }
            else -> {
                Log.i("VaultTestConfig", "✅ PRODUCTION MODE: Using valid configuration")
                VALID_CONFIG
            }
        }
    }

    /**
     * Checks if any test mode is enabled
     */
    fun isAnyTestModeEnabled(): Boolean {
        return ENABLE_MULTI_THREAD_INIT_TEST ||
                ENABLE_GET_INSTANCE_BEFORE_INIT_TEST ||
                ENABLE_INVALID_CREDENTIALS_TEST ||
                ENABLE_INVALID_URL_TEST ||
                ENABLE_ENCRYPTION_DISABLED_TEST ||
                ENABLE_CACHE_DISABLED_TEST ||
                ENABLE_DEBUG_MODE_TEST
    }

    /**
     * Logs the current test configuration
     */
    fun logCurrentConfig() {
        Log.d("VaultTestConfig", "=== Current Test Configuration ===")
        Log.d("VaultTestConfig", "Multi-thread Init Test: $ENABLE_MULTI_THREAD_INIT_TEST")
        Log.d("VaultTestConfig", "Get Instance Before Init Test: $ENABLE_GET_INSTANCE_BEFORE_INIT_TEST")
        Log.d("VaultTestConfig", "Invalid Credentials Test: $ENABLE_INVALID_CREDENTIALS_TEST")
        Log.d("VaultTestConfig", "Invalid URL Test: $ENABLE_INVALID_URL_TEST")
        Log.d("VaultTestConfig", "Encryption Disabled Test: $ENABLE_ENCRYPTION_DISABLED_TEST")
        Log.d("VaultTestConfig", "Cache Disabled Test: $ENABLE_CACHE_DISABLED_TEST")
        Log.d("VaultTestConfig", "Debug Mode Test: $ENABLE_DEBUG_MODE_TEST")
        Log.d("VaultTestConfig", "===============================")
    }
}