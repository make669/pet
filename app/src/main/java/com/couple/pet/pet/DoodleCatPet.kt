package com.couple.pet.pet

import android.graphics.Canvas
import kotlin.math.sin

/**
 * 软萌线条小猫 (DoodleCatPet) 继承自通用宠物基类 [BasePet]
 * 经典黑白手绘治愈猫咪：俏皮三角尖耳、胡须腮腮、修长卷尾巴。
 */
class DoodleCatPet : BasePet(
    speciesId = "doodle_cat",
    speciesName = "软萌奶猫",
    speciesEmoji = "🐱",
    defaultNick = "咪咪",
    description = "傲娇又黏人、喜欢揣手手晒太阳的治愈线条猫咪",
    favoriteFood = "小鱼干"
) {

    override fun getGreeting(): String = "喵呜~ 今天也要乖乖摸本喵哦 🐾"

    override fun getTouchReaction(): String = "舒服得眯起眼，喉咙里发出呼噜呼噜的踩奶声 ❤️"

    override fun getSpecialTrickName(): String = "猫猫揣手踩奶"

    // ================= 1. 耳朵绘制 (俏皮三角尖耳朵) =================
    override fun drawEars(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val headY = -12f + breath

        // 左耳 (立尖耳)
        leftEarPath.reset()
        leftEarPath.moveTo(-22f, headY - 14f)
        leftEarPath.lineTo(-32f, headY - 38f)
        leftEarPath.lineTo(-10f, headY - 24f)
        leftEarPath.close()
        canvas.drawPath(leftEarPath, paints.furPaint)
        canvas.drawPath(leftEarPath, paints.strokePaint)

        // 右耳 (立尖耳)
        rightEarPath.reset()
        rightEarPath.moveTo(22f, headY - 14f)
        rightEarPath.lineTo(32f, headY - 38f)
        rightEarPath.lineTo(10f, headY - 24f)
        rightEarPath.close()
        canvas.drawPath(rightEarPath, paints.furPaint)
        canvas.drawPath(rightEarPath, paints.strokePaint)
    }

    // ================= 2. 头部轮廓绘制 =================
    override fun drawHead(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val headY = -10f + breath
        headPath.reset()
        headPath.moveTo(-28f, headY)
        headPath.cubicTo(-30f, headY - 24f, 30f, headY - 24f, 28f, headY)
        headPath.cubicTo(34f, headY + 20f, -34f, headY + 20f, -28f, headY)
        headPath.close()
        canvas.drawPath(headPath, paints.furPaint)
        canvas.drawPath(headPath, paints.strokePaint)
    }

    // ================= 3. 五官细节绘制 (猫猫胡须 + 倒三角纽扣鼻) =================
    override fun drawFace(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val faceY = -8f + breath

        // 萌萌大猫眼
        canvas.drawCircle(-13f, faceY, 3.4f, paints.eyePaint)
        canvas.drawCircle(-14.2f, faceY - 1.2f, 1.2f, paints.eyeGlintPaint)
        canvas.drawCircle(13f, faceY, 3.4f, paints.eyePaint)
        canvas.drawCircle(11.8f, faceY - 1.2f, 1.2f, paints.eyeGlintPaint)

        // 粉腮红
        canvas.drawCircle(-22f, faceY + 4f, 4f, paints.blushPaint)
        canvas.drawCircle(22f, faceY + 4f, 4f, paints.blushPaint)

        // 倒三角粉嫩小猫鼻
        canvas.drawCircle(0f, faceY + 3.2f, 2.2f, paints.tonguePaint)

        // 人字形微笑猫咪嘴
        canvas.drawLine(-3f, faceY + 6.5f, 0f, faceY + 5.5f, paints.fineStrokePaint)
        canvas.drawLine(0f, faceY + 5.5f, 3f, faceY + 6.5f, paints.fineStrokePaint)

        // 左右两对标志性可爱胡须
        canvas.drawLine(-22f, faceY + 2f, -34f, faceY, paints.fineStrokePaint)
        canvas.drawLine(-22f, faceY + 6f, -33f, faceY + 8f, paints.fineStrokePaint)

        canvas.drawLine(22f, faceY + 2f, 34f, faceY, paints.fineStrokePaint)
        canvas.drawLine(22f, faceY + 6f, 33f, faceY + 8f, paints.fineStrokePaint)
    }

    // ================= 4. 身体躯干绘制 =================
    override fun drawBody(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val bodyY = 14f + breath
        bodyPath.reset()
        bodyPath.moveTo(-24f, bodyY - 8f)
        bodyPath.cubicTo(-34f, bodyY + 16f, -22f, bodyY + 26f, -12f, bodyY + 26f)
        bodyPath.lineTo(12f, bodyY + 26f)
        bodyPath.cubicTo(22f, bodyY + 26f, 34f, bodyY + 16f, 24f, bodyY - 8f)
        bodyPath.close()

        canvas.drawPath(bodyPath, paints.furPaint)
        canvas.drawPath(bodyPath, paints.strokePaint)
    }

    // ================= 5. 四肢爪子绘制 =================
    override fun drawLimbs(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val bodyY = 14f + breath
        val isWalking = motion == UniversalPetAction.WALK
        val stride = if (isWalking) sin(tick * 0.28).toFloat() * 3.5f else 0f

        // 软萌小猫爪 (微圆带爪缝微线)
        canvas.drawCircle(-17f - stride * 0.4f, bodyY + 25f + stride, 4.4f, paints.furPaint)
        canvas.drawCircle(-17f - stride * 0.4f, bodyY + 25f + stride, 4.4f, paints.strokePaint)

        canvas.drawCircle(17f + stride * 0.4f, bodyY + 25f - stride, 4.4f, paints.furPaint)
        canvas.drawCircle(17f + stride * 0.4f, bodyY + 25f - stride, 4.4f, paints.strokePaint)
    }

    // ================= 6. 猫咪长卷尾巴 (优雅波浪轻晃) =================
    override fun drawTail(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val tailBaseX = 24f
        val tailBaseY = 22f + breath
        val wave = sin(tick * 0.18).toFloat() * 12f

        canvas.save()
        tailPath.reset()
        tailPath.moveTo(tailBaseX, tailBaseY)
        tailPath.cubicTo(
            tailBaseX + 16f, tailBaseY - 14f + wave,
            tailBaseX + 30f, tailBaseY - 26f - wave,
            tailBaseX + 18f, tailBaseY - 38f
        )
        tailPath.cubicTo(
            tailBaseX + 12f, tailBaseY - 40f,
            tailBaseX + 10f, tailBaseY - 32f,
            tailBaseX + 14f, tailBaseY - 24f + wave
        )
        tailPath.cubicTo(
            tailBaseX + 18f, tailBaseY - 16f,
            tailBaseX + 8f, tailBaseY - 4f,
            tailBaseX, tailBaseY + 4f
        )
        tailPath.close()

        canvas.drawPath(tailPath, paints.furPaint)
        canvas.drawPath(tailPath, paints.strokePaint)
        canvas.restore()
    }
}
