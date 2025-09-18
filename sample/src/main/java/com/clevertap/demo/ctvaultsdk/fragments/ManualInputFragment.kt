package com.clevertap.demo.ctvaultsdk.fragments

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.clevertap.demo.ctvaultsdk.MainActivity
import com.clevertap.demo.ctvaultsdk.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

/**
 * Fragment for manual data input with various parsing options
 */
class ManualInputFragment : Fragment(), BatchInputInterface {

    private lateinit var currentDataType: MainActivity.DataType

    // UI Components
    private lateinit var inputFormatChipGroup: ChipGroup
    private lateinit var chipCommaSeparated: Chip
    private lateinit var chipLineSeparated: Chip
    private lateinit var chipSpaceSeparated: Chip

    private lateinit var manualInputEditText: TextInputEditText
    private lateinit var pasteClipboardButton: MaterialButton
    private lateinit var clearAllButton: MaterialButton
    private lateinit var addSampleButton: MaterialButton

    private lateinit var validationStatusText: TextView
    private lateinit var itemCountText: TextView
    private lateinit var previewText: TextView

    // Data
    private var currentInputFormat = InputFormat.COMMA_SEPARATED
    private var parsedData: List<Any>? = null

    enum class InputFormat(val displayName: String, val separator: String) {
        COMMA_SEPARATED("Comma", ","),
        LINE_SEPARATED("Line", "\n"),
        SPACE_SEPARATED("Space", " ")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manual_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupClickListeners()
        setupTextWatcher()

        // Initialize with default data type
        currentDataType = MainActivity.DataType.STRING
        updateUIForDataType()
    }

    private fun initializeViews(view: View) {
        inputFormatChipGroup = view.findViewById(R.id.inputFormatChipGroup)
        chipCommaSeparated = view.findViewById(R.id.chipCommaSeparated)
        chipLineSeparated = view.findViewById(R.id.chipLineSeparated)
        chipSpaceSeparated = view.findViewById(R.id.chipSpaceSeparated)

        manualInputEditText = view.findViewById(R.id.manualInputEditText)
        pasteClipboardButton = view.findViewById(R.id.pasteClipboardButton)
        clearAllButton = view.findViewById(R.id.clearAllButton)
        addSampleButton = view.findViewById(R.id.addSampleButton)

        validationStatusText = view.findViewById(R.id.validationStatusText)
        itemCountText = view.findViewById(R.id.itemCountText)
        previewText = view.findViewById(R.id.previewText)

        // Set default selection
        chipCommaSeparated.isChecked = true
    }

    private fun setupClickListeners() {
        inputFormatChipGroup.setOnCheckedChangeListener { _, checkedId ->
            currentInputFormat = when (checkedId) {
                R.id.chipCommaSeparated -> InputFormat.COMMA_SEPARATED
                R.id.chipLineSeparated -> InputFormat.LINE_SEPARATED
                R.id.chipSpaceSeparated -> InputFormat.SPACE_SEPARATED
                else -> InputFormat.COMMA_SEPARATED
            }
            parseAndValidateInput()
        }

        pasteClipboardButton.setOnClickListener {
            pasteFromClipboard()
        }

        clearAllButton.setOnClickListener {
            clearData()
        }

        addSampleButton.setOnClickListener {
            addSampleData()
        }
    }

    private fun setupTextWatcher() {
        manualInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                parseAndValidateInput()
            }
        })
    }

    private fun parseAndValidateInput() {
        val inputText = manualInputEditText.text.toString().trim()

        if (inputText.isEmpty()) {
            parsedData = null
            updateValidationStatus("Enter data to validate", ValidationStatus.EMPTY)
            return
        }

        try {
            val rawValues = when (currentInputFormat) {
                InputFormat.LINE_SEPARATED -> inputText.split("\n")
                else -> inputText.split(currentInputFormat.separator)
            }.map { it.trim() }.filter { it.isNotEmpty() }

            if (rawValues.isEmpty()) {
                parsedData = null
                updateValidationStatus("No valid items found", ValidationStatus.ERROR)
                return
            }

            // Validate and convert based on data type
            val convertedValues = mutableListOf<Any>()
            val errors = mutableListOf<String>()

            rawValues.forEachIndexed { index, rawValue ->
                try {
                    val convertedValue = convertValueToType(rawValue, currentDataType)
                    convertedValues.add(convertedValue)
                } catch (e: Exception) {
                    errors.add("Item ${index + 1}: '$rawValue' - ${e.message}")
                }
            }

            if (errors.isNotEmpty()) {
                parsedData = null
                updateValidationStatus("${errors.size} validation error(s):\n${errors.take(3).joinToString("\n")}", ValidationStatus.ERROR)
            } else {
                parsedData = convertedValues
                updateValidationStatus("✅ All ${convertedValues.size} items valid", ValidationStatus.SUCCESS)
            }

        } catch (e: Exception) {
            parsedData = null
            updateValidationStatus("Parsing error: ${e.message}", ValidationStatus.ERROR)
        }

        updatePreview()
    }

    private fun convertValueToType(value: String, dataType: MainActivity.DataType): Any {
        return when (dataType) {
            MainActivity.DataType.STRING -> value
            MainActivity.DataType.INT -> value.toIntOrNull() ?: throw IllegalArgumentException("Not a valid integer")
            MainActivity.DataType.LONG -> value.toLongOrNull() ?: throw IllegalArgumentException("Not a valid long")
            MainActivity.DataType.FLOAT -> value.toFloatOrNull() ?: throw IllegalArgumentException("Not a valid float")
            MainActivity.DataType.DOUBLE -> value.toDoubleOrNull() ?: throw IllegalArgumentException("Not a valid double")
            MainActivity.DataType.BOOLEAN -> when (value.lowercase()) {
                "true", "1", "yes", "y" -> true
                "false", "0", "no", "n" -> false
                else -> throw IllegalArgumentException("Not a valid boolean (use true/false)")
            }
        }
    }

    private fun updateValidationStatus(message: String, status: ValidationStatus) {
        validationStatusText.text = message
        validationStatusText.setTextColor(
            resources.getColor(
                when (status) {
                    ValidationStatus.SUCCESS -> android.R.color.holo_green_dark
                    ValidationStatus.ERROR -> android.R.color.holo_red_dark
                    ValidationStatus.EMPTY -> android.R.color.darker_gray
                }, null
            )
        )
    }

    private fun updatePreview() {
        parsedData?.let { data ->
            itemCountText.text = "${data.size} items"

            val preview = if (data.size <= 5) {
                data.joinToString("\n") { formatValueForDisplay(it) }
            } else {
                val first5 = data.take(5).joinToString("\n") { formatValueForDisplay(it) }
                "$first5\n... and ${data.size - 5} more items"
            }

            previewText.text = preview
        } ?: run {
            itemCountText.text = "0 items"
            previewText.text = "No valid data to preview"
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

    private fun pasteFromClipboard() {
        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboardManager.primaryClip

        if (clipData != null && clipData.itemCount > 0) {
            val pastedText = clipData.getItemAt(0).text.toString()

            if (pastedText.isNotBlank()) {
                manualInputEditText.setText(pastedText)
                showToast("Pasted ${pastedText.length} characters from clipboard")
            } else {
                showToast("Clipboard is empty")
            }
        } else {
            showToast("Nothing to paste from clipboard")
        }
    }

    private fun addSampleData() {
        val sampleValues = when (currentDataType) {
            MainActivity.DataType.STRING -> listOf("sample@email.com", "555-12-3456", "+1-555-123-4567")
            MainActivity.DataType.INT -> listOf("123", "456", "789")
            MainActivity.DataType.LONG -> listOf("9876543210", "1234567890", "5555666677")
            MainActivity.DataType.FLOAT -> listOf("123.45", "678.90", "999.99")
            MainActivity.DataType.DOUBLE -> listOf("123.456789", "987.654321", "555.123456")
            MainActivity.DataType.BOOLEAN -> listOf("true", "false", "true")
        }

        val sampleText = when (currentInputFormat) {
            InputFormat.LINE_SEPARATED -> sampleValues.joinToString("\n")
            else -> sampleValues.joinToString(currentInputFormat.separator)
        }

        val currentText = manualInputEditText.text.toString().trim()
        val newText = if (currentText.isEmpty()) {
            sampleText
        } else {
            when (currentInputFormat) {
                InputFormat.LINE_SEPARATED -> "$currentText\n$sampleText"
                else -> "$currentText${currentInputFormat.separator}$sampleText"
            }
        }

        manualInputEditText.setText(newText)
        showToast("Added ${sampleValues.size} sample ${currentDataType.displayName} values")
    }

    private fun updateUIForDataType() {
        // Update hint based on data type and format
        updateInputHint()

        // Clear and revalidate current input
        parseAndValidateInput()
    }

    private fun updateInputHint() {
        val separator = when (currentInputFormat) {
            InputFormat.COMMA_SEPARATED -> ", "
            InputFormat.LINE_SEPARATED -> "\n"
            InputFormat.SPACE_SEPARATED -> " "
        }

        val hint = when (currentDataType) {
            MainActivity.DataType.STRING -> "Enter strings separated by ${currentInputFormat.displayName.lowercase()}${separator}e.g. john@email.com${separator}555-12-3456"
            MainActivity.DataType.INT -> "Enter integers separated by ${currentInputFormat.displayName.lowercase()}${separator}e.g. 123${separator}456"
            MainActivity.DataType.LONG -> "Enter long values separated by ${currentInputFormat.displayName.lowercase()}${separator}e.g. 9876543210${separator}1234567890"
            MainActivity.DataType.FLOAT -> "Enter floats separated by ${currentInputFormat.displayName.lowercase()}${separator}e.g. 123.45${separator}678.90"
            MainActivity.DataType.DOUBLE -> "Enter doubles separated by ${currentInputFormat.displayName.lowercase()}${separator}e.g. 123.456789${separator}987.654321"
            MainActivity.DataType.BOOLEAN -> "Enter booleans separated by ${currentInputFormat.displayName.lowercase()}${separator}e.g. true${separator}false"
        }

        manualInputEditText.hint = hint
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // BatchInputInterface implementation
    override fun onDataTypeChanged(dataType: MainActivity.DataType) {
        currentDataType = dataType
        updateUIForDataType()
    }

    override fun getBatchData(): List<Any>? {
        return parsedData
    }

    override fun getTokensForDetokenization(): List<String>? {
        // Manual input doesn't provide tokens for detokenization
        return null
    }

    override fun onTabSelected() {
        // Focus on input field when tab becomes visible
        manualInputEditText.requestFocus()
    }

    override fun onTabUnselected() {
        // Nothing specific needed
    }

    override fun clearData() {
        manualInputEditText.setText("")
        parsedData = null
        updateValidationStatus("Enter data to validate", ValidationStatus.EMPTY)
        updatePreview()
    }

    enum class ValidationStatus {
        SUCCESS, ERROR, EMPTY
    }
}