package com.couple.pet.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.couple.pet.supabase.AuthState
import com.couple.pet.supabase.SupabaseAuthManager
import com.couple.pet.supabase.SupabaseConfig
import com.couple.pet.ui.theme.PetColors
import kotlinx.coroutines.launch

/**
 * 【云端邮箱登录与注册界面】
 * 2D 极简手绘线条风 + 暖调烘焙配色
 * 支持邮箱注册、密码登录、离线游客模式直达与 Supabase 自定义凭证配置
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onEnterGuestMode: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authState by SupabaseAuthManager.authState.collectAsState()

    var isRegisterMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nicknameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showConfigPanel by remember { mutableStateOf(false) }

    // 自定义 Supabase 配置临时编辑态
    var customUrl by remember { mutableStateOf(SupabaseConfig.currentUrl) }
    var customKey by remember { mutableStateOf(SupabaseConfig.currentAnonKey) }

    // 监听认证成功
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PetColors.Background)
    ) {
        // 背景 24 种萌宠足印底纹
        AnimalFootprintsBackground(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.30f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // 1. 顶部手绘情侣小萌宠头像徽章
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(PetColors.Surface)
                    .border(2.5.dp, PetColors.Primary, CircleShape)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🐻", fontSize = 28.sp)
                    Text("🐾", fontSize = 16.sp, modifier = Modifier.padding(horizontal = 2.dp))
                    Text("🐰", fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "小宠云端小窝",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PetColors.TextMain
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "异地情侣双端连线 · 永久云端漫步",
                fontSize = 12.5.sp,
                color = PetColors.TextSub
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. 主体登录/注册表单卡片 (大圆角白色卡片)
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = PetColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PetColors.CardBorder),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 登录 / 注册 Tab 切换
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PetColors.SurfaceVariant.copy(alpha = 0.6f))
                            .padding(4.dp)
                    ) {
                        TabPill(
                            label = "登录小窝",
                            selected = !isRegisterMode,
                            onClick = { isRegisterMode = false },
                            modifier = Modifier.weight(1f)
                        )
                        TabPill(
                            label = "注册新账号",
                            selected = isRegisterMode,
                            onClick = { isRegisterMode = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 邮箱输入框
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("电子邮箱") },
                        placeholder = { Text("your_name@example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "邮箱", tint = PetColors.Primary)
                        },
                        trailingIcon = {
                            if (emailInput.isNotEmpty()) {
                                IconButton(onClick = { emailInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清空邮箱", tint = PetColors.TextSub)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PetColors.Primary,
                            unfocusedBorderColor = PetColors.CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 密码输入框
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("账户密码") },
                        placeholder = { Text(if (isRegisterMode) "至少 6 位字符" else "请输入密码") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "密码", tint = PetColors.Primary)
                        },
                        trailingIcon = {
                            Row {
                                if (passwordInput.isNotEmpty()) {
                                    IconButton(onClick = { passwordInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清空密码", tint = PetColors.TextSub)
                                    }
                                }
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "查看密码",
                                        tint = PetColors.TextSub
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PetColors.Primary,
                            unfocusedBorderColor = PetColors.CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 注册模式额外字段：昵称
                    if (isRegisterMode) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = nicknameInput,
                            onValueChange = { nicknameInput = it },
                            label = { Text("我的昵称") },
                            placeholder = { Text("例如：小熊") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = "昵称", tint = PetColors.Primary)
                            },
                            trailingIcon = {
                                if (nicknameInput.isNotEmpty()) {
                                    IconButton(onClick = { nicknameInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清空昵称", tint = PetColors.TextSub)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PetColors.Primary,
                                unfocusedBorderColor = PetColors.CardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 快捷填入测试情侣账号胶囊
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PetColors.SurfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    emailInput = "bear@love.com"
                                    passwordInput = "123456"
                                    nicknameInput = "小熊"
                                }
                                .padding(vertical = 6.dp),
                        ) {
                            Text(
                                text = "🐻 快捷填入小熊",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = PetColors.TextSub,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PetColors.SurfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    emailInput = "rabbit@love.com"
                                    passwordInput = "123456"
                                    nicknameInput = "小兔"
                                }
                                .padding(vertical = 6.dp),
                        ) {
                            Text(
                                text = "🐰 快捷填入小兔",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = PetColors.TextSub,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 错误提示条
                    if (authState is AuthState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFECEE),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB3B8)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = (authState as AuthState.Error).message,
                                color = Color(0xFFD32F2F),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 登录 / 注册确认大按钮
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (isRegisterMode) {
                                    SupabaseAuthManager.signUp(emailInput, passwordInput, nicknameInput)
                                } else {
                                    SupabaseAuthManager.signIn(emailInput, passwordInput)
                                }
                            }
                        },
                        enabled = authState !is AuthState.Loading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isRegisterMode) "✨ 立即注册并开启" else "❤️ 登录云端小窝",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. 离线/单人游客模式直达 (避免阻碍体验)
            TextButton(
                onClick = {
                    SupabaseAuthManager.enterGuestMode()
                    onEnterGuestMode()
                }
            ) {
                Text(
                    text = "🐾 先作为离线游客直接体验 >",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = PetColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. 自定义 Supabase 项目凭据展开抽屉
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PetColors.Surface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PetColors.CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showConfigPanel = !showConfigPanel }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("☁️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "云端连接配置 (Supabase URL & Key)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetColors.TextMain
                            )
                        }
                        Icon(
                            imageVector = if (showConfigPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "展开",
                            tint = PetColors.TextSub,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = showConfigPanel) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "可在 Supabase Project Settings -> API 中复制你的专属 Project URL 与 anon public key：",
                                fontSize = 11.sp,
                                color = PetColors.TextSub
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customUrl,
                                onValueChange = { customUrl = it },
                                label = { Text("Supabase Project URL") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customKey,
                                onValueChange = { customKey = it },
                                label = { Text("Anon Public Key") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    SupabaseConfig.saveConfig(customUrl, customKey)
                                    Toast.makeText(context, "云端凭证已更新！", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("保存云端连接设置", fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PetColors.Primary else Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else PetColors.TextSub
            )
        }
    }
}
