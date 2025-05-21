package com.clevertap.android.vault.sdk.api

import com.clevertap.android.vault.sdk.model.AuthTokenResponse
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Retrofit interface for authentication API
 */
interface AuthApi {
    /**
     * Gets an access token using client credentials flow
     *
     * @param params The request parameters (grant_type, client_id, client_secret)
     * @return The authentication token response
     */
    @FormUrlEncoded
    @POST("protocol/openid-connect/token")
    suspend fun getToken(@FieldMap params: Map<String, String>): Response<AuthTokenResponse>
}
