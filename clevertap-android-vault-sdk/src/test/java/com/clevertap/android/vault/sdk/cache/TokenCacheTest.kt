package com.clevertap.android.vault.sdk.cache

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.Assert.*
import java.util.*

// ====================================
// Constructor and State Tests
// ====================================
@RunWith(Parameterized::class)
class TokenCacheConstructorTest(
    private val enabled: Boolean,
    private val description: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should create cache with {1} state")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(true, "Enabled"),
                arrayOf(false, "Disabled")
            )
        }
    }

    @Test
    fun shouldCreateCacheWithDifferentEnabledStates() {
        // Act
        val cache = TokenCache(enabled)

        // Assert
        assertNotNull(cache)
        assertEquals(enabled, cache.isEnabled())
    }
}

class TokenCacheStateTest {
    @Test
    fun shouldMaintainEnabledStateConsistency() {
        // Arrange
        val enabledCache = TokenCache(enabled = true)
        val disabledCache = TokenCache(enabled = false)

        // Act & Assert
        assertTrue(enabledCache.isEnabled())
        assertFalse(disabledCache.isEnabled())

        // State should remain consistent after operations
        enabledCache.putToken("value", "token", "string")
        disabledCache.putToken("value", "token", "string")

        assertTrue(enabledCache.isEnabled())
        assertFalse(disabledCache.isEnabled())
    }
}

// ====================================
// Token Operations - Enabled Cache Tests
// ====================================
@RunWith(Parameterized::class)
class TokenCacheEnabledStorageRetrievalTest(
    private val token: String,
    private val value: String,
    private val dataType: String,
    private val description: String
) {
    private lateinit var cache: TokenCache

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle: {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("token_123", "value_123", "string", "Basic token-value pair"),
                arrayOf("555-67-8901", "555-12-3456", "string", "SSN format"),
                arrayOf("abc@example.com", "john@example.com", "string", "Email format"),
                arrayOf("4333-5555-7777-8888", "4111-2222-3333-4444", "string", "Credit card format"),
                arrayOf("12345", "67890", "integer", "Integer token"),
                arrayOf("9876543210", "1234567890", "long", "Long token"),
                arrayOf("123.45", "67.89", "float", "Float token"),
                arrayOf("true", "false", "boolean", "Boolean token"),
                arrayOf("", "", "string", "Empty values"),
                arrayOf("🚀", "🌟", "string", "Unicode tokens"),
                arrayOf("multi\nline\ntoken", "multi\nline\nvalue", "string", "Multi-line content")
            )
        }
    }

    @Before
    fun setUp() {
        cache = TokenCache(enabled = true)
    }

    @Test
    fun shouldStoreAndRetrieveTokensCorrectly() {
        // Act - Store token mapping
        cache.putToken(value, token, dataType)

        // Assert - Retrieve token
        val retrievedTokenPair = cache.getToken(value)
        assertNotNull("Should retrieve token for: $description", retrievedTokenPair)
        assertEquals("Token should match for: $description", token, retrievedTokenPair!!.first)
        assertEquals("DataType should match for: $description", dataType, retrievedTokenPair.second)
    }

    @Test
    fun shouldStoreAndRetrieveValuesCorrectly() {
        // Act - Store value mapping
        cache.putValue(token, value, dataType)

        // Assert - Retrieve value
        val retrievedValuePair = cache.getValue(token)
        assertNotNull("Should retrieve value for: $description", retrievedValuePair)
        assertEquals("Value should match for: $description", value, retrievedValuePair!!.first)
        assertEquals("DataType should match for: $description", dataType, retrievedValuePair.second)
    }
}

@RunWith(Parameterized::class)
class TokenCacheDataTypeHandlingTest(
    private val inputDataType: String?,
    private val expectedDataType: String,
    private val description: String
) {
    private lateinit var cache: TokenCache

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle dataType properly: {2}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf(null, "string", "Null dataType should default to string"),
                arrayOf("string", "string", "String dataType"),
                arrayOf("integer", "integer", "Integer dataType"),
                arrayOf("long", "long", "Long dataType"),
                arrayOf("float", "float", "Float dataType"),
                arrayOf("double", "double", "Double dataType"),
                arrayOf("boolean", "boolean", "Boolean dataType"),
                arrayOf("custom_type", "custom_type", "Custom dataType")
            )
        }
    }

    @Before
    fun setUp() {
        cache = TokenCache(enabled = true)
    }

    @Test
    fun shouldHandleDifferentDataTypesCorrectly() {
        // Arrange
        val token = "test_token"
        val value = "test_value"

        // Act
        cache.putToken(value, token, inputDataType)

        // Assert
        val retrievedPair = cache.getToken(value)
        assertNotNull(retrievedPair)
        assertEquals(description, expectedDataType, retrievedPair!!.second)
    }
}

class TokenCacheEnabledOperationsTest {
    private lateinit var cache: TokenCache

    @Before
    fun setUp() {
        cache = TokenCache(enabled = true)
    }

    @Test
    fun shouldHandleBidirectionalStorageCorrectly() {
        // Arrange
        val token = "token_123"
        val value = "value_123"
        val dataType = "string"

        // Act
        cache.putBidirectional(token, value, dataType)

        // Assert - Both directions should work
        val tokenPair = cache.getToken(value)
        val valuePair = cache.getValue(token)

        assertNotNull(tokenPair)
        assertNotNull(valuePair)
        assertEquals(token, tokenPair!!.first)
        assertEquals(dataType, tokenPair.second)
        assertEquals(value, valuePair!!.first)
        assertEquals(dataType, valuePair.second)
    }

    @Test
    fun shouldUpdateExistingMappings() {
        // Arrange
        val value = "test_value"
        val oldToken = "old_token"
        val newToken = "new_token"
        val oldDataType = "string"
        val newDataType = "integer"

        // Act - Store initial mapping
        cache.putToken(value, oldToken, oldDataType)
        val initialPair = cache.getToken(value)

        // Act - Update mapping
        cache.putToken(value, newToken, newDataType)
        val updatedPair = cache.getToken(value)

        // Assert
        assertNotNull(initialPair)
        assertEquals(oldToken, initialPair!!.first)
        assertEquals(oldDataType, initialPair.second)

        assertNotNull(updatedPair)
        assertEquals(newToken, updatedPair!!.first)
        assertEquals(newDataType, updatedPair.second)
    }

    @Test
    fun shouldHandleMultipleDifferentMappings() {
        // Arrange
        val mappings = listOf(
            Triple("value1", "token1", "string"),
            Triple("value2", "token2", "integer"),
            Triple("value3", "token3", "boolean")
        )

        // Act
        mappings.forEach { (value, token, dataType) ->
            cache.putToken(value, token, dataType)
        }

        // Assert
        mappings.forEach { (value, expectedToken, expectedDataType) ->
            val retrievedPair = cache.getToken(value)
            assertNotNull("Should find mapping for $value", retrievedPair)
            assertEquals(expectedToken, retrievedPair!!.first)
            assertEquals(expectedDataType, retrievedPair.second)
        }

    }
}

// ====================================
// Token Operations - Disabled Cache Tests
// ====================================
class TokenCacheDisabledOperationsTest {
    private lateinit var cache: TokenCache

    @Before
    fun setUp() {
        cache = TokenCache(enabled = false)
    }

    @Test
    fun shouldNotStoreTokensWhenDisabled() {
        // Act
        cache.putToken("value", "token", "string")
        cache.putValue("token", "value", "string")
        cache.putBidirectional("token2", "value2", "string")

        // Assert
        assertNull(cache.getToken("value"))
        assertNull(cache.getValue("token"))
        assertNull(cache.getToken("value2"))
        assertNull(cache.getValue("token2"))
    }

    @Test
    fun shouldReturnNullForAllQueriesWhenDisabled() {
        // Act & Assert
        assertNull(cache.getToken("any_value"))
        assertNull(cache.getValue("any_token"))
    }

}

// ====================================
// Cache Management Tests
// ====================================
class TokenCacheManagementTest {
    private lateinit var cache: TokenCache

    @Before
    fun setUp() {
        cache = TokenCache(enabled = true)
    }

    @Test
    fun shouldClearAllEntries() {
        // Arrange
        cache.putToken("value1", "token1", "string")
        cache.putToken("value2", "token2", "integer")
        cache.putBidirectional("token3", "value3", "boolean")

        // Act
        cache.clear()

        // Assert
        assertNull(cache.getToken("value1"))
        assertNull(cache.getToken("value2"))
        assertNull(cache.getValue("token3"))
        assertNull(cache.getToken("value3"))
    }

}

// ====================================
// Edge Cases and Error Handling Tests
// ====================================
class TokenCacheEdgeCaseTest {
    private lateinit var cache: TokenCache

    @Before
    fun setUp() {
        cache = TokenCache(enabled = true)
    }

    @Test
    fun shouldHandleEmptyKeysGracefully() {
        // Act & Assert - Should not throw exceptions
        val emptyKey = ""
        cache.putToken(emptyKey, "token", "string")
        cache.putValue(emptyKey, "value", "string")
        cache.getToken(emptyKey)
        cache.getValue(emptyKey)
        // Test passes if no exceptions are thrown
    }

    @Test
    fun shouldHandleDuplicateTokensWithDifferentValues() {
        // Arrange
        val token = "duplicate_token"
        val value1 = "value1"
        val value2 = "value2"

        // Act
        cache.putValue(token, value1, "string")
        val firstResult = cache.getValue(token)

        cache.putValue(token, value2, "string") // Update
        val secondResult = cache.getValue(token)

        // Assert
        assertNotNull(firstResult)
        assertEquals(value1, firstResult!!.first)

        assertNotNull(secondResult)
        assertEquals(value2, secondResult!!.first) // Should be updated
    }

    @Test
    fun shouldHandleDuplicateValuesWithDifferentTokens() {
        // Arrange
        val value = "duplicate_value"
        val token1 = "token1"
        val token2 = "token2"

        // Act
        cache.putToken(value, token1, "string")
        val firstResult = cache.getToken(value)

        cache.putToken(value, token2, "string") // Update
        val secondResult = cache.getToken(value)

        // Assert
        assertNotNull(firstResult)
        assertEquals(token1, firstResult!!.first)

        assertNotNull(secondResult)
        assertEquals(token2, secondResult!!.first) // Should be updated
    }


    @Test
    fun shouldHandleSpecialCharactersInTokensAndValues() {
        // Arrange
        val specialCases = listOf(
            "token with spaces" to "value with spaces",
            "token\nwith\nnewlines" to "value\nwith\nnewlines",
            "token!@#$%^&*()" to "value!@#$%^&*()",
            "token_🚀🌟✨" to "value_🚀🌟✨",
            "token\t\r\n" to "value\t\r\n"
        )

        // Act & Assert
        specialCases.forEach { (token, value) ->
            cache.putBidirectional(token, value, "string")

            val retrievedTokenPair = cache.getToken(value)
            val retrievedValuePair = cache.getValue(token)

            assertNotNull("Should handle special token: $token", retrievedTokenPair)
            assertNotNull("Should handle special value: $value", retrievedValuePair)

            assertEquals(token, retrievedTokenPair!!.first)
            assertEquals(value, retrievedValuePair!!.first)
        }
    }
}
