package com.clevertap.android.vault.sdk.encryption

import com.clevertap.android.vault.sdk.util.VaultLogger
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * This class implements the AES-GCM Crypt algorithm
 *
 */
class AESGCMCrypt(private val logger: VaultLogger) {

    /**
     * This method actually performs both the encryption and decryption crypt task.
     *
     * @param mode - mode to determine encryption/decryption
     * @param data - data to be crypted
     * @param iv - iv required for decryption
     * @return AESGCMCryptResult
     */
    fun performCryptOperation(
        mode: Int,
        data: ByteArray,
        iv: ByteArray? = null,
        secretKey: SecretKey?
    ): AESGCMCryptResult? {
        return try {

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            when (mode) {
                Cipher.ENCRYPT_MODE -> {
                    cipher.init(mode, secretKey)
                    val generatedIv = cipher.iv // Automatically generates 12-byte IV for GCM
                    val encryptedBytes = cipher.doFinal(data)
                    AESGCMCryptResult(generatedIv, encryptedBytes)
                }

                Cipher.DECRYPT_MODE -> {
                    if (iv != null) {
                        val gcmParameterSpec =
                            GCMParameterSpec(128, iv) // 128-bit authentication tag length
                        cipher.init(mode, secretKey, gcmParameterSpec)
                        val decryptedBytes = cipher.doFinal(data)
                        AESGCMCryptResult(iv, decryptedBytes)
                    } else {
                        logger.d("IV is required for decryption")
                        null
                    }
                }

                else -> {
                    logger.d("Invalid mode used")
                    null
                }
            }
        } catch (e: Exception) {
            logger.e("Error performing crypt operation", e)
            null
        }
    }

    data class AESGCMCryptResult(
        val iv: ByteArray,
        val encryptedBytes: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AESGCMCryptResult

            if (!iv.contentEquals(other.iv)) return false
            if (!encryptedBytes.contentEquals(other.encryptedBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = iv.contentHashCode()
            result = 31 * result + encryptedBytes.contentHashCode()
            return result
        }
    }
}