package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundEffects {
    @Volatile
    var enabled: Boolean = true

    private val executor = Executors.newSingleThreadExecutor()
    private val sampleRate = 22050

    fun playLaunch(power: Float) {
        if (!enabled) return
        val startFreq = 190f + power * 140f
        val endFreq = 460f + power * 360f
        playSweep(startFreq = startFreq, endFreq = endFreq, durationMs = 150, volume = 0.28f)
    }

    fun playAirFlip() {
        if (!enabled) return
        playSweep(startFreq = 360f, endFreq = 720f, durationMs = 110, volume = 0.24f)
    }

    fun playLand(isPerfect: Boolean, streak: Int) {
        if (!enabled) return
        if (isPerfect) {
            val baseFreq = 523.25f * (1f + (streak.coerceAtMost(6) * 0.06f))
            playTwoTone(baseFreq, baseFreq * 1.25f, durationMs = 170, volume = 0.30f)
        } else {
            playSweep(startFreq = 260f, endFreq = 160f, durationMs = 95, volume = 0.26f)
        }
    }

    fun playWobbleSave() {
        if (!enabled) return
        playTwoTone(340f, 440f, durationMs = 150, volume = 0.22f)
    }

    fun playCrash() {
        if (!enabled) return
        playSweep(startFreq = 340f, endFreq = 88f, durationMs = 280, volume = 0.32f)
    }

    fun playClick() {
        if (!enabled) return
        playSweep(startFreq = 620f, endFreq = 480f, durationMs = 45, volume = 0.16f)
    }

    private fun playSweep(startFreq: Float, endFreq: Float, durationMs: Int, volume: Float) {
        executor.execute {
            runCatching {
                val count = (sampleRate * durationMs) / 1000
                val buffer = ShortArray(count)
                var phase = 0.0
                for (i in 0 until count) {
                    val t = i.toFloat() / count
                    val freq = startFreq + (endFreq - startFreq) * t
                    val env = exp((-3.2f * t).toDouble()).toFloat() * (1f - t * 0.25f)
                    phase += 2.0 * PI * freq / sampleRate
                    val sample = sin(phase) * volume * env
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                writePcm(buffer)
            }
        }
    }

    private fun playTwoTone(f1: Float, f2: Float, durationMs: Int, volume: Float) {
        executor.execute {
            runCatching {
                val count = (sampleRate * durationMs) / 1000
                val half = count / 2
                val buffer = ShortArray(count)
                var phase = 0.0
                for (i in 0 until count) {
                    val t = i.toFloat() / count
                    val freq = if (i < half) f1 else f2
                    val localT = if (i < half) i.toFloat() / half else (i - half).toFloat() / (count - half)
                    val env = exp((-3.0f * localT).toDouble()).toFloat()
                    phase += 2.0 * PI * freq / sampleRate
                    val sample = sin(phase) * volume * env
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                writePcm(buffer)
            }
        }
    }

    private fun writePcm(samples: ShortArray) {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(samples.size * 2)

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
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        track.play()
        val sleepMs = (samples.size * 1000L) / sampleRate + 25L
        Thread.sleep(sleepMs)
        track.stop()
        track.release()
    }

    fun release() {
        executor.shutdownNow()
    }
}
