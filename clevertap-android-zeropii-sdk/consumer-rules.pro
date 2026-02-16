
#  GSON MODELS - All network request/response classes
-keep class com.clevertap.android.zeropii.sdk.model.*Request { *; }
-keep class com.clevertap.android.zeropii.sdk.model.*Response { *; }
-keep public class com.clevertap.android.zeropii.sdk.model.Batch*Summary { *; }

# Auth provider interfaces (implemented by consumers)
-keep class com.clevertap.android.zeropii.sdk.auth.** { *; }
