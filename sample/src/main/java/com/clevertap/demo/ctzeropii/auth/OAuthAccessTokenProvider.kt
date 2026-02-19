package com.clevertap.demo.ctzeropii.auth

import com.clevertap.android.zeropii.sdk.auth.AccessTokenCallback
import com.clevertap.android.zeropii.sdk.auth.AccessTokenInfo
import com.clevertap.android.zeropii.sdk.auth.AccessTokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Sample implementation of [AccessTokenProvider] that fetches tokens from a
 * Keycloak-compatible OAuth2 server using the client-credentials grant.
 *
 * Configure [authUrl], [clientId], and [clientSecret] via local.properties:
 *   OAUTH_URL=https://your-keycloak-host/realms/your-realm/
 *   OAUTH_CLIENT_ID=your-client-id
 *   OAUTH_CLIENT_SECRET=your-client-secret
 */
internal class OAuthAccessTokenProvider(
    authUrl: String,
    private val clientId: String,
    private val clientSecret: String
) : AccessTokenProvider {

    private val interceptor = HttpLoggingInterceptor().apply {
        this.level = HttpLoggingInterceptor.Level.BODY
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val api: OAuthApi = Retrofit.Builder()
        .baseUrl(authUrl)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OAuthApi::class.java)

    override fun fetchToken(callback: AccessTokenCallback) {
        scope.launch {
            try {
                val response = api.getToken(
                    mapOf(
                        "grant_type"    to "client_credentials",
                        "client_id"     to clientId,
                        "client_secret" to clientSecret
                    )
                )
//                delay(5000)
//                callback.onFailure(Exception("OAuth failed: ${response.code()} - custom test err"))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    callback.onSuccess(AccessTokenInfo(body.accessToken, body.expiresIn.toLong()))
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    callback.onFailure(Exception("OAuth failed: ${response.code()} - $error"))
                }
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }
}
