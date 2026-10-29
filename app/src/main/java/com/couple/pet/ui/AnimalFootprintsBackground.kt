package com.couple.pet.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.graphics.toColorInt

/**
 * 24 种经典动物手印/足印元数据定义
 */
data class AnimalTrackInfo(
    val id: String,
    val name: String,
    val emoji: String,
    val relX: Float,        // 相对横坐标 (避开卡片遮挡，分布在四周开阔可见区域)
    val relY: Float,        // 相对纵坐标
    val rotation: Float,    // 旋转倾斜角 (-45f ~ 45f)
    val scale: Float = 1.6f // 放大比例，确保清晰可辨
)

/**
 * 24 种精细动物足印库定义 (精心布局在屏幕开阔边缘与下方留白大区，绝不被卡片遮挡)
 */
object AnimalFootprintLibrary {

    val TRACKS = listOf(
        // Zone A: 顶部栏开阔区域 (4种)
        AnimalTrackInfo("dog", "小狗", "🐶", 0.07f, 0.038f, 18f, 1.6f),
        AnimalTrackInfo("cat", "小猫", "🐱", 0.93f, 0.038f, -22f, 1.5f),
        AnimalTrackInfo("hamster", "仓鼠", "🐹", 0.38f, 0.020f, 12f, 1.4f),
        AnimalTrackInfo("bird", "小鸟", "🐥", 0.62f, 0.020f, -16f, 1.4f),

        // Zone B: 左侧留白侧边栏 (6种，沿着屏幕左缘从上到下漫步)
        AnimalTrackInfo("rabbit", "小兔", "🐰", 0.045f, 0.16f, 25f, 1.6f),
        AnimalTrackInfo("turtle", "乌龟", "🐢", 0.045f, 0.28f, -12f, 1.55f),
        AnimalTrackInfo("duck", "鸭子", "🦆", 0.045f, 0.40f, 20f, 1.5f),
        AnimalTrackInfo("frog", "青蛙", "🐸", 0.045f, 0.52f, -28f, 1.5f),
        AnimalTrackInfo("sheep", "小羊", "🐑", 0.045f, 0.64f, 15f, 1.5f),
        AnimalTrackInfo("pig", "小猪", "🐷", 0.045f, 0.74f, -20f, 1.55f),

        // Zone C: 右侧留白侧边栏 (6种，沿着屏幕右缘从上到下漫步)
        AnimalTrackInfo("monkey", "猴子", "🐒", 0.955f, 0.16f, -18f, 1.65f),
        AnimalTrackInfo("bear", "棕熊", "🐻", 0.955f, 0.28f, 16f, 1.8f),
        AnimalTrackInfo("fox", "狐狸", "🦊", 0.955f, 0.40f, -15f, 1.55f),
        AnimalTrackInfo("raccoon", "浣熊", "🦝", 0.955f, 0.52f, 22f, 1.6f),
        AnimalTrackInfo("hedgehog", "刺猬", "🦔", 0.955f, 0.64f, -15f, 1.45f),
        AnimalTrackInfo("sloth", "树懒", "🦥", 0.955f, 0.74f, 25f, 1.6f),

        // Zone D: 下方开阔空白大区 (8种，围绕在漫步按钮与底部大空间，醒目清晰！)
        AnimalTrackInfo("koala", "考拉", "🐨", 0.50f, 0.72f, -12f, 1.6f),
        AnimalTrackInfo("elephant", "大象", "🐘", 0.13f, 0.81f, -15f, 2.0f),
        AnimalTrackInfo("tiger", "老虎", "🐯", 0.87f, 0.81f, 20f, 1.9f),
        AnimalTrackInfo("horse", "小马", "🐴", 0.11f, 0.91f, 18f, 1.7f),
        AnimalTrackInfo("cow", "小牛", "🐮", 0.89f, 0.91f, -22f, 1.7f),
        AnimalTrackInfo("panda", "熊猫", "🐼", 0.26f, 0.965f, -12f, 1.8f),
        AnimalTrackInfo("penguin", "企鹅", "🐧", 0.50f, 0.965f, 0f, 1.5f),
        AnimalTrackInfo("deer", "小鹿", "🦌", 0.74f, 0.965f, 15f, 1.6f)
    )
}

/**
 * 温暖手绘动物足迹底纹组件 (AnimalFootprintsBackground)
 * 采用清晰可见的焦糖暖栗色（高对比度 + 适中透明度），清晰呈现 24 种动物足印。
 */
@Composable
fun AnimalFootprintsBackground(
    modifier: Modifier = Modifier,
    alpha: Float = 0.42f // 大幅提高透明度，由先前的 0.10f 提升到 0.42f，清晰立现！
) {
    // 采用更浓郁清晰的焦糖栗木色 #7D4E2D
    val trackPaint = remember(alpha) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#7D4E2D".toColorInt()
            style = Paint.Style.FILL
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        }
    }

    val strokePaint = remember(alpha) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#7D4E2D".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 3.2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        }
    }

    val labelPaint = remember(alpha) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#6E4223".toColorInt()
            textSize = 22f
            textAlign = Paint.Align.CENTER
            this.alpha = ((alpha + 0.15f).coerceAtMost(1.0f) * 255).toInt()
        }
    }

    val path = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val w = size.width
            val h = size.height

            for (track in AnimalFootprintLibrary.TRACKS) {
                val cx = track.relX * w
                val cy = track.relY * h

                canvas.save()
                canvas.translate(cx, cy)

                // 绘制足印本身 (带旋转与放大比例)
                canvas.save()
                canvas.rotate(track.rotation)
                canvas.scale(track.scale, track.scale)

                when (track.id) {
                    "dog" -> drawDogPaw(canvas, path, trackPaint)
                    "cat" -> drawCatPaw(canvas, path, trackPaint)
                    "rabbit" -> drawRabbitTrack(canvas, path, trackPaint)
                    "monkey" -> drawMonkeyHand(canvas, path, trackPaint)
                    "turtle" -> drawTurtleFoot(canvas, path, trackPaint)
                    "bear" -> drawBearPaw(canvas, path, trackPaint)
                    "duck" -> drawDuckWeb(canvas, path, trackPaint, strokePaint)
                    "bird" -> drawBirdTrack(canvas, path, strokePaint)
                    "elephant" -> drawElephantFoot(canvas, path, trackPaint)
                    "horse" -> drawHorseHoof(canvas, path, trackPaint)
                    "cow" -> drawCowHoof(canvas, path, trackPaint)
                    "sheep" -> drawSheepHoof(canvas, path, trackPaint)
                    "pig" -> drawPigHoof(canvas, path, trackPaint)
                    "fox" -> drawFoxPaw(canvas, path, trackPaint)
                    "raccoon" -> drawRaccoonHand(canvas, path, trackPaint)
                    "hamster" -> drawHamsterPaw(canvas, path, trackPaint)
                    "frog" -> drawFrogFoot(canvas, path, trackPaint, strokePaint)
                    "penguin" -> drawPenguinFoot(canvas, path, trackPaint, strokePaint)
                    "deer" -> drawDeerHoof(canvas, path, trackPaint)
                    "hedgehog" -> drawHedgehogFoot(canvas, path, trackPaint)
                    "panda" -> drawPandaPaw(canvas, path, trackPaint)
                    "tiger" -> drawTigerPaw(canvas, path, trackPaint)
                    "sloth" -> drawSlothClaw(canvas, path, strokePaint)
                    "koala" -> drawKoalaHand(canvas, path, trackPaint)
                }
                canvas.restore()

                // 在足印下方带一个超可爱微缩标签（如 "小狗"、"乌龟"、"猴子"），辨识度拉满！
                canvas.drawText(track.name, 0f, 26f * track.scale, labelPaint)

                canvas.restore()
            }
        }
    }
}

// ================= 24 种动物足印几何绘制核心实现 =================

/** 1. 小狗：中央三叶掌垫 + 4 颗椭圆脚趾肉垫 */
private fun drawDogPaw(canvas: Canvas, path: Path, paint: Paint) {
    path.reset()
    path.moveTo(-8f, 6f)
    path.cubicTo(-12f, 12f, 12f, 12f, 8f, 6f)
    path.cubicTo(14f, 0f, 6f, -8f, 0f, -4f)
    path.cubicTo(-6f, -8f, -14f, 0f, -8f, 6f)
    path.close()
    canvas.drawPath(path, paint)

    canvas.drawOval(RectF(-13f, -16f, -7f, -7f), paint)
    canvas.drawOval(RectF(-5f, -20f, -1f, -10f), paint)
    canvas.drawOval(RectF(1f, -20f, 5f, -10f), paint)
    canvas.drawOval(RectF(7f, -16f, 13f, -7f), paint)
}

/** 2. 小猫：圆润心形掌垫 + 4 颗小圆肉垫 (无抓痕收缩自如) */
private fun drawCatPaw(canvas: Canvas, path: Path, paint: Paint) {
    path.reset()
    path.moveTo(0f, 8f)
    path.cubicTo(-12f, 2f, -12f, -6f, 0f, -1f)
    path.cubicTo(12f, -6f, 12f, 2f, 0f, 8f)
    path.close()
    canvas.drawPath(path, paint)

    canvas.drawCircle(-10f, -10f, 3.4f, paint)
    canvas.drawCircle(-3.5f, -14f, 3.6f, paint)
    canvas.drawCircle(3.5f, -14f, 3.6f, paint)
    canvas.drawCircle(10f, -10f, 3.4f, paint)
}

/** 3. 兔子：长条状修长后足脚印 + 两枚前足圆印 */
private fun drawRabbitTrack(canvas: Canvas, path: Path, paint: Paint) {
    // 两个长后脚掌
    canvas.drawOval(RectF(-11f, -16f, -5f, 12f), paint)
    canvas.drawOval(RectF(5f, -16f, 11f, 12f), paint)
    // 两个小前爪
    canvas.drawCircle(-2f, -8f, 3.2f, paint)
    canvas.drawCircle(2f, 0f, 3.2f, paint)
}

/** 4. 猴子：细长灵巧小手掌 + 4 根手指 + 1 根向外伸出的拇指 */
private fun drawMonkeyHand(canvas: Canvas, path: Path, paint: Paint) {
    // 掌心
    canvas.drawOval(RectF(-7f, 0f, 7f, 12f), paint)
    // 拇指 (朝左侧外展)
    canvas.drawOval(RectF(-14f, 3f, -6f, 7f), paint)
    // 食指、中指、无名指、小指
    canvas.drawOval(RectF(-7f, -12f, -3.5f, 2f), paint)
    canvas.drawOval(RectF(-2.5f, -15f, 1f, 2f), paint)
    canvas.drawOval(RectF(2f, -13f, 5.5f, 2f), paint)
    canvas.drawOval(RectF(6f, -10f, 9.5f, 2f), paint)
}

/** 5. 乌龟：爬虫宽掌印 + 5 枚向外放射的尖锐抓痕 */
private fun drawTurtleFoot(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawCircle(0f, 2f, 7f, paint)
    canvas.drawOval(RectF(-12f, -6f, -8f, 0f), paint)
    canvas.drawOval(RectF(-8f, -11f, -4f, -4f), paint)
    canvas.drawOval(RectF(-2f, -13f, 2f, -5f), paint)
    canvas.drawOval(RectF(4f, -11f, 8f, -4f), paint)
    canvas.drawOval(RectF(8f, -6f, 12f, 0f), paint)
}

/** 6. 棕熊：宽大肾形厚重脚掌 + 5 颗粗大脚趾 + 趾尖锋利抓痕点 */
private fun drawBearPaw(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawOval(RectF(-13f, -2f, 13f, 12f), paint)
    for (i in -2..2) {
        val x = i * 5.5f
        val y = -7f - kotlin.math.abs(i) * 1.5f
        canvas.drawCircle(x, y, 2.6f, paint)
        canvas.drawCircle(x, y - 4f, 1.0f, paint) // 爪尖痕
    }
}

/** 7. 鸭子：三角相连的薄膜蹼足印 */
private fun drawDuckWeb(canvas: Canvas, path: Path, fillPaint: Paint, strokePaint: Paint) {
    path.reset()
    path.moveTo(0f, 10f)
    path.lineTo(-12f, -10f)
    path.quadTo(-5f, -7f, 0f, -14f)
    path.quadTo(5f, -7f, 12f, -10f)
    path.close()
    canvas.drawPath(path, fillPaint)
    canvas.drawPath(path, strokePaint)
}

/** 8. 小鸟/小鸡：前三趾叉开 + 后一短距 */
private fun drawBirdTrack(canvas: Canvas, path: Path, strokePaint: Paint) {
    path.reset()
    // 中心点向三前趾
    path.moveTo(0f, 2f)
    path.lineTo(-11f, -11f)
    path.moveTo(0f, 2f)
    path.lineTo(0f, -14f)
    path.moveTo(0f, 2f)
    path.lineTo(11f, -11f)
    // 后脚趾
    path.moveTo(0f, 2f)
    path.lineTo(0f, 9f)
    canvas.drawPath(path, strokePaint)
}

/** 9. 大象：巨大的圆形厚实平足印 + 5 枚扁圆指甲边缘标记 */
private fun drawElephantFoot(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawCircle(0f, 2f, 12f, paint)
    for (i in -2..2) {
        val x = i * 4.6f
        val y = -8f - kotlin.math.abs(i) * 0.8f
        canvas.drawOval(RectF(x - 2f, y - 2f, x + 2f, y + 2f), paint)
    }
}

/** 10. 小马：标准 U 形弧马蹄印 */
private fun drawHorseHoof(canvas: Canvas, path: Path, paint: Paint) {
    path.reset()
    path.moveTo(-8f, 8f)
    path.cubicTo(-12f, -8f, 12f, -8f, 8f, 8f)
    path.lineTo(5f, 8f)
    path.cubicTo(8f, -4f, -8f, -4f, -5f, 8f)
    path.close()
    canvas.drawPath(path, paint)
}

/** 11. 小牛：深裂两瓣偶蹄印 */
private fun drawCowHoof(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawOval(RectF(-7.5f, -10f, -0.5f, 8f), paint)
    canvas.drawOval(RectF(0.5f, -10f, 7.5f, 8f), paint)
}

/** 12. 小羊：纤细水滴状并拢双瓣偶蹄 */
private fun drawSheepHoof(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawOval(RectF(-5.5f, -8f, -0.5f, 6f), paint)
    canvas.drawOval(RectF(0.5f, -8f, 5.5f, 6f), paint)
}

/** 13. 小猪：两瓣前蹄印 + 背后两枚悬蹄小圆点 */
private fun drawPigHoof(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawOval(RectF(-6f, -10f, -0.5f, 4f), paint)
    canvas.drawOval(RectF(0.5f, -10f, 6f, 4f), paint)
    canvas.drawCircle(-5f, 8f, 1.8f, paint)
    canvas.drawCircle(5f, 8f, 1.8f, paint)
}

/** 14. 狐狸：修长尖窄小狐爪印 */
private fun drawFoxPaw(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawOval(RectF(-5f, 0f, 5f, 8f), paint)
    canvas.drawOval(RectF(-8f, -12f, -4f, -4f), paint)
    canvas.drawOval(RectF(-3f, -15f, 0f, -5f), paint)
    canvas.drawOval(RectF(0f, -15f, 3f, -5f), paint)
    canvas.drawOval(RectF(4f, -12f, 8f, -4f), paint)
}

/** 15. 浣熊：类人细长小手印 (5 根均匀修长指头) */
private fun drawRaccoonHand(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawOval(RectF(-6f, 1f, 6f, 9f), paint)
    for (i in -2..2) {
        val x = i * 3.5f
        val h = 12f - kotlin.math.abs(i) * 2f
        canvas.drawOval(RectF(x - 1.2f, -h, x + 1.2f, 2f), paint)
    }
}

/** 16. 仓鼠：微型小爪星印 */
private fun drawHamsterPaw(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawCircle(0f, 2f, 2.8f, paint)
    canvas.drawCircle(-4.5f, -2f, 1.2f, paint)
    canvas.drawCircle(-2f, -5.5f, 1.4f, paint)
    canvas.drawCircle(2f, -5.5f, 1.4f, paint)
    canvas.drawCircle(4.5f, -2f, 1.2f, paint)
}

/** 17. 青蛙：细长叉指 + 指端圆润吸盘小圆点 */
private fun drawFrogFoot(canvas: Canvas, path: Path, fillPaint: Paint, strokePaint: Paint) {
    path.reset()
    for (angle in listOf(-60f, -20f, 20f, 60f)) {
        val rad = Math.toRadians(angle.toDouble())
        val tx = (kotlin.math.sin(rad) * 12.0).toFloat()
        val ty = (-kotlin.math.cos(rad) * 12.0).toFloat()
        canvas.drawLine(0f, 3f, tx, ty, strokePaint)
        canvas.drawCircle(tx, ty, 2.2f, fillPaint)
    }
    canvas.drawCircle(0f, 3f, 3.2f, fillPaint)
}

/** 18. 企鹅：短粗扇形蹼足 */
private fun drawPenguinFoot(canvas: Canvas, path: Path, fillPaint: Paint, strokePaint: Paint) {
    path.reset()
    path.moveTo(0f, 8f)
    path.lineTo(-10f, -8f)
    path.lineTo(0f, -11f)
    path.lineTo(10f, -8f)
    path.close()
    canvas.drawPath(path, fillPaint)
    canvas.drawPath(path, strokePaint)
}

/** 19. 小鹿：修长心形尖细偶蹄 */
private fun drawDeerHoof(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawOval(RectF(-6f, -12f, -0.6f, 6f), paint)
    canvas.drawOval(RectF(0.6f, -12f, 6f, 6f), paint)
}

/** 20. 刺猬：小肉垫与 5 枚尖指 */
private fun drawHedgehogFoot(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawOval(RectF(-5f, 0f, 5f, 7f), paint)
    for (i in -2..2) {
        val x = i * 2.8f
        canvas.drawCircle(x, -4f - kotlin.math.abs(i) * 1.0f, 1.4f, paint)
    }
}

/** 21. 熊猫：圆滚大掌印 + 宽厚假拇指 */
private fun drawPandaPaw(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawCircle(0f, 3f, 8f, paint)
    canvas.drawCircle(-9f, 2f, 3.2f, paint) // 假拇指
    for (i in -2..2) {
        val x = i * 4.2f
        canvas.drawCircle(x, -6f, 2.8f, paint)
    }
}

/** 22. 老虎：霸气硕大猫科重掌垫 + 4 颗大肉掌 */
private fun drawTigerPaw(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawOval(RectF(-12f, -2f, 12f, 10f), paint)
    canvas.drawCircle(-11f, -8f, 4.2f, paint)
    canvas.drawCircle(-4f, -12f, 4.6f, paint)
    canvas.drawCircle(4f, -12f, 4.6f, paint)
    canvas.drawCircle(11f, -8f, 4.2f, paint)
}

/** 23. 树懒：3 枚长弯钩爪痕 */
private fun drawSlothClaw(canvas: Canvas, path: Path, strokePaint: Paint) {
    canvas.drawArc(RectF(-8f, -14f, -2f, 6f), 120f, 180f, false, strokePaint)
    canvas.drawArc(RectF(-3f, -16f, 3f, 6f), 120f, 180f, false, strokePaint)
    canvas.drawArc(RectF(2f, -14f, 8f, 6f), 120f, 180f, false, strokePaint)
}

/** 24. 考拉：独特的对生双拇指 (2 个拇指 + 3 根手指) */
private fun drawKoalaHand(canvas: Canvas, path: Path, paint: Paint) {
    canvas.drawOval(RectF(-6f, 1f, 6f, 8f), paint)
    // 两个拇指组 (左)
    canvas.drawOval(RectF(-11f, -8f, -7f, 1f), paint)
    canvas.drawOval(RectF(-7f, -11f, -3f, 0f), paint)
    // 三个手指组 (右)
    canvas.drawOval(RectF(-1f, -12f, 2.5f, 0f), paint)
    canvas.drawOval(RectF(3f, -11f, 6.5f, 0f), paint)
    canvas.drawOval(RectF(7f, -8f, 10.5f, 1f), paint)
}
