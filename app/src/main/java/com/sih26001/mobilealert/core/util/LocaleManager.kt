package com.sih26001.mobilealert.core.util

import android.content.Context
import android.content.SharedPreferences
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
    HINDI("hi", "हिन्दी"),
    MARATHI("mr", "मराठी"),
    ASSAMESE("as", "অসমীয়া");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

object LocaleManager {
    private const val PREFS_NAME = "geoagent_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language_code"

    private var sharedPreferences: SharedPreferences? = null
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs
        val savedCode = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.code)
        _currentLanguage.value = AppLanguage.fromCode(savedCode)
    }

    fun setLanguage(language: AppLanguage, context: Context? = null) {
        _currentLanguage.value = language
        val prefs = sharedPreferences ?: context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs?.edit()?.putString(KEY_LANGUAGE, language.code)?.apply()
    }

    fun getLocale(): Locale {
        return when (_currentLanguage.value) {
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.HINDI -> Locale("hi")
            AppLanguage.MARATHI -> Locale("mr")
            AppLanguage.ASSAMESE -> Locale("as")
        }
    }

    fun getLocalizedContext(context: Context): Context {
        val locale = getLocale()
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
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
        AppLanguage.MARATHI -> Locale("mr")
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
