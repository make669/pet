package com.couple.pet.pet

import android.graphics.Canvas
import kotlin.math.sin

/**
 * 垂耳线条小兔 (DoodleRabbitPet) 继承自通用宠物基类 [BasePet]
 * 经典黑白手绘软萌兔子：长长软垂耳、小粉鼻、圆滚滚毛球尾巴。
 */
class DoodleRabbitPet : BasePet(
    speciesId = "doodle_rabbit",
    speciesName = "垂耳小兔",
    speciesEmoji = "🐰",
    defaultNick = "团团",
    description = "胆小软糯、最喜欢抱大胡萝卜的黑白线条小兔",
    favoriteFood = "苹果片"
) {

    override fun getGreeting(): String = "咕咕~ 软软小兔来找你玩耍啦 🐾"

    override fun getTouchReaction(): String = "小耳朵害羞地抖了抖，轻轻挨着你的手指 ❤️"

    override fun getSpecialTrickName(): String = "动鼻子嚼胡萝卜"

    // ================= 1. 耳朵绘制 (萌萌长软垂耳) =================
    override fun drawEars(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val headY = -12f + breath
        val earHop = if (motion == UniversalPetAction.WALK || motion == UniversalPetAction.RUN) sin(tick * 0.35).toFloat() * 6f else 0f

        // 左长耳
        canvas.save()
        canvas.rotate(earHop, -18f, headY - 18f)
        leftEarPath.reset()
        leftEarPath.moveTo(-18f, headY - 18f)
        leftEarPath.cubicTo(-28f, headY - 48f, -12f, headY - 56f, -6f, headY - 46f)
        leftEarPath.cubicTo(0f, headY - 36f, -8f, headY - 22f, -10f, headY - 18f)
        leftEarPath.close()
        canvas.drawPath(leftEarPath, paints.furPaint)
        canvas.drawPath(leftEarPath, paints.strokePaint)
        canvas.restore()

        // 右长耳
        canvas.save()
        canvas.rotate(-earHop, 18f, headY - 18f)
        rightEarPath.reset()
        rightEarPath.moveTo(10f, headY - 18f)
        rightEarPath.cubicTo(8f, headY - 22f, 0f, headY - 36f, 6f, headY - 46f)
        rightEarPath.cubicTo(12f, headY - 56f, 28f, headY - 48f, 18f, headY - 18f)
        rightEarPath.close()
        canvas.drawPath(rightEarPath, paints.furPaint)
        canvas.drawPath(rightEarPath, paints.strokePaint)
        canvas.restore()
    }

    // ================= 2. 头部轮廓绘制 =================
    override fun drawHead(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val headY = -10f + breath
        headPath.reset()
        headPath.moveTo(-26f, headY)
        headPath.cubicTo(-28f, headY - 22f, 28f, headY - 22f, 26f, headY)
        headPath.cubicTo(32f, headY + 22f, -32f, headY + 22f, -26f, headY)
        headPath.close()
        canvas.drawPath(headPath, paints.furPaint)
        canvas.drawPath(headPath, paints.strokePaint)
    }

    // ================= 3. 五官细节绘制 (兔兔Y形小萌嘴) =================
    override fun drawFace(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val faceY = -8f + breath

        // 水灵豆豆眼
        canvas.drawCircle(-12f, faceY, 3.2f, paints.eyePaint)
        canvas.drawCircle(-13.1f, faceY - 1.1f, 1.1f, paints.eyeGlintPaint)
        canvas.drawCircle(12f, faceY, 3.2f, paints.eyePaint)
        canvas.drawCircle(10.9f, faceY - 1.1f, 1.1f, paints.eyeGlintPaint)

        // 软萌粉腮红
        canvas.drawCircle(-20f, faceY + 4f, 4f, paints.blushPaint)
        canvas.drawCircle(20f, faceY + 4f, 4f, paints.blushPaint)

        // 粉红微三角鼻
        canvas.drawCircle(0f, faceY + 2.8f, 2.0f, paints.tonguePaint)

        // 经典 Y 形三瓣兔嘴
        canvas.drawLine(0f, faceY + 4.8f, 0f, faceY + 7.5f, paints.fineStrokePaint)
        canvas.drawLine(0f, faceY + 7.5f, -3.2f, faceY + 9.5f, paints.fineStrokePaint)
        canvas.drawLine(0f, faceY + 7.5f, 3.2f, faceY + 9.5f, paints.fineStrokePaint)
    }

    // ================= 4. 身体躯干绘制 =================
    override fun drawBody(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val bodyY = 14f + breath
        bodyPath.reset()
        bodyPath.moveTo(-24f, bodyY - 8f)
        bodyPath.cubicTo(-34f, bodyY + 16f, -22f, bodyY + 28f, -12f, bodyY + 28f)
        bodyPath.lineTo(12f, bodyY + 28f)
        bodyPath.cubicTo(22f, bodyY + 28f, 34f, bodyY + 16f, 24f, bodyY - 8f)
        bodyPath.close()

        canvas.drawPath(bodyPath, paints.furPaint)
        canvas.drawPath(bodyPath, paints.strokePaint)
    }

    // ================= 5. 四肢爪子绘制 =================
    override fun drawLimbs(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val bodyY = 14f + breath
        val isWalking = motion == UniversalPetAction.WALK
        val stride = if (isWalking) sin(tick * 0.32).toFloat() * 3.8f else 0f

        // 兔兔大后脚爪 (比小狗稍显修长)
        canvas.drawOval(-22f - stride * 0.4f, bodyY + 24f + stride, -12f - stride * 0.4f, bodyY + 31f + stride, paints.furPaint)
        canvas.drawOval(-22f - stride * 0.4f, bodyY + 24f + stride, -12f - stride * 0.4f, bodyY + 31f + stride, paints.strokePaint)

        canvas.drawOval(12f + stride * 0.4f, bodyY + 24f - stride, 22f + stride * 0.4f, bodyY + 31f - stride, paints.furPaint)
        canvas.drawOval(12f + stride * 0.4f, bodyY + 24f - stride, 22f + stride * 0.4f, bodyY + 31f - stride, paints.strokePaint)
    }

    // ================= 6. 尾巴绘制 (圆圆小毛球尾) =================
    override fun drawTail(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val tailX = 26f
        val tailY = 22f + breath
        canvas.drawCircle(tailX, tailY, 7.5f, paints.furPaint)
        canvas.drawCircle(tailX, tailY, 7.5f, paints.strokePaint)
    }
}
