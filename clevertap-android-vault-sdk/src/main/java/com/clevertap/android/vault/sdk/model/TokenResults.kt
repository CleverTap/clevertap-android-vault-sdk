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
        val token: String,//TODO check nullable
        val exists: Boolean,
        val newlyCreated: Boolean,
        val dataType: String? //TODO check final data type
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
sealed class DetokenizeResult {
    /**
     * Successful detokenization result
     *
     * @property value The original value
     * @property exists Whether the token exists
     * @property dataType The data type of the value (string or number)
     */
    data class Success(
        val value: String?,
        val exists: Boolean,
        val dataType: String? //TODO check final data type
    ) : DetokenizeResult()

    /**
     * Error during detokenization
     *
     * @property message The error message
     */
    data class Error(val message: String) : DetokenizeResult()
}

