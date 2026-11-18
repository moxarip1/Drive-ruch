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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class VoiceChatEngine(private val appContext: Context) {

    companion object {
        private const val SAMPLE_RATE = 8000
        private const val MAX_DURATION_MS = 4500L
        private const val MAX_RAW_8BIT_BYTES = 36000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recordingFlag = AtomicBoolean(false)
    private var recordJob: Job? = null
    private var capturedBytes = ByteArray(0)
    private var captureStartMs = 0L

    @Volatile var isMuted: Boolean = false

    fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun startRecording(onMaxDurationReached: (audioB64: String, durationMs: Long) -> Unit) {
        if (!hasRecordPermission() || !recordingFlag.compareAndSet(false, true)) return

        captureStartMs = System.currentTimeMillis()
        capturedBytes = ByteArray(0)

        recordJob = scope.launch {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            var recorder: AudioRecord? = null
            val out = ByteArrayOutputStream()
            try {
                @Suppress("MissingPermission")
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )
                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    recordingFlag.set(false)
                    return@launch
                }

                recorder.startRecording()
                val pcm = ShortArray(minBuf / 2)
                while (isActive && recordingFlag.get() && out.size() < MAX_RAW_8BIT_BYTES) {
                    val read = recorder.read(pcm, 0, pcm.size)
                    if (read > 0) {
                        for (i in 0 until read) {
                            // Compress 16-bit PCM sample -> signed 8-bit sample for fast low-latency Firestore transport
                            val q = (pcm[i].toInt() shr 8).coerceIn(-128, 127)
                            out.write(q)
                            if (out.size() >= MAX_RAW_8BIT_BYTES) break
                        }
                    }
                    if (System.currentTimeMillis() - captureStartMs >= MAX_DURATION_MS) {
                        break
                    }
                }
                try {
                    recorder.stop()
                } catch (_: Exception) {
                }
            } catch (_: Exception) {
            } finally {
                try {
                    recorder?.release()
                } catch (_: Exception) {
                }
                val bytes = out.toByteArray()
                capturedBytes = bytes
                val wasAutoStopped = recordingFlag.getAndSet(false)
                if (wasAutoStopped && bytes.size >= 800) {
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val dur = (System.currentTimeMillis() - captureStartMs).coerceIn(250L, 5000L)
                    onMaxDurationReached(b64, dur)
                }
            }
        }
    }

    suspend fun stopRecordingAndEncode(): Pair<String, Long>? {
        if (!recordingFlag.get() && recordJob?.isActive != true) {
            return if (capturedBytes.size >= 800) {
                val b64 = Base64.encodeToString(capturedBytes, Base64.NO_WRAP)
                capturedBytes = ByteArray(0)
                b64 to (System.currentTimeMillis() - captureStartMs).coerceIn(250L, 5000L)
            } else null
        }

        recordingFlag.set(false)
        try {
            recordJob?.join()
        } catch (_: Exception) {
        }
        val bytes = capturedBytes
        capturedBytes = ByteArray(0)
        if (bytes.size < 800) return null
        val dur = (System.currentTimeMillis() - captureStartMs).coerceIn(250L, 5000L)
        return Base64.encodeToString(bytes, Base64.NO_WRAP) to dur
    }

    fun playVoiceBase64(audioB64: String) {
        if (isMuted || audioB64.isBlank()) return
        scope.launch {
            val raw8 = try {
                Base64.decode(audioB64, Base64.DEFAULT)
            } catch (_: Exception) {
                return@launch
            }
            if (raw8.isEmpty()) return@launch

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
                val sleepMs = ((raw8.size.toFloat() / SAMPLE_RATE) * 1000f).toLong().coerceIn(200L, 5500L)
                kotlinx.coroutines.delay(sleepMs + 120L)
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
    }

    fun release() {
        recordingFlag.set(false)
        recordJob?.cancel()
        scope.cancel()
    }
}
