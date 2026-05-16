package com.stormquest.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*

object SoundManager {

    private const val SAMPLE_RATE = 22050
    var enabled = true
    private val rng = java.util.Random()

    // All amplitudes kept low (0.12–0.18) and every sound has fade-in + fade-out
    // to avoid the click/pop that occurs when audio starts or ends at non-zero amplitude.

    fun click()    = tone(880f,  55,  0.12f)
    fun select()   = arp(listOf(660f, 880f),            85,  0.13f)
    fun nextChar() = arp(listOf(523f, 659f, 784f),      95,  0.14f)
    fun attack()   = thump(110, 0.16f)
    fun magic()    = sweep(320f, 760f, 210, 0.13f)
    fun hit()      = tone(180f,  90,  0.16f, decay = true)
    fun victory()  = arp(listOf(523f, 659f, 784f, 1047f), 105, 0.14f)
    fun defeat()   = arp(listOf(370f, 330f, 294f, 220f),  125, 0.14f)

    private fun tone(freq: Float, ms: Int, vol: Float, decay: Boolean = false) {
        Thread {
            val n    = SAMPLE_RATE * ms / 1000
            val ramp = (SAMPLE_RATE * 0.010).toInt().coerceAtLeast(2)  // 10 ms ramp
            val buf  = ShortArray(n) { i ->
                val fadeIn  = if (i < ramp) i.toDouble() / ramp else 1.0
                val fadeOut = if (i > n - ramp) (n - i).toDouble() / ramp else 1.0
                val shape   = if (decay) 1.0 - i.toDouble() / n else 1.0
                (fadeIn * fadeOut * shape * vol * 32767 * sin(2 * PI * freq * i / SAMPLE_RATE)).toInt().toShort()
            }
            play(buf)
        }.start()
    }

    private fun arp(freqs: List<Float>, noteMs: Int, vol: Float) {
        Thread {
            val noteN = SAMPLE_RATE * noteMs / 1000
            val ramp  = (noteN * 0.08).toInt().coerceAtLeast(2)
            val buf   = ShortArray(noteN * freqs.size)
            freqs.forEachIndexed { idx, freq ->
                val off = idx * noteN
                for (i in 0 until noteN) {
                    val fadeIn  = if (i < ramp) i.toDouble() / ramp else 1.0
                    val fadeOut = if (i > noteN - ramp) (noteN - i).toDouble() / ramp else 1.0
                    val decay   = 1.0 - i.toDouble() / noteN * 0.65
                    val t       = (off + i).toDouble() / SAMPLE_RATE
                    buf[off + i] = (fadeIn * fadeOut * decay * vol * 32767 * sin(2 * PI * freq * t)).toInt().toShort()
                }
            }
            play(buf)
        }.start()
    }

    // Low-frequency thump with minimal noise — much gentler than white noise burst
    private fun thump(ms: Int, vol: Float) {
        Thread {
            val n    = SAMPLE_RATE * ms / 1000
            val ramp = (n * 0.06).toInt().coerceAtLeast(2)
            val buf  = ShortArray(n) { i ->
                val fadeIn  = if (i < ramp) i.toDouble() / ramp else 1.0
                val decay   = (1.0 - i.toDouble() / n).pow(1.8)
                val body    = sin(2 * PI * 75.0 * i / SAMPLE_RATE) * 0.78
                val texture = (rng.nextDouble() * 2 - 1) * 0.22
                (fadeIn * decay * vol * 32767 * (body + texture)).toInt().toShort()
            }
            play(buf)
        }.start()
    }

    private fun sweep(f0: Float, f1: Float, ms: Int, vol: Float) {
        Thread {
            val n    = SAMPLE_RATE * ms / 1000
            val dur  = ms / 1000.0
            val ramp = (n * 0.10).toInt().coerceAtLeast(2)
            val buf  = ShortArray(n) { i ->
                val t       = i.toDouble() / SAMPLE_RATE
                val fadeIn  = if (i < ramp) i.toDouble() / ramp else 1.0
                val fadeOut = if (i > n - ramp) (n - i).toDouble() / ramp else 1.0
                val phase   = 2 * PI * (f0 * t + (f1 - f0) * t * t / (2 * dur))
                (fadeIn * fadeOut * vol * 32767 * sin(phase)).toInt().toShort()
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
            Thread.sleep(buffer.size.toLong() * 1000L / SAMPLE_RATE + 60L)
            track.stop()
            track.release()
        } catch (_: Exception) {}
    }
}
