package com.couple.pet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.couple.pet.ui.theme.PetColors

/**
 * 【我的】个人中心页面
 * 依据手绘与用户要求：预留页面，采用温暖奶油色调
 */
@Composable
fun ProfileScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(PetColors.Background)
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PetColors.PrimaryContainer)
            ) {
                Text(text = "💌", fontSize = 36.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "我的温暖小屋",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PetColors.TextMain
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "（个人档案与情侣纪念空间已就绪，待后续绘制设计）",
                fontSize = 13.sp,
                color = PetColors.TextSub
            )
        }
    }
}
