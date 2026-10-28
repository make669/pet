package com.couple.pet.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import androidx.core.graphics.toColorInt
import com.couple.pet.model.PetMotionState
import com.couple.pet.network.PetSyncEngine
import kotlin.math.abs

/**
 * 桌面悬浮宠物核心绘制与物理交互视图
 */
@SuppressLint("ViewConstructor")
class PetFloatingView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams
) : View(context) {

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF9E80".toColorInt() // 可爱橘猫暖橙色
        style = Paint.Style.FILL
    }

    private val earPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF6E40".toColorInt()
        style = Paint.Style.FILL
    }

    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#263238".toColorInt()
        style = Paint.Style.FILL
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF80AB".toColorInt() // 粉嫩腮红
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#D81B60".toColorInt()
        textSize = 32f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private var motionState: PetMotionState = PetMotionState.IDLE
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    // 预分配 Shape 矩形对象，避免在 onDraw 频繁创建垃圾触发 GC
    private val leftEarRect = RectF()
    private val rightEarRect = RectF()
    private val bodyRect = RectF()

    // 自由落体动画
    private var dropAnimator: ValueAnimator? = null

    // 心跳/动画帧游标
    private var animTick = 0
    private var showHeartBubble = false
    private val handler = Handler(Looper.getMainLooper())
    private val animRunnable = object : Runnable {
        override fun run() {
            animTick = (animTick + 1) % 60
            invalidate()
            handler.postDelayed(this, 80)
        }
    }

    init {
        handler.post(animRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(animRunnable)
        dropAnimator?.cancel()
        dropAnimator = null
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val centerX = w / 2f
        val centerY = h / 2f

        // 呼吸微动偏移
        val breathOffset = if (motionState == PetMotionState.IDLE) {
            ((animTick % 10) - 5) * 0.4f
        } else {
            0f
        }

        // 1. 绘制耳朵
        leftEarRect.set(centerX - 48f, centerY - 52f, centerX - 12f, centerY - 16f)
        rightEarRect.set(centerX + 12f, centerY - 52f, centerX + 48f, centerY - 16f)
        canvas.drawRoundRect(leftEarRect, 8f, 8f, earPaint)
        canvas.drawRoundRect(rightEarRect, 8f, 8f, earPaint)

        // 2. 绘制圆润身体
        bodyRect.set(centerX - 50f, centerY - 28f + breathOffset, centerX + 50f, centerY + 46f + breathOffset)
        canvas.drawRoundRect(bodyRect, 32f, 32f, bodyPaint)

        // 3. 绘制眼睛（被拎起时眯眼，平时圆眼）
        if (motionState == PetMotionState.DRAGGED) {
            // 挣扎闭眼 > <
            canvas.drawLine(centerX - 24f, centerY - 6f, centerX - 12f, centerY, eyePaint)
            canvas.drawLine(centerX - 12f, centerY, centerX - 24f, centerY + 6f, eyePaint)
            canvas.drawLine(centerX + 24f, centerY - 6f, centerX + 12f, centerY, eyePaint)
            canvas.drawLine(centerX + 12f, centerY, centerX + 24f, centerY + 6f, eyePaint)
        } else {
            // 眨眼动画
            val isBlinking = animTick in 45..48
            if (isBlinking) {
                canvas.drawLine(centerX - 24f, centerY, centerX - 12f, centerY, eyePaint)
                canvas.drawLine(centerX + 12f, centerY, centerX + 24f, centerY, eyePaint)
            } else {
                canvas.drawCircle(centerX - 18f, centerY - 2f + breathOffset, 5f, eyePaint)
                canvas.drawCircle(centerX + 18f, centerY - 2f + breathOffset, 5f, eyePaint)
            }
        }

        // 4. 粉嫩腮红
        canvas.drawCircle(centerX - 30f, centerY + 10f + breathOffset, 7f, blushPaint)
        canvas.drawCircle(centerX + 30f, centerY + 10f + breathOffset, 7f, blushPaint)

        // 5. 点击冒爱心气泡
        if (showHeartBubble) {
            canvas.drawText("❤️ +1", centerX, centerY - 50f, textPaint)
        }
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
                try {
                    windowManager.updateViewLayout(this, layoutParams)
                } catch (_: Exception) {}
                return true
            }

            MotionEvent.ACTION_UP -> {
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

    private fun onPetClicked() {
        motionState = PetMotionState.IDLE
        showHeartBubble = true
        PetSyncEngine.poke()
        invalidate()
        handler.postDelayed({
            showHeartBubble = false
            invalidate()
        }, 1200)
    }

    private fun onPetReleased() {
        val screenHeight = context.resources.displayMetrics.heightPixels
        val targetY = (screenHeight * 0.72f).toInt() // 掉落停靠目标线

        if (layoutParams.y < targetY) {
            motionState = PetMotionState.FALLING
            dropAnimator?.cancel()
            dropAnimator = ValueAnimator.ofInt(layoutParams.y, targetY).apply {
                duration = 600
                interpolator = BounceInterpolator() // 落地Q弹回跳效果
                addUpdateListener { anim ->
                    layoutParams.y = anim.animatedValue as Int
                    try {
                        windowManager.updateViewLayout(this@PetFloatingView, layoutParams)
                    } catch (_: Exception) {}
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        motionState = PetMotionState.IDLE
                        invalidate()
                    }
                })
                start()
            }
        } else {
            motionState = PetMotionState.IDLE
            invalidate()
        }
    }
}
