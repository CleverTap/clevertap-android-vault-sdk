package com.clevertap.android.vault.sdk.encryption

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class CTKeyGenerator() {
    fun generateSecretKey(): SecretKey {
        // If key doesn't exist, generate a new one and store it
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256) // 256-bit AES key
        val secretKey = keyGenerator.generateKey()
        return secretKey
    }
}