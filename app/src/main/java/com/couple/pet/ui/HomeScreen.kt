package com.couple.pet.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * 深度实现“全手机生命唯一守恒（One Living Puppy）”：
 * 1. 小狗未放桌面时：小狗在相框中自主慢步漫步、碰壁转身、歪头嗅嗅；
 * 2. 小狗放桌面后：小狗跃出相框去屏幕漫步，相框内留守空垫子与外出脚印；
 * 3. 叫回小狗后：小狗快乐钻回相框继续漫步。
 */
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    isPetFloating: Boolean = false,
    onToggleFloatingPet: () -> Unit = {}
) {
    val petStats by PetSyncEngine.petStats.collectAsState()
    val coupleProfile by PetSyncEngine.coupleProfile.collectAsState()

    var feedbackTip by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
                // 拍立得照片视窗 (集成了相框自主游荡与外出留守逻辑)
                PolaroidPuppyViewport(
                    isPetFloating = isPetFloating,
                    onPetTouched = {
                        feedbackTip = "小狗开心地蹭了蹭你的手掌 ❤️"
                        coroutineScope.launch {
                            delay(1800)
                            feedbackTip = null
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 拍立得底部白色留白注记区 (Polaroid Note Area)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPetFloating) "线条小狗 · 桌面探险中" else "线条小狗 · 相框小憩中",
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
                    text = if (isPetFloating) {
                        "“小狗正在手机桌面上巡逻玩耍，随时可叫它回家哦～”"
                    } else {
                        "“在拍立得小房间里悠闲漫步，随时可以带它去桌面玩耍~”"
                    },
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

        // 临时反馈浮条
        if (feedbackTip != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(PetColors.TextMain.copy(alpha = 0.85f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = feedbackTip!!,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

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
                    feedbackTip = if (isPetFloating) "隔空投喂！桌面小狗吃到了小鱼干 🐟" else "投喂美味小鱼干，饱食度 +15 🐟"
                    coroutineScope.launch {
                        delay(1800)
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

            // 按钮 2：桌宠漫步开关 (生命唯一性联动)
            Button(
                onClick = onToggleFloatingPet,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPetFloating) PetColors.DrawerPet else PetColors.Primary
                ),
                contentPadding = PaddingValues(vertical = 15.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isPetFloating) "🐾 叫小狗回相框" else "🐾 放桌面漫步",
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
