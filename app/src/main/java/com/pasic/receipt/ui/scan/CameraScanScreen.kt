package com.pasic.receipt.ui.scan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Image as ImageIcon
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.pasic.receipt.ui.scan.camera.CameraPermissionManager
import com.pasic.receipt.ui.scan.camera.CameraPreviewView
import com.pasic.receipt.ui.scan.camera.imageProxyToBitmap
import com.pasic.receipt.ui.scan.components.AiScanningOverlay
import com.pasic.receipt.ui.scan.components.CameraHelpDialog
import com.pasic.receipt.ui.scan.components.ReceiptScanFailureDialog
import com.pasic.receipt.ui.scan.components.ReceiptScanOverlay
import com.pasic.receipt.ui.scan.util.loadLatestGalleryThumbnail

@Composable
fun CameraScanScreen(
    viewModel: ScanSharedViewModel,
    onNavigateToResult: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Load recent gallery thumbnail image asynchronously on IO thread
    var recentGalleryBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            recentGalleryBitmap = loadLatestGalleryThumbnail(context)
        }
    }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processGalleryUri(context, uri) {
                onNavigateToResult()
            }
        }
    }

    CameraPermissionManager(
        onGalleryClick = { galleryLauncher.launch("image/*") }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. CameraX Preview
            CameraPreviewView(
                onImageCaptureCreated = { imageCapture = it },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Scanning Overlay Guide
            ReceiptScanOverlay()

            // 3. Top Header Bar (Left: Help Info Icon, Right: Close X Icon — No Square Background)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Left: Help Info Icon (No background)
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Info,
                        contentDescription = "촬영 팁 안내",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Top Right: Close Icon (No background)
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

            // 4. Bottom Controls (Left: Recent Gallery Thumbnail, Center: Shutter Button, Right: Empty)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 36.dp, vertical = 28.dp),
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
                        val bitmap = recentGalleryBitmap
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "최근 앨범 이미지",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.ImageIcon,
                                contentDescription = "갤러리 선택",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Center: Compact Shutter Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF38BDF8), Color(0xFF2563EB))
                            )
                        )
                        .clickable(enabled = !isCapturing && !uiState.isScanning) {
                            val capture = imageCapture ?: return@clickable
                            isCapturing = true

                            capture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = imageProxyToBitmap(image)
                                        image.close()
                                        isCapturing = false

                                        viewModel.processBitmap(context, bitmap) {
                                            onNavigateToResult()
                                        }
                                    }

                                    override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                                        exception.printStackTrace()
                                        isCapturing = false
                                    }
                                }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // Right: Kept empty for symmetry
                Spacer(modifier = Modifier.size(52.dp))
            }

            // 5. AI Scanning Progress Translucent Overlay
            if (uiState.isScanning) {
                AiScanningOverlay(
                    scanStep = uiState.scanStep,
                    isStepDone = uiState.isStepDone,
                    capturedBitmap = uiState.capturedBitmap,
                    onCancel = { viewModel.cancelScanning() }
                )
            }

            // 6. Camera Help Dialog
            if (showHelpDialog) {
                CameraHelpDialog(onDismiss = { showHelpDialog = false })
            }

            // 7. Receipt Scan Failure Dialog
            if (uiState.isScanFailed) {
                ReceiptScanFailureDialog(
                    onRetry = { viewModel.resetScanFailure() },
                    onContactSupport = {
                        viewModel.resetScanFailure()
                        onClose()
                    },
                    onDismiss = { viewModel.resetScanFailure() }
                )
            }
        }
    }
}
