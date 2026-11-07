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
    SLEEPING,           // 夜间深度睡眠
    PRANCE_URGENT,      // 憋急小碎步寻隐蔽角落
    POOPING,            // 屏幕边缘深蹲拱背排便（眼神锁定求保护）
    PEEING,             // 抬腿撒尿标记领地
    POST_ZOOMIES,       // 排便一身轻桌面狂欢冲刺
    BUSTED              // 被主人抓现行瞬间石化
}

/**
 * 宠物成长与三大核心数值模型
 */
data class PetStats(
    val hunger: Int = 85,          // 饱食度 0~100
    val mood: Int = 90,            // 心情值 0~100
    val clean: Int = 95,           // 清洁度 0~100
    val coins: Int = 120,          // 铲屎官金币资产 (打扫/摸摸/相伴赚取)
    val level: Int = 1,            // 等级
    val exp: Int = 0,              // 当前经验值
    val maxExp: Int = 100,         // 升级所需经验
    val stage: PetGrowthStage = PetGrowthStage.BABY, // 当前生命阶段
    val affinity: PersonalityAffinity = PersonalityAffinity() // 性格倾向积分
)

/**
 * 宠物成长形态阶段枚举（五大生命周期与三大觉醒分支）
 */
enum class PetGrowthStage(
    val stageIndex: Int,       // 1 ~ 5
    val title: String,         // 显示全名
    val shortLabel: String,    // 简短标签
    val minLevel: Int,         // 达成最低等级
    val description: String    // 阶段特征描述
) {
    BABY(
        stageIndex = 1,
        title = "奶糕幼崽期 🍼",
        shortLabel = "幼崽",
        minLevel = 1,
        description = "糯米团子般软萌，叼着小奶嘴，走两步容易一屁股摔坐"
    ),
    CHILD(
        stageIndex = 2,
        title = "好奇探索期 🎒",
        shortLabel = "探索",
        minLevel = 5,
        description = "背着小草莓挎包，学会轻快跑跳巡逻，充满探索好奇心"
    ),
    AWAKEN_FOODIE(
        stageIndex = 3,
        title = "觉醒·食神圆滚体 🥐",
        shortLabel = "食神",
        minLevel = 10,
        description = "肚皮鼓鼓圆如大福，爱抱大骨头，走起路来duangduang弹动"
    ),
    AWAKEN_SPORTY(
        stageIndex = 3,
        title = "觉醒·活力特技体 ⚡",
        shortLabel = "活力",
        minLevel = 10,
        description = "系着运动小领巾，身手矫健，最爱在屏幕边缘倒立与后空翻"
    ),
    AWAKEN_FLUFFY(
        stageIndex = 3,
        title = "觉醒·优雅蓬蓬体 🎩",
        shortLabel = "蓬蓬",
        minLevel = 10,
        description = "云朵般松软长毛，别着小蝴蝶结，极度粘人，爱翻肚皮求摸摸"
    ),
    ADULT_GUARDIAN(
        stageIndex = 4,
        title = "成熟·守护伴侣犬 👑",
        shortLabel = "守护",
        minLevel = 20,
        description = "体态优雅匀称，佩戴情侣定制对戒项圈，成为安静守护伴侣"
    ),
    MYTHIC_ANGEL(
        stageIndex = 5,
        title = "神化·天使羁绊体 🕊️",
        shortLabel = "天使",
        minLevel = 30,
        description = "展开纯白小天使羽翼，脚踏金色梅花光晕，开启跨屏神迹"
    );

    val label: String get() = title

    companion object {
        // 向后兼容旧常量
        val JUVENILE get() = CHILD
        val ADULT get() = ADULT_GUARDIAN
    }
}

/**
 * 宠物三大性格行为积分倾向
 */
data class PersonalityAffinity(
    val foodiePoints: Int = 0,    // 食神倾向积分 (投喂零食/大餐)
    val sportyPoints: Int = 0,    // 活力倾向积分 (玩耍/快跑/抛接)
    val fluffyPoints: Int = 0     // 优雅倾向积分 (洗澡/梳理/打扫)
) {
    /**
     * 判定当前最优势的觉醒分支
     */
    fun determineBranch(): PetGrowthStage {
        val maxPoints = maxOf(foodiePoints, sportyPoints, fluffyPoints)
        return when {
            maxPoints == foodiePoints -> PetGrowthStage.AWAKEN_FOODIE
            maxPoints == sportyPoints -> PetGrowthStage.AWAKEN_SPORTY
            else -> PetGrowthStage.AWAKEN_FLUFFY
        }
    }
}

/**
 * 双端情侣互联状态模型
 */
data class CoupleProfile(
    val isPaired: Boolean = false,
    val pairCode: String = "PET-520",
    val myNickname: String = "小熊",
    val partnerNickname: String = "小兔",
    val myAvatar: String = "🐻",
    val partnerAvatar: String = "🐰",
    val partnerBattery: Int = 88,
    val isPartnerCharging: Boolean = true,
    val isPartnerOnline: Boolean = true,
    val daysTogether: Int = 128,
    val lastSyncTime: Long = System.currentTimeMillis()
)
