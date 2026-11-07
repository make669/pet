package com.couple.pet.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.couple.pet.model.PetGrowthStage
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.ui.theme.PetColors

/**
 * 【形态进化图谱与性格偏好抽屉】
 * 查看 7 种生命形态蜕变树、3 维性格偏好积分与实时形态变幻试衣镜
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolutionPreviewSheet(
    onDismiss: () -> Unit
) {
    val stats by PetSyncEngine.petStats.collectAsState()
    val affinity = stats.affinity
    val currentStage = stats.stage

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PetColors.Background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 顶部标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🐾 线条小狗 · 形态进化图谱",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PetColors.TextMain
                    )
                    Text(
                        text = "当前等级: Lv.${stats.level} · 当前形态: ${currentStage.title}",
                        fontSize = 12.sp,
                        color = PetColors.Primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = PetColors.TextSub)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. 性格三维倾向积分卡
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PetColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "✨ 双人日常抚育偏好 (Lv.10 决定觉醒形态)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PetColors.TextMain
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AffinityBar(
                        icon = "🥐",
                        name = "食神偏好",
                        points = affinity.foodiePoints,
                        desc = "日常投喂大餐/零食偏向",
                        color = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AffinityBar(
                        icon = "⚡",
                        name = "活力偏好",
                        points = affinity.sportyPoints,
                        desc = "陪玩抛接/快跑互动偏向",
                        color = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AffinityBar(
                        icon = "🎩",
                        name = "优雅偏好",
                        points = affinity.fluffyPoints,
                        desc = "洗澡香香/清洁打扫偏向",
                        color = Color(0xFFE91E63)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 7 种生命形态列表与变幻试衣镜
            Text(
                text = "👑 全形态进化树 (点击可立即变幻试衣体验):",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = PetColors.TextMain
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PetGrowthStage.values()) { stage ->
                    val isCurrent = stage == currentStage
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrent) PetColors.Primary.copy(alpha = 0.08f) else PetColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isCurrent) 1.5.dp else 1.dp,
                            if (isCurrent) PetColors.Primary else PetColors.CardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                PetSyncEngine.debugSetGrowthStage(stage, triggerCelebration = true)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 阶段序号圆点
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) PetColors.Primary else PetColors.SurfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (stage) {
                                        PetGrowthStage.BABY -> "🍼"
                                        PetGrowthStage.CHILD -> "🎒"
                                        PetGrowthStage.AWAKEN_FOODIE -> "🥐"
                                        PetGrowthStage.AWAKEN_SPORTY -> "⚡"
                                        PetGrowthStage.AWAKEN_FLUFFY -> "🎩"
                                        PetGrowthStage.ADULT_GUARDIAN -> "👑"
                                        PetGrowthStage.MYTHIC_ANGEL -> "🕊️"
                                    },
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stage.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) PetColors.Primary else PetColors.TextMain
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lv.${stage.minLevel}+",
                                        fontSize = 10.sp,
                                        color = PetColors.TextSub
                                    )
                                }
                                Text(
                                    text = stage.description,
                                    fontSize = 11.sp,
                                    color = PetColors.TextSub,
                                    lineHeight = 15.sp
                                )
                            }

                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "当前形态",
                                    tint = PetColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun AffinityBar(
    icon: String,
    name: String,
    points: Int,
    desc: String,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PetColors.TextMain
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "($desc)", fontSize = 10.sp, color = PetColors.TextSub)
            }
            Text(
                text = "${points} 分",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (points / 50f).coerceIn(0.05f, 1f) },
            color = color,
            trackColor = PetColors.SurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}
