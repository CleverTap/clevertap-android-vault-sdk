package com.clevertap.android.zeropii.sdk.retry

/**
 * Default retry policy used by the SDK when no custom policy is provided.
 *
 * Retries on server errors (500, 502, 503, 504), rate limiting (429), and network
 * errors (null status code). Uses exponential backoff: 2s, 4s, 8s, …
 *
 * @param maxRetries Maximum number of retries. Defaults to 1.
 */
class DefaultRetryPolicy(private val maxRetries: Int = 1) : RetryPolicy {

    private val retryableStatusCodes = setOf(500, 502, 503, 504, 429)

    override fun shouldRetry(attempt: Int, httpStatusCode: Int?): Boolean {
        if (attempt >= maxRetries) return false
        // null = network error (IOException), always retryable
        return httpStatusCode == null || httpStatusCode in retryableStatusCodes
    }

    override fun retryDelayMs(attempt: Int): Long {
        return 1000L * (1 shl (attempt + 1)) // 2s, 4s, 8s, …
    }
}
