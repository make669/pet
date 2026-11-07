package com.couple.pet.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.toColorInt
import com.couple.pet.MainActivity
import com.couple.pet.model.PetGrowthStage
import com.couple.pet.model.PetMotionState
import com.couple.pet.network.PetSyncEngine
import kotlin.math.*

/**
 * 桌面悬浮宠物：极简治愈系“线条小狗”
 * 深度生命力与生物本能仿真引擎 (Biological Instinct & Drive System)：
 * 1. 生物体能与精力周期 (Energy & Fatigue Cycle)：
 *    - 奔跑/追尾巴消耗体力，体力低时自主触发：打哈欠 (YAWNING) -> 伸懒腰 (STRETCHING) -> 蜷成小狗包团子打瞌睡 (NAPPING)
 *    - 睡醒自然伸懒腰、甩毛抖擞精神 (SHAKING)，重新满血探索
 * 2. 幼犬无聊自娱与求关注本能 (Boredom & Social Instinct)：
 *    - 无人互动无聊时：原地疯狂追尾巴转圈 (CHASING_TAIL)、在地面疯狂刨坑扒拉 (DIGGING)、水汪汪大萌眼求摸摸 (BEGGING)
 * 3. 极乐抚摸本能 (Affection Instinct)：
 *    - 连续抚摸触发翻肚皮模式 (BELLY_UP)，四脚朝天乱蹬、极乐眯眼、冒红心
 * 4. 拎起悬空扑腾本能 (Air-Paddling Reflex)：
 *    - 手指按住拖拽时，四脚在空中像游泳般慌乱扑腾，尾巴紧缩夹起，双眼惊恐睁大 (O.O)，落地甩毛自愈
 * 5. 走跑鲜明对比：
 *    - 慢悠漫步：从容步速(2.0px/帧)、闲适晃脑、小步倒腾
 *    - 撒欢快跑：冲刺步速(6.2px/帧)、大跨步起伏跃动、迎风飞耳、吐舌哈气(👅)、脚底手绘冲刺烟尘圈
 */
@SuppressLint("ViewConstructor")
class PetFloatingView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams
) : View(context) {

    // 1. 画笔配置
    private val furPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E1E1E".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 5.0f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fineStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E1E1E".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E1E1E".toColorInt()
        style = Paint.Style.FILL
    }

    private val eyeGlintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFAFC0".toColorInt()
        style = Paint.Style.FILL
    }

    private val tonguePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF6888".toColorInt()
        style = Paint.Style.FILL
    }

    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF4B68".toColorInt()
        style = Paint.Style.FILL
    }

    private val dustPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#999999".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeCap = Paint.Cap.ROUND
    }

    private val dustFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#EAEAEA".toColorInt()
        style = Paint.Style.FILL
    }

    private val bubbleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private val smallEmojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    private val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E1E1E".toColorInt()
        textSize = 12.5f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    // 生理排泄本能驱动
    private var bladderUrge: Float = 0f
    private var lastHandledPoopDemoTrigger: Long = 0L
    private var eliminationIsPoop: Boolean = true
    private var zoomiesLapsRemaining: Int = 0
    private var desktopSpeechBubble: String? = null
    private var bubbleTicksRemaining: Int = 0

    // 2. 状态机与物理属性
    private var motionState: PetMotionState = PetMotionState.IDLE
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    // 落地形变缩放 (Squash & Stretch)
    private var scaleXFactor = 1.0f
    private var scaleYFactor = 1.0f
    private var isParachuteOpen = false

    // 动画游标与心跳
    private var tick = 0
    private var showHeart = false
    private var heartOffsetY = 0f

    // 3. 全屏幕自主漫步与智能探索引擎
    private var currentExactX = layoutParams.x.toFloat()
    private var currentExactY = layoutParams.y.toFloat()
    private var targetX = layoutParams.x.toFloat()
    private var targetY = layoutParams.y.toFloat()
    private var walkDirection = 1 // 1: 向右, -1: 向左
    private var idleTicks = 0
    private var restDuration = 45 // 停留歇息时长
    private var currentSpeed = 2.0f // 慢走约 2.0f，快跑约 6.2f
    private var stateTicks = 0 // 特殊动作持续倒计时

    // 4. 生物本能核心驱动属性 (Biological Drives)
    private var energy = 80f          // 体力精力 (0~100)
    private var boredom = 10f         // 无聊值 (0~100)
    private var affectionTaps = 0     // 连击抚摸计数
    private var lastPetTime = 0L      // 上次抚摸时间
    private var spinAngle = 0f        // 追尾巴旋转角度
    private var airPaddlePhase = 0f   // 空中扑腾相位

    // 5. 双击检测
    private var lastClickTime = 0L

    // 6. 系统电量与充电状态
    private var batteryLevel: Int = 100
    private var isCharging: Boolean = false

    // 预分配 Path
    private val headPath = Path()
    private val leftEarPath = Path()
    private val rightEarPath = Path()
    private val bodyPath = Path()
    private val tailPath = Path()
    private val heartPath = Path()
    private val tonguePath = Path()
    private val thoughtBubblePath = Path()

    private val handler = Handler(Looper.getMainLooper())
    private val animRunnable = object : Runnable {
        override fun run() {
            tick++
            if (showHeart) {
                heartOffsetY = (heartOffsetY - 2.0f).coerceAtLeast(-45f)
            }

            if (bubbleTicksRemaining > 0) {
                bubbleTicksRemaining--
                if (bubbleTicksRemaining == 0) {
                    desktopSpeechBubble = null
                }
            }

            // 更新生物体内在生理数值
            updateBiologicalMetabolism()

            // 全屏幕自由漫步与生命本能驱动
            updateFreeRoam()

            invalidate()
            handler.postDelayed(this, 30) // ~33fps
        }
    }

    init {
        currentExactX = layoutParams.x.toFloat()
        currentExactY = layoutParams.y.toFloat()
        targetX = currentExactX
        targetY = currentExactY
        handler.post(animRunnable)
    }

    fun updateBatteryStatus(level: Int, charging: Boolean) {
        this.batteryLevel = level
        this.isCharging = charging
        invalidate()
    }

    fun showSpeechBubble(text: String, durationTicks: Int = 120, withHeart: Boolean = false) {
        desktopSpeechBubble = text
        bubbleTicksRemaining = durationTicks
        if (withHeart) {
            showHeart = true
            heartOffsetY = 0f
        }
        postInvalidate()
    }

    fun onWasteCleanedByOwner() {
        showHeart = true
        heartOffsetY = 0f
        showSpeechBubble("小窝干干净净啦！谢谢铲屎官主人 ❤️", 90)
        triggerSquashAndStretch()
        invalidate()
    }

    fun startEliminationSequence(isPoop: Boolean) {
        if (isDragging) return
        eliminationIsPoop = isPoop
        motionState = PetMotionState.PRANCE_URGENT
        stateTicks = 80 // 约 2.4 秒憋急小碎步寻隐蔽角落
        showSpeechBubble("急急急！快憋不住啦 😖", 80)

        // 寻找最近的屏幕角落边缘
        val bounds = getScreenBounds()
        val toLeft = kotlin.math.abs(currentExactX - bounds.left)
        val toRight = kotlin.math.abs(currentExactX - bounds.right)
        targetX = if (toLeft < toRight) bounds.left else bounds.right
        targetY = bounds.bottom - 15f
        walkDirection = if (targetX >= currentExactX) 1 else -1
        currentSpeed = 3.6f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(animRunnable)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /**
     * 更新生物内在新陈代谢与生理需求
     */
    private fun updateBiologicalMetabolism() {
        // 观察外部排泄演示触发
        val demoTrigger = com.couple.pet.network.PetSyncEngine.poopDemoTrigger.value
        if (demoTrigger > 0L && demoTrigger != lastHandledPoopDemoTrigger) {
            lastHandledPoopDemoTrigger = demoTrigger
            startEliminationSequence(isPoop = true)
            return
        }

        // 自然生理排泄需求累积（基于胃肠消化热量动态危险度积分模型）
        if (motionState != PetMotionState.POOPING &&
            motionState != PetMotionState.PEEING &&
            motionState != PetMotionState.PRANCE_URGENT &&
            motionState != PetMotionState.POST_ZOOMIES &&
            motionState != PetMotionState.BUSTED) {
            val hazard = com.couple.pet.numerical.NumericalPhysicsEngine.calculatePoopHazardRate(
                calorieHistory = PetSyncEngine.calorieHistory,
                currentTimeMs = System.currentTimeMillis()
            )
            bladderUrge += hazard * 100f
            if (bladderUrge >= 100f && (motionState == PetMotionState.IDLE || motionState == PetMotionState.WALKING)) {
                bladderUrge = 0f
                startEliminationSequence(isPoop = Math.random() < 0.6)
                return
            }
        }

        when (motionState) {
            PetMotionState.RUNNING, PetMotionState.POST_ZOOMIES -> energy = (energy - 0.22f).coerceAtLeast(0f)
            PetMotionState.CHASING_TAIL, PetMotionState.DIGGING -> energy = (energy - 0.30f).coerceAtLeast(0f)
            PetMotionState.WALKING, PetMotionState.PRANCE_URGENT -> energy = (energy - 0.05f).coerceAtLeast(0f)
            PetMotionState.NAPPING -> energy = (energy + 0.26f).coerceAtMost(100f) // 瞌睡快速充沛精力
            PetMotionState.IDLE -> energy = (energy + 0.04f).coerceAtMost(100f)
            else -> {}
        }

        // 无人理睬时，无聊感缓缓上升
        if (motionState != PetMotionState.DRAGGED && motionState != PetMotionState.BELLY_UP && motionState != PetMotionState.NAPPING) {
            boredom = (boredom + 0.06f).coerceAtMost(100f)
        }
    }

    /**
     * 统一屏幕安全活动边界
     */
    private fun getScreenBounds(): RectF {
        val screenW = context.resources.displayMetrics.widthPixels.toFloat()
        val screenH = context.resources.displayMetrics.heightPixels.toFloat()
        val minX = 20f
        val maxX = (screenW - width - 20f).coerceAtLeast(minX)
        val minY = 120f
        val maxY = (screenH - height - 80f).coerceAtLeast(minY)
        return RectF(minX, minY, maxX, maxY)
    }

    /**
     * 全屏幕生物本能驱动状态机引擎
     */
    private fun updateFreeRoam() {
        if (isDragging) return

        val bounds = getScreenBounds()

        when (motionState) {
            // 0. 降落伞缓缓降落
            PetMotionState.FALLING -> {
                val sway = sin(tick * 0.12).toFloat() * 2.2f
                currentExactX = (currentExactX + sway).coerceIn(bounds.left, bounds.right)
                currentExactY += 4.2f
                if (currentExactY >= bounds.bottom) {
                    currentExactY = bounds.bottom
                    isParachuteOpen = false
                    motionState = PetMotionState.SHAKING
                    stateTicks = 20
                    triggerSquashAndStretch()
                    showSpeechBubble("安全着陆！呼~ 🐾", 60)
                }
                layoutParams.x = currentExactX.toInt()
                layoutParams.y = currentExactY.toInt()
                try {
                    windowManager.updateViewLayout(this, layoutParams)
                } catch (_: Exception) {}
            }

            // 0. 屏幕边缘攀爬/悬空荡秋千
            PetMotionState.CLIMBING -> {
                stateTicks--
                if (stateTicks <= 0) {
                    if (currentExactY < bounds.bottom - 100f) {
                        motionState = PetMotionState.FALLING
                        isParachuteOpen = true
                        showSpeechBubble("松开小爪爪~ 降落伞飘落 🪂", 80)
                    } else {
                        motionState = PetMotionState.IDLE
                        idleTicks = 0
                        restDuration = 40
                    }
                }
            }

            // 1. 打瞌睡中：沉睡蓄能，直到体力充盈醒来
            PetMotionState.NAPPING -> {
                if (energy >= 88f) {
                    // 睡醒了！伸个懒腰并甩毛抖擞精神
                    motionState = PetMotionState.STRETCHING
                    stateTicks = 32
                }
            }

            // 2. 伸懒腰中 (下犬式拉伸)
            PetMotionState.STRETCHING -> {
                stateTicks--
                if (stateTicks <= 0) {
                    motionState = PetMotionState.SHAKING
                    stateTicks = 22
                }
            }

            // 3. 打哈欠中
            PetMotionState.YAWNING -> {
                stateTicks--
                if (stateTicks <= 0) {
                    if (energy < 28f) {
                        // 实在太困了，趴下打瞌睡
                        motionState = PetMotionState.NAPPING
                    } else {
                        motionState = PetMotionState.IDLE
                        idleTicks = 0
                        restDuration = 35
                    }
                }
            }

            // 4. 追尾巴打转中 (360°旋转)
            PetMotionState.CHASING_TAIL -> {
                stateTicks--
                spinAngle = (spinAngle + 24f) % 360f
                if (stateTicks <= 0) {
                    spinAngle = 0f
                    boredom = 10f
                    // 转晕了，晃脑甩毛
                    motionState = PetMotionState.SHAKING
                    stateTicks = 25
                }
            }

            // 5. 刨坑扒拉地面中
            PetMotionState.DIGGING -> {
                stateTicks--
                if (stateTicks <= 0) {
                    boredom = 10f
                    // 刨完坑低头嗅探战果
                    motionState = PetMotionState.SNIFFING
                    stateTicks = 35
                }
            }

            // 6. 摇尾乞怜求关注中 (Begging)
            PetMotionState.BEGGING -> {
                stateTicks--
                if (stateTicks <= 0) {
                    motionState = PetMotionState.IDLE
                    idleTicks = 0
                    restDuration = 35
                }
            }

            // 7. 翻肚皮求摸摸中 (Belly Up)
            PetMotionState.BELLY_UP -> {
                stateTicks--
                if (stateTicks <= 0) {
                    motionState = PetMotionState.SHAKING
                    stateTicks = 20
                }
            }

            // 8. 甩毛抖抖中
            PetMotionState.SHAKING -> {
                stateTicks--
                if (stateTicks <= 0) {
                    motionState = PetMotionState.IDLE
                    idleTicks = 0
                    restDuration = 35
                }
            }

            // 9. 停下低头嗅嗅中
            PetMotionState.SNIFFING -> {
                stateTicks--
                val sniffStep = 0.35f * walkDirection
                currentExactX = (currentExactX + sniffStep).coerceIn(bounds.left, bounds.right)
                layoutParams.x = currentExactX.toInt()
                try {
                    windowManager.updateViewLayout(this, layoutParams)
                } catch (_: Exception) {}

                if (stateTicks <= 0) {
                    if (Math.random() < 0.40 && energy > 35f) {
                        motionState = PetMotionState.RUNNING
                        currentSpeed = (5.8f + Math.random() * 1.5f).toFloat()
                    } else {
                        motionState = PetMotionState.WALKING
                        currentSpeed = (1.8f + Math.random() * 0.6f).toFloat()
                    }
                }
            }

            // 10. 站立发呆 / 生物本能主动决策
            PetMotionState.IDLE -> {
                idleTicks++
                if (idleTicks > restDuration) {
                    idleTicks = 0
                    restDuration = (35..65).random()

                    val isNearBottom = currentExactY >= bounds.bottom - 120f

                    // 本能决策 1：极度疲劳困倦 -> 困得打哈欠 -> 趴下入睡
                    if (energy < 26f) {
                        if (!isNearBottom) {
                            // 先慢步降落到地面，再安心睡觉
                            targetX = currentExactX
                            targetY = bounds.bottom - 10f
                            motionState = PetMotionState.WALKING
                            currentSpeed = 2.0f
                            return
                        } else {
                            motionState = PetMotionState.YAWNING
                            stateTicks = 35
                            return
                        }
                    }

                    // 本能决策 2：太无聊自娱自乐 (追尾巴 / 刨坑 / 求摸摸)
                    if (boredom > 60f && energy > 35f) {
                        val boredAction = Math.random()
                        when {
                            boredAction < 0.38 -> {
                                motionState = PetMotionState.CHASING_TAIL
                                stateTicks = 38 // 追尾巴转圈约 1.2 秒
                                return
                            }
                            boredAction < 0.72 && isNearBottom -> {
                                motionState = PetMotionState.DIGGING
                                stateTicks = 42 // 在地面疯狂刨坑
                                return
                            }
                            else -> {
                                motionState = PetMotionState.BEGGING
                                stateTicks = 60 // 水汪汪大萌眼求关注 2 秒
                                return
                            }
                        }
                    }

                    // 本能决策 3：正常犬类探索巡逻 (挑选中意的新目标)
                    var nextTargetX = currentExactX
                    var nextTargetY = currentExactY
                    for (i in 0 until 6) {
                        val candX = (bounds.left + Math.random() * (bounds.right - bounds.left)).toFloat()
                        val candY = if (isNearBottom) {
                            if (Math.random() < 0.82) {
                                bounds.bottom - (Math.random() * 25f).toFloat()
                            } else {
                                (bounds.top + (bounds.bottom - bounds.top) * 0.55f + Math.random() * ((bounds.bottom - bounds.top) * 0.35f)).toFloat()
                            }
                        } else {
                            val r = Math.random()
                            when {
                                r < 0.55 -> bounds.bottom - (Math.random() * 40f).toFloat()
                                r < 0.80 -> (bounds.top + (bounds.bottom - bounds.top) * 0.35f + Math.random() * ((bounds.bottom - bounds.top) * 0.35f)).toFloat()
                                else -> (bounds.top + Math.random() * ((bounds.bottom - bounds.top) * 0.35f)).toFloat()
                            }
                        }.coerceIn(bounds.top, bounds.bottom)

                        if (hypot((candX - currentExactX).toDouble(), (candY - currentExactY).toDouble()) > 70f || i == 5) {
                            nextTargetX = candX
                            nextTargetY = candY
                            break
                        }
                    }

                    targetX = nextTargetX
                    targetY = nextTargetY
                    walkDirection = if (targetX >= currentExactX) 1 else -1

                    val dist = hypot((targetX - currentExactX).toDouble(), (targetY - currentExactY).toDouble()).toFloat()

                    // 依据马尔可夫决策转移网络与动态生理参数决策下一动作状态
                    val currentStats = PetSyncEngine.petStats.value
                    val nextRoam = com.couple.pet.numerical.NumericalPhysicsEngine.decideNextRoamState(
                        currentState = motionState,
                        hunger = currentStats.hunger,
                        mood = currentStats.mood,
                        clean = currentStats.clean,
                        energy = energy
                    )

                    when (nextRoam) {
                        PetMotionState.RUNNING -> {
                            motionState = PetMotionState.RUNNING
                            currentSpeed = (5.8f + Math.random() * 1.6f).toFloat()
                        }
                        PetMotionState.SCRATCHING -> {
                            motionState = PetMotionState.SCRATCHING
                            stateTicks = 36
                        }
                        PetMotionState.NAPPING -> {
                            if (isNearBottom) {
                                motionState = PetMotionState.NAPPING
                            } else {
                                targetX = currentExactX
                                targetY = bounds.bottom - 10f
                                motionState = PetMotionState.WALKING
                                currentSpeed = 2.0f
                            }
                        }
                        PetMotionState.CLIMBING -> {
                            val snapToLeft = currentExactX < (bounds.left + bounds.right) / 2
                            targetX = if (snapToLeft) bounds.left else bounds.right
                            walkDirection = if (snapToLeft) -1 else 1
                            motionState = PetMotionState.CLIMBING
                            stateTicks = 100
                        }
                        else -> {
                            if (Math.random() < 0.25) {
                                motionState = PetMotionState.SNIFFING
                                stateTicks = (30..45).random()
                            } else {
                                motionState = PetMotionState.WALKING
                                currentSpeed = (1.8f + Math.random() * 0.6f).toFloat()
                            }
                        }
                    }
                }
            }

            // 11. 慢走与快跑中的动态事件
            PetMotionState.WALKING, PetMotionState.RUNNING -> {
                val dx = targetX - currentExactX
                val dy = targetY - currentExactY
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                // 慢走途中偶发小惊喜：精力充沛突然加速狂奔，或发现气味停下嗅嗅
                if (motionState == PetMotionState.WALKING && dist > 150f && energy > 40f && Math.random() < 0.018) {
                    motionState = PetMotionState.RUNNING
                    currentSpeed = (6.0f + Math.random() * 1.5f).toFloat()
                }

                if (dist <= currentSpeed + 1.5f || dist < 1.0f) {
                    // 到达目的地
                    currentExactX = targetX
                    currentExactY = targetY
                    layoutParams.x = targetX.toInt()
                    layoutParams.y = targetY.toInt()
                    try {
                        windowManager.updateViewLayout(this, layoutParams)
                    } catch (_: Exception) {}

                    if (motionState == PetMotionState.RUNNING) {
                        val p = Math.random()
                        when {
                            p < 0.40 -> {
                                motionState = PetMotionState.SHAKING
                                stateTicks = 22 // 跑完急速甩毛
                            }
                            p < 0.70 -> {
                                motionState = PetMotionState.SNIFFING
                                stateTicks = 35
                            }
                            else -> {
                                motionState = PetMotionState.IDLE
                                idleTicks = 0
                                restDuration = (40..70).random()
                            }
                        }
                    } else {
                        if (Math.random() < 0.35) {
                            motionState = PetMotionState.SNIFFING
                            stateTicks = 35
                        } else {
                            motionState = PetMotionState.IDLE
                            idleTicks = 0
                            restDuration = (35..65).random()
                        }
                    }
                } else {
                    val stepX = (dx / dist) * currentSpeed
                    val stepY = (dy / dist) * currentSpeed

                    if (!stepX.isNaN() && !stepY.isNaN()) {
                        currentExactX = (currentExactX + stepX).coerceIn(bounds.left, bounds.right)
                        currentExactY = (currentExactY + stepY).coerceIn(bounds.top, bounds.bottom)
                        walkDirection = if (dx >= 0) 1 else -1

                        layoutParams.x = currentExactX.toInt()
                        layoutParams.y = currentExactY.toInt()
                        try {
                            windowManager.updateViewLayout(this, layoutParams)
                        } catch (_: Exception) {}
                    }
                }
            }

            // 12. 憋急小碎步寻隐蔽角落
            PetMotionState.PRANCE_URGENT -> {
                stateTicks--
                val dx = targetX - currentExactX
                val dy = targetY - currentExactY
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist > 10f) {
                    val stepX = (dx / dist) * currentSpeed
                    val stepY = (dy / dist) * currentSpeed
                    if (!stepX.isNaN() && !stepY.isNaN()) {
                        currentExactX = (currentExactX + stepX).coerceIn(bounds.left, bounds.right)
                        currentExactY = (currentExactY + stepY).coerceIn(bounds.top, bounds.bottom)
                        walkDirection = if (dx >= 0) 1 else -1
                        layoutParams.x = currentExactX.toInt()
                        layoutParams.y = currentExactY.toInt()
                        try { windowManager.updateViewLayout(this, layoutParams) } catch (_: Exception) {}
                    }
                }

                if (stateTicks <= 0) {
                    if (eliminationIsPoop) {
                        motionState = PetMotionState.POOPING
                        stateTicks = 85 // 拱背深蹲约 2.6 秒
                        showSpeechBubble("主人看好我，别让坏人过来... 🥺", 85)
                    } else {
                        motionState = PetMotionState.PEEING
                        stateTicks = 75 // 抬腿撒尿标记约 2.3 秒
                        showSpeechBubble("抬腿标记！这里归我守护 🐾", 75)
                    }
                }
            }

            // 13. 屏幕边缘深蹲拱背排便（眼神锁定求保护）
            PetMotionState.POOPING -> {
                stateTicks--
                if (stateTicks <= 0) {
                    // 落地排泄物小徽章
                    val wasteX = if (walkDirection == 1) layoutParams.x - 25 else layoutParams.x + 135
                    val wasteY = layoutParams.y + 115
                    com.couple.pet.service.PetFloatingService.getInstance()?.spawnDesktopWaste(wasteX, wasteY, isPoop = true)

                    // 开启拉后一身轻疯狂冲刺 (Zoomies)
                    motionState = PetMotionState.POST_ZOOMIES
                    zoomiesLapsRemaining = 3
                    targetX = if (walkDirection == 1) bounds.left else bounds.right
                    walkDirection = if (targetX >= currentExactX) 1 else -1
                    currentSpeed = 8.5f
                    showSpeechBubble("拉完啦！一身轻松冲冲冲 💨💨", 90)
                }
            }

            // 14. 抬腿撒尿标记领地
            PetMotionState.PEEING -> {
                stateTicks--
                if (stateTicks <= 0) {
                    val wasteX = if (walkDirection == 1) layoutParams.x - 20 else layoutParams.x + 125
                    val wasteY = layoutParams.y + 115
                    com.couple.pet.service.PetFloatingService.getInstance()?.spawnDesktopWaste(wasteX, wasteY, isPoop = false)

                    motionState = PetMotionState.IDLE
                    idleTicks = 0
                    restDuration = 40
                }
            }

            // 15. 拉完一身轻全屏疯狂冲刺 (Zoomies)
            PetMotionState.POST_ZOOMIES -> {
                val dx = targetX - currentExactX
                if (abs(dx) > 20f) {
                    val step = (if (targetX >= currentExactX) 1f else -1f) * currentSpeed
                    currentExactX = (currentExactX + step).coerceIn(bounds.left, bounds.right)
                    layoutParams.x = currentExactX.toInt()
                    try { windowManager.updateViewLayout(this, layoutParams) } catch (_: Exception) {}
                } else {
                    zoomiesLapsRemaining--
                    if (zoomiesLapsRemaining > 0) {
                        targetX = if (targetX <= bounds.left + 20f) bounds.right else bounds.left
                        walkDirection = if (targetX >= currentExactX) 1 else -1
                    } else {
                        motionState = PetMotionState.IDLE
                        idleTicks = 0
                        restDuration = 45
                        desktopSpeechBubble = null
                    }
                }
            }

            // 16. 抓现行瞬间石化定格
            PetMotionState.BUSTED -> {
                stateTicks--
                if (stateTicks <= 0) {
                    motionState = PetMotionState.WALKING
                    desktopSpeechBubble = null
                }
            }

            else -> {
                motionState = PetMotionState.IDLE
                idleTicks = 0
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val centerX = w / 2f
        val centerY = h / 2f + 5f

        val isRunning = motionState == PetMotionState.RUNNING || motionState == PetMotionState.POST_ZOOMIES
        val isWalking = motionState == PetMotionState.WALKING
        val isSniffing = motionState == PetMotionState.SNIFFING
        val isShaking = motionState == PetMotionState.SHAKING
        val isIdle = motionState == PetMotionState.IDLE
        val isNapping = motionState == PetMotionState.NAPPING
        val isStretching = motionState == PetMotionState.STRETCHING
        val isYawning = motionState == PetMotionState.YAWNING
        val isChasingTail = motionState == PetMotionState.CHASING_TAIL
        val isScratching = motionState == PetMotionState.SCRATCHING
        val isDigging = motionState == PetMotionState.DIGGING
        val isBegging = motionState == PetMotionState.BEGGING
        val isBellyUp = motionState == PetMotionState.BELLY_UP
        val isDragged = motionState == PetMotionState.DRAGGED
        val isPrancing = motionState == PetMotionState.PRANCE_URGENT
        val isPooping = motionState == PetMotionState.POOPING
        val isPeeing = motionState == PetMotionState.PEEING
        val isBusted = motionState == PetMotionState.BUSTED
        val isPostZoomies = motionState == PetMotionState.POST_ZOOMIES

        // 垂直起伏或生物动作偏移
        val motionOffsetY = when {
            isPostZoomies -> -abs(sin(tick * 0.65).toFloat()) * 10f
            isRunning -> -abs(sin(tick * 0.52).toFloat()) * 8.5f // 活泼的大跨步跃起
            isWalking -> sin(tick * 0.24).toFloat() * 2.2f       // 悠闲温和微晃
            isPrancing -> sin(tick * 0.8).toFloat() * 3.5f       // 憋急小碎步微颠
            isPooping -> 8.5f + sin(tick * 0.6).toFloat() * 0.8f // 拱背深蹲下沉
            isPeeing -> 3.5f
            isBusted -> sin(tick * 1.5).toFloat() * 1.2f         // 僵硬定格微颤
            isSniffing -> 4.5f + sin(tick * 0.6).toFloat() * 1.5f // 低头嗅闻微耸动
            isNapping -> 7.0f + sin(tick * 0.08).toFloat() * 2.2f // 趴睡沉稳呼吸
            isStretching -> 3.0f                                  // 伸懒腰伏地
            isDigging -> sin(tick * 1.2).toFloat() * 3.0f        // 刨坑上下颠簸
            isDragged -> sin(tick * 0.45).toFloat() * 3.0f
            isIdle -> sin(tick * 0.12).toFloat() * 2.0f          // 待机呼吸
            else -> 0f
        }

        // 身体前倾倾角
        val bodyTiltAngle = when {
            motionState == PetMotionState.CLIMBING -> sin(tick * 0.14).toFloat() * 7.5f // 悬空荡秋千摇晃
            isPostZoomies -> 14.0f * walkDirection               // 极速俯冲
            isRunning -> 7.0f * walkDirection
            isWalking -> sin(tick * 0.24).toFloat() * 4.5f * walkDirection
            isPrancing -> sin(tick * 0.4).toFloat() * 5.0f * walkDirection
            isPeeing -> -8.0f * walkDirection                    // 抬腿撒尿侧倾
            isPooping -> 4.0f * walkDirection                    // 拱背微倾
            isBusted -> 0f                                       // 石化定格
            isSniffing -> 8.5f * walkDirection
            isDigging -> 13.0f * walkDirection // 刨坑前倾
            isStretching -> -16.0f * walkDirection // 伸懒腰翘臀
            isScratching -> 11.0f * walkDirection // 挠痒歪坐
            isShaking -> sin(tick * 1.3).toFloat() * 13.0f // 甩毛狂摇
            isIdle -> {
                // 偶尔萌化歪头 11° 打量主人
                if ((tick % 180) in 70..130) 11.0f else 0f
            }
            else -> 0f
        }

        // 动态形变缩放 (Squash & Stretch)
        var dynamicScaleX = scaleXFactor
        var dynamicScaleY = scaleYFactor
        if (isRunning) {
            val gallopPhase = sin(tick * 0.52).toFloat()
            dynamicScaleX *= (1.0f + gallopPhase * 0.09f)
            dynamicScaleY *= (1.0f - gallopPhase * 0.07f)
        } else if (isNapping) {
            // 趴睡时压扁成软乎乎的小肉卷
            dynamicScaleX *= 1.12f
            dynamicScaleY *= 0.88f
        } else if (isPooping) {
            // 拱背深蹲挤压
            dynamicScaleX *= 1.08f
            dynamicScaleY *= 0.88f
        }

        canvas.save()
        canvas.scale(dynamicScaleX, dynamicScaleY, centerX, centerY + 35f)

        // 奔跑脚底烟尘
        if (isRunning) {
            drawRunningDustPuffs(canvas, centerX, centerY + 28f)
        }

        // 刨坑时后方飞扬的小泥星
        if (isDigging) {
            drawDiggingDirt(canvas, centerX, centerY + 28f)
        }

        // 追尾巴模式旋转
        if (isChasingTail) {
            canvas.rotate(spinAngle, centerX, centerY + 20f)
        }

        // 翻肚皮模式旋转 (四脚朝天)
        if (isBellyUp) {
            canvas.rotate(180f, centerX, centerY + 18f)
        }

        canvas.save()
        canvas.rotate(bodyTiltAngle, centerX, centerY + 25f)

        // 跟随朝向镜像翻转
        canvas.scale(walkDirection.toFloat(), 1f, centerX, centerY)

        // 绘制尾巴
        drawTail(canvas, centerX, centerY, motionOffsetY)

        // 绘制身体与四肢
        drawBody(canvas, centerX, centerY, motionOffsetY)

        // 绘制脑袋与耳朵
        drawHeadAndEars(canvas, centerX, centerY, motionOffsetY)

        // 绘制五官、大萌眼与小舌头
        drawFaceFeatures(canvas, centerX, centerY, motionOffsetY)

        // 绘制前爪爪与胸口小爱心
        drawFrontPawsAndHeart(canvas, centerX, centerY, motionOffsetY)

        // 绘制当前形态专属装扮与特征 (奶嘴/草莓包/大骨头/红领巾/小蝴蝶结/对戒/天使羽翼)
        drawStageAccessories(canvas, centerX, centerY, motionOffsetY)

        // 嗅嗅气味记号
        if (isSniffing) {
            drawSniffScentMarks(canvas, centerX, centerY + motionOffsetY)
        }

        canvas.restore()
        canvas.restore()

        // 降落伞缓缓降落时撑开的手绘降落伞
        if (isParachuteOpen) {
            drawParachute(canvas, centerX, centerY - 45f)
        }

        // 2. 状态标识与生物状态气泡
        if (isNapping) {
            drawNappingZzz(canvas, centerX + 18f, centerY - 45f)
        } else if (isBegging) {
            drawBeggingBubble(canvas, centerX + 20f, centerY - 50f)
        } else {
            drawBatteryIndicators(canvas, centerX, centerY - 55f)
        }

        // 3. 点击冒红心
        if (showHeart) {
            drawFloatingHeart(canvas, centerX, centerY - 60f + heartOffsetY)
        }

        // 4. 生理与心理状态对话气泡
        if (desktopSpeechBubble != null && bubbleTicksRemaining > 0) {
            drawSpeechBubble(canvas, centerX, centerY - 54f, desktopSpeechBubble!!)
        }
    }

    /**
     * 绘制萌系对话气泡（手绘圆角线框 + 指向小狗的小尾巴）
     */
    private fun drawSpeechBubble(canvas: Canvas, cx: Float, topY: Float, text: String) {
        val textWidth = bubbleTextPaint.measureText(text)
        val padH = 10f
        val bubbleW = (textWidth + padH * 2f).coerceAtMost(226f)
        val bubbleH = 26f
        val bubbleLeft = (cx - bubbleW / 2f).coerceIn(6f, width - 6f - bubbleW)
        val bubbleRight = bubbleLeft + bubbleW
        val bubbleTop = (topY - bubbleH).coerceAtLeast(4f)
        val bubbleBottom = bubbleTop + bubbleH

        val rect = RectF(bubbleLeft, bubbleTop, bubbleRight, bubbleBottom)
        canvas.drawRoundRect(rect, 10f, 10f, bubbleBgPaint)
        canvas.drawRoundRect(rect, 10f, 10f, fineStrokePaint)

        // 向下尖角小尾巴
        val arrowX = cx.coerceIn(bubbleLeft + 14f, bubbleRight - 14f)
        val arrowY = bubbleBottom
        val tailP = Path().apply {
            moveTo(arrowX - 4f, arrowY)
            lineTo(arrowX, arrowY + 5f)
            lineTo(arrowX + 4f, arrowY)
            close()
        }
        canvas.drawPath(tailP, bubbleBgPaint)
        canvas.drawLine(arrowX - 4f, arrowY, arrowX, arrowY + 5f, fineStrokePaint)
        canvas.drawLine(arrowX, arrowY + 5f, arrowX + 4f, arrowY, fineStrokePaint)

        // 居中文本渲染
        val fm = bubbleTextPaint.fontMetrics
        val textY = bubbleTop + (bubbleH - (fm.descent + fm.ascent)) / 2f
        canvas.drawText(text, (bubbleLeft + bubbleRight) / 2f, textY, bubbleTextPaint)
    }

    /**
     * 睡觉打瞌睡时向上浮动的可爱 z Z Z
     */
    private fun drawNappingZzz(canvas: Canvas, cx: Float, topY: Float) {
        val float1 = (tick % 45) / 45f
        val y1 = topY - float1 * 26f
        val alpha1 = ((1f - float1) * 220).toInt().coerceIn(0, 255)
        smallEmojiPaint.alpha = alpha1
        canvas.drawText("z", cx - 6f, y1, smallEmojiPaint)

        val float2 = ((tick + 22) % 45) / 45f
        val y2 = topY - 14f - float2 * 26f
        val alpha2 = ((1f - float2) * 240).toInt().coerceIn(0, 255)
        emojiPaint.alpha = alpha2
        canvas.drawText("Z", cx + 8f, y2, emojiPaint)
    }

    /**
     * 摇尾乞怜求关注时头顶的可爱祈盼气泡 (🦴 或 💕)
     */
    private fun drawBeggingBubble(canvas: Canvas, cx: Float, topY: Float) {
        val bubbleY = topY + sin(tick * 0.2).toFloat() * 3f
        thoughtBubblePath.reset()
        thoughtBubblePath.addCircle(cx, bubbleY, 16f, Path.Direction.CW)
        canvas.drawPath(thoughtBubblePath, bubbleBgPaint)
        canvas.drawPath(thoughtBubblePath, strokePaint)

        canvas.drawCircle(cx - 10f, bubbleY + 16f, 3.5f, bubbleBgPaint)
        canvas.drawCircle(cx - 10f, bubbleY + 16f, 3.5f, strokePaint)
        canvas.drawCircle(cx - 15f, bubbleY + 22f, 2.0f, bubbleBgPaint)
        canvas.drawCircle(cx - 15f, bubbleY + 22f, 2.0f, strokePaint)

        val icon = if ((tick / 80) % 2 == 0) "🦴" else "💕"
        canvas.drawText(icon, cx, bubbleY + 7f, smallEmojiPaint)
    }

    /**
     * 奔跑冲刺烟尘圈
     */
    private fun drawRunningDustPuffs(canvas: Canvas, cx: Float, groundY: Float) {
        val trailX = cx - 26f * walkDirection
        for (i in 0..2) {
            val progress = ((tick * 2 + i * 4) % 12) / 12f
            val puffRadius = 3.5f + progress * 5.5f
            val puffX = trailX - (i * 12f + progress * 8f) * walkDirection
            val puffY = groundY - progress * 4.5f
            val alpha = ((1.0f - progress) * 180).toInt().coerceIn(0, 255)
            dustFillPaint.alpha = (alpha * 0.7f).toInt()
            dustPaint.alpha = alpha
            canvas.drawCircle(puffX, puffY, puffRadius, dustFillPaint)
            canvas.drawCircle(puffX, puffY, puffRadius, dustPaint)
        }
        val dashX = trailX - 35f * walkDirection
        dustPaint.alpha = 140
        canvas.drawLine(dashX, groundY - 3f, dashX - 12f * walkDirection, groundY - 3f, dustPaint)
        canvas.drawLine(dashX + 5f * walkDirection, groundY + 4f, dashX - 8f * walkDirection, groundY + 4f, dustPaint)
    }

    /**
     * 刨坑时飞溅的小土粒
     */
    private fun drawDiggingDirt(canvas: Canvas, cx: Float, groundY: Float) {
        val backX = cx - 18f * walkDirection
        for (i in 0..2) {
            val progress = ((tick * 3 + i * 5) % 15) / 15f
            val dirtX = backX - (progress * 28f) * walkDirection
            val dirtY = groundY - progress * 16f + (progress * progress) * 14f
            val alpha = ((1.0f - progress) * 220).toInt().coerceIn(0, 255)
            dustPaint.alpha = alpha
            canvas.drawCircle(dirtX, dirtY, 2.2f, strokePaint)
        }
    }

    /**
     * 嗅嗅探险气味轨迹
     */
    private fun drawSniffScentMarks(canvas: Canvas, cx: Float, cy: Float) {
        val faceY = cy - 8f
        val noseX = cx + 8f
        val noseY = faceY + 12f
        dustPaint.alpha = 190
        val waveOffset = sin(tick * 0.6).toFloat() * 1.5f
        canvas.drawCircle(noseX + 10f, noseY + waveOffset, 2.2f, dustPaint)
        canvas.drawCircle(noseX + 16f, noseY - 3f + waveOffset, 1.8f, dustPaint)
        canvas.drawCircle(noseX + 22f, noseY - 7f + waveOffset, 1.4f, dustPaint)
    }

    /**
     * 充电或低电状态标记
     */
    private fun drawBatteryIndicators(canvas: Canvas, cx: Float, topY: Float) {
        if (isCharging) {
            val sparkY = topY + sin(tick * 0.2).toFloat() * 3f
            canvas.drawText("⚡", cx + 26f, sparkY, emojiPaint)
        } else if (batteryLevel < 20) {
            canvas.drawText("💤", cx + 26f, topY, emojiPaint)
        }
    }

    private fun drawTail(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val tailBaseX = cx + 28f
        val tailBaseY = cy + 24f + breath

        // 尾巴生理反馈：快跑螺旋桨，求摸摸地面拍打，被拎起夹紧收拢
        val tailAngle = when (motionState) {
            PetMotionState.POST_ZOOMIES -> sin(tick * 1.1).toFloat() * 45f // 狂欢极速甩尾
            PetMotionState.RUNNING -> sin(tick * 0.72).toFloat() * 36f
            PetMotionState.POOPING -> -42f                                  // 拱背排便尾巴高高翘起卷着
            PetMotionState.PEEING -> -35f + sin(tick * 0.2).toFloat() * 4f // 撒尿尾巴高扬微颤
            PetMotionState.PRANCE_URGENT -> -20f + sin(tick * 0.8).toFloat() * 15f // 憋急夹尾小幅摆动
            PetMotionState.BUSTED -> 0f                                     // 石化完全僵直
            PetMotionState.BEGGING -> sin(tick * 0.85).toFloat() * 26f // 兴奋扫地
            PetMotionState.DRAGGED -> -55f                             // 夹尾巴
            PetMotionState.WALKING -> sin(tick * 0.24).toFloat() * 20f
            PetMotionState.SNIFFING -> -28f + sin(tick * 0.65).toFloat() * 14f
            PetMotionState.NAPPING -> -12f
            PetMotionState.STRETCHING -> 32f + sin(tick * 0.4).toFloat() * 15f
            PetMotionState.IDLE -> sin(tick * 0.16).toFloat() * 16f
            else -> 0f
        }

        canvas.save()
        canvas.rotate(tailAngle, tailBaseX, tailBaseY)

        tailPath.reset()
        tailPath.moveTo(tailBaseX, tailBaseY)
        tailPath.quadTo(tailBaseX + 16f, tailBaseY - 12f, tailBaseX + 10f, tailBaseY - 22f)
        tailPath.quadTo(tailBaseX + 5f, tailBaseY - 24f, tailBaseX + 3f, tailBaseY - 17f)
        tailPath.quadTo(tailBaseX + 7f, tailBaseY - 8f, tailBaseX, tailBaseY + 6f)
        tailPath.close()

        canvas.drawPath(tailPath, furPaint)
        canvas.drawPath(tailPath, strokePaint)
        canvas.restore()
    }

    private fun drawBody(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val bodyY = cy + 15f + breath
        bodyPath.reset()
        bodyPath.moveTo(cx - 26f, bodyY - 8f)
        bodyPath.quadTo(cx - 36f, bodyY + 18f, cx - 24f, bodyY + 27f)
        bodyPath.quadTo(cx - 17f, bodyY + 29f, cx - 12f, bodyY + 27f)
        bodyPath.lineTo(cx + 12f, bodyY + 27f)
        bodyPath.quadTo(cx + 17f, bodyY + 29f, cx + 24f, bodyY + 27f)
        bodyPath.quadTo(cx + 36f, bodyY + 18f, cx + 26f, bodyY - 8f)
        bodyPath.close()

        canvas.drawPath(bodyPath, furPaint)
        canvas.drawPath(bodyPath, strokePaint)

        // 四肢生理本能微动：被拎起空中扑腾 (Air-Paddling) / 奔跑大步倒腾 / 挠痒抬后腿 / 抬腿撒尿
        if (motionState == PetMotionState.PEEING) {
            // 抬起一条后腿撒尿标记
            canvas.save()
            val liftLegX = cx - 18f
            canvas.rotate(-38f, liftLegX, bodyY + 22f)
            canvas.drawRoundRect(liftLegX - 3.5f, bodyY + 10f, liftLegX + 3.5f, bodyY + 28f, 3f, 3f, furPaint)
            canvas.drawRoundRect(liftLegX - 3.5f, bodyY + 10f, liftLegX + 3.5f, bodyY + 28f, 3f, 3f, strokePaint)
            canvas.restore()

            // 另一条支撑后腿与前腿
            canvas.drawCircle(cx - 8f, bodyY + 26f, 4.8f, furPaint)
            canvas.drawCircle(cx - 8f, bodyY + 26f, 4.8f, strokePaint)

            canvas.drawCircle(cx + 18f, bodyY + 26f, 4.8f, furPaint)
            canvas.drawCircle(cx + 18f, bodyY + 26f, 4.8f, strokePaint)

            // 小水滴飞溅微粒
            canvas.drawCircle(cx - 28f, bodyY + 24f, 1.8f, fineStrokePaint)
            canvas.drawCircle(cx - 34f, bodyY + 28f, 1.4f, fineStrokePaint)
        } else {
            val legStride = when (motionState) {
                PetMotionState.POST_ZOOMIES -> sin(tick * 0.72).toFloat() * 9.0f
                PetMotionState.RUNNING -> sin(tick * 0.52).toFloat() * 7.5f
                PetMotionState.WALKING -> sin(tick * 0.24).toFloat() * 3.5f
                PetMotionState.PRANCE_URGENT -> sin(tick * 0.85).toFloat() * 4.5f
                PetMotionState.DRAGGED -> sin(tick * 0.65).toFloat() * 6.5f // 慌乱扑腾爪爪
                PetMotionState.BELLY_UP -> sin(tick * 0.55).toFloat() * 5.0f
                else -> 0f
            }

            val scratchY = if (motionState == PetMotionState.SCRATCHING) sin(tick * 1.5).toFloat() * 8f - 8f else 0f

            // 后腿与前腿
            canvas.drawCircle(cx - 18f - legStride * 0.4f, bodyY + 26f + legStride + scratchY, 4.8f, furPaint)
            canvas.drawCircle(cx - 18f - legStride * 0.4f, bodyY + 26f + legStride + scratchY, 4.8f, strokePaint)

            canvas.drawCircle(cx + 18f + legStride * 0.4f, bodyY + 26f - legStride, 4.8f, furPaint)
            canvas.drawCircle(cx + 18f + legStride * 0.4f, bodyY + 26f - legStride, 4.8f, strokePaint)
        }
    }

    private fun drawHeadAndEars(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val headY = cy - 10f + breath

        // 耳朵生理迎风与抽动反馈
        val leftEarAngle = when (motionState) {
            PetMotionState.POST_ZOOMIES -> -25f + sin(tick * 0.75).toFloat() * 15f // 狂奔飞耳
            PetMotionState.RUNNING -> -20f + sin(tick * 0.52).toFloat() * 12f // 迎风向后掀起
            PetMotionState.BUSTED -> 0f                                       // 石化僵直
            PetMotionState.POOPING -> -10f                                    // 拱背微后缩
            PetMotionState.PRANCE_URGENT -> sin(tick * 0.8).toFloat() * 10f   // 急迫抖耳
            PetMotionState.SHAKING -> sin(tick * 1.3).toFloat() * 26f         // 甩毛狂拍
            PetMotionState.SNIFFING -> 8f                                     // 探寻前耸
            PetMotionState.NAPPING -> 12f                                     // 趴睡贴耳
            PetMotionState.DRAGGED -> -25f                                    // 受困紧贴
            PetMotionState.WALKING -> sin(tick * 0.24).toFloat() * 4.0f
            else -> 0f
        }

        val rightEarAngle = when (motionState) {
            PetMotionState.POST_ZOOMIES -> -22f - sin(tick * 0.75).toFloat() * 15f
            PetMotionState.RUNNING -> -18f - sin(tick * 0.52).toFloat() * 12f
            PetMotionState.BUSTED -> 0f
            PetMotionState.POOPING -> -10f
            PetMotionState.PRANCE_URGENT -> -sin(tick * 0.8).toFloat() * 10f
            PetMotionState.SHAKING -> -sin(tick * 1.3).toFloat() * 26f
            PetMotionState.SNIFFING -> 8f
            PetMotionState.NAPPING -> 12f
            PetMotionState.DRAGGED -> -25f
            PetMotionState.WALKING -> -sin(tick * 0.24).toFloat() * 4.0f
            else -> 0f
        }

        // 左耳
        canvas.save()
        canvas.rotate(leftEarAngle, cx - 24f, headY - 14f)
        leftEarPath.reset()
        leftEarPath.moveTo(cx - 24f, headY - 14f)
        leftEarPath.cubicTo(cx - 45f, headY - 14f, cx - 44f, headY + 16f, cx - 25f, headY + 12f)
        leftEarPath.close()
        canvas.drawPath(leftEarPath, furPaint)
        canvas.drawPath(leftEarPath, strokePaint)
        canvas.restore()

        // 右耳
        canvas.save()
        canvas.rotate(rightEarAngle, cx + 24f, headY - 14f)
        rightEarPath.reset()
        rightEarPath.moveTo(cx + 24f, headY - 14f)
        rightEarPath.cubicTo(cx + 45f, headY - 14f, cx + 44f, headY + 16f, cx + 25f, headY + 12f)
        rightEarPath.close()
        canvas.drawPath(rightEarPath, furPaint)
        canvas.drawPath(rightEarPath, strokePaint)
        canvas.restore()

        // 脑袋
        headPath.reset()
        headPath.moveTo(cx - 28f, headY)
        headPath.cubicTo(cx - 30f, headY - 26f, cx + 30f, headY - 26f, cx + 28f, headY)
        headPath.cubicTo(cx + 34f, headY + 22f, cx - 34f, headY + 22f, cx - 28f, headY)
        headPath.close()

        canvas.drawPath(headPath, furPaint)
        canvas.drawPath(headPath, strokePaint)
    }

    private fun drawFaceFeatures(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val faceY = cy - 8f + breath
        val isRunning = motionState == PetMotionState.RUNNING
        val isNapping = motionState == PetMotionState.NAPPING
        val isBegging = motionState == PetMotionState.BEGGING
        val isYawning = motionState == PetMotionState.YAWNING
        val isBellyUp = motionState == PetMotionState.BELLY_UP
        val isDragged = motionState == PetMotionState.DRAGGED
        val isPrancing = motionState == PetMotionState.PRANCE_URGENT
        val isPooping = motionState == PetMotionState.POOPING
        val isPeeing = motionState == PetMotionState.PEEING
        val isBusted = motionState == PetMotionState.BUSTED
        val isPostZoomies = motionState == PetMotionState.POST_ZOOMIES

        val isBlinking = (motionState == PetMotionState.IDLE || motionState == PetMotionState.WALKING) &&
                (tick % 130) < 5

        // 眼睛渲染 (结合求摸摸水汪汪大眼睛 / 困倦眼 / 挣扎惊吓眼 / 开心眼 / 石化定格眼 / 排便锁定眼)
        when {
            isBusted -> {
                // 抓现行瞬间石化：圆瞪大眼 O.O
                canvas.drawCircle(cx - 18f, faceY - 3f, 5.2f, strokePaint)
                canvas.drawCircle(cx - 18f, faceY - 3f, 2.0f, eyePaint)
                canvas.drawCircle(cx + 18f, faceY - 3f, 5.2f, strokePaint)
                canvas.drawCircle(cx + 18f, faceY - 3f, 2.0f, eyePaint)

                // 额头大冷汗水滴
                canvas.drawOval(cx + 20f, faceY - 22f, cx + 27f, faceY - 11f, blushPaint)
                canvas.drawOval(cx + 20f, faceY - 22f, cx + 27f, faceY - 11f, strokePaint)

                // 旁边悬挂三根石化尴尬竖线 |||
                canvas.drawLine(cx - 24f, faceY - 22f, cx - 24f, faceY - 12f, fineStrokePaint)
                canvas.drawLine(cx - 20f, faceY - 24f, cx - 20f, faceY - 12f, fineStrokePaint)
                canvas.drawLine(cx - 16f, faceY - 22f, cx - 16f, faceY - 12f, fineStrokePaint)
            }
            isPooping -> {
                // 拱背深蹲：眼神专注对视锁定主人求庇护 (大豆豆眼 + 明亮双高光)
                canvas.drawCircle(cx - 18f, faceY - 3f, 4.6f, eyePaint)
                canvas.drawCircle(cx + 18f, faceY - 3f, 4.6f, eyePaint)
                canvas.drawCircle(cx - 19.2f, faceY - 4.2f, 1.4f, eyeGlintPaint)
                canvas.drawCircle(cx + 16.8f, faceY - 4.2f, 1.4f, eyeGlintPaint)

                // 用力微汗
                canvas.drawOval(cx + 21f, faceY - 14f, cx + 25f, faceY - 8f, fineStrokePaint)
            }
            isPrancing -> {
                // 憋急小碎步：慌张小豆豆眼 + 急迫汗珠
                canvas.drawCircle(cx - 18f, faceY - 3f, 3.8f, eyePaint)
                canvas.drawCircle(cx + 18f, faceY - 3f, 3.8f, eyePaint)
                canvas.drawOval(cx + 21f, faceY - 16f, cx + 25f, faceY - 10f, fineStrokePaint)
            }
            isDragged -> {
                // 惊慌受困大圆眼 (O O)
                canvas.drawCircle(cx - 18f, faceY - 3f, 5.0f, strokePaint)
                canvas.drawCircle(cx + 18f, faceY - 3f, 5.0f, strokePaint)
                canvas.drawCircle(cx - 18f, faceY - 3f, 2.5f, eyePaint)
                canvas.drawCircle(cx + 18f, faceY - 3f, 2.5f, eyePaint)
            }
            isBegging -> {
                // 水汪汪求抚摸大萌眼 (带两颗纯真高光)
                canvas.drawCircle(cx - 18f, faceY - 3f, 5.5f, eyePaint)
                canvas.drawCircle(cx + 18f, faceY - 3f, 5.5f, eyePaint)
                // 亮晶晶主高光
                canvas.drawCircle(cx - 19.5f, faceY - 4.5f, 2.0f, eyeGlintPaint)
                canvas.drawCircle(cx + 16.5f, faceY - 4.5f, 2.0f, eyeGlintPaint)
                // 次高光
                canvas.drawCircle(cx - 16.5f, faceY - 1.5f, 1.2f, eyeGlintPaint)
                canvas.drawCircle(cx + 19.5f, faceY - 1.5f, 1.2f, eyeGlintPaint)
            }
            isYawning -> {
                // 打哈欠挤眼 (> <)
                canvas.drawLine(cx - 21f, faceY - 5f, cx - 15f, faceY - 2f, strokePaint)
                canvas.drawLine(cx - 15f, faceY - 2f, cx - 21f, faceY + 1f, strokePaint)
                canvas.drawLine(cx + 21f, faceY - 5f, cx + 15f, faceY - 2f, strokePaint)
                canvas.drawLine(cx + 15f, faceY - 2f, cx + 21f, faceY + 1f, strokePaint)
            }
            isNapping -> {
                // 沉睡平和闭眼 (u u)
                canvas.drawArc(cx - 21f, faceY - 4f, cx - 13f, faceY + 2f, 10f, 160f, false, strokePaint)
                canvas.drawArc(cx + 13f, faceY - 4f, cx + 21f, faceY + 2f, 10f, 160f, false, strokePaint)
            }
            showHeart || isRunning || isPostZoomies || isBellyUp -> {
                // 弯月笑眼 (^ ^)
                canvas.drawArc(cx - 22f, faceY - 8f, cx - 12f, faceY + 2f, 200f, 140f, false, strokePaint)
                canvas.drawArc(cx + 12f, faceY - 8f, cx + 22f, faceY + 2f, 200f, 140f, false, strokePaint)
            }
            isBlinking -> {
                // 自然眨眼闭合 (- -)
                canvas.drawLine(cx - 21f, faceY - 3f, cx - 13f, faceY - 3f, strokePaint)
                canvas.drawLine(cx + 13f, faceY - 3f, cx + 21f, faceY - 3f, strokePaint)
            }
            batteryLevel < 20 && !isCharging -> {
                // 疲倦下垂眼
                canvas.drawLine(cx - 21f, faceY - 2f, cx - 14f, faceY - 2f, strokePaint)
                canvas.drawLine(cx + 14f, faceY - 2f, cx + 21f, faceY - 2f, strokePaint)
            }
            else -> {
                // 乌溜溜纯真豆豆眼
                canvas.drawCircle(cx - 18f, faceY - 3f, 4.0f, eyePaint)
                canvas.drawCircle(cx + 18f, faceY - 3f, 4.0f, eyePaint)
                // 纯真微光
                canvas.drawCircle(cx - 19f, faceY - 4.2f, 1.3f, eyeGlintPaint)
                canvas.drawCircle(cx + 17f, faceY - 4.2f, 1.3f, eyeGlintPaint)
            }
        }

        // 软萌粉腮红
        val blushAlpha = if (showHeart || isRunning || isPostZoomies || isBellyUp || isBegging || isPooping || isBusted) 235 else 160
        blushPaint.alpha = blushAlpha
        canvas.drawCircle(cx - 24f, faceY + 5f, 6.5f, blushPaint)
        canvas.drawCircle(cx + 24f, faceY + 5f, 6.5f, blushPaint)

        // 纽扣小黑鼻
        val noseOffsetY = if (motionState == PetMotionState.SNIFFING) sin(tick * 0.7).toFloat() * 1.8f else 0f
        canvas.drawOval(cx - 4.0f, faceY + 2f + noseOffsetY, cx + 4.0f, faceY + 6.8f + noseOffsetY, eyePaint)

        // 嘴巴与舌头动作
        when {
            isBusted -> {
                // 震惊微张小圆嘴 o
                canvas.drawCircle(cx, faceY + 9f, 2.8f, fineStrokePaint)
            }
            isPooping -> {
                // 专注紧抿一字嘴
                canvas.drawLine(cx - 4.5f, faceY + 8f, cx + 4.5f, faceY + 8f, fineStrokePaint)
            }
            isPrancing -> {
                // 慌张波浪嘴 ~
                canvas.drawLine(cx - 5f, faceY + 8.5f, cx - 2f, faceY + 6.5f, fineStrokePaint)
                canvas.drawLine(cx - 2f, faceY + 6.5f, cx + 2f, faceY + 9.5f, fineStrokePaint)
                canvas.drawLine(cx + 2f, faceY + 9.5f, cx + 5f, faceY + 7.5f, fineStrokePaint)
            }
            isYawning -> {
                // 打哈欠张大嘴
                canvas.drawOval(cx - 6f, faceY + 6f, cx + 6f, faceY + 16f, tonguePaint)
                canvas.drawOval(cx - 6f, faceY + 6f, cx + 6f, faceY + 16f, fineStrokePaint)
            }
            isRunning || isPostZoomies -> {
                // 奔跑吐舌头哈气 (👅)
                val tongueBounce = sin(tick * (if (isPostZoomies) 0.75 else 0.52)).toFloat() * 2.2f
                val tongueX = cx + 4.5f
                val tongueY = faceY + 8f + tongueBounce

                tonguePath.reset()
                tonguePath.moveTo(tongueX - 5.5f, tongueY)
                tonguePath.cubicTo(tongueX - 6.5f, tongueY + 11f, tongueX + 6.5f, tongueY + 11f, tongueX + 5.5f, tongueY)
                tonguePath.close()

                canvas.drawPath(tonguePath, tonguePaint)
                canvas.drawPath(tonguePath, fineStrokePaint)
                canvas.drawLine(tongueX, tongueY + 2f, tongueX, tongueY + 8f, fineStrokePaint)
                canvas.drawArc(cx - 7f, faceY + 4f, cx + 7f, faceY + 10f, 10f, 160f, false, strokePaint)
            }
            else -> {
                // 温暖微笑嘴
                canvas.drawArc(cx - 7f, faceY + 4.5f, cx, faceY + 11.5f, 10f, 160f, false, strokePaint)
                canvas.drawArc(cx, faceY + 4.5f, cx + 7f, faceY + 11.5f, 10f, 160f, false, strokePaint)
            }
        }
    }

    private fun drawFrontPawsAndHeart(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val chestY = cy + 11f + breath
        val isPrancing = motionState == PetMotionState.PRANCE_URGENT
        val isPooping = motionState == PetMotionState.POOPING
        val isPeeing = motionState == PetMotionState.PEEING
        val isBusted = motionState == PetMotionState.BUSTED

        // 排泄、憋急与抓现行时不显示胸前红心
        if (!isPrancing && !isPooping && !isPeeing && !isBusted) {
            // 胸前小爱心
            heartPath.reset()
            val heartScale = 0.48f
            heartPath.moveTo(cx, chestY + 10f)
            heartPath.cubicTo(cx - 18f * heartScale, chestY - 18f * heartScale, cx - 36f * heartScale, chestY + 4f * heartScale, cx, chestY + 32f * heartScale)
            heartPath.cubicTo(cx + 36f * heartScale, chestY + 4f * heartScale, cx + 18f * heartScale, chestY - 18f * heartScale, cx, chestY + 10f)
            heartPath.close()

            canvas.drawPath(heartPath, heartPaint)
            canvas.drawPath(heartPath, strokePaint)
        }

        // 奔跑或刨坑时的前爪微动
        val isRunning = motionState == PetMotionState.RUNNING || motionState == PetMotionState.POST_ZOOMIES
        val isDigging = motionState == PetMotionState.DIGGING
        val isBegging = motionState == PetMotionState.BEGGING
        val isStretching = motionState == PetMotionState.STRETCHING

        val frontPawBounce = when {
            isRunning -> sin(tick * 0.52).toFloat() * 2.8f
            isDigging -> sin(tick * 1.4).toFloat() * 6.5f // 刨坑快速交替扒拉
            isPrancing -> sin(tick * 0.85).toFloat() * 2.0f
            else -> 0f
        }

        val stretchForward = if (isStretching) 10f else 0f
        val beggingUp = if (isBegging) -4f else 0f

        canvas.drawCircle(cx - 11f + stretchForward, chestY + 10f + frontPawBounce + beggingUp, 5.5f, furPaint)
        canvas.drawCircle(cx - 11f + stretchForward, chestY + 10f + frontPawBounce + beggingUp, 5.5f, strokePaint)

        canvas.drawCircle(cx + 11f + stretchForward, chestY + 10f - frontPawBounce + beggingUp, 5.5f, furPaint)
        canvas.drawCircle(cx + 11f + stretchForward, chestY + 10f - frontPawBounce + beggingUp, 5.5f, strokePaint)
    }

    private fun drawFloatingHeart(canvas: Canvas, cx: Float, cy: Float) {
        val floatHeart = Path()
        val s = 0.6f
        floatHeart.moveTo(cx, cy + 8f * s)
        floatHeart.cubicTo(cx - 20f * s, cy - 20f * s, cx - 40f * s, cy + 5f * s, cx, cy + 36f * s)
        floatHeart.cubicTo(cx + 40f * s, cy + 5f * s, cx + 20f * s, cy - 20f * s, cx, cy + 8f * s)
        floatHeart.close()

        canvas.drawPath(floatHeart, heartPaint)
        canvas.drawPath(floatHeart, strokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 如果正处于排泄相关动作中 (憋急小碎步、深蹲排便、抬腿撒尿)，点击触发瞬间被抓现行石化！
                if (motionState == PetMotionState.POOPING || motionState == PetMotionState.PEEING || motionState == PetMotionState.PRANCE_URGENT) {
                    motionState = PetMotionState.BUSTED
                    stateTicks = 85 // 石化定格约 2.6 秒
                    showSpeechBubble("(被抓现行，瞬间石化不敢动... 😳)", 85)
                    invalidate()
                    return true
                }
                // 如果正处于抓现行石化定格中，小狗如雕像般不能拖拽
                if (motionState == PetMotionState.BUSTED) {
                    return true
                }

                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                motionState = PetMotionState.DRAGGED
                idleTicks = 0
                currentExactX = initialX.toFloat()
                currentExactY = initialY.toFloat()
                targetX = currentExactX
                targetY = currentExactY
                scaleXFactor = 0.94f
                scaleYFactor = 1.08f
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                if (abs(dx) > 10 || abs(dy) > 10) {
                    isDragging = true
                }

                val bounds = getScreenBounds()
                val clampedX = (initialX + dx).coerceIn(bounds.left.toInt(), bounds.right.toInt())
                val clampedY = (initialY + dy).coerceIn(bounds.top.toInt(), bounds.bottom.toInt())

                layoutParams.x = clampedX
                layoutParams.y = clampedY
                currentExactX = clampedX.toFloat()
                currentExactY = clampedY.toFloat()
                targetX = currentExactX
                targetY = currentExactY
                try {
                    windowManager.updateViewLayout(this, layoutParams)
                } catch (_: Exception) {}
                return true
            }

            MotionEvent.ACTION_UP -> {
                scaleXFactor = 1.0f
                scaleYFactor = 1.0f
                val wasDragging = isDragging
                isDragging = false

                if (!wasDragging) {
                    performClick()
                    handlePetClick()
                } else {
                    onPetReleased()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                scaleXFactor = 1.0f
                scaleYFactor = 1.0f
                isDragging = false
                onPetReleased()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handlePetClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < 350) {
            // 双击：开启伴侣应用主界面
            lastClickTime = 0L
            launchMainActivity()
            return
        }
        lastClickTime = now

        // 抚摸消除无聊
        boredom = 0f

        // 如果小狗在打瞌睡，轻轻抚摸会温顺醒来
        if (motionState == PetMotionState.NAPPING) {
            motionState = PetMotionState.STRETCHING
            stateTicks = 25
            showHeart = true
            heartOffsetY = 0f
            invalidate()
            return
        }

        // 如果在乞怜求摸摸，一摸立即幸福翻肚皮
        if (motionState == PetMotionState.BEGGING) {
            motionState = PetMotionState.BELLY_UP
            stateTicks = 70 // 翻肚皮享受 2.2 秒
            showHeart = true
            heartOffsetY = 0f
            triggerSquashAndStretch()
            invalidate()
            return
        }

        // 短时间连续抚摸连击判定 (连续摸 3 次触发翻肚皮求摸摸本能)
        if (now - lastPetTime < 900) {
            affectionTaps++
            if (affectionTaps >= 3) {
                affectionTaps = 0
                motionState = PetMotionState.BELLY_UP
                stateTicks = 70
                showHeart = true
                heartOffsetY = 0f
                triggerSquashAndStretch()
                invalidate()
                return
            }
        } else {
            affectionTaps = 1
        }
        lastPetTime = now

        // 单击：摸摸小狗，开心眯眼、红心冒出、Q弹回弹
        PetSyncEngine.poke()
        showHeart = true
        heartOffsetY = 0f
        triggerSquashAndStretch()
        motionState = PetMotionState.IDLE
        idleTicks = 0
        restDuration = 45
        invalidate()
        handler.removeCallbacks(hideHeartRunnable)
        handler.postDelayed(hideHeartRunnable, 1200)
    }

    private val hideHeartRunnable = Runnable {
        showHeart = false
        invalidate()
    }

    private fun launchMainActivity() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }

    private fun onPetReleased() {
        val bounds = getScreenBounds()
        isDragging = false
        val releaseX = layoutParams.x.toFloat()
        val releaseY = layoutParams.y.toFloat()

        // 1. 边缘吸附判定：如果松手位置贴近屏幕左边缘或右边缘
        if (releaseX <= bounds.left + 40f) {
            currentExactX = bounds.left
            layoutParams.x = bounds.left.toInt()
            walkDirection = 1 // 朝向内侧探头
            motionState = PetMotionState.CLIMBING
            stateTicks = 120 // 边缘挂载 4 秒
            showSpeechBubble("抓住屏幕左边啦！两脚悬空晃悠悠 🐾", 90)
            triggerSquashAndStretch()
            try { windowManager.updateViewLayout(this, layoutParams) } catch (_: Exception) {}
            invalidate()
            return
        } else if (releaseX >= bounds.right - 40f) {
            currentExactX = bounds.right
            layoutParams.x = bounds.right.toInt()
            walkDirection = -1 // 朝向内侧探头
            motionState = PetMotionState.CLIMBING
            stateTicks = 120 // 边缘挂载 4 秒
            showSpeechBubble("爪爪抠紧右边框！探头偷看你 👀", 90)
            triggerSquashAndStretch()
            try { windowManager.updateViewLayout(this, layoutParams) } catch (_: Exception) {}
            invalidate()
            return
        }

        // 2. 高空释放降落伞判定：若在屏幕上方或中上方松手
        if (releaseY < bounds.bottom - 120f) {
            currentExactX = releaseX.coerceIn(bounds.left, bounds.right)
            currentExactY = releaseY
            layoutParams.x = currentExactX.toInt()
            layoutParams.y = currentExactY.toInt()
            targetX = currentExactX
            targetY = bounds.bottom
            motionState = PetMotionState.FALLING
            isParachuteOpen = true
            showSpeechBubble("小蒲公英降落伞开！缓缓降落 🪂", 90)
            invalidate()
            return
        }

        // 3. 常规地面释放：甩毛恢复
        currentExactX = releaseX.coerceIn(bounds.left, bounds.right)
        currentExactY = releaseY.coerceIn(bounds.top, bounds.bottom)
        layoutParams.x = currentExactX.toInt()
        layoutParams.y = currentExactY.toInt()
        targetX = currentExactX
        targetY = currentExactY

        // 落地生物反射：空中被抓放下后，先原地甩毛甩掉尘土 (0.6秒)
        motionState = PetMotionState.SHAKING
        stateTicks = 18
        idleTicks = 0
        restDuration = 40
        triggerSquashAndStretch()
        try {
            windowManager.updateViewLayout(this, layoutParams)
        } catch (_: Exception) {}
        invalidate()
    }

    private fun drawParachute(canvas: Canvas, cx: Float, topY: Float) {
        val sway = sin(tick * 0.12).toFloat() * 8f
        canvas.save()
        canvas.rotate(sway, cx, topY + 40f)

        // 3 条纤细拉绳
        val canopyY = topY - 30f
        canvas.drawLine(cx - 30f, canopyY, cx - 10f, topY + 30f, fineStrokePaint)
        canvas.drawLine(cx, canopyY - 4f, cx, topY + 30f, fineStrokePaint)
        canvas.drawLine(cx + 30f, canopyY, cx + 10f, topY + 30f, fineStrokePaint)

        // 降落伞手绘穹顶 (圆弧 + 烘焙白底 + 彩色条纹)
        val canopyPath = Path().apply {
            moveTo(cx - 38f, canopyY)
            cubicTo(cx - 40f, canopyY - 32f, cx + 40f, canopyY - 32f, cx + 38f, canopyY)
            quadTo(cx + 25f, canopyY - 6f, cx + 12f, canopyY)
            quadTo(cx, canopyY - 6f, cx - 12f, canopyY)
            quadTo(cx - 25f, canopyY - 6f, cx - 38f, canopyY)
            close()
        }
        val parachutePaint = Paint(furPaint).apply { color = "#FAF2E6".toColorInt() }
        canvas.drawPath(canopyPath, parachutePaint)
        canvas.drawPath(canopyPath, strokePaint)

        // 伞面中央手绘小心心
        val heartP = Path().apply {
            val s = 0.45f
            val hy = canopyY - 14f
            moveTo(cx, hy + 6f * s)
            cubicTo(cx - 16f * s, hy - 16f * s, cx - 32f * s, hy + 4f * s, cx, hy + 26f * s)
            cubicTo(cx + 32f * s, hy + 4f * s, cx + 16f * s, hy - 16f * s, cx, hy + 6f * s)
            close()
        }
        canvas.drawPath(heartP, heartPaint)
        canvas.drawPath(heartP, strokePaint)

        canvas.restore()
    }

    private fun drawStageAccessories(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val stage = PetSyncEngine.petStats.value.stage
        val chestY = cy + 14f + breath
        val headY = cy - 10f + breath

        // 伴学专注模式：戴黑框小眼镜
        if (PetSyncEngine.isPomodoroActive.value) {
            canvas.drawCircle(cx - 8f, headY, 5.2f, fineStrokePaint)
            canvas.drawCircle(cx + 8f, headY, 5.2f, fineStrokePaint)
            canvas.drawLine(cx - 2.8f, headY, cx + 2.8f, headY, fineStrokePaint)
        }

        when (stage) {
            PetGrowthStage.BABY -> {
                // 1. 奶糕幼崽期：可爱小口水兜 + 小奶嘴
                val bibPaint = Paint(furPaint).apply { color = "#FFF9C4".toColorInt() }
                canvas.drawRoundRect(cx - 12f, chestY, cx + 12f, chestY + 14f, 6f, 6f, bibPaint)
                canvas.drawRoundRect(cx - 12f, chestY, cx + 12f, chestY + 14f, 6f, 6f, fineStrokePaint)

                val pacifierPaint = Paint(furPaint).apply { color = "#80DEEA".toColorInt() }
                canvas.drawCircle(cx, headY + 8f, 3.8f, pacifierPaint)
                canvas.drawCircle(cx, headY + 8f, 3.8f, fineStrokePaint)
                canvas.drawCircle(cx, headY + 13f, 3f, fineStrokePaint)
            }
            PetGrowthStage.CHILD -> {
                // 2. 好奇探索期：草莓小挎包
                canvas.drawLine(cx - 16f, chestY - 6f, cx + 18f, chestY + 16f, fineStrokePaint)
                val bagPaint = Paint(furPaint).apply { color = "#FF6B81".toColorInt() }
                val leafPaint = Paint(furPaint).apply { color = "#81C784".toColorInt() }
                val bagX = cx + 18f
                val bagY = chestY + 16f
                canvas.drawOval(bagX - 6f, bagY - 6f, bagX + 6f, bagY + 7f, bagPaint)
                canvas.drawOval(bagX - 6f, bagY - 6f, bagX + 6f, bagY + 7f, fineStrokePaint)
                canvas.drawCircle(bagX - 2f, bagY - 6f, 2.2f, leafPaint)
                canvas.drawCircle(bagX + 2f, bagY - 6f, 2.2f, leafPaint)
            }
            PetGrowthStage.AWAKEN_FOODIE -> {
                // 3. 觉醒·食神圆滚体：胸前抱大肉骨头
                val bonePaint = Paint(furPaint).apply { color = "#FFF8E1".toColorInt() }
                val y = chestY + 8f
                canvas.drawRoundRect(cx - 14f, y - 3.5f, cx + 14f, y + 3.5f, 2f, 2f, bonePaint)
                canvas.drawRoundRect(cx - 14f, y - 3.5f, cx + 14f, y + 3.5f, 2f, 2f, fineStrokePaint)
                canvas.drawCircle(cx - 14f, y - 3.5f, 3.2f, bonePaint)
                canvas.drawCircle(cx - 14f, y - 3.5f, 3.2f, fineStrokePaint)
                canvas.drawCircle(cx - 14f, y + 3.5f, 3.2f, bonePaint)
                canvas.drawCircle(cx - 14f, y + 3.5f, 3.2f, fineStrokePaint)
                canvas.drawCircle(cx + 14f, y - 3.5f, 3.2f, bonePaint)
                canvas.drawCircle(cx + 14f, y - 3.5f, 3.2f, fineStrokePaint)
                canvas.drawCircle(cx + 14f, y + 3.5f, 3.2f, bonePaint)
                canvas.drawCircle(cx + 14f, y + 3.5f, 3.2f, fineStrokePaint)
            }
            PetGrowthStage.AWAKEN_SPORTY -> {
                // 4. 觉醒·活力特技体：运动小红领巾
                val redPaint = Paint(furPaint).apply { color = "#E53935".toColorInt() }
                val p = Path().apply {
                    moveTo(cx - 14f, chestY - 2f)
                    lineTo(cx + 14f, chestY - 2f)
                    lineTo(cx, chestY + 9f)
                    close()
                }
                canvas.drawPath(p, redPaint)
                canvas.drawPath(p, fineStrokePaint)
                val flutter = sin(tick * 0.4).toFloat() * 3.5f
                canvas.drawLine(cx, chestY + 9f, cx + 6f + flutter, chestY + 15f, strokePaint)
            }
            PetGrowthStage.AWAKEN_FLUFFY -> {
                // 5. 觉醒·优雅蓬蓬体：粉红小蝴蝶结
                val pinkPaint = Paint(furPaint).apply { color = "#FF4081".toColorInt() }
                val bx = cx
                val by = headY - 14f
                val leftWing = Path().apply {
                    moveTo(bx, by); lineTo(bx - 8f, by - 5f); lineTo(bx - 8f, by + 5f); close()
                }
                canvas.drawPath(leftWing, pinkPaint)
                canvas.drawPath(leftWing, fineStrokePaint)
                val rightWing = Path().apply {
                    moveTo(bx, by); lineTo(bx + 8f, by - 5f); lineTo(bx + 8f, by + 5f); close()
                }
                canvas.drawPath(rightWing, pinkPaint)
                canvas.drawPath(rightWing, fineStrokePaint)
                canvas.drawCircle(bx, by, 3f, pinkPaint)
                canvas.drawCircle(bx, by, 3f, fineStrokePaint)
            }
            PetGrowthStage.ADULT_GUARDIAN -> {
                // 6. 成熟·守护伴侣犬：情侣对戒项圈
                val collarPaint = Paint(furPaint).apply { color = "#5D4037".toColorInt() }
                val goldPaint = Paint(furPaint).apply { color = "#FFD700".toColorInt() }
                canvas.drawRoundRect(cx - 16f, chestY - 2f, cx + 16f, chestY + 3f, 2f, 2f, collarPaint)
                canvas.drawRoundRect(cx - 16f, chestY - 2f, cx + 16f, chestY + 3f, 2f, 2f, fineStrokePaint)
                canvas.drawCircle(cx, chestY + 6f, 3.8f, goldPaint)
                canvas.drawCircle(cx, chestY + 6f, 3.8f, fineStrokePaint)
                canvas.drawCircle(cx, chestY + 6f, 1.8f, furPaint)
            }
            PetGrowthStage.MYTHIC_ANGEL -> {
                // 7. 神化·天使羁绊体：天使光环与纯白双翼
                val goldStroke = Paint(strokePaint).apply { color = "#FFC107".toColorInt(); strokeWidth = 3f }
                val haloFloat = sin(tick * 0.15).toFloat() * 2f
                canvas.drawOval(cx - 14f, headY - 24f + haloFloat, cx + 14f, headY - 18f + haloFloat, goldStroke)

                val wingFlap = sin(tick * 0.3).toFloat() * 6f
                canvas.save()
                canvas.rotate(-wingFlap, cx - 24f, chestY)
                val leftWing = Path().apply {
                    moveTo(cx - 22f, chestY - 2f)
                    cubicTo(cx - 46f, chestY - 18f, cx - 44f, chestY + 12f, cx - 22f, chestY + 6f)
                    close()
                }
                canvas.drawPath(leftWing, furPaint)
                canvas.drawPath(leftWing, strokePaint)
                canvas.restore()

                canvas.save()
                canvas.rotate(wingFlap, cx + 24f, chestY)
                val rightWing = Path().apply {
                    moveTo(cx + 22f, chestY - 2f)
                    cubicTo(cx + 46f, chestY - 18f, cx + 44f, chestY + 12f, cx + 22f, chestY + 6f)
                    close()
                }
                canvas.drawPath(rightWing, furPaint)
                canvas.drawPath(rightWing, strokePaint)
                canvas.restore()
            }
        }
    }

    fun triggerSquashAndStretch() {
        val squashAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320
            interpolator = OvershootInterpolator(2.5f)
            addUpdateListener { anim ->
                val f = anim.animatedFraction
                val squashAmount = sin(f * Math.PI).toFloat() * 0.22f
                scaleXFactor = 1.0f + squashAmount
                scaleYFactor = 1.0f - squashAmount
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    scaleXFactor = 1.0f
                    scaleYFactor = 1.0f
                    invalidate()
                }
            })
        }
        squashAnim.start()
    }
}
