# ProGuard & R8 Optimization Rules for FleetOpt Enterprise Release

# Keep FleetOpt Domain Models & Entities for Room Database and JSON reflection
-keep class com.fleetopt.app.data.local.entity.** { *; }
-keep class com.fleetopt.app.core.engine.** { *; }
-keep class com.fleetopt.app.core.forecasting.** { *; }
-keep class com.fleetopt.app.core.physics.** { *; }
-keep class com.fleetopt.app.core.simulation.** { *; }
-keep class com.fleetopt.app.core.security.** { *; }
-keep class com.fleetopt.app.ui.viewmodel.** { *; }

# Room SQLite runtime
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Google Play Services & Maps
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.maps.**
-keep class com.google.maps.android.compose.** { *; }

# Jetpack Compose runtime
-keepclassmembers class * extends androidx.compose.runtime.State { *; }
-dontwarn androidx.compose.**

# Keep line numbers for meaningful stack traces in release crash reports
-keepattributes SourceFile,LineNumberTable
