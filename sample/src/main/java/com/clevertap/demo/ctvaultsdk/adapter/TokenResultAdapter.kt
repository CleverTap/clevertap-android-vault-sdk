package com.clevertap.demo.ctvaultsdk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.clevertap.demo.ctvaultsdk.MainActivity
import com.clevertap.demo.ctvaultsdk.R
import com.clevertap.demo.ctvaultsdk.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced adapter for displaying comprehensive Vault SDK test results
 * Supports all data types, operation types, and file import functionality
 */
class TokenResultAdapter(
    private val items: List<TokenDisplayItem>
) : RecyclerView.Adapter<TokenResultAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val longDateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_token_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val contentText: TextView = itemView.findViewById(R.id.contentText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)

        fun bind(item: TokenDisplayItem) {
            // Set title using extension function
            titleText.text = item.getDisplayTitle()

            // Set content using extension function
            contentText.text = item.getDisplayContent()

            // Set background color using extension function
            itemView.setBackgroundColor(item.getBackgroundColor())

            // Set timestamp
            timestampText.text = getTimestamp(item)

            // Add click listener for detailed view and interactions
            itemView.setOnClickListener {
                handleItemClick(item)
            }

            // Add long click listener for additional actions
            itemView.setOnLongClickListener {
                handleItemLongClick(item)
                true
            }
        }

        private fun getTimestamp(item: TokenDisplayItem): String {
            return when (item) {
                is TokenDisplayItem.Loading -> dateFormat.format(Date())
                is TokenDisplayItem.SingleTokenize -> dateFormat.format(Date())
                is TokenDisplayItem.SingleDetokenize -> dateFormat.format(Date())
                is TokenDisplayItem.BatchTokenize -> dateFormat.format(Date())
                is TokenDisplayItem.BatchDetokenize -> dateFormat.format(Date())
                is TokenDisplayItem.FileImported -> dateFormat.format(Date(item.timestamp))
                is TokenDisplayItem.Error -> longDateFormat.format(Date(item.timestamp))
                is TokenDisplayItem.CacheCleared -> dateFormat.format(Date(item.timestamp))
                is TokenDisplayItem.TypeInfo -> dateFormat.format(Date(item.timestamp))
                is TokenDisplayItem.PerformanceTest -> longDateFormat.format(Date(item.timestamp))
                is TokenDisplayItem.ComparisonResult -> dateFormat.format(Date(item.timestamp))
                is TokenDisplayItem.BatchStatistics -> dateFormat.format(Date(item.timestamp))
            }
        }

        private fun handleItemClick(item: TokenDisplayItem) {
            when (item) {
                is TokenDisplayItem.SingleTokenize -> {
                    // Copy token to clipboard for easy detokenization testing
                    copyToClipboard("Token: ${item.token}", "Token copied!")
                    showItemSummary("Single Tokenize", """
                        ✅ Successfully tokenized ${item.inputDataType.displayName}
                        📝 Original: ${item.originalValue}
                        🔑 Token: ${item.token}
                        📊 Status: ${if (item.exists) "Existing" else "New"}
                        🎯 Type: ${item.dataType}
                    """.trimIndent())
                }

                is TokenDisplayItem.SingleDetokenize -> {
                    // Copy original value to clipboard
                    copyToClipboard("Value: ${item.originalValue}", "Value copied!")
                    showItemSummary("Single Detokenize", """
                        ✅ Successfully detokenized to ${item.outputDataType.displayName}
                        🔑 Token: ${item.token}
                        📝 Value: ${item.originalValue}
                        📊 Found: ${item.exists}
                        🎯 Type: ${item.dataType}
                    """.trimIndent())
                }

                is TokenDisplayItem.BatchTokenize -> {
                    val sourceText = if (item.isImported) "imported from ${item.fileName}" else "sample"
                    showBatchTokenizeDetails(item, sourceText)
                }

                is TokenDisplayItem.BatchDetokenize -> {
                    showBatchDetokenizeDetails(item)
                }

                is TokenDisplayItem.FileImported -> {
                    showFileImportDetails(item)
                }

                is TokenDisplayItem.Error -> {
                    showErrorDetails(item)
                }

                is TokenDisplayItem.CacheCleared -> {
                    showToast("Cache was cleared at ${dateFormat.format(Date(item.timestamp))}")
                }

                is TokenDisplayItem.PerformanceTest -> {
                    showPerformanceDetails(item)
                }

                is TokenDisplayItem.ComparisonResult -> {
                    showComparisonDetails(item)
                }

                is TokenDisplayItem.BatchStatistics -> {
                    showStatisticsDetails(item)
                }

                else -> {
                    // Default action - show basic item info
                    showItemSummary("Item Details", item.getDisplayContent())
                }
            }
        }

        private fun handleItemLongClick(item: TokenDisplayItem) {
            // Long click provides additional actions
            when (item) {
                is TokenDisplayItem.SingleTokenize -> {
                    showActionDialog("Token Actions", listOf(
                        "Copy Token" to { copyToClipboard("${item.token}", "Token copied!") },
                        "Copy Original" to { copyToClipboard("${item.originalValue}", "Original value copied!") },
                        "Copy Both" to { copyToClipboard("${item.originalValue} → ${item.token}", "Token mapping copied!") }
                    ))
                }

                is TokenDisplayItem.BatchTokenize -> {
                    showActionDialog("Batch Actions", listOf(
                        "Copy All Tokens" to {
                            val tokens = item.results.joinToString(",") { it.token }
                            copyToClipboard(tokens, "${item.results.size} tokens copied!")
                        },
                        "Copy All Originals" to {
                            val originals = item.results.joinToString(",") { it.originalValue }
                            copyToClipboard(originals, "${item.results.size} original values copied!")
                        },
                        "Export Results" to { exportBatchResults(item) }
                    ))
                }

                is TokenDisplayItem.FileImported -> {
                    showActionDialog("File Actions", listOf(
                        "Copy Valid Values" to {
                            val values = item.sampleValues.joinToString(",")
                            copyToClipboard(values, "Sample values copied!")
                        },
                        "Show Import Stats" to { showFileImportDetails(item) }
                    ))
                }

                else -> {
                    // Default long click - copy content
                    copyToClipboard(item.getDisplayContent(), "Content copied!")
                }
            }
        }

        // ================================
        // ENHANCED DETAIL DIALOGS
        // ================================

        private fun showBatchTokenizeDetails(item: TokenDisplayItem.BatchTokenize, sourceText: String) {
            val successRate = if (item.results.isNotEmpty()) {
                (item.summary.processedCount.toFloat() / item.results.size * 100).toInt()
            } else 0

            val details = """
                📊 Batch Tokenize Results
                
                📁 Source: ${sourceText.capitalize()}
                🎯 Data Type: ${item.inputDataType.displayName}
                
                📈 Statistics:
                • Total Items: ${item.results.size}
                • Processed: ${item.summary.processedCount}
                • Existing Tokens: ${item.summary.existingCount}
                • New Tokens: ${item.summary.newlyCreatedCount}
                • Success Rate: $successRate%
                
                ⚡ Performance:
                • Batch vs Individual: ${item.results.size}x faster
                • API Calls Saved: ${item.results.size - 1}
                
                🔍 Sample Results:
                ${item.results.take(5).joinToString("\n") { "• ${it.originalValue} → ${it.token}" }}
                ${if (item.results.size > 5) "\n... and ${item.results.size - 5} more" else ""}
            """.trimIndent()

            showItemSummary("Batch Tokenize Details", details)
        }

        private fun showBatchDetokenizeDetails(item: TokenDisplayItem.BatchDetokenize) {
            val foundRate = if (item.summary.processedCount > 0) {
                (item.summary.foundCount.toFloat() / item.summary.processedCount * 100).toInt()
            } else 0

            val details = """
                📊 Batch Detokenize Results
                
                🎯 Data Type: ${item.outputDataType.displayName}
                
                📈 Statistics:
                • Total Tokens: ${item.results.size}
                • Processed: ${item.summary.processedCount}
                • Found Values: ${item.summary.foundCount}
                • Not Found: ${item.summary.notFoundCount}
                • Found Rate: $foundRate%
                
                🔍 Sample Results:
                ${item.results.take(5).joinToString("\n") { "• ${it.token} → ${it.value ?: "null"}" }}
                ${if (item.results.size > 5) "\n... and ${item.results.size - 5} more" else ""}
            """.trimIndent()

            showItemSummary("Batch Detokenize Details", details)
        }

        private fun showFileImportDetails(item: TokenDisplayItem.FileImported) {
            val successRate = (item.validValues.toFloat() / item.totalValues * 100).toInt()
            val errorRate = (item.invalidValues.toFloat() / item.totalValues * 100).toInt()

            val details = """
                📂 File Import Summary
                
                📁 File: ${item.fileName}
                🎯 Target Type: ${item.dataType.displayName}
                ⏰ Imported: ${dateFormat.format(Date(item.timestamp))}
                
                📊 Validation Results:
                • Total Values: ${item.totalValues}
                • ✅ Valid: ${item.validValues} (${successRate}%)
                • ❌ Invalid: ${item.invalidValues} (${errorRate}%)
                
                🔍 Sample Valid Values:
                ${item.sampleValues.joinToString("\n") { "• $it" }}
                ${if (item.validValues > item.sampleValues.size) "\n... and ${item.validValues - item.sampleValues.size} more" else ""}
                
                💡 Ready for batch tokenization!
            """.trimIndent()

            showItemSummary("File Import Details", details)
        }

        private fun showErrorDetails(item: TokenDisplayItem.Error) {
            val details = """
                ❌ Error Details
                
                🏷️ Type: ${item.title}
                ⏰ Time: ${longDateFormat.format(Date(item.timestamp))}
                
                📝 Message:
                ${item.message}
                
                💡 Troubleshooting:
                • Check network connection
                • Verify API credentials
                • Ensure data format is correct
                • Try refreshing the token
            """.trimIndent()

            showItemSummary("Error Details", details)
        }

        private fun showPerformanceDetails(item: TokenDisplayItem.PerformanceTest) {
            val throughput = if (item.duration > 0) {
                (item.itemCount.toFloat() / item.duration * 1000).toInt()
            } else 0

            val details = """
                ⚡ Performance Test Results
                
                🧪 Test: ${item.testType}
                🎯 Data Type: ${item.dataType.displayName}
                ⏰ Timestamp: ${longDateFormat.format(Date(item.timestamp))}
                
                📊 Metrics:
                • Items Processed: ${item.itemCount}
                • Duration: ${item.duration}ms
                • Throughput: $throughput items/sec
                • Status: ${if (item.success) "✅ Success" else "❌ Failed"}
                
                📝 Details:
                ${item.details}
            """.trimIndent()

            showItemSummary("Performance Details", details)
        }

        private fun showComparisonDetails(item: TokenDisplayItem.ComparisonResult) {
            val details = """
                🔍 Comparison Results
                
                ⏰ Analysis Time: ${dateFormat.format(Date(item.timestamp))}
                
                📊 Comparison: ${item.title}
                
                🔄 Operations:
                • Original: ${item.originalOperation}
                • New: ${item.newOperation}
                
                📝 Key Differences:
                ${item.differences.joinToString("\n") { "• $it" }}
            """.trimIndent()

            showItemSummary("Comparison Details", details)
        }

        private fun showStatisticsDetails(item: TokenDisplayItem.BatchStatistics) {
            val successRate = if (item.totalItems > 0) {
                (item.successfulItems.toFloat() / item.totalItems * 100).toInt()
            } else 0

            val details = """
                📊 Batch Operation Statistics
                
                🎯 Operation: ${item.operationType}
                📋 Data Type: ${item.dataType.displayName}
                ⏰ Analysis Time: ${dateFormat.format(Date(item.timestamp))}
                
                📈 Performance Metrics:
                • Total Items: ${item.totalItems}
                • Successful: ${item.successfulItems} (${successRate}%)
                • Failed: ${item.failedItems}
                • Avg Response Time: ${item.avgResponseTime}ms
                • Cache Hit Rate: ${(item.cacheHitRate * 100).toInt()}%
                
                💡 Insights:
                • Batch efficiency: High
                • Cache utilization: ${if (item.cacheHitRate > 0.5) "Excellent" else "Moderate"}
                • Error rate: ${if (item.failedItems == 0) "None" else "Low"}
            """.trimIndent()

            showItemSummary("Statistics Details", details)
        }

        // ================================
        // UTILITY METHODS
        // ================================

        private fun copyToClipboard(text: String, confirmationMessage: String) {
            val clipboard = itemView.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Vault SDK Result", text)
            clipboard.setPrimaryClip(clip)

            showToast(confirmationMessage)
        }

        private fun showToast(message: String) {
            android.widget.Toast.makeText(itemView.context, message, android.widget.Toast.LENGTH_SHORT).show()
        }

        private fun showItemSummary(title: String, content: String) {
            androidx.appcompat.app.AlertDialog.Builder(itemView.context)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("OK", null)
                .setNeutralButton("Copy") { _, _ ->
                    copyToClipboard(content, "Details copied!")
                }
                .show()
        }

        private fun showActionDialog(title: String, actions: List<Pair<String, () -> Unit>>) {
            val actionNames = actions.map { it.first }.toTypedArray()

            androidx.appcompat.app.AlertDialog.Builder(itemView.context)
                .setTitle(title)
                .setItems(actionNames) { _, which ->
                    actions[which].second.invoke()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun exportBatchResults(item: TokenDisplayItem.BatchTokenize) {
            // Create exportable format
            val exportData = buildString {
                appendLine("# Vault SDK Batch Tokenization Results")
                appendLine("# Generated: ${dateFormat.format(Date())}")
                appendLine("# Source: ${if (item.isImported) item.fileName else "Sample Data"}")
                appendLine("# Data Type: ${item.inputDataType.displayName}")
                appendLine("# Total Items: ${item.results.size}")
                appendLine()
                appendLine("# Format: OriginalValue,Token,Status,DataType")

                item.results.forEach { result ->
                    val status = when {
                        result.exists -> "EXISTING"
                        result.newlyCreated -> "NEW"
                        else -> "UNKNOWN"
                    }
                    appendLine("${result.originalValue},${result.token},$status,${result.dataType}")
                }
            }

            copyToClipboard(exportData, "Batch results exported to clipboard!")
        }
    }
}

/**
 * Extension functions for enhanced formatting
 */
fun TokenDisplayItem.SingleTokenize.getFormattedSummary(): String {
    return "✅ ${inputDataType.displayName} tokenized successfully"
}

fun TokenDisplayItem.SingleDetokenize.getFormattedSummary(): String {
    return "✅ Token detokenized to ${outputDataType.displayName}"
}

fun TokenDisplayItem.BatchTokenize.getSuccessRate(): Float {
    return if (results.isNotEmpty()) {
        (summary.processedCount.toFloat() / results.size)
    } else 0f
}

fun TokenDisplayItem.BatchDetokenize.getFoundRate(): Float {
    return if (summary.processedCount > 0) {
        (summary.foundCount.toFloat() / summary.processedCount)
    } else 0f
}

fun TokenDisplayItem.FileImported.getValidationSummary(): String {
    val successRate = (validValues.toFloat() / totalValues * 100).toInt()
    return "✅ $validValues valid, ❌ $invalidValues invalid ($successRate% success)"
}

/**
 * Helper class for formatting different data types consistently
 */
object DataTypeFormatter {
    fun formatValue(value: Any?, dataType: MainActivity.DataType): String {
        if (value == null) return "null"

        return when (dataType) {
            MainActivity.DataType.STRING -> "\"$value\""
            MainActivity.DataType.INT -> value.toString()
            MainActivity.DataType.LONG -> "${value}L"
            MainActivity.DataType.FLOAT -> "${value}f"
            MainActivity.DataType.DOUBLE -> value.toString()
            MainActivity.DataType.BOOLEAN -> value.toString().lowercase()
        }
    }

    fun getTypeDescription(dataType: MainActivity.DataType): String {
        return when (dataType) {
            MainActivity.DataType.STRING -> "Text data (PII, emails, etc.)"
            MainActivity.DataType.INT -> "32-bit integers"
            MainActivity.DataType.LONG -> "64-bit long integers"
            MainActivity.DataType.FLOAT -> "32-bit floating point"
            MainActivity.DataType.DOUBLE -> "64-bit double precision"
            MainActivity.DataType.BOOLEAN -> "True/false values"
        }
    }

    fun getImportInstructions(dataType: MainActivity.DataType): String {
        return when (dataType) {
            MainActivity.DataType.STRING -> "Import text values separated by commas"
            MainActivity.DataType.INT -> "Import integers like: 123,456,789"
            MainActivity.DataType.LONG -> "Import long numbers like: 9876543210,1234567890"
            MainActivity.DataType.FLOAT -> "Import decimals like: 123.45,678.90"
            MainActivity.DataType.DOUBLE -> "Import precise decimals like: 123.456789,987.654321"
            MainActivity.DataType.BOOLEAN -> "Import boolean values: true,false,1,0,yes,no"
        }
    }
}