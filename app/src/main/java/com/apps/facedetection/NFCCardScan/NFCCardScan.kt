/**
 * NFCCardScan.kt
 *
 * This Kotlin file is part of the Yea App - Youth Banking Project.
 *
 * Author: Harrison Kungu
 * Date: 30/01/2025
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

package com.apps.facedetection.NFCCardScan

import NFCViewModelFactory
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apps.facedetection.NfcAvailability

//on opening this screen instialize nfc class and start scanning
// the nfc should be a class not an activity

@SuppressLint("RememberReturnType")
@Composable
fun NFCCardScanScreen() {
//    val context = LocalContext.current
//    val nfcViewModelFactory = remember { NFCViewModelFactory(context as ComponentActivity) }
//    val nfcViewModel: NFCViewModel = viewModel(factory = nfcViewModelFactory)
//


    val context = LocalContext.current
    val nfcViewModelFactory = remember { NFCViewModelFactory(context as ComponentActivity) }
    val nfcViewModel: NFCViewModel = viewModel(factory = nfcViewModelFactory)
    val nfcData: NFCData by nfcViewModel.nfcData.collectAsState()
    val nfcAvailability: NfcAvailability by nfcViewModel.nfcAvailability.collectAsState()

    LaunchedEffect(key1 = true) {
        nfcViewModel.enableNFCForegroundDispatch()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding(),
    ) { paddingValues: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {


                    Column(Modifier.padding(paddingValues)) {


                        when (nfcAvailability) {
                            NfcAvailability.NotAvailable -> {
                                Text(text = "NFC is not available on this device.")
                            }

                            NfcAvailability.Disabled -> {
                                Text(text = "NFC is disabled. Please enable it in your device settings.")
                            }

                            NfcAvailability.Error -> {
                                Text(text = "An error occurred while initializing NFC.")
                            }

                            NfcAvailability.Available -> {
                                Text(text = "Available Now")
                                if (nfcData.errorMessage != null) {
                                    Text(text = "Error: ${nfcData.errorMessage}")
                                } else {
                                    if (nfcData.cardNumber != null) {
                                        Text(text = "Card Number: ${nfcData.cardNumber}")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (nfcData.dateOfBirth != null) {
                                        Text(text = "Date of Birth: ${nfcData.dateOfBirth}")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (nfcData.base64Image != null) {
                                        Text(text = "Base64 Image: ${nfcData.base64Image}")
                                        // You would decode and display the image here
                                    }
                                }
                            }

                            else -> {
                                Text(text = "Scanning for NFC tag...")
                            }
                        }



                    }



        }

    }
}





