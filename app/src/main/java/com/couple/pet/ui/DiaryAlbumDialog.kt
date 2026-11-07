package com.couple.pet.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.couple.pet.model.DiaryEntry
import com.couple.pet.model.DiaryManager
import com.couple.pet.model.DiarySeason
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.ui.theme.PetColors

/**
 * 120 篇秘密回忆相册主界面（四季分类相册集）
 */
@Composable
fun DiaryAlbumDialog(onDismiss: () -> Unit) {
    val entries by DiaryManager.entries.collectAsState()
    var selectedSeason by remember { mutableStateOf(DiarySeason.SPRING) }
    var viewingEntry by remember { mutableStateOf<DiaryEntry?>(null) }

    val filteredEntries = entries.filter { it.season == selectedSeason }
    val unlockedCount = entries.count { it.isUnlocked }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = PetColors.Background,
            border = BorderStroke(2.dp, PetColors.CardBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. 顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📖 恋人秘密回忆日记",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PetColors.TextMain
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PetColors.PrimaryContainer,
                                border = BorderStroke(1.dp, PetColors.PrimaryBorder)
                            ) {
                                Text(
                                    text = "$unlockedCount/${entries.size} 篇",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PetColors.Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "“将我们与线条小狗的每一个瞬间，印成永不褪色的手绘相纸”",
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

                Spacer(modifier = Modifier.height(14.dp))

                // 2. 四季时代导航胶囊条
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DiarySeason.values().forEach { season ->
                        val isSelected = season == selectedSeason
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PetColors.Primary else PetColors.Surface,
                            border = BorderStroke(1.dp, if (isSelected) PetColors.Primary else PetColors.CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSeason = season }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${season.emoji} ${season.title.take(2)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else PetColors.TextMain
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. 对应季度的日记列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEntries) { entry ->
                        DiaryCardItem(
                            entry = entry,
                            onClick = {
                                if (entry.isUnlocked) {
                                    viewingEntry = entry
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 底部提示
                Text(
                    text = "💡 日常喂养、达成等级与纪念日将自然解锁新相纸，点击相纸背面可书写双方留白悄悄话",
                    fontSize = 10.5.sp,
                    color = PetColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // 4. 拟真 3D 翻转相纸模态弹窗
    if (viewingEntry != null) {
        FlipPolaroidCardDialog(
            entry = viewingEntry!!,
            onDismiss = { viewingEntry = null },
            onSaveNotes = { noteA, noteB ->
                DiaryManager.updateNotes(viewingEntry!!.id, noteA, noteB)
            }
        )
    }
}

/**
 * 单张日记卡片
 */
@Composable
private fun DiaryCardItem(
    entry: DiaryEntry,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (entry.isUnlocked) PetColors.Surface else PetColors.SurfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (entry.isUnlocked) PetColors.CardBorder else PetColors.Divider),
        shadowElevation = if (entry.isUnlocked) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.isUnlocked, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标徽章
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (entry.isUnlocked) PetColors.PrimaryContainer else PetColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (entry.isUnlocked) {
                    Text(text = entry.emoji, fontSize = 22.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "未解锁",
                        tint = PetColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isUnlocked) PetColors.TextMain else PetColors.TextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "#${entry.id.toString().padStart(3, '0')}",
                        fontSize = 10.sp,
                        color = PetColors.TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (entry.isUnlocked) entry.dateText else "🔒 解锁条件: ${entry.unlockConditionDesc}",
                    fontSize = 11.sp,
                    color = if (entry.isUnlocked) PetColors.Primary else PetColors.TextSub,
                    maxLines = 1
                )

                if (entry.isUnlocked) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.sceneDescription,
                        fontSize = 11.sp,
                        color = PetColors.TextSub,
                        maxLines = 1,
                        lineHeight = 15.sp
                    )
                }
            }

            if (entry.isUnlocked) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PetColors.PrimaryContainer,
                    border = BorderStroke(1.dp, PetColors.PrimaryBorder)
                ) {
                    Text(
                        text = "翻阅 >",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PetColors.Primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 拟真 3D 翻转拍立得相纸弹窗（正面手绘剧照，反面双人手写信）
 */
@Composable
fun FlipPolaroidCardDialog(
    entry: DiaryEntry,
    onDismiss: () -> Unit,
    onSaveNotes: (String, String) -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }
    var noteA by remember { mutableStateOf(entry.noteA) }
    var noteB by remember { mutableStateOf(entry.noteB) }
    val coupleProfile by PetSyncEngine.coupleProfile.collectAsState()

    // 沿 Y 轴 180 度翻转动效
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "PolaroidFlipAnimation"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (rotation <= 90f) PetColors.Surface else Color(0xFFFBF6EC)),
                border = BorderStroke(1.5.dp, PetColors.CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 460.dp, max = 560.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp))
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
            ) {
                if (rotation <= 90f) {
                    // ================= 正面: 拍立得手绘剧照 =================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 顶部小红把手与关闭
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🐾 拍立得纪念相纸 #${entry.id.toString().padStart(3, '0')}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetColors.TextSub
                            )
                            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = PetColors.TextMain,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 照片手绘主图区
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PetColors.Background)
                                .border(1.dp, PetColors.CardBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = entry.emoji, fontSize = 64.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = entry.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PetColors.TextMain
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 日期与小图戳
                        Text(
                            text = "📅 ${entry.dateText}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PetColors.Primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 情境描写文字
                        Text(
                            text = "“${entry.sceneDescription}”",
                            fontSize = 13.sp,
                            color = PetColors.TextMain,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // 翻转按钮
                        Button(
                            onClick = { isFlipped = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔄 翻转相纸查看双方悄悄话", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    // ================= 反面: 牛皮纸手写双人信 =================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                            .graphicsLayer {
                                rotationY = 180f // 文字反向抵消镜面
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💌 恋人留白悄悄话",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PetColors.TextMain
                            )
                            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = PetColors.TextMain,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "每一张相纸背面，都留存着你们当时的心情",
                            fontSize = 11.sp,
                            color = PetColors.TextSub
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 伴侣 A 留言区
                        OutlinedTextField(
                            value = noteA,
                            onValueChange = { noteA = it },
                            label = { Text("🐾 【${coupleProfile.myNickname}】写道:", fontSize = 12.sp) },
                            placeholder = { Text("写下你当时的心情吧...", fontSize = 11.sp) },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PetColors.Primary,
                                unfocusedBorderColor = PetColors.CardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 伴侣 B 留言区
                        OutlinedTextField(
                            value = noteB,
                            onValueChange = { noteB = it },
                            label = { Text("🐾 【${coupleProfile.partnerNickname}】写道:", fontSize = 12.sp) },
                            placeholder = { Text("伴侣留白区域...", fontSize = 11.sp) },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PetColors.Primary,
                                unfocusedBorderColor = PetColors.CardBorder
                            )
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isFlipped = false },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🔄 翻回正面", fontSize = 12.sp, color = PetColors.TextMain)
                            }

                            Button(
                                onClick = {
                                    onSaveNotes(noteA, noteB)
                                    isFlipped = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("💾 保存悄悄话", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
