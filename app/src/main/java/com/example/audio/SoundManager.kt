package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager {
    var isMuted: Boolean = false

    private val sampleRate = 22050

    // Pre-allocated static AudioTracks for instant, zero-allocation playback
    private var flapTrack: AudioTrack? = null
    private var pointTrack: AudioTrack? = null
    private var coinTrack: AudioTrack? = null
    private var hitTrack: AudioTrack? = null
    private var swooshTrack: AudioTrack? = null
    private var clickTrack: AudioTrack? = null

    init {
        try {
            flapTrack = createStaticTrack(generateFlapSound())
            pointTrack = createStaticTrack(generatePointSound())
            coinTrack = createStaticTrack(generateCoinSound())
            hitTrack = createStaticTrack(generateHitSound())
            swooshTrack = createStaticTrack(generateSwooshSound())
            clickTrack = createStaticTrack(generateClickSound())
        } catch (_: Exception) {
            // Audio hardware fallback
        }
    }

    fun playFlap() {
        playTrack(flapTrack)
    }

    fun playPoint() {
        playTrack(pointTrack)
    }

    fun playCoin() {
        playTrack(coinTrack)
    }

    fun playHit() {
        playTrack(hitTrack)
    }

    fun playSwoosh() {
        playTrack(swooshTrack)
    }

    fun playClick() {
        playTrack(clickTrack)
    }

    private fun playTrack(track: AudioTrack?) {
        if (isMuted || track == null) return
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.setPlaybackHeadPosition(0)
            track.play()
        } catch (_: Exception) {
            // Ignore audio glitches safely
        }
    }

    private fun createStaticTrack(pcmData: ShortArray): AudioTrack? {
        return try {
            val bufferSize = pcmData.size * 2
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
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, pcmData.size)
            track
        } catch (_: Exception) {
            null
        }
    }

    // Generate classic retro rising chirp for flap
    private fun generateFlapSound(): ShortArray {
        val duration = 0.08f // 80ms
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val startFreq = 420.0
        val endFreq = 900.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * progress
            val envelope = (1.0 - progress) * (1.0 - exp(-progress * 25.0))
            val wave = sin(2.0 * PI * freq * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.45).toInt().toShort()
        }
        return buffer
    }

    // Generate classic double-ding chime for passing a pipe
    private fun generatePointSound(): ShortArray {
        val duration = 0.16f // 160ms
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val halfSamples = numSamples / 2

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val isFirstHalf = i < halfSamples
            val noteProgress = if (isFirstHalf) {
                i.toDouble() / halfSamples
            } else {
                (i - halfSamples).toDouble() / (numSamples - halfSamples)
            }
            val freq = if (isFirstHalf) 987.77 else 1318.51 // B5 to E6
            val envelope = exp(-noteProgress * 5.0)
            val wave = sin(2.0 * PI * freq * t) + 0.3 * sin(4.0 * PI * freq * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.4).toInt().toShort()
        }
        return buffer
    }

    // Generate sparkling high chime for coin pickup
    private fun generateCoinSound(): ShortArray {
        val duration = 0.14f
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val half = numSamples / 2

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = if (i < half) 1174.66 else 1760.0 // D6 to A6
            val prog = if (i < half) i.toDouble() / half else (i - half).toDouble() / half
            val envelope = exp(-prog * 6.0)
            val wave = sin(2.0 * PI * freq * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.4).toInt().toShort()
        }
        return buffer
    }

    // Generate thud & noise crunch for collision
    private fun generateHitSound(): ShortArray {
        val duration = 0.20f
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val envelope = exp(-progress * 8.0)
            val baseFreq = 160.0 * (1.0 - progress * 0.8)
            val tone = sin(2.0 * PI * baseFreq * t)
            val noise = (Math.random() * 2.0 - 1.0) * 0.6
            val mixed = (tone * 0.4 + noise * 0.6) * envelope
            buffer[i] = (mixed * Short.MAX_VALUE * 0.6).toInt().toShort()
        }
        return buffer
    }

    // Generate quick low-to-high whoosh
    private fun generateSwooshSound(): ShortArray {
        val duration = 0.10f
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val envelope = sin(progress * PI)
            val freq = 200.0 + progress * 600.0
            val t = i.toDouble() / sampleRate
            val wave = sin(2.0 * PI * freq * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.35).toInt().toShort()
        }
        return buffer
    }

    // Generate UI button click
    private fun generateClickSound(): ShortArray {
        val duration = 0.04f
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val envelope = exp(-progress * 12.0)
            val freq = 800.0
            val t = i.toDouble() / sampleRate
            val wave = sin(2.0 * PI * freq * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.3).toInt().toShort()
        }
        return buffer
    }

    fun release() {
        try {
            flapTrack?.release()
            pointTrack?.release()
            coinTrack?.release()
            hitTrack?.release()
            swooshTrack?.release()
            clickTrack?.release()
        } catch (_: Exception) {}
    }
}
