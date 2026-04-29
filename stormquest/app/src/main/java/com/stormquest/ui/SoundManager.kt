package com.stormquest.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*

object SoundManager {

    private const val SAMPLE_RATE = 22050
    var enabled = true

    fun click()   = playNote(880f,  50, 0.30f, decay = false)
    fun select()  = playArp(listOf(660f, 880f), 90, 0.35f)
    fun nextChar()= playArp(listOf(523f, 659f, 784f), 100, 0.38f)
    fun attack()  = playNoise(130, 0.45f)
    fun magic()   = playSweep(300f, 800f, 220, 0.40f)
    fun hit()     = playNote(200f, 100, 0.50f, decay = true)
    fun victory() = playArp(listOf(523f, 659f, 784f, 1047f), 110, 0.40f)
    fun defeat()  = playArp(listOf(400f, 350f, 300f, 220f), 130, 0.40f)

    private fun playNote(freq: Float, ms: Int, vol: Float, decay: Boolean) {
        Thread {
            val n = SAMPLE_RATE * ms / 1000
            val buf = ShortArray(n) { i ->
                val progress = i.toDouble() / n
                val env = if (decay) 1.0 - progress else minOf(1.0, progress / 0.1)
                (env * vol * 32767 * sin(2 * PI * freq * i / SAMPLE_RATE)).toInt().toShort()
            }
            play(buf)
        }.start()
    }

    private fun playArp(freqs: List<Float>, noteMs: Int, vol: Float) {
        Thread {
            val noteN = SAMPLE_RATE * noteMs / 1000
            val total = noteN * freqs.size
            val buf = ShortArray(total)
            freqs.forEachIndexed { idx, freq ->
                val offset = idx * noteN
                for (i in 0 until noteN) {
                    val progress = i.toDouble() / noteN
                    val env = if (progress < 0.05) progress / 0.05 else 1.0 - progress
                    val t = (offset + i).toDouble() / SAMPLE_RATE
                    buf[offset + i] = (env * vol * 32767 * sin(2 * PI * freq * t)).toInt().toShort()
                }
            }
            play(buf)
        }.start()
    }

    private fun playNoise(ms: Int, vol: Float) {
        val rng = java.util.Random()
        Thread {
            val n = SAMPLE_RATE * ms / 1000
            val buf = ShortArray(n) { i ->
                val env = 1.0 - i.toDouble() / n
                (env * vol * 32767 * (rng.nextDouble() * 2 - 1)).toInt().toShort()
            }
            play(buf)
        }.start()
    }

    private fun playSweep(f0: Float, f1: Float, ms: Int, vol: Float) {
        Thread {
            val n = SAMPLE_RATE * ms / 1000
            val dur = ms / 1000.0
            val buf = ShortArray(n) { i ->
                val t = i.toDouble() / SAMPLE_RATE
                val progress = t / dur
                val phase = 2 * PI * (f0 * t + (f1 - f0) * t * t / (2 * dur))
                val env = if (progress < 0.1) progress / 0.1 else 1.0 - progress
                (env * vol * 32767 * sin(phase)).toInt().toShort()
            }
            play(buf)
        }.start()
    }

    private fun play(buffer: ShortArray) {
        if (!enabled) return
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(buffer.size.toLong() * 1000L / SAMPLE_RATE + 50L)
            track.stop()
            track.release()
        } catch (_: Exception) {}
    }
}
