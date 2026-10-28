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

/**
 * 网络同步中枢引擎（负责双端配对与数据双向同步）
 */
object PetSyncEngine {

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _petStats = MutableStateFlow(PetStats())
    val petStats: StateFlow<PetStats> = _petStats.asStateFlow()

    private val _coupleProfile = MutableStateFlow(CoupleProfile())
    val coupleProfile: StateFlow<CoupleProfile> = _coupleProfile.asStateFlow()

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
        val current = _petStats.value
        val updatedHunger = (current.hunger + amount).coerceAtMost(100)
        val updatedStats = addExpAndCheckLevelUp(current, 10).copy(
            hunger = updatedHunger
        )
        _petStats.value = updatedStats
        _remoteEvents.tryEmit(RemotePetEvent.PartnerFed("小鱼干", updatedHunger))
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
