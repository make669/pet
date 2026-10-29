package com.couple.pet.network

import com.couple.pet.model.CoupleProfile
import com.couple.pet.model.PetGrowthStage
import com.couple.pet.model.PetStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

/**
 * 远程实时推送事件
 */
sealed class RemotePetEvent {
    data class PartnerPoked(val message: String = "TA 正在想你 ❤️") : RemotePetEvent()
    data class PartnerFed(val foodName: String, val newHunger: Int) : RemotePetEvent()
    data class PartnerBathed(val newClean: Int) : RemotePetEvent()
    data class PartnerBatteryChanged(val batteryLevel: Int, val isCharging: Boolean) : RemotePetEvent()
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

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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

    fun triggerPoopDemo() {
        _poopDemoTrigger.value = System.currentTimeMillis()
    }

    fun updateDeskNote(note: String) {
        _deskNote.value = note.trim().ifBlank { "今天也要元气满满呀 🐾" }
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
    }

    /**
     * 清理打扫指定排泄物 (铲屎官互动)
     */
    fun cleanWaste(wasteId: Long) {
        _floorWastes.value = _floorWastes.value.filterNot { it.id == wasteId }
        val stats = _petStats.value
        val newExp = stats.exp + 15
        _petStats.value = stats.copy(
            clean = 100,
            mood = (stats.mood + 10).coerceAtMost(100),
            exp = newExp
        )
    }

    /**
     * 清理全部排泄物（如洗澡后或全屏打扫）
     */
    fun cleanAllWaste() {
        _floorWastes.value = emptyList()
        val stats = _petStats.value
        _petStats.value = stats.copy(clean = 100, mood = (stats.mood + 15).coerceAtMost(100))
    }

    private val _remoteEvents = MutableSharedFlow<RemotePetEvent>(extraBufferCapacity = 64)
    val remoteEvents: SharedFlow<RemotePetEvent> = _remoteEvents.asSharedFlow()

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
        val updatedHunger = (current.hunger + hungerBoost).coerceIn(0, 100)
        val updatedMood = (current.mood + moodBoost).coerceIn(0, 100)
        val updatedClean = (current.clean + cleanBoost).coerceIn(0, 100)
        val expGain = ((hungerBoost + moodBoost + cleanBoost) / 3).coerceAtLeast(6)
        val updatedStats = addExpAndCheckLevelUp(current, expGain).copy(
            hunger = updatedHunger,
            mood = updatedMood,
            clean = updatedClean
        )
        _petStats.value = updatedStats
        _remoteEvents.tryEmit(RemotePetEvent.PartnerFed(name, updatedHunger))
    }

    /**
     * 洗澡互动
     */
    fun bath() {
        val current = _petStats.value
        val updatedStats = addExpAndCheckLevelUp(current, 10).copy(
            clean = 100
        )
        _petStats.value = updatedStats
        _remoteEvents.tryEmit(RemotePetEvent.PartnerBathed(100))
    }

    /**
     * 摸摸头 / 双击比心
     */
    fun poke() {
        val current = _petStats.value
        val updatedMood = (current.mood + 10).coerceAtMost(100)
        val updatedStats = addExpAndCheckLevelUp(current, 5).copy(
            mood = updatedMood
        )
        _petStats.value = updatedStats
        _remoteEvents.tryEmit(RemotePetEvent.PartnerPoked("摸了摸宠物头顶 ❤️"))
    }

    /**
     * 绑定配对码
     */
    fun pairWithCode(code: String): Boolean {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.length >= 4) {
            _coupleProfile.value = _coupleProfile.value.copy(
                isPaired = true,
                pairCode = cleanCode
            )
            return true
        }
        return false
    }

    /**
     * 更新情侣昵称
     */
    fun updateNicknames(myNick: String, partnerNick: String) {
        _coupleProfile.value = _coupleProfile.value.copy(
            myNickname = myNick.trim().ifBlank { "小熊" },
            partnerNickname = partnerNick.trim().ifBlank { "小兔" }
        )
    }

    /**
     * 更新相伴天数
     */
    fun updateDaysTogether(days: Int) {
        _coupleProfile.value = _coupleProfile.value.copy(
            daysTogether = days.coerceAtLeast(1)
        )
    }

    /**
     * 经验计算与升级进阶处理
     */
    private fun addExpAndCheckLevelUp(current: PetStats, addExp: Int): PetStats {
        var newExp = current.exp + addExp
        var newLevel = current.level
        var newMaxExp = current.maxExp
        while (newExp >= newMaxExp) {
            newExp -= newMaxExp
            newLevel++
            newMaxExp = newLevel * 100
        }
        val newStage = when {
            newLevel >= 10 -> PetGrowthStage.ADULT
            newLevel >= 5 -> PetGrowthStage.JUVENILE
            else -> PetGrowthStage.BABY
        }
        return current.copy(
            level = newLevel,
            exp = newExp,
            maxExp = newMaxExp,
            stage = newStage
        )
    }

    /**
     * 周期性数值自然衰减（模拟生命活力）
     */
    fun decayStats() {
        val current = _petStats.value
        _petStats.value = current.copy(
            hunger = (current.hunger - 1).coerceAtLeast(0),
            mood = (current.mood - 1).coerceAtLeast(0),
            clean = (current.clean - 1).coerceAtLeast(0)
        )
    }
}
