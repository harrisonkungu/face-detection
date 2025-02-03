/**
 * NFCViewModel.kt
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

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.util.Log
import androidx.compose.animation.core.copy
import androidx.compose.ui.input.key.type
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.facedetection.NFCManager
import com.apps.facedetection.NfcAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class NFCViewModel(private val nfcManager: NFCManager) : ViewModel() {

    private val _nfcData = MutableStateFlow(NFCData())
    val nfcData: StateFlow<NFCData> = _nfcData

    val nfcAvailability: StateFlow<NfcAvailability> = nfcManager.nfcAvailability
    companion object {
        const val TAG = "nfc_test"
    }


//    private val _nfcAvailability = MutableStateFlow(NfcAvailability.Unknown)
//    val nfcAvailability: StateFlow<NfcAvailability> = _nfcAvailability




    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

    private val _tagDiscovered = MutableStateFlow<Tag?>(null)
    val tagDiscovered: StateFlow<Tag?> = _tagDiscovered




    init {
        viewModelScope.launch {

            Log.e("NFC:STEP1", "==>>")



            nfcManager.tagDiscovered.collectLatest { tag ->
                if (tag != null) {
                    readTagData(tag)
                }else{
                    Log.e("NFC1", "Tag is null")
                }
            }
        }
    }

    fun enableNFCForegroundDispatch() {
        nfcManager.enableForegroundDispatch()
    }

    fun disableNFCForegroundDispatch() {
        nfcManager.disableForegroundDispatch()
    }

    fun handleIntent(intent: Intent) {
        nfcManager.handleIntent(intent)
    }
    private fun readTagData(tag: Tag) {
        Log.e("NFC1", "Error reading tag")
        viewModelScope.launch {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()
                    val ndefMessage = ndef.ndefMessage
                    val records = ndefMessage.records
                    for (record in records) {
                        if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                            readTextRecord(record)
                        }
                    }
                } catch (e: Exception) {
                    _nfcData.value = NFCData(errorMessage = "Error reading tag: ${e.message}")
                    Log.e("NFC", "Error reading tag", e)
                } finally {
                    try {
                        ndef.close()
                    } catch (e: Exception) {
                        Log.e("NFC", "Error closing tag", e)
                    }
                }
            } else {
                _nfcData.value = NFCData(errorMessage = "Tag is not NDEF formatted")
            }
        }
    }










    private fun readTextRecord(record: NdefRecord) {
        val text = getTextFromRecord(record)
        // Basic parsing logic - you'll need to adapt this based on your card's data format
        if (text.contains("CardNumber:")) {
            val cardNumber = text.substringAfter("CardNumber:").trim()
            _nfcData.value = _nfcData.value.copy(cardNumber = cardNumber)
        }
        if (text.contains("DateOfBirth:")) {
            val dateOfBirth = text.substringAfter("DateOfBirth:").trim()
            _nfcData.value = _nfcData.value.copy(dateOfBirth = dateOfBirth)
        }
        if (text.contains("Base64Image:")) {
            val base64Image = text.substringAfter("Base64Image:").trim()
            _nfcData.value = _nfcData.value.copy(base64Image = base64Image)
        }
    }

    private fun getTextFromRecord(record: NdefRecord): String {
        val payload = record.payload
        val textEncoding = if ((payload[0].toInt() and 128) == 0) "UTF-8" else "UTF-16"
//        val languageCodeLength = payload[0].toInt() and 0063
        val languageCodeLength = payload[0].toInt()
        return try {
            String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, Charset.forName(textEncoding))
        } catch (e: UnsupportedEncodingException) {
            "Unsupported Encoding"
        }
    }

    private fun toHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (i in bytes.size - 1 downTo 0) {
            val b = bytes[i].toInt() and 0xff
            if (b < 0x10) sb.append('0')
            sb.append(Integer.toHexString(b))
            if (i > 0) {
                sb.append(" ")
            }
        }
        return sb.toString()
    }

    private fun toReversedHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (i in bytes.indices) {
            if (i > 0) {
                sb.append(" ")
            }
            val b = bytes[i].toInt() and 0xff
            if (b < 0x10) sb.append('0')
            sb.append(Integer.toHexString(b))
        }
        return sb.toString()
    }

    private fun toDec(bytes: ByteArray): Long {
        var result: Long = 0
        var factor: Long = 1
        for (i in bytes.indices) {
            val value = bytes[i].toLong() and 0xffL
            result += value * factor
            factor *= 256L
        }
        return result
    }

    private fun toReversedDec(bytes: ByteArray): Long {
        var result: Long = 0
        var factor: Long = 1
        for (i in bytes.size - 1 downTo 0) {
            val value = bytes[i].toLong() and 0xffL
            result += value * factor
            factor *= 256L
        }
        return result
    }
}

data class NFCData(
    val cardNumber: String? = null,
    val base64Image: String? = null,
    val dateOfBirth: String? = null,
    val errorMessage: String? = null
)