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

/**
 * 【主页】：对应手绘原型左侧主图
 * - 顶部汉堡菜单按钮「三」唤醒 5 个小圆圈功能抽屉
 * - 屏幕正中央巨大的【当前 pet 各个数值】大圆盘看板
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
            .background(Color(0xFFFBF8F5))
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
                    .background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "打开功能菜单",
                    tint = Color(0xFF263238)
                )
            }

            Text(
                text = "🐾 pet 主页",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238)
            )

            // 右侧占位保持居中对称
            Spacer(modifier = Modifier.size(46.dp))
        }

        Spacer(modifier = Modifier.weight(0.12f))

        // 2. 核心大圆盘：【当前 pet 各个数值】（完全还原手绘中央大圆）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(310.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFFFF9F0), Color(0xFFFFECE5))
                    )
                )
                .border(6.dp, Color(0xFFFFD5CD), CircleShape)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // 小狗头像与等级
                Text(
                    text = "🐶",
                    fontSize = 58.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "线条小狗 · Lv.${petStats.level}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF263238)
                )
                Text(
                    text = if (coupleProfile.isPaired) "💕 双人共同抚养中" else "🐾 单人陪伴中",
                    fontSize = 11.sp,
                    color = Color(0xFF78909C),
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                // 核心三大数值显示
                CircularStatProgress("🍖 饱食度", petStats.hunger, Color(0xFFFF7043))
                Spacer(modifier = Modifier.height(4.dp))
                CircularStatProgress("❤️ 心情值", petStats.mood, Color(0xFFE91E63))
                Spacer(modifier = Modifier.height(4.dp))
                CircularStatProgress("🛁 清洁度", petStats.clean, Color(0xFF29B6F6))
            }
        }

        Spacer(modifier = Modifier.weight(0.2f))

        // 底部提示
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Text(text = "💡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "点击左上角「三」菜单，可快速唤醒桌面小狗、进行投喂或绑定情侣！",
                    fontSize = 12.sp,
                    color = Color(0xFF546E7A)
                )
            }
        }
    }
}

@Composable
private fun CircularStatProgress(label: String, value: Int, color: Color) {
    val progress by animateFloatAsState(targetValue = value / 100f, label = "progress")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF455A64))
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = Color(0xFFEEEEEE),
            modifier = Modifier
                .width(110.dp)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Text(
            text = "$value%",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF263238),
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}
