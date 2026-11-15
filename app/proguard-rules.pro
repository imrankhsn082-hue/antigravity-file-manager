# Add project specific ProGuard rules here.
# For more details, see https://developer.android.com/guide/developing/tools/proguard.html

# Keep the app entry point (referenced from the manifest).
-keep class com.antigravity.filemanager.MainActivity { *; }

# Keep generated BuildConfig fields used at runtime.
-keep class com.antigravity.filemanager.BuildConfig { *; }

# Preserve line numbers for readable crash reports while hiding the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin metadata is needed by reflection-based libraries (e.g. Coil, Compose).
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Jetpack Compose
-dontwarn androidx.compose.**

# Coil
-dontwarn coil.**
