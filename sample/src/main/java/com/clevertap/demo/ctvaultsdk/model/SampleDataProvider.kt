package com.clevertap.demo.ctvaultsdk.model

import com.clevertap.demo.ctvaultsdk.MainActivity
import kotlin.random.Random

/**
 * Provides sample data for testing different data types with the Vault SDK
 */
object SampleDataProvider {

    // String sample data (PII examples)
    private val sampleStrings = listOf(
        "555-12-3456",              // SSN
        "john.doe@example.com",     // Email
        "4111-2222-3333-4444",     // Credit Card
        "jane.smith@test.com",      // Email
        "987-65-4321",             // SSN
        "+1-555-123-4567",         // Phone
        "alice.johnson@demo.org",   // Email
        "123-45-6789",             // SSN
        "5555-4444-3333-2222",     // Credit Card
        "bob.wilson@sample.net",    // Email
        "+44-20-7946-0958",        // UK Phone
        "111-22-3333",             // SSN
        "michael.brown@example.co.uk", // Email
        "6011-1111-1111-1117",     // Discover Card
        "+91-9876543210"           // Indian Phone
    )

    // Integer sample data
    private val sampleInts = listOf(
        123456,
        987654,
        555123,
        789456,
        111222,
        333444,
        567890,
        246813,
        135792,
        864209,
        999888,
        777666,
        444555,
        888999,
        123321
    )

    // Long sample data (large numbers, IDs, etc.)
    private val sampleLongs = listOf(
        9876543210L,
        1234567890L,
        5555666677L,
        9999888877L,
        1111222233L,
        7777666655L,
        4444333322L,
        8888999900L,
        2222111100L,
        6666777788L,
        3333444455L,
        5555444433L,
        9999000011L,
        1111333355L,
        7777888899L
    )

    // Float sample data (prices, measurements, etc.)
    private val sampleFloats = listOf(
        123.45f,
        987.65f,
        555.12f,
        789.33f,
        111.22f,
        333.44f,
        567.89f,
        246.81f,
        135.79f,
        864.20f,
        999.88f,
        777.66f,
        444.55f,
        888.99f,
        123.32f
    )

    // Double sample data (precise measurements, coordinates, etc.)
    private val sampleDoubles = listOf(
        123.456789,
        987.654321,
        555.123456,
        789.987654,
        111.222333,
        333.444555,
        567.890123,
        246.813579,
        135.792468,
        864.209753,
        999.888777,
        777.666555,
        444.555666,
        888.999000,
        123.321123
    )

    // Boolean sample data
    private val sampleBooleans = listOf(
        true, false, true, true, false,
        false, true, false, true, false,
        true, true, false, false, true
    )

    /**
     * Gets a random sample data item for the specified data type
     */
    fun getRandomSampleData(dataType: MainActivity.DataType): Any {
        return when (dataType) {
            MainActivity.DataType.STRING -> sampleStrings.random()
            MainActivity.DataType.INT -> sampleInts.random()
            MainActivity.DataType.LONG -> sampleLongs.random()
            MainActivity.DataType.FLOAT -> sampleFloats.random()
            MainActivity.DataType.DOUBLE -> sampleDoubles.random()
            MainActivity.DataType.BOOLEAN -> sampleBooleans.random()
        }
    }

    /**
     * Gets a batch of sample data for the specified data type
     */
    fun getBatchSampleData(dataType: MainActivity.DataType): List<Any> {
        val batchSize = Random.nextInt(3, 8) // Random batch size between 3-7

        return when (dataType) {
            MainActivity.DataType.STRING -> sampleStrings.shuffled().take(batchSize)
            MainActivity.DataType.INT -> sampleInts.shuffled().take(batchSize)
            MainActivity.DataType.LONG -> sampleLongs.shuffled().take(batchSize)
            MainActivity.DataType.FLOAT -> sampleFloats.shuffled().take(batchSize)
            MainActivity.DataType.DOUBLE -> sampleDoubles.shuffled().take(batchSize)
            MainActivity.DataType.BOOLEAN -> sampleBooleans.shuffled().take(batchSize)
        }
    }

    /**
     * Gets all sample data for a specific type (useful for comprehensive testing)
     */
    fun getAllSampleData(dataType: MainActivity.DataType): List<Any> {
        return when (dataType) {
            MainActivity.DataType.STRING -> sampleStrings
            MainActivity.DataType.INT -> sampleInts
            MainActivity.DataType.LONG -> sampleLongs
            MainActivity.DataType.FLOAT -> sampleFloats
            MainActivity.DataType.DOUBLE -> sampleDoubles
            MainActivity.DataType.BOOLEAN -> sampleBooleans
        }
    }

    /**
     * Generates new random sample data for testing edge cases
     */
    fun generateRandomData(dataType: MainActivity.DataType): Any {
        return when (dataType) {
            MainActivity.DataType.STRING -> generateRandomString()
            MainActivity.DataType.INT -> Random.nextInt(100000, 999999)
            MainActivity.DataType.LONG -> Random.nextLong(1000000000L, 9999999999L)
            MainActivity.DataType.FLOAT -> Random.nextFloat() * 1000
            MainActivity.DataType.DOUBLE -> Random.nextDouble() * 10000
            MainActivity.DataType.BOOLEAN -> Random.nextBoolean()
        }
    }

    /**
     * Generates a random string with various formats
     */
    private fun generateRandomString(): String {
        val formats = listOf(
            { generateSSN() },
            { generateEmail() },
            { generatePhone() },
            { generateCreditCard() }
        )
        return formats.random().invoke()
    }

    private fun generateSSN(): String {
        val area = Random.nextInt(100, 999)
        val group = Random.nextInt(10, 99)
        val serial = Random.nextInt(1000, 9999)
        return "$area-$group-$serial"
    }

    private fun generateEmail(): String {
        val firstNames = listOf("john", "jane", "bob", "alice", "charlie", "diana", "eve", "frank")
        val lastNames = listOf("smith", "doe", "johnson", "brown", "wilson", "garcia", "miller", "davis")
        val domains = listOf("example.com", "test.org", "demo.net", "sample.co", "mock.io")

        val firstName = firstNames.random()
        val lastName = lastNames.random()
        val domain = domains.random()

        return "$firstName.$lastName@$domain"
    }

    private fun generatePhone(): String {
        val countryCode = listOf("+1", "+44", "+91", "+33", "+49").random()
        val number = Random.nextLong(1000000000L, 9999999999L)
        return "$countryCode-$number"
    }

    private fun generateCreditCard(): String {
        val prefixes = listOf("4111", "5555", "3782", "6011") // Visa, MC, Amex, Discover
        val prefix = prefixes.random()
        val middle1 = Random.nextInt(1000, 9999)
        val middle2 = Random.nextInt(1000, 9999)
        val last = Random.nextInt(1000, 9999)
        return "$prefix-$middle1-$middle2-$last"
    }

    /**
     * Gets data type specific information for UI display
     */
    fun getDataTypeInfo(dataType: MainActivity.DataType): DataTypeInfo {
        return when (dataType) {
            MainActivity.DataType.STRING -> DataTypeInfo(
                name = "String",
                description = "Text data including PII like emails, SSN, phone numbers",
                examples = listOf("john@example.com", "555-12-3456", "+1-555-123-4567"),
                icon = "📝"
            )
            MainActivity.DataType.INT -> DataTypeInfo(
                name = "Integer",
                description = "Whole numbers (32-bit)",
                examples = listOf("123456", "987654", "555123"),
                icon = "🔢"
            )
            MainActivity.DataType.LONG -> DataTypeInfo(
                name = "Long",
                description = "Large whole numbers (64-bit)",
                examples = listOf("9876543210", "1234567890", "5555666677"),
                icon = "📊"
            )
            MainActivity.DataType.FLOAT -> DataTypeInfo(
                name = "Float",
                description = "Decimal numbers (32-bit precision)",
                examples = listOf("123.45", "987.65", "555.12"),
                icon = "💰"
            )
            MainActivity.DataType.DOUBLE -> DataTypeInfo(
                name = "Double",
                description = "High precision decimal numbers (64-bit)",
                examples = listOf("123.456789", "987.654321", "555.123456"),
                icon = "🎯"
            )
            MainActivity.DataType.BOOLEAN -> DataTypeInfo(
                name = "Boolean",
                description = "True or false values",
                examples = listOf("true", "false"),
                icon = "✅"
            )
        }
    }

    data class DataTypeInfo(
        val name: String,
        val description: String,
        val examples: List<String>,
        val icon: String
    )
}