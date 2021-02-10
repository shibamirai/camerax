package com.example.camerax

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.util.*

class FaceDetectorProcessor(context: Context, detectorOptions: FaceDetectorOptions) {

    private val detector: FaceDetector

    // Whether this processor is already shut down
    private var isShutdown = false

    init {
        val options = detectorOptions
        detector = FaceDetection.getClient(options)
    }

    @ExperimentalGetImage
    fun processImageProxy(image: ImageProxy, graphicOverlay: GraphicOverlay) {
        if (isShutdown) { return }

        requestDetectInImage(
                InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees),
                graphicOverlay
        ).addOnCompleteListener { image.close() }
    }

    private fun requestDetectInImage(
            image: InputImage,
            graphicOverlay: GraphicOverlay
    ): Task<List<Face>> {
        return detectInImage(image).addOnSuccessListener(TaskExecutors.MAIN_THREAD) { results ->
            graphicOverlay.clear()
            this.onSuccess(results, graphicOverlay)
            graphicOverlay.postInvalidate()
        }.addOnFailureListener(TaskExecutors.MAIN_THREAD) { e ->
            graphicOverlay.clear()
            graphicOverlay.postInvalidate()
            e.printStackTrace()
            this.onFailure(e)
        }
    }

    fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    fun onSuccess(faces: List<Face>, graphicOverlay: GraphicOverlay) {
        for (face in faces) {
            graphicOverlay.add(FaceGraphic(graphicOverlay, face))
        }
    }

    fun onFailure(e: Exception) {
        Log.e(TAG, "Face detection failed $e")
    }

    fun stop() {
        isShutdown = true
        detector.close()
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
        private fun logExtrasForTesting(face: Face?) {
            if (face != null) {
                Log.v(
                        TAG,
                        "face bounding box: " + face.boundingBox.flattenToString()
                )
                Log.v(
                        TAG,
                        "face Euler Angle X: " + face.headEulerAngleX
                )
                Log.v(
                        TAG,
                        "face Euler Angle Y: " + face.headEulerAngleY
                )
                Log.v(
                        TAG,
                        "face Euler Angle Z: " + face.headEulerAngleZ
                )
                // All landmarks
                val landMarkTypes = intArrayOf(
                        FaceLandmark.MOUTH_BOTTOM,
                        FaceLandmark.MOUTH_RIGHT,
                        FaceLandmark.MOUTH_LEFT,
                        FaceLandmark.RIGHT_EYE,
                        FaceLandmark.LEFT_EYE,
                        FaceLandmark.RIGHT_EAR,
                        FaceLandmark.LEFT_EAR,
                        FaceLandmark.RIGHT_CHEEK,
                        FaceLandmark.LEFT_CHEEK,
                        FaceLandmark.NOSE_BASE
                )
                val landMarkTypesStrings = arrayOf(
                        "MOUTH_BOTTOM",
                        "MOUTH_RIGHT",
                        "MOUTH_LEFT",
                        "RIGHT_EYE",
                        "LEFT_EYE",
                        "RIGHT_EAR",
                        "LEFT_EAR",
                        "RIGHT_CHEEK",
                        "LEFT_CHEEK",
                        "NOSE_BASE"
                )
                for (i in landMarkTypes.indices) {
                    val landmark = face.getLandmark(landMarkTypes[i])
                    if (landmark == null) {
                        Log.v(
                                TAG,
                                "No landmark of type: " + landMarkTypesStrings[i] + " has been detected"
                        )
                    } else {
                        val landmarkPosition = landmark.position
                        val landmarkPositionStr =
                                String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y)
                        Log.v(
                                TAG,
                                "Position for face landmark: " +
                                        landMarkTypesStrings[i] +
                                        " is :" +
                                        landmarkPositionStr
                        )
                    }
                }
                Log.v(
                        TAG,
                        "face left eye open probability: " + face.leftEyeOpenProbability
                )
                Log.v(
                        TAG,
                        "face right eye open probability: " + face.rightEyeOpenProbability
                )
                Log.v(
                        TAG,
                        "face smiling probability: " + face.smilingProbability
                )
                Log.v(
                        TAG,
                        "face tracking id: " + face.trackingId
                )
            }
        }
    }
}