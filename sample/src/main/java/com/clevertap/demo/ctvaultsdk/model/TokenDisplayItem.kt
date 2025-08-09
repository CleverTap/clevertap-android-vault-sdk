package com.clevertap.demo.ctvaultsdk.model

import com.clevertap.android.vault.sdk.model.BatchDetokenItem
import com.clevertap.android.vault.sdk.model.BatchDetokenizeSummary
import com.clevertap.android.vault.sdk.model.BatchTokenItem
import com.clevertap.android.vault.sdk.model.BatchTokenizeSummary


sealed class TokenDisplayItem {
    data class Loading(val message: String) : TokenDisplayItem()

    data class SingleTokenize(
        val originalValue: String,
        val token: String,
        val exists: Boolean,
        val newlyCreated: Boolean,
        val dataType: String
    ) : TokenDisplayItem()

    data class SingleDetokenize(
        val token: String,
        val originalValue: String,
        val exists: Boolean,
        val dataType: String
    ) : TokenDisplayItem()

    data class BatchTokenize(
        val results: List<BatchTokenItem>,
        val summary: BatchTokenizeSummary
    ) : TokenDisplayItem()

    data class BatchDetokenize(
        val results: List<BatchDetokenItem>,
        val summary: BatchDetokenizeSummary
    ) : TokenDisplayItem()

    data class Error(
        val title: String,
        val message: String
    ) : TokenDisplayItem()

    data class CacheCleared(
        val timestamp: Long
    ) : TokenDisplayItem()
}