package com.clevertap.android.zeropii.sdk.repository

import com.clevertap.android.zeropii.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.TokenizeRepoResult


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
    suspend fun tokenize(value: String): TokenizeRepoResult

    /**
     * Tokenizes multiple sensitive values in a batch operation without encryption
     *
     * @param values The list of sensitive values to tokenize
     * @return The batch tokenization result
     */
    suspend fun batchTokenize(values: List<String>): BatchTokenizeRepoResult

    /**
     * Tokenizes a single sensitive value with encryption over transit
     *
     * @param value The sensitive value to tokenize
     * @return The tokenization result
     */
    suspend fun tokenizeWithEncryptionOverTransit(value: String): TokenizeRepoResult

    /**
     * Tokenizes multiple sensitive values in a batch operation with encryption over transit
     *
     * @param values The list of sensitive values to tokenize
     * @return The batch tokenization result
     */
    suspend fun batchTokenizeWithEncryptionOverTransit(values: List<String>): BatchTokenizeRepoResult
}
