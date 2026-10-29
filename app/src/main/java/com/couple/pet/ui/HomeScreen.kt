package com.couple.pet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.couple.pet.model.CustomFoodItem
import com.couple.pet.model.PetFoodManager
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.pet.PetRegistry
import com.couple.pet.ui.theme.PetColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 【主页】：依据手绘线稿原型精准构建的三段式极简美学看板
 * 1. 顶部视窗卡片 (Box 1): 拍立得画框，纯正手绘黑白线稿小狗自主漫步巡游
 * 2. 中部信息卡片 (Box 2): 伴侣状态与克制小巧数值胶囊 (数字精简克制，不抢占视觉重心) + 桌面漫步切换
 * 3. 底部投喂餐盘 (Box 3): [ O  O  O ] 三大触控小圆圈，支持自定义食物/零食与趣味投喂互动
 */
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    isPetFloating: Boolean = false,
    onToggleFloatingPet: () -> Unit = {}
) {
    val petStats by PetSyncEngine.petStats.collectAsState()
    val coupleProfile by PetSyncEngine.coupleProfile.collectAsState()
    val activeSlots by PetFoodManager.activeSlots.collectAsState()
    val activePet by PetRegistry.activePet.collectAsState()

    var feedbackTip by remember { mutableStateOf<String?>(null) }
    var feedTrigger by remember { mutableLongStateOf(0L) }
    var lastFedFoodName by remember { mutableStateOf("") }
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // 辅助触发气泡提示
    fun showToast(msg: String) {
        feedbackTip = msg
        coroutineScope.launch {
            delay(2000)
            feedbackTip = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PetColors.Background)
    ) {
        // 24 种动物手印足迹温暖印染底纹 (大幅提高清晰度与对比度，清晰立现)
        AnimalFootprintsBackground(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.45f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ================= 顶部操作与品牌栏 =================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 10.dp)
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PetColors.Surface)
                        .border(1.dp, PetColors.CardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "打开功能菜单",
                        tint = PetColors.TextMain,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "🐾 ${activePet.speciesName}伴侣",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PetColors.TextMain
                )

                // 顶部右侧：24种足迹图鉴按钮 + 陪伴状态胶囊
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PetColors.SurfaceVariant.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showTracksDialog = true }
                    ) {
                        Text(
                            text = "🐾 24种足迹",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = PetColors.TextMain,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(PetColors.PrimaryContainer)
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (coupleProfile.isPaired) "💕 双人" else "🐾 单人",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PetColors.Primary
                        )
                    }
                }
            }

        Spacer(modifier = Modifier.height(6.dp))

        // ================= Box 1: 拍立得画框小狗生活视窗 (大圆角矩形) =================
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PetColors.Surface),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(PetColors.CardBorder)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                PolaroidPuppyViewport(
                    isPetFloating = isPetFloating,
                    onPetTouched = {
                        showToast("小狗开心地蹭了蹭你的手掌 ❤️")
                    },
                    feedTrigger = feedTrigger,
                    fedFoodName = lastFedFoodName,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ================= Box 2: 状态注记与精简数值胶囊卡片 (中圆角矩形) =================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PetColors.Surface),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(PetColors.CardBorder)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 第一行：状态标题 + 桌面漫步快捷切换按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${activePet.speciesName} · " + if (isPetFloating) "桌面漫步中" else "相框小憩中",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PetColors.TextMain
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PetColors.SurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Lv.${petStats.level}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetColors.TextSub
                            )
                        }
                    }

                    // 右侧轻柔状态标签
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isPetFloating) PetColors.PrimaryContainer else PetColors.SurfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isPetFloating) "桌面漫步中" else "相框小憩中",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isPetFloating) PetColors.Primary else PetColors.TextSub
                        )
                    }
                }

                // 第二行：温情寄语
                Text(
                    text = if (isPetFloating) {
                        "“小狗正在手机桌面上巡逻玩耍，随时可叫它回家哦～”"
                    } else {
                        "“在拍立得小房间里悠闲漫步，随时可以带它去桌面玩耍~”"
                    },
                    fontSize = 11.sp,
                    color = PetColors.TextSub,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                // 第三行：精炼克制的三色数值胶囊 (克制小字，不喧宾夺主)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactStatPill(
                        icon = "🍖",
                        label = "饱食",
                        value = petStats.hunger,
                        fillColor = PetColors.Hunger,
                        trackColor = PetColors.HungerTrack,
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatPill(
                        icon = "❤️",
                        label = "心情",
                        value = petStats.mood,
                        fillColor = PetColors.Mood,
                        trackColor = PetColors.MoodTrack,
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatPill(
                        icon = "🫧",
                        label = "清洁",
                        value = petStats.clean,
                        fillColor = PetColors.Clean,
                        trackColor = PetColors.CleanTrack,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ================= Box 3: 底部自定义食物与互动小餐盘 [ O  O  O ] =================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PetColors.Surface),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(PetColors.CardBorder)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 托盘标题与自定义入口
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🥣 投喂小餐盘",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PetColors.TextMain
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "即点即喂 · 随时自定义",
                            fontSize = 10.5.sp,
                            color = PetColors.TextMuted
                        )
                    }

                    // ✏️ 自定义管理按钮
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PetColors.SurfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCustomizeDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "自定义食物",
                                tint = PetColors.TextSub,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "自定义",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = PetColors.TextSub
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 原型精髓：三大触控圆圈 [ O    O    O ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val slots = activeSlots.take(3)
                    for (i in 0..2) {
                        val foodItem = slots.getOrNull(i) ?: PetFoodManager.PRESET_LIBRARY[i]
                        CircleFoodButton(
                            food = foodItem,
                            onClick = {
                                PetSyncEngine.feedItem(
                                    name = foodItem.name,
                                    hungerBoost = foodItem.hungerBoost,
                                    moodBoost = foodItem.moodBoost,
                                    cleanBoost = foodItem.cleanBoost
                                )
                                lastFedFoodName = foodItem.name
                                feedTrigger++
                                showToast("投喂了【${foodItem.name}】！${foodItem.buffDescription} 😋")
                            },
                            onLongClick = {
                                showCustomizeDialog = true
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.45f))

        // ================= 核心操作：放桌面漫步 / 叫回相框 切换大按钮 (位于下方空白区域，大小比例优雅舒展) =================
        Button(
            onClick = onToggleFloatingPet,
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPetFloating) PetColors.PrimaryContainer else PetColors.Primary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp),
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(50.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isPetFloating) "🐾 叫${activePet.defaultNick}回相框" else "🐾 放桌面漫步",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPetFloating) PetColors.Primary else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.55f))

        // 浮动交互即时反馈条
        AnimatedVisibility(
            visible = feedbackTip != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            feedbackTip?.let { tip ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(PetColors.TextMain.copy(alpha = 0.88f))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = tip,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    } // 闭合 Column
    } // 闭合底纹 Box

    // ================= 自定义投喂托盘配置弹窗 =================
    if (showCustomizeDialog) {
        CustomFoodTrayDialog(
            activeSlots = activeSlots,
            onDismiss = { showCustomizeDialog = false },
            onSelectSlotFood = { slotIndex, food ->
                PetFoodManager.setSlot(slotIndex, food)
                showToast("已将【${food.name}】装备到槽位 ${slotIndex + 1}")
            },
            onAddCustomFood = { name, emoji, hunger, mood, clean, targetSlot ->
                val newFood = PetFoodManager.addCustomFood(
                    name = name,
                    emoji = emoji,
                    hungerBoost = hunger,
                    moodBoost = mood,
                    cleanBoost = clean,
                    targetSlotIndex = targetSlot
                )
                showToast("自制爱心零食【${newFood.name}】已成功放入餐盘！🎉")
            },
            onResetDefaults = {
                PetFoodManager.resetToDefaults()
                showToast("已恢复默认食物餐盘")
            }
        )
    }

    // ================= 24 种动物足印图鉴弹窗 =================
    if (showTracksDialog) {
        AnimalTracksEncyclopediaDialog(onDismiss = { showTracksDialog = false })
    }
}

/**
 * 24 种动物足印图鉴弹窗
 */
@Composable
private fun AnimalTracksEncyclopediaDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PetColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🐾 24种动物足印图鉴",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PetColors.TextMain
                        )
                        Text(
                            text = "背景印染的萌宠与飞禽走兽手印一览",
                            fontSize = 11.sp,
                            color = PetColors.TextSub
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = PetColors.TextSub,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimalFootprintLibrary.TRACKS.chunked(3).forEach { rowTracks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTracks.forEach { track ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PetColors.SurfaceVariant.copy(alpha = 0.45f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Text(text = track.emoji, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = track.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PetColors.TextMain
                                        )
                                    }
                                }
                            }
                            if (rowTracks.size < 3) {
                                for (k in 0 until (3 - rowTracks.size)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 精巧克制的三维数值微胶囊 (微缩进度条与小字，解决数字过大喧宾夺主问题)
 */
@Composable
private fun CompactStatPill(
    icon: String,
    label: String,
    value: Int,
    fillColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(targetValue = value / 100f, label = "statProgress")

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = PetColors.SurfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = icon, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = PetColors.TextSub,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "$value%",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PetColors.TextMain
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 极简微型纤细进度线 (2.5dp)
            LinearProgressIndicator(
                progress = { progress },
                color = fillColor,
                trackColor = trackColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(1.5.dp))
            )
        }
    }
}

/**
 * 托盘中的大触控圆圈按钮 [ O ]
 */
@Composable
private fun CircleFoodButton(
    food: CustomFoodItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        label = "circleScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        // 圆圈容器
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(PetColors.SurfaceVariant.copy(alpha = 0.6f))
                .border(1.5.dp, PetColors.CardBorder, CircleShape)
                .clickable {
                    onClick()
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = food.emoji,
                    fontSize = 26.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 食物名称
        Text(
            text = food.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PetColors.TextMain,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 增益小标签
        Text(
            text = if (food.hungerBoost > 0) "+${food.hungerBoost}饱食" else if (food.moodBoost > 0) "+${food.moodBoost}心情" else "+${food.cleanBoost}清洁",
            fontSize = 9.sp,
            color = PetColors.Primary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 自定义投喂餐盘管理弹窗 (支持从预设库挑选更换或新建自定义美食)
 */
@Composable
private fun CustomFoodTrayDialog(
    activeSlots: List<CustomFoodItem>,
    onDismiss: () -> Unit,
    onSelectSlotFood: (slotIndex: Int, food: CustomFoodItem) -> Unit,
    onAddCustomFood: (name: String, emoji: String, hunger: Int, mood: Int, clean: Int, targetSlot: Int) -> Unit,
    onResetDefaults: () -> Unit
) {
    var selectedSlotIndex by remember { mutableIntStateOf(0) }
    var isCreatingNew by remember { mutableStateOf(false) }

    // 新建自定义食物表单字段
    var customName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🍖") }
    var customEffectType by remember { mutableStateOf("饱食") } // 饱食, 心情, 清洁

    val emojiCandidates = listOf("🍖", "🥩", "🐟", "🍗", "🍮", "🥛", "🧀", "🍎", "🍰", "🍪", "🥐", "🥣", "🎾", "🧼")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PetColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 弹窗顶栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🥣 自定义投喂餐盘",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PetColors.TextMain
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = PetColors.TextSub,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "点击上方槽位，再选择下方美食即可替换装备到餐盘：",
                    fontSize = 11.5.sp,
                    color = PetColors.TextSub,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // 1. 当前装备的 3 个槽位指示器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0..2) {
                        val food = activeSlots.getOrNull(i) ?: PetFoodManager.PRESET_LIBRARY[i]
                        val isSelected = selectedSlotIndex == i

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) PetColors.PrimaryContainer else PetColors.SurfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) PetColors.Primary else PetColors.CardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clickable { selectedSlotIndex = i }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(text = "槽位 ${i + 1}", fontSize = 10.sp, color = PetColors.TextSub)
                                Text(text = food.emoji, fontSize = 22.sp, modifier = Modifier.padding(vertical = 2.dp))
                                Text(
                                    text = food.name,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PetColors.Primary else PetColors.TextMain,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab 切换：已有备选库 vs 新建自制
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !isCreatingNew,
                        onClick = { isCreatingNew = false },
                        label = { Text("🍰 零食选库", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = isCreatingNew,
                        onClick = { isCreatingNew = true },
                        label = { Text("✨ 自制新食物", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isCreatingNew) {
                    // ================= 模块 A：零食库挑选 =================
                    val allLibrary = PetFoodManager.getAllAvailableFoods()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allLibrary.chunked(2).forEach { rowFoods ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowFoods.forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = PetColors.SurfaceVariant.copy(alpha = 0.35f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                onSelectSlotFood(selectedSlotIndex, item)
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Text(text = item.emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = item.name,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PetColors.TextMain
                                                )
                                                Text(
                                                    text = item.buffDescription,
                                                    fontSize = 9.5.sp,
                                                    color = PetColors.Primary
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowFoods.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    // ================= 模块 B：新建自定义食物 =================
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { if (it.length <= 6) customName = it },
                            placeholder = { Text("输入美食名称（最多6字，如：和牛小丸子）", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "选择图标 Emoji：", fontSize = 11.sp, color = PetColors.TextSub)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            items(emojiCandidates) { emoji ->
                                val isSelected = selectedEmoji == emoji
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) PetColors.PrimaryContainer else PetColors.SurfaceVariant)
                                        .border(
                                            1.dp,
                                            if (isSelected) PetColors.Primary else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { selectedEmoji = emoji }
                                ) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(text = "主要增益属性：", fontSize = 11.sp, color = PetColors.TextSub)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        ) {
                            listOf("饱食", "心情", "清洁").forEach { type ->
                                FilterChip(
                                    selected = customEffectType == type,
                                    onClick = { customEffectType = type },
                                    label = { Text("加$type (+20)", fontSize = 11.sp) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (customName.isNotBlank()) {
                                    val hunger = if (customEffectType == "饱食") 20 else 5
                                    val mood = if (customEffectType == "心情") 20 else 5
                                    val clean = if (customEffectType == "清洁") 25 else 0
                                    onAddCustomFood(
                                        customName,
                                        selectedEmoji,
                                        hunger,
                                        mood,
                                        clean,
                                        selectedSlotIndex
                                    )
                                    customName = ""
                                    isCreatingNew = false
                                }
                            },
                            enabled = customName.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "制作并装备到槽位 ${selectedSlotIndex + 1} 🥣",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 底部轻度重置操作
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onResetDefaults) {
                        Text(
                            text = "重置为默认餐盘",
                            fontSize = 11.sp,
                            color = PetColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}
