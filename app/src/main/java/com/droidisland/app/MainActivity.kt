package com.droidisland.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.droidisland.app.data.IslandConfig
import com.droidisland.app.data.IslandPreferences
import com.droidisland.app.ui.components.MaterialExpressiveBottomBar
import com.droidisland.app.ui.components.NavTab
import com.droidisland.app.ui.screens.DashboardScreen
import com.droidisland.app.ui.screens.OnboardingScreen
import com.droidisland.app.ui.screens.SettingsScreen
import com.droidisland.app.ui.screens.TestsScreen
import com.droidisland.app.ui.screens.TriggersScreen
import com.droidisland.app.ui.theme.DarkBackground
import com.droidisland.app.ui.theme.DroidIslandTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferences: IslandPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferences = IslandPreferences(this)

        if (android.provider.Settings.canDrawOverlays(this)) {
            com.droidisland.app.service.IslandOverlayService.start(this)
        }

        setContent {
            // Material Expressive с поддержкой динамических цветов системы (Material You)
            DroidIslandTheme(dynamicColor = true) {
                MainAppNavigation(preferences = preferences)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.droidisland.app.service.IslandController.isOverlayPaused = false
        if (android.provider.Settings.canDrawOverlays(this)) {
            com.droidisland.app.service.IslandOverlayService.start(this)
        }
    }
}

enum class FlowState {
    LOADING,
    ONBOARDING,
    MAIN_APP
}

@Composable
fun MainAppNavigation(preferences: IslandPreferences) {
    val scope = rememberCoroutineScope()
    val config by preferences.configFlow.collectAsState(initial = null)
    var flowState by remember { mutableStateOf(FlowState.LOADING) }
    var currentTab by remember { mutableStateOf(NavTab.DASHBOARD) }

    if (config != null && flowState == FlowState.LOADING) {
        flowState = if (config!!.isOnboardingCompleted) {
            FlowState.MAIN_APP
        } else {
            FlowState.ONBOARDING
        }
    }

    when (flowState) {
        FlowState.LOADING -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        FlowState.ONBOARDING -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            OnboardingScreen(
                onFinishOnboarding = {
                    scope.launch {
                        preferences.setOnboardingCompleted(true)
                    }
                    if (android.provider.Settings.canDrawOverlays(context)) {
                        com.droidisland.app.service.IslandOverlayService.start(context)
                    } else {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                    flowState = FlowState.MAIN_APP
                }
            )
        }

        FlowState.MAIN_APP -> {
            val islandConfig = config ?: IslandConfig()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            ) {
                // Плавная смена экранов в стиле Google Pixel
                androidx.compose.animation.Crossfade(
                    targetState = currentTab,
                    animationSpec = androidx.compose.animation.core.tween(260),
                    label = "tabCrossfade"
                ) { tab ->
                    when (tab) {
                        NavTab.DASHBOARD -> DashboardScreen(
                            config = islandConfig,
                            preferences = preferences,
                            onNavigateToTests = { currentTab = NavTab.TESTS }
                        )
                        NavTab.TRIGGERS -> TriggersScreen(config = islandConfig, preferences = preferences)
                        NavTab.SETTINGS -> SettingsScreen(config = islandConfig, preferences = preferences)
                        NavTab.TESTS -> TestsScreen(config = islandConfig)
                    }
                }

                // Нижний хотбар в стиле Material Expressive (плавающая капсула с эффектом матового стекла)
                MaterialExpressiveBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
