package com.couple.pet.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * 背包库存与装扮物品管理中心
 * 1. 物品库存流 (inventory): Map<itemId, count>
 * 2. 已拥有装扮流 (ownedOutfits): Set<outfitId>
 * 3. 正在佩戴装扮流 (equippedOutfits): Set<outfitId>
 */
object InventoryManager {

    private const val PREFS_NAME = "pet_inventory_prefs_v1"
    private const val KEY_ITEMS = "inventory_items"
    private const val KEY_OWNED_OUTFITS = "owned_outfits"
    private const val KEY_EQUIPPED_OUTFITS = "equipped_outfits"

    // 默认新手大礼包（首次启动赠送）
    private val DEFAULT_STARTER_ITEMS = mapOf(
        "food_fish" to 5,
        "food_bone" to 3,
        "food_pudding" to 3,
        "food_milk" to 2
    )

    private val _inventory = MutableStateFlow<Map<String, Int>>(DEFAULT_STARTER_ITEMS)
    val inventory: StateFlow<Map<String, Int>> = _inventory.asStateFlow()

    private val _ownedOutfits = MutableStateFlow<Set<String>>(emptySet())
    val ownedOutfits: StateFlow<Set<String>> = _ownedOutfits.asStateFlow()

    private val _equippedOutfits = MutableStateFlow<Set<String>>(emptySet())
    val equippedOutfits: StateFlow<Set<String>> = _equippedOutfits.asStateFlow()

    private var prefs: SharedPreferences? = null

    /**
     * 初始化并读取本地持久化数据
     */
    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadData()
    }

    private fun loadData() {
        val sp = prefs ?: return

        // 1. 读取道具库存
        val itemsJson = sp.getString(KEY_ITEMS, null)
        if (!itemsJson.isNullOrBlank()) {
            try {
                val obj = JSONObject(itemsJson)
                val map = mutableMapOf<String, Int>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = obj.optInt(k, 0)
                }
                _inventory.value = map
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // 首次启动，保存默认新手物品
            saveInventory()
        }

        // 2. 读取已拥有装扮
        val ownedJson = sp.getString(KEY_OWNED_OUTFITS, null)
        if (!ownedJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(ownedJson)
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
                _ownedOutfits.value = set
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. 读取正在佩戴装扮
        val equippedJson = sp.getString(KEY_EQUIPPED_OUTFITS, null)
        if (!equippedJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(equippedJson)
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
                _equippedOutfits.value = set
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveInventory() {
        val sp = prefs ?: return
        val obj = JSONObject()
        _inventory.value.forEach { (k, v) ->
            obj.put(k, v)
        }
        sp.edit().putString(KEY_ITEMS, obj.toString()).apply()
    }

    private fun saveOwnedOutfits() {
        val sp = prefs ?: return
        val arr = JSONArray()
        _ownedOutfits.value.forEach { arr.put(it) }
        sp.edit().putString(KEY_OWNED_OUTFITS, arr.toString()).apply()
    }

    private fun saveEquippedOutfits() {
        val sp = prefs ?: return
        val arr = JSONArray()
        _equippedOutfits.value.forEach { arr.put(it) }
        sp.edit().putString(KEY_EQUIPPED_OUTFITS, arr.toString()).apply()
    }

    /**
     * 获取指定物品库存
     */
    fun getItemCount(itemId: String): Int {
        return _inventory.value[itemId] ?: 0
    }

    /**
     * 获取总道具库存件数
     */
    fun getTotalItemCount(): Int {
        return _inventory.value.values.sum()
    }

    /**
     * 购买/获得道具入库
     */
    fun addItem(itemId: String, count: Int = 1) {
        if (count <= 0) return
        val map = _inventory.value.toMutableMap()
        val current = map[itemId] ?: 0
        map[itemId] = current + count
        _inventory.value = map
        saveInventory()
    }

    /**
     * 投喂/使用消耗道具
     * @return true 消耗成功，false 库存不足
     */
    fun consumeItem(itemId: String, count: Int = 1): Boolean {
        if (count <= 0) return true
        val map = _inventory.value.toMutableMap()
        val current = map[itemId] ?: 0
        if (current < count) {
            return false
        }
        val remainder = current - count
        if (remainder > 0) {
            map[itemId] = remainder
        } else {
            map[itemId] = 0 // 保留槽位计数为 0
        }
        _inventory.value = map
        saveInventory()
        return true
    }

    /**
     * 购买解锁装扮
     */
    fun buyOutfit(outfitId: String) {
        val set = _ownedOutfits.value.toMutableSet()
        set.add(outfitId)
        _ownedOutfits.value = set
        saveOwnedOutfits()
    }

    /**
     * 切换佩戴/卸下装扮
     */
    fun toggleEquipOutfit(outfitId: String) {
        val set = _equippedOutfits.value.toMutableSet()
        if (set.contains(outfitId)) {
            set.remove(outfitId)
        } else {
            set.add(outfitId)
        }
        _equippedOutfits.value = set
        saveEquippedOutfits()
    }

    /**
     * 检查装扮是否已拥有
     */
    fun isOutfitOwned(outfitId: String): Boolean {
        return _ownedOutfits.value.contains(outfitId)
    }

    /**
     * 检查装扮是否正在佩戴
     */
    fun isOutfitEquipped(outfitId: String): Boolean {
        return _equippedOutfits.value.contains(outfitId)
    }
}
