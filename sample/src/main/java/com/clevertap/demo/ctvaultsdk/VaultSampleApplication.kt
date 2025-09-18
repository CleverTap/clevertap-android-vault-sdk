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
                clientId = "navin-client",
                clientSecret = "83EpGODDbeyepKaGRy1Srg4c4YVEFQG8",
//                clientId = "",
//                clientSecret = "",
//                clientId = null,
//                clientSecret =null,
                apiUrl = "https://vault.clevertap-kishlaya.net/ct-vault/",
                authUrl = "https://auth-test.clevertap.net/auth/realms/master/",
                logLevel = VaultLogger.LogLevel.VERBOSE

            )

            android.util.Log.d("VaultSample", "Vault SDK initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("VaultSample", "Failed to initialize Vault SDK", e)
        }
    }
}