package com.yangnu.videoaudiocapture

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AudioManager(private val context: AppCompatActivity) {
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    var onAudioCaptured: (String) -> Unit = {}

    var sampleRate = 16000
    var frameMs = 25
    var bytesPerSample = 2
    var channelConfig = AudioFormat.CHANNEL_IN_MONO
    var audioFormat = AudioFormat.ENCODING_PCM_16BIT
    var audioSource = MediaRecorder.AudioSource.MIC

    fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(requestCode: Int) =
        ActivityCompat.requestPermissions(
            context as Activity,
            REQUIRED_PERMISSIONS,
            requestCode
        )

    @SuppressLint("MissingPermission")
    fun capturingAudio() {
        if (!allPermissionsGranted()) return
        if (isRecording) return
        val frameSamples = sampleRate * frameMs / 1000
        val frameBytes = frameSamples * bytesPerSample
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBuf, frameBytes * 4)

        audioRecord = AudioRecord(
            audioSource,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        val chunkBuffer = ByteArray(frameBytes)
        audioRecord!!.startRecording()
        isRecording = true

        executor.execute {
            try {
                while (isRecording) {
                    val read =
                        audioRecord!!.read(chunkBuffer, 0, frameBytes, AudioRecord.READ_BLOCKING)
                    if (read == frameBytes) {
                        val base64String = Base64.encodeToString(chunkBuffer, 0, frameBytes, Base64.NO_WRAP)
                        onAudioCaptured(base64String)
                    } else
                        break
                }
            } catch (_: Exception) {
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        if (!isRecording) return
        isRecording = false
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        audioRecord?.release()
        audioRecord = null
    }

    fun release() {
        stop()
        executor.shutdownNow()
    }
}