package com.clevertap.demo.ctzeropii.auth

import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

internal interface OAuthApi {
    @FormUrlEncoded
    @POST("protocol/openid-connect/token")
    suspend fun getToken(@FieldMap params: Map<String, String>): Response<OAuthTokenResponse>
}
