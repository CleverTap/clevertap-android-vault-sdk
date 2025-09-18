package com.clevertap.android.vault.sdk.repository

import com.clevertap.android.vault.sdk.model.BatchDetokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeSummary
import com.clevertap.android.vault.sdk.model.BatchTokenItemResponse
import com.clevertap.android.vault.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeSummary

sealed class CacheCheckResult<TRequest, TResponse> {
    abstract val originalRequest: TRequest

    data class CompleteFromCache<TRequest, TResponse>(
        override val originalRequest: TRequest,
        val result: TResponse
    ) : CacheCheckResult<TRequest, TResponse>()

    data class PartialFromCache<TRequest, TResponse>(
        override val originalRequest: TRequest,
        val cachedItems: List<Any>, // BatchTokenItemResponse or BatchDetokenItemResponse
        val uncachedRequest: TRequest // The part that needs API call
    ) : CacheCheckResult<TRequest, TResponse>()

    data class NothingFromCache<TRequest, TResponse>(
        override val originalRequest: TRequest,
        val uncachedRequest: TRequest
    ) : CacheCheckResult<TRequest, TResponse>()
}

data class BatchTokenizeState(
    val originalValues: List<String>,
    val cachedResults: List<BatchTokenItemResponse>,
    val uncachedValues: List<String>
) {
    val isComplete: Boolean = uncachedValues.isEmpty()

    fun createCompleteResult(): BatchTokenizeRepoResult.Success {
        return BatchTokenizeRepoResult.Success(
            results = cachedResults,
            summary = BatchTokenizeSummary(
                processedCount = cachedResults.size,
                existingCount = cachedResults.size,
                newlyCreatedCount = 0
            )
        )
    }
}

data class BatchDetokenizeState(
    val originalTokens: List<String>,
    val cachedResults: List<BatchDetokenItemResponse>,
    val uncachedTokens: List<String>
) {
    val isComplete: Boolean = uncachedTokens.isEmpty()

    fun createCompleteResult(): BatchDetokenizeRepoResult.Success {
        return BatchDetokenizeRepoResult.Success(
            results = cachedResults,
            summary = BatchDetokenizeSummary(
                processedCount = cachedResults.size,
                foundCount = cachedResults.size,
                notFoundCount = 0
            )
        )
    }
}