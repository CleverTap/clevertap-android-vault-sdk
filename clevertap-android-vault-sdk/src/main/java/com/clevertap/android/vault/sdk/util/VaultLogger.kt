package com.clevertap.android.vault.sdk.util

import android.util.Log

/**
 * Logger utility for the Vault SDK.
 */
class VaultLogger(private var debugLevel: Int) {

    companion object {
        private const val TAG = "CT-VaultSDK"
    }

    /**
     * Log levels for controlling verbosity
     */
    enum class LogLevel(val intValue: Int) {
        OFF(0),      // No logging
        ERROR(1),    // Only errors
        INFO(2),     // Errors + Info
        DEBUG(3),    // Errors + Info + Debug
        VERBOSE(4)   // All logs including verbose
    }

    /**
     * Logs a debug message.
     * Only logs if debug level is DEBUG or higher.
     */
    fun d(message: String) {
        if (debugLevel >= LogLevel.DEBUG.intValue) {
            Log.d(TAG, message)
        }
    }

    fun d(suffix: String, message: String) {
        if (debugLevel >= LogLevel.DEBUG.intValue) {
            if (message.length > 4000) {
                Log.d("$TAG:$suffix", message.substring(0, 4000))
                d(suffix, message.substring(4000))
            } else {
                Log.d("$TAG:$suffix", message)
            }
        }
    }

    fun d(suffix: String, message: String, throwable: Throwable) {
        if (debugLevel >= LogLevel.DEBUG.intValue) {
            Log.d("$TAG:$suffix", message, throwable)
        }
    }

    fun d(message: String, throwable: Throwable) {
        if (debugLevel >= LogLevel.DEBUG.intValue) {
            Log.d(TAG, message, throwable)
        }
    }

    /**
     * Logs an error message.
     * Only logs if debug level is ERROR or higher.
     */
    fun e(message: String, throwable: Throwable? = null) {
        if (debugLevel >= LogLevel.ERROR.intValue) {
            Log.e(TAG, message, throwable)
        }
    }

    fun e(suffix: String, message: String) {
        if (debugLevel >= LogLevel.ERROR.intValue) {
            Log.e("$TAG:$suffix", message)
        }
    }

    fun e(suffix: String, message: String, throwable: Throwable) {
        if (debugLevel >= LogLevel.ERROR.intValue) {
            Log.e("$TAG:$suffix", message, throwable)
        }
    }

    /**
     * Logs an info message.
     * Only logs if debug level is INFO or higher.
     */
    fun i(message: String) {
        if (debugLevel >= LogLevel.INFO.intValue) {
            Log.i(TAG, message)
        }
    }

    fun i(suffix: String, message: String) {
        if (debugLevel >= LogLevel.INFO.intValue) {
            Log.i("$TAG:$suffix", message)
        }
    }

    fun i(suffix: String, message: String, throwable: Throwable) {
        if (debugLevel >= LogLevel.INFO.intValue) {
            Log.i("$TAG:$suffix", message, throwable)
        }
    }

    fun i(message: String, throwable: Throwable) {
        if (debugLevel >= LogLevel.INFO.intValue) {
            Log.i(TAG, message, throwable)
        }
    }

    /**
     * Logs a warning message.
     * Only logs if debug level is INFO or higher.
     */
    fun w(message: String) {
        if (debugLevel >= LogLevel.INFO.intValue) {
            Log.w(TAG, message)
        }
    }

    fun w(suffix: String, message: String) {
        if (debugLevel >= LogLevel.INFO.intValue) {
            Log.w("$TAG:$suffix", message)
        }
    }

    fun w(suffix: String, message: String, throwable: Throwable) {
        if (debugLevel >= LogLevel.INFO.intValue) {
            Log.w("$TAG:$suffix", message, throwable)
        }
    }

    fun w(message: String, throwable: Throwable) {
        if (debugLevel >= LogLevel.INFO.intValue) {
            Log.w(TAG, message, throwable)
        }
    }

    /**
     * Logs a verbose message.
     * Only logs if debug level is VERBOSE.
     */
    fun v(message: String) {
        if (debugLevel >= LogLevel.VERBOSE.intValue) {
            Log.v(TAG, message)
        }
    }

    fun v(suffix: String, message: String) {
        if (debugLevel >= LogLevel.VERBOSE.intValue) {
            if (message.length > 4000) {
                Log.v("$TAG:$suffix", message.substring(0, 4000))
                v(suffix, message.substring(4000))
            } else {
                Log.v("$TAG:$suffix", message)
            }
        }
    }

    fun v(suffix: String, message: String, throwable: Throwable) {
        if (debugLevel >= LogLevel.VERBOSE.intValue) {
            Log.v("$TAG:$suffix", message, throwable)
        }
    }

    fun v(message: String, throwable: Throwable) {
        if (debugLevel >= LogLevel.VERBOSE.intValue) {
            Log.v(TAG, message, throwable)
        }
    }

}