# =========================================================================
# ProGuard / R8 Rules for DroidIsland Release Build (Google Play Ready)
# =========================================================================

# 1. Android Jetpack Compose Runtime and Compiler
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature

# 2. Android Lifecycle and ViewModel
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# 3. Android DataStore Preferences & Kotlin Serialization
-keep class androidx.datastore.preferences.** { *; }
-keep class com.droidisland.app.data.** { *; }
-keepclassmembers class com.droidisland.app.data.IslandConfig { *; }

# 4. App Models (MVI IslandState, BluetoothHeadphone, MusicTrack)
-keep class com.droidisland.app.model.** { *; }
-keepclassmembers class com.droidisland.app.model.** { *; }
-keep enum com.droidisland.app.model.** { *; }

# 5. Core Services and Broadcast Receivers (Must survive R8 minification)
-keep class com.droidisland.app.service.IslandOverlayService { *; }
-keep class com.droidisland.app.service.IslandAccessibilityService { *; }
-keep class com.droidisland.app.service.IslandNotificationListener { *; }
-keep class com.droidisland.app.service.IslandController { *; }
-keep class com.droidisland.app.service.BootReceiver { *; }
-keep class com.droidisland.app.service.BluetoothHeadphoneManager { *; }

# 6. AGSL / RenderEffect Shaders and Graphics
-keep class android.graphics.RenderEffect { *; }
-keep class android.graphics.RuntimeShader { *; }
-keep class com.droidisland.app.ui.components.LiquidGlassShaderManager { *; }

# 7. Kotlin Coroutines & Reflection
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 8. Suppress warnings from optional system services
-dontwarn android.app.usage.UsageStatsManager
-dontwarn android.app.usage.UsageEvents
