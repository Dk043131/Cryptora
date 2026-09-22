# Cryptora Secure Chat Proguard Rules

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Retrofit & OkHttp
-dontwarn okio.**
-dontwarn retrofit2.Platform$Java8
-keepattributes Signature,Exceptions
-dontwarn javax.annotation.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Android Keystore & Crypto (Tink / Security Crypto)
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**

# Coroutines
-dontwarn kotlinx.coroutines.**

# Strip debug log calls in release builds to prevent plaintext leakage
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
