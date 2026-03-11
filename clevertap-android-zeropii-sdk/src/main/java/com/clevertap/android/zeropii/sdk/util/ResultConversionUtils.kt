package com.clevertap.android.zeropii.sdk.util

import com.clevertap.android.zeropii.sdk.model.BatchTokenItem
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeResult
import com.clevertap.android.zeropii.sdk.model.TokenizeRepoResult
import com.clevertap.android.zeropii.sdk.model.TokenizeResult


// ========================================
// SINGLE VALUE CONVERSIONS
// ========================================

/**
 * Converts TokenizeRepoResult to TokenizeResult (Public API)
 */
fun TokenizeRepoResult.toPublicResult(): TokenizeResult {
    return when (this) {
        is TokenizeRepoResult.Success -> TokenizeResult.Success(
            token = this.token,
            exists = this.exists,
            newlyCreated = this.newlyCreated,
            dataType = this.dataType
        )

        is TokenizeRepoResult.Error -> TokenizeResult.Error(this.message, this.httpStatusCode)
    }
}

// ========================================
// BATCH CONVERSIONS
// ========================================

/**
 * Converts BatchTokenizeRepoResult to BatchTokenizeResult
 */
fun BatchTokenizeRepoResult.toPublicResult(): BatchTokenizeResult {
    return when (this) {
        is BatchTokenizeRepoResult.Success -> {
            try {
                val publicItems = this.results.map { repoItem ->
                    BatchTokenItem(
                        originalValue = repoItem.originalValue,
                        token = repoItem.token,
                        exists = repoItem.exists,
                        newlyCreated = repoItem.newlyCreated,
                        dataType = repoItem.dataType
                    )
                }

                BatchTokenizeResult.Success(
                    results = publicItems,
                    summary = this.summary
                )
            } catch (e: Exception) {
                BatchTokenizeResult.Error("Failed to convert batch tokenize result: ${e.message}")
            }
        }

        is BatchTokenizeRepoResult.Error -> BatchTokenizeResult.Error(this.message, this.httpStatusCode)
    }
}
