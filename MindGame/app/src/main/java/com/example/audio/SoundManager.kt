package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var appContext: Context? = null
    private var mediaPlayer: MediaPlayer? = null

    var isSfxEnabled: Boolean = true
    var isBgmEnabled: Boolean = false
    private var isAppInForeground: Boolean = true

    private const val SAMPLE_RATE = 22050
    private var currentTrackResId: Int? = null

    val bgmTracks = listOf(
        R.raw.bgm_01_little_idea,
        R.raw.bgm_02_sunny,
        R.raw.bgm_03_light_playful,
        R.raw.bgm_04_refreshing,
        R.raw.bgm_05_caves_of_dawn,
        R.raw.bgm_06_battle_dragons,
        R.raw.bgm_07_melody_nature,
        R.raw.bgm_08_lofi_study,
        R.raw.bgm_09_clay_zen,
        R.raw.bgm_10_peaceful_piano,
        R.raw.bgm_11_ceramic_breeze
    )

    fun initialize(context: Context) {
        appContext = context.applicationContext
        scope.launch {
            initShortSfxTracks()
        }
    }

    fun onAppFocusChanged(isFocused: Boolean) {
        isAppInForeground = isFocused
        if (!isFocused) {
            pauseBgm()
        } else if (isBgmEnabled) {
            resumeBgm()
        }
    }

    fun pauseBgm() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    fun resumeBgm() {
        if (!isBgmEnabled || !isAppInForeground) return
        try {
            if (mediaPlayer != null) {
                mediaPlayer?.start()
            } else {
                playRandomBgm()
            }
        } catch (_: Exception) {
            playRandomBgm()
        }
    }

    fun setBgmState(enabled: Boolean) {
        isBgmEnabled = enabled
        if (enabled) {
            resumeBgm()
        } else {
            stopBgm()
        }
    }

    fun stopBgm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    /**
     * 當使用者切換至不同遊戲畫面時呼叫，自動換一首音樂
     */
    fun onGameSwitched() {
        if (isBgmEnabled && isAppInForeground) {
            playRandomBgm(forceNew = true)
        }
    }

    fun playRandomBgm(forceNew: Boolean = false) {
        if (!isBgmEnabled || !isAppInForeground) return
        val context = appContext ?: return
        val availableTracks = if (bgmTracks.size > 1 && currentTrackResId != null) {
            bgmTracks.filter { it != currentTrackResId }
        } else {
            bgmTracks
        }
        val nextResId = availableTracks.randomOrNull() ?: return
        playTrack(context, nextResId)
    }

    private fun playTrack(context: Context, resId: Int) {
        stopBgm()
        try {
            currentTrackResId = resId
            mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(0.35f, 0.35f)
                isLooping = false
                setOnCompletionListener {
                    playRandomBgm(forceNew = true)
                }
                start()
            }
        } catch (_: Exception) {
            mediaPlayer = null
        }
    }

    private val cachedErrorSamples: ShortArray by lazy {
        generateToneSamples(180.0, 120, 0.35f, isSawtooth = true)
    }

    private val cachedClickSamples: ShortArray by lazy {
        generateToneSamples(880.0, 45, 0.2f, isSawtooth = false)
    }

    private val cachedWoodblockChopSamples: ShortArray by lazy {
        val durationMs = 65
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = Math.exp(-55.0 * t)
            val wave = 0.75 * sin(2.0 * Math.PI * 960.0 * t) + 0.25 * sin(2.0 * Math.PI * 2180.0 * t)
            samples[i] = (wave * decay * 0.55 * Short.MAX_VALUE).toInt().toShort()
        }
        samples
    }

    private val cachedMetalClangSamples: ShortArray by lazy {
        val durationMs = 90
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = Math.exp(-35.0 * t)
            val wave = 0.5 * sin(2.0 * Math.PI * 1850.0 * t) + 0.5 * sin(2.0 * Math.PI * 2780.0 * t)
            samples[i] = (wave * decay * 0.45 * Short.MAX_VALUE).toInt().toShort()
        }
        samples
    }

    private val cachedBlenderSamples: ShortArray by lazy {
        val durationMs = 450
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            val baseFreq = 220.0 + sin(2.0 * Math.PI * 12.0 * t) * 50.0 + progress * 140.0
            val wave = sin(2.0 * Math.PI * baseFreq * t) * 0.6 + (Math.random() - 0.5) * 0.4
            val env = Math.sin(Math.PI * progress)
            samples[i] = (wave * env * 0.38 * Short.MAX_VALUE).toInt().toShort()
        }
        samples
    }

    private var errorAudioTrack: AudioTrack? = null
    private var clickAudioTrack: AudioTrack? = null
    private var woodblockAudioTrack1: AudioTrack? = null
    private var woodblockAudioTrack2: AudioTrack? = null
    private var woodblockTrackToggle = false
    private var metalAudioTrack: AudioTrack? = null
    private var blenderAudioTrack: AudioTrack? = null

    private fun initShortSfxTracks() {
        try {
            if (errorAudioTrack == null) errorAudioTrack = createStaticTrack(cachedErrorSamples)
            if (clickAudioTrack == null) clickAudioTrack = createStaticTrack(cachedClickSamples)
            if (woodblockAudioTrack1 == null) woodblockAudioTrack1 = createStaticTrack(cachedWoodblockChopSamples)
            if (woodblockAudioTrack2 == null) woodblockAudioTrack2 = createStaticTrack(cachedWoodblockChopSamples)
            if (metalAudioTrack == null) metalAudioTrack = createStaticTrack(cachedMetalClangSamples)
            if (blenderAudioTrack == null) blenderAudioTrack = createStaticTrack(cachedBlenderSamples)
        } catch (_: Exception) {
        }
    }

    private fun createStaticTrack(samples: ShortArray): AudioTrack? {
        return try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, samples.size * 2)
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build().apply {
                    write(samples, 0, samples.size)
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun playPreloadedTrack(track: AudioTrack?): Boolean {
        if (track == null) return false
        return try {
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.stop()
                track.reloadStaticData()
                track.play()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun playClick() {
        if (!isSfxEnabled) return
        if (!playPreloadedTrack(clickAudioTrack)) {
            scope.launch {
                playPcm(cachedClickSamples)
            }
        }
    }

    fun playSuccess() {
        if (!isSfxEnabled) return
        scope.launch {
            playTone(523.25, 70, 0.25f) // C5
            delay(60)
            playTone(659.25, 70, 0.25f) // E5
            delay(60)
            playTone(783.99, 110, 0.3f) // G5
        }
    }

    fun playWin() {
        if (!isSfxEnabled) return
        scope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.50)
            for (freq in notes) {
                playTone(freq, 90, 0.3f)
                delay(80)
            }
        }
    }

    fun playError() {
        if (!isSfxEnabled) return
        // 優先透過快取的預載 AudioTrack 即刻播放（0ms延遲）
        if (!playPreloadedTrack(errorAudioTrack)) {
            scope.launch {
                playPcm(cachedErrorSamples)
            }
        }
    }

    fun playCatMeow() {
        if (!isSfxEnabled) return
        scope.launch {
            val durationMs = 180
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val freq = 600.0 + 350.0 * sin(Math.PI * (i.toDouble() / numSamples))
                val wave = sin(2.0 * Math.PI * freq * t)
                val envelope = (1.0 - (i.toDouble() / numSamples)) * 0.3
                samples[i] = (wave * envelope * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(samples)
        }
    }

    /**
     * 塊陶啊！陶塊落子沉穩叩擊聲 (Ceramic Click & Tap)
     */
    fun playClayDrop() {
        if (!isSfxEnabled) return
        scope.launch {
            val durationMs = 65
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // 陶器敲擊物理諧波 (基頻 480Hz + 泛音 1320Hz + 指數快速衰減)
                val decay = Math.exp(-45.0 * t)
                val wave = 0.7 * sin(2.0 * Math.PI * 480.0 * t) + 0.3 * sin(2.0 * Math.PI * 1324.8 * t)
                samples[i] = (wave * decay * 0.45 * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(samples)
        }
    }

    /**
     * 塊陶啊！消除連擊階梯爬升音階 (清脆水滴/水晶琴鍵)
     */
    fun playComboChime(comboLevel: Int) {
        if (!isSfxEnabled) return
        scope.launch {
            // 連擊音階 (C5, D5, E5, G5, A5, C6, E6...)
            val baseFreqs = listOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50, 1318.51)
            val freq = baseFreqs[(comboLevel - 1).coerceIn(0, baseFreqs.size - 1)]
            val durationMs = 120
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val decay = Math.exp(-18.0 * t)
                val wave = 0.75 * sin(2.0 * Math.PI * freq * t) + 0.25 * sin(2.0 * Math.PI * freq * 2.0 * t)
                samples[i] = (wave * decay * 0.4 * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(samples)
        }
    }

    /**
     * 塊陶啊！蜈蚣竄逃滑稽滑音 (Pitch Slide) + 碎步聲
     */
    fun playCentipedeEscape() {
        if (!isSfxEnabled) return
        scope.launch {
            val durationMs = 280
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                // 滑稽上滑音 380Hz -> 850Hz + 微小快速小腳踏步顫音
                val freq = 380.0 + 470.0 * (progress * progress)
                val footstepPatter = 1.0 + 0.3 * sin(2.0 * Math.PI * 32.0 * t)
                val wave = sin(2.0 * Math.PI * freq * t) * footstepPatter
                val decay = (1.0 - progress) * (if (progress < 0.1) progress * 10.0 else 1.0)
                samples[i] = (wave * decay * 0.35 * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(samples)
        }
    }

    /**
     * 塊陶啊！雙向十字爆破共鳴音 (Cross Boom)
     */
    fun playCrossBoom() {
        if (!isSfxEnabled) return
        scope.launch {
            val durationMs = 220
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val decay = Math.exp(-12.0 * t)
                // 陶土瓦解低頻爆破 + 晶體碎裂
                val lowBoom = sin(2.0 * Math.PI * 110.0 * t) * 0.6
                val midCrack = sin(2.0 * Math.PI * 340.0 * t) * 0.3
                val noise = ((Math.random() - 0.5) * 0.2)
                val wave = lowBoom + midCrack + noise
                samples[i] = (wave * decay * 0.5 * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(samples)
        }
    }

    /**
     * 靈光一閃方塊旋轉音
     */
    fun playRotate() {
        if (!isSfxEnabled) return
        scope.launch {
            playTone(659.25, 50, 0.25f) // E5
            delay(40)
            playTone(880.00, 70, 0.3f)  // A5
        }
    }

    /**
     * 水果切切樂：柔和清脆破空刀痕聲 (Swish)
     */
    fun playFruitSliceSwish() {
        if (!isSfxEnabled) return
        scope.launch {
            val durationMs = 80
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val freq = 1400.0 - progress * 900.0 // 1400Hz -> 500Hz 快速下掠
                val sine = sin(2.0 * Math.PI * freq * t)
                val noise = (Math.random() - 0.5) * 0.4
                val decay = Math.sin(Math.PI * progress) * Math.exp(-progress * 2.5)
                samples[i] = ((sine * 0.6 + noise * 0.4) * decay * 0.28 * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(samples)
        }
    }

    /**
     * 水果切切樂：多段和弦微調爆汁音 (真實破皮白噪音 + 和弦爆汁質感)
     */
    fun playFruitJuiceSplash() {
        if (!isSfxEnabled) return
        scope.launch {
            val durationMs = 95
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            val baseFreq = listOf(440.0, 554.37, 659.25).random()
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val decay = Math.exp(-32.0 * t)
                val tone1 = sin(2.0 * Math.PI * baseFreq * t)
                val tone2 = sin(2.0 * Math.PI * (baseFreq * 1.5) * t) * 0.4
                val popNoise = if (progress < 0.25) (Math.random() - 0.5) * 0.6 else 0.0
                val mixed = (tone1 * 0.5 + tone2 + popNoise) * decay
                samples[i] = (mixed * 0.45 * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(samples)
        }
    }

    /**
     * 水果切切樂：切片工坊圓潤木質砧板打擊音 (Woodblock ASMR) - 0ms 極速無延遲
     */
    fun playWoodblockChop() {
        if (!isSfxEnabled) return
        woodblockTrackToggle = !woodblockTrackToggle
        val track = if (woodblockTrackToggle) woodblockAudioTrack1 else woodblockAudioTrack2
        if (!playPreloadedTrack(track)) {
            scope.launch {
                playPcm(cachedWoodblockChopSamples)
            }
        }
    }

    /**
     * 水果切切樂：切片工坊金屬砧板彈刀清脆撞擊音 - 0ms 極速無延遲
     */
    fun playMetalClang() {
        if (!isSfxEnabled) return
        if (!playPreloadedTrack(metalAudioTrack)) {
            scope.launch {
                playPcm(cachedMetalClangSamples)
            }
        }
    }

    /**
     * 水果切切樂：切片工坊特調果汁旋轉攪拌音 (Blender Whirl)
     */
    fun playBlenderWhir() {
        if (!isSfxEnabled) return
        if (!playPreloadedTrack(blenderAudioTrack)) {
            scope.launch {
                playPcm(cachedBlenderSamples)
            }
        }
    }

    /**
     * 水果切切樂：心跳果刃戰低頻心跳脈衝音效 (咚-咚 雙擊，支援動態頻率加速)
     */
    fun playHeartbeatPulse(speedMultiplier: Float = 1.0f) {
        if (!isSfxEnabled) return
        scope.launch {
            val durationMs = 70
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            // 第一跳：75Hz
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = Math.sin(Math.PI * (i.toDouble() / numSamples))
                val wave = sin(2.0 * Math.PI * 75.0 * t)
                samples[i] = (wave * env * 0.6 * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(samples)

            val delayInterval = (90L / speedMultiplier.coerceAtLeast(1f)).toLong()
            delay(delayInterval)

            // 第二跳：90Hz 微弱
            val samples2 = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = Math.sin(Math.PI * (i.toDouble() / numSamples))
                val wave = sin(2.0 * Math.PI * 90.0 * t)
                samples2[i] = (wave * env * 0.45 * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(samples2)
        }
    }

    /**
     * 水果切切樂：炸彈爆炸沉悶轟鳴音
     */
    fun playBombExplosion() {
        if (!isSfxEnabled) return
        scope.launch {
            val durationMs = 220
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val decay = Math.exp(-14.0 * t)
                val wave = sin(2.0 * Math.PI * 85.0 * t) * 0.5 + (Math.random() - 0.5) * 0.5
                samples[i] = (wave * decay * 0.65 * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(samples)
        }
    }

    /**
     * 水果切切樂：冰凍香蕉寒冰晶瑩凝結音
     */
    fun playFreezeEffect() {
        if (!isSfxEnabled) return
        scope.launch {
            playTone(1318.51, 60, 0.28f) // E6
            delay(50)
            playTone(1567.98, 90, 0.32f) // G6
        }
    }

    /**
     * 水果切切樂：彩虹西瓜全場引爆衝擊音
     */
    fun playRainbowExplosion() {
        if (!isSfxEnabled) return
        scope.launch {
            val freqs = listOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
            for (f in freqs) {
                playTone(f, 40, 0.22f)
                delay(30)
            }
        }
    }

    /**
     * 水果切切樂：狂熱模式啟動歡呼衝擊音
     */
    fun playFeverFanfare() {
        if (!isSfxEnabled) return
        scope.launch {
            playTone(587.33, 60, 0.25f) // D5
            delay(50)
            playTone(880.00, 60, 0.3f)  // A5
            delay(50)
            playTone(1174.66, 120, 0.35f) // D6
        }
    }

    private fun generateToneSamples(freq: Double, durationMs: Int, volume: Float, isSawtooth: Boolean = false): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val wave = if (isSawtooth) {
                2.0 * (t * freq - Math.floor(t * freq + 0.5))
            } else {
                sin(2.0 * Math.PI * freq * t)
            }
            val attackSamples = (numSamples * 0.1).toInt().coerceAtLeast(1)
            val releaseSamples = (numSamples * 0.3).toInt().coerceAtLeast(1)
            val env = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i > numSamples - releaseSamples -> (numSamples - i).toDouble() / releaseSamples
                else -> 1.0
            }
            samples[i] = (wave * env * volume * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    private fun playTone(freq: Double, durationMs: Int, volume: Float, isSawtooth: Boolean = false) {
        try {
            val samples = generateToneSamples(freq, durationMs, volume, isSawtooth)
            playPcm(samples)
        } catch (_: Exception) {
        }
    }

    private fun playPcm(samples: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, samples.size * 2)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        scope.launch {
            delay(300)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
            }
        }
    }
}

