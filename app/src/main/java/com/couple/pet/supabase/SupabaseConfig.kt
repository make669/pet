package com.couple.pet.supabase

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Supabase 云端连接配置中心
 * 支持运行时动态配置或读取预设的 Supabase Project URL 与 Anon Key
 */
object SupabaseConfig {

    private const val PREFS_NAME = "supabase_config_prefs_v1"
    private const val KEY_URL = "supabase_url"
    private const val KEY_KEY = "supabase_anon_key"

    // 默认预设（用户可在登录界面自定义替换为自己的 Supabase 项目地址）
    private const val DEFAULT_URL = "https://your-pet-project.supabase.co"
    private const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.demo_placeholder_key"

    private var prefs: SharedPreferences? = null

    private val _urlState = MutableStateFlow(DEFAULT_URL)
    val urlState: StateFlow<String> = _urlState.asStateFlow()

    private val _anonKeyState = MutableStateFlow(DEFAULT_ANON_KEY)
    val anonKeyState: StateFlow<String> = _anonKeyState.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs?.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        val savedKey = prefs?.getString(KEY_KEY, DEFAULT_ANON_KEY) ?: DEFAULT_ANON_KEY
        _urlState.value = savedUrl
        _anonKeyState.value = savedKey
    }

    val currentUrl: String
        get() = _urlState.value.trim().removeSuffix("/")

    val currentAnonKey: String
        get() = _anonKeyState.value.trim()

    /**
     * 判断是否已配置了真实有效的 Supabase 项目凭据
     */
    fun isConfigured(): Boolean {
        val url = currentUrl
        val key = currentAnonKey
        return url.isNotBlank() &&
                !url.contains("your-pet-project") &&
                key.isNotBlank() &&
                !key.contains("demo_placeholder_key")
    }

    /**
     * 保存自定义凭据
     */
    fun saveConfig(url: String, anonKey: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        val cleanKey = anonKey.trim()
        _urlState.value = cleanUrl
        _anonKeyState.value = cleanKey
        prefs?.edit()?.apply {
            putString(KEY_URL, cleanUrl)
            putString(KEY_KEY, cleanKey)
            apply()
        }
    }
}
