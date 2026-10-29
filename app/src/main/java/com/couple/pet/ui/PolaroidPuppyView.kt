package com.couple.pet.ui

import android.graphics.Paint
import android.graphics.Path
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.ui.theme.PetColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * 相框内小狗自主游荡行为状态
 */
enum class FramePuppyBehavior {
    WALKING,     // 漫步游荡
    SNIFFING,    // 低头嗅闻
    TILTING,     // 好奇歪头
    RESTING,     // 舒坦就地趴坐
    HAPPY_JUMP   // 受到抚摸兴奋欢跃
}

/**
 * 【拍立得相框相纸视窗】：
 * 100% 贯彻“同一个线条小狗（The Exact Same Living Puppy）”：
 * - 无论是相框内还是桌面上，都是同一只黑线白底、萌萌粉腮红、胸前抱小爱心的真正「线条小狗」；
 * - 绝不使用系统自带的黄色卡通狗 Emoji；
 * - 当小狗在相框中时：自主迈步漫游、耳朵微晃、小碎步踱步、尾巴摇摆；
 * - 当小狗放桌面漫步时：相框呈现手绘线稿风的温馨留守态（空垫窝与小爪印）。
 */
@Composable
fun PolaroidPuppyViewport(
    isPetFloating: Boolean,
    onPetTouched: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 游荡坐标与状态
    var currentX by remember { mutableFloatStateOf(0f) }
    var targetX by remember { mutableFloatStateOf(0f) }
    var facingRight by remember { mutableStateOf(true) }
    var behavior by remember { mutableStateOf(FramePuppyBehavior.WALKING) }
    var tick by remember { mutableLongStateOf(0L) }
    var speechBubble by remember { mutableStateOf<String?>(null) }
    var showHeartEffect by remember { mutableStateOf(false) }

    // 触控回弹动画
    val jumpScale by animateFloatAsState(
        targetValue = if (behavior == FramePuppyBehavior.HAPPY_JUMP) 1.22f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "jumpScale"
    )

    // 主动画时钟与自主游荡 AI 循环 (只在相框内时运行)
    LaunchedEffect(isPetFloating) {
        if (!isPetFloating) {
            // 从桌面回相框时的欢欣归巢动效
            behavior = FramePuppyBehavior.HAPPY_JUMP
            speechBubble = "小狗回家啦 🐾"
            showHeartEffect = true
            delay(1200)
            showHeartEffect = false
            speechBubble = null
            behavior = FramePuppyBehavior.WALKING

            while (isActive) {
                // 1. 决定下一个游荡目标点与行为
                val nextBehavior = when (Random.nextInt(100)) {
                    in 0..55 -> FramePuppyBehavior.WALKING
                    in 56..72 -> FramePuppyBehavior.SNIFFING
                    in 73..86 -> FramePuppyBehavior.TILTING
                    else -> FramePuppyBehavior.RESTING
                }

                behavior = nextBehavior

                when (nextBehavior) {
                    FramePuppyBehavior.WALKING -> {
                        // 在 [-80dp, 80dp] 走廊区间内随机寻点
                        targetX = Random.nextFloat() * 160f - 80f
                        facingRight = targetX >= currentX
                        speechBubble = null

                        // 慢步行走直至到达目标
                        while (abs(targetX - currentX) > 2f && behavior == FramePuppyBehavior.WALKING) {
                            val step = if (facingRight) 2.0f else -2.0f
                            currentX += step
                            tick++
                            delay(40)
                        }
                    }
                    FramePuppyBehavior.SNIFFING -> {
                        speechBubble = "💭 嗅嗅..."
                        val sniffStartTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - sniffStartTime < 2200) {
                            tick++
                            delay(40)
                        }
                        speechBubble = null
                    }
                    FramePuppyBehavior.TILTING -> {
                        speechBubble = "💭 歪头看你"
                        val tiltStartTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - tiltStartTime < 2000) {
                            tick++
                            delay(40)
                        }
                        speechBubble = null
                    }
                    FramePuppyBehavior.RESTING -> {
                        speechBubble = "💭 呼噜噜~"
                        val restStartTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - restStartTime < 3000) {
                            tick++
                            delay(40)
                        }
                        speechBubble = null
                    }
                    else -> delay(1000)
                }

                // 两次行为之间的随机小憩
                val pauseTicks = Random.nextInt(25, 60)
                for (i in 0 until pauseTicks) {
                    tick++
                    delay(40)
                }
            }
        }
    }

    // 拍立得照片底框
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFAF7F2)) // 柔和温润相纸底色
            .border(1.dp, Color(0xFFF0EAE1), RoundedCornerShape(18.dp))
    ) {
        if (!isPetFloating) {
            // ================= 状态 A：真正的线条小狗在相框中自主漫步生活 =================

            // 1. 相框右上角：心境小标签
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (behavior) {
                        FramePuppyBehavior.WALKING -> "🐾 巡游中"
                        FramePuppyBehavior.SNIFFING -> "🔍 探秘中"
                        FramePuppyBehavior.TILTING -> "✨ 歪头看你"
                        FramePuppyBehavior.RESTING -> "💤 小憩中"
                        FramePuppyBehavior.HAPPY_JUMP -> "❤️ 超开心"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = PetColors.TextSub
                )
            }

            // 2. 地面阴影与微地平线
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 26.dp)
                    .width(240.dp)
                    .height(2.dp)
                    .background(Color(0xFFEADBCE))
            )

            // 3. 真正的“线条小狗”手绘 Canvas 角色主体 (与桌面悬浮桌宠 100% 视觉同源)
            val tiltAngle = if (behavior == FramePuppyBehavior.TILTING) (if (facingRight) 12f else -12f) else 0f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(x = currentX.dp, y = 14.dp)
                    .scale(jumpScale)
                    .rotate(tiltAngle)
                    .graphicsLayer {
                        scaleX = if (facingRight) 1f else -1f
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onPetTouched()
                        PetSyncEngine.poke()
                        behavior = FramePuppyBehavior.HAPPY_JUMP
                        showHeartEffect = true
                        speechBubble = "小狗开心地蹭你的指尖 ❤️"
                    }
            ) {
                // 小狗心声泡泡 (如果朝左则反转字体防镜像翻转)
                if (speechBubble != null) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer { scaleX = if (facingRight) 1f else -1f }
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .border(1.dp, PetColors.CardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = speechBubble!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PetColors.TextMain
                        )
                    }
                }

                // 🎨 核心：原生绘制真正的黑线白底线条小狗
                DoodlePuppyNativeCanvas(
                    behavior = behavior,
                    tick = tick,
                    isHappy = showHeartEffect || behavior == FramePuppyBehavior.HAPPY_JUMP,
                    modifier = Modifier.size(150.dp, 138.dp)
                )
            }

            // 4. 抚摸爱心浮动
            if (showHeartEffect) {
                Box(
                    modifier = Modifier
                        .offset(x = currentX.dp, y = (-42).dp)
                ) {
                    Text(text = "❤️ ✨", fontSize = 28.sp)
                }
            }

        } else {
            // ================= 状态 B：小狗已出屋，去手机桌面漫步巡逻 =================

            // 1. 相框右上角：外出标签
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🍃 外出漫步中",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = PetColors.Primary
                )
            }

            // 2. 相框内留下的温馨空巢场景：软软小垫子 + 伸向相框外的小爪印
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                // 手绘风小窝垫子
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3ECE2))
                        .border(1.5.dp, Color(0xFFE5DDD0), CircleShape)
                ) {
                    Text(text = "🛏️", fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 一排延伸出相框的浅浅小脚印
                Text(
                    text = "🐾   🐾   🐾",
                    fontSize = 13.sp,
                    color = Color(0xFFD6CBC0)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 外出木牌说明
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .border(1.dp, PetColors.CardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "小狗正在手机桌面上漫步巡逻中 🐾",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PetColors.TextMain
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "（点击下方按钮可随时唤小狗回相框）",
                    fontSize = 11.sp,
                    color = PetColors.TextMuted
                )
            }
        }
    }
}

/**
 * 真正的黑白线条小狗原生 Canvas 绘制组件：
 * 与 PetFloatingView 使用完全相同的几何线条、黑白配色与五官微表情！
 */
@Composable
fun DoodlePuppyNativeCanvas(
    behavior: FramePuppyBehavior,
    tick: Long,
    isHappy: Boolean,
    modifier: Modifier = Modifier
) {
    // 复用高性能绘制画笔
    val furPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
    }

    val strokePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#1E1E1E".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 4.6f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    val eyePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#1E1E1E".toColorInt()
            style = Paint.Style.FILL
        }
    }

    val eyeGlintPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
    }

    val blushPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFAFC0".toColorInt()
            style = Paint.Style.FILL
        }
    }

    val tonguePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF6888".toColorInt()
            style = Paint.Style.FILL
        }
    }

    val heartPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF4B68".toColorInt()
            style = Paint.Style.FILL
        }
    }

    // 绘制 Path
    val headPath = remember { Path() }
    val leftEarPath = remember { Path() }
    val rightEarPath = remember { Path() }
    val bodyPath = remember { Path() }
    val tailPath = remember { Path() }
    val heartPath = remember { Path() }

    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f - 4f

            // 行走微起伏
            val isWalking = behavior == FramePuppyBehavior.WALKING
            val isSniffing = behavior == FramePuppyBehavior.SNIFFING
            val isResting = behavior == FramePuppyBehavior.RESTING
            val breath = if (isWalking) sin(tick * 0.28).toFloat() * 2.4f else sin(tick * 0.14).toFloat() * 1.6f

            nativeCanvas.save()
            nativeCanvas.scale(1.72f, 1.72f, cx, cy + 18f)

            // 1. 尾巴 (摇摆动效)
            val tailBaseX = cx + 26f
            val tailBaseY = cy + 22f + breath
            val tailAngle = if (isWalking || isHappy) sin(tick * 0.32).toFloat() * 22f else sin(tick * 0.16).toFloat() * 14f

            nativeCanvas.save()
            nativeCanvas.rotate(tailAngle, tailBaseX, tailBaseY)
            tailPath.reset()
            tailPath.moveTo(tailBaseX, tailBaseY)
            tailPath.quadTo(tailBaseX + 16f, tailBaseY - 12f, tailBaseX + 10f, tailBaseY - 22f)
            tailPath.quadTo(tailBaseX + 5f, tailBaseY - 24f, tailBaseX + 3f, tailBaseY - 17f)
            tailPath.quadTo(tailBaseX + 7f, tailBaseY - 8f, tailBaseX, tailBaseY + 6f)
            tailPath.close()
            nativeCanvas.drawPath(tailPath, furPaint)
            nativeCanvas.drawPath(tailPath, strokePaint)
            nativeCanvas.restore()

            // 2. 身体与四肢
            val bodyY = cy + 14f + breath
            bodyPath.reset()
            bodyPath.moveTo(cx - 26f, bodyY - 8f)
            bodyPath.quadTo(cx - 36f, bodyY + 18f, cx - 24f, bodyY + 27f)
            bodyPath.quadTo(cx - 17f, bodyY + 29f, cx - 12f, bodyY + 27f)
            bodyPath.lineTo(cx + 12f, bodyY + 27f)
            bodyPath.quadTo(cx + 17f, bodyY + 29f, cx + 24f, bodyY + 27f)
            bodyPath.quadTo(cx + 36f, bodyY + 18f, cx + 26f, bodyY - 8f)
            bodyPath.close()

            nativeCanvas.drawPath(bodyPath, furPaint)
            nativeCanvas.drawPath(bodyPath, strokePaint)

            // 后肢小圆脚
            val legStride = if (isWalking) sin(tick * 0.28).toFloat() * 3.5f else 0f
            nativeCanvas.drawCircle(cx - 18f - legStride * 0.4f, bodyY + 26f + legStride, 4.6f, furPaint)
            nativeCanvas.drawCircle(cx - 18f - legStride * 0.4f, bodyY + 26f + legStride, 4.6f, strokePaint)
            nativeCanvas.drawCircle(cx + 18f + legStride * 0.4f, bodyY + 26f - legStride, 4.6f, furPaint)
            nativeCanvas.drawCircle(cx + 18f + legStride * 0.4f, bodyY + 26f - legStride, 4.6f, strokePaint)

            // 3. 脑袋与耳朵
            val headY = cy - 10f + breath

            // 左耳微晃
            val leftEarAngle = if (isWalking) sin(tick * 0.28).toFloat() * 5.0f else 0f
            nativeCanvas.save()
            nativeCanvas.rotate(leftEarAngle, cx - 24f, headY - 14f)
            leftEarPath.reset()
            leftEarPath.moveTo(cx - 24f, headY - 14f)
            leftEarPath.cubicTo(cx - 45f, headY - 14f, cx - 44f, headY + 16f, cx - 25f, headY + 12f)
            leftEarPath.close()
            nativeCanvas.drawPath(leftEarPath, furPaint)
            nativeCanvas.drawPath(leftEarPath, strokePaint)
            nativeCanvas.restore()

            // 右耳微晃
            val rightEarAngle = if (isWalking) -sin(tick * 0.28).toFloat() * 5.0f else 0f
            nativeCanvas.save()
            nativeCanvas.rotate(rightEarAngle, cx + 24f, headY - 14f)
            rightEarPath.reset()
            rightEarPath.moveTo(cx + 24f, headY - 14f)
            rightEarPath.cubicTo(cx + 45f, headY - 14f, cx + 44f, headY + 16f, cx + 25f, headY + 12f)
            rightEarPath.close()
            nativeCanvas.drawPath(rightEarPath, furPaint)
            nativeCanvas.drawPath(rightEarPath, strokePaint)
            nativeCanvas.restore()

            // 萌萌圆头
            headPath.reset()
            headPath.moveTo(cx - 28f, headY)
            headPath.cubicTo(cx - 30f, headY - 26f, cx + 30f, headY - 26f, cx + 28f, headY)
            headPath.cubicTo(cx + 34f, headY + 22f, cx - 34f, headY + 22f, cx - 28f, headY)
            headPath.close()
            nativeCanvas.drawPath(headPath, furPaint)
            nativeCanvas.drawPath(headPath, strokePaint)

            // 4. 五官细节：豆豆眼、粉腮红、纽扣鼻、微笑嘴
            val faceY = cy - 8f + breath

            // 眼睛 (开心弯月眼 / 眨眼 / 纯真大眼)
            val isBlinking = (tick % 110) in 70..74
            if (isHappy) {
                // 开心眯眯笑眼 (^ ^)
                nativeCanvas.drawArc(cx - 22f, faceY - 8f, cx - 12f, faceY + 2f, 200f, 140f, false, strokePaint)
                nativeCanvas.drawArc(cx + 12f, faceY - 8f, cx + 22f, faceY + 2f, 200f, 140f, false, strokePaint)
            } else if (isResting) {
                // 沉睡平和闭眼 (u u)
                nativeCanvas.drawArc(cx - 21f, faceY - 4f, cx - 13f, faceY + 2f, 10f, 160f, false, strokePaint)
                nativeCanvas.drawArc(cx + 13f, faceY - 4f, cx + 21f, faceY + 2f, 10f, 160f, false, strokePaint)
            } else if (isBlinking) {
                // 自然眨眼
                nativeCanvas.drawLine(cx - 21f, faceY - 3f, cx - 13f, faceY - 3f, strokePaint)
                nativeCanvas.drawLine(cx + 13f, faceY - 3f, cx + 21f, faceY - 3f, strokePaint)
            } else {
                // 纯真乌溜溜大豆豆眼 (带水汪汪高光)
                nativeCanvas.drawCircle(cx - 18f, faceY - 3f, 4.2f, eyePaint)
                nativeCanvas.drawCircle(cx + 18f, faceY - 3f, 4.2f, eyePaint)
                nativeCanvas.drawCircle(cx - 19f, faceY - 4.2f, 1.4f, eyeGlintPaint)
                nativeCanvas.drawCircle(cx + 17f, faceY - 4.2f, 1.4f, eyeGlintPaint)
            }

            // 软萌粉嫩腮红
            nativeCanvas.drawCircle(cx - 24f, faceY + 5f, 6.2f, blushPaint)
            nativeCanvas.drawCircle(cx + 24f, faceY + 5f, 6.2f, blushPaint)

            // 纽扣小黑鼻
            val noseOffsetY = if (isSniffing) sin(tick * 0.7).toFloat() * 1.8f else 0f
            nativeCanvas.drawOval(cx - 4.0f, faceY + 2f + noseOffsetY, cx + 4.0f, faceY + 6.8f + noseOffsetY, eyePaint)

            // 微笑小嘴
            nativeCanvas.drawArc(cx - 7f, faceY + 4.5f, cx, faceY + 11.5f, 10f, 160f, false, strokePaint)
            nativeCanvas.drawArc(cx, faceY + 4.5f, cx + 7f, faceY + 11.5f, 10f, 160f, false, strokePaint)

            // 5. 胸前小红心与小前爪
            val chestY = cy + 11f + breath
            heartPath.reset()
            val heartScale = 0.46f
            heartPath.moveTo(cx, chestY + 10f)
            heartPath.cubicTo(cx - 18f * heartScale, chestY - 18f * heartScale, cx - 36f * heartScale, chestY + 4f * heartScale, cx, chestY + 32f * heartScale)
            heartPath.cubicTo(cx + 36f * heartScale, chestY + 4f * heartScale, cx + 18f * heartScale, chestY - 18f * heartScale, cx, chestY + 10f)
            heartPath.close()

            nativeCanvas.drawPath(heartPath, heartPaint)
            nativeCanvas.drawPath(heartPath, strokePaint)

            // 两只握住小爱心的小白爪
            nativeCanvas.drawCircle(cx - 11f, chestY + 10f, 5.2f, furPaint)
            nativeCanvas.drawCircle(cx - 11f, chestY + 10f, 5.2f, strokePaint)
            nativeCanvas.drawCircle(cx + 11f, chestY + 10f, 5.2f, furPaint)
            nativeCanvas.drawCircle(cx + 11f, chestY + 10f, 5.2f, strokePaint)

            nativeCanvas.restore()
        }
    }
}
