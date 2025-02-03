package com.apps.facedetection

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.util.Log
import android.widget.Toast
import com.apps.facedetection.NFCCardScan.NFCViewModel.Companion.TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NFCManager(private val activity: Activity) {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

    private val _tagDiscovered = MutableStateFlow<Tag?>(null)
    val tagDiscovered: StateFlow<Tag?> = _tagDiscovered

    private val _nfcAvailability = MutableStateFlow<NfcAvailability>(NfcAvailability.Unknown)
    val nfcAvailability: StateFlow<NfcAvailability> = _nfcAvailability







    init {
        var nfcAvailable = false
        var nfcEnabled = false
        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(activity)


            Log.e("NFC:STEP2", "==>>")


            if (nfcAdapter == null) {
                Toast.makeText(activity, "NO NFC Capabilities", Toast.LENGTH_SHORT).show()
                Log.e("NFC", "NFC not available on this device")
                _nfcAvailability.value = NfcAvailability.NotAvailable
            } else {
                nfcAvailable = true
                if (!nfcAdapter!!.isEnabled) {
                    Log.e("NFC", "NFC is disabled")
                    _nfcAvailability.value = NfcAvailability.Disabled
                } else {
                    nfcEnabled = true
                    _nfcAvailability.value = NfcAvailability.Available
                }
            }

            if (nfcAvailable && nfcEnabled) {
                val intent = Intent(activity, activity.javaClass).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                pendingIntent =
                    PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE)

                val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                    try {
                        addDataType("text/plain")
                    } catch (e: IntentFilter.MalformedMimeTypeException) {
                        throw RuntimeException("fail", e)
                    }
                }
                val tag = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
                val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)

                Log.e("NFC", "Tag discovered  $tag")

                intentFiltersArray = arrayOf(ndef, tag, tech)

                techListsArray = arrayOf(arrayOf(Ndef::class.java.name))
            }
        } catch (e: Exception) {
            Log.e("NFC", "Error initializing NFC", e)
            _nfcAvailability.value = NfcAvailability.Error
        }
    }


    fun enableForegroundDispatch() {
        if (nfcAvailability.value == NfcAvailability.Available) {
            nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, intentFiltersArray, techListsArray)
        }
    }

    fun disableForegroundDispatch() {
        if (nfcAvailability.value == NfcAvailability.Available) {
            nfcAdapter?.disableForegroundDispatch(activity)
        }
    }

    fun handleIntent(intent: Intent) {
        if (nfcAvailability.value == NfcAvailability.Available) {
            if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED
            ) {
                val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                _tagDiscovered.value = tag
            }
        }
    }

    fun detectTagData(tag: Tag): String {



//        val isoDep = IsoDep.get(tag)
//        isoDep.connect()
        readCardnfo(tag)

//        val selectRoot = byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x00, 0x02, 0x3F.toByte(), 0x00)
//        val response = isoDep.transceive(selectRoot)
//        Log.d("APDU", "Root Select Response: ${toHex(response)}")
//
//
//
//        val getUID = byteArrayOf(0x80.toByte(), 0xCA.toByte(), 0x00, 0x00, 0x00)
//        val uidResponse = isoDep.transceive(getUID)
//        Log.d("APDU", "Card UID: ${toHex(uidResponse)}")
//
//
//        val readData = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x10)
//        val dataResponse = isoDep.transceive(readData)
//        Log.d("APDU", "Read Data: ${dataResponse.toAscii()}")


//        Root Select Response: 00 90
//        Card UID: 00 6d


        val sb = StringBuilder()
        val id = tag.id
        sb.append("ID (hex): ").append(toHex(id)).append('\n')
        sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n')
        sb.append("ID (dec): ").append(toDec(id)).append('\n')
        sb.append("ID (reversed dec): ").append(toReversedDec(id)).append('\n')

        val prefix = "android.nfc.tech."
        sb.append("Technologies: ")
        for (tech in tag.techList) {
            sb.append(tech.substring(prefix.length))
            sb.append(", ")
        }

        sb.delete(sb.length - 2, sb.length)

        for (tech in tag.techList) {
            if (tech == MifareClassic::class.java.name) {
                sb.append('\n')
                var type = "Unknown"
                try {
                    val mifareTag = MifareClassic.get(tag)
                    type = when (mifareTag.type) {
                        MifareClassic.TYPE_CLASSIC -> "Classic"
                        MifareClassic.TYPE_PLUS -> "Plus"
                        MifareClassic.TYPE_PRO -> "Pro"
                        else -> "Unknown"
                    }
                    sb.append("Mifare Classic type: ")
                    sb.append(type)
                    sb.append('\n')

                    sb.append("Mifare size: ")
                    sb.append("${mifareTag.size} bytes")
                    sb.append('\n')

                    sb.append("Mifare sectors: ")
                    sb.append(mifareTag.sectorCount)
                    sb.append('\n')

                    sb.append("Mifare blocks: ")
                    sb.append(mifareTag.blockCount)
                } catch (e: Exception) {
                    sb.append("Mifare classic error: ${e.message}")
                }
            }

            if (tech == MifareUltralight::class.java.name) {
                sb.append('\n')
                val mifareUlTag = MifareUltralight.get(tag)
                val type = when (mifareUlTag.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> "Ultralight"
                    MifareUltralight.TYPE_ULTRALIGHT_C -> "Ultralight C"
                    else -> "Unknown"
                }
                sb.append("Mifare Ultralight type: ")
                sb.append(type)
            }
        }
        Log.v(TAG, sb.toString())
        return sb.toString()
    }

    fun readCardnfo(tag: Tag): String {
        val isoDep = IsoDep.get(tag)
        isoDep.connect()

        try {
            // 1️⃣ Select Root
            val selectRoot = byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x00, 0x02, 0x3F.toByte(), 0x00)
            val response = isoDep.transceive(selectRoot)
            Log.d("APDU", "Root Select Response (Hex): ${toHex(response)}")

            if (response.size < 2 || response[response.size - 2] != 0x90.toByte() || response[response.size - 1] != 0x00.toByte()) {
                Log.e("APDU", "Select Root failed: ${toHex(response)}")
                return "Select Root Failed"
            }

            // 2️⃣ Get UID (Using Tag ID Instead of APDU)
            val uidHex = toHex(tag.id)
            Log.d("APDU", "Card UID (Tag ID): $uidHex")

            // 3️⃣ Select a File Before Reading Data
//            val selectFile = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x00, 0x02, 0xE1.toByte(), 0x01)
//            val fileResponse = isoDep.transceive(selectFile)
//            Log.d("APDU", "Select File Response: ${toHex(fileResponse)}")





//            val selectFile = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x00, 0x02, 0x00.toByte(), 0x02.toByte()) // Example File ID
//            val fileResponse = isoDep.transceive(selectFile)
//            Log.d("APDU", "Select File Response: ${toHex(fileResponse)}")
//            if (fileResponse.size < 2 || fileResponse[fileResponse.size - 2] != 0x90.toByte() || fileResponse[fileResponse.size - 1] != 0x00.toByte()) {
//                Log.e("APDU", "File selection failed: ${toHex(fileResponse)}")
//                return "File Selection Failed"
//            }



//            val selectPSE = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x0E,
//                0x31.toByte(), 0x50.toByte(), 0x41.toByte(), 0x59.toByte(), 0x2E.toByte(), 0x53.toByte(), 0x59.toByte(), 0x53.toByte(),
//                0x2E.toByte(), 0x44.toByte(), 0x44.toByte(), 0x46.toByte(), 0x30.toByte(), 0x31.toByte())
//            val pseResponse = isoDep.transceive(selectPSE)
//            Log.d("APDU", "Select PSE Response: ${toHex(pseResponse)}")



            val listFiles = byteArrayOf(0x00, 0xA8.toByte(), 0x00, 0x00, 0x00)
            val listResponse = isoDep.transceive(listFiles)
            Log.d("APDU", "List Files Response: ${toHex(listResponse)}")


            // 4️⃣ Read Data (Try Reading More)
            val readData = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x20)  // Read 32 bytes
            val dataResponse = isoDep.transceive(readData)
            Log.d("APDU", "Read Data (Hex): ${toHex(dataResponse)}")
            Log.d("APDU", "Read Data (ASCII): ${dataResponse.toAscii()}")





            val selectDir = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x00, 0x02, 0x2F.toByte(), 0x00)
            val dirResponse = isoDep.transceive(selectDir)
            Log.d("APDU", "Select Directory Response: ${toHex(dirResponse)}")


            val selectCardAccess = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x00, 0x02, 0x2F.toByte(), 0x01)
            val accessResponse = isoDep.transceive(selectCardAccess)
            Log.d("APDU", "Select Card Access Response: ${toHex(accessResponse)}")

// Try reading 32 bytes from it
            val readCardAccess = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x20)
            val readResponse = isoDep.transceive(readCardAccess)
            Log.d("APDU", "Read Card Access Data: ${toHex(readResponse)}")





            val readGeneric = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x20) // Read 32 bytes
            val readGenericResponse = isoDep.transceive(readGeneric)
            Log.d("APDU", "Read Generic Data: ${toHex(readGenericResponse)}")





            val readMoreData = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x50) // Read 80 bytes
            val readMoreResponse = isoDep.transceive(readMoreData)
            Log.d("APDU", "Read More Data: ${toHex(readMoreResponse)}")




            val selectEfIcc = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x00, 0x02, 0x2F.toByte(), 0x03)
            val iccResponse = isoDep.transceive(selectEfIcc)
            Log.d("APDU", "Select EF.ICC Response: ${toHex(iccResponse)}")

            val readIcc = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x20) // Read 32 bytes
            val readIccResponse = isoDep.transceive(readIcc)
            Log.d("APDU", "Read ICC Data: ${toHex(readIccResponse)}")






            val selectFile1 = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x00, 0x02, 0x2F.toByte(), 0x02) // EF.ATR or EF.INFO
            val selectFile2 = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x00, 0x02, 0x2F.toByte(), 0x01) // EF.CARD
            val selectFile3 = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x00, 0x02, 0x2F.toByte(), 0x04) // EF.SUPPORT

            val response1 = isoDep.transceive(selectFile1)
            Log.d("APDU", "Select EF.ATR Response1: ${toHex(response1)}")
            val response2 = isoDep.transceive(selectFile2)
            Log.d("APDU", "Select EF.ATR Response2: ${toHex(response2)}")
            val response3 = isoDep.transceive(selectFile3)
            Log.d("APDU", "Select EF.ATR Response3: ${toHex(response3)}")




            return "Success"
        } catch (e: Exception) {
            Log.e("APDU", "Error communicating with NFC tag", e)
            return "Error: ${e.message}"
        } finally {
            isoDep.close()
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

    fun ByteArray.toAscii(): String {
        return this.map { if (it in 0x20..0x7E) it.toInt().toChar() else '.' }
            .joinToString("")
    }


}

enum class NfcAvailability {
    Unknown,
    Available,
    NotAvailable,
    Disabled,
    Error
}