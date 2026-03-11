package com.clevertap.demo.ctzeropii.model

import com.clevertap.android.zeropii.sdk.model.BatchTokenItem
import com.clevertap.android.zeropii.sdk.model.BatchTokenizeSummary
import com.clevertap.demo.ctzeropii.MainActivity

/**
 * Enhanced display models for showcasing all ZeroPii SDK operations with file import support
 */
sealed class TokenDisplayItem {

    data class Loading(val message: String) : TokenDisplayItem()

    data class SingleTokenize(
        val originalValue: String,
        val token: String,
        val exists: Boolean,
        val newlyCreated: Boolean,
        val dataType: String,
        val inputDataType: MainActivity.DataType
    ) : TokenDisplayItem()

    data class BatchTokenize(
        val results: List<BatchTokenItem>,
        val summary: BatchTokenizeSummary,
        val inputDataType: MainActivity.DataType,
        val isImported: Boolean = false,
        val fileName: String? = null
    ) : TokenDisplayItem()

    data class FileImported(
        val fileName: String,
        val totalValues: Int,
        val validValues: Int,
        val invalidValues: Int,
        val dataType: MainActivity.DataType,
        val sampleValues: List<String>,
        val timestamp: Long = System.currentTimeMillis()
    ) : TokenDisplayItem()

    data class Error(
        val title: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : TokenDisplayItem()

    data class TypeInfo(
        val dataType: MainActivity.DataType,
        val info: SampleDataProvider.DataTypeInfo,
        val timestamp: Long = System.currentTimeMillis()
    ) : TokenDisplayItem()

    data class PerformanceTest(
        val testType: String,
        val dataType: MainActivity.DataType,
        val duration: Long,
        val itemCount: Int,
        val success: Boolean,
        val details: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : TokenDisplayItem()

    /**
     * Comparison result for showing differences between operations
     */
    data class ComparisonResult(
        val title: String,
        val originalOperation: String,
        val newOperation: String,
        val differences: List<String>,
        val timestamp: Long = System.currentTimeMillis()
    ) : TokenDisplayItem()

}

/**
 * Extension functions for better display formatting
 */
fun TokenDisplayItem.getDisplayTitle(): String {
    return when (this) {
        is TokenDisplayItem.Loading -> "⏳ Loading"
        is TokenDisplayItem.SingleTokenize -> "${getDataTypeIcon(inputDataType)} Single Tokenize (${inputDataType.displayName})"
        is TokenDisplayItem.BatchTokenize -> {
            val sourceIcon = if (isImported) "📁" else "${getDataTypeIcon(inputDataType)}"
            val sourceText = if (isImported) "Imported Batch" else "Sample Batch"
            "$sourceIcon $sourceText Tokenize (${inputDataType.displayName})"
        }
        is TokenDisplayItem.FileImported -> "📂 File Imported"
        is TokenDisplayItem.Error -> "❌ $title"
        is TokenDisplayItem.TypeInfo -> "${info.icon} ${info.name} Type Info"
        is TokenDisplayItem.PerformanceTest -> "${if (success) "⚡" else "⚠️"} $testType Performance"
        is TokenDisplayItem.ComparisonResult -> "🔍 $title"
    }
}

fun TokenDisplayItem.getDisplayContent(): String {
    return when (this) {
        is TokenDisplayItem.Loading -> message

        is TokenDisplayItem.SingleTokenize -> buildString {
            append("Input: $originalValue\n")
            append("Token: $token\n")
            append("Status: ${if (exists) "Existing" else "New"}\n")
            append("Data Type: $dataType\n")
            append("Input Type: ${inputDataType.displayName}")
        }

        is TokenDisplayItem.BatchTokenize -> buildString {
            append("Source: ${if (isImported) "Imported from $fileName" else "Sample Data"}\n")
            append("Summary:\n")
            append("• Processed: ${summary.processedCount}\n")
            append("• Existing: ${summary.existingCount}\n")
            append("• New: ${summary.newlyCreatedCount}\n")
            append("• Input Type: ${inputDataType.displayName}\n")

            if (isImported) {
                append("• Success Rate: ${(summary.processedCount.toFloat() / results.size * 100).toInt()}%\n")
            }

            append("\nResults:\n")
            results.take(3).forEach { result ->
                append("• ${result.originalValue} → ${result.token}\n")
            }
            if (results.size > 3) {
                append("... and ${results.size - 3} more")
            }
        }

        is TokenDisplayItem.FileImported -> buildString {
            append("File: $fileName\n")
            append("Data Type: ${dataType.displayName}\n")
            append("Total Values: $totalValues\n")
            append("✅ Valid: $validValues\n")
            if (invalidValues > 0) {
                append("❌ Invalid: $invalidValues\n")
            }
            append("Success Rate: ${(validValues.toFloat() / totalValues * 100).toInt()}%\n\n")
            append("Sample Values:\n")
            sampleValues.forEach { sample ->
                append("• $sample\n")
            }
            if (validValues > sampleValues.size) {
                append("... and ${validValues - sampleValues.size} more")
            }
        }

        is TokenDisplayItem.Error -> message

        is TokenDisplayItem.TypeInfo -> buildString {
            append("${info.description}\n\n")
            append("Examples:\n")
            info.examples.forEach { example ->
                append("• $example\n")
            }
        }

        is TokenDisplayItem.PerformanceTest -> buildString {
            append("Test: $testType\n")
            append("Data Type: ${dataType.displayName}\n")
            append("Items: $itemCount\n")
            append("Duration: ${duration}ms\n")
            append("Status: ${if (success) "✅ Success" else "❌ Failed"}\n")
            append("Details: $details")
        }

        is TokenDisplayItem.ComparisonResult -> buildString {
            append("Original: $originalOperation\n")
            append("New: $newOperation\n\n")
            append("Differences:\n")
            differences.forEach { diff ->
                append("• $diff\n")
            }
        }
    }
}

fun TokenDisplayItem.getBackgroundColor(): Int {
    return when (this) {
        is TokenDisplayItem.Loading -> 0xFFFFF3C4.toInt()
        is TokenDisplayItem.SingleTokenize -> 0xFFE8F5E8.toInt()
        is TokenDisplayItem.BatchTokenize -> {
            if (isImported) 0xFFE8F5E8.toInt()
            else 0xFFF3E5F5.toInt()
        }
        is TokenDisplayItem.FileImported -> 0xFFE3F2FD.toInt()
        is TokenDisplayItem.Error -> 0xFFFFEBEE.toInt()
        is TokenDisplayItem.TypeInfo -> 0xFFE8EAF6.toInt()
        is TokenDisplayItem.PerformanceTest -> if (success) 0xFFE8F5E8.toInt() else 0xFFFFF3E0.toInt()
        is TokenDisplayItem.ComparisonResult -> 0xFFF1F8E9.toInt()
    }
}

private fun getDataTypeIcon(dataType: MainActivity.DataType): String {
    return when (dataType) {
        MainActivity.DataType.STRING -> "📝"
        MainActivity.DataType.INT -> "🔢"
        MainActivity.DataType.LONG -> "📊"
        MainActivity.DataType.FLOAT -> "💰"
        MainActivity.DataType.DOUBLE -> "🎯"
        MainActivity.DataType.BOOLEAN -> "✅"
    }
}
