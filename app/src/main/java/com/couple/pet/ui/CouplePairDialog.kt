package com.couple.pet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.couple.pet.network.PartnerActionType
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.ui.theme.PetColors

/**
 * 【情侣双人实时连线对齐面板】
 * 2D 极简线条风 + 暖调烘焙配色
 * 支持配对码生成/复制/填入绑定、双人实时状态看板、拍一拍互动、仿真伴侣动作触发
 */
@Composable
fun CouplePairDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coupleProfile by PetSyncEngine.coupleProfile.collectAsState()
    val deskNote by PetSyncEngine.deskNote.collectAsState()

    var inputCode by remember { mutableStateOf("") }
    var myNickInput by remember(coupleProfile.myNickname) { mutableStateOf(coupleProfile.myNickname) }
    var partnerNickInput by remember(coupleProfile.partnerNickname) { mutableStateOf(coupleProfile.partnerNickname) }
    var showUnpairConfirm by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = PetColors.Surface,
            border = androidx.compose.foundation.BorderStroke(2.dp, PetColors.Primary),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (coupleProfile.isPaired) "💕 双人甜蜜连线中" else "💕 情侣领养连线",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PetColors.TextMain
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = PetColors.TextSub
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. 双人头像交叠与连线态卡片
                DualAvatarConnectionHeader(
                    isPaired = coupleProfile.isPaired,
                    myNick = coupleProfile.myNickname,
                    partnerNick = coupleProfile.partnerNickname
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!coupleProfile.isPaired) {
                    // ================= 未连线模式 (输入配对码) =================
                    // 我的专属配对码卡片
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PetColors.PrimaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "你的专属配对码",
                                fontSize = 12.sp,
                                color = PetColors.TextSub
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = coupleProfile.pairCode,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PetColors.Primary,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("CouplePairCode", coupleProfile.pairCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "配对码已复制到剪贴板 🐾", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "复制",
                                        tint = PetColors.Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "复制配对码发给 TA，或在下方输入 TA 的码",
                                fontSize = 11.sp,
                                color = PetColors.TextSub
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 输入 TA 的配对码
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        label = { Text("输入 TA 的专属配对码") },
                        placeholder = { Text("例如：PET-520") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PetColors.Primary,
                            unfocusedBorderColor = PetColors.CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 快速填入测试按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "💡 填入测试码: PET-520",
                            fontSize = 11.5.sp,
                            color = PetColors.Primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { inputCode = "PET-520" }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 自定义称呼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = myNickInput,
                            onValueChange = { myNickInput = it },
                            label = { Text("我的昵称") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = partnerNickInput,
                            onValueChange = { partnerNickInput = it },
                            label = { Text("TA的昵称") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val targetCode = inputCode.ifBlank { "PET-520" }
                            if (PetSyncEngine.pairWithCode(targetCode, myNickInput, partnerNickInput)) {
                                Toast.makeText(context, "🎉 连线成功！与【${partnerNickInput.ifBlank { "小兔" }}】共同领养了萌宠", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "请输入至少4位的有效配对码", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("❤️ 立即甜蜜连线", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                } else {
                    // ================= 已连线模式 (伴侣看板与互动) =================
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = PetColors.SurfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${coupleProfile.partnerNickname} 在线陪伴中",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PetColors.TextMain
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PetColors.PrimaryContainer,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = "🔋 ${coupleProfile.partnerBattery}%${if (coupleProfile.isPartnerCharging) " ⚡" else ""}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PetColors.Primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = PetColors.CardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📅 相伴相恋：${coupleProfile.daysTogether} 天",
                                    fontSize = 12.sp,
                                    color = PetColors.TextSub
                                )
                                Text(
                                    text = "🔗 专属码: ${coupleProfile.pairCode}",
                                    fontSize = 12.sp,
                                    color = PetColors.TextSub
                                )
                            }

                            // 桌面悄悄话摘要
                            Text(
                                text = "💌 桌面悄悄话: \"$deskNote\"",
                                fontSize = 11.5.sp,
                                color = PetColors.Primary,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 即时互动快捷按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                PetSyncEngine.sendPokeToPartner()
                                feedbackMessage = "你轻轻戳了戳【${coupleProfile.partnerNickname}】❤️"
                                Toast.makeText(context, feedbackMessage, Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Text("❤️ 拍一拍 TA", fontSize = 13.5.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                val whispers = listOf(
                                    "今天也超级想你呀 ❤️",
                                    "天冷了记得多穿一件衣服 🧣",
                                    "小狗在乖乖等我们回家 🐾",
                                    "下班一起去吃大餐吧 🍲"
                                )
                                val randomWhisper = whispers.random()
                                PetSyncEngine.updateDeskNote("【${coupleProfile.myNickname}】$randomWhisper")
                                feedbackMessage = "已向【${coupleProfile.partnerNickname}】桌面发送便签 💌"
                                Toast.makeText(context, feedbackMessage, Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, PetColors.Primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Text("💌 发悄悄话", fontSize = 13.5.sp, color = PetColors.Primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ================= 伴侣实时模拟测试工具条 (开发与离线检验利器) =================
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PetColors.Background,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🧪 模拟 TA 实时互动 (即时同步测试)：",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetColors.TextSub
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ActionPillButton("🐾 拍你") {
                                    PetSyncEngine.simulatePartnerAction(PartnerActionType.POKE)
                                }
                                ActionPillButton("🍖 喂食") {
                                    PetSyncEngine.simulatePartnerAction(PartnerActionType.FEED)
                                }
                                ActionPillButton("🧹 铲屎") {
                                    PetSyncEngine.simulatePartnerAction(PartnerActionType.CLEAN_POOP)
                                }
                                ActionPillButton("💌 传信") {
                                    PetSyncEngine.simulatePartnerAction(PartnerActionType.NOTE)
                                }
                                ActionPillButton("⚡ 电量") {
                                    PetSyncEngine.simulatePartnerAction(PartnerActionType.BATTERY)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 解除绑定
                    TextButton(
                        onClick = { showUnpairConfirm = true }
                    ) {
                        Text("💔 解除情侣连线", color = Color(0xFFCC5555), fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // 解除绑定二次确认弹窗
    if (showUnpairConfirm) {
        AlertDialog(
            onDismissRequest = { showUnpairConfirm = false },
            title = { Text("解除情侣连线", fontWeight = FontWeight.Bold, color = PetColors.TextMain) },
            text = { Text("解除后小宠将转为单人抚养模式，配对码将保留。确定要断开与【${coupleProfile.partnerNickname}】的连线吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        PetSyncEngine.unpair()
                        showUnpairConfirm = false
                        Toast.makeText(context, "已断开情侣连线", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("确认解除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairConfirm = false }) {
                    Text("取消", color = PetColors.TextSub)
                }
            }
        )
    }
}

@Composable
private fun DualAvatarConnectionHeader(
    isPaired: Boolean,
    myNick: String,
    partnerNick: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 我的头像
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(PetColors.SurfaceVariant)
                    .border(2.dp, PetColors.Primary, CircleShape)
            ) {
                Text("🐻", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(myNick, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PetColors.TextMain)
            Text("我", fontSize = 10.sp, color = PetColors.TextSub)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 中间连线爱心
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "爱心",
                tint = if (isPaired) PetColors.Primary else PetColors.CardBorder,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (isPaired) "已连接" else "待连线",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isPaired) PetColors.Primary else PetColors.TextSub
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // TA 的头像
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(PetColors.SurfaceVariant)
                    .border(
                        2.dp,
                        if (isPaired) PetColors.Primary else PetColors.CardBorder,
                        CircleShape
                    )
            ) {
                Text("🐰", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(partnerNick, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PetColors.TextMain)
            Text("TA", fontSize = 10.sp, color = PetColors.TextSub)
        }
    }
}

@Composable
private fun ActionPillButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = PetColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.Primary.copy(alpha = 0.7f)),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PetColors.Primary,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
        )
    }
}
