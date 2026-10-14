package com.arcade.missionattack

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class SoundEngine {

    var sfxVolume: Float = 0.8f
    var musicVolume: Float = 0.25f

    private var audioTrack: AudioTrack? = null
    private var isPlayingMusic = false
    private var musicThread: Thread? = null

    // Melodia Stinger idêntica à versão WebAudio
    private val melodyNotes = doubleArrayOf(
        440.0, 523.2, 659.2, 523.2, 587.3, 659.2, 783.9, 659.2,
        349.2, 440.0, 523.2, 440.0, 392.0, 493.8, 587.3, 783.9,
        880.0, 659.2, 523.2, 659.2, 783.9, 587.3, 493.8, 587.3,
        698.4, 523.2, 440.0, 523.2, 659.2, 493.8, 587.3, 659.2
    )

    private val bassNotes = doubleArrayOf(
        110.0, 110.0, 220.0, 110.0, 110.0, 220.0, 110.0, 220.0,
        87.31, 87.31, 174.6, 87.31, 98.0, 98.0, 196.0, 98.0,
        110.0, 220.0, 110.0, 220.0, 98.0, 196.0, 98.0, 196.0,
        87.31, 174.6, 87.31, 174.6, 82.41, 164.8, 82.41, 164.8
    )

    fun startMusic() {
        if (isPlayingMusic) return
        isPlayingMusic = true
        musicThread = Thread {
            val sampleRate = 22050
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(max(minBufSize, 4096))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                var step = 0
                val noteDurationMs = 150
                val samplesPerNote = (sampleRate * (noteDurationMs / 1000.0)).toInt()
                val noteBuffer = ShortArray(samplesPerNote)

                while (isPlayingMusic) {
                    if (musicVolume <= 0f) {
                        Thread.sleep(100)
                        continue
                    }

                    val mFreq = melodyNotes[step % melodyNotes.size]
                    val bFreq = bassNotes[step % bassNotes.size]
                    step++

                    val vol = musicVolume * 0.35f
                    for (i in 0 until samplesPerNote) {
                        val t = i.toDouble() / sampleRate
                        val env = max(0.0, 1.0 - (i.toDouble() / samplesPerNote))
                        // Onda quadrada para melodia + onda triangular para baixo
                        val square = if (sin(2.0 * PI * mFreq * t) >= 0) 1.0 else -1.0
                        val tri = 2.0 * abs(2.0 * (t * bFreq - floor(t * bFreq + 0.5))) - 1.0
                        val sample = (square * 0.4 + tri * 0.6) * env * vol
                        noteBuffer[i] = (sample * 32767).toInt().coerceIn(-32768, 32767).toShort()
                    }

                    audioTrack?.write(noteBuffer, 0, samplesPerNote)
                }
            } catch (e: Exception) {
                // Tratamento seguro em caso de interrupção
            } finally {
                audioTrack?.release()
                audioTrack = null
            }
        }.apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    fun stopMusic() {
        isPlayingMusic = false
        musicThread?.interrupt()
        musicThread = null
    }

    fun playSfx(type: String) {
        if (sfxVolume <= 0f) return
        Thread {
            try {
                val sampleRate = 22050
                val samples = when (type) {
                    "laser" -> generateToneSweep(sampleRate, 0.08, 740.0, 130.0, "square", 0.35f * sfxVolume)
                    "laserBeam" -> generateToneSweep(sampleRate, 0.15, 1150.0, 260.0, "sawtooth", 0.40f * sfxVolume)
                    "explosion", "cataclysm" -> generateNoiseBurst(sampleRate, 0.35, 95.0, 0.50f * sfxVolume)
                    "turretBoom" -> generateNoiseBurst(sampleRate, 0.25, 140.0, 0.45f * sfxVolume)
                    "hit" -> generateToneSweep(sampleRate, 0.07, 210.0, 55.0, "triangle", 0.30f * sfxVolume)
                    "xp" -> generateDualTone(sampleRate, 0.09, 560.0, 840.0, 0.25f * sfxVolume)
                    "lvlup" -> generateArpeggio(sampleRate, 0.40, doubleArrayOf(440.0, 554.0, 659.0, 880.0), 0.45f * sfxVolume)
                    "hyperDash" -> generateToneSweep(sampleRate, 0.22, 420.0, 1100.0, "sawtooth", 0.45f * sfxVolume)
                    "shieldDeflect" -> generateToneSweep(sampleRate, 0.08, 920.0, 1400.0, "sine", 0.35f * sfxVolume)
                    "click" -> generateToneSweep(sampleRate, 0.04, 620.0, 400.0, "square", 0.20f * sfxVolume)
                    "countdownBeep" -> generateToneSweep(sampleRate, 0.12, 740.0, 987.0, "square", 0.35f * sfxVolume)
                    else -> generateToneSweep(sampleRate, 0.06, 440.0, 220.0, "square", 0.25f * sfxVolume)
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
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep((samples.size * 1000L / sampleRate) + 50)
                track.release()
            } catch (e: Exception) {
            }
        }.start()
    }

    private fun generateToneSweep(sampleRate: Int, duration: Double, startF: Double, endF: Double, wave: String, vol: Float): ShortArray {
        val total = (sampleRate * duration).toInt()
        val buf = ShortArray(total)
        var phase = 0.0
        for (i in 0 until total) {
            val t = i.toDouble() / total
            val freq = startF + (endF - startF) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = (1.0 - t).pow(1.5)
            val v = when (wave) {
                "square" -> if (sin(phase) >= 0) 1.0 else -1.0
                "sawtooth" -> 2.0 * (phase / (2.0 * PI) - floor(phase / (2.0 * PI) + 0.5))
                "triangle" -> 2.0 * abs(2.0 * (phase / (2.0 * PI) - floor(phase / (2.0 * PI) + 0.5))) - 1.0
                else -> sin(phase)
            }
            buf[i] = (v * env * vol * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }

    private fun generateNoiseBurst(sampleRate: Int, duration: Double, lowPass: Double, vol: Float): ShortArray {
        val total = (sampleRate * duration).toInt()
        val buf = ShortArray(total)
        var last = 0.0
        val rc = 1.0 / (2.0 * PI * lowPass)
        val dt = 1.0 / sampleRate
        val alpha = dt / (rc + dt)
        val random = java.util.Random()

        for (i in 0 until total) {
            val t = i.toDouble() / total
            val white = (random.nextDouble() * 2.0 - 1.0)
            last += alpha * (white - last)
            val env = (1.0 - t).pow(2.0)
            buf[i] = (last * env * vol * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }

    private fun generateDualTone(sampleRate: Int, duration: Double, f1: Double, f2: Double, vol: Float): ShortArray {
        val total = (sampleRate * duration).toInt()
        val half = total / 2
        val buf = ShortArray(total)
        for (i in 0 until total) {
            val freq = if (i < half) f1 else f2
            val t = i.toDouble() / sampleRate
            val sample = if (sin(2.0 * PI * freq * t) >= 0) 1.0 else -1.0
            buf[i] = (sample * vol * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }

    private fun generateArpeggio(sampleRate: Int, duration: Double, notes: DoubleArray, vol: Float): ShortArray {
        val total = (sampleRate * duration).toInt()
        val seg = total / notes.size
        val buf = ShortArray(total)
        for (i in 0 until total) {
            val noteIdx = min(notes.size - 1, i / seg)
            val freq = notes[noteIdx]
            val t = i.toDouble() / sampleRate
            val env = 1.0 - (i.toDouble() / total) * 0.4
            val sample = if (sin(2.0 * PI * freq * t) >= 0) 1.0 else -1.0
            buf[i] = (sample * env * vol * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buf
    }
}
