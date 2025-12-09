package com.clevertap.android.vault.sdk.model

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
     */
    data class Error(val message: String) : TokenizeRepoResult()
}

/**
 * Repository layer result class for detokenization operations
 * Used internally by TokenRepositoryImpl (String values only)
 */
sealed class DetokenizeRepoResult {
    /**
     * Successful detokenization result
     *
     * @property value The original string value
     * @property exists Whether the token exists
     * @property dataType The data type of the value
     */
    data class Success(
        val value: String?,
        val exists: Boolean,
        val dataType: String?
    ) : DetokenizeRepoResult()

    /**
     * Error during detokenization
     *
     * @property message The error message
     */
    data class Error(val message: String) : DetokenizeRepoResult()
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
     */
    data class Error(val message: String) : BatchTokenizeRepoResult()
}

/**
 * Repository layer result class for batch detokenization operations
 * Used internally by TokenRepositoryImpl (String values only)
 */
sealed class BatchDetokenizeRepoResult {
    /**
     * Successful batch detokenization result
     *
     * @property results List of individual detokenization results (Repository layer items)
     * @property summary Summary statistics for the batch operation
     */
    data class Success(
        val results: List<BatchDetokenItemResponse>,
        val summary: BatchDetokenizeSummary
    ) : BatchDetokenizeRepoResult()

    /**
     * Error during batch detokenization
     *
     * @property message The error message
     */
    data class Error(val message: String) : BatchDetokenizeRepoResult()
}
