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
import com.couple.pet.pet.BasePet
import com.couple.pet.pet.PetPaintKit
import com.couple.pet.pet.PetRegistry
import com.couple.pet.pet.UniversalPetAction
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
    feedTrigger: Long = 0L,
    fedFoodName: String = "",
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

    // 投喂响应动效
    LaunchedEffect(feedTrigger) {
        if (feedTrigger > 0L && !isPetFloating) {
            behavior = FramePuppyBehavior.HAPPY_JUMP
            speechBubble = if (fedFoodName.isNotEmpty()) "吃到了 $fedFoodName 😋" else "好香好香~ 😋"
            showHeartEffect = true
            delay(1600)
            showHeartEffect = false
            speechBubble = null
            behavior = FramePuppyBehavior.WALKING
        }
    }

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
 * 全物种通用原生 Canvas 绘制组件：
 * 支持 BasePet 任意多态子类（线条小狗、软萌奶猫、垂耳小兔等）在相框内的高性能原生绘制。
 */
@Composable
fun UniversalPetNativeCanvas(
    pet: BasePet,
    behavior: FramePuppyBehavior,
    tick: Long,
    isHappy: Boolean,
    modifier: Modifier = Modifier
) {
    val paints = remember { PetPaintKit.defaultKit }
    val motion = when {
        isHappy || behavior == FramePuppyBehavior.HAPPY_JUMP -> UniversalPetAction.HAPPY_JUMP
        behavior == FramePuppyBehavior.WALKING -> UniversalPetAction.WALK
        behavior == FramePuppyBehavior.RESTING -> UniversalPetAction.REST
        behavior == FramePuppyBehavior.SNIFFING -> UniversalPetAction.IDLE
        else -> UniversalPetAction.IDLE
    }

    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f - 4f

            pet.render(
                canvas = nativeCanvas,
                cx = cx,
                cy = cy + 18f,
                scale = 1.72f,
                motion = motion,
                tick = tick,
                facingRight = true,
                paints = paints
            )
        }
    }
}

/**
 * 兼容原有调用的绘制入口，自动委托给当前活跃的 BasePet 多态子类
 */
@Composable
fun DoodlePuppyNativeCanvas(
    behavior: FramePuppyBehavior,
    tick: Long,
    isHappy: Boolean,
    modifier: Modifier = Modifier
) {
    val activePet by PetRegistry.activePet.collectAsState()
    UniversalPetNativeCanvas(
        pet = activePet,
        behavior = behavior,
        tick = tick,
        isHappy = isHappy,
        modifier = modifier
    )
}
