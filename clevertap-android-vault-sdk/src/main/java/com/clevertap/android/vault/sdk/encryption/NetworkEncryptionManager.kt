package com.clevertap.android.vault.sdk.encryption

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey

class NetworkEncryptionManager(
    private val keyGenerator: CTKeyGenerator,
    private val aesgcm: AESGCMCrypt
) {

    companion object {
        private var sessionKey: SecretKey? = null
    }

    /**
     * Returns session key for encryption
     */
    private fun sessionKeyForEncryption(): SecretKey {
        return sessionKey ?: keyGenerator.generateSecretKey().also { sessionKey = it }
    }

    private fun sessionKeyBytes(): ByteArray {
        return sessionKeyForEncryption().encoded
    }

    fun sessionEncryptionKey() = Base64.encodeToString(sessionKeyBytes(), Base64.NO_WRAP)

    /**
     * Returns EncryptionResult which contains encrypted response, sessionKey and iv
     */
    fun encrypt(data: String): EncryptionResult {
        val result =
            aesgcm.performCryptOperation(
                mode = Cipher.ENCRYPT_MODE,
                data = data.toByteArray(),
                iv = null,
                secretKey = sessionKeyForEncryption()
            )

        return if (result != null) {
            EncryptionSuccess(
                encryptedPayload = convertByteArrayToString(result.encryptedBytes),
                sessionKey = sessionEncryptionKey(),
                iv = convertByteArrayToString(result.iv)
            )
        } else {
            EncryptionFailure("Encryption failed")
        }
    }

    /**
     * Returns DecryptionResult which contains decrypted response
     */
    fun decryptResponse(
        response: String,
        iv: String // base64 encoded from BE
    ): DecryptionResult {

        val decodedResponse = Base64.decode(response, Base64.NO_WRAP)
        val decodedIv = Base64.decode(iv, Base64.NO_WRAP)
        val result =
            aesgcm.performCryptOperation(
                mode = Cipher.DECRYPT_MODE,
                data = decodedResponse,
                iv = decodedIv,
                secretKey = sessionKeyForEncryption()
            )

        return if (result != null) {
            DecryptionSuccess(
                data = String(result.encryptedBytes),
            )
        } else {
            DecryptionFailure("Decryption failed")
        }
    }

    /**
     * Converts byte array to base64 string
     */
    private fun convertByteArrayToString(arr: ByteArray): String {
        //return arr.toString(Charsets.UTF_8) // might have some restricted chars
        // return java.util.Base64.getEncoder().encodeToString(arr) // Requires min api 26
        return Base64.encodeToString(arr, Base64.NO_WRAP)
    }

}