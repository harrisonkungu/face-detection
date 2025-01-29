package com.apps.facedetection.FaceDetection


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import com.apps.facedetection.FaceDetection.FaceDetector.Companion.blinkCount
import com.apps.facedetection.FaceDetection.FaceDetector.Companion.checksPassed
import com.apps.facedetection.FaceDetection.FaceDetector.Companion.hasTurnedLeft
import com.apps.facedetection.FaceDetection.FaceDetector.Companion.hasTurnedRight
import com.apps.facedetection.R
import com.chargemap.compose.numberpicker.ListItemPicker
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executor
import kotlin.math.abs
import kotlin.math.absoluteValue


const val OVAL_WIDTH_DP = 300
const val OVAL_HEIGHT_DP = 350
private const val OVAL_LEFT_OFFSET_RATIO = 2
private const val OVAL_TOP_OFFSET_RATIO = 3

@Composable
fun SmoothScrollingList() {
    val messages = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val itemHeight = 80.dp // Adjust as needed
    val itemWidth = 150.dp // Adjust as needed
    val itemHeightPx = with(LocalDensity.current) { itemHeight.roundToPx() }

    // Simulate adding new messages
    LaunchedEffect(Unit) {
        for (i in 1..20) {
            delay(500)
            messages.add("Message $i")
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LazyColumn(
            modifier = Modifier
                .width(itemWidth)
                .align(Alignment.Center),
            state = listState,
            reverseLayout = false,
            verticalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(
                top = itemHeight * 1.5f,
                bottom = itemHeight * 1.5f
            )
        ) {
            items(messages) { message ->
                MessageItem(message, listState, itemHeight)
            }
        }
    }

    // Scroll to the bottom when new messages are added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }
}

@Composable
fun MessageItem(
    message: String,
    listState: LazyListState,
    itemHeight: Dp
) {
    val itemIndex = listState.layoutInfo.visibleItemsInfo.find { it.key == message }?.index ?: -1
    val isVisible = itemIndex != -1
    val isMiddleItem =
        listState.layoutInfo.visibleItemsInfo.size == 3 && listState.layoutInfo.visibleItemsInfo[1].key == message

    val alpha by animateFloatAsState(
        targetValue = if (isMiddleItem) 1f else 0.5f,
        label = "alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isMiddleItem) 1f else 0.8f,
        label = "scale"
    )

    val offset by animateFloatAsState(
        targetValue = if (isMiddleItem) 0f else if (itemIndex < listState.firstVisibleItemIndex) -itemHeight.value else itemHeight.value,
        label = "offset"
    )

    Column(
        modifier = Modifier
            .height(itemHeight)
            .offset(y = offset.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}



//@Composable
//fun SmoothPeekViewList() {
//    val itemsList = (1..10).map { "Item $it" }
//    val density = LocalDensity.current
//
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(vertical = 20.dp),
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        itemsIndexed(itemsList) { index, item ->
//            var alpha = when (index) {
//                1 -> 1f
//                0, 2 -> 0.5f
//                else -> 0f
//            }
//            val blur = when (index) {
//                1 -> 0.dp
//                0, 2 -> 8.dp
//                else -> 16.dp
//            }
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(80.dp)
//                    .graphicsLayer {
//                        translationY = if (index == 1) 0f else 0f
//                        alpha = alpha
//                    }
//                    .blur(blur)
//                    .padding(horizontal = 16.dp)
//            ) {
//                Text(
//                    text = item,
//                    fontSize = 20.sp,
//                    modifier = Modifier.align(alignment = androidx.compose.ui.Alignment.Center)
//                )
//            }
//        }
//    }
//}

//@Composable
//fun SmoothPeekViewList() {
//    val itemsList = (1..10).map { "Item $it" }
//    var listHeight by remember { mutableStateOf(0f) } // Changed to Float
//    var itemHeight by remember { mutableStateOf(0f) } // Changed to Float
//
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .onGloballyPositioned { layoutCoordinates ->
//                listHeight = layoutCoordinates.size.height.toFloat() // Convert to Float
//            },
//        verticalArrangement = Arrangement.spacedBy(15.dp)
//    ) {
//        itemsIndexed(itemsList) { index, item ->
//            var itemOffset by remember { mutableStateOf(Offset(0f, 0f)) }
//            val isCentered = itemOffset.y in (listHeight / 2 - itemHeight / 2)..(listHeight / 2 + itemHeight / 2)
//            val blur = if (isCentered) 0.dp else 16.dp
//            val alpha = animateFloatAsState(targetValue = if (isCentered) 1f else 0.5f)
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(10.dp)
//                    .onGloballyPositioned { coordinates ->
//                        itemOffset = coordinates.positionInParent()
//                        itemHeight = coordinates.size.height.toFloat() // Convert to Float
//                    }
//                    .blur(blur)
//                    .alpha(alpha.value)
//                    .padding(horizontal = 16.dp)
//            ) {
//                Text(
//                    text = item,
//                    fontSize = 10.sp,
//                    modifier = Modifier.align(alignment = androidx.compose.ui.Alignment.Center)
//                )
//            }
//        }
//    }
//}



//@Composable
//fun SmoothPeekViewList() {
//    val itemsList = (1..10).map { "Item $it" }
//    val listState = rememberLazyListState()
//    var listHeight by remember { mutableStateOf(0f) }
//    var itemHeight by remember { mutableStateOf(0f) }
//
//    LazyColumn(
//        state = listState,
//        modifier = Modifier
//            .fillMaxSize()
//            .onGloballyPositioned { layoutCoordinates ->
//                listHeight = layoutCoordinates.size.height.toFloat()
//            },
//        verticalArrangement = Arrangement.spacedBy(15.dp),
//        contentPadding = PaddingValues(vertical = (listHeight / 2 - itemHeight / 2).dp)
//    ) {
//        itemsIndexed(itemsList) { index, item ->
//            var itemOffset by remember { mutableStateOf(Offset(0f, 0f)) }
//
//            val distanceFromCenter = (listHeight / 2 - (itemOffset.y + itemHeight / 2)).absoluteValue
//            val visibilityFactor = (1 - (distanceFromCenter / (listHeight / 2))).coerceIn(0f, 1f)
//
//            val blur = animateFloatAsState(targetValue = (1 - visibilityFactor) * 16f)
//            val alpha = animateFloatAsState(targetValue = visibilityFactor)
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(80.dp)
//                    .onGloballyPositioned { coordinates ->
//                        itemOffset = coordinates.positionInParent()
//                        itemHeight = coordinates.size.height.toFloat()
//                    }
//                    .blur(blur.value.dp)
//                    .alpha(alpha.value)
//                    .padding(horizontal = 16.dp)
//            ) {
//                Text(
//                    text = item,
//                    fontSize = 20.sp,
//                    modifier = Modifier.align(alignment = androidx.compose.ui.Alignment.Center)
//                )
//            }
//        }
//    }
//
//    // LaunchedEffect to snap to the nearest item after scrolling
//    LaunchedEffect(listState.isScrollInProgress) {
//        if (!listState.isScrollInProgress) {
//            delay(100) // Small delay to ensure scroll has completely stopped
//            val centerItemIndex = (listState.firstVisibleItemIndex +
//                    if (abs(listState.firstVisibleItemScrollOffset) > itemHeight / 2) 1 else 0)
//            listState.animateScrollToItem(centerItemIndex)
//        }
//    }
//}





@Composable
fun SmoothPeekViewList() {
    val itemsList = (1..10).map { "Item $it" }
    val listState = rememberLazyListState()
    var listHeight by remember { mutableStateOf(0f) }
    var itemHeight by remember { mutableStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                listHeight = layoutCoordinates.size.height.toFloat()
            },
        verticalArrangement = Arrangement.spacedBy(15.dp),
        contentPadding = PaddingValues(vertical = (listHeight / 2).dp - (itemHeight / 2).dp)
    ) {
        itemsIndexed(itemsList) { index, item ->
            var itemOffset by remember { mutableStateOf(Offset(0f, 0f)) }

            val distanceFromCenter = (listHeight / 2 - (itemOffset.y + itemHeight / 2)).absoluteValue
            val visibilityFactor = (1 - (distanceFromCenter / (listHeight / 2))).coerceIn(0f, 1f)

            val blur = animateFloatAsState(targetValue = (1 - visibilityFactor) * 16f)
            val alpha = animateFloatAsState(targetValue = visibilityFactor)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .onGloballyPositioned { coordinates ->
                        itemOffset = coordinates.positionInParent()
                        itemHeight = coordinates.size.height.toFloat()
                    }
                    .blur(blur.value.dp)
                    .alpha(alpha.value)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = item,
                    fontSize = 20.sp,
                    modifier = Modifier.align(alignment = androidx.compose.ui.Alignment.Center)
                )
            }
        }
    }

    // LaunchedEffect to snap to the nearest item after scrolling
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            // Delay to ensure scroll has stopped
            delay(100)

            // Find the center of the list and calculate the index of the item closest to the center
            val centerPosition = listHeight / 2
            val closestItemIndex = listState.layoutInfo.visibleItemsInfo.minByOrNull {
                abs(it.offset + itemHeight / 2 - centerPosition)
            }?.index

            // Animate scroll to center the closest item
            closestItemIndex?.let {
                listState.animateScrollToItem(it)
            }
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> InfiniteCircularList(
    width: Dp,
    itemHeight: Dp,
    numberOfDisplayedItems: Int = 3,
    items: List<T>,
    initialItem: T,
    itemScaleFact: Float = 1.5f,
    textStyle: TextStyle,
    textColor: Color,
    onItemSelected: (index: Int, item: T) -> Unit = { _, _ -> }
) {
    val itemHalfHeight = LocalDensity.current.run { itemHeight.toPx() / 2f }
    val scrollState = rememberLazyListState(0)
    var lastSelectedIndex by remember { mutableIntStateOf(0) }
    var itemsState by remember { mutableStateOf(items) }

    LaunchedEffect(items) {
        var targetIndex = items.indexOf(initialItem) - 1
        targetIndex += ((Int.MAX_VALUE / 2) / items.size) * items.size
        itemsState = items
        lastSelectedIndex = targetIndex
        scrollState.scrollToItem(targetIndex)
    }

    LazyColumn(
        modifier = Modifier
            .width(width)
            .height(itemHeight * numberOfDisplayedItems),
        state = scrollState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = scrollState)
    ) {
        items(
            count = Int.MAX_VALUE,
            itemContent = { i ->
                val item = itemsState[i % itemsState.size]
                var itemOffset by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            val y = coordinates.positionInParent().y - itemHalfHeight
                            itemOffset = y
                            val parentHalfHeight =
                                (coordinates.parentCoordinates?.size?.height ?: 0) / 2f
                            val isSelected =
                                (y > parentHalfHeight - itemHalfHeight && y < parentHalfHeight + itemHalfHeight)
                            if (isSelected && lastSelectedIndex != i) {
                                onItemSelected(i % itemsState.size, item)
                                lastSelectedIndex = i
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val distanceFromCenter = (itemOffset - itemHalfHeight).absoluteValue
                    val visibilityFactor = (1 - (distanceFromCenter / itemHalfHeight)).coerceIn(0f, 1f)
                    val blur = animateFloatAsState(targetValue = (1 - visibilityFactor) * 16f,
                        label = ""
                    )
                    val alpha = animateFloatAsState(targetValue = visibilityFactor, label = "")

                    Text(
                        text = item.toString(),
                        style = textStyle,
                        color = textColor,
                        fontSize = textStyle.fontSize * if (lastSelectedIndex == i) itemScaleFact else 1f,
                        modifier = Modifier
                            .blur(blur.value.dp)
                            .alpha(alpha.value)
                    )
                }
            }
        )
    }
}





//@SuppressLint("RememberReturnType")
//@Composable
//fun FaceDetectionScreen() {
//    val context: Context = LocalContext.current
//    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
//
//    var isCameraShown by remember { mutableStateOf(true) }
//    var isFaceDetected by remember { mutableStateOf(false) }
//    var imageCorrect = remember { mutableStateOf(false) }
//
//    var capturedPhoto by remember { mutableStateOf<ImageBitmap?>(null) }
//    var ovalCenter by remember { mutableStateOf<Offset?>(null) }
//
//    val cameraController: LifecycleCameraController =
//        remember { LifecycleCameraController(context) }
//    val cameraProvider = remember { ProcessCameraProvider.getInstance(context) }
//    var isFrontCameraAvailable = remember { mutableStateOf(false) }
//    var isBackCameraAvailable = remember { mutableStateOf(false) }
//    var isUsingFrontCamera = remember { mutableStateOf(true) }
//
//
//
//    val cameraPreviewView = remember {
//        mutableStateOf(PreviewView(context))
//    }
//
//    Scaffold(
//        modifier = Modifier
//            .fillMaxSize()
//            .navigationBarsPadding()
//            .statusBarsPadding(),
//    ) { paddingValues: PaddingValues ->
//        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//
//            if(!imageCorrect.value) {
//                if (isCameraShown) {
//                    CameraView(paddingValues, cameraPreviewView)
//
//                } else {
//                    capturedPhoto?.let { photo ->
//                        CapturedPhotoView(photo)
//                    }
//                }
//                OvalOverlay(
//                    modifier = Modifier.fillMaxSize(),
//                    isFaceDetected = isFaceDetected,
//                    onCenterCalculated = { ovalCenter = it },
//                    autoCapture = {
//                        capturePhotoAndReplaceBackground(
//                            context,
//                            cameraController,
//                            isFaceDetected
//                        ) { capturedBitmap ->
//                            capturedPhoto = capturedBitmap.asImageBitmap()
//                            if (isFaceDetected)
//                                isCameraShown = false
//                        }
//                    }
//                )
//                ovalCenter?.let { offset ->
//                    startFaceDetection(
//                        context = context,
//                        cameraController = cameraController,
//                        lifecycleOwner = lifecycleOwner,
//                        previewView = cameraPreviewView.value,
//                        ovalRect = offset,
//                        onFaceDetected = { detected ->
//                            isFaceDetected = detected
//                        },
//                    )
//                }
//                if (isCameraShown) {
//                    Column(modifier = Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
//
//                        CapturePhotoButton(
//                            modifier = Modifier
//                                .padding(bottom = 10.dp),
//                            isFaceDetected = isFaceDetected,
//                            onButtonClicked = {
//                                capturePhotoAndReplaceBackground(
//                                    context,
//                                    cameraController,
//                                    isFaceDetected
//                                ) { capturedBitmap ->
//                                    capturedPhoto = capturedBitmap.asImageBitmap()
//                                    if (isFaceDetected)
//                                        isCameraShown = false
//                                }
//                            },
//                        )
//
//                        CameraToggleButton(cameraController, cameraProvider, isFrontCameraAvailable, isBackCameraAvailable, isUsingFrontCamera)
//                    }
//
//
//
//                } else {
//                    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
//
//                        RecaptureButton(
//                            modifier = Modifier
//                                .padding(bottom = 20.dp),
//                            onButtonClicked = {
//                                isCameraShown = true
//                                isFaceDetected = false
//                                hasTurnedLeft.value = false
//                                hasTurnedRight.value = false
//                                blinkCount = 0
//                            },
//                        )
//                        CorrectButton(
//                            modifier = Modifier
//                                .padding(bottom = 50.dp),
//                            onButtonClicked = {
//                                imageCorrect.value = true
//                                hasTurnedLeft.value = false
//                                hasTurnedRight.value = false
//                                blinkCount = 0
//                            },
//                        )
//                    }
//
//
//                }
//
//            }else if (imageCorrect.value && capturedPhoto!=null){
//                capturedPhoto?.let { photo ->
//                    CorrectImageAvatar(photo)
//                }
//            }
//        }
//
//    }
//}


@SuppressLint("RememberReturnType")
@Composable
fun FaceDetectionScreen() {
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
    val loaderState = remember { mutableStateOf(false) }



    val cameraPreviewView = remember {
        mutableStateOf(PreviewView(context))
    }
    val scope = rememberCoroutineScope()


    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding(),
    ) { paddingValues: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

            if(!imageCorrect.value) {
                if (isCameraShown) {
                    CameraView(paddingValues, cameraPreviewView)

                } else {
                    capturedPhoto?.let { photo ->
                        CapturedPhotoView(photo)
                    }
                }


                OvalOverlay(
                    modifier = Modifier.fillMaxSize(),
                    isFaceDetected = isFaceDetected,
                    onCenterCalculated = { ovalCenter = it },
                    isLoading = loaderState,
                    autoCapture = {
                        capturePhotoAndReplaceBackground(
                            context,
                            cameraController,
                            scope,
                            isFaceDetected,
                            loaderState
                        ) { capturedBitmap ->
                            capturedPhoto = capturedBitmap.asImageBitmap()
                            if (isFaceDetected)
                                isCameraShown = false
                        }
                    }
                )
                ovalCenter?.let { offset ->
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
                    Column(modifier = Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {

                        CapturePhotoButton(
                            modifier = Modifier
                                .padding(bottom = 10.dp),
                            isFaceDetected = isFaceDetected,
                            loaderState = loaderState,
                            onButtonClicked = {
                                capturePhotoAndReplaceBackground(
                                    context,
                                    cameraController,
                                    scope,
                                    isFaceDetected,
                                    loaderState
                                ) { capturedBitmap ->
                                    capturedPhoto = capturedBitmap.asImageBitmap()
                                    if (isFaceDetected)
                                        isCameraShown = false
                                }
                            },
                        )

                        CameraToggleButton(cameraController, cameraProvider, isFrontCameraAvailable, isBackCameraAvailable, isUsingFrontCamera)
                    }



                } else {
                    Column(modifier = Modifier.align(Alignment.BottomCenter)) {

                        RecaptureButton(
                            modifier = Modifier
                                .padding(bottom = 20.dp),
                            onButtonClicked = {
                                isCameraShown = true
                                isFaceDetected = false
                                hasTurnedLeft.value = false
                                hasTurnedRight.value = false
                                blinkCount = 0
                            },
                        )
                        CorrectButton(
                            modifier = Modifier
                                .padding(bottom = 50.dp),
                            onButtonClicked = {
                                imageCorrect.value = true
                                hasTurnedLeft.value = false
                                hasTurnedRight.value = false
                                blinkCount = 0
                            },
                        )
                    }


                }

            }else if (imageCorrect.value && capturedPhoto!=null){
                capturedPhoto?.let { photo ->


                    ScrollableColumnWithTitleImageAndButtons(
                        title = "Photo captured successfully",
                        photo =photo,
                        button1Text = "Update",
                        onButton1Click = {  },
                        button2Text = "Cancel",
                        onButton2Click = {  }
                    )
//
                }
            }
        }

    }
}


@Composable
fun CameraToggleButton(
    cameraController: LifecycleCameraController,
    cameraProvider: ListenableFuture<ProcessCameraProvider>,
    isFrontCameraAvailable: MutableState<Boolean>,
    isBackCameraAvailable: MutableState<Boolean>,
    isUsingFrontCamera: MutableState<Boolean>,
) {

    LaunchedEffect(cameraProvider) {
        cameraProvider.get().run {
            isFrontCameraAvailable.value = hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            isBackCameraAvailable.value = hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        }
    }

    LaunchedEffect(isUsingFrontCamera.value) {
        val lensFacing = if (isUsingFrontCamera.value) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        cameraController.cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }



    FlipCamera(
        modifier = Modifier.padding(bottom = 20.dp),
        onButtonClicked = {
            isUsingFrontCamera.value = !isUsingFrontCamera.value
            hasTurnedLeft.value = false
            hasTurnedRight.value = false
            blinkCount = 0
        },
        enabled = (isUsingFrontCamera.value && isBackCameraAvailable.value) ||
                (!isUsingFrontCamera.value && isFrontCameraAvailable.value),
    )

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
//        contentScale = ContentScale.Crop,
    )
}



@Composable
private fun CorrectImageAvatar(photo: ImageBitmap) {
    Box(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .size(350.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Gray)
            .border(2.dp, Color.White, RoundedCornerShape(16.dp))
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            bitmap = photo,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
}







//import coil.compose.rememberAsyncImagePainter
//import com.mcoopcash.yea.R

@Composable
fun ScrollableColumnWithTitleImageAndButtons(
    title: String,
    photo: ImageBitmap, // Can be a String (URL), Int (drawable resource), or null
    button1Text: String,
    onButton1Click: () -> Unit,
    button2Text: String,
    onButton2Click: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )



        // Image Avatar
        CorrectImageAvatar(photo = photo)

        Spacer(modifier = Modifier.height(32.dp))

        // Button 1
        Button(
            onClick = onButton1Click,
            modifier = Modifier.padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text = button1Text, color = Color.White)
        }

        // Button 2
        Button(
            onClick = onButton2Click,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(text = button2Text, color = Color.White)
        }
    }
}

//@Composable
//fun CorrectImageAvatar(photo: Any?) {
//    val painter = when (photo) {
//        is String -> rememberAsyncImagePainter(
//            model = photo,
//            placeholder = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with your placeholder
//            error = painterResource(id = R.drawable.ic_launcher_foreground) // Replace with your error image
//        )
//        is Int -> painterResource(id = photo)
//        else -> painterResource(id = R.drawable.ic_launcher_foreground) // Default placeholder if null or invalid type
//    }
//
//    Image(
//        painter = painter,
//        contentDescription = "Avatar",
//        modifier = Modifier
//            .size(120.dp)
//            .clip(CircleShape),
//        contentScale = ContentScale.Crop
//    )
//}



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
    onButtonClicked: () -> Unit,
    loaderState: MutableState<Boolean>
) {
    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        BlinkingText(loaderState)
        Image(
            modifier = modifier
                .padding(top = 10.dp)
                .size(50.dp)
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

}

@Composable
fun BlinkingText(loaderState: MutableState<Boolean>) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )


//    val possibleValues = remember {
//        listOf(
//            ""
//        )
//    }
//    var state by remember { mutableStateOf(possibleValues[0]) }
//    ListItemPicker(
//        modifier = Modifier.fillMaxWidth(),
//        label = { it },
//        value = state,
//        onValueChange = { state = it },
//        dividersColor = Color.Transparent,
//        textStyle = TextStyle.Default.copy(color = Color.White, fontSize = 16.sp),
//        list = possibleValues
//    )
//


    val checkCount by checksPassed.collectAsState(initial = 0)

//    "Face Detected: Ensure your face is fully visible in the frame.",
//    "Blink Detection: Blink naturally when prompted.",
//    "Looking Straight: Look directly at the camera.",
//    "Head Turn (Right): Turn your head slowly to the right.",
//    "Head Turn (Left): Turn your head slowly to the left.",
//    "Face Not Far Away: Stay at a comfortable distance from the camera."

    val possibleValues = remember {
        listOf(
            "Ensure your face is fully visible",
            "Look directly at the camera",
            "Head Turn (Right).",
            "Blink eyes",
            "Move Closer",
            "Okay",
            "Saving..."
        )
    }

    // Mutable list to keep track of failed tests
    var failedTests by remember { mutableStateOf(emptyList<String>()) }

    // Sample function to simulate test results
    fun updateFailedTests(test: String) {
        if (!failedTests.contains(test)) {
            failedTests = failedTests + test
        }
    }

//    var state by remember { mutableStateOf(failedTests.getOrElse(4) { "" }) }
    var state by remember { mutableStateOf(possibleValues[checkCount]) }

    ListItemPicker(
        modifier = Modifier.fillMaxWidth(),
        label = { it },
        value = possibleValues[checkCount],
        onValueChange = { state = it },
        dividersColor = Color.Transparent,
        textStyle = TextStyle.Default.copy(color = Color.White, fontSize = 16.sp),
        list = possibleValues
    )


}



@Composable
private fun RecaptureButton(
    modifier: Modifier = Modifier,
    onButtonClicked: () -> Unit
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(durationMillis = 700, easing = LinearEasing), // Smooth animation
        label = ""
    )

    Image(
        modifier = modifier
            .padding(top = 20.dp)
            .size(50.dp)
            .graphicsLayer(
                rotationZ = animatedRotation
            )
            .clickable {
                rotationAngle += 360f
                onButtonClicked()
            },
        painter = painterResource(id = R.drawable.recapture_24),
        contentDescription = null,
    )
}

@Composable
private fun FlipCamera(
    modifier: Modifier = Modifier,
    onButtonClicked: () -> Unit,
    enabled :Boolean = false
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(durationMillis = 700, easing = LinearEasing), // Smooth animation
        label = ""
    )
    if (enabled){
        Image(
            modifier = modifier
                .padding(top = 20.dp)
                .size(50.dp)
                .graphicsLayer(
                    rotationZ = animatedRotation
                )
                .clickable {
                    rotationAngle += 180f
                    onButtonClicked()
                },
            painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
            contentDescription = null,
        )
    }else{
        Image(
            modifier = modifier
                .padding(top = 20.dp)
                .size(50.dp)
                .graphicsLayer(
                    rotationZ = animatedRotation
                ),
            painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.Gray)
        )
    }

}



@Composable
private fun CorrectButton(
    modifier: Modifier = Modifier,
    onButtonClicked: () -> Unit
) {
    var isZoomedIn by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isZoomedIn) 1.5f else 1f,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = ""
    )
    Image(
        modifier = modifier
            .padding(top = 20.dp)
            .size(50.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .clickable {
                isZoomedIn = !isZoomedIn
                onButtonClicked()
            },
        painter = painterResource(id = R.drawable.correct_check_24),
        contentDescription = null,
    )
}


fun capturePhotoAndReplaceBackground(
    context: Context,
    cameraController: LifecycleCameraController,
    scope: CoroutineScope,
    isFaceDetected: Boolean,
    loaderState: MutableState<Boolean>,
    onBackgroundReplaced: (Bitmap) -> Unit,
) {

    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    if (isFaceDetected) {
        scope.launch {
//            loaderState.value = true
            delay(1000) // 3 seconds delay
            cameraController.takePicture(
                mainExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {


                        try {
                            val buffer = image.planes[0].buffer
                            buffer.rewind()
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            var bitmap =  BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                            if (cameraController.cameraInfo!!.lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                bitmap = bitmap.flipHorizontally()
                            }
                            val degrees = image.imageInfo.rotationDegrees
                            val fileUri = saveBitmapToFile(context, bitmap, "captured_image.jpg")
                            if (degrees != 0) {
                                //samsung
                                bitmap = rotateBitmap(bitmap, degrees)
                                processCapturedPhotoAndReplaceBackground(true, bitmap, context, fileUri) { processedBitmap ->
                                    if (fileUri != null) {
                                        scope.launch {
                                            val validImageFeatures= validateImageFeatures(processedBitmap)
                                            if (validImageFeatures){
                                                Log.d("FACE DETECT", "Detected face successfully")
//                                                Toast.makeText(context, "Done Processing1", Toast.LENGTH_SHORT).show()
                                                onBackgroundReplaced(processedBitmap)
                                            }else{
                                                Toast.makeText(context, "Invalid Image", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }else{
                                        Toast.makeText(context, "Unable to save the mage", Toast.LENGTH_SHORT).show()
                                    }

                                }
                            }else{
                                processCapturedPhotoAndReplaceBackground(false, bitmap, context, fileUri) { processedBitmap ->
                                    if (fileUri != null) {

                                        scope.launch {
                                            val validImageFeatures= validateImageFeatures(processedBitmap)
                                            if (validImageFeatures){
                                                Log.d("FACE DETECT", "Detected face successfully")
//                                                Toast.makeText(context, "Done Processing1", Toast.LENGTH_SHORT).show()
                                                onBackgroundReplaced(processedBitmap)
                                            }else{
                                                Toast.makeText(context, "Invalid Image", Toast.LENGTH_SHORT).show()
                                            }
                                        }



                                    }else{
                                        Toast.makeText(context, "Unable to save the image", Toast.LENGTH_SHORT).show()
                                    }

                                }

                                if (fileUri != null) {
                                    println("Captured image URI: $fileUri")
                                } else {
                                    println("Failed to save captured image to cache")
                                }

                            }

                            image.close()
                        }catch (e:Exception){
                            println("Failed to save captured image to cache $e")
                        }finally {
                            loaderState.value = false
                        }

                    }

                    override fun onError(exception: ImageCaptureException) {
                        exception.printStackTrace()
                    }
                },
            )
        }

    }
}

fun Bitmap.flipHorizontally(): Bitmap {
    val matrix = Matrix().apply { postScale(-1f, 1f, width / 2f, height / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}


fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): Uri? {
    return try {
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    val rotationMatrix = Matrix()
    rotationMatrix.postRotate(rotationDegrees.toFloat())
    val rotatedBitmap =
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotationMatrix, true)
    bitmap.recycle()
    return rotatedBitmap
}

fun processCapturedPhotoAndReplaceBackground(
    rotate: Boolean,
    capturedBitmap: Bitmap,
    context: Context,
    fileUrl: Uri?,
    onBackgroundReplaced: (Bitmap) -> Unit,
) {
    val rawBitmap = if (rotate) {
        handleSamplingAndRotationBitmap(context, fileUrl)
    } else {
        capturedBitmap
    }

    val squareSize = rawBitmap.width.coerceAtMost(rawBitmap.height)
    val cropStartX = ((rawBitmap.width - squareSize) / 2).coerceAtLeast(0)
    val cropStartY = ((rawBitmap.height - squareSize) / 2).coerceAtLeast(0)
    val croppedBitmap = Bitmap.createBitmap(
        rawBitmap,
        cropStartX,
        cropStartY,
        squareSize,
        squareSize
    )

    onBackgroundReplaced(croppedBitmap)
}

@Throws(IOException::class)
fun handleSamplingAndRotationBitmap(context: Context, selectedImage: Uri?): Bitmap {
    val MAX_HEIGHT = 1024
    val MAX_WIDTH = 1024
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    var imageStream = context.contentResolver.openInputStream(selectedImage!!)
    BitmapFactory.decodeStream(imageStream, null, options)
    imageStream!!.close()
    options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT)
    options.inJustDecodeBounds = false
    imageStream = context.contentResolver.openInputStream(selectedImage)
    var img = BitmapFactory.decodeStream(imageStream, null, options)
    img = rotateImageIfRequired(context, img!!, selectedImage)
    return img
}

fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int, reqHeight: Int
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
        val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
        inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        val totalPixels = (width * height).toFloat()
        val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()

        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }
    }
    return inSampleSize
}

@Throws(IOException::class)
private fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap {
    val input = context.contentResolver.openInputStream(selectedImage)
    val exif = ExifInterface(input!!)
    if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equals("6", ignoreCase = true)) {
        return rotateImage(img, 90)
    } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equals("8", ignoreCase = true)) {
        return rotateImage(img, 270)
    } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equals("3", ignoreCase = true)) {
        return rotateImage(img, 180)
    } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equals("0", ignoreCase = true)) {
        return  rotateImage(img, 90)
    }else{
        return img
    }
}

fun rotateImage(img: Bitmap, degree: Int): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degree.toFloat())
    val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    img.recycle()
    return rotatedImg
}





@Composable
private fun OvalOverlay(
    modifier: Modifier = Modifier,
    onCenterCalculated: (Offset) -> Unit = {},
    isFaceDetected: Boolean,
    autoCapture: () -> Unit,
    isLoading: MutableState<Boolean> // New parameter for loader state
) {
    val checkCount by checksPassed.collectAsState(initial = 0)
    val sweepAngle by animateFloatAsState(
        targetValue = checkCount * 72f, // Each check contributes 120 degrees
        animationSpec = tween(
            durationMillis = 400,
            easing = LinearEasing
        ), label = ""
    )

    LaunchedEffect(sweepAngle) {
        if (sweepAngle >= 360f) {
            isLoading.value = true
            autoCapture()
        }
    }

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

            val ovalRect = Rect(
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

        if (isLoading.value) {
            val infiniteTransition = rememberInfiniteTransition()
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = ""
            )

            Canvas(modifier = modifier) {
                val ovalSize = Size(OVAL_WIDTH_DP.dp.toPx(), OVAL_HEIGHT_DP.dp.toPx())
                val ovalLeft = (size.width - ovalSize.width) / OVAL_LEFT_OFFSET_RATIO
                val ovalTop = (size.height - ovalSize.height) / OVAL_TOP_OFFSET_RATIO - ovalSize.height

                drawOval(
                    color = Color.Gray,
                    style = Stroke(width = OVAL_TOP_OFFSET_RATIO.dp.toPx()),
                    topLeft = Offset(ovalLeft, ovalTop + ovalSize.height),
                    size = ovalSize,
                )

                drawArc(
                    color = Color(0xFF21FDFF),
                    startAngle = rotation,
                    sweepAngle = 90f, // Adjust to show parts of the arc
                    useCenter = false,
                    topLeft = Offset(ovalLeft, ovalTop + ovalSize.height),
                    size = ovalSize,
                    style = Stroke(width = 10f)
                )
            }
        } else {
            Canvas(modifier = modifier) {
                val ovalSize = Size(OVAL_WIDTH_DP.dp.toPx(), OVAL_HEIGHT_DP.dp.toPx())
                val ovalLeft = (size.width - ovalSize.width) / OVAL_LEFT_OFFSET_RATIO
                val ovalTop = (size.height - ovalSize.height) / OVAL_TOP_OFFSET_RATIO - ovalSize.height

                drawOval(
                    color = Color.Gray,
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
}
