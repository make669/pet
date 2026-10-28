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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.ui.theme.PetColors

/**
 * 【主页】：依据手绘原型全新升级治愈系暖牛奶配色 (Warm Milk & Pastel Rose)
 * - 顶部汉堡菜单按钮「三」唤醒 5 个小圆圈功能抽屉
 * - 屏幕正中央巨大的【当前 pet 各个数值】精致大圆盘看板
 */
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit
) {
    val petStats by PetSyncEngine.petStats.collectAsState()
    val coupleProfile by PetSyncEngine.coupleProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PetColors.Background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 顶部操作栏（汉堡菜单按钮「三」）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp)
        ) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(PetColors.Surface)
                    .border(1.dp, PetColors.CardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "打开功能菜单",
                    tint = PetColors.TextMain
                )
            }

            Text(
                text = "🐾 线条小狗伴侣",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PetColors.TextMain
            )

            // 右侧占位保持居中对称
            Spacer(modifier = Modifier.size(46.dp))
        }

        Spacer(modifier = Modifier.weight(0.12f))

        // 2. 核心大圆盘：【当前 pet 各个数值】（精致双层渐变温润圆盘）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(316.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            PetColors.SurfaceElevated,
                            PetColors.PrimaryContainer
                        )
                    )
                )
                .border(4.dp, PetColors.PrimaryBorder, CircleShape)
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // 小狗头像圆圈底座
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, PetColors.PrimaryBorder, CircleShape)
                ) {
                    Text(
                        text = "🐶",
                        fontSize = 46.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "线条小狗 · Lv.${petStats.level}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PetColors.TextMain
                )

                // 抚养状态气泡胶囊
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PetColors.PrimaryContainer)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (coupleProfile.isPaired) "💕 双人共同抚养中" else "🐾 单人暖心陪伴中",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = PetColors.Primary
                    )
                }

                // 核心三大数值显示（马卡龙治愈色阶）
                CircularStatProgress("🍖 饱食度", petStats.hunger, PetColors.Hunger, PetColors.HungerTrack)
                Spacer(modifier = Modifier.height(6.dp))
                CircularStatProgress("❤️ 心情值", petStats.mood, PetColors.Mood, PetColors.MoodTrack)
                Spacer(modifier = Modifier.height(6.dp))
                CircularStatProgress("🛁 清洁度", petStats.clean, PetColors.Clean, PetColors.CleanTrack)
            }
        }

        Spacer(modifier = Modifier.weight(0.2f))

        // 3. 底部提示卡片（圆角精致温和）
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PetColors.Surface),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PetColors.CardBorder, PetColors.CardBorder))),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PetColors.PrimaryContainer)
                ) {
                    Text(text = "💡", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "点击左上角「三」快捷魔盒，随时唤醒桌面小狗、进行投喂洗澡或配对情侣！",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = PetColors.TextSub
                )
            }
        }
    }
}

@Composable
private fun CircularStatProgress(
    label: String,
    value: Int,
    progressColor: Color,
    trackColor: Color
) {
    val progress by animateFloatAsState(targetValue = value / 100f, label = "progress")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .padding(vertical = 1.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = PetColors.TextMain
        )
        LinearProgressIndicator(
            progress = { progress },
            color = progressColor,
            trackColor = trackColor,
            modifier = Modifier
                .width(115.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Text(
            text = "$value%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = PetColors.TextMain,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}
