package com.clevertap.demo.ctvaultsdk.fragments

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clevertap.demo.ctvaultsdk.MainActivity
import com.clevertap.demo.ctvaultsdk.R
import com.clevertap.demo.ctvaultsdk.model.TokenDisplayItem
import com.google.android.material.button.MaterialButton
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Fragment for importing data from files, clipboard, and previous results
 */
class ImportDataFragment : Fragment(), BatchInputInterface {

    private lateinit var currentDataType: MainActivity.DataType

    // UI Components
    private lateinit var chooseFileButton: MaterialButton
    private lateinit var fileStatusText: TextView
    private lateinit var pasteClipboardButton: MaterialButton
    private lateinit var clipboardPreviewText: TextView

    private lateinit var tokenSourceSpinner: Spinner
    private lateinit var useTokensButton: MaterialButton
    private lateinit var availableTokensRecycler: RecyclerView

    private lateinit var importPreviewText: TextView
    private lateinit var itemCountText: TextView
    private lateinit var clearImportButton: MaterialButton

    // Data
    private var importedData: List<Any>? = null
    private var availableTokensForDetokenization: List<String>? = null
    private var tokenResults: List<TokenDisplayItem> = emptyList()

    // File picker
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleFileImport(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_import_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupClickListeners()
        setupTokenSourceSpinner()

        // Initialize with default data type
        currentDataType = MainActivity.DataType.STRING
        updateUIForDataType()
    }

    private fun initializeViews(view: View) {
        chooseFileButton = view.findViewById(R.id.chooseFileButton)
        fileStatusText = view.findViewById(R.id.fileStatusText)
        pasteClipboardButton = view.findViewById(R.id.pasteClipboardButton)
        clipboardPreviewText = view.findViewById(R.id.clipboardPreviewText)

        tokenSourceSpinner = view.findViewById(R.id.tokenSourceSpinner)
        useTokensButton = view.findViewById(R.id.useTokensButton)
        availableTokensRecycler = view.findViewById(R.id.availableTokensRecycler)

        importPreviewText = view.findViewById(R.id.importPreviewText)
        itemCountText = view.findViewById(R.id.itemCountText)
        clearImportButton = view.findViewById(R.id.clearImportButton)

        // Setup recycler view
        availableTokensRecycler.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners() {
        chooseFileButton.setOnClickListener {
            openFilePicker()
        }

        pasteClipboardButton.setOnClickListener {
            handleClipboardImport()
        }

        useTokensButton.setOnClickListener {
            handleTokenReuse()
        }

        clearImportButton.setOnClickListener {
            clearData()
        }
    }

    private fun setupTokenSourceSpinner() {
        val tokenSources = arrayOf(
            "Previous Tokenization Results",
            "Previous Batch Results",
            "All Available Tokens"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            tokenSources
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tokenSourceSpinner.adapter = adapter
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/plain",
                "text/csv",
                "application/json",
                "application/octet-stream"
            ))
        }

        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Choose file to import"))
        } catch (e: Exception) {
            showToast("No file manager available")
        }
    }

    private fun handleFileImport(uri: Uri) {
        try {
            val contentResolver = requireContext().contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.readText()
            reader.close()

            if (content.isBlank()) {
                showToast("File is empty")
                fileStatusText.text = "❌ File is empty"
                return
            }

            // Try to parse the content
            parseImportedContent(content, "file")

            // Update file status
            val fileName = getFileName(uri)
            fileStatusText.text = "✅ Loaded: $fileName (${content.length} chars)"

        } catch (e: Exception) {
            showToast("Error reading file: ${e.message}")
            fileStatusText.text = "❌ Error reading file"
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "unknown"

        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex) ?: "unknown"
                }
            }
        }

        return fileName
    }

    private fun handleClipboardImport() {
        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboardManager.primaryClip

        if (clipData != null && clipData.itemCount > 0) {
            val clipboardText = clipData.getItemAt(0).text.toString()

            if (clipboardText.isNotBlank()) {
                parseImportedContent(clipboardText, "clipboard")

                // Show preview of clipboard content
                val preview = if (clipboardText.length > 200) {
                    "${clipboardText.take(200)}..."
                } else {
                    clipboardText
                }
                clipboardPreviewText.text = "📋 Clipboard: $preview"
                clipboardPreviewText.visibility = View.VISIBLE

                showToast("Imported ${clipboardText.length} characters from clipboard")
            } else {
                showToast("Clipboard is empty")
            }
        } else {
            showToast("Nothing in clipboard")
        }
    }

    private fun parseImportedContent(content: String, source: String) {
        try {
            // Try different parsing strategies
            val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

            // Strategy 1: Line-separated values
            if (lines.size > 1) {
                parseAndValidateData(lines, source)
                return
            }

            // Strategy 2: Comma-separated values
            val commaSeparated = content.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (commaSeparated.size > 1) {
                parseAndValidateData(commaSeparated, source)
                return
            }

            // Strategy 3: Space-separated values
            val spaceSeparated = content.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
            if (spaceSeparated.size > 1) {
                parseAndValidateData(spaceSeparated, source)
                return
            }

            // Strategy 4: JSON array
            if (content.trim().startsWith("[") && content.trim().endsWith("]")) {
                try {
                    // Simple JSON array parsing (not using full JSON parser)
                    val jsonContent = content.trim().removeSurrounding("[", "]")
                    val jsonItems = jsonContent.split(",").map {
                        it.trim().removeSurrounding("\"", "\"")
                    }.filter { it.isNotEmpty() }

                    if (jsonItems.isNotEmpty()) {
                        parseAndValidateData(jsonItems, source)
                        return
                    }
                } catch (e: Exception) {
                    // Fall through to single value
                }
            }

            // Strategy 5: Single value
            if (content.trim().isNotEmpty()) {
                parseAndValidateData(listOf(content.trim()), source)
            } else {
                showToast("No valid data found in $source")
            }

        } catch (e: Exception) {
            showToast("Error parsing $source: ${e.message}")
        }
    }

    private fun parseAndValidateData(rawValues: List<String>, source: String) {
        try {
            val convertedValues = mutableListOf<Any>()
            val errors = mutableListOf<String>()

            rawValues.forEachIndexed { index, rawValue ->
                try {
                    val convertedValue = convertValueToType(rawValue, currentDataType)
                    convertedValues.add(convertedValue)
                } catch (e: Exception) {
                    errors.add("Item ${index + 1}: '$rawValue'")
                }
            }

            if (convertedValues.isNotEmpty()) {
                importedData = convertedValues
                updatePreview()

                val successCount = convertedValues.size
                val errorCount = errors.size
                val totalCount = successCount + errorCount

                if (errorCount > 0) {
                    showToast("Imported $successCount/$totalCount items from $source ($errorCount errors)")
                } else {
                    showToast("Successfully imported $successCount items from $source")
                }
            } else {
                showToast("No valid ${currentDataType.displayName} values found in $source")
            }

        } catch (e: Exception) {
            showToast("Error converting data: ${e.message}")
        }
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
                else -> throw IllegalArgumentException("Not a valid boolean")
            }
        }
    }

    private fun handleTokenReuse() {
        if (tokenResults.isEmpty()) {
            showToast("No token results available. Perform some tokenization operations first.")
            return
        }

        val selectedSource = tokenSourceSpinner.selectedItemPosition
        val availableTokens = extractTokensFromResults(selectedSource)

        if (availableTokens.isNotEmpty()) {
            availableTokensForDetokenization = availableTokens
            showToast("Selected ${availableTokens.size} tokens for detokenization")
            updateTokensPreview()
        } else {
            showToast("No ${currentDataType.displayName} tokens found in previous results")
        }
    }

    private fun extractTokensFromResults(sourceType: Int): List<String> {
        val tokens = mutableListOf<String>()
        val targetDataTypeName = getDataTypeName(currentDataType)

        tokenResults.forEach { item ->
            when (item) {
                is TokenDisplayItem.SingleTokenize -> {
                    if (item.dataType.equals(targetDataTypeName, ignoreCase = true)) {
                        tokens.add(item.token)
                    }
                }
                is TokenDisplayItem.BatchTokenize -> {
                    item.results.forEach { result ->
                        if (result.dataType?.equals(targetDataTypeName, ignoreCase = true) == true) {
                            tokens.add(result.token)
                        }
                    }
                }
                else -> { /* Ignore other types */ }
            }
        }

        // Limit based on source type
        return when (sourceType) {
            0 -> tokens.take(10) // Previous Tokenization Results
            1 -> tokens.take(20) // Previous Batch Results
            2 -> tokens // All Available Tokens
            else -> tokens
        }
    }

    private fun getDataTypeName(dataType: MainActivity.DataType): String {
        return when (dataType) {
            MainActivity.DataType.STRING -> "string"
            MainActivity.DataType.INT -> "integer"
            MainActivity.DataType.LONG -> "long"
            MainActivity.DataType.FLOAT -> "float"
            MainActivity.DataType.DOUBLE -> "double"
            MainActivity.DataType.BOOLEAN -> "boolean"
        }
    }

    private fun updatePreview() {
        importedData?.let { data ->
            itemCountText.text = "${data.size} items imported"

            val preview = if (data.size <= 5) {
                data.joinToString("\n") { formatValueForDisplay(it) }
            } else {
                val first5 = data.take(5).joinToString("\n") { formatValueForDisplay(it) }
                "$first5\n... and ${data.size - 5} more items"
            }

            importPreviewText.text = preview
        } ?: run {
            itemCountText.text = "0 items"
            importPreviewText.text = "No data imported yet"
        }
    }

    private fun updateTokensPreview() {
        availableTokensForDetokenization?.let { tokens ->
            itemCountText.text = "${tokens.size} tokens selected"

            val preview = if (tokens.size <= 5) {
                tokens.joinToString("\n")
            } else {
                val first5 = tokens.take(5).joinToString("\n")
                "$first5\n... and ${tokens.size - 5} more tokens"
            }

            importPreviewText.text = preview
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
        // Clear imported data when data type changes
        importedData = null
        availableTokensForDetokenization = null
        updatePreview()

        // Update available tokens based on new data type
        if (tokenResults.isNotEmpty()) {
            val availableTokenCount = extractTokensFromResults(2).size // All tokens
            if (availableTokenCount > 0) {
                useTokensButton.text = "🔄 Use $availableTokenCount ${currentDataType.displayName} Tokens"
                useTokensButton.isEnabled = true
            } else {
                useTokensButton.text = "❌ No ${currentDataType.displayName} Tokens Available"
                useTokensButton.isEnabled = false
            }
        }
    }

    /**
     * Update available token results from MainActivity
     */
    fun updateTokenResults(results: List<TokenDisplayItem>) {
        tokenResults = results
        updateUIForDataType()
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
        return importedData
    }

    override fun getTokensForDetokenization(): List<String>? {
        return availableTokensForDetokenization
    }

    override fun onTabSelected() {
        // Refresh token availability when tab becomes visible
        updateUIForDataType()
    }

    override fun onTabUnselected() {
        // Nothing specific needed
    }

    override fun clearData() {
        importedData = null
        availableTokensForDetokenization = null
        fileStatusText.text = "📄 No file selected"
        clipboardPreviewText.visibility = View.GONE
        updatePreview()
    }
}