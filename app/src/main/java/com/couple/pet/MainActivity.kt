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
import com.couple.pet.ui.theme.PetColors
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

    fun toggleFloatingPet() {
        if (isPetFloating) {
            onStopPet()
            isPetFloating = false
        } else {
            onStartPet()
            isPetFloating = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = PetColors.Surface
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "⚙️ 快捷魔盒",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PetColors.TextMain,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
                Text(
                    text = "点击小圆圈快速互动",
                    fontSize = 13.sp,
                    color = PetColors.TextSub,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 18.dp)
                )

                // 5 个功能小圆圈按钮列表（手绘原型 [O O O O O]）
                DrawerCircleActionRow("🐾", if (isPetFloating) "收起桌宠" else "唤醒桌宠", PetColors.DrawerPet) {
                    toggleFloatingPet()
                    coroutineScope.launch { drawerState.close() }
                }

                DrawerCircleActionRow("🐟", "喂小鱼干 (+15)", PetColors.DrawerFeed) {
                    PetSyncEngine.feed(15)
                    coroutineScope.launch { drawerState.close() }
                }

                DrawerCircleActionRow("❤️", "摸摸小狗 (+10)", PetColors.DrawerPoke) {
                    PetSyncEngine.poke()
                    coroutineScope.launch { drawerState.close() }
                }

                DrawerCircleActionRow("🛁", "洗香香澡 (拉满)", PetColors.DrawerBath) {
                    PetSyncEngine.bath()
                    coroutineScope.launch { drawerState.close() }
                }

                DrawerCircleActionRow("💕", "情侣配对码", PetColors.DrawerPair) {
                    showPairDialog = true
                    coroutineScope.launch { drawerState.close() }
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = PetColors.Surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "主页") },
                        label = { Text("主页", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PetColors.Primary,
                            selectedTextColor = PetColors.Primary,
                            indicatorColor = PetColors.PrimaryContainer,
                            unselectedIconColor = PetColors.TextMuted,
                            unselectedTextColor = PetColors.TextSub
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "商店") },
                        label = { Text("商店", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PetColors.Primary,
                            selectedTextColor = PetColors.Primary,
                            indicatorColor = PetColors.PrimaryContainer,
                            unselectedIconColor = PetColors.TextMuted,
                            unselectedTextColor = PetColors.TextSub
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                        label = { Text("我的", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PetColors.Primary,
                            selectedTextColor = PetColors.Primary,
                            indicatorColor = PetColors.PrimaryContainer,
                            unselectedIconColor = PetColors.TextMuted,
                            unselectedTextColor = PetColors.TextSub
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        isPetFloating = isPetFloating,
                        onToggleFloatingPet = { toggleFloatingPet() }
                    )
                    1 -> ShopScreen()
                    2 -> ProfileScreen()
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
            shape = RoundedCornerShape(24.dp),
            containerColor = PetColors.Surface,
            title = { Text("💕 情侣双人网络领养", fontWeight = FontWeight.Bold, color = PetColors.TextMain) },
            text = {
                Column {
                    Text(
                        text = "专属配对码：${coupleProfile.pairCode}",
                        color = PetColors.Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        label = { Text("输入 TA 的 6 位配对码") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PetColors.Primary,
                            unfocusedBorderColor = PetColors.CardBorder
                        ),
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
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary)
                ) {
                    Text("确认绑定", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPairDialog = false }) {
                    Text("取消", color = PetColors.TextSub)
                }
            }
        )
    }
}

@Composable
fun DrawerCircleActionRow(emoji: String, title: String, circleBg: Color, onClick: () -> Unit) {
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
                .size(46.dp)
                .clip(CircleShape)
                .background(circleBg)
        ) {
            Text(text = emoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = PetColors.TextMain
        )
    }
}
