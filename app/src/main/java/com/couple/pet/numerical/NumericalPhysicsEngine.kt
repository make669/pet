package com.couple.pet.numerical

import com.couple.pet.model.PetMotionState
import java.util.Calendar
import kotlin.math.*

/**
 * 核心生命数值类别
 */
enum class VitalType {
    HUNGER,
    MOOD,
    CLEAN
}

/**
 * 食物营养学定义
 */
data class FoodNutrition(
    val id: String,
    val name: String,
    val emoji: String,
    val price: Int,
    val hungerGain: Int,
    val moodGain: Int,
    val cleanGain: Int = 0,
    val calories: Int,
    val foodiePoints: Int,
    val sportyPoints: Int,
    val fluffyPoints: Int,
    val cooldownMinutes: Int,
    val shareBonusDescription: String
)

/**
 * 《线条小狗伴侣》工业级数值动力学引擎 (Numerical Physics & State Evaluator)
 * 汇聚：分段多项式经验曲线、昼夜生理衰减方程、马尔可夫漫游转移矩阵、
 * 胃肠排泄危险度积分、锁相环触控共振与生活自适应同理心补偿算法。
 */
object NumericalPhysicsEngine {

    // ================= 1. 五阶分段平滑经验曲线公式 =================
    /**
     * 计算升到下一级所需总经验 (Segmented Polynomial Model)
     * Stage I (Lv.1~4): 50, 75, 100, 125
     * Stage II (Lv.5~9): 150, 190, 230, 270, 310
     * Stage III (Lv.10~19): 360 + 50*(L-10) + 4*(L-10)^2
     * Stage IV (Lv.20~29): 900 + 70*(L-20) + 6*(L-20)^2
     * Stage V (Lv.30+): 2200 + 120*(L-30)
     */
    fun calculateNextLevelExp(level: Int): Int {
        val lvl = level.coerceAtLeast(1)
        return when (lvl) {
            in 1..4 -> 50 + 25 * (lvl - 1)
            in 5..9 -> 150 + 40 * (lvl - 5)
            in 10..19 -> {
                val d = lvl - 10
                360 + 50 * d + 4 * d * d
            }
            in 20..29 -> {
                val d = lvl - 20
                900 + 70 * d + 6 * d * d
            }
            else -> {
                val d = lvl - 30
                2200 + 120 * d
            }
        }
    }

    // ================= 2. 每日经验产出防肝防刷软硬上限 =================
    const val DAILY_EXP_SOFT_CAP = 260
    const val DAILY_EXP_HARD_CAP = 340
    const val SOFT_CAP_YIELD_RATE = 0.25f

    /**
     * 结算单次行为实际能获取的有效经验
     */
    fun clampEffectiveExpGain(currentDailyExp: Int, rawExp: Int): Int {
        if (currentDailyExp >= DAILY_EXP_HARD_CAP) return 0
        val remainingToHard = DAILY_EXP_HARD_CAP - currentDailyExp
        val effective = if (currentDailyExp >= DAILY_EXP_SOFT_CAP) {
            (rawExp * SOFT_CAP_YIELD_RATE).roundToInt().coerceAtLeast(1)
        } else {
            rawExp
        }
        return effective.coerceAtMost(remainingToHard)
    }

    // ================= 3. 昼夜分段动态生理衰减方程 =================
    // 日间标准消耗速率 (点/小时)
    const val DAY_DECAY_HUNGER_PER_HOUR = 4.0f
    const val DAY_DECAY_CLEAN_PER_HOUR = 2.5f
    const val DAY_DECAY_MOOD_PER_HOUR = 3.0f

    // 夜间睡眠消耗速率 (保留 25%)
    const val NIGHT_DECAY_HUNGER_PER_HOUR = 1.0f
    const val NIGHT_DECAY_CLEAN_PER_HOUR = 0.5f
    const val NIGHT_DECAY_MOOD_PER_HOUR = 0.5f

    /**
     * 判断当前是否处于夜间沉睡时段 (23:00 ~ 08:00)
     */
    fun isNightTime(calendar: Calendar = Calendar.getInstance()): Boolean {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour >= 23 || hour < 8
    }

    /**
     * 计算离散时间片段内的衰减量
     * @param elapsedMinutes 逝去分钟数
     * @param isNight 是否夜间时段
     * @param hunger 当前饱食度 (用于计算连带惩罚)
     * @param clean 当前清洁度 (用于计算连带惩罚)
     * @param stressIndex 现实生活压力指数 0.0~1.0 (用于懂事自适应减缓)
     */
    fun calculateVitalDecayStep(
        vital: VitalType,
        elapsedMinutes: Float,
        isNight: Boolean,
        hunger: Int,
        clean: Int,
        stressIndex: Float = 0f
    ): Float {
        val hours = elapsedMinutes / 60.0f
        val compassionMultiplier = (1.0f - 0.70f * stressIndex.coerceIn(0f, 1f)).coerceIn(0.30f, 1.0f)

        return when (vital) {
            VitalType.HUNGER -> {
                val rate = if (isNight) NIGHT_DECAY_HUNGER_PER_HOUR else DAY_DECAY_HUNGER_PER_HOUR
                rate * hours * compassionMultiplier
            }
            VitalType.CLEAN -> {
                val rate = if (isNight) NIGHT_DECAY_CLEAN_PER_HOUR else DAY_DECAY_CLEAN_PER_HOUR
                rate * hours * compassionMultiplier
            }
            VitalType.MOOD -> {
                val baseRate = if (isNight) NIGHT_DECAY_MOOD_PER_HOUR else DAY_DECAY_MOOD_PER_HOUR
                // 连带惩罚因子：饱食或清洁跌破 20% 时心情衰减加速最高 1.5 倍
                val penalty = 1.0f +
                        (if (hunger < 20) 0.25f else 0f) +
                        (if (clean < 20) 0.25f else 0f)
                baseRate * penalty * hours * compassionMultiplier
            }
        }
    }

    // ================= 4. 胃肠排泄累积危险度函数 =================
    /**
     * 计算瞬间排泄危险度强度 lambda(t)
     * 遵循：lambda(t) = lambda0 + sum(alpha * Cal * (dt/theta) * exp(-dt/theta))
     */
    fun calculatePoopHazardRate(
        calorieHistory: List<Pair<Long, Int>>, // Pair<进食时间戳ms, 卡路里>
        currentTimeMs: Long,
        baseLambda: Float = 0.0005f,
        alpha: Float = 0.00028f,
        thetaMinutes: Float = 90.0f
    ): Float {
        var hazard = baseLambda
        for ((feedTime, cal) in calorieHistory) {
            val elapsedMinutes = (currentTimeMs - feedTime) / 60000.0f
            if (elapsedMinutes in 0.0f..360.0f) {
                val ratio = elapsedMinutes / thetaMinutes
                hazard += alpha * cal * ratio * exp(-ratio)
            }
        }
        return hazard.coerceIn(0.0001f, 0.05f)
    }

    // ================= 5. 锁相环触控共振倍率 =================
    /**
     * 当双方同时触碰小狗时的时间差共振倍率
     */
    fun calculateResonanceMultiplier(
        timeDiffMs: Long,
        sigmaMs: Float = 250.0f,
        maxMultiplier: Float = 2.45f
    ): Float {
        val diff = abs(timeDiffMs).toFloat()
        val gaussian = exp(-(diff * diff) / (2f * sigmaMs * sigmaMs))
        return 1.0f + (maxMultiplier - 1.0f) * gaussian
    }

    // ================= 6. 异地恋物理距离补偿 =================
    /**
     * 根据两地球面大圆距离 km 给予加成系数 (1.0 ~ 1.35)
     */
    fun calculateLongDistanceBonus(
        distanceKm: Float,
        halfScaleKm: Float = 500.0f,
        maxBonus: Float = 0.35f
    ): Float {
        if (distanceKm <= 20.0f) return 1.0f
        return 1.0f + maxBonus * (distanceKm / (distanceKm + halfScaleKm))
    }

    // ================= 7. 桌面漫游隐马尔可夫状态决策矩阵 =================
    /**
     * 7 大漫游运动决策状态：
     * S0: IDLE (待机嗅嗅)
     * S1: WALKING (悠闲小踱步)
     * S2: PRANCING (欢快小跳步)
     * S3: ZOOMIES (发疯狂奔/跑酷)
     * S4: SCRATCHING (坐地挠痒/打哈欠)
     * S5: SLEEPING (趴地打瞌睡)
     * S6: CLIMBING (屏幕边缘悬挂荡秋千)
     */
    fun decideNextRoamState(
        currentState: PetMotionState,
        hunger: Int,
        mood: Int,
        clean: Int,
        energy: Float
    ): PetMotionState {
        // 强制保护分支：体力极低强制小憩
        if (energy < 15f && Math.random() < 0.85) {
            return PetMotionState.NAPPING
        }

        // 基准马尔可夫无条件概率向量 (经平稳分布反推与心理学调优)
        val weights = when (currentState) {
            PetMotionState.IDLE, PetMotionState.SNIFFING -> doubleArrayOf(
                0.20, 0.35, 0.20, 0.05, 0.10, 0.05, 0.05
            )
            PetMotionState.WALKING -> doubleArrayOf(
                0.30, 0.15, 0.30, 0.10, 0.05, 0.05, 0.05
            )
            PetMotionState.RUNNING, PetMotionState.POST_ZOOMIES -> doubleArrayOf(
                0.10, 0.20, 0.10, 0.05, 0.40, 0.15, 0.00
            )
            PetMotionState.SCRATCHING, PetMotionState.YAWNING -> doubleArrayOf(
                0.40, 0.20, 0.10, 0.00, 0.10, 0.15, 0.05
            )
            PetMotionState.NAPPING -> doubleArrayOf(
                0.50, 0.20, 0.10, 0.00, 0.10, 0.10, 0.00
            )
            else -> doubleArrayOf(
                0.28, 0.23, 0.18, 0.05, 0.11, 0.10, 0.05
            )
        }

        // 动态生理算子修正
        if (mood >= 80) {
            weights[3] *= 2.8 // 高心情时发疯跑酷概率大幅增加
            weights[2] *= 1.5 // 小跳步增加
        }
        if (hunger < 30) {
            weights[3] = 0.0  // 饥饿时禁止狂奔
            weights[0] *= 2.0 // 待机求食增加
        }
        if (clean < 30) {
            weights[4] *= 2.2 // 脏乱时频繁坐地抓挠
        }

        // 轮盘赌归一化选择
        val total = weights.sum()
        var rand = Math.random() * total
        for (i in weights.indices) {
            rand -= weights[i]
            if (rand <= 0.0) {
                return when (i) {
                    0 -> PetMotionState.IDLE
                    1 -> PetMotionState.WALKING
                    2 -> PetMotionState.WALKING // 对应欢快轻跳模式
                    3 -> PetMotionState.RUNNING // 发疯极速狂奔
                    4 -> PetMotionState.SCRATCHING
                    5 -> PetMotionState.NAPPING
                    6 -> PetMotionState.CLIMBING
                    else -> PetMotionState.IDLE
                }
            }
        }
        return PetMotionState.IDLE
    }

    // ================= 8. 全食物卡路里与性格矩阵全谱 =================
    val FOOD_NUTRITION_LIBRARY: List<FoodNutrition> = listOf(
        FoodNutrition(
            id = "food_fish",
            name = "小鱼干",
            emoji = "🐟",
            price = 0,
            hungerGain = 15,
            moodGain = 8,
            calories = 35,
            foodiePoints = 2,
            sportyPoints = 0,
            fluffyPoints = 0,
            cooldownMinutes = 30,
            shareBonusDescription = "伴侣同吃双方饱食额外+5"
        ),
        FoodNutrition(
            id = "food_bone",
            name = "和牛骨",
            emoji = "🍖",
            price = 20,
            hungerGain = 30,
            moodGain = 18,
            calories = 80,
            foodiePoints = 5,
            sportyPoints = 1,
            fluffyPoints = 0,
            cooldownMinutes = 120,
            shareBonusDescription = "触发小狗抱骨头打滚特技"
        ),
        FoodNutrition(
            id = "food_pudding",
            name = "甜布丁",
            emoji = "🍮",
            price = 15,
            hungerGain = 12,
            moodGain = 25,
            calories = 40,
            foodiePoints = 3,
            sportyPoints = 0,
            fluffyPoints = 1,
            cooldownMinutes = 60,
            shareBonusDescription = "心情瞬间溢出粉红气泡"
        ),
        FoodNutrition(
            id = "food_milk",
            name = "鲜羊奶",
            emoji = "🥛",
            price = 18,
            hungerGain = 16,
            moodGain = 15,
            cleanGain = 5,
            calories = 45,
            foodiePoints = 3,
            sportyPoints = 0,
            fluffyPoints = 3,
            cooldownMinutes = 60,
            shareBonusDescription = "温热舒胃，毛发光泽+10"
        ),
        FoodNutrition(
            id = "food_jerky",
            name = "原切纯肉干",
            emoji = "🥩",
            price = 25,
            hungerGain = 28,
            moodGain = 14,
            calories = 70,
            foodiePoints = 4,
            sportyPoints = 2,
            fluffyPoints = 0,
            cooldownMinutes = 90,
            shareBonusDescription = "奔跑速度提高15%"
        ),
        FoodNutrition(
            id = "food_cheese",
            name = "奶酪卷",
            emoji = "🧀",
            price = 22,
            hungerGain = 20,
            moodGain = 16,
            calories = 55,
            foodiePoints = 3,
            sportyPoints = 0,
            fluffyPoints = 2,
            cooldownMinutes = 90,
            shareBonusDescription = "幼崽期食用成长经验翻倍"
        ),
        FoodNutrition(
            id = "food_cupcake",
            name = "情侣纸杯蛋糕",
            emoji = "🧁",
            price = 45,
            hungerGain = 28,
            moodGain = 35,
            calories = 60,
            foodiePoints = 4,
            sportyPoints = 2,
            fluffyPoints = 4,
            cooldownMinutes = 360,
            shareBonusDescription = "桌面展开情侣专属微缩野餐布"
        )
    )

    fun findNutrition(foodNameOrId: String): FoodNutrition? {
        return FOOD_NUTRITION_LIBRARY.find {
            it.id == foodNameOrId || it.name == foodNameOrId
        }
    }
}
