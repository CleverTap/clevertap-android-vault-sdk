package com.clevertap.demo.ctvaultsdk

import android.app.Application
import com.clevertap.android.vault.sdk.VaultSDK

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
                apiUrl = "http://3.35.104.51:8080/ct-vault/",
                authUrl = "https://auth-test.clevertap.net/auth/realms/master/",
                enableEncryption = true,
                enableCache = true,
                debugMode = true
            )

            android.util.Log.d("VaultSample", "Vault SDK initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("VaultSample", "Failed to initialize Vault SDK", e)
        }
    }
}