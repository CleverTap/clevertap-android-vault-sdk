package com.clevertap.android.vault.sdk.encryption

import com.clevertap.android.vault.sdk.util.VaultLogger

/**
 * Manager for encryption operations
 */
class EncryptionManager(private val enabled: Boolean, private val logger: VaultLogger) {

    // Lazily initialize these components to avoid overhead if encryption is disabled
    private val keyGenerator: CTKeyGenerator by lazy { CTKeyGenerator() }
    private val aesgcmCrypt: AESGCMCrypt by lazy { AESGCMCrypt(logger) }
    private val networkEncryptionManager: NetworkEncryptionManager by lazy {
        NetworkEncryptionManager(keyGenerator, aesgcmCrypt)
    }

    /**
     * Checks if encryption is enabled
     *
     * @return True if encryption is enabled, false otherwise
     */
    fun isEnabled(): Boolean = enabled

    /**
     * Encrypts the provided data
     *
     * @param data The data to encrypt
     * @return The encryption result
     */
    fun encrypt(data: String): EncryptionResult {
        if (!enabled) {
            return EncryptionFailure("Encryption is disabled")
        }

        return try {
            networkEncryptionManager.encrypt(data)
        } catch (e: Exception) {
            EncryptionFailure("Encryption error: ${e.message}")
        }
    }

    /**
     * Decrypts the provided data
     *
     * @param encryptedPayload The encrypted data
     * @param iv The initialization vector
     * @return The decryption result
     */
    fun decrypt(encryptedPayload: String, iv: String): DecryptionResult {
        if (!enabled) {
            return DecryptionFailure("Encryption is disabled")
        }

        return try {
            networkEncryptionManager.decryptResponse(encryptedPayload, iv)
        } catch (e: Exception) {
            DecryptionFailure("Decryption error: ${e.message}")
        }
    }

}
