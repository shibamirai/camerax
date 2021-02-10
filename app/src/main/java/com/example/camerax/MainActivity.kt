package com.example.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var graphicOverlay: GraphicOverlay? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        graphicOverlay = findViewById(R.id.graphic_overlay)
//        viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
//                    .setTargetRotation(Surface.ROTATION_90)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            needUpdateGraphicOverlayImageSourceInfo = true
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, { imageProxy: ImageProxy ->
                        if (needUpdateGraphicOverlayImageSourceInfo) {
                            val isImageFlipped =
                                    lensFacing == CameraSelector.LENS_FACING_FRONT
                            val rotationDegrees =
                                    imageProxy.imageInfo.rotationDegrees
                            if (rotationDegrees == 0 || rotationDegrees == 180) {
                                graphicOverlay!!.setImageSourceInfo(
                                        imageProxy.width, imageProxy.height, isImageFlipped
                                )
                            } else {
                                graphicOverlay!!.setImageSourceInfo(
                                        imageProxy.height, imageProxy.width, isImageFlipped
                                )
                            }
                            needUpdateGraphicOverlayImageSourceInfo = false
                        }
                        try {
                            val faceDetectorOptions = FaceDetectorOptions.Builder()
                                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                                    .setMinFaceSize(0.1F)
                                    .enableTracking()
                                    .build()
                            FaceDetectorProcessor(this, faceDetectorOptions).processImageProxy(imageProxy, graphicOverlay!!)
                        } catch (e: MlKitException) {
                            Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage
                            )
                            Toast.makeText(
                                applicationContext,
                                e.localizedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }

            val cameraSelector = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permessions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}