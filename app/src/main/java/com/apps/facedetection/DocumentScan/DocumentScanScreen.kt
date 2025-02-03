/**
 * DocumentScanScreen.kt
 *
 * This Kotlin file is part of the Yea App - Youth Banking Project.
 *
 * Author: Harrison Kungu
 * Date: 02/01/2025
 *
 * Copyright (c) 2025 Co-operative Bank of Kenya - We are You
 * All rights reserved.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.apps.facedetection.DocumentScan

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.Executors


@SuppressLint("RememberReturnType")
@Composable
fun DocumentScanScreen() {
    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    var isCameraShown by remember { mutableStateOf(true) }
    var isFaceDetected by remember { mutableStateOf(false) }
    var imageCorrect = remember { mutableStateOf(false) }

    var capturedPhoto by remember { mutableStateOf<ImageBitmap?>(null) }
    var ovalCenter by remember { mutableStateOf<Offset?>(null) }

    val cameraController: LifecycleCameraController =
        remember { LifecycleCameraController(context) }
    val cameraProvider = remember { ProcessCameraProvider.getInstance(context) }
    var isFrontCameraAvailable = remember { mutableStateOf(false) }
    var isBackCameraAvailable = remember { mutableStateOf(false) }
    var isUsingFrontCamera = remember { mutableStateOf(true) }

    val cornerColor = remember { mutableStateOf(Color.White) }

    val cameraPreviewView = remember {
        mutableStateOf(PreviewView(context))
    }




    val TAG = "IdCaptureStepTwo"
    var lensFacing by remember { mutableIntStateOf(1) }
    val previewView = remember { PreviewView(context) }

    var imageCapture: ImageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(Surface.ROTATION_0)
            .build()
    }
    val imageAnalysis: ImageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    var validCard: Boolean by remember { mutableStateOf(false) }

    var counter by remember {
        mutableIntStateOf(0)
    }


//    LaunchedEffect(key1 = Unit) {
//        imageAnalysis.setAnalyzer(
//            ContextCompat.getMainExecutor(context),
//            TextRecognitionAnalyzer(
//                onTextDetected = { text ->
//
//                    val escapedText = text.replace("\n", " ")
//
//
//                },
//                onObjectsDetected = { objects -> }
//            )
//        );
//    }



    var isSystemPermissionDialog by remember {
        mutableStateOf(false)
    }
    var isCustomPermissionDialogVisible by remember {
        mutableStateOf(false)
    }


    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding(),
    ) { paddingValues: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

            if(!imageCorrect.value) {
                if (isCameraShown) {
                    Column(Modifier.padding(paddingValues)) {
                        RealTimeCameraAnalyzer(
                            cornerColor,
                            onTextDetected = {
                                Log.d("ImageCapture", "Image saved at: $it")
                            },
                            onImageCaptured = {
                                Log.e("MLKit", "Text recognition success: ${it}")
                            })
                    }


                }

            }

        }

    }
}





@Composable
fun RealTimeCameraAnalyzer(cornerColor:MutableState<Color>, onTextDetected: (String) -> Unit,  onImageCaptured: (Uri?) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as LifecycleOwner
    val isFlashOn = remember { mutableStateOf(false) } // State to manage flashlight status
    val outputDirectory = remember { context.filesDir } // Output directory for captured images

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        )  {

            Column {  }

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    toggleFlashlight(context, lifecycleOwner, !isFlashOn.value)
                    isFlashOn.value = !isFlashOn.value
                }) {
                    Text(text = if (isFlashOn.value) "Turn Off Flash" else "Turn On Flash")
                }
                Button(onClick = {
                    takePicture(context, outputDirectory,) {
                        Log.d("test-->> ", it.toString())
                    }
//                    captureImage(context, lifecycleOwner, outputDirectory, onImageCaptured)
                }) {
                    Text(text = "Capture Image")
                }
            }


            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    startCamera(context, lifecycleOwner, previewView){ detectedText ->
                        onTextDetected(detectedText)
                        val keywords = listOf("CERTIFICATE OF BIRTH", "Birth in the", "Proof of Kenyan",  "Province",   "proof of")
                        if (keywords.all { keyword -> detectedText.contains(keyword, ignoreCase = true) }) {
                            cornerColor.value = Color.Green
                        } else {
                            cornerColor.value = Color.White
                        }

                    }
                    previewView
                },
                modifier = Modifier
                    .fillMaxWidth()
            )

            // Flashlight and Capture Buttons
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    toggleFlashlight(context, lifecycleOwner, !isFlashOn.value)
                    isFlashOn.value = !isFlashOn.value
                }) {
                    Text(text = if (isFlashOn.value) "Turn Off Flash" else "Turn On Flash")
                }
                Button(onClick = {
                    takePicture(context, outputDirectory,) {
                        Log.d("test-->> ", it.toString())
                    }
//                    captureImage(context, lifecycleOwner, outputDirectory, onImageCaptured)
                }) {
                    Text(text = "Capture Image")
                }
            }

        }



        Canvas(modifier = Modifier.fillMaxSize()) {
            val aspectRatio = 16f / 9f
            val guideWidth = size.width * 1f
            val guideHeight = (guideWidth / aspectRatio) + 180

            val guideLeft = (size.width - guideWidth) / 2f
            val guideTop = (size.height - guideHeight) / 2f

            val cornerLength = 40f
            val strokeWidth = 6f

            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                size = size
            )

            drawRect(
                color = Color.Transparent,
                topLeft = Offset(guideLeft, guideTop),
                size = Size(guideWidth, guideHeight),
                blendMode = BlendMode.Clear
            )

            drawLine(
                color = cornerColor.value,
                start = Offset(guideLeft, guideTop),
                end = Offset(guideLeft + cornerLength, guideTop),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor.value,
                start = Offset(guideLeft, guideTop),
                end = Offset(guideLeft, guideTop + cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )


            drawLine(
                color = cornerColor.value,
                start = Offset(guideLeft + guideWidth, guideTop),
                end = Offset(guideLeft + guideWidth - cornerLength, guideTop),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor.value,
                start = Offset(guideLeft + guideWidth, guideTop),
                end = Offset(guideLeft + guideWidth, guideTop + cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )


            drawLine(
                color = cornerColor.value,
                start = Offset(guideLeft, guideTop + guideHeight),
                end = Offset(guideLeft + cornerLength, guideTop + guideHeight),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor.value,
                start = Offset(guideLeft, guideTop + guideHeight),
                end = Offset(guideLeft, guideTop + guideHeight - cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )


            drawLine(
                color = cornerColor.value,
                start = Offset(guideLeft + guideWidth, guideTop + guideHeight),
                end = Offset(guideLeft + guideWidth - cornerLength, guideTop + guideHeight),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor.value,
                start = Offset(guideLeft + guideWidth, guideTop + guideHeight),
                end = Offset(guideLeft + guideWidth, guideTop + guideHeight - cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}


//private fun startCamera(
//    context: Context,
//    lifecycleOwner: LifecycleOwner,
//    previewView: PreviewView,
//    onTextDetected: (String) -> Unit
//) {
//    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
//    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//    val cameraExecutor = Executors.newSingleThreadExecutor()
//
//    cameraProviderFuture.addListener({
//        val cameraProvider = cameraProviderFuture.get()
//        val preview = Preview.Builder().build().apply {
//            setSurfaceProvider(previewView.surfaceProvider)
//        }
//
//        val imageAnalyzer = ImageAnalysis.Builder()
//            .setTargetResolution(android.util.Size(1280, 720))
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//            .also { analysis ->
//                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
//                    processImageProxy(imageProxy, textRecognizer, onTextDetected)
//                }
//            }
//
//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//        try {
//            cameraProvider.unbindAll()
//            cameraProvider.bindToLifecycle(
//                lifecycleOwner,
//                cameraSelector,
//                preview,
//                imageAnalyzer
//            )
//        } catch (e: Exception) {
//            Log.e("CameraX", "Binding failed", e)
//        }
//    }, ContextCompat.getMainExecutor(context))
//}



private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onTextDetected: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val cameraExecutor = Executors.newSingleThreadExecutor()

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        // Create Preview Use Case
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

        // Create Image Analysis Use Case
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9) // Use aspect ratio for compatibility
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        processImageProxy(imageProxy, textRecognizer, onTextDetected)
                    } catch (e: Exception) {
                        Log.e("CameraX", "Error analyzing image", e)
                    }
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // Initialize ImageCapture
        initializeImageCapture()


        try {
            // Unbind all use cases before binding new ones
            cameraProvider.unbindAll()

            // Bind the preview and image analyzer use cases
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (e: IllegalArgumentException) {
            Log.e("CameraX", "No supported use case combination found", e)
        } catch (e: Exception) {
            Log.e("CameraX", "Binding failed", e)
        }
    }, ContextCompat.getMainExecutor(context))

    // Shutdown executor when lifecycle ends
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            cameraExecutor.shutdown()
        }
    })
}


private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        // Initialize ImageCapture
        initializeImageCapture()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e("CameraX", "Use case binding failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}


@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    textRecognizer: com.google.mlkit.vision.text.TextRecognizer,
    onTextDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                onTextDetected(visionText.text)
            }
            .addOnFailureListener { e ->
                Log.e("MLKit", "Text recognition failed: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}




// Function to toggle flashlight
private fun toggleFlashlight(context: Context, lifecycleOwner: LifecycleOwner, enable: Boolean) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
        )

        val cameraControl = camera.cameraControl
        cameraControl.enableTorch(enable) // Enable or disable flashlight
    }, ContextCompat.getMainExecutor(context))
}

// Function to capture image
//private fun captureImage(
//    context: Context,
//    lifecycleOwner: LifecycleOwner,
//    outputDirectory: File,
//    onImageCaptured: (Uri?) -> Unit
//) {
//    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
//    cameraProviderFuture.addListener({
//        val cameraProvider = cameraProviderFuture.get()
//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//        val imageCapture = ImageCapture.Builder().build()
//        cameraProvider.bindToLifecycle(
//            lifecycleOwner,
//            cameraSelector,
//            imageCapture
//        )
//
//        val photoFile = File(outputDirectory, "captured_image_${System.currentTimeMillis()}.jpg")
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(context),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
//                    onImageCaptured(savedUri)
//                }
//
//                override fun onError(exception: ImageCaptureException) {
//                    Log.e("CameraX", "Image capture failed: ${exception.message}", exception)
//                    onImageCaptured(null)
//                }
//            }
//        )
//    }, ContextCompat.getMainExecutor(context))
//}
//


private fun captureImage(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    outputDirectory: File,
    onImageCaptured: (Uri?) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        // Unbind all previous use cases
        cameraProvider.unbindAll()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // Create ImageCapture Use Case
        val imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            imageCapture
        )

        val photoFile = File(outputDirectory, "captured_image_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    onImageCaptured(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Image capture failed: ${exception.message}", exception)
                    onImageCaptured(null)
                }
            }
        )
    }, ContextCompat.getMainExecutor(context))
}



private var imageCapture: ImageCapture? = null

fun initializeImageCapture() {
    imageCapture = ImageCapture.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()
}

fun takePicture(context:Context, outputDirectory: File, onImageCaptured: (Uri?) -> Unit) {
    Log.d("test-->> ", "1")
    imageCapture?.let { capture ->
        val photoFile = File(outputDirectory, "captured_image_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("test-->> ", "2")
                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    onImageCaptured(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Image capture failed: ${exception.message}", exception)
                    onImageCaptured(null)
                }
            }
        )
    }
}
