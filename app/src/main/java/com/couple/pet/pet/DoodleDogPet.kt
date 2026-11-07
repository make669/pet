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
        val stage = com.couple.pet.network.PetSyncEngine.petStats.value.stage
        val isFoodie = stage == com.couple.pet.model.PetGrowthStage.AWAKEN_FOODIE
        val widthExtra = if (isFoodie) 8f else 0f
        val bodyY = 14f + breath
        bodyPath.reset()
        bodyPath.moveTo(-26f - widthExtra, bodyY - 8f)
        bodyPath.cubicTo(-36f - widthExtra * 1.2f, bodyY + 18f, -24f - widthExtra, bodyY + 27f + (if (isFoodie) 4f else 0f), -12f, bodyY + 27f + (if (isFoodie) 4f else 0f))
        bodyPath.lineTo(12f, bodyY + 27f + (if (isFoodie) 4f else 0f))
        bodyPath.cubicTo(24f + widthExtra, bodyY + 27f + (if (isFoodie) 4f else 0f), 36f + widthExtra * 1.2f, bodyY + 18f, 26f + widthExtra, bodyY - 8f)
        bodyPath.close()

        canvas.drawPath(bodyPath, paints.furPaint)
        canvas.drawPath(bodyPath, paints.strokePaint)

        // 蓬蓬体额外毛流波纹
        if (stage == com.couple.pet.model.PetGrowthStage.AWAKEN_FLUFFY) {
            canvas.drawArc(-36f, bodyY + 2f, -22f, bodyY + 16f, 120f, 140f, false, paints.fineStrokePaint)
            canvas.drawArc(22f, bodyY + 2f, 36f, bodyY + 16f, 280f, 140f, false, paints.fineStrokePaint)
        }
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

    // ================= 7. 多形态生命特征装扮与配饰 =================
    override fun drawAccessories(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        val stage = com.couple.pet.network.PetSyncEngine.petStats.value.stage
        val chestY = 14f + breath
        val headY = -10f + breath
        val isExcreting = motion in listOf(
            UniversalPetAction.POOP,
            UniversalPetAction.PEE,
            UniversalPetAction.PRANCE_URGENT,
            UniversalPetAction.BUSTED_FREEZE
        )

        when (stage) {
            com.couple.pet.model.PetGrowthStage.BABY -> {
                // 1. 奶糕幼崽期：婴儿小口水兜 + 萌萌小奶嘴
                drawBabyBibAndPacifier(canvas, headY + 2f, chestY, paints)
            }
            com.couple.pet.model.PetGrowthStage.CHILD -> {
                // 2. 好奇探索期：草莓小挎包 (粉红草莓 + 绿叶)
                drawStrawberryBag(canvas, chestY, paints)
            }
            com.couple.pet.model.PetGrowthStage.AWAKEN_FOODIE -> {
                // 3. 觉醒·食神圆滚体：两爪抱大香肉骨头
                drawFoodieBone(canvas, chestY, paints)
            }
            com.couple.pet.model.PetGrowthStage.AWAKEN_SPORTY -> {
                // 4. 觉醒·活力特技体：红色运动小领巾 (红领结随风微摆)
                drawSportyBandana(canvas, chestY - 4f, tick, paints)
            }
            com.couple.pet.model.PetGrowthStage.AWAKEN_FLUFFY -> {
                // 5. 觉醒·优雅蓬蓬体：头顶粉红小蝴蝶结
                drawFluffyBowknot(canvas, headY - 14f, paints)
            }
            com.couple.pet.model.PetGrowthStage.ADULT_GUARDIAN -> {
                // 6. 成熟·守护伴侣犬：闪耀情侣对戒项圈
                drawGuardianCollar(canvas, chestY - 2f, paints)
            }
            com.couple.pet.model.PetGrowthStage.MYTHIC_ANGEL -> {
                // 7. 神化·天使羁绊体：金色天使光环与纯白天使翅膀
                drawAngelWingsAndHalo(canvas, headY, chestY, tick, paints)
            }
        }

        // 伴学专注模式：戴黑框小眼镜与展开翻开的小书本
        if (com.couple.pet.network.PetSyncEngine.isPomodoroActive.value) {
            drawStudyGlassesAndBook(canvas, headY, chestY, paints)
        }

        // 非特殊排泄状态下保留默认抱小爱心 (若不是食神抱骨头或幼崽咬奶嘴)
        if (stage != com.couple.pet.model.PetGrowthStage.AWAKEN_FOODIE &&
            stage != com.couple.pet.model.PetGrowthStage.BABY &&
            !isExcreting) {
            super.drawAccessories(canvas, breath, motion, tick, paints)
        }
    }

    private fun drawStudyGlassesAndBook(canvas: Canvas, headY: Float, chestY: Float, paints: PetPaintKit) {
        // 1. 黑框小眼镜
        canvas.drawCircle(-8f, headY - 1f, 5.5f, paints.fineStrokePaint)
        canvas.drawCircle(8f, headY - 1f, 5.5f, paints.fineStrokePaint)
        canvas.drawLine(-2.5f, headY - 1f, 2.5f, headY - 1f, paints.fineStrokePaint)
        // 镜腿微斜
        canvas.drawLine(-13.5f, headY - 1f, -18f, headY - 3f, paints.fineStrokePaint)
        canvas.drawLine(13.5f, headY - 1f, 18f, headY - 3f, paints.fineStrokePaint)

        // 2. 胸前摊开的小书本 (两页手绘折纸)
        val bookY = chestY + 12f
        val bookPaint = android.graphics.Paint(paints.furPaint).apply { color = 0xFFFFFDF9.toInt() }
        // 左书页
        val leftPage = android.graphics.Path().apply {
            moveTo(0f, bookY + 6f)
            lineTo(-12f, bookY + 3f)
            lineTo(-12f, bookY - 6f)
            lineTo(0f, bookY - 3f)
            close()
        }
        canvas.drawPath(leftPage, bookPaint)
        canvas.drawPath(leftPage, paints.fineStrokePaint)
        // 右书页
        val rightPage = android.graphics.Path().apply {
            moveTo(0f, bookY + 6f)
            lineTo(12f, bookY + 3f)
            lineTo(12f, bookY - 6f)
            lineTo(0f, bookY - 3f)
            close()
        }
        canvas.drawPath(rightPage, bookPaint)
        canvas.drawPath(rightPage, paints.fineStrokePaint)
        // 中缝书脊
        canvas.drawLine(0f, bookY - 4f, 0f, bookY + 7f, paints.fineStrokePaint)
    }

    private fun drawBabyBibAndPacifier(canvas: Canvas, faceY: Float, chestY: Float, paints: PetPaintKit) {
        // 小围兜
        val bibPaint = android.graphics.Paint(paints.furPaint).apply { color = 0xFFFFF9C4.toInt() }
        canvas.drawRoundRect(-12f, chestY, 12f, chestY + 14f, 6f, 6f, bibPaint)
        canvas.drawRoundRect(-12f, chestY, 12f, chestY + 14f, 6f, 6f, paints.fineStrokePaint)

        // 嘴部奶嘴手柄与小圆环
        val pacifierPaint = android.graphics.Paint(paints.furPaint).apply { color = 0xFF80DEEA.toInt() }
        canvas.drawCircle(0f, faceY + 7f, 4f, pacifierPaint)
        canvas.drawCircle(0f, faceY + 7f, 4f, paints.fineStrokePaint)
        canvas.drawCircle(0f, faceY + 12f, 3f, paints.fineStrokePaint)
    }

    private fun drawStrawberryBag(canvas: Canvas, chestY: Float, paints: PetPaintKit) {
        // 斜跨背带
        canvas.drawLine(-16f, chestY - 6f, 18f, chestY + 16f, paints.fineStrokePaint)

        // 草莓小包包
        val bagPaint = android.graphics.Paint(paints.furPaint).apply { color = 0xFFFF6B81.toInt() }
        val leafPaint = android.graphics.Paint(paints.furPaint).apply { color = 0xFF81C784.toInt() }
        val bagX = 18f
        val bagY = chestY + 16f
        canvas.drawOval(bagX - 6f, bagY - 6f, bagX + 6f, bagY + 7f, bagPaint)
        canvas.drawOval(bagX - 6f, bagY - 6f, bagX + 6f, bagY + 7f, paints.fineStrokePaint)
        // 绿叶
        canvas.drawCircle(bagX - 2f, bagY - 6f, 2.2f, leafPaint)
        canvas.drawCircle(bagX + 2f, bagY - 6f, 2.2f, leafPaint)
    }

    private fun drawFoodieBone(canvas: Canvas, chestY: Float, paints: PetPaintKit) {
        // 香喷喷特大骨头
        val bonePaint = android.graphics.Paint(paints.furPaint).apply { color = 0xFFFFF8E1.toInt() }
        val y = chestY + 8f
        // 骨头中间轴
        canvas.drawRoundRect(-14f, y - 3.5f, 14f, y + 3.5f, 2f, 2f, bonePaint)
        canvas.drawRoundRect(-14f, y - 3.5f, 14f, y + 3.5f, 2f, 2f, paints.fineStrokePaint)
        // 左端两关节
        canvas.drawCircle(-14f, y - 3.5f, 3.2f, bonePaint)
        canvas.drawCircle(-14f, y - 3.5f, 3.2f, paints.fineStrokePaint)
        canvas.drawCircle(-14f, y + 3.5f, 3.2f, bonePaint)
        canvas.drawCircle(-14f, y + 3.5f, 3.2f, paints.fineStrokePaint)
        // 右端两关节
        canvas.drawCircle(14f, y - 3.5f, 3.2f, bonePaint)
        canvas.drawCircle(14f, y - 3.5f, 3.2f, paints.fineStrokePaint)
        canvas.drawCircle(14f, y + 3.5f, 3.2f, bonePaint)
        canvas.drawCircle(14f, y + 3.5f, 3.2f, paints.fineStrokePaint)
    }

    private fun drawSportyBandana(canvas: Canvas, chestY: Float, tick: Long, paints: PetPaintKit) {
        val redPaint = android.graphics.Paint(paints.furPaint).apply { color = 0xFFE53935.toInt() }
        val p = android.graphics.Path().apply {
            moveTo(-14f, chestY)
            lineTo(14f, chestY)
            lineTo(0f, chestY + 11f)
            close()
        }
        canvas.drawPath(p, redPaint)
        canvas.drawPath(p, paints.fineStrokePaint)

        // 飘带
        val flutter = sin(tick * 0.4).toFloat() * 3.5f
        canvas.drawLine(0f, chestY + 11f, 6f + flutter, chestY + 16f, paints.strokePaint)
    }

    private fun drawFluffyBowknot(canvas: Canvas, headTopY: Float, paints: PetPaintKit) {
        val pinkPaint = android.graphics.Paint(paints.furPaint).apply { color = 0xFFFF4081.toInt() }
        val bx = 0f
        val by = headTopY - 2f
        // 蝴蝶结左翼
        val leftWing = android.graphics.Path().apply {
            moveTo(bx, by)
            lineTo(bx - 8f, by - 5f)
            lineTo(bx - 8f, by + 5f)
            close()
        }
        canvas.drawPath(leftWing, pinkPaint)
        canvas.drawPath(leftWing, paints.fineStrokePaint)
        // 蝴蝶结右翼
        val rightWing = android.graphics.Path().apply {
            moveTo(bx, by)
            lineTo(bx + 8f, by - 5f)
            lineTo(bx + 8f, by + 5f)
            close()
        }
        canvas.drawPath(rightWing, pinkPaint)
        canvas.drawPath(rightWing, paints.fineStrokePaint)
        // 中心圆结
        canvas.drawCircle(bx, by, 3f, pinkPaint)
        canvas.drawCircle(bx, by, 3f, paints.fineStrokePaint)
    }

    private fun drawGuardianCollar(canvas: Canvas, chestY: Float, paints: PetPaintKit) {
        // 项圈
        val collarPaint = android.graphics.Paint(paints.furPaint).apply { color = 0xFF5D4037.toInt() }
        val goldPaint = android.graphics.Paint(paints.furPaint).apply { color = 0xFFFFD700.toInt() }
        canvas.drawRoundRect(-16f, chestY - 2f, 16f, chestY + 3f, 2f, 2f, collarPaint)
        canvas.drawRoundRect(-16f, chestY - 2f, 16f, chestY + 3f, 2f, 2f, paints.fineStrokePaint)

        // 挂坠情侣对戒
        canvas.drawCircle(0f, chestY + 6f, 3.8f, goldPaint)
        canvas.drawCircle(0f, chestY + 6f, 3.8f, paints.fineStrokePaint)
        canvas.drawCircle(0f, chestY + 6f, 1.8f, paints.furPaint)
    }

    private fun drawAngelWingsAndHalo(canvas: Canvas, headY: Float, chestY: Float, tick: Long, paints: PetPaintKit) {
        // 1. 金色天使光环
        val goldStroke = android.graphics.Paint(paints.strokePaint).apply { color = 0xFFFFC107.toInt(); strokeWidth = 3f }
        val haloFloat = sin(tick * 0.15).toFloat() * 2f
        canvas.drawOval(-14f, headY - 24f + haloFloat, 14f, headY - 18f + haloFloat, goldStroke)

        // 2. 纯白小天使羽翼
        val wingFlap = sin(tick * 0.3).toFloat() * 6f
        canvas.save()
        canvas.rotate(-wingFlap, -24f, chestY)
        val leftWing = android.graphics.Path().apply {
            moveTo(-22f, chestY - 2f)
            cubicTo(-46f, chestY - 18f, -44f, chestY + 12f, -22f, chestY + 6f)
            close()
        }
        canvas.drawPath(leftWing, paints.furPaint)
        canvas.drawPath(leftWing, paints.strokePaint)
        canvas.restore()

        canvas.save()
        canvas.rotate(wingFlap, 24f, chestY)
        val rightWing = android.graphics.Path().apply {
            moveTo(22f, chestY - 2f)
            cubicTo(46f, chestY - 18f, 44f, chestY + 12f, 22f, chestY + 6f)
            close()
        }
        canvas.drawPath(rightWing, paints.furPaint)
        canvas.drawPath(rightWing, paints.strokePaint)
        canvas.restore()
    }

    // ================= 8. 专属绝活：追尾巴原地转圈 =================
    override fun drawSpecialTrickPose(canvas: Canvas, breath: Float, tick: Long, paints: PetPaintKit) {
        val spinAngle = (tick * 14f) % 360f
        canvas.save()
        canvas.rotate(spinAngle, 0f, 0f)
        super.drawStandardPipeline(canvas, breath, UniversalPetAction.SPECIAL_TRICK, tick, paints)
        canvas.restore()
    }
}
