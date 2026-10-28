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
 * 针对手机小屏幕进行了深度体验优化：
 * 1. 彻底去除遮挡屏幕的头顶气泡操作栏，占位紧凑纯净（200x200），不误触其他应用
 * 2. 真实 2D 全屏幕自主到处游荡与智能探索漫步（X / Y 轴自由漫游）
 * 3. 待机呼吸、小尾巴摇摆、走路晃脑、碰壁折返、转向自然面朝行进方向
 * 4. 触屏微交互：单击开心微笑+冒小心心+Q弹萌动；双击一键开启伴侣小屋
 * 5. 系统电量感知：低电困倦 zZZ，充电闪烁金色小闪电 ⚡
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

    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF4B68".toColorInt()
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
    private var dropAnimator: ValueAnimator? = null

    // 3. 全屏幕自主漫步与智能探索引擎
    private var currentExactX = layoutParams.x.toFloat()
    private var currentExactY = layoutParams.y.toFloat()
    private var targetX = layoutParams.x.toFloat()
    private var targetY = layoutParams.y.toFloat()
    private var walkDirection = 1 // 1: 向右, -1: 向左
    private var idleTicks = 0
    private var restDuration = 60 // 停留歇息时长（约 1.5~2.5 秒）
    private var walkSpeed = 3.0f // 像素/帧

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

    private val handler = Handler(Looper.getMainLooper())
    private val animRunnable = object : Runnable {
        override fun run() {
            tick++
            if (showHeart) {
                heartOffsetY = (heartOffsetY - 2.0f).coerceAtLeast(-45f)
            }

            // 全屏幕自由漫步算法驱动
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
        dropAnimator?.cancel()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /**
     * 全屏幕 2D 自主游荡探索算法 (X & Y 自由游走)
     */
    private fun updateFreeRoam() {
        if (isDragging) return

        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        val minX = 20f
        val maxX = (screenW - width - 20).toFloat().coerceAtLeast(minX)
        val minY = 180f // 避免挡住状态栏
        val maxY = (screenH - height - 160).toFloat().coerceAtLeast(minY)

        if (motionState == PetMotionState.IDLE) {
            idleTicks++
            // 停下歇息完成后，挑选下一个全屏幕游荡目标点
            if (idleTicks > restDuration) {
                idleTicks = 0
                restDuration = (45..90).random() // 歇息 1.5 ~ 3 秒

                // 挑选屏幕上的游荡新坐标 (兼顾底部溜达与全屏漫步)
                val rand = Math.random()
                val nextTargetY = when {
                    rand < 0.45 -> (screenH * 0.65f + Math.random() * (maxY - screenH * 0.65f)).toFloat()
                    rand < 0.80 -> (screenH * 0.32f + Math.random() * (screenH * 0.33f)).toFloat()
                    else -> (minY + Math.random() * (screenH * 0.25f)).toFloat()
                }.coerceIn(minY, maxY)

                val nextTargetX = (minX + Math.random() * (maxX - minX)).toFloat().coerceIn(minX, maxX)

                targetX = nextTargetX
                targetY = nextTargetY
                walkDirection = if (targetX >= currentExactX) 1 else -1
                walkSpeed = (2.4f + Math.random() * 1.4f).toFloat() // 略微变频步态
                motionState = PetMotionState.WALKING
            }
        } else if (motionState == PetMotionState.WALKING) {
            val dx = targetX - currentExactX
            val dy = targetY - currentExactY
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            if (dist <= walkSpeed + 2f) {
                // 顺利到达目标点，坐下休息嗅闻
                currentExactX = targetX
                currentExactY = targetY
                layoutParams.x = targetX.toInt()
                layoutParams.y = targetY.toInt()
                try {
                    windowManager.updateViewLayout(this, layoutParams)
                } catch (_: Exception) {}
                motionState = PetMotionState.IDLE
                idleTicks = 0
            } else {
                // 沿矢量向目标步进
                val stepX = (dx / dist) * walkSpeed
                val stepY = (dy / dist) * walkSpeed

                currentExactX = (currentExactX + stepX).coerceIn(minX, maxX)
                currentExactY = (currentExactY + stepY).coerceIn(minY, maxY)

                walkDirection = if (dx >= 0) 1 else -1

                layoutParams.x = currentExactX.toInt()
                layoutParams.y = currentExactY.toInt()
                try {
                    windowManager.updateViewLayout(this, layoutParams)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val centerX = w / 2f
        val centerY = h / 2f + 5f

        // 1. 落地挤压缩放与朝向翻转
        canvas.save()
        canvas.scale(scaleXFactor, scaleYFactor, centerX, centerY + 35f)

        // 漫步或待机微动
        val breathOffset = if (motionState == PetMotionState.IDLE) {
            sin(tick * 0.12).toFloat() * 2.0f
        } else if (motionState == PetMotionState.WALKING) {
            // 走路时的晃脑与微颠簸
            sin(tick * 0.38).toFloat() * 3.2f
        } else {
            0f
        }

        // 漫步摇摆角度
        val waddleAngle = if (motionState == PetMotionState.WALKING) {
            sin(tick * 0.38).toFloat() * 5.0f * walkDirection
        } else {
            0f
        }

        canvas.save()
        canvas.rotate(waddleAngle, centerX, centerY + 25f)

        // 行走时跟随朝向整体镜像翻转（小狗面朝前进方向）
        canvas.scale(walkDirection.toFloat(), 1f, centerX, centerY)

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

        // 2. 充电或低电状态标识（贴身小标记）
        drawBatteryIndicators(canvas, centerX, centerY - 55f)

        // 3. 点击冒红心
        if (showHeart) {
            drawFloatingHeart(canvas, centerX, centerY - 60f + heartOffsetY)
        }
    }

    /**
     * 充电光标或低电量标记
     */
    private fun drawBatteryIndicators(canvas: Canvas, cx: Float, topY: Float) {
        if (isCharging) {
            // 充电闪烁金黄小闪电 ⚡
            val sparkY = topY + sin(tick * 0.2).toFloat() * 3f
            canvas.drawText("⚡", cx + 26f, sparkY, emojiPaint)
        } else if (batteryLevel < 20) {
            // 低电量困倦 zZZ
            canvas.drawText("💤", cx + 26f, topY, emojiPaint)
        }
    }

    private fun drawTail(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val tailBaseX = cx + 28f
        val tailBaseY = cy + 24f + breath

        val tailAngle = when (motionState) {
            PetMotionState.DRAGGED -> sin(tick * 0.45).toFloat() * 28f
            PetMotionState.WALKING -> sin(tick * 0.38).toFloat() * 22f
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

        // 走动时脚掌微抬交互
        val walkLegOffset = if (motionState == PetMotionState.WALKING) {
            sin(tick * 0.38).toFloat() * 3.5f
        } else {
            0f
        }

        // 小短腿
        canvas.drawCircle(cx - 18f, bodyY + 26f + walkLegOffset, 4.8f, furPaint)
        canvas.drawCircle(cx - 18f, bodyY + 26f + walkLegOffset, 4.8f, strokePaint)

        canvas.drawCircle(cx + 18f, bodyY + 26f - walkLegOffset, 4.8f, furPaint)
        canvas.drawCircle(cx + 18f, bodyY + 26f - walkLegOffset, 4.8f, strokePaint)
    }

    private fun drawHeadAndEars(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val headY = cy - 10f + breath

        // 左耳
        leftEarPath.reset()
        leftEarPath.moveTo(cx - 24f, headY - 14f)
        leftEarPath.cubicTo(cx - 45f, headY - 14f, cx - 44f, headY + 16f, cx - 25f, headY + 12f)
        leftEarPath.close()
        canvas.drawPath(leftEarPath, furPaint)
        canvas.drawPath(leftEarPath, strokePaint)

        // 右耳
        rightEarPath.reset()
        rightEarPath.moveTo(cx + 24f, headY - 14f)
        rightEarPath.cubicTo(cx + 45f, headY - 14f, cx + 44f, headY + 16f, cx + 25f, headY + 12f)
        rightEarPath.close()
        canvas.drawPath(rightEarPath, furPaint)
        canvas.drawPath(rightEarPath, strokePaint)

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

        if (motionState == PetMotionState.DRAGGED) {
            // 被拎起受困闭眼 (> <)
            canvas.drawLine(cx - 21f, faceY - 7f, cx - 14f, faceY - 2f, strokePaint)
            canvas.drawLine(cx - 14f, faceY - 2f, cx - 21f, faceY + 3f, strokePaint)

            canvas.drawLine(cx + 21f, faceY - 7f, cx + 14f, faceY - 2f, strokePaint)
            canvas.drawLine(cx + 14f, faceY - 2f, cx + 21f, faceY + 3f, strokePaint)
        } else {
            if (showHeart) {
                // 开心微笑眯眼 (^ ^)
                canvas.drawArc(cx - 22f, faceY - 8f, cx - 12f, faceY + 2f, 200f, 140f, false, strokePaint)
                canvas.drawArc(cx + 12f, faceY - 8f, cx + 22f, faceY + 2f, 200f, 140f, false, strokePaint)
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
        val blushAlpha = if (showHeart) 230 else 160
        blushPaint.alpha = blushAlpha
        canvas.drawCircle(cx - 24f, faceY + 5f, 6.5f, blushPaint)
        canvas.drawCircle(cx + 24f, faceY + 5f, 6.5f, blushPaint)

        // 纽扣小黑鼻与小狗微笑嘴
        canvas.drawOval(cx - 4.0f, faceY + 2f, cx + 4.0f, faceY + 6.8f, eyePaint)
        canvas.drawArc(cx - 7f, faceY + 4.5f, cx, faceY + 11.5f, 10f, 160f, false, strokePaint)
        canvas.drawArc(cx, faceY + 4.5f, cx + 7f, faceY + 11.5f, 10f, 160f, false, strokePaint)
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

        canvas.drawCircle(cx - 11f, chestY + 10f, 5.5f, furPaint)
        canvas.drawCircle(cx - 11f, chestY + 10f, 5.5f, strokePaint)
        canvas.drawCircle(cx + 11f, chestY + 10f, 5.5f, furPaint)
        canvas.drawCircle(cx + 11f, chestY + 10f, 5.5f, strokePaint)
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
                }

                val screenWidth = context.resources.displayMetrics.widthPixels
                val screenHeight = context.resources.displayMetrics.heightPixels
                val clampedX = (initialX + dx).coerceIn(0, screenWidth - width)
                val clampedY = (initialY + dy).coerceIn(0, screenHeight - height)

                layoutParams.x = clampedX
                layoutParams.y = clampedY
                currentExactX = clampedX.toFloat()
                currentExactY = clampedY.toFloat()
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
                    handlePetClick()
                } else {
                    onPetReleased()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handlePetClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < 350) {
            // 双击：无缝开启伴侣应用主界面
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
        restDuration = 70 // 抚摸后温顺停下歇息 2 秒
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
        currentExactX = layoutParams.x.toFloat()
        currentExactY = layoutParams.y.toFloat()
        motionState = PetMotionState.IDLE
        idleTicks = 0
        restDuration = 40 // 释放后先停稳 1 秒
        triggerSquashAndStretch()
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
