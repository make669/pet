package com.couple.pet.ui

import android.widget.Toast
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.couple.pet.model.InventoryManager
import com.couple.pet.network.PetSyncEngine
import com.couple.pet.pet.PetRegistry
import com.couple.pet.ui.theme.PetColors

data class ShopItem(
    val id: String,
    val name: String,
    val category: String, // "pet", "食物", "装扮", "道具"
    val iconEmoji: String,
    val description: String,
    val price: Int
)

/**
 * 【商店与集市】：资产联动、背包入库与购买闭环
 */
@Composable
fun ShopScreen() {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    val categories = listOf("全部", "食物", "道具", "装扮", "pet")

    val petStats by PetSyncEngine.petStats.collectAsState()
    val activePet by PetRegistry.activePet.collectAsState()
    val inventory by InventoryManager.inventory.collectAsState()
    val ownedOutfits by InventoryManager.ownedOutfits.collectAsState()
    val equippedOutfits by InventoryManager.equippedOutfits.collectAsState()

    val allItems = remember {
        listOf(
            ShopItem("food_fish", "鲜香小鱼干", "食物", "🐟", "饱食度 +15，酥脆可口", 10),
            ShopItem("food_bone", "和牛肉骨棒", "食物", "🍖", "饱食度 +25，耐嚼解馋", 25),
            ShopItem("food_pudding", "草莓甜布丁", "食物", "🍮", "饱食 +8，心情 +25 甜蜜治愈", 15),
            ShopItem("food_milk", "温热鲜羊奶", "食物", "🥛", "饱食 +12，清洁 +5 毛发光亮", 12),
            ShopItem("food_chicken", "金黄大鸡腿", "食物", "🍗", "饱食度 +30，香气四溢", 28),
            ShopItem("food_jerky", "纯肉冻干条", "食物", "🥩", "饱食 +20，心情 +10 原肉原切", 20),
            ShopItem("food_cheese", "香浓奶酪块", "食物", "🧀", "饱食 +18，营养高钙", 18),
            ShopItem("food_soap", "泡泡沐浴露", "道具", "🧼", "清洁度直接拉满 100", 20),
            ShopItem("food_ball", "趣味弹力球", "道具", "🎾", "心情值 +30，追逐跑跳解闷", 18),
            ShopItem("outfit_bow", "绅士红领结", "装扮", "🎀", "系在宠物胸前，可爱度翻倍", 35),
            ShopItem("outfit_hat", "夏日小草帽", "装扮", "👒", "遮阳出游必备，悠闲度日", 45),
            ShopItem("outfit_bag", "小恐龙双肩包", "装扮", "🎒", "背在身后，像要去上幼儿园", 60),
            ShopItem("doodle_dog", "经典线条小狗", "pet", "🐶", "黑白治愈系萌萌小白狗", 0),
            ShopItem("doodle_cat", "傲娇线条奶猫", "pet", "🐱", "傲娇揣手手的黑白线条奶猫", 0),
            ShopItem("doodle_rabbit", "软萌垂耳小兔", "pet", "🐰", "软糯长垂耳黑白线条兔兔", 0)
        )
    }

    val filteredItems = allItems.filter { item ->
        (selectedCategory == "全部" || item.category == selectedCategory) &&
                (searchQuery.isBlank() || item.name.contains(searchQuery) || item.description.contains(searchQuery))
    }

    val totalItemsCount = inventory.values.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PetColors.Background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 1. 顶部标题
        Text(
            text = "🐾 宠物萌物集市",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PetColors.TextMain,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // 2. 资产看板（金币资产 + 背包道具总览 + 赚钱温馨提示）
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PetColors.Surface),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(PetColors.CardBorder)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧资产标签
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 金币胶囊
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PetColors.PriceBadge)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🪙", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${petStats.coins}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetColors.PriceText
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "金币",
                                fontSize = 11.sp,
                                color = PetColors.PriceText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 背包件数胶囊
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PetColors.PrimaryContainer.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎒", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$totalItemsCount",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetColors.Primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "件库存",
                                fontSize = 11.sp,
                                color = PetColors.Primary
                            )
                        }
                    }
                }

                // 右侧温情赚钱指南提示
                Text(
                    text = "打扫便便 / 抚摸赚金币 ✨",
                    fontSize = 11.sp,
                    color = PetColors.TextSub,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 3. 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索美食、玩具、装扮或新宠物...", color = PetColors.TextMuted, fontSize = 13.5.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索", tint = PetColors.TextSub) },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PetColors.Surface,
                unfocusedContainerColor = PetColors.Surface,
                focusedBorderColor = PetColors.Primary,
                unfocusedBorderColor = PetColors.CardBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. 横向分类胶囊栏
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
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else PetColors.TagTextUnselected,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. 商品列表展示
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredItems) { item ->
                ShopItemCard(
                    item = item,
                    inventoryCount = inventory[item.id] ?: 0,
                    isOutfitOwned = ownedOutfits.contains(item.id),
                    isOutfitEquipped = equippedOutfits.contains(item.id),
                    isCurrentPet = item.category == "pet" && activePet.speciesId == item.id,
                    onBuyFoodOrTool = {
                        val success = PetSyncEngine.spendCoins(item.price)
                        if (success) {
                            InventoryManager.addItem(item.id, 1)
                            Toast.makeText(context, "购买成功！${item.name} +1 放入小窝背包 🎒", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "金币不够啦！快去打扫便便、摸摸宠物赚金币吧 🐾", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBuyOutfit = {
                        val success = PetSyncEngine.spendCoins(item.price)
                        if (success) {
                            InventoryManager.buyOutfit(item.id)
                            Toast.makeText(context, "购买成功！${item.name} 已收入小窝衣柜 🎀", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "金币不够啦！快去打扫便便、摸摸宠物赚金币吧 🐾", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onToggleEquipOutfit = {
                        InventoryManager.toggleEquipOutfit(item.id)
                        val nowEquipped = InventoryManager.isOutfitEquipped(item.id)
                        val action = if (nowEquipped) "已佩戴" else "已卸下"
                        Toast.makeText(context, "${item.name} $action ✨", Toast.LENGTH_SHORT).show()
                    },
                    onSwitchPet = {
                        PetRegistry.switchPet(item.id)
                        Toast.makeText(context, "已切换为“${item.name}”陪伴你 🐾", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(80.dp)) // 避开底部导航栏
            }
        }
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    inventoryCount: Int,
    isOutfitOwned: Boolean,
    isOutfitEquipped: Boolean,
    isCurrentPet: Boolean,
    onBuyFoodOrTool: () -> Unit,
    onBuyOutfit: () -> Unit,
    onToggleEquipOutfit: () -> Unit,
    onSwitchPet: () -> Unit
) {
    val categoryBg = when (item.category) {
        "食物" -> PetColors.DrawerFeed
        "装扮" -> PetColors.DrawerPoke
        "pet" -> PetColors.DrawerPet
        else -> PetColors.DrawerBath
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PetColors.Surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(PetColors.CardBorder)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 左侧：商品图标方块
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(categoryBg)
            ) {
                Text(text = item.iconEmoji, fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 中间：文字信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PetColors.TextMain
                )
                Text(
                    text = item.description,
                    fontSize = 11.5.sp,
                    color = PetColors.TextSub,
                    modifier = Modifier.padding(top = 1.dp, bottom = 4.dp)
                )

                // 价格与库存状态小胶囊
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.category == "pet") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFECFDF5))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "永久陪伴",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    } else if (item.category == "装扮" && isOutfitOwned) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFECFDF5))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "衣柜已拥有",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PetColors.PriceBadge)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🪙 ${item.price} 金币",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetColors.PriceText
                            )
                        }
                    }

                    // 如果是食物或道具，显示当前库存
                    if (item.category == "食物" || item.category == "道具") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "库存: $inventoryCount",
                            fontSize = 10.5.sp,
                            color = if (inventoryCount > 0) PetColors.Primary else PetColors.TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 右侧：交互按钮
            when (item.category) {
                "pet" -> {
                    Button(
                        onClick = onSwitchPet,
                        enabled = !isCurrentPet,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCurrentPet) PetColors.PrimaryContainer else PetColors.Primary,
                            disabledContainerColor = PetColors.PrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isCurrentPet) "🐾 陪伴中" else "切换陪伴",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentPet) PetColors.Primary else Color.White
                        )
                    }
                }

                "装扮" -> {
                    if (isOutfitOwned) {
                        OutlinedButton(
                            onClick = onToggleEquipOutfit,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isOutfitEquipped) PetColors.Primary else PetColors.CardBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isOutfitEquipped) PetColors.PrimaryContainer else PetColors.Surface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isOutfitEquipped) "✨ 已佩戴" else "佩戴",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOutfitEquipped) PetColors.Primary else PetColors.TextSub
                            )
                        }
                    } else {
                        Button(
                            onClick = onBuyOutfit,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "购买",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                else -> {
                    // 食物与道具
                    Button(
                        onClick = onBuyFoodOrTool,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PetColors.Primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "购买",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
