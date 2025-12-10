# CleverTap Vault SDK for Android

## Overview

CleverTap Vault SDK provides a secure way to tokenize Personally Identifiable Information (PII) in your Android applications. By replacing sensitive data with format-preserving tokens, you can minimize the exposure of sensitive information while maintaining data utility.

## Features

- **Tokenization & Detokenization**: Replace sensitive data with format-preserving tokens and retrieve original values when needed
- **Batch Operations**: Efficiently process multiple values at once
- **Secure Authentication**: OAuth2 client credentials flow
- **Encryption in Transit**: API request and response payloads are encrypted while being transmitted over the network
- **In-Memory Caching**: Improve performance by caching token mappings

## Requirements

### System Requirements
- **Minimum Android SDK**: 21
- **Compile Android SDK**: 36
- **Java Version**: 8 or higher

### Dependencies
The SDK automatically includes these dependencies:
```gradle
implementation 'com.squareup.retrofit2:retrofit:2.11.0'
implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
implementation 'com.google.code.gson:gson:2.13.1'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1'
```

### Permissions
No special permissions required. The SDK uses standard internet connectivity.

---

## Installation

Add the CleverTap Vault SDK to your app's `build.gradle` file:

```gradle
dependencies {
    implementation 'com.clevertap.android:clevertap-vault-sdk:1.0.0'
}
```

---

## Quick Start

### 1. Initialize the SDK

Initialize the SDK in your `Application` class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize with minimum required parameters
        VaultSDK.initialize(
            clientId = "YOUR_CLIENT_ID",
            clientSecret = "YOUR_CLIENT_SECRET", 
            apiUrl = "YOUR_API_URL",
            authUrl = "YOUR_AUTH_URL"
        )
        
        // Or with custom log level
        VaultSDK.initialize(
            clientId = "YOUR_CLIENT_ID",
            clientSecret = "YOUR_CLIENT_SECRET",
            apiUrl = "YOUR_API_URL",
            authUrl = "YOUR_AUTH_URL",
            logLevel = VaultLogger.LogLevel.DEBUG // Optional
        )
    }
}
```

> **Note**: You **must** call `initialize()` before using any other SDK methods. Calling `getInstance()` before initialization will throw `IllegalStateException`.

### 2. Basic Tokenization

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var vaultSDK: VaultSDK
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get SDK instance (after initialization)
        vaultSDK = VaultSDK.getInstance()
        
        // Tokenize sensitive data
        vaultSDK.tokenize("555-12-3456") { result ->
            when (result) {
                is TokenizeResult.Success -> {
                    val token = result.token
                    val wasExisting = result.exists
                    val isNew = result.newlyCreated
                    // Use the token instead of original value
                    Log.d("VaultSDK", "Token: $token")
                }
                is TokenizeResult.Error -> {
                    Log.e("VaultSDK", "Tokenization failed: ${result.message}")
                }
            }
        }
    }
}
```

### 3. Basic Detokenization

```kotlin
vaultSDK.deTokenizeAsString("555-67-8901") { result ->
    when (result) {
        is DetokenizeResult.Success -> {
            val originalValue:String = result.value
            val exists = result.exists
            Log.d("VaultSDK", "Original value: $originalValue")
        }
        is DetokenizeResult.Error -> {
            Log.e("VaultSDK", "Detokenization failed: ${result.message}")
        }
    }
}
```

### 4. Batch Tokenization

```kotlin
// Tokenize multiple sensitive values at once
val sensitiveData = listOf(
    "555-12-3456",
    "555-98-7654", 
    "555-11-2233",
    "555-44-5566"
)

vaultSDK.batchTokenizeStringValues(sensitiveData) { result ->
    when (result) {
        is BatchTokenizeResult.Success -> {
            val summary = result.summary
            Log.d("VaultSDK", "Processed: ${summary.processedCount}")
            Log.d("VaultSDK", "New tokens: ${summary.newlyCreatedCount}")
            Log.d("VaultSDK", "Existing tokens: ${summary.existingCount}")
            
            // Access individual results
            result.results.forEach { item ->
                Log.d("VaultSDK", "${item.originalValue} -> ${item.token}")
            }
        }
        is BatchTokenizeResult.Error -> {
            Log.e("VaultSDK", "Batch tokenization failed: ${result.message}")
        }
    }
}
```

### 5. Batch Detokenization

```kotlin
// Detokenize multiple tokens at once
val tokens = listOf(
    "TKN_555123456_001",
    "TKN_555987654_002",
    "TKN_555112233_003",
    "TKN_555445566_004"
)

vaultSDK.batchDeTokenizeAsString(tokens) { result ->
    when (result) {
        is BatchDetokenizeResult.Success -> {
            val summary = result.summary
            Log.d("VaultSDK", "Processed: ${summary.processedCount}")
            Log.d("VaultSDK", "Found: ${summary.foundCount}")
            Log.d("VaultSDK", "Not found: ${summary.notFoundCount}")
            
            // Access individual results
            result.results.forEach { item ->
                if (item.exists) {
                    Log.d("VaultSDK", "${item.token} -> ${item.value}")
                } else {
                    Log.d("VaultSDK", "${item.token} -> Not found")
                }
            }
        }
        is BatchDetokenizeResult.Error -> {
            Log.e("VaultSDK", "Batch detokenization failed: ${result.message}")
        }
    }
}
```

### **Note:**

- All tokenize/de-tokenize SDK APIs execute on background threads
- Callbacks are delivered on the main thread - Safe to update UI directly from callbacks
- *Batch Size Limits:* For tokenization, maximum 1,000 values per batch and for detokenization, maximum 10,000 tokens per batch

---

## API Reference

### Initialization

#### `initialize()`

```kotlin
fun initialize(
    clientId: String,
    clientSecret: String, 
    apiUrl: String,
    authUrl: String,
    logLevel: VaultLogger.LogLevel = VaultLogger.LogLevel.OFF
): VaultSDK
```

**Parameters:**
- `clientId` - Your OAuth2 client ID
- `clientSecret` - Your OAuth2 client secret
- `apiUrl` - Base URL for tokenization API
- `authUrl` - Base URL for authentication API
- `logLevel` - Logging verbosity level (optional)

#### `getInstance()`

```kotlin
fun getInstance(): VaultSDK
```

Returns the initialized SDK instance. Throws `IllegalStateException` if called before `initialize()`.

### Single Value Operations

#### Tokenization Methods

```kotlin
// String tokenization  
fun tokenize(value: String, callback: (TokenizeResult) -> Unit)

// Numeric tokenization
fun tokenize(value: Int, callback: (TokenizeResult) -> Unit)
fun tokenize(value: Long, callback: (TokenizeResult) -> Unit) 
fun tokenize(value: Float, callback: (TokenizeResult) -> Unit)
fun tokenize(value: Double, callback: (TokenizeResult) -> Unit)

// Boolean tokenization
fun tokenize(value: Boolean, callback: (TokenizeResult) -> Unit)
```

#### Detokenization Methods

```kotlin
// Type-specific detokenization
fun deTokenizeAsString(token: String, callback: (DetokenizeResult<String>) -> Unit)
fun deTokenizeAsInt(token: String, callback: (DetokenizeResult<Int>) -> Unit)
fun deTokenizeAsLong(token: String, callback: (DetokenizeResult<Long>) -> Unit)
fun deTokenizeAsFloat(token: String, callback: (DetokenizeResult<Float>) -> Unit)
fun deTokenizeAsDouble(token: String, callback: (DetokenizeResult<Double>) -> Unit) 
fun deTokenizeAsBoolean(token: String, callback: (DetokenizeResult<Boolean>) -> Unit)
```

### Batch Operations

#### Batch Tokenization

```kotlin
// Type-specific batch tokenization
fun batchTokenizeStringValues(values: List<String>, callback: (BatchTokenizeResult) -> Unit)
fun batchTokenizeIntValues(values: List<Int>, callback: (BatchTokenizeResult) -> Unit)
fun batchTokenizeLongValues(values: List<Long>, callback: (BatchTokenizeResult) -> Unit)
fun batchTokenizeFloatValues(values: List<Float>, callback: (BatchTokenizeResult) -> Unit)  
fun batchTokenizeDoubleValues(values: List<Double>, callback: (BatchTokenizeResult) -> Unit)
fun batchTokenizeBooleanValues(values: List<Boolean>, callback: (BatchTokenizeResult) -> Unit)
```

#### Batch Detokenization

```kotlin
// Type-specific batch detokenization
fun batchDeTokenizeAsString(tokens: List<String>, callback: (BatchDetokenizeResult<String>) -> Unit)
fun batchDeTokenizeAsInt(tokens: List<String>, callback: (BatchDetokenizeResult<Int>) -> Unit)
fun batchDeTokenizeAsLong(tokens: List<String>, callback: (BatchDetokenizeResult<Long>) -> Unit)
fun batchDeTokenizeAsFloat(tokens: List<String>, callback: (BatchDetokenizeResult<Float>) -> Unit)
fun batchDeTokenizeAsDouble(tokens: List<String>, callback: (BatchDetokenizeResult<Double>) -> Unit)
fun batchDeTokenizeAsBoolean(tokens: List<String>, callback: (BatchDetokenizeResult<Boolean>) -> Unit)
```

### Utility Methods

```kotlin
// Clear token cache
fun clearCache()
```

---

## Data Types

### Result Classes

#### `TokenizeResult`

```kotlin
sealed class TokenizeResult {
    data class Success(
        val token: String,        // Generated token
        val exists: Boolean,      // Token already existed
        val newlyCreated: Boolean,// Token was newly created
        val dataType: String?     // "string" or "number"
    ) : TokenizeResult()
    
    data class Error(
        val message: String       // Error description
    ) : TokenizeResult()
}
```

#### `DetokenizeResult<T>`

```kotlin
sealed class DetokenizeResult<T> {
    data class Success<T>(
        val value: T?,           // Original value
        val exists: Boolean,     // Token exists in system
        val dataType: String?    // "string" or "number"  
    ) : DetokenizeResult<T>()
    
    data class Error<T>(
        val message: String      // Error description
    ) : DetokenizeResult<T>()
}
```

#### `BatchTokenizeResult`

```kotlin
sealed class BatchTokenizeResult {
    data class Success(
        val results: List<BatchTokenItem>, // Individual results
        val summary: BatchTokenizeSummary  // Operation summary
    ) : BatchTokenizeResult()
    
    data class Error(
        val message: String               // Error description
    ) : BatchTokenizeResult()
}

data class BatchTokenItem(
    val originalValue: String,
    val token: String,
    val exists: Boolean,
    val newlyCreated: Boolean,
    val dataType: String?
)

data class BatchTokenizeSummary(
    val processedCount: Int,    // Total processed
    val existingCount: Int,     // Already existed  
    val newlyCreatedCount: Int  // Newly created
)
```

#### `BatchDetokenizeResult<T>`

```kotlin
sealed class BatchDetokenizeResult<T> {
    data class Success<T>(
        val results: List<BatchDetokenItem<T>>, // Individual results
        val summary: BatchDetokenizeSummary     // Operation summary
    ) : BatchDetokenizeResult<T>()
    
    data class Error<T>(
        val message: String                    // Error description
    ) : BatchDetokenizeResult<T>()
}

data class BatchDetokenItem<T>(
    val token: String,
    val value: T?,
    val exists: Boolean,
    val dataType: String?
)

data class BatchDetokenizeSummary(
    val processedCount: Int,  // Total processed
    val foundCount: Int,      // Successfully found
    val notFoundCount: Int    // Not found
)
```

### Log Levels

```kotlin
enum class LogLevel(val intValue: Int) {
    OFF(0),      // No logging, default value
    ERROR(1),    // Only errors  
    INFO(2),     // Errors + Info
    DEBUG(3),    // Errors + Info + Debug
    VERBOSE(4)   // All logs including verbose
}
```

## ProGuard/R8 Configuration

The SDK includes consumer ProGuard rules automatically.