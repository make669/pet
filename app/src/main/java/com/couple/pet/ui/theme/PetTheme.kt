package com.couple.pet.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 治愈系手绘线条小狗专属配色系统 (Warm Milk & Pastel Blossom Palette)
 * 告别生硬高饱和度原色，采用日韩顶尖插画与治愈系萌宠 App 标准色彩规范：
 * - 纯净暖白牛奶底色，温润不刺眼
 * - 肉桂暖桃与珊瑚粉红主色，充满爱意与陪伴感
 * - 清新低饱和马卡龙三原色状态条（蜂蜜金、草莓粉、苏打蓝）
 * - 温暖炭墨黑字阶，保留手绘 line-art 独特质感
 */
object PetColors {
    // 1. 基础页面底色与容器背景
    val Background = Color(0xFFF9F7F4)       // 温暖燕麦奶油白
    val Surface = Color(0xFFFFFFFF)          // 纯白卡片表面
    val SurfaceElevated = Color(0xFFFFFDFB)  // 悬浮微光白
    val SurfaceVariant = Color(0xFFF3EFEA)   // 柔和微灰奶油底
    val Divider = Color(0xFFEDE8E1)          // 极细柔和分隔线
    val CardBorder = Color(0xFFECE7DF)       // 精致卡片边框色

    // 2. 品牌与交互主色 (Coral Rose / 治愈珊瑚粉红)
    val Primary = Color(0xFFFF6584)          // 温暖治愈粉红
    val PrimaryLight = Color(0xFFFF8DA1)     // 柔粉高光
    val PrimaryContainer = Color(0xFFFFF0F3) // 草莓牛奶底衬
    val PrimaryBorder = Color(0xFFFFD8E0)    // 柔粉精致轮廓

    // 3. 手绘墨色与文字阶梯 (Doodle Ink Hierarchy)
    val TextMain = Color(0xFF232529)         // 温暖炭墨黑，柔和高级
    val TextSub = Color(0xFF64748B)          // 铅灰副标题
    val TextMuted = Color(0xFF94A3B8)        // 浅灰说明与占位
    val TextHint = Color(0xFFCBD5E1)         // 弱化提示

    // 4. 宠物数值状态三原色 (Macaron Stat Palette)
    val Hunger = Color(0xFFF59E0B)           // 饱食度：暖蜜金饼干
    val HungerTrack = Color(0xFFFEF3C7)      // 浅蜜奶黄底轨
    val Mood = Color(0xFFFF5277)             // 心情值：草莓甜心粉
    val MoodTrack = Color(0xFFFFE4E9)        // 浅粉奶昔底轨
    val Clean = Color(0xFF0284C7)            // 清洁度：清爽苏打蓝
    val CleanTrack = Color(0xFFE0F2FE)       // 浅蓝苏打底轨

    // 5. 快捷抽屉功能按键专属马卡龙底色 (Drawer Pill Tones)
    val DrawerPet = Color(0xFFFEF3C7)        // 🐾 桌宠唤醒 (暖黄)
    val DrawerFeed = Color(0xFFFFEDD5)       // 🐟 喂小鱼干 (杏橙)
    val DrawerPoke = Color(0xFFFCE7F3)       // ❤️ 摸摸小狗 (柔粉)
    val DrawerBath = Color(0xFFE0F2FE)       // 🛁 洗香香澡 (冰蓝)
    val DrawerPair = Color(0xFFEDE9FE)       // 💕 情侣配对 (香芋紫)

    // 6. 商店分类标签色彩
    val TagSelected = Color(0xFF232529)      // 选中的炭黑胶囊
    val TagUnselected = Color(0xFFFFFFFF)    // 未选中的白底胶囊
    val TagBorder = Color(0xFFE2DDD5)        // 胶囊细腻描边
    val TagTextUnselected = Color(0xFF5B6B7C)// 未选中的文字色
    val PriceBadge = Color(0xFFFFF7ED)       // 金币气泡底色
    val PriceText = Color(0xFFD97706)        // 金币明艳金黄
}
