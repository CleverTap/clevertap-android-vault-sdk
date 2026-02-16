package com.clevertap.demo.ctzeropii

import android.app.Application
import com.clevertap.android.zeropii.sdk.ZeroPiiSDK
import com.clevertap.android.zeropii.sdk.auth.AccessTokenCallback
import com.clevertap.android.zeropii.sdk.auth.AccessTokenProvider
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger

class ZeroPiiSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the ZeroPii SDK
        try {
            ZeroPiiSDK.initialize(
                tokenProvider = object : AccessTokenProvider {
                    override fun fetchToken(callback: AccessTokenCallback) {
                        // TODO: Replace with real token provider
                        callback.onFailure(Exception("Token provider not configured"))
                    }
                },
                apiUrl = "",
                logLevel = ZeroPiiLogger.LogLevel.VERBOSE
            )

            android.util.Log.d("ZeroPiiSample", "ZeroPii SDK initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("ZeroPiiSample", "Failed to initialize ZeroPii SDK", e)
        }
    }
}
