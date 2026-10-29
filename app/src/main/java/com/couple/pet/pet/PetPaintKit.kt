package com.couple.pet.pet

import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.toColorInt

/**
 * 宠物全物种原生绘制画笔工具箱 (Pet Paint Kit)
 * 集中管理高性能防锯齿画笔，避免在每帧渲染时重复分配内存触发 GC。
 */
class PetPaintKit {

    // 1. 身体毛色填充画笔 (纯白或各物种底色)
    val furPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // 2. 标志性手绘黑线主描边画笔
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E1E1E".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 4.8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // 3. 极细细节描边画笔 (用于胡须、爪爪缝隙、微表情)
    val fineStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E1E1E".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // 4. 眼睛与纽扣鼻画笔
    val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#1E1E1E".toColorInt()
        style = Paint.Style.FILL
    }

    // 5. 眼神高光小光点画笔
    val eyeGlintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // 6. 萌萌粉色腮红画笔
    val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFAFC0".toColorInt()
        style = Paint.Style.FILL
    }

    // 7. 舌头与嘴巴微红画笔
    val tonguePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF6888".toColorInt()
        style = Paint.Style.FILL
    }

    // 8. 互动爱心画笔
    val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF4B68".toColorInt()
        style = Paint.Style.FILL
    }

    // 9. 冲刺与刨地烟尘描边画笔
    val dustPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#999999".toColorInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeCap = Paint.Cap.ROUND
    }

    // 10. 烟尘填充画笔
    val dustFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#EAEAEA".toColorInt()
        style = Paint.Style.FILL
    }

    // 11. 心情文字与 Emoji 画笔
    val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    companion object {
        // 单例默认实例，便于各渲染层即开即用
        val defaultKit by lazy { PetPaintKit() }
    }
}
