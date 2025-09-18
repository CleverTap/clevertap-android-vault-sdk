package com.clevertap.android.vault.sdk.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * Cache for token and value mappings with dataType preservation
 */
class TokenCache(
    private val enabled: Boolean
) {
    // In-memory token-to-value and value-to-token maps with dataType information
    // Pair structure: Pair<String, String> where first = value/token, second = dataType
    private val tokenToValueDataTypePairMap = ConcurrentHashMap<String, Pair<String, String>>()
    private val valueToTokenDataTypePairMap = ConcurrentHashMap<String, Pair<String, String>>()

    /**
     * Checks if caching is enabled
     *
     * @return True if caching is enabled, false otherwise
     */
    fun isEnabled(): Boolean = enabled

    /**
     * Gets a token and dataType for a given value from the cache
     *
     * @param value The value to look up
     * @return A Pair of (token, dataType) if found, null otherwise
     */
    fun getToken(value: String): Pair<String, String>? {
        if (!enabled) return null
        return valueToTokenDataTypePairMap[value]
    }

    /**
     * Gets a value and dataType for a given token from the cache
     *
     * @param token The token to look up
     * @return A Pair of (value, dataType) if found, null otherwise
     */
    fun getValue(token: String): Pair<String, String>? {
        if (!enabled) return null
        return tokenToValueDataTypePairMap[token]
    }

    /**
     * Stores a token-to-value mapping with dataType in the cache
     *
     * @param token The token
     * @param value The value
     * @param dataType The data type (nullable, defaults to "string")
     */
    fun putValue(token: String, value: String, dataType: String?) {
        if (!enabled) return
        val actualDataType = dataType ?: "string"
        tokenToValueDataTypePairMap[token] = Pair(value, actualDataType)
    }

    /**
     * Stores a value-to-token mapping with dataType in the cache
     *
     * @param value The value
     * @param token The token
     * @param dataType The data type (nullable, defaults to "string")
     */
    fun putToken(value: String, token: String, dataType: String?) {
        if (!enabled) return
        val actualDataType = dataType ?: "string"
        valueToTokenDataTypePairMap[value] = Pair(token, actualDataType)
    }

    /**
     * Stores both mappings (bidirectional) with dataType in the cache
     * This is a convenience method to ensure both directions are cached consistently
     *
     * @param token The token
     * @param value The value
     * @param dataType The data type (nullable, defaults to "string")
     */
    fun putBidirectional(token: String, value: String, dataType: String?) {
        if (!enabled) return
        val actualDataType = dataType ?: "string"
        tokenToValueDataTypePairMap[token] = Pair(value, actualDataType)
        valueToTokenDataTypePairMap[value] = Pair(token, actualDataType)
    }

    /**
     * Clears all entries from the cache
     */

    fun clear() {
        if (!enabled) return
        tokenToValueDataTypePairMap.clear()
        valueToTokenDataTypePairMap.clear()
    }

}