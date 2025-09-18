package com.clevertap.demo.ctvaultsdk

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clevertap.android.vault.sdk.VaultSDK
import com.clevertap.android.vault.sdk.model.*
import com.clevertap.demo.ctvaultsdk.adapter.TokenResultAdapter
import com.clevertap.demo.ctvaultsdk.model.TokenDisplayItem
import com.clevertap.demo.ctvaultsdk.model.SampleDataProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.widget.TextView
import com.clevertap.demo.ctvaultsdk.MainActivity.DataType

class MainActivityBkp : AppCompatActivity() {

    private lateinit var vaultSDK: VaultSDK
    private lateinit var inputEditText: TextInputEditText
    private lateinit var dataTypeChipGroup: ChipGroup
    private lateinit var operationChipGroup: ChipGroup

    // Single Operation Buttons
    private lateinit var tokenizeButton: MaterialButton
    private lateinit var detokenizeButton: MaterialButton

    // Batch Operation Buttons
    private lateinit var batchTokenizeButton: MaterialButton
    private lateinit var batchDetokenizeButton: MaterialButton

    // Sample Data Buttons
    private lateinit var sampleStringButton: MaterialButton
    private lateinit var sampleIntButton: MaterialButton
    private lateinit var sampleLongButton: MaterialButton
    private lateinit var sampleFloatButton: MaterialButton
    private lateinit var sampleDoubleButton: MaterialButton
    private lateinit var sampleBooleanButton: MaterialButton

    // Utility Buttons
    private lateinit var clearCacheButton: MaterialButton
    private lateinit var clearResultsButton: MaterialButton

    // Status Elements
    private lateinit var chipEncryption: Chip
    private lateinit var chipCache: Chip
    private lateinit var statusText: TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TokenResultAdapter

    private val tokenResults = mutableListOf<TokenDisplayItem>()
    private var selectedDataType = DataType.STRING
    private var selectedOperation = Operation.SINGLE
/*
    enum class DataType(val displayName: String) {
        STRING("String"),
        INT("Int"),
        LONG("Long"),
        FLOAT("Float"),
        DOUBLE("Double"),
        BOOLEAN("Boolean")
    }*/

    enum class Operation(val displayName: String) {
        SINGLE("Single"),
        BATCH("Batch")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeVaultSDK()
        setupRecyclerView()
        setupChipGroups()
        setupClickListeners()
        updateStatusChips()
        updateUIForCurrentSelection()
    }

    private fun initializeViews() {
        inputEditText = findViewById(R.id.inputEditText)
        dataTypeChipGroup = findViewById(R.id.dataTypeChipGroup)
        operationChipGroup = findViewById(R.id.operationChipGroup)

        tokenizeButton = findViewById(R.id.tokenizeButton)
        detokenizeButton = findViewById(R.id.detokenizeButton)
        batchTokenizeButton = findViewById(R.id.batchTokenizeButton)
        batchDetokenizeButton = findViewById(R.id.batchDetokenizeButton)

        sampleStringButton = findViewById(R.id.sampleStringButton)
        sampleIntButton = findViewById(R.id.sampleIntButton)
        sampleLongButton = findViewById(R.id.sampleLongButton)
        sampleFloatButton = findViewById(R.id.sampleFloatButton)
        sampleDoubleButton = findViewById(R.id.sampleDoubleButton)
        sampleBooleanButton = findViewById(R.id.sampleBooleanButton)

        clearCacheButton = findViewById(R.id.clearCacheButton)
        clearResultsButton = findViewById(R.id.clearResultsButton)

        chipEncryption = findViewById(R.id.chipEncryption)
        chipCache = findViewById(R.id.chipCache)
        statusText = findViewById(R.id.statusText)

        recyclerView = findViewById(R.id.recyclerView)
    }

    private fun initializeVaultSDK() {
        try {
            vaultSDK = VaultSDK.getInstance()
            showToast("✅ Vault SDK ready!")
            statusText.text = "SDK Status: Ready"
        } catch (e: IllegalStateException) {
            showToast("❌ Error: ${e.message}")
            statusText.text = "SDK Status: Error - ${e.message}"
            disableAllButtons()
        }
    }

    private fun setupRecyclerView() {
        adapter = TokenResultAdapter(tokenResults)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupChipGroups() {
        // Setup Data Type Chips
        DataType.values().forEach { dataType ->
            val chip = Chip(this).apply {
                text = dataType.displayName
                isCheckable = true
                isChecked = dataType == selectedDataType
            }
            dataTypeChipGroup.addView(chip)
        }

        // Setup Operation Chips
        Operation.values().forEach { operation ->
            val chip = Chip(this).apply {
                text = operation.displayName
                isCheckable = true
                isChecked = operation == selectedOperation
            }
            operationChipGroup.addView(chip)
        }

        // Handle data type selection
        dataTypeChipGroup.setOnCheckedChangeListener { _, checkedId ->
            val checkedChip = findViewById<Chip>(checkedId)
            checkedChip?.let {
                selectedDataType = DataType.values().first { dt -> dt.displayName == checkedChip.text }
                updateUIForCurrentSelection()
                updateSampleDataHint()
            }
        }

        // Handle operation selection
        operationChipGroup.setOnCheckedChangeListener { _, checkedId ->
            val checkedChip = findViewById<Chip>(checkedId)
            checkedChip?.let {
                selectedOperation = Operation.values().first { op -> op.displayName == checkedChip.text }
                updateUIForCurrentSelection()
            }
        }
    }

    private fun setupClickListeners() {
        // Single operation buttons
        tokenizeButton.setOnClickListener {
            val input = inputEditText.text.toString().trim()
            if (input.isNotEmpty()) {
                tokenizeSingleValue(input)
            } else {
                showToast("Please enter a value to tokenize")
            }
        }

        detokenizeButton.setOnClickListener {
            val input = inputEditText.text.toString().trim()
            if (input.isNotEmpty()) {
                detokenizeSingleValue(input)
            } else {
                showToast("Please enter a token to detokenize")
            }
        }

        // Batch operation buttons
        batchTokenizeButton.setOnClickListener {
            batchTokenizeValues()
        }

        batchDetokenizeButton.setOnClickListener {
            batchDetokenizeTokens()
        }

        // Sample data buttons
        sampleStringButton.setOnClickListener {
            inputEditText.setText(SampleDataProvider.getRandomSampleData(DataType.STRING).toString())
        }
        sampleIntButton.setOnClickListener {
            inputEditText.setText(SampleDataProvider.getRandomSampleData(DataType.INT).toString())
        }
        sampleLongButton.setOnClickListener {
            inputEditText.setText(SampleDataProvider.getRandomSampleData(DataType.LONG).toString())
        }
        sampleFloatButton.setOnClickListener {
            inputEditText.setText(SampleDataProvider.getRandomSampleData(DataType.FLOAT).toString())
        }
        sampleDoubleButton.setOnClickListener {
            inputEditText.setText(SampleDataProvider.getRandomSampleData(DataType.DOUBLE).toString())
        }
        sampleBooleanButton.setOnClickListener {
            inputEditText.setText(SampleDataProvider.getRandomSampleData(DataType.BOOLEAN).toString())
        }

        // Utility buttons
        clearCacheButton.setOnClickListener {
            clearCache()
        }
        clearResultsButton.setOnClickListener {
            clearResults()
        }
    }

    private fun tokenizeSingleValue(input: String) {
        val parsedValue = parseInputValue(input) ?: return

        showToast("Tokenizing ${selectedDataType.displayName}: $input")
        addResult(TokenDisplayItem.Loading("Tokenizing $input as ${selectedDataType.displayName}..."))

        when (selectedDataType) {
            DataType.STRING -> {
                vaultSDK.tokenize(parsedValue as String) { result ->
                    handleTokenizeResult(input, result)
                }
            }
            DataType.INT -> {
                vaultSDK.tokenize(parsedValue as Int) { result ->
                    handleTokenizeResult(input, result)
                }
            }
            DataType.LONG -> {
                vaultSDK.tokenize(parsedValue as Long) { result ->
                    handleTokenizeResult(input, result)
                }
            }
            DataType.FLOAT -> {
                vaultSDK.tokenize(parsedValue as Float) { result ->
                    handleTokenizeResult(input, result)
                }
            }
            DataType.DOUBLE -> {
                vaultSDK.tokenize(parsedValue as Double) { result ->
                    handleTokenizeResult(input, result)
                }
            }
            DataType.BOOLEAN -> {
                vaultSDK.tokenize(parsedValue as Boolean) { result ->
                    handleTokenizeResult(input, result)
                }
            }
        }
    }

    private fun detokenizeSingleValue(token: String) {
        showToast("Detokenizing as ${selectedDataType.displayName}: $token")
        addResult(TokenDisplayItem.Loading("Detokenizing $token as ${selectedDataType.displayName}..."))

        when (selectedDataType) {
            DataType.STRING -> {
                vaultSDK.deTokenizeAsString(token) { result ->
                    handleDetokenizeResult(token, result)
                }
            }
            DataType.INT -> {
                vaultSDK.deTokenizeAsInt(token) { result ->
                    handleDetokenizeResult(token, result)
                }
            }
            DataType.LONG -> {
                vaultSDK.deTokenizeAsLong(token) { result ->
                    handleDetokenizeResult(token, result)
                }
            }
            DataType.FLOAT -> {
                vaultSDK.deTokenizeAsFloat(token) { result ->
                    handleDetokenizeResult(token, result)
                }
            }
            DataType.DOUBLE -> {
                vaultSDK.deTokenizeAsDouble(token) { result ->
                    handleDetokenizeResult(token, result)
                }
            }
            DataType.BOOLEAN -> {
                vaultSDK.deTokenizeAsBoolean(token) { result ->
                    handleDetokenizeResult(token, result)
                }
            }
        }
    }

    private fun batchTokenizeValues() {
        val sampleData = SampleDataProvider.getBatchSampleData(selectedDataType)

        showToast("Batch tokenizing ${sampleData.size} ${selectedDataType.displayName} values")
        addResult(TokenDisplayItem.Loading("Batch tokenizing ${sampleData.size} ${selectedDataType.displayName} values..."))

        when (selectedDataType) {
            DataType.STRING -> {
                vaultSDK.batchTokenizeStringValues(sampleData as List<String>) { result ->
                    handleBatchTokenizeResult(result)
                }
            }
            DataType.INT -> {
                vaultSDK.batchTokenizeIntValues(sampleData as List<Int>) { result ->
                    handleBatchTokenizeResult(result)
                }
            }
            DataType.LONG -> {
                vaultSDK.batchTokenizeLongValues(sampleData as List<Long>) { result ->
                    handleBatchTokenizeResult(result)
                }
            }
            DataType.FLOAT -> {
                vaultSDK.batchTokenizeFloatValues(sampleData as List<Float>) { result ->
                    handleBatchTokenizeResult(result)
                }
            }
            DataType.DOUBLE -> {
                vaultSDK.batchTokenizeDoubleValues(sampleData as List<Double>) { result ->
                    handleBatchTokenizeResult(result)
                }
            }
            DataType.BOOLEAN -> {
                vaultSDK.batchTokenizeBooleanValues(sampleData as List<Boolean>) { result ->
                    handleBatchTokenizeResult(result)
                }
            }
        }
    }

    private fun batchDetokenizeTokens() {
        // Collect tokens from previous results based on selected data type
        val availableTokens = tokenResults.mapNotNull { item ->
            when (item) {
                is TokenDisplayItem.SingleTokenize -> {
                    if (item.dataType.equals(getDataTypeName(selectedDataType), ignoreCase = true)) {
                        item.token
                    } else null
                }
                is TokenDisplayItem.BatchTokenize -> {
                    item.results.firstOrNull { result ->
                        result.dataType?.equals(getDataTypeName(selectedDataType), ignoreCase = true) == true
                    }?.token
                }
                else -> null
            }
        }.take(5) // Limit to 5 tokens for demo

        if (availableTokens.isEmpty()) {
            showToast("No ${selectedDataType.displayName} tokens available. Please tokenize some ${selectedDataType.displayName} values first.")
            return
        }

        showToast("Batch detokenizing ${availableTokens.size} ${selectedDataType.displayName} tokens")
        addResult(TokenDisplayItem.Loading("Batch detokenizing ${availableTokens.size} ${selectedDataType.displayName} tokens..."))

        when (selectedDataType) {
            DataType.STRING -> {
                vaultSDK.batchDeTokenizeAsString(availableTokens) { result ->
                    handleBatchDetokenizeResult(result)
                }
            }
            DataType.INT -> {
                vaultSDK.batchDeTokenizeAsInt(availableTokens) { result ->
                    handleBatchDetokenizeResult(result)
                }
            }
            DataType.LONG -> {
                vaultSDK.batchDeTokenizeAsLong(availableTokens) { result ->
                    handleBatchDetokenizeResult(result)
                }
            }
            DataType.FLOAT -> {
                vaultSDK.batchDeTokenizeAsFloat(availableTokens) { result ->
                    handleBatchDetokenizeResult(result)
                }
            }
            DataType.DOUBLE -> {
                vaultSDK.batchDeTokenizeAsDouble(availableTokens) { result ->
                    handleBatchDetokenizeResult(result)
                }
            }
            DataType.BOOLEAN -> {
                vaultSDK.batchDeTokenizeAsBoolean(availableTokens) { result ->
                    handleBatchDetokenizeResult(result)
                }
            }
        }
    }

    // Result handlers
    private fun handleTokenizeResult(originalValue: String, result: TokenizeResult) {
        removeLastLoadingItem()
        when (result) {
            is TokenizeResult.Success -> {
                val item = TokenDisplayItem.SingleTokenize(
                    originalValue = originalValue,
                    token = result.token,
                    exists = result.exists,
                    newlyCreated = result.newlyCreated,
                    dataType = result.dataType ?: "unknown",
                    inputDataType = selectedDataType
                )
                addResult(item)
                showToast("✅ Tokenized successfully!")
            }
            is TokenizeResult.Error -> {
                val item = TokenDisplayItem.Error("${selectedDataType.displayName} Tokenize Error", result.message)
                addResult(item)
                showToast("❌ Error: ${result.message}")
            }
        }
    }

    private fun <T> handleDetokenizeResult(token: String, result: DetokenizeResult<T>) {
        removeLastLoadingItem()
        when (result) {
            is DetokenizeResult.Success -> {
                val item = TokenDisplayItem.SingleDetokenize(
                    token = token,
                    originalValue = result.value?.toString() ?: "null",
                    exists = result.exists,
                    dataType = result.dataType ?: "unknown",
                    outputDataType = selectedDataType
                )
                addResult(item)
                showToast("✅ Detokenized successfully!")
            }
            is DetokenizeResult.Error -> {
                val item = TokenDisplayItem.Error("${selectedDataType.displayName} Detokenize Error", result.message)
                addResult(item)
                showToast("❌ Error: ${result.message}")
            }
        }
    }

    private fun handleBatchTokenizeResult(result: BatchTokenizeResult) {
        removeLastLoadingItem()
        when (result) {
            is BatchTokenizeResult.Success -> {
                val item = TokenDisplayItem.BatchTokenize(
                    results = result.results,
                    summary = result.summary,
                    inputDataType = selectedDataType
                )
                addResult(item)
                showToast("✅ Batch tokenized: ${result.summary.processedCount} ${selectedDataType.displayName} items")
            }
            is BatchTokenizeResult.Error -> {
                val item = TokenDisplayItem.Error("${selectedDataType.displayName} Batch Tokenize Error", result.message)
                addResult(item)
                showToast("❌ Batch Error: ${result.message}")
            }
        }
    }

    private fun <T> handleBatchDetokenizeResult(result: BatchDetokenizeResult<T>) {
        removeLastLoadingItem()
        when (result) {
            is BatchDetokenizeResult.Success -> {
                val item = TokenDisplayItem.BatchDetokenize(
                    results = result.results.map { batchItem ->
                        // Convert generic BatchDetokenItem<T> to BatchDetokenItem<String> for display
                        BatchDetokenItem(
                            token = batchItem.token,
                            value = batchItem.value?.toString(),
                            exists = batchItem.exists,
                            dataType = batchItem.dataType
                        )
                    },
                    summary = result.summary,
                    outputDataType = selectedDataType
                )
                addResult(item)
                showToast("✅ Batch detokenized: ${result.summary.foundCount} ${selectedDataType.displayName} found")
            }
            is BatchDetokenizeResult.Error -> {
                val item = TokenDisplayItem.Error("${selectedDataType.displayName} Batch Detokenize Error", result.message)
                addResult(item)
                showToast("❌ Batch Error: ${result.message}")
            }
        }
    }

    private fun clearCache() {
        try {
            vaultSDK.clearCache()
            addResult(TokenDisplayItem.CacheCleared(System.currentTimeMillis()))
            showToast("✅ Cache cleared successfully!")
        } catch (e: Exception) {
            showToast("❌ Error clearing cache: ${e.message}")
        }
    }

    private fun clearResults() {
        tokenResults.clear()
        adapter.notifyDataSetChanged()
        showToast("Results cleared")
    }

    // Helper methods
    private fun parseInputValue(input: String): Any? {
        return try {
            when (selectedDataType) {
                DataType.STRING -> input
                DataType.INT -> input.toInt()
                DataType.LONG -> input.toLong()
                DataType.FLOAT -> input.toFloat()
                DataType.DOUBLE -> input.toDouble()
                DataType.BOOLEAN -> input.toBoolean()
            }
        } catch (e: Exception) {
            showToast("❌ Invalid ${selectedDataType.displayName} format: $input")
            null
        }
    }

    private fun getDataTypeName(dataType: DataType): String {
        return when (dataType) {
            DataType.STRING -> "string"
            DataType.INT -> "integer"
            DataType.LONG -> "long"
            DataType.FLOAT -> "float"
            DataType.DOUBLE -> "double"
            DataType.BOOLEAN -> "boolean"
        }
    }

    private fun updateUIForCurrentSelection() {
        // Update button visibility based on operation type
        when (selectedOperation) {
            Operation.SINGLE -> {
                tokenizeButton.visibility = android.view.View.VISIBLE
                detokenizeButton.visibility = android.view.View.VISIBLE
                batchTokenizeButton.visibility = android.view.View.GONE
                batchDetokenizeButton.visibility = android.view.View.GONE
            }
            Operation.BATCH -> {
                tokenizeButton.visibility = android.view.View.GONE
                detokenizeButton.visibility = android.view.View.GONE
                batchTokenizeButton.visibility = android.view.View.VISIBLE
                batchDetokenizeButton.visibility = android.view.View.VISIBLE
            }
        }

        updateSampleDataHint()
    }

    private fun updateSampleDataHint() {
        val hint = when (selectedDataType) {
            DataType.STRING -> "Enter string value/Token "
            DataType.INT -> "Enter integer value (e.g., 123456)"
            DataType.LONG -> "Enter long value (e.g., 9876543210)"
            DataType.FLOAT -> "Enter float value (e.g., 123.45)"
            DataType.DOUBLE -> "Enter double value (e.g., 123.456789)"
            DataType.BOOLEAN -> "Enter boolean value (true or false)"
        }
        inputEditText.hint = hint
    }

    private fun addResult(item: TokenDisplayItem) {
        tokenResults.add(0, item) // Add to top
        adapter.notifyItemInserted(0)
        recyclerView.scrollToPosition(0)
    }

    private fun removeLastLoadingItem() {
        val lastIndex = tokenResults.indexOfFirst { it is TokenDisplayItem.Loading }
        if (lastIndex != -1) {
            tokenResults.removeAt(lastIndex)
            adapter.notifyItemRemoved(lastIndex)
        }
    }

    private fun updateStatusChips() {
        chipEncryption.text = "🔐 Encryption: ON"
        chipCache.text = "💾 Cache: ON"
    }

    private fun disableAllButtons() {
        tokenizeButton.isEnabled = false
        detokenizeButton.isEnabled = false
        batchTokenizeButton.isEnabled = false
        batchDetokenizeButton.isEnabled = false
        clearCacheButton.isEnabled = false
        clearResultsButton.isEnabled = false
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}