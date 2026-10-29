package com.couple.pet.pet

import android.graphics.Canvas
import kotlin.math.sin

/**
 * 经典线条小狗 (DoodleDogPet) 继承自通用宠物基类 [BasePet]
 * 100% 保持经典的极简手绘黑白线稿、萌萌粉腮红、摇尾巴与抱爱心视觉。
 */
class DoodleDogPet : BasePet(
    speciesId = "doodle_dog",
    speciesName = "线条小狗",
    speciesEmoji = "🐶",
    defaultNick = "小白",
    description = "元气满满、忠诚黏人的黑白手绘治愈系小狗",
    favoriteFood = "小鱼干"
) {

    override fun getGreeting(): String = "汪汪！今天也想陪在你的屏幕上 🐾"

    override fun getTouchReaction(): String = "开心地摇着小尾巴，小脑袋直往你手心蹭 ❤️"

    override fun getSpecialTrickName(): String = "追尾巴原地打转"

    // ================= 1. 耳朵绘制 (软塌塌小耷耳) =================
    override fun drawEars(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val headY = -10f + breath
        val isWalking = motion == UniversalPetAction.WALK
        val isRunning = motion == UniversalPetAction.RUN

        // 左耳微晃
        val earSwing = when {
            isRunning -> sin(tick * 0.45).toFloat() * 12.0f
            isWalking -> sin(tick * 0.28).toFloat() * 5.0f
            else -> 0f
        }

        canvas.save()
        canvas.rotate(earSwing, -24f, headY - 14f)
        leftEarPath.reset()
        leftEarPath.moveTo(-24f, headY - 14f)
        leftEarPath.cubicTo(-45f, headY - 14f, -44f, headY + 16f, -25f, headY + 12f)
        leftEarPath.close()
        canvas.drawPath(leftEarPath, paints.furPaint)
        canvas.drawPath(leftEarPath, paints.strokePaint)
        canvas.restore()

        // 右耳微晃 (相位相反)
        canvas.save()
        canvas.rotate(-earSwing, 24f, headY - 14f)
        rightEarPath.reset()
        rightEarPath.moveTo(24f, headY - 14f)
        rightEarPath.cubicTo(45f, headY - 14f, 44f, headY + 16f, 25f, headY + 12f)
        rightEarPath.close()
        canvas.drawPath(rightEarPath, paints.furPaint)
        canvas.drawPath(rightEarPath, paints.strokePaint)
        canvas.restore()
    }

    // ================= 2. 头部轮廓绘制 =================
    override fun drawHead(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val headY = -10f + breath
        headPath.reset()
        headPath.moveTo(-28f, headY)
        headPath.cubicTo(-30f, headY - 26f, 30f, headY - 26f, 28f, headY)
        headPath.cubicTo(34f, headY + 22f, -34f, headY + 22f, -28f, headY)
        headPath.close()
        canvas.drawPath(headPath, paints.furPaint)
        canvas.drawPath(headPath, paints.strokePaint)
    }

    // ================= 3. 五官细节绘制 =================
    override fun drawFace(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val faceY = -8f + breath

        when (motion) {
            UniversalPetAction.POOP -> {
                // 拱背排便：眼神锁定对视主人 (专注微大豆豆眼 + 高光求保护)
                canvas.drawCircle(-13f, faceY, 3.8f, paints.eyePaint)
                canvas.drawCircle(-14.2f, faceY - 1.2f, 1.3f, paints.eyeGlintPaint)
                canvas.drawCircle(13f, faceY, 3.8f, paints.eyePaint)
                canvas.drawCircle(11.8f, faceY - 1.2f, 1.3f, paints.eyeGlintPaint)

                // 用力/害羞红腮
                canvas.drawCircle(-21f, faceY + 4f, 5f, paints.blushPaint)
                canvas.drawCircle(21f, faceY + 4f, 5f, paints.blushPaint)

                // 纽扣小黑鼻
                canvas.drawCircle(0f, faceY + 3.2f, 2.5f, paints.eyePaint)

                // 专注紧抿小嘴
                canvas.drawLine(-3.8f, faceY + 7.5f, 3.8f, faceY + 7.5f, paints.fineStrokePaint)
            }
            UniversalPetAction.BUSTED_FREEZE -> {
                // 抓现行瞬间石化：圆瞪大眼 O.O + 震惊小嘴
                canvas.drawCircle(-13f, faceY, 4.2f, paints.strokePaint)
                canvas.drawCircle(-13f, faceY, 1.8f, paints.eyePaint)
                canvas.drawCircle(13f, faceY, 4.2f, paints.strokePaint)
                canvas.drawCircle(13f, faceY, 1.8f, paints.eyePaint)

                canvas.drawCircle(-21f, faceY + 4f, 4.2f, paints.blushPaint)
                canvas.drawCircle(21f, faceY + 4f, 4.2f, paints.blushPaint)

                canvas.drawCircle(0f, faceY + 3.2f, 2.5f, paints.eyePaint)

                // 震惊微张小圆嘴
                canvas.drawCircle(0f, faceY + 8f, 2.2f, paints.fineStrokePaint)
            }
            UniversalPetAction.PRANCE_URGENT -> {
                // 憋急小碎步：慌张豆豆眼 + 波浪嘴 ~
                canvas.drawCircle(-13f, faceY, 3.2f, paints.eyePaint)
                canvas.drawCircle(13f, faceY, 3.2f, paints.eyePaint)

                canvas.drawCircle(-21f, faceY + 4f, 4.2f, paints.blushPaint)
                canvas.drawCircle(21f, faceY + 4f, 4.2f, paints.blushPaint)

                canvas.drawCircle(0f, faceY + 3.2f, 2.5f, paints.eyePaint)

                // 慌张波浪嘴
                canvas.drawLine(-4f, faceY + 8f, -1.5f, faceY + 6.5f, paints.fineStrokePaint)
                canvas.drawLine(-1.5f, faceY + 6.5f, 1.5f, faceY + 8.5f, paints.fineStrokePaint)
                canvas.drawLine(1.5f, faceY + 8.5f, 4f, faceY + 7f, paints.fineStrokePaint)
            }
            UniversalPetAction.POOP_ZOOMIES -> {
                // 拉后一身轻狂欢：弯弯笑眼 ^ ^ + 吐舌头大乐
                canvas.drawArc(-17f, faceY - 4f, -9f, faceY + 4f, 200f, 140f, false, paints.strokePaint)
                canvas.drawArc(9f, faceY - 4f, 17f, faceY + 4f, 200f, 140f, false, paints.strokePaint)

                canvas.drawCircle(-21f, faceY + 4f, 5f, paints.blushPaint)
                canvas.drawCircle(21f, faceY + 4f, 5f, paints.blushPaint)

                canvas.drawCircle(0f, faceY + 3.2f, 2.5f, paints.eyePaint)

                // 吐舌头开怀大笑
                canvas.drawOval(-4f, faceY + 5.5f, 4f, faceY + 13f, paints.tonguePaint)
                canvas.drawOval(-4f, faceY + 5.5f, 4f, faceY + 13f, paints.fineStrokePaint)
            }
            else -> {
                // 豆豆眼 (带高光闪烁)
                canvas.drawCircle(-13f, faceY, 3.2f, paints.eyePaint)
                canvas.drawCircle(-14.2f, faceY - 1.2f, 1.1f, paints.eyeGlintPaint)
                canvas.drawCircle(13f, faceY, 3.2f, paints.eyePaint)
                canvas.drawCircle(11.8f, faceY - 1.2f, 1.1f, paints.eyeGlintPaint)

                // 少女心粉嫩腮红
                canvas.drawCircle(-21f, faceY + 4f, 4.2f, paints.blushPaint)
                canvas.drawCircle(21f, faceY + 4f, 4.2f, paints.blushPaint)

                // 纽扣小黑鼻
                canvas.drawCircle(0f, faceY + 3.2f, 2.5f, paints.eyePaint)

                // 微笑嘴与舌头 (奔跑或进食时吐小舌头)
                if (motion == UniversalPetAction.RUN || motion == UniversalPetAction.EAT) {
                    canvas.drawOval(-4f, faceY + 5.5f, 4f, faceY + 12f, paints.tonguePaint)
                    canvas.drawOval(-4f, faceY + 5.5f, 4f, faceY + 12f, paints.fineStrokePaint)
                } else {
                    canvas.drawLine(-3.5f, faceY + 6.5f, 0f, faceY + 8f, paints.fineStrokePaint)
                    canvas.drawLine(0f, faceY + 8f, 3.5f, faceY + 6.5f, paints.fineStrokePaint)
                }
            }
        }
    }

    // ================= 4. 身体躯干绘制 =================
    override fun drawBody(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val bodyY = 14f + breath
        bodyPath.reset()
        bodyPath.moveTo(-26f, bodyY - 8f)
        bodyPath.cubicTo(-36f, bodyY + 18f, -24f, bodyY + 27f, -12f, bodyY + 27f)
        bodyPath.lineTo(12f, bodyY + 27f)
        bodyPath.cubicTo(24f, bodyY + 27f, 36f, bodyY + 18f, 26f, bodyY - 8f)
        bodyPath.close()

        canvas.drawPath(bodyPath, paints.furPaint)
        canvas.drawPath(bodyPath, paints.strokePaint)
    }

    // ================= 5. 四肢腿脚绘制 =================
    override fun drawLimbs(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val bodyY = 14f + breath
        val isWalking = motion == UniversalPetAction.WALK
        val isRunning = motion == UniversalPetAction.RUN

        val stride = when {
            isRunning -> sin(tick * 0.45).toFloat() * 7.5f
            isWalking -> sin(tick * 0.28).toFloat() * 3.5f
            else -> 0f
        }

        // 左前脚与右前脚
        canvas.drawCircle(-18f - stride * 0.4f, bodyY + 26f + stride, 4.6f, paints.furPaint)
        canvas.drawCircle(-18f - stride * 0.4f, bodyY + 26f + stride, 4.6f, paints.strokePaint)

        canvas.drawCircle(18f + stride * 0.4f, bodyY + 26f - stride, 4.6f, paints.furPaint)
        canvas.drawCircle(18f + stride * 0.4f, bodyY + 26f - stride, 4.6f, paints.strokePaint)
    }

    // ================= 6. 尾巴绘制 (欢快摇摆) =================
    override fun drawTail(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val tailBaseX = 26f
        val tailBaseY = 22f + breath
        val tailAngle = when (motion) {
            UniversalPetAction.POOP -> -35f // 排便时尾巴高高翘起避开地面
            UniversalPetAction.POOP_ZOOMIES, UniversalPetAction.RUN, UniversalPetAction.HAPPY_JUMP -> sin(tick * 0.5).toFloat() * 32f
            UniversalPetAction.BUSTED_FREEZE -> 0f // 石化定格
            else -> sin(tick * 0.22).toFloat() * 16f
        }

        canvas.save()
        canvas.rotate(tailAngle, tailBaseX, tailBaseY)
        tailPath.reset()
        tailPath.moveTo(tailBaseX, tailBaseY)
        tailPath.quadTo(tailBaseX + 16f, tailBaseY - 12f, tailBaseX + 10f, tailBaseY - 22f)
        tailPath.quadTo(tailBaseX + 5f, tailBaseY - 24f, tailBaseX + 3f, tailBaseY - 17f)
        tailPath.quadTo(tailBaseX + 7f, tailBaseY - 8f, tailBaseX, tailBaseY + 6f)
        tailPath.close()
        canvas.drawPath(tailPath, paints.furPaint)
        canvas.drawPath(tailPath, paints.strokePaint)
        canvas.restore()
    }

    // ================= 7. 专属绝活：追尾巴原地转圈 =================
    override fun drawSpecialTrickPose(canvas: Canvas, breath: Float, tick: Long, paints: PetPaintKit) {
        val spinAngle = (tick * 14f) % 360f
        canvas.save()
        canvas.rotate(spinAngle, 0f, 0f)
        super.drawStandardPipeline(canvas, breath, UniversalPetAction.SPECIAL_TRICK, tick, paints)
        canvas.restore()
    }
}
