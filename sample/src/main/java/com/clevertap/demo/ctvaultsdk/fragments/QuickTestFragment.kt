package com.clevertap.demo.ctvaultsdk.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.clevertap.demo.ctvaultsdk.MainActivity
import com.clevertap.demo.ctvaultsdk.R
import com.clevertap.demo.ctvaultsdk.model.SampleDataProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Fragment for quick testing with random sample data
 */
class QuickTestFragment : Fragment(), BatchInputInterface {

    private lateinit var currentDataType: MainActivity.DataType

    // UI Components
    private lateinit var dataTypeInfoCard: MaterialCardView
    private lateinit var dataTypeTitle: TextView
    private lateinit var dataTypeDescription: TextView
    private lateinit var dataTypeExamples: TextView

    private lateinit var generateRandomButton: MaterialButton
    private lateinit var previewDataText: TextView
    private lateinit var itemCountText: TextView

    // Data
    private var currentBatchData: List<Any>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quick_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupClickListeners()

        // Initialize with default data type
        currentDataType = MainActivity.DataType.STRING
        updateUIForDataType()
    }

    private fun initializeViews(view: View) {
        dataTypeInfoCard = view.findViewById(R.id.dataTypeInfoCard)
        dataTypeTitle = view.findViewById(R.id.dataTypeTitle)
        dataTypeDescription = view.findViewById(R.id.dataTypeDescription)
        dataTypeExamples = view.findViewById(R.id.dataTypeExamples)

        generateRandomButton = view.findViewById(R.id.generateRandomButton)
        previewDataText = view.findViewById(R.id.previewDataText)
        itemCountText = view.findViewById(R.id.itemCountText)
    }

    private fun setupClickListeners() {
        generateRandomButton.setOnClickListener {
            generateRandomBatchData()
        }
    }

    private fun generateRandomBatchData() {
        currentBatchData = SampleDataProvider.getBatchSampleData(currentDataType)
        updatePreview()
    }

    private fun updatePreview() {
        currentBatchData?.let { data ->
            itemCountText.text = "${data.size} items"

            val preview = if (data.size <= 3) {
                data.joinToString("\n") { formatValueForDisplay(it) }
            } else {
                val first3 = data.take(3).joinToString("\n") { formatValueForDisplay(it) }
                "$first3\n... and ${data.size - 3} more items"
            }

            previewDataText.text = preview
            previewDataText.visibility = View.VISIBLE
        } ?: run {
            itemCountText.text = "0 items"
            previewDataText.visibility = View.GONE
        }
    }

    private fun formatValueForDisplay(value: Any): String {
        return when (currentDataType) {
            MainActivity.DataType.STRING -> "\"$value\""
            MainActivity.DataType.LONG -> "${value}L"
            MainActivity.DataType.FLOAT -> "${value}f"
            MainActivity.DataType.BOOLEAN -> value.toString().lowercase()
            else -> value.toString()
        }
    }

    private fun updateUIForDataType() {
        val dataTypeInfo = SampleDataProvider.getDataTypeInfo(currentDataType)

        dataTypeTitle.text = "${dataTypeInfo.icon} ${dataTypeInfo.name}"
        dataTypeDescription.text = dataTypeInfo.description

        val examplesText = "Examples: " + dataTypeInfo.examples.joinToString(", ") { "\"$it\"" }
        dataTypeExamples.text = examplesText

        // Clear current data when data type changes
        currentBatchData = null
        updatePreview()

        // Update generate button text
        generateRandomButton.text = "🎲 Generate Random ${dataTypeInfo.name} Batch"
    }

    // BatchInputInterface implementation
    override fun onDataTypeChanged(dataType: MainActivity.DataType) {
        currentDataType = dataType
        updateUIForDataType()
    }

    override fun getBatchData(): List<Any>? {
        return currentBatchData
    }

    override fun getTokensForDetokenization(): List<String>? {
        // Quick test doesn't handle tokens from previous results
        // This will be handled by the Import tab
        return null
    }

    override fun onTabSelected() {
        // Auto-generate data if none exists
        if (currentBatchData == null) {
            generateRandomBatchData()
        }
    }

    override fun onTabUnselected() {
        // Nothing specific needed
    }

    override fun clearData() {
        currentBatchData = null
        updatePreview()
    }
}