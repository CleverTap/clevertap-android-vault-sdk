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

    data class DataTypeInfo(
        val name: String,
        val description: String,
        val examples: List<String>,
        val icon: String
    )
}