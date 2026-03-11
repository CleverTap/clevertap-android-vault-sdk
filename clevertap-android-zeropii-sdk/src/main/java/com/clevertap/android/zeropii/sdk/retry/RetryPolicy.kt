package com.clevertap.android.zeropii.sdk.retry

/**
 * Controls retry behavior for failed tokenization network requests.
 *
 * Implement this interface to customize when and how often the SDK retries
 * failed API calls. Inject your implementation via [com.clevertap.android.zeropii.sdk.ZeroPiiSDK.initialize].
 *
 * Note: 401 Unauthorized responses are handled internally by the SDK (token refresh
 * + immediate retry) and are never passed to this policy.
 *
 * @see DefaultRetryPolicy
 */
interface RetryPolicy {

    /**
     * Determines whether the SDK should retry a failed request.
     *
     * Called after each failed attempt before the next retry. Return false to stop
     * retrying and surface the error to the app via the tokenization callback.
     *
     * @param attempt 0-based attempt index. 0 = after the first failure (before first retry).
     * @param httpStatusCode HTTP status code of the failed response.
     *                       null if the failure was a network error (no HTTP response received,
     *                       e.g. timeout or no connectivity).
     * @return true to retry, false to stop and deliver the error to the callback.
     */
    fun shouldRetry(attempt: Int, httpStatusCode: Int?): Boolean

    /**
     * Returns how long to wait before the next retry attempt.
     *
     * Only called when [shouldRetry] returns true.
     *
     * @param attempt 0-based attempt index (same value passed to [shouldRetry]).
     * @return delay in milliseconds. Return 0 for an immediate retry.
     */
    fun retryDelayMs(attempt: Int): Long
}
