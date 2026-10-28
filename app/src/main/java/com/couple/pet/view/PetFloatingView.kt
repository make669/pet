package com.couple.pet.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.toColorInt
import com.couple.pet.model.PetMotionState
import com.couple.pet.network.PetSyncEngine
import kotlin.math.abs
import kotlin.math.sin

/**
 * 桌面悬浮宠物：极简治愈系“线条小狗”高质感渲染与物理引擎
 */
@SuppressLint("ViewConstructor")
class PetFloatingView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams
) : View(context) {

    // 1. 纯白蓬松狗毛画笔
    private val furPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // 2. 手绘黑线轮廓画笔（仿马克笔质感）
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E1E1E".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 5.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // 3. 呆萌豆豆眼与小黑鼻
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E1E1E".toColorInt()
        style = Paint.Style.FILL
    }

    // 4. 软萌少女粉圆腮红
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFAFC0".toColorInt()
        style = Paint.Style.FILL
    }

    // 5. 手绘红爱心
    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF4B68".toColorInt()
        style = Paint.Style.FILL
    }

    // 6. 状态与坐标管理
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

    // 预分配 Path 杜绝 onDraw 中 GC
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
            invalidate()
            handler.postDelayed(this, 30) // ~33fps 平滑物理帧
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
        val centerY = h / 2f + 6f

        // 落地挤压缩放
        canvas.save()
        canvas.scale(scaleXFactor, scaleYFactor, centerX, centerY + 45f)

        // 呼吸微动位移 (Idle 状态下小起伏)
        val breathOffset = if (motionState == PetMotionState.IDLE) {
            sin(tick * 0.12).toFloat() * 2.2f
        } else {
            0f
        }

        // 1. 绘制摇摇小尾巴 (位于身体后层)
        drawTail(canvas, centerX, centerY, breathOffset)

        // 2. 绘制小狗身体与小短腿
        drawBody(canvas, centerX, centerY, breathOffset)

        // 3. 绘制蓬松大圆头与呆萌耳朵
        drawHeadAndEars(canvas, centerX, centerY, breathOffset)

        // 4. 绘制五官：豆豆眼、眨眼、小黑鼻、微笑嘴与小圆腮红
        drawFaceFeatures(canvas, centerX, centerY, breathOffset)

        // 5. 绘制两只前爪爪（双手捧爱心）
        drawFrontPawsAndHeart(canvas, centerX, centerY, breathOffset)

        canvas.restore()

        // 6. 点击冒出手绘小爱心
        if (showHeart) {
            drawFloatingHeart(canvas, centerX, centerY - 65f + heartOffsetY)
        }
    }

    /**
     * 绘制摇摇晃晃的小卷尾
     */
    private fun drawTail(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val tailBaseX = cx + 32f
        val tailBaseY = cy + 28f + breath

        // 根据运动状态计算摆尾幅度
        val tailAngle = when (motionState) {
            PetMotionState.DRAGGED -> sin(tick * 0.45).toFloat() * 28f // 慌张甩尾
            PetMotionState.IDLE -> sin(tick * 0.16).toFloat() * 16f    // 欢快摇尾
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

    /**
     * 绘制圆滚滚的白肚皮和小短腿
     */
    private fun drawBody(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val bodyY = cy + 18f + breath
        bodyPath.reset()
        // 软萌坐姿下半身轮廓
        bodyPath.moveTo(cx - 30f, bodyY - 10f)
        bodyPath.quadTo(cx - 42f, bodyY + 22f, cx - 28f, bodyY + 32f) // 左小屁股与左脚
        bodyPath.quadTo(cx - 20f, bodyY + 34f, cx - 14f, bodyY + 32f)
        bodyPath.lineTo(cx + 14f, bodyY + 32f)                         // 肚底
        bodyPath.quadTo(cx + 20f, bodyY + 34f, cx + 28f, bodyY + 32f) // 右小屁股与右脚
        bodyPath.quadTo(cx + 42f, bodyY + 22f, cx + 30f, bodyY - 10f)
        bodyPath.close()

        canvas.drawPath(bodyPath, furPaint)
        canvas.drawPath(bodyPath, strokePaint)

        // 脚丫趾甲小黑点痕迹
        val footY = bodyY + 31f
        canvas.drawLine(cx - 24f, footY - 4f, cx - 24f, footY, strokePaint)
        canvas.drawLine(cx + 24f, footY - 4f, cx + 24f, footY, strokePaint)
    }

    /**
     * 绘制标志性的“线条小狗”蓬松头型与耷拉耳朵
     */
    private fun drawHeadAndEars(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val headY = cy - 14f + breath

        // 左耳（微垂软萌）
        val earWiggle = if (motionState == PetMotionState.DRAGGED) sin(tick * 0.35).toFloat() * 6f else 0f
        leftEarPath.reset()
        leftEarPath.moveTo(cx - 32f, headY - 18f)
        leftEarPath.cubicTo(cx - 56f, headY - 26f + earWiggle, cx - 58f, headY + 12f + earWiggle, cx - 38f, headY + 6f)
        leftEarPath.close()
        canvas.drawPath(leftEarPath, furPaint)
        canvas.drawPath(leftEarPath, strokePaint)

        // 右耳
        rightEarPath.reset()
        rightEarPath.moveTo(cx + 32f, headY - 18f)
        rightEarPath.cubicTo(cx + 56f, headY - 26f - earWiggle, cx + 58f, headY + 12f - earWiggle, cx + 38f, headY + 6f)
        rightEarPath.close()
        canvas.drawPath(rightEarPath, furPaint)
        canvas.drawPath(rightEarPath, strokePaint)

        // 蓬松白大头（带头顶微翘小毛撮）
        headPath.reset()
        headPath.moveTo(cx - 36f, headY + 14f)
        headPath.cubicTo(cx - 48f, headY - 16f, cx - 26f, headY - 34f, cx - 6f, headY - 34f)
        // 头顶小呆毛撮
        headPath.lineTo(cx, headY - 40f)
        headPath.lineTo(cx + 4f, headY - 34f)
        headPath.cubicTo(cx + 26f, headY - 34f, cx + 48f, headY - 16f, cx + 36f, headY + 14f)
        // 软圆下颚弧度
        headPath.quadTo(cx, headY + 36f, cx - 36f, headY + 14f)
        headPath.close()

        canvas.drawPath(headPath, furPaint)
        canvas.drawPath(headPath, strokePaint)
    }

    /**
     * 绘制五官：经典的线条小狗表情
     */
    private fun drawFaceFeatures(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val faceY = cy - 10f + breath

        // 1. 眼睛表现（被拎起闭眼 > <，平时眨眼或圆豆豆眼）
        if (motionState == PetMotionState.DRAGGED) {
            // 慌张挣扎眼 > <
            val eyeR = 6f
            canvas.drawLine(cx - 22f - eyeR, faceY - 4f - eyeR, cx - 22f, faceY - 4f, strokePaint)
            canvas.drawLine(cx - 22f, faceY - 4f, cx - 22f - eyeR, faceY - 4f + eyeR, strokePaint)

            canvas.drawLine(cx + 22f + eyeR, faceY - 4f - eyeR, cx + 22f, faceY - 4f, strokePaint)
            canvas.drawLine(cx + 22f, faceY - 4f, cx + 22f + eyeR, faceY - 4f + eyeR, strokePaint)
        } else {
            // 规律眨眼机制 (每隔一段时间眨一下)
            val isBlinking = (tick % 75 in 0..3)
            if (isBlinking || showHeart) {
                // 开心眯眯弯月眼 ^ ^
                canvas.drawArc(cx - 26f, faceY - 10f, cx - 14f, faceY + 2f, 200f, 140f, false, strokePaint)
                canvas.drawArc(cx + 14f, faceY - 10f, cx + 26f, faceY + 2f, 200f, 140f, false, strokePaint)
            } else {
                // 乌溜溜纯真豆豆眼
                canvas.drawCircle(cx - 20f, faceY - 4f, 4.6f, eyePaint)
                canvas.drawCircle(cx + 20f, faceY - 4f, 4.6f, eyePaint)
            }
        }

        // 2. 软萌圆腮红
        val blushAlpha = if (showHeart) 230 else 170
        blushPaint.alpha = blushAlpha
        canvas.drawCircle(cx - 27f, faceY + 6f, 7.5f, blushPaint)
        canvas.drawCircle(cx + 27f, faceY + 6f, 7.5f, blushPaint)

        // 3. 纽扣小黑鼻
        canvas.drawOval(cx - 4.5f, faceY + 2f, cx + 4.5f, faceY + 7.5f, eyePaint)

        // 4. “ω”型呆萌小狗微笑嘴
        canvas.drawArc(cx - 8f, faceY + 5f, cx, faceY + 13f, 10f, 160f, false, strokePaint)
        canvas.drawArc(cx, faceY + 5f, cx + 8f, faceY + 13f, 10f, 160f, false, strokePaint)
    }

    /**
     * 绘制两只小爪子与胸前抱着的手绘爱心
     */
    private fun drawFrontPawsAndHeart(canvas: Canvas, cx: Float, cy: Float, breath: Float) {
        val chestY = cy + 13f + breath

        // 胸前抱着的爱心
        heartPath.reset()
        val heartScale = 0.55f
        heartPath.moveTo(cx, chestY + 12f)
        heartPath.cubicTo(cx - 18f * heartScale, chestY - 18f * heartScale, cx - 36f * heartScale, chestY + 4f * heartScale, cx, chestY + 32f * heartScale)
        heartPath.cubicTo(cx + 36f * heartScale, chestY + 4f * heartScale, cx + 18f * heartScale, chestY - 18f * heartScale, cx, chestY + 12f)
        heartPath.close()

        canvas.drawPath(heartPath, heartPaint)
        canvas.drawPath(heartPath, strokePaint)

        // 两只白白软软小前爪抱住爱心两侧
        // 左爪
        canvas.drawCircle(cx - 13f, chestY + 12f, 6.5f, furPaint)
        canvas.drawCircle(cx - 13f, chestY + 12f, 6.5f, strokePaint)
        // 右爪
        canvas.drawCircle(cx + 13f, chestY + 12f, 6.5f, furPaint)
        canvas.drawCircle(cx + 13f, chestY + 12f, 6.5f, strokePaint)
    }

    /**
     * 点击时上方飘起的手绘爱心
     */
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
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dropAnimator?.cancel()
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                motionState = PetMotionState.DRAGGED
                scaleXFactor = 0.94f // 被拎起轻微纵向拉长
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

    private fun onPetClicked() {
        motionState = PetMotionState.IDLE
        showHeart = true
        heartOffsetY = 0f
        PetSyncEngine.poke()
        invalidate()
        handler.postDelayed({
            showHeart = false
            invalidate()
        }, 1200)
    }

    private fun onPetReleased() {
        val screenHeight = context.resources.displayMetrics.heightPixels
        val targetY = (screenHeight * 0.72f).toInt() // 掉落底线

        if (layoutParams.y < targetY) {
            motionState = PetMotionState.FALLING
            dropAnimator?.cancel()
            dropAnimator = ValueAnimator.ofInt(layoutParams.y, targetY).apply {
                duration = 620
                interpolator = BounceInterpolator() // 落地Q弹跳跃
                addUpdateListener { anim ->
                    layoutParams.y = anim.animatedValue as Int
                    try {
                        windowManager.updateViewLayout(this@PetFloatingView, layoutParams)
                    } catch (_: Exception) {}
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        motionState = PetMotionState.IDLE
                        triggerSquashAndStretch()
                        invalidate()
                    }
                })
                start()
            }
        } else {
            motionState = PetMotionState.IDLE
            triggerSquashAndStretch()
            invalidate()
        }
    }

    /**
     * 落地弹性挤压与回弹特效 (Squash & Stretch)
     */
    private fun triggerSquashAndStretch() {
        val squashAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320
            interpolator = OvershootInterpolator(2.5f)
            addUpdateListener { anim ->
                val f = anim.animatedFraction
                // 触底瞬间压扁：X变宽，Y变扁，然后弹性复原
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
