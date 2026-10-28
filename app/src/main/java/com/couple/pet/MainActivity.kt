package com.couple.pet

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.service.PetFloatingService
import com.couple.pet.ui.HomeScreen
import com.couple.pet.ui.ProfileScreen
import com.couple.pet.ui.ShopScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PetAppRoot(
                    onCheckPermission = { checkOverlayPermission() },
                    onRequestPermission = { requestOverlayPermission() },
                    onStartPet = { startPetService() },
                    onStopPet = { PetFloatingService.stop(this) }
                )
            }
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun startPetService() {
        if (checkOverlayPermission()) {
            PetFloatingService.start(this)
            Toast.makeText(this, "线条小狗跳上你的屏幕啦 🐾", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "请先开启悬浮窗权限哦", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
        }
    }
}

@Composable
fun PetAppRoot(
    onCheckPermission: () -> Boolean,
    onRequestPermission: () -> Unit,
    onStartPet: () -> Unit,
    onStopPet: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 主页, 1: 商店, 2: 我的
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var isPetFloating by remember { mutableStateOf(false) }
    var showPairDialog by remember { mutableStateOf(false) }

    // 抽屉导航：完全对应手绘左上角「三」展开出来的 5 个圆圈功能快捷键
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = Color.White
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "⚙️ 快捷魔盒",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Text(
                    text = "点击小圆圈快速互动",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 20.dp)
                )

                // 5 个功能小圆圈按钮列表（手绘原型 [O O O O O]）
                DrawerCircleActionRow("🐾", if (isPetFloating) "收起桌宠" else "唤醒桌宠") {
                    if (isPetFloating) {
                        onStopPet()
                        isPetFloating = false
                    } else {
                        onStartPet()
                        isPetFloating = true
                    }
                    coroutineScope.launch { drawerState.close() }
                }

                DrawerCircleActionRow("🐟", "喂小鱼干 (+15)") {
                    PetSyncEngine.feed(15)
                    coroutineScope.launch { drawerState.close() }
                }

                DrawerCircleActionRow("❤️", "摸摸小狗 (+10)") {
                    PetSyncEngine.poke()
                    coroutineScope.launch { drawerState.close() }
                }

                DrawerCircleActionRow("🛁", "洗香香澡 (拉满)") {
                    PetSyncEngine.bath()
                    coroutineScope.launch { drawerState.close() }
                }

                DrawerCircleActionRow("💕", "情侣配对码") {
                    showPairDialog = true
                    coroutineScope.launch { drawerState.close() }
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                // 底部 3 大 Tab 导航栏（完全对齐手绘：主页、商店、我的）
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "主页") },
                        label = { Text("主页", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF8DA1),
                            selectedTextColor = Color(0xFFFF8DA1),
                            indicatorColor = Color(0xFFFFF0F3)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "商店") },
                        label = { Text("商店", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF8DA1),
                            selectedTextColor = Color(0xFFFF8DA1),
                            indicatorColor = Color(0xFFFFF0F3)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                        label = { Text("我的", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF8DA1),
                            selectedTextColor = Color(0xFFFF8DA1),
                            indicatorColor = Color(0xFFFFF0F3)
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> HomeScreen(onOpenDrawer = { coroutineScope.launch { drawerState.open() } })
                    1 -> ShopScreen()
                    2 -> ProfileScreen() // 按手绘要求留白
                }
            }
        }
    }

    // 情侣配对弹窗
    if (showPairDialog) {
        val coupleProfile by PetSyncEngine.coupleProfile.collectAsState()
        var inputCode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPairDialog = false },
            title = { Text("💕 情侣双人网络领养", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "专属配对码：${coupleProfile.pairCode}",
                        color = Color(0xFFFF7043),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        label = { Text("输入 TA 的 6 位配对码") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (PetSyncEngine.pairWithCode(inputCode)) {
                            showPairDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8DA1))
                ) {
                    Text("确认绑定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPairDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun DrawerCircleActionRow(emoji: String, title: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        // 圆圈图标 (O)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF2F4))
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF37474F)
        )
    }
}
