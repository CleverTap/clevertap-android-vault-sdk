package com.clevertap.demo.ctvaultsdk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.clevertap.demo.ctvaultsdk.R
import com.clevertap.demo.ctvaultsdk.model.TokenDisplayItem
import java.text.SimpleDateFormat
import java.util.*

class TokenResultAdapter(
    private val items: List<TokenDisplayItem>
) : RecyclerView.Adapter<TokenResultAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

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
            timestampText.text = dateFormat.format(Date())

            when (item) {
                is TokenDisplayItem.Loading -> {
                    titleText.text = "⏳ Loading"
                    contentText.text = item.message
                    itemView.setBackgroundColor(0xFFFFF3C4.toInt()) // Light yellow
                }

                is TokenDisplayItem.SingleTokenize -> {
                    titleText.text = "🔐 Single Tokenize"
                    contentText.text = buildString {
                        append("Input: ${item.originalValue}\n")
                        append("Token: ${item.token}\n")
                        append("Status: ${if (item.exists) "Existing" else "New"}\n")
                        append("Type: ${item.dataType}")
                    }
                    itemView.setBackgroundColor(0xFFE8F5E8.toInt()) // Light green
                }

                is TokenDisplayItem.SingleDetokenize -> {
                    titleText.text = "🔓 Single Detokenize"
                    contentText.text = buildString {
                        append("Token: ${item.token}\n")
                        append("Value: ${item.originalValue}\n")
                        append("Exists: ${item.exists}\n")
                        append("Type: ${item.dataType}")
                    }
                    itemView.setBackgroundColor(0xFFE3F2FD.toInt()) // Light blue
                }

                is TokenDisplayItem.BatchTokenize -> {
                    titleText.text = "📦 Batch Tokenize"
                    contentText.text = buildString {
                        append("Summary:\n")
                        append("• Processed: ${item.summary.processedCount}\n")
                        append("• Existing: ${item.summary.existingCount}\n")
                        append("• New: ${item.summary.newlyCreatedCount}\n\n")
                        append("Results:\n")
                        item.results.take(3).forEach { result ->
                            append("• ${result.originalValue} → ${result.token}\n")
                        }
                        if (item.results.size > 3) {
                            append("... and ${item.results.size - 3} more")
                        }
                    }
                    itemView.setBackgroundColor(0xFFF3E5F5.toInt()) // Light purple
                }

                is TokenDisplayItem.BatchDetokenize -> {
                    titleText.text = "📋 Batch Detokenize"
                    contentText.text = buildString {
                        append("Summary:\n")
                        append("• Processed: ${item.summary.processedCount}\n")
                        append("• Found: ${item.summary.foundCount}\n")
                        append("• Not Found: ${item.summary.notFoundCount}\n\n")
                        append("Results:\n")
                        item.results.take(3).forEach { result ->
                            append("• ${result.token} → ${result.value ?: "null"}\n")
                        }
                        if (item.results.size > 3) {
                            append("... and ${item.results.size - 3} more")
                        }
                    }
                    itemView.setBackgroundColor(0xFFE0F2F1.toInt()) // Light teal
                }

                is TokenDisplayItem.Error -> {
                    titleText.text = "❌ ${item.title}"
                    contentText.text = item.message
                    itemView.setBackgroundColor(0xFFFFEBEE.toInt()) // Light red
                }

                is TokenDisplayItem.CacheCleared -> {
                    titleText.text = "🗑️ Cache Cleared"
                    contentText.text = "Token cache has been cleared successfully"
                    itemView.setBackgroundColor(0xFFFFF9C4.toInt()) // Light amber
                }
            }
        }
    }
}