# 1. PUBLIC API - Main SDK class and public result models
-keep public class com.clevertap.android.vault.sdk.VaultSDK { public *; }
-keep public class com.clevertap.android.vault.sdk.model.*Result** { *; }
-keep public class com.clevertap.android.vault.sdk.model.Batch*Item { *; }
-keep public class com.clevertap.android.vault.sdk.model.Batch*Summary { *; }
-keep public class com.clevertap.android.vault.sdk.util.VaultLogger$LogLevel { *; }

# 2. RETROFIT API INTERFACES
-keep interface com.clevertap.android.vault.sdk.api.** { *; }

# 3. GSON MODELS - All network request/response classes
-keep class com.clevertap.android.vault.sdk.model.*Request { *; }
-keep class com.clevertap.android.vault.sdk.model.*Response { *; }
-keep class com.clevertap.android.vault.sdk.model.AuthTokenResponse { *; }
-keep class com.clevertap.android.vault.sdk.model.Encrypted* { *; }


# 5. ENCRYPTION CLASSES
-keep class com.clevertap.android.vault.sdk.encryption.** { *; }

# 6. PRESERVE GSON SERIALIZATION
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 7. RETROFIT ANNOTATIONS
-keepattributes *Annotation*
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}