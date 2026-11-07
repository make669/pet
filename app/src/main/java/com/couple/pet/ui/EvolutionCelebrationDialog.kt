package com.couple.pet.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.couple.pet.model.PetGrowthStage
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.ui.theme.PetColors
import kotlin.math.sin

/**
 * 【形态蜕变与进化典藏仪式弹窗】
 * 2D 极简手绘暖风 + 拍立得照片卡交付 + 金光闪耀粒子动效
 */
@Composable
fun EvolutionCelebrationDialog(
    stage: PetGrowthStage,
    onDismiss: () -> Unit
) {
    val coupleProfile by PetSyncEngine.coupleProfile.collectAsState()
    val infiniteTransition = rememberInfiniteTransition(label = "EvolveGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // 拍立得手绘典藏卡
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PetColors.Background,
                border = androidx.compose.foundation.BorderStroke(2.5.dp, PetColors.Primary),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. 顶部光环标题
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "✨ 奇迹蜕变 · 小宠进化啦！",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PetColors.Primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. 拍立得相纸框内小宠与新形态大徽章
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.5.dp, PetColors.CardBorder, RoundedCornerShape(16.dp))
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 动态发光徽章
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(PetColors.SurfaceVariant.copy(alpha = 0.6f))
                                    .border(2.dp, PetColors.Primary, CircleShape),
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
                                    fontSize = 42.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 新形态标题
                            Text(
                                text = stage.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PetColors.TextMain
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 阶段特征简述
                            Text(
                                text = stage.description,
                                fontSize = 13.sp,
                                color = PetColors.TextSub,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // 拍立得手写签名栏
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🐾 ${coupleProfile.myNickname} ❤️ ${coupleProfile.partnerNickname}",
                                    fontSize = 11.5.sp,
                                    color = PetColors.TextSub,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "相伴第 ${coupleProfile.daysTogether} 天",
                                    fontSize = 11.sp,
                                    color = PetColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. 庆祝确认大按钮
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "开心地抱起【${stage.shortLabel}】小狗 ❤️",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
