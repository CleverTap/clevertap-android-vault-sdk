package com.clevertap.android.vault.sdk.model

/**
 * Result class for tokenization operations
 */
sealed class TokenizeResult {
    /**
     * Successful tokenization result
     *
     * @property token The generated token
     * @property exists Whether the token already existed for this value
     * @property newlyCreated Whether the token was newly created
     * @property dataType The data type of the token (string or number)
     */
    data class Success(
        val token: String,
        val exists: Boolean,
        val newlyCreated: Boolean,
        val dataType: String?
    ) : TokenizeResult()

    /**
     * Error during tokenization
     *
     * @property message The error message
     */
    data class Error(val message: String) : TokenizeResult()
}

/**
 * Result class for detokenization operations
 */
sealed class DetokenizeResult<T> {
    /**
     * Successful detokenization result
     *
     * @property value The original value
     * @property exists Whether the token exists
     * @property dataType The data type of the value (string or number)
     */
    data class Success<T>(
        val value: T?,
        val exists: Boolean,
        val dataType: String?
    ) : DetokenizeResult<T>()

    /**
     * Error during detokenization
     *
     * @property message The error message
     */
    data class Error<T>(val message: String) : DetokenizeResult<T>()
}

/**
 * Result class for batch tokenization operations
 */
sealed class BatchTokenizeResult {
    /**
     * Successful batch tokenization result
     *
     * @property results List of individual tokenization results (Repository layer items)
     * @property summary Summary statistics for the batch operation
     */
    data class Success(
        val results: List<BatchTokenItem>,
        val summary: BatchTokenizeSummary
    ) : BatchTokenizeResult()

    /**
     * Error during batch tokenization
     *
     * @property message The error message
     */
    data class Error(val message: String) : BatchTokenizeResult()
}

/**
 * Result class for batch detokenization operations
 */
sealed class BatchDetokenizeResult<T> {
    /**
     * Successful batch detokenization result
     *
     * @property results List of individual detokenization results
     * @property summary Summary statistics for the batch operation
     */
    data class Success<T>(
        val results: List<BatchDetokenItem<T>>,
        val summary: BatchDetokenizeSummary
    ) : BatchDetokenizeResult<T>()

    /**
     * Error during batch detokenization
     *
     * @property message The error message
     */
    data class Error<T>(val message: String) : BatchDetokenizeResult<T>()
}

/**
 * Repository layer batch item for tokenization results
 * Used in public API responses
 */
data class BatchTokenItem(
    val originalValue: String,
    val token: String,
    val exists: Boolean,
    val newlyCreated: Boolean,
    val dataType: String?
)

/**
 * Repository layer batch item for detokenization results
 * Used in public API responses
 */
data class BatchDetokenItem<T>(
    val token: String,
    val value: T?,
    val exists: Boolean,
    val dataType: String?
)