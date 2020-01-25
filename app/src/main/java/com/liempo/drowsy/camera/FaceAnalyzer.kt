package com.liempo.drowsy.camera
import android.graphics.PointF
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class FaceAnalyzer : ImageAnalysis.Analyzer {

    private var isAnalyzing = AtomicBoolean(false)
    var pointsListListener: ((List<PointF>) -> Unit)? = null
    var analysisSizeListener: ((Size) -> Unit)? = null
    var eyesClosedListener: ((Boolean) -> Unit)? = null
    var noFaceListener: (() -> Unit)? = null

    private val faceDetector: FirebaseVisionFaceDetector by lazy {
        val options = Builder()
            .setPerformanceMode(FAST)
            .setClassificationMode(ALL_CLASSIFICATIONS)
            .build()
        FirebaseVision.getInstance()
            .getVisionFaceDetector(options)
    }

    private val successListener = OnSuccessListener<List<FirebaseVisionFace>> { faces ->
        isAnalyzing.set(false)
        val points = mutableListOf<PointF>()

        if (faces.isEmpty()) noFaceListener?.invoke()

        for (face in faces) {
            val contours = face.getContour(FirebaseVisionFaceContour.ALL_POINTS)
            points += contours.points.map { PointF(it.x, it.y) }

                eyesClosedListener?.invoke(
                    (face.leftEyeOpenProbability < 0.1f &&
                     face.rightEyeOpenProbability < 0.1f)
                )
        }
        pointsListListener?.invoke(points)
    }

    private val failureListener = OnFailureListener { e ->
        isAnalyzing.set(false)
        Timber.e(e, "Face analysis failure.")
    }

    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
        val cameraImage = image?.image ?: return

        if (isAnalyzing.get()) return
        isAnalyzing.set(true)

        analysisSizeListener?.invoke(Size(image.width, image.height))

        val firebaseVisionImage =
            FirebaseVisionImage.fromMediaImage(
                cameraImage, getRotationConstant(rotationDegrees))

        faceDetector.detectInImage(firebaseVisionImage)
            .addOnSuccessListener(successListener)
            .addOnFailureListener(failureListener)
   }

    private fun getRotationConstant(rotationDegrees: Int): Int {
        return when (rotationDegrees) {
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> FirebaseVisionImageMetadata.ROTATION_0
        }
    }
}