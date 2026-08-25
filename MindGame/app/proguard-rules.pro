# Project Specific ProGuard Rules
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Room Database Keep Rules
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# ViewModel & AndroidViewModel Keep Rules
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Project Models & Logic Classes
-keep class com.example.data.model.** { *; }
-keep class com.example.data.db.** { *; }
-keep class com.example.game.** { *; }
-keep class com.example.ui.theme.** { *; }

# Moshi & Retrofit Keep Rules
-keep class com.squareup.moshi.** { *; }
-keep class retrofit2.** { *; }
-dontwarn okio.**
-dontwarn com.squareup.moshi.**

# Kotlin Coroutines & Serialization
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Google Play Services Ads Keep Rules
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

