package com.clevertap.android.vault.sdk.util

import TypeConverter
import com.clevertap.android.vault.sdk.model.BatchDetokenItem
import com.clevertap.android.vault.sdk.model.BatchDetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchDetokenizeResult
import com.clevertap.android.vault.sdk.model.BatchTokenItem
import com.clevertap.android.vault.sdk.model.BatchTokenizeRepoResult
import com.clevertap.android.vault.sdk.model.BatchTokenizeResult
import com.clevertap.android.vault.sdk.model.DetokenizeRepoResult
import com.clevertap.android.vault.sdk.model.DetokenizeResult
import com.clevertap.android.vault.sdk.model.TokenizeRepoResult
import com.clevertap.android.vault.sdk.model.TokenizeResult


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

        is TokenizeRepoResult.Error -> TokenizeResult.Error(this.message)
    }
}

/**
 * Converts DetokenizeRepoResult (String) to DetokenizeResult<T> (Public API) using TypeConverter
 */
fun <T> DetokenizeRepoResult.toPublicResult(converter: TypeConverter<T>): DetokenizeResult<T> {
    return when (this) {
        is DetokenizeRepoResult.Success -> {
            try {
                val convertedValue = converter.fromStringNullable(this.value)
                DetokenizeResult.Success(
                    value = convertedValue,
                    exists = this.exists,
                    dataType = this.dataType
                )
            } catch (e: Exception) {
                DetokenizeResult.Error("Failed to convert value: ${e.message}")
            }
        }

        is DetokenizeRepoResult.Error -> DetokenizeResult.Error(this.message)
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

        is BatchTokenizeRepoResult.Error -> BatchTokenizeResult.Error(this.message)
    }
}

/**
 * Converts BatchDetokenizeRepoResult (String) to BatchDetokenizeResult<T> (Public API) using TypeConverter
 */
fun <T> BatchDetokenizeRepoResult.toPublicResult(converter: TypeConverter<T>): BatchDetokenizeResult<T> {
    return when (this) {
        is BatchDetokenizeRepoResult.Success -> {
            try {
                val publicItems = this.results.map { repoItem ->
                    val convertedValue = converter.fromStringNullable(repoItem.value)

                    BatchDetokenItem(
                        token = repoItem.token,
                        value = convertedValue,
                        exists = repoItem.exists,
                        dataType = repoItem.dataType
                    )
                }

                BatchDetokenizeResult.Success(
                    results = publicItems,
                    summary = this.summary
                )
            } catch (e: Exception) {
                BatchDetokenizeResult.Error("Failed to convert batch detokenize result: ${e.message}")
            }
        }

        is BatchDetokenizeRepoResult.Error -> BatchDetokenizeResult.Error(this.message)
    }
}