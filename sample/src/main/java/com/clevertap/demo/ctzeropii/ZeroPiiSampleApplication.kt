package com.clevertap.demo.ctzeropii

import android.app.Application
import com.clevertap.android.zeropii.sdk.ZeroPiiSDK
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger
import com.clevertap.demo.ctzeropii.auth.OAuthAccessTokenProvider

class ZeroPiiSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the ZeroPii SDK
        try {
            ZeroPiiSDK.initialize(
                tokenProvider = OAuthAccessTokenProvider(
                    authUrl      = BuildConfig.OAUTH_URL,
                    clientId     = BuildConfig.OAUTH_CLIENT_ID,
                    clientSecret = BuildConfig.OAUTH_CLIENT_SECRET
                ),
                apiUrl   = BuildConfig.API_URL,
                logLevel = ZeroPiiLogger.LogLevel.VERBOSE
            )

            android.util.Log.d("ZeroPiiSample", "ZeroPii SDK initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("ZeroPiiSample", "Failed to initialize ZeroPii SDK", e)
        }
    }
}
