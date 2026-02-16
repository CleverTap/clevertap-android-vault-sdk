package com.clevertap.demo.ctzeropii

import android.app.Application
import com.clevertap.android.zeropii.sdk.ZeroPiiSDK
import com.clevertap.android.zeropii.sdk.util.ZeroPiiLogger

class ZeroPiiSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the ZeroPii SDK
        try {
            ZeroPiiSDK.initialize(
                clientId = "",
                clientSecret = "",
                apiUrl = "",
                authUrl = "",
                logLevel = ZeroPiiLogger.LogLevel.VERBOSE

            )

            android.util.Log.d("ZeroPiiSample", "ZeroPii SDK initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("ZeroPiiSample", "Failed to initialize ZeroPii SDK", e)
        }
    }
}