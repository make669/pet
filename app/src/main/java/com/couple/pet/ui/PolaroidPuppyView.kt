package com.couple.pet.ui

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * 实现“全手机生命唯一守恒（One Living Puppy）”：
 * 1. 当未放桌面漫步 (isPetFloating == false)：小狗在相框内部自主游荡、碰壁掉头、歪头打盹；
 * 2. 当小狗去桌面漫步 (isPetFloating == true)：小狗离开相框，相框内呈现温馨的“外出漫步留守态”与空垫子小脚印；
 * 3. 叫回小狗时，小狗快乐跳回相框继续漫步。
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
    var stepTick by remember { mutableIntStateOf(0) }
    var speechBubble by remember { mutableStateOf<String?>(null) }
    var showHeartEffect by remember { mutableStateOf(false) }

    // 触控回弹动画
    val jumpScale by animateFloatAsState(
        targetValue = if (behavior == FramePuppyBehavior.HAPPY_JUMP) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "jumpScale"
    )

    // 尾巴与头部摇晃动画
    val infiniteTransition = rememberInfiniteTransition(label = "wagTail")
    val wagRotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wagRotation"
    )

    // 自主游荡 AI 循环 (只在相框内时运行)
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
                    in 0..50 -> FramePuppyBehavior.WALKING
                    in 51..70 -> FramePuppyBehavior.SNIFFING
                    in 71..85 -> FramePuppyBehavior.TILTING
                    else -> FramePuppyBehavior.RESTING
                }

                behavior = nextBehavior

                when (nextBehavior) {
                    FramePuppyBehavior.WALKING -> {
                        // 在 [-85dp, 85dp] 走廊区间内随机寻点
                        targetX = Random.nextFloat() * 170f - 85f
                        facingRight = targetX >= currentX
                        speechBubble = null

                        // 慢步行走直至到达目标
                        while (abs(targetX - currentX) > 2f && behavior == FramePuppyBehavior.WALKING) {
                            val step = if (facingRight) 2.2f else -2.2f
                            currentX += step
                            stepTick++
                            delay(40)
                        }
                    }
                    FramePuppyBehavior.SNIFFING -> {
                        speechBubble = "💭 嗅嗅..."
                        delay(2200)
                        speechBubble = null
                    }
                    FramePuppyBehavior.TILTING -> {
                        speechBubble = "💭 歪头看你"
                        delay(2000)
                        speechBubble = null
                    }
                    FramePuppyBehavior.RESTING -> {
                        speechBubble = "💭 呼噜噜~"
                        delay(3000)
                        speechBubble = null
                    }
                    else -> delay(1000)
                }

                // 两次行为之间的随机小憩
                delay(Random.nextLong(1000, 2500))
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
            // ================= 状态 A：小狗在相框中自主漫步生活 =================

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
                        FramePuppyBehavior.TILTING -> "✨ 好奇看你"
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
                    .padding(bottom = 36.dp)
                    .width(220.dp)
                    .height(2.dp)
                    .background(Color(0xFFEADBCE))
            )

            // 3. 自主游荡的线条小狗主体 (带行走起伏与左右转身)
            val bobbingY = if (behavior == FramePuppyBehavior.WALKING) sin(stepTick * 0.45f) * 4f else 0f
            val tiltAngle = if (behavior == FramePuppyBehavior.TILTING) (if (facingRight) 14f else -14f) else 0f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(x = currentX.dp, y = (16 + bobbingY).dp)
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
                            .background(Color.White.copy(alpha = 0.9f))
                            .border(1.dp, PetColors.CardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = speechBubble!!,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PetColors.TextMain
                        )
                    }
                }

                // 线条小狗主角
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "🐶",
                        fontSize = 72.sp,
                        modifier = Modifier.rotate(if (behavior == FramePuppyBehavior.WALKING) wagRotation else 0f)
                    )
                }
            }

            // 4. 抚摸爱心浮动
            if (showHeartEffect) {
                Box(
                    modifier = Modifier
                        .offset(x = currentX.dp, y = (-45).dp)
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
                // 暂时空置的柔软小垫窝
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3ECE2))
                        .border(1.5.dp, Color(0xFFE5DDD0), CircleShape)
                ) {
                    Text(text = "🛏️", fontSize = 38.sp)
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
