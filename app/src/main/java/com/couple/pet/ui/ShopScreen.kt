package com.couple.pet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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

data class ShopItem(
    val id: String,
    val name: String,
    val category: String, // "pet", "食物", "装扮", "道具"
    val iconEmoji: String,
    val description: String,
    val price: Int,
    val onBuy: () -> Unit
)

@Composable
fun ShopScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    val categories = listOf("全部", "pet", "食物", "装扮", "道具")

    val allItems = remember {
        listOf(
            ShopItem("1", "鲜香小鱼干", "食物", "🐟", "饱食度 +20，小狗最爱", 10) { PetSyncEngine.feed(20) },
            ShopItem("2", "豪华牛肉罐头", "食物", "🍖", "饱食度 +45，经验 +15", 25) { PetSyncEngine.feed(45) },
            ShopItem("3", "甜心甜甜圈", "食物", "🍩", "心情值 +30，情侣甜蜜分享", 15) { PetSyncEngine.poke() },
            ShopItem("4", "泡泡沐浴露", "道具", "🧼", "清洁度直接拉满 100", 20) { PetSyncEngine.bath() },
            ShopItem("5", "经典线条小狗", "pet", "🐶", "当前正在陪伴的呆萌小白狗", 0) {},
            ShopItem("6", "憨厚小柴犬", "pet", "🐕", "元气满满的忠诚柴柴", 100) {},
            ShopItem("7", "软萌垂耳兔", "pet", "🐰", "耳朵软软的胆小小兔", 120) {},
            ShopItem("8", "绅士红色领结", "装扮", "🎀", "系在小狗胸前，可爱度翻倍", 30) {},
            ShopItem("9", "夏日小草帽", "装扮", "👒", "遮阳出游必备，悠闲度日", 35) {},
            ShopItem("10", "小恐龙背包", "装扮", "🎒", "背在身后，像要去上幼儿园", 50) {}
        )
    }

    val filteredItems = allItems.filter { item ->
        (selectedCategory == "全部" || item.category == selectedCategory) &&
                (searchQuery.isBlank() || item.name.contains(searchQuery) || item.description.contains(searchQuery))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFBF8F5))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 1. 顶部标题与搜索框 (完全对应手绘原型顶部的搜索框)
        Text(
            text = "🐾 宠物萌物商店",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF263238),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索食物、装扮或新宠物...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFFFF8DA1),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. 横向分类胶囊栏 (对应手绘原型中的 [pet] [食物] [O] [O])
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF263238) else Color.White)
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else Color(0xFF546E7A),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 商品列表展示 (左方块 [ ]，右文字三行，完全对齐手绘原型)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredItems) { item ->
                ShopItemCard(item)
            }
            item {
                Spacer(modifier = Modifier.height(80.dp)) // 避开底部导航栏
            }
        }
    }
}

@Composable
fun ShopItemCard(item: ShopItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 左侧：商品图标方块 [ ]
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF5E6))
            ) {
                Text(text = item.iconEmoji, fontSize = 34.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 中间：三行文字信息 (名称、说明、价格)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF263238)
                )
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = Color(0xFF78909C),
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                Text(
                    text = if (item.price == 0) "已拥有" else "🪙 ${item.price} 金币",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.price == 0) Color(0xFF4CAF50) else Color(0xFFFF7043)
                )
            }

            // 右侧：快捷兑换/使用按钮
            Button(
                onClick = item.onBuy,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (item.price == 0) Color(0xFFE0E0E0) else Color(0xFFFF8DA1)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (item.price == 0) "使用" else "购买",
                    fontSize = 13.sp,
                    color = if (item.price == 0) Color.DarkGray else Color.White
                )
            }
        }
    }
}
