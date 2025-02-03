package com.apps.facedetection

import NFCViewModelFactory
import android.Manifest.permission.CAMERA
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apps.facedetection.DocumentScan.DocumentScanScreen
import com.apps.facedetection.FaceDetection.FaceDetectionScreen
import com.apps.facedetection.NFCCardScan.NFCCardScanScreen
import com.apps.facedetection.NFCCardScan.NFCViewModel
//import com.apps.facedetection.FaceDetection.InfiniteCircularList
//import com.apps.facedetection.FaceDetection.SmoothPeekViewList
//import com.apps.facedetection.FaceDetection.SmoothScrollingList
import com.apps.facedetection.ui.theme.FaceDetectionTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    private lateinit var nfcManager: NFCManager
    override fun onCreate(savedInstanceState: Bundle?) {
        nfcManager = NFCManager(this)
        super.onCreate(savedInstanceState)
        setContent {
            FaceDetectionTheme {
                MainScreen()
            }
        }
    }



    override fun onResume() {
        super.onResume()
        nfcManager.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch()
    }

//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        nfcManager.handleIntent(intent)
//    }




    override fun onNewIntent(intent: Intent) {
        Log.d("test log", "handle intent")
        super.onNewIntent(intent)
        setIntent(intent)
        resolveIntent(intent)
    }

    private fun resolveIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == action
        ) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                nfcManager.detectTagData(it)
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


    // Create the ViewModel and factory here
//    val nfcViewModelFactory = remember { NFCViewModelFactory(context as ComponentActivity) }
//    val nfcViewModel: NFCViewModel = viewModel(factory = nfcViewModelFactory)



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
                "nfcCardRead" -> {
                    NFCCardScanScreen()
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
                    DetectNFC(
                        onRequestPermission = {
                            faceDetection = false
                            documentScan = true
                            if (cameraPermissionState.status.isGranted) {
                                screenType = "nfcCardRead"
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

@Composable
fun DetectNFC(onRequestPermission: () -> Unit) {
    Button(onClick = { onRequestPermission() }) {
        Text(text = "NFC Card Detect")
    }
}

