## CleverTap Vault SDK CHANGE LOG
### Version 1.1.0 (December 10, 2025)

#### 🎉 Initial Release

The CleverTap Vault SDK provides secure tokenization of Personally Identifiable Information (PII) for Android applications.

#### Features

##### Core Tokenization
- **Single Value Tokenization**: Convert sensitive data into format-preserving tokens
- **Single Value Detokenization**: Retrieve original values from tokens
- **Type-Safe APIs**: Support for multiple data types including `String`, `Int`, `Long`, `Float`, `Double`, and `Boolean`

##### Batch Operations
- **Batch Tokenization**: Process up to 1,000 values in a single request
- **Batch Detokenization**: Process up to 10,000 tokens in a single request
- **Detailed Results**: Get summary with processed count, success count, and failure count
