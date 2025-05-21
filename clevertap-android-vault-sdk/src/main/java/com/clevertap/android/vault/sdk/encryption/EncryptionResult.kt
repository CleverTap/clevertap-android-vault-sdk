package com.clevertap.android.vault.sdk.encryption

/**
 * Sealed class for encryption results
 */
sealed class EncryptionResult

/**
 * Successful encryption result
 *
 * @property encryptedPayload The encrypted data
 * @property sessionKey The session key used for encryption
 * @property iv The initialization vector
 */
data class EncryptionSuccess(
    val encryptedPayload: String,
    val sessionKey: String,
    val iv: String
) : EncryptionResult()

/**
 * Failed encryption result
 *
 * @property message The error message
 */
data class EncryptionFailure(val message: String) : EncryptionResult()

/**
 * Sealed class for decryption results
 */
sealed class DecryptionResult

/**
 * Successful decryption result
 *
 * @property data The decrypted data
 */
data class DecryptionSuccess(val data: String) : DecryptionResult()

/**
 * Failed decryption result
 *
 * @property message The error message
 */
data class DecryptionFailure(val message: String) : DecryptionResult()