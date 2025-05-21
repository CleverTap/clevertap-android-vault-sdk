package com.clevertap.android.vault.sdk.api

import com.clevertap.android.vault.sdk.model.BatchDetokenizeRequest
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeRequest
import com.clevertap.android.vault.sdk.model.BatchTokenizeResponse
import com.clevertap.android.vault.sdk.model.DetokenizeRequest
import com.clevertap.android.vault.sdk.model.DetokenizeResponse
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

}
