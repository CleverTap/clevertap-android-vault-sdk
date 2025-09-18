package com.clevertap.android.vault.sdk

import com.clevertap.android.vault.sdk.model.BatchDetokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeSummary
import com.clevertap.android.vault.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeSummary
import com.clevertap.android.vault.sdk.model.DetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.DetokenizeResult
import com.clevertap.android.vault.sdk.model.TokenizeRepoResult
import com.clevertap.android.vault.sdk.model.TokenizeResult
import com.clevertap.android.vault.sdk.repository.TokenRepositoryImpl
import com.clevertap.android.vault.sdk.repository.TokenRepository
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

// ====================================
// Initialization and Singleton Tests
// ====================================
@OptIn(ExperimentalCoroutinesApi::class)
class VaultSDKInitializationTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Reset singleton instance before each test
        resetVaultSDKInstance()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetVaultSDKInstance()
        unmockkAll()
    }

    @Test
    fun shouldInitializeSingletonCorrectly() {
        // Act
        val sdk = VaultSDK.initialize(
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            apiUrl = "https://api.test.com/",
            authUrl = "https://auth.test.com/"
        )

        // Assert
        assertNotNull("SDK should be initialized", sdk)
    }

    @Test
    fun shouldReturnSameSingletonInstance() {
        // Act
        val sdk1 = VaultSDK.initialize(
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            apiUrl = "https://api.test.com/",
            authUrl = "https://auth.test.com/"
        )

        val sdk2 = VaultSDK.getInstance()

        // Assert
        assertSame("Should return same singleton instance", sdk1, sdk2)
    }

    @Test
    fun shouldThrowExceptionWhenGetInstanceCalledBeforeInitialize() {
        // Act & Assert
        try {
            VaultSDK.getInstance()
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
        val sdk1 = VaultSDK.initialize(
            clientId = "client1",
            clientSecret = "secret1",
            apiUrl = "https://api1.test.com/",
            authUrl = "https://auth1.test.com/"
        )

        val sdk2 = VaultSDK.initialize(
            clientId = "client2",
            clientSecret = "secret2",
            apiUrl = "https://api2.test.com/",
            authUrl = "https://auth2.test.com/"
        )

        // Assert - Should return same instance (first initialization wins)
        assertSame("Should return same instance on double initialization", sdk1, sdk2)
    }

    private fun resetVaultSDKInstance() {
        try {
            val instanceField: Field = VaultSDK::class.java.getDeclaredField("INSTANCE")
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
class VaultSDKSingleTokenizeTest(
    private val value: Any,
    private val valueType: Class<*>,
    private val expectedStringValue: String,
    private val description: String
) {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var sdk: VaultSDK
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
        resetVaultSDKInstance()

        // Create SDK
        sdk = VaultSDK.initialize(
            clientId = "test-client",
            clientSecret = "test-secret",
            apiUrl = "https://api.test.com/",
            authUrl = "https://auth.test.com/"
        )
        mockTokenRepository = mockk<TokenRepositoryImpl>(relaxed = true)
        sdk.tokenRepository = mockTokenRepository
        sdk.sdkScope = TestScope(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetVaultSDKInstance()
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

    private fun resetVaultSDKInstance() {
        try {
            val instanceField: Field = VaultSDK::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            // Fallback if reflection fails
        }
    }
}

// ====================================
// Single Detokenize Operation Tests
// ====================================
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class VaultSDKSingleDetokenizeTest(
    private val expectedValue: Any,
    private val valueType: Class<*>,
    private val methodName: String,
    private val description: String
) {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var sdk: VaultSDK
    private lateinit var mockTokenRepository: TokenRepository

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Detokenize {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("test-string", String::class.java, "deTokenizeAsString", "String value"),
                arrayOf(123, Int::class.java, "deTokenizeAsInt", "Int value"),
                arrayOf(123L, Long::class.java, "deTokenizeAsLong", "Long value"),
                arrayOf(123.45f, Float::class.java, "deTokenizeAsFloat", "Float value"),
                arrayOf(123.45, Double::class.java, "deTokenizeAsDouble", "Double value"),
                arrayOf(true, Boolean::class.java, "deTokenizeAsBoolean", "Boolean value")
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        resetVaultSDKInstance()

        sdk = VaultSDK.initialize(
            clientId = "test-client",
            clientSecret = "test-secret",
            apiUrl = "https://api.test.com/",
            authUrl = "https://auth.test.com/"
        )
        mockTokenRepository = mockk<TokenRepositoryImpl>(relaxed = true)
        sdk.tokenRepository = mockTokenRepository
        sdk.sdkScope = TestScope(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetVaultSDKInstance()
        unmockkAll()
    }

    @Test
    fun shouldDetokenizeValueCorrectly() = runTest {
        // Arrange
        val testToken = "test-token"
        val expectedStringValue = expectedValue.toString()

        coEvery {
            mockTokenRepository.detokenizeWithEncryptionOverTransit(testToken)
        } returns DetokenizeRepoResult.Success(
            expectedStringValue, true, valueType.simpleName
        )

        var actualResult: DetokenizeResult<*>? = null

        // Act
        when (methodName) {
            "deTokenizeAsString" -> sdk.deTokenizeAsString(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsInt" -> sdk.deTokenizeAsInt(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsLong" -> sdk.deTokenizeAsLong(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsFloat" -> sdk.deTokenizeAsFloat(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsDouble" -> sdk.deTokenizeAsDouble(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsBoolean" -> sdk.deTokenizeAsBoolean(testToken) { result ->
                actualResult = result
            }
        }

        advanceUntilIdle()

        // Assert
        assertNotNull("Result should not be null", actualResult)
        assertTrue("Result should be success", actualResult is DetokenizeResult.Success)
        val successResult = actualResult as DetokenizeResult.Success
        assertEquals("Value should match", expectedValue, successResult.value)

        coVerify(exactly = 1) { mockTokenRepository.detokenizeWithEncryptionOverTransit(testToken) }
    }

    @Test
    fun shouldHandleDetokenizeError() = runTest {
        // Arrange
        val testToken = "invalid-token"

        coEvery {
            mockTokenRepository.detokenizeWithEncryptionOverTransit(testToken)
        } returns DetokenizeRepoResult.Error("Token not found")

        var actualResult: DetokenizeResult<*>? = null

        // Act
        when (methodName) {
            "deTokenizeAsString" -> sdk.deTokenizeAsString(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsInt" -> sdk.deTokenizeAsInt(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsLong" -> sdk.deTokenizeAsLong(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsFloat" -> sdk.deTokenizeAsFloat(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsDouble" -> sdk.deTokenizeAsDouble(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsBoolean" -> sdk.deTokenizeAsBoolean(testToken) { result ->
                actualResult = result
            }
        }

        advanceUntilIdle()

        // Assert
        assertNotNull("Result should not be null", actualResult)
        assertTrue("Result should be error", actualResult is DetokenizeResult.Error)
        val errorResult = actualResult as DetokenizeResult.Error
        assertEquals("Error message should match", "Token not found", errorResult.message)
    }

    @Test
    fun shouldHandleDetokenizeWithNullValue() = runTest {
        // Arrange
        val testToken = "non-existent-token"

        coEvery {
            mockTokenRepository.detokenizeWithEncryptionOverTransit(testToken)
        } returns DetokenizeRepoResult.Success(
            null,
            false,
            null
        )

        var actualResult: DetokenizeResult<*>? = null

        // Act
        when (methodName) {
            "deTokenizeAsString" -> sdk.deTokenizeAsString(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsInt" -> sdk.deTokenizeAsInt(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsLong" -> sdk.deTokenizeAsLong(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsFloat" -> sdk.deTokenizeAsFloat(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsDouble" -> sdk.deTokenizeAsDouble(testToken) { result ->
                actualResult = result
            }

            "deTokenizeAsBoolean" -> sdk.deTokenizeAsBoolean(testToken) { result ->
                actualResult = result
            }
        }

        advanceUntilIdle()

        // Assert
        assertNotNull("Result should not be null", actualResult)
        if (actualResult is DetokenizeResult.Success) {
            val successResult = actualResult as DetokenizeResult.Success
            // For string type, null is acceptable, for numeric types it depends on implementation
            if (valueType == String::class.java) {
                assertEquals(
                    "Value should be null for non-existent token",
                    null,
                    successResult.value
                )
            }
        }
    }

    private fun resetVaultSDKInstance() {
        try {
            val instanceField: Field = VaultSDK::class.java.getDeclaredField("INSTANCE")
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
class VaultSDKBatchTokenizeTest(
    private val values: List<Any>,
    private val valueType: Class<*>,
    private val methodName: String,
    private val description: String
) {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var sdk: VaultSDK
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
        resetVaultSDKInstance()

        sdk = VaultSDK.initialize(
            clientId = "test-client",
            clientSecret = "test-secret",
            apiUrl = "https://api.test.com/",
            authUrl = "https://auth.test.com/"
        )

        mockTokenRepository = mockk<TokenRepositoryImpl>(relaxed = true)
        sdk.tokenRepository = mockTokenRepository
        sdk.sdkScope = TestScope(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetVaultSDKInstance()
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

    private fun resetVaultSDKInstance() {
        try {
            val instanceField: Field = VaultSDK::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            // Fallback if reflection fails
        }
    }
}

// ====================================
// Batch Detokenize Operation Tests
// ====================================
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class VaultSDKBatchDetokenizeTest(
    private val tokens: List<String>,
    private val valueType: Class<*>,
    private val methodName: String,
    private val description: String
) {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var sdk: VaultSDK
    private lateinit var mockTokenRepository: TokenRepository

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Batch Detokenize {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    listOf("token1", "token2", "token3"),
                    String::class.java,
                    "batchDeTokenizeAsString",
                    "String values"
                ),
                arrayOf(
                    listOf("token1", "token2", "token3"),
                    Int::class.java,
                    "batchDeTokenizeAsInt",
                    "Int values"
                ),
                arrayOf(
                    listOf("token1", "token2", "token3"),
                    Long::class.java,
                    "batchDeTokenizeAsLong",
                    "Long values"
                ),
                arrayOf(
                    listOf("token1", "token2", "token3"),
                    Float::class.java,
                    "batchDeTokenizeAsFloat",
                    "Float values"
                ),
                arrayOf(
                    listOf("token1", "token2", "token3"),
                    Double::class.java,
                    "batchDeTokenizeAsDouble",
                    "Double values"
                ),
                arrayOf(
                    listOf("token1", "token2", "token3"),
                    Boolean::class.java,
                    "batchDeTokenizeAsBoolean",
                    "Boolean values"
                )
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        resetVaultSDKInstance()

        sdk = VaultSDK.initialize(
            clientId = "test-client",
            clientSecret = "test-secret",
            apiUrl = "https://api.test.com/",
            authUrl = "https://auth.test.com/"
        )

        mockTokenRepository = mockk<TokenRepositoryImpl>(relaxed = true)
        sdk.tokenRepository = mockTokenRepository
        sdk.sdkScope = TestScope(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetVaultSDKInstance()
        unmockkAll()
    }

    @Test
    fun shouldBatchDetokenizeCorrectly() = runTest {
        // Arrange - Create realistic expected values based on data type
        val expectedRepoResults = when (valueType) {
            String::class.java -> listOf(
                BatchDetokenItemResponse("token1", "value1", true, "string"),
                BatchDetokenItemResponse("token2", "value2", true, "string"),
                BatchDetokenItemResponse("token3", "value3", true, "string")
            )

            Int::class.java -> listOf(
                BatchDetokenItemResponse("token1", "123", true, "integer"),
                BatchDetokenItemResponse("token2", "456", true, "integer"),
                BatchDetokenItemResponse("token3", "789", true, "integer")
            )

            Long::class.java -> listOf(
                BatchDetokenItemResponse("token1", "123456789", true, "long"),
                BatchDetokenItemResponse("token2", "987654321", true, "long"),
                BatchDetokenItemResponse("token3", "555666777", true, "long")
            )

            Float::class.java -> listOf(
                BatchDetokenItemResponse("token1", "12.34", true, "float"),
                BatchDetokenItemResponse("token2", "56.78", true, "float"),
                BatchDetokenItemResponse("token3", "90.12", true, "float")
            )

            Double::class.java -> listOf(
                BatchDetokenItemResponse("token1", "123.456789", true, "double"),
                BatchDetokenItemResponse("token2", "987.654321", true, "double"),
                BatchDetokenItemResponse("token3", "555.666777", true, "double")
            )

            Boolean::class.java -> listOf(
                BatchDetokenItemResponse("token1", "true", true, "boolean"),
                BatchDetokenItemResponse("token2", "false", true, "boolean"),
                BatchDetokenItemResponse("token3", "true", true, "boolean")
            )

            else -> emptyList()
        }

        val expectedRepoResult = BatchDetokenizeRepoResult.Success(
            results = expectedRepoResults,
            summary = BatchDetokenizeSummary(tokens.size, tokens.size, 0)
        )

        coEvery { mockTokenRepository.batchDetokenizeWithEncryptionOverTransit(tokens) } returns expectedRepoResult

        // Act & Assert with type-specific results
        when (methodName) {
            "batchDeTokenizeAsString" -> {
                var actualResult: BatchDetokenizeResult<String>? = null
                sdk.batchDeTokenizeAsString(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("String result should not be null", actualResult)
                assertTrue(
                    "String result should be success",
                    actualResult is BatchDetokenizeResult.Success
                )
                val successResult = actualResult as BatchDetokenizeResult.Success<String>
                assertEquals(
                    "Summary should match",
                    tokens.size,
                    successResult.summary.processedCount
                )

                // Verify actual converted String values
                if (successResult.results.isNotEmpty()) {
                    assertEquals(
                        "First value should be 'value1'",
                        "value1",
                        successResult.results[0].value
                    )
                    assertEquals(
                        "Second value should be 'value2'",
                        "value2",
                        successResult.results[1].value
                    )
                    assertEquals(
                        "Third value should be 'value3'",
                        "value3",
                        successResult.results[2].value
                    )
                }
            }

            "batchDeTokenizeAsInt" -> {
                var actualResult: BatchDetokenizeResult<Int>? = null
                sdk.batchDeTokenizeAsInt(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Int result should not be null", actualResult)
                assertTrue(
                    "Int result should be success",
                    actualResult is BatchDetokenizeResult.Success
                )
                val successResult = actualResult as BatchDetokenizeResult.Success<Int>
                assertEquals(
                    "Summary should match",
                    tokens.size,
                    successResult.summary.processedCount
                )

                // Verify actual converted Int values
                if (successResult.results.isNotEmpty()) {
                    assertEquals("First value should be 123", 123, successResult.results[0].value)
                    assertEquals("Second value should be 456", 456, successResult.results[1].value)
                    assertEquals("Third value should be 789", 789, successResult.results[2].value)
                    assertTrue("Values should be Int type", successResult.results[0].value is Int)
                }
            }

            "batchDeTokenizeAsLong" -> {
                var actualResult: BatchDetokenizeResult<Long>? = null
                sdk.batchDeTokenizeAsLong(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Long result should not be null", actualResult)
                assertTrue(
                    "Long result should be success",
                    actualResult is BatchDetokenizeResult.Success
                )
                val successResult = actualResult as BatchDetokenizeResult.Success<Long>
                assertEquals(
                    "Summary should match",
                    tokens.size,
                    successResult.summary.processedCount
                )

                // Verify actual converted Long values
                if (successResult.results.isNotEmpty()) {
                    assertEquals(
                        "First value should be 123456789L",
                        123456789L,
                        successResult.results[0].value
                    )
                    assertEquals(
                        "Second value should be 987654321L",
                        987654321L,
                        successResult.results[1].value
                    )
                    assertEquals(
                        "Third value should be 555666777L",
                        555666777L,
                        successResult.results[2].value
                    )
                    assertTrue("Values should be Long type", successResult.results[0].value is Long)
                }
            }

            "batchDeTokenizeAsFloat" -> {
                var actualResult: BatchDetokenizeResult<Float>? = null
                sdk.batchDeTokenizeAsFloat(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Float result should not be null", actualResult)
                assertTrue(
                    "Float result should be success",
                    actualResult is BatchDetokenizeResult.Success
                )
                val successResult = actualResult as BatchDetokenizeResult.Success<Float>
                assertEquals(
                    "Summary should match",
                    tokens.size,
                    successResult.summary.processedCount
                )

                // Verify actual converted Float values
                if (successResult.results.isNotEmpty()) {
                    assertEquals(
                        "First value should be 12.34f",
                        12.34f,
                        successResult.results[0].value!!,
                        0.001f
                    )
                    assertEquals(
                        "Second value should be 56.78f",
                        56.78f,
                        successResult.results[1].value!!,
                        0.001f
                    )
                    assertEquals(
                        "Third value should be 90.12f",
                        90.12f,
                        successResult.results[2].value!!,
                        0.001f
                    )
                    assertTrue(
                        "Values should be Float type",
                        successResult.results[0].value is Float
                    )
                }
            }

            "batchDeTokenizeAsDouble" -> {
                var actualResult: BatchDetokenizeResult<Double>? = null
                sdk.batchDeTokenizeAsDouble(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Double result should not be null", actualResult)
                assertTrue(
                    "Double result should be success",
                    actualResult is BatchDetokenizeResult.Success
                )
                val successResult = actualResult as BatchDetokenizeResult.Success<Double>
                assertEquals(
                    "Summary should match",
                    tokens.size,
                    successResult.summary.processedCount
                )

                // Verify actual converted Double values
                if (successResult.results.isNotEmpty()) {
                    assertEquals(
                        "First value should be 123.456789",
                        123.456789,
                        successResult.results[0].value!!,
                        0.000001
                    )
                    assertEquals(
                        "Second value should be 987.654321",
                        987.654321,
                        successResult.results[1].value!!,
                        0.000001
                    )
                    assertEquals(
                        "Third value should be 555.666777",
                        555.666777,
                        successResult.results[2].value!!,
                        0.000001
                    )
                    assertTrue(
                        "Values should be Double type",
                        successResult.results[0].value is Double
                    )
                }
            }

            "batchDeTokenizeAsBoolean" -> {
                var actualResult: BatchDetokenizeResult<Boolean>? = null
                sdk.batchDeTokenizeAsBoolean(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Boolean result should not be null", actualResult)
                assertTrue(
                    "Boolean result should be success",
                    actualResult is BatchDetokenizeResult.Success
                )
                val successResult = actualResult as BatchDetokenizeResult.Success<Boolean>
                assertEquals(
                    "Summary should match",
                    tokens.size,
                    successResult.summary.processedCount
                )

                // Verify actual converted Boolean values
                if (successResult.results.isNotEmpty()) {
                    assertEquals("First value should be true", true, successResult.results[0].value)
                    assertEquals(
                        "Second value should be false",
                        false,
                        successResult.results[1].value
                    )
                    assertEquals("Third value should be true", true, successResult.results[2].value)
                    assertTrue(
                        "Values should be Boolean type",
                        successResult.results[0].value is Boolean
                    )
                }
            }
        }

        coVerify(exactly = 1) { mockTokenRepository.batchDetokenizeWithEncryptionOverTransit(tokens) }
    }

    @Test
    fun shouldHandleBatchDetokenizeError() = runTest {
        // Arrange
        coEvery {
            mockTokenRepository.batchDetokenizeWithEncryptionOverTransit(tokens)
        } returns BatchDetokenizeRepoResult.Error("Batch detokenization failed")

        // Act & Assert with type-specific error results
        when (methodName) {
            "batchDeTokenizeAsString" -> {
                var actualResult: BatchDetokenizeResult<String>? = null
                sdk.batchDeTokenizeAsString(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("String result should not be null", actualResult)
                assertTrue(
                    "String result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertEquals(
                    "Error message should match",
                    "Batch detokenization failed",
                    errorResult.message
                )
            }

            "batchDeTokenizeAsInt" -> {
                var actualResult: BatchDetokenizeResult<Int>? = null
                sdk.batchDeTokenizeAsInt(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Int result should not be null", actualResult)
                assertTrue(
                    "Int result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertEquals(
                    "Error message should match",
                    "Batch detokenization failed",
                    errorResult.message
                )
            }

            "batchDeTokenizeAsLong" -> {
                var actualResult: BatchDetokenizeResult<Long>? = null
                sdk.batchDeTokenizeAsLong(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Long result should not be null", actualResult)
                assertTrue(
                    "Long result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertEquals(
                    "Error message should match",
                    "Batch detokenization failed",
                    errorResult.message
                )
            }

            "batchDeTokenizeAsFloat" -> {
                var actualResult: BatchDetokenizeResult<Float>? = null
                sdk.batchDeTokenizeAsFloat(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Float result should not be null", actualResult)
                assertTrue(
                    "Float result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertEquals(
                    "Error message should match",
                    "Batch detokenization failed",
                    errorResult.message
                )
            }

            "batchDeTokenizeAsDouble" -> {
                var actualResult: BatchDetokenizeResult<Double>? = null
                sdk.batchDeTokenizeAsDouble(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Double result should not be null", actualResult)
                assertTrue(
                    "Double result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertEquals(
                    "Error message should match",
                    "Batch detokenization failed",
                    errorResult.message
                )
            }

            "batchDeTokenizeAsBoolean" -> {
                var actualResult: BatchDetokenizeResult<Boolean>? = null
                sdk.batchDeTokenizeAsBoolean(tokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Boolean result should not be null", actualResult)
                assertTrue(
                    "Boolean result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertEquals(
                    "Error message should match",
                    "Batch detokenization failed",
                    errorResult.message
                )
            }
        }
    }

    @Test
    fun shouldHandleEmptyBatchDetokenize() = runTest {
        // Arrange
        val emptyTokens = emptyList<String>()

        // Act & Assert with type-specific empty results
        when (methodName) {
            "batchDeTokenizeAsString" -> {
                var actualResult: BatchDetokenizeResult<String>? = null
                sdk.batchDeTokenizeAsString(emptyTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("String result should not be null", actualResult)
                assertTrue(
                    "String result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should mention empty request",
                    errorResult.message.contains("no tokens") || errorResult.message.contains("empty")
                )
            }

            "batchDeTokenizeAsInt" -> {
                var actualResult: BatchDetokenizeResult<Int>? = null
                sdk.batchDeTokenizeAsInt(emptyTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Int result should not be null", actualResult)
                assertTrue(
                    "Int result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should mention empty request",
                    errorResult.message.contains("no tokens") || errorResult.message.contains("empty")
                )
            }

            "batchDeTokenizeAsLong" -> {
                var actualResult: BatchDetokenizeResult<Long>? = null
                sdk.batchDeTokenizeAsLong(emptyTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Long result should not be null", actualResult)
                assertTrue(
                    "Long result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should mention empty request",
                    errorResult.message.contains("no tokens") || errorResult.message.contains("empty")
                )
            }

            "batchDeTokenizeAsFloat" -> {
                var actualResult: BatchDetokenizeResult<Float>? = null
                sdk.batchDeTokenizeAsFloat(emptyTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Float result should not be null", actualResult)
                assertTrue(
                    "Float result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should mention empty request",
                    errorResult.message.contains("no tokens") || errorResult.message.contains("empty")
                )
            }

            "batchDeTokenizeAsDouble" -> {
                var actualResult: BatchDetokenizeResult<Double>? = null
                sdk.batchDeTokenizeAsDouble(emptyTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Double result should not be null", actualResult)
                assertTrue(
                    "Double result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should mention empty request",
                    errorResult.message.contains("no tokens") || errorResult.message.contains("empty")
                )
            }

            "batchDeTokenizeAsBoolean" -> {
                var actualResult: BatchDetokenizeResult<Boolean>? = null
                sdk.batchDeTokenizeAsBoolean(emptyTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Boolean result should not be null", actualResult)
                assertTrue(
                    "Boolean result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should mention empty request",
                    errorResult.message.contains("no tokens") || errorResult.message.contains("empty")
                )
            }
        }
    }

    private fun resetVaultSDKInstance() {
        try {
            val instanceField: Field = VaultSDK::class.java.getDeclaredField("INSTANCE")
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
class VaultSDKErrorHandlingTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var sdk: VaultSDK
    private lateinit var mockTokenRepository: TokenRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        resetVaultSDKInstance()

        sdk = VaultSDK.initialize(
            clientId = "test-client",
            clientSecret = "test-secret",
            apiUrl = "https://api.test.com/",
            authUrl = "https://auth.test.com/"
        )

        mockTokenRepository = mockk<TokenRepositoryImpl>(relaxed = true)
        sdk.tokenRepository = mockTokenRepository
        sdk.sdkScope = TestScope(testDispatcher)

    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetVaultSDKInstance()
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

    @Test
    fun shouldHandleTypeConversionException() = runTest {
        // Arrange - Return invalid format for integer conversion
        coEvery {
            mockTokenRepository.detokenizeWithEncryptionOverTransit("invalid-token")
        } returns DetokenizeRepoResult.Success(
            "not-a-number", true, "int"
        )

        var actualResult: DetokenizeResult<Int>? = null

        // Act
        sdk.deTokenizeAsInt("invalid-token") { result -> actualResult = result }
        advanceUntilIdle()

        // Assert
        assertNotNull("Result should not be null", actualResult)
        assertTrue(
            "Should handle type conversion gracefully",
            actualResult is DetokenizeResult.Success
        )
        val successResult = actualResult as DetokenizeResult.Success
        assertEquals("Value should match", null, successResult.value)
    }

    private fun resetVaultSDKInstance() {
        try {
            val instanceField: Field = VaultSDK::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            // Fallback if reflection fails
        }
    }
}

// ====================================
// Type Conversion Error Tests (Parameterized)
// ====================================
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class VaultSDKTypeConversionErrorTest(
    private val invalidValue: String,
    private val valueType: Class<*>,
    private val methodName: String,
    private val dataType: String,
    private val description: String
) {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var sdk: VaultSDK
    private lateinit var mockTokenRepository: TokenRepository

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Type Conversion Error {4}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    "not-a-number",
                    Int::class.java,
                    "deTokenizeAsInt",
                    "integer",
                    "Int with invalid string"
                ),
                arrayOf(
                    "not-a-long",
                    Long::class.java,
                    "deTokenizeAsLong",
                    "long",
                    "Long with invalid string"
                ),
                arrayOf(
                    "not-a-float",
                    Float::class.java,
                    "deTokenizeAsFloat",
                    "float",
                    "Float with invalid string"
                ),
                arrayOf(
                    "not-a-double",
                    Double::class.java,
                    "deTokenizeAsDouble",
                    "double",
                    "Double with invalid string"
                ),
                arrayOf(
                    "not-a-boolean",
                    Boolean::class.java,
                    "deTokenizeAsBoolean",
                    "boolean",
                    "Boolean with invalid string"
                ),
                arrayOf(
                    "",
                    String::class.java,
                    "deTokenizeAsString",
                    "string",
                    "String with empty value"
                )
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        resetVaultSDKInstance()

        sdk = VaultSDK.initialize(
            clientId = "test-client",
            clientSecret = "test-secret",
            apiUrl = "https://api.test.com/",
            authUrl = "https://auth.test.com/"
        )

        mockTokenRepository = mockk<TokenRepositoryImpl>(relaxed = true)
        sdk.tokenRepository = mockTokenRepository
        sdk.sdkScope = TestScope(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetVaultSDKInstance()
        unmockkAll()
    }

    @Test
    fun shouldHandleTypeConversionErrors() = runTest {
        // Arrange - Return invalid format for type conversion
        coEvery {
            mockTokenRepository.detokenizeWithEncryptionOverTransit("invalid-token")
        } returns DetokenizeRepoResult.Success(
            invalidValue, true, dataType
        )

        // Act & Assert with type-specific error handling
        when (methodName) {
            "deTokenizeAsString" -> {
                var actualResult: DetokenizeResult<String>? = null
                sdk.deTokenizeAsString("invalid-token") { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("String result should not be null", actualResult)
                assertTrue(
                    "String should handle conversion gracefully",
                    actualResult is DetokenizeResult.Success
                )
                val successResult = actualResult as DetokenizeResult.Success<String>
                assertEquals(
                    "String value should match (even if empty)",
                    invalidValue,
                    successResult.value
                )
            }

            "deTokenizeAsInt" -> {
                var actualResult: DetokenizeResult<Int>? = null
                sdk.deTokenizeAsInt("invalid-token") { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Int result should not be null", actualResult)
                assertTrue(
                    "Int should handle conversion gracefully",
                    actualResult is DetokenizeResult.Success
                )
                val successResult = actualResult as DetokenizeResult.Success<Int>
                assertEquals(
                    "Int conversion should return null for invalid format",
                    null,
                    successResult.value
                )
            }

            "deTokenizeAsLong" -> {
                var actualResult: DetokenizeResult<Long>? = null
                sdk.deTokenizeAsLong("invalid-token") { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Long result should not be null", actualResult)
                assertTrue(
                    "Long should handle conversion gracefully",
                    actualResult is DetokenizeResult.Success
                )
                val successResult = actualResult as DetokenizeResult.Success<Long>
                assertEquals(
                    "Long conversion should return null for invalid format",
                    null,
                    successResult.value
                )
            }

            "deTokenizeAsFloat" -> {
                var actualResult: DetokenizeResult<Float>? = null
                sdk.deTokenizeAsFloat("invalid-token") { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Float result should not be null", actualResult)
                assertTrue(
                    "Float should handle conversion gracefully",
                    actualResult is DetokenizeResult.Success
                )
                val successResult = actualResult as DetokenizeResult.Success<Float>
                assertEquals(
                    "Float conversion should return null for invalid format",
                    null,
                    successResult.value
                )
            }

            "deTokenizeAsDouble" -> {
                var actualResult: DetokenizeResult<Double>? = null
                sdk.deTokenizeAsDouble("invalid-token") { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Double result should not be null", actualResult)
                assertTrue(
                    "Double should handle conversion gracefully",
                    actualResult is DetokenizeResult.Success
                )
                val successResult = actualResult as DetokenizeResult.Success<Double>
                assertEquals(
                    "Double conversion should return null for invalid format",
                    null,
                    successResult.value
                )
            }

            "deTokenizeAsBoolean" -> {
                var actualResult: DetokenizeResult<Boolean>? = null
                sdk.deTokenizeAsBoolean("invalid-token") { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Boolean result should not be null", actualResult)
                assertTrue(
                    "Boolean should handle conversion gracefully",
                    actualResult is DetokenizeResult.Error
                )
            }
        }

        coVerify(exactly = 1) { mockTokenRepository.detokenizeWithEncryptionOverTransit("invalid-token") }
    }

    private fun resetVaultSDKInstance() {
        try {
            val instanceField: Field = VaultSDK::class.java.getDeclaredField("INSTANCE")
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
class VaultSDKBatchErrorHandlingTest(
    private val valueType: Class<*>,
    private val tokenizeMethodName: String,
    private val detokenizeMethodName: String,
    private val description: String
) {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var sdk: VaultSDK
    private lateinit var mockTokenRepository: TokenRepository

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Batch Error Handling {3}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    String::class.java,
                    "batchTokenizeStringValues",
                    "batchDeTokenizeAsString",
                    "String batch operations"
                ),
                arrayOf(
                    Int::class.java,
                    "batchTokenizeIntValues",
                    "batchDeTokenizeAsInt",
                    "Int batch operations"
                ),
                arrayOf(
                    Long::class.java,
                    "batchTokenizeLongValues",
                    "batchDeTokenizeAsLong",
                    "Long batch operations"
                ),
                arrayOf(
                    Float::class.java,
                    "batchTokenizeFloatValues",
                    "batchDeTokenizeAsFloat",
                    "Float batch operations"
                ),
                arrayOf(
                    Double::class.java,
                    "batchTokenizeDoubleValues",
                    "batchDeTokenizeAsDouble",
                    "Double batch operations"
                ),
                arrayOf(
                    Boolean::class.java,
                    "batchTokenizeBooleanValues",
                    "batchDeTokenizeAsBoolean",
                    "Boolean batch operations"
                )
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        resetVaultSDKInstance()

        sdk = VaultSDK.initialize(
            clientId = "test-client",
            clientSecret = "test-secret",
            apiUrl = "https://api.test.com/",
            authUrl = "https://auth.test.com/"
        )

        mockTokenRepository = mockk<TokenRepositoryImpl>(relaxed = true)
        sdk.tokenRepository = mockTokenRepository
        sdk.sdkScope = TestScope(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        resetVaultSDKInstance()
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

    @Test
    fun shouldHandleBatchDetokenizeRepositoryException() = runTest {
        // Arrange
        val testTokens = listOf("token1", "token2", "token3")

        coEvery {
            mockTokenRepository.batchDetokenizeWithEncryptionOverTransit(testTokens)
        } throws RuntimeException("Batch detokenize repository failure")

        // Act & Assert with type-specific exception handling
        when (detokenizeMethodName) {
            "batchDeTokenizeAsString" -> {
                var actualResult: BatchDetokenizeResult<String>? = null
                sdk.batchDeTokenizeAsString(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("String batch result should not be null", actualResult)
                assertTrue(
                    "String batch result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch detokenize repository failure")
                )
            }

            "batchDeTokenizeAsInt" -> {
                var actualResult: BatchDetokenizeResult<Int>? = null
                sdk.batchDeTokenizeAsInt(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Int batch result should not be null", actualResult)
                assertTrue(
                    "Int batch result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch detokenize repository failure")
                )
            }

            "batchDeTokenizeAsLong" -> {
                var actualResult: BatchDetokenizeResult<Long>? = null
                sdk.batchDeTokenizeAsLong(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Long batch result should not be null", actualResult)
                assertTrue(
                    "Long batch result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch detokenize repository failure")
                )
            }

            "batchDeTokenizeAsFloat" -> {
                var actualResult: BatchDetokenizeResult<Float>? = null
                sdk.batchDeTokenizeAsFloat(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Float batch result should not be null", actualResult)
                assertTrue(
                    "Float batch result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch detokenize repository failure")
                )
            }

            "batchDeTokenizeAsDouble" -> {
                var actualResult: BatchDetokenizeResult<Double>? = null
                sdk.batchDeTokenizeAsDouble(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Double batch result should not be null", actualResult)
                assertTrue(
                    "Double batch result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch detokenize repository failure")
                )
            }

            "batchDeTokenizeAsBoolean" -> {
                var actualResult: BatchDetokenizeResult<Boolean>? = null
                sdk.batchDeTokenizeAsBoolean(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Boolean batch result should not be null", actualResult)
                assertTrue(
                    "Boolean batch result should be error",
                    actualResult is BatchDetokenizeResult.Error
                )
                val errorResult = actualResult as BatchDetokenizeResult.Error
                assertTrue(
                    "Error should contain exception details",
                    errorResult.message.contains("Batch detokenize repository failure")
                )
            }
        }
    }

    @Test
    fun shouldHandleBatchDetokenizeWithInvalidTypeConversions() = runTest {
        // Arrange - Create mixed valid/invalid conversion data
        val testTokens = listOf("token1", "token2", "token3")
        val invalidConversionResults = when (valueType) {
            String::class.java -> listOf(
                BatchDetokenItemResponse("token1", "value1", true, "string"),
                BatchDetokenItemResponse("token2", "", true, "string"),
                BatchDetokenItemResponse("token3", "value3", true, "string")
            )

            Int::class.java -> listOf(
                BatchDetokenItemResponse("token1", "123", true, "integer"),
                BatchDetokenItemResponse("token2", "not-a-number", true, "integer"), // Invalid
                BatchDetokenItemResponse("token3", "789", true, "integer")
            )

            Long::class.java -> listOf(
                BatchDetokenItemResponse("token1", "123456789", true, "long"),
                BatchDetokenItemResponse("token2", "not-a-long", true, "long"), // Invalid
                BatchDetokenItemResponse("token3", "987654321", true, "long")
            )

            Float::class.java -> listOf(
                BatchDetokenItemResponse("token1", "12.34", true, "float"),
                BatchDetokenItemResponse("token2", "not-a-float", true, "float"), // Invalid
                BatchDetokenItemResponse("token3", "56.78", true, "float")
            )

            Double::class.java -> listOf(
                BatchDetokenItemResponse("token1", "123.456", true, "double"),
                BatchDetokenItemResponse("token2", "not-a-double", true, "double"), // Invalid
                BatchDetokenItemResponse("token3", "789.123", true, "double")
            )

            Boolean::class.java -> listOf(
                BatchDetokenItemResponse("token1", "true", true, "boolean"),
                BatchDetokenItemResponse("token2", "not-a-boolean", true, "boolean"), // Invalid
                BatchDetokenItemResponse("token3", "false", true, "boolean")
            )

            else -> emptyList()
        }

        val expectedRepoResult = BatchDetokenizeRepoResult.Success(
            results = invalidConversionResults,
            summary = BatchDetokenizeSummary(testTokens.size, testTokens.size, 0)
        )

        coEvery { mockTokenRepository.batchDetokenizeWithEncryptionOverTransit(testTokens) } returns expectedRepoResult

        // Act & Assert - Should handle invalid conversions gracefully
        when (detokenizeMethodName) {
            "batchDeTokenizeAsString" -> {
                var actualResult: BatchDetokenizeResult<String>? = null
                sdk.batchDeTokenizeAsString(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("String result should not be null", actualResult)
                assertTrue(
                    "String result should be success",
                    actualResult is BatchDetokenizeResult.Success
                )
                val successResult = actualResult as BatchDetokenizeResult.Success<String>
                // All string conversions should work, even empty strings
                assertEquals("Should have 3 results", 3, successResult.results.size)
            }

            "batchDeTokenizeAsInt" -> {
                var actualResult: BatchDetokenizeResult<Int>? = null
                sdk.batchDeTokenizeAsInt(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Int result should not be null", actualResult)
                assertTrue(
                    "Int result should be success",
                    actualResult is BatchDetokenizeResult.Success
                )
                val successResult = actualResult as BatchDetokenizeResult.Success<Int>
                assertEquals("Should have 3 results", 3, successResult.results.size)
                // Invalid conversion should result in null value
                assertEquals(
                    "Second item should have null value",
                    null,
                    successResult.results[1].value
                )
            }

            "batchDeTokenizeAsLong" -> {
                var actualResult: BatchDetokenizeResult<Long>? = null
                sdk.batchDeTokenizeAsLong(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Long result should not be null", actualResult)
                assertTrue(
                    "Long result should be success",
                    actualResult is BatchDetokenizeResult.Success
                )
                val successResult = actualResult as BatchDetokenizeResult.Success<Long>
                assertEquals("Should have 3 results", 3, successResult.results.size)
                assertEquals(
                    "Second item should have null value",
                    null,
                    successResult.results[1].value
                )
            }

            "batchDeTokenizeAsFloat" -> {
                var actualResult: BatchDetokenizeResult<Float>? = null
                sdk.batchDeTokenizeAsFloat(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Float result should not be null", actualResult)
                assertTrue(
                    "Float result should be success",
                    actualResult is BatchDetokenizeResult.Success
                )
                val successResult = actualResult as BatchDetokenizeResult.Success<Float>
                assertEquals("Should have 3 results", 3, successResult.results.size)
                assertEquals(
                    "Second item should have null value",
                    null,
                    successResult.results[1].value
                )
            }

            "batchDeTokenizeAsDouble" -> {
                var actualResult: BatchDetokenizeResult<Double>? = null
                sdk.batchDeTokenizeAsDouble(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Double result should not be null", actualResult)
                assertTrue(
                    "Double result should be success",
                    actualResult is BatchDetokenizeResult.Success
                )
                val successResult = actualResult as BatchDetokenizeResult.Success<Double>
                assertEquals("Should have 3 results", 3, successResult.results.size)
                assertEquals(
                    "Second item should have null value",
                    null,
                    successResult.results[1].value
                )
            }

            "batchDeTokenizeAsBoolean" -> {
                var actualResult: BatchDetokenizeResult<Boolean>? = null
                sdk.batchDeTokenizeAsBoolean(testTokens) { result ->
                    actualResult = result
                }
                advanceUntilIdle()

                assertNotNull("Boolean result should not be null", actualResult)
                assertTrue(
                    "Boolean result should be error",
                    actualResult is BatchDetokenizeResult.Error
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

    private fun resetVaultSDKInstance() {
        try {
            val instanceField: Field = VaultSDK::class.java.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            // Fallback if reflection fails
        }
    }
}