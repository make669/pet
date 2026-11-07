package com.couple.pet.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.ui.theme.PetColors

/**
 * 桌面情侣自律伴学番茄钟弹窗
 */
@Composable
fun FocusPomodoroDialog(onDismiss: () -> Unit) {
    val isPomodoroActive by PetSyncEngine.isPomodoroActive.collectAsState()
    val remainingSeconds by PetSyncEngine.pomodoroRemainingSeconds.collectAsState()
    val totalMinutes by PetSyncEngine.pomodoroTotalMinutes.collectAsState()
    val coupleProfile by PetSyncEngine.coupleProfile.collectAsState()

    var selectedTargetMinutes by remember { mutableIntStateOf(totalMinutes) }

    val totalSeconds = totalMinutes * 60
    val progress = if (totalSeconds > 0) {
        remainingSeconds.toFloat() / totalSeconds.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "PomodoroProgress")

    val minutesPart = remainingSeconds / 60
    val secondsPart = remainingSeconds % 60
    val timeFormatted = "%02d:%02d".format(minutesPart, secondsPart)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = PetColors.Surface,
            border = BorderStroke(1.5.dp, PetColors.CardBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👓", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "伴学专注番茄钟",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PetColors.TextMain
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
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

                // 2. 居中拟真倒计时表盘
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 底轨浅色圆环
                        drawCircle(
                            color = Color(0xFFF3ECE0),
                            radius = size.minDimension / 2 - 12f,
                            style = Stroke(width = 16f)
                        )

                        // 进度前景色圆弧
                        drawArc(
                            color = if (isPomodoroActive) PetColors.Primary else Color(0xFFEADBCE),
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            style = Stroke(width = 16f, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isPomodoroActive) timeFormatted else "%02d:00".format(selectedTargetMinutes),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isPomodoroActive) PetColors.Primary else PetColors.TextMain
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPomodoroActive) "专注自律中..." else "待启动",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PetColors.TextSub
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. 伴侣同频状态胶囊
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PetColors.PrimaryContainer,
                    border = BorderStroke(1.dp, PetColors.PrimaryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "💕", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "伴侣【${coupleProfile.partnerNickname}】: 同步开启可享双倍自律积分！",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PetColors.Primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. 时长选择器（未启动时可切换）
                if (!isPomodoroActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 25, 45).forEach { mins ->
                            val isSelected = selectedTargetMinutes == mins
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PetColors.Primary else PetColors.SurfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) PetColors.Primary else PetColors.CardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTargetMinutes = mins }
                            ) {
                                Text(
                                    text = "${mins} 分钟",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else PetColors.TextMain,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            PetSyncEngine.startPomodoro(selectedTargetMinutes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "🚀 开启伴学专注",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // 已启动状态：暂停与提前终止
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                PetSyncEngine.stopPomodoro()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("⏹️ 放弃专注", fontSize = 13.sp, color = Color(0xFFEF4444))
                        }

                        Button(
                            onClick = {
                                PetSyncEngine.pausePomodoro()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("⏸️ 暂停一下", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "💡 专注期间小狗会戴上黑框眼镜趴在桌角安静看书，完成专注可得丰厚金币、经验并解锁专属回忆日记 #10",
                    fontSize = 10.5.sp,
                    color = PetColors.TextSub,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
