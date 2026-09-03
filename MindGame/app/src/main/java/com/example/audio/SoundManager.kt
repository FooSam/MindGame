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
        R.raw.bgm_07_melody_nature
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

    private var errorAudioTrack: AudioTrack? = null
    private var clickAudioTrack: AudioTrack? = null

    private fun initShortSfxTracks() {
        try {
            if (errorAudioTrack == null) {
                errorAudioTrack = createStaticTrack(cachedErrorSamples)
            }
            if (clickAudioTrack == null) {
                clickAudioTrack = createStaticTrack(cachedClickSamples)
            }
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

