package com.couple.pet.pet

import android.graphics.Canvas
import android.graphics.Path
import kotlin.math.sin
import kotlin.random.Random

/**
 * 宠物抽象基类 (BasePet)
 * 【设计模式：模板方法 (Template Method) + 状态机策略】
 *
 * 作为所有宠物的顶层父类，统一规范并实现了：
 * 1. 基础生物身份与物种标识 (物种ID、显示名、Emoji、物种描述、偏爱食物)；
 * 2. 状态机驱动与姿态节拍 (当前通用动作、朝向、帧计数 Tick、气泡系统)；
 * 3. 语言与情感反应接口 (触碰反馈、投喂反馈、入睡梦话、饥饿叫唤)；
 * 4. 原生 Canvas 渲染管线 (呼吸起伏、步态计算、镜像翻转与身体分层解耦绘制)。
 */
abstract class BasePet(
    val speciesId: String,
    val speciesName: String,
    val speciesEmoji: String,
    val defaultNick: String,
    val description: String,
    val favoriteFood: String = "小鱼干"
) {

    // 动作与物理朝向
    var currentMotion: UniversalPetAction = UniversalPetAction.IDLE
    var motionTick: Long = 0L
    var facingRight: Boolean = true
    var speechBubble: String? = null

    // 绘制复用 Path (避免在 draw 中频繁 new)
    protected val headPath = Path()
    protected val leftEarPath = Path()
    protected val rightEarPath = Path()
    protected val bodyPath = Path()
    protected val tailPath = Path()
    protected val propPath = Path()

    // ================= 1. 情感与交互反应体系 =================

    /**
     * 伴侣唤醒/初次登场问候语
     */
    open fun getGreeting(): String = "你好呀！我是$speciesName 🐾"

    /**
     * 投喂指定美食的满意反馈
     */
    open fun getEatReaction(foodName: String): String {
        return if (foodName.contains(favoriteFood)) {
            "哇！最喜欢的【$foodName】！超满足~ 💕"
        } else {
            "大口吃掉了【$foodName】~ 吧唧吧唧 😋"
        }
    }

    /**
     * 被主人指尖抚摸触碰的喜悦反馈
     */
    open fun getTouchReaction(): String = "蹭了蹭你的手心，发出舒服的呼噜声 ❤️"

    /**
     * 入睡时的可爱梦话
     */
    open fun getSleepBubble(): String = "呼噜噜... 💤"

    /**
     * 肚子饿时的撒娇提示
     */
    open fun getHungryBubble(): String = "肚肚好扁... 想吃$favoriteFood 🥺"

    /**
     * 获取物种专属绝活动作名称
     */
    abstract fun getSpecialTrickName(): String

    // ================= 2. 行为自主 AI 决策引擎 =================

    /**
     * 根据生理需求与精力自动决策下一个动作
     * 子类可按物种特性调整概率权重 (如猫咪偏爱晒太阳打瞌睡，小狗偏爱快跑与追尾巴)
     */
    open fun decideNextMotion(hunger: Int, mood: Int, fatigue: Int): UniversalPetAction {
        // 极度疲劳强制入睡
        if (fatigue > 85) return UniversalPetAction.SLEEP

        // 肚子饿求喂食
        if (hunger < 30 && Random.nextInt(100) < 60) return UniversalPetAction.BEG

        // 心情极好随机快跑或专属绝活
        if (mood > 80 && Random.nextInt(100) < 35) {
            return if (Random.nextBoolean()) UniversalPetAction.RUN else UniversalPetAction.SPECIAL_TRICK
        }

        // 默认自然行为轮换
        return when (Random.nextInt(100)) {
            in 0..45 -> UniversalPetAction.WALK
            in 46..65 -> UniversalPetAction.IDLE
            in 66..75 -> UniversalPetAction.STRETCH
            in 76..85 -> UniversalPetAction.YAWN
            in 86..92 -> UniversalPetAction.REST
            else -> UniversalPetAction.SHAKE
        }
    }

    // ================= 3. 原生 Canvas 渲染总管线 (模板方法) =================

    /**
     * 核心渲染入口：由 PolaroidPuppyView 或 PetFloatingView 在 onDraw 中统一调用
     */
    open fun render(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        scale: Float = 1.0f,
        motion: UniversalPetAction = currentMotion,
        tick: Long = motionTick,
        facingRight: Boolean = this.facingRight,
        paints: PetPaintKit = PetPaintKit.defaultKit
    ) {
        val isWalking = motion == UniversalPetAction.WALK
        val isRunning = motion == UniversalPetAction.RUN
        val isSleeping = motion == UniversalPetAction.SLEEP

        // 呼吸与步态基础物理起伏
        val breath = when {
            isRunning -> sin(tick * 0.45).toFloat() * 4.0f
            isWalking -> sin(tick * 0.28).toFloat() * 2.4f
            isSleeping -> sin(tick * 0.08).toFloat() * 1.2f
            else -> sin(tick * 0.14).toFloat() * 1.6f
        }

        canvas.save()

        // 1. 全局缩放与水平镜像翻转 (朝左时水平翻转 -1)
        canvas.translate(cx, cy)
        canvas.scale(if (facingRight) scale else -scale, scale)

        // 2. 特殊姿态分支派发
        when (motion) {
            UniversalPetAction.SLEEP -> {
                drawSleepingPose(canvas, breath, tick, paints)
            }
            UniversalPetAction.BELLY_UP -> {
                drawBellyUpPose(canvas, breath, tick, paints)
            }
            UniversalPetAction.STRETCH -> {
                drawStretchingPose(canvas, breath, tick, paints)
            }
            UniversalPetAction.SPECIAL_TRICK -> {
                drawSpecialTrickPose(canvas, breath, tick, paints)
            }
            else -> {
                // 标准站姿/走跑流水线
                drawStandardPipeline(canvas, breath, motion, tick, paints)
            }
        }

        canvas.restore()
    }

    /**
     * 标准姿态层叠绘制管线：尾巴 -> 身体与四肢 -> 脑袋与耳朵 -> 五官细节 -> 装饰/食物
     */
    protected open fun drawStandardPipeline(
        canvas: Canvas,
        breath: Float,
        motion: UniversalPetAction,
        tick: Long,
        paints: PetPaintKit
    ) {
        // 1. 绘制尾巴 (最底层)
        drawTail(canvas, breath, motion, tick, paints)

        // 2. 绘制身体躯干
        drawBody(canvas, breath, motion, tick, paints)

        // 3. 绘制四肢与脚爪
        drawLimbs(canvas, breath, motion, tick, paints)

        // 4. 绘制双耳
        drawEars(canvas, breath, motion, tick, paints)

        // 5. 绘制头部轮廓
        drawHead(canvas, breath, motion, tick, paints)

        // 6. 绘制眼睛、嘴巴、鼻头与红腮帮
        drawFace(canvas, breath, motion, tick, paints)

        // 7. 绘制随身道具或手捧爱心
        drawAccessories(canvas, breath, motion, tick, paints)
    }

    // ================= 4. 子类必须实现的各物种形体绘制接口 =================

    /**
     * 绘制耳朵 (如：狗的垂耳、猫的尖耳、兔的长耳)
     */
    protected abstract fun drawEars(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit)

    /**
     * 绘制头部轮廓
     */
    protected abstract fun drawHead(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit)

    /**
     * 绘制五官微表情
     */
    protected abstract fun drawFace(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit)

    /**
     * 绘制躯干
     */
    protected abstract fun drawBody(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit)

    /**
     * 绘制腿部与小爪子
     */
    protected abstract fun drawLimbs(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit)

    /**
     * 绘制尾巴 (如：狗的摇摆尾、猫的长卷尾、兔的毛球尾)
     */
    protected abstract fun drawTail(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit)

    // ================= 5. 子类可按物种特性重写的特殊姿态 =================

    /**
     * 绘制配饰或前爪抱物 (默认绘制粉红爱心)
     */
    protected open fun drawAccessories(canvas: Canvas, breath: Float, motion: UniversalPetAction, tick: Long, paints: PetPaintKit) {
        // 默认两只小前爪合抱在胸前的小爱心
        val heartY = 8f + breath
        propPath.reset()
        propPath.moveTo(0f, heartY + 12f)
        propPath.cubicTo(-14f, heartY, -14f, heartY - 12f, 0f, heartY - 5f)
        propPath.cubicTo(14f, heartY - 12f, 14f, heartY, 0f, heartY + 12f)
        propPath.close()
        canvas.drawPath(propPath, paints.heartPaint)
    }

    /**
     * 蜷缩睡觉姿态 (默认通用的毛球小包裹睡觉法)
     */
    protected open fun drawSleepingPose(canvas: Canvas, breath: Float, tick: Long, paints: PetPaintKit) {
        // 团成圆滚滚毛球
        bodyPath.reset()
        bodyPath.addOval(-28f, -12f + breath, 28f, 22f + breath, Path.Direction.CW)
        canvas.drawPath(bodyPath, paints.furPaint)
        canvas.drawPath(bodyPath, paints.strokePaint)

        // 闭眼睫毛
        canvas.drawLine(-14f, 4f + breath, -6f, 4f + breath, paints.fineStrokePaint)
        canvas.drawLine(6f, 4f + breath, 14f, 4f + breath, paints.fineStrokePaint)

        // 腮红
        canvas.drawCircle(-16f, 8f + breath, 4f, paints.blushPaint)
        canvas.drawCircle(16f, 8f + breath, 4f, paints.blushPaint)
    }

    /**
     * 翻肚皮求摸姿态
     */
    protected open fun drawBellyUpPose(canvas: Canvas, breath: Float, tick: Long, paints: PetPaintKit) {
        drawStandardPipeline(canvas, breath, UniversalPetAction.BELLY_UP, tick, paints)
    }

    /**
     * 伸懒腰姿态
     */
    protected open fun drawStretchingPose(canvas: Canvas, breath: Float, tick: Long, paints: PetPaintKit) {
        drawStandardPipeline(canvas, breath, UniversalPetAction.STRETCH, tick, paints)
    }

    /**
     * 物种专属绝活姿态
     */
    protected open fun drawSpecialTrickPose(canvas: Canvas, breath: Float, tick: Long, paints: PetPaintKit) {
        drawStandardPipeline(canvas, breath, UniversalPetAction.SPECIAL_TRICK, tick, paints)
    }
}
