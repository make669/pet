package com.couple.pet.pet

/**
 * 宠物全物种通用动作枚举 (Universal Pet Actions)
 * 适用于所有宠物（狗、猫、兔、熊、鸭等）的统一动作规范体系
 */
enum class UniversalPetAction(
    val actionName: String,
    val description: String,
    val isResting: Boolean = false,
    val canMove: Boolean = false,
    val defaultDurationMs: Long = 2000L
) {
    // 1. 基础物理与位移动作
    IDLE("发呆站立", "原地平稳呼吸打量四周", canMove = false, defaultDurationMs = 2000L),
    WALK("悠闲漫步", "小碎步前行，轻快晃脑", canMove = true, defaultDurationMs = 3000L),
    RUN("撒欢快跑", "加速大跨步奔跑，带冲刺动效", canMove = true, defaultDurationMs = 2500L),

    // 2. 休息与生理节律动作
    SIT("乖巧坐下", "后爪收拢端正坐立", isResting = true, defaultDurationMs = 2500L),
    REST("舒坦趴卧", "四肢伸展趴在地上休息", isResting = true, defaultDurationMs = 3500L),
    SLEEP("团圆安睡", "蜷缩成毛球团子入睡，伴随Zzz呼吸浮动", isResting = true, defaultDurationMs = 6000L),
    YAWN("疲倦哈欠", "张大嘴巴大呼一口气，舒适眯眼", isResting = false, defaultDurationMs = 2200L),
    STRETCH("下犬拉伸", "前爪向前大伸展，翘起屁股舒展筋骨", isResting = false, defaultDurationMs = 2200L),
    SHAKE("全身甩毛", "身体与头部高频左右抖动甩去浮尘", isResting = false, defaultDurationMs = 1500L),

    // 3. 互动与情感表达动作
    HAPPY_JUMP("兴奋欢跃", "受到抚摸或表扬时开怀翻滚跳跃", isResting = false, defaultDurationMs = 1600L),
    EAT("吧唧干饭", "两爪抱住食物鼓动腮帮大快朵颐", isResting = false, defaultDurationMs = 2000L),
    BEG("作揖求摸", "肉爪合十眼巴巴求零食与关注", isResting = false, defaultDurationMs = 2500L),
    BELLY_UP("翻肚享受", "四脚朝天左右轻蹬，露出软软肚皮求揉", isResting = false, defaultDurationMs = 3000L),
    DRAGGED("空中悬浮", "被手指按住悬空扑腾四爪", isResting = false, defaultDurationMs = 1000L),

    // 4. 物种专属独特绝活 (由各宠物子类自主个性化重写)
    SPECIAL_TRICK("专属绝活", "物种专属的个性绝活展示", isResting = false, defaultDurationMs = 2800L);

    /**
     * 判断是否是静态驻留动作
     */
    val isStationary: Boolean
        get() = !canMove
}
