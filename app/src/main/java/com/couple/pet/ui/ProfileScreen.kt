package com.couple.pet.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.pet.BasePet
import com.couple.pet.pet.DoodleCatPet
import com.couple.pet.pet.DoodleDogPet
import com.couple.pet.pet.DoodleRabbitPet
import com.couple.pet.pet.PetRegistry
import com.couple.pet.pet.UniversalPetAction
import com.couple.pet.ui.theme.PetColors

/**
 * 【我的】个人中心页面
 * 依据用户手绘线框原型与沟通共识打造：
 * - 顶部中央大圆圈：1(A+B) 活体萌宠 + 底部双人交叠头像徽章
 * - 中间主大卡片容器：5 个功能条目矩形行：
 *   1. 💕 情侣专属羁绊 / 配对码 (双人在线与电量 / 单人模式邀请码)
 *   2. 💌 桌面传信 / 备忘便签 (情侣发桌面悄悄话 / 单人设桌面提醒便签)
 *   3. 📅 纪念日历 / 倒数日 (含打卡日历、倒数日与成长时光轴)
 *   4. 🐾 动物图鉴 (卡片式：喜爱食物、黄金互动时段与24种足迹)
 *   5. ⚙️ 小窝设置与关于 (音效、震动、清理缓存、版本)
 */
@Composable
fun ProfileScreen(
    isPetFloating: Boolean = false,
    onToggleFloatingPet: () -> Unit = {},
    onOpenPairDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val coupleProfile by PetSyncEngine.coupleProfile.collectAsState()
    val petStats by PetSyncEngine.petStats.collectAsState()
    val activePet by PetRegistry.activePet.collectAsState()
    val deskNote by PetSyncEngine.deskNote.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var showEditNicknameDialog by remember { mutableStateOf(false) }
    var showDeskNoteDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showAnimalEncyclopediaDialog by remember { mutableStateOf(false) }
    var showTracksDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var hapticFeedbackEnabled by remember { mutableStateOf(true) }
    var soundEffectEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PetColors.Background)
    ) {
        // 1. 24 种动物足印背景底纹 (高对比暖栗色，保持与主页全屋一致)
        AnimalFootprintsBackground(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.35f
        )

        // 2. 主体纵向内容（按手绘原型：顶部圆圈 + 中间大方框含 5 行）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // ================= 模块 A：顶部大圆圈 1(A+B) =================
            // A: 活体萌宠居中，B: 下缘双人交叠头像/单人徽章
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .wrapContentSize()
                    .clickable { showEditNicknameDialog = true }
            ) {
                // 主大圆圈 (放置活体萌宠)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(PetColors.Surface)
                        .border(2.5.dp, PetColors.Primary, CircleShape)
                ) {
                    // 绘制活生生的 2D 黑白线条小萌宠
                    Canvas(modifier = Modifier.size(86.dp)) {
                        drawIntoCanvas { composeCanvas ->
                            activePet.render(
                                canvas = composeCanvas.nativeCanvas,
                                cx = size.width / 2f,
                                cy = size.height / 2f + 8f,
                                scale = 1.35f,
                                motion = UniversalPetAction.IDLE,
                                tick = 0L,
                                facingRight = true
                            )
                        }
                    }
                }

                // 下边缘双人交叠小徽章 [我] ❤️ [TA] 或单人小徽章
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 10.dp)
                ) {
                    if (coupleProfile.isPaired) {
                        // 双人交叠头像徽章
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(PetColors.Surface)
                                .border(1.dp, PetColors.CardBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(text = "🐻", fontSize = 12.sp)
                            Text(text = "❤️", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 2.dp))
                            Text(text = "🐰", fontSize = 12.sp)
                        }
                    } else {
                        // 单人独享徽章
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(PetColors.Surface)
                                .border(1.dp, PetColors.CardBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(text = "🐻", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "单人陪伴 🐾",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = PetColors.Primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 昵称与相伴天数文字
            Text(
                text = if (coupleProfile.isPaired) "${coupleProfile.myNickname} ＆ ${coupleProfile.partnerNickname}" else coupleProfile.myNickname,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PetColors.TextMain
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (coupleProfile.isPaired) {
                    "与 ${coupleProfile.partnerNickname} 相恋相伴的第 ${coupleProfile.daysTogether} 天 💕"
                } else {
                    "与 ${activePet.speciesName} 独享温馨的第 ${coupleProfile.daysTogether} 天 🐾"
                },
                fontSize = 12.sp,
                color = PetColors.TextSub
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ================= 模块 B：中间大方框容器 (内嵌 5 个矩形条) =================
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PetColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PetColors.CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ---- 第 1 条：情侣专属羁绊 / 配对码 ----
                    ProfileFrameworkRowItem(
                        icon = "💕",
                        title = "情侣专属羁绊",
                        subtitle = if (coupleProfile.isPaired) {
                            "TA在线 · 🔋${coupleProfile.partnerBattery}% · 码: ${coupleProfile.pairCode}"
                        } else {
                            "专属配对码: ${coupleProfile.pairCode} (单人模式)"
                        },
                        badge = if (coupleProfile.isPaired) "已连线" else "去配对",
                        badgeHighlight = !coupleProfile.isPaired,
                        onClick = onOpenPairDialog
                    )

                    // ---- 第 2 条：桌面传信 / 备忘便签 (双模自适应) ----
                    ProfileFrameworkRowItem(
                        icon = "💌",
                        title = if (coupleProfile.isPaired) "桌面悄悄话 (传信)" else "小宠备忘录 (便签)",
                        subtitle = deskNote,
                        badge = if (coupleProfile.isPaired) "发给TA" else "写便签",
                        badgeHighlight = false,
                        onClick = { showDeskNoteDialog = true }
                    )

                    // ---- 第 3 条：纪念日历 / 倒数日 ----
                    ProfileFrameworkRowItem(
                        icon = "📅",
                        title = "纪念日历 / 倒数日",
                        subtitle = "相伴打卡 · 下一个纪念日还有 24 天",
                        badge = "查日历",
                        badgeHighlight = false,
                        onClick = { showCalendarDialog = true }
                    )

                    // ---- 第 4 条：动物图鉴 (卡片式) ----
                    ProfileFrameworkRowItem(
                        icon = "🐾",
                        title = "动物百科图鉴",
                        subtitle = "喜好美食 · 黄金互动时段与足印",
                        badge = "图鉴卡",
                        badgeHighlight = false,
                        onClick = { showAnimalEncyclopediaDialog = true }
                    )

                    // ---- 第 5 条：小窝设置与关于 ----
                    ProfileFrameworkRowItem(
                        icon = "⚙️",
                        title = "小窝设置与关于",
                        subtitle = "清理缓存 · 音效震动 · v2.0",
                        badge = "设置",
                        badgeHighlight = false,
                        onClick = { showSettingsDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 底部留白温馨语
            Text(
                text = "✨ 线条小宠 · 治愈陪伴空间 ✨",
                fontSize = 11.5.sp,
                color = PetColors.TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ================= 弹窗系统 =================

    // 1. 修改昵称与单双人模式弹窗
    if (showEditNicknameDialog) {
        var myNameInput by remember { mutableStateOf(coupleProfile.myNickname) }
        var partnerNameInput by remember { mutableStateOf(coupleProfile.partnerNickname) }
        var isPairedState by remember { mutableStateOf(coupleProfile.isPaired) }

        Dialog(onDismissRequest = { showEditNicknameDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PetColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "✏️ 修改档案与模式",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PetColors.TextMain
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = myNameInput,
                        onValueChange = { myNameInput = it },
                        label = { Text("我的昵称") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PetColors.Primary,
                            unfocusedBorderColor = PetColors.CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = partnerNameInput,
                        onValueChange = { partnerNameInput = it },
                        label = { Text("TA 的昵称 (单人可留空)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PetColors.Primary,
                            unfocusedBorderColor = PetColors.CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 系统自动计算天数提示
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PetColors.SurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("🗓️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "相伴天数：系统自动计时，当前第 ${coupleProfile.daysTogether} 天",
                                fontSize = 11.5.sp,
                                color = PetColors.TextSub
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 切换单人/情侣模式开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("情侣连线模式", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PetColors.TextMain)
                            Text(if (isPairedState) "双人甜蜜连线中 💕" else "当前处于单人独享模式 🐾", fontSize = 11.sp, color = PetColors.TextSub)
                        }
                        Switch(
                            checked = isPairedState,
                            onCheckedChange = { isPairedState = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PetColors.Primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditNicknameDialog = false }) {
                            Text("取消", color = PetColors.TextSub)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                PetSyncEngine.updateNicknames(myNameInput, partnerNameInput)
                                if (isPairedState != coupleProfile.isPaired) {
                                    if (isPairedState) {
                                        PetSyncEngine.pairWithCode("PET-520")
                                    } else {
                                        // 切换单人
                                        PetSyncEngine.pairWithCode("")
                                    }
                                }
                                showEditNicknameDialog = false
                                Toast.makeText(context, "档案更新成功 ❤️", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary)
                        ) {
                            Text("保存", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 2. 桌面传信 / 备忘便签弹窗
    if (showDeskNoteDialog) {
        var inputNote by remember { mutableStateOf(deskNote) }

        Dialog(onDismissRequest = { showDeskNoteDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PetColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                        Text(
                            text = if (coupleProfile.isPaired) "💌 桌面悄悄话 (给TA)" else "📝 小宠备忘便签",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PetColors.TextMain
                        )
                        IconButton(onClick = { showDeskNoteDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = PetColors.TextSub)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (coupleProfile.isPaired) {
                            "发送后，TA 手机桌面上漫步的小宠会顶着爱心气泡弹出这句话哦~"
                        } else {
                            "给自己留一条贴心便签，小宠会在桌面上替你记着并提醒你~"
                        },
                        fontSize = 12.sp,
                        color = PetColors.TextSub
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputNote,
                        onValueChange = { inputNote = it },
                        placeholder = { Text("输入想说的话或待办备忘...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PetColors.Primary,
                            unfocusedBorderColor = PetColors.CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 常用快捷语
                    Text("💡 快捷灵感：", fontSize = 11.5.sp, color = PetColors.TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    val quickTips = if (coupleProfile.isPaired) {
                        listOf("想你啦，小宠正在替我抱你 ❤️", "多喝水哦，今天也要元气满满 ✨", "今晚一起吃好吃的！😋")
                    } else {
                        listOf("多喝热水，今天也要爱自己 ✨", "休息一下，看看窗外绿植 🌿", "今天表现超级棒，辛苦啦！🐾")
                    }

                    quickTips.forEach { tip ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PetColors.SurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { inputNote = tip }
                        ) {
                            Text(
                                text = tip,
                                fontSize = 11.sp,
                                color = PetColors.TextMain,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            PetSyncEngine.updateDeskNote(inputNote)
                            showDeskNoteDialog = false
                            Toast.makeText(context, if (coupleProfile.isPaired) "已发送到 TA 的桌宠气泡 💌" else "已设定桌面小宠提醒 ✨", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (coupleProfile.isPaired) "发送到 TA 桌面" else "保存到桌宠便签", color = Color.White)
                    }
                }
            }
        }
    }

    // 3. 纪念日历 / 倒数日与时光轴弹窗 (一体化卡片，下方回忆录横向滑动，告别过长占屏)
    if (showCalendarDialog) {
        Dialog(onDismissRequest = { showCalendarDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PetColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // 头部标题 + 关闭按钮 (一体化呈现，不分成两页)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📅 纪念日历 · 倒数日",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetColors.TextMain
                            )
                            Text(
                                text = "相伴时光轴与仪式感打卡",
                                fontSize = 11.sp,
                                color = PetColors.TextSub
                            )
                        }
                        IconButton(onClick = { showCalendarDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = PetColors.TextSub)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. 倒数日横幅卡片
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PetColors.PrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (coupleProfile.isPaired) "🎉 距离相恋 200 天纪念日" else "🎉 距离领养 100 天纪念日",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PetColors.TextMain
                                )
                                Text(
                                    text = "下一个重要仪式，小宠会戴上小礼帽哦",
                                    fontSize = 9.5.sp,
                                    color = PetColors.TextSub
                                )
                            }
                            Text(
                                text = "还剩 24 天",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetColors.Primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. 当月打卡日历示意
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🗓️ 2026 年 10 月", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PetColors.TextMain)
                            Text("✨ 已打卡相伴 7 天", fontSize = 10.sp, color = PetColors.Primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        val weekdays = listOf("日", "一", "二", "三", "四", "五", "六")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            weekdays.forEach { w ->
                                Text(w, fontSize = 10.5.sp, color = PetColors.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        val daysSample = (25..31).toList()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            daysSample.forEach { day ->
                                val isToday = day == 29
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (isToday) PetColors.Primary else PetColors.SurfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = if (isToday) "今" else day.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) Color.White else PetColors.TextMain
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. 成长相伴时光轴（横向滑动卡片 LazyRow，一体化且不占高！）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📜 成长相伴时光卡：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PetColors.TextMain)
                        Text("👉 左右滑动查看", fontSize = 10.sp, color = PetColors.TextMuted)
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val milestones = listOf(
                        Triple("🌱 第 1 天", "迎来了属于我们的小小生命【${activePet.speciesName}】", "🏠"),
                        Triple("🐟 第 7 天", "第一次喂它吃美味的小鱼干，开心地转圈圈", "✨"),
                        Triple("💕 第 30 天", if (coupleProfile.isPaired) "双人情侣成功绑定，开始共同喂养" else "小家伙已经完全熟悉小窝，开始呼呼大睡", "❤️"),
                        Triple("👑 第 50 天", "小宠成长升阶！解锁了专属绝活【${activePet.getSpecialTrickName()}】", "🐾"),
                        Triple("🌟 第 ${coupleProfile.daysTogether} 天", "今天也充满爱意，一起继续温馨相伴吧~", "🎉")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(milestones.size) { i ->
                            val (time, desc, emoji) = milestones[i]
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = PetColors.SurfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(82.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(emoji, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(time, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PetColors.Primary)
                                    }
                                    Text(
                                        text = desc,
                                        fontSize = 10.5.sp,
                                        color = PetColors.TextMain,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. 动物图鉴（卡片式）弹窗
    if (showAnimalEncyclopediaDialog) {
        val allPets = remember {
            listOf(
                DoodleDogPet(),
                DoodleCatPet(),
                DoodleRabbitPet()
            )
        }

        Dialog(onDismissRequest = { showAnimalEncyclopediaDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PetColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                                text = "🐾 动物百科图鉴",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetColors.TextMain
                            )
                            Text(
                                text = "左右滑动查看动物喜好与互动时段",
                                fontSize = 11.5.sp,
                                color = PetColors.TextSub
                            )
                        }
                        IconButton(onClick = { showAnimalEncyclopediaDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = PetColors.TextSub)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 横向滑动卡片
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(allPets) { _, pet ->
                            AnimalDossierCard(pet = pet)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 底部 24 种足印图鉴入口
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PetColors.PrimaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAnimalEncyclopediaDialog = false
                                showTracksDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📖", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("查看 24 种动物足印库", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PetColors.TextMain)
                                    Text("包含仓鼠、考拉、树懒、大象手绘足印", fontSize = 10.5.sp, color = PetColors.TextSub)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PetColors.Primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    // 24 种动物足印图鉴弹窗
    if (showTracksDialog) {
        AnimalTracksEncyclopediaDialog(onDismiss = { showTracksDialog = false })
    }

    // 5. 小窝设置与关于弹窗
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = PetColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                        Text(
                            text = "⚙️ 小窝设置与关于",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PetColors.TextMain
                        )
                        IconButton(onClick = { showSettingsDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = PetColors.TextSub)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 触感微震
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("双人互动触感微震", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PetColors.TextMain)
                            Text("TA 摸摸或喂食小宠时轻柔震动", fontSize = 11.sp, color = PetColors.TextSub)
                        }
                        Switch(
                            checked = hapticFeedbackEnabled,
                            onCheckedChange = { hapticFeedbackEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PetColors.Primary
                            )
                        )
                    }

                    // 声音音效
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("投喂与按键萌趣音效", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PetColors.TextMain)
                            Text("清脆快门声与吧唧小吃食声", fontSize = 11.sp, color = PetColors.TextSub)
                        }
                        Switch(
                            checked = soundEffectEnabled,
                            onCheckedChange = { soundEffectEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PetColors.Primary
                            )
                        )
                    }

                    HorizontalDivider(color = PetColors.CardBorder, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // 清理缓存
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                Toast.makeText(context, "已清理 12.8 MB 缓存，小窝清爽飞快 ✨", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("🧹 清理小窝缓存", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PetColors.TextMain)
                            Text("释放动画帧缓存与临时数据 (12.8 MB)", fontSize = 11.sp, color = PetColors.TextSub)
                        }
                        Text("一键清理", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PetColors.Primary)
                    }

                    HorizontalDivider(color = PetColors.CardBorder, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // 版本
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("📱 软件版本", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PetColors.TextMain)
                            Text("线条小宠 · 治愈情侣伴侣版 (纯净无广告)", fontSize = 11.sp, color = PetColors.TextSub)
                        }
                        Text("v2.0.0", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PetColors.TextSub)
                    }
                }
            }
        }
    }
}

/**
 * 动物百科档案卡组件 (按用户要求：记录喜欢吃的美食、黄金互动时段与收益)
 */
@Composable
private fun AnimalDossierCard(pet: BasePet) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PetColors.SurfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
        modifier = Modifier
            .width(220.dp)
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 活体手绘形象
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(PetColors.Surface)
                    .border(1.5.dp, PetColors.CardBorder, CircleShape)
            ) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    drawIntoCanvas { composeCanvas ->
                        pet.render(
                            canvas = composeCanvas.nativeCanvas,
                            cx = size.width / 2f,
                            cy = size.height / 2f + 5f,
                            scale = 0.95f,
                            motion = UniversalPetAction.IDLE,
                            tick = 0L,
                            facingRight = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pet.speciesName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PetColors.TextMain
            )
            Text(
                text = pet.description,
                fontSize = 10.5.sp,
                color = PetColors.TextSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 喜爱的食物
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🍽️ 最爱食物：", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PetColors.TextMain)
                Text(pet.favoriteFood, fontSize = 11.sp, color = PetColors.Primary, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 黄金互动时段
            val goldenHourInfo = when (pet.speciesId) {
                "doodle_dog" -> "☀️ 14:00~16:00 (漫步心情翻倍)"
                "doodle_cat" -> "🌙 20:00~22:00 (巡逻经验+50%)"
                else -> "🌅 08:00~10:00 (清晨清洁度锁满)"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏰ 最佳时段：", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PetColors.TextMain)
            }
            Text(
                text = goldenHourInfo,
                fontSize = 10.5.sp,
                color = PetColors.TextSub,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 专属绝活
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✨ 专属绝活：", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PetColors.TextMain)
                Text(pet.getSpecialTrickName(), fontSize = 10.5.sp, color = PetColors.Primary)
            }
        }
    }
}

/**
 * 原型中大方框内的横向矩形功能条目（标准可点击行）
 */
@Composable
private fun ProfileFrameworkRowItem(
    icon: String,
    title: String,
    subtitle: String,
    badge: String,
    badgeHighlight: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PetColors.SurfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 矩形左侧小圆圈
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(PetColors.Surface)
                        .border(1.dp, PetColors.CardBorder, CircleShape)
                ) {
                    Text(text = icon, fontSize = 17.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PetColors.TextMain
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = PetColors.TextSub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (badgeHighlight) PetColors.Primary else PetColors.Surface,
                    border = if (badgeHighlight) null else androidx.compose.foundation.BorderStroke(0.8.dp, PetColors.CardBorder)
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (badgeHighlight) Color.White else PetColors.TextSub,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = PetColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
