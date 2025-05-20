package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.model.DetokenizeResult
import com.clevertap.android.vault.sdk.model.TokenizeResult

/**
 * Repository interface for tokenization operations
 */
interface TokenRepository {
    /**
     * Tokenizes a single sensitive value without encryption over transit
     *
     * @param value The sensitive value to tokenize
     * @return The tokenization result
     */
    suspend fun tokenize(value: String): TokenizeResult

    /**
     * Detokenizes a single token to retrieve the original value
     *
     * @param token The token to detokenize
     * @return The detokenization result
     */
    suspend fun detokenize(token: String): DetokenizeResult

}
