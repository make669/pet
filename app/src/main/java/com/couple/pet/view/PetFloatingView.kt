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
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.toColorInt
import com.couple.pet.MainActivity
import com.couple.pet.model.PetMotionState
import com.couple.pet.network.PetSyncEngine
import kotlin.math.*

/**
 * 桌面悬浮宠物：极简治愈系“线条小狗”
 * 包含：
 * 1. 2D 极简手绘动作与呼吸/摆尾/被抓受困/触底Q弹物理
 * 2. 悬浮窗顶部展开的即时径向气泡菜单（投喂/抚摸/洗澡/主页）
 * 3. 屏幕底部自主漫步踱步状态机
 * 4. 系统电量与充电状态实时感知动效
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
        strokeWidth = 5.5f
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

    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF4B68".toColorInt()
        style = Paint.Style.FILL
    }

    private val bubbleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val bubbleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF8DA1".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }

    private val textToastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF4B68".toColorInt()
        textSize = 26f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val feedbackPillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pillRect = RectF()

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
    private var feedbackText: String? = null
    private var feedbackOffsetY = 0f
    private var dropAnimator: ValueAnimator? = null

    // 3. 自主漫步状态机参数
    private var idleTicks = 0
    private var walkDirection = 1 // 1: 向右, -1: 向左
    private var walkTicksRemaining = 0

    // 4. 系统电量与充电状态
    private var batteryLevel: Int = 100
    private var isCharging: Boolean = false

    // 5. 桌面即时气泡快捷菜单
    private var isMenuExpanded = false
    private var menuAnimProgress = 0f // 0f ~ 1f
    private var menuAnimator: ValueAnimator? = null

    data class ActionBubble(
        val emoji: String,
        val angleDeg: Float, // 扇形展开角度
        val action: () -> Unit
    )

    private val bubbles = listOf(
        ActionBubble("🐟", 145f) {
            PetSyncEngine.feed(15)
            showFloatingFeedback("饱食 +15 🐟")
        },
        ActionBubble("❤️", 115f) {
            PetSyncEngine.poke()
            showFloatingFeedback("心情 +10 ❤️")
        },
        ActionBubble("🛁", 65f) {
            PetSyncEngine.bath()
            showFloatingFeedback("干爽 +30 🛁")
        },
        ActionBubble("🏠", 35f) {
            launchMainActivity()
        }
    )

    // 预分配 Path
    private val headPath = Path()
    private val leftEarPath = Path()
    private val rightEarPath = Path()
    private val bodyPath = Path()
    private val tailPath = Path()
    private val heartPath = Path()

    private val handler = Handler(Looper.getMainLooper())
    private val animRunnable = object : Runnable {
        override fun run() {
            tick++
            if (showHeart) {
                heartOffsetY = (heartOffsetY - 1.8f).coerceAtLeast(-45f)
            }
            if (feedbackText != null) {
                feedbackOffsetY = (feedbackOffsetY - 1.4f).coerceAtLeast(-42f)
            }

            // 自主漫步逻辑驱动
            updateAutonomousWander()

            invalidate()
            handler.postDelayed(this, 30) // ~33fps
        }
    }

    init {
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
        dropAnimator?.cancel()
        menuAnimator?.cancel()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /**
     * 投喂与互动的即时悬浮反馈动效
     */
    private fun showFloatingFeedback(text: String) {
        feedbackText = text
        feedbackOffsetY = 0f
        showHeart = true
        heartOffsetY = 0f
        triggerSquashAndStretch()
        invalidate()
        handler.removeCallbacks(hideFeedbackRunnable)
        handler.postDelayed(hideFeedbackRunnable, 1600)
    }

    private val hideFeedbackRunnable = Runnable {
        feedbackText = null
        showHeart = false
        invalidate()
    }

    /**
     * 桌面自主漫步算法
     */
    private fun updateAutonomousWander() {
        if (isDragging || isMenuExpanded) return

        val screenWidth = context.resources.displayMetrics.widthPixels

        // 避免在顶部遮挡通知栏区域漫步
        if (layoutParams.y < 200) return

        if (motionState == PetMotionState.IDLE) {
            idleTicks++
            // 闲置约 3 秒后（90 ticks），自动开启一段萌态踱步
            if (idleTicks > 90) {
                idleTicks = 0
                walkDirection = if (Math.random() > 0.5) 1 else -1
                walkTicksRemaining = (70..130).random() // 漫步 2~4 秒
                motionState = PetMotionState.WALKING
            }
        } else if (motionState == PetMotionState.WALKING) {
            walkTicksRemaining--
            val stepSize = 3
            val nextX = layoutParams.x + (walkDirection * stepSize)

            // 边缘碰壁自动折返掉头
            if (nextX <= 10) {
                walkDirection = 1
                layoutParams.x = 10
            } else if (nextX >= screenWidth - width - 10) {
                walkDirection = -1
                layoutParams.x = screenWidth - width - 10
            } else {
                layoutParams.x = nextX
            }

            try {
                windowManager.updateViewLayout(this, layoutParams)
            } catch (_: Exception) {}

            if (walkTicksRemaining <= 0) {
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
        val centerY = h / 2f + 25f // 稍向下沉，给上方气泡留出位置

        // 1. 绘制展开的桌面即时气泡菜单
        if (menuAnimProgress > 0.05f) {
            drawActionBubbles(canvas, centerX, centerY)
        }

        // 2. 落地挤压缩放
        canvas.save()
        canvas.scale(scaleXFactor, scaleYFactor, centerX, centerY + 45f)

        // 漫步或待机微动
        val breathOffset = if (motionState == PetMotionState.IDLE) {
            sin(tick * 0.12).toFloat() * 2.2f
        } else if (motionState == PetMotionState.WALKING) {
            // 走路时的晃脑与微颠簸
            sin(tick * 0.35).toFloat() * 3.5f
        } else {
            0f
        }

        // 漫步摇摆角度
        val waddleAngle = if (motionState == PetMotionState.WALKING) {
            sin(tick * 0.35).toFloat() * 4.5f * walkDirection
        } else {
            0f
        }

        canvas.save()
        canvas.rotate(waddleAngle, centerX, centerY + 30f)

        // 绘制尾巴
        drawTail(canvas, centerX, centerY, breathOffset)

        // 绘制身体与小短腿
        drawBody(canvas, centerX, centerY, breathOffset)

        // 绘制头和耳朵
        drawHeadAndEars(canvas, centerX, centerY, breathOffset)

        // 绘制五官
        drawFaceFeatures(canvas, centerX, centerY, breathOffset)

        // 绘制前爪爪与爱心
        drawFrontPawsAndHeart(canvas, centerX, centerY, breathOffset)

        canvas.restore()
        canvas.restore()

        // 3. 充电或低电状态标识
        drawBatteryIndicators(canvas, centerX, centerY - 65f)

        // 4. 点击冒红心
        if (showHeart && feedbackText == null) {
            drawFloatingHeart(canvas, centerX, centerY - 70f + heartOffsetY)
        }

        // 5. 即时文字气泡反馈（饱食/心情/干爽加分胶囊提示）
        if (feedbackText != null) {
            val textY = centerY - 100f + feedbackOffsetY
            val textWidth = textToastPaint.measureText(feedbackText!!)
            val halfW = textWidth / 2f + 14f
            pillRect.set(centerX - halfW, textY - 24f, centerX + halfW, textY + 8f)

            // 胶囊白底
            feedbackPillPaint.color = "#F5FFFFFF".toColorInt()
            feedbackPillPaint.style = Paint.Style.FILL
            canvas.drawRoundRect(pillRect, 16f, 16f, feedbackPillPaint)

            // 胶囊粉红细边框
            feedbackPillPaint.color = "#FFAFC0".toColorInt()
            feedbackPillPaint.style = Paint.Style.STROKE
            feedbackPillPaint.strokeWidth = 2f
            canvas.drawRoundRect(pillRect, 16f, 16f, feedbackPillPaint)

            canvas.drawText(feedbackText!!, centerX, textY, textToastPaint)
        }
    }

    /**
     * 绘制 4 个弧形展开的桌面即时操作气泡
     */
    private fun drawActionBubbles(canvas: Canvas, cx: Float, cy: Float) {
        val bubbleRadius = 22f
        val distance = 92f * menuAnimProgress

        for (bubble in bubbles) {
            val rad = Math.toRadians(bubble.angleDeg.toDouble())
            val bx = cx + (distance * cos(rad)).toFloat()
            val by = cy - (distance * sin(rad)).toFloat()

            canvas.save()
            canvas.scale(menuAnimProgress, menuAnimProgress, bx, by)

            // 气泡白色背景与粉色描边
            canvas.drawCircle(bx, by, bubbleRadius, bubbleBgPaint)
            canvas.drawCircle(bx, by, bubbleRadius, bubbleStrokePaint)

            // Emoji 图标绘制
            canvas.drawText(bubble.emoji, bx, by + 11f, emojiPaint)
            canvas.restore()
        }
    }

    /**
     * 充电光标或低电量标记
     */
    private fun drawBatteryIndicators(canvas: Canvas, cx: Float, topY: Float) {
        if (isCharging) {
            // 充电闪烁金黄小闪电 ⚡
            val sparkY = topY + sin(tick * 0.2).toFloat() * 3f
            canvas.drawText("⚡", cx + 28f, sparkY, emojiPaint)
        } else if (batteryLevel < 20) {
            // 低电量困倦 zZZ
            canvas.drawText("💤", cx + 28f, topY, emojiPaint)
        }
    }

    private fun drawTail(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val tailBaseX = cx + 32f
        val tailBaseY = cy + 28f + breath

        val tailAngle = when (motionState) {
            PetMotionState.DRAGGED -> sin(tick * 0.45).toFloat() * 28f
            PetMotionState.WALKING -> sin(tick * 0.35).toFloat() * 22f
            PetMotionState.IDLE -> sin(tick * 0.16).toFloat() * 16f
            else -> 0f
        }

        canvas.save()
        canvas.rotate(tailAngle, tailBaseX, tailBaseY)

        tailPath.reset()
        tailPath.moveTo(tailBaseX, tailBaseY)
        tailPath.quadTo(tailBaseX + 18f, tailBaseY - 14f, tailBaseX + 12f, tailBaseY - 26f)
        tailPath.quadTo(tailBaseX + 6f, tailBaseY - 28f, tailBaseX + 4f, tailBaseY - 20f)
        tailPath.quadTo(tailBaseX + 8f, tailBaseY - 10f, tailBaseX, tailBaseY + 8f)
        tailPath.close()

        canvas.drawPath(tailPath, furPaint)
        canvas.drawPath(tailPath, strokePaint)
        canvas.restore()
    }

    private fun drawBody(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val bodyY = cy + 18f + breath
        bodyPath.reset()
        bodyPath.moveTo(cx - 30f, bodyY - 10f)
        bodyPath.quadTo(cx - 42f, bodyY + 22f, cx - 28f, bodyY + 32f)
        bodyPath.quadTo(cx - 20f, bodyY + 34f, cx - 14f, bodyY + 32f)
        bodyPath.lineTo(cx + 14f, bodyY + 32f)
        bodyPath.quadTo(cx + 20f, bodyY + 34f, cx + 28f, bodyY + 32f)
        bodyPath.quadTo(cx + 42f, bodyY + 22f, cx + 30f, bodyY - 10f)
        bodyPath.close()

        canvas.drawPath(bodyPath, furPaint)
        canvas.drawPath(bodyPath, strokePaint)

        // 走动时脚掌微抬交互
        val walkPawOffset = if (motionState == PetMotionState.WALKING) {
            sin(tick * 0.35).toFloat() * 3f
        } else {
            0f
        }

        val footY = bodyY + 31f
        canvas.drawLine(cx - 24f, footY - 4f - walkPawOffset, cx - 24f, footY - walkPawOffset, strokePaint)
        canvas.drawLine(cx + 24f, footY - 4f + walkPawOffset, cx + 24f, footY + walkPawOffset, strokePaint)
    }

    private fun drawHeadAndEars(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val headY = cy - 14f + breath
        val earWiggle = if (motionState == PetMotionState.DRAGGED) sin(tick * 0.35).toFloat() * 6f else 0f

        leftEarPath.reset()
        leftEarPath.moveTo(cx - 32f, headY - 18f)
        leftEarPath.cubicTo(cx - 56f, headY - 26f + earWiggle, cx - 58f, headY + 12f + earWiggle, cx - 38f, headY + 6f)
        leftEarPath.close()
        canvas.drawPath(leftEarPath, furPaint)
        canvas.drawPath(leftEarPath, strokePaint)

        rightEarPath.reset()
        rightEarPath.moveTo(cx + 32f, headY - 18f)
        rightEarPath.cubicTo(cx + 56f, headY - 26f - earWiggle, cx + 58f, headY + 12f - earWiggle, cx + 38f, headY + 6f)
        rightEarPath.close()
        canvas.drawPath(rightEarPath, furPaint)
        canvas.drawPath(rightEarPath, strokePaint)

        headPath.reset()
        headPath.moveTo(cx - 36f, headY + 14f)
        headPath.cubicTo(cx - 48f, headY - 16f, cx - 26f, headY - 34f, cx - 6f, headY - 34f)
        headPath.lineTo(cx, headY - 40f)
        headPath.lineTo(cx + 4f, headY - 34f)
        headPath.cubicTo(cx + 26f, headY - 34f, cx + 48f, headY - 16f, cx + 36f, headY + 14f)
        headPath.quadTo(cx, headY + 36f, cx - 36f, headY + 14f)
        headPath.close()

        canvas.drawPath(headPath, furPaint)
        canvas.drawPath(headPath, strokePaint)
    }

    private fun drawFaceFeatures(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val faceY = cy - 10f + breath

        if (motionState == PetMotionState.DRAGGED) {
            val eyeR = 6f
            canvas.drawLine(cx - 22f - eyeR, faceY - 4f - eyeR, cx - 22f, faceY - 4f, strokePaint)
            canvas.drawLine(cx - 22f, faceY - 4f, cx - 22f - eyeR, faceY - 4f + eyeR, strokePaint)

            canvas.drawLine(cx + 22f + eyeR, faceY - 4f - eyeR, cx + 22f, faceY - 4f, strokePaint)
            canvas.drawLine(cx + 22f, faceY - 4f, cx + 22f + eyeR, faceY - 4f + eyeR, strokePaint)
        } else {
            val isBlinking = (tick % 75 in 0..3)
            if (isBlinking || showHeart || isMenuExpanded) {
                // 开心眯眯眼
                canvas.drawArc(cx - 26f, faceY - 10f, cx - 14f, faceY + 2f, 200f, 140f, false, strokePaint)
                canvas.drawArc(cx + 14f, faceY - 10f, cx + 26f, faceY + 2f, 200f, 140f, false, strokePaint)
            } else if (batteryLevel < 20 && !isCharging) {
                // 低电量疲倦下垂眼
                canvas.drawLine(cx - 24f, faceY - 3f, cx - 16f, faceY - 3f, strokePaint)
                canvas.drawLine(cx + 16f, faceY - 3f, cx + 24f, faceY - 3f, strokePaint)
            } else {
                // 乌溜溜纯真豆豆眼
                canvas.drawCircle(cx - 20f, faceY - 4f, 4.6f, eyePaint)
                canvas.drawCircle(cx + 20f, faceY - 4f, 4.6f, eyePaint)
            }
        }

        // 软萌粉腮红
        val blushAlpha = if (showHeart || isMenuExpanded) 230 else 170
        blushPaint.alpha = blushAlpha
        canvas.drawCircle(cx - 27f, faceY + 6f, 7.5f, blushPaint)
        canvas.drawCircle(cx + 27f, faceY + 6f, 7.5f, blushPaint)

        // 纽扣小黑鼻与小狗微笑嘴
        canvas.drawOval(cx - 4.5f, faceY + 2f, cx + 4.5f, faceY + 7.5f, eyePaint)
        canvas.drawArc(cx - 8f, faceY + 5f, cx, faceY + 13f, 10f, 160f, false, strokePaint)
        canvas.drawArc(cx, faceY + 5f, cx + 8f, faceY + 13f, 10f, 160f, false, strokePaint)
    }

    private fun drawFrontPawsAndHeart(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val chestY = cy + 13f + breath
        heartPath.reset()
        val heartScale = 0.55f
        heartPath.moveTo(cx, chestY + 12f)
        heartPath.cubicTo(cx - 18f * heartScale, chestY - 18f * heartScale, cx - 36f * heartScale, chestY + 4f * heartScale, cx, chestY + 32f * heartScale)
        heartPath.cubicTo(cx + 36f * heartScale, chestY + 4f * heartScale, cx + 18f * heartScale, chestY - 18f * heartScale, cx, chestY + 12f)
        heartPath.close()

        canvas.drawPath(heartPath, heartPaint)
        canvas.drawPath(heartPath, strokePaint)

        canvas.drawCircle(cx - 13f, chestY + 12f, 6.5f, furPaint)
        canvas.drawCircle(cx - 13f, chestY + 12f, 6.5f, strokePaint)
        canvas.drawCircle(cx + 13f, chestY + 12f, 6.5f, furPaint)
        canvas.drawCircle(cx + 13f, chestY + 12f, 6.5f, strokePaint)
    }

    private fun drawFloatingHeart(canvas: Canvas, cx: Float, cy: Float) {
        val floatHeart = Path()
        val s = 0.7f
        floatHeart.moveTo(cx, cy + 8f * s)
        floatHeart.cubicTo(cx - 20f * s, cy - 20f * s, cx - 40f * s, cy + 5f * s, cx, cy + 36f * s)
        floatHeart.cubicTo(cx + 40f * s, cy + 5f * s, cx + 20f * s, cy - 20f * s, cx, cy + 8f * s)
        floatHeart.close()

        canvas.drawPath(floatHeart, heartPaint)
        canvas.drawPath(floatHeart, strokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 气泡菜单点击判定优先
                if (isMenuExpanded) {
                    val clickedBubble = checkBubbleClicked(touchX, touchY)
                    if (clickedBubble != null) {
                        clickedBubble.action()
                        toggleMenu(false)
                        return true
                    }
                }

                dropAnimator?.cancel()
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                motionState = PetMotionState.DRAGGED
                idleTicks = 0
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
                    // 拖拽时收起气泡
                    if (isMenuExpanded) {
                        toggleMenu(false)
                    }
                }

                val screenWidth = context.resources.displayMetrics.widthPixels
                val screenHeight = context.resources.displayMetrics.heightPixels
                val clampedX = (initialX + dx).coerceIn(0, screenWidth - width)
                val clampedY = (initialY + dy).coerceIn(0, screenHeight - height)

                layoutParams.x = clampedX
                layoutParams.y = clampedY
                try {
                    windowManager.updateViewLayout(this, layoutParams)
                } catch (_: Exception) {}
                return true
            }

            MotionEvent.ACTION_UP -> {
                scaleXFactor = 1.0f
                scaleYFactor = 1.0f
                if (!isDragging) {
                    performClick()
                    onPetClicked()
                } else {
                    onPetReleased()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 检查点击是否落在某个气泡内
     */
    private fun checkBubbleClicked(tx: Float, ty: Float): ActionBubble? {
        val cx = width / 2f
        val cy = height / 2f + 25f
        val distance = 92f * max(menuAnimProgress, 0.75f)
        val bubbleRadius = 24f

        for (bubble in bubbles) {
            val rad = Math.toRadians(bubble.angleDeg.toDouble())
            val bx = cx + (distance * cos(rad)).toFloat()
            val by = cy - (distance * sin(rad)).toFloat()
            val dist = hypot((tx - bx).toDouble(), (ty - by).toDouble()).toFloat()
            if (dist <= bubbleRadius + 22f) {
                return bubble
            }
        }
        return null
    }

    private fun onPetClicked() {
        // 单击：展开或收起即时桌面气泡菜单
        toggleMenu(!isMenuExpanded)
        showHeart = true
        heartOffsetY = 0f
        invalidate()
        handler.postDelayed({
            showHeart = false
            invalidate()
        }, 1200)
    }

    private fun toggleMenu(expand: Boolean) {
        if (isMenuExpanded == expand) return
        isMenuExpanded = expand
        if (expand) {
            motionState = PetMotionState.IDLE
            idleTicks = 0
        }

        menuAnimator?.cancel()
        val target = if (expand) 1f else 0f
        menuAnimator = ValueAnimator.ofFloat(menuAnimProgress, target).apply {
            duration = 300
            interpolator = if (expand) OvershootInterpolator(1.8f) else BounceInterpolator()
            addUpdateListener {
                menuAnimProgress = it.animatedValue as Float
                invalidate()
            }
        }
        menuAnimator?.start()
    }

    private fun launchMainActivity() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }

    private fun onPetReleased() {
        val screenHeight = context.resources.displayMetrics.heightPixels
        val targetY = (screenHeight * 0.72f).toInt()

        if (layoutParams.y < targetY) {
            motionState = PetMotionState.FALLING
            dropAnimator?.cancel()
            dropAnimator = ValueAnimator.ofInt(layoutParams.y, targetY).apply {
                duration = 620
                interpolator = BounceInterpolator()
                addUpdateListener { anim ->
                    layoutParams.y = anim.animatedValue as Int
                    try {
                        windowManager.updateViewLayout(this@PetFloatingView, layoutParams)
                    } catch (_: Exception) {}
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        motionState = PetMotionState.IDLE
                        idleTicks = 0
                        triggerSquashAndStretch()
                        invalidate()
                    }
                })
                start()
            }
        } else {
            motionState = PetMotionState.IDLE
            idleTicks = 0
            triggerSquashAndStretch()
            invalidate()
        }
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
