package com.example.sweetrush.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
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

/**
 * Audio engine for Sweet Rush: Candy World.
 * Plays the studio MP3 background soundtracks ("Gumball Horizon" & "Turbo Candy Drift")
 * from res/raw via MediaPlayer with continuous playlist cycling, real-time volume control,
 * and lifecycle pause/resume support, plus lightweight arcade SFX cues.
 */
class CandyAudioEngine(private val context: Context? = null) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val playerLock = Any()

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: MusicTrack = MusicTrack.NONE
    private var activeRawResId: Int = 0
    private var isPausedByLifecycle: Boolean = false

    @Volatile
    var musicVolume: Float = 0.72f

    @Volatile
    var sfxVolume: Float = 0.85f

    private val sampleRate = 22050

    private val playlistResIds = intArrayOf(
        R.raw.gumball_horizon,
        R.raw.turbo_candy_drift
    )

    fun currentSongTitle(): String = when (activeRawResId) {
        R.raw.turbo_candy_drift -> "Turbo Candy Drift"
        R.raw.gumball_horizon -> "Gumball Horizon"
        else -> "Sweet Rush OST"
    }

    fun setVolumes(music: Float, sfx: Float) {
        musicVolume = music.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        synchronized(playerLock) {
            try {
                mediaPlayer?.setVolume(musicVolume, musicVolume)
            } catch (_: Exception) {
            }
        }
    }

    fun playMusic(track: MusicTrack) {
        synchronized(playerLock) {
            if (track == MusicTrack.NONE) {
                currentTrack = MusicTrack.NONE
                stopAndReleasePlayerLocked()
                return
            }

            val preferredResId = when (track) {
                MusicTrack.RACE -> R.raw.turbo_candy_drift
                MusicTrack.MENU, MusicTrack.VICTORY -> R.raw.gumball_horizon
                MusicTrack.NONE -> 0
            }

            // If the preferred MP3 is already playing, keep it playing smoothly without restarting
            if (currentTrack == track && activeRawResId == preferredResId && mediaPlayer != null) {
                try {
                    mediaPlayer?.setVolume(musicVolume, musicVolume)
                    if (!isPausedByLifecycle && mediaPlayer?.isPlaying == false) {
                        mediaPlayer?.start()
                    }
                    return
                } catch (_: Exception) {
                }
            }

            // If switching between MENU and VICTORY while Gumball Horizon is already playing, keep continuity
            if ((track == MusicTrack.MENU || track == MusicTrack.VICTORY) &&
                activeRawResId == R.raw.gumball_horizon &&
                mediaPlayer != null
            ) {
                currentTrack = track
                try {
                    mediaPlayer?.setVolume(musicVolume, musicVolume)
                    if (!isPausedByLifecycle && mediaPlayer?.isPlaying == false) {
                        mediaPlayer?.start()
                    }
                    return
                } catch (_: Exception) {
                }
            }

            currentTrack = track
            startRawTrackLocked(preferredResId)
        }
    }

    /**
     * Switches between the two studio MP3 background tracks ("Gumball Horizon" <-> "Turbo Candy Drift").
     */
    fun switchBackgroundSong(): String {
        synchronized(playerLock) {
            val nextRes = if (activeRawResId == R.raw.turbo_candy_drift) {
                R.raw.gumball_horizon
            } else {
                R.raw.turbo_candy_drift
            }
            if (currentTrack == MusicTrack.NONE) {
                currentTrack = MusicTrack.MENU
            }
            startRawTrackLocked(nextRes)
            return currentSongTitle()
        }
    }

    private fun startRawTrackLocked(resId: Int) {
        stopAndReleasePlayerLocked()
        val appCtx = context?.applicationContext ?: return
        if (resId == 0) return

        try {
            val mp = MediaPlayer.create(appCtx, resId) ?: return
            activeRawResId = resId
            mp.isLooping = false
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setVolume(musicVolume, musicVolume)
            mp.setOnCompletionListener {
                // Automatically alternate to the other uploaded soundtrack MP3 when a song finishes
                synchronized(playerLock) {
                    if (currentTrack != MusicTrack.NONE) {
                        val nextIdx = (playlistResIds.indexOf(resId) + 1).mod(playlistResIds.size)
                        startRawTrackLocked(playlistResIds[nextIdx])
                    }
                }
            }
            mediaPlayer = mp
            if (!isPausedByLifecycle) {
                mp.start()
            }
        } catch (_: Exception) {
            activeRawResId = resId
        }
    }

    fun pauseMusic() {
        synchronized(playerLock) {
            isPausedByLifecycle = true
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun resumeMusic() {
        synchronized(playerLock) {
            isPausedByLifecycle = false
            try {
                if (currentTrack != MusicTrack.NONE && mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.setVolume(musicVolume, musicVolume)
                    mediaPlayer?.start()
                } else if (currentTrack != MusicTrack.NONE && mediaPlayer == null) {
                    val res = if (activeRawResId != 0) activeRawResId else R.raw.gumball_horizon
                    startRawTrackLocked(res)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun stopAndReleasePlayerLocked() {
        val mp = mediaPlayer
        mediaPlayer = null
        if (mp != null) {
            try {
                mp.setOnCompletionListener(null)
                if (mp.isPlaying) {
                    mp.stop()
                }
            } catch (_: Exception) {
            }
            try {
                mp.release()
            } catch (_: Exception) {
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
                pcm[i] = (wave * env * vol * 0.26 * 32767.0).toInt()
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
        synchronized(playerLock) {
            currentTrack = MusicTrack.NONE
            activeRawResId = 0
            stopAndReleasePlayerLocked()
        }
    }

    private data class Quad(val ms: Int, val startHz: Float, val endHz: Float, val harm: Float)
}
