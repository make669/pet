package com.couple.pet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.couple.pet.pet.PetRegistry
import com.couple.pet.ui.theme.PetColors

data class ShopItem(
    val id: String,
    val name: String,
    val category: String, // "pet", "食物", "装扮", "道具"
    val iconEmoji: String,
    val description: String,
    val price: Int,
    val onBuy: () -> Unit
)

/**
 * 【商店】：全新日系治愈暖色调重构
 */
@Composable
fun ShopScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    val categories = listOf("全部", "pet", "食物", "装扮", "道具")
    val activePet by PetRegistry.activePet.collectAsState()

    val allItems = remember {
        listOf(
            ShopItem("1", "鲜香小鱼干", "食物", "🐟", "饱食度 +20，小狗最爱", 10) { PetSyncEngine.feed(20) },
            ShopItem("2", "豪华牛肉罐头", "食物", "🍖", "饱食度 +45，经验 +15", 25) { PetSyncEngine.feed(45) },
            ShopItem("3", "甜心甜甜圈", "食物", "🍩", "心情值 +30，情侣甜蜜分享", 15) { PetSyncEngine.poke() },
            ShopItem("4", "泡泡沐浴露", "道具", "🧼", "清洁度直接拉满 100", 20) { PetSyncEngine.bath() },
            ShopItem("5", "经典线条小狗", "pet", "🐶", "黑白治愈系萌萌小白狗", 0) { PetRegistry.switchPet("doodle_dog") },
            ShopItem("6", "软萌线条奶猫", "pet", "🐱", "傲娇揣手手的黑白线条奶猫", 0) { PetRegistry.switchPet("doodle_cat") },
            ShopItem("7", "软萌垂耳小兔", "pet", "🐰", "软糯长垂耳黑白线条兔兔", 0) { PetRegistry.switchPet("doodle_rabbit") },
            ShopItem("8", "绅士红色领结", "装扮", "🎀", "系在宠物胸前，可爱度翻倍", 30) {},
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
            .background(PetColors.Background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 1. 顶部标题与搜索框
        Text(
            text = "🐾 宠物萌物集市",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PetColors.TextMain,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索食物、装扮或新宠物...", color = PetColors.TextMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索", tint = PetColors.TextSub) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PetColors.Surface,
                unfocusedContainerColor = PetColors.Surface,
                focusedBorderColor = PetColors.Primary,
                unfocusedBorderColor = PetColors.CardBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. 横向分类胶囊栏
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) PetColors.TagSelected else PetColors.TagUnselected)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) PetColors.TagSelected else PetColors.TagBorder,
                            shape = CircleShape
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else PetColors.TagTextUnselected,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 商品列表展示
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
    val activePet by PetRegistry.activePet.collectAsState()
    val categoryBg = when (item.category) {
        "食物" -> PetColors.DrawerFeed
        "装扮" -> PetColors.DrawerPoke
        "pet" -> PetColors.DrawerPet
        else -> PetColors.DrawerBath
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PetColors.Surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PetColors.CardBorder)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 左侧：商品图标方块
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(categoryBg)
            ) {
                Text(text = item.iconEmoji, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 中间：文字信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PetColors.TextMain
                )
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = PetColors.TextSub,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )
                // 价格小药丸
                if (item.price == 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECFDF5))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "已拥有",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PetColors.PriceBadge)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🪙 ${item.price} 金币",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PetColors.PriceText
                        )
                    }
                }
            }

            // 右侧：兑换/使用按钮
            val isCurrentPet = item.category == "pet" && activePet.speciesEmoji == item.iconEmoji
            val isPetCategory = item.category == "pet"

            Button(
                onClick = item.onBuy,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isCurrentPet -> PetColors.PrimaryContainer
                        isPetCategory -> PetColors.Primary
                        item.price == 0 -> PetColors.SurfaceVariant
                        else -> PetColors.Primary
                    }
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when {
                        isCurrentPet -> "🐾 陪伴中"
                        isPetCategory -> "✨ 切换陪伴"
                        item.price == 0 -> "已拥有"
                        else -> "购买"
                    },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCurrentPet -> PetColors.Primary
                        isPetCategory -> Color.White
                        item.price == 0 -> PetColors.TextSub
                        else -> Color.White
                    }
                )
            }
        }
    }
}
