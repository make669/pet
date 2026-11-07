package com.couple.pet.pet

import android.graphics.Canvas
import kotlin.math.sin

/**
 * 原版线条小狗之守护伴侣：小金毛 / 小黄狗 (GoldenPuppyPet)
 * 经典燕麦暖棕色皮毛 (#D5C2A5)、大水滴垂耳、憨憨三瓣嘴角、以及标志性珊瑚暖粉细项圈 (#FF7663)。
 */
class GoldenPuppyPet : BasePet(
    speciesId = "golden_puppy",
    speciesName = "小金毛",
    speciesEmoji = "🦮",
    defaultNick = "小黄",
    description = "温厚宽容、忠诚守护的燕麦曲奇小金毛",
    favoriteFood = "肉骨头"
) {

    override fun getGreeting(): String = "汪呜！我会一直守在你和小狗身旁的 🐾"

    override fun getTouchReaction(): String = "憨憨地把大脑袋靠在你膝盖上，蹭得呼噜呼噜响 ❤️"

    override fun getSpecialTrickName(): String = "大爪击掌与暖心靠靠"

    // ================= 1. 耳朵绘制 (水滴状宽大垂耳) =================
    override fun drawEars(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val headY = -9f + breath
        val isWalking = motion == UniversalPetAction.WALK
        val isRunning = motion == UniversalPetAction.RUN

        val earSwing = when {
            isRunning -> sin(tick * 0.45).toFloat() * 10.0f
            isWalking -> sin(tick * 0.28).toFloat() * 4.5f
            else -> 0f
        }

        // 左大耳：从头顶侧面自然垂坠至下颌
        canvas.save()
        canvas.rotate(earSwing, -26f, headY - 12f)
        leftEarPath.reset()
        leftEarPath.moveTo(-24f, headY - 14f)
        leftEarPath.cubicTo(-48f, headY - 12f, -48f, headY + 24f, -22f, headY + 22f)
        leftEarPath.close()
        canvas.drawPath(leftEarPath, paints.goldenFurPaint)
        canvas.drawPath(leftEarPath, paints.strokePaint)
        canvas.restore()

        // 右大耳 (相位相反)
        canvas.save()
        canvas.rotate(-earSwing, 26f, headY - 12f)
        rightEarPath.reset()
        rightEarPath.moveTo(24f, headY - 14f)
        rightEarPath.cubicTo(48f, headY - 12f, 48f, headY + 24f, 22f, headY + 22f)
        rightEarPath.close()
        canvas.drawPath(rightEarPath, paints.goldenFurPaint)
        canvas.drawPath(rightEarPath, paints.strokePaint)
        canvas.restore()
    }

    // ================= 2. 头部轮廓绘制 (大弧度平滑圆润曲奇体) =================
    override fun drawHead(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val headY = -9f + breath
        headPath.reset()
        // 头部主轮廓：一次饱满的大弧，腮帮子微鼓
        headPath.moveTo(-29f, headY)
        headPath.cubicTo(-31f, headY - 30f, 31f, headY - 30f, 29f, headY)
        headPath.cubicTo(33f, headY + 22f, -33f, headY + 22f, -29f, headY)
        headPath.close()

        canvas.drawPath(headPath, paints.goldenFurPaint)
        canvas.drawPath(headPath, paints.strokePaint)
    }

    // ================= 3. 五官绘制 (经典黑豆圆眼 + 倒 w 微笑嘴) =================
    override fun drawFace(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val headY = -9f + breath
        val isHappy = motion == UniversalPetAction.HAPPY_JUMP
        val isSleeping = motion == UniversalPetAction.SLEEP

        if (isSleeping) {
            // 熟睡闭合弧线
            canvas.drawLine(-16f, headY - 2f, -6f, headY - 2f, paints.fineStrokePaint)
            canvas.drawLine(6f, headY - 2f, 16f, headY - 2f, paints.fineStrokePaint)
        } else if (isHappy) {
            // 开心笑眯眯弯眼 ^ ^
            canvas.drawArc(-17f, headY - 8f, -7f, headY + 2f, 180f, 180f, false, paints.fineStrokePaint)
            canvas.drawArc(7f, headY - 8f, 17f, headY + 2f, 180f, 180f, false, paints.fineStrokePaint)
        } else {
            // 纯黑黑曜石圆眼
            canvas.drawCircle(-12f, headY - 2f, 3.8f, paints.eyePaint)
            canvas.drawCircle(12f, headY - 2f, 3.8f, paints.eyePaint)
            // 眼神高光小光点
            canvas.drawCircle(-13.2f, headY - 3.2f, 1.2f, paints.eyeGlintPaint)
            canvas.drawCircle(10.8f, headY - 3.2f, 1.2f, paints.eyeGlintPaint)
        }

        // 倒三角圆润小鼻头
        canvas.drawCircle(0f, headY + 4f, 3.2f, paints.eyePaint)

        // 倒 "w" 憨憨三瓣唇微笑嘴
        propPath.reset()
        propPath.moveTo(-8f, headY + 9f)
        propPath.quadTo(-4f, headY + 14f, 0f, headY + 10f)
        propPath.quadTo(4f, headY + 14f, 8f, headY + 9f)
        canvas.drawPath(propPath, paints.fineStrokePaint)

        // 腮红 (温暖红晕)
        canvas.drawCircle(-21f, headY + 5f, 4.2f, paints.blushPaint)
        canvas.drawCircle(21f, headY + 5f, 4.2f, paints.blushPaint)
    }

    // ================= 4. 躯干与小爪绘制 =================
    override fun drawBody(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val chestY = 16f + breath
        bodyPath.reset()
        // 金毛微胖厚实身躯
        bodyPath.moveTo(-22f, chestY - 14f)
        bodyPath.cubicTo(-38f, chestY + 18f, 38f, chestY + 18f, 22f, chestY - 14f)
        bodyPath.close()

        canvas.drawPath(bodyPath, paints.goldenFurPaint)
        canvas.drawPath(bodyPath, paints.strokePaint)

        // 敦实的小前爪
        canvas.drawRoundRect(-18f, chestY + 12f, -4f, chestY + 26f, 6f, 6f, paints.goldenFurPaint)
        canvas.drawRoundRect(-18f, chestY + 12f, -4f, chestY + 26f, 6f, 6f, paints.strokePaint)

        canvas.drawRoundRect(4f, chestY + 12f, 18f, chestY + 26f, 6f, 6f, paints.goldenFurPaint)
        canvas.drawRoundRect(4f, chestY + 12f, 18f, chestY + 26f, 6f, 6f, paints.strokePaint)
    }

    // ================= 4.1 四肢腿脚绘制 =================
    override fun drawLimbs(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val bodyY = 16f + breath
        val isWalking = motion == UniversalPetAction.WALK
        val isRunning = motion == UniversalPetAction.RUN

        val stride = when {
            isRunning -> sin(tick * 0.45).toFloat() * 7.5f
            isWalking -> sin(tick * 0.28).toFloat() * 3.5f
            else -> 0f
        }

        // 金毛憨厚敦实的左右小脚爪
        canvas.drawCircle(-16f - stride * 0.4f, bodyY + 24f + stride, 5.2f, paints.goldenFurPaint)
        canvas.drawCircle(-16f - stride * 0.4f, bodyY + 24f + stride, 5.2f, paints.strokePaint)

        canvas.drawCircle(16f + stride * 0.4f, bodyY + 24f - stride, 5.2f, paints.goldenFurPaint)
        canvas.drawCircle(16f + stride * 0.4f, bodyY + 24f - stride, 5.2f, paints.strokePaint)
    }

    // ================= 5. 尾巴绘制 (蓬松麦穗大尾) =================
    override fun drawTail(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val tailY = 18f + breath
        val isWalking = motion == UniversalPetAction.WALK
        val isRunning = motion == UniversalPetAction.RUN
        val isHappy = motion == UniversalPetAction.HAPPY_JUMP

        val tailAngle = when {
            isHappy || isRunning -> sin(tick * 0.55).toFloat() * 26.0f
            isWalking -> sin(tick * 0.35).toFloat() * 14.0f
            else -> sin(tick * 0.15).toFloat() * 6.0f
        }

        canvas.save()
        canvas.rotate(tailAngle, 22f, tailY)
        tailPath.reset()
        tailPath.moveTo(22f, tailY)
        tailPath.cubicTo(38f, tailY - 14f, 48f, tailY + 8f, 30f, tailY + 18f)
        tailPath.close()

        canvas.drawPath(tailPath, paints.goldenFurPaint)
        canvas.drawPath(tailPath, paints.strokePaint)
        canvas.restore()
    }

    // ================= 6. 标志性配件：珊瑚粉细项圈 (#FF7663) =================
    override fun drawAccessories(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val chestY = 16f + breath

        // 珊瑚粉红细项圈紧贴颈圈前弧
        propPath.reset()
        propPath.moveTo(-18f, chestY - 10f)
        propPath.quadTo(0f, chestY - 4f, 18f, chestY - 10f)
        canvas.drawPath(propPath, paints.pinkCollarPaint)

        // 伴学专注模式：佩戴小夜灯发箍与书本
        if (com.couple.pet.network.PetSyncEngine.isPomodoroActive.value) {
            val headY = -9f + breath
            canvas.drawCircle(-12f, headY - 2f, 5.5f, paints.fineStrokePaint)
            canvas.drawCircle(12f, headY - 2f, 5.5f, paints.fineStrokePaint)
            canvas.drawLine(-6.5f, headY - 2f, 6.5f, headY - 2f, paints.fineStrokePaint)
        }
    }
}
