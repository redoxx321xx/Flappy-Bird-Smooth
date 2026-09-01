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

    // Pre-allocated static AudioTracks for instant, zero-latency playback
    private var flapTrack: AudioTrack? = null
    private var pointTrack: AudioTrack? = null
    private var milestoneTrack: AudioTrack? = null
    private var coinTrack: AudioTrack? = null
    private var hitTrack: AudioTrack? = null
    private var swooshTrack: AudioTrack? = null
    private var clickTrack: AudioTrack? = null
    private var welcomeTrack: AudioTrack? = null

    init {
        try {
            flapTrack = createStaticTrack(generateSatisfyingFlapSound())
            pointTrack = createStaticTrack(generateSatisfyingPointSound())
            milestoneTrack = createStaticTrack(generateMilestoneSound())
            coinTrack = createStaticTrack(generateSatisfyingCoinSound())
            hitTrack = createStaticTrack(generateSatisfyingHitSound())
            swooshTrack = createStaticTrack(generateSatisfyingSwooshSound())
            clickTrack = createStaticTrack(generateSatisfyingClickSound())
            welcomeTrack = createStaticTrack(generateWelcomeSound())
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

    fun playMilestone() {
        playTrack(milestoneTrack)
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

    fun playWelcome() {
        playTrack(welcomeTrack)
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

    // Ultra-satisfying ASMR bubble pop + soft acoustic resonant chime
    private fun generateSatisfyingFlapSound(): ShortArray {
        val duration = 0.09f // 90ms
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val startFreq = 520.0
        val endFreq = 960.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * (progress * progress)
            // Smooth bubble envelope with rapid attack and warm resonant body
            val envelope = (1.0 - progress) * (1.0 - exp(-progress * 30.0))
            val wave = sin(2.0 * PI * freq * t) + 0.25 * sin(4.0 * PI * freq * t) + 0.15 * sin(1.0 * PI * (startFreq * 0.5) * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.42).toInt().toShort()
        }
        return buffer
    }

    // Heavenly crystal double-ding chime for passing a pipe (C6 -> E6 -> G6 overtone)
    private fun generateSatisfyingPointSound(): ShortArray {
        val duration = 0.22f // 220ms
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
            val freq = if (isFirstHalf) 1046.50 else 1318.51 // C6 to E6
            val envelope = exp(-noteProgress * 4.2)
            // Pure bell crystal timbre with shimmer harmonic
            val wave = sin(2.0 * PI * freq * t) + 0.35 * sin(4.0 * PI * freq * t) + 0.15 * sin(6.0 * PI * freq * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.44).toInt().toShort()
        }
        return buffer
    }

    // Glorious milestone celebration fanfare chord (G5 - B5 - D6 - G6 cascade)
    private fun generateMilestoneSound(): ShortArray {
        val duration = 0.45f // 450ms
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val step = numSamples / 4
        val notes = doubleArrayOf(783.99, 987.77, 1174.66, 1567.98) // G5, B5, D6, G6

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val noteIndex = (i / step).coerceAtMost(3)
            val noteProg = (i % step).toDouble() / step
            val globalProg = i.toDouble() / numSamples
            val freq = notes[noteIndex]
            val envelope = exp(-noteProg * 3.5) * (1.0 - globalProg * 0.4)
            val wave = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * (freq * 2.0) * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.48).toInt().toShort()
        }
        return buffer
    }

    // Sparkling juicy gold coin chime
    private fun generateSatisfyingCoinSound(): ShortArray {
        val duration = 0.18f
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val half = numSamples / 2

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = if (i < half) 1318.51 else 1975.53 // E6 to B6
            val prog = if (i < half) i.toDouble() / half else (i - half).toDouble() / half
            val envelope = exp(-prog * 5.0)
            val wave = sin(2.0 * PI * freq * t) + 0.28 * sin(4.0 * PI * freq * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.44).toInt().toShort()
        }
        return buffer
    }

    // Soft warm retro impact with low-frequency resonance (gentle thud, satisfying)
    private fun generateSatisfyingHitSound(): ShortArray {
        val duration = 0.18f
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val envelope = exp(-progress * 9.0)
            val baseFreq = 180.0 * (1.0 - progress * 0.75)
            val tone = sin(2.0 * PI * baseFreq * t)
            val softNoise = (Math.random() * 2.0 - 1.0) * 0.35
            val mixed = (tone * 0.65 + softNoise * 0.35) * envelope
            buffer[i] = (mixed * Short.MAX_VALUE * 0.50).toInt().toShort()
        }
        return buffer
    }

    // Silky aerodynamic swoosh
    private fun generateSatisfyingSwooshSound(): ShortArray {
        val duration = 0.12f
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val envelope = sin(progress * PI)
            val freq = 220.0 + progress * 750.0
            val t = i.toDouble() / sampleRate
            val wave = sin(2.0 * PI * freq * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.38).toInt().toShort()
        }
        return buffer
    }

    // Clean tactile button click
    private fun generateSatisfyingClickSound(): ShortArray {
        val duration = 0.05f
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val envelope = exp(-progress * 14.0)
            val freq = 880.0
            val t = i.toDouble() / sampleRate
            val wave = sin(2.0 * PI * freq * t)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.35).toInt().toShort()
        }
        return buffer
    }

    // Welcome Screen Opening Harmonic Chord (C5 - E5 - G5 - C6)
    private fun generateWelcomeSound(): ShortArray {
        val duration = 0.6f // 600ms
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val envelope = (1.0 - progress) * (1.0 - exp(-progress * 15.0))
            val wave = (
                sin(2.0 * PI * 523.25 * t) +       // C5
                sin(2.0 * PI * 659.25 * t) * 0.8 + // E5
                sin(2.0 * PI * 783.99 * t) * 0.7 + // G5
                sin(2.0 * PI * 1046.50 * t) * 0.6  // C6
            ) * 0.28
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
        return buffer
    }

    fun release() {
        try {
            flapTrack?.release()
            pointTrack?.release()
            milestoneTrack?.release()
            coinTrack?.release()
            hitTrack?.release()
            swooshTrack?.release()
            clickTrack?.release()
            welcomeTrack?.release()
        } catch (_: Exception) {}
    }
}
