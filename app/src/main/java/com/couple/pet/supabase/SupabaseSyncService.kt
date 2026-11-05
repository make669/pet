package com.couple.pet.supabase

import android.util.Log
import com.couple.pet.model.PetStats
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.network.RemotePetEvent
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Supabase 云端数据同步与 WebSocket 实时推送服务
 */
object SupabaseSyncService {

    private const val TAG = "SupabaseSync"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private var activeWebSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var subscribedCoupleId: String? = null

    /**
     * 向云端保存当前小宠状态
     */
    fun pushPetStatsToCloud(coupleId: String, stats: PetStats) {
        if (!SupabaseConfig.isConfigured() || SupabaseAuthManager.currentToken == null) return
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("hunger", stats.hunger)
                    put("mood", stats.mood)
                    put("clean", stats.clean)
                    put("coins", stats.coins)
                    put("level", stats.level)
                    put("exp", stats.exp)
                    put("growth_stage", stats.stage.name)
                    put("updated_at", "now()")
                }
                val token = SupabaseAuthManager.currentToken ?: return@launch
                val request = Request.Builder()
                    .url("${SupabaseConfig.currentUrl}/rest/v1/pets?couple_id=eq.$coupleId")
                    .addHeader("apikey", SupabaseConfig.currentAnonKey)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .patch(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.w(TAG, "pushPetStatsToCloud failed: ${e.message}")
            }
        }
    }

    /**
     * 发送实时情侣动作流 (广播至 WebSocket)
     */
    fun broadcastCoupleAction(coupleId: String, eventType: String, payload: JSONObject) {
        if (!SupabaseConfig.isConfigured() || SupabaseAuthManager.currentToken == null) return
        scope.launch {
            try {
                val user = SupabaseAuthManager.currentUser ?: return@launch
                val token = SupabaseAuthManager.currentToken ?: return@launch

                // 1. 插入 sync_events 表，自动触发 Supabase Realtime 数据库监听
                val body = JSONObject().apply {
                    put("couple_id", coupleId)
                    put("sender_id", user.id)
                    put("sender_nickname", user.nickname)
                    put("event_type", eventType)
                    put("payload", payload)
                }
                val request = Request.Builder()
                    .url("${SupabaseConfig.currentUrl}/rest/v1/sync_events")
                    .addHeader("apikey", SupabaseConfig.currentAnonKey)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.w(TAG, "broadcastCoupleAction failed: ${e.message}")
            }
        }
    }

    /**
     * 订阅情侣专属实时长连接 (基于 Phoenix Channels 协议)
     */
    fun subscribeCoupleRealtime(coupleId: String) {
        if (subscribedCoupleId == coupleId && activeWebSocket != null) return
        subscribedCoupleId = coupleId
        disconnectWebSocket()

        if (!SupabaseConfig.isConfigured()) return

        val host = SupabaseConfig.currentUrl.removePrefix("https://").removePrefix("http://")
        val wsUrl = "wss://$host/realtime/v1/websocket?apikey=${SupabaseConfig.currentAnonKey}&vsn=1.0.0"

        val request = Request.Builder().url(wsUrl).build()

        activeWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Supabase Realtime connected!")
                // 加入房间频道
                val joinMsg = JSONObject().apply {
                    put("topic", "realtime:public:sync_events:couple_id=eq.$coupleId")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            put("broadcast", JSONObject().apply { put("ack", false); put("self", false) })
                            put("postgres_changes", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "INSERT")
                                    put("schema", "public")
                                    put("table", "sync_events")
                                    put("filter", "couple_id=eq.$coupleId")
                                })
                            })
                        })
                    })
                    put("ref", "join_1")
                }
                webSocket.send(joinMsg.toString())

                // 启动 25 秒周期心跳维持长连接
                startHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)
                    val event = root.optString("event")
                    val payload = root.optJSONObject("payload") ?: return

                    // 处理 Postgres 数据变更推送
                    if (event == "postgres_changes" || event == "broadcast") {
                        val data = payload.optJSONObject("data") ?: payload
                        val record = data.optJSONObject("record") ?: data
                        val senderNick = record.optString("sender_nickname", "TA")
                        val eventType = record.optString("event_type")
                        val eventPayload = record.optJSONObject("payload") ?: JSONObject()

                        // 忽略自己发出的动作，只处理对方发送的
                        val myNick = SupabaseAuthManager.currentUser?.nickname ?: "我"
                        if (senderNick != myNick) {
                            dispatchRemoteEvent(eventType, senderNick, eventPayload)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Parse realtime message failed: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Supabase Realtime connection failure: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Supabase Realtime closed: $reason")
            }
        })
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(25_000L)
                val ping = JSONObject().apply {
                    put("topic", "phoenix")
                    put("event", "heartbeat")
                    put("payload", JSONObject())
                    put("ref", "ping_${System.currentTimeMillis()}")
                }
                ws.send(ping.toString())
            }
        }
    }

    fun disconnectWebSocket() {
        heartbeatJob?.cancel()
        activeWebSocket?.close(1000, "Normal closure")
        activeWebSocket = null
    }

    private fun dispatchRemoteEvent(eventType: String, senderNick: String, payload: JSONObject) {
        when (eventType) {
            "POKE" -> {
                val msg = payload.optString("message", "${senderNick} 隔空捏了捏你的脸蛋 ❤️")
                PetSyncEngine.emitRemoteEvent(RemotePetEvent.PartnerPoked(senderNick, msg))
            }
            "FEED" -> {
                val food = payload.optString("food", "小鱼干")
                val hunger = payload.optInt("hunger", 100)
                PetSyncEngine.emitRemoteEvent(RemotePetEvent.PartnerFed(senderNick, food, hunger))
            }
            "CLEAN_POOP" -> {
                PetSyncEngine.cleanAllWaste()
                PetSyncEngine.emitRemoteEvent(RemotePetEvent.PartnerCleanedWaste(senderNick, isPoop = true, coinsEarned = 15))
            }
            "BATH" -> {
                PetSyncEngine.emitRemoteEvent(RemotePetEvent.PartnerBathed(senderNick, 100))
            }
            "NOTE" -> {
                val note = payload.optString("note", "")
                if (note.isNotBlank()) {
                    PetSyncEngine.updateDeskNote(note)
                }
            }
        }
    }
}
