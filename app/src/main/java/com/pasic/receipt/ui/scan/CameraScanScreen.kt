package com.pasic.receipt.ui.scan

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Image as ImageIcon
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ReceiptText
import com.composables.icons.lucide.Scan
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.X

@Composable
fun CameraScanScreen(
    viewModel: ScanSharedViewModel,
    onNavigateToResult: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Load recent gallery thumbnail image
    val recentGalleryBitmap = remember { loadLatestGalleryThumbnail(context) }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isCapturing = true
            viewModel.processGalleryUri(context, uri) {
                isCapturing = false
                onNavigateToResult()
            }
        }
    }

    CameraPermissionHandler(
        onGalleryClick = { galleryLauncher.launch("image/*") }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. CameraX Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Scanning Overlay
            ReceiptScanOverlay()

            // 3. Top Right Close Button [X] (No Background)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "닫기",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 4. Bottom Controls (Recent Gallery Photo Thumbnail, Smaller Shutter, Sample Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 36.dp, vertical = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Recent Gallery Thumbnail Button
                Surface(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (recentGalleryBitmap != null) {
                            Image(
                                bitmap = recentGalleryBitmap.asImageBitmap(),
                                contentDescription = "최근 앨범 이미지",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.ImageIcon,
                                contentDescription = "갤러리",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Center: Smaller Compact Shutter Button (66dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF06B6D4), // Cyan
                                    Color(0xFF10B981), // Emerald
                                    Color(0xFF3B82F6), // Blue
                                    Color(0xFF06B6D4)  // Cyan
                                )
                            )
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color(0xFF64748B) else Color.White)
                        .clickable(enabled = !isCapturing) {
                            val capture = imageCapture
                            if (capture != null) {
                                isCapturing = true
                                capture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = imageProxyToBitmap(image)
                                            image.close()
                                            viewModel.processBitmap(context, bitmap) {
                                                isCapturing = false
                                                onNavigateToResult()
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                            viewModel.setSimulationResult(context) {
                                                isCapturing = false
                                                onNavigateToResult()
                                            }
                                        }
                                    }
                                )
                            } else {
                                isCapturing = true
                                viewModel.setSimulationResult(context) {
                                    isCapturing = false
                                    onNavigateToResult()
                                }
                            }
                        }
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color(0xFF06B6D4),
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Right: Help / Tips Button (Circle Help)
                Surface(
                    onClick = { showHelpDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Lucide.Info,
                            contentDescription = "도움말",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Camera Scan Help Dialog
        if (showHelpDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = {
                    Text("영수증 촬영 팁", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. 어두운 배경 위에 영수증을 평평하게 펼쳐주세요.", fontSize = 14.sp, color = Color(0xFF475569))
                        Text("2. 상호명, 결제일시, 합계금액이 선명하게 보이도록 촬영해 주세요.", fontSize = 14.sp, color = Color(0xFF475569))
                        Text("3. 조명 빛 반사가 적은 장소에서 찍으시면 인식 정확도가 훨씬 높아집니다.", fontSize = 14.sp, color = Color(0xFF475569))
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = { showHelpDialog = false },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("확인", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun ReceiptScanOverlay() {
    var isGuideVisible by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "magnifierAnim")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -16f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "magX"
    )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "magY"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        isGuideVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Clean subtle top/bottom scrim for readability
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.5f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.6f)
                    )
                ),
                size = size
            )
        }

        // Center Guide Content (Scan Frame + Moving Magnifier + 3-Line Text) — Fades out after 5 seconds
        androidx.compose.animation.AnimatedVisibility(
            visible = isGuideVisible,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(animationSpec = tween(600)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Overlapped Dual Icon Emblem (Scan Focus Frame + Moving Magnifying Glass)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(110.dp)
                    ) {
                        // Outer Layer: Scan Focus Frame Icon
                        Icon(
                            imageVector = Lucide.Scan,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(96.dp)
                        )

                        // Inner Layer: Moving Magnifying Glass Icon (Bounded inside Scan frame)
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(36.dp)
                                .offset(x = offsetX.dp, y = offsetY.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Multiline Guide Text
                    Text(
                        text = "직접 방문한 장소 영수증의\n사업자 정보와 결제 정보가\n잘 나오게 찍어주세요.",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 25.sp
                    )
                }
            }
        }
    }
}

private fun loadLatestGalleryThumbnail(context: Context): Bitmap? {
    return try {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = it.getLong(idColumn)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(120, 120), null)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Thumbnails.getThumbnail(
                        context.contentResolver,
                        id,
                        MediaStore.Images.Thumbnails.MINI_KIND,
                        null
                    )
                }
            } else null
        }
    } catch (e: Exception) {
        null
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes[0]
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val matrix = Matrix().apply {
        postRotate(image.imageInfo.rotationDegrees.toFloat())
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
