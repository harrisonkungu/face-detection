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
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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



    val cameraPreviewView = remember {
        mutableStateOf(PreviewView(context))
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
                        RealTimeCameraAnalyzer(){
                            Log.e("MLKit", "Text recognition success: ${it}")
                        }
                    }


                }

            }

        }

    }
}





@Composable
fun RealTimeCameraAnalyzer(onTextDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as LifecycleOwner

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = androidx.camera.view.PreviewView(ctx)
                startCamera(context, lifecycleOwner, previewView, onTextDetected)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay for guide marks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val aspectRatio = 16f / 9f // Landscape aspect ratio
            val guideWidth = size.width * 1f // Maximize width
            val guideHeight = (guideWidth / aspectRatio)+80 // Calculate height based on aspect ratio

            val guideLeft = (size.width - guideWidth) / 2f
            val guideTop = (size.height - guideHeight) / 2f

            val cornerLength = 40f // Length of each corner line
            val strokeWidth = 6f
            val cornerColor = Color.White

            // Draw blurred outside region
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                size = size
            )

            // Clear the guide rectangle area
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(guideLeft, guideTop),
                size = Size(guideWidth, guideHeight),
                blendMode = BlendMode.Clear
            )

            // Draw guide corners
            // Top-left corner
            drawLine(
                color = cornerColor,
                start = Offset(guideLeft, guideTop),
                end = Offset(guideLeft + cornerLength, guideTop),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(guideLeft, guideTop),
                end = Offset(guideLeft, guideTop + cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Top-right corner
            drawLine(
                color = cornerColor,
                start = Offset(guideLeft + guideWidth, guideTop),
                end = Offset(guideLeft + guideWidth - cornerLength, guideTop),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(guideLeft + guideWidth, guideTop),
                end = Offset(guideLeft + guideWidth, guideTop + cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-left corner
            drawLine(
                color = cornerColor,
                start = Offset(guideLeft, guideTop + guideHeight),
                end = Offset(guideLeft + cornerLength, guideTop + guideHeight),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(guideLeft, guideTop + guideHeight),
                end = Offset(guideLeft, guideTop + guideHeight - cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-right corner
            drawLine(
                color = cornerColor,
                start = Offset(guideLeft + guideWidth, guideTop + guideHeight),
                end = Offset(guideLeft + guideWidth - cornerLength, guideTop + guideHeight),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(guideLeft + guideWidth, guideTop + guideHeight),
                end = Offset(guideLeft + guideWidth, guideTop + guideHeight - cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}


private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: androidx.camera.view.PreviewView,
    onTextDetected: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val cameraExecutor = Executors.newSingleThreadExecutor()

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy, textRecognizer, onTextDetected)
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e("CameraX", "Binding failed", e)
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



