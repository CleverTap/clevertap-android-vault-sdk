package com.clevertap.android.zeropii.sdk.util

/**
 * Interface for time operations to enable testability.
 * Use [Clock.SYSTEM] for production code.
 *
 * This interface is internal and not part of the public API.
 */
internal interface Clock {
    /**
     * Returns the current time in milliseconds since epoch.
     */
    fun currentTimeMillis(): Long

    companion object {
        /**
         * System clock implementation that delegates to [System.currentTimeMillis].
         */
        val SYSTEM: Clock = object : Clock {
            override fun currentTimeMillis(): Long = System.currentTimeMillis()
        }
    }
}
