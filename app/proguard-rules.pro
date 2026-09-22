# Add project specific ProGuard rules here.

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.dettle.app.**$$serializer { *; }
-keepclassmembers class com.dettle.app.** { *** Companion; }
-keepclasseswithmembers class com.dettle.app.** { kotlinx.serialization.KSerializer serializer(...); }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Retrofit
-keepattributes Signature, Exceptions
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers class * { @javax.inject.Inject <fields>; }

# WebView JavaScript Interface (critical — must not be renamed)
-keepclassmembers class com.dettle.app.data.webview.** {
    @android.webkit.JavascriptInterface public *;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep data classes for serialization
-keep class com.dettle.app.domain.model.** { *; }
-keep class com.dettle.app.data.api.** { *; }
