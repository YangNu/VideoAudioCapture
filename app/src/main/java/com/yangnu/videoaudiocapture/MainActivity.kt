package com.yangnu.videoaudiocapture

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Size
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.yangnu.videoaudiocapture.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var videoManager: VideoManager
    private val REQUEST_CODE_CAMERA_PERMISSIONS = 10

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

        if (videoManager.allPermissionsGranted()) {
            videoManager.bindingCamera(binding.viewFinder)
        } else {
            videoManager.requestPermission(REQUEST_CODE_CAMERA_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSIONS)
            if (videoManager.allPermissionsGranted())
                videoManager.bindingCamera(binding.viewFinder)
    }

    private fun onFrameAnalyzed(base64: String) {
        runOnUiThread {
            val decodedByte = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
            binding.preview.setImageBitmap(bitmap)
        }
    }
}