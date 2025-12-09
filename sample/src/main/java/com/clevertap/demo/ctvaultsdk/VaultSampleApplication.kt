package com.clevertap.demo.ctvaultsdk

import android.app.Application
import com.clevertap.android.vault.sdk.VaultSDK
import com.clevertap.android.vault.sdk.util.VaultLogger

class VaultSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the Vault SDK
        try {
            VaultSDK.initialize(
                clientId = "",
                clientSecret = "",
                apiUrl = "",
                authUrl = "",
                logLevel = VaultLogger.LogLevel.VERBOSE

            )

            android.util.Log.d("VaultSample", "Vault SDK initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("VaultSample", "Failed to initialize Vault SDK", e)
        }
    }
}