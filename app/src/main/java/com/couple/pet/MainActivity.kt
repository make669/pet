package com.couple.pet

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.service.PetFloatingService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFBF8F5),
                ) {
                    PetHomeDashboard(
                        onCheckPermission = { checkOverlayPermission() },
                        onRequestPermission = { requestOverlayPermission() },
                        onStartPet = { startPetService() },
                        onStopPet = { PetFloatingService.stop(this) },
                    )
                }
            }
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri(),
        )
        startActivity(intent)
    }

    private fun startPetService() {
        if (checkOverlayPermission()) {
            PetFloatingService.start(this)
            Toast.makeText(this, "pet 已经跳上你的屏幕啦 🐾", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "请先开启悬浮窗权限哦", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
        }
    }
}

@Composable
fun PetHomeDashboard(
    onCheckPermission: () -> Boolean,
    onRequestPermission: () -> Unit,
    onStartPet: () -> Unit,
    onStopPet: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(onCheckPermission()) }
    val petStats by PetSyncEngine.petStats.collectAsState()
    val coupleProfile by PetSyncEngine.coupleProfile.collectAsState()
    var inputCode by remember { mutableStateOf("") }

    // 自动感知从系统权限设置页返回 app 时的权限变更
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = onCheckPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 标题区
        Text(
            text = "🐾 pet 桌面宠物",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF37474F),
        )
        Text(
            text = "双端云同步 · 情侣共同抚养",
            fontSize = 14.sp,
            color = Color(0xFF78909C),
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        // 1. 悬浮窗开关与权限卡片
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "桌面常驻控制",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF263238),
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!hasPermission) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("⚠️ 点击授权悬浮窗权限")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = onStartPet,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("唤醒桌宠")
                        }
                        OutlinedButton(
                            onClick = onStopPet,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("收起桌宠")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 核心数值与升级进阶卡片
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "宠物状态看板 (Lv.${petStats.level})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF263238),
                    )
                    Text(
                        text = petStats.stage.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8E24AA),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 经验条
                Text(
                    text = "EXP: ${petStats.exp} / ${petStats.maxExp}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
                LinearProgressIndicator(
                    progress = { petStats.exp.toFloat() / petStats.maxExp.toFloat() },
                    color = Color(0xFFAB47BC),
                    trackColor = Color(0xFFF3E5F5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .padding(top = 2.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatRow("🍖 饱食度", petStats.hunger, Color(0xFFFF7043))
                StatRow("❤️ 心情值", petStats.mood, Color(0xFFE91E63))
                StatRow("🛁 清洁度", petStats.clean, Color(0xFF29B6F6))

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FilledTonalButton(onClick = { PetSyncEngine.feed(15) }) {
                        Text("喂小鱼干")
                    }
                    FilledTonalButton(onClick = { PetSyncEngine.poke() }) {
                        Text("摸摸头")
                    }
                    FilledTonalButton(onClick = { PetSyncEngine.bath() }) {
                        Text("洗个澡")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 情侣双人网络配对卡片
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💕 情侣云端双人领养",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF263238),
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (coupleProfile.isPaired) {
                    Text(
                        text = "已与伴侣绑定成功！共同抚养中 💕",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "配对码：${coupleProfile.pairCode}",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    Text(
                        text = "专属配对码：${coupleProfile.pairCode}",
                        fontSize = 14.sp,
                        color = Color(0xFF78909C),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        label = { Text("输入 TA 的 6 位配对码") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (PetSyncEngine.pairWithCode(inputCode)) {
                                Toast.makeText(context, "绑定成功！你们可以一起抚养 TA 啦 💕", Toast.LENGTH_SHORT).show()
                                inputCode = ""
                            } else {
                                Toast.makeText(context, "请输入有效配对码哦", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC407A)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("确认绑定，一起养 TA")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun StatRow(label: String, value: Int, progressColor: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, fontSize = 14.sp, color = Color(0xFF455A64))
            Text(text = "$value / 100", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = { value / 100f },
            color = progressColor,
            trackColor = Color(0xFFEEEEEE),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .padding(top = 4.dp),
        )
    }
}
