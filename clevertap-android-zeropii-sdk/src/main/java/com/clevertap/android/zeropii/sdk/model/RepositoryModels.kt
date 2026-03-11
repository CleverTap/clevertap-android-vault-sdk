package com.clevertap.android.zeropii.sdk.model

/**
 * Repository layer result class for tokenization operations
 * Used internally by TokenRepositoryImpl
 */
sealed class TokenizeRepoResult {
    /**
     * Successful tokenization result
     *
     * @property token The generated token
     * @property exists Whether the token already existed for this value
     * @property newlyCreated Whether the token was newly created
     * @property dataType The data type of the token
     */
    data class Success(
        val token: String,
        val exists: Boolean,
        val newlyCreated: Boolean,
        val dataType: String?
    ) : TokenizeRepoResult()

    /**
     * Error during tokenization
     *
     * @property message The error message
     * @property httpStatusCode HTTP status code from the API response, or null for network errors
     */
    data class Error(val message: String, val httpStatusCode: Int? = null) : TokenizeRepoResult()
}

/**
 * Repository layer result class for batch tokenization operations
 * Used internally by TokenRepositoryImpl (String values only)
 */
sealed class BatchTokenizeRepoResult {
    /**
     * Successful batch tokenization result
     *
     * @property results List of individual tokenization results (Repository layer items)
     * @property summary Summary statistics for the batch operation
     */
    data class Success(
        val results: List<BatchTokenItemResponse>,
        val summary: BatchTokenizeSummary
    ) : BatchTokenizeRepoResult()

    /**
     * Error during batch tokenization
     *
     * @property message The error message
     * @property httpStatusCode HTTP status code from the API response, or null for network errors
     */
    data class Error(val message: String, val httpStatusCode: Int? = null) : BatchTokenizeRepoResult()
}
