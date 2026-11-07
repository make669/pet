package com.couple.pet.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * 触觉-声效-动效（HAV）三位一体联觉执行器 (Haptic-Audio-Visual Synesthesia Orchestrator)
 * 纯算法级无外置音频资产依赖：内置合成物理正弦波与包络，支持毫秒级低延迟响应。
 */
object SynesthesiaOrchestrator {
    private var vibrator: Vibrator? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        isInitialized = true
    }

    /**
     * 触发指定感官交互事件
     */
    fun emit(event: SynesthesiaEvent) {
        playHaptic(event)
        playSyntheticAudio(event)
    }

    private fun playHaptic(event: SynesthesiaEvent) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (event) {
                SynesthesiaEvent.PET_HEAD -> {
                    // 柔和微颤抚摸 (双 Tick 组合)
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.40f, 0)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.55f, 45)
                        .compose()
                    vib.vibrate(effect)
                }
                SynesthesiaEvent.FEED_CRUNCH -> {
                    // 咔嚓清脆断裂 (强 Click + Quick Fall)
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.95f, 0)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.35f, 20)
                        .compose()
                    vib.vibrate(effect)
                }
                SynesthesiaEvent.COUPLE_SNUGGLE -> {
                    // 浪漫双重跳动 (心跳咚-哒，间隔 160ms)
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.70f, 0)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f, 160)
                        .compose()
                    vib.vibrate(effect)
                }
                SynesthesiaEvent.BUBBLE_POP -> {
                    // 气泡破裂超微冲击 (Micro Tap)
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.30f, 0)
                        .compose()
                    vib.vibrate(effect)
                }
                SynesthesiaEvent.STAMP_IMPACT -> {
                    // 实木沉稳盖章 (Heavy Click)
                    val effect = VibrationEffect.createOneShot(45, 255)
                    vib.vibrate(effect)
                }
                SynesthesiaEvent.SQUEAKY_DUCK -> {
                    // 啾啾小黄鸭压缩弹跳
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.80f, 0)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.60f, 70)
                        .compose()
                    vib.vibrate(effect)
                }
                SynesthesiaEvent.YARN_ROLL -> {
                    // 毛线球轻盈滚动细微连续感
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.35f, 0)
                        .compose()
                    vib.vibrate(effect)
                }
                SynesthesiaEvent.POMODORO_CHIME -> {
                    // 胜利渐进三阶波
                    val effect = VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 60, 80, 70, 120),
                        intArrayOf(0, 100, 0, 180, 0, 255),
                        -1
                    )
                    vib.vibrate(effect)
                }
            }
        } else {
            // Android 8.0 ~ 10 回退兼容
            val duration = when (event) {
                SynesthesiaEvent.PET_HEAD -> 30L
                SynesthesiaEvent.FEED_CRUNCH -> 45L
                SynesthesiaEvent.COUPLE_SNUGGLE -> 70L
                SynesthesiaEvent.BUBBLE_POP -> 12L
                SynesthesiaEvent.STAMP_IMPACT -> 50L
                SynesthesiaEvent.SQUEAKY_DUCK -> 35L
                SynesthesiaEvent.YARN_ROLL -> 18L
                SynesthesiaEvent.POMODORO_CHIME -> 100L
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(duration)
            }
        }
    }

    /**
     * 极轻量算法声学合成器：用 AudioTrack 实时生成纯正正弦波或白噪断裂音
     */
    private fun playSyntheticAudio(event: SynesthesiaEvent) {
        scope.launch {
            try {
                val sampleRate = 22050
                val durationMs = when (event) {
                    SynesthesiaEvent.PET_HEAD -> 180
                    SynesthesiaEvent.FEED_CRUNCH -> 90
                    SynesthesiaEvent.COUPLE_SNUGGLE -> 320
                    SynesthesiaEvent.BUBBLE_POP -> 80
                    SynesthesiaEvent.STAMP_IMPACT -> 140
                    SynesthesiaEvent.SQUEAKY_DUCK -> 120
                    SynesthesiaEvent.YARN_ROLL -> 100
                    SynesthesiaEvent.POMODORO_CHIME -> 450
                }
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(100)
                val buffer = ShortArray(numSamples)

                when (event) {
                    SynesthesiaEvent.PET_HEAD -> {
                        // 暖喉呼噜声：220Hz 正弦波 + 低频调幅 (Tremolo)
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            val env = sin(Math.PI * (i.toDouble() / numSamples)) // 钟形包络
                            val tremolo = 0.7 + 0.3 * sin(2.0 * Math.PI * 18.0 * t)
                            val sample = sin(2.0 * Math.PI * 220.0 * t) * env * tremolo
                            buffer[i] = (sample * 8000).toInt().toShort()
                        }
                    }
                    SynesthesiaEvent.FEED_CRUNCH -> {
                        // 酥脆咀嚼音：高频爆发衰减 (Crunch)
                        val random = java.util.Random()
                        for (i in 0 until numSamples) {
                            val decay = (1.0 - (i.toDouble() / numSamples))
                            val noise = (random.nextDouble() * 2.0 - 1.0)
                            val click = sin(2.0 * Math.PI * 1800.0 * (i.toDouble() / sampleRate))
                            val sample = (noise * 0.65 + click * 0.35) * decay * decay
                            buffer[i] = (sample * 11000).toInt().toShort()
                        }
                    }
                    SynesthesiaEvent.COUPLE_SNUGGLE -> {
                        // 432Hz 黄金和弦治愈音 (432Hz + 648Hz 五度音)
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            val env = (1.0 - (i.toDouble() / numSamples))
                            val sample = (sin(2.0 * Math.PI * 432.0 * t) * 0.65 + sin(2.0 * Math.PI * 648.0 * t) * 0.35) * env
                            buffer[i] = (sample * 9500).toInt().toShort()
                        }
                    }
                    SynesthesiaEvent.BUBBLE_POP -> {
                        // 下行滑音啵唧声 (880Hz -> 440Hz)
                        for (i in 0 until numSamples) {
                            val frac = i.toDouble() / numSamples
                            val freq = 880.0 - 440.0 * frac
                            val t = i.toDouble() / sampleRate
                            val env = 1.0 - frac
                            val sample = sin(2.0 * Math.PI * freq * t) * env
                            buffer[i] = (sample * 12000).toInt().toShort()
                        }
                    }
                    SynesthesiaEvent.STAMP_IMPACT -> {
                        // 实木沉实落地声 (140Hz 阻尼)
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            val decay = Math.exp(-25.0 * t)
                            val sample = sin(2.0 * Math.PI * 140.0 * t) * decay
                            buffer[i] = (sample * 14000).toInt().toShort()
                        }
                    }
                    SynesthesiaEvent.SQUEAKY_DUCK -> {
                        // 小黄鸭欢快高音 (1200Hz 啾啾音)
                        for (i in 0 until numSamples) {
                            val frac = i.toDouble() / numSamples
                            val freq = 1100.0 + 350.0 * sin(Math.PI * frac)
                            val t = i.toDouble() / sampleRate
                            val env = sin(Math.PI * frac)
                            val sample = sin(2.0 * Math.PI * freq * t) * env
                            buffer[i] = (sample * 13000).toInt().toShort()
                        }
                    }
                    SynesthesiaEvent.YARN_ROLL -> {
                        // 毛线轻柔摩擦音
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            val env = sin(Math.PI * (i.toDouble() / numSamples))
                            val sample = sin(2.0 * Math.PI * 320.0 * t) * env * 0.5
                            buffer[i] = (sample * 6000).toInt().toShort()
                        }
                    }
                    SynesthesiaEvent.POMODORO_CHIME -> {
                        // 铜铃长泛音 (587Hz D大调)
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            val decay = Math.exp(-5.0 * t)
                            val sample = (sin(2.0 * Math.PI * 587.33 * t) * 0.7 + sin(2.0 * Math.PI * 1174.66 * t) * 0.3) * decay
                            buffer[i] = (sample * 10000).toInt().toShort()
                        }
                    }
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                // 播放完毕后异步释放
                kotlinx.coroutines.delay(durationMs + 50L)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // 静默忽略音频异常，保证高稳定性
            }
        }
    }
}

enum class SynesthesiaEvent {
    PET_HEAD,        // 摸摸头
    FEED_CRUNCH,     // 投喂食物
    COUPLE_SNUGGLE,  // 双狗贴贴
    BUBBLE_POP,      // 戳破气泡
    STAMP_IMPACT,    // 纪念盖章
    SQUEAKY_DUCK,    // 啾啾小黄鸭
    YARN_ROLL,       // 弹力毛线球
    POMODORO_CHIME   // 伴学专注完成
}
