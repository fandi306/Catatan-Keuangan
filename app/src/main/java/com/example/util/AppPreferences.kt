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

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        prefs.edit().putString(KEY_LANG, lang.code).apply()
    }

    companion object {
        private const val KEY_THEME = "app_theme_mode"
        private const val KEY_LANG = "app_language"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
