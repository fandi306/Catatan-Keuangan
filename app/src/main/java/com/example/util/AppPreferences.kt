package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val code: String, val displayName: String) {
    SYSTEM("system", "Default Sistem"),
    INDONESIA("in", "Bahasa Indonesia"),
    JAWA("jv", "Basa Jawa"),
    BALI("ban", "Basa Bali"),
    ENGLISH("en", "English")
}

data class HomeScreenSettings(
    val showQuotes: Boolean = true,
    val showAccountChips: Boolean = true,
    val showFinancialWarnings: Boolean = true,
    val showRecentTransactions: Boolean = true,
    val showSparklineCurve: Boolean = true
)

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("catatan_uang_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching {
            AppThemeMode.valueOf(prefs.getString(KEY_THEME, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
        }.getOrDefault(AppThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(
        runCatching {
            AppLanguage.values().firstOrNull { it.code == prefs.getString(KEY_LANG, AppLanguage.SYSTEM.code) }
                ?: AppLanguage.SYSTEM
        }.getOrDefault(AppLanguage.SYSTEM)
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _homeSettings = MutableStateFlow(
        HomeScreenSettings(
            showQuotes = prefs.getBoolean(KEY_SHOW_QUOTES, true),
            showAccountChips = prefs.getBoolean(KEY_SHOW_ACCOUNT_CHIPS, true),
            showFinancialWarnings = prefs.getBoolean(KEY_SHOW_WARNINGS, true),
            showRecentTransactions = prefs.getBoolean(KEY_SHOW_RECENT_TX, true),
            showSparklineCurve = prefs.getBoolean(KEY_SHOW_SPARKLINE, true)
        )
    )
    val homeSettings: StateFlow<HomeScreenSettings> = _homeSettings.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        prefs.edit().putString(KEY_LANG, lang.code).apply()
    }

    fun updateHomeScreenSettings(newSettings: HomeScreenSettings) {
        _homeSettings.value = newSettings
        prefs.edit()
            .putBoolean(KEY_SHOW_QUOTES, newSettings.showQuotes)
            .putBoolean(KEY_SHOW_ACCOUNT_CHIPS, newSettings.showAccountChips)
            .putBoolean(KEY_SHOW_WARNINGS, newSettings.showFinancialWarnings)
            .putBoolean(KEY_SHOW_RECENT_TX, newSettings.showRecentTransactions)
            .putBoolean(KEY_SHOW_SPARKLINE, newSettings.showSparklineCurve)
            .apply()
    }

    fun setShowQuotes(enabled: Boolean) {
        updateHomeScreenSettings(_homeSettings.value.copy(showQuotes = enabled))
    }

    fun setShowAccountChips(enabled: Boolean) {
        updateHomeScreenSettings(_homeSettings.value.copy(showAccountChips = enabled))
    }

    fun setShowFinancialWarnings(enabled: Boolean) {
        updateHomeScreenSettings(_homeSettings.value.copy(showFinancialWarnings = enabled))
    }

    fun setShowRecentTransactions(enabled: Boolean) {
        updateHomeScreenSettings(_homeSettings.value.copy(showRecentTransactions = enabled))
    }

    fun setShowSparklineCurve(enabled: Boolean) {
        updateHomeScreenSettings(_homeSettings.value.copy(showSparklineCurve = enabled))
    }

    fun resetHomeSettingsToDefault() {
        updateHomeScreenSettings(HomeScreenSettings())
    }

    fun setSuperCleanMode() {
        updateHomeScreenSettings(
            HomeScreenSettings(
                showQuotes = false,
                showAccountChips = false,
                showFinancialWarnings = false,
                showRecentTransactions = false,
                showSparklineCurve = false
            )
        )
    }

    companion object {
        private const val KEY_THEME = "app_theme_mode"
        private const val KEY_LANG = "app_language"

        private const val KEY_SHOW_QUOTES = "home_show_quotes"
        private const val KEY_SHOW_ACCOUNT_CHIPS = "home_show_account_chips"
        private const val KEY_SHOW_WARNINGS = "home_show_warnings"
        private const val KEY_SHOW_RECENT_TX = "home_show_recent_tx"
        private const val KEY_SHOW_SPARKLINE = "home_show_sparkline"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
