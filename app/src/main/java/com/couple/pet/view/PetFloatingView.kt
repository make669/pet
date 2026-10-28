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
 * 深度生命力拟真交互系统：
 * 1. 真实犬类行为状态机：慢悠漫步 (WALKING)、兴奋撒欢快跑 (RUNNING)、停下低头嗅嗅 (SNIFFING)、全身甩毛抖抖 (SHAKING)、歪头观察眨眼发呆 (IDLE)
 * 2. 走路 vs 快跑强烈视觉差异：
 *    - 慢慢走：从容步速(2.0px/帧)、闲适晃脑、微幅度小步倒腾、轻摇小尾巴、静谧微笑
 *    - 撒欢跑：冲刺步速(6.2px/帧)、大跳跃颠簸起伏、随风向后飞起的耳朵、吐出粉嫩小舌头哈气、螺旋桨飞速摇尾、脚底手绘冲刺烟尘圈
 * 3. 探险与嗅闻细节：低头贴地嗅闻、小黑鼻耸动、嗅探气味轨迹微动点
 * 4. 甩毛萌动：快跑后原地高频抖动全身，耳朵疯狂拍打，治愈感拉满
 * 5. 待机眨眼与好奇歪头：每隔几秒自然眨动豆豆眼，偶尔好奇歪头 11° 打量屏幕前的你
 * 6. 严密边界防瞬移与自愈触控：统一屏幕活动安全区，拖拽释放自愈，绝不卡停
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

    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E1E1E".toColorInt()
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

    private val fineStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E1E1E".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
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

    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
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
    private var restDuration = 45 // 停留歇息时长（约 1.2~2.0 秒）
    private var currentSpeed = 2.0f // 当前行进速度（慢走约 2.0f，快跑约 6.2f）
    private var stateTicks = 0 // 特殊动作（嗅嗅/甩毛）持续倒计时

    // 4. 双击检测
    private var lastClickTime = 0L

    // 5. 系统电量与充电状态
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

    private val handler = Handler(Looper.getMainLooper())
    private val animRunnable = object : Runnable {
        override fun run() {
            tick++
            if (showHeart) {
                heartOffsetY = (heartOffsetY - 2.0f).coerceAtLeast(-45f)
            }

            // 全屏幕自由漫步与生命力状态机驱动
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
     * 统一屏幕安全活动边界，避免拖拽与漫步边界脱节导致瞬移闪现
     */
    private fun getScreenBounds(): RectF {
        val screenW = context.resources.displayMetrics.widthPixels.toFloat()
        val screenH = context.resources.displayMetrics.heightPixels.toFloat()
        val minX = 20f
        val maxX = (screenW - width - 20f).coerceAtLeast(minX)
        val minY = 120f // 避免挡住顶部通知栏
        val maxY = (screenH - height - 80f).coerceAtLeast(minY) // 贴合底部导航栏上方，舒适停靠地面
        return RectF(minX, minY, maxX, maxY)
    }

    /**
     * 全屏幕 2D 自主游荡与拟真犬类行为状态机引擎
     */
    private fun updateFreeRoam() {
        if (isDragging) return

        // 容错自愈：非拖拽中若处于受困态，自愈为 IDLE
        if (motionState == PetMotionState.DRAGGED) {
            motionState = PetMotionState.IDLE
            idleTicks = 0
        }

        val bounds = getScreenBounds()

        when (motionState) {
            PetMotionState.IDLE -> {
                idleTicks++
                if (idleTicks > restDuration) {
                    idleTicks = 0
                    restDuration = (35..65).random()

                    // 判断是否在近底面
                    val isNearBottom = currentExactY >= bounds.bottom - 120f

                    // 挑选下一个巡视/探索目标点
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

                    // 决策本次行为：慢步走 vs 撒欢快跑 vs 停下嗅嗅
                    val randAction = Math.random()
                    when {
                        // 距离适中或较远时，高概率触发兴奋撒欢小跑 (Zoomies!)
                        dist > 120f && randAction < 0.50 -> {
                            motionState = PetMotionState.RUNNING
                            currentSpeed = (5.8f + Math.random() * 1.6f).toFloat() // 跑速 5.8 ~ 7.4 px/帧
                        }
                        // 停下好奇嗅嗅地面
                        randAction < 0.20 -> {
                            motionState = PetMotionState.SNIFFING
                            stateTicks = (30..50).random() // 嗅嗅约 1~1.5 秒
                        }
                        // 其余概率悠闲慢慢步巡游
                        else -> {
                            motionState = PetMotionState.WALKING
                            currentSpeed = (1.8f + Math.random() * 0.6f).toFloat() // 慢步速 1.8 ~ 2.4 px/帧
                        }
                    }
                }
            }

            PetMotionState.SNIFFING -> {
                stateTicks--
                // 嗅闻期间极微小地往前探步
                val sniffStep = 0.35f * walkDirection
                currentExactX = (currentExactX + sniffStep).coerceIn(bounds.left, bounds.right)
                layoutParams.x = currentExactX.toInt()
                try {
                    windowManager.updateViewLayout(this, layoutParams)
                } catch (_: Exception) {}

                if (stateTicks <= 0) {
                    // 嗅完后，大概率继续走或兴奋开跑
                    if (Math.random() < 0.40) {
                        motionState = PetMotionState.RUNNING
                        currentSpeed = (5.8f + Math.random() * 1.5f).toFloat()
                    } else {
                        motionState = PetMotionState.WALKING
                        currentSpeed = (1.8f + Math.random() * 0.6f).toFloat()
                    }
                }
            }

            PetMotionState.SHAKING -> {
                stateTicks--
                if (stateTicks <= 0) {
                    motionState = PetMotionState.IDLE
                    idleTicks = 0
                    restDuration = 40
                }
            }

            PetMotionState.WALKING, PetMotionState.RUNNING -> {
                val dx = targetX - currentExactX
                val dy = targetY - currentExactY
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                // 慢走途中偶发小惊喜：发现新大陆突然加速狂奔，或突然停下嗅嗅
                if (motionState == PetMotionState.WALKING && dist > 150f && Math.random() < 0.015) {
                    // 突然兴奋撒欢跑起来！
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

                    // 跑完后大概率甩毛或嗅嗅，走完后大概率坐下发呆
                    if (motionState == PetMotionState.RUNNING) {
                        val p = Math.random()
                        when {
                            p < 0.45 -> {
                                motionState = PetMotionState.SHAKING
                                stateTicks = 22 // 甩毛抖抖 0.7秒
                            }
                            p < 0.75 -> {
                                motionState = PetMotionState.SNIFFING
                                stateTicks = 35 // 嗅嗅歇气
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
                    // 向目标平滑前进
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

        // 1. 奔跑快跳与走动微晃律动计算
        val isRunning = motionState == PetMotionState.RUNNING
        val isWalking = motionState == PetMotionState.WALKING
        val isSniffing = motionState == PetMotionState.SNIFFING
        val isShaking = motionState == PetMotionState.SHAKING
        val isIdle = motionState == PetMotionState.IDLE

        // 奔跑时大跳跃起伏 vs 慢步微晃
        val motionOffsetY = when {
            isRunning -> -abs(sin(tick * 0.52).toFloat()) * 8.5f // 活泼的大跨步跃起
            isWalking -> sin(tick * 0.24).toFloat() * 2.2f       // 悠闲温和微晃
            isSniffing -> 4.5f + sin(tick * 0.6).toFloat() * 1.5f // 低头嗅闻微耸动
            isIdle -> sin(tick * 0.12).toFloat() * 2.0f          // 待机呼吸
            else -> 0f
        }

        // 身体前倾倾角：快跑时身体前倾冲刺，嗅嗅时向下前探，慢走时左右可爱摇晃
        val bodyTiltAngle = when {
            isRunning -> 7.0f * walkDirection
            isWalking -> sin(tick * 0.24).toFloat() * 4.5f * walkDirection
            isSniffing -> 8.5f * walkDirection
            isShaking -> sin(tick * 1.3).toFloat() * 13.0f // 甩毛抖抖狂摇
            isIdle -> {
                // 发呆时偶尔好奇歪头打量主人 (11° 萌化歪头)
                if ((tick % 180) in 70..130) 11.0f else 0f
            }
            else -> 0f
        }

        // 奔跑时的动态弹力拉伸 (Squash & Stretch)
        val dynamicScaleX: Float
        val dynamicScaleY: Float
        if (isRunning) {
            val gallopPhase = sin(tick * 0.52).toFloat()
            dynamicScaleX = scaleXFactor * (1.0f + gallopPhase * 0.09f) // 跃起向前拉伸
            dynamicScaleY = scaleYFactor * (1.0f - gallopPhase * 0.07f)
        } else {
            dynamicScaleX = scaleXFactor
            dynamicScaleY = scaleYFactor
        }

        canvas.save()
        canvas.scale(dynamicScaleX, dynamicScaleY, centerX, centerY + 35f)

        // 奔跑时在脚底后方绘制手绘小烟尘圈 (Dust Puffs)
        if (isRunning) {
            drawRunningDustPuffs(canvas, centerX, centerY + 28f)
        }

        canvas.save()
        canvas.rotate(bodyTiltAngle, centerX, centerY + 25f)

        // 行走与奔跑时跟随朝向整体镜像翻转（小狗始终面朝行进方向）
        canvas.scale(walkDirection.toFloat(), 1f, centerX, centerY)

        // 绘制尾巴（快跑时螺旋桨飞摇，慢走时从容摆动，嗅嗅时高高竖起小尾巴）
        drawTail(canvas, centerX, centerY, motionOffsetY)

        // 绘制身体与小短腿（快跑时大踏步倒腾，慢走时小步交替）
        drawBody(canvas, centerX, centerY, motionOffsetY)

        // 绘制头和耳朵（快跑时耳朵向后随风迎风飘起，甩毛时左右啪嗒飞甩）
        drawHeadAndEars(canvas, centerX, centerY, motionOffsetY)

        // 绘制五官与小舌头（快跑时伸出粉嫩小舌头哈气，待机时自然眨眼，低头嗅嗅动鼻子）
        drawFaceFeatures(canvas, centerX, centerY, motionOffsetY)

        // 绘制前爪爪与胸口小爱心
        drawFrontPawsAndHeart(canvas, centerX, centerY, motionOffsetY)

        // 嗅嗅时在鼻前绘制气味小微痕
        if (isSniffing) {
            drawSniffScentMarks(canvas, centerX, centerY + motionOffsetY)
        }

        canvas.restore()
        canvas.restore()

        // 2. 充电或低电状态标识（贴身小标记）
        drawBatteryIndicators(canvas, centerX, centerY - 55f)

        // 3. 点击冒红心
        if (showHeart) {
            drawFloatingHeart(canvas, centerX, centerY - 60f + heartOffsetY)
        }
    }

    /**
     * 奔跑撒欢时脚底冒出的手绘小烟尘 (小气旋尘烟圈与可爱冲刺线)
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
        // 冲刺小速度线
        val dashX = trailX - 35f * walkDirection
        dustPaint.alpha = 140
        canvas.drawLine(dashX, groundY - 3f, dashX - 12f * walkDirection, groundY - 3f, dustPaint)
        canvas.drawLine(dashX + 5f * walkDirection, groundY + 4f, dashX - 8f * walkDirection, groundY + 4f, dustPaint)
    }

    /**
     * 嗅嗅探险时在鼻尖前方的气味微动记号
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
     * 充电光标或低电量标记
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

        // 尾巴摆角：快跑时如螺旋桨飞速欢摇(38°)，嗅嗅时高高翘起急促颤动
        val tailAngle = when (motionState) {
            PetMotionState.RUNNING -> sin(tick * 0.72).toFloat() * 36f
            PetMotionState.WALKING -> sin(tick * 0.24).toFloat() * 20f
            PetMotionState.SNIFFING -> -28f + sin(tick * 0.65).toFloat() * 14f // 高竖兴奋颤动
            PetMotionState.DRAGGED -> sin(tick * 0.45).toFloat() * 28f
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

        // 奔跑时大步飞奔 pedaling vs 慢步小碎步
        val legStride = when (motionState) {
            PetMotionState.RUNNING -> sin(tick * 0.52).toFloat() * 7.5f // 奔跑前后大跨步
            PetMotionState.WALKING -> sin(tick * 0.24).toFloat() * 3.5f // 慢走小碎步
            else -> 0f
        }

        // 小短腿
        canvas.drawCircle(cx - 18f - legStride * 0.4f, bodyY + 26f + legStride, 4.8f, furPaint)
        canvas.drawCircle(cx - 18f - legStride * 0.4f, bodyY + 26f + legStride, 4.8f, strokePaint)

        canvas.drawCircle(cx + 18f + legStride * 0.4f, bodyY + 26f - legStride, 4.8f, furPaint)
        canvas.drawCircle(cx + 18f + legStride * 0.4f, bodyY + 26f - legStride, 4.8f, strokePaint)
    }

    private fun drawHeadAndEars(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val headY = cy - 10f + breath

        // 耳朵迎风与飞动物理计算：奔跑时耳朵向后迎风吹起 (Wind Flap)
        val leftEarAngle = when (motionState) {
            PetMotionState.RUNNING -> -20f + sin(tick * 0.52).toFloat() * 12f // 迎风向后掀起
            PetMotionState.SHAKING -> sin(tick * 1.3).toFloat() * 26f         // 甩毛狂拍
            PetMotionState.SNIFFING -> 8f                                     // 探寻前耸
            PetMotionState.WALKING -> sin(tick * 0.24).toFloat() * 4.0f
            else -> 0f
        }

        val rightEarAngle = when (motionState) {
            PetMotionState.RUNNING -> -18f - sin(tick * 0.52).toFloat() * 12f // 迎风向后掀起
            PetMotionState.SHAKING -> -sin(tick * 1.3).toFloat() * 26f        // 甩毛狂拍
            PetMotionState.SNIFFING -> 8f
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

        // 眨眼判定（待机或慢步走时，每隔约 4 秒自然眨眨豆豆眼）
        val isBlinking = (motionState == PetMotionState.IDLE || motionState == PetMotionState.WALKING) &&
                (tick % 130) < 5

        if (motionState == PetMotionState.DRAGGED) {
            // 被拎起闭眼挣扎 (> <)
            canvas.drawLine(cx - 21f, faceY - 7f, cx - 14f, faceY - 2f, strokePaint)
            canvas.drawLine(cx - 14f, faceY - 2f, cx - 21f, faceY + 3f, strokePaint)

            canvas.drawLine(cx + 21f, faceY - 7f, cx + 14f, faceY - 2f, strokePaint)
            canvas.drawLine(cx + 14f, faceY - 2f, cx + 21f, faceY + 3f, strokePaint)
        } else {
            if (showHeart || isRunning) {
                // 开心微笑眯眼 / 奔跑兴奋弯弯眼 (^ ^)
                canvas.drawArc(cx - 22f, faceY - 8f, cx - 12f, faceY + 2f, 200f, 140f, false, strokePaint)
                canvas.drawArc(cx + 12f, faceY - 8f, cx + 22f, faceY + 2f, 200f, 140f, false, strokePaint)
            } else if (isBlinking) {
                // 自然眨眼闭合 (- -)
                canvas.drawLine(cx - 21f, faceY - 3f, cx - 13f, faceY - 3f, strokePaint)
                canvas.drawLine(cx + 13f, faceY - 3f, cx + 21f, faceY - 3f, strokePaint)
            } else if (batteryLevel < 20 && !isCharging) {
                // 低电量疲倦下垂眼
                canvas.drawLine(cx - 21f, faceY - 2f, cx - 14f, faceY - 2f, strokePaint)
                canvas.drawLine(cx + 14f, faceY - 2f, cx + 21f, faceY - 2f, strokePaint)
            } else {
                // 乌溜溜纯真豆豆眼
                canvas.drawCircle(cx - 18f, faceY - 3f, 4.0f, eyePaint)
                canvas.drawCircle(cx + 18f, faceY - 3f, 4.0f, eyePaint)
            }
        }

        // 软萌粉腮红
        val blushAlpha = if (showHeart || isRunning) 235 else 160
        blushPaint.alpha = blushAlpha
        canvas.drawCircle(cx - 24f, faceY + 5f, 6.5f, blushPaint)
        canvas.drawCircle(cx + 24f, faceY + 5f, 6.5f, blushPaint)

        // 纽扣小黑鼻（嗅嗅时鼻子耸动）
        val noseOffsetY = if (motionState == PetMotionState.SNIFFING) sin(tick * 0.7).toFloat() * 1.8f else 0f
        canvas.drawOval(cx - 4.0f, faceY + 2f + noseOffsetY, cx + 4.0f, faceY + 6.8f + noseOffsetY, eyePaint)

        // 嘴巴与吐舌头交互
        if (isRunning) {
            // 奔跑时张开小嘴，吐出粉嫩小舌头哈气 (Tongue Panting 👅)
            val tongueBounce = sin(tick * 0.52).toFloat() * 2.2f
            val tongueX = cx + 4.5f
            val tongueY = faceY + 8f + tongueBounce

            tonguePath.reset()
            tonguePath.moveTo(tongueX - 5.5f, tongueY)
            tonguePath.cubicTo(tongueX - 6.5f, tongueY + 11f, tongueX + 6.5f, tongueY + 11f, tongueX + 5.5f, tongueY)
            tonguePath.close()

            canvas.drawPath(tonguePath, tonguePaint)
            canvas.drawPath(tonguePath, fineStrokePaint)
            // 舌中细缝
            canvas.drawLine(tongueX, tongueY + 2f, tongueX, tongueY + 8f, fineStrokePaint)

            // 张开的快乐大嘴
            canvas.drawArc(cx - 7f, faceY + 4f, cx + 7f, faceY + 10f, 10f, 160f, false, strokePaint)
        } else {
            // 平时温顺安静微笑嘴
            canvas.drawArc(cx - 7f, faceY + 4.5f, cx, faceY + 11.5f, 10f, 160f, false, strokePaint)
            canvas.drawArc(cx, faceY + 4.5f, cx + 7f, faceY + 11.5f, 10f, 160f, false, strokePaint)
        }
    }

    private fun drawFrontPawsAndHeart(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val chestY = cy + 11f + breath
        heartPath.reset()
        val heartScale = 0.48f
        heartPath.moveTo(cx, chestY + 10f)
        heartPath.cubicTo(cx - 18f * heartScale, chestY - 18f * heartScale, cx - 36f * heartScale, chestY + 4f * heartScale, cx, chestY + 32f * heartScale)
        heartPath.cubicTo(cx + 36f * heartScale, chestY + 4f * heartScale, cx + 18f * heartScale, chestY - 18f * heartScale, cx, chestY + 10f)
        heartPath.close()

        canvas.drawPath(heartPath, heartPaint)
        canvas.drawPath(heartPath, strokePaint)

        val isRunning = motionState == PetMotionState.RUNNING
        val frontPawBounce = if (isRunning) sin(tick * 0.52).toFloat() * 2.8f else 0f

        canvas.drawCircle(cx - 11f, chestY + 10f + frontPawBounce, 5.5f, furPaint)
        canvas.drawCircle(cx - 11f, chestY + 10f + frontPawBounce, 5.5f, strokePaint)
        canvas.drawCircle(cx + 11f, chestY + 10f - frontPawBounce, 5.5f, furPaint)
        canvas.drawCircle(cx + 11f, chestY + 10f - frontPawBounce, 5.5f, strokePaint)
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
                // 触控瞬间锁定当前目标，杜绝残留位移
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
                isDragging = false // 立即重置拖拽标志

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
                isDragging = false // 立即重置拖拽标志
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

        // 单击：摸摸小狗，开心眯眼、红心冒出、Q弹回弹
        PetSyncEngine.poke()
        showHeart = true
        heartOffsetY = 0f
        triggerSquashAndStretch()
        motionState = PetMotionState.IDLE
        idleTicks = 0
        restDuration = 45 // 抚摸后温顺停下歇息 1.5 秒
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
        isDragging = false // 双重保险
        currentExactX = layoutParams.x.toFloat().coerceIn(bounds.left, bounds.right)
        currentExactY = layoutParams.y.toFloat().coerceIn(bounds.top, bounds.bottom)
        layoutParams.x = currentExactX.toInt()
        layoutParams.y = currentExactY.toInt()
        targetX = currentExactX
        targetY = currentExactY
        motionState = PetMotionState.IDLE
        idleTicks = 0
        restDuration = 40 // 释放后先安稳停下歇息 1.2 秒，平稳过渡
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
