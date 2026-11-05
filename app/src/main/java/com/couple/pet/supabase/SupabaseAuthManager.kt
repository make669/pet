package com.couple.pet.supabase

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SupabaseUser(
    val id: String,
    val email: String,
    val nickname: String = "小熊",
    val avatar: String = "🐻"
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: SupabaseUser, val accessToken: String) : AuthState()
    data class Error(val message: String) : AuthState()
    object GuestMode : AuthState() // 离线游客模式
}

/**
 * Supabase 邮箱认证与会话管理中心
 */
object SupabaseAuthManager {

    private const val PREFS_NAME = "supabase_auth_prefs_v1"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NICK = "user_nickname"
    private const val KEY_GUEST_MODE = "guest_mode"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var prefs: SharedPreferences? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        restoreSession()
    }

    private fun restoreSession() {
        val p = prefs ?: return
        val isGuest = p.getBoolean(KEY_GUEST_MODE, false)
        if (isGuest) {
            _authState.value = AuthState.GuestMode
            return
        }

        val token = p.getString(KEY_ACCESS_TOKEN, null)
        val userId = p.getString(KEY_USER_ID, null)
        val email = p.getString(KEY_USER_EMAIL, null)
        val nick = p.getString(KEY_USER_NICK, "小熊") ?: "小熊"

        if (!token.isNullOrBlank() && !userId.isNullOrBlank() && !email.isNullOrBlank()) {
            _authState.value = AuthState.Authenticated(
                user = SupabaseUser(id = userId, email = email, nickname = nick),
                accessToken = token
            )
        } else {
            _authState.value = AuthState.Idle
        }
    }

    /**
     * 邮箱注册
     */
    suspend fun signUp(email: String, pass: String, nickname: String): Result<SupabaseUser> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()
            val cleanNick = nickname.trim().ifBlank { "小熊" }
            if (!isValidEmail(cleanEmail)) {
                val err = "请输入正确的邮箱格式"
                _authState.value = AuthState.Error(err)
                return@withContext Result.failure(Exception(err))
            }
            if (pass.length < 6) {
                val err = "密码长度至少需要 6 位"
                _authState.value = AuthState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            _authState.value = AuthState.Loading

            // 如果尚未配置真实 Supabase 凭证，提供顺滑的模拟云端体验
            if (!SupabaseConfig.isConfigured()) {
                val mockUser = SupabaseUser(
                    id = "usr_" + System.currentTimeMillis(),
                    email = cleanEmail,
                    nickname = cleanNick
                )
                saveSession(mockUser, "mock_token_" + System.currentTimeMillis(), "")
                _authState.value = AuthState.Authenticated(mockUser, "mock_token")
                return@withContext Result.success(mockUser)
            }

            try {
                val jsonBody = JSONObject().apply {
                    put("email", cleanEmail)
                    put("password", pass)
                    put("data", JSONObject().apply { put("nickname", cleanNick) })
                }
                val request = Request.Builder()
                    .url("${SupabaseConfig.currentUrl}/auth/v1/signup")
                    .addHeader("apikey", SupabaseConfig.currentAnonKey)
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val respStr = response.body?.string() ?: "{}"
                val json = JSONObject(respStr)

                if (response.isSuccessful) {
                    val userObj = json.optJSONObject("user") ?: json
                    val userId = userObj.optString("id", System.currentTimeMillis().toString())
                    val token = json.optString("access_token", "anon_session_token")
                    val refreshToken = json.optString("refresh_token", "")
                    val user = SupabaseUser(id = userId, email = cleanEmail, nickname = cleanNick)
                    saveSession(user, token, refreshToken)
                    _authState.value = AuthState.Authenticated(user, token)
                    Result.success(user)
                } else {
                    val msg = json.optString("error_description", json.optString("msg", "注册失败，请检查网络或凭据"))
                    _authState.value = AuthState.Error(msg)
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                val msg = "连接云端失败: ${e.localizedMessage ?: "网络超时"}"
                _authState.value = AuthState.Error(msg)
                Result.failure(e)
            }
        }

    /**
     * 邮箱密码登录
     */
    suspend fun signIn(email: String, pass: String): Result<SupabaseUser> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()
            if (!isValidEmail(cleanEmail)) {
                val err = "请输入有效的邮箱地址"
                _authState.value = AuthState.Error(err)
                return@withContext Result.failure(Exception(err))
            }
            if (pass.isEmpty()) {
                val err = "请输入密码"
                _authState.value = AuthState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            _authState.value = AuthState.Loading

            // 模拟免配置云端登录
            if (!SupabaseConfig.isConfigured()) {
                val mockUser = SupabaseUser(
                    id = "usr_" + cleanEmail.hashCode(),
                    email = cleanEmail,
                    nickname = cleanEmail.substringBefore("@")
                )
                saveSession(mockUser, "mock_token_" + System.currentTimeMillis(), "")
                _authState.value = AuthState.Authenticated(mockUser, "mock_token")
                return@withContext Result.success(mockUser)
            }

            try {
                val jsonBody = JSONObject().apply {
                    put("email", cleanEmail)
                    put("password", pass)
                }
                val request = Request.Builder()
                    .url("${SupabaseConfig.currentUrl}/auth/v1/token?grant_type=password")
                    .addHeader("apikey", SupabaseConfig.currentAnonKey)
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val respStr = response.body?.string() ?: "{}"
                val json = JSONObject(respStr)

                if (response.isSuccessful) {
                    val token = json.getString("access_token")
                    val refreshToken = json.optString("refresh_token", "")
                    val userObj = json.getJSONObject("user")
                    val userId = userObj.getString("id")
                    val userMeta = userObj.optJSONObject("user_metadata")
                    val nick = userMeta?.optString("nickname", cleanEmail.substringBefore("@")) ?: cleanEmail.substringBefore("@")

                    val user = SupabaseUser(id = userId, email = cleanEmail, nickname = nick)
                    saveSession(user, token, refreshToken)
                    _authState.value = AuthState.Authenticated(user, token)
                    Result.success(user)
                } else {
                    val msg = json.optString("error_description", json.optString("msg", "邮箱或密码错误"))
                    _authState.value = AuthState.Error(msg)
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                val msg = "连接云端失败: ${e.localizedMessage ?: "网络超时"}"
                _authState.value = AuthState.Error(msg)
                Result.failure(e)
            }
        }

    /**
     * 退出登录
     */
    fun signOut() {
        prefs?.edit()?.apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NICK)
            putBoolean(KEY_GUEST_MODE, false)
            apply()
        }
        _authState.value = AuthState.Idle
    }

    /**
     * 开启离线游客模式 (无需登录直接体验)
     */
    fun enterGuestMode() {
        prefs?.edit()?.putBoolean(KEY_GUEST_MODE, true)?.apply()
        _authState.value = AuthState.GuestMode
    }

    private fun saveSession(user: SupabaseUser, token: String, refreshToken: String) {
        prefs?.edit()?.apply {
            putString(KEY_ACCESS_TOKEN, token)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_NICK, user.nickname)
            putBoolean(KEY_GUEST_MODE, false)
            apply()
        }
    }

    val currentUser: SupabaseUser?
        get() = (_authState.value as? AuthState.Authenticated)?.user

    val currentToken: String?
        get() = (_authState.value as? AuthState.Authenticated)?.accessToken

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
