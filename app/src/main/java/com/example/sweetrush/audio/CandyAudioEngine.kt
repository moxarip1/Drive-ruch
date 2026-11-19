package com.example.sweetrush.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
 * Exclusively plays the user-uploaded studio MP3 background soundtracks
 * ("Turbo Candy Drift" & "Gumball Horizon") from res/raw via MediaPlayer.
 * All legacy procedural sine-wave noise generation has been removed so only
 * the uploaded MP3 soundtracks play cleanly.
 */
class CandyAudioEngine(private val context: Context? = null) {

    private val playerLock = Any()

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: MusicTrack = MusicTrack.NONE
    private var activeRawResId: Int = R.raw.turbo_candy_drift
    private var userManuallySelectedSong: Boolean = false
    private var isPausedByLifecycle: Boolean = false

    private val _activeSongTitle = MutableStateFlow("Turbo Candy Drift")
    val activeSongTitle: StateFlow<String> = _activeSongTitle.asStateFlow()

    private val _isMusicEnabled = MutableStateFlow(true)
    val isMusicEnabled: StateFlow<Boolean> = _isMusicEnabled.asStateFlow()

    private var savedNonZeroMusicVolume: Float = 0.85f

    @Volatile
    var musicVolume: Float = 0.85f

    @Volatile
    var sfxVolume: Float = 0.85f

    private val playlistResIds = intArrayOf(
        R.raw.turbo_candy_drift,
        R.raw.gumball_horizon
    )

    fun currentSongTitle(): String = when (activeRawResId) {
        R.raw.turbo_candy_drift -> "Turbo Candy Drift"
        R.raw.gumball_horizon -> "Gumball Horizon"
        else -> "Turbo Candy Drift"
    }

    fun isPlayingMusic(): Boolean {
        synchronized(playerLock) {
            return try {
                mediaPlayer?.isPlaying == true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun setVolumes(music: Float, sfx: Float) {
        musicVolume = music.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        val enabled = musicVolume > 0.01f
        if (enabled) {
            savedNonZeroMusicVolume = musicVolume
        }
        _isMusicEnabled.value = enabled
        synchronized(playerLock) {
            try {
                mediaPlayer?.setVolume(musicVolume, musicVolume)
                if (!enabled) {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                    }
                } else if (currentTrack != MusicTrack.NONE && !isPausedByLifecycle) {
                    if (mediaPlayer == null) {
                        val targetRes = if (activeRawResId != 0) activeRawResId else R.raw.turbo_candy_drift
                        startRawTrackLocked(targetRes)
                    } else if (mediaPlayer?.isPlaying == false) {
                        mediaPlayer?.start()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun setMusicEnabled(enabled: Boolean): Boolean {
        synchronized(playerLock) {
            _isMusicEnabled.value = enabled
            if (!enabled) {
                if (musicVolume > 0.05f) {
                    savedNonZeroMusicVolume = musicVolume
                }
                musicVolume = 0f
                try {
                    mediaPlayer?.setVolume(0f, 0f)
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                    }
                } catch (_: Exception) {
                }
            } else {
                musicVolume = savedNonZeroMusicVolume.coerceIn(0.65f, 1.0f)
                if (currentTrack == MusicTrack.NONE) {
                    currentTrack = MusicTrack.MENU
                }
                try {
                    val mp = mediaPlayer
                    if (mp != null) {
                        mp.setVolume(musicVolume, musicVolume)
                        if (!isPausedByLifecycle && !mp.isPlaying) {
                            mp.start()
                        }
                    } else {
                        val targetRes = if (activeRawResId != 0) activeRawResId else R.raw.turbo_candy_drift
                        startRawTrackLocked(targetRes)
                    }
                } catch (_: Exception) {
                    val targetRes = if (activeRawResId != 0) activeRawResId else R.raw.turbo_candy_drift
                    startRawTrackLocked(targetRes)
                }
            }
            return _isMusicEnabled.value
        }
    }

    fun toggleMusicEnabled(): Boolean {
        val next = !_isMusicEnabled.value
        return setMusicEnabled(next)
    }

    fun playMusic(track: MusicTrack) {
        synchronized(playerLock) {
            if (track == MusicTrack.NONE) {
                currentTrack = MusicTrack.NONE
                stopAndReleasePlayerLocked()
                return
            }

            val preferredResId = if (userManuallySelectedSong && activeRawResId != 0) {
                activeRawResId
            } else {
                when (track) {
                    MusicTrack.RACE -> R.raw.turbo_candy_drift
                    MusicTrack.MENU, MusicTrack.VICTORY -> R.raw.gumball_horizon
                    MusicTrack.NONE -> R.raw.turbo_candy_drift
                }
            }

            // If the target MP3 is already loaded, keep it playing smoothly without restarting from 0:00
            if (activeRawResId == preferredResId && mediaPlayer != null) {
                currentTrack = track
                try {
                    mediaPlayer?.setVolume(musicVolume, musicVolume)
                    if (_isMusicEnabled.value && musicVolume > 0.01f && !isPausedByLifecycle && mediaPlayer?.isPlaying == false) {
                        mediaPlayer?.start()
                    }
                    _activeSongTitle.value = currentSongTitle()
                    return
                } catch (_: Exception) {
                    // Fall through and recreate MediaPlayer if it entered an invalid state
                }
            }

            currentTrack = track
            startRawTrackLocked(preferredResId)
        }
    }

    /**
     * Switches immediately between the two uploaded MP3 tracks ("Turbo Candy Drift" <-> "Gumball Horizon").
     */
    fun switchBackgroundSong(): String {
        synchronized(playerLock) {
            userManuallySelectedSong = true
            _isMusicEnabled.value = true
            val nextRes = if (activeRawResId == R.raw.turbo_candy_drift) {
                R.raw.gumball_horizon
            } else {
                R.raw.turbo_candy_drift
            }
            if (currentTrack == MusicTrack.NONE) {
                currentTrack = MusicTrack.MENU
            }
            if (musicVolume < 0.15f) {
                musicVolume = savedNonZeroMusicVolume.coerceIn(0.75f, 1.0f)
            }
            startRawTrackLocked(nextRes)
            return currentSongTitle()
        }
    }

    private fun startRawTrackLocked(resId: Int) {
        stopAndReleasePlayerLocked()
        if (resId == 0) return
        activeRawResId = resId
        _activeSongTitle.value = currentSongTitle()

        val appCtx = context?.applicationContext ?: return

        try {
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val afd = appCtx.resources.openRawResourceFd(resId)
            val mp: MediaPlayer = if (afd != null) {
                MediaPlayer().apply {
                    // CRITICAL: setAudioAttributes MUST be called in Idle state BEFORE setDataSource & prepare()!
                    setAudioAttributes(audioAttrs)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                }
            } else {
                val audioMgr = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val sessionId = audioMgr?.generateAudioSessionId() ?: AudioManager.AUDIO_SESSION_ID_GENERATE
                MediaPlayer.create(appCtx, resId, audioAttrs, sessionId) ?: return
            }

            mp.isLooping = false
            mp.setVolume(musicVolume, musicVolume)
            mp.setOnErrorListener { _, _, _ ->
                // Return true so MediaPlayer does not fire OnCompletionListener on error
                true
            }
            mp.setOnCompletionListener {
                // Automatically alternate to the other uploaded MP3 soundtrack when a song finishes
                synchronized(playerLock) {
                    if (currentTrack != MusicTrack.NONE && _isMusicEnabled.value) {
                        val nextIdx = (playlistResIds.indexOf(resId) + 1).mod(playlistResIds.size)
                        startRawTrackLocked(playlistResIds[nextIdx])
                    }
                }
            }
            mediaPlayer = mp
            if (!isPausedByLifecycle && _isMusicEnabled.value && musicVolume > 0.01f) {
                mp.start()
            }
        } catch (_: Exception) {
            activeRawResId = resId
            _activeSongTitle.value = currentSongTitle()
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
            if (!_isMusicEnabled.value || musicVolume <= 0.01f) return
            try {
                if (currentTrack == MusicTrack.NONE) {
                    currentTrack = MusicTrack.MENU
                }
                val mp = mediaPlayer
                if (mp != null) {
                    mp.setVolume(musicVolume, musicVolume)
                    if (!mp.isPlaying) {
                        mp.start()
                    }
                } else {
                    val res = if (activeRawResId != 0) activeRawResId else R.raw.turbo_candy_drift
                    startRawTrackLocked(res)
                }
            } catch (_: Exception) {
                val res = if (activeRawResId != 0) activeRawResId else R.raw.turbo_candy_drift
                startRawTrackLocked(res)
            }
        }
    }

    private fun stopAndReleasePlayerLocked() {
        val mp = mediaPlayer
        mediaPlayer = null
        if (mp != null) {
            try {
                mp.setOnCompletionListener(null)
                mp.setOnErrorListener(null)
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

    fun playSfx(@Suppress("UNUSED_PARAMETER") effect: SoundEffect) {
        // Legacy procedural sine-wave AudioTrack noise has been intentionally removed
        // so that only the user's uploaded MP3 background music plays cleanly.
    }

    fun release() {
        synchronized(playerLock) {
            currentTrack = MusicTrack.NONE
            stopAndReleasePlayerLocked()
        }
    }
}
