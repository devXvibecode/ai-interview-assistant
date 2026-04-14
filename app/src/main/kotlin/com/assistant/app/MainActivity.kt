package com.assistant.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.assistant.app.ai.ConversationManager
import com.assistant.app.data.AppPreferences
import com.assistant.app.service.OverlayService
import com.assistant.app.ui.screens.HomeScreen
import com.assistant.app.ui.screens.SettingsScreen
import com.assistant.app.ui.theme.AssistantTheme
import com.assistant.app.utils.PermissionHelper
import com.assistant.app.utils.showToast

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences
    private lateinit var convManager: ConversationManager

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            if (pendingOverlayStart) {
                pendingOverlayStart = false
                launchOverlayService()
            }
        } else {
            showToast("Some permissions denied")
        }
    }

    private var pendingOverlayStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // DO NOT call setDecorFitsSystemWindows(false)
        // Let the system handle insets normally
        // This fixes the overflow issue

        prefs = AppPreferences(this)
        convManager = ConversationManager(prefs)

        setContent {
            AssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNav()
                }
            }
        }
    }

    @Composable
    private fun AppNav() {
        val nav = rememberNavController()

        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    convManager = convManager,
                    prefs = prefs,
                    onSettings = { nav.navigate("settings") },
                    onStartOverlay = { startOverlay() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    prefs = prefs,
                    convManager = convManager,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }

    fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            showToast("Grant overlay permission first")
            PermissionHelper.requestOverlayPermission(this)
            return
        }
        if (!PermissionHelper.hasAudioPermission(this)) {
            showToast("Microphone permission required")
            pendingOverlayStart = true
            permLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
            return
        }
        if (!PermissionHelper.hasNotificationPermission(this)) {
            pendingOverlayStart = true
            val perms = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            permLauncher.launch(perms.toTypedArray())
            return
        }
        launchOverlayService()
    }

    private fun launchOverlayService() {
        if (!PermissionHelper.hasAudioPermission(this)) {
            showToast("Microphone permission required")
            return
        }
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        showToast("AI bubble launched!")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionHelper.REQUEST_OVERLAY) {
            if (Settings.canDrawOverlays(this))
                showToast("Overlay permission granted!")
            else
                showToast("Overlay permission denied")
        }
    }
}