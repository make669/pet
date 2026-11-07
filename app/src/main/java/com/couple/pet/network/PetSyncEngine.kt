package com.couple.pet.network

import android.content.Context
import android.content.SharedPreferences
import com.couple.pet.model.CoupleProfile
import com.couple.pet.model.PetGrowthStage
import com.couple.pet.model.PetStats
import com.couple.pet.supabase.SupabaseSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 远程实时推送事件 (情侣双向互动流)
 */
sealed class RemotePetEvent {
    data class PartnerPoked(
        val partnerName: String = "TA",
        val message: String = "TA 正在想你 ❤️"
    ) : RemotePetEvent() {
        constructor(message: String) : this("TA", message)
    }

    data class PartnerFed(
        val partnerName: String = "TA",
        val foodName: String = "小鱼干",
        val newHunger: Int = 100
    ) : RemotePetEvent() {
        constructor(foodName: String, newHunger: Int) : this("TA", foodName, newHunger)
    }

    data class PartnerBathed(
        val partnerName: String = "TA",
        val newClean: Int = 100
    ) : RemotePetEvent() {
        constructor(newClean: Int) : this("TA", newClean)
    }

    data class PartnerCleanedWaste(
        val partnerName: String = "TA",
        val isPoop: Boolean = true,
        val coinsEarned: Int = 15
    ) : RemotePetEvent()

    data class PartnerNoteUpdated(
        val partnerName: String = "TA",
        val note: String = ""
    ) : RemotePetEvent()

    data class PartnerBatteryChanged(
        val batteryLevel: Int,
        val isCharging: Boolean
    ) : RemotePetEvent()

    data class PartnerWasteSpawned(
        val isPoop: Boolean = true,
        val x: Float = 0f
    ) : RemotePetEvent()

    data class PetEvolved(
        val partnerName: String = "TA",
        val stage: com.couple.pet.model.PetGrowthStage,
        val message: String
    ) : RemotePetEvent()
}

/**
 * 伴侣模拟操作类型 (用于单机/离线环境快速连线验证)
 */
enum class PartnerActionType {
    POKE,        // TA 隔空拍一拍
    FEED,        // TA 远程投喂美食
    BATH,        // TA 远程洗香香
    CLEAN_POOP,  // TA 远程帮忙铲屎打扫
    NOTE,        // TA 悄悄更新桌面便签
    BATTERY      // TA 手机电量变动
}

data class FloorWaste(
    val id: Long = System.currentTimeMillis(),
    val x: Float, // 相框内相对横坐标 [-80f, 80f]
    val isPoop: Boolean, // true = 螺丝卷臭臭, false = 撒尿水滩
    val speciesId: String = "doodle_dog"
)

/**
 * 网络同步中枢引擎（负责双端配对与数据双向同步）
 */
object PetSyncEngine {

    private const val PREFS_NAME = "pet_couple_sync_prefs_v1"
    private const val KEY_IS_PAIRED = "is_paired"
    private const val KEY_PAIR_CODE = "pair_code"
    private const val KEY_MY_NICK = "my_nickname"
    private const val KEY_PARTNER_NICK = "partner_nickname"
    private const val KEY_PARTNER_BATTERY = "partner_battery"
    private const val KEY_PARTNER_CHARGING = "partner_charging"
    private const val KEY_DAYS_TOGETHER = "days_together"
    private const val KEY_DESK_NOTE = "desk_note"
    private const val KEY_COINS = "pet_coins"
    private const val KEY_LEVEL = "pet_level"
    private const val KEY_EXP = "pet_exp"
    private const val KEY_HUNGER = "pet_hunger"
    private const val KEY_MOOD = "pet_mood"
    private const val KEY_CLEAN = "pet_clean"
    private const val KEY_STAGE = "pet_growth_stage"
    private const val KEY_FOODIE_POINTS = "foodie_points"
    private const val KEY_SPORTY_POINTS = "sporty_points"
    private const val KEY_FLUFFY_POINTS = "fluffy_points"
    private const val KEY_DAILY_EXP = "pet_daily_exp"
    private const val KEY_DAILY_RESET_DAY = "pet_daily_reset_day"

    // 胃肠热量消化历史记录 (用于泊松排泄动力学危险度计算)
    private val _calorieHistory = mutableListOf<Pair<Long, Int>>()
    val calorieHistory: List<Pair<Long, Int>>
        get() = synchronized(_calorieHistory) { _calorieHistory.toList() }

    fun recordCalorieFeed(calories: Int) {
        val now = System.currentTimeMillis()
        synchronized(_calorieHistory) {
            _calorieHistory.add(Pair(now, calories))
            _calorieHistory.removeAll { (time, _) -> now - time > 6 * 3600 * 1000L }
        }
    }

    private var prefs: SharedPreferences? = null
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var heartbeatJob: Job? = null

    private val _petStats = MutableStateFlow(PetStats())
    val petStats: StateFlow<PetStats> = _petStats.asStateFlow()

    private val _coupleProfile = MutableStateFlow(CoupleProfile())
    val coupleProfile: StateFlow<CoupleProfile> = _coupleProfile.asStateFlow()

    private val _deskNote = MutableStateFlow("记得多喝水，小狗会一直陪着你哦 ❤️")
    val deskNote: StateFlow<String> = _deskNote.asStateFlow()

    private val _floorWastes = MutableStateFlow<List<FloorWaste>>(emptyList())
    val floorWastes: StateFlow<List<FloorWaste>> = _floorWastes.asStateFlow()

    private val _poopDemoTrigger = MutableStateFlow(0L)
    val poopDemoTrigger: StateFlow<Long> = _poopDemoTrigger.asStateFlow()

    private val _evolutionCelebrationEvent = MutableStateFlow<PetGrowthStage?>(null)
    val evolutionCelebrationEvent: StateFlow<PetGrowthStage?> = _evolutionCelebrationEvent.asStateFlow()

    fun clearEvolutionCelebration() {
        _evolutionCelebrationEvent.value = null
    }

    /**
     * 开发者与测试快捷指令：切换/预览指定形态
     */
    fun debugSetGrowthStage(stage: PetGrowthStage, triggerCelebration: Boolean = false) {
        val current = _petStats.value
        _petStats.value = current.copy(stage = stage)
        saveStats()
        if (triggerCelebration) {
            _evolutionCelebrationEvent.value = stage
        }
        _remoteEvents.tryEmit(
            RemotePetEvent.PetEvolved(
                partnerName = _coupleProfile.value.myNickname,
                stage = stage,
                message = "切换形态为【${stage.title}】！"
            )
        )
    }

    private val _remoteEvents = MutableSharedFlow<RemotePetEvent>(extraBufferCapacity = 64)
    val remoteEvents: SharedFlow<RemotePetEvent> = _remoteEvents.asSharedFlow()

    fun emitRemoteEvent(event: RemotePetEvent) {
        _remoteEvents.tryEmit(event)
    }

    init {
        // 开启后台活力衰减引擎（每 30 秒消耗 1 点体力/心情/清洁度）
        engineScope.launch {
            while (isActive) {
                delay(30_000L)
                decayStats()
            }
        }
    }

    /**
     * 初始化引擎并加载本地持久化状态
     */
    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadData()
        startCompanionHeartbeat()
        if (_coupleProfile.value.isPaired) {
            SupabaseSyncService.subscribeCoupleRealtime(_coupleProfile.value.pairCode)
        }
    }

    private fun loadData() {
        val p = prefs ?: return
        val isPaired = p.getBoolean(KEY_IS_PAIRED, false)
        val pairCode = p.getString(KEY_PAIR_CODE, "PET-520") ?: "PET-520"
        val myNick = p.getString(KEY_MY_NICK, "小熊") ?: "小熊"
        val partnerNick = p.getString(KEY_PARTNER_NICK, "小兔") ?: "小兔"
        val partnerBat = p.getInt(KEY_PARTNER_BATTERY, 88)
        val partnerCharging = p.getBoolean(KEY_PARTNER_CHARGING, true)
        val days = p.getInt(KEY_DAYS_TOGETHER, 128)

        _coupleProfile.value = CoupleProfile(
            isPaired = isPaired,
            pairCode = pairCode,
            myNickname = myNick,
            partnerNickname = partnerNick,
            partnerBattery = partnerBat,
            isPartnerCharging = partnerCharging,
            isPartnerOnline = isPaired,
            daysTogether = days
        )

        val note = p.getString(KEY_DESK_NOTE, "记得多喝水，小狗会一直陪着你哦 ❤️") ?: "记得多喝水，小狗会一直陪着你哦 ❤️"
        _deskNote.value = note

        val coins = p.getInt(KEY_COINS, 120)
        val level = p.getInt(KEY_LEVEL, 1)
        val exp = p.getInt(KEY_EXP, 0)
        val hunger = p.getInt(KEY_HUNGER, 85)
        val mood = p.getInt(KEY_MOOD, 90)
        val clean = p.getInt(KEY_CLEAN, 95)

        val foodiePoints = p.getInt(KEY_FOODIE_POINTS, 0)
        val sportyPoints = p.getInt(KEY_SPORTY_POINTS, 0)
        val fluffyPoints = p.getInt(KEY_FLUFFY_POINTS, 0)
        val affinity = com.couple.pet.model.PersonalityAffinity(foodiePoints, sportyPoints, fluffyPoints)

        val savedStageName = p.getString(KEY_STAGE, null)
        val stage = savedStageName?.let {
            try { PetGrowthStage.valueOf(it) } catch (_: Exception) { null }
        } ?: calculateGrowthStage(level, PetGrowthStage.BABY, affinity)

        val dailyExp = p.getInt(KEY_DAILY_EXP, 0)
        val dailyResetDay = p.getLong(KEY_DAILY_RESET_DAY, 0L)
        val currentDay = System.currentTimeMillis() / (24 * 3600 * 1000L)
        val effectiveDailyExp = if (dailyResetDay == currentDay) dailyExp else 0
        val maxExp = com.couple.pet.numerical.NumericalPhysicsEngine.calculateNextLevelExp(level)

        _petStats.value = PetStats(
            hunger = hunger,
            mood = mood,
            clean = clean,
            coins = coins,
            level = level,
            exp = exp,
            maxExp = maxExp,
            stage = stage,
            affinity = affinity,
            dailyExpAcquired = effectiveDailyExp,
            dailyExpResetDay = currentDay
        )
    }

    private fun saveProfile() {
        prefs?.edit()?.apply {
            val prof = _coupleProfile.value
            putBoolean(KEY_IS_PAIRED, prof.isPaired)
            putString(KEY_PAIR_CODE, prof.pairCode)
            putString(KEY_MY_NICK, prof.myNickname)
            putString(KEY_PARTNER_NICK, prof.partnerNickname)
            putInt(KEY_PARTNER_BATTERY, prof.partnerBattery)
            putBoolean(KEY_PARTNER_CHARGING, prof.isPartnerCharging)
            putInt(KEY_DAYS_TOGETHER, prof.daysTogether)
            apply()
        }
    }

    private fun saveDeskNote() {
        prefs?.edit()?.putString(KEY_DESK_NOTE, _deskNote.value)?.apply()
    }

    private fun saveStats() {
        prefs?.edit()?.apply {
            val s = _petStats.value
            putInt(KEY_COINS, s.coins)
            putInt(KEY_LEVEL, s.level)
            putInt(KEY_EXP, s.exp)
            putInt(KEY_HUNGER, s.hunger)
            putInt(KEY_MOOD, s.mood)
            putInt(KEY_CLEAN, s.clean)
            putString(KEY_STAGE, s.stage.name)
            putInt(KEY_FOODIE_POINTS, s.affinity.foodiePoints)
            putInt(KEY_SPORTY_POINTS, s.affinity.sportyPoints)
            putInt(KEY_FLUFFY_POINTS, s.affinity.fluffyPoints)
            putInt(KEY_DAILY_EXP, s.dailyExpAcquired)
            putLong(KEY_DAILY_RESET_DAY, s.dailyExpResetDay)
            apply()
        }
    }

    private fun startCompanionHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = engineScope.launch {
            while (isActive) {
                delay(60_000L) // 伴侣定期在线脉冲感知
                if (_coupleProfile.value.isPaired) {
                    val prof = _coupleProfile.value
                    val currentBat = prof.partnerBattery
                    val delta = if (prof.isPartnerCharging) 1 else -1
                    val newBat = (currentBat + delta).coerceIn(20, 100)
                    _coupleProfile.value = prof.copy(
                        partnerBattery = newBat,
                        isPartnerOnline = true,
                        lastSyncTime = System.currentTimeMillis()
                    )
                    saveProfile()
                }
            }
        }
    }

    fun triggerPoopDemo() {
        _poopDemoTrigger.value = System.currentTimeMillis()
    }

    fun updateDeskNote(note: String) {
        _deskNote.value = note.trim().ifBlank { "今天也要元气满满呀 🐾" }
        saveDeskNote()
        if (_coupleProfile.value.isPaired) {
            _remoteEvents.tryEmit(
                RemotePetEvent.PartnerNoteUpdated(
                    partnerName = _coupleProfile.value.myNickname,
                    note = _deskNote.value
                )
            )
            SupabaseSyncService.broadcastCoupleAction(
                _coupleProfile.value.pairCode,
                "NOTE",
                JSONObject().apply {
                    put("note", _deskNote.value)
                }
            )
        }
    }

    /**
     * 生成排泄物（拉臭臭或撒尿水花）
     */
    fun spawnWaste(x: Float, isPoop: Boolean, speciesId: String = "doodle_dog") {
        val current = _floorWastes.value.toMutableList()
        if (current.size >= 3) {
            current.removeAt(0)
        }
        current.add(FloorWaste(id = System.currentTimeMillis(), x = x, isPoop = isPoop, speciesId = speciesId))
        _floorWastes.value = current

        // 发生排泄，清洁度扣减 15 点
        val stats = _petStats.value
        _petStats.value = stats.copy(clean = (stats.clean - 15).coerceAtLeast(10))
        saveStats()
    }

    /**
     * 清理打扫指定排泄物 (铲屎官互动)
     */
    fun cleanWaste(wasteId: Long) {
        _floorWastes.value = _floorWastes.value.filterNot { it.id == wasteId }
        val stats = _petStats.value
        val updated = addExpAndCheckLevelUp(stats, 15, addFluffy = 2).copy(
            clean = 100,
            mood = (stats.mood + 10).coerceAtMost(100),
            coins = stats.coins + 15 // 铲屎官辛苦津贴 +15 金币
        )
        _petStats.value = updated
        saveStats()
        if (_coupleProfile.value.isPaired) {
            SupabaseSyncService.pushPetStatsToCloud(_coupleProfile.value.pairCode, updated)
            SupabaseSyncService.broadcastCoupleAction(
                _coupleProfile.value.pairCode,
                "CLEAN_POOP",
                JSONObject().apply {
                    put("is_poop", true)
                }
            )
        }
    }

    /**
     * 清理全部排泄物（如洗澡后或全屏打扫）
     */
    fun cleanAllWaste() {
        _floorWastes.value = emptyList()
        val stats = _petStats.value
        val updated = stats.copy(clean = 100, mood = (stats.mood + 15).coerceAtMost(100))
        _petStats.value = updated
        saveStats()
        if (_coupleProfile.value.isPaired) {
            SupabaseSyncService.pushPetStatsToCloud(_coupleProfile.value.pairCode, updated)
        }
    }

    /**
     * 投喂食物
     */
    fun feed(amount: Int = 15) {
        feedItem("小鱼干", hungerBoost = amount)
    }

    /**
     * 投喂自定义美食或互动道具
     */
    fun feedItem(
        name: String,
        hungerBoost: Int = 15,
        moodBoost: Int = 0,
        cleanBoost: Int = 0
    ) {
        val current = _petStats.value
        val nutrition = com.couple.pet.numerical.NumericalPhysicsEngine.findNutrition(name)
        val effHunger = nutrition?.hungerGain ?: hungerBoost
        val effMood = nutrition?.moodGain ?: moodBoost
        val effClean = nutrition?.cleanGain ?: cleanBoost
        val addFoodie = nutrition?.foodiePoints ?: 2
        val addSporty = nutrition?.sportyPoints ?: 0
        val addFluffy = nutrition?.fluffyPoints ?: 0
        val calories = nutrition?.calories ?: 35

        recordCalorieFeed(calories)

        val updatedHunger = (current.hunger + effHunger).coerceIn(0, 100)
        val updatedMood = (current.mood + effMood).coerceIn(0, 100)
        val updatedClean = (current.clean + effClean).coerceIn(0, 100)
        val expGain = ((effHunger + effMood + effClean) / 3).coerceAtLeast(6)
        val updatedStats = addExpAndCheckLevelUp(
            current,
            expGain,
            addFoodie = addFoodie,
            addSporty = addSporty,
            addFluffy = addFluffy
        ).copy(
            hunger = updatedHunger,
            mood = updatedMood,
            clean = updatedClean
        )
        _petStats.value = updatedStats
        saveStats()
        _remoteEvents.tryEmit(
            RemotePetEvent.PartnerFed(
                partnerName = _coupleProfile.value.myNickname,
                foodName = name,
                newHunger = updatedHunger
            )
        )
        if (_coupleProfile.value.isPaired) {
            SupabaseSyncService.pushPetStatsToCloud(_coupleProfile.value.pairCode, updatedStats)
            SupabaseSyncService.broadcastCoupleAction(
                _coupleProfile.value.pairCode,
                "FEED",
                JSONObject().apply {
                    put("food", name)
                    put("hunger", updatedHunger)
                }
            )
        }
    }

    /**
     * 洗澡互动
     */
    fun bath() {
        val current = _petStats.value
        val updatedStats = addExpAndCheckLevelUp(current, 10, addFluffy = 2).copy(
            clean = 100
        )
        _petStats.value = updatedStats
        saveStats()
        _remoteEvents.tryEmit(
            RemotePetEvent.PartnerBathed(
                partnerName = _coupleProfile.value.myNickname,
                newClean = 100
            )
        )
        if (_coupleProfile.value.isPaired) {
            SupabaseSyncService.pushPetStatsToCloud(_coupleProfile.value.pairCode, updatedStats)
            SupabaseSyncService.broadcastCoupleAction(
                _coupleProfile.value.pairCode,
                "BATH",
                JSONObject().apply {
                    put("clean", 100)
                }
            )
        }
    }

    /**
     * 摸摸头 / 双击比心
     */
    fun poke() {
        val current = _petStats.value
        val updatedMood = (current.mood + 10).coerceAtMost(100)
        val coinBonus = if (Math.random() < 0.45) 2 else 0 // 亲密抚摸概率掉落金币
        val updatedStats = addExpAndCheckLevelUp(current, 5, addSporty = 1).copy(
            mood = updatedMood,
            coins = current.coins + coinBonus
        )
        _petStats.value = updatedStats
        saveStats()
        _remoteEvents.tryEmit(
            RemotePetEvent.PartnerPoked(
                partnerName = _coupleProfile.value.myNickname,
                message = "摸了摸宠物头顶 ❤️"
            )
        )
    }

    /**
     * 隔空给 TA 拍一拍
     */
    fun sendPokeToPartner() {
        poke()
        if (_coupleProfile.value.isPaired) {
            val msg = "${_coupleProfile.value.myNickname} 拍了拍 ${_coupleProfile.value.partnerNickname} ❤️"
            _remoteEvents.tryEmit(
                RemotePetEvent.PartnerPoked(
                    partnerName = _coupleProfile.value.myNickname,
                    message = msg
                )
            )
            SupabaseSyncService.pushPetStatsToCloud(_coupleProfile.value.pairCode, _petStats.value)
            SupabaseSyncService.broadcastCoupleAction(
                _coupleProfile.value.pairCode,
                "POKE",
                JSONObject().apply {
                    put("message", msg)
                }
            )
        }
    }

    /**
     * 绑定配对码并建立双人连线
     */
    fun pairWithCode(
        code: String,
        myNick: String = "小熊",
        partnerNick: String = "小兔"
    ): Boolean {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.length >= 4) {
            _coupleProfile.value = _coupleProfile.value.copy(
                isPaired = true,
                pairCode = cleanCode,
                myNickname = myNick.ifBlank { "小熊" },
                partnerNickname = partnerNick.ifBlank { "小兔" },
                isPartnerOnline = true,
                lastSyncTime = System.currentTimeMillis()
            )
            saveProfile()

            // 连线成功发射专属连线欢迎事件
            _remoteEvents.tryEmit(
                RemotePetEvent.PartnerPoked(
                    partnerName = _coupleProfile.value.partnerNickname,
                    message = "${_coupleProfile.value.partnerNickname} 与你甜蜜连线成功！共同领养了小萌宠 ❤️"
                )
            )
            SupabaseSyncService.subscribeCoupleRealtime(cleanCode)
            return true
        }
        return false
    }

    /**
     * 解除配对
     */
    fun unpair() {
        _coupleProfile.value = _coupleProfile.value.copy(
            isPaired = false,
            isPartnerOnline = false
        )
        saveProfile()
        SupabaseSyncService.disconnectWebSocket()
    }

    /**
     * 更新情侣昵称
     */
    fun updateNicknames(myNick: String, partnerNick: String) {
        _coupleProfile.value = _coupleProfile.value.copy(
            myNickname = myNick.trim().ifBlank { "小熊" },
            partnerNickname = partnerNick.trim().ifBlank { "小兔" }
        )
        saveProfile()
    }

    /**
     * 更新相伴天数
     */
    fun updateDaysTogether(days: Int) {
        _coupleProfile.value = _coupleProfile.value.copy(
            daysTogether = days.coerceAtLeast(1)
        )
        saveProfile()
    }

    /**
     * 模拟 TA 实时互动 (方便单人开发环境或测试一键闭环检验)
     */
    fun simulatePartnerAction(type: PartnerActionType, customMsg: String? = null) {
        val partner = _coupleProfile.value.partnerNickname
        when (type) {
            PartnerActionType.POKE -> {
                val msg = customMsg ?: "${partner} 隔空戳了戳你的小脸蛋 ❤️"
                val stats = _petStats.value
                _petStats.value = addExpAndCheckLevelUp(stats, 5, addSporty = 1).copy(
                    mood = (stats.mood + 10).coerceAtMost(100)
                )
                saveStats()
                _remoteEvents.tryEmit(RemotePetEvent.PartnerPoked(partner, msg))
            }
            PartnerActionType.FEED -> {
                val food = customMsg ?: "大鸡腿 🍗"
                val stats = _petStats.value
                val newHunger = (stats.hunger + 20).coerceAtMost(100)
                val newMood = (stats.mood + 10).coerceAtMost(100)
                _petStats.value = addExpAndCheckLevelUp(stats, 10, addFoodie = 2).copy(
                    hunger = newHunger,
                    mood = newMood
                )
                saveStats()
                _remoteEvents.tryEmit(RemotePetEvent.PartnerFed(partner, food, newHunger))
            }
            PartnerActionType.BATH -> {
                val stats = _petStats.value
                _petStats.value = addExpAndCheckLevelUp(stats, 10, addFluffy = 2).copy(
                    clean = 100,
                    mood = (stats.mood + 10).coerceAtMost(100)
                )
                cleanAllWaste()
                saveStats()
                _remoteEvents.tryEmit(RemotePetEvent.PartnerBathed(partner, 100))
            }
            PartnerActionType.CLEAN_POOP -> {
                val wastes = _floorWastes.value
                val isPoop = wastes.firstOrNull()?.isPoop ?: true
                cleanAllWaste()
                val stats = _petStats.value
                val newCoins = stats.coins + 15
                _petStats.value = addExpAndCheckLevelUp(stats, 15, addFluffy = 2).copy(
                    clean = 100,
                    coins = newCoins,
                    mood = (stats.mood + 10).coerceAtMost(100)
                )
                saveStats()
                _remoteEvents.tryEmit(RemotePetEvent.PartnerCleanedWaste(partner, isPoop = isPoop, coinsEarned = 15))
            }
            PartnerActionType.NOTE -> {
                val note = customMsg ?: "【${partner}】今天也超级想你，晚上吃好吃的呀 🐾"
                _deskNote.value = note
                saveDeskNote()
                _remoteEvents.tryEmit(RemotePetEvent.PartnerNoteUpdated(partner, note))
            }
            PartnerActionType.BATTERY -> {
                val currentBat = _coupleProfile.value.partnerBattery
                val newBat = if (currentBat >= 95) 75 else (currentBat + 7).coerceAtMost(100)
                val charging = !_coupleProfile.value.isPartnerCharging
                _coupleProfile.value = _coupleProfile.value.copy(
                    partnerBattery = newBat,
                    isPartnerCharging = charging
                )
                saveProfile()
                _remoteEvents.tryEmit(RemotePetEvent.PartnerBatteryChanged(newBat, charging))
            }
        }
    }

    /**
     * 消费金币
     * @return true 消费成功，false 余额不足
     */
    fun spendCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        val current = _petStats.value
        if (current.coins >= amount) {
            _petStats.value = current.copy(coins = current.coins - amount)
            saveStats()
            return true
        }
        return false
    }

    /**
     * 奖励/增加金币
     */
    fun addCoins(amount: Int) {
        if (amount <= 0) return
        val current = _petStats.value
        _petStats.value = current.copy(coins = current.coins + amount)
        saveStats()
    }

    /**
     * 经验计算与升级进阶处理（支持性格积分累积与多形态蜕变判定）
     */
    private fun addExpAndCheckLevelUp(
        current: PetStats,
        addExp: Int,
        addFoodie: Int = 0,
        addSporty: Int = 0,
        addFluffy: Int = 0
    ): PetStats {
        val currentDay = System.currentTimeMillis() / (24 * 3600 * 1000L)
        val dailyAcquired = if (current.dailyExpResetDay == currentDay) current.dailyExpAcquired else 0
        val clampedExp = com.couple.pet.numerical.NumericalPhysicsEngine.clampEffectiveExpGain(dailyAcquired, addExp)

        var newExp = current.exp + clampedExp
        var newLevel = current.level
        var newMaxExp = com.couple.pet.numerical.NumericalPhysicsEngine.calculateNextLevelExp(newLevel)
        var bonusCoins = 0
        while (newExp >= newMaxExp) {
            newExp -= newMaxExp
            newLevel++
            newMaxExp = com.couple.pet.numerical.NumericalPhysicsEngine.calculateNextLevelExp(newLevel)
            bonusCoins += newLevel * 50 // 进阶升级大红包
        }

        val updatedAffinity = current.affinity.copy(
            foodiePoints = current.affinity.foodiePoints + addFoodie,
            sportyPoints = current.affinity.sportyPoints + addSporty,
            fluffyPoints = current.affinity.fluffyPoints + addFluffy
        )

        val newStage = calculateGrowthStage(newLevel, current.stage, updatedAffinity)

        if (newStage != current.stage) {
            // 触发进化大典事件
            _evolutionCelebrationEvent.value = newStage
            _remoteEvents.tryEmit(
                RemotePetEvent.PetEvolved(
                    partnerName = _coupleProfile.value.myNickname,
                    stage = newStage,
                    message = "线条小狗迎来生命蜕变，进化为【${newStage.title}】！"
                )
            )
        }

        return current.copy(
            level = newLevel,
            exp = newExp,
            maxExp = newMaxExp,
            stage = newStage,
            affinity = updatedAffinity,
            coins = current.coins + bonusCoins,
            dailyExpAcquired = dailyAcquired + clampedExp,
            dailyExpResetDay = currentDay
        )
    }

    private fun calculateGrowthStage(
        level: Int,
        currentStage: PetGrowthStage,
        affinity: com.couple.pet.model.PersonalityAffinity
    ): PetGrowthStage {
        return when {
            level >= 30 -> PetGrowthStage.MYTHIC_ANGEL
            level >= 20 -> PetGrowthStage.ADULT_GUARDIAN
            level >= 10 -> {
                if (currentStage == PetGrowthStage.AWAKEN_FOODIE ||
                    currentStage == PetGrowthStage.AWAKEN_SPORTY ||
                    currentStage == PetGrowthStage.AWAKEN_FLUFFY
                ) {
                    currentStage
                } else {
                    affinity.determineBranch()
                }
            }
            level >= 5 -> PetGrowthStage.CHILD
            else -> PetGrowthStage.BABY
        }
    }

    private var hungerDecayAccumulator = 0f
    private var cleanDecayAccumulator = 0f
    private var moodDecayAccumulator = 0f

    /**
     * 周期性数值自然衰减（遵循昼夜作息与心理同理心动力学方程）
     * 昼间 (08:00~23:00): 饱食 -4/h, 清洁 -2.5/h, 心情 -3/h
     * 夜间 (23:00~08:00): 消耗保留 25% (饱食 -1/h, 清洁 -0.5/h, 心情 -0.5/h)
     */
    fun decayStats(elapsedSeconds: Float = 30f) {
        val current = _petStats.value
        val isNight = com.couple.pet.numerical.NumericalPhysicsEngine.isNightTime()
        val elapsedMinutes = elapsedSeconds / 60.0f

        val dH = com.couple.pet.numerical.NumericalPhysicsEngine.calculateVitalDecayStep(
            com.couple.pet.numerical.VitalType.HUNGER, elapsedMinutes, isNight, current.hunger, current.clean
        )
        val dC = com.couple.pet.numerical.NumericalPhysicsEngine.calculateVitalDecayStep(
            com.couple.pet.numerical.VitalType.CLEAN, elapsedMinutes, isNight, current.hunger, current.clean
        )
        val dM = com.couple.pet.numerical.NumericalPhysicsEngine.calculateVitalDecayStep(
            com.couple.pet.numerical.VitalType.MOOD, elapsedMinutes, isNight, current.hunger, current.clean
        )

        hungerDecayAccumulator += dH
        cleanDecayAccumulator += dC
        moodDecayAccumulator += dM

        var newH = current.hunger
        var newC = current.clean
        var newM = current.mood
        var changed = false

        if (hungerDecayAccumulator >= 1.0f) {
            val drop = hungerDecayAccumulator.toInt()
            newH = (newH - drop).coerceAtLeast(0)
            hungerDecayAccumulator -= drop
            changed = true
        }
        if (cleanDecayAccumulator >= 1.0f) {
            val drop = cleanDecayAccumulator.toInt()
            newC = (newC - drop).coerceAtLeast(0)
            cleanDecayAccumulator -= drop
            changed = true
        }
        if (moodDecayAccumulator >= 1.0f) {
            val drop = moodDecayAccumulator.toInt()
            newM = (newM - drop).coerceAtLeast(0)
            moodDecayAccumulator -= drop
            changed = true
        }

        if (changed) {
            _petStats.value = current.copy(hunger = newH, clean = newC, mood = newM)
            saveStats()
        }
    }
}
