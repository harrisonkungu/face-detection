package com.apps.facedetection.FaceDetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import androidx.camera.core.AspectRatio
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.copy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException




class FaceDetector(
    private val ovalCenter: Offset,
    private val ovalRadiusX: Float,
    private val ovalRadiusY: Float,
    private val onFaceDetected: (Boolean) -> Unit,
) : ImageAnalysis.Analyzer {

    companion object {
        var blinkCount = 0
        var headMovement = 0
        val checksPassed = MutableStateFlow(0)
        val checksStateFlow = MutableStateFlow(ChecksState())
        private const val EYE_OPENED_PROBABILITY = 0.4F
        private var blinkStartTime: Long = 0
        private val EYE_CLOSED_PROBABILITY = 0.4f
        private val blinkDurationThreshold = 1
        private var isBlinking = false
        val hasTurnedLeft = MutableStateFlow(false)
        val hasTurnedRight = MutableStateFlow(false)
    }

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())





    // Define the conditions and their order
    enum class CheckCondition {
        FaceDetected,
//        FaceTilted,
//        FaceNotFarAway,
        FaceStraight,
        EyesBlinked,
        FaceInRange
    }

    // State for each condition
    data class CheckState(
        val condition: CheckCondition,
        val isMet: Boolean = false,
        val sweepAngle: Float = 0f,
        val guideMessage: String = ""
    )

    // Overall state
    data class ChecksState(
        val checks: List<CheckState> = listOf(
            CheckState(CheckCondition.FaceDetected,  guideMessage = "Ensure your face is fully visible"),
            CheckState(CheckCondition.FaceInRange, guideMessage = "Face out of Range"),
            CheckState(CheckCondition.FaceStraight , guideMessage = "Look directly at the camera"),
            CheckState(CheckCondition.EyesBlinked,  guideMessage = "Blink your eyes")
        ),
        val allChecksPassed: Boolean = false,
        val currentGuideMessage: String = "Ensure your face is fully visible"
    )


    private fun updateChecks(
        faceDetected: Boolean,
        faceTilted: Boolean,
        eyesBlinked: Boolean,
        faceStraight: Boolean,
        turnedLeft: Boolean,
        turnedRight: Boolean,
        faceNotFarAway: Boolean,
        faceNotTooClose:Boolean,
        faceInRange: Boolean
    ) {
        checksStateFlow.update { currentState ->
            val updatedChecks = currentState.checks.mapIndexed { index, checkState ->
                val isMet = when (checkState.condition) {
                    CheckCondition.FaceDetected -> faceDetected
                    CheckCondition.FaceInRange -> faceInRange
                    CheckCondition.FaceStraight -> faceStraight
                    CheckCondition.EyesBlinked -> eyesBlinked
                }

                // Check if previous conditions are met
                val previousConditionsMet = if (index > 0) {
                    currentState.checks.subList(0, index).all { it.isMet }
                } else {
                    true
                }

                val newSweepAngle = if (isMet && previousConditionsMet) {
                    90f // Each check contributes 90 degrees
                } else {
                    0f
                }

                checkState.copy(isMet = isMet, sweepAngle = newSweepAngle)
            }

            val allChecksPassed = updatedChecks.all { it.isMet }

            val newGuideMessage = when {
                !faceDetected -> "Ensure your face is fully visible"
                !faceInRange -> "Face out of Range"
                !faceStraight -> "Look directly at the camera"
                !eyesBlinked -> "Blink your eyes"
                else -> "All checks passed"
            }

            ChecksState(updatedChecks, allChecksPassed, newGuideMessage)
        }
    }





    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .build()
    private val faceDetector: FaceDetector = FaceDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        scope.launch {
            val mediaImage = imageProxy.image ?: run {
                return@launch
            }

            val inputImage = InputImage.fromMediaImage(
                mediaImage, imageProxy.imageInfo.rotationDegrees
            )

            try {
                val faces = detectFaces(inputImage)
                if (faces.isEmpty()) {
                    updateChecks(
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false
                    )
                    onFaceDetected(false)
                }else if (faces.size > 2) {
                    updateChecks(
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false
                    )
                    onFaceDetected(false)
                  return@launch
                }



                val isFaceDetected = faces.any {
                    validateBlinkedEyes(
                        it.leftEyeOpenProbability,
                        it.rightEyeOpenProbability
                    ) { blinked->
                        if (blinked){
                            Log.d("Blinking> ", blinkCount.toString())
                            blinkCount++
                        }

                    }
                    val boundingBox = it.boundingBox
                    val faceWidth = boundingBox.width().toFloat()
                    val faceHeight = boundingBox.height().toFloat()
                    val minFaceWidth = 170f
                    val minFaceHeight = 170f
                    val idealFaceWidth = 200f
                    val idealFaceHeight = 205f


                    val maxFaceWidth = 200f
                    val maxFaceHeight = 210f



                    val faceNotFarAway = faceWidth in (minFaceWidth..idealFaceWidth) && faceHeight in (minFaceHeight..idealFaceHeight)
                    val faceNotTooClose = faceWidth in (idealFaceWidth..maxFaceWidth) && faceHeight in (idealFaceHeight..maxFaceHeight)

                    val faceInRange = faceWidth in (minFaceWidth..maxFaceWidth) && faceHeight in (minFaceHeight..maxFaceHeight)


                    val headPitchAngle = it.headEulerAngleX
                    val isHeadPitchValid = headPitchAngle in -15f..15f

                    val noseLandmark = it.getLandmark(FaceLandmark.NOSE_BASE)
                    val leftEyeLandmark = it.getLandmark(FaceLandmark.LEFT_EYE)
                    val rightEyeLandmark = it.getLandmark(FaceLandmark.RIGHT_EYE)
                    val leftCheekLandmark = it.getLandmark(FaceLandmark.LEFT_CHEEK)
                    val rightCheekLandmark = it.getLandmark(FaceLandmark.RIGHT_CHEEK)
                    val mouthLeftLandmark = it.getLandmark(FaceLandmark.MOUTH_LEFT)
                    val mouthBottomLandmark = it.getLandmark(FaceLandmark.MOUTH_BOTTOM)
                    val mouthRightLandmark = it.getLandmark(FaceLandmark.MOUTH_RIGHT)

                    val nosePosition = noseLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val mouthBottomPosition = mouthBottomLandmark?.position?.let { res-> Offset(res.x, res.y-30) }
                    val leftEyePosition = leftEyeLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val rightEyePosition = rightEyeLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val leftCheekPosition = leftCheekLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val rightCheekPosition = rightCheekLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val mouthLeftPosition = mouthLeftLandmark?.position?.let  { res-> Offset(res.x, res.y) }
                    val mouthRightPosition = mouthRightLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val isLeftEyeOpen = (it.leftEyeOpenProbability?.toDouble() ?: 0.0) > 0.5
                    val isRightEyeOpen = (it.rightEyeOpenProbability?.toDouble() ?: 0.0) > 0.5
                    isFaceInsideOval(
                        faceCenter = Offset(it.boundingBox.centerX().toFloat(), it.boundingBox.centerY().toFloat()-120),
                        it.boundingBox.width().toFloat(),
                        it.boundingBox.height().toFloat(),
                        nosePosition,
                        leftEyePosition,
                        rightEyePosition,
                        leftCheekPosition,
                        rightCheekPosition,
                        mouthLeftPosition,
                        mouthBottomPosition,
                        mouthRightPosition,
                        isLeftEyeOpen,
                        isRightEyeOpen,
                        blinkCount > 1,
                        isHeadPitchValid,
                        headMovement > 0,
                        faceNotFarAway,
                        faceNotTooClose,
                        faceInRange,
                        it
                    )
                }
                Log.d("isFaceDetected4", isFaceDetected.toString())
                onFaceDetected(isFaceDetected)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                imageProxy.close()
            }
        }
    }

    private fun detectFaces(inputImage: InputImage): List<Face> {
        return try {
            Tasks.await(faceDetector.process(inputImage))
        } catch (e: Exception) {
            emptyList()
        }
    }




    private fun validateBlinkedEyes(
        leftEyeProbability: Float?,
        rightEyeProbability: Float?,
        callbackBlinked: (Boolean) -> Unit
    ): Boolean {
        val isLeftEyeOpened = (leftEyeProbability ?: 0f) > EYE_OPENED_PROBABILITY
        val isRightEyeOpened = (rightEyeProbability ?: 0f) > EYE_OPENED_PROBABILITY

        return (isLeftEyeOpened && isRightEyeOpened).also {
            if (!it) callbackBlinked.invoke(true)
        }
    }


//    private fun validateBlinkedEyes(
//        leftEyeProbability: Float?,
//        rightEyeProbability: Float?,
//        callbackBlinked: (Boolean) -> Unit
//    ): Boolean {
//        val isLeftEyeClosed = (leftEyeProbability ?: 0f) < EYE_CLOSED_PROBABILITY
//        val isRightEyeClosed = (rightEyeProbability ?: 0f) < EYE_CLOSED_PROBABILITY
//        val currentTime = System.currentTimeMillis()
//
//        if (isLeftEyeClosed && isRightEyeClosed) {
//            if (!isBlinking) {
//                // Start of a potential blink
//                isBlinking = true
//                blinkStartTime = currentTime
//            }
//
////            else {
////                // Check if the blink duration is long enough
////                if (currentTime - blinkStartTime >= blinkDurationThreshold) {
////                    // Confirmed blink
////                    callbackBlinked.invoke(true)
////                    isBlinking = false // Reset for the next blink
////                    return true
////                }
////            }
//        } else {
//            // Eyes are open, reset the blink state
//            isBlinking = false
//        }
//        return false
//    }


    private fun isFaceInsideOval(
        faceCenter: Offset,
        faceWidth: Float,
        faceHeight: Float,
        nosePosition: Offset?,
        leftEyePosition: Offset?,
        rightEyePosition: Offset?,
        leftCheekPosition: Offset?,
        rightCheekPosition: Offset?,
        mouthLeftPosition: Offset?,
        mouthBottomPosition: Offset?,
        mouthRightPosition: Offset?,
        isLeftEyeOpen: Boolean,
        isRightEyeOpen: Boolean,
        isBlinkDetected: Boolean,
        isHeadPitchValid: Boolean,
        headMovement: Boolean,
        faceNotFarAway: Boolean,
        faceNotTooClose:Boolean,
        faceInRange:Boolean,
        face: Face
    ): Boolean {

        val isFaceCentered = isFaceCenteredInOval(faceCenter)


        val isLandmarksInsideOval = areLandmarksInsideOval(
            nosePosition,
            leftEyePosition,
            rightEyePosition,
            leftCheekPosition,
            rightCheekPosition,
            mouthLeftPosition,
            mouthRightPosition,
            mouthBottomPosition
        )


        val faceDetected = isFaceCentered && isLandmarksInsideOval

        Log.d("faceDetected status", faceDetected.toString())


        val isTurnedToRight = face.headEulerAngleY > -4f
        val isTurnedToLeft = face.headEulerAngleY < -4f
        val isLookingStraight = face.headEulerAngleY in -10f..10f

        if (isTurnedToLeft) hasTurnedLeft.value = true
        if (isTurnedToRight) hasTurnedRight.value = true



        val faceTilted = isTurnedToLeft && isTurnedToRight


        Log.d("isBlinkDetected", isBlinkDetected.toString())
        Log.d("isLookingStraight", isLookingStraight.toString())
        Log.d("hasTurnedRight", hasTurnedRight.value.toString())
        Log.d("hasTurnedLeft", hasTurnedLeft.value.toString())
        Log.d("faceNotFarAway", faceNotFarAway.toString())
        Log.d("checks passed", checksPassed.value.toString())


        updateChecks(faceDetected, faceTilted, isBlinkDetected , isLookingStraight, hasTurnedRight.value, hasTurnedLeft.value, faceNotFarAway, faceNotTooClose, faceInRange)
//        return faceDetected && hasTurnedRight.value && hasTurnedLeft.value && isBlinkDetected && isLookingStraight && faceNotFarAway && faceNotTooClose && faceInRange
        return faceDetected &&  isBlinkDetected && isLookingStraight && faceInRange
    }



    private fun isPointInsideOval(point: Offset): Boolean {
        val dx = (point.x - ovalCenter.x) / ovalRadiusX
        val dy = (point.y - ovalCenter.y) / ovalRadiusY
        return (dx * dx + dy * dy) <= 1.0
    }

    // Helper function to check if the face is centered within the oval (with tolerance)
    private fun isFaceCenteredInOval(faceCenter: Offset): Boolean {
        val centerXTolerance = ovalRadiusX * 0.2f // 20% tolerance
        val centerYTolerance = ovalRadiusY * 0.2f // 20% tolerance

        val dx = faceCenter.x - ovalCenter.x
        val dy = faceCenter.y - ovalCenter.y

        return dx in -centerXTolerance..centerXTolerance && dy in -centerYTolerance..centerYTolerance
    }
    // Helper function to check if the face landmarks are inside the oval
    private fun areLandmarksInsideOval(
        nosePosition: Offset?,
        leftEyePosition: Offset?,
        rightEyePosition: Offset?,
        leftCheekPosition: Offset?,
        rightCheekPosition: Offset?,
        mouthLeftPosition: Offset?,
        mouthRightPosition: Offset?,
        mouthBottomPosition:Offset?
    ): Boolean {
        val landmarks = listOfNotNull(
            nosePosition,
            leftEyePosition,
            rightEyePosition,
            mouthBottomPosition
//            leftCheekPosition,
//            rightCheekPosition,
//            mouthLeftPosition,
//            mouthRightPosition
        )

        return landmarks.all { isPointInsideOval(it) }
    }
























}



suspend fun validateImageFeatures(bitmap: Bitmap): Boolean {
    val image = InputImage.fromBitmap(bitmap, 0)

    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    val detector = FaceDetection.getClient(options)

    return suspendCancellableCoroutine { continuation ->
        detector.process(image)
            .addOnSuccessListener { faces ->
                val allFeaturesValid = faces.isNotEmpty() && faces.all { face ->
                    val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
                    val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
                    val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
                    val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
                    val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)
                    val eulerY = face.headEulerAngleY
                    val eyesOpenProbability = (face.leftEyeOpenProbability ?: 0.0F) > 0.4 &&
                            (face.rightEyeOpenProbability ?: 0.0F) > 0.4

                    nose != null && leftEye != null && rightEye != null &&
                            mouthLeft != null && mouthRight != null &&
                            eyesOpenProbability && eulerY in -10.0..10.0  // Adjust angle range as needed
                }
                continuation.resume(allFeaturesValid)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
            .addOnCompleteListener {
                detector.close()
            }
    }
}


fun startFaceDetection(
    context: Context,
    cameraController: LifecycleCameraController,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    ovalRect: Offset,
    onFaceDetected: (Boolean) -> Unit,

    ) {
    cameraController.imageAnalysisTargetSize = CameraController.OutputSize(AspectRatio.RATIO_4_3)
    cameraController.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
    cameraController.setImageAnalysisAnalyzer(
        ContextCompat.getMainExecutor(context),
        FaceDetector(
            onFaceDetected = onFaceDetected,
            ovalCenter = ovalRect,
            ovalRadiusX = OVAL_WIDTH_DP / 2f,
            ovalRadiusY = OVAL_HEIGHT_DP / 2f,
        ),
    )

    cameraController.bindToLifecycle(lifecycleOwner)
    previewView.controller = cameraController
}










fun increaseBrightness(bitmap: Bitmap, factor: Float): Bitmap {
    val outputBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
    val canvas = Canvas(outputBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setToScale(factor, factor, factor, 1.0f)
    val colorFilter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = colorFilter.asAndroidColorFilter()
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return outputBitmap
}



class FaceOverlayView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    var nosePosition: Offset? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        nosePosition?.let {
            canvas.drawCircle(it.x, it.y, 10f, paint) // Adjust radius (10f) as needed
        }
    }
}


