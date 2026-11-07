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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.couple.pet.audio.SynesthesiaEvent
import com.couple.pet.audio.SynesthesiaOrchestrator
import com.couple.pet.ui.theme.PetColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * 经典线条小狗：马尔济斯小白狗与小金毛双犬依偎贴贴原生 Canvas 渲染组件
 * 严格贯彻美术设计圣经：
 * - 纯净棉花糖白狗 (#FFFDF9) 与 暖燕麦曲奇小金毛 (#D5C2A5)
 * - 脸颊软体接触凹陷 (4.5dp 挤压形变)
 * - 小白倾斜 +12.5°，小金毛倾斜 -17.2°
 * - 小金毛珊瑚粉细项圈 (#FF7663)
 * - 触碰触发 HAV 心跳双振与 432Hz 和弦共振
 */
@Composable
fun DualPuppySnuggleCanvas(
    modifier: Modifier = Modifier,
    tick: Long = 0L,
    isSnuggleHappy: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "snuggleBreath")
    val breath by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f + 10f

            // 1. 初始化纯正手绘暖炭墨与色彩画笔
            val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#2B201A")
                strokeWidth = 4.2f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val fineInkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#2B201A")
                strokeWidth = 2.6f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val whiteFurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FFFDF9")
                style = Paint.Style.FILL
            }

            val goldenFurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#D5C2A5")
                style = Paint.Style.FILL
            }

            val pinkCollarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FF7663")
                strokeWidth = 4.8f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#1F1612")
                style = Paint.Style.FILL
            }

            val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FFAFC0")
                style = Paint.Style.FILL
            }

            val tonguePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FF6888")
                style = Paint.Style.FILL
            }

            // ----------------------------------------------------
            // 2. 绘制右侧小金毛 (燕麦曲奇体，向左偏靠 -17.2°)
            // ----------------------------------------------------
            native.save()
            val goldenCenterX = cx + 38f
            val goldenCenterY = cy + breath * 0.4f
            native.rotate(-16.8f, goldenCenterX, goldenCenterY)

            // 金毛大耳朵 (左垂耳贴向中缝，右垂耳向外)
            val goldenEar = Path().apply {
                moveTo(goldenCenterX + 22f, goldenCenterY - 24f)
                cubicTo(goldenCenterX + 46f, goldenCenterY - 20f, goldenCenterX + 44f, goldenCenterY + 22f, goldenCenterX + 20f, goldenCenterY + 20f)
                close()
            }
            native.drawPath(goldenEar, goldenFurPaint)
            native.drawPath(goldenEar, inkPaint)

            // 金毛头部与躯干
            val goldenBody = Path().apply {
                // 头顶平滑弧
                moveTo(goldenCenterX - 24f, goldenCenterY - 14f)
                cubicTo(goldenCenterX - 18f, goldenCenterY - 34f, goldenCenterX + 24f, goldenCenterY - 34f, goldenCenterX + 26f, goldenCenterY - 12f)
                // 右侧脸与身子
                cubicTo(goldenCenterX + 38f, goldenCenterY + 12f, goldenCenterX + 32f, goldenCenterY + 38f, goldenCenterX + 8f, goldenCenterY + 40f)
                // 腹部
                cubicTo(goldenCenterX - 12f, goldenCenterY + 42f, goldenCenterX - 28f, goldenCenterY + 36f, goldenCenterX - 28f, goldenCenterY + 16f)
                // 贴合面：向内凹陷挤压面 (与小白狗脸蛋紧贴嵌合)
                cubicTo(goldenCenterX - 22f, goldenCenterY + 4f, goldenCenterX - 20f, goldenCenterY - 8f, goldenCenterX - 24f, goldenCenterY - 14f)
                close()
            }
            native.drawPath(goldenBody, goldenFurPaint)
            native.drawPath(goldenBody, inkPaint)

            // 金毛珊瑚红细项圈
            val goldenCollar = Path().apply {
                moveTo(goldenCenterX - 20f, goldenCenterY + 18f)
                quadTo(goldenCenterX + 4f, goldenCenterY + 24f, goldenCenterX + 24f, goldenCenterY + 16f)
            }
            native.drawPath(goldenCollar, pinkCollarPaint)

            // 金毛五官：两颗黑豆眼 + 倒 w 微笑嘴
            if (isSnuggleHappy) {
                // 眯眼笑弯弯
                native.drawArc(goldenCenterX - 12f, goldenCenterY - 10f, goldenCenterX - 2f, goldenCenterY, 180f, 180f, false, fineInkPaint)
                native.drawArc(goldenCenterX + 6f, goldenCenterY - 10f, goldenCenterX + 16f, goldenCenterY, 180f, 180f, false, fineInkPaint)
            } else {
                native.drawCircle(goldenCenterX - 7f, goldenCenterY - 5f, 3.4f, eyePaint)
                native.drawCircle(goldenCenterX + 11f, goldenCenterY - 5f, 3.4f, eyePaint)
            }
            // 鼻头
            native.drawCircle(goldenCenterX + 2f, goldenCenterY + 1f, 2.8f, eyePaint)
            // 倒 w 嘴
            val goldenMouth = Path().apply {
                moveTo(goldenCenterX - 5f, goldenCenterY + 5f)
                quadTo(goldenCenterX - 1f, goldenCenterY + 9f, goldenCenterX + 2f, goldenCenterY + 6f)
                quadTo(goldenCenterX + 5f, goldenCenterY + 9f, goldenCenterX + 9f, goldenCenterY + 5f)
            }
            native.drawPath(goldenMouth, fineInkPaint)

            // 腮红
            native.drawCircle(goldenCenterX + 18f, goldenCenterY + 2f, 3.5f, blushPaint)

            // 金毛尾巴 (向右翘起)
            val goldenTail = Path().apply {
                moveTo(goldenCenterX + 26f, goldenCenterY + 28f)
                cubicTo(goldenCenterX + 42f, goldenCenterY + 24f, goldenCenterX + 45f, goldenCenterY + 38f, goldenCenterX + 30f, goldenCenterY + 40f)
                close()
            }
            native.drawPath(goldenTail, goldenFurPaint)
            native.drawPath(goldenTail, inkPaint)
            native.restore()

            // ----------------------------------------------------
            // 3. 绘制左侧小白狗 (棉花糖云朵体，向右微靠 +12.5°)
            // ----------------------------------------------------
            native.save()
            val whiteCenterX = cx - 36f
            val whiteCenterY = cy + breath * 0.45f
            native.rotate(12.2f, whiteCenterX, whiteCenterY)

            // 左垂耳 (3段波浪软耳)
            val whiteLeftEar = Path().apply {
                moveTo(whiteCenterX - 18f, whiteCenterY - 22f)
                cubicTo(whiteCenterX - 42f, whiteCenterY - 18f, whiteCenterX - 40f, whiteCenterY + 18f, whiteCenterX - 18f, whiteCenterY + 12f)
                close()
            }
            native.drawPath(whiteLeftEar, whiteFurPaint)
            native.drawPath(whiteLeftEar, inkPaint)

            // 小白头部与躯干 (3段棉花云团头顶)
            val whiteBody = Path().apply {
                moveTo(whiteCenterX - 22f, whiteCenterY - 10f)
                // 3段波浪云头顶
                cubicTo(whiteCenterX - 24f, whiteCenterY - 26f, whiteCenterX - 8f, whiteCenterY - 32f, whiteCenterX + 2f, whiteCenterY - 28f)
                cubicTo(whiteCenterX + 12f, whiteCenterY - 32f, whiteCenterX + 24f, whiteCenterY - 22f, whiteCenterX + 22f, whiteCenterY - 8f)
                // 右侧脸与前胸 (紧贴金毛挤压面)
                cubicTo(whiteCenterX + 24f, whiteCenterY + 12f, whiteCenterX + 18f, whiteCenterY + 36f, whiteCenterX - 4f, whiteCenterY + 38f)
                // 屁股与后背
                cubicTo(whiteCenterX - 24f, whiteCenterY + 38f, whiteCenterX - 34f, whiteCenterY + 20f, whiteCenterX - 28f, whiteCenterY + 4f)
                close()
            }
            native.drawPath(whiteBody, whiteFurPaint)
            native.drawPath(whiteBody, inkPaint)

            // 小白五官：黑豆圆眼 + 俏皮小鼻尖
            if (isSnuggleHappy) {
                // 眯眼笑弯弯 ^ ^
                native.drawArc(whiteCenterX - 14f, whiteCenterY - 10f, whiteCenterX - 4f, whiteCenterY, 180f, 180f, false, fineInkPaint)
                native.drawArc(whiteCenterX + 4f, whiteCenterY - 10f, whiteCenterX + 14f, whiteCenterY, 180f, 180f, false, fineInkPaint)
            } else {
                native.drawCircle(whiteCenterX - 9f, whiteCenterY - 5f, 3.2f, eyePaint)
                native.drawCircle(whiteCenterX + 9f, whiteCenterY - 5f, 3.2f, eyePaint)
            }
            // 鼻头
            native.drawCircle(whiteCenterX, whiteCenterY + 1f, 2.6f, eyePaint)
            // 倒 w 嘴
            val whiteMouth = Path().apply {
                moveTo(whiteCenterX - 6f, whiteCenterY + 5f)
                quadTo(whiteCenterX - 2f, whiteCenterY + 9f, whiteCenterX, whiteCenterY + 6f)
                quadTo(whiteCenterX + 2f, whiteCenterY + 9f, whiteCenterX + 6f, whiteCenterY + 5f)
            }
            native.drawPath(whiteMouth, fineInkPaint)

            // 腮红
            native.drawCircle(whiteCenterX - 18f, whiteCenterY + 2f, 3.5f, blushPaint)

            // 小白短尾巴 (向左欢快摆动)
            val whiteTail = Path().apply {
                moveTo(whiteCenterX - 26f, whiteCenterY + 26f)
                cubicTo(whiteCenterX - 38f, whiteCenterY + 22f, whiteCenterX - 40f, whiteCenterY + 34f, whiteCenterX - 26f, whiteCenterY + 36f)
                close()
            }
            native.drawPath(whiteTail, whiteFurPaint)
            native.drawPath(whiteTail, inkPaint)
            native.restore()

            // ----------------------------------------------------
            // 4. 头顶浮升的治愈小红心 (在双狗交汇处正上方)
            // ----------------------------------------------------
            val heartX = cx + 2f
            val heartY = cy - 42f + sin(tick * 0.15).toFloat() * 3f
            val heartPath = Path().apply {
                moveTo(heartX, heartY)
                cubicTo(heartX - 6f, heartY - 8f, heartX - 12f, heartY + 2f, heartX, heartY + 10f)
                cubicTo(heartX + 12f, heartY + 2f, heartX + 6f, heartY - 8f, heartX, heartY)
                close()
            }
            val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FF5271")
                style = Paint.Style.FILL
            }
            native.drawPath(heartPath, heartPaint)
            native.drawPath(heartPath, fineInkPaint)
        }
    }
}

/**
 * 双狗依偎贴贴互动对话弹窗 (Dual Puppy Snuggle Modal Dialog)
 */
@Composable
fun DualPuppySnuggleDialog(
    onDismiss: () -> Unit
) {
    var isHappy by remember { mutableStateOf(false) }
    var affectionScore by remember { mutableIntStateOf(99) }
    var tipText by remember { mutableStateOf("点击两只小狗，感受依偎心跳与和弦共鸣 💕") }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            color = PetColors.Background,
            border = androidx.compose.foundation.BorderStroke(2.dp, PetColors.CardBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部标题与关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🐾 依偎贴贴 · 永不分离",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetColors.TextMain
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PetColors.PrimaryContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.PrimaryBorder)
                            ) {
                                Text(
                                    text = "羁绊 $affectionScore%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PetColors.Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "“小白狗与小金毛的脸颊，永远嵌合在一起”",
                            fontSize = 11.sp,
                            color = PetColors.TextSub
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(PetColors.SurfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = PetColors.TextMain,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 核心双犬依偎 Canvas 交互舞台
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF3EDE2))
                        .border(1.dp, PetColors.CardBorder, RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // 激发 HAV 联觉共鸣：双心跳触感与 432Hz 尼龙吉他治愈音
                            SynesthesiaOrchestrator.emit(SynesthesiaEvent.COUPLE_SNUGGLE)
                            isHappy = true
                            affectionScore = (affectionScore + 1).coerceAtMost(100)
                            tipText = "✨ 心跳同频！【小白】与【小金毛】舒服地眯起了眼睛~"
                            coroutineScope.launch {
                                delay(2200)
                                isHappy = false
                            }
                        }
                ) {
                    DualPuppySnuggleCanvas(
                        modifier = Modifier.fillMaxSize(),
                        isSnuggleHappy = isHappy
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 温馨提示胶囊
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PetColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tipText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PetColors.TextMain,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 底部行动按钮
                Button(
                    onClick = {
                        SynesthesiaOrchestrator.emit(SynesthesiaEvent.COUPLE_SNUGGLE)
                        isHappy = true
                        tipText = "💕 记录了一次深情贴贴，已存入秘密回忆日记！"
                        coroutineScope.launch {
                            delay(2000)
                            isHappy = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("❤️ 双狗心动贴贴 (+10 亲密度)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
