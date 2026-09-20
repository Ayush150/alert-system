package com.sih26001.mobilealert.presentation.emergency

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sih26001.mobilealert.MainActivity
import com.sih26001.mobilealert.core.ui.theme.SIH26001MobileAlertTheme
import com.sih26001.mobilealert.core.util.ProvideAppLocale
import com.sih26001.mobilealert.di.DependencyContainer

class EmergencyAlertActivity : ComponentActivity() {

    private lateinit var viewModel: EmergencyAlertViewModel
    private var alertId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Wake screen and present over lockscreen (Full-Screen Alarm/Emergency Protocol)
        configureScreenWakeAndKeyguard()

        // 2. Resolve alert_id from intent
        alertId = resolveAlertId(intent)

        // 3. Initialize ViewModel
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EmergencyAlertViewModel(
                    alertId = alertId,
                    alertRepository = DependencyContainer.alertRepository,
                    acknowledgeAlertUseCase = DependencyContainer.acknowledgeAlertUseCase,
                    alarmController = DependencyContainer.alarmController
                ) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[EmergencyAlertViewModel::class.java]

        // 4. Set Compose UI
        setContent {
            ProvideAppLocale {
                SIH26001MobileAlertTheme {
                    EmergencyAlertScreen(
                        viewModel = viewModel,
                        onNavigateToRoute = { id ->
                            // Navigate to safe route via MainActivity
                            val routeIntent = Intent(this, MainActivity::class.java).apply {
                                data = Uri.parse("sih26001://route/$id")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            startActivity(routeIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newAlertId = resolveAlertId(intent)
        if (newAlertId.isNotBlank() && newAlertId != alertId) {
            alertId = newAlertId
            recreate()
        }
    }

    private fun configureScreenWakeAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun resolveAlertId(intent: Intent?): String {
        val extraId = intent?.getStringExtra("alert_id")?.trim()
        if (!extraId.isNullOrEmpty()) {
            return extraId
        }

        val dataUri = intent?.data
        if (dataUri != null) {
            val lastPath = dataUri.lastPathSegment?.trim()
            if (!lastPath.isNullOrEmpty()) {
                return lastPath
            }
        }

        return ""
    }

    /**
     * Ergonomic safety: Volume down key mutes emergency siren
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (::viewModel.isInitialized && viewModel.isSirenActive.value) {
                viewModel.silenceSiren()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
