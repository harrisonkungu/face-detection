import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apps.facedetection.NFCCardScan.NFCViewModel
import com.apps.facedetection.NFCManager

/**
 * ViewModelFactory.kt
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

class NFCViewModelFactory(private val activity: Activity) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NFCViewModel::class.java)) {
            val nfcManager = NFCManager(activity)
            @Suppress("UNCHECKED_CAST")
            return NFCViewModel(nfcManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}