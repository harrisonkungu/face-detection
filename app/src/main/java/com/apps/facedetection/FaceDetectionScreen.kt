package com.apps.facedetection

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Paint
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

private const val OVAL_WIDTH_DP = 300
private const val OVAL_HEIGHT_DP = 350
private const val OVAL_LEFT_OFFSET_RATIO = 2
private const val OVAL_TOP_OFFSET_RATIO = 3

@SuppressLint("RememberReturnType")
@Composable
fun FaceDetectionScreen() {
    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    var isCameraShown by remember { mutableStateOf(true) }
    var isFaceDetected by remember { mutableStateOf(false) }

    var capturedPhoto by remember { mutableStateOf<ImageBitmap?>(null) }
    var ovalCenter by remember { mutableStateOf<Offset?>(null) }

    val cameraController: LifecycleCameraController =
        remember { LifecycleCameraController(context) }
    cameraController.cameraSelector =
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

    val cameraPreviewView = remember {
        mutableStateOf(PreviewView(context))
    }
    val density = LocalDensity.current

    val ovalWidthPx = with(density) { OVAL_WIDTH_DP.dp.toPx() }
    val ovalHeightPx = with(density) { OVAL_HEIGHT_DP.dp.toPx() }


    // Locks portrait orientation for duration of challenge and resets on complete


    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding(),
    ) { paddingValues: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isCameraShown) {
//                LockPortraitOrientation { resetOrientation ->
//                    Surface(color = MaterialTheme.colorScheme.background) {
//                        AlwaysOnMaxBrightnessScreen()
//                        CameraView(paddingValues, cameraPreviewView)
//                    }
//                }
                CameraView(paddingValues, cameraPreviewView)

            } else {
                capturedPhoto?.let { photo ->
                    CapturedPhotoView(photo)
                }
            }

            OvalOverlay(
                modifier = Modifier.fillMaxSize(),
                isFaceDetected = isFaceDetected,
                onCenterCalculated = { ovalCenter = it }
            )


            ovalCenter?.let { offset->
                startFaceDetection(
                    context = context,
                    cameraController = cameraController,
                    lifecycleOwner = lifecycleOwner,
                    previewView = cameraPreviewView.value,
                    ovalRect = offset,
                    onFaceDetected = { detected ->
                        isFaceDetected = detected
                    },
                )
            }

            if (isCameraShown) {
                CapturePhotoButton(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 50.dp),
                    isFaceDetected = isFaceDetected,
                    onButtonClicked = {
                        capturePhotoAndReplaceBackground(
                            context,
                            cameraController,
                            isFaceDetected
                        ) { capturedBitmap ->
                            capturedPhoto = capturedBitmap.asImageBitmap()
                            if (isFaceDetected)
                                isCameraShown = false
                        }
                    },
                )
            }
        }

    }
}

@Composable
private fun CapturedPhotoView(photo: ImageBitmap) {
    Image(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize(),
        bitmap = photo,
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun CameraView(
    paddingValues: PaddingValues,
    cameraPreviewView: MutableState<PreviewView>,
) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }.also {
                cameraPreviewView.value = it
            }
        },
    )
}

@Composable
private fun CapturePhotoButton(
    modifier: Modifier,
    isFaceDetected: Boolean,
    onButtonClicked: ()->Unit
) {

    Image(
        modifier = modifier
            .padding(top = 20.dp)
            .size(92.dp)
            .clickable {
                onButtonClicked()

            },
        painter = painterResource(
            id =
            if (isFaceDetected)
                R.drawable.camera_button_enabled
            else
                R.drawable.camera_button_disabled,
        ),
        contentDescription = null,
    )

}

private fun capturePhotoAndReplaceBackground(
    context: Context,
    cameraController: LifecycleCameraController,
    isFaceDetected: Boolean,
    onBackgroundReplaced: (Bitmap) -> Unit,
) {
    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    if (isFaceDetected) {
        cameraController.takePicture(
            mainExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val capturedBitmap: Bitmap =
                        image.toBitmap().rotateBitmap(image.imageInfo.rotationDegrees)

                    processCapturedPhotoAndReplaceBackground(capturedBitmap) { processedBitmap ->
                        onBackgroundReplaced(processedBitmap)
                    }

                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                }
            },
        )
    }
}

private fun processCapturedPhotoAndReplaceBackground(
    capturedBitmap: Bitmap,
    onBackgroundReplaced: (Bitmap) -> Unit,
) {
    onBackgroundReplaced(capturedBitmap)
}

private fun startFaceDetection(
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


@Composable
private fun OvalOverlay(
    modifier: Modifier = Modifier,
    isFaceDetected: Boolean,
    onCenterCalculated: (Offset) -> Unit = {},
) {
    val sweepAngle by animateFloatAsState(
        targetValue = if (isFaceDetected) 360f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        ), label = ""
    )


    val ovalColor =
        if (isFaceDetected) Color.Green else Color.Red
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        val ovalCenterOffset = remember {
            mutableStateOf<Offset?>(null)
        }
        LaunchedEffect(ovalCenterOffset) {
            ovalCenterOffset.value?.let { onCenterCalculated(it) }
        }
        Canvas(modifier = modifier) {
            val ovalSize = Size(OVAL_WIDTH_DP.dp.toPx(), OVAL_HEIGHT_DP.dp.toPx())
            val ovalLeftOffset = (size.width - ovalSize.width) / OVAL_LEFT_OFFSET_RATIO
            val ovalTopOffset = (size.height - ovalSize.height) / OVAL_TOP_OFFSET_RATIO

            ovalCenterOffset.value =
                Offset(
                    (ovalLeftOffset + OVAL_WIDTH_DP / OVAL_LEFT_OFFSET_RATIO),
                    (ovalTopOffset - OVAL_HEIGHT_DP / OVAL_TOP_OFFSET_RATIO)
                )

            val ovalRect =
                Rect(
                    ovalLeftOffset,
                    ovalTopOffset,
                    ovalLeftOffset + ovalSize.width,
                    ovalTopOffset + ovalSize.height
                )
            val ovalPath = Path().apply {
                addOval(ovalRect)
            }
            clipPath(ovalPath, clipOp = ClipOp.Difference) {
                drawRect(SolidColor(Color.Black.copy(alpha = 0.95f)))
            }
        }


        Canvas(
            modifier = modifier,
        ) {
            val ovalSize = Size(OVAL_WIDTH_DP.dp.toPx(), OVAL_HEIGHT_DP.dp.toPx())
            val ovalLeft = (size.width - ovalSize.width) / OVAL_LEFT_OFFSET_RATIO
            val ovalTop =
                (size.height - ovalSize.height) / OVAL_TOP_OFFSET_RATIO - ovalSize.height
            drawOval(
                color = ovalColor,
                style = Stroke(width = OVAL_TOP_OFFSET_RATIO.dp.toPx()),
                topLeft = Offset(ovalLeft, ovalTop + ovalSize.height),
                size = ovalSize,
            )

            drawArc(
                color = Color.Green,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(ovalLeft, ovalTop + ovalSize.height),
                size = ovalSize,
                style = Stroke(width = 10f)
            )
        }

    }
}



@Composable
fun AnimatedOvalOverlay(
    modifier: Modifier = Modifier,
    isFaceDetected: Boolean,
    ovalCenter: Offset?,
    ovalWidth: Float,
    ovalHeight: Float
) {
    val sweepAngle by animateFloatAsState(
        targetValue = if (isFaceDetected) 360f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        ), label = ""
    )

    if (ovalCenter != null) {
        Canvas(modifier = modifier) {


            // Draw the static oval
            drawOval(
                color = Color.Gray.copy(alpha = 0.5f),
                topLeft = Offset(
                    ovalCenter.x - ovalWidth / 2,
                    ovalCenter.y - ovalHeight / 2
                ),
                size = Size(ovalWidth, ovalHeight),
                style = Stroke(width = 5f)
            )

            // Draw the animated arc
            drawArc(
                color = Color.Green,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    ovalCenter.x - ovalWidth / 2,
                    ovalCenter.y - ovalHeight / 2
                ),
                size = Size(ovalWidth, ovalHeight),
                style = Stroke(width = 10f)
            )
        }
    }
}




@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun LockPortraitOrientation(content: @Composable (resetOrientation: () -> Unit) -> Unit) {

    val context = LocalContext.current
    val activity = context.findActivity()



    val originalOrientation by rememberSaveable { mutableIntStateOf(activity!!.requestedOrientation) }
    SideEffect {
        if (activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // wait until screen is rotated correctly
    if (activity != null) {
        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            content {
                activity.requestedOrientation = originalOrientation
            }
        }
    }
}


internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


@Composable
internal fun AlwaysOnMaxBrightnessScreen() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val originalBrightness = activity.window.attributes.screenBrightness
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity.window.attributes = activity.window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        }
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.window.attributes = activity.window.attributes.apply {
                screenBrightness = originalBrightness
            }
        }
    }
}
