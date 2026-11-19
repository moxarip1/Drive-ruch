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
import kotlin.math.tanh

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
    var musicVolume: Float = 0.72f

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
            ).coerceAtLeast(4096)

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

                // 32-step high-energy polyphonic Sugar-Pop Synthwave & Arcade Racing arrangements
                val leadNotes = when (track) {
                    MusicTrack.MENU -> floatArrayOf(
                        523.25f, 659.25f, 783.99f, 987.77f, 1046.50f, 987.77f, 783.99f, 659.25f,
                        587.33f, 698.46f, 880.00f, 1046.50f, 1174.66f, 1046.50f, 880.00f, 698.46f,
                        659.25f, 783.99f, 987.77f, 1174.66f, 1318.51f, 1174.66f, 987.77f, 783.99f,
                        587.33f, 783.99f, 880.00f, 987.77f, 1046.50f, 880.00f, 783.99f, 659.25f
                    )
                    MusicTrack.RACE -> floatArrayOf(
                        523.25f, 0f, 659.25f, 783.99f, 1046.50f, 0f, 987.77f, 880.00f,
                        783.99f, 659.25f, 783.99f, 880.00f, 1046.50f, 1174.66f, 1318.51f, 0f,
                        1174.66f, 1046.50f, 880.00f, 698.46f, 880.00f, 1046.50f, 1174.66f, 0f,
                        987.77f, 880.00f, 783.99f, 880.00f, 987.77f, 1174.66f, 1318.51f, 1567.98f
                    )
                    MusicTrack.VICTORY -> floatArrayOf(
                        523.25f, 659.25f, 783.99f, 1046.50f, 1046.50f, 0f, 880.00f, 1046.50f,
                        1318.51f, 1174.66f, 1046.50f, 987.77f, 1046.50f, 1318.51f, 1567.98f, 0f
                    )
                    MusicTrack.NONE -> floatArrayOf(0f)
                }

                // Driving octave bass root progression (Hz)
                val bassRoots = when (track) {
                    MusicTrack.MENU -> floatArrayOf(
                        130.81f, 261.63f, 130.81f, 196.00f, 146.83f, 293.66f, 146.83f, 220.00f,
                        164.81f, 329.63f, 164.81f, 246.94f, 196.00f, 196.00f, 146.83f, 196.00f
                    )
                    MusicTrack.RACE -> floatArrayOf(
                        130.81f, 261.63f, 130.81f, 261.63f, 164.81f, 329.63f, 196.00f, 261.63f,
                        146.83f, 293.66f, 146.83f, 293.66f, 196.00f, 392.00f, 196.00f, 246.94f
                    )
                    MusicTrack.VICTORY -> floatArrayOf(
                        130.81f, 261.63f, 196.00f, 261.63f, 174.61f, 349.23f, 196.00f, 261.63f
                    )
                    MusicTrack.NONE -> floatArrayOf(0f)
                }

                // Shimmering triad chord arp accompaniment (Hz)
                val arpNotes = floatArrayOf(
                    523.25f, 659.25f, 783.99f, 987.77f,
                    587.33f, 698.46f, 880.00f, 1046.50f,
                    659.25f, 783.99f, 987.77f, 1174.66f,
                    587.33f, 783.99f, 987.77f, 1174.66f
                )

                val stepSamples = when (track) {
                    MusicTrack.RACE -> (sampleRate * 60) / (152 * 4) // 152 BPM crisp 16th-note pulse
                    MusicTrack.VICTORY -> (sampleRate * 60) / (138 * 4)
                    else -> (sampleRate * 60) / (118 * 4)
                }

                val chunk = ShortArray(stepSamples)
                var stepIdx = 0
                var phaseLead = 0.0
                var phaseLeadSub = 0.0
                var phaseBass = 0.0
                var phaseArp = 0.0
                var phaseKick = 0.0
                var lfsr = 0x5A3F19u

                while (isActive) {
                    val masterVol = musicVolume * 0.34
                    val leadFreq = leadNotes[stepIdx % leadNotes.size]
                    val bassFreq = bassRoots[(stepIdx / 2) % bassRoots.size] * (if (stepIdx % 2 == 1) 2f else 1f)
                    val arpFreq = arpNotes[(stepIdx * 3) % arpNotes.size]

                    // 4-on-the-floor arcade drum pattern
                    val beatStep = stepIdx % 16
                    val hasKick = (track != MusicTrack.MENU && (beatStep % 4 == 0)) ||
                        (track == MusicTrack.MENU && (beatStep == 0 || beatStep == 6 || beatStep == 10))
                    val hasSnare = (track != MusicTrack.MENU && (beatStep == 4 || beatStep == 12))
                    val hasHat = (beatStep % 2 == 1) || (track == MusicTrack.RACE)

                    if (hasKick) phaseKick = 0.0

                    for (i in 0 until stepSamples) {
                        val tNorm = i.toFloat() / stepSamples.toFloat()

                        // 1) Plucky FM-tinted Candy Lead Synth
                        var leadSample = 0.0
                        if (leadFreq > 1f) {
                            val envLead = exp(-tNorm * if (track == MusicTrack.RACE) 2.5 else 2.0)
                            phaseLead += 2.0 * PI * leadFreq / sampleRate
                            phaseLeadSub += 2.0 * PI * (leadFreq * 0.5) / sampleRate
                            val brightMod = sin(phaseLead * 2.0) * 0.55 * envLead
                            val leadWave = sin(phaseLead + brightMod) * 0.65 +
                                sin(phaseLead * 3.0) * 0.14 +
                                sin(phaseLeadSub) * 0.22
                            leadSample = leadWave * envLead * 0.42
                        }

                        // 2) Punchy Octave Saw/Tri Bassline
                        phaseBass += 2.0 * PI * bassFreq / sampleRate
                        val bassEnv = exp(-tNorm * 2.2)
                        val sawApprox = sin(phaseBass) * 0.72 + sin(phaseBass * 2.0) * 0.34 + sin(phaseBass * 3.0) * 0.16
                        val bassSample = sawApprox * bassEnv * 0.42

                        // 3) Sparkling Candy Bell Arpeggio
                        phaseArp += 2.0 * PI * arpFreq / sampleRate
                        val arpEnv = exp(-tNorm * 5.2)
                        val arpSample = (sin(phaseArp) + sin(phaseArp * 2.0) * 0.25) * arpEnv * 0.17

                        // 4) Drum Groove (Kick + Crisp Snare + Tight Hi-Hat)
                        var drumSample = 0.0
                        if (hasKick) {
                            val kickHz = 138.0 * exp(-tNorm * 6.5) + 46.0
                            phaseKick += 2.0 * PI * kickHz / sampleRate
                            val kickEnv = exp(-tNorm * 5.2)
                            drumSample += sin(phaseKick) * kickEnv * 0.55
                        }
                        if (hasSnare || hasHat) {
                            lfsr = (lfsr shl 1) xor (if ((lfsr and 0x80000000u) != 0u) 0x04C11DB7u else 0u)
                            val whiteNoise = ((lfsr and 0xFFFFu).toDouble() / 32768.0) - 1.0
                            if (hasSnare) {
                                val snareEnv = exp(-tNorm * 7.5)
                                drumSample += (whiteNoise * 0.42 + sin(2.0 * PI * 195.0 * tNorm) * 0.25) * snareEnv * 0.40
                            } else if (hasHat) {
                                val hatEnv = exp(-tNorm * 14.0)
                                drumSample += whiteNoise * hatEnv * 0.14
                            }
                        }

                        val mixed = (leadSample + bassSample + arpSample + drumSample) * masterVol * 1.45
                        val saturated = tanh(mixed)
                        chunk[i] = (saturated * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
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
