package com.clevertap.android.zeropii.sdk

import com.clevertap.android.zeropii.sdk.auth.AccessTokenCallback
import com.clevertap.android.zeropii.sdk.auth.AccessTokenInfo
import com.clevertap.android.zeropii.sdk.auth.AccessTokenProvider
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeResult
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeSummary
import com.clevertap.android.zeropii.sdk.model.TokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.TokenizeResult
import com.clevertap.android.zeropii.sdk.repository.TokenRepositoryImpl
import com.clevertap.android.zeropii.sdk.repository.TokenRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.reflect.Field

private val stubTokenProvider = object : AccessTokenProvider {
    override fun fetchToken(callback: AccessTokenCallback) {
        callback.onSuccess(AccessTokenInfo("test-token", 3600))
    }
}

// ====================================
// Initialization and Singleton Tests
// ====================================
@OptIn(ExperimentalCoroutinesApi::class)
class ZeroPiiSDKInitializationTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Reset singleton instance before each test
        resetZeroPiiSDKInstance()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetZeroPiiSDKInstance()
        unmockkAll()
    }

    @Test
    fun shouldInitializeSingletonCorrectly() {
        // Act
        val sdk = ZeroPiiSDK.initialize(
            tokenProvider = stubTokenProvider,
            apiUrl = "https://api.test.com/"
        )

        // Assert
        assertNotNull("SDK should be initialized", sdk)
    }

    @Test
    fun shouldReturnSameSingletonInstance() {
        // Act
        val sdk1 = ZeroPiiSDK.initialize(
            tokenProvider = stubTokenProvider,
            apiUrl = "https://api.test.com/"
        )

        val sdk2 = ZeroPiiSDK.getInstance()

        // Assert
        assertSame("Should return same singleton instance", sdk1, sdk2)
    }

    @Test
    fun shouldThrowExceptionWhenGetInstanceCalledBeforeInitialize() {
        // Act & Assert
        try {
            ZeroPiiSDK.getInstance()
            fail("Should throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Should contain proper error message",
                e.message?.contains("not initialized") == true
            )
        }
    }

    @Test
    fun shouldHandleDoubleInitialization() {
        // Act
        val sdk1 = ZeroPiiSDK.initialize(
            tokenProvider = stubTokenProvider,
            apiUrl = "https://api1.test.com/"
        )

        val sdk2 = ZeroPiiSDK.initialize(
            tokenProvider = stubTokenProvider,
            apiUrl = "https://api2.test.com/"
        )

        // Assert - Should return same instance (first initialization wins)
        assertSame("Should return same instance on double initialization", sdk1, sdk2)
    }

    private fun resetZeroPiiSDKInstance() {
        try {
            val instanceField: Field = ZeroPiiSDK::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            // Fallback if reflection fails
        }
    }
}

// ====================================
// Single Tokenize Operation Tests
// ====================================
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class ZeroPiiSDKSingleTokenizeTest(
    private val value: Any,
    private val valueType: Class<*>,
    private val expectedStringValue: String,
    private val description: String
) {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var sdk: ZeroPiiSDK
    private lateinit var mockTokenRepository: TokenRepository

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Tokenize {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("test-string", String::class.java, "test-string", "String value"),
                arrayOf(123, Int::class.java, "123", "Int value"),
                arrayOf(123L, Long::class.java, "123", "Long value"),
                arrayOf(123.45f, Float::class.java, "123.45", "Float value"),
                arrayOf(123.45, Double::class.java, "123.45", "Double value"),
                arrayOf(true, Boolean::class.java, "true", "Boolean value")
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        resetZeroPiiSDKInstance()

        // Create SDK
        sdk = ZeroPiiSDK.initialize(
            tokenProvider = stubTokenProvider,
            apiUrl = "https://api.test.com/"
        )
        mockTokenRepository = mockk<TokenRepositoryImpl>(relaxed = true)
        sdk.tokenRepository = mockTokenRepository
        sdk.sdkScope = TestScope(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetZeroPiiSDKInstance()
        unmockkAll()
    }

    @Test
    fun shouldTokenizeValueCorrectly() = runTest {
        // Arrange
        coEvery {
            mockTokenRepository.tokenizeWithEncryptionOverTransit(expectedStringValue)
        } returns TokenizeRepoResult.Success(
            "test-token", false, true, valueType.simpleName
        )

        var actualResult: TokenizeResult? = null

        // Act
        when (valueType) {
            String::class.java -> sdk.tokenize(value as String) { result ->
                actualResult = result
            }

            Int::class.java -> sdk.tokenize(value as Int) { result -> actualResult = result }
            Long::class.java -> sdk.tokenize(value as Long) { result -> actualResult = result }
            Float::class.java -> sdk.tokenize(value as Float) { result -> actualResult = result }
            Double::class.java -> sdk.tokenize(value as Double) { result ->
                actualResult = result
            }

            Boolean::class.java -> sdk.tokenize(value as Boolean) { result ->
                actualResult = result
            }
        }

        advanceUntilIdle() // Wait for coroutine to complete

        // Assert
        assertNotNull("Result should not be null", actualResult)
        assertTrue("Result should be success", actualResult is TokenizeResult.Success)
        val successResult = actualResult as TokenizeResult.Success
        assertEquals("Token should match", "test-token", successResult.token)

        // Verify the mock was called
        coVerify(exactly = 1) {
            mockTokenRepository.tokenizeWithEncryptionOverTransit(
                expectedStringValue
            )
        }
    }

    @Test
    fun shouldHandleTokenizeError() = runTest {
        // Arrange
        coEvery {
            mockTokenRepository.tokenizeWithEncryptionOverTransit(expectedStringValue)
        } returns TokenizeRepoResult.Error("Tokenization failed")

        var actualResult: TokenizeResult? = null

        // Act
        when (valueType) {
            String::class.java -> sdk.tokenize(value as String) { result ->
                actualResult = result
            }

            Int::class.java -> sdk.tokenize(value as Int) { result -> actualResult = result }
            Long::class.java -> sdk.tokenize(value as Long) { result -> actualResult = result }
            Float::class.java -> sdk.tokenize(value as Float) { result -> actualResult = result }
            Double::class.java -> sdk.tokenize(value as Double) { result ->
                actualResult = result
            }

            Boolean::class.java -> sdk.tokenize(value as Boolean) { result ->
                actualResult = result
            }
        }

        advanceUntilIdle()

        // Assert
        assertNotNull("Result should not be null", actualResult)
        assertTrue("Result should be error", actualResult is TokenizeResult.Error)
        val errorResult = actualResult as TokenizeResult.Error
        assertEquals("Error message should match", "Tokenization failed", errorResult.message)
    }

    private fun resetZeroPiiSDKInstance() {
        try {
            val instanceField: Field = ZeroPiiSDK::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            // Fallback if reflection fails
        }
    }
}

// ====================================
// Batch Tokenize Operation Tests
// ====================================
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class ZeroPiiSDKBatchTokenizeTest(
    private val values: List<Any>,
    private val valueType: Class<*>,
    private val methodName: String,
    private val description: String
) {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var sdk: ZeroPiiSDK
    private lateinit var mockTokenRepository: TokenRepository

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Batch Tokenize {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    listOf("val1", "val2", "val3"),
                    String::class.java,
                    "batchTokenizeStringValues",
                    "String values"
                ),
                arrayOf(
                    listOf(123, 456, 789),
                    Int::class.java,
                    "batchTokenizeIntValues",
                    "Int values"
                ),
                arrayOf(
                    listOf(123L, 456L, 789L),
                    Long::class.java,
                    "batchTokenizeLongValues",
                    "Long values"
                ),
                arrayOf(
                    listOf(12.3f, 45.6f, 78.9f),
                    Float::class.java,
                    "batchTokenizeFloatValues",
                    "Float values"
                ),
                arrayOf(
                    listOf(12.3, 45.6, 78.9),
                    Double::class.java,
                    "batchTokenizeDoubleValues",
                    "Double values"
                ),
                arrayOf(
                    listOf(true, false, true),
                    Boolean::class.java,
                    "batchTokenizeBooleanValues",
                    "Boolean values"
                )
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        resetZeroPiiSDKInstance()

        sdk = ZeroPiiSDK.initialize(
            tokenProvider = stubTokenProvider,
            apiUrl = "https://api.test.com/"
        )

        mockTokenRepository = mockk<TokenRepositoryImpl>(relaxed = true)
        sdk.tokenRepository = mockTokenRepository
        sdk.sdkScope = TestScope(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetZeroPiiSDKInstance()
        unmockkAll()
    }

    @Test
    fun shouldBatchTokenizeValuesCorrectly() = runTest {
        // Arrange
        val stringValues = values.map { it.toString() }
        val expectedRepoResult = BatchTokenizeRepoResult.Success(
            results = emptyList(), // Simplified for test
            summary = BatchTokenizeSummary(values.size, 1, values.size - 1)
        )

        coEvery { mockTokenRepository.batchTokenizeWithEncryptionOverTransit(stringValues) } returns expectedRepoResult

        var actualResult: BatchTokenizeResult? = null

        // Act
        when (methodName) {
            "batchTokenizeStringValues" -> sdk.batchTokenizeStringValues(values as List<String>) { result ->
                actualResult = result
            }

            "batchTokenizeIntValues" -> sdk.batchTokenizeIntValues(values as List<Int>) { result ->
                actualResult = result
            }

            "batchTokenizeLongValues" -> sdk.batchTokenizeLongValues(values as List<Long>) { result ->
                actualResult = result
            }

            "batchTokenizeFloatValues" -> sdk.batchTokenizeFloatValues(values as List<Float>) { result ->
                actualResult = result
            }

            "batchTokenizeDoubleValues" -> sdk.batchTokenizeDoubleValues(values as List<Double>) { result ->
                actualResult = result
            }

            "batchTokenizeBooleanValues" -> sdk.batchTokenizeBooleanValues(values as List<Boolean>) { result ->
                actualResult = result
            }
        }

        advanceUntilIdle()

        // Assert
        assertNotNull("Result should not be null", actualResult)
        assertTrue("Result should be success", actualResult is BatchTokenizeResult.Success)
        val successResult = actualResult as BatchTokenizeResult.Success
        assertEquals("Summary should match", values.size, successResult.summary.processedCount)

        coVerify(exactly = 1) {
            mockTokenRepository.batchTokenizeWithEncryptionOverTransit(
                stringValues
            )
        }
    }

    @Test
    fun shouldHandleBatchTokenizeError() = runTest {
        // Arrange
        val stringValues = values.map { it.toString() }

        coEvery {
            mockTokenRepository.batchTokenizeWithEncryptionOverTransit(stringValues)
        } returns BatchTokenizeRepoResult.Error("Batch tokenization failed")

        var actualResult: BatchTokenizeResult? = null

        // Act
        when (methodName) {
            "batchTokenizeStringValues" -> sdk.batchTokenizeStringValues(values as List<String>) { result ->
                actualResult = result
            }

            "batchTokenizeIntValues" -> sdk.batchTokenizeIntValues(values as List<Int>) { result ->
                actualResult = result
            }

            "batchTokenizeLongValues" -> sdk.batchTokenizeLongValues(values as List<Long>) { result ->
                actualResult = result
            }

            "batchTokenizeFloatValues" -> sdk.batchTokenizeFloatValues(values as List<Float>) { result ->
                actualResult = result
            }

            "batchTokenizeDoubleValues" -> sdk.batchTokenizeDoubleValues(values as List<Double>) { result ->
                actualResult = result
            }

            "batchTokenizeBooleanValues" -> sdk.batchTokenizeBooleanValues(values as List<Boolean>) { result ->
                actualResult = result
            }
        }

        advanceUntilIdle()

        // Assert
        assertNotNull("Result should not be null", actualResult)
        assertTrue("Result should be error", actualResult is BatchTokenizeResult.Error)
        val errorResult = actualResult as BatchTokenizeResult.Error
        assertEquals("Error message should match", "Batch tokenization failed", errorResult.message)
    }

    @Test
    fun shouldHandleEmptyBatchTokenize() = runTest {
        // Arrange
        val emptyValues = emptyList<Any>()
        var actualResult: BatchTokenizeResult? = null

        // Act
        when (methodName) {
            "batchTokenizeStringValues" -> sdk.batchTokenizeStringValues(emptyValues as List<String>) { result ->
                actualResult = result
            }

            "batchTokenizeIntValues" -> sdk.batchTokenizeIntValues(emptyValues as List<Int>) { result ->
                actualResult = result
            }

            "batchTokenizeLongValues" -> sdk.batchTokenizeLongValues(emptyValues as List<Long>) { result ->
                actualResult = result
            }

            "batchTokenizeFloatValues" -> sdk.batchTokenizeFloatValues(emptyValues as List<Float>) { result ->
                actualResult = result
            }

            "batchTokenizeDoubleValues" -> sdk.batchTokenizeDoubleValues(emptyValues as List<Double>) { result ->
                actualResult = result
            }

            "batchTokenizeBooleanValues" -> sdk.batchTokenizeBooleanValues(emptyValues as List<Boolean>) { result ->
                actualResult = result
            }
        }

        advanceUntilIdle()

        // Assert
        assertNotNull("Result should not be null", actualResult)
        assertTrue("Result should be error", actualResult is BatchTokenizeResult.Error)
        val errorResult = actualResult as BatchTokenizeResult.Error
        assertTrue(
            "Error should mention empty request",
            errorResult.message.contains("no values") || errorResult.message.contains("empty")
        )
    }

    private fun resetZeroPiiSDKInstance() {
        try {
            val instanceField: Field = ZeroPiiSDK::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            // Fallback if reflection fails
        }
    }
}

// ====================================
// Error Handling and Edge Cases Tests
// ====================================
@OptIn(ExperimentalCoroutinesApi::class)
class ZeroPiiSDKErrorHandlingTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var sdk: ZeroPiiSDK
    private lateinit var mockTokenRepository: TokenRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        resetZeroPiiSDKInstance()

        sdk = ZeroPiiSDK.initialize(
            tokenProvider = stubTokenProvider,
            apiUrl = "https://api.test.com/"
        )

        mockTokenRepository = mockk<TokenRepositoryImpl>(relaxed = true)
        sdk.tokenRepository = mockTokenRepository
        sdk.sdkScope = TestScope(testDispatcher)

    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetZeroPiiSDKInstance()
        unmockkAll()
    }


    @Test
    fun shouldHandleRepositoryExceptions() = runTest {
        // Arrange
        coEvery {
            mockTokenRepository.tokenizeWithEncryptionOverTransit(any())
        } throws RuntimeException("Repository failure")

        var actualResult: TokenizeResult? = null

        // Act
        sdk.tokenize("test-value") { result -> actualResult = result }
        advanceUntilIdle()

        // Assert
        assertNotNull("Result should not be null", actualResult)
        assertTrue("Result should be error", actualResult is TokenizeResult.Error)
        val errorResult = actualResult as TokenizeResult.Error
        assertTrue(
            "Error message should contain exception details",
            errorResult.message.contains("Repository failure")
        )
    }

    private fun resetZeroPiiSDKInstance() {
        try {
            val instanceField: Field = ZeroPiiSDK::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            // Fallback if reflection fails
        }
    }
}

// ====================================
// Batch Error Handling Tests (Parameterized)
// ====================================
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class ZeroPiiSDKBatchErrorHandlingTest(
    private val valueType: Class<*>,
    private val tokenizeMethodName: String,
    private val description: String
) {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var sdk: ZeroPiiSDK
    private lateinit var mockTokenRepository: TokenRepository

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Batch Error Handling {2}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    String::class.java,
                    "batchTokenizeStringValues",
                    "String batch operations"
                ),
                arrayOf(
                    Int::class.java,
                    "batchTokenizeIntValues",
                    "Int batch operations"
                ),
                arrayOf(
                    Long::class.java,
                    "batchTokenizeLongValues",
                    "Long batch operations"
                ),
                arrayOf(
                    Float::class.java,
                    "batchTokenizeFloatValues",
                    "Float batch operations"
                ),
                arrayOf(
                    Double::class.java,
                    "batchTokenizeDoubleValues",
                    "Double batch operations"
                ),
                arrayOf(
                    Boolean::class.java,
                    "batchTokenizeBooleanValues",
                    "Boolean batch operations"
                )
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        resetZeroPiiSDKInstance()

        sdk = ZeroPiiSDK.initialize(
            tokenProvider = stubTokenProvider,
            apiUrl = "https://api.test.com/"
        )

        mockTokenRepository = mockk<TokenRepositoryImpl>(relaxed = true)
        sdk.tokenRepository = mockTokenRepository
        sdk.sdkScope = TestScope(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetZeroPiiSDKInstance()
        unmockkAll()
    }

    @Test
    fun shouldHandleBatchTokenizeRepositoryException() = runTest {
        // Arrange
        val testValues = getSampleValuesForType(valueType)
        val stringValues = testValues.map { it.toString() }

        coEvery {
            mockTokenRepository.batchTokenizeWithEncryptionOverTransit(stringValues)
        } throws RuntimeException("Batch tokenize repository failure")

        // Act & Assert with type-specific exception handling
        when (tokenizeMethodName) {
            "batchTokenizeStringValues" -> {
                var actualResult: BatchTokenizeResult? = null
                sdk.batchTokenizeStringValues(testValues as List<String>) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("String batch result should not be null", actualResult)
                assertTrue(
                    "String batch result should be error",
                    actualResult is BatchTokenizeResult.Error
                )
                val errorResult = actualResult as BatchTokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch tokenize repository failure")
                )
            }

            "batchTokenizeIntValues" -> {
                var actualResult: BatchTokenizeResult? = null
                sdk.batchTokenizeIntValues(testValues as List<Int>) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Int batch result should not be null", actualResult)
                assertTrue(
                    "Int batch result should be error",
                    actualResult is BatchTokenizeResult.Error
                )
                val errorResult = actualResult as BatchTokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch tokenize repository failure")
                )
            }

            "batchTokenizeLongValues" -> {
                var actualResult: BatchTokenizeResult? = null
                sdk.batchTokenizeLongValues(testValues as List<Long>) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Long batch result should not be null", actualResult)
                assertTrue(
                    "Long batch result should be error",
                    actualResult is BatchTokenizeResult.Error
                )
                val errorResult = actualResult as BatchTokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch tokenize repository failure")
                )
            }

            "batchTokenizeFloatValues" -> {
                var actualResult: BatchTokenizeResult? = null
                sdk.batchTokenizeFloatValues(testValues as List<Float>) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Float batch result should not be null", actualResult)
                assertTrue(
                    "Float batch result should be error",
                    actualResult is BatchTokenizeResult.Error
                )
                val errorResult = actualResult as BatchTokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch tokenize repository failure")
                )
            }

            "batchTokenizeDoubleValues" -> {
                var actualResult: BatchTokenizeResult? = null
                sdk.batchTokenizeDoubleValues(testValues as List<Double>) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Double batch result should not be null", actualResult)
                assertTrue(
                    "Double batch result should be error",
                    actualResult is BatchTokenizeResult.Error
                )
                val errorResult = actualResult as BatchTokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch tokenize repository failure")
                )
            }

            "batchTokenizeBooleanValues" -> {
                var actualResult: BatchTokenizeResult? = null
                sdk.batchTokenizeBooleanValues(testValues as List<Boolean>) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Boolean batch result should not be null", actualResult)
                assertTrue(
                    "Boolean batch result should be error",
                    actualResult is BatchTokenizeResult.Error
                )
                val errorResult = actualResult as BatchTokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch tokenize repository failure")
                )
            }
        }
    }

    private fun getSampleValuesForType(type: Class<*>): List<Any> {
        return when (type) {
            String::class.java -> listOf("value1", "value2", "value3")
            Int::class.java -> listOf(123, 456, 789)
            Long::class.java -> listOf(123L, 456L, 789L)
            Float::class.java -> listOf(12.3f, 45.6f, 78.9f)
            Double::class.java -> listOf(12.3, 45.6, 78.9)
            Boolean::class.java -> listOf(true, false, true)
            else -> emptyList()
        }
    }

    private fun resetZeroPiiSDKInstance() {
        try {
            val instanceField: Field = ZeroPiiSDK::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            // Fallback if reflection fails
        }
    }
}
