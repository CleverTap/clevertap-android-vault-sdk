package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.cache.TokenCache
import com.clevertap.android.vault.sdk.model.BatchDetokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchTokenItemResponse
import com.clevertap.android.vault.sdk.util.VaultLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// ====================================
// Single Token Retrieval Tests
// ====================================
@RunWith(Parameterized::class)
class CacheManagerGetTokenTest(
    private val inputValue: String,
    private val cacheEnabled: Boolean,
    private val cachedTokenPair: Pair<String, String>?,
    private val expectedResult: String?, // null, "token", or token value
    private val description: String
) {
    private lateinit var mockTokenCache: TokenCache
    private lateinit var mockLogger: VaultLogger
    private lateinit var cacheManager: CacheManager

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle token retrieval: {4}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                // Cache enabled scenarios
                arrayOf(
                    "555-12-3456",
                    true,
                    "555-67-8901" to "string",
                    "555-67-8901",
                    "SSN found in enabled cache"
                ),
                arrayOf(
                    "john@example.com",
                    true,
                    "abc@example.com" to "string",
                    "abc@example.com",
                    "Email found in enabled cache"
                ),
                arrayOf(
                    "123456",
                    true,
                    "987654" to "integer",
                    "987654",
                    "Integer found in enabled cache"
                ),
                arrayOf(
                    "true",
                    true,
                    "false" to "boolean",
                    "false",
                    "Boolean found in enabled cache"
                ),
                arrayOf(
                    "",
                    true,
                    "empty-token" to "string",
                    "empty-token",
                    "Empty value found in enabled cache"
                ),
                arrayOf("🚀", true, "🌟" to "string", "🌟", "Unicode found in enabled cache"),
                arrayOf("new-value", true, null, null, "Value not found in enabled cache"),

                // Cache disabled scenarios
                arrayOf(
                    "555-12-3456",
                    false,
                    "555-67-8901" to "string",
                    null,
                    "Cache disabled - should return null"
                ),
                arrayOf("john@example.com", false, null, null, "Cache disabled with no data"),
            )
        }
    }

    @Before
    fun setUp() {
        mockTokenCache = mockk()
        mockLogger = mockk(relaxed = true)

        every { mockTokenCache.isEnabled() } returns cacheEnabled
        every { mockTokenCache.getToken(inputValue) } returns cachedTokenPair

        cacheManager = CacheManager(mockTokenCache, mockLogger)
    }

    @Test
    fun shouldGetTokenFromCacheCorrectly() {
        // Act
        val result = cacheManager.getTokenFromCache(inputValue)

        // Assert
        if (expectedResult == null) {
            assertNull("Should return null for: $description", result)
        } else {
            assertNotNull("Should return result for: $description", result)
            assertTrue("Should return TokenResult", result is CacheResult.TokenResult)
            val tokenResult = result as CacheResult.TokenResult
            assertEquals("Token should match", expectedResult, tokenResult.token)

            if (cachedTokenPair != null) {
                assertEquals("DataType should match", cachedTokenPair.second, tokenResult.dataType)
            }
        }

        // Verify interactions
        verify(exactly = 1) { mockTokenCache.isEnabled() }
        if (cacheEnabled) {
            verify(exactly = 1) { mockTokenCache.getToken(inputValue) }
            if (result != null) {
                verify { mockLogger.d("Token found in cache") }
            }
        } else {
            verify(exactly = 0) { mockTokenCache.getToken(any()) }
        }
    }
}

// ====================================
// Single Value Retrieval Tests
// ====================================
@RunWith(Parameterized::class)
class CacheManagerGetValueTest(
    private val inputToken: String,
    private val cacheEnabled: Boolean,
    private val cachedValuePair: Pair<String, String>?,
    private val expectedResult: String?, // null, "value", or value
    private val description: String
) {
    private lateinit var mockTokenCache: TokenCache
    private lateinit var mockLogger: VaultLogger
    private lateinit var cacheManager: CacheManager

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle value retrieval: {4}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                // Cache enabled scenarios
                arrayOf(
                    "555-67-8901",
                    true,
                    "555-12-3456" to "string",
                    "555-12-3456",
                    "SSN token found in enabled cache"
                ),
                arrayOf(
                    "abc@example.com",
                    true,
                    "john@example.com" to "string",
                    "john@example.com",
                    "Email token found in enabled cache"
                ),
                arrayOf(
                    "987654",
                    true,
                    "123456" to "integer",
                    "123456",
                    "Integer token found in enabled cache"
                ),
                arrayOf(
                    "false",
                    true,
                    "true" to "boolean",
                    "true",
                    "Boolean token found in enabled cache"
                ),
                arrayOf(
                    "empty-token",
                    true,
                    "" to "string",
                    "",
                    "Empty value token found in enabled cache"
                ),
                arrayOf("🌟", true, "🚀" to "string", "🚀", "Unicode token found in enabled cache"),
                arrayOf("invalid-token", true, null, null, "Token not found in enabled cache"),

                // Cache disabled scenarios
                arrayOf(
                    "555-67-8901",
                    false,
                    "555-12-3456" to "string",
                    null,
                    "Cache disabled - should return null"
                ),
                arrayOf("invalid-token", false, null, null, "Cache disabled with no data"),
            )
        }
    }

    @Before
    fun setUp() {
        mockTokenCache = mockk()
        mockLogger = mockk(relaxed = true)

        every { mockTokenCache.isEnabled() } returns cacheEnabled
        every { mockTokenCache.getValue(inputToken) } returns cachedValuePair

        cacheManager = CacheManager(mockTokenCache, mockLogger)
    }

    @Test
    fun shouldGetValueFromCacheCorrectly() {
        // Act
        val result = cacheManager.getValueFromCache(inputToken)

        // Assert
        if (expectedResult == null) {
            assertNull("Should return null for: $description", result)
        } else {
            assertNotNull("Should return result for: $description", result)
            assertTrue("Should return ValueResult", result is CacheResult.ValueResult)
            val valueResult = result as CacheResult.ValueResult
            assertEquals("Value should match", expectedResult, valueResult.value)

            if (cachedValuePair != null) {
                assertEquals("DataType should match", cachedValuePair.second, valueResult.dataType)
            }
        }

        // Verify interactions
        verify(exactly = 1) { mockTokenCache.isEnabled() }
        if (cacheEnabled) {
            verify(exactly = 1) { mockTokenCache.getValue(inputToken) }
            if (result != null) {
                verify { mockLogger.d("Value found in cache") }
            }
        } else {
            verify(exactly = 0) { mockTokenCache.getValue(any()) }
        }
    }
}

// ====================================
// Batch Token Retrieval Tests
// ====================================
@RunWith(Parameterized::class)
class CacheManagerBatchTokenTest(
    private val inputValues: List<String>,
    private val cacheEnabled: Boolean,
    private val cachedPairs: Map<String, Pair<String, String>>, // value -> (token, dataType)
    private val expectedCachedCount: Int,
    private val expectedUncachedCount: Int,
    private val description: String
) {
    private lateinit var mockTokenCache: TokenCache
    private lateinit var mockLogger: VaultLogger
    private lateinit var cacheManager: CacheManager

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle batch token retrieval: {5}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // Cache enabled - all cached
                arrayOf(
                    listOf("555-12-3456", "john@example.com"),
                    true,
                    mapOf(
                        "555-12-3456" to ("555-67-8901" to "string"),
                        "john@example.com" to ("abc@example.com" to "string")
                    ),
                    2, 0,
                    "All values cached with cache enabled"
                ),
                // Cache enabled - none cached
                arrayOf(
                    listOf("new-value-1", "new-value-2"),
                    true,
                    emptyMap<String, Pair<String, String>>(),
                    0, 2,
                    "No values cached with cache enabled"
                ),
                // Cache enabled - partial cached
                arrayOf(
                    listOf("555-12-3456", "new-value", "john@example.com"),
                    true,
                    mapOf(
                        "555-12-3456" to ("555-67-8901" to "string"),
                        "john@example.com" to ("abc@example.com" to "string")
                    ),
                    2, 1,
                    "Partial values cached with cache enabled"
                ),
                // Cache enabled - mixed data types
                arrayOf(
                    listOf("123456", "true", "555-12-3456"),
                    true,
                    mapOf(
                        "123456" to ("987654" to "integer"),
                        "true" to ("false" to "boolean")
                    ),
                    2, 1,
                    "Mixed data types with cache enabled"
                ),
                // Cache disabled scenarios
                arrayOf(
                    listOf("555-12-3456", "john@example.com"),
                    false,
                    emptyMap<String, Pair<String, String>>(),
                    0, 2,
                    "Cache disabled - all uncached"
                ),
                // Empty input
                arrayOf(
                    emptyList<String>(),
                    true,
                    emptyMap<String, Pair<String, String>>(),
                    0, 0,
                    "Empty input list"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockTokenCache = mockk()
        mockLogger = mockk(relaxed = true)

        every { mockTokenCache.isEnabled() } returns cacheEnabled

        // Mock individual getToken calls
        inputValues.forEach { value ->
            val cachedPair = cachedPairs[value]
            every { mockTokenCache.getToken(value) } returns cachedPair
        }

        cacheManager = CacheManager(mockTokenCache, mockLogger)
    }

    @Test
    fun shouldGetBatchTokensFromCacheCorrectly() {
        // Act
        val result = cacheManager.getBatchTokensFromCache(inputValues)

        // Assert
        assertNotNull("Should return BatchCacheResult", result)
        assertEquals("Cached count should match", expectedCachedCount, result.cached.size)
        assertEquals("Uncached count should match", expectedUncachedCount, result.uncached.size)

        // Verify cached items are BatchTokenItemResponse
        result.cached.forEach { item ->
            assertTrue(
                "Cached item should be BatchTokenItemResponse",
                item is BatchTokenItemResponse
            )
            val tokenItem = item as BatchTokenItemResponse
            assertTrue("Cached item should exist", tokenItem.exists)
            assertFalse("Cached item should not be newly created", tokenItem.newlyCreated)

            // Verify the token and dataType match expectations
            val expectedPair = cachedPairs[tokenItem.originalValue]
            if (expectedPair != null) {
                assertEquals("Token should match", expectedPair.first, tokenItem.token)
                assertEquals("DataType should match", expectedPair.second, tokenItem.dataType)
            }
        }

        // Verify uncached items
        val expectedUncached = inputValues.filter { !cachedPairs.containsKey(it) }
        assertEquals(
            "Uncached items should match",
            expectedUncached.sorted(),
            result.uncached.sorted()
        )

        // Verify interactions
        verify(exactly = 1) { mockTokenCache.isEnabled() }
        if (cacheEnabled && inputValues.isNotEmpty()) {
            inputValues.forEach { value ->
                verify(exactly = 1) { mockTokenCache.getToken(value) }
            }
        } else if (!cacheEnabled) {
            verify(exactly = 0) { mockTokenCache.getToken(any()) }
        }
    }
}


// ====================================
// Batch Value Retrieval Tests
// ====================================
@RunWith(Parameterized::class)
class CacheManagerBatchValueTest(
    private val inputTokens: List<String>,
    private val cacheEnabled: Boolean,
    private val cachedPairs: Map<String, Pair<String, String>>, // token -> (value, dataType)
    private val expectedCachedCount: Int,
    private val expectedUncachedCount: Int,
    private val description: String
) {
    private lateinit var mockTokenCache: TokenCache
    private lateinit var mockLogger: VaultLogger
    private lateinit var cacheManager: CacheManager

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle batch value retrieval: {5}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // Cache enabled - all cached
                arrayOf(
                    listOf("555-67-8901", "abc@example.com"),
                    true,
                    mapOf(
                        "555-67-8901" to ("555-12-3456" to "string"),
                        "abc@example.com" to ("john@example.com" to "string")
                    ),
                    2, 0,
                    "All tokens cached with cache enabled"
                ),
                // Cache enabled - none cached
                arrayOf(
                    listOf("invalid-token-1", "invalid-token-2"),
                    true,
                    emptyMap<String, Pair<String, String>>(),
                    0, 2,
                    "No tokens cached with cache enabled"
                ),
                // Cache enabled - partial cached
                arrayOf(
                    listOf("555-67-8901", "invalid-token", "abc@example.com"),
                    true,
                    mapOf(
                        "555-67-8901" to ("555-12-3456" to "string"),
                        "abc@example.com" to ("john@example.com" to "string")
                    ),
                    2, 1,
                    "Partial tokens cached with cache enabled"
                ),
                // Cache disabled scenarios
                arrayOf(
                    listOf("555-67-8901", "abc@example.com"),
                    false,
                    emptyMap<String, Pair<String, String>>(),
                    0, 2,
                    "Cache disabled - all uncached"
                ),
                // Empty input
                arrayOf(
                    emptyList<String>(),
                    true,
                    emptyMap<String, Pair<String, String>>(),
                    0, 0,
                    "Empty input list"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockTokenCache = mockk()
        mockLogger = mockk(relaxed = true)

        every { mockTokenCache.isEnabled() } returns cacheEnabled

        // Mock individual getValue calls
        inputTokens.forEach { token ->
            val cachedPair = cachedPairs[token]
            every { mockTokenCache.getValue(token) } returns cachedPair
        }

        cacheManager = CacheManager(mockTokenCache, mockLogger)
    }

    @Test
    fun shouldGetBatchValuesFromCacheCorrectly() {
        // Act
        val result = cacheManager.getBatchValuesFromCache(inputTokens)

        // Assert
        assertNotNull("Should return BatchCacheResult", result)
        assertEquals("Cached count should match", expectedCachedCount, result.cached.size)
        assertEquals("Uncached count should match", expectedUncachedCount, result.uncached.size)

        // Verify cached items are BatchDetokenItemResponse
        result.cached.forEach { item ->
            assertTrue(
                "Cached item should be BatchDetokenItemResponse",
                item is BatchDetokenItemResponse
            )
            val valueItem = item as BatchDetokenItemResponse
            assertTrue("Cached item should exist", valueItem.exists)

            // Verify the value and dataType match expectations
            val expectedPair = cachedPairs[valueItem.token]
            if (expectedPair != null) {
                assertEquals("Value should match", expectedPair.first, valueItem.value)
                assertEquals("DataType should match", expectedPair.second, valueItem.dataType)
            }
        }

        // Verify uncached items
        val expectedUncached = inputTokens.filter { !cachedPairs.containsKey(it) }
        assertEquals(
            "Uncached items should match",
            expectedUncached.sorted(),
            result.uncached.sorted()
        )

        // Verify interactions
        verify(exactly = 1) { mockTokenCache.isEnabled() }
        if (cacheEnabled && inputTokens.isNotEmpty()) {
            inputTokens.forEach { token ->
                verify(exactly = 1) { mockTokenCache.getValue(token) }
            }
        } else if (!cacheEnabled) {
            verify(exactly = 0) { mockTokenCache.getValue(any()) }
        }
    }
}

// ====================================
// Single Token Storage Tests
// ====================================
@RunWith(Parameterized::class)
class CacheManagerStoreTokenTest(
    private val value: String,
    private val token: String,
    private val dataType: String?,
    private val cacheEnabled: Boolean,
    private val description: String
) {
    private lateinit var mockTokenCache: TokenCache
    private lateinit var mockLogger: VaultLogger
    private lateinit var cacheManager: CacheManager

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle token storage: {4}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf(
                    "555-12-3456",
                    "555-67-8901",
                    "string",
                    true,
                    "Store SSN token with cache enabled"
                ),
                arrayOf(
                    "john@example.com",
                    "abc@example.com",
                    "string",
                    true,
                    "Store email token with cache enabled"
                ),
                arrayOf(
                    "123456",
                    "987654",
                    "integer",
                    true,
                    "Store integer token with cache enabled"
                ),
                arrayOf("true", "false", "boolean", true, "Store boolean token with cache enabled"),
                arrayOf(
                    "test-value",
                    "test-token",
                    null,
                    true,
                    "Store token with null dataType and cache enabled"
                ),
                arrayOf(
                    "",
                    "empty-token",
                    "string",
                    true,
                    "Store empty value token with cache enabled"
                ),
                arrayOf("🚀", "🌟", "string", true, "Store unicode token with cache enabled"),

                // Cache disabled scenarios
                arrayOf(
                    "555-12-3456",
                    "555-67-8901",
                    "string",
                    false,
                    "Store token with cache disabled"
                ),
                arrayOf(
                    "test-value",
                    "test-token",
                    null,
                    false,
                    "Store token with null dataType and cache disabled"
                ),
            )
        }
    }

    @Before
    fun setUp() {
        mockTokenCache = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)

        every { mockTokenCache.isEnabled() } returns cacheEnabled

        cacheManager = CacheManager(mockTokenCache, mockLogger)
    }

    @Test
    fun shouldStoreTokenInCacheCorrectly() {
        // Act
        cacheManager.storeTokenInCache(value, token, dataType)

        // Assert & Verify
        if (cacheEnabled) {
            verify(exactly = 1) { mockTokenCache.putToken(value, token, dataType) }
        } else {
            verify(exactly = 0) { mockTokenCache.putToken(any(), any(), any()) }
        }

        verify(exactly = 1) { mockTokenCache.isEnabled() }
    }
}

// ====================================
// Single Value Storage Tests
// ====================================
@RunWith(Parameterized::class)
class CacheManagerStoreValueTest(
    private val token: String,
    private val value: String,
    private val dataType: String?,
    private val cacheEnabled: Boolean,
    private val description: String
) {
    private lateinit var mockTokenCache: TokenCache
    private lateinit var mockLogger: VaultLogger
    private lateinit var cacheManager: CacheManager

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle value storage: {4}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf(
                    "555-67-8901",
                    "555-12-3456",
                    "string",
                    true,
                    "Store SSN value with cache enabled"
                ),
                arrayOf(
                    "abc@example.com",
                    "john@example.com",
                    "string",
                    true,
                    "Store email value with cache enabled"
                ),
                arrayOf(
                    "987654",
                    "123456",
                    "integer",
                    true,
                    "Store integer value with cache enabled"
                ),
                arrayOf("false", "true", "boolean", true, "Store boolean value with cache enabled"),
                arrayOf(
                    "test-token",
                    "test-value",
                    null,
                    true,
                    "Store value with null dataType and cache enabled"
                ),
                arrayOf("empty-token", "", "string", true, "Store empty value with cache enabled"),
                arrayOf("🌟", "🚀", "string", true, "Store unicode value with cache enabled"),

                // Cache disabled scenarios
                arrayOf(
                    "555-67-8901",
                    "555-12-3456",
                    "string",
                    false,
                    "Store value with cache disabled"
                ),
                arrayOf(
                    "test-token",
                    "test-value",
                    null,
                    false,
                    "Store value with null dataType and cache disabled"
                ),
            )
        }
    }

    @Before
    fun setUp() {
        mockTokenCache = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)

        every { mockTokenCache.isEnabled() } returns cacheEnabled

        cacheManager = CacheManager(mockTokenCache, mockLogger)
    }

    @Test
    fun shouldStoreValueInCacheCorrectly() {
        // Act
        cacheManager.storeValueInCache(token, value, dataType)

        // Assert & Verify
        if (cacheEnabled) {
            verify(exactly = 1) { mockTokenCache.putValue(token, value, dataType) }
        } else {
            verify(exactly = 0) { mockTokenCache.putValue(any(), any(), any()) }
        }

        verify(exactly = 1) { mockTokenCache.isEnabled() }
    }
}

// ====================================
// Batch Token Storage Tests
// ====================================
@RunWith(Parameterized::class)
class CacheManagerStoreBatchTokensTest(
    private val results: List<BatchTokenItemResponse>,
    private val cacheEnabled: Boolean,
    private val expectedStoreCalls: Int,
    private val description: String
) {
    private lateinit var mockTokenCache: TokenCache
    private lateinit var mockLogger: VaultLogger
    private lateinit var cacheManager: CacheManager

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle batch token storage: {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                // All items should be stored (exists=true or newlyCreated=true)
                arrayOf(
                    listOf(
                        BatchTokenItemResponse(
                            "value1",
                            "token1",
                            true,
                            false,
                            "string"
                        ),  // exists=true
                        BatchTokenItemResponse(
                            "value2",
                            "token2",
                            false,
                            true,
                            "string"
                        ),  // newlyCreated=true
                        BatchTokenItemResponse(
                            "value3",
                            "token3",
                            true,
                            true,
                            "integer"
                        )   // both true
                    ),
                    true, 3,
                    "All items should be stored with cache enabled"
                ),
                // Some items should not be stored (exists=false and newlyCreated=false)
                arrayOf(
                    listOf(
                        BatchTokenItemResponse(
                            "value1",
                            "token1",
                            true,
                            false,
                            "string"
                        ),   // exists=true - store
                        BatchTokenItemResponse(
                            "value2",
                            "token2",
                            false,
                            false,
                            "string"
                        ),  // both false - don't store
                        BatchTokenItemResponse(
                            "value3",
                            "token3",
                            false,
                            true,
                            "integer"
                        )   // newlyCreated=true - store
                    ),
                    true, 2,
                    "Some items should be stored with cache enabled"
                ),
                // No items should be stored (all have exists=false and newlyCreated=false)
                arrayOf(
                    listOf(
                        BatchTokenItemResponse("value1", "token1", false, false, "string"),
                        BatchTokenItemResponse("value2", "token2", false, false, "string")
                    ),
                    true, 0,
                    "No items should be stored with cache enabled"
                ),
                // Cache disabled - no items stored regardless of flags
                arrayOf(
                    listOf(
                        BatchTokenItemResponse("value1", "token1", true, true, "string"),
                        BatchTokenItemResponse("value2", "token2", true, false, "string")
                    ),
                    false, 0,
                    "No items stored with cache disabled"
                ),
                // Empty results
                arrayOf(
                    emptyList<BatchTokenItemResponse>(),
                    true, 0,
                    "Empty results with cache enabled"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockTokenCache = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)

        every { mockTokenCache.isEnabled() } returns cacheEnabled

        cacheManager = CacheManager(mockTokenCache, mockLogger)
    }

    @Test
    fun shouldStoreBatchTokensCorrectly() {
        // Act
        cacheManager.storeBatchTokensInCache(results)

        // Assert & Verify
        verify(exactly = 1) { mockTokenCache.isEnabled() }

        if (cacheEnabled) {
            verify(exactly = expectedStoreCalls) { mockTokenCache.putToken(any(), any(), any()) }

            // Verify only appropriate items were stored
            results.forEach { item ->
                if (item.exists || item.newlyCreated) {
                    verify(exactly = 1) {
                        mockTokenCache.putToken(item.originalValue, item.token, item.dataType)
                    }
                } else {
                    verify(exactly = 0) {
                        mockTokenCache.putToken(item.originalValue, item.token, item.dataType)
                    }
                }
            }
        } else {
            verify(exactly = 0) { mockTokenCache.putToken(any(), any(), any()) }
        }
    }
}

// ====================================
// Batch Value Storage Tests
// ====================================
@RunWith(Parameterized::class)
class CacheManagerStoreBatchValuesTest(
    private val results: List<BatchDetokenItemResponse>,
    private val cacheEnabled: Boolean,
    private val expectedStoreCalls: Int,
    private val description: String
) {
    private lateinit var mockTokenCache: TokenCache
    private lateinit var mockLogger: VaultLogger
    private lateinit var cacheManager: CacheManager

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle batch value storage: {3}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                // All items should be stored (exists=true and value!=null)
                arrayOf(
                    listOf(
                        BatchDetokenItemResponse("token1", "value1", true, "string"),
                        BatchDetokenItemResponse("token2", "value2", true, "integer"),
                        BatchDetokenItemResponse("token3", "value3", true, "string")
                    ),
                    true, 3,
                    "All items should be stored with cache enabled"
                ),
                // Some items should not be stored (exists=false or value=null)
                arrayOf(
                    listOf(
                        BatchDetokenItemResponse("token1", "value1", true, "string"),   // exists=true, value!=null - store
                        BatchDetokenItemResponse("token2", null, false, null),         // exists=false - don't store
                        BatchDetokenItemResponse("token3", null, true, "string"),      // exists=true, value=null - don't store
                        BatchDetokenItemResponse("token4", "value4", true, "integer")  // exists=true, value!=null - store
                    ),
                    true, 2,
                    "Some items should be stored with cache enabled"
                ),
                // No items should be stored (all have exists=false or value=null)
                arrayOf(
                    listOf(
                        BatchDetokenItemResponse("token1", null, false, null),
                        BatchDetokenItemResponse("token2", null, true, "string"), // exists=true but value=null
                        BatchDetokenItemResponse("token3", "value3", false, "string") // value!=null but exists=false
                    ),
                    true, 0,
                    "No items should be stored with cache enabled"
                ),
                // Cache disabled - no items stored regardless of flags
                arrayOf(
                    listOf(
                        BatchDetokenItemResponse("token1", "value1", true, "string"),
                        BatchDetokenItemResponse("token2", "value2", true, "integer")
                    ),
                    false, 0,
                    "No items stored with cache disabled"
                ),
                // Empty results
                arrayOf(
                    emptyList<BatchDetokenItemResponse>(),
                    true, 0,
                    "Empty results with cache enabled"
                )
            )
        }
    }

    @Before
    fun setUp() {
        mockTokenCache = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)

        every { mockTokenCache.isEnabled() } returns cacheEnabled

        cacheManager = CacheManager(mockTokenCache, mockLogger)
    }

    @Test
    fun shouldStoreBatchValuesCorrectly() {
        // Act
        cacheManager.storeBatchValuesInCache(results)

        // Assert & Verify
        verify(exactly = 1) { mockTokenCache.isEnabled() }

        if (cacheEnabled) {
            verify(exactly = expectedStoreCalls) { mockTokenCache.putValue(any(), any(), any()) }


            // Verify only appropriate items were stored
            results.forEach { item ->
                if (item.exists && item.value != null) {
                    verify(exactly = 1) {
                        mockTokenCache.putValue(item.token, item.value!!, item.dataType)
                    }
                } else {
                    verify(exactly = 0) {
                        mockTokenCache.putValue(item.token, item.value ?: any(), item.dataType)
                    }
                }
            }
        } else {
            verify(exactly = 0) { mockTokenCache.putValue(any(), any(), any()) }
        }
    }
}