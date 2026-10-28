package com.couple.pet.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.ui.theme.PetColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 【主页】：方案 2 · 极简拍立得画框风 (Minimalist Polaroid Canvas)
 * - 视觉中心：一张大尺寸温润拍立得白卡，线条小狗在相框中灵动互动
 * - 细节留白：拍立得手写签名、实时状态寄语与精细马卡龙胶囊条
 * - 底部操控：双核极简圆角按钮 [ 🦴 快速投喂 ] 与 [ 🐾 桌宠漫步 ]，简洁大方
 */
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    isPetFloating: Boolean = false,
    onToggleFloatingPet: () -> Unit = {}
) {
    val petStats by PetSyncEngine.petStats.collectAsState()
    val coupleProfile by PetSyncEngine.coupleProfile.collectAsState()

    var petBounce by remember { mutableStateOf(false) }
    var showHeart by remember { mutableStateOf(false) }
    var feedbackTip by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val scaleAnim by animateFloatAsState(
        targetValue = if (petBounce) 1.16f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
        label = "polaroidBounce"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PetColors.Background)
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ================= 1. 顶部操作栏 =================
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 12.dp)
        ) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PetColors.Surface)
                    .border(1.dp, PetColors.CardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "打开功能菜单",
                    tint = PetColors.TextMain,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "🐾 线条小狗伴侣",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = PetColors.TextMain
            )

            // 陪伴状态胶囊
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(PetColors.PrimaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (coupleProfile.isPaired) "💕 双人" else "🐾 单人",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PetColors.Primary
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.08f))

        // ================= 2. 核心：象牙白拍立得卡片 (Polaroid Card) =================
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = PetColors.Surface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PetColors.CardBorder)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // 拍立得照片视窗 (Photo Viewport)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFAF7F2)) // 柔和温润相纸底色
                        .border(1.dp, Color(0xFFF0EAE1), RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            PetSyncEngine.poke()
                            petBounce = true
                            showHeart = true
                            feedbackTip = "小狗开心地蹭了蹭你的手掌 ❤️"
                            coroutineScope.launch {
                                delay(250)
                                petBounce = false
                                delay(1600)
                                showHeart = false
                                feedbackTip = null
                            }
                        }
                ) {
                    // 相片右上角：天气心境小标签
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "☀️ 阳光正好",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PetColors.TextSub
                        )
                    }

                    // 拍立得相框中的线条小狗主角
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(scaleAnim)
                    ) {
                        Text(
                            text = "🐶",
                            fontSize = 76.sp
                        )
                    }

                    // 飘动的爱心动效
                    if (showHeart) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-55).dp)
                        ) {
                            Text(text = "❤️ ✨", fontSize = 28.sp)
                        }
                    }

                    // 互动气泡提示条
                    if (feedbackTip != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PetColors.TextMain.copy(alpha = 0.85f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = feedbackTip!!,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 拍立得底部白色留白注记区 (Polaroid Note Area)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "线条小狗 · 温暖陪伴中",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PetColors.TextMain
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PetColors.SurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Lv.${petStats.level}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PetColors.TextMain
                        )
                    }
                }

                Text(
                    text = "“今天也是元气满满的一天，记得按时吃饱饱哦～”",
                    fontSize = 12.sp,
                    color = PetColors.TextSub,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                // 拍立得内部精致三色胶囊进度
                PolaroidStatRow("🍖 饱食", petStats.hunger, PetColors.Hunger, PetColors.HungerTrack)
                Spacer(modifier = Modifier.height(6.dp))
                PolaroidStatRow("❤️ 心情", petStats.mood, PetColors.Mood, PetColors.MoodTrack)
                Spacer(modifier = Modifier.height(6.dp))
                PolaroidStatRow("🫧 清洁", petStats.clean, PetColors.Clean, PetColors.CleanTrack)
            }
        }

        Spacer(modifier = Modifier.weight(0.12f))

        // ================= 3. 底部两大核心操作按钮 (简洁、大方) =================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 按钮 1：快速投喂
            Button(
                onClick = {
                    PetSyncEngine.feed(15)
                    petBounce = true
                    feedbackTip = "投喂美味小鱼干，饱食度 +15 🐟"
                    coroutineScope.launch {
                        delay(250)
                        petBounce = false
                        delay(1600)
                        feedbackTip = null
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PetColors.DrawerFeed),
                contentPadding = PaddingValues(vertical = 15.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "🦴 快速投喂",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PetColors.TextMain
                )
            }

            // 按钮 2：桌宠漫步开关
            Button(
                onClick = onToggleFloatingPet,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPetFloating) PetColors.SurfaceVariant else PetColors.Primary
                ),
                contentPadding = PaddingValues(vertical = 15.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isPetFloating) "🐾 收起桌宠" else "🐾 放桌面漫步",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPetFloating) PetColors.TextMain else Color.White
                )
            }
        }
    }
}

@Composable
private fun PolaroidStatRow(
    label: String,
    value: Int,
    fillColor: Color,
    trackColor: Color
) {
    val progress by animateFloatAsState(targetValue = value / 100f, label = "stat")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = PetColors.TextMain
        )

        LinearProgressIndicator(
            progress = { progress },
            color = fillColor,
            trackColor = trackColor,
            modifier = Modifier
                .width(170.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        Text(
            text = "$value%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PetColors.TextSub,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}
