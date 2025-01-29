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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
        private var lastBlinkTimestamp: Long = 0
        private var isLeftEyePreviouslyClosed = false
        private var isRightEyePreviouslyClosed = false
        var blinkCount = 0
        var headMovement = 0
        val checksPassed = MutableStateFlow(0)
        private const val EYE_OPENED_PROBABILITY = 0.4F
        private val blinkState = MutableStateFlow(false)
        val hasTurnedLeft = MutableStateFlow(false)
        val hasTurnedRight = MutableStateFlow(false)
    }

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())



    private fun updateChecks(
        faceDetected: Boolean,
        faceTilted: Boolean,
        eyesBlinked: Boolean,
        faceStraight: Boolean,
        turnedLeft: Boolean,
        turnedRight: Boolean,
        faceNotFarAway: Boolean
    ) {
        checksPassed.value = listOf( faceDetected,  eyesBlinked, faceStraight, turnedLeft, turnedRight, faceNotFarAway).count { it }
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

                    val faceNotFarAway = faceWidth in (minFaceWidth..idealFaceWidth) && faceHeight in (minFaceHeight..idealFaceHeight)


                    val headPitchAngle = it.headEulerAngleX
                    val isHeadPitchValid = headPitchAngle in -15f..15f

                    val noseLandmark = it.getLandmark(FaceLandmark.NOSE_BASE)
                    val leftEyeLandmark = it.getLandmark(FaceLandmark.LEFT_EYE)
                    val rightEyeLandmark = it.getLandmark(FaceLandmark.RIGHT_EYE)
                    val leftCheekLandmark = it.getLandmark(FaceLandmark.LEFT_CHEEK)
                    val rightCheekLandmark = it.getLandmark(FaceLandmark.RIGHT_CHEEK)
                    val mouthLeftLandmark = it.getLandmark(FaceLandmark.MOUTH_LEFT)
                    val mouthRightLandmark = it.getLandmark(FaceLandmark.MOUTH_RIGHT)

                    val nosePosition = noseLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val leftEyePosition = leftEyeLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val rightEyePosition = rightEyeLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val leftCheekPosition = leftCheekLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val rightCheekPosition = rightCheekLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val mouthLeftPosition = mouthLeftLandmark?.position?.let  { res-> Offset(res.x, res.y) }
                    val mouthRightPosition = mouthRightLandmark?.position?.let { res-> Offset(res.x, res.y) }
                    val isLeftEyeOpen = (it.leftEyeOpenProbability?.toDouble() ?: 0.0) > 0.5
                    val isRightEyeOpen = (it.rightEyeOpenProbability?.toDouble() ?: 0.0) > 0.5
                    isFaceInsideOval(
                        Offset(it.boundingBox.centerX().toFloat(), it.boundingBox.centerY().toFloat()),
                        it.boundingBox.width().toFloat(),
                        it.boundingBox.height().toFloat(),
                        nosePosition,
                        leftEyePosition,
                        rightEyePosition,
                        leftCheekPosition,
                        rightCheekPosition,
                        mouthLeftPosition,
                        mouthRightPosition,
                        isLeftEyeOpen,
                        isRightEyeOpen,
                        blinkCount > 0,
                        isHeadPitchValid,
                        headMovement > 0,
                        faceNotFarAway,
                        it
                    )
                }
                onFaceDetected(checksPassed.value == 5)
//                onFaceDetected(isFaceDetected && checksPassed.value == 4)
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








    private fun detectBlink(
        isLeftEyeOpen: Boolean,
        isRightEyeOpen: Boolean
    ) {
        val currentTimestamp = System.currentTimeMillis()

        val isLeftEyeClosed = !isLeftEyeOpen
        val isRightEyeClosed = !isRightEyeOpen

        val isLeftEyeBlink = isLeftEyePreviouslyClosed && isLeftEyeOpen &&
                (currentTimestamp - lastBlinkTimestamp > 200)
        isLeftEyePreviouslyClosed = isLeftEyeClosed

        val isRightEyeBlink = isRightEyePreviouslyClosed && isRightEyeOpen &&
                (currentTimestamp - lastBlinkTimestamp > 200)
        isRightEyePreviouslyClosed = isRightEyeClosed

        if ((isLeftEyeBlink || isRightEyeBlink) && (currentTimestamp - lastBlinkTimestamp > 200)) {
            lastBlinkTimestamp = currentTimestamp
            blinkCount++
            blinkState.value = true
            Log.d("BlinkCounter", "Blink count: $blinkCount")
        } else {
            blinkState.value = false
        }
    }


    private fun validateBlinkedEyes(
        leftEyeProbability: Float?,
        rightEyeProbability: Float?,
        callbackBlinked: (Boolean) -> Unit
    ): Boolean {
        Log.d("BlinkCounter", "Blink count: $blinkCount")
        val isLeftEyeOpened = (leftEyeProbability ?: 0f) > EYE_OPENED_PROBABILITY
        val isRightEyeOpened = (rightEyeProbability ?: 0f) > EYE_OPENED_PROBABILITY

        return (isLeftEyeOpened && isRightEyeOpened).also {
            if (!it) callbackBlinked.invoke(true)
        }
    }

//    private fun validateHeadMovement(
//        headEulerAngleY: Float,
//        headMovement: HeadMovement,
//        callbackHeadMovement: (Boolean) -> Unit
//    ) {
//        if (detectEulerYMovement(headEulerAngleY) == headMovement) {
//            callbackHeadMovement(true)
//        }
//    }
//
//    private fun detectEulerYMovement(
//        headEulerAngleY: Float
//    ): HeadMovement {
//        return when {
//            headEulerAngleY > EULER_Y_RIGHT_MOVEMENT -> HeadMovement.RIGHT
//            headEulerAngleY < EULER_Y_LEFT_MOVEMENT -> HeadMovement.LEFT
//            else -> HeadMovement.CENTER
//        }
//    }



//    private fun isFaceInsideOval(
//        faceCenter: Offset,
//        faceWidth: Float,
//        faceHeight: Float,
//        nosePosition: Offset?,
//        leftEyePosition: Offset?,
//        rightEyePosition: Offset?,
//        leftCheekPosition: Offset?,
//        rightCheekPosition: Offset?,
//        mouthLeftPosition: Offset?,
//        mouthRightPosition: Offset?,
//        isLeftEyeOpen: Boolean,
//        isRightEyeOpen: Boolean,
//        isBlinkDetected: Boolean,
//        isHeadPitchValid: Boolean,
//        headMovement: Boolean,
//        face: Face
//    ): Boolean {
//
//        val dx = (faceCenter.x - ovalCenter.x) / ovalRadiusX
//        val dy = (faceCenter.y - ovalCenter.y) / ovalRadiusY
//        val isCenterInsideOval = (dx * dx + dy * dy) <= 1.0
//
//        val faceRect = Rect(
//            faceCenter.x - faceWidth / 2,
//            faceCenter.y - faceHeight / 2,
//            faceCenter.x + faceWidth / 2,
//            faceCenter.y + faceHeight / 2
//        )
//        val ovalRect = Rect(
//            ovalCenter.x - ovalRadiusX,
//            ovalCenter.y - ovalRadiusY,
//            ovalCenter.x + ovalRadiusX,
//            ovalCenter.y + ovalRadiusY
//        )
//        val overlapsOval = faceRect.overlaps(ovalRect)
//
//        val isNoseInsideOval = nosePosition?.let {
//            val dxNose = (it.x - ovalCenter.x) / ovalRadiusX
//            val dyNose = (it.y - ovalCenter.y) / ovalRadiusY
//            (dxNose * dxNose + dyNose * dyNose) <= 1.0
//        } ?: false
//
//        val isLeftEyeInsideOval = leftEyePosition?.let {
//            val dxLeftEye = (it.x - ovalCenter.x) / ovalRadiusX
//            val dyLeftEye = (it.y - ovalCenter.y) / ovalRadiusY
//            (dxLeftEye * dxLeftEye + dyLeftEye * dyLeftEye) <= 1.0
//        } ?: false
//
//        val isRightEyeInsideOval = rightEyePosition?.let {
//            val dxRightEye = (it.x - ovalCenter.x) / ovalRadiusX
//            val dyRightEye = (it.y - ovalCenter.y) / ovalRadiusY
//            (dxRightEye * dxRightEye + dyRightEye * dyRightEye) <= 1.0
//        } ?: false
//
//        val isMouthLeftInsideOval = mouthLeftPosition?.let {
//            val dxMouthLeft = (it.x - ovalCenter.x) / ovalRadiusX
//            val dyMouthLeft = (it.y - ovalCenter.y) / ovalRadiusY
//            (dxMouthLeft * dxMouthLeft + dyMouthLeft * dyMouthLeft) <= 1.0
//        } ?: false
//
//        val isMouthRightInsideOval = mouthRightPosition?.let {
//            val dxMouthRight = (it.x - ovalCenter.x) / ovalRadiusX
//            val dyMouthRight = (it.y - ovalCenter.y) / ovalRadiusY
//            (dxMouthRight * dxMouthRight + dyMouthRight * dyMouthRight) <= 1.0
//        } ?: false
//
//        val isLeftCheekInsideOval = leftCheekPosition?.let {
//            val dxLeftCheek = (it.x - ovalCenter.x) / ovalRadiusX
//            val dyLeftCheek = (it.y - ovalCenter.y) / ovalRadiusY
//            (dxLeftCheek * dxLeftCheek + dyLeftCheek * dyLeftCheek) <= 1.0
//        } ?: false
//
//        val isRightCheekInsideOval = rightCheekPosition?.let {
//            val dxRightCheek = (it.x - ovalCenter.x) / ovalRadiusX
//            val dyRightCheek = (it.y - ovalCenter.y) / ovalRadiusY
//            (dxRightCheek * dxRightCheek + dyRightCheek * dyRightCheek) <= 1.0
//        } ?: false
//
//        val areEyesOpen = isLeftEyeOpen && isRightEyeOpen
//
//        val faceFitsInOval = faceWidth in (MIN_FACE_SIZE)..(ovalRadiusX * FACE_SIZE_MULTIPLIER) &&
//                faceHeight in MIN_FACE_SIZE..(ovalRadiusY * FACE_SIZE_MULTIPLIER)
//
//
//
//
//        val isTurningRight = face.headEulerAngleY > 10f
//        val isTurningLeft = face.headEulerAngleY < -10f
//
//        // Update the state for turning left and right
//        if (isTurningLeft) hasTurnedLeft.value = true
//        if (isTurningRight) hasTurnedRight.value = true
//
//
//
//        return isCenterInsideOval && overlapsOval && isNoseInsideOval && isLeftEyeInsideOval
//                &&
//                isRightEyeInsideOval
//////
////                && isMouthLeftInsideOval
////                && isMouthRightInsideOval
//                &&
//                isLeftCheekInsideOval && isRightCheekInsideOval && areEyesOpen && faceFitsInOval && hasTurnedRight.value && hasTurnedLeft.value
//
////                && isHeadPitchValid && headMovement
//    }
//



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
        mouthRightPosition: Offset?,
        isLeftEyeOpen: Boolean,
        isRightEyeOpen: Boolean,
        isBlinkDetected: Boolean,
        isHeadPitchValid: Boolean,
        headMovement: Boolean,
        faceNotFarAway: Boolean,
        face: Face
    ): Boolean {

        val dx = (faceCenter.x - ovalCenter.x) / ovalRadiusX
        val dy = (faceCenter.y - ovalCenter.y) / ovalRadiusY
        val isCenterInsideOval = (dx * dx + dy * dy) <= 1.0

        val faceRect = Rect(
            faceCenter.x - faceWidth / 2,
            faceCenter.y - faceHeight / 2,
            faceCenter.x + faceWidth / 2,
            faceCenter.y + faceHeight / 2
        )
        val ovalRect = Rect(
            ovalCenter.x - ovalRadiusX,
            ovalCenter.y - ovalRadiusY,
            ovalCenter.x + ovalRadiusX,
            ovalCenter.y + ovalRadiusY
        )
        val overlapsOval = faceRect.overlaps(ovalRect)
        val faceDetected = isCenterInsideOval && overlapsOval









        val isTurnedToRight = face.headEulerAngleY > -4f
        val isTurnedToLeft = face.headEulerAngleY < -4f
        val isLookingStraight = face.headEulerAngleY in -10f..10f

        if (isTurnedToLeft) hasTurnedLeft.value = true
        if (isTurnedToRight) hasTurnedRight.value = true

        // Example of calling updateChecks with determined conditions

        val faceTilted = isTurnedToLeft && isTurnedToRight

        Log.d("faceDetected", faceDetected.toString())
        Log.d("isBlinkDetected", isBlinkDetected.toString())
        Log.d("isLookingStraight", isLookingStraight.toString())
        Log.d("hasTurnedRight", hasTurnedRight.value.toString())
        Log.d("hasTurnedLeft", hasTurnedLeft.value.toString())
        Log.d("faceNotFarAway", faceNotFarAway.toString())
        Log.d("checks passed", checksPassed.value.toString())


        updateChecks(faceDetected, faceTilted, isBlinkDetected , isLookingStraight, hasTurnedRight.value, hasTurnedLeft.value, faceNotFarAway)

        return faceDetected && hasTurnedRight.value && hasTurnedLeft.value && isBlinkDetected && isLookingStraight && faceNotFarAway
    }


}


//fun validateImageFeatures(bitmap: Bitmap, callback: (Boolean) -> Unit) {
//    val image = InputImage.fromBitmap(bitmap, 0)
//
//    val options = FaceDetectorOptions.Builder()
//        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
//        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
//        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
//        .build()
//
//    val detector = FaceDetection.getClient(options)
//
//    detector.process(image)
//        .addOnSuccessListener { faces ->
//            val allFeaturesPresent = faces.isNotEmpty() && faces.all { face ->
//                face.getLandmark(FaceLandmark.NOSE_BASE) != null &&
//                        face.getLandmark(FaceLandmark.LEFT_EYE) != null &&
//                        face.getLandmark(FaceLandmark.RIGHT_EYE) != null &&
//                        face.getLandmark(FaceLandmark.MOUTH_LEFT) != null &&
//                        face.getLandmark(FaceLandmark.MOUTH_RIGHT) != null
//            }
//            callback(allFeaturesPresent)
//        }
//        .addOnFailureListener { e ->
//            e.printStackTrace()
//            callback(false)
//        }
//        .addOnCompleteListener {
//            detector.close()
//        }
//}

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


