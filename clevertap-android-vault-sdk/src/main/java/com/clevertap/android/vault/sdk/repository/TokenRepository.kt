package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.model.BatchDetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.vault.sdk.model.DetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.TokenizeRepoResult


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
     * Detokenizes a single token to retrieve the original value
     *
     * @param token The token to detokenize
     * @return The detokenization result
     */
    suspend fun detokenize(token: String): DetokenizeRepoResult

    /**
     * Tokenizes multiple sensitive values in a batch operation without encryption
     *
     * @param values The list of sensitive values to tokenize
     * @return The batch tokenization result
     */
    suspend fun batchTokenize(values: List<String>): BatchTokenizeRepoResult

    /**
     * Detokenizes multiple tokens in a batch operation
     *
     * @param tokens The list of tokens to detokenize
     * @return The batch detokenization result
     */
    suspend fun batchDetokenize(tokens: List<String>): BatchDetokenizeRepoResult

    /**
     * Tokenizes a single sensitive value with encryption over transit
     *
     * @param value The sensitive value to tokenize
     * @return The tokenization result
     */
    suspend fun tokenizeWithEncryptionOverTransit(value: String): TokenizeRepoResult

    /**
     * Detokenizes a single token with encryption over transit
     *
     * @param token The token to detokenize
     * @return The detokenization result
     */
    suspend fun detokenizeWithEncryptionOverTransit(token: String): DetokenizeRepoResult

    /**
     * Tokenizes multiple sensitive values in a batch operation with encryption over transit
     *
     * @param values The list of sensitive values to tokenize
     * @return The batch tokenization result
     */
    suspend fun batchTokenizeWithEncryptionOverTransit(values: List<String>): BatchTokenizeRepoResult

    /**
     * Detokenizes multiple tokens in a batch operation with encryption over transit
     *
     * @param tokens The list of tokens to detokenize
     * @return The batch detokenization result
     */
    suspend fun batchDetokenizeWithEncryptionOverTransit(tokens: List<String>): BatchDetokenizeRepoResult
}
