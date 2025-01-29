/**
 * TextRecognitionAnalyzer.kt
 *
 * This Kotlin file is part of the Yea App - Youth Banking Project.
 *
 * Author: Harrison Kungu
 * Date: 03/01/2025
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
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


//class TextRecognitionAnalyzer(
//    private val onTextDetected: (String) -> Unit,
//    private val onObjectsDetected: (List<DetectedObject>) -> Unit,
//) : ImageAnalysis.Analyzer {
//
//    companion object {
//        const val THROTTLE_TIMEOUT_MS = 250L // Reduce the delay to improve speed
//    }
//    private val TAG = "TextRecognition"
//
//    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//    private val textRecognizer: TextRecognizer =
//        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//
//    private val options = ObjectDetectorOptions.Builder()
//        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
//        .enableMultipleObjects()
//        .enableClassification()  // Optional
//        .build()
//    private val objectRecognizer = ObjectDetection.getClient(options)
//
//    @ExperimentalGetImage
//    override fun analyze(imageProxy: ImageProxy) {
//        val mediaImage = imageProxy.image
//        if (mediaImage != null) {
//            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//            scope.launch {
//                val textFlow = processTextRecognition(inputImage)
//                val objectFlow = processObjectDetection(inputImage)
//
//                combine(textFlow, objectFlow) { textResult, objectResult ->
//                    onTextDetected(textResult)
//                    onObjectsDetected(objectResult)
//                }.collectLatest {  }
//                delay(THROTTLE_TIMEOUT_MS)
//                imageProxy.close()
//            }
//        } else {
//            imageProxy.close()
//        }
//    }
//    private fun processTextRecognition(inputImage: InputImage) = callbackFlow {
//        textRecognizer.process(inputImage)
//            .addOnSuccessListener { visionText: Text ->
//                Log.w("TEXT DETECTED ","TEXT DETECTED "+ visionText.text.replace("\n", " "))
//
//                trySend(visionText.text).isSuccess
//            }
//            .addOnFailureListener {
//                trySend("None").isFailure
//            }
//            .addOnCompleteListener {
//                close()
//            }
//        awaitClose { /* Cleanup if needed */ }
//    }
//
//    private fun processObjectDetection(inputImage: InputImage) = callbackFlow {
//        objectRecognizer.process(inputImage)
//            .addOnSuccessListener { objects: List<DetectedObject> ->
//                trySend(objects).isSuccess
//            }
//            .addOnFailureListener {
//                trySend(emptyList()).isFailure
//            }
//            .addOnCompleteListener {
//                close()
//            }
//        awaitClose { /* Cleanup if needed */ }
//    }
//
//}






