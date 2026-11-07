package com.couple.pet.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/**
 * 手绘米浆纸微噪点修饰符 (Hand-Drawn Washi Parchment Paper Modifier)
 * 纯算法生成无依赖：通过正片叠底在背景上渲染轻微有机纤维与手作纸浆质感。
 */
fun Modifier.handDrawnWashiParchment(
    alpha: Float = 0.22f
): Modifier = composed {
    val paperBitmap = remember {
        val size = 200
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = android.graphics.Color.parseColor("#4A3B32")
        }
        val rand = Random(42)
        // 渲染植物细纤维短线与微噪点
        for (i in 0 until 140) {
            val x = rand.nextFloat() * size
            val y = rand.nextFloat() * size
            val len = 2f + rand.nextFloat() * 7f
            val angle = rand.nextFloat() * 3.14159f
            paint.strokeWidth = 0.8f + rand.nextFloat() * 0.8f
            paint.alpha = 12 + rand.nextInt(32)
            canvas.drawLine(x, y, x + (cos(angle) * len).toFloat(), y + (sin(angle) * len).toFloat(), paint)
        }
        bmp.asImageBitmap()
    }

    this.drawWithCache {
        onDrawWithContent {
            drawContent()
            drawImage(
                image = paperBitmap,
                blendMode = BlendMode.Multiply,
                alpha = alpha
            )
        }
    }
}
