package com.clevertap.android.zeropii.sdk.util

import com.clevertap.android.zeropii.sdk.model.BatchTokenItemResponse
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeResult
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeSummary
import com.clevertap.android.zeropii.sdk.model.TokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.TokenizeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// ====================================
// Single Value Conversion Tests
// ====================================

class TokenizeRepoResultConversionTest {

    @Test
    fun shouldConvertSuccessfulTokenizeRepoResultToPublicResult() {
        // Arrange
        val repoResult = TokenizeRepoResult.Success(
            token = "token123",
            exists = true,
            newlyCreated = false,
            dataType = "string"
        )

        // Act
        val publicResult = repoResult.toPublicResult()

        // Assert
        assertTrue("Should be Success", publicResult is TokenizeResult.Success)
        val success = publicResult as TokenizeResult.Success
        assertEquals("token123", success.token)
        assertTrue(success.exists)
        assertFalse(success.newlyCreated)
        assertEquals("string", success.dataType)
    }

    @Test
    fun shouldConvertErrorTokenizeRepoResultToPublicResult() {
        // Arrange
        val repoResult = TokenizeRepoResult.Error("Tokenization failed")

        // Act
        val publicResult = repoResult.toPublicResult()

        // Assert
        assertTrue("Should be Error", publicResult is TokenizeResult.Error)
        val error = publicResult as TokenizeResult.Error
        assertEquals("Tokenization failed", error.message)
    }

    @Test
    fun shouldHandleNullDataTypeInTokenizeRepoResult() {
        // Arrange
        val repoResult = TokenizeRepoResult.Success(
            token = "token123",
            exists = false,
            newlyCreated = true,
            dataType = null
        )

        // Act
        val publicResult = repoResult.toPublicResult()

        // Assert
        assertTrue("Should be Success", publicResult is TokenizeResult.Success)
        val success = publicResult as TokenizeResult.Success
        assertNull(success.dataType)
    }
}

// ====================================
// Batch Tokenize Conversion Tests
// ====================================

class BatchTokenizeRepoResultConversionTest {

    @Test
    fun shouldConvertSuccessfulBatchTokenizeRepoResultToPublicResult() {
        // Arrange
        val repoItems = listOf(
            BatchTokenItemResponse(
                originalValue = "value1",
                token = "token1",
                exists = false,
                newlyCreated = true,
                dataType = "string"
            ),
            BatchTokenItemResponse(
                originalValue = "value2",
                token = "token2",
                exists = true,
                newlyCreated = false,
                dataType = "string"
            )
        )
        val summary = BatchTokenizeSummary(
            processedCount = 2,
            existingCount = 1,
            newlyCreatedCount = 1
        )
        val repoResult = BatchTokenizeRepoResult.Success(repoItems, summary)

        // Act
        val publicResult = repoResult.toPublicResult()

        // Assert
        assertTrue("Should be Success", publicResult is BatchTokenizeResult.Success)
        val success = publicResult as BatchTokenizeResult.Success
        assertEquals(2, success.results.size)

        val firstItem = success.results[0]
        assertEquals("value1", firstItem.originalValue)
        assertEquals("token1", firstItem.token)
        assertFalse(firstItem.exists)
        assertTrue(firstItem.newlyCreated)
        assertEquals("string", firstItem.dataType)

        val secondItem = success.results[1]
        assertEquals("value2", secondItem.originalValue)
        assertEquals("token2", secondItem.token)
        assertTrue(secondItem.exists)
        assertFalse(secondItem.newlyCreated)
        assertEquals("string", secondItem.dataType)

        assertEquals(summary, success.summary)
    }

    @Test
    fun shouldConvertErrorBatchTokenizeRepoResultToPublicResult() {
        // Arrange
        val repoResult = BatchTokenizeRepoResult.Error("Batch tokenization failed")

        // Act
        val publicResult = repoResult.toPublicResult()

        // Assert
        assertTrue("Should be Error", publicResult is BatchTokenizeResult.Error)
        val error = publicResult as BatchTokenizeResult.Error
        assertEquals("Batch tokenization failed", error.message)
    }

    @Test
    fun shouldHandleEmptyBatchTokenizeRepoResult() {
        // Arrange
        val summary = BatchTokenizeSummary(
            processedCount = 0,
            existingCount = 0,
            newlyCreatedCount = 0
        )
        val repoResult = BatchTokenizeRepoResult.Success(emptyList(), summary)

        // Act
        val publicResult = repoResult.toPublicResult()

        // Assert
        assertTrue("Should be Success", publicResult is BatchTokenizeResult.Success)
        val success = publicResult as BatchTokenizeResult.Success
        assertTrue(success.results.isEmpty())
        assertEquals(0, success.summary.processedCount)
    }
}
