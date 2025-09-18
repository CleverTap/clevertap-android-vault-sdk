package com.clevertap.android.vault.sdk.model

import com.google.gson.annotations.SerializedName

/**
 * Authentication token response
 */
data class AuthTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("refresh_expires_in")
    val refreshExpiresIn: Int,
    @SerializedName("token_type")
    val tokenType: String,
    val scope: String
)

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
 * Request model for detokenization
 */
data class DetokenizeRequest(val token: String)

/**
 * Response model for detokenization
 */
data class DetokenizeResponse(
    val value: String?,
    val exists: Boolean,
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
 * Request model for batch detokenization
 */
data class BatchDetokenizeRequest(val tokens: List<String>)

/**
 * Response model for batch detokenization
 */
data class BatchDetokenizeResponse(
    val results: List<BatchDetokenItemResponse>,
    val summary: BatchDetokenizeSummary
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
 * Result for individual item in batch detokenization
 */
data class BatchDetokenItemResponse(
    val token: String,
    val value: String?,
    val exists: Boolean,
    val dataType: String?
)

/**
 * Summary statistics for batch detokenization
 */
data class BatchDetokenizeSummary(
    val processedCount: Int,
    val foundCount: Int,
    val notFoundCount: Int
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