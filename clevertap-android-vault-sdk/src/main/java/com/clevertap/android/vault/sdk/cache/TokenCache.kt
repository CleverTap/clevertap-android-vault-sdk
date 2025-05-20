package com.clevertap.android.vault.sdk.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * Cache for token and value mappings
 */
class TokenCache(
    private val enabled: Boolean
) {
    // In-memory token-to-value and value-to-token maps
    private val tokenToValueMap = ConcurrentHashMap<String, String>()
    private val valueToTokenMap = ConcurrentHashMap<String, String>()

    /**
     * Checks if caching is enabled
     *
     * @return True if caching is enabled, false otherwise
     */
    fun isEnabled(): Boolean = enabled

    /**
     * Gets a token for a given value from the cache
     *
     * @param value The value to look up
     * @return The token if found, null otherwise
     */
    fun getToken(value: String): String? {
        if (!enabled) return null
        return valueToTokenMap[value]
    }

    /**
     * Gets a value for a given token from the cache
     *
     * @param token The token to look up
     * @return The value if found, null otherwise
     */
    fun getValue(token: String): String? {
        if (!enabled) return null
        return tokenToValueMap[token]
    }

    /**
     * Stores a token-to-value mapping in the cache
     *
     * @param token The token
     * @param value The value
     */
    fun putValue(token: String, value: String) {
        if (!enabled) return
        tokenToValueMap[token] = value
    }

    /**
     * Stores a value-to-token mapping in the cache
     *
     * @param value The value
     * @param token The token
     */
    fun putToken(value: String, token: String) {
        if (!enabled) return
        valueToTokenMap[value] = token
    }

    /**
     * Clears all entries from the cache
     */
    fun clear() {
        if (!enabled) return
        tokenToValueMap.clear()
        valueToTokenMap.clear()
    }

    /**
     * Gets the size of the cache
     *
     * @return The number of entries in the cache
     */
    fun size(): Int {
        if (!enabled) return 0
        return tokenToValueMap.size
    }
}
