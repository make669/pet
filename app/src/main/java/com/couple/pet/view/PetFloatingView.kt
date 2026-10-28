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
        when (motionState) {
            PetMotionState.RUNNING -> energy = (energy - 0.22f).coerceAtLeast(0f)
            PetMotionState.CHASING_TAIL, PetMotionState.DIGGING -> energy = (energy - 0.30f).coerceAtLeast(0f)
            PetMotionState.WALKING -> energy = (energy - 0.05f).coerceAtLeast(0f)
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

                    // 根据体力与距离决策是慢走、冲刺撒欢还是嗅闻
                    val randAction = Math.random()
                    when {
                        // 精力充沛且距离适中 -> 兴奋撒欢快跑 (Zoomies)
                        dist > 120f && randAction < 0.52 && energy > 35f -> {
                            motionState = PetMotionState.RUNNING
                            currentSpeed = (5.8f + Math.random() * 1.6f).toFloat()
                        }
                        // 偶尔打个大哈欠
                        randAction < 0.12 && energy < 50f -> {
                            motionState = PetMotionState.YAWNING
                            stateTicks = 35
                        }
                        // 偶尔坐下挠后腿
                        randAction < 0.22 -> {
                            motionState = PetMotionState.SCRATCHING
                            stateTicks = 36 // 挠耳朵约 1.2 秒
                        }
                        // 停下好奇嗅嗅地面
                        randAction < 0.35 -> {
                            motionState = PetMotionState.SNIFFING
                            stateTicks = (30..45).random()
                        }
                        // 其余概率悠闲慢慢步巡游
                        else -> {
                            motionState = PetMotionState.WALKING
                            currentSpeed = (1.8f + Math.random() * 0.6f).toFloat()
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

        val isRunning = motionState == PetMotionState.RUNNING
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

        // 垂直起伏或生物动作偏移
        val motionOffsetY = when {
            isRunning -> -abs(sin(tick * 0.52).toFloat()) * 8.5f // 活泼的大跨步跃起
            isWalking -> sin(tick * 0.24).toFloat() * 2.2f       // 悠闲温和微晃
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
            isRunning -> 7.0f * walkDirection
            isWalking -> sin(tick * 0.24).toFloat() * 4.5f * walkDirection
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

        // 嗅嗅气味记号
        if (isSniffing) {
            drawSniffScentMarks(canvas, centerX, centerY + motionOffsetY)
        }

        canvas.restore()
        canvas.restore()

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
            PetMotionState.RUNNING -> sin(tick * 0.72).toFloat() * 36f
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

        // 四肢生理本能微动：被拎起空中扑腾 (Air-Paddling) / 奔跑大步倒腾 / 挠痒抬后腿
        val legStride = when (motionState) {
            PetMotionState.RUNNING -> sin(tick * 0.52).toFloat() * 7.5f
            PetMotionState.WALKING -> sin(tick * 0.24).toFloat() * 3.5f
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

    private fun drawHeadAndEars(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val headY = cy - 10f + breath

        // 耳朵生理迎风与抽动反馈
        val leftEarAngle = when (motionState) {
            PetMotionState.RUNNING -> -20f + sin(tick * 0.52).toFloat() * 12f // 迎风向后掀起
            PetMotionState.SHAKING -> sin(tick * 1.3).toFloat() * 26f         // 甩毛狂拍
            PetMotionState.SNIFFING -> 8f                                     // 探寻前耸
            PetMotionState.NAPPING -> 12f                                     // 趴睡贴耳
            PetMotionState.DRAGGED -> -25f                                    // 受困紧贴
            PetMotionState.WALKING -> sin(tick * 0.24).toFloat() * 4.0f
            else -> 0f
        }

        val rightEarAngle = when (motionState) {
            PetMotionState.RUNNING -> -18f - sin(tick * 0.52).toFloat() * 12f
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

        val isBlinking = (motionState == PetMotionState.IDLE || motionState == PetMotionState.WALKING) &&
                (tick % 130) < 5

        // 眼睛渲染 (结合求摸摸水汪汪大眼睛 / 困倦眼 / 挣扎惊吓眼 / 开心眼)
        when {
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
            showHeart || isRunning || isBellyUp -> {
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
        val blushAlpha = if (showHeart || isRunning || isBellyUp || isBegging) 235 else 160
        blushPaint.alpha = blushAlpha
        canvas.drawCircle(cx - 24f, faceY + 5f, 6.5f, blushPaint)
        canvas.drawCircle(cx + 24f, faceY + 5f, 6.5f, blushPaint)

        // 纽扣小黑鼻
        val noseOffsetY = if (motionState == PetMotionState.SNIFFING) sin(tick * 0.7).toFloat() * 1.8f else 0f
        canvas.drawOval(cx - 4.0f, faceY + 2f + noseOffsetY, cx + 4.0f, faceY + 6.8f + noseOffsetY, eyePaint)

        // 嘴巴与舌头动作
        when {
            isYawning -> {
                // 打哈欠张大嘴
                canvas.drawOval(cx - 6f, faceY + 6f, cx + 6f, faceY + 16f, tonguePaint)
                canvas.drawOval(cx - 6f, faceY + 6f, cx + 6f, faceY + 16f, fineStrokePaint)
            }
            isRunning -> {
                // 奔跑吐舌头哈气 (👅)
                val tongueBounce = sin(tick * 0.52).toFloat() * 2.2f
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

        // 胸前小爱心
        heartPath.reset()
        val heartScale = 0.48f
        heartPath.moveTo(cx, chestY + 10f)
        heartPath.cubicTo(cx - 18f * heartScale, chestY - 18f * heartScale, cx - 36f * heartScale, chestY + 4f * heartScale, cx, chestY + 32f * heartScale)
        heartPath.cubicTo(cx + 36f * heartScale, chestY + 4f * heartScale, cx + 18f * heartScale, chestY - 18f * heartScale, cx, chestY + 10f)
        heartPath.close()

        canvas.drawPath(heartPath, heartPaint)
        canvas.drawPath(heartPath, strokePaint)

        // 奔跑或刨坑时的前爪微动
        val isRunning = motionState == PetMotionState.RUNNING
        val isDigging = motionState == PetMotionState.DIGGING
        val isBegging = motionState == PetMotionState.BEGGING
        val isStretching = motionState == PetMotionState.STRETCHING

        val frontPawBounce = when {
            isRunning -> sin(tick * 0.52).toFloat() * 2.8f
            isDigging -> sin(tick * 1.4).toFloat() * 6.5f // 刨坑快速交替扒拉
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
        currentExactX = layoutParams.x.toFloat().coerceIn(bounds.left, bounds.right)
        currentExactY = layoutParams.y.toFloat().coerceIn(bounds.top, bounds.bottom)
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

    private fun triggerSquashAndStretch() {
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
