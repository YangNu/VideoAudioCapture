package com.yangnu.videoaudiocapture

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Base64
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoManager(private val context: AppCompatActivity) {
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    var onFrameAnalyzed: (String) -> Unit = {  }
    var size = Size(96, 96)
    var fps = 25
    var quality = 100

    fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(requestCode: Int) =
        ActivityCompat.requestPermissions(
            context as Activity,
            REQUIRED_PERMISSIONS,
            requestCode
        )

    fun bindingCamera(viewFinder: PreviewView) {
        val processCameraProvider = ProcessCameraProvider.getInstance(context)
        processCameraProvider.addListener({
            val cameraProvider = processCameraProvider.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val preview = Preview.Builder()
                .build().also {
                    it.surfaceProvider = viewFinder.surfaceProvider
                }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(size)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor, FrameAnalyzer())
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(context, cameraSelector, preview, imageAnalyzer)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    private inner class FrameAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L
        private val frameIntervalMs = 1000L / fps

        override fun analyze(image: ImageProxy) {
            val currentTimestamp = System.currentTimeMillis()
            if (currentTimestamp - lastAnalyzedTimestamp >= frameIntervalMs) {
                lastAnalyzedTimestamp = currentTimestamp
                val bitmap = imageProxyToBitmap(image)
                val base64String = bitmapToBase64(bitmap)
                onFrameAnalyzed(base64String)
            }
            image.close()
        }

        private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
            val planes = image.planes
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), quality, out)

            val imageBytes = out.toByteArray()
            val rawBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            val matrix = Matrix().apply {
                postRotate(90f)
            }

            return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        }

        private fun bitmapToBase64(bitmap: Bitmap): String {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.NO_WRAP)
        }
    }
}