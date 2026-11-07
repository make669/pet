package com.couple.pet.model

import com.couple.pet.audio.SynesthesiaEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 玩具分类
 */
enum class ToyCategory(val label: String, val emoji: String) {
    PLAY("运动玩乐", "⚽"),
    RELAX("舒缓睡眠", "💤"),
    SENSORY("益智感官", "🧩"),
    COMPANION("情侣羁绊", "💕")
}

/**
 * 互动玩具模型 (Interactive Toy)
 */
data class InteractiveToy(
    val id: String,
    val name: String,
    val emoji: String,
    val category: ToyCategory,
    val description: String,
    val materialType: String,
    val bounciness: Float, // 恢复系数 e (0.05 ~ 0.95)
    val massKg: Float,     // 物理质量 (kg)
    val synesthesiaEvent: SynesthesiaEvent,
    val isUnlocked: Boolean = true
)

/**
 * 微缩家园 30 种玩具仓库与摆放中枢 (ToyManager)
 */
object ToyManager {
    // 30 种全量玩具图鉴
    val TOY_CATALOG: List<InteractiveToy> = listOf(
        InteractiveToy("yarn_ball", "弹力毛线球", "🧶", ToyCategory.PLAY, "玫瑰粉手绘线团，滚动时拖曳出细细红毛线", "编织纯羊毛", 0.55f, 0.08f, SynesthesiaEvent.YARN_ROLL),
        InteractiveToy("squeaky_duck", "哔啵小黄鸭", "🐥", ToyCategory.SENSORY, "按压瞬间压扁50%，发出清脆欢快啾啾音", "软橡胶", 0.78f, 0.05f, SynesthesiaEvent.SQUEAKY_DUCK),
        InteractiveToy("toast_cushion", "巨型生吐司软垫", "🍞", ToyCategory.RELAX, "厚切生吐司造型，跳上去陷入高斯软坑", "慢回弹记忆棉", 0.05f, 0.60f, SynesthesiaEvent.PET_HEAD),
        InteractiveToy("box_fort", "纸箱秘密城堡", "📦", ToyCategory.PLAY, "瓦楞纸箱改造的小堡垒，露出两双圆豆豆眼", "环保瓦楞纸", 0.12f, 0.45f, SynesthesiaEvent.STAMP_IMPACT),
        InteractiveToy("bubble_wand", "自动吹泡泡机", "🫧", ToyCategory.SENSORY, "持续喷出半透明彩色肥皂泡，戳破激起水花", "复合塑料", 0.20f, 0.25f, SynesthesiaEvent.BUBBLE_POP),
        InteractiveToy("chew_stick", "耐咬天然原木棒", "🪵", ToyCategory.PLAY, "浅栗色原木树枝，抱在怀里用力啃咬出木屑", "天然木材", 0.25f, 0.18f, SynesthesiaEvent.FEED_CRUNCH),
        InteractiveToy("music_box", "复古手摇八音盒", "🎵", ToyCategory.COMPANION, "纯铜手摇柄与齿轮，奏响舒缓治愈催眠和弦", "黄铜与硬木", 0.10f, 0.50f, SynesthesiaEvent.POMODORO_CHIME),
        InteractiveToy("tug_rope", "彩虹拔河棉绳", "🪢", ToyCategory.PLAY, "双宠各叼一端拔河，绳结绷直拉扯出小火花", "粗纤维棉绳", 0.20f, 0.22f, SynesthesiaEvent.COUPLE_SNUGGLE),
        InteractiveToy("laser_pointer", "极光激光笔红点", "🔴", ToyCategory.SENSORY, "地面上随机游走的微光斑，小狗疯狂追逐", "光斑投影", 0.00f, 0.00f, SynesthesiaEvent.BUBBLE_POP),
        InteractiveToy("goose_pillow", "柔软大白鹅抱枕", "🦢", ToyCategory.RELAX, "长脖子大白鹅，小狗把下巴枕在上面打呼噜", "羽绒长毛绒", 0.08f, 0.35f, SynesthesiaEvent.PET_HEAD),
        InteractiveToy("frisbee", "飞旋微风飞盘", "🥏", ToyCategory.PLAY, "平滑滑翔，小狗腾空飞扑至最高点精准嘴接", "柔性工程塑料", 0.62f, 0.12f, SynesthesiaEvent.YARN_ROLL),
        InteractiveToy("trampoline", "迷你跳跳床", "🤾", ToyCategory.PLAY, "高弹性弹簧网布，跳上去产生连续3次递增弹跳", "弹性弹簧网", 0.92f, 1.20f, SynesthesiaEvent.SQUEAKY_DUCK),
        InteractiveToy("polaroid_frame", "情侣拍立得相框", "🖼️", ToyCategory.COMPANION, "木质斜相框，存留两人与小狗最新的合照", "松木与玻璃", 0.05f, 0.20f, SynesthesiaEvent.STAMP_IMPACT),
        InteractiveToy("picnic_mat", "阳光格子野餐垫", "🧺", ToyCategory.COMPANION, "红白格子帆布野餐垫，在此进食享受双倍好感", "双层纯棉帆布", 0.02f, 0.15f, SynesthesiaEvent.COUPLE_SNUGGLE),
        InteractiveToy("feather_wand", "旋转逗狗羽毛棒", "🪶", ToyCategory.SENSORY, "细长碳素弹性杆，羽毛随风飘荡引小狗抓捕", "弹性杆与羽毛", 0.30f, 0.04f, SynesthesiaEvent.BUBBLE_POP),
        InteractiveToy("knitted_beanie", "冬日手织毛线帽", "🧶", ToyCategory.RELAX, "焦糖暖橙粗针线帽，头顶一颗蓬松白绒球", "粗针毛线", 0.15f, 0.06f, SynesthesiaEvent.PET_HEAD),
        InteractiveToy("pinwheel", "春日粉樱风车", "🌸", ToyCategory.SENSORY, "四瓣樱花风车，随窗外微风自动旋转", "聚酯磨砂薄膜", 0.10f, 0.03f, SynesthesiaEvent.YARN_ROLL),
        InteractiveToy("snuffle_mat", "解谜嗅闻花垫", "🧩", ToyCategory.SENSORY, "层叠花瓣毛毡垫，藏满香脆小零食等待发掘", "环保厚毛毡", 0.05f, 0.25f, SynesthesiaEvent.FEED_CRUNCH),
        InteractiveToy("donut_collar", "充气甜甜圈项圈", "🍩", ToyCategory.RELAX, "草莓糖霜泳圈式软充气颈托，防止生病抓挠", "充气环保PVC", 0.80f, 0.10f, SynesthesiaEvent.SQUEAKY_DUCK),
        InteractiveToy("starry_jar", "星空夜光玻璃瓶", "🌟", ToyCategory.COMPANION, "装满夜光手折小纸星，散发安神幽蓝微光", "硼硅耐热玻璃", 0.15f, 0.40f, SynesthesiaEvent.POMODORO_CHIME),
        InteractiveToy("ball_launcher", "自动发球网球机", "🎾", ToyCategory.PLAY, "定时弹出发球，小狗狂奔追逐叼回塞入桶中", "ABS工程塑料", 0.50f, 1.50f, SynesthesiaEvent.FEED_CRUNCH),
        InteractiveToy("croissant_plush", "软软牛角包抱枕", "🥐", ToyCategory.RELAX, "层层酥皮手绘折皱，叼在嘴里四处炫耀", "短毛绒", 0.20f, 0.10f, SynesthesiaEvent.PET_HEAD),
        InteractiveToy("voice_box", "情侣录音信笺盒", "💌", ToyCategory.COMPANION, "伴侣发来语音或悄悄话时，铁盒轻微跳动喷爱心", "马口铁", 0.25f, 0.15f, SynesthesiaEvent.COUPLE_SNUGGLE),
        InteractiveToy("bongo_drum", "熊掌双联拍拍鼓", "🥁", ToyCategory.PLAY, "两只前爪交替拍击鼓面，发出清脆咚咚节奏", "羊皮与硬实木", 0.35f, 0.30f, SynesthesiaEvent.FEED_CRUNCH),
        InteractiveToy("towel_cape", "魔力吸水速干巾", "🦸", ToyCategory.RELAX, "洗完澡后披上，小狗在地上飞速蹭行擦干", "超细复合纤维", 0.05f, 0.07f, SynesthesiaEvent.PET_HEAD),
        InteractiveToy("wicker_basket", "手提编织野餐篮", "🧺", ToyCategory.COMPANION, "篮盖可掀开，里面装满金黄小鱼干与大肉骨", "天然柳条编织", 0.18f, 0.38f, SynesthesiaEvent.FEED_CRUNCH),
        InteractiveToy("rainbow_slide", "四色手绘滑梯", "🛝", ToyCategory.PLAY, "平缓弧形木滑道，小狗屁股坐在滑道顺溜滑下", "彩漆光滑实木", 0.30f, 2.50f, SynesthesiaEvent.SQUEAKY_DUCK),
        InteractiveToy("running_wheel", "慢跑静音小滚轮", "🎡", ToyCategory.PLAY, "小狗跳入奔跑，运动步数实时转化为体力", "软木与榉木", 0.10f, 3.20f, SynesthesiaEvent.YARN_ROLL),
        InteractiveToy("time_capsule", "时光秘密胶囊罐", "⏳", ToyCategory.COMPANION, "封存一张恋爱纪念卡，锁定30天后自动启封", "粗陶蜡封", 0.08f, 0.55f, SynesthesiaEvent.STAMP_IMPACT),
        InteractiveToy("treat_dispenser", "漏食迷宫保龄球", "🎳", ToyCategory.SENSORY, "爪推圆球滚动3圈掉落金黄小饼干", "硬质ABS塑料", 0.45f, 0.30f, SynesthesiaEvent.FEED_CRUNCH)
    )

    // 当前在小窝中放置的活跃互动玩具 (默认放置弹力毛线球)
    private val _activeToy = MutableStateFlow<InteractiveToy?>(TOY_CATALOG[0])
    val activeToy: StateFlow<InteractiveToy?> = _activeToy.asStateFlow()

    fun placeToy(toy: InteractiveToy) {
        _activeToy.value = toy
    }

    fun removeToy() {
        _activeToy.value = null
    }
}
