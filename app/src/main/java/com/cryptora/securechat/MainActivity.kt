package com.cryptora.securechat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.cryptora.securechat.presentation.splash.SplashScreen
import com.cryptora.securechat.core.designsystem.CryptoraTheme
import com.cryptora.securechat.core.navigation.AppNavHost
import com.cryptora.securechat.core.navigation.AppNavigator
import com.cryptora.securechat.core.navigation.Screen
import com.cryptora.securechat.domain.usecase.auth.RestoreSessionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: AppNavigator

    @Inject
    lateinit var restoreSessionUseCase: RestoreSessionUseCase

    @Inject
    lateinit var demoDataSeeder: com.cryptora.securechat.core.demo.DemoDataSeeder

    private val startDestination = MutableStateFlow<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Notification permission response handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        lifecycleScope.launch {
            // Seed rich demo conversations, encrypted notes, and access requests
            demoDataSeeder.seedAllDemoData()

            val sessionResult = restoreSessionUseCase()
            val user = sessionResult.getOrNull()
            startDestination.value = if (user != null) {
                Screen.Home.route
            } else {
                Screen.Auth.route
            }
        }

        setContent {
            CryptoraTheme {
                val destination by startDestination.collectAsState()
                if (destination == null) {
                    SplashScreen(statusMessage = "Verifying cryptographic enclave...")
                } else {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        navigator = navigator,
                        startDestination = destination!!
                    )
                }
            }
        }
    }
}
