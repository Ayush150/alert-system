package com.sih26001.mobilealert.core.util

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिंदी"),
    ASSAMESE("as", "অসমীয়া")
}

object LocaleManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }
}

@Composable
fun ProvideAppLocale(
    content: @Composable () -> Unit
) {
    val selectedLanguage by LocaleManager.currentLanguage.collectAsState()
    val context = LocalContext.current

    val locale = when (selectedLanguage) {
        AppLanguage.ENGLISH -> Locale("en")
        AppLanguage.HINDI -> Locale("hi")
        AppLanguage.ASSAMESE -> Locale("as")
    }

    val configuration = Configuration(LocalConfiguration.current).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }

    val localizedContext = context.createConfigurationContext(configuration)

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides configuration
    ) {
        content()
    }
}
