# Правила ProGuard / R8 для DroidIsland
# Сохранение Compose-компонентов
-keep class androidx.compose.** { *; }

# Сохранение DataStore Preferences
-keep class androidx.datastore.preferences.** { *; }

# Сохранение нашего сервиса оверлея
-keep class com.droidisland.app.service.IslandOverlayService { *; }
-keepclassmembers class com.droidisland.app.data.IslandConfig { *; }
