package com.clevertap.demo.ctzeropii.auth

import com.google.gson.annotations.SerializedName

internal data class OAuthTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in")   val expiresIn: Int
)
