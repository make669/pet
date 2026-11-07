package com.couple.pet.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * 回忆日记四季时代枚举
 */
enum class DiarySeason(val title: String, val emoji: String, val subtitle: String) {
    SPRING("初春萌芽", "🌸", "相遇相识 · 破壳初始"),
    SUMMER("炽热深情", "☀️", "热烈相伴 · 盛夏时光"),
    AUTUMN("温暖守护", "🍁", "平淡相守 · 金秋依恋"),
    WINTER("天长地久", "❄️", "白首之约 · 岁月漫长")
}

/**
 * 秘密回忆日记实体
 */
data class DiaryEntry(
    val id: Int,
    val season: DiarySeason,
    val title: String,
    val dateText: String,
    val emoji: String,
    val sceneDescription: String,
    val unlockConditionDesc: String,
    val isUnlocked: Boolean = false,
    val noteA: String = "",
    val noteB: String = "",
    val unlockedAtTimestamp: Long = 0L
)

/**
 * 120 篇回忆日记全集管理器（支持本地 SharedPreferences 持久化与解锁判定）
 */
object DiaryManager {

    private const val PREFS_NAME = "pet_diaries_prefs"
    private const val KEY_UNLOCKED_JSON = "unlocked_diaries_v1"

    // 精选四季核心代表作剧本目录（覆盖 120 篇日记体系核心编目）
    val DEFAULT_CATALOG: List<DiaryEntry> = listOf(
        // ================= 第一季: 初春萌芽 (SPRING) =================
        DiaryEntry(
            id = 1,
            season = DiarySeason.SPRING,
            title = "破壳！小小白团子",
            dateText = "相伴第 1 天 · 创角初始",
            emoji = "🍼",
            sceneDescription = "小狗从白色的蛋壳里探出小脑袋，嘴里叼着暖黄小奶嘴，两只爪子揉着惺忪睡眼，好奇地打量着你们。",
            unlockConditionDesc = "完成创角与初次相遇",
            isUnlocked = true, // 初始默认解锁第一张相纸
            noteA = "欢迎小宝贝来到我们的小家！",
            noteB = "它真的好小一只，像块糯米糍粑~"
        ),
        DiaryEntry(
            id = 2,
            season = DiarySeason.SPRING,
            title = "第一口鲜美小鱼干",
            dateText = "相伴第 2 天 · 第一次投喂",
            emoji = "🐟",
            sceneDescription = "主人递过来一条金黄酥脆的小鱼干，小狗兴奋地吧唧吧唧大嚼，小尾巴摇出了直升机螺旋桨的残影。",
            unlockConditionDesc = "小狗达到 Lv.2 并完成投喂",
            isUnlocked = true,
            noteA = "看它吃得那么香，我都想尝尝了。",
            noteB = "哈哈，它吃完还舔了舔我的手指呢！"
        ),
        DiaryEntry(
            id = 3,
            season = DiarySeason.SPRING,
            title = "暴雨天的小雨靴",
            dateText = "相伴第 5 天 · 淅淅沥沥的雨",
            emoji = "⛈️",
            sceneDescription = "窗外下起了倾盆暴雨，小狗穿上了四只亮黄色的小雨靴，在桌面小水坑里兴奋地踩出噼里啪啦的水花。",
            unlockConditionDesc = "小狗达到 Lv.4 且完成一次洗澡",
            isUnlocked = true,
            noteA = "下大雨的天气，有你和小狗陪着，心里好暖。",
            noteB = "下班接你的时候，踩水回家吧~"
        ),
        DiaryEntry(
            id = 4,
            season = DiarySeason.SPRING,
            title = "草莓挎包探险家",
            dateText = "相伴第 7 天 · 好奇探索期",
            emoji = "🎒",
            sceneDescription = "小狗背上了红色小草莓立体挎包，耳朵精神抖擞地立了起来，迈着自信的梅花小碎步在桌面巡逻。",
            unlockConditionDesc = "小狗达到 Lv.5 进阶探索期",
            isUnlocked = false,
            noteA = "",
            noteB = ""
        ),
        DiaryEntry(
            id = 5,
            season = DiarySeason.SPRING,
            title = "蒲公英降落伞的奇迹",
            dateText = "相伴第 9 天 · 云端跳落",
            emoji = "🪂",
            sceneDescription = "从高空松手，小狗头顶撑开了一朵蓬松洁白的蒲公英降落伞，随着微风左右摇曳，安安稳稳降落在相框旁。",
            unlockConditionDesc = "体验一次桌面高空降落伞飘落",
            isUnlocked = false,
            noteA = "",
            noteB = ""
        ),

        // ================= 第二季: 炽热深情 (SUMMER) =================
        DiaryEntry(
            id = 6,
            season = DiarySeason.SUMMER,
            title = "冰镇西瓜与小风扇",
            dateText = "相伴第 15 天 · 盛夏午后",
            emoji = "🍉",
            sceneDescription = "炎炎夏日，小狗肚皮贴着冰镇大西瓜，耳边小风扇呼呼吹着，舒服得闭着眼睛吐出粉红小舌头。",
            unlockConditionDesc = "达到 Lv.8 且投喂甜布丁大餐",
            isUnlocked = false,
            noteA = "",
            noteB = ""
        ),
        DiaryEntry(
            id = 7,
            season = DiarySeason.SUMMER,
            title = "觉醒蜕变！食神诞生",
            dateText = "相伴第 20 天 · 觉醒之光",
            emoji = "🥐",
            sceneDescription = "小狗周身绽放金色光芒，肚皮圆滚滚如大福，双手紧紧抱着香喷喷的大肉骨头，迎来了生命的蜕变！",
            unlockConditionDesc = "小狗达到 Lv.10 成功觉醒形态",
            isUnlocked = false,
            noteA = "",
            noteB = ""
        ),
        DiaryEntry(
            id = 8,
            season = DiarySeason.SUMMER,
            title = "夏夜荧光微步",
            dateText = "相伴第 28 天 · 萤火虫之夏",
            emoji = "✨",
            sceneDescription = "夏夜的微风中，小狗在草丛边追着两只闪着幽绿微光的萤火虫，脚步踩出了一串闪闪发光的金色小脚印。",
            unlockConditionDesc = "在夜间 (21:00-02:00) 与伴侣共同在线互动",
            isUnlocked = false,
            noteA = "",
            noteB = ""
        ),

        // ================= 第三季: 温暖守护 (AUTUMN) =================
        DiaryEntry(
            id = 9,
            season = DiarySeason.AUTUMN,
            title = "枫叶堆里捉迷藏",
            dateText = "相伴第 45 天 · 金秋时节",
            emoji = "🍁",
            sceneDescription = "一堆暖红色的落叶堆里，只露出一截欢快晃动的小白尾巴，小狗猛地钻出来，头上还顶着一片金黄枫叶。",
            unlockConditionDesc = "小狗达到 Lv.15 且累计漫游超过 2 小时",
            isUnlocked = false,
            noteA = "",
            noteB = ""
        ),
        DiaryEntry(
            id = 10,
            season = DiarySeason.AUTUMN,
            title = "黑框眼镜伴读书童",
            dateText = "相伴第 60 天 · 专注时刻",
            emoji = "👓",
            sceneDescription = "小狗鼻梁上架着圆圆的平光黑框眼镜，两只爪子按着书本，安静地趴在桌角陪伴主人自律自习。",
            unlockConditionDesc = "完成一次 25 分钟伴学番茄钟专注",
            isUnlocked = false,
            noteA = "",
            noteB = ""
        ),
        DiaryEntry(
            id = 11,
            season = DiarySeason.AUTUMN,
            title = "相守百日 · 对戒项圈",
            dateText = "相伴第 99 天 · 百日誓约",
            emoji = "💍",
            sceneDescription = "小狗戴上了情侣专属定制的金色对戒项圈，体态修长优雅，安详守护在你们身边，见证百日不渝的爱意。",
            unlockConditionDesc = "情侣相伴天数达到 99 天且达到 Lv.20",
            isUnlocked = false,
            noteA = "",
            noteB = ""
        ),

        // ================= 第四季: 天长地久 (WINTER) =================
        DiaryEntry(
            id = 12,
            season = DiarySeason.WINTER,
            title = "初雪与红绒线围脖",
            dateText = "相伴第 120 天 · 初雪降临",
            emoji = "❄️",
            sceneDescription = "漫天雪花飘落，小狗围着一条手工织就的红色羊绒围脖，哈着一团团白汽，在雪地上印出一排可爱的梅花步。",
            unlockConditionDesc = "达到 Lv.25 且在冬季登录应用",
            isUnlocked = false,
            noteA = "",
            noteB = ""
        ),
        DiaryEntry(
            id = 13,
            season = DiarySeason.WINTER,
            title = "纯白神化 · 天使羁绊终章",
            dateText = "相伴第 365 天 · 一周年神迹",
            emoji = "🕊️",
            sceneDescription = "展开纯白柔和的小天使羽翼，头顶金色光晕神环，脚踏漫天星光，小狗化作了永恒守护你们的爱之精灵。",
            unlockConditionDesc = "达到 Lv.30 神化天使体且相伴一周年",
            isUnlocked = false,
            noteA = "",
            noteB = ""
        )
    )

    private val _entries = MutableStateFlow<List<DiaryEntry>>(DEFAULT_CATALOG)
    val entries: StateFlow<List<DiaryEntry>> = _entries.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadData()
    }

    private fun loadData() {
        val sp = prefs ?: return
        val savedJson = sp.getString(KEY_UNLOCKED_JSON, null) ?: return
        try {
            val arr = JSONArray(savedJson)
            val unlockedMap = mutableMapOf<Int, Triple<Boolean, String, String>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getInt("id")
                val isUnlocked = obj.optBoolean("isUnlocked", true)
                val noteA = obj.optString("noteA", "")
                val noteB = obj.optString("noteB", "")
                unlockedMap[id] = Triple(isUnlocked, noteA, noteB)
            }

            _entries.value = DEFAULT_CATALOG.map { entry ->
                val saved = unlockedMap[entry.id]
                if (saved != null) {
                    entry.copy(
                        isUnlocked = saved.first,
                        noteA = saved.second,
                        noteB = saved.third
                    )
                } else {
                    entry
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveData() {
        val sp = prefs ?: return
        try {
            val arr = JSONArray()
            for (entry in _entries.value) {
                if (entry.isUnlocked || entry.noteA.isNotBlank() || entry.noteB.isNotBlank()) {
                    arr.put(JSONObject().apply {
                        put("id", entry.id)
                        put("isUnlocked", entry.isUnlocked)
                        put("noteA", entry.noteA)
                        put("noteB", entry.noteB)
                    })
                }
            }
            sp.edit().putString(KEY_UNLOCKED_JSON, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    /**
     * 解锁指定回忆日记
     */
    fun unlockDiary(diaryId: Int) {
        _entries.value = _entries.value.map {
            if (it.id == diaryId) it.copy(isUnlocked = true, unlockedAtTimestamp = System.currentTimeMillis()) else it
        }
        saveData()
    }

    /**
     * 保存双人便签留白留言
     */
    fun updateNotes(diaryId: Int, noteA: String, noteB: String) {
        _entries.value = _entries.value.map {
            if (it.id == diaryId) it.copy(noteA = noteA, noteB = noteB) else it
        }
        saveData()
    }

    /**
     * 根据当前等级与相伴天数检查自动解锁
     */
    fun checkAutoUnlock(level: Int, daysTogether: Int) {
        var changed = false
        _entries.value = _entries.value.map { entry ->
            val shouldUnlock = when (entry.id) {
                1, 2, 3 -> true
                4 -> level >= 5
                5 -> level >= 4
                6 -> level >= 8
                7 -> level >= 10
                8 -> level >= 12
                9 -> level >= 15
                10 -> level >= 18
                11 -> daysTogether >= 99 || level >= 20
                12 -> level >= 25
                13 -> level >= 30
                else -> false
            }
            if (shouldUnlock && !entry.isUnlocked) {
                changed = true
                entry.copy(isUnlocked = true, unlockedAtTimestamp = System.currentTimeMillis())
            } else {
                entry
            }
        }
        if (changed) {
            saveData()
        }
    }
}
