package com.couple.pet.ui

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.couple.pet.audio.SynesthesiaOrchestrator
import com.couple.pet.model.InteractiveToy
import com.couple.pet.model.ToyCategory
import com.couple.pet.model.ToyManager
import com.couple.pet.ui.theme.PetColors

/**
 * 微缩家园 30 种玩具百宝箱弹窗 (Interactive Toy Catalog Dialog)
 */
@Composable
fun InteractiveToyDialog(
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ToyCategory?>(null) }
    var selectedToy by remember { mutableStateOf<InteractiveToy?>(ToyManager.activeToy.value ?: ToyManager.TOY_CATALOG[0]) }
    val activePlacedToy by ToyManager.activeToy.collectAsState()

    val filteredToys = remember(selectedCategory) {
        if (selectedCategory == null) ToyManager.TOY_CATALOG
        else ToyManager.TOY_CATALOG.filter { it.category == selectedCategory }
    }

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
            border = androidx.compose.foundation.BorderStroke(2.dp, PetColors.CardBorder),
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
                                text = "🧸 微缩家园 · 玩具百宝箱",
                                fontSize = 18.sp,
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
                                    text = "30 件全收录",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PetColors.Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "“让每一件小玩具，都充满真实的触觉与物理弹性”",
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

                Spacer(modifier = Modifier.height(12.dp))

                // 2. 四大分类筛选胶囊条
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedCategory == null) PetColors.Primary else PetColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedCategory == null) PetColors.Primary else PetColors.CardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = null }
                    ) {
                        Text(
                            text = "全部 30 件",
                            fontSize = 11.sp,
                            fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedCategory == null) Color.White else PetColors.TextMain,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    ToyCategory.values().forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PetColors.Primary else PetColors.Surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PetColors.Primary else PetColors.CardBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = "${cat.emoji} ${cat.label}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else PetColors.TextMain,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. 当前选定玩具的特写物理详情卡片
                if (selectedToy != null) {
                    val toy = selectedToy!!
                    val isPlaced = activePlacedToy?.id == toy.id

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = PetColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PetColors.PrimaryBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 玩具大图标与试试手感按钮
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFF7ED))
                                        .border(1.dp, PetColors.CardBorder, CircleShape)
                                        .clickable {
                                            // 激发感官联觉
                                            SynesthesiaOrchestrator.emit(toy.synesthesiaEvent)
                                        }
                                ) {
                                    Text(text = toy.emoji, fontSize = 30.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "👆 点我试触感",
                                    fontSize = 9.sp,
                                    color = PetColors.Primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // 玩具物理参数
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = toy.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PetColors.TextMain
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = toy.materialType,
                                        fontSize = 10.5.sp,
                                        color = PetColors.TextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = toy.description,
                                    fontSize = 11.sp,
                                    color = PetColors.TextSub,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("弹性: ${toy.bounciness}", fontSize = 10.sp, color = PetColors.Primary)
                                    Text("质量: ${toy.massKg}kg", fontSize = 10.sp, color = PetColors.TextMuted)
                                }
                            }

                            // 摆放/撤回行动按钮
                            Button(
                                onClick = {
                                    if (isPlaced) {
                                        ToyManager.removeToy()
                                    } else {
                                        ToyManager.placeToy(toy)
                                        SynesthesiaOrchestrator.emit(toy.synesthesiaEvent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaced) PetColors.SurfaceVariant else PetColors.Primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isPlaced) "已在小窝" else "摆放到小窝",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlaced) PetColors.TextMain else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. 玩具列表滚动视图
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredToys) { toy ->
                        val isSelected = selectedToy?.id == toy.id
                        val isPlaced = activePlacedToy?.id == toy.id

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) PetColors.PrimaryContainer else PetColors.Surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) PetColors.PrimaryBorder else PetColors.CardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedToy = toy
                                    SynesthesiaOrchestrator.emit(toy.synesthesiaEvent)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(toy.emoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = toy.name,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PetColors.TextMain
                                        )
                                        if (isPlaced) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = PetColors.Primary
                                            ) {
                                                Text(
                                                    text = "小窝中",
                                                    fontSize = 9.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = toy.description,
                                        fontSize = 10.5.sp,
                                        color = PetColors.TextSub,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "${toy.bounciness}e",
                                    fontSize = 11.sp,
                                    color = PetColors.TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
