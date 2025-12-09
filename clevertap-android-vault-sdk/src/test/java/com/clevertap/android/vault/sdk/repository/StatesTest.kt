package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.model.BatchDetokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchTokenItemResponse
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// ====================================
// CacheCheckResult Tests
// ====================================
@RunWith(Parameterized::class)
class CacheCheckResultCompleteFromCacheTest(
    private val originalRequest: Any,
    private val result: Any,
    private val description: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle CompleteFromCache: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("single-value", "single-result", "Single value request"),
                arrayOf(listOf("value1", "value2"), "batch-result", "Batch values request"),
                arrayOf("", "empty-result", "Empty string request"),
                arrayOf(listOf<String>(), "empty-batch-result", "Empty list request"),
                arrayOf("🌟🚀💯", "unicode-result", "Unicode content request"),
                arrayOf(
                    listOf("very-long-value-" + "x".repeat(1000)),
                    "large-result",
                    "Large content request"
                )
            )
        }
    }

    @Test
    fun shouldCreateCompleteFromCacheCorrectly() {
        // Act
        val cacheResult = CacheCheckResult.CompleteFromCache(
            originalRequest = originalRequest,
            result = result
        )

        // Assert
        assertEquals("Original request should match", originalRequest, cacheResult.originalRequest)
        assertEquals("Result should match", result, cacheResult.result)
    }
}

@RunWith(Parameterized::class)
class CacheCheckResultPartialFromCacheTest(
    private val originalRequest: Any,
    private val cachedItems: List<Any>,
    private val uncachedRequest: Any,
    private val description: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle PartialFromCache: {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    listOf("value1", "value2", "value3"),
                    listOf("cached-item1", "cached-item2"),
                    listOf("value3"),
                    "Partial tokenization cache"
                ),
                arrayOf(
                    listOf("token1", "token2", "token3", "token4"),
                    listOf("cached-token1"),
                    listOf("token2", "token3", "token4"),
                    "Partial detokenization cache"
                ),
                arrayOf(
                    listOf("single-item"),
                    emptyList<String>(),
                    listOf("single-item"),
                    "Empty cached items"
                ),
                arrayOf(
                    emptyList<String>(),
                    listOf("some-cached-item"),
                    emptyList<String>(),
                    "Empty original and uncached"
                )
            )
        }
    }

    @Test
    fun shouldCreatePartialFromCacheCorrectly() {
        // Act
        val cacheResult = CacheCheckResult.PartialFromCache<Any, String>(
            originalRequest = originalRequest,
            cachedItems = cachedItems,
            uncachedRequest = uncachedRequest
        )

        // Assert
        assertEquals("Original request should match", originalRequest, cacheResult.originalRequest)
        assertEquals("Cached items should match", cachedItems, cacheResult.cachedItems)
        assertEquals("Uncached request should match", uncachedRequest, cacheResult.uncachedRequest)
    }
}

@RunWith(Parameterized::class)
class CacheCheckResultNothingFromCacheTest(
    private val originalRequest: Any,
    private val uncachedRequest: Any,
    private val description: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle NothingFromCache: {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("single-value", "single-value", "Single value not cached"),
                arrayOf(
                    listOf("value1", "value2"),
                    listOf("value1", "value2"),
                    "Batch values not cached"
                ),
                arrayOf("", "", "Empty string not cached"),
                arrayOf(listOf<String>(), listOf<String>(), "Empty list not cached"),
                arrayOf(
                    "special-chars-!@#$%",
                    "special-chars-!@#$%",
                    "Special characters not cached"
                )
            )
        }
    }

    @Test
    fun shouldCreateNothingFromCacheCorrectly() {
        // Act
        val cacheResult = CacheCheckResult.NothingFromCache<Any, String>(
            originalRequest = originalRequest,
            uncachedRequest = uncachedRequest
        )

        // Assert
        assertEquals("Original request should match", originalRequest, cacheResult.originalRequest)
        assertEquals("Uncached request should match", uncachedRequest, cacheResult.uncachedRequest)
    }
}

// ====================================
// BatchTokenizeState Tests
// ====================================
@RunWith(Parameterized::class)
class BatchTokenizeStateConstructorTest(
    private val originalValues: List<String>,
    private val cachedResults: List<BatchTokenItemResponse>,
    private val uncachedValues: List<String>,
    private val expectedIsComplete: Boolean,
    private val description: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should create BatchTokenizeState: {4}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    listOf("value1", "value2", "value3"),
                    listOf(
                        BatchTokenItemResponse("value1", "token1", true, false, "string"),
                        BatchTokenItemResponse("value2", "token2", false, true, "string")
                    ),
                    listOf("value3"),
                    false,
                    "Partial cache state"
                ),
                arrayOf(
                    listOf("value1", "value2"),
                    listOf(
                        BatchTokenItemResponse("value1", "token1", true, false, "string"),
                        BatchTokenItemResponse("value2", "token2", true, false, "string")
                    ),
                    emptyList<String>(),
                    true,
                    "Complete cache state"
                ),
                arrayOf(
                    listOf("value1", "value2", "value3"),
                    emptyList<BatchTokenItemResponse>(),
                    listOf("value1", "value2", "value3"),
                    false,
                    "No cache state"
                ),
                arrayOf(
                    emptyList<String>(),
                    emptyList<BatchTokenItemResponse>(),
                    emptyList<String>(),
                    true,
                    "Empty state"
                ),
                arrayOf(
                    listOf("single-value"),
                    listOf(
                        BatchTokenItemResponse(
                            "single-value",
                            "single-token",
                            false,
                            true,
                            "string"
                        )
                    ),
                    emptyList<String>(),
                    true,
                    "Single complete item"
                )
            )
        }
    }

    @Test
    fun shouldCreateBatchTokenizeStateCorrectly() {
        // Act
        val state = BatchTokenizeState(
            originalValues = originalValues,
            cachedResults = cachedResults,
            uncachedValues = uncachedValues
        )

        // Assert
        assertEquals("Original values should match", originalValues, state.originalValues)
        assertEquals("Cached results should match", cachedResults, state.cachedResults)
        assertEquals("Uncached values should match", uncachedValues, state.uncachedValues)
        assertEquals("IsComplete should match expected", expectedIsComplete, state.isComplete)
    }
}

class BatchTokenizeStateCreateCompleteResultTest {
    @Test
    fun shouldCreateCompleteResultForFullyCachedState() {
        // Arrange
        val cachedResults = listOf(
            BatchTokenItemResponse("value1", "token1", true, false, "string"),
            BatchTokenItemResponse("value2", "token2", false, true, "integer"),
            BatchTokenItemResponse("value3", "token3", true, false, "boolean")
        )

        val state = BatchTokenizeState(
            originalValues = listOf("value1", "value2", "value3"),
            cachedResults = cachedResults,
            uncachedValues = emptyList()
        )

        // Act
        val result = state.createCompleteResult()

        // Assert
        assertEquals("Results should match cached results", cachedResults, result.results)
        assertEquals("Processed count should match", 3, result.summary.processedCount)
        assertEquals("Existing count should match", 3, result.summary.existingCount)
        assertEquals("Newly created count should be 0", 0, result.summary.newlyCreatedCount)
    }

    @Test
    fun shouldCreateCompleteResultForEmptyState() {
        // Arrange
        val state = BatchTokenizeState(
            originalValues = emptyList(),
            cachedResults = emptyList(),
            uncachedValues = emptyList()
        )

        // Act
        val result = state.createCompleteResult()

        // Assert
        assertEquals("Results should be empty", emptyList<BatchTokenItemResponse>(), result.results)
        assertEquals("Processed count should be 0", 0, result.summary.processedCount)
        assertEquals("Existing count should be 0", 0, result.summary.existingCount)
        assertEquals("Newly created count should be 0", 0, result.summary.newlyCreatedCount)
    }

    @Test
    fun shouldCreateCompleteResultWithMixedExistsValues() {
        // Arrange
        val cachedResults = listOf(
            BatchTokenItemResponse("value1", "token1", true, false, "string"), // exists
            BatchTokenItemResponse("value2", "token2", false, true, "string"), // new
            BatchTokenItemResponse("value3", "token3", true, false, "string"), // exists
            BatchTokenItemResponse("value4", "token4", false, true, "string")  // new
        )

        val state = BatchTokenizeState(
            originalValues = listOf("value1", "value2", "value3", "value4"),
            cachedResults = cachedResults,
            uncachedValues = emptyList()
        )

        // Act
        val result = state.createCompleteResult()

        // Assert
        assertEquals("Results should match cached results", cachedResults, result.results)
        assertEquals("Processed count should be 4", 4, result.summary.processedCount)
        assertEquals("Existing count should be 4 (all from cache)", 4, result.summary.existingCount)
        assertEquals(
            "Newly created count should be 0 (from cache)",
            0,
            result.summary.newlyCreatedCount
        )
    }
}

// ====================================
// BatchDetokenizeState Tests
// ====================================
@RunWith(Parameterized::class)
class BatchDetokenizeStateConstructorTest(
    private val originalTokens: List<String>,
    private val cachedResults: List<BatchDetokenItemResponse>,
    private val uncachedTokens: List<String>,
    private val expectedIsComplete: Boolean,
    private val description: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should create BatchDetokenizeState: {4}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    listOf("token1", "token2", "token3"),
                    listOf(
                        BatchDetokenItemResponse("token1", "value1", true, "string"),
                        BatchDetokenItemResponse("token2", "value2", true, "integer")
                    ),
                    listOf("token3"),
                    false,
                    "Partial cache state"
                ),
                arrayOf(
                    listOf("token1", "token2"),
                    listOf(
                        BatchDetokenItemResponse("token1", "value1", true, "string"),
                        BatchDetokenItemResponse("token2", "value2", true, "string")
                    ),
                    emptyList<String>(),
                    true,
                    "Complete cache state"
                ),
                arrayOf(
                    listOf("token1", "token2", "token3"),
                    emptyList<BatchDetokenItemResponse>(),
                    listOf("token1", "token2", "token3"),
                    false,
                    "No cache state"
                ),
                arrayOf(
                    emptyList<String>(),
                    emptyList<BatchDetokenItemResponse>(),
                    emptyList<String>(),
                    true,
                    "Empty state"
                ),
                arrayOf(
                    listOf("single-token"),
                    listOf(
                        BatchDetokenItemResponse(
                            "single-token",
                            "single-value",
                            true,
                            "string"
                        )
                    ),
                    emptyList<String>(),
                    true,
                    "Single complete item"
                ),
                arrayOf(
                    listOf("token1", "token2"),
                    listOf(
                        BatchDetokenItemResponse("token1", "value1", true, "string"),
                        BatchDetokenItemResponse("token2", null, false, null) // token not found
                    ),
                    emptyList<String>(),
                    true,
                    "Complete state with not found token"
                )
            )
        }
    }

    @Test
    fun shouldCreateBatchDetokenizeStateCorrectly() {
        // Act
        val state = BatchDetokenizeState(
            originalTokens = originalTokens,
            cachedResults = cachedResults,
            uncachedTokens = uncachedTokens
        )

        // Assert
        assertEquals("Original tokens should match", originalTokens, state.originalTokens)
        assertEquals("Cached results should match", cachedResults, state.cachedResults)
        assertEquals("Uncached tokens should match", uncachedTokens, state.uncachedTokens)
        assertEquals("IsComplete should match expected", expectedIsComplete, state.isComplete)
    }
}

class BatchDetokenizeStateCreateCompleteResultTest {
    @Test
    fun shouldCreateCompleteResultForFullyCachedState() {
        // Arrange
        val cachedResults = listOf(
            BatchDetokenItemResponse("token1", "value1", true, "string"),
            BatchDetokenItemResponse("token2", "value2", true, "integer"),
            BatchDetokenItemResponse("token3", "value3", true, "boolean")
        )

        val state = BatchDetokenizeState(
            originalTokens = listOf("token1", "token2", "token3"),
            cachedResults = cachedResults,
            uncachedTokens = emptyList()
        )

        // Act
        val result = state.createCompleteResult()

        // Assert
        assertEquals("Results should match cached results", cachedResults, result.results)
        assertEquals("Processed count should match", 3, result.summary.processedCount)
        assertEquals("Found count should match", 3, result.summary.foundCount)
        assertEquals("Not found count should be 0", 0, result.summary.notFoundCount)
    }

    @Test
    fun shouldCreateCompleteResultForEmptyState() {
        // Arrange
        val state = BatchDetokenizeState(
            originalTokens = emptyList(),
            cachedResults = emptyList(),
            uncachedTokens = emptyList()
        )

        // Act
        val result = state.createCompleteResult()

        // Assert
        assertEquals(
            "Results should be empty",
            emptyList<BatchDetokenItemResponse>(),
            result.results
        )
        assertEquals("Processed count should be 0", 0, result.summary.processedCount)
        assertEquals("Found count should be 0", 0, result.summary.foundCount)
        assertEquals("Not found count should be 0", 0, result.summary.notFoundCount)
    }

    @Test
    fun shouldCreateCompleteResultWithMixedFoundValues() {
        // Arrange
        val cachedResults = listOf(
            BatchDetokenItemResponse("token1", "value1", true, "string"), // found
            BatchDetokenItemResponse("token2", null, false, null),        // not found
            BatchDetokenItemResponse("token3", "value3", true, "integer"), // found
            BatchDetokenItemResponse("token4", null, false, null)         // not found
        )

        val state = BatchDetokenizeState(
            originalTokens = listOf("token1", "token2", "token3", "token4"),
            cachedResults = cachedResults,
            uncachedTokens = emptyList()
        )

        // Act
        val result = state.createCompleteResult()

        // Assert
        assertEquals("Results should match cached results", cachedResults, result.results)
        assertEquals("Processed count should be 4", 4, result.summary.processedCount)
        assertEquals("Found count should be 4 (all from cache)", 4, result.summary.foundCount)
        assertEquals("Not found count should be 0 (from cache)", 0, result.summary.notFoundCount)
    }

    @Test
    fun shouldCreateCompleteResultWithNullValues() {
        // Arrange
        val cachedResults = listOf(
            BatchDetokenItemResponse("token1", null, true, "string"),  // exists but null value
            BatchDetokenItemResponse("token2", "value2", true, null),  // exists with null dataType
            BatchDetokenItemResponse("token3", null, false, null)      // not found
        )

        val state = BatchDetokenizeState(
            originalTokens = listOf("token1", "token2", "token3"),
            cachedResults = cachedResults,
            uncachedTokens = emptyList()
        )

        // Act
        val result = state.createCompleteResult()

        // Assert
        assertEquals("Results should match cached results", cachedResults, result.results)
        assertEquals("Processed count should be 3", 3, result.summary.processedCount)
        assertEquals("Found count should be 3 (all from cache)", 3, result.summary.foundCount)
        assertEquals("Not found count should be 0 (from cache)", 0, result.summary.notFoundCount)
    }
}
