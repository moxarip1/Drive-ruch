package com.example.sweetrush.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class VoiceChatEngine(private val appContext: Context) {

    companion object {
        private const val SAMPLE_RATE = 8000
        private const val CHUNK_BYTES = 10000 // ~1.25s per chunk at 8kHz 8-bit PCM (~13.3 KB Base64)
        private const val MIN_FLUSH_BYTES = 2000 // 250ms minimum speech clip
        private const val VAD_PEAK_THRESHOLD = 220
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val continuousMicFlag = AtomicBoolean(false)
    private var recordJob: Job? = null
    private val playbackChannel = Channel<String>(capacity = 24)

    @Volatile var isMuted: Boolean = false
    val isMicActive: Boolean get() = continuousMicFlag.get()

    init {
        scope.launch {
            for (audioB64 in playbackChannel) {
                if (!isMuted && audioB64.isNotBlank()) {
                    playBlocking(audioB64)
                }
            }
        }
    }

    fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun startContinuousMic(onChunkReady: (audioB64: String, durationMs: Long) -> Unit): Boolean {
        if (!hasRecordPermission()) return false
        if (!continuousMicFlag.compareAndSet(false, true)) return true

        recordJob = scope.launch {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            var recorder: AudioRecord? = null
            val out = ByteArrayOutputStream(CHUNK_BYTES + 256)
            var chunkStartMs = System.currentTimeMillis()
            var peakAbs = 0

            fun flushChunkIfSpoken() {
                val bytes = out.toByteArray()
                val dur = (System.currentTimeMillis() - chunkStartMs).coerceIn(250L, 4000L)
                out.reset()
                val hadSpeech = peakAbs >= VAD_PEAK_THRESHOLD
                peakAbs = 0
                chunkStartMs = System.currentTimeMillis()
                if (bytes.size >= MIN_FLUSH_BYTES && hadSpeech) {
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    onChunkReady(b64, dur)
                }
            }

            try {
                @Suppress("MissingPermission")
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    recorder.release()
                    @Suppress("MissingPermission")
                    recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBuf * 2
                    )
                }
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    continuousMicFlag.set(false)
                    return@launch
                }

                recorder.startRecording()
                val pcm = ShortArray(minBuf / 2)
                while (isActive && continuousMicFlag.get()) {
                    val read = recorder.read(pcm, 0, pcm.size)
                    if (read > 0) {
                        for (i in 0 until read) {
                            val s = pcm[i].toInt()
                            val a = abs(s)
                            if (a > peakAbs) peakAbs = a
                            // Quantize 16-bit -> signed 8-bit PCM for ultra-fast Firestore relay
                            val q = (s shr 8).coerceIn(-128, 127)
                            out.write(q)
                            if (out.size() >= CHUNK_BYTES) {
                                flushChunkIfSpoken()
                            }
                        }
                    } else {
                        delay(15L)
                    }
                }
                try {
                    recorder.stop()
                } catch (_: Exception) {
                }
                flushChunkIfSpoken()
            } catch (_: Exception) {
            } finally {
                continuousMicFlag.set(false)
                try {
                    recorder?.release()
                } catch (_: Exception) {
                }
            }
        }
        return true
    }

    fun stopContinuousMic() {
        continuousMicFlag.set(false)
    }

    fun playVoiceBase64(audioB64: String) {
        if (isMuted || audioB64.isBlank()) return
        playbackChannel.trySend(audioB64)
    }

    private suspend fun playBlocking(audioB64: String) {
        val raw8 = try {
            Base64.decode(audioB64, Base64.DEFAULT)
        } catch (_: Exception) {
            return
        }
        if (raw8.isEmpty()) return

        val pcm16 = ShortArray(raw8.size) { idx ->
            (raw8[idx].toInt() shl 8).toShort()
        }
        val byteSize = pcm16.size * 2
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteSize.coerceAtLeast(2048))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm16, 0, pcm16.size)
            track.play()
            val sleepMs = ((raw8.size.toFloat() / SAMPLE_RATE) * 1000f).toLong().coerceIn(200L, 4500L)
            delay(sleepMs + 40L)
            try {
                track.stop()
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
        } finally {
            try {
                track?.release()
            } catch (_: Exception) {
            }
        }
    }

    fun release() {
        continuousMicFlag.set(false)
        recordJob?.cancel()
        playbackChannel.close()
        scope.cancel()
    }
}
