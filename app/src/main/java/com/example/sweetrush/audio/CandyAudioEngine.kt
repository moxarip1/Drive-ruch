package com.example.sweetrush.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class MusicTrack {
    NONE,
    MENU,
    RACE,
    VICTORY
}

enum class SoundEffect {
    UI_CLICK,
    COUNTDOWN_BEEP,
    COUNTDOWN_GO,
    DRIFT_SPARK,
    NITRO_BOOST,
    JUMP,
    POWERUP_PICKUP,
    POWERUP_USE,
    COLLISION,
    LAP_COMPLETE
}

class CandyAudioEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var musicJob: Job? = null
    private var currentTrack: MusicTrack = MusicTrack.NONE

    @Volatile
    var musicVolume: Float = 0.70f

    @Volatile
    var sfxVolume: Float = 0.85f

    private val sampleRate = 22050

    fun setVolumes(music: Float, sfx: Float) {
        musicVolume = music.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
    }

    fun playMusic(track: MusicTrack) {
        if (currentTrack == track && musicJob?.isActive == true) return
        currentTrack = track
        musicJob?.cancel()
        if (track == MusicTrack.NONE) return

        musicJob = scope.launch {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val audioTrack = try {
                AudioTrack.Builder()
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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                return@launch
            }

            try {
                audioTrack.play()
                val notes = when (track) {
                    MusicTrack.MENU -> floatArrayOf(
                        523.25f, 659.25f, 783.99f, 1046.50f,
                        880.00f, 783.99f, 659.25f, 587.33f,
                        523.25f, 659.25f, 783.99f, 659.25f,
                        587.33f, 698.46f, 880.00f, 783.99f
                    )
                    MusicTrack.RACE -> floatArrayOf(
                        523.25f, 523.25f, 659.25f, 783.99f,
                        698.46f, 783.99f, 880.00f, 1046.50f,
                        987.77f, 880.00f, 783.99f, 659.25f,
                        587.33f, 659.25f, 783.99f, 1046.50f
                    )
                    MusicTrack.VICTORY -> floatArrayOf(
                        523.25f, 659.25f, 783.99f, 1046.50f,
                        1046.50f, 880.00f, 1046.50f, 1318.51f
                    )
                    MusicTrack.NONE -> floatArrayOf(0f)
                }

                val stepSamples = when (track) {
                    MusicTrack.RACE -> sampleRate / 6 // upbeat 180 BPM eighth-notes
                    MusicTrack.VICTORY -> sampleRate / 4
                    else -> sampleRate / 4
                }

                val chunk = ShortArray(stepSamples)
                var stepIdx = 0
                var phaseLead = 0.0
                var phaseBass = 0.0

                while (isActive) {
                    val vol = musicVolume * 0.22f
                    val freq = notes[stepIdx % notes.size]
                    val bassFreq = freq * 0.25f

                    for (i in 0 until stepSamples) {
                        val tNorm = i.toFloat() / stepSamples.toFloat()
                        val env = exp(-tNorm * 3.2f)
                        phaseLead += 2.0 * PI * freq / sampleRate
                        phaseBass += 2.0 * PI * bassFreq / sampleRate

                        // Warm candy square-triangle blend
                        val leadWave = (sin(phaseLead) * 0.65 + sin(phaseLead * 2.0) * 0.20)
                        val bassWave = sin(phaseBass) * 0.45
                        val sample = ((leadWave * env + bassWave * 0.6) * vol * 32767.0).toInt()
                            .coerceIn(-32767, 32767).toShort()
                        chunk[i] = sample
                    }
                    audioTrack.write(chunk, 0, chunk.size)
                    stepIdx++
                }
            } catch (_: Exception) {
                // Safe audio fallback in headless/test environments
            } finally {
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun playSfx(effect: SoundEffect) {
        val vol = sfxVolume
        if (vol <= 0.01f) return

        scope.launch {
            val (durationMs, startHz, endHz, harmonic) = when (effect) {
                SoundEffect.UI_CLICK -> Quad(65, 680f, 920f, 0.2f)
                SoundEffect.COUNTDOWN_BEEP -> Quad(180, 520f, 520f, 0.3f)
                SoundEffect.COUNTDOWN_GO -> Quad(340, 780f, 1180f, 0.4f)
                SoundEffect.DRIFT_SPARK -> Quad(140, 420f, 760f, 0.5f)
                SoundEffect.NITRO_BOOST -> Quad(290, 260f, 880f, 0.45f)
                SoundEffect.JUMP -> Quad(190, 320f, 720f, 0.25f)
                SoundEffect.POWERUP_PICKUP -> Quad(240, 580f, 1160f, 0.35f)
                SoundEffect.POWERUP_USE -> Quad(220, 440f, 960f, 0.4f)
                SoundEffect.COLLISION -> Quad(160, 190f, 75f, 0.6f)
                SoundEffect.LAP_COMPLETE -> Quad(360, 660f, 1320f, 0.3f)
            }

            val count = (sampleRate * durationMs) / 1000
            val pcm = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val p = i.toFloat() / count.toFloat()
                val hz = startHz + (endHz - startHz) * p
                val env = (1f - p) * (1f - p * 0.4f)
                phase += 2.0 * PI * hz / sampleRate
                val wave = sin(phase) + sin(phase * 2.0) * harmonic
                pcm[i] = (wave * env * vol * 0.30 * 32767.0).toInt()
                    .coerceIn(-32767, 32767).toShort()
            }

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
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                kotlinx.coroutines.delay(durationMs.toLong() + 40L)
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }
    }

    fun release() {
        musicJob?.cancel()
        currentTrack = MusicTrack.NONE
    }

    private data class Quad(val ms: Int, val startHz: Float, val endHz: Float, val harm: Float)
}
