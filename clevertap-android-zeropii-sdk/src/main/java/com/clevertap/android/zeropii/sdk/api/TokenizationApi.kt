package com.clevertap.android.zeropii.sdk.api

import com.clevertap.android.zeropii.sdk.model.BatchTokenizeRequest
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeResponse
import com.clevertap.android.zeropii.sdk.model.EncryptedRequest
import com.clevertap.android.zeropii.sdk.model.EncryptedResponse
import com.clevertap.android.zeropii.sdk.model.TokenizeRequest
import com.clevertap.android.zeropii.sdk.model.TokenizeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for tokenization API
 */
interface TokenizationApi {
    /**
     * Tokenizes a single value
     *
     * @param authorization The authorization header with bearer token
     * @param request The tokenization request
     * @return The tokenization response
     */
    @POST("/ct-vault/api/v2/tokenize")
    suspend fun tokenize(
        @Header("Authorization") authorization: String,
        @Body request: TokenizeRequest
    ): Response<TokenizeResponse>

    /**
     * Tokenizes multiple values in a batch
     *
     * @param authorization The authorization header with bearer token
     * @param request The batch tokenization request
     * @return The batch tokenization response
     */
    @POST("/ct-vault/api/v2/tokenize/batch")
    suspend fun batchTokenize(
        @Header("Authorization") authorization: String,
        @Body request: BatchTokenizeRequest
    ): Response<BatchTokenizeResponse>

    /**
     * Tokenizes a single value with encryption
     *
     * @param authorization The authorization header with bearer token
     * @param request The encrypted tokenization request
     * @return The encrypted tokenization response
     */
    @POST("/ct-vault/api/v2/tokenize")
    suspend fun tokenizeEncrypted(
        @Header("Authorization") authorization: String,
        @Header("Encrypted") encryptionEnabled: Boolean = true,
        @Body request: EncryptedRequest
    ): Response<EncryptedResponse>

    /**
     * Tokenizes multiple values in a batch with encryption
     *
     * @param authorization The authorization header with bearer token
     * @param request The encrypted batch tokenization request
     * @return The encrypted batch tokenization response
     */
    @POST("/ct-vault/api/v2/tokenize/batch")
    suspend fun batchTokenizeEncrypted(
        @Header("Authorization") authorization: String,
        @Header("Encrypted") encryptionEnabled: Boolean = true,
        @Body request: EncryptedRequest
    ): Response<EncryptedResponse>
}
