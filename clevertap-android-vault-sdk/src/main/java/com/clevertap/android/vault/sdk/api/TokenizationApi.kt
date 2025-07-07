package com.clevertap.android.vault.sdk.api

import com.clevertap.android.vault.sdk.model.BatchDetokenizeRequest
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeRequest
import com.clevertap.android.vault.sdk.model.BatchTokenizeResponse
import com.clevertap.android.vault.sdk.model.DetokenizeRequest
import com.clevertap.android.vault.sdk.model.DetokenizeResponse
import com.clevertap.android.vault.sdk.model.EncryptedRequest
import com.clevertap.android.vault.sdk.model.EncryptedResponse
import com.clevertap.android.vault.sdk.model.TokenizeRequest
import com.clevertap.android.vault.sdk.model.TokenizeResponse
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
    @POST("api/tokenization/getToken")
    suspend fun tokenize(
        @Header("Authorization") authorization: String,
        @Body request: TokenizeRequest
    ): Response<TokenizeResponse>

    /**
     * Detokenizes a single token
     *
     * @param authorization The authorization header with bearer token
     * @param request The detokenization request
     * @return The detokenization response
     */
    @POST("api/tokenization/getRawValue")
    suspend fun detokenize(
        @Header("Authorization") authorization: String,
        @Body request: DetokenizeRequest
    ): Response<DetokenizeResponse>

    /**
     * Tokenizes multiple values in a batch
     *
     * @param authorization The authorization header with bearer token
     * @param request The batch tokenization request
     * @return The batch tokenization response
     */
    @POST("api/tokenization/tokens/batch")
    suspend fun batchTokenize(
        @Header("Authorization") authorization: String,
        @Body request: BatchTokenizeRequest
    ): Response<BatchTokenizeResponse>

    /**
     * Detokenizes multiple tokens in a batch
     *
     * @param authorization The authorization header with bearer token
     * @param request The batch detokenization request
     * @return The batch detokenization response
     */
    @POST("api/tokenization/tokens/values/batch")
    suspend fun batchDetokenize(
        @Header("Authorization") authorization: String,
        @Body request: BatchDetokenizeRequest
    ): Response<BatchDetokenizeResponse>

    /**
     * Tokenizes a single value with encryption
     *
     * @param authorization The authorization header with bearer token
     * @param request The encrypted tokenization request
     * @return The encrypted tokenization response
     */
    @POST("api/tokenization/getToken")
    suspend fun tokenizeEncrypted(
        @Header("Authorization") authorization: String,
        @Header("Encrypted") encryptionEnabled: Boolean = true,
        @Body request: EncryptedRequest
    ): Response<EncryptedResponse>

    /**
     * Detokenizes a single token with encryption
     *
     * @param authorization The authorization header with bearer token
     * @param request The encrypted detokenization request
     * @return The encrypted detokenization response
     */
    @POST("api/tokenization/getRawValue")
    suspend fun detokenizeEncrypted(
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
    @POST("api/tokenization/tokens/batch")
    suspend fun batchTokenizeEncrypted(
        @Header("Authorization") authorization: String,
        @Header("Encrypted") encryptionEnabled: Boolean = true,
        @Body request: EncryptedRequest
    ): Response<EncryptedResponse>

    /**
     * Detokenizes multiple tokens in a batch with encryption
     *
     * @param authorization The authorization header with bearer token
     * @param request The encrypted batch detokenization request
     * @return The encrypted batch detokenization response
     */
    @POST("api/tokenization/tokens/values/batch")
    suspend fun batchDetokenizeEncrypted(
        @Header("Authorization") authorization: String,
        @Header("Encrypted") encryptionEnabled: Boolean = true,
        @Body request: EncryptedRequest
    ): Response<EncryptedResponse>
}