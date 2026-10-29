package com.couple.pet.pet

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 宠物物种注册与生命中枢 (PetRegistry)
 * 集中管理所有宠物子类物种，支持跨物种动态切换与本地持久化。
 */
object PetRegistry {

    private const val PREFS_NAME = "pet_species_prefs"
    private const val KEY_ACTIVE_PET_ID = "active_pet_species_id"

    // 默认内置注册的宠物大家族
    private val SPECIES_REGISTRY = mapOf<String, () -> BasePet>(
        "doodle_dog" to { DoodleDogPet() },
        "doodle_cat" to { DoodleCatPet() },
        "doodle_rabbit" to { DoodleRabbitPet() }
    )

    private val _activePet = MutableStateFlow<BasePet>(DoodleDogPet())
    val activePet: StateFlow<BasePet> = _activePet.asStateFlow()

    private var prefs: SharedPreferences? = null

    /**
     * 初始化物种管理器
     */
    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs?.getString(KEY_ACTIVE_PET_ID, "doodle_dog") ?: "doodle_dog"
        switchPet(savedId)
    }

    /**
     * 切换当前伴侣陪伴的宠物物种
     */
    fun switchPet(speciesId: String): BasePet {
        val factory = SPECIES_REGISTRY[speciesId] ?: SPECIES_REGISTRY["doodle_dog"]!!
        val pet = factory()
        _activePet.value = pet
        prefs?.edit()?.putString(KEY_ACTIVE_PET_ID, pet.speciesId)?.apply()
        return pet
    }

    /**
     * 获取所有可领养的宠物清单
     */
    fun getAllPets(): List<BasePet> {
        return SPECIES_REGISTRY.values.map { it() }
    }

    /**
     * 按 ID 获取宠物实例
     */
    fun getPetById(id: String): BasePet {
        return (SPECIES_REGISTRY[id] ?: SPECIES_REGISTRY["doodle_dog"]!!)()
    }
}
