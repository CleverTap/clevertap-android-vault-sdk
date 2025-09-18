package com.clevertap.android.vault.sdk.util

import BooleanConverter
import DoubleConverter
import FloatConverter
import IntConverter
import LongConverter
import StringConverter
import TypeConverter
import com.clevertap.android.vault.sdk.model.BatchDetokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeSummary
import com.clevertap.android.vault.sdk.model.BatchTokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeSummary
import com.clevertap.android.vault.sdk.model.DetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.DetokenizeResult
import com.clevertap.android.vault.sdk.model.TokenizeRepoResult
import com.clevertap.android.vault.sdk.model.TokenizeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

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
// DetokenizeRepoResult Tests with All Converters
// ====================================

@RunWith(Parameterized::class)
class DetokenizeRepoResultStringConversionTest(
    private val repoValue: String?,
    private val exists: Boolean,
    private val dataType: String?,
    private val expectedValue: String?,
    private val description: String
) {
    private lateinit var converter: StringConverter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle: {4}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf("test", true, "string", "test", "valid string value"),
                arrayOf("", true, "string", "", "empty string value"),
                arrayOf(null, false, null, null, "null value"),
                arrayOf("value123", false, "string", "value123", "non-existing value"),
                arrayOf("unicode 🚀", true, "string", "unicode 🚀", "unicode characters"),
                arrayOf("special!@#$", true, "string", "special!@#$", "special characters")
            )
        }
    }

    @Before
    fun setUp() {
        converter = StringConverter()
    }

    @Test
    fun shouldConvertSuccessfulDetokenizeRepoResultWithStringConverter() {
        // Arrange
        val repoResult = DetokenizeRepoResult.Success(
            value = repoValue,
            exists = exists,
            dataType = dataType
        )

        // Act
        val publicResult = repoResult.toPublicResult(converter)

        // Assert
        assertTrue("Should be Success", publicResult is DetokenizeResult.Success)
        val success = publicResult as DetokenizeResult.Success<String>
        assertEquals(expectedValue, success.value)
        assertEquals(exists, success.exists)
        assertEquals(dataType, success.dataType)
    }
}

@RunWith(Parameterized::class)
class DetokenizeRepoResultIntConversionTest(
    private val repoValue: String?,
    private val expectedValue: Int?,
    private val shouldSucceed: Boolean,
    private val description: String
) {
    private lateinit var converter: IntConverter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle: {3}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf("123", 123, true, "valid positive integer"),
                arrayOf("-456", -456, true, "valid negative integer"),
                arrayOf("0", 0, true, "zero value"),
                arrayOf(Int.MAX_VALUE.toString(), Int.MAX_VALUE, true, "maximum integer value"),
                arrayOf(Int.MIN_VALUE.toString(), Int.MIN_VALUE, true, "minimum integer value"),
                arrayOf(null, null, true, "null value"),
                arrayOf("invalid", null, true, "invalid integer string"),
                arrayOf("123.45", null, true, "decimal number"),
                arrayOf("", null, true, "empty string")
            )
        }
    }

    @Before
    fun setUp() {
        converter = IntConverter()
    }

    @Test
    fun shouldHandleIntConverterWithVariousValues() {
        // Arrange
        val repoResult = DetokenizeRepoResult.Success(
            value = repoValue,
            exists = repoValue != null,
            dataType = "integer"
        )

        // Act
        val publicResult = repoResult.toPublicResult(converter)

        // Assert
        if (shouldSucceed) {
            assertTrue(
                "Should be Success for $description",
                publicResult is DetokenizeResult.Success
            )
            val success = publicResult as DetokenizeResult.Success<Int>
            assertEquals("Value should match for $description", expectedValue, success.value)
        } else {
            assertTrue("Should be Error for $description", publicResult is DetokenizeResult.Error)
        }
    }
}

@RunWith(Parameterized::class)
class DetokenizeRepoResultLongConversionTest(
    private val repoValue: String?,
    private val expectedValue: Long?,
    private val shouldSucceed: Boolean,
    private val description: String
) {
    private lateinit var converter: LongConverter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle: {3}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf("123456789", 123456789L, true, "valid positive long"),
                arrayOf("-987654321", -987654321L, true, "valid negative long"),
                arrayOf("0", 0L, true, "zero value"),
                arrayOf(Long.MAX_VALUE.toString(), Long.MAX_VALUE, true, "maximum long value"),
                arrayOf(Long.MIN_VALUE.toString(), Long.MIN_VALUE, true, "minimum long value"),
                arrayOf(null, null, true, "null value"),
                arrayOf("invalid", null, true, "invalid long string"),
                arrayOf("123.45", null, true, "decimal number"),
                arrayOf("", null, true, "empty string")
            )
        }
    }

    @Before
    fun setUp() {
        converter = LongConverter()
    }

    @Test
    fun shouldHandleLongConverterWithVariousValues() {
        // Arrange
        val repoResult = DetokenizeRepoResult.Success(
            value = repoValue,
            exists = repoValue != null,
            dataType = "long"
        )

        // Act
        val publicResult = repoResult.toPublicResult(converter)

        // Assert
        if (shouldSucceed) {
            assertTrue(
                "Should be Success for $description",
                publicResult is DetokenizeResult.Success
            )
            val success = publicResult as DetokenizeResult.Success<Long>
            assertEquals("Value should match for $description", expectedValue, success.value)
        } else {
            assertTrue("Should be Error for $description", publicResult is DetokenizeResult.Error)
        }
    }
}

@RunWith(Parameterized::class)
class DetokenizeRepoResultFloatConversionTest(
    private val repoValue: String?,
    private val expectedValue: Float?,
    private val shouldSucceed: Boolean,
    private val description: String
) {
    private lateinit var converter: FloatConverter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle: {3}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf("123.45", 123.45f, true, "valid positive float"),
                arrayOf("-456.789", -456.789f, true, "valid negative float"),
                arrayOf("0.0", 0.0f, true, "zero value"),
                arrayOf("123", 123.0f, true, "integer as float"),
                arrayOf(Float.MAX_VALUE.toString(), Float.MAX_VALUE, true, "maximum float value"),
                arrayOf(Float.MIN_VALUE.toString(), Float.MIN_VALUE, true, "minimum float value"),
                arrayOf("Infinity", Float.POSITIVE_INFINITY, true, "positive infinity"),
                arrayOf("-Infinity", Float.NEGATIVE_INFINITY, true, "negative infinity"),
                arrayOf("NaN", Float.NaN, true, "NaN value"),
                arrayOf(null, null, true, "null value"),
                arrayOf("invalid", null, true, "invalid float string"),
                arrayOf("", null, true, "empty string")
            )
        }
    }

    @Before
    fun setUp() {
        converter = FloatConverter()
    }

    @Test
    fun shouldHandleFloatConverterWithVariousValues() {
        // Arrange
        val repoResult = DetokenizeRepoResult.Success(
            value = repoValue,
            exists = repoValue != null,
            dataType = "float"
        )

        // Act
        val publicResult = repoResult.toPublicResult(converter)

        // Assert
        if (shouldSucceed) {
            assertTrue(
                "Should be Success for $description",
                publicResult is DetokenizeResult.Success
            )
            val success = publicResult as DetokenizeResult.Success<Float>
            if (expectedValue != null && expectedValue.isNaN()) {
                assertTrue("Value should be NaN for $description", success.value?.isNaN() == true)
            } else {
                assertEquals("Value should match for $description", expectedValue, success.value)
            }
        } else {
            assertTrue("Should be Error for $description", publicResult is DetokenizeResult.Error)
        }
    }
}

@RunWith(Parameterized::class)
class DetokenizeRepoResultDoubleConversionTest(
    private val repoValue: String?,
    private val expectedValue: Double?,
    private val shouldSucceed: Boolean,
    private val description: String
) {
    private lateinit var converter: DoubleConverter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle: {3}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf("123.456789", 123.456789, true, "valid positive double"),
                arrayOf("-987.654321", -987.654321, true, "valid negative double"),
                arrayOf("0.0", 0.0, true, "zero value"),
                arrayOf("123", 123.0, true, "integer as double"),
                arrayOf(
                    Double.MAX_VALUE.toString(),
                    Double.MAX_VALUE,
                    true,
                    "maximum double value"
                ),
                arrayOf(
                    Double.MIN_VALUE.toString(),
                    Double.MIN_VALUE,
                    true,
                    "minimum double value"
                ),
                arrayOf("Infinity", Double.POSITIVE_INFINITY, true, "positive infinity"),
                arrayOf("-Infinity", Double.NEGATIVE_INFINITY, true, "negative infinity"),
                arrayOf("NaN", Double.NaN, true, "NaN value"),
                arrayOf(null, null, true, "null value"),
                arrayOf("invalid", null, true, "invalid double string"),
                arrayOf("", null, true, "empty string")
            )
        }
    }

    @Before
    fun setUp() {
        converter = DoubleConverter()
    }

    @Test
    fun shouldHandleDoubleConverterWithVariousValues() {
        // Arrange
        val repoResult = DetokenizeRepoResult.Success(
            value = repoValue,
            exists = repoValue != null,
            dataType = "double"
        )

        // Act
        val publicResult = repoResult.toPublicResult(converter)

        // Assert
        if (shouldSucceed) {
            assertTrue(
                "Should be Success for $description",
                publicResult is DetokenizeResult.Success
            )
            val success = publicResult as DetokenizeResult.Success<Double>
            if (expectedValue != null && expectedValue.isNaN()) {
                assertTrue("Value should be NaN for $description", success.value?.isNaN() == true)
            } else {
                assertEquals("Value should match for $description", expectedValue, success.value)
            }
        } else {
            assertTrue("Should be Error for $description", publicResult is DetokenizeResult.Error)
        }
    }
}

@RunWith(Parameterized::class)
class DetokenizeRepoResultBooleanConversionTest(
    private val repoValue: String?,
    private val expectedValue: Boolean?,
    private val shouldSucceed: Boolean,
    private val description: String
) {
    private lateinit var converter: BooleanConverter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle: {3}")
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf("true", true, true, "lowercase true"),
                arrayOf("TRUE", true, true, "uppercase true"),
                arrayOf("True", true, true, "mixed case true"),
                arrayOf("false", false, true, "lowercase false"),
                arrayOf("FALSE", false, true, "uppercase false"),
                arrayOf("False", false, true, "mixed case false"),
                arrayOf(null, null, true, "null value"),
                arrayOf("yes", null, false, "yes string"),
                arrayOf("no", null, false, "no string"),
                arrayOf("1", null, false, "numeric 1"),
                arrayOf("0", null, false, "numeric 0"),
                arrayOf("maybe", null, false, "invalid boolean string"),
                arrayOf("", null, false, "empty string")
            )
        }
    }

    @Before
    fun setUp() {
        converter = BooleanConverter()
    }

    @Test
    fun shouldHandleBooleanConverterWithVariousValues() {
        // Arrange
        val repoResult = DetokenizeRepoResult.Success(
            value = repoValue,
            exists = repoValue != null,
            dataType = "boolean"
        )

        // Act
        val publicResult = repoResult.toPublicResult(converter)

        // Assert
        if (shouldSucceed) {
            assertTrue(
                "Should be Success for $description",
                publicResult is DetokenizeResult.Success
            )
            val success = publicResult as DetokenizeResult.Success<Boolean>
            assertEquals("Value should match for $description", expectedValue, success.value)
        } else {
            assertTrue("Should be Error for $description", publicResult is DetokenizeResult.Error)
        }
    }
}

class DetokenizeRepoResultErrorHandlingTest {

    @Test
    fun shouldConvertErrorDetokenizeRepoResultToPublicResult() {
        // Arrange
        val repoResult = DetokenizeRepoResult.Error("Detokenization failed")
        val converter = StringConverter()

        // Act
        val publicResult = repoResult.toPublicResult(converter)

        // Assert
        assertTrue("Should be Error", publicResult is DetokenizeResult.Error)
        val error = publicResult as DetokenizeResult.Error<String>
        assertEquals("Detokenization failed", error.message)
    }

}

// ====================================
// Batch Conversion Tests with All Converters
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

@RunWith(Parameterized::class)
class BatchDetokenizeRepoResultWithAllConvertersTest(
    private val converterType: String,
    private val converter: TypeConverter<*>,
    private val testValues: List<String?>,
    private val expectedResults: List<Any?>,
    private val description: String
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should handle batch conversion with: {4}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    "String",
                    StringConverter(),
                    listOf("value1", "value2", null, ""),
                    listOf("value1", "value2", null, ""),
                    "String converter"
                ),
                arrayOf(
                    "Int",
                    IntConverter(),
                    listOf("123", "-456", null, "0"),
                    listOf(123, -456, null, 0),
                    "Int converter"
                ),
                arrayOf(
                    "Long",
                    LongConverter(),
                    listOf("123456789", "-987654321", null, "0"),
                    listOf(123456789L, -987654321L, null, 0L),
                    "Long converter"
                ),
                arrayOf(
                    "Float",
                    FloatConverter(),
                    listOf("123.45", "-456.78", null, "0.0"),
                    listOf(123.45f, -456.78f, null, 0.0f),
                    "Float converter"
                ),
                arrayOf(
                    "Double",
                    DoubleConverter(),
                    listOf("123.456789", "-987.654321", null, "0.0"),
                    listOf(123.456789, -987.654321, null, 0.0),
                    "Double converter"
                ),
                arrayOf(
                    "Boolean",
                    BooleanConverter(),
                    listOf("true", "false", null, "TRUE"),
                    listOf(true, false, null, true),
                    "Boolean converter"
                )
            )
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun shouldConvertBatchDetokenizeRepoResultWithSpecificConverter() {
        // Arrange
        val repoItems = testValues.mapIndexed { index, value ->
            BatchDetokenItemResponse(
                token = "token$index",
                value = value,
                exists = value != null,
                dataType = converterType.lowercase()
            )
        }
        val summary = BatchDetokenizeSummary(
            processedCount = testValues.size,
            foundCount = testValues.count { it != null },
            notFoundCount = testValues.count { it == null }
        )
        val repoResult = BatchDetokenizeRepoResult.Success(repoItems, summary)

        // Act
        val publicResult = repoResult.toPublicResult(converter as TypeConverter<Any>)

        // Assert
        assertTrue(
            "Should be Success for $description",
            publicResult is BatchDetokenizeResult.Success
        )
        val success = publicResult as BatchDetokenizeResult.Success<Any>
        assertEquals(
            "Results count should match for $description",
            testValues.size,
            success.results.size
        )

        expectedResults.forEachIndexed { index, expectedValue ->
            val actualValue = success.results[index].value
            if (expectedValue is Float && expectedValue.isNaN()) {
                assertTrue(
                    "Should be NaN for $description at index $index",
                    (actualValue as Float).isNaN()
                )
            } else if (expectedValue is Double && expectedValue.isNaN()) {
                assertTrue(
                    "Should be NaN for $description at index $index",
                    (actualValue as Double).isNaN()
                )
            } else {
                assertEquals(
                    "Value should match for $description at index $index",
                    expectedValue,
                    actualValue
                )
            }
        }
        assertEquals("Summary should match for $description", summary, success.summary)
    }
}

class BatchDetokenizeRepoResultErrorHandlingTest {

    @Test
    fun shouldConvertErrorBatchDetokenizeRepoResultToPublicResult() {
        // Arrange
        val repoResult = BatchDetokenizeRepoResult.Error("Batch detokenization failed")
        val converter = StringConverter()

        // Act
        val publicResult = repoResult.toPublicResult(converter)

        // Assert
        assertTrue("Should be Error", publicResult is BatchDetokenizeResult.Error)
        val error = publicResult as BatchDetokenizeResult.Error<String>
        assertEquals("Batch detokenization failed", error.message)
    }

    @Test
    fun shouldHandleEmptyBatchDetokenizeResult() {
        // Arrange
        val summary = BatchDetokenizeSummary(
            processedCount = 0,
            foundCount = 0,
            notFoundCount = 0
        )
        val repoResult = BatchDetokenizeRepoResult.Success(emptyList(), summary)
        val converter = StringConverter()

        // Act
        val publicResult = repoResult.toPublicResult(converter)

        // Assert
        assertTrue("Should be Success", publicResult is BatchDetokenizeResult.Success)
        val success = publicResult as BatchDetokenizeResult.Success<String>
        assertTrue(success.results.isEmpty())
        assertEquals(0, success.summary.processedCount)
    }
}
