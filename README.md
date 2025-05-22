# CleverTap Vault SDK for Android

## Overview

CleverTap Vault SDK provides a secure way to tokenize Personally Identifiable Information (PII) in your Android applications. By replacing sensitive data with format-preserving tokens, you can minimize the exposure of sensitive information while maintaining data utility.

## Features

- **Tokenization & Detokenization**: Replace sensitive data with format-preserving tokens and retrieve original values when needed
- **Batch Operations**: Efficiently process multiple values at once
- **Secure Authentication**: OAuth2 client credentials flow
- **Data Encryption**: Optional encryption for enhanced security
- **In-Memory Caching**: Improve performance by caching token mappings
- **Automatic Token Refresh**: Handles authentication token expiration seamlessly

## Installation

Add the CleverTap Vault SDK to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'com.clevertap.android:clevertap-vault:1.0.0'
}
```

## Usage

### Initialize the SDK

Initialize the SDK in your Application class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        VaultSDK.initialize(
            clientId = "YOUR_CLIENT_ID",
            clientSecret = "YOUR_CLIENT_SECRET",
            apiUrl = "https://api.clevertap.com/vault/",
            authUrl = "https://auth.clevertap.com/",
            enableEncryption = true,
            enableCache = true,
            debugMode = BuildConfig.DEBUG
        )
    }
}
```

### Tokenize PII Data

Replace sensitive data with tokens:

```kotlin
val vaultSDK = VaultSDK.getInstance()

// Single value tokenization
vaultSDK.tokenize("555-12-3456") { result ->
    when (result) {
        is TokenizeResult.Success -> {
            val token = result.token
            // Store or use the token instead of the original value
            Log.d("VaultSDK", "Token: $token")
        }
        is TokenizeResult.Error -> {
            Log.e("VaultSDK", "Error: ${result.message}")
        }
    }
}

// Batch tokenization
val sensitiveValues = listOf("555-12-3456", "john.doe@example.com", "4111-2222-3333-4444")
vaultSDK.batchTokenize(sensitiveValues) { result ->
    when (result) {
        is BatchTokenizeResult.Success -> {
            val tokens = result.results.map { it.token }
            Log.d("VaultSDK", "Tokens: $tokens")
        }
        is BatchTokenizeResult.Error -> {
            Log.e("VaultSDK", "Error: ${result.message}")
        }
    }
}
```

### Retrieve Original Values

Get the original values from tokens when needed:

```kotlin
// Single token detokenization
vaultSDK.detokenize("555-67-8901") { result ->
    when (result) {
        is DetokenizeResult.Success -> {
            val originalValue = result.value
            Log.d("VaultSDK", "Original value: $originalValue")
        }
        is DetokenizeResult.Error -> {
            Log.e("VaultSDK", "Error: ${result.message}")
        }
    }
}

// Batch detokenization
val tokens = listOf("555-67-8901", "abc-def@example.com")
vaultSDK.batchDetokenize(tokens) { result ->
    when (result) {
        is BatchDetokenizeResult.Success -> {
            result.results.forEach { item ->
                Log.d("VaultSDK", "Token: ${item.token}, Value: ${item.value}")
            }
        }
        is BatchDetokenizeResult.Error -> {
            Log.e("VaultSDK", "Error: ${result.message}")
        }
    }
}
```

### Clear Token Cache

Clear the token cache when needed (e.g., user logout):

```kotlin
VaultSDK.getInstance().clearCache()
```

## Batch Size Limits

The SDK enforces the following batch size limits:

- Batch tokenization: Maximum 1,000 values per request
- Batch detokenization: Maximum 10,000 tokens per request

## Encryption

The SDK supports encrypting requests and responses for added security. Enable or disable encryption during initialization.

## Caching

The SDK provides in-memory caching of token mappings to improve performance. The cache is automatically cleared when the application process ends.

## Thread Safety

All SDK operations are thread-safe and run on a background thread, with callbacks delivered on the main thread.
