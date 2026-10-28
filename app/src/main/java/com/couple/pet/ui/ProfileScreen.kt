package com.couple.pet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 【我的】个人中心页面
 * 依据用户设计要求：暂时预留空白页，后续扩展情侣档案与设置
 */
@Composable
fun ProfileScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFBF8F5))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "💌 我的小屋",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90A4AE)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "（个人与情侣空间已预留，待后续规划绘制）",
                fontSize = 13.sp,
                color = Color(0xFFB0BEC5)
            )
        }
    }
}
