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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.chip.Chip

class MainActivity : AppCompatActivity() {

    private lateinit var vaultSDK: VaultSDK
    private lateinit var inputEditText: TextInputEditText
    private lateinit var tokenizeButton: MaterialButton
    private lateinit var detokenizeButton: MaterialButton
    private lateinit var batchTokenizeButton: MaterialButton
    private lateinit var batchDetokenizeButton: MaterialButton
    private lateinit var clearCacheButton: MaterialButton
    private lateinit var chipEncryption: Chip
    private lateinit var chipCache: Chip
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TokenResultAdapter

    private val tokenResults = mutableListOf<TokenDisplayItem>()

    // Sample data for batch operations
    private val samplePIIData = listOf(
        "555-12-3456",           // SSN
        "john.doe@example.com",  // Email
        "4111-2222-3333-4444",  // Credit Card
        "jane.smith@test.com",   // Another Email
        "987-65-4321",          // Another SSN
        "+1-555-123-4567"       // Phone Number
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeVaultSDK()
        setupRecyclerView()
        setupClickListeners()
        updateStatusChips()
    }

    private fun initializeViews() {
        inputEditText = findViewById(R.id.inputEditText)
        tokenizeButton = findViewById(R.id.tokenizeButton)
        detokenizeButton = findViewById(R.id.detokenizeButton)
        batchTokenizeButton = findViewById(R.id.batchTokenizeButton)
        batchDetokenizeButton = findViewById(R.id.batchDetokenizeButton)
        clearCacheButton = findViewById(R.id.clearCacheButton)
        chipEncryption = findViewById(R.id.chipEncryption)
        chipCache = findViewById(R.id.chipCache)
        recyclerView = findViewById(R.id.recyclerView)
    }

    private fun initializeVaultSDK() {
        try {
            vaultSDK = VaultSDK.getInstance()
            showToast("Vault SDK ready!")
        } catch (e: IllegalStateException) {
            showToast("Error: ${e.message}")
            // Disable all buttons if SDK is not initialized
            disableAllButtons()
        }
    }

    private fun setupRecyclerView() {
        adapter = TokenResultAdapter(tokenResults)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
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

        batchTokenizeButton.setOnClickListener {
            batchTokenizeValues()
        }

        batchDetokenizeButton.setOnClickListener {
            batchDetokenizeTokens()
        }

        clearCacheButton.setOnClickListener {
            clearCache()
        }
    }

    private fun tokenizeSingleValue(value: String) {
        showToast("Tokenizing: $value")
        addResult(TokenDisplayItem.Loading("Tokenizing '$value'..."))

        vaultSDK.tokenize(value) { result ->
            removeLastLoadingItem()
            when (result) {
                is TokenizeResult.Success -> {
                    val item = TokenDisplayItem.SingleTokenize(
                        originalValue = value,
                        token = result.token,
                        exists = result.exists,
                        newlyCreated = result.newlyCreated,
                        dataType = result.dataType ?: "unknown"
                    )
                    addResult(item)
                    showToast("✓ Tokenized successfully!")
                }
                is TokenizeResult.Error -> {
                    val item = TokenDisplayItem.Error("Tokenize Error", result.message)
                    addResult(item)
                    showToast("✗ Error: ${result.message}")
                }
            }
        }
    }

    private fun detokenizeSingleValue(token: String) {
        showToast("Detokenizing: $token")
        addResult(TokenDisplayItem.Loading("Detokenizing '$token'..."))

        vaultSDK.detokenize(token) { result ->
            removeLastLoadingItem()
            when (result) {
                is DetokenizeResult.Success -> {
                    val item = TokenDisplayItem.SingleDetokenize(
                        token = token,
                        originalValue = result.value ?: "null",
                        exists = result.exists,
                        dataType = result.dataType ?: "unknown"
                    )
                    addResult(item)
                    showToast("✓ Detokenized successfully!")
                }
                is DetokenizeResult.Error -> {
                    val item = TokenDisplayItem.Error("Detokenize Error", result.message)
                    addResult(item)
                    showToast("✗ Error: ${result.message}")
                }
            }
        }
    }

    private fun batchTokenizeValues() {
        showToast("Batch tokenizing ${samplePIIData.size} values")
        addResult(TokenDisplayItem.Loading("Batch tokenizing ${samplePIIData.size} values..."))

        vaultSDK.batchTokenize(samplePIIData) { result ->
            removeLastLoadingItem()
            when (result) {
                is BatchTokenizeResult.Success -> {
                    val item = TokenDisplayItem.BatchTokenize(
                        results = result.results,
                        summary = result.summary
                    )
                    addResult(item)
                    showToast("✓ Batch tokenized: ${result.summary.processedCount} items")
                }
                is BatchTokenizeResult.Error -> {
                    val item = TokenDisplayItem.Error("Batch Tokenize Error", result.message)
                    addResult(item)
                    showToast("✗ Batch Error: ${result.message}")
                }
            }
        }
    }

    private fun batchDetokenizeTokens() {
        // First, collect some tokens from previous tokenization results
        val availableTokens = tokenResults.mapNotNull { item ->
            when (item) {
                is TokenDisplayItem.SingleTokenize -> item.token
                is TokenDisplayItem.BatchTokenize -> item.results.firstOrNull()?.token
                else -> null
            }
        }.take(3) // Take first 3 tokens

        if (availableTokens.isEmpty()) {
            showToast("No tokens available. Please tokenize some values first.")
            return
        }

        showToast("Batch detokenizing ${availableTokens.size} tokens")
        addResult(TokenDisplayItem.Loading("Batch detokenizing ${availableTokens.size} tokens..."))

        vaultSDK.batchDetokenize(availableTokens) { result ->
            removeLastLoadingItem()
            when (result) {
                is BatchDetokenizeResult.Success -> {
                    val item = TokenDisplayItem.BatchDetokenize(
                        results = result.results,
                        summary = result.summary
                    )
                    addResult(item)
                    showToast("✓ Batch detokenized: ${result.summary.foundCount} found")
                }
                is BatchDetokenizeResult.Error -> {
                    val item = TokenDisplayItem.Error("Batch Detokenize Error", result.message)
                    addResult(item)
                    showToast("✗ Batch Error: ${result.message}")
                }
            }
        }
    }

    private fun clearCache() {
        try {
            vaultSDK.clearCache()
            addResult(TokenDisplayItem.CacheCleared(System.currentTimeMillis()))
            showToast("✓ Cache cleared successfully!")
        } catch (e: Exception) {
            showToast("✗ Error clearing cache: ${e.message}")
        }
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
        // Note: In a real implementation, you would get these values from the SDK
        chipEncryption.text = "Encryption: ON"
        chipCache.text = "Cache: ON"
    }

    private fun disableAllButtons() {
        tokenizeButton.isEnabled = false
        detokenizeButton.isEnabled = false
        batchTokenizeButton.isEnabled = false
        batchDetokenizeButton.isEnabled = false
        clearCacheButton.isEnabled = false
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}