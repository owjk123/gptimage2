# Kotlin
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Gson
-keep class com.google.gson.** { *; }
-keep class com.gptimage2.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Compose
-dontwarn androidx.compose.**
