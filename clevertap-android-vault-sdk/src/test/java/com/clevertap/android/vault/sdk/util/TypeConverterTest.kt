package com.clevertap.android.vault.sdk.util


import BooleanConverter
import DoubleConverter
import FloatConverter
import IntConverter
import LongConverter
import StringConverter
import TypeConverter
import TypeConverterRegistry
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
// TypeConverterRegistry Tests
// ====================================
class TypeConverterRegistryTest {

    @Before
    fun setUp() {
        // Registry is initialized statically, so it should have default converters
    }

    @Test
    fun shouldHaveAllBuiltInConvertersRegistered() {
        // Act & Assert
        assertNotNull(TypeConverterRegistry.getConverter(String::class.java))
        assertNotNull(TypeConverterRegistry.getConverter(Int::class.java))
        assertNotNull(TypeConverterRegistry.getConverter(Long::class.java))
        assertNotNull(TypeConverterRegistry.getConverter(Float::class.java))
        assertNotNull(TypeConverterRegistry.getConverter(Double::class.java))
        assertNotNull(TypeConverterRegistry.getConverter(Boolean::class.java))
    }

    @Test
    fun shouldReturnCorrectConverterInstances() {
        // Act & Assert
        assertTrue(TypeConverterRegistry.getConverter(String::class.java) is StringConverter)
        assertTrue(TypeConverterRegistry.getConverter(Int::class.java) is IntConverter)
        assertTrue(TypeConverterRegistry.getConverter(Long::class.java) is LongConverter)
        assertTrue(TypeConverterRegistry.getConverter(Float::class.java) is FloatConverter)
        assertTrue(TypeConverterRegistry.getConverter(Double::class.java) is DoubleConverter)
        assertTrue(TypeConverterRegistry.getConverter(Boolean::class.java) is BooleanConverter)
    }

    @Test
    fun shouldReturnNullForUnregisteredTypes() {
        // Act
        val converter = TypeConverterRegistry.getConverter(List::class.java)

        // Assert
        assertNull(converter)
    }

    @Test
    fun shouldAllowRegisteringCustomConverters() {
        // Arrange
        val customConverter = object : TypeConverter<List<String>> {
            override fun toString(value: List<String>): String = value.joinToString(",")
            override fun fromString(value: String): List<String> = value.split(",")
            override fun fromStringNullable(value: String?): List<String>? = value?.split(",")
            override val typeName: String = "list"
        }

        // Act
        TypeConverterRegistry.register(List::class.java as Class<List<String>>, customConverter)
        val retrievedConverter =
            TypeConverterRegistry.getConverter(List::class.java as Class<List<String>>)

        // Assert
        assertNotNull(retrievedConverter)
        assertEquals(customConverter, retrievedConverter)
    }
}

// ====================================
// StringConverter Tests
// ====================================
class StringConverterTest {

    private lateinit var converter: StringConverter

    @Before
    fun setUp() {
        converter = StringConverter()
    }

    @Test
    fun shouldHaveCorrectTypeName() {
        assertEquals("string", converter.typeName)
    }

    @Test
    fun shouldConvertStringToString() {
        val testValues = listOf("", "test", "123", "special!@#$%", "unicode 🚀", "multi\nline")

        testValues.forEach { input ->
            val result = converter.toString(input)
            assertEquals(input, result)
        }
    }

    @Test
    fun shouldConvertStringFromString() {
        val testValues = listOf("", "test", "123", "special!@#$%", "unicode 🚀")

        testValues.forEach { input ->
            val result = converter.fromString(input)
            assertEquals(input, result)
        }
    }

    @Test
    fun shouldHandleNullStringConversion() {
        val result = converter.fromStringNullable(null)
        assertNull(result)
    }

    @Test
    fun shouldHandleNullableStringConversion() {
        val testValues = listOf("", "test", "null", null)

        testValues.forEach { input ->
            val result = converter.fromStringNullable(input)
            assertEquals(input, result)
        }
    }
}

// ====================================
// IntConverter Tests
// ====================================
@RunWith(Parameterized::class)
class IntConverterValidConversionTest(
    private val intValue: Int,
    private val stringValue: String
) {
    private lateinit var converter: IntConverter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Int {0} <-> String ''{1}''")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(0, "0"),
                arrayOf(123, "123"),
                arrayOf(-456, "-456"),
                arrayOf(Int.MAX_VALUE, Int.MAX_VALUE.toString()),
                arrayOf(Int.MIN_VALUE, Int.MIN_VALUE.toString())
            )
        }
    }

    @Before
    fun setUp() {
        converter = IntConverter()
    }

    @Test
    fun shouldConvertIntToString() {
        val result = converter.toString(intValue)
        assertEquals(stringValue, result)
    }

    @Test
    fun shouldConvertStringToInt() {
        val result = converter.fromString(stringValue)
        assertEquals(intValue, result)
    }
}

class IntConverterInvalidConversionTest {
    private lateinit var converter: IntConverter

    @Before
    fun setUp() {
        converter = IntConverter()
    }

    @Test
    fun shouldHaveCorrectTypeName() {
        assertEquals("integer", converter.typeName)
    }

    @Test
    fun shouldReturnNullForNullString() {
        val result = converter.fromStringNullable(null)
        assertNull(result)
    }

    @Test
    fun shouldHandleValidNullableIntConversion() {
        val testValues = listOf("123", "-456", "0")

        testValues.forEach { input ->
            val result = converter.fromStringNullable(input)
            assertNotNull(result)
            assertEquals(input.toInt(), result)
        }
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForEmptyString() {
        converter.fromString("")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForNonNumericString() {
        converter.fromString("abc")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForFloatString() {
        converter.fromString("123.45")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForLongSuffix() {
        converter.fromString("123L")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForIntOverflow() {
        converter.fromString("2147483648") // Int.MAX_VALUE + 1
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForIntUnderflow() {
        converter.fromString("-2147483649") // Int.MIN_VALUE - 1
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForMixedString() {
        converter.fromString("123abc")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForStringWithSpaces() {
        converter.fromString("  123  ")
    }
}

// ====================================
// LongConverter Tests
// ====================================
@RunWith(Parameterized::class)
class LongConverterValidConversionTest(
    private val longValue: Long,
    private val stringValue: String
) {
    private lateinit var converter: LongConverter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Long {0} <-> String ''{1}''")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(0L, "0"),
                arrayOf(123L, "123"),
                arrayOf(-456L, "-456"),
                arrayOf(9876543210L, "9876543210"),
                arrayOf(Long.MAX_VALUE, Long.MAX_VALUE.toString()),
                arrayOf(Long.MIN_VALUE, Long.MIN_VALUE.toString())
            )
        }
    }

    @Before
    fun setUp() {
        converter = LongConverter()
    }

    @Test
    fun shouldConvertLongToString() {
        val result = converter.toString(longValue)
        assertEquals(stringValue, result)
    }

    @Test
    fun shouldConvertStringToLong() {
        val result = converter.fromString(stringValue)
        assertEquals(longValue, result)
    }
}

class LongConverterInvalidConversionTest {
    private lateinit var converter: LongConverter

    @Before
    fun setUp() {
        converter = LongConverter()
    }

    @Test
    fun shouldHaveCorrectTypeName() {
        assertEquals("long", converter.typeName)
    }

    @Test
    fun shouldReturnNullForNullString() {
        val result = converter.fromStringNullable(null)
        assertNull(result)
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForEmptyString() {
        converter.fromString("")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForNonNumericString() {
        converter.fromString("abc")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForFloatString() {
        converter.fromString("123.45")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForLongOverflow() {
        converter.fromString("9223372036854775808") // Long.MAX_VALUE + 1
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForLongUnderflow() {
        converter.fromString("-9223372036854775809") // Long.MIN_VALUE - 1
    }
}

// ====================================
// FloatConverter Tests
// ====================================
@RunWith(Parameterized::class)
class FloatConverterValidConversionTest(
    private val floatValue: Float,
    private val stringValue: String
) {
    private lateinit var converter: FloatConverter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Float {0} <-> String ''{1}''")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(0.0f, "0.0"),
                arrayOf(123.45f, "123.45"),
                arrayOf(-456.789f, "-456.789"),
                arrayOf(Float.MAX_VALUE, Float.MAX_VALUE.toString()),
                arrayOf(Float.MIN_VALUE, Float.MIN_VALUE.toString()),
                arrayOf(Float.POSITIVE_INFINITY, "Infinity"),
                arrayOf(Float.NEGATIVE_INFINITY, "-Infinity")
            )
        }
    }

    @Before
    fun setUp() {
        converter = FloatConverter()
    }

    @Test
    fun shouldConvertFloatToString() {
        val result = converter.toString(floatValue)
        assertEquals(stringValue, result)
    }
}

class FloatConverterTest {
    private lateinit var converter: FloatConverter

    @Before
    fun setUp() {
        converter = FloatConverter()
    }

    @Test
    fun shouldHaveCorrectTypeName() {
        assertEquals("float", converter.typeName)
    }

    @Test
    fun shouldConvertValidFloatStrings() {
        assertEquals(123.45f, converter.fromString("123.45"))
        assertEquals(0.0f, converter.fromString("0.0"))
        assertEquals(-456.789f, converter.fromString("-456.789"))
    }

    @Test
    fun shouldHandleNaNConversion() {
        val result = converter.fromString("NaN")
        assertTrue(result.isNaN())
    }

    @Test
    fun shouldReturnNullForNullString() {
        val result = converter.fromStringNullable(null)
        assertNull(result)
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForEmptyString() {
        converter.fromString("")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForNonNumericString() {
        converter.fromString("abc")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForMixedString() {
        converter.fromString("123abc")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForStringWithSpaces() {
        converter.fromString("  123. 45  ")
    }
}

// ====================================
// DoubleConverter Tests
// ====================================
class DoubleConverterTest {
    private lateinit var converter: DoubleConverter

    @Before
    fun setUp() {
        converter = DoubleConverter()
    }

    @Test
    fun shouldHaveCorrectTypeName() {
        assertEquals("double", converter.typeName)
    }

    @Test
    fun shouldConvertDoubleToString() {
        val input = 123.456789
        val result = converter.toString(input)
        assertEquals("123.456789", result)
    }

    @Test
    fun shouldConvertStringToDouble() {
        val input = "123.456789"
        val result = converter.fromString(input)
        assertEquals(123.456789, result, 0.0)
    }

    @Test
    fun shouldHandleSpecialDoubleValues() {
        assertEquals(Double.POSITIVE_INFINITY, converter.fromString("Infinity"), 0.0)
        assertEquals(Double.NEGATIVE_INFINITY, converter.fromString("-Infinity"), 0.0)
        assertTrue(converter.fromString("NaN").isNaN())
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForEmptyString() {
        converter.fromString("")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForNonNumericString() {
        converter.fromString("abc")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForMixedString() {
        converter.fromString("123abc")
    }

    @Test(expected = NumberFormatException::class)
    fun shouldThrowNumberFormatExceptionForStringWithSpaces() {
        converter.fromString("  123. 45  ")
    }
}

// ====================================
// BooleanConverter Tests
// ====================================
@RunWith(Parameterized::class)
class BooleanConverterValidTrueTest(private val input: String) {
    private lateinit var converter: BooleanConverter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should convert ''{0}'' to true")
        fun data(): Collection<Array<Any>> {
            return listOf("true", "TRUE", "True", "TrUe").map { arrayOf(it) }
        }
    }

    @Before
    fun setUp() {
        converter = BooleanConverter()
    }

    @Test
    fun shouldConvertValidTrueStringsToBoolean() {
        val result = converter.fromString(input)
        assertTrue(result)
    }

    @Test
    fun shouldHandleNullableTrueConversion() {
        val result = converter.fromStringNullable(input)
        assertEquals(true, result)
    }
}

@RunWith(Parameterized::class)
class BooleanConverterValidFalseTest(private val input: String) {
    private lateinit var converter: BooleanConverter

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Should convert ''{0}'' to false")
        fun data(): Collection<Array<Any>> {
            return listOf("false", "FALSE", "False", "FaLsE").map { arrayOf(it) }
        }
    }

    @Before
    fun setUp() {
        converter = BooleanConverter()
    }

    @Test
    fun shouldConvertValidFalseStringsToBoolean() {
        val result = converter.fromString(input)
        assertFalse(result)
    }

    @Test
    fun shouldHandleNullableFalseConversion() {
        val result = converter.fromStringNullable(input)
        assertEquals(false, result)
    }
}

class BooleanConverterTest {
    private lateinit var converter: BooleanConverter

    @Before
    fun setUp() {
        converter = BooleanConverter()
    }

    @Test
    fun shouldHaveCorrectTypeName() {
        assertEquals("boolean", converter.typeName)
    }

    @Test
    fun shouldConvertBooleanToString() {
        assertEquals("true", converter.toString(true))
        assertEquals("false", converter.toString(false))
    }

    @Test
    fun shouldReturnNullForNullString() {
        val result = converter.fromStringNullable(null)
        assertNull(result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowIllegalArgumentExceptionForEmptyString() {
        converter.fromString("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowIllegalArgumentExceptionForYes() {
        converter.fromString("yes")
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowIllegalArgumentExceptionForNo() {
        converter.fromString("no")
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowIllegalArgumentExceptionForNumericOne() {
        converter.fromString("1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowIllegalArgumentExceptionForNumericZero() {
        converter.fromString("0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowIllegalArgumentExceptionForSingleT() {
        converter.fromString("t")
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowIllegalArgumentExceptionForSingleF() {
        converter.fromString("f")
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowIllegalArgumentExceptionForStringWithSpaces() {
        converter.fromString("  true  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldThrowIllegalArgumentExceptionForMaybe() {
        converter.fromString("maybe")
    }
}

// ====================================
// Integration Tests
// ====================================
class TypeConverterIntegrationTest {

    @Test
    fun shouldMaintainTypeConsistencyAcrossConversions() {
        // Test String round-trip
        val stringConverter = TypeConverterRegistry.getConverter(String::class.java)!!
        val originalString = "test value"
        assertEquals(
            originalString,
            stringConverter.fromString(stringConverter.toString(originalString))
        )

        // Test Int round-trip
        val intConverter = TypeConverterRegistry.getConverter(Int::class.java)!!
        val originalInt = 12345
        assertEquals(originalInt, intConverter.fromString(intConverter.toString(originalInt)))

        // Test Boolean round-trip
        val booleanConverter = TypeConverterRegistry.getConverter(Boolean::class.java)!!
        val originalBoolean = true
        assertEquals(
            originalBoolean,
            booleanConverter.fromString(booleanConverter.toString(originalBoolean))
        )
    }

    @Test
    fun shouldHandleEdgeValuesCorrectly() {
        // Test Int edge values
        val intConverter = TypeConverterRegistry.getConverter(Int::class.java)!!
        assertEquals(
            Int.MAX_VALUE,
            intConverter.fromString(intConverter.toString(Int.MAX_VALUE))
        )
        assertEquals(
            Int.MIN_VALUE,
            intConverter.fromString(intConverter.toString(Int.MIN_VALUE))
        )

        // Test Float edge values
        val floatConverter = TypeConverterRegistry.getConverter(Float::class.java)!!
        assertEquals(
            Float.MAX_VALUE,
            floatConverter.fromString(floatConverter.toString(Float.MAX_VALUE))
        )
        assertEquals(
            Float.MIN_VALUE,
            floatConverter.fromString(floatConverter.toString(Float.MIN_VALUE))
        )
    }
}