package com.clevertap.android.zeropii.sdk.model

/**
 * Request model for tokenization
 */
data class TokenizeRequest(val value: String)

/**
 * Response model for tokenization
 */
data class TokenizeResponse(
    val token: String,
    val exists: Boolean,
    val newlyCreated: Boolean,
    val dataType: String?
)

/**
 * Request model for batch tokenization
 */
data class BatchTokenizeRequest(val values: List<String>)

/**
 * Response model for batch tokenization
 */
data class BatchTokenizeResponse(
    val results: List<BatchTokenItemResponse>,
    val summary: BatchTokenizeSummary
)

/**
 * Result for individual item in batch tokenization
 */
data class BatchTokenItemResponse(
    val originalValue: String,
    val token: String,
    val exists: Boolean,
    val newlyCreated: Boolean,
    val dataType: String?
)

/**
 * Summary statistics for batch tokenization
 */
data class BatchTokenizeSummary(
    val processedCount: Int,
    val existingCount: Int,
    val newlyCreatedCount: Int
)

/**
 * Encrypted request model
 */
data class EncryptedRequest(
    val itp: String,
    val itk: String,
    val itv: String
)

/**
 * Encrypted response model
 */
data class EncryptedResponse(
    val itp: String,
    val itv: String
)