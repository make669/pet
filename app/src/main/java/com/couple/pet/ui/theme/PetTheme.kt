package com.couple.pet.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 治愈系手绘线条萌宠专属配色系统 (Warm Bakery & Apricot Sunlight Palette)
 * 全面调优为温润惬意的暖色调体系：
 * - 温暖麦香烤奶背景色 (Background)，告别苍白冰冷，充满烘焙小屋阳光感
 * - 温润象牙白相纸卡片 (Surface)，细腻柔滑
 * - 暖柿暖珊瑚主色 (Primary)，温暖、活泼、亲近
 * - 浓郁暖深栗咖文字墨色 (TextMain)，极具手绘温度与绘本文艺感
 */
object PetColors {
    // 1. 基础页面底色与容器背景 (温暖奶茶烘焙暖调)
    val Background = Color(0xFFFAF2E6)       // 温暖麦香奶油黄 (比之前发灰发白的颜色更暖、更治愈)
    val Surface = Color(0xFFFFFDF9)          // 温润象牙白 (暖白，不刺眼)
    val SurfaceElevated = Color(0xFFFFFFFF)  // 悬浮纯净白
    val SurfaceVariant = Color(0xFFF4E8D7)   // 香草烤麦奶润底
    val Divider = Color(0xFFEDE0D1)          // 温暖淡麦柔和分割线
    val CardBorder = Color(0xFFEADBCE)       // 暖卡其精致轮廓边框

    // 2. 品牌与交互主色 (Warm Peach Terracotta / 温暖暖柿色)
    val Primary = Color(0xFFFF6E54)          // 温暖治愈暖柿红 (生动、热情、温暖，比偏冷粉红更温暖治愈)
    val PrimaryLight = Color(0xFFFF927D)     // 柔暖高光
    val PrimaryContainer = Color(0xFFFFEFE9) // 暖杏草莓牛奶底衬
    val PrimaryBorder = Color(0xFFFFD5C8)    // 暖杏精致轮廓

    // 3. 手绘墨色与文字阶梯 (Warm Roasted Espresso)
    val TextMain = Color(0xFF382A22)         // 浓郁暖深栗咖，纯正手绘温情质感
    val TextSub = Color(0xFF7A685D)          // 暖调奶咖灰
    val TextMuted = Color(0xFFA69488)        // 暖饼干浅灰
    val TextHint = Color(0xFFD6C8BC)         // 弱化提示

    // 4. 宠物数值状态 (Warm Macaron Palette)
    val Hunger = Color(0xFFF59E0B)           // 饱食度：暖蜜金饼干
    val HungerTrack = Color(0xFFFEF3C7)      // 浅蜜奶黄底轨
    val Mood = Color(0xFFFF5478)             // 心情值：草莓甜心暖粉
    val MoodTrack = Color(0xFFFFE5EB)        // 浅粉奶昔底轨
    val Clean = Color(0xFF10B981)            // 清洁度：暖薄荷青草绿
    val CleanTrack = Color(0xFFD1FAE5)       // 浅草薄荷底轨

    // 5. 快捷抽屉功能按键专属暖色调
    val DrawerPet = Color(0xFFFDE68A)        // 🐾 桌宠唤醒 (暖麦金)
    val DrawerFeed = Color(0xFFFFD7BA)       // 🐟 喂小鱼干 (烤杏桃)
    val DrawerPoke = Color(0xFFFFDFDF)       // ❤️ 摸摸小狗 (柔暖粉)
    val DrawerBath = Color(0xFFD1FAE5)       // 🛁 洗香香澡 (薄荷绿)
    val DrawerPair = Color(0xFFF0E5FF)       // 💕 情侣配对 (香芋紫)

    // 6. 商店分类标签色彩
    val TagSelected = Color(0xFF382A22)      // 选中的暖栗黑胶囊
    val TagUnselected = Color(0xFFFFFDF9)    // 未选中的暖白底胶囊
    val TagBorder = Color(0xFFEADBCE)        // 胶囊细腻描边
    val TagTextUnselected = Color(0xFF7A685D)// 未选中的文字色
    val PriceBadge = Color(0xFFFFF7ED)       // 金币气泡底色
    val PriceText = Color(0xFFD97706)        // 金币明艳金黄
}
