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
import androidx.compose.ui.zIndex
import androidx.core.graphics.toColorInt
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.pet.BasePet
import com.couple.pet.pet.PetPaintKit
import com.couple.pet.pet.PetRegistry
import com.couple.pet.pet.UniversalPetAction
import com.couple.pet.ui.theme.PetColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * 相框内小狗自主游荡行为状态 (含真实生理排泄与狂欢闭环)
 */
enum class FramePuppyBehavior {
    WALKING,      // 漫步游荡
    SNIFFING,     // 低头嗅闻
    TILTING,      // 好奇歪头
    RESTING,      // 舒坦就地趴坐
    HAPPY_JUMP,   // 受到抚摸兴奋欢跃
    PRANCING,     // 憋急小碎步寻觅角落
    POOPING,      // 拱背排便（眼神锁定求保护）
    PEEING,       // 抬腿撒尿标记地盘
    POST_ZOOMIES, // 排便一身轻极速冲刺 (Zoomies)
    BUSTED        // 被抓现行瞬间石化
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
    var isBusyWithSequence by remember { mutableStateOf(false) }
    val activePet by PetRegistry.activePet.collectAsState()
    val floorWastes by PetSyncEngine.floorWastes.collectAsState()
    val poopDemoTrigger by PetSyncEngine.poopDemoTrigger.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // 真实排泄行为管线：憋急小碎步 -> 寻觅角落 -> 弓背排便/撒尿 -> 拉后暴走冲刺
    suspend fun performEliminationSequence(isPoop: Boolean) {
        if (isBusyWithSequence) return
        isBusyWithSequence = true
        try {
            // 1. 憋急小碎步高频踩踏
            behavior = FramePuppyBehavior.PRANCING
            speechBubble = activePet.getPranceBubble()
            targetX = if (Random.nextBoolean()) -65f else 65f
            facingRight = targetX >= currentX
            val pranceStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - pranceStart < 2200 && (behavior == FramePuppyBehavior.PRANCING)) {
                if (abs(targetX - currentX) > 4f) {
                    currentX += (if (facingRight) 2.6f else -2.6f)
                }
                tick++
                delay(40)
            }

            // 2. 停下排泄 (弓背拉臭臭 或 抬腿撒尿)
            if (isPoop) {
                behavior = FramePuppyBehavior.POOPING
                speechBubble = activePet.getPoopBubble()
                val poopStart = System.currentTimeMillis()
                while (System.currentTimeMillis() - poopStart < 2800 && behavior == FramePuppyBehavior.POOPING) {
                    tick++
                    delay(40)
                }
                if (behavior == FramePuppyBehavior.POOPING) {
                    val wasteX = (currentX - (if (facingRight) 18f else -18f)).coerceIn(-85f, 85f)
                    PetSyncEngine.spawnWaste(wasteX, isPoop = true, activePet.speciesId)

                    // 3. 拉完一身轻：疯狂冲刺 (Zoomies)
                    behavior = FramePuppyBehavior.POST_ZOOMIES
                    speechBubble = activePet.getZoomiesBubble()
                    repeat(3) { lap ->
                        targetX = if (lap % 2 == 0) 70f else -70f
                        facingRight = targetX >= currentX
                        while (abs(targetX - currentX) > 6f && behavior == FramePuppyBehavior.POST_ZOOMIES) {
                            currentX += (if (facingRight) 5.5f else -5.5f)
                            tick++
                            delay(30)
                        }
                    }
                    speechBubble = null
                    behavior = FramePuppyBehavior.WALKING
                }
            } else {
                behavior = FramePuppyBehavior.PEEING
                speechBubble = activePet.getPeeBubble()
                val peeStart = System.currentTimeMillis()
                while (System.currentTimeMillis() - peeStart < 2200 && behavior == FramePuppyBehavior.PEEING) {
                    tick++
                    delay(40)
                }
                if (behavior == FramePuppyBehavior.PEEING) {
                    val wasteX = (currentX - (if (facingRight) 16f else -16f)).coerceIn(-85f, 85f)
                    PetSyncEngine.spawnWaste(wasteX, isPoop = false, activePet.speciesId)
                    speechBubble = null
                    behavior = FramePuppyBehavior.WALKING
                }
            }
        } finally {
            if (behavior != FramePuppyBehavior.BUSTED) {
                isBusyWithSequence = false
            }
        }
    }

    // 观察排泄行为触发
    LaunchedEffect(poopDemoTrigger) {
        if (poopDemoTrigger > 0L && !isPetFloating) {
            performEliminationSequence(isPoop = true)
        }
    }

    // 投喂响应动效 (在相框小狗头顶气泡中温馨呈现，如用户原型图所示)
    LaunchedEffect(feedTrigger) {
        if (feedTrigger > 0L && !isPetFloating && !isBusyWithSequence) {
            isBusyWithSequence = true
            try {
                behavior = FramePuppyBehavior.HAPPY_JUMP
                speechBubble = activePet.getEatReaction(fedFoodName.ifEmpty { "小鱼干" })
                showHeartEffect = true
                delay(2000)
                showHeartEffect = false
                speechBubble = null
                behavior = FramePuppyBehavior.WALKING
            } finally {
                isBusyWithSequence = false
            }
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
                if (isBusyWithSequence) {
                    delay(150)
                    continue
                }

                // 1. 决定下一个游荡目标点与行为 (含生理排泄与狂欢冲刺)
                val nextBehavior = when (Random.nextInt(100)) {
                    in 0..42 -> FramePuppyBehavior.WALKING
                    in 43..54 -> FramePuppyBehavior.SNIFFING
                    in 55..66 -> FramePuppyBehavior.TILTING
                    in 67..78 -> FramePuppyBehavior.RESTING
                    in 79..86 -> FramePuppyBehavior.PRANCING
                    else -> FramePuppyBehavior.WALKING
                }

                behavior = nextBehavior

                when (nextBehavior) {
                    FramePuppyBehavior.PRANCING -> {
                        performEliminationSequence(isPoop = Random.nextBoolean())
                    }
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
                        FramePuppyBehavior.PRANCING -> "😖 憋急了"
                        FramePuppyBehavior.POOPING -> "💩 方便中"
                        FramePuppyBehavior.PEEING -> "💦 标记地盘"
                        FramePuppyBehavior.POST_ZOOMIES -> "💨 狂欢冲刺"
                        FramePuppyBehavior.BUSTED -> "😳 僵硬石化"
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
                        // 正在排泄时被主人戳中抓现行 -> 瞬间石化定格！
                        if (behavior == FramePuppyBehavior.POOPING || behavior == FramePuppyBehavior.PEEING) {
                            behavior = FramePuppyBehavior.BUSTED
                            speechBubble = activePet.getBustedBubble()
                            coroutineScope.launch {
                                delay(2200)
                                speechBubble = null
                                behavior = FramePuppyBehavior.WALKING
                                isBusyWithSequence = false
                            }
                            return@clickable
                        }
                        onPetTouched()
                        PetSyncEngine.poke()
                        behavior = FramePuppyBehavior.HAPPY_JUMP
                        showHeartEffect = true
                        speechBubble = activePet.getTouchReaction()
                        coroutineScope.launch {
                            delay(2000)
                            showHeartEffect = false
                            speechBubble = null
                            behavior = FramePuppyBehavior.WALKING
                        }
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

            // 5. 地面排泄物互动图层 (置顶层级，可爱小臭臭与水花，点击一键铲屎)
            floorWastes.forEach { waste ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = waste.x.dp, y = (-20).dp)
                        .zIndex(20f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.2.dp, PetColors.Primary, RoundedCornerShape(14.dp))
                        .clickable {
                            PetSyncEngine.cleanWaste(waste.id)
                            speechBubble = activePet.getCleanThankBubble()
                            showHeartEffect = true
                            coroutineScope.launch {
                                delay(2000)
                                showHeartEffect = false
                                speechBubble = null
                            }
                        }
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (waste.isPoop) "💩" else "💧", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (waste.isPoop) "铲臭臭" else "擦水渍", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PetColors.Primary)
                    }
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
        behavior == FramePuppyBehavior.PRANCING -> UniversalPetAction.PRANCE_URGENT
        behavior == FramePuppyBehavior.POOPING -> UniversalPetAction.POOP
        behavior == FramePuppyBehavior.PEEING -> UniversalPetAction.PEE
        behavior == FramePuppyBehavior.POST_ZOOMIES -> UniversalPetAction.POOP_ZOOMIES
        behavior == FramePuppyBehavior.BUSTED -> UniversalPetAction.BUSTED_FREEZE
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
