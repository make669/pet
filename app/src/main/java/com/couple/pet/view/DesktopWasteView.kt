package com.couple.pet.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.couple.pet.network.PetSyncEngine

/**
 * 桌面排泄物悬浮小徽章 (小巧黑白手绘风格 [💩 扫] / [💧 扫])
 * 点击即一键清扫，爆出爱心并恢复小宠清洁度与好感度
 */
@SuppressLint("ViewConstructor")
class DesktopWasteView(
    context: Context,
    private val windowManager: WindowManager,
    val wasteId: Long,
    val isPoop: Boolean,
    val initialX: Int,
    val initialY: Int,
    private val onCleanCallback: (DesktopWasteView) -> Unit
) : View(context) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6F59")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6F59")
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private var isCleaned = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && !isCleaned) {
            isCleaned = true
            PetSyncEngine.cleanWaste(wasteId)
            onCleanCallback(this)
            return true
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // 胶囊药丸圆角矩形
        val rect = RectF(4f, 4f, w - 4f, h - 4f)
        canvas.drawRoundRect(rect, 20f, 20f, bgPaint)
        canvas.drawRoundRect(rect, 20f, 20f, borderPaint)

        // 图标与文字
        val icon = if (isPoop) "💩" else "💧"
        val label = if (isPoop) "扫" else "擦"

        canvas.drawText(icon, w * 0.35f, h * 0.68f, emojiPaint)
        canvas.drawText(label, w * 0.72f, h * 0.68f, textPaint)
    }
}
