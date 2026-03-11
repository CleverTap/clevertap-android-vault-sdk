# CleverTap ZeroPii SDK for Android

## Overview

CleverTap ZeroPii SDK provides a secure way to tokenize Personally Identifiable Information (PII) in your Android applications. By replacing sensitive data with format-preserving tokens, you can minimize the exposure of sensitive information while maintaining data utility.

## Features

- **Tokenization**: Replace sensitive data with format-preserving tokens
- **Batch Operations**: Efficiently tokenize multiple values at once (up to 1,000 per batch)
- **Pluggable Authentication**: Bring your own token provider via the `AccessTokenProvider` interface
- **Encryption in Transit**: API request and response payloads are encrypted using AES-256-GCM
- **Pluggable Retry Policy**: Customize retry behavior or use the built-in exponential backoff

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

Add the CleverTap ZeroPii SDK to your app's `build.gradle` file:

```gradle
dependencies {
    implementation 'com.clevertap.android:clevertap-zeropii-sdk:1.0.0'
}
```

---

## Quick Start

### 1. Implement AccessTokenProvider

The SDK requires you to supply access tokens via the `AccessTokenProvider` interface. This gives you full control over how tokens are fetched (OAuth2, custom auth, etc.):

```kotlin
class MyTokenProvider : AccessTokenProvider {
    override fun fetchToken(callback: AccessTokenCallback) {
        // Fetch token from your auth server
        // Call exactly ONE of onSuccess or onFailure
        try {
            val token = "..." // your token fetching logic
            val expiresIn = 3600L // token lifetime in seconds
            callback.onSuccess(AccessTokenInfo(token, expiresIn))
        } catch (e: Exception) {
            callback.onFailure(e)
        }
    }
}
```

### 2. Initialize the SDK

Initialize the SDK in your `Application` class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize with minimum required parameters
        ZeroPiiSDK.initialize(
            tokenProvider = MyTokenProvider(),
            apiUrl = "YOUR_API_URL"
        )

        // Or with custom log level
        ZeroPiiSDK.initialize(
            tokenProvider = MyTokenProvider(),
            apiUrl = "YOUR_API_URL",
            logLevel = ZeroPiiLogger.LogLevel.DEBUG
        )
    }
}
```

> **Note**: You **must** call `initialize()` before using any other SDK methods. Calling `getInstance()` before initialization will throw `IllegalStateException`.

### 3. Basic Tokenization

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var zeroPiiSDK: ZeroPiiSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get SDK instance (after initialization)
        zeroPiiSDK = ZeroPiiSDK.getInstance()

        // Tokenize sensitive data
        zeroPiiSDK.tokenize("555-12-3456") { result ->
            when (result) {
                is TokenizeResult.Success -> {
                    val token = result.token
                    val wasExisting = result.exists
                    val isNew = result.newlyCreated
                    // Use the token instead of original value
                    Log.d("ZeroPiiSDK", "Token: $token")
                }
                is TokenizeResult.Error -> {
                    Log.e("ZeroPiiSDK", "Tokenization failed: ${result.message}")
                }
            }
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

zeroPiiSDK.batchTokenizeStringValues(sensitiveData) { result ->
    when (result) {
        is BatchTokenizeResult.Success -> {
            val summary = result.summary
            Log.d("ZeroPiiSDK", "Processed: ${summary.processedCount}")
            Log.d("ZeroPiiSDK", "New tokens: ${summary.newlyCreatedCount}")
            Log.d("ZeroPiiSDK", "Existing tokens: ${summary.existingCount}")

            // Access individual results
            result.results.forEach { item ->
                Log.d("ZeroPiiSDK", "${item.originalValue} -> ${item.token}")
            }
        }
        is BatchTokenizeResult.Error -> {
            Log.e("ZeroPiiSDK", "Batch tokenization failed: ${result.message}")
        }
    }
}
```

### **Note:**

- All SDK APIs execute on background threads
- Callbacks are delivered on the main thread — safe to update UI directly from callbacks
- *Batch Size Limit:* Maximum 1,000 values per batch

---

## API Reference

### Initialization

#### `initialize()`

```kotlin
fun initialize(
    tokenProvider: AccessTokenProvider,
    apiUrl: String,
    logLevel: ZeroPiiLogger.LogLevel = ZeroPiiLogger.LogLevel.OFF,
    retryPolicy: RetryPolicy = /* default exponential backoff policy */
): ZeroPiiSDK
```

**Parameters:**
- `tokenProvider` - Your implementation of `AccessTokenProvider` that supplies access tokens
- `apiUrl` - Base URL for the tokenization API
- `logLevel` - Logging verbosity level (optional, defaults to `OFF`)
- `retryPolicy` - Custom retry policy (optional, defaults to built-in exponential backoff policy)

#### `getInstance()`

```kotlin
fun getInstance(): ZeroPiiSDK
```

Returns the initialized SDK instance. Throws `IllegalStateException` if called before `initialize()`.

### Authentication

#### `AccessTokenProvider`

```kotlin
interface AccessTokenProvider {
    fun fetchToken(callback: AccessTokenCallback)
}
```

Implement this interface to supply access tokens to the SDK. The SDK calls `fetchToken` when it needs a token (first request, token expired, or after a 401 response).

#### `AccessTokenCallback`

```kotlin
interface AccessTokenCallback {
    fun onSuccess(tokenInfo: AccessTokenInfo)
    fun onFailure(error: Exception)
}
```

Call exactly **one** of `onSuccess` or `onFailure` in your `fetchToken` implementation.

#### `AccessTokenInfo`

```kotlin
data class AccessTokenInfo(
    val token: String,
    val expiresInSeconds: Long
)
```

- `token` - The access token string (e.g., a Bearer token)
- `expiresInSeconds` - Token lifetime in seconds from now

### Retry Policy

#### `RetryPolicy`

```kotlin
interface RetryPolicy {
    fun shouldRetry(attempt: Int, httpStatusCode: Int?): Boolean
    fun retryDelayMs(attempt: Int): Long
}
```

Implement this interface to customize retry behavior.

- `shouldRetry` - Called after a failed request. `attempt` is 0-based (0 = after first failure). `httpStatusCode` is `null` for network errors (timeout, no connectivity).
- `retryDelayMs` - Delay in milliseconds before the retry attempt.

> **Note**: 401 Unauthorized responses bypass `RetryPolicy` entirely — the SDK automatically refreshes the token and retries immediately.

#### Default Behavior

If no custom `retryPolicy` is provided, the SDK uses a built-in exponential backoff policy:
- Retries on server errors (500, 502, 503, 504), rate limiting (429), and network errors
- Does not retry on client errors (4xx except 401)
- Exponential backoff delays: 2s, 4s, 8s, ...
- Maximum 1 retry by default

#### Custom Retry Policy Example

Here's an example of a linear retry policy that retries up to 3 times with a fixed 2-second delay:

```kotlin
class LinearRetryPolicy(private val maxRetries: Int = 3) : RetryPolicy {

    private val retryableStatusCodes = setOf(500, 502, 503, 504, 429)

    override fun shouldRetry(attempt: Int, httpStatusCode: Int?): Boolean {
        if (attempt >= maxRetries) return false
        // Retry on network errors (null) or server errors
        return httpStatusCode == null || httpStatusCode in retryableStatusCodes
    }

    override fun retryDelayMs(attempt: Int): Long {
        return 2000L // Fixed 2-second delay
    }
}

// Use it during initialization
ZeroPiiSDK.initialize(
    tokenProvider = MyTokenProvider(),
    apiUrl = "YOUR_API_URL",
    retryPolicy = LinearRetryPolicy(maxRetries = 3)
)
```

### Single Value Tokenization

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

### Batch Tokenization

```kotlin
fun batchTokenizeStringValues(values: List<String>, callback: (BatchTokenizeResult) -> Unit)
fun batchTokenizeIntValues(values: List<Int>, callback: (BatchTokenizeResult) -> Unit)
fun batchTokenizeLongValues(values: List<Long>, callback: (BatchTokenizeResult) -> Unit)
fun batchTokenizeFloatValues(values: List<Float>, callback: (BatchTokenizeResult) -> Unit)
fun batchTokenizeDoubleValues(values: List<Double>, callback: (BatchTokenizeResult) -> Unit)
fun batchTokenizeBooleanValues(values: List<Boolean>, callback: (BatchTokenizeResult) -> Unit)
```

---

## Data Types

### Supported Types

The SDK supports tokenization of the following data types:

| Type | Single | Batch |
|------|--------|-------|
| `String` | `tokenize(value: String, ...)` | `batchTokenizeStringValues(...)` |
| `Int` | `tokenize(value: Int, ...)` | `batchTokenizeIntValues(...)` |
| `Long` | `tokenize(value: Long, ...)` | `batchTokenizeLongValues(...)` |
| `Float` | `tokenize(value: Float, ...)` | `batchTokenizeFloatValues(...)` |
| `Double` | `tokenize(value: Double, ...)` | `batchTokenizeDoubleValues(...)` |
| `Boolean` | `tokenize(value: Boolean, ...)` | `batchTokenizeBooleanValues(...)` |

### Result Classes

#### `TokenizeResult`

```kotlin
sealed class TokenizeResult {
    data class Success(
        val token: String,         // Generated token
        val exists: Boolean,       // Token already existed
        val newlyCreated: Boolean, // Token was newly created
        val dataType: String?      // "string" or "number"
    ) : TokenizeResult()

    data class Error(
        val message: String,       // Error description
        val httpStatusCode: Int?   // HTTP status code (null for network/SDK errors)
    ) : TokenizeResult()
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
        val message: String,               // Error description
        val httpStatusCode: Int?           // HTTP status code (null for network/SDK errors)
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
