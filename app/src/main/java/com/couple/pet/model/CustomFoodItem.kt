package com.couple.pet.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * 自定义投喂食物/物品数据模型
 */
data class CustomFoodItem(
    val id: String,
    val name: String,
    val emoji: String,
    val hungerBoost: Int = 15,
    val moodBoost: Int = 0,
    val cleanBoost: Int = 0,
    val description: String = ""
) {
    val buffDescription: String
        get() {
            val parts = mutableListOf<String>()
            if (hungerBoost > 0) parts.add("饱食+$hungerBoost")
            if (moodBoost > 0) parts.add("心情+$moodBoost")
            if (cleanBoost > 0) parts.add("清洁+$cleanBoost")
            return parts.joinToString(" · ")
        }
}

/**
 * 投喂托盘与食物库管理中心（支持 3 个自定义圆圈槽位与本地 SharedPreferences 持久化）
 */
object PetFoodManager {

    private const val PREFS_NAME = "pet_custom_food_prefs"
    private const val KEY_ACTIVE_SLOTS = "active_slots_3_v1"
    private const val KEY_CUSTOM_ITEMS = "user_custom_items_v1"

    // 官方精心预设的丰富零食与用品库
    val PRESET_LIBRARY = listOf(
        CustomFoodItem("food_fish", "小鱼干", "🐟", hungerBoost = 15, moodBoost = 5, description = "鲜美酥脆，小狗最爱"),
        CustomFoodItem("food_bone", "和牛骨", "🍖", hungerBoost = 25, moodBoost = 5, description = "大号肉骨棒，耐嚼解馋"),
        CustomFoodItem("food_pudding", "甜布丁", "🍮", hungerBoost = 8, moodBoost = 25, description = "草莓奶香，心情直接转晴"),
        CustomFoodItem("food_milk", "鲜羊奶", "🥛", hungerBoost = 12, moodBoost = 8, cleanBoost = 5, description = "温热舒胃，毛发光亮"),
        CustomFoodItem("food_jerky", "纯肉干", "🥩", hungerBoost = 20, moodBoost = 10, description = "纯正原切原肉冻干"),
        CustomFoodItem("food_cheese", "奶酪块", "🧀", hungerBoost = 18, moodBoost = 8, description = "香浓拉丝，营养高钙"),
        CustomFoodItem("food_apple", "苹果片", "🍎", hungerBoost = 10, moodBoost = 12, description = "甜脆多汁，果香浓郁"),
        CustomFoodItem("food_chicken", "大鸡腿", "🍗", hungerBoost = 30, moodBoost = 10, description = "金黄诱人，饱腹感十足"),
        CustomFoodItem("food_ball", "弹力球", "🎾", hungerBoost = 0, moodBoost = 30, description = "磨牙解闷，追逐跑跳"),
        CustomFoodItem("food_soap", "沐浴泡", "🧼", hungerBoost = 0, moodBoost = 10, cleanBoost = 35, description = "温和绵密泡沫，香气扑鼻")
    )

    // 默认在托盘上的 3 个圆圈槽位（完美对应手绘原型图 [O O O]）
    private val DEFAULT_ACTIVE_IDS = listOf("food_fish", "food_bone", "food_pudding")

    private val _activeSlots = MutableStateFlow<List<CustomFoodItem>>(
        DEFAULT_ACTIVE_IDS.mapNotNull { id -> PRESET_LIBRARY.find { it.id == id } }
    )
    val activeSlots: StateFlow<List<CustomFoodItem>> = _activeSlots.asStateFlow()

    private val _userCustomItems = MutableStateFlow<List<CustomFoodItem>>(emptyList())
    val userCustomItems: StateFlow<List<CustomFoodItem>> = _userCustomItems.asStateFlow()

    private var prefs: SharedPreferences? = null

    /**
     * 初始化加载本地持久化配置
     */
    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadData()
    }

    private fun loadData() {
        val sp = prefs ?: return

        // 1. 读取用户自制的新食物
        val customJson = sp.getString(KEY_CUSTOM_ITEMS, null)
        val customList = mutableListOf<CustomFoodItem>()
        if (!customJson.isNullOrBlank()) {
            try {
                val array = JSONArray(customJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    customList.add(
                        CustomFoodItem(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            emoji = obj.getString("emoji"),
                            hungerBoost = obj.optInt("hungerBoost", 15),
                            moodBoost = obj.optInt("moodBoost", 0),
                            cleanBoost = obj.optInt("cleanBoost", 0),
                            description = obj.optString("description", "自定义专属美食")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _userCustomItems.value = customList

        // 2. 读取 3 个圆圈槽位
        val allLibrary = PRESET_LIBRARY + customList
        val activeIdsJson = sp.getString(KEY_ACTIVE_SLOTS, null)
        val loadedActive = mutableListOf<CustomFoodItem>()

        if (!activeIdsJson.isNullOrBlank()) {
            try {
                val idArray = JSONArray(activeIdsJson)
                for (i in 0 until idArray.length()) {
                    val id = idArray.getString(i)
                    allLibrary.find { it.id == id }?.let { loadedActive.add(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 若不足 3 个则补足为默认 3 槽位
        if (loadedActive.size < 3) {
            loadedActive.clear()
            for (id in DEFAULT_ACTIVE_IDS) {
                allLibrary.find { it.id == id }?.let { loadedActive.add(it) }
            }
        }

        _activeSlots.value = loadedActive.take(3)
    }

    /**
     * 获取所有可选用的零食（内置预设 + 用户自定义）
     */
    fun getAllAvailableFoods(): List<CustomFoodItem> {
        return PRESET_LIBRARY + _userCustomItems.value
    }

    /**
     * 替换某个槽位 (0..2) 的食物
     */
    fun setSlot(slotIndex: Int, food: CustomFoodItem) {
        val current = _activeSlots.value.toMutableList()
        if (slotIndex in 0..2) {
            while (current.size <= slotIndex) {
                current.add(PRESET_LIBRARY.first())
            }
            current[slotIndex] = food
            _activeSlots.value = current.take(3)
            saveActiveSlots()
        }
    }

    /**
     * 新增用户自定义食物，并可直接装备到指定槽位
     */
    fun addCustomFood(
        name: String,
        emoji: String,
        hungerBoost: Int,
        moodBoost: Int,
        cleanBoost: Int,
        targetSlotIndex: Int = -1
    ): CustomFoodItem {
        val newId = "custom_${System.currentTimeMillis()}"
        val newItem = CustomFoodItem(
            id = newId,
            name = name.trim().take(6),
            emoji = emoji.trim().ifEmpty { "🍖" },
            hungerBoost = hungerBoost,
            moodBoost = moodBoost,
            cleanBoost = cleanBoost,
            description = "自制爱心美食"
        )

        val updatedCustomList = _userCustomItems.value.toMutableList().apply { add(newItem) }
        _userCustomItems.value = updatedCustomList
        saveCustomItems()

        if (targetSlotIndex in 0..2) {
            setSlot(targetSlotIndex, newItem)
        }

        return newItem
    }

    /**
     * 重置为官方默认 3 槽位
     */
    fun resetToDefaults() {
        val defaultItems = DEFAULT_ACTIVE_IDS.mapNotNull { id -> PRESET_LIBRARY.find { it.id == id } }
        _activeSlots.value = defaultItems
        saveActiveSlots()
    }

    private fun saveActiveSlots() {
        val sp = prefs ?: return
        val array = JSONArray()
        _activeSlots.value.forEach { array.put(it.id) }
        sp.edit().putString(KEY_ACTIVE_SLOTS, array.toString()).apply()
    }

    private fun saveCustomItems() {
        val sp = prefs ?: return
        val array = JSONArray()
        _userCustomItems.value.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("emoji", item.emoji)
                put("hungerBoost", item.hungerBoost)
                put("moodBoost", item.moodBoost)
                put("cleanBoost", item.cleanBoost)
                put("description", item.description)
            }
            array.put(obj)
        }
        sp.edit().putString(KEY_CUSTOM_ITEMS, array.toString()).apply()
    }
}
