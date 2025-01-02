package com.apps.facedetection

import android.Manifest.permission.CAMERA
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.apps.facedetection.DocumentScan.DocumentScanScreen
import com.apps.facedetection.FaceDetection.FaceDetectionScreen
import com.apps.facedetection.ui.theme.FaceDetectionTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FaceDetectionTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    var faceDetection by remember { mutableStateOf(false) }
    var documentScan by remember { mutableStateOf(false) }
    var screenType by remember { mutableStateOf("") }
    val context = LocalContext.current

    val cameraPermissionState = rememberPermissionState(permission = CAMERA) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_LONG).show()
        } else {
            when {
                faceDetection -> screenType = "faceDetect"
                documentScan -> screenType = "docScan"
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            when (screenType) {
                "docScan" -> {
                    DocumentScanScreen()
                }

                "faceDetect" -> {
                    FaceDetectionScreen()
                }

                else -> {
                    FaceDetection(
                        onRequestPermission = {
                            faceDetection = true
                            documentScan = false
                            if (cameraPermissionState.status.isGranted) {
                                screenType = "faceDetect"
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }
                    )
                    DocumentScan(
                        onRequestPermission = {
                            faceDetection = false
                            documentScan = true
                            if (cameraPermissionState.status.isGranted) {
                                screenType = "docScan"
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FaceDetection(onRequestPermission: () -> Unit) {
    Button(onClick = { onRequestPermission() }) {
        Text(text = "Face Detection")
    }
}


@Composable
fun DocumentScan(onRequestPermission: () -> Unit) {
    Button(onClick = { onRequestPermission() }) {
        Text(text = "Scan Document")
    }
}
