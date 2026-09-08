package com.sih26001.mobilealert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sih26001.mobilealert.core.navigation.AppNavigation
import com.sih26001.mobilealert.core.ui.theme.SIH26001MobileAlertTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SIH26001MobileAlertTheme {
                AppNavigation()
            }
        }
    }
}