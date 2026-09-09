package com.sih26001.mobilealert

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.sih26001.mobilealert.core.alarm.AlarmControllerImpl
import com.sih26001.mobilealert.core.navigation.AppNavigation
import com.sih26001.mobilealert.core.notification.AlertNotificationManagerImpl
import com.sih26001.mobilealert.core.notification.NotificationChannels
import com.sih26001.mobilealert.core.ui.theme.SIH26001MobileAlertTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // In a full app, we might update UI state to warn the user if denied.
    }

    private val mainViewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val notificationManager = AlertNotificationManagerImpl(this@MainActivity)
                val alarmController = AlarmControllerImpl(this@MainActivity)
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(notificationManager, alarmController) as T
            }
        }
    }

    private var navController: androidx.navigation.NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create Notification Channels on app start
        NotificationChannels.createChannels(this)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Explicitly evaluate ViewModel delegate to guarantee initialization and start observing alerts
        mainViewModel.let { }

        // Retrieve FCM token safely for development validation
        retrieveFcmToken()

        // Check if opened from an FCM notification / background payload containing alert_id
        checkIntentForAlertId(intent)

        setContent {
            SIH26001MobileAlertTheme {
                val controller = androidx.navigation.compose.rememberNavController()
                navController = controller
                androidx.compose.runtime.LaunchedEffect(controller) {
                    if (intent.data != null) {
                        controller.handleDeepLink(intent)
                    }
                }
                AppNavigation(navController = controller)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIntentForAlertId(intent)
        navController?.handleDeepLink(intent)
    }

    private fun retrieveFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed: ${task.exception?.message}")
                return@addOnCompleteListener
            }
            val token = task.result
            val preview = if (token != null && token.length > 8) "${token.take(4)}...${token.takeLast(4)}" else "***"
            Log.i(TAG, "FCM token retrieved (length=${token?.length ?: 0}, preview=$preview)")
        }
    }

    private fun checkIntentForAlertId(intent: Intent?) {
        val alertId = intent?.extras?.getString("alert_id")?.trim()
        if (!alertId.isNullOrEmpty()) {
            Log.i(TAG, "FCM alert_id received from launch intent: $alertId")
            // Only set data if data == null and extra is a validated, non-blank alert_id
            if (intent.data == null) {
                intent.data = android.net.Uri.parse("sih26001://alert/$alertId")
            }
        }
    }
}