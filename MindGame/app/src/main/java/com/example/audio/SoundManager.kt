package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var bgmJob: Job? = null

    var isSfxEnabled: Boolean = true
    var isBgmEnabled: Boolean = false

    private const val SAMPLE_RATE = 22050

    fun playClick() {
        if (!isSfxEnabled) return
        scope.launch {
            playTone(880.0, 45, 0.2f)
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
        scope.launch {
            playTone(180.0, 120, 0.35f, isSawtooth = true)
        }
    }

    fun playCatMeow() {
        if (!isSfxEnabled) return
        scope.launch {
            // Frequency sweep to simulate cute meow chime
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

    fun setBgmState(enabled: Boolean) {
        isBgmEnabled = enabled
        if (enabled) {
            startBgm()
        } else {
            stopBgm()
        }
    }

    private fun startBgm() {
        bgmJob?.cancel()
        bgmJob = scope.launch {
            // Calm ambient pentatonic arpeggio sequence
            val bgmNotes = listOf(
                261.63, 293.66, 329.63, 392.00, 440.00, 523.25,
                392.00, 329.63, 293.66, 261.63, 329.63, 392.00
            )
            var index = 0
            while (isActive && isBgmEnabled) {
                val freq = bgmNotes[index % bgmNotes.size]
                playTone(freq, 220, 0.08f)
                index++
                delay(350)
            }
        }
    }

    private fun stopBgm() {
        bgmJob?.cancel()
        bgmJob = null
    }

    private fun playTone(freq: Double, durationMs: Int, volume: Float, isSawtooth: Boolean = false) {
        try {
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val wave = if (isSawtooth) {
                    2.0 * (t * freq - Math.floor(t * freq + 0.5))
                } else {
                    sin(2.0 * Math.PI * freq * t)
                }
                // Smooth attack and release envelope to prevent click artifacts
                val attackSamples = (numSamples * 0.1).toInt().coerceAtLeast(1)
                val releaseSamples = (numSamples * 0.3).toInt().coerceAtLeast(1)
                val env = when {
                    i < attackSamples -> i.toDouble() / attackSamples
                    i > numSamples - releaseSamples -> (numSamples - i).toDouble() / releaseSamples
                    else -> 1.0
                }
                samples[i] = (wave * env * volume * Short.MAX_VALUE).toInt().toShort()
            }
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
