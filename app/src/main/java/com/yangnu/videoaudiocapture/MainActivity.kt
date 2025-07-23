package com.yangnu.videoaudiocapture

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.util.Size
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.yangnu.videoaudiocapture.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var videoManager: VideoManager
    private lateinit var audioManager: AudioManager

    private val REQUEST_CODE_CAMERA_PERMISSIONS = 10
    private val REQUEST_CODE_RECORD_PERMISSIONS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        videoManager = VideoManager(this).apply {
            quality = 70
            size = Size(224, 224)
            onFrameAnalyzed = ::onFrameAnalyzed
        }

        audioManager = AudioManager(this).apply {
            onAudioCaptured = ::onAudioCaptured
        }

        if (videoManager.allPermissionsGranted())
            videoManager.bindingCamera(binding.viewFinder)
        else
            videoManager.requestPermission(REQUEST_CODE_CAMERA_PERMISSIONS)

        if (audioManager.allPermissionsGranted())
            audioManager.capturingAudio()
        else
            audioManager.requestPermission(REQUEST_CODE_RECORD_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSIONS)
            if (videoManager.allPermissionsGranted())
                videoManager.bindingCamera(binding.viewFinder)

        if (requestCode == REQUEST_CODE_RECORD_PERMISSIONS)
            if (audioManager.allPermissionsGranted())
                audioManager.capturingAudio()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }

    private fun onFrameAnalyzed(base64: String) {
        runOnUiThread {
            val decodedByte = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
            binding.preview.setImageBitmap(bitmap)
        }
    }

    private fun onAudioCaptured(base64: String) {
        Log.d("@@@", "${base64.length}\t"+base64.take(100))
    }
}