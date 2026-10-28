package com.couple.pet.model

/**
 * 宠物当前的物理/运动状态
 */
enum class PetMotionState {
    IDLE,               // 站立发呆/歪头打量
    WALKING,            // 悠闲慢步走动
    RUNNING,            // 撒欢兴奋快跑 (Zoomies)
    SNIFFING,           // 停下低头嗅嗅探险
    SHAKING,            // 全身甩毛抖抖
    CHASING_TAIL,       // 追尾巴打转自娱
    STRETCHING,         // 伸懒腰(下犬式大拉伸)
    YAWNING,            // 打大哈欠
    SCRATCHING,         // 后爪高频挠耳朵
    DIGGING,            // 刨坑扒拉屏幕地面
    BEGGING,            // 抬头大萌眼求摸摸/渴望关注
    NAPPING,            // 蜷成小狗包趴着打瞌睡
    BELLY_UP,           // 翻肚皮求摸摸享受
    CLIMBING,           // 边缘攀爬
    DRAGGED,            // 被手指拎起挣扎扑腾四爪
    FALLING,            // 松手自由落体
    SULKING,            // 背对画圈生闷气
    EATING,             // 吃东西中
    SLEEPING            // 夜间深度睡眠
}

/**
 * 宠物成长与三大核心数值模型
 */
data class PetStats(
    val hunger: Int = 85,          // 饱食度 0~100
    val mood: Int = 90,            // 心情值 0~100
    val clean: Int = 95,           // 清洁度 0~100
    val level: Int = 1,            // 等级
    val exp: Int = 0,              // 当前经验值
    val maxExp: Int = 100,         // 升级所需经验
    val stage: PetGrowthStage = PetGrowthStage.BABY // 当前生命阶段
)

enum class PetGrowthStage(val label: String) {
    BABY("幼崽期 🐾"),       // 幼崽期
    JUVENILE("成长期 🌿"),   // 成长期
    ADULT("成年期 👑")       // 成年期
}

/**
 * 双端情侣互联状态模型
 */
data class CoupleProfile(
    val isPaired: Boolean = false,
    val pairCode: String = "PET-520",
    val myNickname: String = "小熊",
    val partnerNickname: String = "小兔",
    val partnerBattery: Int = 100,
    val isPartnerCharging: Boolean = false,
    val isPartnerOnline: Boolean = true
)
