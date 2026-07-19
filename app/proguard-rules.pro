# Add project specific ProGuard rules here.

# Keep data classes used for JSON serialization (calculator inputs/results, DataStore models)
-keepclassmembers class com.financeplanner.app.domain.model.** {
    <fields>;
}

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt / Dagger — generated code, standard keep rules already applied by the Hilt
# Gradle plugin; nothing extra needed here unless custom reflection is introduced.

# Strip debug-only logging in release builds (security requirement — no sensitive
# data or tokens should reach Logcat in production)
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
